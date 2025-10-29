# Error Handling Guide - NeoCard

Bu kılavuz, NeoCard projesinde user-friendly error handling implementasyonunu açıklar.

## 📋 İçindekiler

1. [Genel Bakış](#genel-bakış)
2. [Resource.Error Yapısı](#resourceerror-yapısı)
3. [ErrorMapper Kullanımı](#errormapper-kullanımı)
4. [UI'da Hata Gösterimi](#uida-hata-gösterimi)
5. [ViewModel'de Kullanım](#viewmodelda-kullanım)
6. [Best Practices](#best-practices)

---

## 🎯 Genel Bakış

NeoCard projesi üç katmanlı error handling sistemi kullanır:

1. **Technical Message:** Debug ve loglama için teknik mesaj
2. **User Message:** Kullanıcıya gösterilen anlaşılır mesaj
3. **Error Metadata:** Retry edilebilir mi, başlık, vb.

### Temel Özellikler

- ✅ Otomatik Firebase exception mapping
- ✅ User-friendly Turkish messages
- ✅ Retry eligibility detection
- ✅ Flexible UI display options
- ✅ Context-aware error titles

---

## 📦 Resource.Error Yapısı

### Yeni Yapı

```kotlin
data class Error(
    val exception: Throwable,              // Orijinal exception
    val message: String?,                   // Teknik mesaj (debugging)
    val userMessage: String                 // Kullanıcıya gösterilecek mesaj
) : Resource<Nothing>()
```

### Kullanım

```kotlin
// Otomatik user message mapping
Resource.Error(
    exception = FirebaseFirestoreException(...),
    message = "Firestore error at line 42",
    userMessage = "Sunucu şu anda kullanılamıyor. Lütfen tekrar deneyin"
)

// Manuel user message
Resource.Error(
    exception = Exception("Network error"),
    message = "Connection timeout",
    userMessage = "İnternet bağlantınızı kontrol edin"
)
```

---

## 🗺️ ErrorMapper Kullanımı

### Exception'ı User Message'a Map Etme

```kotlin
val exception = FirebaseFirestoreException(Code.UNAVAILABLE)
val userMessage = ErrorMapper.getUserMessage(exception)
// Result: "Sunucu şu anda kullanılamıyor. Lütfen tekrar deneyin"
```

### Error Title Alma

```kotlin
val exception = FirebaseFirestoreException(Code.PERMISSION_DENIED)
val title = ErrorMapper.getErrorTitle(exception)
// Result: "Yetki Hatası"
```

### Retry Eligibility Kontrolü

```kotlin
val exception = UnknownHostException()
val canRetry = ErrorMapper.isRetryableError(exception)
// Result: true (network errors are retryable)
```

### Desteklenen Exception'lar

#### Firebase Firestore

| Exception Code | User Message | Retryable |
|----------------|--------------|-----------|
| UNAVAILABLE | "Sunucu şu anda kullanılamıyor..." | ✅ Yes |
| PERMISSION_DENIED | "Bu işlem için yetkiniz yok" | ❌ No |
| NOT_FOUND | "İstenen veri bulunamadı" | ❌ No |
| UNAUTHENTICATED | "Lütfen giriş yapın" | ❌ No |
| DEADLINE_EXCEEDED | "İşlem zaman aşımına uğradı" | ✅ Yes |
| ABORTED | "İşlem durduruldu..." | ✅ Yes |
| ALREADY_EXISTS | "Bu veri zaten mevcut" | ❌ No |
| INVALID_ARGUMENT | "Geçersiz parametre" | ❌ No |

#### Firebase Auth

| Error Code | User Message |
|------------|--------------|
| ERROR_INVALID_EMAIL | "Geçersiz e-posta adresi" |
| ERROR_WRONG_PASSWORD | "Hatalı şifre" |
| ERROR_USER_NOT_FOUND | "Kullanıcı bulunamadı" |
| ERROR_EMAIL_ALREADY_IN_USE | "Bu e-posta adresi zaten kullanımda" |
| ERROR_WEAK_PASSWORD | "Şifre çok zayıf..." |

#### Network Exceptions

| Exception | User Message | Retryable |
|-----------|--------------|-----------|
| UnknownHostException | "İnternet bağlantınızı kontrol edin" | ✅ Yes |
| SocketTimeoutException | "Bağlantı zaman aşımına uğradı..." | ✅ Yes |
| IOException | "Ağ bağlantısı hatası..." | ✅ Yes |

---

## 🎨 UI'da Hata Gösterimi

### 1. ErrorDisplay (Card Format)

Full-screen veya section için kart formatında hata gösterimi.

```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    when (val state = uiState) {
        is Resource.Error -> {
            ErrorDisplay(
                error = state,
                onRetry = { viewModel.loadData() }
            )
        }
        is Resource.Success -> {
            // Show content
        }
        is Resource.Loading -> {
            CircularProgressIndicator()
        }
    }
}
```

**Özellikler:**
- ✅ Error icon
- ✅ Error title
- ✅ User message
- ✅ Retry button (retryable errors için)

### 2. ErrorSnackbar

Geçici hata mesajları için.

```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel) {
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(uiState) {
        if (uiState is Resource.Error) {
            snackbarHostState.showSnackbar(
                message = (uiState as Resource.Error).userMessage,
                actionLabel = if (ErrorMapper.isRetryableError(
                    (uiState as Resource.Error).exception
                )) "Tekrar Dene" else null
            )
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        // Content
    }
}
```

### 3. ErrorAlertDialog

Önemli hatalar için modal dialog.

```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var showErrorDialog by remember { mutableStateOf(false) }
    
    when (val state = uiState) {
        is Resource.Error -> {
            ErrorAlertDialog(
                error = state,
                onDismiss = { 
                    showErrorDialog = false
                    viewModel.resetState()
                },
                onRetry = { viewModel.loadData() }
            )
        }
        else -> { /* ... */ }
    }
}
```

### 4. InlineErrorMessage

Form alanları için inline hata mesajı.

```kotlin
@Composable
fun MyFormField() {
    val fieldError: Resource.Error? = // ... from state
    
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = { /* ... */ },
            isError = fieldError != null
        )
        
        if (fieldError != null) {
            InlineErrorMessage(error = fieldError)
        }
    }
}
```

---

## 🎭 ViewModel'de Kullanım

### Doğru Kullanım ✅

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: MyRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<Resource<Data>>(Resource.Loading)
    val uiState: StateFlow<Resource<Data>> = _uiState
    
    fun loadData() {
        viewModelScope.launch {
            _uiState.value = Resource.Loading
            
            // safeApiCall otomatik olarak userMessage oluşturur
            when (val result = repository.getData()) {
                is Resource.Success -> {
                    _uiState.value = Resource.Success(result.data)
                }
                is Resource.Error -> {
                    // userMessage zaten set edilmiş
                    _uiState.value = result
                }
                is Resource.Loading -> {
                    // Already loading
                }
            }
        }
    }
}
```

### Manuel Error Oluşturma

Bazen custom error oluşturmanız gerekebilir:

```kotlin
fun validateAndSave() {
    if (name.isEmpty()) {
        _uiState.value = Resource.Error(
            exception = IllegalArgumentException("Name is empty"),
            message = "Validation failed: name is empty",
            userMessage = "Lütfen isim girin"
        )
        return
    }
    
    // Continue with save...
}
```

### Context-Dependent Messages

String resources kullanarak:

```kotlin
fun saveCard(context: Context) {
    viewModelScope.launch {
        when (val result = repository.saveCard()) {
            is Resource.Error -> {
                _uiState.value = Resource.Error(
                    exception = result.exception,
                    message = result.message,
                    userMessage = when (result.exception) {
                        is PremiumRequiredException -> 
                            context.getString(R.string.premium_required)
                        else -> result.userMessage
                    }
                )
            }
            // ...
        }
    }
}
```

---

## ✨ Best Practices

### 1. Her Zaman userMessage Kullan

```kotlin
// ✅ Good
Text(text = error.userMessage)

// ❌ Bad
Text(text = error.message ?: "Error")
Text(text = error.exception.message ?: "Error")
```

### 2. Retry Butonunu Akıllıca Göster

```kotlin
// ✅ Good: Check if retryable
if (ErrorMapper.isRetryableError(error.exception)) {
    Button(onClick = onRetry) {
        Text("Tekrar Dene")
    }
}

// ❌ Bad: Always show retry
Button(onClick = onRetry) {
    Text("Tekrar Dene")
}
```

### 3. Error Title Kullan

```kotlin
// ✅ Good: Descriptive title
AlertDialog(
    title = { Text(ErrorMapper.getErrorTitle(error.exception)) },
    text = { Text(error.userMessage) }
)

// ❌ Bad: Generic title
AlertDialog(
    title = { Text("Hata") },
    text = { Text(error.userMessage) }
)
```

### 4. Log Technical Messages

```kotlin
// ✅ Good: Log technical details
when (val result = repository.getData()) {
    is Resource.Error -> {
        Log.e("MyViewModel", "Load failed: ${result.message}", result.exception)
        _uiState.value = result // userMessage shown to user
    }
}
```

### 5. Reset Error State

```kotlin
// ✅ Good: Clear error after showing
fun dismissError() {
    _uiState.value = Resource.Success(defaultState)
}

// ❌ Bad: Error state persists
// No way to clear error
```

---

## 🧪 Testing

### ViewModel Tests

```kotlin
@Test
fun `error includes user message`() = runTest {
    // Given
    val exception = FirebaseFirestoreException(Code.UNAVAILABLE)
    coEvery { repository.getData() } returns Resource.Error(
        exception = exception,
        userMessage = "Sunucu şu anda kullanılamıyor. Lütfen tekrar deneyin"
    )
    
    // When
    viewModel.loadData()
    
    // Then
    val state = viewModel.uiState.value
    assertTrue(state is Resource.Error)
    assertEquals(
        "Sunucu şu anda kullanılamıyor. Lütfen tekrar deneyin",
        (state as Resource.Error).userMessage
    )
}
```

### ErrorMapper Tests

```kotlin
@Test
fun `maps network exception to user-friendly message`() {
    val exception = UnknownHostException()
    val message = ErrorMapper.getUserMessage(exception)
    
    assertEquals("İnternet bağlantınızı kontrol edin", message)
}
```

---

## 📊 Error Flow Diagram

```
┌─────────────────┐
│   API Call      │
└────────┬────────┘
         │
         ▼
    ┌────────┐
    │Success?│──Yes──► Resource.Success
    └────┬───┘
         │ No
         ▼
    ┌─────────────────────┐
    │ safeApiCall catches │
    │    exception        │
    └────────┬────────────┘
             │
             ▼
    ┌──────────────────────┐
    │ ErrorMapper.         │
    │ getUserMessage()     │
    └────────┬─────────────┘
             │
             ▼
    ┌──────────────────────┐
    │ Resource.Error       │
    │ - exception          │
    │ - message (tech)     │
    │ - userMessage (UI)   │
    └────────┬─────────────┘
             │
             ▼
    ┌──────────────────────┐
    │ UI Display           │
    │ Shows userMessage    │
    └──────────────────────┘
```

---

## 🎯 Migration Guide

### Updating Existing ViewModels

**Before:**
```kotlin
is Resource.Error -> {
    _uiState.value = Resource.Error(
        exception = result.exception,
        message = result.message
    )
}
```

**After:**
```kotlin
is Resource.Error -> {
    // userMessage is automatically set by safeApiCall
    _uiState.value = result
}
```

### Updating UI

**Before:**
```kotlin
Text(text = error.message ?: "Error occurred")
```

**After:**
```kotlin
Text(text = error.userMessage)
```

---

## 📚 Kaynaklar

- `ErrorMapper.kt` - Exception mapping logic
- `ErrorDisplay.kt` - UI components
- `Resource.kt` - Resource sealed class
- `ErrorMapperTest.kt` - Unit tests

---

*Error Handling Guide - NeoCard Project*

