package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.FirestoreRepository
import com.example.data.GeminiNutritionService
import com.example.data.MealAnalysisResult
import com.example.i18n.LocalLanguage
import com.example.i18n.SnapCalStrings
import com.example.i18n.SnapLanguage
import com.example.i18n.t
import com.example.model.FoodItem
import com.example.model.Meal
import kotlin.math.max
import com.example.model.Micronutrient
import com.example.model.calculateTotals
import com.example.ui.components.CameraCaptureView
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
import com.example.ui.theme.SnapSecondary
import com.example.ui.theme.SnapSuccess
import com.example.ui.theme.SnapWarm
import com.example.ui.theme.SnapWarmSoft
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

sealed interface SnapState {
    data object Idle : SnapState
    data class Analyzing(val photoBitmap: Bitmap) : SnapState
    data class NoFood(val photoBitmap: Bitmap, val reason: String) : SnapState
    data class Error(val photoBitmap: Bitmap, val errorCode: String) : SnapState
    data class Result(
        val photoBitmap: Bitmap,
        val thumbnailBase64: String?,
        val mealName: String,
        val mealType: String,
        val items: List<FoodItem>,
        val micronutrients: List<Micronutrient>,
        val healthScore: Int,
        val healthReason: String,
        val confidence: String,
        val notes: List<String>,
        val contextText: String = "",
        val isSaved: Boolean = false
    ) : SnapState
}

@Composable
fun SnapScreen(
    currentUser: FirebaseUser?,
    repository: FirestoreRepository,
    nutritionService: GeminiNutritionService,
    snackbarHostState: SnackbarHostState,
    onRequestAuth: (String) -> Unit,
    onNavigateToDiary: () -> Unit,
    hasCameraPermission: Boolean = false,
    onRequestCameraPermission: ((onGranted: () -> Unit) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLang = LocalLanguage.current
    val scope = rememberCoroutineScope()

    var snapState by remember { mutableStateOf<SnapState>(SnapState.Idle) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var isAddingItem by remember { mutableStateOf(false) }
    var isSavingMeal by remember { mutableStateOf(false) }
    var isCameraActive by remember { mutableStateOf(false) }

    // Intercept back button when CameraX preview is active
    if (isCameraActive) {
        BackHandler {
            isCameraActive = false
        }
    }

    // Camera launcher (legacy fallback)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            val bitmap = loadAndCompressBitmap(context, tempCameraUri!!)
            if (bitmap != null) {
                startAnalysis(bitmap, "", nutritionService, currentLang, scope) { snapState = it }
            } else {
                scope.launch { snackbarHostState.showSnackbar(SnapCalStrings.get("err.cantRead", currentLang)) }
            }
        }
    }

    // Gallery picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = loadAndCompressBitmap(context, uri)
            if (bitmap != null) {
                startAnalysis(bitmap, "", nutritionService, currentLang, scope) { snapState = it }
            } else {
                scope.launch { snackbarHostState.showSnackbar(SnapCalStrings.get("err.cantRead", currentLang)) }
            }
        }
    }

    fun launchCamera() {
        if (hasCameraPermission) {
            isCameraActive = true
        } else {
            onRequestCameraPermission?.invoke {
                isCameraActive = true
            } ?: run {
                try {
                    val photoFile = File.createTempFile("snapcal_photo_", ".jpg", context.cacheDir)
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                    tempCameraUri = uri
                    cameraLauncher.launch(uri)
                } catch (e: Exception) {
                    galleryLauncher.launch("image/*")
                }
            }
        }
    }

    val scrollState = rememberScrollState()

    if (isCameraActive && hasCameraPermission) {
        CameraCaptureView(
            onPhotoCaptured = { photo ->
                isCameraActive = false
                startAnalysis(photo, "", nutritionService, currentLang, scope) { snapState = it }
            },
            onDismiss = {
                isCameraActive = false
            },
            modifier = modifier.fillMaxSize()
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            when (val state = snapState) {
                is SnapState.Idle -> {
                    IdleView(
                        onTakePhoto = { launchCamera() },
                        onUploadPhoto = { galleryLauncher.launch("image/*") }
                    )
                }

            is SnapState.Analyzing -> {
                AnalyzingView(photoBitmap = state.photoBitmap)
            }

            is SnapState.NoFood -> {
                NoFoodView(
                    photoBitmap = state.photoBitmap,
                    reason = state.reason,
                    onRetry = { snapState = SnapState.Idle }
                )
            }

            is SnapState.Error -> {
                ErrorView(
                    photoBitmap = state.photoBitmap,
                    errorCode = state.errorCode,
                    onRetry = {
                        startAnalysis(state.photoBitmap, "", nutritionService, currentLang, scope) { snapState = it }
                    },
                    onNewPhoto = { snapState = SnapState.Idle }
                )
            }

            is SnapState.Result -> {
                ResultView(
                    state = state,
                    isSaving = isSavingMeal,
                    isAddingItem = isAddingItem,
                    onUpdateItem = { updatedItem ->
                        val newItems = state.items.map { if (it.id == updatedItem.id) updatedItem else it }
                        snapState = state.copy(items = newItems, isSaved = false)
                    },
                    onRemoveItem = { itemToRemove ->
                        val newItems = state.items.filter { it.id != itemToRemove.id }
                        snapState = state.copy(items = newItems, isSaved = false)
                    },
                    onAddItem = { name, grams ->
                        isAddingItem = true
                        scope.launch {
                            val addResult = nutritionService.estimateSingleFood(name, grams, currentLang)
                            isAddingItem = false
                            if (addResult.isSuccess) {
                                val newItem = addResult.getOrThrow()
                                snapState = state.copy(items = state.items + newItem, isSaved = false)
                            } else {
                                snackbarHostState.showSnackbar(SnapCalStrings.get("err.lookup", currentLang))
                            }
                        }
                    },
                    onMealTypeChange = { newType ->
                        snapState = state.copy(mealType = newType, isSaved = false)
                    },
                    onReanalyze = { contextText ->
                        startAnalysis(state.photoBitmap, contextText, nutritionService, currentLang, scope) { snapState = it }
                    },
                    onSaveMeal = {
                        if (currentUser == null) {
                            onRequestAuth(SnapCalStrings.get("auth.reasonSave", currentLang))
                        } else {
                            isSavingMeal = true
                            scope.launch {
                                val totals = calculateTotals(state.items)
                                val isoDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())

                                val meal = Meal(
                                    id = "",
                                    userId = currentUser.uid,
                                    name = state.mealName,
                                    mealType = state.mealType,
                                    eatenAt = isoDate,
                                    thumbnail = state.thumbnailBase64,
                                    items = state.items.map { it.toMap() },
                                    micronutrients = state.micronutrients.map { it.toMap() },
                                    calories = totals.calories.roundToInt().toDouble(),
                                    protein_g = (totals.protein_g * 10).roundToInt() / 10.0,
                                    carbs_g = (totals.carbs_g * 10).roundToInt() / 10.0,
                                    fat_g = (totals.fat_g * 10).roundToInt() / 10.0,
                                    fiber_g = (totals.fiber_g * 10).roundToInt() / 10.0,
                                    sugar_g = (totals.sugar_g * 10).roundToInt() / 10.0,
                                    sodium_mg = totals.sodium_mg.roundToInt().toDouble(),
                                    health_score = state.healthScore,
                                    health_reason = state.healthReason,
                                    confidence = state.confidence,
                                    notes = state.notes
                                )

                                val saveRes = repository.saveMeal(currentUser.uid, meal)
                                isSavingMeal = false
                                if (saveRes.isSuccess) {
                                    snapState = state.copy(isSaved = true)
                                    snackbarHostState.showSnackbar(SnapCalStrings.get("result.saved", currentLang))
                                } else {
                                    snackbarHostState.showSnackbar(SnapCalStrings.get("result.saveFailed", currentLang))
                                }
                            }
                        }
                    },
                    onViewDiary = onNavigateToDiary,
                    onSnapAnother = { snapState = SnapState.Idle }
                )
            }
        }
    }
}
}

private fun startAnalysis(
    bitmap: Bitmap,
    contextText: String,
    service: GeminiNutritionService,
    language: com.example.i18n.SnapLanguage,
    scope: kotlinx.coroutines.CoroutineScope,
    onStateChange: (SnapState) -> Unit
) {
    onStateChange(SnapState.Analyzing(bitmap))
    scope.launch {
        val analysis = service.analyzeMeal(bitmap, contextText, language)
        when (analysis) {
            is MealAnalysisResult.Success -> {
                val thumb = generateThumbnailBase64(bitmap)
                val mealType = guessMealType()
                onStateChange(
                    SnapState.Result(
                        photoBitmap = bitmap,
                        thumbnailBase64 = thumb,
                        mealName = analysis.mealName,
                        mealType = mealType,
                        items = analysis.items,
                        micronutrients = analysis.micronutrients,
                        healthScore = analysis.healthScore,
                        healthReason = analysis.healthReason,
                        confidence = analysis.confidence,
                        notes = analysis.notes,
                        contextText = contextText,
                        isSaved = false
                    )
                )
            }
            is MealAnalysisResult.NoFood -> {
                onStateChange(SnapState.NoFood(bitmap, analysis.reason))
            }
            is MealAnalysisResult.Error -> {
                onStateChange(SnapState.Error(bitmap, analysis.errorCode))
            }
        }
    }
}

@Composable
fun IdleView(
    onTakePhoto: () -> Unit,
    onUploadPhoto: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // Hero Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SnapCard)
                .border(1.dp, SnapCardBorder, RoundedCornerShape(24.dp))
                .clickable { onTakePhoto() }
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SnapPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = SnapPrimaryForeground,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "${t("snap.title1")}\n${t("snap.title2")}",
                fontFamily = FrauncesFontFamily,
                fontSize = 32.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Bold,
                color = SnapForeground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = t("snap.subtitle"),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = SnapMutedForeground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onTakePhoto,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SnapPrimary,
                        contentColor = SnapPrimaryForeground
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("snap_camera_button")
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = t("how.s1t"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onUploadPhoto,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SnapForeground),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = SolidColor(SnapCardBorder)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("upload_gallery_button")
                ) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = t("snap.upload"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // How it works section
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = t("how.title"),
                fontFamily = FrauncesFontFamily,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = SnapForeground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            HowItWorksCard(
                step = 1,
                icon = Icons.Default.CameraAlt,
                title = t("how.s1t"),
                desc = t("how.s1d")
            )
            HowItWorksCard(
                step = 2,
                icon = Icons.Default.AutoAwesome,
                title = t("how.s2t"),
                desc = t("how.s2d")
            )
            HowItWorksCard(
                step = 3,
                icon = Icons.Default.TrackChanges,
                title = t("how.s3t"),
                desc = t("how.s3d")
            )
        }
    }
}

@Composable
fun HowItWorksCard(step: Int, icon: ImageVector, title: String, desc: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SnapCard)
            .border(1.dp, SnapCardBorder, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SnapPrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = SnapPrimary, modifier = Modifier.size(20.dp))
            }
            Text(
                text = "$step",
                fontFamily = FrauncesFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SnapMutedForeground
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = SnapForeground)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = desc, fontSize = 13.sp, color = SnapMutedForeground, lineHeight = 18.sp)
    }
}

@Composable
fun AnalyzingView(photoBitmap: Bitmap) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = -0.1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLine"
    )

    var currentMsgIdx by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2200)
            currentMsgIdx = (currentMsgIdx + 1) % 5
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Image with scanner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, SnapCardBorder, RoundedCornerShape(24.dp))
        ) {
            Image(
                bitmap = photoBitmap.asImageBitmap(),
                contentDescription = t("analyzing.alt"),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Horizontal scanning bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .offset { IntOffset(x = 0, y = (scanProgress * 300 * density).roundToInt()) }
                    .background(SnapPrimary)
            )
        }

        // Spinner & cycling text
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = SnapPrimary,
                strokeWidth = 2.dp
            )
            Text(
                text = t("analyzing.$currentMsgIdx"),
                fontSize = 14.sp,
                color = SnapMutedForeground,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun NoFoodView(photoBitmap: Bitmap, reason: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Image(
            bitmap = photoBitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(24.dp))
        )

        Text(
            text = t("nofood.title"),
            fontFamily = FrauncesFontFamily,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = SnapForeground,
            textAlign = TextAlign.Center
        )

        Text(
            text = reason.ifEmpty { t("nofood.default") },
            fontSize = 14.sp,
            color = SnapMutedForeground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SnapPrimary, contentColor = SnapPrimaryForeground),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = t("nofood.retry"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun ErrorView(photoBitmap: Bitmap, errorCode: String, onRetry: () -> Unit, onNewPhoto: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Image(
            bitmap = photoBitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .alpha(0.7f)
                .clip(RoundedCornerShape(24.dp))
        )

        Text(
            text = t("error.title"),
            fontFamily = FrauncesFontFamily,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = SnapForeground,
            textAlign = TextAlign.Center
        )

        Text(
            text = t(errorCode),
            fontSize = 14.sp,
            color = SnapMutedForeground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SnapPrimary, contentColor = SnapPrimaryForeground),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = t("error.retry"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }

        OutlinedButton(
            onClick = onNewPhoto,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SnapForeground),
            border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(SnapCardBorder)),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = t("error.newPhoto"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun ResultView(
    state: SnapState.Result,
    isSaving: Boolean,
    isAddingItem: Boolean,
    onUpdateItem: (FoodItem) -> Unit,
    onRemoveItem: (FoodItem) -> Unit,
    onAddItem: (String, Double?) -> Unit,
    onMealTypeChange: (String) -> Unit,
    onReanalyze: (String) -> Unit,
    onSaveMeal: () -> Unit,
    onViewDiary: () -> Unit,
    onSnapAnother: () -> Unit
) {
    val totals = calculateTotals(state.items)
    var contextInput by remember { mutableStateOf(state.contextText) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Main Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SnapCard)
                .border(1.dp, SnapCardBorder, RoundedCornerShape(24.dp))
        ) {
            Image(
                bitmap = state.photoBitmap.asImageBitmap(),
                contentDescription = state.mealName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            )

            Column(modifier = Modifier.padding(20.dp)) {
                // Name & Confidence Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.mealName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                        fontFamily = FrauncesFontFamily,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = SnapForeground,
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when (state.confidence) {
                                    "high" -> SnapPrimary
                                    "medium" -> SnapSecondary
                                    else -> SnapMuted
                                }
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = t("conf.${state.confidence}"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (state.confidence == "high") SnapPrimaryForeground else SnapForeground
                        )
                    }
                }

                // Total Calories
                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "${totals.calories.roundToInt()}",
                        fontFamily = FrauncesFontFamily,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 54.sp,
                        color = SnapForeground
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = t("unit.kcal"),
                        fontSize = 18.sp,
                        color = SnapMutedForeground,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // MacroDonut
                MacroDonut(
                    proteinGrams = totals.protein_g,
                    carbsGrams = totals.carbs_g,
                    fatGrams = totals.fat_g
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 4 Stat Tiles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatTile(label = t("nut.fiber"), value = "${String.format(Locale.US, "%.1f", totals.fiber_g)} g", modifier = Modifier.weight(1f))
                    StatTile(label = t("nut.sugar"), value = "${String.format(Locale.US, "%.1f", totals.sugar_g)} g", modifier = Modifier.weight(1f))
                    StatTile(label = t("nut.sodium"), value = "${totals.sodium_mg.roundToInt()} mg", modifier = Modifier.weight(1f))
                    StatTile(label = t("nut.satFat"), value = "${String.format(Locale.US, "%.1f", totals.saturated_fat_g)} g", modifier = Modifier.weight(1f))
                }
            }
        }

        // 2. Section "What's on the plate"
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = t("result.plate"),
                fontFamily = FrauncesFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = SnapForeground
            )
            Text(
                text = t("result.plateHint"),
                fontSize = 13.sp,
                color = SnapMutedForeground
            )
            Spacer(modifier = Modifier.height(4.dp))
            ItemEditor(
                items = state.items,
                onUpdateItem = onUpdateItem,
                onRemoveItem = onRemoveItem,
                onAddItem = onAddItem,
                isAddingItem = isAddingItem
            )
        }

        // 3. Micronutrients Card (if any)
        if (state.micronutrients.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SnapCard)
                    .border(1.dp, SnapCardBorder, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Text(
                    text = t("result.micros"),
                    fontFamily = FrauncesFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SnapForeground
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.micronutrients.forEach { micro ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(SnapPrimarySoft)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${micro.name} · ${micro.amount}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = SnapForeground
                            )
                        }
                    }
                }
            }
        }

        // 4. Health Score Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SnapCard)
                .border(1.dp, SnapCardBorder, RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val scoreBg = when {
                state.healthScore >= 7 -> SnapPrimarySoft
                state.healthScore >= 4 -> SnapWarmSoft
                else -> Color(0x33D9453B)
            }
            val scoreTextColor = when {
                state.healthScore >= 7 -> SnapSuccess
                state.healthScore >= 4 -> SnapWarm
                else -> SnapDestructive
            }

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(scoreBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${state.healthScore}",
                    fontFamily = FrauncesFontFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = scoreTextColor
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = t("result.health"),
                    fontFamily = FrauncesFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SnapForeground
                )
                if (state.healthReason.isNotBlank()) {
                    Text(
                        text = state.healthReason,
                        fontSize = 13.sp,
                        color = SnapMutedForeground,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // 5. Assumptions Card (if any notes)
        if (state.notes.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SnapCard)
                    .border(1.dp, SnapCardBorder, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Text(
                    text = t("result.assumptions"),
                    fontFamily = FrauncesFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SnapForeground
                )
                Spacer(modifier = Modifier.height(8.dp))
                state.notes.forEach { note ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(text = "• ", color = SnapPrimary, fontSize = 14.sp)
                        Text(text = note, fontSize = 13.sp, color = SnapMutedForeground)
                    }
                }
            }
        }

        // 6. Context Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SnapCard)
                .border(1.dp, SnapCardBorder, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Text(
                text = t("result.contextTitle"),
                fontFamily = FrauncesFontFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = SnapForeground
            )
            Text(
                text = t("result.contextHint"),
                fontSize = 13.sp,
                color = SnapMutedForeground,
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            BasicTextField(
                value = contextInput,
                onValueChange = { contextInput = it.take(500) },
                textStyle = androidx.compose.ui.text.TextStyle(color = SnapForeground, fontSize = 14.sp),
                cursorBrush = SolidColor(SnapPrimary),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                            .background(SnapInput, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        if (contextInput.isEmpty()) {
                            Text(
                                text = t("result.contextPlaceholder"),
                                fontSize = 13.sp,
                                color = SnapMutedForeground
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { onReanalyze(contextInput.trim()) },
                enabled = contextInput.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SnapSecondary,
                    contentColor = SnapForeground
                ),
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = t("result.reanalyze"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // 7. Save Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SnapCard)
                .border(1.dp, SnapCardBorder, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Text(
                text = t("result.saveTitle"),
                fontFamily = FrauncesFontFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = SnapForeground
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Meal Type Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("breakfast", "lunch", "dinner", "snack").forEach { type ->
                    val isSelected = state.mealType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) SnapPrimary else SnapCard)
                            .border(1.dp, if (isSelected) SnapPrimary else SnapCardBorder, RoundedCornerShape(20.dp))
                            .clickable { onMealTypeChange(type) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = t("meal.$type"),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) SnapPrimaryForeground else SnapMutedForeground
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (state.isSaved) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SnapPrimarySoft)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = SnapPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = t("result.saved"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SnapForeground
                        )
                    }

                    Button(
                        onClick = onViewDiary,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SnapSecondary, contentColor = SnapForeground),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = t("result.viewDiary"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                Button(
                    onClick = onSaveMeal,
                    enabled = !isSaving,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SnapPrimary,
                        contentColor = SnapPrimaryForeground
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_meal_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = SnapPrimaryForeground,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(imageVector = Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = t("result.save"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // 8. Bottom Disclaimer & Another Photo
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = SnapMutedForeground, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = t("disclaimer"), fontSize = 11.sp, color = SnapMutedForeground)
            }

            OutlinedButton(
                onClick = onSnapAnother,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SnapMutedForeground),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(SnapCardBorder))
            ) {
                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = t("result.another"), fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SnapMuted)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label.uppercase(Locale.ROOT),
            fontSize = 10.sp,
            letterSpacing = 0.5.sp,
            color = SnapMutedForeground,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = SnapForeground
        )
    }
}

private fun guessMealType(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 4..10 -> "breakfast"
        in 11..14 -> "lunch"
        in 17..21 -> "dinner"
        else -> "snack"
    }
}

private fun loadAndCompressBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        val original = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }

        // Scale so longest side <= 1280px
        val maxDim = max(original.width, original.height)
        if (maxDim > 1280) {
            val scale = 1280f / maxDim
            Bitmap.createScaledBitmap(
                original,
                (original.width * scale).roundToInt(),
                (original.height * scale).roundToInt(),
                true
            )
        } else {
            original
        }
    } catch (e: Exception) {
        null
    }
}

private fun generateThumbnailBase64(bitmap: Bitmap): String? {
    return try {
        val maxDim = max(bitmap.width, bitmap.height)
        val scale = if (maxDim > 320) 320f / maxDim else 1f
        val scaled = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).roundToInt(),
            (bitmap.height * scale).roundToInt(),
            true
        )
        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 70, stream)
        val bytes = stream.toByteArray()
        "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    } catch (e: Exception) {
        null
    }
}
