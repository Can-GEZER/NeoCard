package com.cangzr.neocard.ui.screens.home.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned

fun Modifier.onAppear(callback: () -> Unit): Modifier {
    return this.then(
        Modifier.onGloballyPositioned {
            callback()
        }
    )
}
