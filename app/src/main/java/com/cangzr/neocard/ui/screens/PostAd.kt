package com.cangzr.neocard.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.InputStream

@Composable
fun PostAdScreen(navController: NavHostController) {
    // Form verileri için SharedPreferences
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("post_ad_form", Context.MODE_PRIVATE)
    
    var jobTitle by remember { mutableStateOf(prefs.getString("jobTitle", "") ?: "") }
    var companyName by remember { mutableStateOf(prefs.getString("companyName", "") ?: "") }
    var description by remember { mutableStateOf(prefs.getString("description", "") ?: "") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedCategory by remember { mutableStateOf<JobCategory?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var showExitDialog by remember { mutableStateOf(false) }
    var retryCount by remember { mutableStateOf(0) }
    var showRetryDialog by remember { mutableStateOf(false) }
    var errorDetails by remember { mutableStateOf<String?>(null) }
    
    // Kategori uyarısı state'i
    var showCategoryWarning by remember { mutableStateOf(false) }
    
    // Form validasyon state'leri
    var jobTitleError by remember { mutableStateOf<String?>(null) }
    var companyNameError by remember { mutableStateOf<String?>(null) }
    var descriptionError by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    var showCancelDialog by remember { mutableStateOf(false) }

    // Form verilerini kaydet
    fun saveFormData() {
        prefs.edit().apply {
            putString("jobTitle", jobTitle)
            putString("companyName", companyName)
            putString("description", description)
            apply()
        }
    }

    // Form verilerini temizle
    fun clearFormData() {
        prefs.edit().clear().apply()
    }

    // Resim seçici
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                scope.launch {
                    validateAndSetImage(context, it, 800, 800) { isValid, error, optimizedUri ->
                        if (isValid) {
                            selectedImageUri = optimizedUri
                        } else {
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    )

    // Geri tuşu kontrolü
    BackHandler {
        if (formHasChanges(jobTitle, companyName, description, selectedImageUri)) {
            showExitDialog = true
        } else {
            clearFormData()
            navController.popBackStack()
        }
    }

    // Exit Dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Değişikliklerden Vazgeç") },
            text = { Text("Yaptığınız değişiklikler kaydedilmeyecek. Çıkmak istediğinize emin misiniz?") },
            confirmButton = {
                TextButton(onClick = { 
                    clearFormData()
                    navController.popBackStack() 
                }) {
                    Text("Evet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Hayır")
                }
            }
        )
    }

    // Loading Dialog with Progress
    if (isLoading) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Yükleniyor") },
            text = { 
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("İlanınız yükleniyor, lütfen bekleyin...")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = uploadProgress,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "İlerleme: ${(uploadProgress * 100).toInt()}%",
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            },
            confirmButton = { }
        )
    }

    // Retry Dialog
    if (showRetryDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Bağlantı Hatası") },
            text = { 
                Column {
                    Text("Yükleme sırasında bir hata oluştu. Tekrar denemek ister misiniz?")
                    if (errorDetails != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Hata detayı: $errorDetails",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showRetryDialog = false
                    // Yüklemeyi tekrar dene
                    if (validateForm(jobTitle, companyName, description, selectedImageUri, selectedCategory)) {
                        isLoading = true
                        scope.launch {
                            uploadImageAndPostAd(
                                context = context,
                                imageUri = selectedImageUri!!,
                                title = jobTitle,
                                company = companyName,
                                desc = description,
                                category = selectedCategory!!,
                                onProgress = { progress -> uploadProgress = progress },
                                onSuccess = {
                                    isLoading = false
                                    clearFormData()
                                    navController.popBackStack()
                                },
                                onError = { error ->
                                    isLoading = false
                                    errorDetails = error
                                    if (retryCount < 3) {
                                        retryCount++
                                        showRetryDialog = true
                                    } else {
                                        Toast.makeText(context, "Maksimum deneme sayısına ulaşıldı: $error", Toast.LENGTH_LONG).show()
                                    }
                                }
                            )
                        }
                    }
                }) {
                    Text("Tekrar Dene")
                }
            },
            dismissButton = {
                Button(onClick = { 
                    showRetryDialog = false
                    saveFormData() // Form verilerini kaydet
                }) {
                    Text("İptal")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Üst Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (formHasChanges(jobTitle, companyName, description, selectedImageUri)) {
                    showExitDialog = true
                } else {
                    clearFormData()
                    navController.popBackStack()
                }
            }) {
                Icon(Icons.Default.ArrowBack, "Geri")
            }
        Text(
            text = "İlan Paylaş",
                style = MaterialTheme.typography.headlineSmall
            )
            // Boş box for alignment
            Box(modifier = Modifier.size(48.dp))
        }

        // Logo Seçici
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { launcher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Resim Seç",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(24.dp)
                        .offset(x = 25.dp, y = 25.dp)
                    .padding(4.dp)
            )

            if (selectedImageUri != null) {
                AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(selectedImageUri)
                            .crossfade(true)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .build(),
                    contentDescription = "Firma Logosu",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Logo Seç",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
            }
            }
        }

        // Form Alanları
        OutlinedTextField(
            value = jobTitle,
            onValueChange = { 
                jobTitle = it
                jobTitleError = validateJobTitle(it)
            },
            label = { Text("İlan Başlığı") },
            modifier = Modifier.fillMaxWidth(),
            isError = jobTitleError != null,
            supportingText = { jobTitleError?.let { Text(it) } }
        )

        OutlinedTextField(
            value = companyName,
            onValueChange = { 
                companyName = it
                companyNameError = validateCompanyName(it)
            },
            label = { Text("Firma Adı") },
            modifier = Modifier.fillMaxWidth(),
            isError = companyNameError != null,
            supportingText = { companyNameError?.let { Text(it) } }
        )

        OutlinedTextField(
            value = description,
            onValueChange = { 
                description = it
                descriptionError = validateDescription(it)
            },
            label = { Text("İlan Açıklaması") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 5,
            isError = descriptionError != null,
            supportingText = { descriptionError?.let { Text(it) } }
        )

        Text("Kategori Seçin", style = MaterialTheme.typography.labelMedium)

        // Kategori seçimi için uyarı
        if (showCategoryWarning) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Lütfen bir kategori seçin",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        CategorySelector(
            selectedCategory = selectedCategory,
            onCategorySelected = { 
                selectedCategory = it
                showCategoryWarning = false
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showCancelDialog = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("İptal")
            }

        Button(
            onClick = {
                    if (selectedCategory == null) {
                        showCategoryWarning = true
                        return@Button
                    }
                    
                    if (validateForm(jobTitle, companyName, description, selectedImageUri, selectedCategory)) {
                        isLoading = true
                        uploadProgress = 0f
                        retryCount = 0
                        scope.launch {
                    uploadImageAndPostAd(
                        context = context,
                        imageUri = selectedImageUri!!,
                        title = jobTitle,
                        company = companyName,
                        desc = description,
                        category = selectedCategory!!,
                                onProgress = { progress -> uploadProgress = progress },
                                onSuccess = {
                                    isLoading = false
                                    clearFormData()
                                    navController.popBackStack()
                                },
                                onError = { error ->
                                    isLoading = false
                                    errorDetails = error
                                    showRetryDialog = true
                                }
                            )
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isLoading && validateForm(jobTitle, companyName, description, selectedImageUri, selectedCategory)
            ) {
                Text("İlanı Paylaş")
            }
        }
    }

    // İptal Onay Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("İptal Et") },
            text = { Text("İlan paylaşımını iptal etmek istediğinize emin misiniz? Girdiğiniz bilgiler kaydedilecektir.") },
            confirmButton = {
                Button(
                    onClick = { 
                        saveFormData()
                        navController.popBackStack() 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("İptal Et")
                }
            },
            dismissButton = {
                Button(onClick = { showCancelDialog = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

// Validasyon fonksiyonları
private fun validateJobTitle(title: String): String? {
    return when {
        title.isEmpty() -> "İlan başlığı boş olamaz"
        title.length < 10 -> "İlan başlığı en az 10 karakter olmalıdır"
        title.length > 100 -> "İlan başlığı en fazla 100 karakter olabilir"
        else -> null
    }
}

private fun validateCompanyName(name: String): String? {
    return when {
        name.isEmpty() -> "Firma adı boş olamaz"
        name.length < 2 -> "Firma adı en az 2 karakter olmalıdır"
        name.length > 50 -> "Firma adı en fazla 50 karakter olabilir"
        else -> null
    }
}

private fun validateDescription(desc: String): String? {
    return when {
        desc.isEmpty() -> "İlan açıklaması boş olamaz"
        desc.length < 50 -> "İlan açıklaması en az 50 karakter olmalıdır"
        desc.length > 1000 -> "İlan açıklaması en fazla 1000 karakter olabilir"
        else -> null
    }
}

private fun validateForm(
    jobTitle: String,
    companyName: String,
    description: String,
    selectedImageUri: Uri?,
    selectedCategory: JobCategory?
): Boolean {
    return validateJobTitle(jobTitle) == null &&
           validateCompanyName(companyName) == null &&
           validateDescription(description) == null &&
           selectedImageUri != null &&
           selectedCategory != null
}

private fun formHasChanges(
    jobTitle: String,
    companyName: String,
    description: String,
    selectedImageUri: Uri?
): Boolean {
    return jobTitle.isNotEmpty() ||
           companyName.isNotEmpty() ||
           description.isNotEmpty() ||
           selectedImageUri != null
}

// Resim optimizasyonu ve validasyonu
suspend fun validateAndSetImage(
    context: Context,
    uri: Uri,
    maxWidth: Int,
    maxHeight: Int,
    callback: (Boolean, String, Uri?) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            // Resim boyutu kontrolü (max 5MB)
            val fileSize = context.contentResolver.openInputStream(uri)?.use { it.available() } ?: 0
            if (fileSize > 5 * 1024 * 1024) {
                withContext(Dispatchers.Main) {
                    callback(false, "Resim boyutu 5MB'dan büyük olamaz", null)
                }
                return@withContext
            }

            // Resim tipi kontrolü
            val type = context.contentResolver.getType(uri)
            if (type?.startsWith("image/") != true) {
                withContext(Dispatchers.Main) {
                    callback(false, "Sadece resim dosyaları yüklenebilir", null)
                }
                return@withContext
            }

            // Resmi optimize et
            val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
            val scaledBitmap = if (bitmap.width > maxWidth || bitmap.height > maxHeight) {
                val scaleWidth = maxWidth.toFloat() / bitmap.width
                val scaleHeight = maxHeight.toFloat() / bitmap.height
                val scale = minOf(scaleWidth, scaleHeight)
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt(),
                    (bitmap.height * scale).toInt(),
                    true
                )
            } else {
                bitmap
            }

            // Optimize edilmiş resmi geçici dosyaya kaydet
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, calculateQuality(fileSize), outputStream)
            
            withContext(Dispatchers.Main) {
                callback(true, "", uri)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                callback(false, "Resim yüklenirken hata oluştu: ${e.message}", null)
            }
        }
    }
}

// Dosya boyutuna göre sıkıştırma kalitesini hesapla
fun calculateQuality(fileSize: Int): Int {
    return when {
        fileSize > 4 * 1024 * 1024 -> 60  // 4MB'dan büyük
        fileSize > 2 * 1024 * 1024 -> 70  // 2MB'dan büyük
        fileSize > 1024 * 1024 -> 80      // 1MB'dan büyük
        else -> 90                         // 1MB'dan küçük
    }
}

// Resim optimizasyonu için yardımcı fonksiyon
private suspend fun optimizeImage(context: Context, imageUri: Uri): ByteArray {
    return withContext(Dispatchers.IO) {
        val maxWidth = 800
        val maxHeight = 800
        val quality = 85

        val inputStream = context.contentResolver.openInputStream(imageUri)
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream?.close()

        var scale = 1
        while (options.outWidth / scale / 2 >= maxWidth && 
               options.outHeight / scale / 2 >= maxHeight) {
            scale *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = scale
        }

        val scaledBitmap = context.contentResolver.openInputStream(imageUri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: throw IllegalStateException("Resim okunamadı")

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        outputStream.toByteArray()
    }
}

suspend fun uploadImageAndPostAd(
    context: Context,
    imageUri: Uri,
    title: String,
    company: String,
    desc: String,
    category: JobCategory,
    onProgress: (Float) -> Unit,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        if (!isNetworkAvailable(context)) {
            onError("İnternet bağlantısı bulunamadı")
            return
        }

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser ?: throw IllegalStateException("Kullanıcı oturumu açık değil")
        val firestore = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()

        // Resmi optimize et
        val compressedImageData = optimizeImage(context, imageUri)
        
        // Storage referansını hazırla
        val fileName = "job_logos/${System.currentTimeMillis()}_${currentUser.uid}.jpg"
        val imageRef = storage.reference.child(fileName)
        
        // Resmi yükle
        val uploadTask = imageRef.putBytes(compressedImageData)
        
        uploadTask.addOnProgressListener { taskSnapshot ->
            val progress = taskSnapshot.bytesTransferred.toFloat() / taskSnapshot.totalByteCount.toFloat()
            onProgress(progress)
        }.await()

        // URL'yi al ve ilanı kaydet
        val downloadUrl = imageRef.downloadUrl.await().toString()

        val jobPost = hashMapOf(
            "title" to title,
            "company" to company,
            "description" to desc,
            "logoUrl" to downloadUrl,
            "category" to category.name,
            "appliedCardIds" to emptyList<String>(),
            "applications" to emptyList<String>(),
            "userId" to currentUser.uid,
            "isActive" to true,
            "createdAt" to FieldValue.serverTimestamp()
        )

        // İlanı kaydet ve kullanıcı dökümanını güncelle
        val batch = firestore.batch()
        val jobPostRef = firestore.collection("jobPosts").document()
        batch.set(jobPostRef, jobPost)
        
        val userRef = firestore.collection("users").document(currentUser.uid)
        batch.update(userRef, "jobPostIds", FieldValue.arrayUnion(jobPostRef.id))
        
        // Batch işlemini gerçekleştir
        batch.commit().await()

        withContext(Dispatchers.Main) {
            Toast.makeText(context, "İlan başarıyla paylaşıldı!", Toast.LENGTH_SHORT).show()
            onSuccess()
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            val errorMessage = when {
                e.message?.contains("permission") == true -> "Yetki hatası: Lütfen giriş yapın"
                e.message?.contains("network") == true -> "Ağ hatası: İnternet bağlantınızı kontrol edin"
                e.message?.contains("storage") == true -> "Depolama hatası: Resim yüklenemedi"
                else -> "Beklenmeyen hata: ${e.message}"
            }
            onError(errorMessage)
        }
    }
}

// İnternet bağlantısı kontrolü
fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

@Composable
fun CategorySelector(
    selectedCategory: JobCategory?,
    onCategorySelected: (JobCategory) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(JobCategory.values()) { category ->
            val isSelected = category == selectedCategory

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (isSelected) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onCategorySelected(category) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = category.displayName,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimary 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

enum class JobCategory(val displayName: String) {
    SOFTWARE("Yazılım"),
    DESIGN("Tasarım"),
    MARKETING("Pazarlama"),
    FINANCE("Finans"),
    SALES("Satış"),
    HR("İnsan Kaynakları"),
    OTHER("Diğer")
}
