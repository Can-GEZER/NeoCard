package com.cangzr.neocard.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cangzr.neocard.utils.NetworkUtils
import com.cangzr.neocard.utils.OnboardingPreferences
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val auth: FirebaseAuth
) : ViewModel() {
    
    private val _splashState = MutableStateFlow<SplashState>(SplashState.Loading)
    val splashState: StateFlow<SplashState> = _splashState
    
    fun checkAppStartState(
        isNetworkAvailable: Boolean,
        isOnboardingCompleted: Boolean
    ) {
        viewModelScope.launch {
            if (!isNetworkAvailable) {
                _splashState.value = SplashState.NetworkError
                return@launch
            }
            
            val currentUser = auth.currentUser
            
            _splashState.value = when {
                currentUser == null && !isOnboardingCompleted -> {
                    SplashState.NavigateToOnboarding
                }
                currentUser == null && isOnboardingCompleted -> {
                    SplashState.NavigateToAuth
                }
                else -> {
                    SplashState.NavigateToHome
                }
            }
        }
    }
}

sealed class SplashState {
    object Loading : SplashState()
    object NetworkError : SplashState()
    object NavigateToOnboarding : SplashState()
    object NavigateToAuth : SplashState()
    object NavigateToHome : SplashState()
}
