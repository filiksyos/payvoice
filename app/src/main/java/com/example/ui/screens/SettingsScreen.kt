package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.CbePurple
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    var templateText by remember(settings.customTemplate) { mutableStateOf(settings.customTemplate) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Audio & Loudspeaker Settings
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Voice & Speaker Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Max Volume Boost switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Loud Volume Boost",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Forces speaker to maximum volume when speaking so payments aren't missed in noisy shops.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.maxVolumeOverride,
                        onCheckedChange = { viewModel.updateMaxVolumeOverride(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary),
                        modifier = Modifier.testTag("max_volume_switch")
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Repeat Announcement Count
                Text(
                    text = "Repeat Announcement",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "How many times to speak each incoming payment alert:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(1, 2, 3).forEach { count ->
                        val isSelected = settings.repeatCount == count
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { viewModel.updateRepeatCount(count) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (count == 1) "1x (Once)" else "${count}x (${count} times)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Speech Speed Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Speaking Speed",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${String.format("%.1f", settings.speechRate)}x",
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }

                Slider(
                    value = settings.speechRate,
                    onValueChange = { viewModel.updateSpeechRate(it) },
                    valueRange = 0.7f..1.4f,
                    steps = 6,
                    colors = SliderDefaults.colors(thumbColor = EmeraldPrimary, activeTrackColor = EmeraldPrimary),
                    modifier = Modifier.testTag("speech_rate_slider")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Speech Pitch Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Voice Pitch",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${String.format("%.1f", settings.speechPitch)}x",
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent
                    )
                }

                Slider(
                    value = settings.speechPitch,
                    onValueChange = { viewModel.updateSpeechPitch(it) },
                    valueRange = 0.7f..1.3f,
                    steps = 5,
                    colors = SliderDefaults.colors(thumbColor = GoldAccent, activeTrackColor = GoldAccent),
                    modifier = Modifier.testTag("speech_pitch_slider")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.testSpeech() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("preview_voice_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Test voice",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Preview Voice Audio",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Service Provider Filters
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Supported Services",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Telebirr (Ethio Telecom)", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Recognizes SMS from telebirr and 127",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.telebirrEnabled,
                        onCheckedChange = { viewModel.updateTelebirrEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("CBE (Commercial Bank of Ethiopia)", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Recognizes SMS from CBE, CBEBirr, and 996",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.cbeEnabled,
                        onCheckedChange = { viewModel.updateCbeEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = CbePurple)
                    )
                }
            }
        }

        // Counter Mode Screen Settings
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Display & Screen",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Keep Screen On in Counter Mode", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Prevents the device from sleeping while displaying the payment counter tab.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.keepScreenOn,
                        onCheckedChange = { viewModel.updateKeepScreenOn(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary)
                    )
                }
            }
        }

        // Custom Announcement Template
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Announcement Phrase Format",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = {
                        val defaultTemplate = "{service} payment! Received {amount} Birr from {sender}."
                        templateText = defaultTemplate
                        viewModel.updateCustomTemplate(defaultTemplate)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset template",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "Variables: {service}, {amount}, {sender}, {currency}",
                    style = MaterialTheme.typography.bodySmall,
                    color = GoldAccent
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = templateText,
                    onValueChange = {
                        templateText = it
                        viewModel.updateCustomTemplate(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("template_input"),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val preview = templateText
                            .replace("{service}", "Telebirr")
                            .replace("{amount}", "500")
                            .replace("{sender}", "Abebe")
                            .replace("{currency}", "Birr")
                        viewModel.testSpeech(preview)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Test This Template", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
