package com.cangzr.neocard.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import coil.transform.CircleCropTransformation
import com.cangzr.neocard.R
import com.cangzr.neocard.data.model.UserCard
import com.cangzr.neocard.data.model.TextStyleDTO
import com.cangzr.neocard.ui.screens.createcard.utils.CardCreationUtils

@Composable
fun UserCardItem(card: UserCard, onClick: () -> Unit) {
    val nameSurnameColor = Color(android.graphics.Color.parseColor(card.textStyles["NAME_SURNAME"]?.color ?: "#000000"))
    val isDemoCard = card.id == "demo"
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .width(300.dp)
            .height(180.dp)
            .clickable(enabled = !isDemoCard) { onClick() }
            .alpha(if (isDemoCard) 0.7f else 1f),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(1f)
                .background(parseBackground(card, context))
                .padding(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().fillMaxSize()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    if (card.profileImageUrl!!.isNotEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(card.profileImageUrl)
                                .crossfade(true)
                                .size(Size(192, 192)) // Tam olarak ihtiyaç duyulan boyut (64dp = 192px @3x)
                                .memoryCacheKey(card.profileImageUrl)
                                .diskCacheKey(card.profileImageUrl)
                                .placeholder(R.drawable.logo3)
                                .error(R.drawable.logo3)
                                .transformations(CircleCropTransformation())
                                .build(),
                            contentDescription = "Profil Resmi",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("${card.name} ${card.surname}", style = parseTextStyle(card.textStyles["NAME_SURNAME"]))
                        Text(card.title, style = parseTextStyle(card.textStyles["TITLE"]))
                        Text(card.company, style = parseTextStyle(card.textStyles["COMPANY"]))
                        Text(card.email, style = parseTextStyle(card.textStyles["EMAIL"]))
                        Text(card.phone, style = parseTextStyle(card.textStyles["PHONE"]))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (card.website.isNotEmpty()) SocialIcon(R.drawable.web, nameSurnameColor)
                    if (card.linkedin.isNotEmpty()) SocialIcon(R.drawable.linkedin, nameSurnameColor)
                    if (card.github.isNotEmpty()) SocialIcon(R.drawable.github, nameSurnameColor)
                    if (card.twitter.isNotEmpty()) SocialIcon(R.drawable.twitt, nameSurnameColor)
                    if (card.instagram.isNotEmpty()) SocialIcon(R.drawable.insta, nameSurnameColor)
                    if (card.facebook.isNotEmpty()) SocialIcon(R.drawable.face, nameSurnameColor)
                }
            }
        }
    }
}

@Composable
fun SocialIcon(iconRes: Int, color: Color) {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(id = iconRes),
        contentDescription = null,
        modifier = androidx.compose.ui.Modifier.size(24.dp),
        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(color)
    )
}

fun parseBackground(card: UserCard, context: android.content.Context): Brush {
    return if (card.backgroundType == "GRADIENT") {
        var gradient = CardCreationUtils.getPredefinedGradients(context).firstOrNull { it.first == card.selectedGradient }
        
        if (gradient == null) {
            val allGradients = listOf(
                Pair("Gün Batımı", Brush.horizontalGradient(listOf(Color(0xFFFE6B8B), Color(0xFFFF8E53)))),
                Pair("Sunset", Brush.horizontalGradient(listOf(Color(0xFFFE6B8B), Color(0xFFFF8E53)))),
                Pair("Okyanus", Brush.horizontalGradient(listOf(Color(0xFF2196F3), Color(0xFF00BCD4)))),
                Pair("Ocean", Brush.horizontalGradient(listOf(Color(0xFF2196F3), Color(0xFF00BCD4)))),
                Pair("Orman", Brush.horizontalGradient(listOf(Color(0xFF4CAF50), Color(0xFF8BC34A)))),
                Pair("Forest", Brush.horizontalGradient(listOf(Color(0xFF4CAF50), Color(0xFF8BC34A)))),
                Pair("Gece", Brush.verticalGradient(listOf(Color(0xFF2C3E50), Color(0xFF3498DB)))),
                Pair("Night", Brush.verticalGradient(listOf(Color(0xFF2C3E50), Color(0xFF3498DB)))),
                Pair("Mor Sis", Brush.verticalGradient(listOf(Color(0xFF9C27B0), Color(0xFFE91E63)))),
                Pair("Purple Mist", Brush.verticalGradient(listOf(Color(0xFF9C27B0), Color(0xFFE91E63))))
            )
            gradient = allGradients.firstOrNull { it.first == card.selectedGradient }
        }
        
        gradient?.second ?: Brush.verticalGradient(listOf(Color.Gray, Color.LightGray))
    } else {
        Brush.verticalGradient(
            listOf(
                Color(android.graphics.Color.parseColor(card.backgroundColor)),
                Color(android.graphics.Color.parseColor(card.backgroundColor))
            )
        )
    }
}

fun parseTextStyle(dto: TextStyleDTO?): androidx.compose.ui.text.TextStyle {
    return androidx.compose.ui.text.TextStyle(
        fontSize = (dto?.fontSize?.sp ?: 16.sp),
        fontWeight = if (dto?.isBold == true) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
        fontStyle = if (dto?.isItalic == true) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
        textDecoration = if (dto?.isUnderlined == true) androidx.compose.ui.text.style.TextDecoration.Underline else androidx.compose.ui.text.style.TextDecoration.None,
        color = Color(android.graphics.Color.parseColor(dto?.color ?: "#000000"))
    )
}
