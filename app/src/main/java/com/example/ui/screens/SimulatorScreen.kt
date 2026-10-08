package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PaymentParser
import com.example.ui.MainViewModel
import com.example.ui.theme.CbePurple
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent

data class PresetTemplate(
    val title: String,
    val sender: String,
    val serviceBadge: String,
    val badgeColor: Color,
    val body: String
)

@Composable
fun SimulatorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val presets = remember {
        listOf(
            PresetTemplate(
                title = "Telebirr Personal Transfer",
                sender = "telebirr",
                serviceBadge = "Telebirr",
                badgeColor = EmeraldPrimary,
                body = "Dear Customer, you have received 500.00 ETB from Abebe Kebede (251911223344). Transaction ID: TB9928341. Your current balance is 2,450.00 ETB."
            ),
            PresetTemplate(
                title = "Telebirr Merchant Payment",
                sender = "127",
                serviceBadge = "Telebirr",
                badgeColor = EmeraldPrimary,
                body = "Dear Customer, you have received 350.00 ETB payment for Goods and Services from Almaz Ayana. Transaction ID: CR20261008."
            ),
            PresetTemplate(
                title = "CBE Bank Account Credit",
                sender = "CBE",
                serviceBadge = "CBE",
                badgeColor = CbePurple,
                body = "Dear Customer, ETB 1,250.00 has been credited to your account 100023456789 from KEBEDE WORKU on 08/10/2026. Ref: FT260981234. Current balance is ETB 15,200.00."
            ),
            PresetTemplate(
                title = "CBE Birr Wallet Transfer",
                sender = "CBEBirr",
                serviceBadge = "CBE Birr",
                badgeColor = CbePurple,
                body = "You have received ETB 2,500.00 from 251922334455 (Tadesse Alemu). Transaction ID: 987654321. Your new balance is ETB 8,500.00."
            ),
            PresetTemplate(
                title = "CBE Large Deposit",
                sender = "CBE",
                serviceBadge = "CBE",
                badgeColor = CbePurple,
                body = "Your Account 1000***789 has been credited with ETB 8,000.00 on 08/10/2026 from HAILE GEBRE. Reason: Shop purchase."
            )
        )
    }

    var customSender by remember { mutableStateOf("telebirr") }
    var customBody by remember {
        mutableStateOf("Dear Customer, you have received 750.00 ETB from Helen Tadesse (251911334455). Transaction ID: TB556677.")
    }

    // Dynamic parsed preview
    val parsedPreview = remember(customSender, customBody) {
        PaymentParser.parse(customSender, customBody)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Introduction banner
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SMS Payment Simulator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Test how PayVoice parses Ethiopian SMS messages and speaks them out loud. Tap any sample template or compose a custom message.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Custom SMS Form
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Custom SMS Simulator",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Sender quick selection
                Text(
                    text = "SMS Sender / Originator:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("telebirr", "CBE", "127", "CBEBirr").forEach { tag ->
                        OutlinedButton(
                            onClick = { customSender = tag },
                            colors = if (customSender == tag) {
                                ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            } else {
                                ButtonDefaults.outlinedButtonColors()
                            },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(tag, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = customSender,
                    onValueChange = { customSender = it },
                    label = { Text("Sender Name / Shortcode") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("simulator_sender_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = customBody,
                    onValueChange = { customBody = it },
                    label = { Text("SMS Message Body") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("simulator_body_input"),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3,
                    maxLines = 5
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Parser Result Preview
                if (parsedPreview != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "PARSER PREVIEW:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Detected: ETB ${PaymentParser.formatCurrencyAmount(parsedPreview.amount)} (${parsedPreview.serviceType})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            parsedPreview.payerName?.let {
                                Text(text = "Payer: $it", style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Spoken text: \"${parsedPreview.speechAnnouncement}\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = GoldAccent,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Note: No payment amount/details recognized in this text.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        viewModel.simulateSms(customSender, customBody)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("simulate_custom_sms_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Simulate and Speak",
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Simulate & Speak Out Loud",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }

        // Ready-to-Test Preset Templates
        Text(
            text = "One-Tap Payment Presets",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        presets.forEachIndexed { index, preset ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("preset_card_$index")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = preset.badgeColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = preset.serviceBadge,
                                color = preset.badgeColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Text(
                            text = "Sender: ${preset.sender}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = preset.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = preset.body,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                customSender = preset.sender
                                customBody = preset.body
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Load into Editor", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.simulateSms(preset.sender, preset.body)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = preset.badgeColor)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Run and Speak",
                                tint = if (preset.badgeColor == EmeraldPrimary) Color.Black else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Run & Speak",
                                color = if (preset.badgeColor == EmeraldPrimary) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
