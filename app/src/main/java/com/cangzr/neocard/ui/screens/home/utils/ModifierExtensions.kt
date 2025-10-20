package com.cangzr.neocard.ui.screens.home.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned

// onAppear modifier fonksiyonu
fun Modifier.onAppear(callback: () -> Unit): Modifier {
    return this.then(
        Modifier.onGloballyPositioned {
            callback()
        }
    )
}
