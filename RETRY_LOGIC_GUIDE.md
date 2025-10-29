# Retry Logic Guide - NeoCard

Bu döküman, NeoCard projesindeki `safeApiCall` fonksiyonunun retry mekanizmasını açıklar.

## 📋 İçindekiler

1. [Genel Bakış](#genel-bakış)
2. [Retry Stratejisi](#retry-stratejisi)
3. [Exponential Backoff](#exponential-backoff)
4. [Hangi Hatalar Retry Edilir](#hangi-hatalar-retry-edilir)
5. [Kullanım Örnekleri](#kullanım-örnekleri)
6. [Test Coverage](#test-coverage)
7. [Best Practices](#best-practices)

---

## 🎯 Genel Bakış

`safeApiCall` fonksiyonu, Firebase Firestore API çağrılarını güvenli bir şekilde yapar ve geçici ağ hatalarında otomatik olarak retry mekanizması uygular.

### Temel Özellikler

- ✅ **Otomatik Retry:** Geçici hatalar için otomatik yeniden deneme
- ✅ **Exponential Backoff:** Artan bekleme süreleri
- ✅ **Non-blocking:** Dispatchers.IO kullanarak main thread'i bloklamaz
- ✅ **Akıllı Filtreleme:** Sadece retry edilebilir hataları dener
- ✅ **Maximum Limit:** 3 retry ile sınırlı (toplam 4 deneme)

---

## 🔄 Retry Stratejisi

### Retry Parametreleri

```kotlin
val maxRetries = 3                           // Maksimum retry sayısı
val retryDelays = listOf(500L, 1000L, 2000L) // Exponential backoff delays (ms)
```

### Deneme Sırası

| Deneme | Önceki Bekleme | Toplam Süre |
|--------|----------------|-------------|
| 1      | 0ms            | 0ms         |
| 2      | 500ms          | 500ms       |
| 3      | 1000ms         | 1500ms      |
| 4      | 2000ms         | 3500ms      |

### Akış Diyagramı

```
┌─────────────────┐
│  API Call       │
└────────┬────────┘
         │
         ▼
    ┌────────┐
    │Success?│──Yes──► Return Success
    └────┬───┘
         │ No
         ▼
    ┌─────────────┐
    │ Retryable?  │──No──► Return Error
    └────┬────────┘
         │ Yes
         ▼
    ┌──────────────┐
    │ Max Retries? │──Yes──► Return Error
    └────┬─────────┘
         │ No
         ▼
    ┌──────────────┐
    │ Wait (delay) │
    └────┬─────────┘
         │
         └──► Retry API Call
```

---

## 📈 Exponential Backoff

### Nedir?

Exponential backoff, her başarısız denemeden sonra bekleme süresini artırarak sunucu yükünü azaltır ve başarı şansını artırır.

### Uygulama

```kotlin
// Attempt 1: Immediate
// Attempt 2: 500ms delay
delay(retryDelays[0])  // 500ms

// Attempt 3: 1000ms delay
delay(retryDelays[1])  // 1000ms

// Attempt 4: 2000ms delay
delay(retryDelays[2])  // 2000ms
```

### Avantajları

1. **Sunucu Koruma:** Sunucuyu aşırı isteklerden korur
2. **Başarı Şansı:** Geçici sorunların çözülmesi için zaman tanır
3. **Verimlilik:** Gereksiz hızlı retry'lerden kaçınır
4. **Ağ Dostu:** Ağ kaynaklarını verimli kullanır

---

## 🎯 Hangi Hatalar Retry Edilir

### Retry Edilir ✅

#### FirebaseFirestoreException.Code.UNAVAILABLE

```kotlin
is FirebaseFirestoreException -> {
    e.code == FirebaseFirestoreException.Code.UNAVAILABLE
}
```

**Sebep:** Geçici bir ağ sorunu veya sunucu yoğunluğu.

**Örnek Senaryolar:**
- İnternet bağlantısı kesintisi
- Firestore sunucu bakımı
- Geçici ağ yoğunluğu
- Rate limiting

### Retry Edilmez ❌

#### Diğer FirebaseFirestoreException Kodları

- `PERMISSION_DENIED` - Yetki hatası (kullanıcı yetkisi yok)
- `NOT_FOUND` - Doküman bulunamadı
- `ALREADY_EXISTS` - Doküman zaten var
- `INVALID_ARGUMENT` - Geçersiz parametre
- `DEADLINE_EXCEEDED` - Timeout
- `RESOURCE_EXHAUSTED` - Kota aşıldı

**Sebep:** Bu hatalar kalıcıdır, retry yapmak faydasızdır.

#### Generic Exceptions

```kotlin
else -> false  // İllegalArgumentException, NullPointerException, vb.
```

**Sebep:** Uygulama mantığı hataları, retry edilmemeli.

---

## 💡 Kullanım Örnekleri

### Temel Kullanım

```kotlin
override suspend fun getCards(
    userId: String,
    pageSize: Int,
    lastCardId: String?
): Resource<Triple<List<UserCard>, String?, Boolean>> = safeApiCall {
    val query = firestore.collection("users")
        .document(userId)
        .collection("cards")
        .limit(pageSize.toLong())
        .get()
        .await()
    
    // Parse and return data
    Triple(cards, lastId, hasMore)
}
```

### Otomatik Retry

API çağrısı başarısız olursa (UNAVAILABLE), `safeApiCall` otomatik olarak:

1. Hatanın retry edilebilir olup olmadığını kontrol eder
2. Exponential backoff ile bekler
3. API çağrısını tekrar dener
4. Maksimum 3 kez retry yapar
5. Başarısız olursa `Resource.Error` döner

### Başarı Senaryosu

```kotlin
// 1. Deneme: UNAVAILABLE (500ms bekle)
// 2. Deneme: UNAVAILABLE (1000ms bekle)
// 3. Deneme: SUCCESS ✅

val result = safeApiCall { getDataFromFirestore() }
// result = Resource.Success(data)
```

### Hata Senaryosu (Retry Edilemez)

```kotlin
// 1. Deneme: PERMISSION_DENIED ❌
// Hemen hata döner, retry yapmaz

val result = safeApiCall { getDataFromFirestore() }
// result = Resource.Error(PermissionDeniedException)
```

### Hata Senaryosu (Retry Tükendi)

```kotlin
// 1. Deneme: UNAVAILABLE (500ms bekle)
// 2. Deneme: UNAVAILABLE (1000ms bekle)
// 3. Deneme: UNAVAILABLE (2000ms bekle)
// 4. Deneme: UNAVAILABLE ❌
// Retry limit aşıldı

val result = safeApiCall { getDataFromFirestore() }
// result = Resource.Error(FirebaseFirestoreException)
```

---

## 🧪 Test Coverage

### Test Dosyası

`app/src/test/java/com/cangzr/neocard/common/SafeApiCallRetryTest.kt`

### Test Senaryoları (13 test)

1. ✅ İlk denemede başarı (retry yok)
2. ✅ UNAVAILABLE hatası ile retry
3. ✅ Tüm retry'ler tükendikten sonra hata
4. ✅ UNAVAILABLE olmayan hatalar için retry yok
5. ✅ Generic exception için retry yok
6. ✅ Exponential backoff delays
7. ✅ İkinci denemede başarı
8. ✅ Null localizedMessage handling
9. ✅ Doğru exception döndürme
10. ✅ Farklı return type'lar
11. ✅ Maximum retry count kontrolü
12. ✅ Başarılı olunca retry'i durdurma
13. ✅ Çoklu retry senaryoları

### Test Çalıştırma

```bash
# Retry testlerini çalıştır
./gradlew test --tests "SafeApiCallRetryTest"

# Tüm common testleri
./gradlew test --tests "com.cangzr.neocard.common.*"
```

---

## ✨ Best Practices

### 1. safeApiCall Her Yerde Kullanın

```kotlin
// ✅ Good
override suspend fun saveCard(card: UserCard): Resource<String> = safeApiCall {
    firestore.collection("cards").add(card).await()
}

// ❌ Bad
override suspend fun saveCard(card: UserCard): Resource<String> {
    return try {
        val result = firestore.collection("cards").add(card).await()
        Resource.Success(result.id)
    } catch (e: Exception) {
        Resource.Error(e)
    }
}
```

### 2. Dispatchers.IO Gereksiz

`safeApiCall` zaten `Dispatchers.IO` kullanır, tekrar kullanmaya gerek yok:

```kotlin
// ✅ Good
suspend fun getData() = safeApiCall {
    firestore.collection("data").get().await()
}

// ❌ Redundant
suspend fun getData() = withContext(Dispatchers.IO) {
    safeApiCall {
        firestore.collection("data").get().await()
    }
}
```

### 3. Uygun Hata Mesajları

```kotlin
// ✅ Good: Meaningful error messages
catch (e: Exception) {
    Resource.Error(
        exception = e,
        message = e.localizedMessage ?: "Failed to load user cards"
    )
}

// ❌ Bad: Generic messages
catch (e: Exception) {
    Resource.Error(e, "Error")
}
```

### 4. ViewModel'de Kullanım

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: MyRepository
) : ViewModel() {
    
    fun loadData() {
        viewModelScope.launch {
            _uiState.value = Resource.Loading
            
            // safeApiCall otomatik retry yapar
            when (val result = repository.getData()) {
                is Resource.Success -> {
                    _uiState.value = Resource.Success(result.data)
                }
                is Resource.Error -> {
                    _uiState.value = Resource.Error(
                        result.exception,
                        result.message
                    )
                }
                is Resource.Loading -> {
                    // Already loading
                }
            }
        }
    }
}
```

### 5. UI'da Hata Gösterimi

```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    when (val state = uiState) {
        is Resource.Loading -> {
            CircularProgressIndicator()
        }
        is Resource.Success -> {
            ShowContent(state.data)
        }
        is Resource.Error -> {
            ErrorMessage(
                message = state.message ?: "Unknown error",
                onRetry = { viewModel.loadData() }
            )
        }
    }
}
```

---

## 📊 Performance Considerations

### Retry Süresi

Maksimum retry süresi: **3.5 saniye**
- İlk deneme: 0ms
- 1. retry: +500ms = 500ms
- 2. retry: +1000ms = 1500ms
- 3. retry: +2000ms = 3500ms

### Memory Usage

- Minimal overhead
- Sadece `lastException` ve deneme sayacı tutulur
- API response memory'de tutulmaz (streaming)

### Thread Safety

- `withContext(Dispatchers.IO)` kullanır
- Main thread bloklanmaz
- Coroutine-safe implementation

---

## 🔍 Debugging

### Retry Logları

Geliştirim sırasında retry'leri görmek için:

```kotlin
suspend fun <T> safeApiCall(
    apiCall: suspend () -> T
): Resource<T> = withContext(Dispatchers.IO) {
    repeat(maxRetries + 1) { attempt ->
        try {
            val result = apiCall()
            if (BuildConfig.DEBUG && attempt > 0) {
                Log.d("SafeApiCall", "Succeeded after $attempt retries")
            }
            return@withContext Resource.Success(result)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.d("SafeApiCall", "Attempt ${attempt + 1} failed: ${e.message}")
            }
            // ... retry logic
        }
    }
}
```

### Testing Retry Behavior

```kotlin
@Test
fun testRetryBehavior() = runTest {
    var callCount = 0
    val mockApiCall: suspend () -> String = {
        callCount++
        println("API Call attempt: $callCount")
        if (callCount < 3) {
            val exception = mockk<FirebaseFirestoreException>()
            every { exception.code } returns FirebaseFirestoreException.Code.UNAVAILABLE
            throw exception
        }
        "Success"
    }
    
    val result = safeApiCall { mockApiCall() }
    
    println("Total attempts: $callCount")
    println("Result: $result")
}
```

---

## 📚 Kaynaklar

- [Exponential Backoff - Google Cloud](https://cloud.google.com/iot/docs/how-tos/exponential-backoff)
- [Firebase Error Codes](https://firebase.google.com/docs/reference/kotlin/com/google/firebase/firestore/FirebaseFirestoreException.Code)
- [Kotlin Coroutines Best Practices](https://kotlinlang.org/docs/coroutines-guide.html)

---

## 🔄 Changelog

### v2.0.0 - Retry Logic Eklendi
- ✅ Automatic retry on UNAVAILABLE errors
- ✅ Exponential backoff (500ms, 1000ms, 2000ms)
- ✅ Maximum 3 retries
- ✅ Dispatchers.IO for non-blocking operation
- ✅ Comprehensive unit tests (13 tests)

### v1.0.0 - Initial Implementation
- Basic try-catch wrapper
- No retry mechanism

---

*Retry Logic Guide - NeoCard Project*

