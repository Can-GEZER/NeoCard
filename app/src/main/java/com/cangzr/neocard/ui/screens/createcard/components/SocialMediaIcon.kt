package com.cangzr.neocard.ui.screens.createcard.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp

@Composable
fun SocialMediaIcon(iconRes: Int, contentDescription: String, tint: Color) {
    Icon(
        painter = painterResource(id = iconRes),
        contentDescription = contentDescription,
        modifier = Modifier.size(24.dp),
        tint = tint
    )
}
