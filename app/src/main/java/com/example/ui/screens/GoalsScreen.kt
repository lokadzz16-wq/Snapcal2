package com.example.ui.screens

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FirestoreRepository
import com.example.i18n.LocalLanguage
import com.example.i18n.SnapCalStrings
import com.example.i18n.t
import com.example.model.Goal
import com.example.ui.theme.FrauncesFontFamily
import com.example.ui.theme.SnapCard
import com.example.ui.theme.SnapCardBorder
import com.example.ui.theme.SnapForeground
import com.example.ui.theme.SnapInput
import com.example.ui.theme.SnapMutedForeground
import com.example.ui.theme.SnapPrimary
import com.example.ui.theme.SnapPrimaryForeground
import com.example.ui.theme.SnapPrimarySoft
import com.example.ui.theme.SnapSecondary
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun GoalsScreen(
    currentUser: FirebaseUser?,
    repository: FirestoreRepository,
    snackbarHostState: SnackbarHostState,
    onRequestAuth: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLang = LocalLanguage.current
    val scope = rememberCoroutineScope()

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
                    imageVector = Icons.Default.TrackChanges,
                    contentDescription = null,
                    tint = SnapPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = t("goals.title"),
                fontFamily = FrauncesFontFamily,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = SnapForeground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = t("goals.signedOut"),
                fontSize = 14.sp,
                color = SnapMutedForeground,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onRequestAuth(SnapCalStrings.get("auth.reasonGoals", currentLang)) },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SnapPrimary, contentColor = SnapPrimaryForeground),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(48.dp)
            ) {
                Text(text = t("nav.signIn"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        return
    }

    val goalsFlow = remember(currentUser.uid) { repository.observeGoals(currentUser.uid) }
    val currentGoal by goalsFlow.collectAsState(initial = Goal(userId = currentUser.uid))

    var selectedGoalType by remember { mutableStateOf("maintain") }
    var calInputText by remember { mutableStateOf("2000") }
    var proInputText by remember { mutableStateOf("100") }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(currentGoal) {
        selectedGoalType = currentGoal.goalType
        calInputText = currentGoal.calories.toString()
        proInputText = currentGoal.protein_g.toString()
    }

    // Quick Calculator Form State
    var calcAgeText by remember { mutableStateOf("") }
    var calcSex by remember { mutableStateOf("male") }
    var calcHeightText by remember { mutableStateOf("") }
    var calcWeightText by remember { mutableStateOf("") }
    var calcActivity by remember { mutableStateOf("moderate") }

    data class CalcResult(
        val maintenance: Int,
        val targetCalories: Int,
        val targetProtein: Int,
        val proteinRate: Double,
        val floorApplied: Boolean,
        val floorValue: Int
    )

    var calcResult by remember { mutableStateOf<CalcResult?>(null) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Title & Subtitle
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = t("goals.title"),
                fontFamily = FrauncesFontFamily,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = SnapForeground
            )
            Text(
                text = t("goals.subtitle"),
                fontSize = 14.sp,
                color = SnapMutedForeground
            )
        }

        // 1. Goal Type Section
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = t("goals.typeTitle"),
                fontFamily = FrauncesFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = SnapForeground
            )

            listOf("cut", "maintain", "bulk").forEach { type ->
                val isSelected = selectedGoalType == type
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) SnapPrimary else SnapCard)
                        .border(1.dp, if (isSelected) SnapPrimary else SnapCardBorder, RoundedCornerShape(16.dp))
                        .clickable {
                            selectedGoalType = type
                            calcResult = null
                        }
                        .padding(16.dp)
                ) {
                    Text(
                        text = t("goal.$type"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) SnapPrimaryForeground else SnapForeground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = t("goal.${type}Desc"),
                        fontSize = 13.sp,
                        color = if (isSelected) SnapPrimaryForeground.copy(alpha = 0.9f) else SnapMutedForeground,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // 2. Daily Targets Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SnapCard)
                .border(1.dp, SnapCardBorder, RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = t("goals.targets"),
                fontFamily = FrauncesFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = SnapForeground
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Calories input
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = t("goals.calLabel"), fontSize = 12.sp, color = SnapMutedForeground)
                    Spacer(modifier = Modifier.height(6.dp))
                    BasicTextField(
                        value = calInputText,
                        onValueChange = { calInputText = it.filter { ch -> ch.isDigit() }.take(5) },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = SnapForeground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        cursorBrush = SolidColor(SnapPrimary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        decorationBox = { inner ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .background(SnapInput, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                inner()
                            }
                        }
                    )
                }

                // Protein input
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = t("goals.proLabel"), fontSize = 12.sp, color = SnapMutedForeground)
                    Spacer(modifier = Modifier.height(6.dp))
                    BasicTextField(
                        value = proInputText,
                        onValueChange = { proInputText = it.filter { ch -> ch.isDigit() }.take(4) },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = SnapForeground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        cursorBrush = SolidColor(SnapPrimary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        decorationBox = { inner ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .background(SnapInput, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                inner()
                            }
                        }
                    )
                }
            }

            Button(
                onClick = {
                    val cal = calInputText.toIntOrNull() ?: 0
                    val pro = proInputText.toIntOrNull() ?: 0

                    if (cal !in 500..10000) {
                        scope.launch { snackbarHostState.showSnackbar(SnapCalStrings.get("goals.calRange", currentLang)) }
                        return@Button
                    }
                    if (pro !in 10..500) {
                        scope.launch { snackbarHostState.showSnackbar(SnapCalStrings.get("goals.proRange", currentLang)) }
                        return@Button
                    }

                    isSaving = true
                    scope.launch {
                        val newGoal = Goal(
                            userId = currentUser.uid,
                            calories = cal,
                            protein_g = pro,
                            goalType = selectedGoalType
                        )
                        val res = repository.saveGoals(currentUser.uid, newGoal)
                        isSaving = false
                        if (res.isSuccess) {
                            snackbarHostState.showSnackbar(SnapCalStrings.get("goals.saved", currentLang))
                        } else {
                            snackbarHostState.showSnackbar(SnapCalStrings.get("goals.saveFailed", currentLang))
                        }
                    }
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SnapPrimary, contentColor = SnapPrimaryForeground),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_goals_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = SnapPrimaryForeground, strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.Default.TrackChanges, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = t("goals.save"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 3. Quick Calculator Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SnapCard)
                .border(1.dp, SnapCardBorder, RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Calculate, contentDescription = null, tint = SnapPrimary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = t("goals.calc"),
                    fontFamily = FrauncesFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SnapForeground
                )
            }
            Text(
                text = t("goals.calcHint"),
                fontSize = 13.sp,
                color = SnapMutedForeground
            )

            // Age & Sex
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CalcInputField(
                    label = t("goals.age"),
                    placeholder = "32",
                    value = calcAgeText,
                    onValueChange = { calcAgeText = it.filter { ch -> ch.isDigit() }.take(3) },
                    modifier = Modifier.weight(1f)
                )

                // Sex selector
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = t("goals.sex"), fontSize = 12.sp, color = SnapMutedForeground)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(SnapInput, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("male" to t("goals.male"), "female" to t("goals.female")).forEach { (key, label) ->
                            val isSelected = calcSex == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) SnapPrimary else Color.Transparent)
                                    .clickable { calcSex = key }
                                    .fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) SnapPrimaryForeground else SnapMutedForeground
                                )
                            }
                        }
                    }
                }
            }

            // Height & Weight
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CalcInputField(
                    label = t("goals.height"),
                    placeholder = "175",
                    value = calcHeightText,
                    onValueChange = { calcHeightText = it.filter { ch -> ch.isDigit() }.take(3) },
                    modifier = Modifier.weight(1f)
                )

                CalcInputField(
                    label = t("goals.weight"),
                    placeholder = "70",
                    value = calcWeightText,
                    onValueChange = { calcWeightText = it.filter { ch -> ch.isDigit() }.take(3) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Activity level dropdown / selector
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = t("goals.activity"), fontSize = 12.sp, color = SnapMutedForeground)
                Spacer(modifier = Modifier.height(6.dp))
                var activityMenuOpen by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SnapInput)
                            .clickable { activityMenuOpen = true }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = t("act.$calcActivity"),
                            fontSize = 13.sp,
                            color = SnapForeground,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    DropdownMenu(
                        expanded = activityMenuOpen,
                        onDismissRequest = { activityMenuOpen = false }
                    ) {
                        listOf("sedentary", "light", "moderate", "active", "very").forEach { act ->
                            DropdownMenuItem(
                                text = { Text(text = t("act.$act")) },
                                onClick = {
                                    calcActivity = act
                                    activityMenuOpen = false
                                }
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val age = calcAgeText.toIntOrNull()
                    val height = calcHeightText.toDoubleOrNull()
                    val weight = calcWeightText.toDoubleOrNull()

                    if (age == null || age !in 10..120 || height == null || height !in 100.0..250.0 || weight == null || weight !in 30.0..400.0) {
                        scope.launch { snackbarHostState.showSnackbar(SnapCalStrings.get("goals.realistic", currentLang)) }
                        return@Button
                    }

                    // Activity factor
                    val actFactor = when (calcActivity) {
                        "sedentary" -> 1.2
                        "light" -> 1.375
                        "moderate" -> 1.55
                        "active" -> 1.725
                        "very" -> 1.9
                        else -> 1.55
                    }

                    // BMR (Mifflin–St Jeor) = 10×kg + 6.25×cm − 5×age + (male +5 / female −161)
                    val sFactor = if (calcSex == "male") 5.0 else -161.0
                    val bmr = 10.0 * weight + 6.25 * height - 5.0 * age + sFactor
                    val maintenance = ((bmr * actFactor) / 10.0).roundToInt() * 10

                    val goalCalFactor = when (selectedGoalType) {
                        "cut" -> 0.8
                        "bulk" -> 1.1
                        else -> 1.0
                    }
                    val proPerKg = when (selectedGoalType) {
                        "cut" -> 2.0
                        "bulk" -> 1.8
                        else -> 1.6
                    }

                    var rawTargetCal = ((maintenance * goalCalFactor) / 10.0).roundToInt() * 10
                    val floor = if (calcSex == "male") 1500 else 1200
                    val safeFloor = min(floor, maintenance)

                    var floorApplied = false
                    if (selectedGoalType == "cut" && rawTargetCal < safeFloor) {
                        rawTargetCal = safeFloor
                        floorApplied = true
                    }

                    val targetPro = (weight * proPerKg).roundToInt()

                    calcResult = CalcResult(
                        maintenance = maintenance,
                        targetCalories = rawTargetCal,
                        targetProtein = targetPro,
                        proteinRate = proPerKg,
                        floorApplied = floorApplied,
                        floorValue = safeFloor
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SnapSecondary, contentColor = SnapForeground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = t("goals.calculate"), fontWeight = FontWeight.SemiBold)
            }

            // Calculation Result Box
            calcResult?.let { res ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SnapPrimarySoft)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${t("goals.maintenance")}: ${res.maintenance} kcal",
                        fontSize = 14.sp,
                        color = SnapForeground,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${t("goals.target", mapOf("type" to t("goal.$selectedGoalType")))}: ${res.targetCalories} kcal",
                        fontSize = 14.sp,
                        color = SnapForeground,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${t("nut.protein")}: ${res.targetProtein} g · ${t("goals.proteinRate", mapOf("rate" to res.proteinRate))}",
                        fontSize = 14.sp,
                        color = SnapForeground
                    )

                    if (res.floorApplied) {
                        Text(
                            text = t("goals.floorNote", mapOf("n" to res.floorValue)),
                            fontSize = 12.sp,
                            color = SnapPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            calInputText = res.targetCalories.toString()
                            proInputText = res.targetProtein.toString()
                            scope.launch {
                                snackbarHostState.showSnackbar(SnapCalStrings.get("goals.applied", currentLang))
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SnapPrimary, contentColor = SnapPrimaryForeground)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = t("goals.apply"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Disclaimer at bottom
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = SnapMutedForeground, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = t("disclaimer"), fontSize = 11.sp, color = SnapMutedForeground)
        }
    }
}

@Composable
fun CalcInputField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(text = label, fontSize = 12.sp, color = SnapMutedForeground)
        Spacer(modifier = Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = SnapForeground,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            ),
            cursorBrush = SolidColor(SnapPrimary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(SnapInput, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(text = placeholder, color = SnapMutedForeground, fontSize = 14.sp)
                    }
                    inner()
                }
            }
        )
    }
}
