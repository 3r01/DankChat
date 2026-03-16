package com.flxrs.dankchat.chat.compose

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

@Immutable
sealed interface TextResource {
    @Immutable
    data class Plain(val value: String) : TextResource

    @Immutable
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : TextResource

    @Immutable
    data class PluralRes(@PluralsRes val id: Int, val quantity: Int, val args: List<Any> = emptyList()) : TextResource
}

@Composable
fun TextResource.resolve(): String = when (this) {
    is TextResource.Plain -> value
    is TextResource.Res -> {
        val resolvedArgs = args.map { arg ->
            when (arg) {
                is TextResource -> arg.resolve()
                else -> arg
            }
        }
        stringResource(id, *resolvedArgs.toTypedArray())
    }

    is TextResource.PluralRes -> {
        val resolvedArgs = args.map { arg ->
            when (arg) {
                is TextResource -> arg.resolve()
                else -> arg
            }
        }
        pluralStringResource(id, quantity, *resolvedArgs.toTypedArray())
    }
}
