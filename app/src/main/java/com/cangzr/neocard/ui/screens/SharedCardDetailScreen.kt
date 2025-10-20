package com.cangzr.neocard.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.cangzr.neocard.R
import com.cangzr.neocard.data.model.UserCard
import com.cangzr.neocard.data.model.TextStyleDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import coil.size.Size
import com.cangzr.neocard.analytics.CardAnalyticsManager
import com.cangzr.neocard.ads.BottomBannerAd

// CompositionLocal değişkenleri
val LocalCardId = compositionLocalOf<String?> { null }
val LocalCardOwnerId = compositionLocalOf<String?> { null }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedCardDetailScreen(
    cardId: String,
    onBackClick: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    var userCard by remember { mutableStateOf<UserCard?>(null) }
    var cardOwnerId by remember { mutableStateOf<String?>(null) } // 🔥 Kart sahibinin kullanıcı ID'si
    var isLoading by remember { mutableStateOf(true) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }
    var dialogAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var isAlreadyConnected by remember { mutableStateOf(false) }
    var isConnected by remember { mutableStateOf(false) }

    LaunchedEffect(cardId) {
        firestore.collection("users").get()
            .addOnSuccessListener { usersSnapshot ->
                var foundCard: UserCard? = null

                for (userDoc in usersSnapshot.documents) {
                    firestore.collection("users").document(userDoc.id)
                        .collection("cards").document(cardId).get()
                        .addOnSuccessListener { cardDoc ->
                            if (cardDoc.exists()) {
                                foundCard = cardDoc.toObject(UserCard::class.java)?.copy(id = cardDoc.id)
                                userCard = foundCard
                                cardOwnerId = userDoc.id // 🔥 Kartın sahibinin Firestore kullanıcı ID'sini al
                                isLoading = false
                                
                                // Kartvizit görüntülenme olayını kaydet
                                CardAnalyticsManager.getInstance().logCardView(
                                    cardId = cardId,
                                    cardOwnerId = userDoc.id,
                                    viewerUserId = auth.currentUser?.uid
                                )
                            }
                        }
                        .addOnFailureListener {
                            isLoading = false
                        }
                }
                if (foundCard == null) {
                    isLoading = false
                }
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    // 🔥 **Bağlantı isteği durumunu dinle**
    LaunchedEffect(cardOwnerId) {
        auth.currentUser?.uid?.let { currentUserId ->
            cardOwnerId?.let { ownerId ->
                firestore.collection("users").document(currentUserId)
                    .addSnapshotListener { document, _ ->
                        if (document != null) {
                            val connectRequests = document.get("connectRequests") as? List<Map<String, String>> ?: emptyList()
                            isAlreadyConnected = connectRequests.any { it["userId"] == ownerId && it["cardId"] == cardId }
                        }
                    }
            }
        }
    }

    // 🔥 **Bağlantı durumu dinleme (Bağlantılar listesinde mi?)**
    LaunchedEffect(cardOwnerId) {
        auth.currentUser?.uid?.let { currentUserId ->
            cardOwnerId?.let { ownerId ->
                firestore.collection("users").document(currentUserId)
                    .addSnapshotListener { document, _ ->
                        if (document != null) {
                            val connectedUsers = document.get("connected") as? List<Map<String, String>> ?: emptyList()
                            isConnected = connectedUsers.any { it["userId"] == ownerId && it["cardId"] == cardId }
                        }
                    }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(context.getString(R.string.card_detail)) },
                navigationIcon = {
                    IconButton(onClick = { onBackClick() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = context.getString(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (!isConnected) {
                        IconButton(onClick = {
                            dialogMessage = context.getString(R.string.confirm_send_connection_request)
                            dialogAction = {
                                sendConnectionRequest(
                                    auth.currentUser?.uid,  // 🔥 Gönderen kullanıcı ID'si
                                    cardOwnerId,            // 🔥 Kartın sahibinin kullanıcı ID'si
                                    cardId,                 // 🔥 Hangi kartla bağlantı isteği gönderildi
                                    context                 // 🔥 Android context (doğru parametre)
                                ) {
                                    isConnected = true
                                }
                            }
                            showDialog = true
                        }) {
                            Image(
                                painter = painterResource(id = R.drawable.adduser),
                                contentDescription = context.getString(R.string.add_connection),
                                modifier = Modifier.size(24.dp),
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                            )
                        }

                    } else {
                        IconButton(onClick = {
                            dialogMessage = context.getString(R.string.confirm_remove_connection)
                            dialogAction = {
                                removeConnection(
                                    auth.currentUser?.uid, // 🔥 Mevcut kullanıcı ID
                                    cardOwnerId,           // 🔥 Bağlantı kurulan kullanıcı ID
                                    cardId,                // 🔥 Hangi kart ile bağlantı yapılmıştı
                                    context
                                ) {
                                    isConnected = false
                                }
                            }
                            showDialog = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = context.getString(R.string.remove_connection),
                                modifier = Modifier.size(24.dp),
                                tint = Color.Red
                            )
                        }

                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (userCard == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(context.getString(R.string.card_not_found))
            }
        } else {
            val card = userCard!!
            
            // CompositionLocalProvider ile cardId ve cardOwnerId değerlerini alt bileşenlere aktar
            CompositionLocalProvider(
                LocalCardId provides card.id,
                LocalCardOwnerId provides cardOwnerId
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profil resmi
                    Box(
                        modifier = Modifier
                            .padding(vertical = 24.dp)
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (card.profileImageUrl != null && card.profileImageUrl!!.isNotEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(card.profileImageUrl)
                                    .crossfade(true)
                                    .size(Size.ORIGINAL)
                                    .placeholder(R.drawable.logo3)
                                    .error(R.drawable.logo3)
                                    .transformations(CircleCropTransformation())
                                    .build(),
                                contentDescription = context.getString(R.string.profile_picture),
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Kullanıcı Bilgileri
                    InfoDisplayColumn(
                        card = card, 
                        context = context
                    )
                    
                    // Alt banner reklam
                    BottomBannerAd(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
                            title = { Text(context.getString(R.string.confirmation_required)) },
            text = { Text(dialogMessage) },
            confirmButton = {
                Button(onClick = {
                    dialogAction?.invoke()
                    showDialog = false
                }) {
                    Text(context.getString(R.string.yes))
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text(context.getString(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun InfoDisplayColumn(
    card: UserCard,
    context: android.content.Context
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text("${card.name} ${card.surname}", style = MaterialTheme.typography.headlineMedium)
        Text(card.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(card.company, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))

        Spacer(modifier = Modifier.height(16.dp))
        
        // Biyografi alanı
        if (card.bio?.isNotEmpty() == true) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
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
                        text = card.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        InfoItem(R.drawable.email, card.email) {
            try {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:${card.email}")
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.email_app_error), Toast.LENGTH_SHORT).show()
            }
        }
        
        InfoItem(R.drawable.phone, card.phone) {
            try {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:${card.phone}")
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.phone_app_error), Toast.LENGTH_SHORT).show()
            }
        }

        if (card.website.isNotEmpty()) {
            InfoItem(R.drawable.web, card.website) {
                try {
                    var webUrl = card.website
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
        
        // CV alanı
        if (card.cv?.isNotEmpty() == true) {
            InfoItem(R.drawable.document, context.getString(R.string.view_cv)) {
                try {
                    var cvUrl = card.cv
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

        // Sosyal Medya İkonları
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (card.linkedin.isNotEmpty()) SharedCardSocialMediaIconButton(R.drawable.linkedin, "LinkedIn", formatSharedSocialUrl(card.linkedin, "linkedin.com"), context)
            if (card.github.isNotEmpty()) SharedCardSocialMediaIconButton(R.drawable.github, "GitHub", formatSharedSocialUrl(card.github, "github.com"), context)
            if (card.twitter.isNotEmpty()) SharedCardSocialMediaIconButton(R.drawable.twitt, "Twitter", formatSharedSocialUrl(card.twitter, "twitter.com"), context)
            if (card.instagram.isNotEmpty()) SharedCardSocialMediaIconButton(R.drawable.insta, "Instagram", formatSharedSocialUrl(card.instagram, "instagram.com"), context)
            if (card.facebook.isNotEmpty()) SharedCardSocialMediaIconButton(R.drawable.face, "Facebook", formatSharedSocialUrl(card.facebook, "facebook.com"), context)
        }
    }
}

@Composable
fun InfoItem(iconRes: Int, text: String, onClick: () -> Unit) {
    val cardId = (LocalCardId.current ?: "")
    val cardOwnerId = LocalCardOwnerId.current ?: ""
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { 
                // Link tıklanma olayını kaydet
                val linkType = when (iconRes) {
                    R.drawable.email -> "email"
                    R.drawable.phone -> "phone"
                    R.drawable.web -> "website"
                    R.drawable.document -> "cv"
                    else -> "other"
                }
                
                CardAnalyticsManager.getInstance().logLinkClick(
                    cardId = cardId,
                    linkType = linkType,
                    viewerUserId = FirebaseAuth.getInstance().currentUser?.uid
                )
                
                onClick() 
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun SharedCardSocialMediaIconButton(iconRes: Int, contentDescription: String, url: String, context: android.content.Context) {
    val cardId = (LocalCardId.current ?: "")
    val cardOwnerId = LocalCardOwnerId.current ?: ""
    
    IconButton(onClick = { 
        try {
            // Link tıklanma olayını kaydet
            CardAnalyticsManager.getInstance().logLinkClick(
                cardId = cardId,
                linkType = contentDescription.lowercase(),
                viewerUserId = FirebaseAuth.getInstance().currentUser?.uid
            )
            
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

// 🔥 **Bağlantı isteği gönderme fonksiyonu**
fun sendConnectionRequest(
    currentUserId: String?,
    targetUserId: String?,
    cardId: String?,
    context: android.content.Context,
    onSuccess: () -> Unit
) {
    if (currentUserId == null || targetUserId == null || cardId == null) return

    val firestore = FirebaseFirestore.getInstance()
    val requestData = mapOf("userId" to currentUserId, "cardId" to cardId)

    // Önce gönderen kullanıcının bilgilerini al
    firestore.collection("users").document(currentUserId)
        .collection("cards").document(cardId)
        .get()
        .addOnSuccessListener { cardDoc ->
            val senderCard = cardDoc.toObject(UserCard::class.java)
            
            // Bağlantı isteğini gönder
            firestore.collection("users").document(targetUserId)
                .update("connectRequests", FieldValue.arrayUnion(requestData))
                .addOnSuccessListener {
                    // Bildirim oluştur
                    val notification = Notification(
                        userId = targetUserId,
                        title = context.getString(R.string.new_connection_request),
                        message = context.getString(R.string.connection_request_message, "${senderCard?.name} ${senderCard?.surname}"),
                        type = "CONNECTION_REQUEST",
                        relatedId = cardId
                    )

                    // Bildirimi kaydet
                    firestore.collection("notifications")
                        .add(notification)
                        .addOnSuccessListener { notificationRef ->
                            // Kullanıcının bildirimlerini güncelle
                            firestore.collection("users")
                                .document(targetUserId)
                                .update("notifications", FieldValue.arrayUnion(notificationRef.id))
                        }

                    Toast.makeText(context, context.getString(R.string.connection_request_sent), Toast.LENGTH_SHORT).show()
                    onSuccess()
                }
        }
}

// 🔥 **Bağlantıyı kaldırma fonksiyonu**
fun removeConnection(
    currentUserId: String?,
    targetUserId: String?,
    cardId: String?,
    context: android.content.Context,
    onSuccess: () -> Unit
) {
    if (currentUserId == null || targetUserId == null || cardId == null) return

    val firestore = FirebaseFirestore.getInstance()
    val connectionData = mapOf("userId" to targetUserId, "cardId" to cardId)

    firestore.collection("users").document(currentUserId)
        .update("connected", FieldValue.arrayRemove(connectionData))
        .addOnSuccessListener {
            firestore.collection("users").document(targetUserId)
                .update("connected", FieldValue.arrayRemove(mapOf("userId" to currentUserId, "cardId" to cardId)))
                .addOnSuccessListener {
                    Toast.makeText(context, context.getString(R.string.connection_removed), Toast.LENGTH_SHORT).show()
                    onSuccess()
                }
        }
}

// Bildirim veri sınıfı
data class Notification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "",
    val relatedId: String = "",
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

// Sosyal medya URL'lerini düzenleme
fun formatSharedSocialUrl(url: String, domain: String): String {
    return when {
        url.startsWith("http://") || url.startsWith("https://") -> url
        url.contains(domain) -> "https://$url"
        else -> "https://$domain/$url"
    }
}