package com.cangzr.neocard.ui.screens

import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.cangzr.neocard.R
import coil.transform.CircleCropTransformation
import com.cangzr.neocard.storage.FirebaseStorageManager
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JobPostDetailViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<JobPostDetailUiState>(JobPostDetailUiState.Loading)
    val uiState: StateFlow<JobPostDetailUiState> = _uiState

    private val _appliedCards = MutableStateFlow<List<UserCard>>(emptyList())
    val appliedCards: StateFlow<List<UserCard>> = _appliedCards

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun loadJobPost(jobPostId: String) {
        viewModelScope.launch {
            try {
                val document = firestore.collection("jobPosts").document(jobPostId).get().await()
                if (document.exists()) {
                    val jobPost = document.toObject(JobPost::class.java)?.copy(id = document.id)
                    _uiState.value = JobPostDetailUiState.Success(jobPost!!)
                    loadAppliedCards(jobPost.appliedCardIds)
                } else {
                    _uiState.value = JobPostDetailUiState.Error("İlan bulunamadı")
                }
            } catch (e: Exception) {
                _uiState.value = JobPostDetailUiState.Error(e.message ?: "Bir hata oluştu")
            }
        }
    }

    private fun loadAppliedCards(cardIds: List<String>) {
        viewModelScope.launch {
            try {
                val cards = mutableListOf<UserCard>()
                cardIds.forEach { cardId ->
                    val userDocs = firestore.collection("users").get().await()
                    for (userDoc in userDocs.documents) {
                        val cardDoc = userDoc.reference.collection("cards").document(cardId).get().await()
                        if (cardDoc.exists()) {
                            cardDoc.toObject(UserCard::class.java)?.let {
                                cards.add(it.copy(id = cardDoc.id))
                            }
                        }
                    }
                }
                _appliedCards.value = cards
            } catch (e: Exception) {
                Log.e("JobPostDetailVM", "Error loading cards: ${e.message}")
            }
        }
    }

    fun deleteJobPost(jobPostId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // İlk olarak ilanı al (logo URL'si için)
                val jobPostSnapshot = firestore.collection("jobPosts")
                    .document(jobPostId)
                    .get()
                    .await()
                
                val logoUrl = jobPostSnapshot.getString("logoUrl") ?: ""
                
                // İlgili şikayetleri bulalım
                val reportsSnapshot = firestore.collection("reports")
                    .whereEqualTo("jobPostId", jobPostId)
                    .get()
                    .await()
                
                // Batch işlemi başlat
                firestore.runBatch { batch ->
                    // İlanı sil
                    batch.delete(firestore.collection("jobPosts").document(jobPostId))
                    
                    // Kullanıcının jobPostIds listesinden kaldır
                    auth.currentUser?.uid?.let { userId ->
                        batch.update(
                            firestore.collection("users").document(userId),
                            "jobPostIds", FieldValue.arrayRemove(jobPostId)
                        )
                    }
                    
                    // İlgili tüm şikayetleri sil
                    reportsSnapshot.documents.forEach { reportDoc ->
                        batch.delete(reportDoc.reference)
                    }
                }.await()
                
                // Logo resmini sil
                if (logoUrl.isNotEmpty()) {
                    val storageManager = FirebaseStorageManager.getInstance()
                    try {
                        // URL'den dosya yolunu çıkar
                        if (logoUrl.contains("job_logos")) {
                            val path = logoUrl.substringAfter("firebasestorage.googleapis.com/v0/b/")
                                .substringAfter("/o/")
                                .substringBefore("?")
                                .replace("%2F", "/")
                            
                            FirebaseStorage.getInstance().reference.child(path).delete().await()
                        }
                    } catch (e: Exception) {
                        println("❌ İş ilanı logosu silinirken hata: ${e.message}")
                        // Logo silinmese bile işlemi başarılı sayıyoruz
                    }
                }
                
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "İlan silinirken bir hata oluştu")
            }
        }
    }

    fun toggleJobStatus(
        jobPostId: String,
        currentStatus: Boolean,
        onSuccess: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val newStatus = !currentStatus
                firestore.collection("jobPosts").document(jobPostId)
                    .update("isActive", newStatus)
                    .await()
                
                // UI state'i güncelle
                (_uiState.value as? JobPostDetailUiState.Success)?.let { currentState ->
                    _uiState.value = JobPostDetailUiState.Success(
                        currentState.jobPost.copy(isActive = newStatus)
                    )
                }
                
                onSuccess(newStatus)
            } catch (e: Exception) {
                onError(e.message ?: "Durum güncellenirken bir hata oluştu")
            }
        }
    }
}

sealed class JobPostDetailUiState {
    object Loading : JobPostDetailUiState()
    data class Success(val jobPost: JobPost) : JobPostDetailUiState()
    data class Error(val message: String) : JobPostDetailUiState()
}

@Composable
fun JobPostDetail(
    jobPostId: String,
    navController: NavHostController,
    viewModel: JobPostDetailViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val appliedCards by viewModel.appliedCards.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var showFullDescription by remember { mutableStateOf(false) }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    
    LaunchedEffect(jobPostId) {
        viewModel.loadJobPost(jobPostId)
    }

    // Snackbar Host
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is JobPostDetailUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                is JobPostDetailUiState.Error -> {
                    Text(
                        text = (uiState as JobPostDetailUiState.Error).message,
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                is JobPostDetailUiState.Success -> {
                    val jobPost = (uiState as JobPostDetailUiState.Success).jobPost

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // İlan Bilgileri Kartı
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                // Üst Kısım - Logo ve Durum Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                                    // Logo ve Başlık
                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                    ) {
                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(jobPost.logoUrl)
                                                .crossfade(true)
                                                .placeholder(R.drawable.logo3)
                                                .error(R.drawable.logo3)
                                                .transformations(CircleCropTransformation())
                                                .memoryCachePolicy(CachePolicy.ENABLED)
                                                .diskCachePolicy(CachePolicy.ENABLED)
                                                .build(),
                                            contentDescription = "Firma Logosu",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentScale = ContentScale.Crop
                        )
                                        
                        Spacer(modifier = Modifier.width(12.dp))
                                        
                        Column {
                            Text(
                                                text = jobPost.title,
                                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                                text = jobPost.company,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }

                                    // Durum Badge
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (jobPost.isActive) 
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        else 
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                        modifier = Modifier.padding(start = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (jobPost.isActive) 
                                                    Icons.Default.CheckCircle 
                                                else 
                                                    Icons.Default.Close,
                                                contentDescription = null,
                                                tint = if (jobPost.isActive) 
                                                    MaterialTheme.colorScheme.primary
                                                else 
                                                    MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = if (jobPost.isActive) "Aktif" else "Pasif",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (jobPost.isActive) 
                                                    MaterialTheme.colorScheme.primary
                                                else 
                                                    MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Açıklama
                                Column {
                Text(
                                        text = jobPost.description,
                    style = MaterialTheme.typography.bodyMedium,
                                        maxLines = if (showFullDescription) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis
                )
                                    if (jobPost.description.length > 150) {
                                        TextButton(
                                            onClick = { showFullDescription = !showFullDescription }
                                        ) {
                Text(
                                                text = if (showFullDescription) "Daha az göster" else "Devamını oku",
                    color = MaterialTheme.colorScheme.primary
                )
                                        }
                                    }
            }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Başvuru Sayısı Göstergesi
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Toplam Başvuru",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                                            text = "${jobPost.applications.size}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Yönetim Butonları
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Silme Butonu
    Button(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                enabled = !isProcessing
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.onError
                                    )
                                } else {
                                    Text("İlanı Sil")
                                }
                            }

                            // Durum Değiştirme Butonu
                            Button(
                                onClick = {
                                    isProcessing = true
                                    viewModel.toggleJobStatus(
                                        jobPostId = jobPost.id,
                                        currentStatus = jobPost.isActive,
                                        onSuccess = { newStatus ->
                                            isProcessing = false
                                            val message = if (newStatus) "İlan aktif hale getirildi" else "İlan pasif hale getirildi"
                                            snackbarMessage = message
                                            showSnackbar = true
                                        },
                                        onError = { error ->
                                            isProcessing = false
                                            snackbarMessage = error
                                            showSnackbar = true
                                        }
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !isProcessing,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (jobPost.isActive) 
                                        MaterialTheme.colorScheme.error 
                                    else 
                                        MaterialTheme.colorScheme.primary
                                )
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                } else {
                                    Text(if (jobPost.isActive) "Pasif Yap" else "Aktif Yap")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Başvuran Kartlar Bölümü
        Text(
            text = "Başvuran Kartlar",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

                        if (appliedCards.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
            Text(
                                    text = "Henüz başvuru yapılmamış",
                style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
                            }
        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(appliedCards) { index, card ->
                                    UserCardItem(
                                        card = card,
                                        onClick = {
                                            navController.navigate("sharedCardDetail/${card.id}")
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Delete Dialog
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("İlanı Sil") },
                    text = { Text("Bu ilanı silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                isProcessing = true
                                viewModel.deleteJobPost(
                                    jobPostId = jobPostId,
                                    onSuccess = {
                                        isProcessing = false
                                        Toast.makeText(context, "İlan başarıyla silindi", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    },
                                    onError = { error ->
                                        isProcessing = false
                                        snackbarMessage = error
                                        showSnackbar = true
                                        showDeleteDialog = false
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            enabled = !isProcessing
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onError
                                )
                            } else {
                                Text("Evet, Sil")
                            }
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = { showDeleteDialog = false },
                            enabled = !isProcessing
                        ) {
                            Text("İptal")
                        }
                    }
                )
            }

            // Snackbar
            LaunchedEffect(showSnackbar) {
                if (showSnackbar) {
                    snackbarHostState.showSnackbar(
                        message = snackbarMessage,
                        duration = SnackbarDuration.Short
                    )
                    showSnackbar = false
                }
            }
        }
        }
}