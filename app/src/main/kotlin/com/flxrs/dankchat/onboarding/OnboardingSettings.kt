package com.flxrs.dankchat.onboarding

import kotlinx.serialization.Serializable

@Serializable
data class OnboardingSettings(
    val hasCompletedOnboarding: Boolean = false,
    val featureTourVersion: Int = 0,
    val featureTourStep: Int = 0,
    val hasShownAddChannelHint: Boolean = false,
    val hasShownToolbarHint: Boolean = false,
    val onboardingPage: Int = 0,
)
