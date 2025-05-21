package com.cangzr.neocard.ui.screens

import android.app.Activity
import android.content.Intent
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.cangzr.neocard.R
import com.cangzr.neocard.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.Unit
import com.cangzr.neocard.billing.BillingManager
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.cangzr.neocard.storage.FirebaseStorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfileScreen(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    val specialUserId = "bhEx5ZPVyOY4YJ61FdaboFhfy1B2"
    val isLoggedIn = currentUser != null
    val isSpecialUser = currentUser?.uid == specialUserId

    // Premium durumunu kontrol et
    var isPremium by remember { mutableStateOf(false) }
    var hasConnectRequests by remember { mutableStateOf(false) }
    var hasJobPosts by remember { mutableStateOf(false) }
    var updateNotes by remember { mutableStateOf<List<UpdateNote>>(emptyList()) }
    var latestAppVersion by remember { mutableStateOf("1.0.0") }
    var updateAvailable by remember { mutableStateOf(false) }
    var promoCodeList by remember { mutableStateOf<List<PromoCode>>(emptyList()) }

    // Uygulama sürümünü al
    val packageInfo = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: Exception) {
            null
        }
    }
    val currentAppVersion = remember { packageInfo?.versionName ?: "1.0.0" }

    // Güncelleme notlarını ve en son uygulama sürümünü çek
    LaunchedEffect(Unit) {
        // Güncelleme notlarını çek
        firestore.collection("updateNotes")
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                updateNotes = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(UpdateNote::class.java)?.copy(id = doc.id)
                } ?: emptyList()
            }
        
        // En son uygulama sürümünü çek
        firestore.collection("appConfig").document("version")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                
                latestAppVersion = snapshot.getString("latestVersion") ?: "1.0.0"
                
                // Sürüm kontrolü yap
                updateAvailable = isUpdateAvailable(currentAppVersion, latestAppVersion)
            }
            
        // Promosyon kodlarını çek (sadece admin için)
        if (isSpecialUser) {
            firestore.collection("promoCodes")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    promoCodeList = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(PromoCode::class.java)?.copy(id = doc.id)
                    } ?: emptyList()
                }
        }
    }

    // Kullanıcı bilgilerini kontrol et
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            firestore.collection("users").document(currentUser.uid)
                .addSnapshotListener { document, error ->
                    if (error != null || document == null) return@addSnapshotListener

                    isPremium = document.getBoolean("premium") ?: false
                    val connectRequests = document.get("connectRequests") as? List<*>
                    hasConnectRequests = !connectRequests.isNullOrEmpty()

                    val jobPostIds = document.get("jobPostIds") as? List<*>
                    hasJobPosts = !jobPostIds.isNullOrEmpty()
                }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profil Kartı
            item {
                ProfileCard(navController)
            }

            // Güncelleme Kartı (eğer güncelleme varsa)
            if (updateAvailable) {
                item {
                    UpdateAvailableCard(
                        currentVersion = currentAppVersion,
                        latestVersion = latestAppVersion,
                        onUpdateClick = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = android.net.Uri.parse("market://details?id=${context.packageName}")
                                setPackage("com.android.vending") // Google Play Store
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Play Store uygulaması bulunamadıysa web sayfasına yönlendir
                                val webIntent = Intent(Intent.ACTION_VIEW).apply {
                                    data = android.net.Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(webIntent)
                            }
                        }
                    )
                }
            }

            // Promosyon Kodu Kullanma (sadece giriş yapmış ve premium olmayan kullanıcılara göster)
            if (isLoggedIn && !isPremium) {
                item {
                    PromoCodeRedeemCard(
                        onRedeemCode = { code ->
                            redeemPromoCode(code, context, currentUser.uid)
                        }
                    )
                }
            }

            // Premium Kart (sadece giriş yapmış ve premium olmayan kullanıcılara göster)
            if (isLoggedIn && !isPremium) {
                item {
                    PremiumCard()
                }
            }

            // Bağlantı İstekleri (sadece giriş yapmış ve isteği olan kullanıcılara göster)
            if (isLoggedIn) {
                item {
                    ConnectionRequestsSection()
                }
            }

            // İlanlarım (sadece giriş yapmış ve ilanı olan kullanıcılara göster)
            if (isLoggedIn) {
                item {
                    MyJobPostsSection { jobPost ->
                        navController.navigate(Screen.JobPostDetail.createRoute(jobPost.id))
                    }
                }
            }

            // Ayarlar ve İşlemler Kartı
            if (isLoggedIn) {
                item {
                    SettingsAndActionsCard(
                        navController = navController,
                        isSpecialUser = isSpecialUser
                    )
                }
            } else {
                item {
                    SettingsCard()
                }
            }

            // Sonraki Güncellemelerde bölümü
            item {
                UpdateNotesSection(
                    updateNotes = updateNotes,
                    isAdmin = isSpecialUser,
                    onAddNote = { title, description ->
                        // Yeni güncelleme notu ekle
                        val newNote = UpdateNote(
                            title = title,
                            description = description,
                            date = System.currentTimeMillis()
                        )
                        firestore.collection("updateNotes")
                            .add(newNote)
                    }
                )
            }

            // Promosyon Kodu Kartı (sadece admin için)
            if (isLoggedIn && isSpecialUser) {
                item {
                    PromoCodeCard(
                        promoCodeList = promoCodeList,
                        onAddCode = { code, usageLimit ->
                            // Yeni promosyon kodu ekle
                            val newPromoCode = PromoCode(
                                code = code,
                                usageLimit = usageLimit,
                                usageCount = 0,
                                createdAt = System.currentTimeMillis(),
                                isActive = true
                            )
                            firestore.collection("promoCodes")
                                .add(newPromoCode)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Promosyon kodu oluşturuldu", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        },
                        onDeleteCode = { codeId ->
                            firestore.collection("promoCodes").document(codeId)
                                .delete()
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Promosyon kodu silindi", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileCard(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    var userDisplayName by remember { mutableStateOf("") }
    var userPhotoUrl by remember { mutableStateOf<String?>(null) }
    val currentUser = remember { mutableStateOf(auth.currentUser) }

    val isLoggedIn = currentUser.value != null

    LaunchedEffect(currentUser.value) {
        currentUser.value?.let { user ->
            // Google hesabından gelen bilgileri kullan
            userDisplayName = user.displayName ?: "Bilinmeyen Kullanıcı"
            userPhotoUrl = user.photoUrl?.toString()
            
            // Firestore'daki kullanıcı bilgilerini kontrol et ve gerekirse güncelle
            firestore.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Eğer displayName Firestore'da yoksa veya boşsa, Google'dan gelen ismi kullan
                        if (document.getString("displayName").isNullOrEmpty() && !userDisplayName.isNullOrEmpty()) {
                            firestore.collection("users").document(user.uid)
                                .update("displayName", userDisplayName)
                        }
                    }
                }
        }
    }

    LaunchedEffect(Unit) {
        auth.addAuthStateListener { authInstance ->
            currentUser.value = authInstance.currentUser
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (!isLoggedIn) navController.navigate(Screen.Auth.route) },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (isLoggedIn) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Kullanıcı profil fotoğrafı (Google'dan)
                        if (userPhotoUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(userPhotoUrl)
                                    .crossfade(true)
                                    .transformations(CircleCropTransformation())
                                    .build(),
                                contentDescription = "Profil Fotoğrafı",
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // Profil fotoğrafı yoksa varsayılan ikon göster
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.user_icon),
                                    contentDescription = "Varsayılan Profil İkonu",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        
                        Column(
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = userDisplayName.ifEmpty { "Kullanıcı" },
                                style = MaterialTheme.typography.titleLarge,
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = currentUser.value?.email ?: "E-posta adresi yok",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 18.sp
                            )
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { navController.navigate("notifications") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Bildirimler",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = {
                                FirebaseAuth.getInstance().signOut()
                                navController.navigate("auth") {
                                    popUpTo("home") { inclusive = true }
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Çıkış Yap",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Giriş yapın",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Button(
                        onClick = { navController.navigate(Screen.Auth.route) }
                        ) {
                        Text("Giriş Yap")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard() {

    var selectedOption by remember { mutableStateOf<String?>(null) }

    // BottomSheet içeriği
    if (selectedOption != null) {
        BottomSheetContent(selectedOption!!) {
            selectedOption = null // BottomSheet'i kapat
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf(
                R.drawable.privacy to "Gizlilik Politikası",
                R.drawable.info to "Uygulama Hakkında",
                R.drawable.help to "Yardım ve Destek"
            ).forEach { (icon, title) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedOption = title }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = icon),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                        )
                        Text(title)
                    }
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheetContent(selectedOption: String, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        // LazyColumn ile kaydırılabilir içerik
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            item {
                when (selectedOption) {
                    "Gizlilik Politikası" -> {
                        Text("Gizlilik Politikası", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Bu gizlilik politikası, NeoCard tarafından sunulan hizmetlerin kullanımı sırasında toplanan, kullanılan ve paylaşılan bilgiler hakkında sizi bilgilendirmek amacıyla hazırlanmıştır.\n" +
                                    "\n" +
                                    "1. Toplanan Bilgiler\n" +
                                    "\n" +
                                    "- Kullanıcı Bilgileri: Ad, soyad, e-posta adresi.\n" +
                                    "- Kartvizit Verileri: Kullanıcılar tarafından oluşturulan kartvizitler, eklenen bağlantılar ve paylaşımlar.\n" +
                                    "- İş İlanları ve Gönderiler: Kullanıcıların uygulamada oluşturduğu içerikler.\n" +
                                    "- Abonelik Bilgileri: Premium üyelik durumu, ödeme bilgileri ve abonelik süresi.\n" +
                                    "- Promosyon Kodu Kullanımları: Kullanılan promosyon kodları ve kullanım tarihleri.\n" +
                                    "- Cihaz ve Kullanım Bilgileri: IP adresi, tarayıcı bilgisi, cihaz modeli ve işletim sistemi gibi teknik veriler.\n" +
                                    "\n" +
                                    "2. Bilgilerin Kullanımı\n" +
                                    "\n" +
                                    "Toplanan bilgiler aşağıdaki amaçlarla kullanılabilir:\n" +
                                    "- Hesap oluşturma ve kimlik doğrulama işlemleri\n" +
                                    "- Kartvizit paylaşımı ve bağlantı yönetimi\n" +
                                    "- İş ilanları ve gönderi paylaşım hizmetleri\n" +
                                    "- Premium abonelik yönetimi ve fatura işlemleri\n" +
                                    "- Promosyon kodlarının doğrulanması ve kullanımının izlenmesi\n" +
                                    "- Uygulama performansını geliştirmek ve güvenliği sağlamak\n" +
                                    "- Kullanıcılara özel içerikler ve reklamlar göstermek\n" +
                                    "- Uygulama içi satın alma işlemlerini yönetmek\n" +
                                    "\n" +
                                    "3. Bilgilerin Paylaşımı\n" +
                                    "\n" +
                                    "- Ödeme Hizmet Sağlayıcıları: Abonelik işlemlerini gerçekleştirmek için gerekli ödeme bilgileri.\n" +
                                    "- Yasal yükümlülükler gereği yetkili makamlarla belirli bilgiler paylaşılabilir.\n" +
                                    "- Kullanıcıların açık rızası olmadan kişisel bilgiler üçüncü taraflarla paylaşılmaz.\n" +
                                    "\n" +
                                    "4. Güvenlik\n" +
                                    "\n" +
                                    "- Kullanıcı bilgilerinin güvenliği için endüstri standardı teknik ve idari önlemler alınmaktadır.\n" +
                                    "- Ödeme bilgileri şifreleme teknolojileri kullanılarak korunmaktadır.\n" +
                                    "- Ancak, internet üzerinden yapılan hiçbir veri iletimi %100 güvenli değildir.\n" +
                                    "\n" +
                                    "5. Abonelik ve Ödemeler\n" +
                                    "\n" +
                                    "- Premium abonelikler Google Play üzerinden yönetilmektedir.\n" +
                                    "- Abonelik ücretleri, yenileme koşulları ve iptal işlemleri Google Play Mağazası kurallarına tabidir.\n" +
                                    "- Promosyon kodları ile kazanılan üyelikler belirli bir süre için geçerlidir ve süre sonunda otomatik olarak sona erer.\n" +
                                    "\n" +
                                    "6. Değişiklikler\n" +
                                    "\n" +
                                    "- Bu gizlilik politikası zaman zaman güncellenebilir. Önemli değişiklikler hakkında kullanıcılar e-posta veya uygulama bildirimleri aracılığıyla bilgilendirilecektir.\n" +
                                    "\n" +
                                    "7. İletişim\n" +
                                    "\n" +
                                    "- Gizlilik politikası veya veri uygulamalarımızla ilgili sorularınız için, lütfen bizimle info@neocardapp.com adresinden iletişime geçin."
                        )
                    }
                    "Uygulama Hakkında" -> {
                        Text("Uygulama Hakkında", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "NeoCard, kullanıcıların dijital kartvizit oluşturmasını, paylaşmasını ve diğer kullanıcıların kartvizitlerini bağlantılarına eklemesini sağlayan yenilikçi bir platformdur. Ayrıca iş ilanları ve gönderiler paylaşarak profesyonel ağınızı genişletebilirsiniz.\n" +
                                    "\n" +
                                    "- Temel Özellikler\n" +

                                    "- Kayıt ve giriş yaparak kişisel veya kurumsal kartvizit oluşturma\n" +

                                    "- Kartvizitlerinizi paylaşma ve başkalarının kartvizitlerini bağlantılarınıza ekleme\n" +

                                    "- İş ilanları ve gönderiler paylaşma\n" +

                                    "- Uygulama içi reklamlar ve premium üyelik seçenekleri\n" +

                                    "- Güvenli ve hızlı kullanım için modern arayüz\n" +
                                    "\n" +
                                    "NeoCard, profesyonel dünyada daha güçlü bağlantılar kurmanıza yardımcı olmak için tasarlanmıştır. Siz de hemen başlayın ve ağınızı genişletin!"
                        )
                    }
                    "Yardım ve Destek" -> {
                        Text("Yardım ve Destek", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Sorularınız veya yaşadığınız teknik sorunlar için aşağıdaki başlıklara göz atabilirsiniz:\n" +
                                    "\n" +
                                    "Sıkça Sorulan Sorular (SSS)\n" +
                                    "\n" +
                                    "1. Hesap oluşturamıyorum, ne yapmalıyım?\n" +
                                    "Öncelikle internet bağlantınızı kontrol edin. Eğer sorun devam ederse, kayıt sırasında kullandığınız e-posta adresinin doğru olduğundan emin olun.\n" +
                                    "\n" +
                                    "2. Kartvizit nasıl oluşturulur?\n" +
                                    "Alt menü seçeneklerinde 'Kartvizit Oluştur' bölümü için '+' bulunmaktadır, gerekli bilgileri girerek kartvizitinizi oluşturabilirsiniz.\n" +
                                    "\n" +
                                    "3. İş ilanı veya gönderi nasıl paylaşılır?\n" +
                                    "Profil sayfasındaki 'İlan Paylaş' butonuna tıklayarak ilanınızı veya gönderinizi oluşturabilirsiniz.\n" +
                                    "\n" +
                                    "4. Premium üyelik nasıl satın alınır?\n" +
                                    "Profil sayfasında ki 'Premium Üyelik' butonuna tıklayarak ödeme işlemini tamamlayabilirsiniz.\n" +
                                    "\n" +
                                    "5. Promosyon kodunu nasıl kullanabilirim?\n" +
                                    "Profil sayfasında aktif bir promokod bulunuyorsa 'Promosyon Kodu Kullan' kartı görünür buna tıklayarak kodunuzu girebilirsiniz. Geçerli bir kod girdiğinizde 1 haftalık premium üyelik kazanırsınız.\n" +
                                    "\n" +
                                    "6. Hesabımı nasıl silebilirim?\n" +
                                    "Ayarlar menüsünden 'Hesabımı Sil' seçeneğine giderek işlemi tamamlayabilirsiniz. Dikkat: Bu işlem geri alınamaz!\n" +
                                    "\n" +
                                    "Destek Ekibi ile İletişime Geçin\n" +
                                    "\n" +
                                    "Sorularınız için info@neocardapp.com adresinden bizimle iletişime geçebilirsiniz. Destek ekibimiz en kısa sürede size yardımcı olacaktır."
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumCard() {
    var showSheet by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showSheet = true },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD700)
                )
                Column {
                    Text(
                        text = "Premium'a Yüksel",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Sınırsız kartvizit ve daha fazlası",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false }
        ) {
            PremiumContent { showSheet = false }
        }
    }
}

@Composable
fun PremiumContent(onClose: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val billingManager = remember { BillingManager.getInstance(context) }
    val isPremium by billingManager.isPremium.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Başlık Alanı
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "Premium Üyelik",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Text(
                text = "Premium ile daha fazla özellik keşfedin!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Premium Özellikleri
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                listOf(
                    Triple(Icons.Default.Star, "Sınırsız Kartvizit", "İstediğiniz kadar kartvizit oluşturun"),
                    Triple(Icons.Default.Star, "Özel Tasarımlar", "Gradyan arkaplan ve yazı stilleri"),
                    Triple(Icons.Default.Star, "Detaylı Bilgiler", "Biyografi ve CV ekleme seçenekleri"),
                    Triple(Icons.Default.Star, "Reklamsız Deneyim", "Kesintisiz premium deneyim"),
                ).forEach { (icon, title, description) ->
                    PremiumFeatureItemNew(
                        icon = icon,
                        title = title,
                        description = description
                    )
                }
            }
        }

        // Fiyat Kartı
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "29.99₺",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "aylık",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                Button(
                    onClick = { 
                        activity?.let { billingManager.launchBillingFlow(it) }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isPremium
                ) {
                    Text(
                        text = if (isPremium) "Premium Üyeliğiniz Aktif" else "Hemen Başla",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Kapat Butonu
        TextButton(
            onClick = onClose,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                text = "Belki Daha Sonra",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PremiumFeatureItemNew(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Firestore'dan kullanıcı bilgilerini topluca al
fun fetchUsersByIds(userIds: List<String>, onComplete: (List<User>) -> Unit) {
    val firestore = FirebaseFirestore.getInstance()
    val users = mutableListOf<User>()
    val batch = firestore.batch()

    userIds.forEach { uid ->
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)
                if (user != null) {
                    users.add(user)
                }
                if (users.size == userIds.size) {
                    onComplete(users)
                }
            }
            .addOnFailureListener {
                if (users.size == userIds.size) {
                    onComplete(users)
                }
            }
    }
}

// Kullanıcı Modeli (Firestore yapısına uygun olmalı)
data class User(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val premium: Boolean = false
)

@Composable
fun ConnectionRequestsSection() {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    val currentUser = auth.currentUser
    var connectionRequests by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
    var userMap by remember { mutableStateOf<Map<String, User>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            connectionRequests = listOf()
            isLoading = false
        } else {
            firestore.collection("users").document(currentUser.uid)
                .addSnapshotListener { document, error ->
                    if (error != null || document == null) {
                        isLoading = false
                        return@addSnapshotListener
                    }

                    val requestList = document.get("connectRequests") as? List<Map<String, String>> ?: emptyList()
                    val userIds = requestList.mapNotNull { it["userId"] }.distinct()

                    if (userIds.isEmpty()) {
                        connectionRequests = emptyList()
                        isLoading = false
                    } else {
                        fetchUsersByIds(userIds) { users ->
                            userMap = users.associateBy { it.id }
                            connectionRequests = requestList
                            isLoading = false
                        }
                    }
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        // Başlık ve Sayaç
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Bağlantı İstekleri",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            if (connectionRequests.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = CircleShape
                ) {
                    Text(
                        text = "${connectionRequests.size}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // İçerik
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(32.dp)
                )
            }
        } else if (connectionRequests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Henüz bir bağlantı isteği bulunmuyor.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { /* İlan oluşturma sayfasına yönlendir */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("İlan Oluştur")
                    }
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(connectionRequests) { request ->
                    val requestUserId = request["userId"] ?: return@items
                    val cardId = request["cardId"] ?: return@items
                    val user = userMap[requestUserId]

                    ConnectionRequestCard(
                        userId = requestUserId,
                        name = user?.displayName ?: "Bilinmeyen Kullanıcı",
                        email = user?.email ?: "E-posta Bulunamadı",
                        onAccept = {
                            acceptConnectionRequest(currentUser?.uid, requestUserId, cardId) {
                                connectionRequests = connectionRequests.filter { 
                                    it["userId"] != requestUserId && it["cardId"] != cardId 
                                }
                            }
                        },
                        onReject = {
                            rejectConnectionRequest(currentUser?.uid, requestUserId, cardId) {
                                connectionRequests = connectionRequests.filter { 
                                    it["userId"] != requestUserId && it["cardId"] != cardId 
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectionRequestCard(
    userId: String,
    name: String,
    email: String,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(240.dp)
            .padding(4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Profil ve Bilgiler
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profil Resmi
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // İsim ve E-posta
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Butonlar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Reddet Butonu
                Button(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF4444)
                    ),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Reddet",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }

                // Kabul Et Butonu
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Kabul Et",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// 🔥 **Bağlantı İsteğini Kabul Etme (Firestore + UI'den kaldırma)**
fun acceptConnectionRequest(currentUserId: String?, requestUserId: String?, cardId: String?, onComplete: () -> Unit) {
    if (currentUserId == null || requestUserId == null || cardId == null) return

    val firestore = FirebaseFirestore.getInstance()
    val currentUserRef = firestore.collection("users").document(currentUserId)
    val requestUserRef = firestore.collection("users").document(requestUserId)

    firestore.runTransaction { transaction ->
        val currentUserSnapshot = transaction.get(currentUserRef)
        val requestUserSnapshot = transaction.get(requestUserRef)

        val currentRequests = currentUserSnapshot.get("connectRequests") as? MutableList<Map<String, String>> ?: mutableListOf()
        val currentConnected = currentUserSnapshot.get("connected") as? MutableList<Map<String, String>> ?: mutableListOf()
        val requestUserConnected = requestUserSnapshot.get("connected") as? MutableList<Map<String, String>> ?: mutableListOf()

        // 🔥 Bağlantı isteğini doğru şekilde bul
        val requestEntry = currentRequests.find { it["userId"] == requestUserId && it["cardId"] == cardId }

        if (requestEntry != null) {
            currentRequests.remove(requestEntry) // 🔥 `connectRequests` listesinden kaldır

            // 🔥 Kullanıcının kendi bağlantı listesine kart ID'si olmadan sadece userId ekleniyor (boş kartId ile)
            currentConnected.add(mapOf("userId" to requestUserId, "cardId" to ""))

            // 🔥 Karşı kullanıcının bağlantılar listesine gerçek bağlantı bilgisi ekleniyor
            requestUserConnected.add(mapOf("userId" to currentUserId, "cardId" to cardId))

            // Güncellemeleri Firestore'a yaz
            transaction.update(currentUserRef, "connectRequests", currentRequests)
            transaction.update(currentUserRef, "connected", currentConnected)
            transaction.update(requestUserRef, "connected", requestUserConnected)
        }
    }.addOnSuccessListener {
        println("✅ Bağlantı isteği kabul edildi ve karşı kullanıcının bağlantılar listesine kart eklendi!")
        onComplete() // ✅ UI Güncelleme
    }.addOnFailureListener {
        println("❌ Bağlantı isteği kabul edilirken hata oluştu: ${it.message}")
    }
}

// 🔥 **Bağlantı İsteğini Reddetme (Firestore + UI'den kaldırma)**
fun rejectConnectionRequest(currentUserId: String?, requestUserId: String?, cardId: String?, onComplete: () -> Unit) {
    if (currentUserId == null || requestUserId == null || cardId == null) return

    val firestore = FirebaseFirestore.getInstance()
    val currentUserRef = firestore.collection("users").document(currentUserId)

    firestore.runTransaction { transaction ->
        val snapshot = transaction.get(currentUserRef)
        val currentRequests = snapshot.get("connectRequests") as? MutableList<Map<String, String>> ?: mutableListOf()

        // 🔥 Silinecek bağlantı isteğini bul
        val requestEntry = currentRequests.find { it["userId"] == requestUserId && it["cardId"] == cardId }

        if (requestEntry != null) {
            currentRequests.remove(requestEntry) // 🔥 `connectRequests` listesinden kaldır
            transaction.update(currentUserRef, "connectRequests", currentRequests)
        }
    }.addOnSuccessListener {
        println("✅ Bağlantı isteği reddedildi!")
        onComplete() // ✅ UI Güncelleme
    }.addOnFailureListener {
        println("❌ Bağlantı isteği reddedilirken hata oluştu: ${it.message}")
    }
}

@Composable
fun DeleteAccountDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onDeleteConfirmed: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Hesabı Sil") },
            text = { Text("Hesabınızı silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.") },
            confirmButton = {
                TextButton(onClick = onDeleteConfirmed) {
                    Text("Evet, Sil")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

// Firebase Kullanıcı Silme ve Firestore Verilerini Temizleme
fun deleteAccount(onResult: (Boolean, String) -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser
    val firestore = FirebaseFirestore.getInstance()
    val storageManager = FirebaseStorageManager.getInstance()

    if (user != null) {
        val uid = user.uid
        
        // Google ile giriş yapan kullanıcılar için özel işlem
        if (user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }) {
            // Google ile giriş yapan kullanıcılar için yeniden kimlik doğrulama gerekli
            // Bu durumda kullanıcıyı bilgilendir
            onResult(false, "Google hesabı ile giriş yaptığınız için hesabınızı silmek için önce çıkış yapıp tekrar giriş yapmanız gerekiyor.")
            return
        }
        
        // Standart hesap silme işlemi
        user.delete()
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    // Kullanıcının verilerini silmeden önce kartlarını al
                    firestore.collection("users").document(uid)
                        .collection("cards").get()
                        .addOnSuccessListener { cardsSnapshot ->
                            // Batch işlemi başlat
                            val batch = firestore.batch()
                            
                            // Tüm kartları public_cards koleksiyonundan da sil
                            for (cardDoc in cardsSnapshot.documents) {
                                val cardId = cardDoc.id
                                val publicCardRef = firestore.collection("public_cards").document(cardId)
                                batch.delete(publicCardRef)
                            }
                            
                            // Kullanıcının iş ilanlarını da sil
                            firestore.collection("jobPosts")
                                .whereEqualTo("userId", uid)
                                .get()
                                .addOnSuccessListener { jobPostsSnapshot ->
                                    for (jobDoc in jobPostsSnapshot.documents) {
                                        batch.delete(jobDoc.reference)
                                    }
                                    
                                    // Kullanıcı belgesini sil
                                    batch.delete(firestore.collection("users").document(uid))
                                    
                                    // Batch işlemini uygula
                                    batch.commit().addOnSuccessListener {
                                        // Kullanıcının Storage'daki tüm resimlerini sil
                                        CoroutineScope(Dispatchers.IO).launch {
                                            storageManager.deleteAllUserImages(uid)
                                            withContext(Dispatchers.Main) {
                            onResult(true, "Hesap başarıyla silindi.")
                        }
                                        }
                                    }.addOnFailureListener { e ->
                            onResult(false, "Hesap silindi ancak Firestore verileri silinemedi: ${e.localizedMessage}")
                        }
                                }
                                .addOnFailureListener { e ->
                                    onResult(false, "Hesap silindi ancak iş ilanları silinemedi: ${e.localizedMessage}")
                                }
                        }
                        .addOnFailureListener { e ->
                            onResult(false, "Hesap silindi ancak kartlar silinemedi: ${e.localizedMessage}")
                        }
                } else {
                    // Kimlik doğrulama gerekiyorsa
                    if (authTask.exception is FirebaseAuthRecentLoginRequiredException) {
                        onResult(false, "Hesabınızı silmek için yeniden giriş yapmanız gerekiyor. Lütfen çıkış yapıp tekrar giriş yapın.")
                } else {
                    onResult(false, "Hesap silme hatası: ${authTask.exception?.localizedMessage}")
                    }
                }
            }
    } else {
        onResult(false, "Giriş yapan kullanıcı bulunamadı.")
    }
}

@Composable
fun JobPostCard(
    title: String,
    imageUrl: String,
    appliedCardIds: Int,
    onClick: () -> Unit // Tıklanabilirlik için onClick parametresi
) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick), // Tıklanabilir hale getirildi
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Yuvarlak ve küçük resim
            Box(
                modifier = Modifier
                    .size(36.dp) // Resmi daha küçük yapıyoruz (48x48 dp)
                    .clip(RoundedCornerShape(50)) // Resmi yuvarlak yapıyoruz
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .placeholder(R.drawable.logo3)
                        .error(R.drawable.logo3)
                        .transformations(CircleCropTransformation())
                        .build(),
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(), // Resmi alanı tamamen doldurur
                    contentScale = ContentScale.Crop // Resmi alanı tamamen kaplayacak şekilde kırpar
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Başlık ve katılımcı sayısı
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Başlık
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Katılımcı sayısı
                Text(
                    text = "$appliedCardIds kişi katıldı",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// Firestore'dan ilanları çek
fun fetchJobPostsByIds(jobPostIds: List<String>, onComplete: (List<JobPost>) -> Unit) {
    val firestore = FirebaseFirestore.getInstance()
    val jobPosts = mutableListOf<JobPost>()

    if (jobPostIds.isEmpty()) {  // 🔥 Eğer hiç ilan yoksa, işlemi durdur
        onComplete(emptyList())
        return
    }

    jobPostIds.forEachIndexed { index, id ->
        firestore.collection("jobPosts").document(id)
            .addSnapshotListener { document, error ->
                if (error != null || document == null) return@addSnapshotListener

                val jobPost = document.toObject(JobPost::class.java)?.copy(id = document.id)
                if (jobPost != null) {
                    jobPosts.add(jobPost)
                }

                // 🔥 Tüm ilanlar çekildiğinde `onComplete()` çağır
                if (index == jobPostIds.size - 1) {
                    onComplete(jobPosts)
                }
            }
    }
}

@Composable
fun MyJobPostsSection(onJobPostClick: (JobPost) -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    var jobPosts by remember { mutableStateOf<List<JobPost>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // 🔥 Firestore'daki ilanları anlık olarak dinle
    LaunchedEffect(currentUser) {
        currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid)
                .addSnapshotListener { document, error ->
                    if (error != null || document == null) {
                        isLoading = false
                        return@addSnapshotListener
                    }

                    val jobPostIds = document.get("jobPostIds") as? List<String> ?: emptyList()
                    if (jobPostIds.isEmpty()) {
                        jobPosts = emptyList()
                        isLoading = false
                    } else {
                        fetchJobPostsByIds(jobPostIds) { posts ->
                            jobPosts = posts
                            isLoading = false
                        }
                    }
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "İlanlarım",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Text(
                text = "${jobPosts.size} aktif ilan",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(32.dp)
                )
            }
        } else if (jobPosts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Henüz bir ilanınız bulunmuyor.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { /* İlan oluşturma sayfasına yönlendir */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("İlan Oluştur")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                jobPosts.forEach { jobPost ->
                    JobPostCard(
                        title = jobPost.title,
                        imageUrl = jobPost.logoUrl,
                        onClick = { onJobPostClick(jobPost) },
                        appliedCardIds = jobPost.appliedCardIds.size
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsAndActionsCard(
    navController: NavHostController,
    isSpecialUser: Boolean
) {
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    selectedOption?.let {
        BottomSheetContent(it) {
            selectedOption = null
        }
    }

    if (showDeleteDialog) {
        DeleteAccountDialog(
            showDialog = true,
            onDismiss = { showDeleteDialog = false },
            onDeleteConfirmed = {
                showDeleteDialog = false
                deleteAccount { success, message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ActionButton(
                icon = Icons.Default.AddCircle,
                title = "İlan Paylaş",
                onClick = { navController.navigate(Screen.PostAd.route) },
                tint = MaterialTheme.colorScheme.primary
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            if (isSpecialUser) {
                ActionButton(
                    icon = Icons.Default.Warning,
                    title = "Şikayetler",
                    onClick = { navController.navigate("complaints") },
                    tint = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            }

            listOf(
                Triple(R.drawable.privacy, "Gizlilik Politikası", { selectedOption = "Gizlilik Politikası" }),
                Triple(R.drawable.info, "Uygulama Hakkında", { selectedOption = "Uygulama Hakkında" }),
                Triple(R.drawable.help, "Yardım ve Destek", { selectedOption = "Yardım ve Destek" })
            ).forEach { (icon, title, onClick) ->
                ActionButton(
                    iconRes = icon,
                    title = title,
                    onClick = onClick,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            ActionButton(
                icon = Icons.Default.Delete,
                title = "Hesabı Sil",
                onClick = { showDeleteDialog = true },
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector? = null,
    iconRes: Int? = null,
    title: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                icon != null -> Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
                iconRes != null -> Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(tint)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = tint
            )
        }
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = tint.copy(alpha = 0.7f)
        )
    }
}

data class UpdateNote(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val date: Long = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateNotesSection(
    updateNotes: List<UpdateNote>,
    isAdmin: Boolean,
    onAddNote: (String, String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showVersionDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newDescription by remember { mutableStateOf("") }
    var newVersion by remember { mutableStateOf("") }
    val context = LocalContext.current
    val packageInfo = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: Exception) {
            null
        }
    }
    val appVersion = remember { packageInfo?.versionName ?: "1.0.0" }
    val firestore = FirebaseFirestore.getInstance()
    var latestVersion by remember { mutableStateOf("") }
    
    // En son sürümü çek
    LaunchedEffect(Unit) {
        firestore.collection("appConfig").document("version")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                latestVersion = snapshot.getString("latestVersion") ?: ""
            }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Sonraki Güncellemelerde",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = if (isAdmin) Modifier.clickable { showVersionDialog = true } else Modifier
                    ) {
                        Text(
                            text = "Güncel Sürüm: v$appVersion",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        if (latestVersion.isNotEmpty() && latestVersion != appVersion) {
                            Text(
                                text = "→ v$latestVersion",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (isAdmin) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Sürüm Güncelle",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                if (isAdmin) {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Güncelleme Ekle",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        if (updateNotes.isEmpty()) {
            Text(
                text = "Henüz güncelleme notu bulunmuyor.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                updateNotes.forEach { note ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = note.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = note.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        if (isAdmin) {
                            IconButton(
                                onClick = {
                                    // Notu sil
                                    firestore.collection("updateNotes").document(note.id)
                                        .delete()
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "Not silindi", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Notu Sil",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Yeni Güncelleme Notu") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Başlık") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                    OutlinedTextField(
                        value = newDescription,
                        onValueChange = { newDescription = it },
                        label = { Text("Açıklama") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTitle.isNotEmpty() && newDescription.isNotEmpty()) {
                            onAddNote(newTitle, newDescription)
                            newTitle = ""
                            newDescription = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Ekle")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    // Sürüm güncelleme dialog'u (sadece admin için)
    if (showVersionDialog && isAdmin) {
        AlertDialog(
            onDismissRequest = { showVersionDialog = false },
            title = { Text("Uygulama Sürümünü Güncelle") },
            text = {
                Column {
                    Text(
                        text = "Mevcut sürüm: v$appVersion",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    if (latestVersion.isNotEmpty()) {
                        Text(
                            text = "Sunucudaki sürüm: v$latestVersion",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                    OutlinedTextField(
                        value = newVersion,
                        onValueChange = { newVersion = it },
                        label = { Text("Yeni Sürüm (örn: 1.2 veya 1.2.0)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        placeholder = { Text("x.y veya x.y.z formatında") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newVersion.isNotEmpty() && newVersion.matches(Regex("\\d+\\.\\d+(\\.\\d+)?"))) {
                            // Sürümü güncelle
                            firestore.collection("appConfig").document("version")
                                .set(mapOf("latestVersion" to newVersion))
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Sürüm güncellendi: v$newVersion", Toast.LENGTH_SHORT).show()
                                    newVersion = ""
                                    showVersionDialog = false
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(context, "Geçerli bir sürüm numarası girin (x.y veya x.y.z)", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Güncelle")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVersionDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }
}

// Sürüm karşılaştırma fonksiyonu
private fun isUpdateAvailable(currentVersion: String, latestVersion: String): Boolean {
    try {
        // Sürüm numaralarını parçalara ayır
        val current = currentVersion.split(".").map { it.toInt() }
        val latest = latestVersion.split(".").map { it.toInt() }
        
        // Ortak parçaları karşılaştır
        for (i in 0 until minOf(current.size, latest.size)) {
            if (latest[i] > current[i]) return true
            if (latest[i] < current[i]) return false
        }
        
        // Eğer buraya kadar geldiyse ve sürümlerden biri diğerinden daha fazla parça içeriyorsa
        // (örneğin 1.0 ve 1.0.1), daha fazla parça içeren sürüm daha yenidir
        return latest.size > current.size
    } catch (e: Exception) {
        // Herhangi bir hata durumunda güncelleme olmadığını varsay
        return false
    }
}

@Composable
fun UpdateAvailableCard(
    currentVersion: String,
    latestVersion: String,
    onUpdateClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f)
                        )
                    )
                )
                .clickable(onClick = onUpdateClick)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Yeni Sürüm Mevcut",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "v$currentVersion",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "v$latestVersion",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onUpdateClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Güncelle",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// Promosyon kodu veri sınıfı
data class PromoCode(
    val id: String = "",
    val code: String = "",
    val usageLimit: Int = 0,
    val usageCount: Int = 0,
    val createdAt: Long = 0,
    val isActive: Boolean = true
)

// Promosyon kodu kullanma fonksiyonu
fun redeemPromoCode(code: String, context: Context, userId: String) {
    val firestore = FirebaseFirestore.getInstance()
    val billingManager = BillingManager.getInstance(context)
    val promoCodeRef = firestore.collection("promoCodes").whereEqualTo("code", code).limit(1)
    
    promoCodeRef.get().addOnSuccessListener { querySnapshot ->
        if (querySnapshot.isEmpty) {
            Toast.makeText(context, "Geçersiz promosyon kodu", Toast.LENGTH_SHORT).show()
            return@addOnSuccessListener
        }
        
        val promoDoc = querySnapshot.documents[0]
        val promoCode = promoDoc.toObject(PromoCode::class.java)?.copy(id = promoDoc.id)
        
        if (promoCode == null) {
            Toast.makeText(context, "Promosyon kodu bulunamadı", Toast.LENGTH_SHORT).show()
            return@addOnSuccessListener
        }
        
        if (!promoCode.isActive) {
            Toast.makeText(context, "Bu promosyon kodu artık geçerli değil", Toast.LENGTH_SHORT).show()
            return@addOnSuccessListener
        }
        
        if (promoCode.usageCount >= promoCode.usageLimit) {
            Toast.makeText(context, "Bu promosyon kodu maksimum kullanım sayısına ulaştı", Toast.LENGTH_SHORT).show()
            
            // Kodu pasif hale getir
            firestore.collection("promoCodes").document(promoCode.id)
                .update("isActive", false)
                
            return@addOnSuccessListener
        }
        
        // Kullanıcı bu kodu daha önce kullanmış mı kontrol et
        firestore.collection("users").document(userId)
            .collection("usedPromoCodes")
            .whereEqualTo("promoCodeId", promoCode.id)
            .get()
            .addOnSuccessListener { userCodeSnapshot ->
                if (!userCodeSnapshot.isEmpty) {
                    Toast.makeText(context, "Bu promosyon kodunu daha önce kullandınız", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                
                // Kullanım sayısını artır
                firestore.collection("promoCodes").document(promoCode.id)
                    .update("usageCount", promoCode.usageCount + 1)
                
                // Kullanıcıya premium üyelik ver (BillingManager üzerinden)
                val success = billingManager.setPremiumWithPromoCode(userId, BillingManager.PROMO_PREMIUM_DURATION)
                
                if (success) {
                    // Kullanıcının kullandığı kodları kaydet
                    val usedPromoCode = hashMapOf(
                        "promoCodeId" to promoCode.id,
                        "usedAt" to System.currentTimeMillis()
                    )
                    
                    firestore.collection("users").document(userId)
                        .collection("usedPromoCodes")
                        .add(usedPromoCode)
                    
                    Toast.makeText(context, "Promosyon kodu başarıyla kullanıldı! 1 haftalık premium üyelik kazandınız.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Premium üyelik etkinleştirilirken bir hata oluştu.", Toast.LENGTH_SHORT).show()
                }
            }
    }.addOnFailureListener { e ->
        Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun PromoCodeRedeemCard(
    onRedeemCode: (String) -> Unit
) {
    var promoCode by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "Promosyon Kodu Kullan",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "1 haftalık ücretsiz premium",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Promosyon Kodu Kullan") },
            text = {
                Column {
                    Text(
                        text = "Promosyon kodunuzu girerek 1 haftalık premium üyeliğin keyfini çıkarın!",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = promoCode,
                        onValueChange = { promoCode = it.trim().uppercase() },
                        label = { Text("Promosyon Kodu") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (promoCode.isNotEmpty()) {
                            onRedeemCode(promoCode)
                            showDialog = false
                        }
                    }
                ) {
                    Text("Kullan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }
}

@Composable
fun PromoCodeCard(
    promoCodeList: List<PromoCode>,
    onAddCode: (String, Int) -> Unit,
    onDeleteCode: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newPromoCode by remember { mutableStateOf("") }
    var usageLimit by remember { mutableStateOf("10") }
    var expandedCodeId by remember { mutableStateOf<String?>(null) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Promosyon Kodları",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Promosyon Kodu Ekle",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            if (promoCodeList.isEmpty()) {
                Text(
                    text = "Henüz promosyon kodu bulunmuyor.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    promoCodeList.forEach { promoCode ->
                        PromoCodeItem(
                            promoCode = promoCode,
                            isExpanded = expandedCodeId == promoCode.id,
                            onExpandToggle = { 
                                expandedCodeId = if (expandedCodeId == promoCode.id) null else promoCode.id 
                            },
                            onDelete = { onDeleteCode(promoCode.id) }
                        )
                    }
                }
            }
        }
    }
    
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Yeni Promosyon Kodu") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPromoCode,
                        onValueChange = { newPromoCode = it.trim().uppercase() },
                        label = { Text("Promosyon Kodu") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = usageLimit,
                        onValueChange = { 
                            if (it.isEmpty() || it.all { c -> c.isDigit() }) {
                                usageLimit = it
                            } 
                        },
                        label = { Text("Kullanım Limiti") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPromoCode.isNotEmpty() && usageLimit.isNotEmpty()) {
                            onAddCode(newPromoCode, usageLimit.toIntOrNull() ?: 10)
                            newPromoCode = ""
                            usageLimit = "10"
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Ekle")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }
}

@Composable
fun PromoCodeItem(
    promoCode: PromoCode,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onExpandToggle),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (promoCode.isActive) 
                MaterialTheme.colorScheme.surface 
            else 
                MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (promoCode.isActive) 
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else 
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = promoCode.code.take(1),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (promoCode.isActive) 
                                MaterialTheme.colorScheme.primary
                            else 
                                MaterialTheme.colorScheme.error
                        )
                    }
                    
                    Column {
                        Text(
                            text = promoCode.code,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (promoCode.isActive) 
                                MaterialTheme.colorScheme.onSurface
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        
                        val status = if (!promoCode.isActive) {
                            "Pasif"
                        } else if (promoCode.usageCount >= promoCode.usageLimit) {
                            "Limit Doldu"
                        } else {
                            "Aktif"
                        }
                        
                        val statusColor = when(status) {
                            "Aktif" -> Color(0xFF4CAF50) // Yeşil
                            else -> Color(0xFFE53935) // Kırmızı
                        }
                        
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor
                        )
                    }
                }
                
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (promoCode.isActive && promoCode.usageCount < promoCode.usageLimit) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.error
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${promoCode.usageCount}/${promoCode.usageLimit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 10.sp
                    )
                }
            }
            
            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Oluşturulma: ${formatDate(promoCode.createdAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Sil",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Tarih formatlamak için yardımcı fonksiyon
fun formatDate(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val format = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
    return format.format(date)
}
