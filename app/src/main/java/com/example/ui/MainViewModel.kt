package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppPreferences
import com.example.data.PaymentParser
import com.example.data.PaymentRecord
import com.example.data.PaymentRepository
import com.example.data.PaymentServiceType
import com.example.service.PaymentSpeakerService
import com.example.tts.TtsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PaymentRepository(application)
    private val ttsManager = TtsManager.getInstance(application)

    val settings: StateFlow<AppPreferences> = repository.preferencesManager.settings
    val isServiceRunning: StateFlow<Boolean> = PaymentSpeakerService.isRunning
    val isSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking
    val lastSpokenText: StateFlow<String?> = ttsManager.lastSpokenText

    val todayCount: StateFlow<Int> = repository.getTodayPaymentCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayTotalAmount: StateFlow<Double> = repository.getTodayTotalAmount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val latestPayment: StateFlow<PaymentRecord?> = repository.latestPayment
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedFilter = MutableStateFlow<PaymentServiceType?>(null)
    val selectedFilter: StateFlow<PaymentServiceType?> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredPayments: StateFlow<List<PaymentRecord>> = combine(
        repository.allPayments,
        _selectedFilter,
        _searchQuery
    ) { payments, filter, query ->
        payments.filter { payment ->
            val matchesFilter = filter == null || payment.serviceType == filter
            val matchesQuery = query.isBlank() ||
                    payment.payerName?.contains(query, ignoreCase = true) == true ||
                    payment.payerPhone?.contains(query, ignoreCase = true) == true ||
                    payment.accountOrRef?.contains(query, ignoreCase = true) == true ||
                    payment.rawBody.contains(query, ignoreCase = true) ||
                    payment.amount.toString().contains(query)
            matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Automatically start the foreground service if enabled in preferences
        if (settings.value.isServiceEnabled) {
            PaymentSpeakerService.start(getApplication())
        }
    }

    fun setFilter(filter: PaymentServiceType?) {
        _selectedFilter.value = filter
    }

    fun startMonitoringIfEnabled() {
        if (settings.value.isServiceEnabled) PaymentSpeakerService.start(getApplication())
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleService(enabled: Boolean) {
        repository.preferencesManager.updateServiceEnabled(enabled)
        if (enabled) {
            PaymentSpeakerService.start(getApplication())
            // Announce that PayVoice is active
            ttsManager.speak("PayVoice payment alert is now active and listening.", settings.value)
        } else {
            ttsManager.stop()
            PaymentSpeakerService.stop(getApplication())
        }
    }

    fun updateRepeatCount(count: Int) {
        repository.preferencesManager.updateRepeatCount(count)
    }

    fun updateSpeechRate(rate: Float) {
        repository.preferencesManager.updateSpeechRate(rate)
    }

    fun updateSpeechPitch(pitch: Float) {
        repository.preferencesManager.updateSpeechPitch(pitch)
    }

    fun updateMaxVolumeOverride(override: Boolean) {
        repository.preferencesManager.updateMaxVolumeOverride(override)
    }

    fun updateKeepScreenOn(keep: Boolean) {
        repository.preferencesManager.updateKeepScreenOn(keep)
    }

    fun updateCbeEnabled(enabled: Boolean) {
        repository.preferencesManager.updateCbeEnabled(enabled)
    }

    fun updateTelebirrEnabled(enabled: Boolean) {
        repository.preferencesManager.updateTelebirrEnabled(enabled)
    }

    fun updateCustomTemplate(template: String) {
        repository.preferencesManager.updateCustomTemplate(template)
    }

    fun simulateSms(sender: String, body: String) {
        viewModelScope.launch {
            val parsed = PaymentParser.parse(sender, body)
            if (parsed != null) {
                val currentSettings = settings.value
                val speechAnnouncement = PaymentParser.generateDefaultSpeech(
                    serviceType = parsed.serviceType,
                    amount = parsed.amount,
                    payerName = parsed.payerName,
                    payerPhone = parsed.payerPhone,
                    customTemplate = currentSettings.customTemplate
                )

                val record = PaymentRecord(
                    serviceType = parsed.serviceType,
                    senderAddress = parsed.senderAddress,
                    amount = parsed.amount,
                    currency = parsed.currency,
                    payerName = parsed.payerName,
                    payerPhone = parsed.payerPhone,
                    accountOrRef = parsed.accountOrRef,
                    rawBody = parsed.rawBody,
                    timestamp = System.currentTimeMillis(),
                    announcedText = speechAnnouncement,
                    isAnnounced = currentSettings.isServiceEnabled
                )

                val id = repository.insertPayment(record)

                if (currentSettings.isServiceEnabled) {
                    PaymentSpeakerService.speak(getApplication(), speechAnnouncement, id)
                }
            } else {
                // If it couldn't be parsed as payment, let user know
                ttsManager.speak("Simulation test: SMS received from $sender, but no payment details could be parsed.", settings.value)
            }
        }
    }

    fun testSpeech(customText: String? = null) {
        val currentSettings = settings.value
        val textToSpeak = customText ?: "Test payment announcement. One thousand two hundred Birr received from Abebe Kebede via Telebirr."
        ttsManager.speak(textToSpeak, currentSettings)
    }

    fun replayPayment(payment: PaymentRecord) {
        PaymentSpeakerService.speak(getApplication(), payment.announcedText, payment.id)
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    fun deletePayment(payment: PaymentRecord) {
        viewModelScope.launch {
            repository.deletePayment(payment)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }
}
