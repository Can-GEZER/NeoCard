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

class AuthViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val uiState: StateFlow<AuthUiState> = _uiState

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    fun validateEmail(email: String): String? {
        return when {
            email.isBlank() -> "E-posta adresi boş olamaz"
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Geçerli bir e-posta adresi giriniz"
            else -> null
        }
    }

    fun validatePassword(password: String): String? {
        return when {
            password.length < 6 -> "Şifre en az 6 karakter olmalıdır"
            !password.any { it.isDigit() } -> "Şifre en az bir rakam içermelidir"
            !password.any { it.isLetter() } -> "Şifre en az bir harf içermelidir"
            else -> null
        }
    }

    fun validateName(name: String): String? {
        return when {
            name.isBlank() -> "Ad boş olamaz"
            name.length < 2 -> "Ad en az 2 karakter olmalıdır"
            else -> null
        }
    }

    fun validateSurname(surname: String): String? {
        return when {
            surname.isBlank() -> "Soyad boş olamaz"
            surname.length < 2 -> "Soyad en az 2 karakter olmalıdır"
            else -> null
        }
    }

    fun registerUser(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            try {
                _uiState.value = AuthUiState.Loading
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                
                authResult.user?.let { firebaseUser ->
                    val userData = hashMapOf(
                        "id" to firebaseUser.uid,
                        "email" to email,
                        "displayName" to displayName,
                        "premium" to false,
                        "connectRequests" to emptyList<String>(),
                        "connected" to emptyList<String>()
                    )

                    firestore.collection("users").document(firebaseUser.uid)
                        .set(userData)
                        .await()
                    
                    _uiState.value = AuthUiState.Success("Kayıt başarılı")
                }
            } catch (e: Exception) {
                val errorMessage = when (e.message) {
                    "The email address is already in use by another account." -> 
                        "Bu e-posta adresi zaten kullanımda"
                    "A network error (such as timeout, interrupted connection or unreachable host) has occurred." -> 
                        "İnternet bağlantınızı kontrol edin"
                    else -> "Kayıt sırasında bir hata oluştu: ${e.message}"
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
                _uiState.value = AuthUiState.Success("Giriş başarılı")
            } catch (e: Exception) {
                val errorMessage = when (e.message) {
                    "The password is invalid or the user does not have a password." -> 
                        "Hatalı şifre"
                    "There is no user record corresponding to this identifier." -> 
                        "Bu e-posta adresi ile kayıtlı kullanıcı bulunamadı"
                    "A network error (such as timeout, interrupted connection or unreachable host) has occurred." -> 
                        "İnternet bağlantınızı kontrol edin"
                    else -> "Giriş sırasında bir hata oluştu: ${e.message}"
                }
                _uiState.value = AuthUiState.Error(errorMessage)
            }
        }
    }

    // Google ile giriş işlemi
    fun signInWithGoogle(credential: GoogleSignInAccount) {
        viewModelScope.launch {
            try {
                _uiState.value = AuthUiState.Loading
                
                // Google kimlik bilgilerini Firebase kimlik doğrulama bilgilerine dönüştür
                val googleCredential = GoogleAuthProvider.getCredential(credential.idToken, null)
                
                // Firebase ile kimlik doğrulama yap
                val authResult = auth.signInWithCredential(googleCredential).await()
                
                // Kullanıcı bilgilerini Firestore'a kaydet veya güncelle
                authResult.user?.let { firebaseUser ->
                    // Kullanıcının Firestore'da olup olmadığını kontrol et
                    val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
                    
                    if (!userDoc.exists()) {
                        // Yeni kullanıcı, Firestore'a kaydet
                        val userData = hashMapOf(
                            "id" to firebaseUser.uid,
                            "email" to firebaseUser.email,
                            "displayName" to firebaseUser.displayName,
                            "premium" to false,
                            "connectRequests" to emptyList<String>(),
                            "connected" to emptyList<String>()
                        )

                        firestore.collection("users").document(firebaseUser.uid)
                            .set(userData)
                            .await()
                    }
                    
                    _uiState.value = AuthUiState.Success("Giriş başarılı")
                }
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is ApiException -> "Google ile giriş yapılamadı: ${e.statusCode}"
                    else -> "Giriş sırasında bir hata oluştu: ${e.message}"
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

// Google Sign-In istemcisini oluşturmak için yardımcı fonksiyon
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
    
    // Google Sign-In istemcisi
    val googleSignInClient = remember { getGoogleSignInClient(context) }
    
    // Google Sign-In hatası için state
    var signInError by remember { mutableStateOf<String?>(null) }
    
    // Google Sign-In için ActivityResult launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.let { viewModel.signInWithGoogle(it) }
        } catch (e: ApiException) {
            // Google Sign-In hatası
            signInError = "Google ile giriş yapılamadı: ${e.statusCode}"
        }
    }
    
    // Google Sign-In hatası varsa snackbar göster
    LaunchedEffect(signInError) {
        signInError?.let {
            snackbarHostState.showSnackbar(it)
            signInError = null
        }
    }

    // Observe UI state
    val uiState by viewModel.uiState.collectAsState()

    // Handle UI state changes
    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUiState.Success -> {
                snackbarHostState.showSnackbar((uiState as AuthUiState.Success).message)
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
                text = "NeoCard'a Hoş Geldiniz",
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = "Kartvizitlerinizi yönetmek için giriş yapın",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val signInIntent = googleSignInClient.signInIntent
                    launcher.launch(signInIntent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = uiState !is AuthUiState.Loading
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState is AuthUiState.Loading) {
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
                        Text("Google ile Giriş Yap")
                }
            }
            }

            Spacer(modifier = Modifier.height(16.dp))

                Text(
                text = "Google hesabınızla giriş yaparak, Gizlilik Politikası ve Kullanım Koşullarını kabul etmiş olursunuz.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
        }
    }
}
