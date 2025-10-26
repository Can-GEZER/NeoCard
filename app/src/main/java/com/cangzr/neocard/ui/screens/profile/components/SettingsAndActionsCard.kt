package com.cangzr.neocard.ui.screens.profile.components

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.cangzr.neocard.R
import com.cangzr.neocard.utils.LanguageManager
import com.cangzr.neocard.utils.UserUtils
import com.cangzr.neocard.ui.screens.profile.utils.BottomSheetContent
import com.cangzr.neocard.ui.screens.profile.utils.DeleteAccountDialog

@Composable
fun SettingsAndActionsCard(
    navController: NavHostController,
    isSpecialUser: Boolean
) {
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // Get current language
    val currentLanguage = remember { LanguageManager.getSelectedLanguage(context) }
    var selectedLanguage by remember { mutableStateOf(currentLanguage) }

    selectedOption?.let {
        BottomSheetContent(it) {
            selectedOption = null
        }
    }

    if (showDeleteDialog) {
        DeleteAccountDialog(
            showDialog = true,
            onDismiss = { showDeleteDialog = false },
            onDeleteConfirmed = {
                showDeleteDialog = false
                UserUtils.deleteAccount(context) { success: Boolean, message: String ->
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    if (success) {
                        // Hesap silindikten sonra auth ekranına yönlendir
                        navController.navigate("auth") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            }
        )
    }
    
    // Language selection dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(text = context.getString(R.string.language)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Turkish option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedLanguage = "tr" }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = selectedLanguage == "tr",
                            onClick = { selectedLanguage = "tr" }
                        )
                        Text(text = context.getString(R.string.language_turkish))
                    }
                    
                    // English option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedLanguage = "en" }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = selectedLanguage == "en",
                            onClick = { selectedLanguage = "en" }
                        )
                        Text(text = context.getString(R.string.language_english))
                    }
                    
                    // System default option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedLanguage = "" }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = selectedLanguage == "",
                            onClick = { selectedLanguage = "" }
                        )
                        Text(text = context.getString(R.string.theme_system))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Apply language change
                        LanguageManager.setLanguage(context, selectedLanguage)
                        showLanguageDialog = false
                        
                        // Restart the activity to apply changes
                        val activity = context as? Activity
                        activity?.let {
                            val intent = it.intent
                            it.finish()
                            it.startActivity(intent)
                        }
                    }
                ) {
                    Text(text = context.getString(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(text = context.getString(R.string.cancel))
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Language settings
            ActionButton(
                iconRes = R.drawable.settings,
                title = context.getString(R.string.language),
                onClick = { showLanguageDialog = true },
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            listOf(
                Triple(R.drawable.privacy, context.getString(R.string.privacy_policy), { selectedOption = context.getString(R.string.privacy_policy) }),
                Triple(R.drawable.info, context.getString(R.string.about), { selectedOption = context.getString(R.string.about) }),
                Triple(R.drawable.help, context.getString(R.string.help), { selectedOption = context.getString(R.string.help) })
            ).forEach { (icon, title, onClick) ->
                ActionButton(
                    iconRes = icon,
                    title = title,
                    onClick = onClick,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            ActionButton(
                icon = Icons.Default.Delete,
                title = context.getString(R.string.delete),
                onClick = { showDeleteDialog = true },
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector? = null,
    iconRes: Int? = null,
    title: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                icon != null -> Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
                iconRes != null -> androidx.compose.foundation.Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(tint)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = tint
            )
        }
    }
}
