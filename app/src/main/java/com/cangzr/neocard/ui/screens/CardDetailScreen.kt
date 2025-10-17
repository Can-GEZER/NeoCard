package com.cangzr.neocard.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
// removed BarChart to avoid unresolved icon; using Info instead
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.cangzr.neocard.R
import com.cangzr.neocard.data.CardType
import com.cangzr.neocard.ui.screens.UserCard
import com.cangzr.neocard.billing.BillingManager
import com.cangzr.neocard.analytics.CardAnalyticsManager
import com.cangzr.neocard.analytics.CardStatistics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import coil.size.Size
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.cangzr.neocard.Screen
import com.cangzr.neocard.storage.FirebaseStorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// no custom extensions for icons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    cardId: String,
    onBackClick: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    val context = androidx.compose.ui.platform.LocalContext.current
    val billingManager = remember { BillingManager.getInstance(context) }
    val isPremium by billingManager.isPremium.collectAsState()

    var isEditing by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var userCard by remember { mutableStateOf<UserCard?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    var showErrorMessage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var bio by remember { mutableStateOf(userCard?.bio ?: "") }
    var cv by remember { mutableStateOf(userCard?.cv ?: "") }
    var showStatistics by remember { mutableStateOf(false) }
    var cardStatistics by remember { mutableStateOf<CardStatistics?>(null) }
    var isLoadingStats by remember { mutableStateOf(false) }
    
    // Değişkenleri önce tanımlayalım
    var name by remember { mutableStateOf(userCard?.name ?: "") }
    var surname by remember { mutableStateOf(userCard?.surname ?: "") }
    var title by remember { mutableStateOf(userCard?.title ?: "") }
    var company by remember { mutableStateOf(userCard?.company ?: "") }
    var phone by remember { mutableStateOf(userCard?.phone ?: "") }
    var email by remember { mutableStateOf(userCard?.email ?: "") }
    var website by remember { mutableStateOf(userCard?.website ?: "") }
    var linkedin by remember { mutableStateOf(userCard?.linkedin ?: "") }
    var github by remember { mutableStateOf(userCard?.github ?: "") }
    var twitter by remember { mutableStateOf(userCard?.twitter ?: "") }
    var instagram by remember { mutableStateOf(userCard?.instagram ?: "") }
    var facebook by remember { mutableStateOf(userCard?.facebook ?: "") }

    // Resim seçici
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
            }
        }
    )

    // İstatistikleri yükleme fonksiyonu (önce tanımla, sonra çağır)
    fun loadCardStatistics(cardId: String, ownerId: String) {
        isLoadingStats = true
        CardAnalyticsManager.getInstance().getCardStatistics(
            cardId = cardId,
            onSuccess = { stats ->
                cardStatistics = stats
                isLoadingStats = false
            },
            onError = { e ->
                errorMessage = context.getString(R.string.statistics_load_error, e.message)
                showErrorMessage = true
                isLoadingStats = false
            }
        )
    }

    // İlk açılışta veriyi çek
    LaunchedEffect(cardId) {
        isLoading = true
        currentUser?.let { user ->
            firestore.collection("users")
                .document(user.uid)
                .collection("cards")
                .document(cardId)
                .get()
                .addOnSuccessListener { document ->
                    userCard = document.toObject(UserCard::class.java)?.copy(id = document.id)
                    // Tüm state değişkenlerini güncelle
                    userCard?.let { card ->
                        name = card.name
                        surname = card.surname
                        title = card.title
                        company = card.company
                        phone = card.phone
                        email = card.email
                        website = card.website
                        linkedin = card.linkedin
                        github = card.github
                        twitter = card.twitter
                        instagram = card.instagram
                        facebook = card.facebook
                        bio = card.bio
                        cv = card.cv
                    }
                    isLoading = false
                    
                    // Premium ise istatistikleri yükle
                    if (isPremium) {
                        loadCardStatistics(cardId, user.uid)
                    }
                }
                .addOnFailureListener { e ->
                    errorMessage = context.getString(R.string.card_load_error, e.localizedMessage)
                    showErrorMessage = true
                    isLoading = false
                }
        }
    }


    // Başarı mesajı gösterimi
    if (showSuccessMessage) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000)
            showSuccessMessage = false
        }
        Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
    }

    // Hata mesajı gösterimi
    if (showErrorMessage) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000)
            showErrorMessage = false
        }
        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
    }

    if (isLoading) {
        // Yükleniyor durumu
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(context.getString(R.string.loading_card))
            }
        }
        return
    }

    if (userCard == null) {
        // Hata durumu
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(id = R.drawable.info),
                    contentDescription = context.getString(R.string.error),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(context.getString(R.string.card_not_found), style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { onBackClick() }) {
                    Text(context.getString(R.string.go_back))
                }
            }
        }
        return
    }

    val cardType = CardType.entries.find { it.name == userCard?.cardType } ?: CardType.FREELANCE

    // Form validasyon state'leri
    var nameError by remember { mutableStateOf<String?>(null) }
    var surnameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }

    // Validasyon fonksiyonları
    fun validateName(value: String): String? {
        return when {
            value.isEmpty() -> context.getString(R.string.name_empty)
            value.length < 2 -> context.getString(R.string.name_min_length)
            else -> null
        }
    }

    fun validateSurname(value: String): String? {
        return when {
            value.isEmpty() -> context.getString(R.string.surname_empty)
            value.length < 2 -> context.getString(R.string.surname_min_length)
            else -> null
        }
    }

    fun validatePhone(value: String): String? {
        return when {
            value.isEmpty() -> context.getString(R.string.phone_empty)
            !android.util.Patterns.PHONE.matcher(value).matches() -> context.getString(R.string.phone_invalid)
            else -> null
        }
    }

    fun validateEmail(value: String): String? {
        return when {
            value.isEmpty() -> context.getString(R.string.email_empty)
            !android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches() -> context.getString(R.string.email_invalid)
            else -> null
        }
    }

    fun validateForm(): Boolean {
        nameError = validateName(name)
        surnameError = validateSurname(surname)
        phoneError = validatePhone(phone)
        emailError = validateEmail(email)

        return nameError == null && surnameError == null && phoneError == null && emailError == null
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(context.getString(R.string.delete_card)) },
            text = { Text(context.getString(R.string.confirm_delete)) },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        currentUser?.let { user ->
                            // Önce kart bilgilerini al (resim URL'si için)
                            firestore.collection("users")
                                .document(user.uid)
                                .collection("cards")
                                .document(cardId)
                                .get()
                                .addOnSuccessListener { cardDoc ->
                                    val profileImageUrl = cardDoc.getString("profileImageUrl") ?: ""
                                    
                                    // Kartı sil
                            firestore.collection("users")
                                .document(user.uid)
                                .collection("cards")
                                .document(cardId)
                                .delete()
                                .addOnSuccessListener {
                                    // Public kartlar koleksiyonundan da sil
                                    firestore.collection("public_cards")
                                        .document(cardId)
                                        .delete()
                                        .addOnSuccessListener {
                                                    // Resmi Storage'dan sil
                                                    if (profileImageUrl.isNotEmpty()) {
                                                        val storageManager = FirebaseStorageManager.getInstance()
                                                        CoroutineScope(Dispatchers.IO).launch {
                                                            storageManager.deleteCardImage(user.uid, profileImageUrl)
                                                            withContext(Dispatchers.Main) {
                                            isDeleting = false
                                            showDeleteDialog = false
                                            successMessage = context.getString(R.string.card_deleted)
                                            showSuccessMessage = true
                                            onBackClick()
                                                            }
                                                        }
                                                    } else {
                                                        isDeleting = false
                                                        showDeleteDialog = false
                                                        successMessage = context.getString(R.string.card_deleted)
                                                        showSuccessMessage = true
                                                        onBackClick()
                                                    }
                                        }
                                        .addOnFailureListener { e ->
                                            // Ana koleksiyondan silindi, ama public'ten silinemedi hatası
                                            isDeleting = false
                                            showDeleteDialog = false
                                            errorMessage = "Kartvizit silindi ancak dış kaynaktan silinemedi: ${e.localizedMessage}"
                                            showErrorMessage = true
                                            onBackClick()
                                        }
                                }
                                .addOnFailureListener { e ->
                                    isDeleting = false
                                    errorMessage = "Kartvizit silinirken hata oluştu: ${e.localizedMessage}"
                                            showErrorMessage = true
                                        }
                                }
                                .addOnFailureListener { e ->
                                    isDeleting = false
                                    errorMessage = "Kartvizit bilgileri alınırken hata oluştu: ${e.localizedMessage}"
                                    showErrorMessage = true
                                }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onError
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(context.getString(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(context.getString(R.string.cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(context.getString(R.string.card_detail)) },
                navigationIcon = {
                    IconButton(onClick = { onBackClick() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = context.getString(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }, enabled = !isSaving) {
                        Icon(Icons.Default.Delete, contentDescription = context.getString(R.string.delete), tint = MaterialTheme.colorScheme.error)
                    }
                    IconButton(onClick = {
                        if (isEditing) {
                            // Form validasyonu
                            if (validateForm()) {
                                // Kaydetme işlemi
                                isSaving = true
                                currentUser?.let { user ->
                                    val updatedCard = userCard?.copy(
                                        name = name,
                                        surname = surname,
                                        title = title,
                                        company = company,
                                        phone = phone,
                                        email = email,
                                        website = website,
                                        linkedin = linkedin,
                                        github = github,
                                        twitter = twitter,
                                        instagram = instagram,
                                        facebook = facebook,
                                        bio = bio,
                                        cv = cv
                                    )

                                    // Profil resmi yükleme
                                    if (selectedImageUri != null) {
                                        val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
                                        val imageRef = storageRef.child("profile_images/${user.uid}_${System.currentTimeMillis()}.jpg")
                                        
                                        imageRef.putFile(selectedImageUri!!)
                                            .addOnSuccessListener {
                                                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                                                    val cardWithImage = updatedCard?.copy(profileImageUrl = downloadUri.toString())
                                                    
                                                    cardWithImage?.let { card ->
                                                        firestore.collection("users")
                                                            .document(user.uid)
                                                            .collection("cards")
                                                            .document(cardId)
                                                            .set(card)
                                                            .addOnSuccessListener {
                                                                // Public cards koleksiyonunu da güncelle
                                                                val publicCardData = card.toMap().toMutableMap().apply {
                                                                    put("userId", user.uid)
                                                                    put("id", cardId)
                                                                    put("isPublic", card.isPublic)
                                                                }
                                                                
                                                                firestore.collection("public_cards")
                                                                    .document(cardId)
                                                                    .set(publicCardData)
                                                                    .addOnSuccessListener {
                                                                        userCard = card
                                                                        isSaving = false
                                                                        isEditing = false
                                                                        successMessage = context.getString(R.string.card_updated)
                                                                        showSuccessMessage = true
                                                                    }
                                                                    .addOnFailureListener { e ->
                                                                        isSaving = false
                                                                        errorMessage = context.getString(R.string.update_error, e.localizedMessage)
                                                                        showErrorMessage = true
                                                                    }
                                                            }
                                                            .addOnFailureListener { e ->
                                                                isSaving = false
                                                                errorMessage = context.getString(R.string.update_error, e.localizedMessage)
                                                                showErrorMessage = true
                                                            }
                                                    }
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                isSaving = false
                                                errorMessage = context.getString(R.string.profile_image_upload_error, e.localizedMessage)
                                                showErrorMessage = true
                                            }
                                    } else {
                                        updatedCard?.let { card ->
                                            firestore.collection("users")
                                                .document(user.uid)
                                                .collection("cards")
                                                .document(cardId)
                                                .set(card)
                                                .addOnSuccessListener {
                                                    // Public cards koleksiyonunu da güncelle
                                                    val publicCardData = card.toMap().toMutableMap().apply {
                                                        put("userId", user.uid)
                                                        put("id", cardId)
                                                        put("isPublic", card.isPublic)
                                                    }
                                                    
                                                    firestore.collection("public_cards")
                                                        .document(cardId)
                                                        .set(publicCardData)
                                                        .addOnSuccessListener {
                                                            userCard = card
                                                            isSaving = false
                                                            isEditing = false
                                                            successMessage = context.getString(R.string.card_updated)
                                                            showSuccessMessage = true
                                                        }
                                                        .addOnFailureListener { e ->
                                                            isSaving = false
                                                            errorMessage = context.getString(R.string.update_error, e.localizedMessage)
                                                            showErrorMessage = true
                                                        }
                                                }
                                                .addOnFailureListener { e ->
                                                    isSaving = false
                                                    errorMessage = "Kartvizit güncellenirken hata oluştu: ${e.localizedMessage}"
                                                    showErrorMessage = true
                                                }
                                        }
                                    }
                                }
                            }
                        } else {
                            isEditing = true
                        }
                    }, enabled = !isSaving) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Icon(
                                if (isEditing) Icons.Default.Done else Icons.Default.Edit,
                                contentDescription = if (isEditing) context.getString(R.string.save) else context.getString(R.string.edit)
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // İstatistik başlığı ve kartı (yalnızca premium)
            if (isPremium) {
                ExpandableStatisticsHeader(
                    isExpanded = showStatistics,
                    onToggle = { showStatistics = !showStatistics }
                ) {
                    StatisticsCard(
                        statistics = cardStatistics,
                        isLoading = isLoadingStats,
                        onRefresh = { loadCardStatistics(cardId, currentUser?.uid ?: "") }
                    )
                }
            } else {
                // Premium olmayanlar için bilgi kartı
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = context.getString(R.string.statistics),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = context.getString(R.string.statistics_premium_only),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(onClick = { 
                            // Profil ekranına yönlendirerek premium satın alma akışına götürebiliriz
                            navController.navigate(Screen.Profile.route)
                            {
                                popUpTo(Screen.Profile.route) { inclusive = true }}
                        }) {
                            Text(context.getString(R.string.get_premium))
                        }
                    }
                }
            }
            
            // Profil resmi
            Box(
                modifier = Modifier
                    .padding(vertical = 24.dp)
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .clickable(enabled = isEditing) {
                        if (isEditing) {
                            launcher.launch("image/*")
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(selectedImageUri)
                            .crossfade(true)
                            .size(Size.ORIGINAL)
                            .transformations(CircleCropTransformation())
                            .build(),
                            contentDescription = context.getString(R.string.profile_picture),
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else if (userCard?.profileImageUrl != null && userCard?.profileImageUrl?.isNotEmpty() == true) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(userCard?.profileImageUrl)
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
                    Icon(Icons.Default.AccountCircle, null, Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
                }

                if (isEditing) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(Color.Black.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = context.getString(R.string.change_image),
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            if (!isEditing) {
                InfoDisplayColumn(
                    name = name,
                    surname = surname,
                    title = title,
                    company = company,
                    phone = phone,
                    email = email,
                    website = website,
                    linkedin = linkedin,
                    github = github,
                    twitter = twitter,
                    instagram = instagram,
                    facebook = facebook,
                    cardType = cardType,
                    context = context,
                    bio = bio,
                    cv = cv,
                    isPremium = isPremium
                )
            } else {
                InfoEditColumn(
                    name = name,
                    surname = surname,
                    title = title,
                    company = company,
                    phone = phone,
                    email = email,
                    website = website,
                    linkedin = linkedin,
                    github = github,
                    twitter = twitter,
                    instagram = instagram,
                    facebook = facebook,
                    nameError = nameError,
                    surnameError = surnameError,
                    phoneError = phoneError,
                    emailError = emailError,
                    onNameChange = { 
                        name = it
                        nameError = validateName(it)
                    },
                    onSurnameChange = { 
                        surname = it
                        surnameError = validateSurname(it)
                    },
                    onTitleChange = { title = it },
                    onCompanyChange = { company = it },
                    onPhoneChange = { 
                        phone = it
                        phoneError = validatePhone(it)
                    },
                    onEmailChange = { 
                        email = it
                        emailError = validateEmail(it)
                    },
                    onWebsiteChange = { website = it },
                    onLinkedinChange = { linkedin = it },
                    onGithubChange = { github = it },
                    onTwitterChange = { twitter = it },
                    onInstagramChange = { instagram = it },
                    onFacebookChange = { facebook = it },
                    bio = bio,
                    cv = cv,
                    onBioChange = { bio = it },
                    onCvChange = { cv = it },
                    isPremium = isPremium
                )
            }
        }
    }
}

@Composable
fun InfoItem(iconRes: Int, text: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
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
fun SocialMediaIconButton(iconRes: Int, contentDescription: String, url: String, context: android.content.Context) {
    IconButton(onClick = { 
        try {
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
    context: android.content.Context,
    bio: String = "",
    cv: String = "",
    isPremium: Boolean = false
) {
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
            if (linkedin.isNotEmpty()) SocialMediaIconButton(R.drawable.linkedin, "LinkedIn", formatSocialUrl(linkedin, "linkedin.com"), context)
            if (github.isNotEmpty()) SocialMediaIconButton(R.drawable.github, "GitHub", formatSocialUrl(github, "github.com"), context)
            if (twitter.isNotEmpty()) SocialMediaIconButton(R.drawable.twitt, "Twitter", formatSocialUrl(twitter, "twitter.com"), context)
            if (instagram.isNotEmpty()) SocialMediaIconButton(R.drawable.insta, "Instagram", formatSocialUrl(instagram, "instagram.com"), context)
            if (facebook.isNotEmpty()) SocialMediaIconButton(R.drawable.face, "Facebook", formatSocialUrl(facebook, "facebook.com"), context)
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

// Sosyal medya URL'lerini düzenleme
fun formatSocialUrl(url: String, domain: String): String {
    return when {
        url.startsWith("http://") || url.startsWith("https://") -> url
        url.contains(domain) -> "https://$url"
        else -> "https://$domain/$url"
    }
}

@Composable
fun StatisticsCard(
    statistics: CardStatistics?,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Başlık ve Yenile butonu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                            text = context.getString(R.string.card_statistics),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                                        contentDescription = context.getString(R.string.refresh),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isLoading) {
                // Yükleniyor göstergesi
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                }
            } else if (statistics == null) {
                // İstatistik yok
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                        
                        Text(
                            text = context.getString(R.string.no_statistics),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // İstatistik özeti
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        title = context.getString(R.string.views),
                        value = "${statistics.totalViews}",
                        iconRes = R.drawable.eye,
                        modifier = Modifier.weight(1f)
                    )
                    
                    StatItem(
                        title = context.getString(R.string.unique_visitors),
                        value = "${statistics.uniqueViewers}",
                        iconRes = R.drawable.person,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        title = context.getString(R.string.link_clicks),
                        value = "${statistics.linkClicks}",
                        iconRes = R.drawable.web,
                        modifier = Modifier.weight(1f)
                    )
                    
                    StatItem(
                        title = context.getString(R.string.qr_scans),
                        value = "${statistics.qrScans}",
                        iconRes = R.drawable.qr_code,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Link tıklamaları detayı
                if (statistics.linkClicksByType.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = context.getString(R.string.most_clicked_links),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // En çok tıklanan 3 linki göster
                    statistics.linkClicksByType.entries
                        .sortedByDescending { it.value }
                        .take(3)
                        .forEach { (type, count) ->
                            val iconRes = when (type.lowercase()) {
                                "email" -> R.drawable.email
                                "phone" -> R.drawable.phone
                                "website" -> R.drawable.web
                                "linkedin" -> R.drawable.linkedin
                                "github" -> R.drawable.github
                                "twitter" -> R.drawable.twitt
                                "instagram" -> R.drawable.insta
                                "facebook" -> R.drawable.face
                                "cv" -> R.drawable.document
                                else -> R.drawable.web
                            }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = iconRes),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    Text(
                                        text = type.replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                
                                Text(
                                    text = "$count",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                }
            }
        }
    }
}

@Composable
fun ExpandableStatisticsHeader(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Başlık ve ok ikonu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                            text = context.getString(R.string.card_statistics),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) context.getString(R.string.collapse) else context.getString(R.string.expand),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            // Animasyonlu içerik
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun StatItem(
    title: String,
    value: String,
    iconRes: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

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
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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

@Composable
fun FormCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
            content()
        }
    }
}
