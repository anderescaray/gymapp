package com.kova.app.ui.navigation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kova.app.ui.components.BottomTab
import com.kova.app.ui.components.DynamicIslandRestTimer
import com.kova.app.ui.components.FloatingNavBar
import com.kova.app.ui.screens.ActiveWorkoutScreen
import com.kova.app.ui.screens.CalendarScreen
import com.kova.app.ui.screens.ExerciseDetailScreen
import com.kova.app.ui.screens.OnboardingScreen
import com.kova.app.ui.screens.RoutineDetailScreen
import com.kova.app.ui.screens.RoutinesScreen
import com.kova.app.ui.screens.SettingsScreen
import com.kova.app.ui.screens.TodayScreen
import com.kova.app.ui.screens.WorkoutSummarySheet
import com.kova.app.ui.state.KovaViewModel
import com.kova.app.ui.theme.KovaTheme

private object Routes {
    const val TODAY = "today"
    const val ROUTINES = "routines"
    const val CALENDAR = "calendar"
    const val SETTINGS = "settings"
    const val ROUTINE_DETAIL = "routine/{routineId}"
    const val EXERCISE_DETAIL = "exercise/{exerciseId}"
    const val ACTIVE_WORKOUT = "active_workout"

    fun routineDetail(id: Long) = "routine/$id"
    fun exerciseDetail(id: Long) = "exercise/$id"
}

@Composable
fun KovaAppRoot(
    viewModel: KovaViewModel,
    canDrawOverlays: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestExportJson: () -> Unit,
    onRequestImportJson: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val restTimerState by viewModel.restTimerState.collectAsStateWithLifecycle()
    val finishedSummary by viewModel.finishedSummary.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val livePrExerciseIds by viewModel.livePrExerciseIds.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.consumeMessage()
    }

    val colors = KovaTheme.colors

    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.canvas)
        )
        return
    }

    if (!uiState.preferences.hasCompletedOnboarding) {
        Box(modifier = Modifier.fillMaxSize()) {
            OnboardingScreen(
                initialWeeklyGoal = uiState.preferences.weeklyGoalDays,
                accentColor = androidx.compose.ui.graphics.Color(uiState.preferences.accentPalette.previewHexDark),
                routines = uiState.routines,
                onCreateRoutineDay = { name, color, target ->
                    viewModel.createRoutineDay(name, color, target)
                },
                onUpdateRoutineTarget = { wrapper, newTarget ->
                    viewModel.updateRoutineDay(wrapper.routine.copy(targetExerciseCount = newTarget))
                },
                onArchiveRoutineDay = { viewModel.archiveRoutineDay(it) },
                onCreateExercise = { routineId, name, mandatory ->
                    viewModel.createExercise(routineId, name, mandatory)
                },
                onArchiveExercise = { viewModel.archiveExercise(it) },
                onFinishOnboarding = { goal -> viewModel.completeOnboarding(goal) }
            )
            KovaSnackbarOverlay(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 86.dp, start = 16.dp, end = 16.dp)
            )
        }
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in setOf(Routes.TODAY, Routes.ROUTINES, Routes.CALENDAR, Routes.SETTINGS)
    val islandTopOffset by animateDpAsState(
        targetValue = if (restTimerState.isVisible) 62.dp else 0.dp,
        label = "islandTopOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.TODAY,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
            modifier = Modifier
                .fillMaxSize()
                .padding(top = islandTopOffset)
        ) {
            composable(Routes.TODAY) {
                TodayScreen(
                    uiState = uiState,
                    onStartWorkout = { routineId ->
                        viewModel.startWorkout(routineId) {
                            navController.navigate(Routes.ACTIVE_WORKOUT)
                        }
                    },
                    onResumeActiveWorkout = {
                        navController.navigate(Routes.ACTIVE_WORKOUT)
                    },
                    onOpenRoutines = {
                        navController.navigate(Routes.ROUTINES) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenCalendar = {
                        navController.navigate(Routes.CALENDAR) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenRoutineDetail = { id ->
                        navController.navigate(Routes.routineDetail(id))
                    }
                )
            }

            composable(Routes.ROUTINES) {
                RoutinesScreen(
                    routines = uiState.routines,
                    hasActiveSession = uiState.activeSession != null,
                    onCreateRoutineDay = { name, color, target ->
                        viewModel.createRoutineDay(name, color, target)
                    },
                    onOpenRoutineDetail = { id ->
                        navController.navigate(Routes.routineDetail(id))
                    },
                    onStartWorkout = { id ->
                        viewModel.startWorkout(id) {
                            navController.navigate(Routes.ACTIVE_WORKOUT)
                        }
                    }
                )
            }

            composable(
                route = Routes.ROUTINE_DETAIL,
                arguments = listOf(navArgument("routineId") { type = NavType.LongType })
            ) { entry ->
                val routineId = entry.arguments?.getLong("routineId") ?: -1L
                val wrapper = uiState.routines.firstOrNull { it.routine.id == routineId }
                val analyticsMap = remember(wrapper, uiState.allSessions, uiState.allSets, uiState.preferences.weightUnit) {
                    wrapper?.activeExercises?.associate { ex ->
                        ex.id to viewModel.getExerciseAnalytics(ex.id)
                    }.orEmpty()
                }
                RoutineDetailScreen(
                    wrapper = wrapper,
                    defaultRestSeconds = uiState.preferences.defaultRestSeconds,
                    hasActiveSession = uiState.activeSession != null,
                    isActiveSessionForThisRoutine = uiState.activeSession?.session?.routineDayId == routineId,
                    analyticsByExerciseId = analyticsMap,
                    onBack = { navController.popBackStack() },
                    onStartWorkout = { id ->
                        viewModel.startWorkout(id) {
                            navController.navigate(Routes.ACTIVE_WORKOUT)
                        }
                    },
                    onUpdateRoutine = { viewModel.updateRoutineDay(it) },
                    onDeleteRoutine = { viewModel.archiveRoutineDay(it) },
                    onCreateExercise = { rId, name, mandatory, rest ->
                        viewModel.createExercise(rId, name, mandatory, rest)
                    },
                    onUpdateExercise = { viewModel.updateExercise(it) },
                    onDeleteExercise = { viewModel.archiveExercise(it) },
                    onOpenExerciseDetail = { exId ->
                        navController.navigate(Routes.exerciseDetail(exId))
                    }
                )
            }

            composable(
                route = Routes.EXERCISE_DETAIL,
                arguments = listOf(navArgument("exerciseId") { type = NavType.LongType })
            ) { entry ->
                val exerciseId = entry.arguments?.getLong("exerciseId") ?: -1L
                val wrapper = uiState.routines.firstOrNull { r -> r.exercises.any { it.id == exerciseId } }
                val exercise = wrapper?.exercises?.firstOrNull { it.id == exerciseId }
                val analytics = remember(
                    exerciseId,
                    exercise?.isBodyweight,
                    uiState.allSessions,
                    uiState.allSets,
                    uiState.preferences.weightUnit
                ) {
                    viewModel.getExerciseAnalytics(exerciseId)
                }
                ExerciseDetailScreen(
                    exercise = exercise,
                    routineDay = wrapper?.routine,
                    analytics = analytics,
                    unit = uiState.preferences.weightUnit,
                    defaultRestSeconds = uiState.preferences.defaultRestSeconds,
                    onBack = { navController.popBackStack() },
                    onUpdateExercise = { viewModel.updateExercise(it) },
                    onUpdateManualOneRm = { id, oneRmKg -> viewModel.updateExerciseManualOneRm(id, oneRmKg) }
                )
            }

            composable(Routes.ACTIVE_WORKOUT) {
                val active = uiState.activeSession
                val routineWrapper = uiState.routines.firstOrNull { it.routine.id == active?.session?.routineDayId }
                ActiveWorkoutScreen(
                    activeSession = active,
                    routineWrapper = routineWrapper,
                    unit = uiState.preferences.weightUnit,
                    defaultRestSeconds = uiState.preferences.defaultRestSeconds,
                    canDrawOverlays = canDrawOverlays,
                    livePrExerciseIds = livePrExerciseIds,
                    lastSessionSetsProvider = { exId ->
                        viewModel.getLastSessionSetsForExercise(exId, excludeSessionId = active?.session?.id)
                    },
                    onBack = { navController.popBackStack() },
                    onAddSet = { sId, ex, kg, reps -> viewModel.addSet(sId, ex, kg, reps) },
                    onCopyLastSession = { sId, ex -> viewModel.copyLastSessionSets(sId, ex) },
                    onUpdateSet = { set, ex, justDone -> viewModel.updateSet(set, ex, justDone) },
                    onDeleteSet = { viewModel.deleteSet(it) },
                    onCreateExerciseInRoutine = { rId, name, mandatory, rest ->
                        viewModel.createExercise(rId, name, mandatory, rest)
                    },
                    onStartManualTimer = { sec, label -> viewModel.restTimerManager.start(sec, label) },
                    onRequestOverlayPermission = onRequestOverlayPermission,
                    onOpenExerciseAnalytics = { exId ->
                        navController.navigate(Routes.exerciseDetail(exId))
                    },
                    onFinishSession = {
                        viewModel.finishActiveSession {
                            navController.popBackStack(Routes.TODAY, inclusive = false)
                        }
                    },
                    onDiscardSession = {
                        viewModel.discardActiveSession {
                            navController.popBackStack()
                        }
                    }
                )
            }

            composable(Routes.CALENDAR) {
                CalendarScreen(
                    uiState = uiState,
                    setsForSessionProvider = { sId -> viewModel.getSetsForSession(sId) },
                    onUpdateWeeklyGoal = { viewModel.updateWeeklyGoal(it) },
                    onDeleteSession = { viewModel.deleteFinishedSession(it) }
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    preferences = uiState.preferences,
                    canDrawOverlays = canDrawOverlays,
                    onUpdateWeeklyGoal = { viewModel.updateWeeklyGoal(it) },
                    onUpdateWeightUnit = { viewModel.updateWeightUnit(it) },
                    onUpdateThemeMode = { viewModel.updateThemeMode(it) },
                    onUpdateAccentPalette = { viewModel.updateAccentPalette(it) },
                    onUpdateDefaultRestSeconds = { viewModel.updateDefaultRestSeconds(it) },
                    onUpdateAutoStartRestTimer = { viewModel.updateAutoStartRestTimer(it) },
                    onUpdateSoundAndHaptics = { viewModel.updateSoundAndHaptics(it) },
                    onRequestOverlayPermission = onRequestOverlayPermission,
                    onExportBackup = onRequestExportJson,
                    onImportBackup = onRequestImportJson,
                    onGoogleDriveClick = { viewModel.testGoogleDrivePreparation() },
                    onReopenOnboarding = { viewModel.reopenOnboarding() },
                    onClearAllData = { viewModel.clearAllData() }
                )
            }
        }

        // Global Dynamic Island Rest Timer Overlay
        DynamicIslandRestTimer(
            state = restTimerState,
            accentColor = androidx.compose.ui.graphics.Color(uiState.preferences.accentPalette.previewHexDark),
            onAdjust = { viewModel.restTimerManager.adjust(it) },
            onSelectPreset = { viewModel.restTimerManager.selectPreset(it) },
            onTogglePause = { viewModel.restTimerManager.togglePause() },
            onToggleExpand = { viewModel.restTimerManager.toggleExpanded() },
            onDismiss = { viewModel.restTimerManager.dismiss() },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Signature Floating Navigation Pill
        if (showBottomBar) {
            FloatingNavBar(
                currentRoute = currentRoute,
                onNavigate = { tab: BottomTab ->
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        KovaSnackbarOverlay(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (showBottomBar) 92.dp else 20.dp, start = 18.dp, end = 18.dp)
        )
    }

    finishedSummary?.let { summary ->
        WorkoutSummarySheet(
            summary = summary,
            unit = uiState.preferences.weightUnit,
            onDismiss = { viewModel.dismissFinishedSummary() }
        )
    }
}

@Composable
private fun KovaSnackbarOverlay(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val colors = KovaTheme.colors
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
    ) { data ->
        Snackbar(
            snackbarData = data,
            shape = RoundedCornerShape(18.dp),
            containerColor = colors.islandBg,
            contentColor = colors.islandInk,
            actionColor = colors.accent
        )
    }
}
