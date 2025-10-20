package com.cangzr.neocard.ui.screens.createcard.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cangzr.neocard.R
import com.cangzr.neocard.data.CardType
import com.cangzr.neocard.ui.screens.createcard.utils.CardCreationUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.ui.graphics.toArgb

enum class TextType {
    NAME_SURNAME, TITLE, COMPANY, EMAIL, PHONE
}

data class TextStyle(
    var isBold: Boolean = false,
    var isItalic: Boolean = false,
    var isUnderlined: Boolean = false,
    var fontSize: Float = 16f,
    var color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Black
)

enum class BackgroundType {
    SOLID, GRADIENT
}

class CreateCardViewModel : ViewModel() {
    
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    // Form state
    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name
    
    private val _surname = MutableStateFlow("")
    val surname: StateFlow<String> = _surname
    
    private val _phone = MutableStateFlow("")
    val phone: StateFlow<String> = _phone
    
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email
    
    private val _company = MutableStateFlow("")
    val company: StateFlow<String> = _company
    
    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title
    
    private val _website = MutableStateFlow("")
    val website: StateFlow<String> = _website
    
    private val _linkedin = MutableStateFlow("")
    val linkedin: StateFlow<String> = _linkedin
    
    private val _instagram = MutableStateFlow("")
    val instagram: StateFlow<String> = _instagram
    
    private val _twitter = MutableStateFlow("")
    val twitter: StateFlow<String> = _twitter
    
    private val _facebook = MutableStateFlow("")
    val facebook: StateFlow<String> = _facebook
    
    private val _github = MutableStateFlow("")
    val github: StateFlow<String> = _github
    
    // Design state
    private val _backgroundColor = MutableStateFlow(androidx.compose.ui.graphics.Color.White)
    val backgroundColor: StateFlow<androidx.compose.ui.graphics.Color> = _backgroundColor
    
    private val _backgroundType = MutableStateFlow(BackgroundType.SOLID)
    val backgroundType: StateFlow<BackgroundType> = _backgroundType
    
    private val _selectedGradient = MutableStateFlow(Pair("Sunset", androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(androidx.compose.ui.graphics.Color(0xFFFE6B8B), androidx.compose.ui.graphics.Color(0xFFFF8E53)))))
    val selectedGradient: StateFlow<Pair<String, androidx.compose.ui.graphics.Brush>> = _selectedGradient
    
    private val _textStyles = MutableStateFlow(
        mapOf(
            TextType.NAME_SURNAME to TextStyle(fontSize = 18f),
            TextType.TITLE to TextStyle(fontSize = 16f),
            TextType.COMPANY to TextStyle(fontSize = 14f),
            TextType.EMAIL to TextStyle(fontSize = 14f),
            TextType.PHONE to TextStyle(fontSize = 14f)
        )
    )
    val textStyles: StateFlow<Map<TextType, TextStyle>> = _textStyles
    
    private val _selectedCardType = MutableStateFlow<CardType?>(null)
    val selectedCardType: StateFlow<CardType?> = _selectedCardType
    
    private val _profileImageUri = MutableStateFlow<Uri?>(null)
    val profileImageUri: StateFlow<Uri?> = _profileImageUri
    
    private val _selectedImageBitmap = MutableStateFlow<Bitmap?>(null)
    val selectedImageBitmap: StateFlow<Bitmap?> = _selectedImageBitmap
    
    private val _isPublic = MutableStateFlow(true)
    val isPublic: StateFlow<Boolean> = _isPublic
    
    // UI state
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _showPremiumDialog = MutableStateFlow(false)
    val showPremiumDialog: StateFlow<Boolean> = _showPremiumDialog
    
    init {
        checkPremiumStatus()
    }
    
    private fun checkPremiumStatus() {
        viewModelScope.launch {
            _isPremium.value = CardCreationUtils.isUserPremium()
        }
    }
    
    fun updateName(value: String) {
        _name.value = value
    }
    
    fun updateSurname(value: String) {
        _surname.value = value
    }
    
    fun updatePhone(value: String) {
        _phone.value = value
    }
    
    fun updateEmail(value: String) {
        _email.value = value
    }
    
    fun updateCompany(value: String) {
        _company.value = value
    }
    
    fun updateTitle(value: String) {
        _title.value = value
    }
    
    fun updateWebsite(value: String) {
        _website.value = value
    }
    
    fun updateLinkedin(value: String) {
        _linkedin.value = value
    }
    
    fun updateInstagram(value: String) {
        _instagram.value = value
    }
    
    fun updateTwitter(value: String) {
        _twitter.value = value
    }
    
    fun updateFacebook(value: String) {
        _facebook.value = value
    }
    
    fun updateGithub(value: String) {
        _github.value = value
    }
    
    fun updateBackgroundColor(color: androidx.compose.ui.graphics.Color) {
        _backgroundColor.value = color
    }
    
    fun updateBackgroundType(type: BackgroundType) {
        _backgroundType.value = type
    }
    
    fun updateSelectedGradient(gradient: Pair<String, androidx.compose.ui.graphics.Brush>) {
        _selectedGradient.value = gradient
    }
    
    fun updateTextStyle(textType: TextType, style: TextStyle) {
        val newStyles = _textStyles.value.toMutableMap()
        newStyles[textType] = style
        _textStyles.value = newStyles
    }
    
    fun updateSelectedCardType(type: CardType?) {
        _selectedCardType.value = type
    }
    
    fun updateProfileImageUri(uri: Uri?) {
        _profileImageUri.value = uri
    }
    
    fun updateSelectedImageBitmap(bitmap: Bitmap?) {
        _selectedImageBitmap.value = bitmap
    }
    
    fun updateIsPublic(isPublic: Boolean) {
        _isPublic.value = isPublic
    }
    
    fun showPremiumDialog() {
        _showPremiumDialog.value = true
    }
    
    fun hidePremiumDialog() {
        _showPremiumDialog.value = false
    }
    
    fun updateShowPremiumDialog(show: Boolean) {
        _showPremiumDialog.value = show
    }
    
    fun clearForm() {
        _name.value = ""
        _surname.value = ""
        _phone.value = ""
        _email.value = ""
        _company.value = ""
        _title.value = ""
        _website.value = ""
        _linkedin.value = ""
        _instagram.value = ""
        _twitter.value = ""
        _facebook.value = ""
        _github.value = ""
        _backgroundColor.value = androidx.compose.ui.graphics.Color.White
        _backgroundType.value = BackgroundType.SOLID
        _selectedGradient.value = Pair("Sunset", androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(androidx.compose.ui.graphics.Color(0xFFFE6B8B), androidx.compose.ui.graphics.Color(0xFFFF8E53))))
        _textStyles.value = mapOf(
            TextType.NAME_SURNAME to TextStyle(fontSize = 18f),
            TextType.TITLE to TextStyle(fontSize = 16f),
            TextType.COMPANY to TextStyle(fontSize = 14f),
            TextType.EMAIL to TextStyle(fontSize = 14f),
            TextType.PHONE to TextStyle(fontSize = 14f)
        )
        _profileImageUri.value = null
        _selectedImageBitmap.value = null
        _isPublic.value = true
    }
    
    fun saveCard(context: Context, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onError(context.getString(R.string.please_login))
            return
        }
        
        _isLoading.value = true
        val userDocRef = firestore.collection("users").document(currentUser.uid)
        
        viewModelScope.launch {
            try {
                val isPremiumUser = _isPremium.value
                
                if (!isPremiumUser) {
                    val cardCount = userDocRef.collection("cards").get().await().size()
                    if (cardCount >= 1) {
                        withContext(Dispatchers.Main) {
                            onError(context.getString(R.string.premium_card_limit))
                            _isLoading.value = false
                        }
                        return@launch
                    }
                }
                
                val cardData = hashMapOf(
                    "name" to _name.value,
                    "surname" to _surname.value,
                    "phone" to _phone.value,
                    "email" to _email.value,
                    "company" to _company.value,
                    "title" to _title.value,
                    "website" to _website.value,
                    "linkedin" to _linkedin.value,
                    "instagram" to _instagram.value,
                    "twitter" to _twitter.value,
                    "facebook" to _facebook.value,
                    "github" to _github.value,
                    "backgroundType" to _backgroundType.value.name,
                    "backgroundColor" to _backgroundColor.value.toArgb().toHexColor(),
                    "selectedGradient" to _selectedGradient.value.first,
                    "profileImageUrl" to "",
                    "cardType" to (_selectedCardType.value?.name ?: "Genel"),
                    "textStyles" to _textStyles.value.mapKeys { it.key.name }.mapValues { (_, style) ->
                        mapOf(
                            "isBold" to style.isBold,
                            "isItalic" to style.isItalic,
                            "isUnderlined" to style.isUnderlined,
                            "fontSize" to style.fontSize,
                            "color" to style.color.toArgb().toHexColor()
                        )
                    },
                    "isPublic" to _isPublic.value
                )
                
                // Profil fotoğrafı varsa yükle
                if (_profileImageUri.value != null) {
                    try {
                        val bitmap = CardCreationUtils.uriToBitmap(_profileImageUri.value!!, context)
                        if (bitmap != null) {
                            val maxSize = 800
                            val scaledBitmap = if (bitmap.width > maxSize || bitmap.height > maxSize) {
                                val ratio = minOf(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height)
                                val width = (bitmap.width * ratio).toInt()
                                val height = (bitmap.height * ratio).toInt()
                                Bitmap.createScaledBitmap(bitmap, width, height, true)
                            } else {
                                bitmap
                            }
                            
                            val baos = java.io.ByteArrayOutputStream()
                            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                            val imageData = baos.toByteArray()
                            
                            val filename = "profile_${currentUser.uid}_${System.currentTimeMillis()}.jpg"
                            val storageRef = FirebaseStorage.getInstance().reference
                                .child("user_uploads/${currentUser.uid}/$filename")
                            
                            val uploadTask = storageRef.putBytes(imageData).await()
                            val profileImageUrl = storageRef.downloadUrl.await().toString()
                            
                            cardData["profileImageUrl"] = profileImageUrl
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            _isLoading.value = false
                            onError(context.getString(R.string.image_upload_error, e.localizedMessage ?: context.getString(R.string.unknown_error)))
                        }
                        return@launch
                    }
                }
                
                // Kartı kaydet
                val cardDocRef = userDocRef.collection("cards").add(cardData).await()
                
                // Eğer kart herkese açık olarak işaretlendiyse, public_cards koleksiyonuna da ekle
                if (_isPublic.value) {
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
                
                withContext(Dispatchers.Main) {
                    clearForm()
                    _isLoading.value = false
                    onSuccess()
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(context.getString(R.string.error_occurred, e.localizedMessage))
                    _isLoading.value = false
                }
            }
        }
    }
    
    private fun Int.toHexColor(): String {
        return String.format("#%06X", 0xFFFFFF and this)
    }
}
