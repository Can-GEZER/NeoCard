package com.cangzr.neocard.ui.screens

import android.content.Context
import android.content.Intent
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.graphics.ColorFilter
import com.cangzr.neocard.R
import com.cangzr.neocard.utils.ReferralCodeManager
import com.cangzr.neocard.data.repository.ReferralRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val referralRepository: ReferralRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val uiState: StateFlow<AuthUiState> = _uiState

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    fun validateEmail(email: String): String? {
        val context = FirebaseAuth.getInstance().app.applicationContext
        return when {
            email.isBlank() -> context.getString(R.string.email_empty)
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> context.getString(R.string.email_invalid)
            else -> null
        }
    }

    fun validatePassword(password: String): String? {
        val context = FirebaseAuth.getInstance().app.applicationContext
        return when {
            password.length < 6 -> context.getString(R.string.password_min_length)
            !password.any { it.isDigit() } -> context.getString(R.string.password_digit)
            !password.any { it.isLetter() } -> context.getString(R.string.password_letter)
            else -> null
        }
    }

    fun validateName(name: String): String? {
        val context = FirebaseAuth.getInstance().app.applicationContext
        return when {
            name.isBlank() -> context.getString(R.string.name_empty)
            name.length < 2 -> context.getString(R.string.name_min_length)
            else -> null
        }
    }

    fun validateSurname(surname: String): String? {
        val context = FirebaseAuth.getInstance().app.applicationContext
        return when {
            surname.isBlank() -> context.getString(R.string.surname_empty)
            surname.length < 2 -> context.getString(R.string.surname_min_length)
            else -> null
        }
    }

    fun registerUser(email: String, password: String, displayName: String, context: Context) {
        viewModelScope.launch {
            try {
                _uiState.value = AuthUiState.Loading
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                
                authResult.user?.let { firebaseUser ->
                    val referralCodeResult = referralRepository.generateReferralCode(firebaseUser.uid)
                    val referralCode = if (referralCodeResult is com.cangzr.neocard.common.Resource.Success) {
                        referralCodeResult.data
                    } else {
                        null
                    }

                    val userData = hashMapOf(
                        "id" to firebaseUser.uid,
                        "email" to email,
                        "displayName" to displayName,
                        "premium" to false,
                        "connectRequests" to emptyList<String>(),
                        "connected" to emptyList<String>(),
                        "referralCount" to 0L
                    )

                    referralCode?.let {
                        userData["referralCode"] = it
                    }

                    firestore.collection("users").document(firebaseUser.uid)
                        .set(userData)
                        .await()
                    
                    val pendingReferralCode = ReferralCodeManager.getAndClearReferralCode(context)
                    if (pendingReferralCode != null && pendingReferralCode != referralCode) {
                        val referrerIdResult = referralRepository.validateReferralCode(pendingReferralCode)
                        if (referrerIdResult is com.cangzr.neocard.common.Resource.Success && referrerIdResult.data != null) {
                            val referrerId = referrerIdResult.data!!
                            if (referrerId != firebaseUser.uid) {
                                val isReferredResult = referralRepository.isUserReferred(firebaseUser.uid)
                                if (isReferredResult is com.cangzr.neocard.common.Resource.Success && !isReferredResult.data) {
                                    referralRepository.createReferral(
                                        referrerId = referrerId,
                                        referredId = firebaseUser.uid,
                                        referralCode = pendingReferralCode
                                    )
                                }
                            }
                        }
                    }
                    
                    _uiState.value = AuthUiState.Success(context.getString(R.string.registration_successful))
                }
            } catch (e: Exception) {
                val errorMessage = when (e.message) {
                    "The email address is already in use by another account." -> 
                        context.getString(R.string.email_already_in_use)
                    "A network error (such as timeout, interrupted connection or unreachable host) has occurred." -> 
                        context.getString(R.string.network_error)
                    else -> context.getString(R.string.registration_error, e.message)
                }
                _uiState.value = AuthUiState.Error(errorMessage)
            }
        }
    }

    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            try {
                _uiState.value = AuthUiState.Loading
                auth.signInWithEmailAndPassword(email, password).await()
                val context = FirebaseAuth.getInstance().app.applicationContext
                _uiState.value = AuthUiState.Success(context.getString(R.string.login_successful))
            } catch (e: Exception) {
                val context = FirebaseAuth.getInstance().app.applicationContext
                val errorMessage = when (e.message) {
                    "The password is invalid or the user does not have a password." -> 
                        context.getString(R.string.invalid_password)
                    "There is no user record corresponding to this identifier." -> 
                        context.getString(R.string.user_not_found)
                    "A network error (such as timeout, interrupted connection or unreachable host) has occurred." -> 
                        context.getString(R.string.network_error)
                    else -> context.getString(R.string.login_error, e.message)
                }
                _uiState.value = AuthUiState.Error(errorMessage)
            }
        }
    }

    fun signInWithGoogle(credential: GoogleSignInAccount, context: Context) {
        viewModelScope.launch {
            try {
                _uiState.value = AuthUiState.Loading
                
                val googleCredential = GoogleAuthProvider.getCredential(credential.idToken, null)
                
                val authResult = auth.signInWithCredential(googleCredential).await()
                
                authResult.user?.let { firebaseUser ->
                    val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
                    
                    if (!userDoc.exists()) {
                        val referralCodeResult = referralRepository.generateReferralCode(firebaseUser.uid)
                        val referralCode = if (referralCodeResult is com.cangzr.neocard.common.Resource.Success) {
                            referralCodeResult.data
                        } else {
                            null
                        }

                        val userData = hashMapOf(
                            "id" to firebaseUser.uid,
                            "email" to firebaseUser.email,
                            "displayName" to firebaseUser.displayName,
                            "premium" to false,
                            "connectRequests" to emptyList<String>(),
                            "connected" to emptyList<String>(),
                            "referralCount" to 0L
                        )

                        referralCode?.let {
                            userData["referralCode"] = it
                        }

                        firestore.collection("users").document(firebaseUser.uid)
                            .set(userData)
                            .await()
                        
                        val pendingReferralCode = ReferralCodeManager.getAndClearReferralCode(context)
                        if (pendingReferralCode != null && pendingReferralCode != referralCode) {
                            val referrerIdResult = referralRepository.validateReferralCode(pendingReferralCode)
                            if (referrerIdResult is com.cangzr.neocard.common.Resource.Success && referrerIdResult.data != null) {
                                val referrerId = referrerIdResult.data!!
                                if (referrerId != firebaseUser.uid) {
                                    val isReferredResult = referralRepository.isUserReferred(firebaseUser.uid)
                                    if (isReferredResult is com.cangzr.neocard.common.Resource.Success && !isReferredResult.data) {
                                        referralRepository.createReferral(
                                            referrerId = referrerId,
                                            referredId = firebaseUser.uid,
                                            referralCode = pendingReferralCode
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    _uiState.value = AuthUiState.Success(context.getString(R.string.login_successful))
                }
            } catch (e: Exception) {
                val context = FirebaseAuth.getInstance().app.applicationContext
                val errorMessage = when (e) {
                    is ApiException -> context.getString(R.string.google_signin_error, e.statusCode)
                    else -> context.getString(R.string.login_error, e.message)
                }
                _uiState.value = AuthUiState.Error(errorMessage)
            }
        }
    }
}

sealed class AuthUiState {
    object Initial : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val message: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

fun getGoogleSignInClient(context: Context): GoogleSignInClient {
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(R.string.default_web_client_id))
        .requestEmail()
        .build()
    
    return GoogleSignIn.getClient(context, gso)
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    navController: NavController,
    viewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    val googleSignInClient = remember { getGoogleSignInClient(context) }
    
    var signInError by remember { mutableStateOf<String?>(null) }
    var isGoogleSignInLoading by remember { mutableStateOf(false) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isGoogleSignInLoading = false
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.let { viewModel.signInWithGoogle(it, context) }
        } catch (e: ApiException) {
            signInError = context.getString(R.string.google_signin_error, e.statusCode)
        }
    }
    
    LaunchedEffect(signInError) {
        signInError?.let {
            snackbarHostState.showSnackbar(it)
            signInError = null
        }
    }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUiState.Success -> {
                navController.navigate("home") {
                    popUpTo("auth") { inclusive = true }
                }
            }
            is AuthUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as AuthUiState.Error).message)
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.logo3),
                contentDescription = "NeoCard Logo",
                modifier = Modifier
                    .size(150.dp)
                    .semantics { contentDescription = "NeoCard Logo" },
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = context.getString(R.string.welcome_to_neocard),
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = context.getString(R.string.login_to_manage_cards),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    isGoogleSignInLoading = true
                    val signInIntent = googleSignInClient.signInIntent
                    launcher.launch(signInIntent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = uiState !is AuthUiState.Loading && !isGoogleSignInLoading
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState is AuthUiState.Loading || isGoogleSignInLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                        Image(
                            painter = painterResource(id = R.drawable.google_logo),
                            contentDescription = "Google Logo",
                            modifier = Modifier.size(24.dp)
                        )
                        Text(context.getString(R.string.sign_in_with_google))
                }
            }
            }

            Spacer(modifier = Modifier.height(16.dp))

                Text(
                text = context.getString(R.string.privacy_policy_agreement),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
        }
    }
}
