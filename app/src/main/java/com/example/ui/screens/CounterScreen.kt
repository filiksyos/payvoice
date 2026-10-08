package com.example.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PaymentParser
import com.example.data.PaymentServiceType
import com.example.ui.MainViewModel
import com.example.ui.components.AudioWavePulse
import com.example.ui.components.ServicePulseBeacon
import com.example.ui.theme.CbePurple
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CounterScreen(
    viewModel: MainViewModel,
    hasSmsPermission: Boolean,
    onRequestSmsPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val isServiceRunning by viewModel.isServiceRunning.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val lastSpokenText by viewModel.lastSpokenText.collectAsState()
    val latestPayment by viewModel.latestPayment.collectAsState()
    val todayCount by viewModel.todayCount.collectAsState()
    val todayTotalAmount by viewModel.todayTotalAmount.collectAsState()

    val context = LocalContext.current

    // Keep screen on while on the counter tab if enabled in settings
    DisposableEffect(settings.keepScreenOn) {
        val window = (context as? Activity)?.window
        if (settings.keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SMS Permission Alert Banner (if needed)
        if (!hasSmsPermission) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = GoldAccent.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sms_permission_banner")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "SMS Permission Required",
                        tint = GoldAccent,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SMS Permission Required",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Grant SMS read/receive permissions so PayVoice can detect incoming CBE & Telebirr payments.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onRequestSmsPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        modifier = Modifier.testTag("grant_sms_permission_button")
                    ) {
                        Text("Grant", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Always-On Hero Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("service_status_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                if (settings.isServiceEnabled) EmeraldPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ServicePulseBeacon(isActive = settings.isServiceEnabled)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (settings.isServiceEnabled) "ALWAYS ON • LISTENING" else "SPEAKER MUTED",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (settings.isServiceEnabled) EmeraldPrimary else MaterialTheme.colorScheme.outline,
                                letterSpacing = 1.sp
                            )
                        }

                        Switch(
                            checked = settings.isServiceEnabled,
                            onCheckedChange = { viewModel.toggleService(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmeraldPrimary,
                                checkedTrackColor = EmeraldPrimary.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.testTag("service_toggle_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (settings.isServiceEnabled)
                            "Monitoring SMS from CBE & Telebirr in real-time. Incoming payments will be announced out loud instantly."
                        else
                            "Voice announcements are muted. Toggle the switch above to activate always-on listening.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Audio Wave or Test Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AudioWavePulse(
                                isSpeaking = isSpeaking,
                                activeColor = GoldAccent,
                                idleColor = EmeraldPrimary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isSpeaking) "Speaking payment..." else "Audio engine ready",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSpeaking) GoldAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSpeaking) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                if (isSpeaking) viewModel.stopSpeaking() else viewModel.testSpeech()
                            },
                            modifier = Modifier.testTag("test_speech_button")
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                contentDescription = "Test Voice",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isSpeaking) "Stop" else "Test Voice")
                        }
                    }
                }
            }
        }

        // Giant Counter Payment Display (The Main Feature for Merchants)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("latest_payment_display"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "LATEST PAYMENT RECEIVED",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (latestPayment != null) {
                    val payment = latestPayment!!
                    val serviceColor = when (payment.serviceType) {
                        PaymentServiceType.CBE -> CbePurple
                        PaymentServiceType.TELEBIRR -> EmeraldPrimary
                        PaymentServiceType.OTHER -> GoldAccent
                    }
                    val serviceName = when (payment.serviceType) {
                        PaymentServiceType.CBE -> "Commercial Bank of Ethiopia (CBE)"
                        PaymentServiceType.TELEBIRR -> "Ethio Telecom (Telebirr)"
                        PaymentServiceType.OTHER -> "Mobile Banking"
                    }

                    // Service pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = serviceColor.copy(alpha = 0.15f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = serviceColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = serviceName,
                                color = serviceColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Huge Amount Display
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "ETB ",
                            style = MaterialTheme.typography.headlineSmall,
                            color = serviceColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = PaymentParser.formatCurrencyAmount(payment.amount),
                            fontSize = 42.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val payer = when {
                        !payment.payerName.isNullOrBlank() -> payment.payerName
                        !payment.payerPhone.isNullOrBlank() -> payment.payerPhone
                        else -> "Anonymous Customer"
                    }

                    Text(
                        text = "From: $payer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val timeFormatted = remember(payment.timestamp) {
                        SimpleDateFormat("h:mm a • MMM d, yyyy", Locale.getDefault()).format(Date(payment.timestamp))
                    }

                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!payment.accountOrRef.isNullOrBlank()) {
                        Text(
                            text = "Txn: ${payment.accountOrRef}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Replay button
                    Button(
                        onClick = { viewModel.replayPayment(payment) },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .testTag("replay_latest_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = serviceColor)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = "Speak announcement out loud",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Replay Loudly",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    // Empty state
                    Spacer(modifier = Modifier.height(20.dp))
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Waiting for payments...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "When an SMS arrives from CBE or Telebirr, PayVoice will announce it out loud automatically.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Try the quick simulation buttons below to test:",
                        style = MaterialTheme.typography.labelMedium,
                        color = GoldAccent
                    )
                }
            }
        }

        // Today's Stats Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .testTag("today_count_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TODAY'S PAYMENTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$todayCount",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldPrimary
                    )
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .testTag("today_total_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TOTAL REVENUE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "ETB ${PaymentParser.formatCurrencyAmount(todayTotalAmount)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldAccent
                    )
                }
            }
        }

        // Quick Simulator Buttons (Essential for testing in emulator & demoing!)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Instant SMS Test Simulators",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tap to simulate incoming Ethiopian SMS messages and hear PayVoice speak them:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.simulateSms(
                                sender = "telebirr",
                                body = "Dear Customer, you have received 500.00 ETB from Abebe Kebede (251911223344). Transaction ID: TB9928341. Your current balance is 2,450.00 ETB."
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("simulate_telebirr_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Simulate Telebirr", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.simulateSms(
                                sender = "CBE",
                                body = "Dear Customer, ETB 1,250.00 has been credited to your account 100023456789 from KEBEDE WORKU on 08/10/2026. Ref: FT260981234. Current balance is ETB 15,200.00."
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("simulate_cbe_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CbePurple)
                    ) {
                        Text("Simulate CBE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
