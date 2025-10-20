package com.cangzr.neocard.ui.screens.carddetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cangzr.neocard.R

@Composable
fun InfoEditColumn(
    name: String,
    surname: String,
    title: String,
    company: String,
    phone: String,
    email: String,
    website: String,
    linkedin: String,
    github: String,
    twitter: String,
    instagram: String,
    facebook: String,
    nameError: String?,
    surnameError: String?,
    phoneError: String?,
    emailError: String?,
    onNameChange: (String) -> Unit,
    onSurnameChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onCompanyChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onWebsiteChange: (String) -> Unit,
    onLinkedinChange: (String) -> Unit,
    onGithubChange: (String) -> Unit,
    onTwitterChange: (String) -> Unit,
    onInstagramChange: (String) -> Unit,
    onFacebookChange: (String) -> Unit,
    bio: String = "",
    cv: String = "",
    onBioChange: (String) -> Unit = {},
    onCvChange: (String) -> Unit = {},
    isPremium: Boolean = false
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FormCard(context.getString(R.string.personal_info)) {
            OutlinedTextField(
                value = name, 
                onValueChange = onNameChange, 
                label = { Text(context.getString(R.string.name)) },
                isError = nameError != null,
                supportingText = { nameError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = surname, 
                onValueChange = onSurnameChange, 
                label = { Text(context.getString(R.string.surname)) },
                isError = surnameError != null,
                supportingText = { surnameError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            if (isPremium) {
                OutlinedTextField(
                    value = bio, 
                    onValueChange = onBioChange, 
                    label = { Text(context.getString(R.string.bio)) },
                    placeholder = { Text(context.getString(R.string.biography_placeholder)) },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    maxLines = 6
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.premium),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = context.getString(R.string.biography_premium_only),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        FormCard(context.getString(R.string.work_info)) {
            OutlinedTextField(
                value = title, 
                onValueChange = onTitleChange, 
                label = { Text(context.getString(R.string.title)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = company, 
                onValueChange = onCompanyChange, 
                label = { Text(context.getString(R.string.company)) },
                modifier = Modifier.fillMaxWidth()
            )
            if (isPremium) {
                OutlinedTextField(
                    value = cv, 
                    onValueChange = onCvChange, 
                    label = { Text(context.getString(R.string.cv_link)) },
                    placeholder = { Text(context.getString(R.string.cv_placeholder)) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.premium),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = context.getString(R.string.cv_premium_only),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        FormCard(context.getString(R.string.contact_info)) {
            OutlinedTextField(
                value = phone, 
                onValueChange = onPhoneChange, 
                label = { Text(context.getString(R.string.phone)) },
                isError = phoneError != null,
                supportingText = { phoneError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = email, 
                onValueChange = onEmailChange, 
                label = { Text(context.getString(R.string.email)) },
                isError = emailError != null,
                supportingText = { emailError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = website, 
                onValueChange = onWebsiteChange, 
                label = { Text(context.getString(R.string.website)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        FormCard(context.getString(R.string.social_media)) {
            OutlinedTextField(
                value = linkedin, 
                onValueChange = onLinkedinChange, 
                label = { Text(context.getString(R.string.linkedin)) },
                placeholder = { Text(context.getString(R.string.username_or_url)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = github, 
                onValueChange = onGithubChange, 
                label = { Text(context.getString(R.string.github)) },
                placeholder = { Text(context.getString(R.string.username_or_url)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = twitter, 
                onValueChange = onTwitterChange, 
                label = { Text(context.getString(R.string.twitter)) },
                placeholder = { Text(context.getString(R.string.username_or_url)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = instagram, 
                onValueChange = onInstagramChange, 
                label = { Text(context.getString(R.string.instagram)) },
                placeholder = { Text(context.getString(R.string.username_or_url)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = facebook, 
                onValueChange = onFacebookChange, 
                label = { Text(context.getString(R.string.facebook)) },
                placeholder = { Text(context.getString(R.string.username_or_url)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
