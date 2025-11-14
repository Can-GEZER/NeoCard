package com.cangzr.neocard.ui.screens.profile.components

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cangzr.neocard.R
import androidx.hilt.navigation.compose.hiltViewModel
import com.cangzr.neocard.ui.screens.profile.viewmodels.ReferralViewModel

@Composable
fun ReferralCard(
    viewModel: ReferralViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val referralCode by viewModel.referralCode.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val referralCount by viewModel.referralCount.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadReferralCode()
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = !isLoading && referralCode != null
            ) {
                referralCode?.let { code ->
                    val inviteLink = "https://neocardapp.com/invite/$code"
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.invite_message, inviteLink))
                        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.invite_friend))
                    }
                    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share)))
                }
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = context.getString(R.string.invite_friend),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (referralCount > 0) {
                            context.getString(R.string.referral_count, referralCount.toInt())
                        } else {
                            context.getString(R.string.invite_friend_description)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

