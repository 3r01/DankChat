package com.flxrs.dankchat.tour

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.flxrs.dankchat.onboarding.OnboardingDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val CURRENT_TOUR_VERSION = 1

enum class TourStep {
    InputActions,
    OverflowMenu,
    ConfigureActions,
    SwipeGesture,
    RecoveryFab,
}

@OptIn(ExperimentalMaterial3Api::class)
@Stable
class FeatureTourController(
    private val onboardingDataStore: OnboardingDataStore,
    private val scope: CoroutineScope,
) {
    var isActive by mutableStateOf(false)
        private set

    /** Set synchronously on completion to prevent restart from stale datastore reads. */
    private var hasCompleted = false

    var currentStepIndex by mutableIntStateOf(0)
        private set

    val currentStep: TourStep?
        get() = when {
            !isActive                                 -> null
            currentStepIndex >= TourStep.entries.size -> null
            else                                      -> TourStep.entries[currentStepIndex]
        }

    /** When true, ChatInputLayout should force the overflow menu open. */
    var forceOverflowOpen by mutableStateOf(false)
        private set

    /** Set by MainScreen to hide input for the RecoveryFab step. */
    var onHideInput: (() -> Unit)? = null

    /** Set by MainScreen to restore input when the tour completes. */
    var onRestoreInput: (() -> Unit)? = null

    /** Called when the tour finishes (either completed or skipped). */
    var onComplete: (() -> Unit)? = null

    val inputActionsTooltipState = TooltipState(isPersistent = true)
    val overflowMenuTooltipState = TooltipState(isPersistent = true)
    val configureActionsTooltipState = TooltipState(isPersistent = true)
    val swipeGestureTooltipState = TooltipState(isPersistent = true)
    val recoveryFabTooltipState = TooltipState(isPersistent = true)

    fun start() {
        if (isActive || hasCompleted) return
        isActive = true
        val settings = onboardingDataStore.current()
        // Only resume persisted step if it belongs to the current tour (gap == 1).
        // A larger gap means a prior tour was never completed and the step index is stale.
        currentStepIndex = when {
            CURRENT_TOUR_VERSION - settings.featureTourVersion == 1 -> settings.featureTourStep.coerceIn(0, TourStep.entries.size - 1)
            else                                                    -> 0
        }
        applyStepSideEffects()
        showCurrentTooltip()
    }

    fun advance() {
        val leavingStep = currentStep
        // Skip dismiss for ConfigureActions — its tooltip is inside the menu popup,
        // so removing the composable (via step change) handles cleanup.
        // Explicit dismiss() causes a popup exit animation that flashes.
        if (leavingStep != TourStep.ConfigureActions) {
            dismissCurrentTooltip()
        }
        currentStepIndex++
        val nextStep = currentStep
        when {
            nextStep == null -> {
                completeTour()
                return
            }

            else             -> {
                onboardingDataStore.updateAsync { it.copy(featureTourStep = currentStepIndex) }
                applyStepSideEffects()
            }
        }
        if (leavingStep == TourStep.ConfigureActions) {
            // Menu close animation takes ~150ms; show the next tooltip after it finishes
            scope.launch {
                delay(250)
                showCurrentTooltip()
            }
        } else {
            showCurrentTooltip()
        }
    }

    private fun applyStepSideEffects() {
        when (currentStep) {
            TourStep.ConfigureActions -> forceOverflowOpen = true
            TourStep.SwipeGesture     -> forceOverflowOpen = false
            TourStep.RecoveryFab      -> onHideInput?.invoke()
            else                      -> {}
        }
    }

    fun skipTour() {
        dismissCurrentTooltip()
        forceOverflowOpen = false
        completeTour()
    }

    private fun completeTour() {
        isActive = false
        hasCompleted = true
        onRestoreInput?.invoke()
        onComplete?.invoke()
        onboardingDataStore.updateAsync { it.copy(featureTourVersion = CURRENT_TOUR_VERSION, featureTourStep = 0) }
    }

    private fun showCurrentTooltip() {
        val state = tooltipStateForStep(currentStep ?: return)
        scope.launch { state.show() }
    }

    private fun dismissCurrentTooltip() {
        val step = currentStep ?: return
        tooltipStateForStep(step).dismiss()
    }

    private fun tooltipStateForStep(step: TourStep): TooltipState = when (step) {
        TourStep.InputActions     -> inputActionsTooltipState
        TourStep.OverflowMenu     -> overflowMenuTooltipState
        TourStep.ConfigureActions -> configureActionsTooltipState
        TourStep.SwipeGesture     -> swipeGestureTooltipState
        TourStep.RecoveryFab      -> recoveryFabTooltipState
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberFeatureTourController(
    onboardingDataStore: OnboardingDataStore,
): FeatureTourController {
    val scope = rememberCoroutineScope()
    return remember(onboardingDataStore) {
        FeatureTourController(
            onboardingDataStore = onboardingDataStore,
            scope = scope,
        )
    }
}
