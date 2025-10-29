package com.cangzr.neocard.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cangzr.neocard.common.ErrorMapper
import com.cangzr.neocard.common.Resource

/**
 * Displays an error message with retry option
 * 
 * @param error The Resource.Error to display
 * @param onRetry Callback when retry button is clicked
 * @param modifier Modifier for the composable
 */
@Composable
fun ErrorDisplay(
    error: Resource.Error,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    ErrorDisplay(
        title = ErrorMapper.getErrorTitle(error.exception),
        message = error.userMessage,
        showRetry = ErrorMapper.isRetryableError(error.exception),
        onRetry = onRetry,
        modifier = modifier
    )
}

/**
 * Displays an error message with optional retry button
 * 
 * @param title Error title
 * @param message Error message
 * @param showRetry Whether to show retry button
 * @param onRetry Callback when retry button is clicked
 * @param modifier Modifier for the composable
 */
@Composable
fun ErrorDisplay(
    title: String,
    message: String,
    showRetry: Boolean = true,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            
            if (showRetry) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tekrar Dene")
                }
            }
        }
    }
}

/**
 * Shows a customized SnackbarHost for error messages
 * 
 * @param snackbarHostState The SnackbarHostState to show the message
 */
@Composable
fun ErrorSnackbarHost(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier
    ) { data ->
        Snackbar(
            snackbarData = data,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            actionColor = MaterialTheme.colorScheme.error
        )
    }
}

/**
 * Shows an error in an AlertDialog
 * 
 * @param error The Resource.Error to display
 * @param onDismiss Callback when dialog is dismissed
 * @param onRetry Optional callback when retry is clicked
 */
@Composable
fun ErrorAlertDialog(
    error: Resource.Error,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = ErrorMapper.getErrorTitle(error.exception),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = error.userMessage,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            if (onRetry != null && ErrorMapper.isRetryableError(error.exception)) {
                TextButton(onClick = {
                    onDismiss()
                    onRetry()
                }) {
                    Text("Tekrar Dene")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Tamam")
                }
            }
        },
        dismissButton = if (onRetry != null && ErrorMapper.isRetryableError(error.exception)) {
            {
                TextButton(onClick = onDismiss) {
                    Text("İptal")
                }
            }
        } else null
    )
}

/**
 * Inline error message (for form fields, etc.)
 * 
 * @param error The Resource.Error to display
 * @param modifier Modifier for the composable
 */
@Composable
fun InlineErrorMessage(
    error: Resource.Error,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Error",
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = error.userMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

