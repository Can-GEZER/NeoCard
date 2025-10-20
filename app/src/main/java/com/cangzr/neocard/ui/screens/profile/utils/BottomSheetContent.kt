package com.cangzr.neocard.ui.screens.profile.utils

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cangzr.neocard.R
import androidx.compose.material3.rememberModalBottomSheetState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetContent(selectedOption: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        // LazyColumn ile kaydırılabilir içerik
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            item {
                when (selectedOption) {
                    context.getString(R.string.privacy_policy) -> {
                        Text(context.getString(R.string.privacy_policy_title), style = MaterialTheme.typography.headlineSmall)
                        Text(context.getString(R.string.privacy_policy_content))
                    }
                    context.getString(R.string.about) -> {
                        Text(context.getString(R.string.about_app_title), style = MaterialTheme.typography.headlineSmall)
                        Text(context.getString(R.string.about_app_content))
                    }
                    context.getString(R.string.help) -> {
                        Text(context.getString(R.string.help_support_title), style = MaterialTheme.typography.headlineSmall)
                        Text(context.getString(R.string.help_support_content))
                    }
                }
            }
        }
    }
}
