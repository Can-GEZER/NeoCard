package com.cangzr.neocard.ui.screens.createcard.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cangzr.neocard.R
import com.cangzr.neocard.common.Resource
import com.cangzr.neocard.data.CardType
import com.cangzr.neocard.data.model.TextStyleDTO
import com.cangzr.neocard.data.model.UserCard
import com.cangzr.neocard.data.repository.AuthRepository
import com.cangzr.neocard.domain.usecase.GetUserCardsUseCase
import com.cangzr.neocard.domain.usecase.SaveCardUseCase
import com.cangzr.neocard.ui.screens.createcard.utils.CardCreationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.ui.graphics.toArgb
import javax.inject.Inject

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

data class CreateCardUiState(
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class CreateCardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val saveCardUseCase: SaveCardUseCase,
    private val getUserCardsUseCase: GetUserCardsUseCase
) : ViewModel() {
    
    // UI State for save operation
    private val _uiState = MutableStateFlow<Resource<CreateCardUiState>>(Resource.Success(CreateCardUiState()))
    val uiState: StateFlow<Resource<CreateCardUiState>> = _uiState
    
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
    
    fun saveCard(context: Context, onSuccess: () -> Unit) {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            _uiState.value = Resource.Error(
                exception = Exception("User not logged in"),
                message = context.getString(R.string.please_login)
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = Resource.Loading
            
            try {
                val isPremiumUser = _isPremium.value
                
                    // Premium kontrolü
                if (!isPremiumUser) {
                        when (val result = getUserCardsUseCase(
                            userId = currentUser.uid,
                            pageSize = 1,
                            lastCardId = null
                        )) {
                            is Resource.Success -> {
                                val (cards, _, _) = result.data
                                if (cards.isNotEmpty()) {
                                    _uiState.value = Resource.Error(
                                        exception = Exception("Premium required"),
                                        message = context.getString(R.string.premium_card_limit)
                                    )
                                    return@launch
                                }
                            }
                            is Resource.Error -> {
                                _uiState.value = Resource.Error(
                                    exception = result.exception,
                                    message = context.getString(R.string.error_occurred, result.message)
                                )
                                return@launch
                            }
                            is Resource.Loading -> {
                                // Continue
                            }
                        }
                    }
                
                // UI state'den UserCard oluştur
                val card = UserCard(
                    id = "",
                    name = _name.value,
                    surname = _surname.value,
                    phone = _phone.value,
                    email = _email.value,
                    company = _company.value,
                    title = _title.value,
                    website = _website.value,
                    linkedin = _linkedin.value,
                    instagram = _instagram.value,
                    twitter = _twitter.value,
                    facebook = _facebook.value,
                    github = _github.value,
                    bio = "",
                    cv = "",
                    backgroundType = _backgroundType.value.name,
                    backgroundColor = _backgroundColor.value.toArgb().toHexColor(),
                    selectedGradient = _selectedGradient.value.first,
                    profileImageUrl = "",
                    cardType = _selectedCardType.value?.name ?: "Genel",
                    textStyles = _textStyles.value.mapKeys { it.key.name }.mapValues { (_, style) ->
                        TextStyleDTO(
                            isBold = style.isBold,
                            isItalic = style.isItalic,
                            isUnderlined = style.isUnderlined,
                            fontSize = style.fontSize,
                            color = style.color.toArgb().toHexColor()
                        )
                    },
                    isPublic = _isPublic.value
                )
                
                        // UseCase ile kartı kaydet
                        when (val result = saveCardUseCase(
                            userId = currentUser.uid,
                            card = card,
                            imageUri = _profileImageUri.value
                        )) {
                            is Resource.Success -> {
                                clearForm()
                                _uiState.value = Resource.Success(CreateCardUiState(isSaved = true))
                                onSuccess()
                            }
                            is Resource.Error -> {
                                _uiState.value = Resource.Error(
                                    exception = result.exception,
                                    message = context.getString(R.string.error_occurred, result.message)
                                )
                            }
                            is Resource.Loading -> {
                                // Loading already handled
                            }
                }
                
            } catch (e: Exception) {
                _uiState.value = Resource.Error(
                    exception = e,
                    message = context.getString(R.string.error_occurred, e.localizedMessage)
                )
            }
        }
    }
    
    fun resetState() {
        _uiState.value = Resource.Success(CreateCardUiState())
    }
    
    private fun Int.toHexColor(): String {
        return String.format("#%06X", 0xFFFFFF and this)
    }
}
