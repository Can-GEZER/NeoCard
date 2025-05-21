package com.cangzr.neocard.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.LinearGradient
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import coil.transform.CircleCropTransformation
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.cangzr.neocard.R
import com.cangzr.neocard.Screen
import com.cangzr.neocard.data.CardType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import com.google.firebase.firestore.FieldValue
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import android.net.Uri
import android.os.Environment
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.draw.shadow
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.content.FileProvider
import com.cangzr.neocard.ui.screens.predefinedGradients // Gradyanları CreateCardScreen.kt'den import et
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

// onAppear modifier fonksiyonu
fun Modifier.onAppear(callback: () -> Unit): Modifier {
    return this.then(
        Modifier.onGloballyPositioned {
            callback()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    var cards by remember { mutableStateOf<List<UserCard>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showCardTypeDropdown by remember { mutableStateOf(false) }
    var selectedCardType by remember { mutableStateOf("Tümü") }
    
    // Kart tipleri listesi - "Tümü" ve CardType enum değerleri
    val cardTypes = listOf("Tümü") + CardType.entries.map { it.getTitle() }
    
    // Pagination için değişkenler
    var lastVisibleCard by remember { mutableStateOf<String?>(null) }
    var hasMoreCards by remember { mutableStateOf(true) }
    val pageSize = 10

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            // Sayfalama ile kart verilerini yükle
            loadCards(
                userId = currentUser.uid,
                firestore = firestore,
                pageSize = pageSize,
                lastCardId = null,
                onSuccess = { newCards, lastId, hasMore ->
                    cards = newCards
                    lastVisibleCard = lastId
                    hasMoreCards = hasMore
                    isLoading = false
                },
                onError = {
                    isLoading = false
                }
            )
        } else {
            // Kullanıcı giriş yapmamışsa yükleme durumunu kapat
            isLoading = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (isLoading) {
            // Yükleme göstergesi
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Kartvizit Galerisi Başlık
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { showCardTypeDropdown = true }
                                .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = if (selectedCardType == "Tümü") "Kartvizitlerim" else selectedCardType,
                        style = MaterialTheme.typography.titleMedium
                    )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Kart tipini seç",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        // Dropdown menü
                        DropdownMenu(
                            expanded = showCardTypeDropdown,
                            onDismissRequest = { showCardTypeDropdown = false },
                                        modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            cardTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { 
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                    Text(
                                                text = type,
                                                color = if (selectedCardType == type)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.onSurface
                                            )
                                            if (selectedCardType == type) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.filter),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedCardType = type
                                        showCardTypeDropdown = false
                                    }
                                )
                                    }
                                }
                            }
                    

                }

                // Kartvizit Galerisi
                UserCardGallery(
                    navController = navController,
                    filterType = if (selectedCardType == "Tümü") "Tümü"
                                else CardType.entries.first { it.getTitle() == selectedCardType }.name
                )

                // Başlık: Kart Keşfet
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.explore),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Diğer Kartvizitleri Keşfet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Kart Keşfet Bölümü (kompakt versiyon)
                ExploreCardsSection(navController = navController)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserCardGallery(navController: NavHostController, filterType: String) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    var cards by remember { mutableStateOf<List<UserCard>>(emptyList()) }
    var filteredCards by remember { mutableStateOf<List<UserCard>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var selectedCard by remember { mutableStateOf<UserCard?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    var selectedCardId by remember { mutableStateOf<String?>(null) }
    
    // Pagination için değişkenler
    var lastVisibleCard by remember { mutableStateOf<String?>(null) }
    var hasMoreCards by remember { mutableStateOf(true) }
    val pageSize = 10

    // İlk veri yüklemesi
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            loadCards(
                userId = currentUser.uid,
                firestore = firestore,
                pageSize = pageSize,
                lastCardId = null,
                onSuccess = { newCards, lastId, hasMore ->
                    cards = newCards
                    // Filtreleme işlemi
                    filteredCards = if (filterType == "Tümü") {
                        newCards
                    } else {
                        newCards.filter { it.cardType == filterType }
                    }
                    lastVisibleCard = lastId
                    hasMoreCards = hasMore
                    isLoading = false
                },
                onError = {
                    isLoading = false
                }
            )
        } else {
            // Kullanıcı giriş yapmamış, yükleme durumunu kapat
                isLoading = false
        }
    }

    // Filtreleme değiştiğinde yeniden filtrele
    LaunchedEffect(filterType, cards) {
        filteredCards = if (filterType == "Tümü") {
            cards
        } else {
            cards.filter { it.cardType == filterType }
        }
    }

    // Daha fazla kart yükleme fonksiyonu
    fun loadMoreCards() {
        if (!hasMoreCards || isLoadingMore || currentUser == null) return
        
        isLoadingMore = true
        loadCards(
            userId = currentUser.uid,
            firestore = firestore,
            pageSize = pageSize,
            lastCardId = lastVisibleCard,
            onSuccess = { newCards, lastId, hasMore ->
                val updatedCards = cards + newCards
                cards = updatedCards
                // Filtreleme işlemi
                filteredCards = if (filterType == "Tümü") {
                    updatedCards
                } else {
                    updatedCards.filter { it.cardType == filterType }
                }
                lastVisibleCard = lastId
                hasMoreCards = hasMore
                isLoadingMore = false
            },
            onError = {
                isLoadingMore = false
            }
        )
    }

    // Kullanıcı giriş yapmamışsa giriş yapma uyarısı göster
    if (currentUser == null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.cards),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                
                Text(
                    text = "Kartvizitlerinizi oluşturup yönetmek için giriş yapın",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                
                Button(
                    onClick = { navController.navigate(Screen.Auth.route) },
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text("Giriş Yap")
                }
            }
        }
        return
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (filteredCards.isEmpty()) {
        // Kullanıcının hiç kartı yoksa veya filtrelenmiş sonuç boşsa uyarı göster
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (cards.isEmpty()) 
                    "Henüz kartvizit oluşturulmadı.\nYeni bir kartvizit eklemek için + simgesine tıklayın."
                else 
                    "Seçili tipte kartvizit bulunamadı.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    } else {
        Box(modifier = Modifier.fillMaxWidth()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 8.dp)
            ) {
                items(filteredCards) { card ->
                    UserCardItem(card = card, onClick = { selectedCard = card })
                }
                
                // Daha fazla kart varsa yükleme göstergesi
                if (hasMoreCards) {
                    item {
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(180.dp),
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
                                        .onAppear {
                                            loadMoreCards()
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun generateQRCode(content: String): Bitmap? {
        return try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                "https://neocardapp.com/card/$content",
                BarcodeFormat.QR_CODE,
                250,
                250
            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.Black.hashCode() else Color.White.hashCode())
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current
    var selectedCardForShare by remember { mutableStateOf<UserCard?>(null) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    var isExporting by remember { mutableStateOf(false) }

    fun copyToClipboard(context: Context, cardId: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Kartvizit Linki", "https://neocardapp.com/card/$cardId")
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Link kopyalandı", Toast.LENGTH_SHORT).show()
    }
    
    fun shareLink(context: Context, cardId: String) {
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "NeoCard Kartvizitim")
            putExtra(Intent.EXTRA_TEXT, """
                NeoCard Kartvizitimi görüntülemek için:
                🌐 https://neocardapp.com/card/$cardId
            """.trimIndent())
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        try {
            context.startActivity(Intent.createChooser(intent, "Kartviziti Paylaş"))
        } catch (e: Exception) {
            Toast.makeText(context, "Paylaşım yapılırken bir hata oluştu", Toast.LENGTH_SHORT).show()
        }
    }

    // Kartvizit bitmap'ini oluşturan fonksiyon
    // CreateCardScreen.kt'den predefinedGradients listesini kullanıyor
    suspend fun createCardBitmap(card: UserCard, context: Context): Bitmap? {
        return try {
            // Kartvizit boyutları (piksel bazında)
            val density = context.resources.displayMetrics.density
            val width = (300 * density).toInt()
            val height = (180 * density).toInt()

            // Bitmap oluştur
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)

            // Metin stillerini hazırla (ikon renkleri için)
            val nameStyle = card.textStyles["NAME_SURNAME"]
            val nameColor = android.graphics.Color.parseColor(nameStyle?.color ?: "#000000")

            // Arkaplan için Paint nesnesi
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
            }
            
            val rectF = android.graphics.RectF(0f, 0f, width.toFloat(), height.toFloat())
            val cornerRadius = 16 * density
            
            if (card.backgroundType == "GRADIENT") {
                // Gradyan arkaplanı için predefinedGradients'ten doğru gradyanı bul
                val gradient = predefinedGradients.firstOrNull { it.first == card.selectedGradient }
                
                if (gradient != null) {
                    // Gradyandan renkleri al
                    val colors = when {
                        gradient.first == "Gün Batımı" -> intArrayOf(
                            Color(0xFFFE6B8B).toArgb(),
                            Color(0xFFFF8E53).toArgb()
                        )
                        gradient.first == "Okyanus" -> intArrayOf(
                            Color(0xFF2196F3).toArgb(),
                            Color(0xFF00BCD4).toArgb()
                        )
                        gradient.first == "Orman" -> intArrayOf(
                            Color(0xFF4CAF50).toArgb(),
                            Color(0xFF8BC34A).toArgb()
                        )
                        gradient.first == "Gece" -> intArrayOf(
                            Color(0xFF2C3E50).toArgb(),
                            Color(0xFF3498DB).toArgb()
                        )
                        gradient.first == "Mor Sis" -> intArrayOf(
                            Color(0xFF9C27B0).toArgb(),
                            Color(0xFFE91E63).toArgb()
                        )
                        else -> intArrayOf(
                            Color(0xFF2196F3).toArgb(),
                            Color(0xFF00BCD4).toArgb()
                        )
                    }
                    
                    // Gradyanın yönünü belirle (yatay mı dikey mi?)
                    val isVertical = gradient.first == "Gece" || gradient.first == "Mor Sis"
                    
                    // Ona göre LinearGradient oluştur
                    val shader = if (isVertical) {
                        android.graphics.LinearGradient(
                            0f, 0f, 0f, height.toFloat(),
                            colors[0], colors[1],
                            android.graphics.Shader.TileMode.CLAMP
                        )
                    } else {
                        android.graphics.LinearGradient(
                            0f, 0f, width.toFloat(), 0f,
                            colors[0], colors[1],
                            android.graphics.Shader.TileMode.CLAMP
                        )
                    }
                    
                    paint.shader = shader
                } else {
                    // Gradyan bulunamazsa düz renk kullan
                    paint.color = android.graphics.Color.parseColor(card.backgroundColor)
                }
            } else {
                // Düz renk arkaplan
                paint.color = android.graphics.Color.parseColor(card.backgroundColor)
            }
            
            // Arkaplanı çiz
            canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint)

            // İçerik başlangıç koordinatları
            val contentPadding = 16 * density
            var contentStartX = contentPadding
            val contentStartY = contentPadding
            var rightColumnStartX = contentStartX

            // Profil resmi varsa çiz
            var profileImageDrawn = false
            if (card.profileImageUrl != null && card.profileImageUrl.isNotEmpty()) {
                try {
                    // Profil resmini yükle
                    val profileImageBitmap = loadProfileImage(context, card.profileImageUrl)
                    if (profileImageBitmap != null) {
                        // Profil resmini yuvarlak olarak çiz
                        val profileImageSize = (64 * density).toInt()
                        val profileImageRect = android.graphics.RectF(
                            contentStartX,
                            contentStartY,
                            contentStartX + profileImageSize,
                            contentStartY + profileImageSize
                        )

                        // Yuvarlak kırpma maskesi oluştur
                        val path = android.graphics.Path().apply {
                            addCircle(
                                profileImageRect.centerX(),
                                profileImageRect.centerY(),
                                profileImageSize / 2f,
                                android.graphics.Path.Direction.CW
                            )
                        }

                        canvas.save()
                        canvas.clipPath(path)
                        canvas.drawBitmap(
                            profileImageBitmap,
                            null,
                            profileImageRect,
                            android.graphics.Paint().apply { isAntiAlias = true }
                        )
                        canvas.restore()

                        profileImageDrawn = true
                        rightColumnStartX = contentStartX + profileImageSize + (8 * density)
                    } else {
                        // Profil resmi yüklenemezse logo göster
                        val logoDrawable = context.resources.getDrawable(R.drawable.logo3, null)
                        logoDrawable.setBounds(
                            contentStartX.toInt(),
                            contentStartY.toInt(),
                            (contentStartX + 64 * density).toInt(),
                            (contentStartY + 64 * density).toInt()
                        )
                        
                        logoDrawable.draw(canvas)
                        
                        profileImageDrawn = true
                        rightColumnStartX = contentStartX + (64 * density) + (8 * density)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Hata durumunda logo göster
                    val logoDrawable = context.resources.getDrawable(R.drawable.logo3, null)
                    logoDrawable.setBounds(
                        contentStartX.toInt(),
                        contentStartY.toInt(),
                        (contentStartX + 64 * density).toInt(),
                        (contentStartY + 64 * density).toInt()
                    )
                    
                    logoDrawable.draw(canvas)
                    
                    profileImageDrawn = true
                    rightColumnStartX = contentStartX + (64 * density) + (8 * density)
                }
            }

            // Metin stillerini hazırla
            val titleStyle = card.textStyles["TITLE"]
            val companyStyle = card.textStyles["COMPANY"]
            val emailStyle = card.textStyles["EMAIL"]
            val phoneStyle = card.textStyles["PHONE"]

            // Metin renkleri
            val titleColor = android.graphics.Color.parseColor(titleStyle?.color ?: "#000000")
            val companyColor = android.graphics.Color.parseColor(companyStyle?.color ?: "#000000")
            val emailColor = android.graphics.Color.parseColor(emailStyle?.color ?: "#000000")
            val phoneColor = android.graphics.Color.parseColor(phoneStyle?.color ?: "#000000")

            // Metin çizme fonksiyonu
            val textPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.FILL
                color = nameColor
            }

            // İsim soyisim
            textPaint.apply {
                textSize = (nameStyle?.fontSize ?: 16f) * density * 1.2f
                color = nameColor
                isFakeBoldText = nameStyle?.isBold == true
                isUnderlineText = nameStyle?.isUnderlined == true
                typeface = if (nameStyle?.isItalic == true)
                    android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC)
                else
                    android.graphics.Typeface.DEFAULT
            }
            
            var textY = contentStartY
            if (profileImageDrawn) {
                textY += (textPaint.textSize / 2)
            } else {
                textY += textPaint.textSize
            }
            
            canvas.drawText("${card.name} ${card.surname}", rightColumnStartX, textY, textPaint)

            // Başlık
            textPaint.apply {
                textSize = (titleStyle?.fontSize ?: 14f) * density
                color = titleColor
                isFakeBoldText = titleStyle?.isBold == true
                isUnderlineText = titleStyle?.isUnderlined == true
                typeface = if (titleStyle?.isItalic == true)
                    android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC)
                else
                    android.graphics.Typeface.DEFAULT
            }
            textY += textPaint.textSize + (8 * density)
            canvas.drawText(card.title, rightColumnStartX, textY, textPaint)

            // Şirket
            textPaint.apply {
                textSize = (companyStyle?.fontSize ?: 14f) * density
                color = companyColor
                isFakeBoldText = companyStyle?.isBold == true
                isUnderlineText = companyStyle?.isUnderlined == true
                typeface = if (companyStyle?.isItalic == true)
                    android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC)
                else
                    android.graphics.Typeface.DEFAULT
            }
            textY += textPaint.textSize + (8 * density)
            canvas.drawText(card.company, rightColumnStartX, textY, textPaint)

            // Email
            textPaint.apply {
                textSize = (emailStyle?.fontSize ?: 14f) * density
                color = emailColor
                isFakeBoldText = emailStyle?.isBold == true
                isUnderlineText = emailStyle?.isUnderlined == true
                typeface = if (emailStyle?.isItalic == true)
                    android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC)
                else
                    android.graphics.Typeface.DEFAULT
            }
            textY += textPaint.textSize + (8 * density)
            canvas.drawText(card.email, rightColumnStartX, textY, textPaint)

            // Telefon
            textPaint.apply {
                textSize = (phoneStyle?.fontSize ?: 14f) * density
                color = phoneColor
                isFakeBoldText = phoneStyle?.isBold == true
                isUnderlineText = phoneStyle?.isUnderlined == true
                typeface = if (phoneStyle?.isItalic == true)
                    android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC)
                else
                    android.graphics.Typeface.DEFAULT
            }
            textY += textPaint.textSize + (8 * density)
            canvas.drawText(card.phone, rightColumnStartX, textY, textPaint)

            // Sosyal medya ikonları
            val iconSize = (24 * density).toInt()
            val iconSpacing = 8 * density
            var iconX = contentPadding

            // Alt satırda sosyal medya ikonları
            val socialIcons = mutableListOf<Pair<String, Int>>()
            if (card.website.isNotEmpty()) socialIcons.add(Pair("web", R.drawable.web))
            if (card.linkedin.isNotEmpty()) socialIcons.add(Pair("linkedin", R.drawable.linkedin))
            if (card.github.isNotEmpty()) socialIcons.add(Pair("github", R.drawable.github))
            if (card.twitter.isNotEmpty()) socialIcons.add(Pair("twitter", R.drawable.twitt))
            if (card.instagram.isNotEmpty()) socialIcons.add(Pair("instagram", R.drawable.insta))
            if (card.facebook.isNotEmpty()) socialIcons.add(Pair("facebook", R.drawable.face))

            // İkonları çiz
            val iconY = height - contentPadding - iconSize
            socialIcons.forEach { (_, iconRes) ->
                try {
                    val iconDrawable = context.resources.getDrawable(iconRes, null)
                    iconDrawable.setBounds(
                        iconX.toInt(),
                        iconY.toInt(),
                        (iconX + iconSize).toInt(),
                        (iconY + iconSize).toInt()
                    )
                    
                    // İkonun rengini isim rengiyle aynı yap
                    iconDrawable.setTint(nameColor)
                    
                    iconDrawable.draw(canvas)
                    iconX += iconSize + iconSpacing
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            // "NeoCard ile oluşturuldu" ibaresi ekle
            val watermarkPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.FILL
                color = android.graphics.Color.parseColor("#80000000") // Yarı saydam siyah
                textSize = 10 * density
                textAlign = android.graphics.Paint.Align.RIGHT
            }
            
            // Watermark metni
            val watermarkText = "NeoCard ile oluşturuldu"
            
            // Watermark konumu - sağ alt köşe
            val watermarkX = width - contentPadding
            val watermarkY = height - 4 * density
            
            // Watermark'ı çiz
            canvas.drawText(watermarkText, watermarkX, watermarkY, watermarkPaint)

            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Kartviziti görsel olarak paylaşma fonksiyonu
    fun shareCardAsImage(card: UserCard) {
        isExporting = true
        coroutineScope.launch {
            try {
                // Kartviziti programatik olarak çizecek fonksiyonu çağır
                val bitmap = createCardBitmap(card, context)
                
                if (bitmap == null) {
                    Toast.makeText(context, "Kartvizit görüntüsü oluşturulamadı.", Toast.LENGTH_SHORT).show()
                    isExporting = false
                    return@launch
                }
                
                // Bitmap'i dosyaya kaydet
                val imagesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                val imageFile = File(imagesDir, "kartvizit_${card.id}.png")
                
                withContext(Dispatchers.IO) {
                    val outputStream = FileOutputStream(imageFile)
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    outputStream.flush()
                    outputStream.close()
                }
                
                // FileProvider kullanarak URI oluştur
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    imageFile
                )
                
                // Paylaşım linki
                val shareLink = "https://neocardapp.com/card/${card.id}"
                
                // Paylaşım intenti oluştur
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "NeoCard Kartvizitim")
                    putExtra(Intent.EXTRA_TEXT, """
                        NeoCard uygulamasından kartvizitim
                        
                        📱 Kartvizitimi görüntülemek için:
                        🌐 $shareLink
                    """.trimIndent())
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                
                context.startActivity(Intent.createChooser(shareIntent, "Kartviziti Paylaş"))
                isExporting = false
                
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Paylaşım sırasında bir hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
                isExporting = false
            }
        }
    }



    if (selectedCard != null) {
        AlertDialog(
            onDismissRequest = { selectedCard = null },
            title = { Text("Kartvizit İşlemleri") },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            selectedCard?.let { card ->
                                navController.navigate(Screen.CardDetail.createRoute(card.id))
                            }
                            selectedCard = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                            Text("Detayları Görüntüle")
                        }
                    }
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                selectedCard?.let { card ->
                                    qrCodeBitmap = generateQRCode("${card.id}")
                                    selectedCardId = card.id
                                    selectedCardForShare = card
                                }
                                selectedCard = null
                                showSheet = true
                                selectedTabIndex = 0 // Varsayılan olarak QR tab'ini göster
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null)
                            Text("Kartvizit Paylaş")
                        }
                    }
                }
            }
        )
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Paylaş", style = MaterialTheme.typography.titleMedium)

                // Tab yapısı ile farklı paylaşım seçenekleri arasında geçiş
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("QR Kod") },
                        icon = { 
                            Icon(
                                painter = painterResource(id = R.drawable.qr_code), 
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            ) 
                        }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Görsel") },
                        icon = { 
                            Icon(
                                painter = painterResource(id = R.drawable.image), 
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            ) 
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                when (selectedTabIndex) {
                    0 -> {
                // QR Kod Gösterimi
                qrCodeBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "QR Kod",
                        modifier = Modifier.size(250.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Link ve Paylaşım Butonları
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "www.neocardapp.com/card/${selectedCardId}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.copy),
                            contentDescription = "Kopyala",
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { copyToClipboard(context, selectedCardId!!) },
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                        )
                        Image(
                            painter = painterResource(id = R.drawable.export),
                            contentDescription = "Paylaş",
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { shareLink(context, selectedCardId!!) },
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                        )
                            }
                        }
                    }
                    1 -> {
                        // Kartvizit Görsel Önizlemesi
                        if (selectedCardForShare != null) {
                            Box(modifier = Modifier
                                .width(300.dp)
                                .height(180.dp)
                                .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp))
                            ) {
                                UserCardItemForExport(
                                    card = selectedCardForShare!!,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Görsel Paylaşım Butonu
                            Button(
                                onClick = { 
                                    selectedCardForShare?.let { card ->
                                        shareCardAsImage(card)
                                    } 
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isExporting,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isExporting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            painter = painterResource(id = R.drawable.share),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Text(
                                        text = if (isExporting) "Dışa Aktarılıyor..." else "Görsel Olarak Paylaş",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Kapat Butonu
                Text(
                    text = "Kapat",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.clickable { showSheet = false }
                )
            }
        }
    }
}

@Composable
fun SocialIcon(iconRes: Int, color: Color) {
    Image(
        painter = painterResource(id = iconRes),
        contentDescription = null,
        modifier = Modifier.size(24.dp),
        colorFilter = ColorFilter.tint(color)
    )
}

@Composable
fun UserCardItem(card: UserCard, onClick: () -> Unit) {
    val nameSurnameColor = Color(android.graphics.Color.parseColor(card.textStyles["NAME_SURNAME"]?.color ?: "#000000"))
    val isDemoCard = card.id == "demo"
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .width(300.dp)
            .height(180.dp)
            .clickable(enabled = !isDemoCard) { onClick() }
            .alpha(if (isDemoCard) 0.7f else 1f),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(1f)
                .background(parseBackground(card))
                .padding(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().fillMaxSize()
            ) {
                // Üst kısım: Profil resmi + Bilgiler
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Profil resmi (varsa göster)
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
                                .size(64.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Bilgiler
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("${card.name} ${card.surname}", style = parseTextStyle(card.textStyles["NAME_SURNAME"]))
                        Text(card.title, style = parseTextStyle(card.textStyles["TITLE"]))
                        Text(card.company, style = parseTextStyle(card.textStyles["COMPANY"]))
                        Text(card.email, style = parseTextStyle(card.textStyles["EMAIL"]))
                        Text(card.phone, style = parseTextStyle(card.textStyles["PHONE"]))
                    }
                }

                // Alt kısım: Sosyal Medya İkonları (sola hizalı)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (card.website.isNotEmpty()) SocialIcon(R.drawable.web, nameSurnameColor)
                    if (card.linkedin.isNotEmpty()) SocialIcon(R.drawable.linkedin, nameSurnameColor)
                    if (card.github.isNotEmpty()) SocialIcon(R.drawable.github, nameSurnameColor)
                    if (card.twitter.isNotEmpty()) SocialIcon(R.drawable.twitt, nameSurnameColor)
                    if (card.instagram.isNotEmpty()) SocialIcon(R.drawable.insta, nameSurnameColor)
                    if (card.facebook.isNotEmpty()) SocialIcon(R.drawable.face, nameSurnameColor)
                }
            }
        }
    }
}

fun parseBackground(card: UserCard): Brush {
    return if (card.backgroundType == "GRADIENT") {
        predefinedGradients.firstOrNull { it.first == card.selectedGradient }?.second
            ?: Brush.verticalGradient(listOf(Color.Gray, Color.LightGray))
    } else {
        Brush.verticalGradient(
            listOf(
                Color(android.graphics.Color.parseColor(card.backgroundColor)),
                Color(android.graphics.Color.parseColor(card.backgroundColor))
            )
        )
    }
}

fun parseTextStyle(dto: TextStyleDTO?): androidx.compose.ui.text.TextStyle {
    return androidx.compose.ui.text.TextStyle(
        fontSize = (dto?.fontSize?.sp ?: 16.sp),
        fontWeight = if (dto?.isBold == true) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
        fontStyle = if (dto?.isItalic == true) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
        textDecoration = if (dto?.isUnderlined == true) androidx.compose.ui.text.style.TextDecoration.Underline else androidx.compose.ui.text.style.TextDecoration.None,
        color = Color(android.graphics.Color.parseColor(dto?.color ?: "#000000"))
    )
}

// Yeni Data Class: UserCard
data class UserCard(
    val id: String = "",
    val name: String = "",
    val surname: String = "",
    val phone: String = "",
    val email: String = "",
    val company: String = "",
    val title: String = "",
    val website: String = "",
    val linkedin: String = "",
    val instagram: String = "",
    val twitter: String = "",
    val facebook: String = "",
    val github: String = "",
    val backgroundType: String = "SOLID",
    val backgroundColor: String = "#FFFFFF",
    val selectedGradient: String = "",
    val profileImageUrl: String? = "",  // 👈 Burası önemli!
    val cardType: String = "Genel",
    val textStyles: Map<String, TextStyleDTO> = emptyMap(),
    val bio: String = "",  // Biyografi alanı
    val cv: String = "",    // CV linki
    val isPublic: Boolean = true  // Kartvizit paylaşım ayarı
) {
    // Kart verilerini Map'e dönüştürme fonksiyonu
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "surname" to surname,
            "phone" to phone,
            "email" to email,
            "company" to company,
            "title" to title,
            "website" to website,
            "linkedin" to linkedin,
            "instagram" to instagram,
            "twitter" to twitter,
            "facebook" to facebook,
            "github" to github,
            "backgroundType" to backgroundType,
            "backgroundColor" to backgroundColor,
            "selectedGradient" to selectedGradient,
            "profileImageUrl" to profileImageUrl,
            "cardType" to cardType,
            "textStyles" to textStyles.mapKeys { it.key }.mapValues { (_, style) ->
                mapOf(
                    "isBold" to style.isBold,
                    "isItalic" to style.isItalic,
                    "isUnderlined" to style.isUnderlined,
                    "fontSize" to style.fontSize,
                    "color" to style.color
                )
            },
            "bio" to bio,
            "cv" to cv,
            "isPublic" to isPublic
        )
    }
}

data class TextStyleDTO(
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderlined: Boolean = false,
    val fontSize: Float = 16f,
    val color: String = "#000000"
)

// Toast mesajı göstermek için yardımcı fonksiyon
fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

// Kartları sayfalı olarak yükleyen fonksiyon
private fun loadCards(
    userId: String,
    firestore: FirebaseFirestore,
    pageSize: Int,
    lastCardId: String?,
    onSuccess: (List<UserCard>, String?, Boolean) -> Unit,
    onError: () -> Unit
) {
    var query = firestore.collection("users").document(userId).collection("cards")
        .limit(pageSize.toLong())
    
    // Eğer son kart ID'si varsa, o karttan sonrasını getir
    if (lastCardId != null) {
        // Önce son kartın referansını al
        firestore.collection("users").document(userId).collection("cards")
            .document(lastCardId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    // Son karttan sonraki kartları getir
                    query = firestore.collection("users").document(userId).collection("cards")
                        .orderBy("id")
                        .startAfter(documentSnapshot)
                        .limit(pageSize.toLong())
                    
                    executeQuery(query, pageSize, onSuccess, onError)
                } else {
                    onError()
                }
            }
            .addOnFailureListener {
                onError()
            }
    } else {
        // İlk sayfayı getir
        executeQuery(query, pageSize, onSuccess, onError)
    }
}

private fun executeQuery(
    query: com.google.firebase.firestore.Query,
    pageSize: Int,
    onSuccess: (List<UserCard>, String?, Boolean) -> Unit,
    onError: () -> Unit
) {
    query.get()
        .addOnSuccessListener { querySnapshot ->
            try {
                val cards = querySnapshot.documents.mapNotNull { doc ->
                    try {
                        // Temel UserCard nesnesini oluştur
                        val card = doc.toObject(UserCard::class.java)?.copy(
                            id = doc.id, 
                            cardType = doc.getString("cardType") ?: "Genel"
                        )

                        // textStyles alanını manuel olarak parse et
                        val textStylesMap = doc.get("textStyles") as? Map<String, Any>
                        
                        if (card != null && textStylesMap != null) {
                            // Her bir text stili için TextStyleDTO oluştur
                            val parsedTextStyles = mutableMapOf<String, TextStyleDTO>()
                            
                            textStylesMap.forEach { (key, value) ->
                                val styleMap = value as? Map<String, Any>
                                if (styleMap != null) {
                                    val textStyle = TextStyleDTO(
                                        isBold = styleMap["isBold"] as? Boolean ?: false,
                                        isItalic = styleMap["isItalic"] as? Boolean ?: false,
                                        isUnderlined = styleMap["isUnderlined"] as? Boolean ?: false,
                                        fontSize = (styleMap["fontSize"] as? Number)?.toFloat() ?: 16f,
                                        color = styleMap["color"] as? String ?: "#000000"
                                    )
                                    parsedTextStyles[key] = textStyle
                                }
                            }
                            
                            // Doğru yapılandırılmış textStyles ile yeni bir UserCard döndür
                            card.copy(textStyles = parsedTextStyles)
                        } else {
                            card // Stil bilgisi yoksa orijinal kartı döndür
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                
                // Son kartın ID'sini ve daha fazla kart olup olmadığını belirle
                val lastCardId = if (cards.isNotEmpty()) cards.last().id else null
                val hasMoreCards = cards.size >= pageSize
                
                onSuccess(cards, lastCardId, hasMoreCards)
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

// Uygulama başlangıcında çağrılacak Coil yapılandırma fonksiyonu
fun initializeCoil(context: Context) {
    val imageLoader = coil.ImageLoader.Builder(context)
        .memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(0.25) // Kullanılabilir belleğin %25'ini kullan
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizePercent(0.02) // Disk alanının %2'sini kullan
                .build()
        }
        .crossfade(true)
        .build()
    
    coil.Coil.setImageLoader(imageLoader)
}

// Görsel dışa aktarım için kartvizit bileşeni (interaktif olmayan sürüm)
@Composable
fun UserCardItemForExport(card: UserCard, modifier: Modifier = Modifier) {
    val nameSurnameColor = Color(android.graphics.Color.parseColor(card.textStyles["NAME_SURNAME"]?.color ?: "#000000"))
    val context = LocalContext.current

    Box(
        modifier = modifier
            .width(300.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(parseBackground(card))
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().fillMaxSize()
        ) {
            // Üst kısım: Profil resmi + Bilgiler
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Profil resmi (varsa göster)
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
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Bilgiler
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("${card.name} ${card.surname}", style = parseTextStyle(card.textStyles["NAME_SURNAME"]))
                    Text(card.title, style = parseTextStyle(card.textStyles["TITLE"]))
                    Text(card.company, style = parseTextStyle(card.textStyles["COMPANY"]))
                    Text(card.email, style = parseTextStyle(card.textStyles["EMAIL"]))
                    Text(card.phone, style = parseTextStyle(card.textStyles["PHONE"]))
                }
            }

            // Alt kısım: Sosyal Medya İkonları (sola hizalı)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (card.website.isNotEmpty()) SocialIcon(R.drawable.web, nameSurnameColor)
                if (card.linkedin.isNotEmpty()) SocialIcon(R.drawable.linkedin, nameSurnameColor)
                if (card.github.isNotEmpty()) SocialIcon(R.drawable.github, nameSurnameColor)
                if (card.twitter.isNotEmpty()) SocialIcon(R.drawable.twitt, nameSurnameColor)
                if (card.instagram.isNotEmpty()) SocialIcon(R.drawable.insta, nameSurnameColor)
                if (card.facebook.isNotEmpty()) SocialIcon(R.drawable.face, nameSurnameColor)
            }
        }
    }
}

// Profil resmini yüklemek için yardımcı fonksiyon
private suspend fun loadProfileImage(context: Context, imageUrl: String): android.graphics.Bitmap? {
    return try {
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .size(Size.ORIGINAL)
            .allowHardware(false)
            .build()

        val result = coil.Coil.imageLoader(context).execute(request).drawable
        (result as? android.graphics.drawable.BitmapDrawable)?.bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Kart Keşfet Bölümü (kompakt versiyon)
@Composable
fun ExploreCardsSection(navController: NavHostController) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid

    var exploreCards by remember { mutableStateOf<List<ExploreUserCard>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    // İlk veri yüklemesi
    LaunchedEffect(Unit) {
        loadExploreCards(
            firestore = firestore,
            currentUserId = currentUserId,
            pageSize = 10,
            lastCardId = null,
            onSuccess = { cards, _, _ ->
                exploreCards = cards
                    isLoading = false
            },
            onError = {
                isLoading = false
            }
        )
    }
    
                    Column(
                            modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Arama Çubuğu
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { query ->
                searchQuery = query
                // Arama sonuçlarını filtreleme
                if (query.isEmpty()) {
                    loadExploreCards(
                        firestore = firestore,
                        currentUserId = currentUserId,
                        pageSize = 10,
                        lastCardId = null,
                        onSuccess = { cards, _, _ ->
                            exploreCards = cards
                        },
                        onError = { }
                    )
                } else {
                    firestore.collection("public_cards")
                        .whereEqualTo("isPublic", true)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            try {
                                val cards = querySnapshot.documents.mapNotNull { doc ->
                                    try {
                                        // Eğer bu kart kullanıcının kendi kartıysa atla
                                        val userId = doc.getString("userId") ?: ""
                                        if (userId == currentUserId) {
                                            return@mapNotNull null
                                        }
                                        
                                        // Filtreleme yap
                                        val name = doc.getString("name") ?: ""
                                        val surname = doc.getString("surname") ?: ""
                                        val title = doc.getString("title") ?: ""
                                        val company = doc.getString("company") ?: ""
                                        
                                        if (name.contains(query, ignoreCase = true) || 
                                            surname.contains(query, ignoreCase = true) || 
                                            title.contains(query, ignoreCase = true) || 
                                            company.contains(query, ignoreCase = true)) {
                                            
                                            ExploreUserCard(
                                                id = doc.id,
                                                name = name,
                                                surname = surname,
                                                title = title,
                                                company = company,
                                                cardType = doc.getString("cardType") ?: CardType.BUSINESS.name,
                                                profileImageUrl = doc.getString("profileImageUrl") ?: "",
                                                userId = userId,
                                                isPublic = doc.getBoolean("isPublic") ?: true
                                            )
                                        } else null
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        null
                                    }
                                }
                                exploreCards = cards
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("İsim, ünvan veya şirket ara...") },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.search),
                                                contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Temizle",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            ),
            singleLine = true
        )
        
        // Kart Listesi
        if (isLoading) {
                    Box(
                        modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (exploreCards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                        painter = painterResource(id = R.drawable.search),
                                contentDescription = null,
                        modifier = Modifier.size(80.dp),
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            )
                            Text(
                        text = if (searchQuery.isEmpty()) "Henüz keşfedilecek kart yok." 
                               else "Aramanızla eşleşen kart bulunamadı.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
            ) {
                items(exploreCards) { card ->
                    ExploreCardItemCompact(
                        card = card,
                        navController = navController
                    )
                }
            }

            // Daha Fazla Göster Butonu
            if (exploreCards.size >= 10) {
                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(
                        onClick = {
                            // TODO: Buraya tüm kartlar için bir sayfa eklenebilir
                        },
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.explore),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tümünü Gör")
                    }
                }
            }
        }
    }
}

// Kompakt Keşif Kartı Bileşeni
@Composable
fun ExploreCardItemCompact(
    card: ExploreUserCard,
    navController: NavHostController
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .width(200.dp)
            .height(240.dp)
            .clickable { navController.navigate(Screen.SharedCardDetail.createRoute(card.id)) },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Profil Resmi
                    Box(
                        modifier = Modifier
                    .size(80.dp)
                            .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), CircleShape)
                    ) {
                if (card.profileImageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                            .data(card.profileImageUrl)
                                .crossfade(true)
                                .transformations(CircleCropTransformation())
                            .placeholder(R.drawable.person)
                            .error(R.drawable.person)
                                .build(),
                        contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.person),
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.Center),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Kart Bilgileri
                        Text(
                text = "${card.name} ${card.surname}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
                        )
            
                        Text(
                text = card.title,
                            style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = card.company,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Kartın Türü
            val cardType = CardType.entries.find { it.name == card.cardType } ?: CardType.BUSINESS
            val cardTypeTitle = cardType.getTitle()
            val cardTypeIcon = when (cardType) {
                CardType.BUSINESS -> R.drawable.business
                CardType.PERSONAL -> R.drawable.personal
                CardType.FREELANCE -> R.drawable.freelance
                CardType.EDUCATION -> R.drawable.education
                CardType.SOCIAL -> R.drawable.social
            }
            
            Row(
                horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                    painter = painterResource(id = cardTypeIcon),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = cardTypeTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Görüntüle Butonu
                Button(
                onClick = { navController.navigate(Screen.SharedCardDetail.createRoute(card.id)) },
                modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.eye),
                    contentDescription = "Görüntüle",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            Text(
                    text = "Görüntüle",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

// Keşif Kartı Veri Sınıfı
data class ExploreUserCard(
    val id: String = "",
    val name: String = "",
    val surname: String = "",
    val title: String = "",
    val company: String = "",
    val cardType: String = CardType.BUSINESS.name,
    val profileImageUrl: String = "",
    val userId: String = "",
    val isPublic: Boolean = true
)

// Keşif Kartlarını Yükleme Fonksiyonu
private fun loadExploreCards(
    firestore: FirebaseFirestore,
    currentUserId: String?,
    pageSize: Int,
    lastCardId: String?,
    onSuccess: (List<ExploreUserCard>, String?, Boolean) -> Unit,
    onError: () -> Unit
) {
    var query = firestore.collection("public_cards")
        .whereEqualTo("isPublic", true)
        .limit(pageSize.toLong())
    
    // Eğer son kart ID'si varsa, o karttan sonrasını getir
    if (lastCardId != null) {
        firestore.collection("public_cards")
            .document(lastCardId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    query = firestore.collection("public_cards")
                        .whereEqualTo("isPublic", true)
                        .orderBy("id")
                        .startAfter(documentSnapshot)
                        .limit(pageSize.toLong())
                    
                    executeExploreQuery(query, pageSize, currentUserId, onSuccess, onError)
                } else {
                    onError()
                }
            }
            .addOnFailureListener {
                onError()
            }
    } else {
        // İlk sayfayı getir
        executeExploreQuery(query, pageSize, currentUserId, onSuccess, onError)
    }
}

private fun executeExploreQuery(
    query: com.google.firebase.firestore.Query,
    pageSize: Int,
    currentUserId: String?,
    onSuccess: (List<ExploreUserCard>, String?, Boolean) -> Unit,
    onError: () -> Unit
) {
    query.get()
        .addOnSuccessListener { querySnapshot ->
            try {
                val cards = querySnapshot.documents.mapNotNull { doc ->
                    try {
                        // Eğer bu kart kullanıcının kendi kartıysa atla
                        val userId = doc.getString("userId") ?: ""
                        if (userId == currentUserId) {
                            return@mapNotNull null
                        }
                        
                        ExploreUserCard(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            surname = doc.getString("surname") ?: "",
                            title = doc.getString("title") ?: "",
                            company = doc.getString("company") ?: "",
                            cardType = doc.getString("cardType") ?: CardType.BUSINESS.name,
                            profileImageUrl = doc.getString("profileImageUrl") ?: "",
                            userId = userId,
                            isPublic = doc.getBoolean("isPublic") ?: true
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                
                // Son kartın ID'sini ve daha fazla kart olup olmadığını belirle
                val lastCardId = if (cards.isNotEmpty()) cards.last().id else null
                val hasMoreCards = cards.size >= pageSize
                
                onSuccess(cards, lastCardId, hasMoreCards)
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

