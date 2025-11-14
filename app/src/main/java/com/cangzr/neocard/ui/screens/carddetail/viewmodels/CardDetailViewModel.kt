package com.cangzr.neocard.ui.screens.carddetail.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cangzr.neocard.R
import com.cangzr.neocard.analytics.CardAnalyticsManager
import com.cangzr.neocard.analytics.CardStatistics
import com.cangzr.neocard.data.model.UserCard
import com.cangzr.neocard.ui.screens.carddetail.utils.ValidationUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CardDetailViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {
    
    private val _userCard = MutableStateFlow<UserCard?>(null)
    val userCard: StateFlow<UserCard?> = _userCard
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving
    
    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting
    
    private val _cardStatistics = MutableStateFlow<CardStatistics?>(null)
    val cardStatistics: StateFlow<CardStatistics?> = _cardStatistics
    
    private val _isLoadingStats = MutableStateFlow(false)
    val isLoadingStats: StateFlow<Boolean> = _isLoadingStats
    
    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name
    
    private val _surname = MutableStateFlow("")
    val surname: StateFlow<String> = _surname
    
    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title
    
    private val _company = MutableStateFlow("")
    val company: StateFlow<String> = _company
    
    private val _phone = MutableStateFlow("")
    val phone: StateFlow<String> = _phone
    
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email
    
    private val _website = MutableStateFlow("")
    val website: StateFlow<String> = _website
    
    private val _linkedin = MutableStateFlow("")
    val linkedin: StateFlow<String> = _linkedin
    
    private val _github = MutableStateFlow("")
    val github: StateFlow<String> = _github
    
    private val _twitter = MutableStateFlow("")
    val twitter: StateFlow<String> = _twitter
    
    private val _instagram = MutableStateFlow("")
    val instagram: StateFlow<String> = _instagram
    
    private val _facebook = MutableStateFlow("")
    val facebook: StateFlow<String> = _facebook
    
    private val _bio = MutableStateFlow("")
    val bio: StateFlow<String> = _bio
    
    private val _cv = MutableStateFlow("")
    val cv: StateFlow<String> = _cv
    
    private val _nameError = MutableStateFlow<String?>(null)
    val nameError: StateFlow<String?> = _nameError
    
    private val _surnameError = MutableStateFlow<String?>(null)
    val surnameError: StateFlow<String?> = _surnameError
    
    private val _phoneError = MutableStateFlow<String?>(null)
    val phoneError: StateFlow<String?> = _phoneError
    
    private val _emailError = MutableStateFlow<String?>(null)
    val emailError: StateFlow<String?> = _emailError
    
    fun loadCard(cardId: String, context: Context) {
        _isLoading.value = true
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            firestore.collection("users")
                .document(user.uid)
                .collection("cards")
                .document(cardId)
                .get()
                .addOnSuccessListener { document ->
                    val card = document.toObject(UserCard::class.java)?.copy(id = document.id)
                    _userCard.value = card
                    
                    card?.let {
                        _name.value = it.name
                        _surname.value = it.surname
                        _title.value = it.title
                        _company.value = it.company
                        _phone.value = it.phone
                        _email.value = it.email
                        _website.value = it.website
                        _linkedin.value = it.linkedin
                        _github.value = it.github
                        _twitter.value = it.twitter
                        _instagram.value = it.instagram
                        _facebook.value = it.facebook
                        _bio.value = it.bio
                        _cv.value = it.cv
                    }
                    _isLoading.value = false
                }
                .addOnFailureListener { e ->
                    _isLoading.value = false
                }
        }
    }
    
    fun loadCardStatistics(cardId: String, context: Context) {
        _isLoadingStats.value = true
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            CardAnalyticsManager.getInstance().getCardStatistics(
                cardId = cardId,
                onSuccess = { stats ->
                    _cardStatistics.value = stats
                    _isLoadingStats.value = false
                },
                onError = { e ->
                    _isLoadingStats.value = false
                }
            )
        }
    }
    
    fun updateName(value: String, context: Context) {
        _name.value = value
        _nameError.value = ValidationUtils.validateName(value, context)
    }
    
    fun updateSurname(value: String, context: Context) {
        _surname.value = value
        _surnameError.value = ValidationUtils.validateSurname(value, context)
    }
    
    fun updatePhone(value: String, context: Context) {
        _phone.value = value
        _phoneError.value = ValidationUtils.validatePhone(value, context)
    }
    
    fun updateEmail(value: String, context: Context) {
        _email.value = value
        _emailError.value = ValidationUtils.validateEmail(value, context)
    }
    
    fun updateTitle(value: String) {
        _title.value = value
    }
    
    fun updateCompany(value: String) {
        _company.value = value
    }
    
    fun updateWebsite(value: String) {
        _website.value = value
    }
    
    fun updateLinkedin(value: String) {
        _linkedin.value = value
    }
    
    fun updateGithub(value: String) {
        _github.value = value
    }
    
    fun updateTwitter(value: String) {
        _twitter.value = value
    }
    
    fun updateInstagram(value: String) {
        _instagram.value = value
    }
    
    fun updateFacebook(value: String) {
        _facebook.value = value
    }
    
    fun updateBio(value: String) {
        _bio.value = value
    }
    
    fun updateCv(value: String) {
        _cv.value = value
    }
    
    fun validateForm(context: Context): Boolean {
        _nameError.value = ValidationUtils.validateName(_name.value, context)
        _surnameError.value = ValidationUtils.validateSurname(_surname.value, context)
        _phoneError.value = ValidationUtils.validatePhone(_phone.value, context)
        _emailError.value = ValidationUtils.validateEmail(_email.value, context)
        
        return _nameError.value == null && 
               _surnameError.value == null && 
               _phoneError.value == null && 
               _emailError.value == null
    }
    
    fun saveCard(cardId: String, context: Context, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (!validateForm(context)) {
            onError("Form validation failed")
            return
        }
        
        _isSaving.value = true
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val updatedCard = _userCard.value?.copy(
                name = _name.value,
                surname = _surname.value,
                title = _title.value,
                company = _company.value,
                phone = _phone.value,
                email = _email.value,
                website = _website.value,
                linkedin = _linkedin.value,
                github = _github.value,
                twitter = _twitter.value,
                instagram = _instagram.value,
                facebook = _facebook.value,
                bio = _bio.value,
                cv = _cv.value
            )
            
            updatedCard?.let { card ->
                firestore.collection("users")
                    .document(user.uid)
                    .collection("cards")
                    .document(cardId)
                    .set(card)
                    .addOnSuccessListener {
                        val publicCardData = card.toMap().toMutableMap().apply {
                            put("userId", user.uid)
                            put("id", cardId)
                            put("isPublic", card.isPublic)
                        }
                        
                        firestore.collection("public_cards")
                            .document(cardId)
                            .set(publicCardData)
                            .addOnSuccessListener {
                                _userCard.value = card
                                _isSaving.value = false
                                onSuccess()
                            }
                            .addOnFailureListener { e ->
                                _isSaving.value = false
                                onError(context.getString(R.string.update_error, e.localizedMessage))
                            }
                    }
                    .addOnFailureListener { e ->
                        _isSaving.value = false
                        onError(context.getString(R.string.update_error, e.localizedMessage))
                    }
            }
        }
    }
    
    fun deleteCard(cardId: String, context: Context, onSuccess: () -> Unit, onError: (String) -> Unit) {
        _isDeleting.value = true
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            firestore.collection("users")
                .document(user.uid)
                .collection("cards")
                .document(cardId)
                .get()
                .addOnSuccessListener { cardDoc ->
                    val profileImageUrl = cardDoc.getString("profileImageUrl") ?: ""
                    
                    firestore.collection("users")
                        .document(user.uid)
                        .collection("cards")
                        .document(cardId)
                        .delete()
                        .addOnSuccessListener {
                            firestore.collection("public_cards")
                                .document(cardId)
                                .delete()
                                .addOnSuccessListener {
                                    _isDeleting.value = false
                                    onSuccess()
                                }
                                .addOnFailureListener { e ->
                                    _isDeleting.value = false
                                    onError("Kartvizit silindi ancak dış kaynaktan silinemedi: ${e.localizedMessage}")
                                }
                        }
                        .addOnFailureListener { e ->
                            _isDeleting.value = false
                            onError("Kartvizit silinirken hata oluştu: ${e.localizedMessage}")
                        }
                }
                .addOnFailureListener { e ->
                    _isDeleting.value = false
                    onError("Kartvizit bilgileri alınırken hata oluştu: ${e.localizedMessage}")
                }
        }
    }
}
