package com.cangzr.neocard.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cangzr.neocard.R
import com.cangzr.neocard.data.CardType
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.draw.scale
import android.content.Context

@Composable
fun SocialMediaIcon(iconRes: Int, contentDescription: String, tint: Color) {
    Icon(
        painter = painterResource(id = iconRes),
        contentDescription = contentDescription,
        modifier = Modifier.size(24.dp),
        tint = tint
    )
}

enum class TextType {
    NAME_SURNAME, TITLE, COMPANY, EMAIL, PHONE
}

data class TextStyle(
    var isBold: Boolean = false,
    var isItalic: Boolean = false,
    var isUnderlined: Boolean = false,
    var fontSize: Float = 16f,
    var color: Color = Color.Black
)

enum class BackgroundType {
    SOLID, GRADIENT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCardScreen(navController: NavController) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var linkedin by remember { mutableStateOf("") }
    var instagram by remember { mutableStateOf("") }
    var twitter by remember { mutableStateOf("") }
    var facebook by remember { mutableStateOf("") }
    var github by remember { mutableStateOf("") }
    var backgroundColor by remember { mutableStateOf(Color.White) }
    var backgroundType by remember { mutableStateOf(BackgroundType.SOLID) }
    var selectedGradient by remember { mutableStateOf(getPredefinedGradients(context).first()) }
    var selectedText by remember { mutableStateOf<TextType?>(null) }
    var textStyles by remember {
        mutableStateOf(
            mapOf(
                TextType.NAME_SURNAME to TextStyle(fontSize = 18f),
                TextType.TITLE to TextStyle(fontSize = 16f),
                TextType.COMPANY to TextStyle(fontSize = 14f),
                TextType.EMAIL to TextStyle(fontSize = 14f),
                TextType.PHONE to TextStyle(fontSize = 14f)
            )
        )
    }
    var selectedCardType by remember { mutableStateOf<CardType?>(null) }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var showImageOptions by remember { mutableStateOf(false) }
    var selectedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var isPremium by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isPublic by remember { mutableStateOf(true) }

    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    // Kullanıcının premium olup olmadığını kontrol et
    LaunchedEffect(Unit) {
        isPremium = isUserPremium()
    }

    fun Int.toHexColor(): String {
        return String.format("#%06X", 0xFFFFFF and this)
    }

    fun clearForm() {
        name = ""
        surname = ""
        phone = ""
        email = ""
        company = ""
        title = ""
        website = ""
        linkedin = ""
        instagram = ""
        twitter = ""
        facebook = ""
        github = ""
        backgroundColor = Color.White
        backgroundType = BackgroundType.SOLID
        selectedGradient = getPredefinedGradients(context).first()
        selectedText = null
        textStyles = mapOf(
            TextType.NAME_SURNAME to TextStyle(fontSize = 18f),
            TextType.TITLE to TextStyle(fontSize = 16f),
            TextType.COMPANY to TextStyle(fontSize = 14f),
            TextType.EMAIL to TextStyle(fontSize = 14f),
            TextType.PHONE to TextStyle(fontSize = 14f)
        )
        profileImageUri = null
        selectedImageBitmap = null
        isPublic = true
    }

    fun saveCard() {
        if (currentUser == null) {
            Toast.makeText(context, context.getString(R.string.please_login), Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true // Yükleme başladı
        val userDocRef = firestore.collection("users").document(currentUser.uid)

        // Coroutine scope içinde asenkron işlemleri yönet
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Premium kontrolü
                val isPremiumUser = isUserPremium()
                
                if (!isPremiumUser) {
                    // Premium olmayan kullanıcı kontrolü
                    val cardCount = userDocRef.collection("cards").get().await().size()
                    if (cardCount >= 1) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, context.getString(R.string.premium_card_limit), Toast.LENGTH_LONG).show()
                            isLoading = false
                        }
                        return@launch
                    }

                    // Premium olmayan kullanıcı kısıtlamaları
                    backgroundType = BackgroundType.SOLID
                    textStyles = mapOf(
                        TextType.NAME_SURNAME to TextStyle(fontSize = 18f),
                        TextType.TITLE to TextStyle(fontSize = 16f),
                        TextType.COMPANY to TextStyle(fontSize = 14f),
                        TextType.EMAIL to TextStyle(fontSize = 14f),
                        TextType.PHONE to TextStyle(fontSize = 14f)
                    )
                }

                // Profil fotoğrafı varsa yükle
                if (profileImageUri != null) {
                    try {
                        // Resmi küçült
                        val bitmap = if (Build.VERSION.SDK_INT < 28) {
                            MediaStore.Images.Media.getBitmap(context.contentResolver, profileImageUri)
                        } else {
                            val source = ImageDecoder.createSource(context.contentResolver, profileImageUri!!)
                            ImageDecoder.decodeBitmap(source)
                        }
                        
                        // Resmin boyutunu küçült (max 800x800)
                        val maxSize = 800
                        val scaledBitmap = if (bitmap.width > maxSize || bitmap.height > maxSize) {
                            val ratio = minOf(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height)
                            val width = (bitmap.width * ratio).toInt()
                            val height = (bitmap.height * ratio).toInt()
                            Bitmap.createScaledBitmap(bitmap, width, height, true)
                        } else {
                            bitmap
                        }
                        
                        // Bitmap'i JPEG formatına dönüştür
                        val baos = java.io.ByteArrayOutputStream()
                        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                        val imageData = baos.toByteArray()
                        
                        // Firebase'e yükle
                        val filename = "profile_${currentUser.uid}_${System.currentTimeMillis()}.jpg"
                        val storageRef = FirebaseStorage.getInstance().reference
                            .child("user_uploads/${currentUser.uid}/$filename")
                        
                        // Resmi yükle ve aşağıdaki işlemlere devam et
                        val uploadTask = storageRef.putBytes(imageData).await()
                        val profileImageUrl = storageRef.downloadUrl.await().toString()
                        
                        // Kart verilerini hazırla
                        val cardData = hashMapOf(
                            "name" to name,
                            "surname" to surname,
                            "phone" to phone,
                            "email" to email,
                            "company" to company,
                            "title" to title,
                            "website" to website,
                            "linkedin" to linkedin,
                            "instagram" to instagram,
                            "twitter" to twitter,
                            "facebook" to facebook,
                            "github" to github,
                            "backgroundType" to backgroundType.name,
                            "backgroundColor" to backgroundColor.toArgb().toHexColor(),
                            "selectedGradient" to selectedGradient.first,
                            "profileImageUrl" to profileImageUrl,
                            "cardType" to (selectedCardType?.name ?: "Genel"),
                            "textStyles" to textStyles.mapKeys { it.key.name }.mapValues { (_, style) ->
                                mapOf(
                                    "isBold" to style.isBold,
                                    "isItalic" to style.isItalic,
                                    "isUnderlined" to style.isUnderlined,
                                    "fontSize" to style.fontSize,
                                    "color" to style.color.toArgb().toHexColor()
                                )
                            },
                            "isPublic" to isPublic
                        )
        
                        // Kartı kaydet
                        userDocRef.collection("cards").add(cardData).await()
                            .also { cardDocRef ->
                                // Eğer kart herkese açık olarak işaretlendiyse, public_cards koleksiyonuna da ekle
                                if (isPublic) {
                                    val publicCardData = cardData.toMutableMap().apply {
                                        put("id", cardDocRef.id)
                                        put("userId", currentUser.uid)
                                        put("isPublic", true)
                                    }
                                    
                                    firestore.collection("public_cards")
                                        .document(cardDocRef.id)
                                        .set(publicCardData)
                                        .await()
                                }
                            }
        
                        // UI'ı güncelle
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, context.getString(R.string.card_saved), Toast.LENGTH_SHORT).show()
                            clearForm()
                            isLoading = false
                            navController.popBackStack() // Önceki sayfaya dön
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            isLoading = false
                            Toast.makeText(context, context.getString(R.string.image_upload_error, e.localizedMessage ?: context.getString(R.string.unknown_error)), Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    // Resim yoksa normal şekilde kartı kaydet
                    val cardData = hashMapOf(
                        "name" to name,
                        "surname" to surname,
                        "phone" to phone,
                        "email" to email,
                        "company" to company,
                        "title" to title,
                        "website" to website,
                        "linkedin" to linkedin,
                        "instagram" to instagram,
                        "twitter" to twitter,
                        "facebook" to facebook,
                        "github" to github,
                        "backgroundType" to backgroundType.name,
                        "backgroundColor" to backgroundColor.toArgb().toHexColor(),
                        "selectedGradient" to selectedGradient.first,
                        "profileImageUrl" to "",
                        "cardType" to (selectedCardType?.name ?: "Genel"),
                        "textStyles" to textStyles.mapKeys { it.key.name }.mapValues { (_, style) ->
                            mapOf(
                                "isBold" to style.isBold,
                                "isItalic" to style.isItalic,
                                "isUnderlined" to style.isUnderlined,
                                "fontSize" to style.fontSize,
                                "color" to style.color.toArgb().toHexColor()
                            )
                        },
                        "isPublic" to isPublic
                    )
    
                    // Kartı kaydet
                    userDocRef.collection("cards").add(cardData).await()
                        .also { cardDocRef ->
                            // Eğer kart herkese açık olarak işaretlendiyse, public_cards koleksiyonuna da ekle
                            if (isPublic) {
                                val publicCardData = cardData.toMutableMap().apply {
                                    put("id", cardDocRef.id)
                                    put("userId", currentUser.uid)
                                    put("isPublic", true)
                                }
                                
                                firestore.collection("public_cards")
                                    .document(cardDocRef.id)
                                    .set(publicCardData)
                                    .await()
                            }
                        }
    
                    // UI'ı güncelle
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.card_saved), Toast.LENGTH_SHORT).show()
                        clearForm()
                        isLoading = false
                        navController.popBackStack() // Önceki sayfaya dön
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.error_occurred, e.localizedMessage), Toast.LENGTH_LONG).show()
                    isLoading = false
                }
            }
        }
    }

    // Uri'den Bitmap oluşturma fonksiyonu
    fun uriToBitmap(uri: Uri) {
        try {
            selectedImageBitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Galeri launcher'ı
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            profileImageUri = it
            uriToBitmap(it)
            showEditDialog = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Başlık ve Kaydet Butonu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = context.getString(R.string.create_card),
                    style = MaterialTheme.typography.headlineSmall
                )

                Button(
                    onClick = { saveCard() },
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Text(context.getString(R.string.save))
                }
            }

            // Önizleme Alanı
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            when (backgroundType) {
                                BackgroundType.SOLID -> Modifier.background(backgroundColor)
                                BackgroundType.GRADIENT -> Modifier.background(selectedGradient.second)
                            }
                        )
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Profil Fotoğrafı
                            if (selectedImageBitmap != null || profileImageUri != null) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                        .clickable { if (profileImageUri != null) showImageOptions = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        selectedImageBitmap != null -> {
                                            Image(
                                                bitmap = selectedImageBitmap!!.asImageBitmap(),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                        profileImageUri != null -> {
                                            AsyncImage(
                                                model = profileImageUri,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                }
                            }

                            // Kişisel Bilgiler
                            Column {
                                Text(
                                    text = "$name $surname",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = textStyles[TextType.NAME_SURNAME]?.fontSize?.sp ?: 18.sp,
                                        fontWeight = if (textStyles[TextType.NAME_SURNAME]?.isBold == true) FontWeight.Bold else FontWeight.Normal,
                                        fontStyle = if (textStyles[TextType.NAME_SURNAME]?.isItalic == true) FontStyle.Italic else FontStyle.Normal,
                                        textDecoration = if (textStyles[TextType.NAME_SURNAME]?.isUnderlined == true) TextDecoration.Underline else TextDecoration.None
                                    ),
                                    color = textStyles[TextType.NAME_SURNAME]?.color ?: Color.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clickable { selectedText = TextType.NAME_SURNAME }
                                )
                                if (title.isNotEmpty()) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = textStyles[TextType.TITLE]?.fontSize?.sp ?: 16.sp,
                                            fontWeight = if (textStyles[TextType.TITLE]?.isBold == true) FontWeight.Bold else FontWeight.Normal,
                                            fontStyle = if (textStyles[TextType.TITLE]?.isItalic == true) FontStyle.Italic else FontStyle.Normal,
                                            textDecoration = if (textStyles[TextType.TITLE]?.isUnderlined == true) TextDecoration.Underline else TextDecoration.None
                                        ),
                                        color = textStyles[TextType.TITLE]?.color ?: Color.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.clickable { selectedText = TextType.TITLE }
                                    )
                                }
                                if (company.isNotEmpty()) {
                                    Text(
                                        text = company,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = textStyles[TextType.COMPANY]?.fontSize?.sp ?: 14.sp,
                                            fontWeight = if (textStyles[TextType.COMPANY]?.isBold == true) FontWeight.Bold else FontWeight.Normal,
                                            fontStyle = if (textStyles[TextType.COMPANY]?.isItalic == true) FontStyle.Italic else FontStyle.Normal,
                                            textDecoration = if (textStyles[TextType.COMPANY]?.isUnderlined == true) TextDecoration.Underline else TextDecoration.None
                                        ),
                                        color = textStyles[TextType.COMPANY]?.color ?: Color.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.clickable { selectedText = TextType.COMPANY }
                                    )
                                }
                                if (email.isNotEmpty()) {
                                    Text(
                                        text = email,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = textStyles[TextType.EMAIL]?.fontSize?.sp ?: 14.sp,
                                            fontWeight = if (textStyles[TextType.EMAIL]?.isBold == true) FontWeight.Bold else FontWeight.Normal,
                                            fontStyle = if (textStyles[TextType.EMAIL]?.isItalic == true) FontStyle.Italic else FontStyle.Normal,
                                            textDecoration = if (textStyles[TextType.EMAIL]?.isUnderlined == true) TextDecoration.Underline else TextDecoration.None
                                        ),
                                        color = textStyles[TextType.EMAIL]?.color ?: Color.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.clickable { selectedText = TextType.EMAIL }
                                    )
                                }
                                if (phone.isNotEmpty()) {
                                    Text(
                                        text = phone,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = textStyles[TextType.PHONE]?.fontSize?.sp ?: 14.sp,
                                            fontWeight = if (textStyles[TextType.PHONE]?.isBold == true) FontWeight.Bold else FontWeight.Normal,
                                            fontStyle = if (textStyles[TextType.PHONE]?.isItalic == true) FontStyle.Italic else FontStyle.Normal,
                                            textDecoration = if (textStyles[TextType.PHONE]?.isUnderlined == true) TextDecoration.Underline else TextDecoration.None
                                        ),
                                        color = textStyles[TextType.PHONE]?.color ?: Color.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.clickable { selectedText = TextType.PHONE }
                                    )
                                }
                            }
                        }

                        // Sosyal Medya İkonları
                        Row(
                            modifier = Modifier
                                .padding(start = 16.dp, top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (linkedin.isNotEmpty()) {
                                SocialMediaIcon(
                                    iconRes = R.drawable.linkedin,
                                    contentDescription = "LinkedIn",
                                    tint = textStyles[TextType.NAME_SURNAME]?.color ?: Color.Black
                                )
                            }
                            if (github.isNotEmpty()) {
                                SocialMediaIcon(
                                    iconRes = R.drawable.github,
                                    contentDescription = "GitHub",
                                    tint = textStyles[TextType.NAME_SURNAME]?.color ?: Color.Black
                                )
                            }
                            if (twitter.isNotEmpty()) {
                                SocialMediaIcon(
                                    iconRes = R.drawable.twitt,
                                    contentDescription = "Twitter (X)",
                                    tint = textStyles[TextType.NAME_SURNAME]?.color ?: Color.Black
                                )
                            }
                            if (instagram.isNotEmpty()) {
                                SocialMediaIcon(
                                    iconRes = R.drawable.insta,
                                    contentDescription = "Instagram",
                                    tint = textStyles[TextType.NAME_SURNAME]?.color ?: Color.Black
                                )
                            }
                            if (facebook.isNotEmpty()) {
                                SocialMediaIcon(
                                    iconRes = R.drawable.face,
                                    contentDescription = "Facebook",
                                    tint = textStyles[TextType.NAME_SURNAME]?.color ?: Color.Black
                                )
                            }
                            if (website.isNotEmpty()) {
                                SocialMediaIcon(
                                    iconRes = R.drawable.web,
                                    contentDescription = "Web",
                                    tint = textStyles[TextType.NAME_SURNAME]?.color ?: Color.Black
                                )
                            }
                        }
                    }
                }
            }

            // Premium avantajlarını gösteren dialog
            if (showPremiumDialog) {
                AlertDialog(
                    onDismissRequest = { showPremiumDialog = false },
                    title = {
                        Text(
                                            text = context.getString(R.string.premium_benefits),
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = context.getString(R.string.premium_advantages_description),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = context.getString(R.string.premium_unlimited_cards),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = context.getString(R.string.premium_gradient_backgrounds),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = context.getString(R.string.premium_custom_text_styles),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = context.getString(R.string.premium_ad_free),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showPremiumDialog = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(context.getString(R.string.ok))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { navController.navigate("profile") {
                                popUpTo("createcard") { inclusive = true }
                            } }
                        ) {
                            Text(context.getString(R.string.get_premium))
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Form Alanı
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profil Fotoğrafı
                FormCardContent(title = context.getString(R.string.profile_picture_logo)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.gallery),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(context.getString(R.string.choose_from_gallery))
                            }

                            if (profileImageUri != null) {
                                Button(
                                    onClick = {
                                        profileImageUri = null
                                        selectedImageBitmap = null
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(context.getString(R.string.remove_profile_picture))
                                }
                            }
                        }
                    }
                }

                // Kişisel Bilgiler Kartı
                FormCardContent(title = context.getString(R.string.personal_info)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(context.getString(R.string.name)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = surname,
                        onValueChange = { surname = it },
                        label = { Text(context.getString(R.string.surname)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // İletişim Bilgileri Kartı
                FormCardContent(title = context.getString(R.string.contact_info)) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(context.getString(R.string.phone)) },
                        leadingIcon = { Icon(Icons.Default.Phone, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(context.getString(R.string.email)) },
                        leadingIcon = { Icon(Icons.Default.Email, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // İş Bilgileri Kartı
                FormCardContent(title = context.getString(R.string.business_info)) {
                    OutlinedTextField(
                        value = company,
                        onValueChange = { company = it },
                        label = { Text(context.getString(R.string.company)) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.company),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(context.getString(R.string.title)) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.statue),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = website,
                        onValueChange = { website = it },
                        label = { Text(context.getString(R.string.website)) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.web),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Sosyal Medya Kartı
                FormCardContent(title = context.getString(R.string.social_media)) {
                    OutlinedTextField(
                        value = linkedin,
                        onValueChange = { linkedin = it },
                        label = { Text(context.getString(R.string.linkedin)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = github,
                        onValueChange = { github = it },
                        label = { Text(context.getString(R.string.github)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = twitter,
                        onValueChange = { twitter = it },
                        label = { Text(context.getString(R.string.twitter)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = instagram,
                        onValueChange = { instagram = it },
                        label = { Text(context.getString(R.string.instagram)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = facebook,
                        onValueChange = { facebook = it },
                        label = { Text(context.getString(R.string.facebook)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Tasarım Kartı
                FormCardContent(title = context.getString(R.string.design)) {
                    // Arkaplan Tipi Seçimi
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = backgroundType == BackgroundType.SOLID,
                            onClick = { backgroundType = BackgroundType.SOLID }, // SOLID her zaman seçilebilir
                            label = { Text(context.getString(R.string.solid_color)) },
                            enabled = true // SOLID her zaman aktif
                        )
                        FilterChip(
                            selected = backgroundType == BackgroundType.GRADIENT,
                            onClick = { if (isPremium) backgroundType = BackgroundType.GRADIENT }, // GRADYAN sadece premium için
                            label = { Text(context.getString(R.string.gradient)) },
                            enabled = isPremium // GRADYAN sadece premium kullanıcılar için aktif
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    when (backgroundType) {
                        BackgroundType.SOLID -> {
                            Text(context.getString(R.string.background_color))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                val colors = listOf(
                                    Color.White to context.getString(R.string.color_white),
                                    Color.Black to context.getString(R.string.color_black),
                                    Color(0xFFFFD700) to context.getString(R.string.color_gold),
                                    Color(0xFF40E0D0) to context.getString(R.string.color_turquoise),
                                    Color(0xFF4CAF50) to context.getString(R.string.color_green),
                                    Color(0xFFFF9800) to context.getString(R.string.color_orange),
                                    Color(0xFF2196F3) to context.getString(R.string.color_blue),
                                    Color(0xFFE91E63) to context.getString(R.string.color_pink),
                                    Color(0xFF9C27B0) to context.getString(R.string.color_purple),
                                    Color(0xFF795548) to context.getString(R.string.color_brown),
                                    Color(0xFF607D8B) to context.getString(R.string.color_gray),
                                    Color(0xFFF44336) to context.getString(R.string.color_red)
                                )
                                items(colors) { (color, name) ->
                                    ColorButton(
                                        color = color,
                                        selectedColor = backgroundColor,
                                        name = name,
                                        onColorSelected = { backgroundColor = it } // Renk seçimi her zaman aktif
                                    )
                                }
                            }
                        }
                        BackgroundType.GRADIENT -> {
                            Text(context.getString(R.string.select_gradient))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                items(getPredefinedGradients(context)) { (name, brush) ->
                                    GradientButton(
                                        name = name,
                                        brush = brush,
                                        isSelected = selectedGradient.first == name,
                                        onSelect = { if (isPremium) selectedGradient = Pair(name, brush) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Yazı Stilleri Kartı
                if (isPremium) {
                    FormCardContent(title = context.getString(R.string.text_styles)) {
                        if (selectedText != null) {
                            val style = textStyles[selectedText] ?: return@FormCardContent

                            // Stil seçenekleri
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = style.isBold,
                                    onClick = {
                                        textStyles = textStyles.toMutableMap().apply {
                                            this[selectedText!!] = style.copy(isBold = !style.isBold)
                                        }
                                    },
                                    label = { Text("B", fontWeight = FontWeight.Bold) }
                                )
                                FilterChip(
                                    selected = style.isItalic,
                                    onClick = {
                                        textStyles = textStyles.toMutableMap().apply {
                                            this[selectedText!!] = style.copy(isItalic = !style.isItalic)
                                        }
                                    },
                                    label = { Text("I", fontStyle = FontStyle.Italic) }
                                )
                                FilterChip(
                                    selected = style.isUnderlined,
                                    onClick = {
                                        textStyles = textStyles.toMutableMap().apply {
                                            this[selectedText!!] = style.copy(isUnderlined = !style.isUnderlined)
                                        }
                                    },
                                    label = { Text("U", textDecoration = TextDecoration.Underline) }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Font boyutu Slider
                                Text(context.getString(R.string.font_size, style.fontSize.toInt()))
                            Slider(
                                value = style.fontSize,
                                onValueChange = { newSize ->
                                    textStyles = textStyles.toMutableMap().apply {
                                        this[selectedText!!] = style.copy(fontSize = newSize)
                                    }
                                },
                                valueRange = 12f..24f,
                                steps = 11,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Renk seçici
                            Text(context.getString(R.string.text_color))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                val colors = listOf(
                                    Color.White to context.getString(R.string.color_white),
                                    Color.Black to context.getString(R.string.color_black),
                                    Color(0xFFFFD700) to context.getString(R.string.color_gold),
                                    Color(0xFF40E0D0) to context.getString(R.string.color_turquoise),
                                    Color(0xFF4CAF50) to context.getString(R.string.color_green),
                                    Color(0xFFFF9800) to context.getString(R.string.color_orange),
                                    Color(0xFF2196F3) to context.getString(R.string.color_blue),
                                    Color(0xFFE91E63) to context.getString(R.string.color_pink),
                                    Color(0xFF9C27B0) to context.getString(R.string.color_purple),
                                    Color(0xFF795548) to context.getString(R.string.color_brown),
                                    Color(0xFF607D8B) to context.getString(R.string.color_gray),
                                    Color(0xFFF44336) to context.getString(R.string.color_red)
                                )
                                items(colors) { (color, name) ->
                                    ColorButton(
                                        color = color,
                                        selectedColor = style.color,
                                        name = name,
                                        onColorSelected = { newColor ->
                                            textStyles = textStyles.toMutableMap().apply {
                                                this[selectedText!!] = style.copy(color = newColor)
                                            }
                                        }
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = context.getString(R.string.no_text_selected),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Kart Tipi Seçimi
                CardTypeSelector(
                    selectedType = selectedCardType,
                    onTypeSelected = { selectedCardType = it }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Gizlilik Ayarları
                FormCardWithSwitch(
                    title = context.getString(R.string.visibility),
                    isChecked = isPublic,
                    onCheckedChange = { isPublic = it }
                )
                
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Yükleme göstergesi
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .width(200.dp)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                                text = context.getString(R.string.saving_card),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
    // Premium değilse küçük bir bilgi kartı göster
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        if (!isPremium) {
            PremiumInfoCard(onClick = { showPremiumDialog = true })
        }
    }
}

@Composable
fun PremiumInfoCard(onClick: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                painter = painterResource(id = R.drawable.info), // Bilgi ikonu
                contentDescription = context.getString(R.string.premium_benefits),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = context.getString(R.string.get_premium),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun FormCardContent(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            content()
        }
    }
}

@Composable
private fun FormCardWithSwitch(
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.search),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isChecked) context.getString(R.string.on) else context.getString(R.string.off),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Switch(
                        checked = isChecked,
                        onCheckedChange = onCheckedChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.scale(0.8f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Seçime bağlı açıklama metni
            Text(
                text = if (isChecked) 
                    context.getString(R.string.card_visible_to_all) 
                else 
                    context.getString(R.string.card_visible_to_shared),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 28.dp, top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun ColorButton(
    color: Color,
    selectedColor: Color,
    name: String,
    onColorSelected: (Color) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(50.dp)
    ) {
        Button(
            onClick = { onColorSelected(color) },
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = color),
            border = if (color == selectedColor) {
                ButtonDefaults.outlinedButtonBorder
            } else null
        ) { }
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
fun GradientButton(
    name: String,
    brush: Brush,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(brush)
                .clickable(onClick = onSelect)
                .then(
                    if (isSelected) {
                        Modifier.border(
                            2.dp,
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(8.dp)
                        )
                    } else Modifier
                )
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

// Önceden tanımlı gradyanlar
fun getPredefinedGradients(context: Context) = listOf(
    Pair(
        context.getString(R.string.gradient_sunset),
        Brush.horizontalGradient(listOf(Color(0xFFFE6B8B), Color(0xFFFF8E53)))
    ),
    Pair(
        context.getString(R.string.gradient_ocean),
        Brush.horizontalGradient(listOf(Color(0xFF2196F3), Color(0xFF00BCD4)))
    ),
    Pair(
        context.getString(R.string.gradient_forest),
        Brush.horizontalGradient(listOf(Color(0xFF4CAF50), Color(0xFF8BC34A)))
    ),
    Pair(
        context.getString(R.string.gradient_night),
        Brush.verticalGradient(listOf(Color(0xFF2C3E50), Color(0xFF3498DB)))
    ),
    Pair(
        context.getString(R.string.gradient_purple_mist),
        Brush.verticalGradient(listOf(Color(0xFF9C27B0), Color(0xFFE91E63)))
    )
)

@Composable
private fun CardTypeSelector(
    selectedType: CardType?,
    onTypeSelected: (CardType) -> Unit
) {
    val context = LocalContext.current
                FormCardContent(title = context.getString(R.string.card_type)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = context.getString(R.string.select_card_purpose),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(CardType.entries.toTypedArray()) { type ->
                    Card(
                        modifier = Modifier
                            .width(110.dp)
                            .height(90.dp)
                            .clickable { onTypeSelected(type) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedType == type) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = type.getIcon()),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = if (selectedType == type) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = type.getTitle(),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selectedType == type) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// Kullanıcının premium olup olmadığını kontrol eden fonksiyon
private suspend fun isUserPremium(): Boolean {
    val currentUser = FirebaseAuth.getInstance().currentUser
    return if (currentUser != null) {
        val userDocRef = FirebaseFirestore.getInstance().collection("users").document(currentUser.uid)
        val document = userDocRef.get().await()
        document.getBoolean("premium") ?: false
    } else {
        false
    }
}