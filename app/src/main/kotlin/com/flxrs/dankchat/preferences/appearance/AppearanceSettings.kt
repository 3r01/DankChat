package com.flxrs.dankchat.preferences.appearance

import kotlinx.serialization.Serializable

@Serializable
enum class InputAction {
    Search, LastMessage, Stream, RoomState, Fullscreen, HideInput
}

@Serializable
data class AppearanceSettings(
    val theme: ThemePreference = ThemePreference.System,
    val trueDarkTheme: Boolean = false,
    val fontSize: Int = 14,
    val keepScreenOn: Boolean = true,
    val lineSeparator: Boolean = false,
    val checkeredMessages: Boolean = false,
    val showInput: Boolean = true,
    val autoDisableInput: Boolean = true,
    val showChips: Boolean = true,
    val showChangelogs: Boolean = true,
    val inputActions: List<InputAction> = listOf(
        InputAction.Stream, InputAction.RoomState,
        InputAction.Search, InputAction.LastMessage,
    ),
)

enum class ThemePreference { System, Dark, Light }
