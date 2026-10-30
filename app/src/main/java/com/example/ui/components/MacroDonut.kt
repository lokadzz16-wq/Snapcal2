package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.t
import com.example.ui.theme.SnapCarbs
import com.example.ui.theme.SnapFat
import com.example.ui.theme.SnapForeground
import com.example.ui.theme.SnapMuted
import com.example.ui.theme.SnapMutedForeground
import com.example.ui.theme.SnapProtein
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun MacroDonut(
    proteinGrams: Double,
    carbsGrams: Double,
    fatGrams: Double,
    modifier: Modifier = Modifier
) {
    val proCals = proteinGrams * 4.0
    val carbCals = carbsGrams * 4.0
    val fatCals = fatGrams * 9.0
    val totalMacroCals = proCals + carbCals + fatCals

    val proPct = if (totalMacroCals > 0) ((proCals / totalMacroCals) * 100).roundToInt() else 0
    val carbPct = if (totalMacroCals > 0) ((carbCals / totalMacroCals) * 100).roundToInt() else 0
    val fatPct = if (totalMacroCals > 0) (100 - proPct - carbPct).coerceAtLeast(0) else 0

    val ariaDesc = t(
        "donut.aria",
        mapOf("p" to proPct, "c" to carbPct, "f" to fatPct)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = ariaDesc },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Left: 144x144 Donut
        Box(
            modifier = Modifier.size(144.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(144.dp)) {
                val strokeWidth = size.minDimension * 0.16f
                val radius = (size.minDimension - strokeWidth) / 2f

                if (totalMacroCals <= 0.0) {
                    drawCircle(
                        color = SnapMuted,
                        radius = radius,
                        style = Stroke(width = strokeWidth)
                    )
                } else {
                    val gapAngle = 4f
                    val totalAngle = 360f - (gapAngle * 3f)

                    val pSweep = (proCals / totalMacroCals * totalAngle).toFloat().coerceAtLeast(1f)
                    val cSweep = (carbCals / totalMacroCals * totalAngle).toFloat().coerceAtLeast(1f)
                    val fSweep = (fatCals / totalMacroCals * totalAngle).toFloat().coerceAtLeast(1f)

                    var startAngle = -90f

                    // Protein Slice
                    drawArc(
                        color = SnapProtein,
                        startAngle = startAngle,
                        sweepAngle = pSweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    startAngle += pSweep + gapAngle

                    // Carbs Slice
                    drawArc(
                        color = SnapCarbs,
                        startAngle = startAngle,
                        sweepAngle = cSweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    startAngle += cSweep + gapAngle

                    // Fat Slice
                    drawArc(
                        color = SnapFat,
                        startAngle = startAngle,
                        sweepAngle = fSweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            // Center Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${proteinGrams.roundToInt()}g",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = SnapProtein
                )
                Text(
                    text = t("nut.protein").uppercase(Locale.ROOT),
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SnapMutedForeground
                )
            }
        }

        // Right: Legend
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MacroLegendRow(
                color = SnapProtein,
                label = t("nut.protein"),
                grams = proteinGrams,
                pct = proPct
            )
            MacroLegendRow(
                color = SnapCarbs,
                label = t("nut.carbs"),
                grams = carbsGrams,
                pct = carbPct
            )
            MacroLegendRow(
                color = SnapFat,
                label = t("nut.fat"),
                grams = fatGrams,
                pct = fatPct
            )
        }
    }
}

@Composable
private fun MacroLegendRow(
    color: Color,
    label: String,
    grams: Double,
    pct: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                color = SnapForeground,
                fontWeight = FontWeight.Medium
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${String.format(Locale.US, "%.1f", grams)} g",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = SnapForeground
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$pct%",
                fontSize = 13.sp,
                color = SnapMutedForeground
            )
        }
    }
}
