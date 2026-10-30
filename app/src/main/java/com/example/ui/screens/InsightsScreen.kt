package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FirestoreRepository
import com.example.i18n.LocalLanguage
import com.example.i18n.SnapCalStrings
import com.example.i18n.t
import com.example.model.Goal
import com.example.model.Meal
import com.example.ui.theme.FrauncesFontFamily
import com.example.ui.theme.SnapCard
import com.example.ui.theme.SnapCardBorder
import com.example.ui.theme.SnapForeground
import com.example.ui.theme.SnapMuted
import com.example.ui.theme.SnapMutedForeground
import com.example.ui.theme.SnapPrimary
import com.example.ui.theme.SnapPrimaryForeground
import com.example.ui.theme.SnapPrimarySoft
import com.example.ui.theme.SnapProtein
import com.example.ui.theme.SnapWarm
import com.google.firebase.auth.FirebaseUser
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun InsightsScreen(
    currentUser: FirebaseUser?,
    repository: FirestoreRepository,
    onRequestAuth: (String) -> Unit,
    onNavigateToSnap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLang = LocalLanguage.current

    if (currentUser == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(SnapPrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShowChart,
                    contentDescription = null,
                    tint = SnapPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = t("insights.title"),
                fontFamily = FrauncesFontFamily,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = SnapForeground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = t("insights.signedOut"),
                fontSize = 14.sp,
                color = SnapMutedForeground,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onRequestAuth(SnapCalStrings.get("auth.reasonInsights", currentLang)) },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SnapPrimary, contentColor = SnapPrimaryForeground),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(48.dp)
            ) {
                Text(text = t("auth.signIn"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        return
    }

    val mealsFlow = remember(currentUser.uid) { repository.observeMeals(currentUser.uid) }
    val goalsFlow = remember(currentUser.uid) { repository.observeGoals(currentUser.uid) }

    val meals by mealsFlow.collectAsState(initial = emptyList())
    val userGoal by goalsFlow.collectAsState(initial = Goal(userId = currentUser.uid))

    var selectedWindowDays by remember { mutableIntStateOf(7) }

    // Aggregate daily data for the selected window
    val dayPoints = remember(meals, selectedWindowDays) {
        computeDayPoints(meals, selectedWindowDays)
    }

    val totalCalInWindow = dayPoints.sumOf { it.calories }
    val totalProInWindow = dayPoints.sumOf { it.protein }
    val avgCal = totalCalInWindow / selectedWindowDays
    val avgPro = totalProInWindow / selectedWindowDays

    // Average Status
    val diff = (avgCal - userGoal.calories).roundToInt()
    val absDiff = abs(diff)
    val avgState = when (userGoal.goalType) {
        "cut" -> if (diff > 0) "avgOver" else if (absDiff <= (userGoal.calories * 0.05).roundToInt()) "avgMet" else "avgLeft"
        "bulk" -> if (diff >= 0) "avgMet" else "avgShort"
        else -> if (absDiff <= (userGoal.calories * 0.10).roundToInt()) "avgMet" else if (diff > 0) "avgOver" else "avgLeft"
    }
    val isAvgFlagged = (userGoal.goalType == "cut" && diff > 0) || (userGoal.goalType == "bulk" && diff < 0) || (userGoal.goalType == "maintain" && diff > 0)
    val avgStatusText = t("status.$avgState", mapOf("n" to absDiff))
    val goalBadge = t("goal.badge", mapOf("type" to t("goal.${userGoal.goalType}")))

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header Row: Title & 7d / 30d Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = t("insights.title"),
                fontFamily = FrauncesFontFamily,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = SnapForeground
            )

            // Pill toggle
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(SnapCard)
                    .border(1.dp, SnapCardBorder, RoundedCornerShape(20.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                listOf(7, 30).forEach { days ->
                    val isSelected = selectedWindowDays == days
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) SnapPrimary else Color.Transparent)
                            .clickable { selectedWindowDays = days }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = t("insights.days", mapOf("n" to days)),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) SnapPrimaryForeground else SnapMutedForeground
                        )
                    }
                }
            }
        }

        // Subtitle line
        if (meals.isNotEmpty()) {
            Text(
                text = "$goalBadge · $avgStatusText",
                fontSize = 13.sp,
                color = if (isAvgFlagged) SnapWarm else SnapMutedForeground
            )
        }

        if (meals.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SnapCard)
                    .border(1.dp, SnapCardBorder, RoundedCornerShape(24.dp))
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(SnapPrimarySoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = SnapPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = t("insights.emptyTitle"),
                    fontFamily = FrauncesFontFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = SnapForeground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = t("insights.emptyText"),
                    fontSize = 14.sp,
                    color = SnapMutedForeground,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onNavigateToSnap,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SnapPrimary, contentColor = SnapPrimaryForeground)
                ) {
                    Text(text = t("how.s1t"), fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            // Two Stat Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Calories Avg
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(SnapCard)
                        .border(1.dp, SnapCardBorder, RoundedCornerShape(20.dp))
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = t("insights.avgCal").uppercase(Locale.ROOT),
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp,
                        color = SnapMutedForeground,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${avgCal.roundToInt()}",
                        fontFamily = FrauncesFontFamily,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = SnapForeground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = t("insights.goalKcal", mapOf("n" to userGoal.calories)),
                        fontSize = 11.sp,
                        color = SnapMutedForeground
                    )
                }

                // Protein Avg
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(SnapCard)
                        .border(1.dp, SnapCardBorder, RoundedCornerShape(20.dp))
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = t("insights.avgPro").uppercase(Locale.ROOT),
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp,
                        color = SnapMutedForeground,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f", avgPro),
                        fontFamily = FrauncesFontFamily,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = SnapProtein
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = t("insights.goalG", mapOf("n" to userGoal.protein_g)),
                        fontSize = 11.sp,
                        color = SnapMutedForeground
                    )
                }
            }

            // Calories Chart Card
            ChartCard(
                title = t("insights.calTitle"),
                goalLineNote = t("insights.goalLine"),
                goalLabel = t("insights.goal"),
                goalValue = userGoal.calories.toDouble(),
                dataPoints = dayPoints.map { it.label to it.calories },
                barColor = SnapPrimary,
                unit = "kcal"
            )

            // Protein Chart Card
            ChartCard(
                title = t("insights.proTitle"),
                goalLineNote = t("insights.goalLine"),
                goalLabel = t("insights.goal"),
                goalValue = userGoal.protein_g.toDouble(),
                dataPoints = dayPoints.map { it.label to it.protein },
                barColor = SnapProtein,
                unit = "g"
            )
        }
    }
}

data class DayDataPoint(
    val dateKey: String,
    val label: String,
    val calories: Double,
    val protein: Double
)

private fun computeDayPoints(meals: List<Meal>, daysCount: Int): List<DayDataPoint> {
    val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val shortWeekday = SimpleDateFormat("EEE", Locale.US)
    val dayMonth = SimpleDateFormat("d MMM", Locale.US)

    val calMap = mutableMapOf<String, Double>()
    val proMap = mutableMapOf<String, Double>()

    for (m in meals) {
        val key = if (m.eatenAt.length >= 10) m.eatenAt.take(10) else ""
        if (key.isNotEmpty()) {
            calMap[key] = (calMap[key] ?: 0.0) + m.calories
            proMap[key] = (proMap[key] ?: 0.0) + m.protein_g
        }
    }

    val result = mutableListOf<DayDataPoint>()
    val cal = Calendar.getInstance()

    for (i in (daysCount - 1) downTo 0) {
        val targetCal = Calendar.getInstance().apply { add(Calendar.DATE, -i) }
        val dateStr = dayFormat.format(targetCal.time)
        val label = if (daysCount <= 7) shortWeekday.format(targetCal.time) else dayMonth.format(targetCal.time)

        result.add(
            DayDataPoint(
                dateKey = dateStr,
                label = label,
                calories = calMap[dateStr] ?: 0.0,
                protein = proMap[dateStr] ?: 0.0
            )
        )
    }

    return result
}

@Composable
fun ChartCard(
    title: String,
    goalLineNote: String,
    goalLabel: String,
    goalValue: Double,
    dataPoints: List<Pair<String, Double>>,
    barColor: Color,
    unit: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SnapCard)
            .border(1.dp, SnapCardBorder, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Text(
            text = title,
            fontFamily = FrauncesFontFamily,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = SnapForeground
        )
        Text(
            text = goalLineNote,
            fontSize = 12.sp,
            color = SnapMutedForeground,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Custom Bar Chart using Canvas
        val maxVal = max(goalValue * 1.25, (dataPoints.maxOfOrNull { it.second } ?: 100.0) * 1.15)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height - 30.dp.toPx()
                val bottomY = canvasHeight

                // Draw dashed horizontal Cartesian grid lines (3 lines)
                val gridPathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                val gridColor = Color(0xFF332E2A)

                for (lineIdx in 1..3) {
                    val y = bottomY * (lineIdx / 3f)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = gridPathEffect
                    )
                }

                // Draw Goal Reference Line (dashed line with SnapWarm)
                val goalY = bottomY - ((goalValue / maxVal) * bottomY).toFloat()
                if (goalY in 0f..bottomY) {
                    drawLine(
                        color = SnapWarm,
                        start = Offset(0f, goalY),
                        end = Offset(canvasWidth, goalY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )
                }

                // Draw Bars
                val count = dataPoints.size
                val barSpacing = canvasWidth / count
                val barWidth = (barSpacing * 0.55f).coerceIn(4.dp.toPx(), 28.dp.toPx())

                dataPoints.forEachIndexed { idx, pair ->
                    val barHeight = ((pair.second / maxVal) * bottomY).toFloat().coerceAtLeast(0f)
                    val barX = (idx * barSpacing) + (barSpacing - barWidth) / 2f
                    val barY = bottomY - barHeight

                    // Bar with rounded top
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(barX, barY),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }

            // Labels row along the bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val step = if (dataPoints.size > 7) 5 else 1
                dataPoints.forEachIndexed { idx, pair ->
                    if (idx % step == 0 || idx == dataPoints.lastIndex) {
                        Text(
                            text = pair.first,
                            fontSize = 11.sp,
                            color = SnapMutedForeground,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
