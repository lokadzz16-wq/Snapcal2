package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import com.example.data.FirestoreRepository
import com.example.data.GeminiNutritionService
import com.example.data.configureAppCheck
import com.example.i18n.LocalLanguage
import com.example.i18n.SnapLanguage
import com.example.ui.components.AuthDialog
import com.example.ui.components.BottomNavTabs
import com.example.ui.components.SnapCalFooter
import com.example.ui.components.TopNavBar
import com.example.ui.components.performSignOut
import com.example.ui.screens.DiaryScreen
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.SnapScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // MANDATORY: Configure App Check before setContent, ViewModels, or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            SnapCalApp()
        }
    }
}

@Composable
fun SnapCalApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("snapcal_prefs", Context.MODE_PRIVATE) }

    var currentLanguage by remember {
        val savedLangCode = prefs.getString("snapcal.lang", null)
        val initial = if (savedLangCode != null) {
            SnapLanguage.entries.find { it.code == savedLangCode } ?: SnapLanguage.EN
        } else {
            val sysLang = java.util.Locale.getDefault().language.lowercase()
            when {
                sysLang.startsWith("ar") -> SnapLanguage.AR
                sysLang.startsWith("fr") -> SnapLanguage.FR
                else -> SnapLanguage.EN
            }
        }
        mutableStateOf(initial)
    }

    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }
    var selectedTab by remember { mutableStateOf("snap") }
    val snackbarHostState = remember { SnackbarHostState() }

    var showAuthDialog by remember { mutableStateOf(false) }
    var authReason by remember { mutableStateOf("") }

    // Camera runtime permission state and launcher
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var onCameraPermissionGrantedCallback by remember { mutableStateOf<(() -> Unit)?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            onCameraPermissionGrantedCallback?.invoke()
            onCameraPermissionGrantedCallback = null
        } else {
            onCameraPermissionGrantedCallback = null
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Camera permission is required to snap food photos. You can also upload photos from your gallery."
                )
            }
        }
    }

    val repository = remember { FirestoreRepository(context) }
    val nutritionService = remember { GeminiNutritionService() }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    // Predictive back navigation: if not on "snap", Back returns to "snap"
    BackHandler(enabled = selectedTab != "snap") {
        selectedTab = "snap"
    }

    val layoutDirection = if (currentLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalLanguage provides currentLanguage,
        LocalLayoutDirection provides layoutDirection
    ) {
        MyApplicationTheme {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopNavBar(
                        currentUser = currentUser,
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        onLanguageChange = { newLang ->
                            currentLanguage = newLang
                            prefs.edit().putString("snapcal.lang", newLang.code).apply()
                        },
                        onSignInClick = {
                            authReason = ""
                            showAuthDialog = true
                        },
                        onSignOutClick = {
                            performSignOut(context, scope) {
                                currentUser = null
                            }
                        }
                    )
                },
                bottomBar = {
                    BottomNavTabs(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                },
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState)
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            when (selectedTab) {
                                "snap" -> SnapScreen(
                                    currentUser = currentUser,
                                    repository = repository,
                                    nutritionService = nutritionService,
                                    snackbarHostState = snackbarHostState,
                                    onRequestAuth = { reason ->
                                        authReason = reason
                                        showAuthDialog = true
                                    },
                                    onNavigateToDiary = { selectedTab = "diary" },
                                    hasCameraPermission = hasCameraPermission,
                                    onRequestCameraPermission = { onGranted ->
                                        onCameraPermissionGrantedCallback = onGranted
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                )

                                "diary" -> DiaryScreen(
                                    currentUser = currentUser,
                                    repository = repository,
                                    nutritionService = nutritionService,
                                    snackbarHostState = snackbarHostState,
                                    onRequestAuth = { reason ->
                                        authReason = reason
                                        showAuthDialog = true
                                    },
                                    onNavigateToSnap = { selectedTab = "snap" }
                                )

                                "insights" -> InsightsScreen(
                                    currentUser = currentUser,
                                    repository = repository,
                                    onRequestAuth = { reason ->
                                        authReason = reason
                                        showAuthDialog = true
                                    },
                                    onNavigateToSnap = { selectedTab = "snap" }
                                )

                                "goals" -> GoalsScreen(
                                    currentUser = currentUser,
                                    repository = repository,
                                    snackbarHostState = snackbarHostState,
                                    onRequestAuth = { reason ->
                                        authReason = reason
                                        showAuthDialog = true
                                    }
                                )
                            }
                        }

                        // Footer at bottom
                        SnapCalFooter()
                    }
                }
            }

            if (showAuthDialog) {
                AuthDialog(
                    reasonText = authReason,
                    onDismiss = { showAuthDialog = false },
                    onSignedIn = {
                        currentUser = Firebase.auth.currentUser
                        showAuthDialog = false
                    }
                )
            }
        }
    }
}
