package com.cangzr.neocard.ui.screens.createcard.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.cangzr.neocard.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object CardCreationUtils {
    
    fun Int.toHexColor(): String {
        return String.format("#%06X", 0xFFFFFF and this)
    }
    
    fun uriToBitmap(uri: Uri, context: Context): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun isUserPremium(): Boolean {
        val currentUser = FirebaseAuth.getInstance().currentUser
        return if (currentUser != null) {
            val userDocRef = FirebaseFirestore.getInstance().collection("users").document(currentUser.uid)
            val document = userDocRef.get().await()
            document.getBoolean("premium") ?: false
        } else {
            false
        }
    }
    
    fun getPredefinedGradients(context: Context) = listOf(
        Pair(
            context.getString(R.string.gradient_sunset),
            Brush.horizontalGradient(listOf(Color(0xFFFE6B8B), Color(0xFFFF8E53)))
        ),
        Pair(
            context.getString(R.string.gradient_ocean),
            Brush.horizontalGradient(listOf(Color(0xFF2196F3), Color(0xFF00BCD4)))
        ),
        Pair(
            context.getString(R.string.gradient_forest),
            Brush.horizontalGradient(listOf(Color(0xFF4CAF50), Color(0xFF8BC34A)))
        ),
        Pair(
            context.getString(R.string.gradient_night),
            Brush.verticalGradient(listOf(Color(0xFF2C3E50), Color(0xFF3498DB)))
        ),
        Pair(
            context.getString(R.string.gradient_purple_mist),
            Brush.verticalGradient(listOf(Color(0xFF9C27B0), Color(0xFFE91E63)))
        )
    )
}
