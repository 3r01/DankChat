package com.flxrs.dankchat.tour

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.flxrs.dankchat.onboarding.OnboardingDataStore

@Immutable
sealed interface PostOnboardingStep {
    /** Waiting for conditions (onboarding not done yet, or no channels). */
    data object Idle : PostOnboardingStep

    /** Show tooltip on the toolbar plus icon. */
    data object ToolbarPlusHint : PostOnboardingStep

    /** Run the feature tour. */
    data object FeatureTour : PostOnboardingStep

    /** Everything done. */
    data object Complete : PostOnboardingStep
}

@Stable
class PostOnboardingCoordinator(
    private val onboardingDataStore: OnboardingDataStore,
) {
    var step by mutableStateOf<PostOnboardingStep>(PostOnboardingStep.Idle)
        private set

    private var channelsReady = false
    private var isEmpty = true
    private var toolbarHintDone = false

    fun onChannelsChanged(empty: Boolean, ready: Boolean) {
        isEmpty = empty
        channelsReady = ready
        resolveStep()
    }

    /** User already used the toolbar + icon, no need to show the hint. */
    fun onAddedChannelFromToolbar() {
        if (toolbarHintDone) return
        toolbarHintDone = true
        onboardingDataStore.updateAsync { it.copy(hasShownToolbarHint = true) }
    }

    fun onToolbarHintDismissed() {
        if (toolbarHintDone) return // idempotent for external-dismiss handler
        toolbarHintDone = true
        onboardingDataStore.updateAsync { it.copy(hasShownToolbarHint = true) }
        val settings = onboardingDataStore.current()
        step = when {
            settings.featureTourVersion < CURRENT_TOUR_VERSION && !isEmpty -> PostOnboardingStep.FeatureTour
            else                                                           -> PostOnboardingStep.Complete
        }
    }

    fun onTourCompleted() {
        step = PostOnboardingStep.Complete
    }

    private fun resolveStep() {
        if (!channelsReady || step is PostOnboardingStep.Complete) return
        val settings = onboardingDataStore.current()
        val toolbarDone = settings.hasShownToolbarHint || toolbarHintDone

        step = when {
            !settings.hasCompletedOnboarding                   -> PostOnboardingStep.Idle
            isEmpty                                            -> PostOnboardingStep.Idle
            !toolbarDone                                       -> PostOnboardingStep.ToolbarPlusHint
            settings.featureTourVersion < CURRENT_TOUR_VERSION -> PostOnboardingStep.FeatureTour
            else                                               -> PostOnboardingStep.Complete
        }
    }
}

@Composable
fun rememberPostOnboardingCoordinator(onboardingDataStore: OnboardingDataStore): PostOnboardingCoordinator {
    return remember(onboardingDataStore) {
        PostOnboardingCoordinator(onboardingDataStore)
    }
}
