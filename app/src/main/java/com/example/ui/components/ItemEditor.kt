package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.t
import com.example.model.FoodItem
import com.example.ui.theme.SnapCard
import com.example.ui.theme.SnapCardBorder
import com.example.ui.theme.SnapDestructive
import com.example.ui.theme.SnapForeground
import com.example.ui.theme.SnapInput
import com.example.ui.theme.SnapMuted
import com.example.ui.theme.SnapMutedForeground
import com.example.ui.theme.SnapPrimary
import com.example.ui.theme.SnapPrimaryForeground
import com.example.ui.theme.SnapProtein
import com.example.ui.theme.SnapSecondary
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun ItemEditor(
    items: List<FoodItem>,
    onUpdateItem: (FoodItem) -> Unit,
    onRemoveItem: (FoodItem) -> Unit,
    onAddItem: (String, Double?) -> Unit,
    isAddingItem: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.forEach { item ->
            FoodItemCard(
                item = item,
                onUpdate = onUpdateItem,
                onRemove = { onRemoveItem(item) }
            )
        }

        AddFoodItemForm(
            isAdding = isAddingItem,
            onAdd = onAddItem
        )
    }
}

@Composable
fun FoodItemCard(
    item: FoodItem,
    onUpdate: (FoodItem) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditingName by remember { mutableStateOf(false) }
    var editedName by remember(item.name) { mutableStateOf(item.name) }
    var gramsText by remember(item.grams) { mutableStateOf(item.grams.roundToInt().toString()) }

    val nutrients = item.currentNutrients
    val maxSlider = max(500.0, ceil((item.grams * 2.5) / 50.0) * 50.0).toFloat()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SnapCard)
            .border(1.dp, SnapCardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // Top Row: Name + Portion desc left, Calories right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (isEditingName) {
                    BasicTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        textStyle = TextStyle(
                            color = SnapForeground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        cursorBrush = SolidColor(SnapPrimary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (editedName.isNotBlank()) {
                                onUpdate(item.copy(name = editedName.trim()))
                            }
                            isEditingName = false
                        }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SnapInput, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                } else {
                    Text(
                        text = item.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SnapForeground
                    )
                }
                Text(
                    text = item.portion_description,
                    fontSize = 12.sp,
                    color = SnapMutedForeground,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${nutrients.calories.roundToInt()}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SnapForeground
                )
                Text(
                    text = t("unit.kcal"),
                    fontSize = 11.sp,
                    color = SnapMutedForeground
                )
            }
        }

        // Macro row: P, C, F, Fiber
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "${t("nut.p")} ${String.format(Locale.US, "%.1f", nutrients.protein_g)}g",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = SnapProtein
            )
            Text(
                text = "${t("nut.c")} ${String.format(Locale.US, "%.1f", nutrients.carbs_g)}g",
                fontSize = 12.sp,
                color = SnapMutedForeground
            )
            Text(
                text = "${t("nut.f")} ${String.format(Locale.US, "%.1f", nutrients.fat_g)}g",
                fontSize = 12.sp,
                color = SnapMutedForeground
            )
            Text(
                text = "${t("nut.fiber")} ${String.format(Locale.US, "%.1f", nutrients.fiber_g)}g",
                fontSize = 12.sp,
                color = SnapMutedForeground
            )
        }

        // Portion row: Minus 10g, Slider, Plus 10g, Grams Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedIconButton(
                onClick = {
                    val newGrams = (item.grams - 10.0).coerceIn(1.0, 3000.0).roundToInt().toDouble()
                    onUpdate(item.copy(grams = newGrams))
                },
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = t("item.decrease", mapOf("name" to item.name)),
                    tint = SnapForeground,
                    modifier = Modifier.size(16.dp)
                )
            }

            Slider(
                value = item.grams.toFloat(),
                onValueChange = { value ->
                    val rounded = value.roundToInt().toDouble().coerceIn(1.0, 3000.0)
                    onUpdate(item.copy(grams = rounded))
                },
                valueRange = 1f..maxSlider,
                colors = SliderDefaults.colors(
                    thumbColor = SnapPrimary,
                    activeTrackColor = SnapPrimary,
                    inactiveTrackColor = SnapInput
                ),
                modifier = Modifier.weight(1f)
            )

            OutlinedIconButton(
                onClick = {
                    val newGrams = (item.grams + 10.0).coerceIn(1.0, 3000.0).roundToInt().toDouble()
                    onUpdate(item.copy(grams = newGrams))
                },
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = t("item.increase", mapOf("name" to item.name)),
                    tint = SnapForeground,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Number input for grams
            Row(
                modifier = Modifier
                    .width(68.dp)
                    .height(32.dp)
                    .background(SnapInput, RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = gramsText,
                    onValueChange = { text ->
                        val filtered = text.filter { it.isDigit() }.take(4)
                        gramsText = filtered
                        val parsed = filtered.toDoubleOrNull()
                        if (parsed != null) {
                            val clamped = parsed.coerceIn(1.0, 3000.0)
                            onUpdate(item.copy(grams = clamped))
                        }
                    },
                    textStyle = TextStyle(
                        color = SnapForeground,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End
                    ),
                    cursorBrush = SolidColor(SnapPrimary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "g",
                    fontSize = 11.sp,
                    color = SnapMutedForeground
                )
            }
        }

        // Bottom row: Rename & Remove actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    if (isEditingName && editedName.isNotBlank()) {
                        onUpdate(item.copy(name = editedName.trim()))
                    }
                    isEditingName = !isEditingName
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = SnapMutedForeground
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = t("item.rename"),
                    fontSize = 12.sp,
                    color = SnapMutedForeground
                )
            }

            TextButton(
                onClick = onRemove
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = SnapDestructive
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = t("item.remove"),
                    fontSize = 12.sp,
                    color = SnapDestructive
                )
            }
        }
    }
}

@Composable
fun AddFoodItemForm(
    isAdding: Boolean,
    onAdd: (String, Double?) -> Unit,
    modifier: Modifier = Modifier
) {
    var foodName by remember { mutableStateOf("") }
    var gramsText by remember { mutableStateOf("") }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SnapCard)
            .border(
                width = 1.dp,
                color = SnapCardBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Name input
        BasicTextField(
            value = foodName,
            onValueChange = { foodName = it },
            textStyle = TextStyle(color = SnapForeground, fontSize = 14.sp),
            cursorBrush = SolidColor(SnapPrimary),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .background(SnapInput, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (foodName.isEmpty()) {
                        Text(
                            text = t("item.addPlaceholder"),
                            color = SnapMutedForeground,
                            fontSize = 13.sp
                        )
                    }
                    innerTextField()
                }
            },
            modifier = Modifier.weight(1f)
        )

        // Grams input (optional)
        BasicTextField(
            value = gramsText,
            onValueChange = { gramsText = it.filter { ch -> ch.isDigit() }.take(4) },
            textStyle = TextStyle(color = SnapForeground, fontSize = 13.sp, textAlign = TextAlign.Center),
            cursorBrush = SolidColor(SnapPrimary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(38.dp)
                        .background(SnapInput, RoundedCornerShape(10.dp))
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (gramsText.isEmpty()) {
                        Text(
                            text = t("unit.g"),
                            color = SnapMutedForeground,
                            fontSize = 13.sp
                        )
                    }
                    innerTextField()
                }
            }
        )

        // Add button
        Button(
            onClick = {
                val grams = gramsText.toDoubleOrNull()
                onAdd(foodName.trim(), grams)
                foodName = ""
                gramsText = ""
            },
            enabled = !isAdding && foodName.isNotBlank(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SnapSecondary,
                contentColor = SnapForeground
            ),
            modifier = Modifier
                .height(38.dp)
                .testTag("add_item_button")
        ) {
            if (isAdding) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = SnapForeground,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = t("item.add"),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
