package com.cangzr.neocard.ui.screens

import android.app.Activity
import android.content.Intent
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.cangzr.neocard.R
import com.cangzr.neocard.Screen
import com.cangzr.neocard.data.model.User
import com.cangzr.neocard.utils.LanguageManager
import com.cangzr.neocard.utils.acceptConnectionRequest
import com.cangzr.neocard.utils.fetchUsersByIds
import com.cangzr.neocard.utils.rejectConnectionRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.Unit
import com.cangzr.neocard.billing.BillingManager
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.cangzr.neocard.storage.FirebaseStorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfileScreen(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    val specialUserId = "bhEx5ZPVyOY4YJ61FdaboFhfy1B2"
    val isLoggedIn = currentUser != null
    val isSpecialUser = currentUser?.uid == specialUserId

    // Premium durumunu kontrol et
    var isPremium by remember { mutableStateOf(false) }
    var hasConnectRequests by remember { mutableStateOf(false) }

    var promoCodeList by remember { mutableStateOf<List<PromoCode>>(emptyList()) }

    // Uygulama sürümünü al
    val packageInfo = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: Exception) {
            null
        }
    }
    val currentAppVersion = remember { packageInfo?.versionName ?: "1.0.0" }
            
        // Promosyon kodlarını çek (sadece admin için)
    LaunchedEffect(Unit) {
        if (isSpecialUser) {
            firestore.collection("promoCodes")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    promoCodeList = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(PromoCode::class.java)?.copy(id = doc.id)
                    } ?: emptyList()
                }
        }
    }

    // Kullanıcı bilgilerini kontrol et
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            firestore.collection("users").document(currentUser.uid)
                .addSnapshotListener { document, error ->
                    if (error != null || document == null) return@addSnapshotListener

                    isPremium = document.getBoolean("premium") ?: false
                    val connectRequests = document.get("connectRequests") as? List<*>
                    hasConnectRequests = !connectRequests.isNullOrEmpty()


                }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profil Kartı
            item {
                ProfileCard(navController)
            }


            // Promosyon Kodu Kullanma (sadece giriş yapmış ve premium olmayan kullanıcılara göster)
            if (isLoggedIn && !isPremium) {
                item {
                    PromoCodeRedeemCard(
                        onRedeemCode = { code ->
                            redeemPromoCode(code, context, currentUser.uid)
                        }
                    )
                }
            }

            // Premium Kart (sadece giriş yapmış ve premium olmayan kullanıcılara göster)
            if (isLoggedIn && !isPremium) {
                item {
                    PremiumCard()
                }
            }

            // Bağlantı İstekleri (sadece giriş yapmış ve isteği olan kullanıcılara göster)
            if (isLoggedIn) {
                item {
                    ConnectionRequestsSection(navController)
                }
            }



            // Ayarlar ve İşlemler Kartı
            if (isLoggedIn) {
                item {
                    SettingsAndActionsCard(
                        navController = navController,
                        isSpecialUser = isSpecialUser
                    )
                }
            } else {
                item {
                    SettingsCard()
                }
            }


            // Promosyon Kodu Kartı (sadece admin için)
            if (isLoggedIn && isSpecialUser) {
                item {
                    PromoCodeCard(
                        promoCodeList = promoCodeList,
                        onAddCode = { code, usageLimit ->
                            // Yeni promosyon kodu ekle
                            val newPromoCode = PromoCode(
                                code = code,
                                usageLimit = usageLimit,
                                usageCount = 0,
                                createdAt = System.currentTimeMillis(),
                                isActive = true
                            )
                            firestore.collection("promoCodes")
                                .add(newPromoCode)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Promosyon kodu oluşturuldu", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        },
                        onDeleteCode = { codeId ->
                            firestore.collection("promoCodes").document(codeId)
                                .delete()
                                .addOnSuccessListener {
                                    Toast.makeText(context, context.getString(R.string.promo_code_deleted), Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, context.getString(R.string.error_message, e.message ?: ""), Toast.LENGTH_SHORT).show()
                                }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileCard(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var userDisplayName by remember { mutableStateOf("") }
    var userPhotoUrl by remember { mutableStateOf<String?>(null) }
    val currentUser = remember { mutableStateOf(auth.currentUser) }

    val isLoggedIn = currentUser.value != null

    LaunchedEffect(currentUser.value) {
        currentUser.value?.let { user ->
            // Google hesabından gelen bilgileri kullan
            userDisplayName = user.displayName ?: "Bilinmeyen Kullanıcı"
            userPhotoUrl = user.photoUrl?.toString()
            
            // Firestore'daki kullanıcı bilgilerini kontrol et ve gerekirse güncelle
            firestore.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Eğer displayName Firestore'da yoksa veya boşsa, Google'dan gelen ismi kullan
                        if (document.getString("displayName").isNullOrEmpty() && !userDisplayName.isNullOrEmpty()) {
                            firestore.collection("users").document(user.uid)
                                .update("displayName", userDisplayName)
                        }
                    }
                }
        }
    }

    LaunchedEffect(Unit) {
        auth.addAuthStateListener { authInstance ->
            currentUser.value = authInstance.currentUser
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (!isLoggedIn) navController.navigate(Screen.Auth.route) },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (isLoggedIn) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Kullanıcı profil fotoğrafı (Google'dan)
                        if (userPhotoUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(userPhotoUrl)
                                    .crossfade(true)
                                    .transformations(CircleCropTransformation())
                                    .build(),
                                contentDescription = "Profil Fotoğrafı",
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // Profil fotoğrafı yoksa varsayılan ikon göster
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.user_icon),
                                    contentDescription = "Varsayılan Profil İkonu",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        
                        Column(
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = userDisplayName.ifEmpty { context.getString(R.string.user) },
                                style = MaterialTheme.typography.titleLarge,
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = currentUser.value?.email ?: context.getString(R.string.no_email),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 18.sp
                            )
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { navController.navigate("notifications") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Bildirimler",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = {
                                FirebaseAuth.getInstance().signOut()
                                navController.navigate("auth") {
                                    popUpTo("home") { inclusive = true }
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Çıkış Yap",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = context.getString(R.string.login_prompt),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Button(
                        onClick = { navController.navigate(Screen.Auth.route) }
                        ) {
                        Text(context.getString(R.string.login))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsCard() {
    val context = LocalContext.current
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    
    // Get current language
    val currentLanguage = remember { LanguageManager.getSelectedLanguage(context) }
    var selectedLanguage by remember { mutableStateOf(currentLanguage) }

    // BottomSheet içeriği
    if (selectedOption != null) {
        BottomSheetContent(selectedOption!!) {
            selectedOption = null // BottomSheet'i kapat
        }
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
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Language settings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLanguageDialog = true }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.settings),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                    )
                    Text(context.getString(R.string.language))
                }
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            listOf(
                R.drawable.privacy to context.getString(R.string.privacy_policy),
                R.drawable.info to context.getString(R.string.about),
                R.drawable.help to context.getString(R.string.help)
            ).forEach { (icon, title) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedOption = title }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = icon),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                        )
                        Text(title)
                    }
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheetContent(selectedOption: String, onDismiss: () -> Unit) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumCard() {
    var showSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showSheet = true },
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
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD700)
                )
                Column {
                    Text(
                        text = context.getString(R.string.upgrade_to_premium),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = context.getString(R.string.unlimited_cards_and_more),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false }
        ) {
            PremiumContent { showSheet = false }
        }
    }
}

@Composable
fun PremiumContent(onClose: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val billingManager = remember { BillingManager.getInstance(context) }
    val isPremium by billingManager.isPremium.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Başlık Alanı
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = context.getString(R.string.premium_membership),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Text(
                text = context.getString(R.string.discover_more_features),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Premium Özellikleri
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                listOf(
                    Triple(Icons.Default.Star, context.getString(R.string.unlimited_cards), context.getString(R.string.unlimited_cards_desc)),
                    Triple(Icons.Default.Star, context.getString(R.string.custom_designs), context.getString(R.string.custom_designs_desc)),
                    Triple(Icons.Default.Star, context.getString(R.string.detailed_info), context.getString(R.string.detailed_info_desc)),
                    Triple(Icons.Default.Star, context.getString(R.string.ad_free), context.getString(R.string.ad_free_desc)),
                ).forEach { (icon, title, description) ->
                    PremiumFeatureItemNew(
                        icon = icon,
                        title = title,
                        description = description
                    )
                }
            }
        }

        // Fiyat Kartı
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = context.getString(R.string.monthly_price),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = context.getString(R.string.monthly),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                Button(
                    onClick = { 
                        activity?.let { billingManager.launchBillingFlow(it) }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isPremium
                ) {
                    Text(
                        text = if (isPremium) context.getString(R.string.premium_active) else context.getString(R.string.start_now),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Kapat Butonu
        TextButton(
            onClick = onClose,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                text = context.getString(R.string.maybe_later),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PremiumFeatureItemNew(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// fetchUsersByIds function is now imported from shared utils

// User data class is now imported from shared model

@Composable
fun ConnectionRequestsSection(navController: NavHostController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    val currentUser = auth.currentUser
    var connectionRequests by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
    var userMap by remember { mutableStateOf<Map<String, User>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            connectionRequests = listOf()
            isLoading = false
        } else {
            firestore.collection("users").document(currentUser.uid)
                .addSnapshotListener { document, error ->
                    if (error != null || document == null) {
                        isLoading = false
                        return@addSnapshotListener
                    }

                    val requestList = document.get("connectRequests") as? List<Map<String, String>> ?: emptyList()
                    val userIds = requestList.mapNotNull { it["userId"] }.distinct()

                    if (userIds.isEmpty()) {
                        connectionRequests = emptyList()
                        isLoading = false
                    } else {
                        fetchUsersByIds(userIds) { users ->
                            userMap = users.associateBy { it.id }
                            connectionRequests = requestList
                            isLoading = false
                        }
                    }
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        // Başlık ve Sayaç
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = context.getString(R.string.connection_requests),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                /*if (connectionRequests.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "${connectionRequests.size}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }*/
                
                // Tümünü Gör butonu
                TextButton(
                    onClick = { 
                        // Navigation'a bağlantı istekleri ekranına git
                        navController.navigate(Screen.ConnectionRequests.route)
                    }
                ) {
                    Text(
                        text = context.getString(R.string.view_all),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // İçerik
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(32.dp)
                )
            }
        } else if (connectionRequests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = context.getString(R.string.no_connection_requests),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(connectionRequests) { request ->
                    val requestUserId = request["userId"] ?: return@items
                    val cardId = request["cardId"] ?: return@items
                    val user = userMap[requestUserId]

                    ConnectionRequestCard(
                        userId = requestUserId,
                        name = user?.displayName ?: "Bilinmeyen Kullanıcı",
                        email = user?.email ?: "E-posta Bulunamadı",
                        onAccept = {
                            acceptConnectionRequest(currentUser?.uid, requestUserId, cardId) {
                                connectionRequests = connectionRequests.filter { 
                                    it["userId"] != requestUserId && it["cardId"] != cardId 
                                }
                            }
                        },
                        onReject = {
                            rejectConnectionRequest(currentUser?.uid, requestUserId, cardId) {
                                connectionRequests = connectionRequests.filter { 
                                    it["userId"] != requestUserId && it["cardId"] != cardId 
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectionRequestCard(
    userId: String,
    name: String,
    email: String,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(240.dp)
            .padding(4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Profil ve Bilgiler
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profil Resmi
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // İsim ve E-posta
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Butonlar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Reddet Butonu
                Button(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF4444)
                    ),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Reddet",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }

                // Kabul Et Butonu
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Kabul Et",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// Connection utility functions are now imported from shared utils

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

// Firebase Kullanıcı Silme ve Firestore Verilerini Temizleme
fun deleteAccount(context: android.content.Context, onResult: (Boolean, String) -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser
    val firestore = FirebaseFirestore.getInstance()
    val storageManager = FirebaseStorageManager.getInstance()

    if (user != null) {
        val uid = user.uid
        
        // Google ile giriş yapan kullanıcılar için özel işlem
        if (user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }) {
            // Google ile giriş yapan kullanıcılar için yeniden kimlik doğrulama gerekli
            // Bu durumda kullanıcıyı bilgilendir
            onResult(false, context.getString(R.string.google_account_reauth_required))
            return
        }
        
        // Standart hesap silme işlemi
        user.delete()
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    // Kullanıcının verilerini silmeden önce kartlarını al
                    firestore.collection("users").document(uid)
                        .collection("cards").get()
                        .addOnSuccessListener { cardsSnapshot ->
                            // Batch işlemi başlat
                            val batch = firestore.batch()
                            
                            // Tüm kartları public_cards koleksiyonundan da sil
                            for (cardDoc in cardsSnapshot.documents) {
                                val cardId = cardDoc.id
                                val publicCardRef = firestore.collection("public_cards").document(cardId)
                                batch.delete(publicCardRef)
                            }
                            
                            // Kullanıcının iş ilanlarını da sil
                            firestore.collection("jobPosts")
                                .whereEqualTo("userId", uid)
                                .get()
                                .addOnSuccessListener { jobPostsSnapshot ->
                                    for (jobDoc in jobPostsSnapshot.documents) {
                                        batch.delete(jobDoc.reference)
                                    }
                                    
                                    // Kullanıcı belgesini sil
                                    batch.delete(firestore.collection("users").document(uid))
                                    
                                    // Batch işlemini uygula
                                    batch.commit().addOnSuccessListener {
                                        // Kullanıcının Storage'daki tüm resimlerini sil
                                        CoroutineScope(Dispatchers.IO).launch {
                                            storageManager.deleteAllUserImages(uid)
                                            withContext(Dispatchers.Main) {
                            onResult(true, context.getString(R.string.account_deleted_successfully))
                        }
                                        }
                                    }.addOnFailureListener { e ->
                            onResult(false, context.getString(R.string.account_deleted_firestore_error, e.localizedMessage ?: ""))
                        }
                                }
                                .addOnFailureListener { e ->
                                    onResult(false, context.getString(R.string.account_deleted_jobs_error, e.localizedMessage ?: ""))
                                }
                        }
                        .addOnFailureListener { e ->
                            onResult(false, context.getString(R.string.account_deleted_cards_error, e.localizedMessage ?: ""))
                        }
                } else {
                    // Kimlik doğrulama gerekiyorsa
                    if (authTask.exception is FirebaseAuthRecentLoginRequiredException) {
                        onResult(false, context.getString(R.string.reauth_required))
                } else {
                    onResult(false, context.getString(R.string.account_delete_error, authTask.exception?.localizedMessage ?: ""))
                    }
                }
            }
    } else {
        onResult(false, context.getString(R.string.no_logged_in_user))
    }
}


@OptIn(ExperimentalMaterial3Api::class)
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
                deleteAccount(context) { success, message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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
                iconRes != null -> Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(tint)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = tint
            )
        }
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = tint.copy(alpha = 0.7f)
        )
    }
}


// Promosyon kodu veri sınıfı
data class PromoCode(
    val id: String = "",
    val code: String = "",
    val usageLimit: Int = 0,
    val usageCount: Int = 0,
    val createdAt: Long = 0,
    val isActive: Boolean = true
)

// Promosyon kodu kullanma fonksiyonu
fun redeemPromoCode(code: String, context: Context, userId: String) {
    val firestore = FirebaseFirestore.getInstance()
    val billingManager = BillingManager.getInstance(context)
    val promoCodeRef = firestore.collection("promoCodes").whereEqualTo("code", code).limit(1)
    
    promoCodeRef.get().addOnSuccessListener { querySnapshot ->
        if (querySnapshot.isEmpty) {
            Toast.makeText(context, context.getString(R.string.promo_code_invalid), Toast.LENGTH_SHORT).show()
            return@addOnSuccessListener
        }
        
        val promoDoc = querySnapshot.documents[0]
        val promoCode = promoDoc.toObject(PromoCode::class.java)?.copy(id = promoDoc.id)
        
        if (promoCode == null) {
            Toast.makeText(context, context.getString(R.string.promo_code_not_found), Toast.LENGTH_SHORT).show()
            return@addOnSuccessListener
        }
        
        if (!promoCode.isActive) {
            Toast.makeText(context, context.getString(R.string.promo_code_expired), Toast.LENGTH_SHORT).show()
            return@addOnSuccessListener
        }
        
        if (promoCode.usageCount >= promoCode.usageLimit) {
            Toast.makeText(context, context.getString(R.string.promo_code_limit_reached), Toast.LENGTH_SHORT).show()
            
            // Kodu pasif hale getir
            firestore.collection("promoCodes").document(promoCode.id)
                .update("isActive", false)
                
            return@addOnSuccessListener
        }
        
        // Kullanıcı bu kodu daha önce kullanmış mı kontrol et
        firestore.collection("users").document(userId)
            .collection("usedPromoCodes")
            .whereEqualTo("promoCodeId", promoCode.id)
            .get()
            .addOnSuccessListener { userCodeSnapshot ->
                if (!userCodeSnapshot.isEmpty) {
                    Toast.makeText(context, context.getString(R.string.promo_code_already_used), Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                
                // Kullanım sayısını artır
                firestore.collection("promoCodes").document(promoCode.id)
                    .update("usageCount", promoCode.usageCount + 1)
                
                // Kullanıcıya premium üyelik ver (BillingManager üzerinden)
                val success = billingManager.setPremiumWithPromoCode(userId, BillingManager.PROMO_PREMIUM_DURATION)
                
                if (success) {
                    // Kullanıcının kullandığı kodları kaydet
                    val usedPromoCode = hashMapOf(
                        "promoCodeId" to promoCode.id,
                        "usedAt" to System.currentTimeMillis()
                    )
                    
                    firestore.collection("users").document(userId)
                        .collection("usedPromoCodes")
                        .add(usedPromoCode)
                    
                    Toast.makeText(context, context.getString(R.string.promo_code_success), Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, context.getString(R.string.premium_activation_error), Toast.LENGTH_SHORT).show()
                }
            }
    }.addOnFailureListener { e ->
        Toast.makeText(context, context.getString(R.string.error_message, e.message ?: ""), Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun PromoCodeRedeemCard(
    onRedeemCode: (String) -> Unit
) {
    val context = LocalContext.current
    var promoCode by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true },
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
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = context.getString(R.string.promo_code_use),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = context.getString(R.string.promo_code_weekly_premium),
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
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(context.getString(R.string.promo_code_dialog_title)) },
            text = {
                Column {
                    Text(
                        text = context.getString(R.string.promo_code_dialog_description),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = promoCode,
                        onValueChange = { promoCode = it.trim().uppercase() },
                        label = { Text(context.getString(R.string.promo_code_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (promoCode.isNotEmpty()) {
                            onRedeemCode(promoCode)
                            showDialog = false
                        }
                    }
                ) {
                    Text(context.getString(R.string.promo_code_use_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(context.getString(R.string.promo_code_cancel))
                }
            }
        )
    }
}

@Composable
fun PromoCodeCard(
    promoCodeList: List<PromoCode>,
    onAddCode: (String, Int) -> Unit,
    onDeleteCode: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newPromoCode by remember { mutableStateOf("") }
    var usageLimit by remember { mutableStateOf("10") }
    var expandedCodeId by remember { mutableStateOf<String?>(null) }
    
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Promosyon Kodları",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Promosyon Kodu Ekle",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            if (promoCodeList.isEmpty()) {
                Text(
                    text = "Henüz promosyon kodu bulunmuyor.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    promoCodeList.forEach { promoCode ->
                        PromoCodeItem(
                            promoCode = promoCode,
                            isExpanded = expandedCodeId == promoCode.id,
                            onExpandToggle = { 
                                expandedCodeId = if (expandedCodeId == promoCode.id) null else promoCode.id 
                            },
                            onDelete = { onDeleteCode(promoCode.id) }
                        )
                    }
                }
            }
        }
    }
    
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Yeni Promosyon Kodu") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPromoCode,
                        onValueChange = { newPromoCode = it.trim().uppercase() },
                        label = { Text("Promosyon Kodu") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = usageLimit,
                        onValueChange = { 
                            if (it.isEmpty() || it.all { c -> c.isDigit() }) {
                                usageLimit = it
                            } 
                        },
                        label = { Text("Kullanım Limiti") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPromoCode.isNotEmpty() && usageLimit.isNotEmpty()) {
                            onAddCode(newPromoCode, usageLimit.toIntOrNull() ?: 10)
                            newPromoCode = ""
                            usageLimit = "10"
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Ekle")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }
}

@Composable
fun PromoCodeItem(
    promoCode: PromoCode,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onExpandToggle),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (promoCode.isActive) 
                MaterialTheme.colorScheme.surface 
            else 
                MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (promoCode.isActive) 
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else 
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = promoCode.code.take(1),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (promoCode.isActive) 
                                MaterialTheme.colorScheme.primary
                            else 
                                MaterialTheme.colorScheme.error
                        )
                    }
                    
                    Column {
                        Text(
                            text = promoCode.code,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (promoCode.isActive) 
                                MaterialTheme.colorScheme.onSurface
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        
                        val status = if (!promoCode.isActive) {
                            "Pasif"
                        } else if (promoCode.usageCount >= promoCode.usageLimit) {
                            "Limit Doldu"
                        } else {
                            "Aktif"
                        }
                        
                        val statusColor = when(status) {
                            "Aktif" -> Color(0xFF4CAF50) // Yeşil
                            else -> Color(0xFFE53935) // Kırmızı
                        }
                        
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor
                        )
                    }
                }
                
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (promoCode.isActive && promoCode.usageCount < promoCode.usageLimit) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.error
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${promoCode.usageCount}/${promoCode.usageLimit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 10.sp
                    )
                }
            }
            
            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Oluşturulma: ${formatDate(promoCode.createdAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Sil",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Tarih formatlamak için yardımcı fonksiyon
fun formatDate(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val format = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
    return format.format(date)
}
