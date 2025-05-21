package com.cangzr.neocard.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import coil.transform.CircleCropTransformation
import com.cangzr.neocard.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.collections.plus

@Composable
fun JobPostScreen(navController: NavHostController) {
    var cards by remember { mutableStateOf<List<UserCard>>(emptyList()) }
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid
    val context = LocalContext.current

    // Kullanıcı kartvizitlerini yükle
    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            firestore.collection("users").document(currentUserId).collection("cards")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val userCards = querySnapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(UserCard::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                    }
                    cards = userCards
                    if (userCards.isEmpty()) {
                        Toast.makeText(context, "Kartvizit bulunamadı. Başvurmak için kartvizit oluşturmanız gerekiyor.", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Kartvizitler yüklenemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // İlan Listesi
        JobPostListScreen(navController, cards)
    }
}


data class JobPost(
    val id: String = "",
    val title: String = "",
    val company: String = "",
    val description: String = "",
    val logoUrl: String = "",
    val category: String="",
    val applications: List<String> = emptyList(),      // Başvuran kullanıcıların UID'leri
    val appliedCardIds: List<String> = emptyList(),    // Başvuruda kullanılan kartların ID'leri
    val userId: String = "",
    val isActive: Boolean = true // Bu da eklenebilir
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailBottomSheet(
    jobPost: JobPost,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            // Üst Bar - Kapat Butonu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onDismiss() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kapat",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "İlan Detayları",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.size(40.dp))
            }

            // Şirket Logosu ve Başlık
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo
                Box(
                    modifier = Modifier
                        .size(75.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .border(2.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), CircleShape)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(jobPost.logoUrl)
                            .crossfade(true)
                            .size(Size.ORIGINAL)
                            .memoryCacheKey(jobPost.logoUrl)
                            .diskCacheKey(jobPost.logoUrl)
                            .placeholder(R.drawable.logo3)
                            .error(R.drawable.logo3)
                            .transformations(CircleCropTransformation())
                            .build(),
                        contentDescription = "Company Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Başlık ve Şirket Adı
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = jobPost.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = jobPost.company,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // Kategori ve Başvuru Sayısı
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Kategori Etiketi
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(
                            id = JobCategoryHome.values()
                                .find { it.name == jobPost.category }?.imageRes ?: R.drawable.other
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = JobCategoryHome.values()
                            .find { it.name == jobPost.category }?.displayName ?: "Diğer",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Başvuru Sayısı
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.bookmark),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "${jobPost.appliedCardIds.size} Başvuru",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // İlan Açıklaması
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = jobPost.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JobPostListScreen(navController: NavHostController, cards: List<UserCard>) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid

    var jobPosts by remember { mutableStateOf<List<JobPost>>(emptyList()) }
    var filteredPosts by remember { mutableStateOf<List<JobPost>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<JobCategoryHome?>(null) }
    var showCategoryMenu by remember { mutableStateOf(false) }

    // Veri yükleme durumunu takip et
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }

    // Pagination için değişkenler
    var lastVisiblePost by remember { mutableStateOf<String?>(null) }
    var hasMorePosts by remember { mutableStateOf(true) }
    val pageSize = 15

    val context = LocalContext.current
    val adManager = remember { com.cangzr.neocard.ads.AdManager.getInstance(context) }

    // İlk veri yüklemesi
    LaunchedEffect(Unit) {
        loadJobPosts(
            firestore = firestore,
            pageSize = pageSize,
            lastPostId = null,
            onSuccess = { posts, lastId, hasMore ->
                jobPosts = posts
                lastVisiblePost = lastId
                hasMorePosts = hasMore

                // Filtreleme işlemini çağır
                applyFilter(posts, selectedCategory, currentUserId) { filtered ->
                    filteredPosts = filtered
                    isLoading = false
                }
            },
            onError = {
                isLoading = false
            }
        )
    }

    // Daha fazla ilan yükleme fonksiyonu
    fun loadMorePosts() {
        if (!hasMorePosts || isLoadingMore) return

        isLoadingMore = true
        loadJobPosts(
            firestore = firestore,
            pageSize = pageSize,
            lastPostId = lastVisiblePost,
            onSuccess = { newPosts, lastId, hasMore ->
                val updatedPosts = jobPosts + newPosts
                jobPosts = updatedPosts
                lastVisiblePost = lastId
                hasMorePosts = hasMore

                // Filtreleme işlemini çağır
                applyFilter(updatedPosts, selectedCategory, currentUserId) { filtered ->
                    filteredPosts = filtered
                    isLoadingMore = false
                }
            },
            onError = {
                isLoadingMore = false
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            // Yükleme göstergesi
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Başlık ve Filtre Alanı
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Başlık ve Toplam İlan Sayısı
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (selectedCategory) {
                                    JobCategoryHome.MY_POSTS -> "İlanlarım"
                                    null -> "Tüm İlanlar"
                                    else -> selectedCategory!!.displayName
                                },
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "Toplam: ${filteredPosts.size} ilan",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Kategori Filtreleri
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedCategory == null,
                                    onClick = {
                                        selectedCategory = null
                                        applyFilter(jobPosts, null, currentUserId) { filtered ->
                                            filteredPosts = filtered
                                        }
                                    },
                                    label = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.filter),
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text("Tümü")
                                        }
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }

                            items(JobCategoryHome.values()) { category ->
                                FilterChip(
                                    selected = selectedCategory == category,
                                    onClick = {
                                        selectedCategory = category
                                        applyFilter(jobPosts, category, currentUserId) { filtered ->
                                            filteredPosts = filtered
                                        }
                                    },
                                    label = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Image(
                                                painter = painterResource(id = category.imageRes),
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(category.displayName)
                                        }
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }

                if (filteredPosts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.filter),
                                contentDescription = null,
                                modifier = Modifier.size(120.dp),
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            )
                            Text(
                                text = "Seçili kategoride henüz ilan yok.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // withAdItems kullanarak reklamlı öğeleri oluşturalım
                    val adInterval = com.cangzr.neocard.ads.AdManager.JOB_POST_AD_INTERVAL
                    val itemsWithAds = com.cangzr.neocard.ads.withAdItems(
                        items = filteredPosts,
                        adInterval = adInterval,
                        itemContent = { jobPost ->
                            JobPostItem(
                                jobPost = jobPost,
                                navController = navController,
                                cards = cards,
                                modifier = Modifier
                            )
                        },
                        adContent = {
                            com.cangzr.neocard.ads.InlineAdView(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                        }
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        // İlanlar ve reklamlar
                        items(itemsWithAds.size) { index ->
                            itemsWithAds[index]()
                        }

                        // Daha fazla ilan varsa yükleme göstergesi
                        if (hasMorePosts && filteredPosts.isNotEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isLoadingMore) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(30.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        // Son öğeye gelince daha fazla yükle
                                        Spacer(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .layout { measurable, constraints ->
                                                    val placeable = measurable.measure(constraints)
                                                    loadMorePosts()
                                                    layout(placeable.width, placeable.height) {
                                                        placeable.placeRelative(0, 0)
                                                    }
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobPostItem(
    jobPost: JobPost,
    navController: NavHostController,
    cards: List<UserCard>,
    modifier: Modifier = Modifier
) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedCard by remember { mutableStateOf<UserCard?>(null) }
    var showDetailSheet by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val currentUser = auth.currentUser
    val isUserLoggedIn = currentUser != null
    val isOwnPost = currentUser?.uid == jobPost.userId
    
    // Kullanıcının bu ilana daha önce başvurup başvurmadığını kontrol et
    var hasApplied by remember { mutableStateOf(false) }
    
    // Kullanıcı giriş yaptıysa başvuru durumunu kontrol et
    LaunchedEffect(jobPost.id, currentUser?.uid) {
        if (currentUser?.uid != null) {
            hasApplied = jobPost.applications.contains(currentUser.uid)
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDetailSheet = true },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Üst Kısım - Logo, Şirket Bilgisi ve Menü
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo ve Şirket Bilgisi
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), CircleShape)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(jobPost.logoUrl)
                                .crossfade(true)
                                .size(Size.ORIGINAL)
                                .memoryCacheKey(jobPost.logoUrl)
                                .diskCacheKey(jobPost.logoUrl)
                                .placeholder(R.drawable.logo3)
                                .error(R.drawable.logo3)
                                .transformations(CircleCropTransformation())
                                .build(),
                            contentDescription = "Company Logo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = jobPost.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = jobPost.company,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                // Bilgi İkonu
                IconButton(
                    onClick = { if (isUserLoggedIn) showReportDialog = true else showToast(context, "Şikayet etmek için giriş yapmalısınız.") },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    enabled = isUserLoggedIn
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = if (isUserLoggedIn) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // İlan Açıklaması
            Text(
                text = jobPost.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Alt Kısım - İstatistikler ve Başvur Butonu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // İstatistikler
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.bookmark),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "${jobPost.appliedCardIds.size} Başvuru",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                // Başvur Butonu
                Button(
                    onClick = {
                        if (!isUserLoggedIn) {
                            showToast(context, "Başvurmak için giriş yapmalısınız.")
                        } else if (isOwnPost) {
                            showToast(context, "Kendi ilanınıza başvuru yapamazsınız.")
                        } else if (hasApplied) {
                            showToast(context, "Bu ilana zaten başvurdunuz.")
                        } else if (cards.isEmpty()) {
                            showToast(context, "Başvurmak için en az bir kartvizit oluşturmalısınız.")
                        } else {
                            showBottomSheet = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            !isUserLoggedIn || isOwnPost || cards.isEmpty() -> MaterialTheme.colorScheme.surfaceVariant
                            hasApplied -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.primary
                        },
                        contentColor = when {
                            !isUserLoggedIn || isOwnPost || cards.isEmpty() -> MaterialTheme.colorScheme.onSurfaceVariant
                            hasApplied -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> MaterialTheme.colorScheme.onPrimary
                        }
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    enabled = isUserLoggedIn && !isOwnPost && !hasApplied && cards.isNotEmpty()
                ) {
                    Text(
                        text = when {
                            isOwnPost -> "Kendi İlanınız"
                            hasApplied -> "Başvuruldu"
                            cards.isEmpty() -> "Kartvizit Gerekli"
                            else -> "Başvur"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    )
                }
            }
        }
    }

    // Şikayet Dialog'u
    if (showReportDialog) {
        ReportDialog(
            jobPostId = jobPost.id,
            onDismiss = { showReportDialog = false }
        )
    }

    // Başvuru Bottom Sheet
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = if (cards.isEmpty()) "Kartvizit Bulunamadı" else "Kartvizit Seçin",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Kartvizitleri listele
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    if (cards.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .width(300.dp)
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.logo3),
                                        contentDescription = null,
                                        modifier = Modifier.size(50.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = "Henüz kartvizitiniz bulunmuyor. Lütfen önce kartvizit oluşturun.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(cards) { card ->
                            // Demo kartvizit kontrolü
                            if (card.id != "demo") {
                                Card(
                                    modifier = Modifier
                                        .width(300.dp)
                                        .height(180.dp)
                                        .clickable { selectedCard = card }
                                        .border(
                                            width = 2.dp,
                                            color = if (selectedCard == card) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = RoundedCornerShape(16.dp)
                                        ),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = if (selectedCard == card) 8.dp else 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(parseBackground(card))
                                            .padding(16.dp)
                                    ) {
                                        // Kartvizit içeriği buraya gelecek
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                if (card.profileImageUrl!!.isNotEmpty()) {
                                                    AsyncImage(
                                                        model = ImageRequest.Builder(context)
                                                            .data(card.profileImageUrl)
                                                            .crossfade(true)
                                                            .size(Size.ORIGINAL)
                                                            .memoryCacheKey(card.profileImageUrl)
                                                            .diskCacheKey(card.profileImageUrl)
                                                            .placeholder(R.drawable.logo3)
                                                            .error(R.drawable.logo3)
                                                            .transformations(CircleCropTransformation())
                                                            .build(),
                                                        contentDescription = "Profil Resmi",
                                                        modifier = Modifier
                                                            .size(80.dp)
                                                            .clip(CircleShape)
                                                            .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }

                                                Column {
                                                    Text("${card.name} ${card.surname}", style = parseTextStyle(card.textStyles["NAME_SURNAME"]))
                                                    Text(card.title, style = parseTextStyle(card.textStyles["TITLE"]))
                                                    Text(card.company, style = parseTextStyle(card.textStyles["COMPANY"]))
                                                    Text(card.email, style = parseTextStyle(card.textStyles["EMAIL"]))
                                                    Text(card.phone, style = parseTextStyle(card.textStyles["PHONE"]))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (selectedCard != null) {
                            val selectedCardId = selectedCard!!.id
                            val currentUserId = auth.currentUser?.uid

                            if (currentUserId != null) {
                                firestore.collection("jobPosts")
                                    .document(jobPost.id)
                                    .get()
                                    .addOnSuccessListener { document ->
                                        if (document != null) {
                                            val applications = document.get("applications") as? List<String> ?: emptyList()
                                            val appliedCardIds = document.get("appliedCardIds") as? List<String> ?: emptyList()

                                            if (applications.contains(currentUserId)) {
                                                showToast(context, "Bu ilana daha önce başvurdunuz.")
                                            } else {
                                                // İlan başvurusunu güncelle
                                                firestore.collection("jobPosts")
                                                    .document(jobPost.id)
                                                    .update(
                                                        mapOf(
                                                            "applications" to applications + currentUserId,
                                                            "appliedCardIds" to appliedCardIds + selectedCardId
                                                        )
                                                    )
                                                    .addOnSuccessListener {
                                                        showToast(context, "Başvurunuz başarıyla gönderildi.")
                                                        hasApplied = true  // Başvuru durumunu güncelle
                                                        showBottomSheet = false
                                                    }
                                                    .addOnFailureListener { e ->
                                                        showToast(context, "Başvuru sırasında bir hata oluştu: ${e.message}")
                                                    }
                                            }
                                        }
                                    }
                            }
                        } else {
                            showToast(context, "Lütfen bir kartvizit seçin.")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = selectedCard != null && cards.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (cards.isEmpty()) "Kartvizit Oluşturun" else "Başvuruyu Gönder",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    )
                }
            }
        }
    }

    // Detay Bottom Sheet
    if (showDetailSheet) {
        JobDetailBottomSheet(
            jobPost = jobPost,
            onDismiss = { showDetailSheet = false }
        )
    }
}

private fun applyFilter(
    allPosts: List<JobPost>,
    category: JobCategoryHome?,
    userId: String?,
    onResult: (List<JobPost>) -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()

    // MY_POSTS kategorisi için Firestore sorgusu gerekli
    if (category == JobCategoryHome.MY_POSTS && userId != null) {
        // Cache kullanımını etkinleştir
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                try {
                    // Güvenli dönüşüm
                    val jobPostsData = document.get("jobPostIds")
                    val jobPostIds = when (jobPostsData) {
                        is List<*> -> jobPostsData.filterIsInstance<String>()
                        else -> emptyList()
                    }

                    // Bellek içi filtreleme - daha hızlı
                    val filteredPosts = if (jobPostIds.isEmpty()) {
                        emptyList()
                    } else {
                        allPosts.filter { it.id in jobPostIds && it.isActive }
                    }

                    onResult(filteredPosts)
                } catch (e: Exception) {
                    e.printStackTrace()
                    onResult(emptyList())
                }
            }
            .addOnFailureListener {
                it.printStackTrace()
                onResult(emptyList())  // Hata durumunda boş liste dön
            }
    } else {
        // Diğer kategoriler için bellek içi filtreleme - Firestore sorgusu yok
        val filteredPosts = when (category) {
            null -> allPosts.filter { it.isActive }
            else -> allPosts.filter { it.category == category.name && it.isActive }
        }

        // Hemen sonuç dön - asenkron işlem yok
        onResult(filteredPosts)
    }
}

// 🔥 Kategoriler ve ilgili resimler (ikon yerine)
enum class JobCategoryHome(val displayName: String, val imageRes: Int) {
    MY_POSTS("İlanlarım", R.drawable.bookmark),
    SOFTWARE("Yazılım", R.drawable.code),
    DESIGN("Tasarım", R.drawable.desing),
    MARKETING("Pazarlama", R.drawable.marketing),
    FINANCE("Finans", R.drawable.finance),
    SALES("Satış", R.drawable.sales),
    HR("İnsan Kaynakları", R.drawable.hr),
    OTHER("Diğer", R.drawable.other)
}

@Composable
fun ReportDialog(jobPostId: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val reasons = listOf("Yanıltıcı ilan", "Uygunsuz içerik", "Spam")
    var selectedReason by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Başlık
            Text(
                text = "İlanı Şikayet Et",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            // Sebep seçenekleri
            reasons.forEach { reason ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedReason = reason }
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (selectedReason == reason),
                        onClick = { selectedReason = reason },
                        modifier = Modifier.size(18.dp),
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.tertiary,
                            unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Butonlar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(text = "Vazgeç")
                }
                OutlinedButton(
                    onClick = {
                        if (selectedReason != null) {
                            reportJobPost(jobPostId, selectedReason!!, context)
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Lütfen bir sebep seçin", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text(text = "Gönder")
                }
            }
        }
    }
}

fun reportJobPost(jobPostId: String, reason: String, context: Context) {
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val adminUserId = "bhEx5ZPVyOY4YJ61FdaboFhfy1B2"

    val reportData = mapOf(
        "jobPostId" to jobPostId,
        "reporterUserId" to currentUserId,
        "reason" to reason,
        "reportedAt" to FieldValue.serverTimestamp(),
        "status" to "Pending"
    )

    firestore.collection("reports").add(reportData)
        .addOnSuccessListener { reportRef ->
            Toast.makeText(context, "Şikayetiniz alındı, teşekkürler.", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener {
            Toast.makeText(context, "Şikayet sırasında bir hata oluştu.", Toast.LENGTH_SHORT).show()
        }
}

// İlanları sayfalı olarak yükleyen fonksiyon
private fun loadJobPosts(
    firestore: FirebaseFirestore,
    pageSize: Int,
    lastPostId: String?,
    onSuccess: (List<JobPost>, String?, Boolean) -> Unit,
    onError: () -> Unit
) {
    var query = firestore.collection("jobPosts")
        .whereEqualTo("isActive", true)
        .limit(pageSize.toLong())

    // Eğer son ilan ID'si varsa, o ilandan sonrasını getir
    if (lastPostId != null) {
        // Önce son ilanın referansını al
        firestore.collection("jobPosts").document(lastPostId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    // Son ilandan sonraki ilanları getir
                    query = firestore.collection("jobPosts")
                        .whereEqualTo("isActive", true)
                        .orderBy("id")
                        .startAfter(documentSnapshot)
                        .limit(pageSize.toLong())

                    executeJobPostsQuery(query, pageSize, onSuccess, onError)
                } else {
                    onError()
                }
            }
            .addOnFailureListener {
                onError()
            }
    } else {
        // İlk sayfayı getir
        executeJobPostsQuery(query, pageSize, onSuccess, onError)
    }
}

private fun executeJobPostsQuery(
    query: com.google.firebase.firestore.Query,
    pageSize: Int,
    onSuccess: (List<JobPost>, String?, Boolean) -> Unit,
    onError: () -> Unit
) {
    query.get()
        .addOnSuccessListener { querySnapshot ->
            try {
                val posts = querySnapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(JobPost::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }

                // Son ilanın ID'sini ve daha fazla ilan olup olmadığını belirle
                val lastPostId = if (posts.isNotEmpty()) posts.last().id else null
                val hasMorePosts = posts.size >= pageSize

                onSuccess(posts, lastPostId, hasMorePosts)
            } catch (e: Exception) {
                e.printStackTrace()
                onError()
            }
        }
        .addOnFailureListener {
            it.printStackTrace()
            onError()
    }
}

// Gradyan renklerini getir
private fun getGradientColors(gradientName: String): List<Color> {
    return when (gradientName) {
        "ocean" -> listOf(Color(0xFF1A2980), Color(0xFF26D0CE))
        "purple" -> listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))
        "sunset" -> listOf(Color(0xFFFF416C), Color(0xFFFF4B2B))
        "forest" -> listOf(Color(0xFF134E5E), Color(0xFF71B280))
        "cherry" -> listOf(Color(0xFFEB3349), Color(0xFFF45C43))
        else -> listOf(Color.White, Color.LightGray) // Varsayılan
    }
}