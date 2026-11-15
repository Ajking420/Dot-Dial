package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SpamRuleEntity
import com.example.data.system.T9Utils
import com.example.ui.components.DotMatrixTagChip
import com.example.ui.components.NothingSurfaceCard
import com.example.ui.components.SectionHeaderRow
import com.example.ui.theme.DotGothicFamily
import com.example.viewmodel.AnalyticsPeriod
import com.example.viewmodel.CallAnalyticsSummary
import com.example.viewmodel.DailyCallMetric
import com.example.viewmodel.SpamCheckUiState

@Composable
fun AnalyticsScreen(
    analytics: CallAnalyticsSummary,
    selectedPeriod: AnalyticsPeriod,
    spamRules: List<SpamRuleEntity>,
    spamCheckState: SpamCheckUiState,
    onSelectPeriod: (AnalyticsPeriod) -> Unit,
    onAddSpamRule: (pattern: String, label: String) -> Unit,
    onToggleSpamRule: (SpamRuleEntity) -> Unit,
    onDeleteSpamRule: (Long) -> Unit,
    onCheckNumberWithGemini: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var checkNumberInput by remember { mutableStateOf("") }
    var newRulePattern by remember { mutableStateOf("") }
    var newRuleLabel by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
    ) {
        item {
            SectionHeaderRow(
                title = "CALL ANALYTICS & SPAM SHIELD",
                subtitle = "Duration metrics & real-time caller defense"
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnalyticsPeriod.entries.forEach { period ->
                    DotMatrixTagChip(
                        label = period.label,
                        active = selectedPeriod == period,
                        onClick = { onSelectPeriod(period) },
                        testTag = "analytics_period_${period.label}"
                    )
                }
            }
        }

        // 2x2 KPI Summary Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AnalyticsStatCard(
                        label = "TALK TIME",
                        value = T9Utils.formatDuration(analytics.totalDurationSeconds),
                        accent = true,
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsStatCard(
                        label = "AVG DURATION",
                        value = T9Utils.formatDuration(analytics.averageDurationSeconds),
                        accent = false,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AnalyticsStatCard(
                        label = "TOTAL CALLS",
                        value = analytics.totalCalls.toString(),
                        accent = false,
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsStatCard(
                        label = "SPAM BLOCKED",
                        value = analytics.spamBlockedCount.toString(),
                        accent = analytics.spamBlockedCount > 0,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Dot-Matrix 7-Day Call Duration Chart
        item {
            NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "7-DAY DOT-MATRIX DURATION (MIN)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    DotMatrixBarChart(series = analytics.dailySeries)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "IN: ${analytics.incomingCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "OUT: ${analytics.outgoingCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "MISSED: ${analytics.missedCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Top Contacts Leaderboard
        if (analytics.topContacts.isNotEmpty()) {
            item {
                NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "TOP CONTACTS BY TALK TIME",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        analytics.topContacts.forEach { metric ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = metric.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${metric.callCount} calls",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = T9Utils.formatDuration(metric.totalSeconds),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Real-Time Gemini Spam Inspector
        item {
            NothingSurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                highlighted = true
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Spam inspector",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GEMINI REAL-TIME SPAM INSPECTOR",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = checkNumberInput,
                            onValueChange = { checkNumberInput = it },
                            placeholder = { Text("Enter number e.g. +18005550199") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("spam_inspector_input")
                        )
                        Button(
                            onClick = { onCheckNumberWithGemini(checkNumberInput) },
                            enabled = spamCheckState !is SpamCheckUiState.Checking &&
                                checkNumberInput.isNotBlank(),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("spam_inspector_button")
                        ) {
                            if (spamCheckState is SpamCheckUiState.Checking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Inspect number",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    when (spamCheckState) {
                        is SpamCheckUiState.Result -> {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = spamCheckState.assessment.classification,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "RISK ${spamCheckState.assessment.riskScore}/100",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = spamCheckState.assessment.reason,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Action: ${spamCheckState.assessment.recommendedAction}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        is SpamCheckUiState.Error -> {
                            Text(
                                text = spamCheckState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        else -> Unit
                    }
                }
            }
        }

        // Custom Spam Prefix Rules
        item {
            NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "AUTO-BLOCK PREFIX RULES",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newRulePattern,
                            onValueChange = { newRulePattern = it },
                            placeholder = { Text("Prefix e.g. +1800") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newRuleLabel,
                            onValueChange = { newRuleLabel = it },
                            placeholder = { Text("Label") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                onAddSpamRule(newRulePattern, newRuleLabel)
                                newRulePattern = ""
                                newRuleLabel = ""
                            },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add spam rule",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        items(spamRules, key = { it.id }) { rule ->
            NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${rule.pattern}  •  RISK ${rule.riskLevel}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = rule.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = rule.isEnabled,
                            onCheckedChange = { onToggleSpamRule(rule) }
                        )
                        IconButton(
                            onClick = { onDeleteSpamRule(rule.id) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove rule",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsStatCard(
    label: String,
    value: String,
    accent: Boolean,
    modifier: Modifier = Modifier
) {
    NothingSurfaceCard(
        modifier = modifier,
        highlighted = accent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (accent) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontFamily = DotGothicFamily,
                fontSize = 26.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DotMatrixBarChart(series: List<DailyCallMetric>) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveDot = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
    val maxMin = (series.maxOfOrNull { it.totalMinutes } ?: 1f).coerceAtLeast(1f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(118.dp)
        ) {
            if (series.isEmpty()) return@Canvas
            val cols = series.size
            val colWidth = size.width / cols
            val rows = 8
            val dotRadius = 3.5.dp.toPx()
            val rowStep = size.height / rows

            for (c in 0 until cols) {
                val fraction = (series[c].totalMinutes / maxMin).coerceIn(0f, 1f)
                val litDots = (fraction * rows).toInt().coerceAtLeast(if (series[c].callCount > 0) 1 else 0)
                val cx = colWidth * c + colWidth / 2f
                for (r in 0 until rows) {
                    val rowFromBottom = rows - 1 - r
                    val cy = rowStep * r + rowStep / 2f
                    val isLit = rowFromBottom < litDots
                    drawCircle(
                        color = if (isLit) activeColor else inactiveDot,
                        radius = dotRadius,
                        center = Offset(cx, cy)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            series.forEach { day ->
                Text(
                    text = day.dayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
