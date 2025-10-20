package com.cangzr.neocard.ui.screens.profile.utils

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.cangzr.neocard.R

@Composable
fun DeleteAccountDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onDeleteConfirmed: () -> Unit
) {
    if (showDialog) {
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(context.getString(R.string.delete_account_title)) },
            text = { Text(context.getString(R.string.delete_account_message)) },
            confirmButton = {
                TextButton(onClick = onDeleteConfirmed) {
                    Text(context.getString(R.string.yes_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(context.getString(R.string.give_up))
                }
            }
        )
    }
}
