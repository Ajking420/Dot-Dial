package com.example

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.ai.configureAppCheck
import com.example.telecom.CallDisplayMode
import com.example.telecom.CallLifecycleState
import com.example.ui.components.DotDialBackgroundHost
import com.example.ui.screens.ActiveCallOverlay
import com.example.ui.screens.AiCommandBottomSheet
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.DialerScreen
import com.example.ui.screens.FirstLaunchGoogleSignInDialog
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.IncomingFloatingTopBanner
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatusBarCallPill
import com.example.ui.screens.VaultScreen
import com.example.data.preferences.DialerSettings
import com.example.ui.theme.DotDialTheme
import com.example.ui.theme.LocalDotDialStyle
import com.example.ui.theme.LocalUiHapticPulse
import com.example.ui.theme.resolveSystemBarsUseDarkIcons
import com.example.viewmodel.AppRoute
import com.example.viewmodel.DotDialViewModel
import com.example.widget.DotDialContactWidgetProvider
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.math.abs
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val launchIntentState = mutableStateOf<Intent?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or ViewModels
        configureAppCheck(this, intent)

        // STEP 2: Enable Edge-to-Edge and render Compose UI
        enableEdgeToEdge()
        launchIntentState.value = intent

        setContent {
            val viewModel: DotDialViewModel = viewModel()
            DotDialAppRoot(
                viewModel = viewModel,
                launchIntent = launchIntentState.value,
                onApplyDisplayFps = ::applyDisplayFpsSettings
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchIntentState.value = intent
    }

    private fun applyDisplayFpsSettings(
        targetFps: Int,
        adaptiveRefreshRate: Boolean
    ): List<Int> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.setFrameRateBoostOnTouchEnabled(true)
            window.isFrameRatePowerSavingsBalanced = adaptiveRefreshRate
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val display = window?.decorView?.display ?: return emptyList()
            val currentMode = display.mode
            val compatibleModes = display.supportedModes.filter {
                it.physicalWidth == currentMode.physicalWidth &&
                    it.physicalHeight == currentMode.physicalHeight
            }
            val supportedRates = compatibleModes
                .map { it.refreshRate.roundToInt() }
                .distinct()
                .sorted()

            val desiredModeId = if (targetFps <= 0) {
                0
            } else {
                compatibleModes.minByOrNull {
                    abs(it.refreshRate - targetFps.toFloat())
                }?.modeId ?: 0
            }

            if (window.attributes.preferredDisplayModeId != desiredModeId) {
                val attrs = window.attributes
                attrs.preferredDisplayModeId = desiredModeId
                window.attributes = attrs
            }
            return supportedRates
        }
        return listOf(60)
    }
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}

private fun extractDialNumber(intent: Intent?): String? {
    val data = intent?.data ?: return null
    return if (data.scheme == "tel") data.schemeSpecificPart else null
}

private data class NavTabItem(
    val route: AppRoute,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

@Composable
private fun DotDialAppRoot(
    viewModel: DotDialViewModel,
    launchIntent: Intent?,
    onApplyDisplayFps: (Int, Boolean) -> List<Int>
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val firstLaunchSignInReady by viewModel.firstLaunchSignInReady.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val navRequest by viewModel.navigationRequest.collectAsStateWithLifecycle()
    val isCallIdle by remember(viewModel) {
        viewModel.callSession
            .map { it.lifecycle == CallLifecycleState.IDLE }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = true)

    var showFirstLaunchSignInDialog by remember { mutableStateOf(false) }
    LaunchedEffect(firstLaunchSignInReady, isCallIdle) {
        if (firstLaunchSignInReady && isCallIdle) {
            showFirstLaunchSignInDialog = true
            viewModel.dismissGoogleSignInPrompt()
        }
    }
    LaunchedEffect(settings.signedInGoogleEmail) {
        if (settings.signedInGoogleEmail.isNotBlank()) {
            showFirstLaunchSignInDialog = false
        }
    }

    var showAiSheet by remember { mutableStateOf(false) }
    var supportedRefreshRates by remember { mutableStateOf(listOf(60, 90, 120)) }

    LaunchedEffect(settings.preferredFps, settings.adaptiveRefreshRate) {
        val detected = onApplyDisplayFps(settings.preferredFps, settings.adaptiveRefreshRate)
        if (detected.isNotEmpty()) {
            supportedRefreshRates = detected
        }
    }

    val requiredPermissions = remember {
        buildList {
            add(Manifest.permission.CALL_PHONE)
            add(Manifest.permission.READ_CONTACTS)
            add(Manifest.permission.WRITE_CONTACTS)
            add(Manifest.permission.READ_CALL_LOG)
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    var hasPermissions by remember {
        mutableStateOf(
            requiredPermissions.all { perm ->
                ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermissions = results.values.all { it }
        viewModel.syncDeviceContactsAndLogs()
    }

    val driveUiState by viewModel.driveUiState.collectAsStateWithLifecycle()
    val googleAuthState by viewModel.googleAuthState.collectAsStateWithLifecycle()
    val driveConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onDriveConsentActivityResult(result.data)
    }

    LaunchedEffect(driveUiState.pendingConsentIntent) {
        val pending = driveUiState.pendingConsentIntent ?: return@LaunchedEffect
        viewModel.consumeDrivePendingConsent()
        runCatching {
            driveConsentLauncher.launch(IntentSenderRequest.Builder(pending).build())
        }.onFailure { err ->
            viewModel.showMessage(err.localizedMessage ?: "Unable to launch Google Drive authorization.")
        }
    }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    val navController = rememberNavController()
    LaunchedEffect(launchIntent) {
        val activeIntent = launchIntent ?: return@LaunchedEffect
        val openContacts = activeIntent.getBooleanExtra(
            DotDialContactWidgetProvider.EXTRA_OPEN_CONTACTS,
            false
        )
        if (openContacts) {
            navigateTab(navController, AppRoute.Contacts)
            return@LaunchedEffect
        }
        val dialNumber = extractDialNumber(activeIntent)
        if (!dialNumber.isNullOrBlank()) {
            val callImmediately = activeIntent.getBooleanExtra(
                DotDialContactWidgetProvider.EXTRA_WIDGET_CALL_NOW,
                false
            )
            if (callImmediately) {
                viewModel.placeCall(dialNumber)
            } else {
                viewModel.clearDialQuery()
                dialNumber.forEach { ch -> viewModel.appendDialDigit(ch.toString()) }
            }
        }
    }

    LaunchedEffect(navRequest) {
        val req = navRequest ?: return@LaunchedEffect
        navigateTab(navController, req.route)
        viewModel.clearNavigationRequest()
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val tabs = remember {
        listOf(
            NavTabItem(AppRoute.Dialer, R.string.nav_dialer, Icons.Filled.Dialpad, Icons.Outlined.Dialpad, "nav_dialer"),
            NavTabItem(AppRoute.History, R.string.nav_history, Icons.Filled.History, Icons.Outlined.History, "nav_history"),
            NavTabItem(AppRoute.Contacts, R.string.nav_contacts, Icons.Filled.Contacts, Icons.Outlined.Contacts, "nav_contacts"),
            NavTabItem(AppRoute.Vault, R.string.nav_vault, Icons.Filled.Lock, Icons.Outlined.Lock, "nav_vault"),
            NavTabItem(AppRoute.Analytics, R.string.nav_analytics, Icons.Filled.Analytics, Icons.Outlined.Analytics, "nav_analytics"),
            NavTabItem(AppRoute.Settings, R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
        )
    }

    val pinWidgetAction: () -> Unit = {
        val requested = DotDialContactWidgetProvider.requestPinWidget(context)
        viewModel.showMessage(
            if (requested) {
                "Confirm on your home screen to pin the Contact Widget."
            } else {
                "Open your home screen Widgets picker and add Dot Dial Contacts."
            }
        )
    }

    DotDialTheme(
        preset = settings.themePreset,
        autoDayNightThemeEnabled = settings.autoDayNightThemeEnabled,
        amoledBlackEnabled = settings.amoledBlackEnabled,
        dotDensity = if (settings.ultraSmoothMode) minOf(settings.dotDensity, 0.35f) else settings.dotDensity,
        fontPreset = settings.fontPreset,
        fontStyleMode = settings.fontStyleMode,
        fontScale = settings.fontScale,
        coolJazzGlow = settings.coolJazzGlowEnabled && !settings.ultraSmoothMode,
        customFontPath = settings.customFontPath,
        customBgHex = settings.customBgHex,
        customSurfaceHex = settings.customSurfaceHex,
        customAccentHex = settings.customAccentHex,
        customTextHex = settings.customTextHex
    ) {
        val dotStyle = LocalDotDialStyle.current
        val themeBg = MaterialTheme.colorScheme.background
        val (useDarkStatusIcons, useDarkNavIcons) = remember(
            settings.uiBackgroundMode,
            settings.amoledBlackEnabled,
            settings.dotMatrixBgHex,
            settings.autoDayNightThemeEnabled,
            settings.colorPlateStyle,
            settings.solidBgHex,
            settings.gradientStartHex,
            settings.gradientEndHex,
            settings.wallpaperPath,
            settings.wallpaperDimStrength,
            themeBg,
            dotStyle.isDark
        ) {
            resolveSystemBarsUseDarkIcons(
                settings = settings,
                themeBackground = themeBg,
                isThemeDark = dotStyle.isDark
            )
        }
        val view = LocalView.current
        DisposableEffect(context, view, useDarkStatusIcons, useDarkNavIcons) {
            if (!view.isInEditMode) {
                val activity = context.findComponentActivity()
                if (activity != null) {
                    activity.enableEdgeToEdge(
                        statusBarStyle = if (useDarkStatusIcons) {
                            SystemBarStyle.light(
                                android.graphics.Color.TRANSPARENT,
                                android.graphics.Color.TRANSPARENT
                            )
                        } else {
                            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                        },
                        navigationBarStyle = if (useDarkNavIcons) {
                            SystemBarStyle.light(
                                android.graphics.Color.TRANSPARENT,
                                android.graphics.Color.argb(0x40, 0x00, 0x00, 0x00)
                            )
                        } else {
                            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                        }
                    )
                    WindowCompat.getInsetsController(activity.window, view).apply {
                        isAppearanceLightStatusBars = useDarkStatusIcons
                        isAppearanceLightNavigationBars = useDarkNavIcons
                    }
                }
            }
            onDispose {}
        }

        CompositionLocalProvider(
            LocalUiHapticPulse provides { viewModel.pulseUiHaptic() }
        ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Box(modifier = Modifier.fillMaxSize()) {
                DotDialBackgroundHost(settings = settings)

                Row(modifier = Modifier.fillMaxSize()) {
                    if (useRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                        ) {
                            tabs.forEach { tab ->
                                val selected = currentDestination?.hasRoute(tab.route::class) == true
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = {
                                        viewModel.playUiClick()
                                        viewModel.pulseUiHaptic()
                                        navigateTab(navController, tab.route)
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = stringResource(tab.labelRes)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(tab.labelRes),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    modifier = Modifier.testTag(tab.tag)
                                )
                            }
                        }
                    }

                    Scaffold(
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets.safeDrawing,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        floatingActionButton = {
                            if (isCallIdle) {
                                SmallFloatingActionButton(
                                    onClick = {
                                        viewModel.playUiClick()
                                        viewModel.pulseUiHaptic()
                                        showAiSheet = true
                                    },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.testTag("ai_full_control_fab")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = stringResource(R.string.ai_command_pill)
                                    )
                                }
                            }
                        },
                        bottomBar = {
                            if (!useRail) {
                                FloatingCompactBottomBar(
                                    tabs = tabs,
                                    currentDestination = currentDestination,
                                    onTabSelected = { route ->
                                        viewModel.playUiClick()
                                        viewModel.pulseUiHaptic()
                                        navigateTab(navController, route)
                                    }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = AppRoute.Dialer,
                                enterTransition = {
                                    fadeIn(tween(210, easing = FastOutSlowInEasing)) +
                                        scaleIn(
                                            initialScale = 0.975f,
                                            animationSpec = tween(210, easing = FastOutSlowInEasing)
                                        )
                                },
                                exitTransition = {
                                    fadeOut(tween(150, easing = FastOutSlowInEasing))
                                },
                                popEnterTransition = {
                                    fadeIn(tween(210, easing = FastOutSlowInEasing)) +
                                        scaleIn(
                                            initialScale = 0.975f,
                                            animationSpec = tween(210, easing = FastOutSlowInEasing)
                                        )
                                },
                                popExitTransition = {
                                    fadeOut(tween(150, easing = FastOutSlowInEasing))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 680.dp)
                            ) {
                                composable<AppRoute.Dialer> {
                                    val dialerQuery by viewModel.dialerQuery.collectAsStateWithLifecycle()
                                    val isDialpadVisible by viewModel.isDialpadVisible.collectAsStateWithLifecycle()
                                    val predictiveMatches by viewModel.predictiveMatches.collectAsStateWithLifecycle()
                                    val callLogs by viewModel.callLogs.collectAsStateWithLifecycle()
                                    val allContacts by viewModel.contacts.collectAsStateWithLifecycle()
                                    val isDefaultDialer by viewModel.isDefaultDialer.collectAsStateWithLifecycle()
                                    DialerScreen(
                                        dialerQuery = dialerQuery,
                                        isDialpadVisible = isDialpadVisible,
                                        predictiveMatches = predictiveMatches,
                                        recentLogs = callLogs,
                                        contacts = allContacts,
                                        isDefaultDialer = isDefaultDialer,
                                        hasPermissions = hasPermissions,
                                        onRequestPermissions = {
                                            permissionLauncher.launch(requiredPermissions)
                                        },
                                        onRequestDefaultDialer = {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                val rm = context.getSystemService(RoleManager::class.java)
                                                if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                                                    roleLauncher.launch(
                                                        rm.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                                                    )
                                                }
                                            } else {
                                                val roleIntent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                                                    .putExtra(
                                                        TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
                                                        context.packageName
                                                    )
                                                roleLauncher.launch(roleIntent)
                                            }
                                        },
                                        onToggleDialpad = viewModel::toggleDialpadVisible,
                                        onDigitPress = viewModel::appendDialDigit,
                                        onBackspace = viewModel::backspaceDialDigit,
                                        onClearQuery = viewModel::clearDialQuery,
                                        onPlaceCall = viewModel::placeCall,
                                        onSimulateIncoming = { spam ->
                                            viewModel.simulateIncomingCall(spamTest = spam, compactPopup = true)
                                        },
                                        onOpenAiCommand = { showAiSheet = true },
                                        aiAssistantEnabled = settings.aiAssistantEnabled,
                                        aiBusyModeEnabled = settings.aiBusyModeEnabled,
                                        onToggleBusyMode = viewModel::setAiBusyModeEnabled,
                                        signedInName = settings.signedInGoogleName,
                                        signedInEmail = settings.signedInGoogleEmail,
                                        onGoogleAccountClick = {
                                            if (settings.signedInGoogleEmail.isBlank()) {
                                                viewModel.signInWithGoogle(context)
                                            } else {
                                                navigateTab(navController, AppRoute.Settings)
                                            }
                                        }
                                    )
                                }

                                composable<AppRoute.History> {
                                    val filteredHistory by viewModel.filteredHistory.collectAsStateWithLifecycle()
                                    val groupedHistory by viewModel.groupedHistory.collectAsStateWithLifecycle()
                                    val historyFilter by viewModel.historyFilter.collectAsStateWithLifecycle()
                                    HistoryScreen(
                                        logs = filteredHistory,
                                        groupedHistory = groupedHistory,
                                        selectedFilter = historyFilter,
                                        onSelectFilter = viewModel::setHistoryFilter,
                                        onCallNumber = viewModel::placeCall,
                                        onVideoCallNumber = viewModel::placeVideoCall,
                                        onStatusMessage = viewModel::showMessage,
                                        onDeleteLog = viewModel::deleteCallLog,
                                        onClearHistory = viewModel::clearCallHistory,
                                        onExportJsonBackup = viewModel::exportBackupJson,
                                        onExportContactsVcf = viewModel::exportContactsVcf,
                                        onRestoreBackupJson = viewModel::restoreBackupJson,
                                        onOpenDialpadOverlay = {
                                            viewModel.setDialpadVisible(true)
                                            navigateTab(navController, AppRoute.Dialer)
                                        }
                                    )
                                }

                                composable<AppRoute.Contacts> {
                                    val contacts by viewModel.filteredContacts.collectAsStateWithLifecycle()
                                    val contactSearch by viewModel.contactSearchQuery.collectAsStateWithLifecycle()
                                    val vibrationPatterns by viewModel.vibrationPatterns.collectAsStateWithLifecycle()
                                    val discoveredGoogleAccounts by viewModel.discoveredGoogleAccounts.collectAsStateWithLifecycle()
                                    val resolvedContactAccount = remember(
                                        settings.newContactStorageMode,
                                        settings.preferredGoogleContactAccount,
                                        settings.signedInGoogleEmail,
                                        settings.syncedGoogleAccounts,
                                        discoveredGoogleAccounts
                                    ) {
                                        viewModel.resolveTargetGoogleContactAccount(settings)
                                    }
                                    ContactsScreen(
                                        contacts = contacts,
                                        searchQuery = contactSearch,
                                        vibrationPatterns = vibrationPatterns,
                                        showQuickActions = settings.showContactQuickActions,
                                        googleSyncEnabled = settings.googleSyncEnabled,
                                        discoveredGoogleAccounts = discoveredGoogleAccounts,
                                        syncedGoogleAccounts = settings.syncedGoogleAccounts,
                                        newContactStorageMode = settings.newContactStorageMode,
                                        targetGoogleContactAccount = resolvedContactAccount,
                                        onSearchChange = viewModel::setContactSearchQuery,
                                        onSyncDeviceContacts = { viewModel.syncDeviceContactsAndLogs(false) },
                                        onToggleGoogleSync = viewModel::setGoogleSyncEnabled,
                                        onAddGoogleAccount = viewModel::addGoogleSyncAccount,
                                        onRemoveGoogleAccount = viewModel::removeGoogleSyncAccount,
                                        onCallContact = { contact ->
                                            viewModel.placeCall(contact.phoneNumber, contact)
                                        },
                                        onCallNumber = { number, contact ->
                                            viewModel.placeCall(number, contact)
                                        },
                                        onSetDefaultNumber = viewModel::setDefaultContactNumber,
                                        onSaveContact = { id, name, phone, company, email, notes, fav, photo, card, video, vibId, extraJson ->
                                            viewModel.saveContact(
                                                existingId = id,
                                                name = name,
                                                phoneNumber = phone,
                                                company = company,
                                                email = email,
                                                notes = notes,
                                                isFavorite = fav,
                                                photoUri = photo,
                                                callingCardUri = card,
                                                videoBackdropUri = video,
                                                vibrationPatternId = vibId,
                                                additionalNumbersJson = extraJson
                                            )
                                        },
                                        onToggleFavorite = viewModel::toggleFavorite,
                                        onToggleBlocked = viewModel::toggleBlocked,
                                        onAssignPhoto = viewModel::assignContactPhoto,
                                        onAssignCallingCard = viewModel::assignContactCallingCard,
                                        onAssignVideoBackdrop = viewModel::assignContactVideoBackdrop,
                                        onAssignBackdropPhoto = viewModel::assignContactBackdropPhoto,
                                        onAssignBackdropMode = viewModel::assignContactBackdropMode,
                                        onAssignVibrationPattern = viewModel::assignContactVibrationPattern,
                                        onDeleteContact = viewModel::deleteContact,
                                        onPinContactWidget = pinWidgetAction
                                    )
                                }

                                composable<AppRoute.Vault> {
                                    val recordings by viewModel.recordings.collectAsStateWithLifecycle()
                                    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsStateWithLifecycle()
                                    val playingRecordingId by viewModel.playingRecordingId.collectAsStateWithLifecycle()
                                    val summaryState by viewModel.summaryState.collectAsStateWithLifecycle()
                                    VaultScreen(
                                        recordings = recordings,
                                        vaultPinSet = settings.vaultPin.isNotBlank(),
                                        isUnlocked = isVaultUnlocked,
                                        playingRecordingId = playingRecordingId,
                                        summaryState = summaryState,
                                        onUnlockWithPin = viewModel::unlockVault,
                                        onLockVault = viewModel::lockVault,
                                        onTogglePlay = viewModel::toggleRecordingPlayback,
                                        onSummarizeRecording = viewModel::summarizeRecording,
                                        onDeleteRecording = viewModel::deleteRecording
                                    )
                                }

                                composable<AppRoute.Analytics> {
                                    val analyticsSummary by viewModel.analyticsSummary.collectAsStateWithLifecycle()
                                    val analyticsPeriod by viewModel.analyticsPeriod.collectAsStateWithLifecycle()
                                    val spamRules by viewModel.spamRules.collectAsStateWithLifecycle()
                                    val spamCheckState by viewModel.spamCheckState.collectAsStateWithLifecycle()
                                    AnalyticsScreen(
                                        analytics = analyticsSummary,
                                        selectedPeriod = analyticsPeriod,
                                        spamRules = spamRules,
                                        spamCheckState = spamCheckState,
                                        onSelectPeriod = viewModel::setAnalyticsPeriod,
                                        onAddSpamRule = { pattern, label ->
                                            viewModel.addSpamRule(pattern, label)
                                        },
                                        onToggleSpamRule = viewModel::toggleSpamRule,
                                        onDeleteSpamRule = viewModel::deleteSpamRule,
                                        onCheckNumberWithGemini = viewModel::checkNumberWithGemini
                                    )
                                }

                                composable<AppRoute.Settings> {
                                    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
                                    val vibrationPatterns by viewModel.vibrationPatterns.collectAsStateWithLifecycle()
                                    val discoveredGoogleAccounts by viewModel.discoveredGoogleAccounts.collectAsStateWithLifecycle()
                                    val availableAccounts = remember(discoveredGoogleAccounts, settings.syncedGoogleAccounts) {
                                        (discoveredGoogleAccounts + settings.syncedGoogleAccounts.toList()).distinct()
                                    }
                                    val resolvedContactAccount = remember(
                                        settings.newContactStorageMode,
                                        settings.preferredGoogleContactAccount,
                                        settings.signedInGoogleEmail,
                                        settings.syncedGoogleAccounts,
                                        discoveredGoogleAccounts
                                    ) {
                                        viewModel.resolveTargetGoogleContactAccount(settings)
                                    }
                                    SettingsScreen(
                                        settings = settings,
                                        supportedRefreshRates = supportedRefreshRates,
                                        vibrationPatterns = vibrationPatterns,
                                        onSelectTheme = viewModel::setThemePreset,
                                        onToggleAutoDayNight = viewModel::setAutoDayNightThemeEnabled,
                                        onToggleAmoledBlack = viewModel::setAmoledBlack,
                                        onUpdateCustomColors = viewModel::setCustomThemeColors,
                                        onDotDensityChange = viewModel::setDotDensity,
                                        onSelectFontPreset = viewModel::setFontPreset,
                                        onSelectFontStyle = viewModel::setFontStyleMode,
                                        onFontScaleChange = viewModel::setFontScale,
                                        onToggleCoolJazzGlow = viewModel::setCoolJazzGlow,
                                        onImportCustomFontUri = viewModel::importCustomFontUri,
                                        onSetPreferredFps = viewModel::setPreferredFps,
                                        onSetAdaptiveRefreshRate = viewModel::setAdaptiveRefreshRate,
                                        onSetUltraSmoothMode = viewModel::setUltraSmoothMode,
                                        onSetUiSounds = viewModel::setUiSoundsEnabled,
                                        onSetUiVibration = viewModel::setUiVibrationEnabled,
                                        onSetDialpadSoundStyle = viewModel::setDialpadSoundStyle,
                                        onSetAudioOutputMode = viewModel::setAudioOutputMode,
                                        onPickCustomRingtone = viewModel::setCustomRingtoneUri,
                                        onSelectDefaultBackdropMode = viewModel::setDefaultBackdropMode,
                                        onPickDefaultVideo = viewModel::setDefaultVideoBackdrop,
                                        onPickDefaultPhoto = viewModel::setDefaultPhotoBackdrop,
                                        onPickDefaultCallingCard = viewModel::setDefaultCallingCardBackdrop,
                                        contacts = contacts,
                                        onAssignContactCallingCard = viewModel::assignContactCallingCard,
                                        onRedTintStrengthChange = viewModel::setVideoRedTintStrength,
                                        onSelectVibrationPattern = viewModel::selectActiveVibrationPattern,
                                        onTestVibrationPattern = viewModel::testVibrationPattern,
                                        onSaveCustomVibration = viewModel::saveCustomVibrationPattern,
                                        onDeleteCustomVibration = viewModel::deleteCustomVibrationPattern,
                                        onSetAutoRecordMode = viewModel::setAutoRecordMode,
                                        onSetSilentRecording = viewModel::setSilentCallRecording,
                                        onSetAutoSummarize = viewModel::setAutoSummarizeEnabled,
                                        onSetVoiceControl = viewModel::setVoiceCommandControl,
                                        onSetShowContactQuickActions = viewModel::setShowContactQuickActions,
                                        onSelectBackgroundMode = viewModel::setUiBackgroundMode,
                                        onToggleDotMatrix = viewModel::setDotMatrixEnabled,
                                        onSelectDotMatrixBgHex = viewModel::setDotMatrixBgHex,
                                        onSelectColorPlateStyle = viewModel::setColorPlateStyle,
                                        onSelectSolidBgHex = viewModel::setSolidBgHex,
                                        onSelectGradientColors = viewModel::setGradientColors,
                                        onPickWallpaper = viewModel::setWallpaperUri,
                                        onWallpaperDimChange = viewModel::setWallpaperDimStrength,
                                        onSetVaultPin = viewModel::setVaultPin,
                                        onExportBackupJson = viewModel::exportBackupJson,
                                        onExportSettingsJson = viewModel::exportSettingsJson,
                                        onExportContactsVcf = viewModel::exportContactsVcf,
                                        onRestoreBackupJson = viewModel::restoreBackupJson,
                                        onRestoreSettingsJson = viewModel::restoreSettingsJson,
                                        driveUiState = driveUiState,
                                        onConnectDrive = { viewModel.connectGoogleDrive() },
                                        onDisconnectDrive = viewModel::disconnectGoogleDrive,
                                        onBackupToDrive = viewModel::backupToGoogleDrive,
                                        onRefreshDriveBackups = viewModel::refreshDriveBackups,
                                        onRestoreFromDrive = viewModel::restoreFromGoogleDrive,
                                        onSetAiAssistantEnabled = viewModel::setAiAssistantEnabled,
                                        onSetAiBusyMode = viewModel::setAiBusyModeEnabled,
                                        onSetAiOwnerName = viewModel::setAiAssistantOwnerName,
                                        onSetAiTriggerMode = viewModel::setAiAssistantTriggerMode,
                                        onSetAiDelaySeconds = viewModel::setAiAssistantDelaySeconds,
                                        onSetAiLanguageMode = viewModel::setAiAssistantLanguageMode,
                                        onSetAiAutoRecord = viewModel::setAiAssistantAutoRecord,
                                        onPreviewAiVoice = viewModel::previewAiAssistantVoice,
                                        onPinContactWidget = pinWidgetAction,
                                        googleAuthState = googleAuthState,
                                        onGoogleSignInOrSwitch = { viewModel.signInWithGoogle(context) },
                                        onGoogleSignOut = viewModel::signOutGoogleAccount,
                                        availableGoogleAccounts = availableAccounts,
                                        resolvedTargetContactAccount = resolvedContactAccount,
                                        onSetNewContactStorageMode = viewModel::setNewContactStorageMode,
                                        onSelectPreferredGoogleContactAccount = viewModel::setPreferredGoogleContactAccount,
                                        onSetVoiceChangerMode = viewModel::setVoiceChangerMode,
                                        onSelectVoiceChangerPreset = viewModel::setVoiceChangerPreset,
                                        onPreviewVoiceChangerPreset = viewModel::previewVoiceChangerPreset
                                    )
                                }
                            }
                        }
                    }
                }

                CallOverlaysHost(
                    viewModel = viewModel,
                    settings = settings
                )

                if (showFirstLaunchSignInDialog && isCallIdle) {
                    FirstLaunchGoogleSignInDialog(
                        authState = googleAuthState,
                        onSignInClick = { viewModel.signInWithGoogle(context) },
                        onSkipClick = {
                            showFirstLaunchSignInDialog = false
                            viewModel.dismissGoogleSignInPrompt()
                        }
                    )
                }

                if (showAiSheet) {
                    val aiCommandState by viewModel.aiCommandState.collectAsStateWithLifecycle()
                    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
                    AiCommandBottomSheet(
                        aiState = aiCommandState,
                        voiceState = voiceState,
                        onDismiss = {
                            showAiSheet = false
                            viewModel.resetAiState()
                        },
                        onExecuteCommand = viewModel::executeAiOrVoiceCommand,
                        onStartListening = viewModel::startVoiceControlListening,
                        onStopListening = viewModel::stopVoiceControlListening
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun CallOverlaysHost(
    viewModel: DotDialViewModel,
    settings: DialerSettings
) {
    val callSession by viewModel.callSession.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()

    val isFullCallVisible = callSession.lifecycle != CallLifecycleState.IDLE &&
        callSession.displayMode == CallDisplayMode.FULL_SCREEN
    val isCompactIncomingBannerVisible = callSession.lifecycle == CallLifecycleState.INCOMING_RINGING &&
        callSession.displayMode == CallDisplayMode.COMPACT_BANNER
    val isStatusBarPillVisible =
        (callSession.lifecycle == CallLifecycleState.ACTIVE &&
            callSession.displayMode == CallDisplayMode.STATUS_BAR_PILL) ||
            (callSession.lifecycle == CallLifecycleState.INCOMING_RINGING &&
                callSession.displayMode == CallDisplayMode.RINGING_MUTED_STATUS)

    Box(modifier = Modifier.fillMaxSize()) {
        // Compact Top Floating Incoming Popup (swipe up to mute, accept -> status bar pill)
        AnimatedVisibility(
            visible = isCompactIncomingBannerVisible,
            enter = fadeIn(tween(180)) + slideInVertically(
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
                initialOffsetY = { -it }
            ),
            exit = fadeOut(tween(160)) + slideOutVertically(
                animationSpec = tween(180),
                targetOffsetY = { -it }
            )
        ) {
            IncomingFloatingTopBanner(
                session = callSession,
                aiAssistantEnabled = settings.aiAssistantEnabled,
                onAnswerToStatusBar = { viewModel.answerActiveCall(toStatusBar = true) },
                onReject = viewModel::endActiveCall,
                onSwipeUpMute = viewModel::swipeUpMuteIncomingCall,
                onExpandFullScreen = viewModel::openFullCallScreen,
                onStartAiAssistant = viewModel::startAiAssistantForCall,
                onCancelAiCountdown = viewModel::cancelAiAutoAnswerCountdown
            )
        }

        // Status Bar Call Pill (for accepted call timer OR muted incoming icon until ring stops)
        AnimatedVisibility(
            visible = isStatusBarPillVisible,
            enter = fadeIn(tween(180)) + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut(tween(150)) + slideOutVertically(targetOffsetY = { -it })
        ) {
            StatusBarCallPill(
                session = callSession,
                onExpandFullScreen = viewModel::openFullCallScreen,
                onToggleSpeaker = viewModel::toggleSpeaker,
                onEndCall = viewModel::endActiveCall
            )
        }

        // Full-screen Active / Incoming Caller Studio Overlay
        BackHandler(
            enabled = isFullCallVisible && callSession.lifecycle == CallLifecycleState.ACTIVE
        ) {
            viewModel.collapseCallToStatusBar()
        }
        AnimatedVisibility(
            visible = isFullCallVisible,
            enter = fadeIn(tween(200)) + slideInVertically(
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
                initialOffsetY = { it / 5 }
            ),
            exit = fadeOut(tween(180)) + slideOutVertically(targetOffsetY = { it / 5 })
        ) {
            ActiveCallOverlay(
                session = callSession,
                voiceState = voiceState,
                redTintStrength = settings.videoRedTintStrength,
                voiceControlEnabled = settings.voiceCommandControlEnabled,
                silentRecording = settings.silentCallRecording,
                onAnswerCall = { viewModel.answerActiveCall(toStatusBar = false) },
                onEndCall = viewModel::endActiveCall,
                onCollapseToStatusBar = viewModel::collapseCallToStatusBar,
                onToggleMute = viewModel::toggleMute,
                onCycleAudioRoute = viewModel::cycleCallAudioRoute,
                onToggleRecord = viewModel::toggleCallRecording,
                onSimulateVoicePhrase = viewModel::executeAiOrVoiceCommand,
                onUpdateNotes = viewModel::updateCallNotes,
                onSendDtmfDigit = viewModel::sendInCallDtmf,
                onClearDtmfDigits = viewModel::clearInCallDtmf,
                onStartAiAssistantNow = viewModel::startAiAssistantForCall,
                onCancelAiCountdown = viewModel::cancelAiAutoAnswerCountdown,
                onTakeOverFromAi = viewModel::takeOverFromAiAssistant,
                onSendSimulatedCallerSpeech = viewModel::sendSimulatedCallerReplyToAi,
                aiAssistantEnabled = settings.aiAssistantEnabled,
                voiceChangerMode = settings.voiceChangerMode,
                selectedVoicePreset = settings.voiceChangerPreset,
                onSelectVoicePreset = viewModel::setVoiceChangerPreset,
                onToggleIncomingRingMute = viewModel::toggleIncomingRingMuteInPlace
            )
        }
    }
}

private fun navigateTab(navController: NavHostController, route: AppRoute) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun FloatingCompactBottomBar(
    tabs: List<NavTabItem>,
    currentDestination: NavDestination?,
    onTabSelected: (AppRoute) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val selected = currentDestination?.hasRoute(tab.route::class) == true
                    val pillColor by animateColorAsState(
                        targetValue = if (selected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        } else {
                            Color.Transparent
                        },
                        animationSpec = tween(180),
                        label = "nav_pill_color"
                    )
                    val contentTint by animateColorAsState(
                        targetValue = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        animationSpec = tween(180),
                        label = "nav_content_tint"
                    )

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onTabSelected(tab.route) }
                            .testTag(tab.tag),
                        shape = RoundedCornerShape(20.dp),
                        color = pillColor
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = stringResource(tab.labelRes),
                                tint = contentTint,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(tab.labelRes),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                color = contentTint,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
