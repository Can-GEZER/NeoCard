package com.cangzr.neocard.ui.screens.profile.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cangzr.neocard.data.repository.ReferralRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReferralViewModel @Inject constructor(
    private val referralRepository: ReferralRepository
) : ViewModel() {
    
    private val _referralCode = MutableStateFlow<String?>(null)
    val referralCode: StateFlow<String?> = _referralCode
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _referralCount = MutableStateFlow(0L)
    val referralCount: StateFlow<Long> = _referralCount
    
    fun loadReferralCode() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            
            when (val result = referralRepository.getReferralCode(currentUser.uid)) {
                is com.cangzr.neocard.common.Resource.Success -> {
                    _referralCode.value = result.data
                }
                is com.cangzr.neocard.common.Resource.Error -> {
                    when (val genResult = referralRepository.generateReferralCode(currentUser.uid)) {
                        is com.cangzr.neocard.common.Resource.Success -> {
                            _referralCode.value = genResult.data
                        }
                        else -> {}
                    }
                }
                else -> {}
            }
            
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { doc ->
                    _referralCount.value = doc.getLong("referralCount") ?: 0L
                }
            
            _isLoading.value = false
        }
    }
}

