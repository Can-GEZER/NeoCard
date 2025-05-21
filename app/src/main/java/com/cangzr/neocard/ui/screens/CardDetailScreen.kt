import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.cangzr.neocard.R
import com.cangzr.neocard.data.CardType
import com.cangzr.neocard.ui.screens.UserCard
import com.cangzr.neocard.billing.BillingManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import coil.size.Size
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.draw.scale
import com.cangzr.neocard.storage.FirebaseStorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    cardId: String,
    onBackClick: () -> Unit
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
                }
                .addOnFailureListener { e ->
                    errorMessage = "Kartvizit yüklenirken hata oluştu: ${e.localizedMessage}"
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
                Text("Kartvizit yükleniyor...")
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
                    contentDescription = "Hata",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Kartvizit bulunamadı", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onBackClick) {
                    Text("Geri Dön")
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
            value.isEmpty() -> "Ad alanı boş olamaz"
            value.length < 2 -> "Ad en az 2 karakter olmalıdır"
            else -> null
        }
    }

    fun validateSurname(value: String): String? {
        return when {
            value.isEmpty() -> "Soyad alanı boş olamaz"
            value.length < 2 -> "Soyad en az 2 karakter olmalıdır"
            else -> null
        }
    }

    fun validatePhone(value: String): String? {
        return when {
            value.isEmpty() -> "Telefon alanı boş olamaz"
            !android.util.Patterns.PHONE.matcher(value).matches() -> "Geçerli bir telefon numarası giriniz"
            else -> null
        }
    }

    fun validateEmail(value: String): String? {
        return when {
            value.isEmpty() -> "E-posta alanı boş olamaz"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches() -> "Geçerli bir e-posta adresi giriniz"
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
            title = { Text("Kartviziti Sil") },
            text = { Text("Bu kartviziti silmek istediğinizden emin misiniz?") },
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
                                            successMessage = "Kartvizit başarıyla silindi"
                                            showSuccessMessage = true
                                            onBackClick()
                                                            }
                                                        }
                                                    } else {
                                                        isDeleting = false
                                                        showDeleteDialog = false
                                                        successMessage = "Kartvizit başarıyla silindi"
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
                    Text("Sil")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("İptal") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kartvizit Detayı") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }, enabled = !isSaving) {
                        Icon(Icons.Default.Delete, "Sil", tint = MaterialTheme.colorScheme.error)
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
                                                                        successMessage = "Kartvizit başarıyla güncellendi"
                                                                        showSuccessMessage = true
                                                                    }
                                                                    .addOnFailureListener { e ->
                                                                        isSaving = false
                                                                        errorMessage = "Kartvizit güncellenirken hata oluştu: ${e.localizedMessage}"
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
                                            .addOnFailureListener { e ->
                                                isSaving = false
                                                errorMessage = "Profil resmi yüklenirken hata oluştu: ${e.localizedMessage}"
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
                                                            successMessage = "Kartvizit başarıyla güncellendi"
                                                            showSuccessMessage = true
                                                        }
                                                        .addOnFailureListener { e ->
                                                            isSaving = false
                                                            errorMessage = "Kartvizit güncellenirken hata oluştu: ${e.localizedMessage}"
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
                                if (isEditing) "Kaydet" else "Düzenle"
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
                        contentDescription = "Profil Resmi",
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
                        contentDescription = "Profil Resmi",
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
                            contentDescription = "Resim Değiştir",
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
            Toast.makeText(context, "Link açılamadı: $url", Toast.LENGTH_SHORT).show()
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
                        text = "Biyografi",
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
                Toast.makeText(context, "E-posta uygulaması açılamadı", Toast.LENGTH_SHORT).show()
            }
        }
        
        InfoItem(R.drawable.phone, phone) {
            try {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phone")
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Telefon uygulaması açılamadı", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(context, "Web sitesi açılamadı", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        if (cv.isNotEmpty()) {
            InfoItem(R.drawable.document, "CV'mi Görüntüle") {
                try {
                    var cvUrl = cv
                    if (!cvUrl.startsWith("http://") && !cvUrl.startsWith("https://")) {
                        cvUrl = "https://$cvUrl"
                    }
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cvUrl))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "CV linki açılamadı", Toast.LENGTH_SHORT).show()
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
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FormCard("Kişisel Bilgiler") {
            OutlinedTextField(
                value = name, 
                onValueChange = onNameChange, 
                label = { Text("Ad") },
                isError = nameError != null,
                supportingText = { nameError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = surname, 
                onValueChange = onSurnameChange, 
                label = { Text("Soyad") },
                isError = surnameError != null,
                supportingText = { surnameError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            if (isPremium) {
                OutlinedTextField(
                    value = bio, 
                    onValueChange = onBioChange, 
                    label = { Text("Biyografi") },
                    placeholder = { Text("Kısa bir biyografi yazın...") },
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
                            text = "Biyografi özelliği sadece premium üyelere özeldir",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
        FormCard("İş Bilgileri") {
            OutlinedTextField(
                value = title, 
                onValueChange = onTitleChange, 
                label = { Text("Ünvan") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = company, 
                onValueChange = onCompanyChange, 
                label = { Text("Şirket") },
                modifier = Modifier.fillMaxWidth()
            )
            if (isPremium) {
                OutlinedTextField(
                    value = cv, 
                    onValueChange = onCvChange, 
                    label = { Text("CV Linki") },
                    placeholder = { Text("CV'nize ulaşılabilecek bir bağlantı") },
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
                            text = "CV özelliği sadece premium üyelere özeldir",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
        FormCard("İletişim Bilgileri") {
            OutlinedTextField(
                value = phone, 
                onValueChange = onPhoneChange, 
                label = { Text("Telefon") },
                isError = phoneError != null,
                supportingText = { phoneError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = email, 
                onValueChange = onEmailChange, 
                label = { Text("E-posta") },
                isError = emailError != null,
                supportingText = { emailError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = website, 
                onValueChange = onWebsiteChange, 
                label = { Text("Web Sitesi") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        FormCard("Sosyal Medya") {
            OutlinedTextField(
                value = linkedin, 
                onValueChange = onLinkedinChange, 
                label = { Text("LinkedIn") },
                placeholder = { Text("kullanıcıadı veya tam URL") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = github, 
                onValueChange = onGithubChange, 
                label = { Text("GitHub") },
                placeholder = { Text("kullanıcıadı veya tam URL") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = twitter, 
                onValueChange = onTwitterChange, 
                label = { Text("Twitter") },
                placeholder = { Text("kullanıcıadı veya tam URL") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = instagram, 
                onValueChange = onInstagramChange, 
                label = { Text("Instagram") },
                placeholder = { Text("kullanıcıadı veya tam URL") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = facebook, 
                onValueChange = onFacebookChange, 
                label = { Text("Facebook") },
                placeholder = { Text("kullanıcıadı veya tam URL") },
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
