package com.cangzr.neocard.ui.screens.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cangzr.neocard.R
import com.cangzr.neocard.data.model.UserCard
import com.cangzr.neocard.ui.screens.home.components.parseBackground
import com.cangzr.neocard.ui.screens.home.components.parseTextStyle
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import coil.transform.CircleCropTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun ShareBottomSheet(
    card: UserCard,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    
    // Paylaşım launcher'ı
    val shareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { /* Paylaşım tamamlandı */ }
    
    // Kartvizit bitmap'ini oluştur - UserCard boyutunda (300x180dp = 900x540px)
    fun createCardBitmap(): Bitmap {
        // UserCard boyutları: 300dp x 180dp = 900px x 540px (3x density)
        val width = 900
        val height = 540
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        
        // Kartvizit şekli için yuvarlak köşeler (16dp = 48px)
        val cornerRadius = 48f
        
        // Arkaplan rengi/gradyanı
        val backgroundRect = android.graphics.RectF(0f, 0f, width.toFloat(), height.toFloat())
        
        if (card.backgroundType == "GRADIENT") {
            // Gradyan için renk eşleştirmesi
            val gradientName = card.selectedGradient
            val (colors, isVertical) = when (gradientName) {
                "Gün Batımı", "Sunset" -> Pair(intArrayOf(0xFFFE6B8B.toInt(), 0xFFFF8E53.toInt()), false)
                "Okyanus", "Ocean" -> Pair(intArrayOf(0xFF2196F3.toInt(), 0xFF00BCD4.toInt()), false)
                "Orman", "Forest" -> Pair(intArrayOf(0xFF4CAF50.toInt(), 0xFF8BC34A.toInt()), false)
                "Gece", "Night" -> Pair(intArrayOf(0xFF2C3E50.toInt(), 0xFF3498DB.toInt()), true)
                "Mor Sis", "Purple Mist" -> Pair(intArrayOf(0xFF9C27B0.toInt(), 0xFFE91E63.toInt()), true)
                else -> Pair(intArrayOf(0xFF808080.toInt(), 0xFFD3D3D3.toInt()), true)
            }
            
            val shader = if (isVertical) {
                android.graphics.LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    colors,
                    null,
                    android.graphics.Shader.TileMode.CLAMP
                )
            } else {
                android.graphics.LinearGradient(
                    0f, 0f, width.toFloat(), 0f,
                    colors,
                    null,
                    android.graphics.Shader.TileMode.CLAMP
                )
            }
            val paint = Paint().apply { 
                this.shader = shader
                isAntiAlias = true
            }
            canvas.drawRoundRect(backgroundRect, cornerRadius, cornerRadius, paint)
        } else {
            // Tek renk arkaplan
            val paint = Paint().apply {
                color = android.graphics.Color.parseColor(card.backgroundColor)
                isAntiAlias = true
            }
            canvas.drawRoundRect(backgroundRect, cornerRadius, cornerRadius, paint)
        }
        
        // Padding değerleri (16dp = 48px)
        val padding = 48f
        val contentWidth = width - (padding * 2)
        val contentHeight = height - (padding * 2)
        
        // Profil resmi boyutu (64dp = 192px)
        val profileSize = 192f
        val profileX = padding
        val profileY = padding
        
        // Metin alanı başlangıcı - profil resmi varsa gap, yoksa padding'den başla
        val textStartX = if (!card.profileImageUrl.isNullOrEmpty()) {
            padding + profileSize + 24f // 8dp = 24px gap
        } else {
            padding
        }
        val textAreaWidth = if (!card.profileImageUrl.isNullOrEmpty()) {
            contentWidth - profileSize - 24f
        } else {
            contentWidth
        }
        
        // Profil resmi çiz (eğer varsa) - daire şeklinde
        if (!card.profileImageUrl.isNullOrEmpty()) {
            val profilePaint = Paint().apply {
                color = android.graphics.Color.LTGRAY
                isAntiAlias = true
            }
            canvas.drawCircle(
                profileX + profileSize / 2f,
                profileY + profileSize / 2f,
                profileSize / 2f,
                profilePaint
            )
        }
        // Profil resmi yoksa alanı boş bırak (UserCard'da da böyle)
        
        // Metin stilleri - parseTextStyle kullan
        val nameStyle = parseTextStyle(card.textStyles["NAME_SURNAME"])
        val titleStyle = parseTextStyle(card.textStyles["TITLE"])
        val companyStyle = parseTextStyle(card.textStyles["COMPANY"])
        val emailStyle = parseTextStyle(card.textStyles["EMAIL"])
        val phoneStyle = parseTextStyle(card.textStyles["PHONE"])
        
        // Ad Soyad - 16sp = 48px
        val namePaint = Paint().apply {
            color = nameStyle.color.toArgb()
            textSize = 48f
            typeface = if (nameStyle.fontWeight == androidx.compose.ui.text.font.FontWeight.Bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            isAntiAlias = true
        }
        
        val nameText = "${card.name} ${card.surname}"
        val nameY = profileY + 60f
        canvas.drawText(nameText, textStartX, nameY, namePaint)
        
        // Unvan - 14sp = 42px
        if (!card.title.isNullOrEmpty()) {
            val titlePaint = Paint().apply {
                color = titleStyle.color.toArgb()
                textSize = 42f
                typeface = if (titleStyle.fontWeight == androidx.compose.ui.text.font.FontWeight.Bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                isAntiAlias = true
            }
            
            val titleY = nameY + 60f
            canvas.drawText(card.title, textStartX, titleY, titlePaint)
        }
        
        // Şirket - 14sp = 42px
        if (!card.company.isNullOrEmpty()) {
            val companyPaint = Paint().apply {
                color = companyStyle.color.toArgb()
                textSize = 42f
                typeface = if (companyStyle.fontWeight == androidx.compose.ui.text.font.FontWeight.Bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                isAntiAlias = true
            }
            
            val companyY = nameY + 120f
            canvas.drawText(card.company, textStartX, companyY, companyPaint)
        }
        
        // Email - 12sp = 36px
        if (!card.email.isNullOrEmpty()) {
            val emailPaint = Paint().apply {
                color = emailStyle.color.toArgb()
                textSize = 36f
                typeface = if (emailStyle.fontWeight == androidx.compose.ui.text.font.FontWeight.Bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                isAntiAlias = true
            }
            
            val emailY = nameY + 180f
            canvas.drawText(card.email, textStartX, emailY, emailPaint)
        }
        
        // Telefon - 12sp = 36px
        if (!card.phone.isNullOrEmpty()) {
            val phonePaint = Paint().apply {
                color = phoneStyle.color.toArgb()
                textSize = 36f
                typeface = if (phoneStyle.fontWeight == androidx.compose.ui.text.font.FontWeight.Bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                isAntiAlias = true
            }
            
            val phoneY = nameY + 240f
            canvas.drawText(card.phone, textStartX, phoneY, phonePaint)
        }
        
        // Sosyal medya ikonları (alt kısımda) - 24dp = 72px
        val socialIconSize = 72f
        val socialStartY = height - padding - socialIconSize
        val socialSpacing = 96f // 32dp = 96px
        var socialX = textStartX
        
        // Sosyal medya ikonları için renk
        val socialColor = nameStyle.color.toArgb()
        
        // Sosyal medya ikonu çizme fonksiyonu - gerçek drawable ikonları
        fun drawSocialIcon(canvas: AndroidCanvas, x: Float, y: Float, size: Float, color: Int, iconType: String) {
            try {
                val drawableId = when (iconType) {
                    "web" -> R.drawable.web
                    "linkedin" -> R.drawable.linkedin
                    "github" -> R.drawable.github
                    "twitter" -> R.drawable.twitt
                    "instagram" -> R.drawable.insta
                    "facebook" -> R.drawable.face
                    else -> R.drawable.web
                }
                
                val drawable = context.getDrawable(drawableId)
                if (drawable != null) {
                    // Drawable'ı bitmap'e çevir
                    val iconBitmap = android.graphics.Bitmap.createBitmap(
                        size.toInt(), size.toInt(), android.graphics.Bitmap.Config.ARGB_8888
                    )
                    val iconCanvas = android.graphics.Canvas(iconBitmap)
                    
                    // Drawable'ı bitmap'e çiz
                    drawable.setBounds(0, 0, size.toInt(), size.toInt())
                    drawable.draw(iconCanvas)
                    
                    // Renk filtresi uygula - daha basit ve etkili yöntem
                    val paint = Paint().apply {
                        isAntiAlias = true
                        colorFilter = android.graphics.PorterDuffColorFilter(
                            color,
                            android.graphics.PorterDuff.Mode.SRC_IN
                        )
                    }
                    
                    // İkonu canvas'a çiz
                    canvas.drawBitmap(iconBitmap, x, y, paint)
                }
            } catch (e: Exception) {
                // Hata durumunda basit şekil çiz
                val paint = Paint().apply {
                    this.color = color
                    isAntiAlias = true
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(x + size/2, y + size/2, size/2, paint)
            }
        }
        
        if (!card.website.isNullOrEmpty()) {
            drawSocialIcon(canvas, socialX, socialStartY, socialIconSize, socialColor, "web")
            socialX += socialSpacing
        }
        if (!card.linkedin.isNullOrEmpty()) {
            drawSocialIcon(canvas, socialX, socialStartY, socialIconSize, socialColor, "linkedin")
            socialX += socialSpacing
        }
        if (!card.github.isNullOrEmpty()) {
            drawSocialIcon(canvas, socialX, socialStartY, socialIconSize, socialColor, "github")
            socialX += socialSpacing
        }
        if (!card.twitter.isNullOrEmpty()) {
            drawSocialIcon(canvas, socialX, socialStartY, socialIconSize, socialColor, "twitter")
            socialX += socialSpacing
        }
        if (!card.instagram.isNullOrEmpty()) {
            drawSocialIcon(canvas, socialX, socialStartY, socialIconSize, socialColor, "instagram")
            socialX += socialSpacing
        }
        if (!card.facebook.isNullOrEmpty()) {
            drawSocialIcon(canvas, socialX, socialStartY, socialIconSize, socialColor, "facebook")
            socialX += socialSpacing
        }
        
        return bitmap
    }
    
    
    // SocialIcon composable fonksiyonu
    @Composable
    fun SocialIcon(iconRes: Int, color: ComposeColor) {
        androidx.compose.foundation.Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(color)
        )
    }
    
    val clipboardManager = LocalClipboardManager.current
    
    val pagerState = rememberPagerState(pageCount = { 2 })
    var showCopyToast by remember { mutableStateOf(false) }
    
    // QR Code oluşturma
    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(card.id) {
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(
                "https://neocard.app/card/${card.id}",
                BarcodeFormat.QR_CODE,
                200,
                200
            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(
                        x, y,
                        if (bitMatrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE
                    )
                }
            }
            qrCodeBitmap = bitmap
        } catch (e: Exception) {
            // QR kod oluşturma hatası
        }
    }
    
    // Copy toast gösterimi
    LaunchedEffect(showCopyToast) {
        if (showCopyToast) {
            kotlinx.coroutines.delay(2000)
            showCopyToast = false
        }
    }
    
    Column(
        modifier = Modifier
            .padding(16.dp)
    ) {
        // Tab Row
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = { /* Tab click handled by pager */ },
                text = { Text(context.getString(R.string.qr_code)) }
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = { /* Tab click handled by pager */ },
                text = { Text(context.getString(R.string.image)) }
            )
        }
        
        // Horizontal Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
        ) { page ->
            when (page) {
                0 -> {
                    // QR Code ve Link Sayfası
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = context.getString(R.string.share_link),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // QR Code
                        Card(
                            modifier = Modifier.size(200.dp),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (qrCodeBitmap != null) {
                                    Image(
                                        bitmap = qrCodeBitmap!!.asImageBitmap(),
                                        contentDescription = "QR Code",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = context.getString(R.string.loading),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Link Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "https://neocard.app/card/${card.id}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                Button(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString("https://neocard.app/card/${card.id}"))
                                        showCopyToast = true
                                    }
                                ) {
                                    Text(context.getString(R.string.copy))
                                }
                            }
                        }
                    }
                }
                
                1 -> {
                    // Card Image Sayfası
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = context.getString(R.string.share_as_image),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Card Preview - Tam UserCard tasarımı
                        Card(
                            modifier = Modifier
                                .width(300.dp)
                                .height(180.dp),
                            elevation = CardDefaults.cardElevation(4.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(parseBackground(card, context))
                                    .padding(16.dp)
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    // Üst kısım: Profil resmi + Bilgiler
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        // Profil resmi (varsa göster)
                                        if (!card.profileImageUrl.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(card.profileImageUrl)
                                                    .crossfade(true)
                                                    .size(Size(192, 192)) // Tam olarak ihtiyaç duyulan boyut (64dp = 192px @3x)
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
                                            Text(card.title ?: "", style = parseTextStyle(card.textStyles["TITLE"]))
                                            Text(card.company ?: "", style = parseTextStyle(card.textStyles["COMPANY"]))
                                            Text(card.email ?: "", style = parseTextStyle(card.textStyles["EMAIL"]))
                                            Text(card.phone ?: "", style = parseTextStyle(card.textStyles["PHONE"]))
                                        }
                                    }

                                    // Alt kısım: Sosyal Medya İkonları
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val nameSurnameColor = ComposeColor(android.graphics.Color.parseColor(card.textStyles["NAME_SURNAME"]?.color ?: "#000000"))
                                        
                                        if (!card.website.isNullOrEmpty()) SocialIcon(R.drawable.web, nameSurnameColor)
                                        if (!card.linkedin.isNullOrEmpty()) SocialIcon(R.drawable.linkedin, nameSurnameColor)
                                        if (!card.github.isNullOrEmpty()) SocialIcon(R.drawable.github, nameSurnameColor)
                                        if (!card.twitter.isNullOrEmpty()) SocialIcon(R.drawable.twitt, nameSurnameColor)
                                        if (!card.instagram.isNullOrEmpty()) SocialIcon(R.drawable.insta, nameSurnameColor)
                                        if (!card.facebook.isNullOrEmpty()) SocialIcon(R.drawable.face, nameSurnameColor)
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = context.getString(R.string.share_image_description),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Paylaş Butonu
                        Button(
                            onClick = {
                                try {
                                    val cardBitmap = createCardBitmap()
                                    val tempFile = java.io.File.createTempFile("card_", ".png", context.cacheDir)
                                    val outputStream = java.io.FileOutputStream(tempFile)
                                    cardBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                                    outputStream.close()
                                    
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        tempFile
                                    )
                                    
                                    // Paylaşım metni - kartvizit linki ile
                                    val shareText = context.getString(R.string.share_text_with_image) + "\n\n🔗 ${context.getString(R.string.view_my_card)}: https://neocard.app/card/${card.id}"
                                    
                                    val shareIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    
                                    shareLauncher.launch(Intent.createChooser(shareIntent, context.getString(R.string.share_card)))
                                } catch (e: Exception) {
                                    // Hata durumunda log yazdır
                                    android.util.Log.e("ShareBottomSheet", "Paylaşım hatası: ${e.message}")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(context.getString(R.string.share))
                        }
                    }
                }
            }
        }
        
        // Copy Toast
        if (showCopyToast) {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2000)
                showCopyToast = false
            }
        }
    }
}
