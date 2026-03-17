package com.flxrs.dankchat.main

import androidx.compose.runtime.Stable

@Stable
sealed interface InputState {
    object Default : InputState
    object Replying : InputState
    object Whispering : InputState
    object NotLoggedIn : InputState
    object Disconnected : InputState
}
