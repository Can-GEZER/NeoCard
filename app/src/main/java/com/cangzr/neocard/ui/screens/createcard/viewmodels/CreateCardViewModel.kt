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
import com.cangzr.neocard.utils.ValidationResult
import com.cangzr.neocard.utils.ValidationUtils
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
    
    // Validation error states
    private val _nameError = MutableStateFlow<String?>(null)
    val nameError: StateFlow<String?> = _nameError
    
    private val _surnameError = MutableStateFlow<String?>(null)
    val surnameError: StateFlow<String?> = _surnameError
    
    private val _emailError = MutableStateFlow<String?>(null)
    val emailError: StateFlow<String?> = _emailError
    
    private val _phoneError = MutableStateFlow<String?>(null)
    val phoneError: StateFlow<String?> = _phoneError
    
    private val _websiteError = MutableStateFlow<String?>(null)
    val websiteError: StateFlow<String?> = _websiteError
    
    private val _linkedinError = MutableStateFlow<String?>(null)
    val linkedinError: StateFlow<String?> = _linkedinError
    
    private val _githubError = MutableStateFlow<String?>(null)
    val githubError: StateFlow<String?> = _githubError
    
    private val _twitterError = MutableStateFlow<String?>(null)
    val twitterError: StateFlow<String?> = _twitterError
    
    private val _instagramError = MutableStateFlow<String?>(null)
    val instagramError: StateFlow<String?> = _instagramError
    
    private val _facebookError = MutableStateFlow<String?>(null)
    val facebookError: StateFlow<String?> = _facebookError
    
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
        // Clear error when user starts typing
        _nameError.value = null
        // Validate immediately
        validateName()
    }
    
    fun updateSurname(value: String) {
        _surname.value = value
        _surnameError.value = null
        validateSurname()
    }
    
    fun updatePhone(value: String) {
        _phone.value = value
        _phoneError.value = null
        validatePhone()
    }
    
    fun updateEmail(value: String) {
        _email.value = value
        _emailError.value = null
        validateEmail()
    }
    
    fun updateCompany(value: String) {
        _company.value = value
    }
    
    fun updateTitle(value: String) {
        _title.value = value
    }
    
    fun updateWebsite(value: String) {
        _website.value = value
        _websiteError.value = null
        validateWebsite()
    }
    
    fun updateLinkedin(value: String) {
        _linkedin.value = value
        _linkedinError.value = null
        validateLinkedIn()
    }
    
    fun updateInstagram(value: String) {
        _instagram.value = value
        _instagramError.value = null
        validateInstagram()
    }
    
    fun updateTwitter(value: String) {
        _twitter.value = value
        _twitterError.value = null
        validateTwitter()
    }
    
    fun updateFacebook(value: String) {
        _facebook.value = value
        _facebookError.value = null
        validateFacebook()
    }
    
    fun updateGithub(value: String) {
        _github.value = value
        _githubError.value = null
        validateGitHub()
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
    
    // Validation methods
    private fun validateName() {
        val result = ValidationUtils.validateName(_name.value, isRequired = true)
        _nameError.value = result.getErrorOrNull()
    }
    
    private fun validateSurname() {
        val result = ValidationUtils.validateSurname(_surname.value, isRequired = false)
        _surnameError.value = result.getErrorOrNull()
    }
    
    private fun validateEmail() {
        val result = ValidationUtils.validateEmail(_email.value, isRequired = false)
        _emailError.value = result.getErrorOrNull()
    }
    
    private fun validatePhone() {
        val result = ValidationUtils.validatePhone(_phone.value, isRequired = false)
        _phoneError.value = result.getErrorOrNull()
    }
    
    private fun validateWebsite() {
        val result = ValidationUtils.validateWebsite(_website.value, isRequired = false)
        _websiteError.value = result.getErrorOrNull()
    }
    
    private fun validateLinkedIn() {
        val result = ValidationUtils.validateLinkedIn(_linkedin.value)
        _linkedinError.value = result.getErrorOrNull()
    }
    
    private fun validateGitHub() {
        val result = ValidationUtils.validateGitHub(_github.value)
        _githubError.value = result.getErrorOrNull()
    }
    
    private fun validateTwitter() {
        val result = ValidationUtils.validateTwitter(_twitter.value)
        _twitterError.value = result.getErrorOrNull()
    }
    
    private fun validateInstagram() {
        val result = ValidationUtils.validateInstagram(_instagram.value)
        _instagramError.value = result.getErrorOrNull()
    }
    
    private fun validateFacebook() {
        val result = ValidationUtils.validateFacebook(_facebook.value)
        _facebookError.value = result.getErrorOrNull()
    }
    
    /**
     * Validates all form fields
     * @return true if all fields are valid
     */
    private fun validateAllFields(): Boolean {
        validateName()
        validateSurname()
        validateEmail()
        validatePhone()
        validateWebsite()
        validateLinkedIn()
        validateGitHub()
        validateTwitter()
        validateInstagram()
        validateFacebook()
        
        return _nameError.value == null &&
                _surnameError.value == null &&
                _emailError.value == null &&
                _phoneError.value == null &&
                _websiteError.value == null &&
                _linkedinError.value == null &&
                _githubError.value == null &&
                _twitterError.value == null &&
                _instagramError.value == null &&
                _facebookError.value == null
    }
    
    fun saveCard(context: Context, onSuccess: () -> Unit) {
        // Validate all fields before saving
        if (!validateAllFields()) {
            _uiState.value = Resource.Error(
                exception = IllegalArgumentException("Validation failed"),
                message = "Form validation failed",
                userMessage = "Lütfen tüm alanları doğru şekilde doldurun"
            )
            return
        }
        
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
