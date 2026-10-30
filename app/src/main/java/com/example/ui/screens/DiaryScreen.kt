package com.example.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import com.example.i18n.SnapCalStrings
import com.example.i18n.SnapLanguage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.FirestoreRepository
import com.example.data.GeminiNutritionService
import com.example.i18n.LocalLanguage
import com.example.i18n.t
import com.example.model.FoodItem
import com.example.model.Goal
import com.example.model.Meal
import com.example.model.calculateCalorieStatus
import com.example.model.calculateTotals
import com.example.ui.components.ItemEditor
import com.example.ui.components.MacroDonut
import com.example.ui.theme.FrauncesFontFamily
import com.example.ui.theme.SnapCard
import com.example.ui.theme.SnapCardBorder
import com.example.ui.theme.SnapDestructive
import com.example.ui.theme.SnapForeground
import com.example.ui.theme.SnapInput
import com.example.ui.theme.SnapMuted
import com.example.ui.theme.SnapMutedForeground
import com.example.ui.theme.SnapPrimary
import com.example.ui.theme.SnapPrimaryForeground
import com.example.ui.theme.SnapPrimarySoft
import com.example.ui.theme.SnapProtein
import com.example.ui.theme.SnapSecondary
import com.example.ui.theme.SnapWarm
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun DiaryScreen(
    currentUser: FirebaseUser?,
    repository: FirestoreRepository,
    nutritionService: GeminiNutritionService,
    snackbarHostState: SnackbarHostState,
    onRequestAuth: (String) -> Unit,
    onNavigateToSnap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLang = LocalLanguage.current
    val scope = rememberCoroutineScope()

    if (currentUser == null) {
        // Signed out state
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
                    imageVector = Icons.Default.Book,
                    contentDescription = null,
                    tint = SnapPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = t("diary.title"),
                fontFamily = FrauncesFontFamily,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = SnapForeground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = t("diary.signedOut"),
                fontSize = 14.sp,
                color = SnapMutedForeground,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onRequestAuth(SnapCalStrings.get("auth.reasonDiary", currentLang)) },
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

    var selectedMealForDialog by remember { mutableStateOf<Meal?>(null) }
    var isEditingMeal by remember { mutableStateOf(false) }
    var mealToDelete by remember { mutableStateOf<Meal?>(null) }
    var isAddingItemInDialog by remember { mutableStateOf(false) }

    // Group meals by day
    val groupedMeals = remember(meals) { groupMealsByCalendarDay(meals) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = t("diary.title"),
            fontFamily = FrauncesFontFamily,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = SnapForeground
        )

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
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = SnapPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = t("diary.emptyTitle"),
                    fontFamily = FrauncesFontFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = SnapForeground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = t("diary.emptyText"),
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
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(groupedMeals) { dayGroup ->
                    DayMealsSection(
                        dayGroup = dayGroup,
                        goal = userGoal,
                        onMealClick = { meal ->
                            selectedMealForDialog = meal
                            isEditingMeal = false
                        }
                    )
                }
            }
        }
    }

    // Meal Detail / Edit Dialog
    selectedMealForDialog?.let { currentMeal ->
        MealDetailDialog(
            meal = currentMeal,
            isEditing = isEditingMeal,
            isAddingItem = isAddingItemInDialog,
            onDismiss = {
                selectedMealForDialog = null
                isEditingMeal = false
            },
            onStartEdit = { isEditingMeal = true },
            onCancelEdit = { isEditingMeal = false },
            onSaveEdit = { updatedName, updatedItems ->
                scope.launch {
                    val totals = calculateTotals(updatedItems)
                    val updatedMeal = currentMeal.copy(
                        name = updatedName,
                        items = updatedItems.map { it.toMap() },
                        calories = totals.calories.roundToInt().toDouble(),
                        protein_g = (totals.protein_g * 10).roundToInt() / 10.0,
                        carbs_g = (totals.carbs_g * 10).roundToInt() / 10.0,
                        fat_g = (totals.fat_g * 10).roundToInt() / 10.0,
                        fiber_g = (totals.fiber_g * 10).roundToInt() / 10.0,
                        sugar_g = (totals.sugar_g * 10).roundToInt() / 10.0,
                        sodium_mg = totals.sodium_mg.roundToInt().toDouble()
                    )
                    val res = repository.updateMeal(currentUser.uid, updatedMeal)
                    if (res.isSuccess) {
                        selectedMealForDialog = updatedMeal
                        isEditingMeal = false
                        snackbarHostState.showSnackbar(SnapCalStrings.get("diary.updated", currentLang))
                    } else {
                        snackbarHostState.showSnackbar(SnapCalStrings.get("diary.updateFailed", currentLang))
                    }
                }
            },
            onDeleteClick = {
                mealToDelete = currentMeal
            },
            onAddItem = { name, grams ->
                isAddingItemInDialog = true
                scope.launch {
                    val addResult = nutritionService.estimateSingleFood(name, grams, currentLang)
                    isAddingItemInDialog = false
                    if (addResult.isSuccess) {
                        val newItem = addResult.getOrThrow()
                        val currentItems = currentMeal.parsedItems()
                        val newItems = currentItems + newItem
                        selectedMealForDialog = currentMeal.copy(items = newItems.map { it.toMap() })
                    } else {
                        snackbarHostState.showSnackbar(SnapCalStrings.get("err.lookup", currentLang))
                    }
                }
            }
        )
    }

    // Delete confirmation alert dialog
    mealToDelete?.let { meal ->
        AlertDialog(
            onDismissRequest = { mealToDelete = null },
            title = {
                Text(
                    text = t("diary.deleteTitle"),
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = SnapForeground
                )
            },
            text = {
                Text(
                    text = t("diary.deleteText", mapOf("name" to meal.name)),
                    color = SnapMutedForeground,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = meal.id
                        mealToDelete = null
                        selectedMealForDialog = null
                        scope.launch {
                            val res = repository.deleteMeal(currentUser.uid, id)
                            if (res.isSuccess) {
                                snackbarHostState.showSnackbar(SnapCalStrings.get("diary.deleted", currentLang))
                            } else {
                                snackbarHostState.showSnackbar(SnapCalStrings.get("diary.deleteFailed", currentLang))
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SnapDestructive, contentColor = SnapForeground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = t("diary.delete"), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mealToDelete = null }) {
                    Text(text = t("diary.cancel"), color = SnapMutedForeground)
                }
            },
            containerColor = SnapCard,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

data class DayMealGroup(
    val dayKey: String,
    val isToday: Boolean,
    val isYesterday: Boolean,
    val formattedDate: String,
    val totalCalories: Double,
    val totalProtein: Double,
    val meals: List<Meal>
)

@Composable
fun DayMealsSection(
    dayGroup: DayMealGroup,
    goal: Goal,
    onMealClick: (Meal) -> Unit
) {
    val status = calculateCalorieStatus(goal.goalType, dayGroup.totalCalories, goal.calories)
    val statusText = t("status.${status.state}", mapOf("n" to status.diff))
    val goalBadge = t("goal.badge", mapOf("type" to t("goal.${goal.goalType}")))
    val resolvedDayLabel = when {
        dayGroup.isToday -> t("diary.today")
        dayGroup.isYesterday -> t("diary.yesterday")
        else -> dayGroup.formattedDate
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Heading Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = resolvedDayLabel,
                fontFamily = FrauncesFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SnapForeground
            )
            Text(
                text = t("diary.summary", mapOf("kcal" to dayGroup.totalCalories.roundToInt(), "p" to dayGroup.totalProtein.roundToInt())),
                fontSize = 13.sp,
                color = SnapMutedForeground
            )
        }

        // Goal status line
        Text(
            text = "$goalBadge · $statusText",
            fontSize = 13.sp,
            color = if (status.isFlagged) SnapWarm else SnapMutedForeground
        )

        // Two Progress Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val calProgress = (dayGroup.totalCalories / goal.calories).coerceIn(0.0, 1.0).toFloat()
            val proProgress = (dayGroup.totalProtein / goal.protein_g).coerceIn(0.0, 1.0).toFloat()

            // Calorie progress card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SnapCard)
                    .border(1.dp, SnapCardBorder, RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = t("nut.calories"), fontSize = 12.sp, color = SnapForeground, fontWeight = FontWeight.Medium)
                    Text(text = "${dayGroup.totalCalories.roundToInt()} / ${goal.calories}", fontSize = 11.sp, color = SnapMutedForeground)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { calProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = SnapPrimary,
                    trackColor = SnapMuted
                )
            }

            // Protein progress card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SnapCard)
                    .border(1.dp, SnapCardBorder, RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = t("nut.protein"), fontSize = 12.sp, color = SnapForeground, fontWeight = FontWeight.Medium)
                    Text(text = "${dayGroup.totalProtein.roundToInt()} / ${goal.protein_g} g", fontSize = 11.sp, color = SnapMutedForeground)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { proProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = SnapProtein,
                    trackColor = SnapMuted
                )
            }
        }

        // Meal Rows
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            dayGroup.meals.forEach { meal ->
                MealListItemRow(meal = meal, onClick = { onMealClick(meal) })
            }
        }
    }
}

@Composable
fun MealListItemRow(meal: Meal, onClick: () -> Unit) {
    val thumbnailBitmap = remember(meal.thumbnail) {
        decodeBase64Bitmap(meal.thumbnail)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SnapCard)
            .border(1.dp, SnapCardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail or food tile
        if (thumbnailBitmap != null) {
            Image(
                bitmap = thumbnailBitmap.asImageBitmap(),
                contentDescription = meal.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SnapPrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = SnapPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Name and meal type + time
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = meal.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = SnapForeground,
                maxLines = 1
            )
            val timeStr = formatMealTime(meal.eatenAt)
            Text(
                text = "${t("meal.${meal.mealType}")} · $timeStr",
                fontSize = 12.sp,
                color = SnapMutedForeground,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Calories
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${meal.calories.roundToInt()}",
                fontSize = 17.sp,
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
}

@Composable
fun MealDetailDialog(
    meal: Meal,
    isEditing: Boolean,
    isAddingItem: Boolean,
    onDismiss: () -> Unit,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: (String, List<FoodItem>) -> Unit,
    onDeleteClick: () -> Unit,
    onAddItem: (String, Double?) -> Unit
) {
    var editedName by remember(meal.name) { mutableStateOf(meal.name) }
    var currentItems by remember(meal.items) { mutableStateOf(meal.parsedItems()) }
    val thumbnailBitmap = remember(meal.thumbnail) { decodeBase64Bitmap(meal.thumbnail) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SnapCard)
                .border(1.dp, SnapCardBorder, RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isEditing) {
                // Edit Mode
                Text(
                    text = t("diary.editMeal"),
                    fontFamily = FrauncesFontFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = SnapForeground
                )

                // Name Input
                Text(text = t("diary.name"), fontSize = 12.sp, color = SnapMutedForeground)
                BasicTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    textStyle = androidx.compose.ui.text.TextStyle(color = SnapForeground, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                    cursorBrush = SolidColor(SnapPrimary),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SnapInput, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            innerTextField()
                        }
                    }
                )

                ItemEditor(
                    items = currentItems,
                    onUpdateItem = { updated ->
                        currentItems = currentItems.map { if (it.id == updated.id) updated else it }
                    },
                    onRemoveItem = { removed ->
                        currentItems = currentItems.filter { it.id != removed.id }
                    },
                    onAddItem = onAddItem,
                    isAddingItem = isAddingItem
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onSaveEdit(editedName, currentItems) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SnapPrimary, contentColor = SnapPrimaryForeground),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = t("diary.saveChanges"), fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onCancelEdit,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SnapForeground),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(SnapCardBorder))
                    ) {
                        Text(text = t("diary.cancel"))
                    }
                }
            } else {
                // View Mode
                if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap.asImageBitmap(),
                        contentDescription = meal.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = meal.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                            fontFamily = FrauncesFontFamily,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = SnapForeground
                        )
                        Text(
                            text = "${t("meal.${meal.mealType}")} · ${formatMealDate(meal.eatenAt)}",
                            fontSize = 12.sp,
                            color = SnapMutedForeground,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${meal.calories.roundToInt()}",
                            fontFamily = FrauncesFontFamily,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = SnapForeground
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = t("unit.kcal"), fontSize = 14.sp, color = SnapMutedForeground, modifier = Modifier.padding(bottom = 4.dp))
                    }
                }

                MacroDonut(
                    proteinGrams = meal.protein_g,
                    carbsGrams = meal.carbs_g,
                    fatGrams = meal.fat_g
                )

                // Item breakdown list
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    meal.parsedItems().forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = item.name, fontSize = 13.sp, color = SnapForeground)
                            Text(text = "${item.grams.roundToInt()} g", fontSize = 13.sp, color = SnapMutedForeground)
                        }
                    }
                }

                // Health score line if present
                if (meal.health_score != null) {
                    Text(
                        text = "${t("result.health")}: ${meal.health_score}/10",
                        fontSize = 13.sp,
                        color = SnapMutedForeground
                    )
                }

                // Disclaimer
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = SnapMutedForeground, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = t("disclaimer"), fontSize = 11.sp, color = SnapMutedForeground)
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onStartEdit,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SnapSecondary, contentColor = SnapForeground),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = t("diary.edit"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onDeleteClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SnapDestructive),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(SnapDestructive))
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = t("diary.delete"), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

private fun decodeBase64Bitmap(base64Str: String?): android.graphics.Bitmap? {
    if (base64Str.isNullOrEmpty()) return null
    return try {
        val pure = if (base64Str.contains(",")) base64Str.substringAfter(",") else base64Str
        val bytes = Base64.decode(pure, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        null
    }
}

private fun formatMealTime(isoString: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val date = parser.parse(isoString.take(19)) ?: return ""
        val formatter = SimpleDateFormat("HH:mm", Locale.US)
        formatter.format(date)
    } catch (e: Exception) {
        ""
    }
}

private fun formatMealDate(isoString: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val date = parser.parse(isoString.take(19)) ?: return ""
        val formatter = SimpleDateFormat("MMM d, HH:mm", Locale.US)
        formatter.format(date)
    } catch (e: Exception) {
        ""
    }
}

private fun groupMealsByCalendarDay(meals: List<Meal>): List<DayMealGroup> {
    val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val todayStr = dayFormat.format(Date())
    val cal = Calendar.getInstance().apply { add(Calendar.DATE, -1) }
    val yesterdayStr = dayFormat.format(cal.time)

    val groups = mutableMapOf<String, MutableList<Meal>>()
    for (m in meals) {
        val dayKey = if (m.eatenAt.length >= 10) m.eatenAt.take(10) else todayStr
        groups.getOrPut(dayKey) { mutableListOf() }.add(m)
    }

    return groups.keys.sortedDescending().map { dayKey ->
        val dayMeals = groups[dayKey] ?: emptyList()
        var totalCal = 0.0
        var totalPro = 0.0
        for (m in dayMeals) {
            totalCal += m.calories
            totalPro += m.protein_g
        }

        val isToday = dayKey == todayStr
        val isYesterday = dayKey == yesterdayStr
        val formattedDate = try {
            val date = dayFormat.parse(dayKey) ?: Date()
            SimpleDateFormat("EEEE, MMM d", Locale.US).format(date)
        } catch (e: Exception) {
            dayKey
        }

        DayMealGroup(
            dayKey = dayKey,
            isToday = isToday,
            isYesterday = isYesterday,
            formattedDate = formattedDate,
            totalCalories = totalCal,
            totalProtein = totalPro,
            meals = dayMeals
        )
    }
}
