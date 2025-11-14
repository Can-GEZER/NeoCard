package com.cangzr.neocard.ui.screens.profile.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.cangzr.neocard.data.model.PromoCode
import com.cangzr.neocard.data.model.User
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor() : ViewModel() {
    var isPremium by mutableStateOf(false)
        private set
    
    var hasConnectRequests by mutableStateOf(false)
        private set
    
    var promoCodeList by mutableStateOf<List<PromoCode>>(emptyList())
        private set
    
    var promoCode by mutableStateOf("")
        private set
    
    var showPromoDialog by mutableStateOf(false)
        private set
    
    var userDisplayName by mutableStateOf("")
        private set
    
    var userPhotoUrl by mutableStateOf<String?>(null)
        private set
    
    var currentUser by mutableStateOf<FirebaseUser?>(null)
        private set
    
    var connectionRequests by mutableStateOf<List<Map<String, String>>>(emptyList())
        private set
    
    var userMap by mutableStateOf<Map<String, User>>(emptyMap())
        private set
    
    var isLoading by mutableStateOf(true)
        private set
    
    var selectedOption by mutableStateOf<String?>(null)
        private set
    
    var showDeleteDialog by mutableStateOf(false)
        private set
    
    var showLanguageDialog by mutableStateOf(false)
        private set
    
    var selectedLanguage by mutableStateOf("")
        private set
    
    var showPremiumSheet by mutableStateOf(false)
        private set
    
    var showAddPromoDialog by mutableStateOf(false)
        private set
    
    var newPromoCode by mutableStateOf("")
        private set
    
    var usageLimit by mutableStateOf("10")
        private set
    
    var expandedCodeId by mutableStateOf<String?>(null)
        private set
    
    fun updatePremiumStatus(isPremium: Boolean) {
        this.isPremium = isPremium
    }
    
    fun updateConnectRequestsStatus(hasRequests: Boolean) {
        this.hasConnectRequests = hasRequests
    }
    
    fun updatePromoCodeList(codes: List<PromoCode>) {
        this.promoCodeList = codes
    }
    
    fun updatePromoCode(code: String) {
        this.promoCode = code
    }
    
    fun showPromoCodeDialog(show: Boolean) {
        this.showPromoDialog = show
    }
    
    fun updateUserProfile(displayName: String, photoUrl: String?) {
        this.userDisplayName = displayName
        this.userPhotoUrl = photoUrl
    }
    
    fun updateCurrentUser(user: FirebaseUser?) {
        this.currentUser = user
    }
    
    fun updateConnectionRequests(requests: List<Map<String, String>>) {
        this.connectionRequests = requests
    }
    
    fun updateUserMap(userMap: Map<String, User>) {
        this.userMap = userMap
    }
    
    fun updateLoadingState(loading: Boolean) {
        this.isLoading = loading
    }
    
    fun updateSelectedOption(option: String?) {
        this.selectedOption = option
    }
    
    fun showDeleteAccountDialog(show: Boolean) {
        this.showDeleteDialog = show
    }
    
    fun showLanguageDialog(show: Boolean) {
        this.showLanguageDialog = show
    }
    
    fun updateSelectedLanguage(language: String) {
        this.selectedLanguage = language
    }
    
    fun showPremiumSheet(show: Boolean) {
        this.showPremiumSheet = show
    }
    
    fun showAddPromoDialog(show: Boolean) {
        this.showAddPromoDialog = show
    }
    
    fun updateNewPromoCode(code: String) {
        this.newPromoCode = code
    }
    
    fun updateUsageLimit(limit: String) {
        this.usageLimit = limit
    }
    
    fun updateExpandedCodeId(codeId: String?) {
        this.expandedCodeId = codeId
    }
    
    fun resetPromoCodeForm() {
        this.newPromoCode = ""
        this.usageLimit = "10"
    }
}
