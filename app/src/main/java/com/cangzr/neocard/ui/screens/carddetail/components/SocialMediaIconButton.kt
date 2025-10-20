package com.cangzr.neocard.ui.screens.carddetail.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import com.cangzr.neocard.R

@Composable
fun SocialMediaIconButton(iconRes: Int, contentDescription: String, url: String) {
    val context = LocalContext.current
    
    IconButton(onClick = { 
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.link_open_error, url), Toast.LENGTH_SHORT).show()
        }
    }) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
    }
}
