package com.cangzr.neocard.ui.screens.carddetail.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.cangzr.neocard.R
import com.cangzr.neocard.data.CardType
import com.cangzr.neocard.ui.screens.carddetail.utils.UrlUtils

@Composable
fun InfoDisplayColumn(
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
    cardType: CardType,
    bio: String = "",
    cv: String = "",
    isPremium: Boolean = false
) {
    val context = LocalContext.current
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("$name $surname", style = MaterialTheme.typography.headlineMedium)
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(company, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))

        Spacer(modifier = Modifier.height(16.dp))
        
        if (bio.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = context.getString(R.string.bio),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        InfoItem(R.drawable.email, email) {
            try {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:$email")
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.email_app_error), Toast.LENGTH_SHORT).show()
            }
        }
        
        InfoItem(R.drawable.phone, phone) {
            try {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phone")
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.phone_app_error), Toast.LENGTH_SHORT).show()
            }
        }

        if (website.isNotEmpty()) {
            InfoItem(R.drawable.web, website) {
                try {
                    var webUrl = website
                    if (!webUrl.startsWith("http://") && !webUrl.startsWith("https://")) {
                        webUrl = "https://$webUrl"
                    }
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.website_open_error), Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        if (cv.isNotEmpty()) {
            InfoItem(R.drawable.document, context.getString(R.string.view_cv)) {
                try {
                    var cvUrl = cv
                    if (!cvUrl.startsWith("http://") && !cvUrl.startsWith("https://")) {
                        cvUrl = "https://$cvUrl"
                    }
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cvUrl))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.cv_open_error), Toast.LENGTH_SHORT).show()
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (linkedin.isNotEmpty()) SocialMediaIconButton(R.drawable.linkedin, "LinkedIn", UrlUtils.formatSocialUrl(linkedin, "linkedin.com"))
            if (github.isNotEmpty()) SocialMediaIconButton(R.drawable.github, "GitHub", UrlUtils.formatSocialUrl(github, "github.com"))
            if (twitter.isNotEmpty()) SocialMediaIconButton(R.drawable.twitt, "Twitter", UrlUtils.formatSocialUrl(twitter, "twitter.com"))
            if (instagram.isNotEmpty()) SocialMediaIconButton(R.drawable.insta, "Instagram", UrlUtils.formatSocialUrl(instagram, "instagram.com"))
            if (facebook.isNotEmpty()) SocialMediaIconButton(R.drawable.face, "Facebook", UrlUtils.formatSocialUrl(facebook, "facebook.com"))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            colors = CardDefaults.cardColors(containerColor = cardType.getColor().copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = cardType.getIcon()),
                    contentDescription = null,
                    tint = cardType.getColor(),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(cardType.getTitle(), style = MaterialTheme.typography.labelLarge, color = cardType.getColor())
            }
        }
    }
}
