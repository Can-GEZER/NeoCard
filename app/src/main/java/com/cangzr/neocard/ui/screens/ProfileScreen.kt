package com.cangzr.neocard.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.cangzr.neocard.R
import com.cangzr.neocard.billing.BillingManager
import com.cangzr.neocard.data.model.PromoCode
import com.cangzr.neocard.ui.screens.profile.components.ConnectionRequestsSection
import com.cangzr.neocard.ui.screens.profile.components.PremiumCard
import com.cangzr.neocard.ui.screens.profile.components.ProfileCard
import com.cangzr.neocard.ui.screens.profile.components.PromoCodeCard
import com.cangzr.neocard.ui.screens.profile.components.PromoCodeRedeemCard
import com.cangzr.neocard.ui.screens.profile.components.SettingsAndActionsCard
import com.cangzr.neocard.ui.screens.profile.utils.BottomSheetContent
import com.cangzr.neocard.ui.screens.profile.utils.DeleteAccountDialog
import com.cangzr.neocard.ui.screens.profile.viewmodels.ProfileViewModel
import com.cangzr.neocard.utils.LanguageManager
import com.cangzr.neocard.utils.UserUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ProfileScreen(navController: NavHostController) {
    val context = LocalContext.current
    val viewModel: ProfileViewModel = hiltViewModel()
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val billingManager = remember { BillingManager.getInstance(context) }
    
    // Premium status
    val isPremium by billingManager.isPremium.collectAsState()
    viewModel.updatePremiumStatus(isPremium)
    
    // Promo code states
    var promoCodeList by remember { mutableStateOf<List<PromoCode>>(emptyList()) }
    var hasConnectRequests by remember { mutableStateOf(false) }
    var isAdmin by remember { mutableStateOf(false) }

    // Settings states
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val currentLanguage = remember { LanguageManager.getSelectedLanguage(context) }
    var selectedLanguage by remember { mutableStateOf(currentLanguage) }

    // Load promo codes
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
                            firestore.collection("promoCodes")
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val codes = querySnapshot.documents.mapNotNull { doc ->
                        val data = doc.data
                        if (data != null) {
                            PromoCode(
                                id = doc.id,
                                code = data["code"] as? String ?: "",
                                usageLimit = (data["usageLimit"] as? Long)?.toInt() ?: 0,
                                usedCount = (data["usedCount"] as? Long)?.toInt() ?: 0,
                                createdAt = data["createdAt"] as? String ?: "",
                                isActive = data["isActive"] as? Boolean ?: true
                            )
                        } else null
                    }
                    promoCodeList = codes
                }
        }
    }

    // Check for connection requests
    LaunchedEffect(Unit) {
    val currentUser = auth.currentUser
        if (currentUser != null) {
            firestore.collection("users").document(currentUser.uid)
                .addSnapshotListener { document, _ ->
                    if (document != null && document.exists()) {
                        val requests = document.get("connectRequests") as? List<Map<String, String>> ?: emptyList()
                        hasConnectRequests = requests.isNotEmpty()
                    }
                }
        }
    }

    // Check if user is admin
    LaunchedEffect(Unit) {
    val currentUser = auth.currentUser
        isAdmin = UserUtils.isAdmin(currentUser?.uid)
    }

    // Bottom sheet content
    selectedOption?.let {
        BottomSheetContent(it) {
            selectedOption = null
        }
    }

    // Delete account dialog
    if (showDeleteDialog) {
        DeleteAccountDialog(
            showDialog = true,
            onDismiss = { showDeleteDialog = false },
            onDeleteConfirmed = {
                showDeleteDialog = false
                UserUtils.deleteAccount(context) { success: Boolean, message: String ->
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    if (success) {
                        // Hesap silindikten sonra auth ekranına yönlendir
                        navController.navigate("auth") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            }
        )
    }
    
    // Language selection dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(text = context.getString(R.string.language)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Turkish option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedLanguage = "tr" }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = selectedLanguage == "tr",
                            onClick = { selectedLanguage = "tr" }
                        )
                        Text(text = context.getString(R.string.language_turkish))
                    }
                    
                    // English option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedLanguage = "en" }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = selectedLanguage == "en",
                            onClick = { selectedLanguage = "en" }
                        )
                        Text(text = context.getString(R.string.language_english))
                    }
                    
                    // System default option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedLanguage = "" }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = selectedLanguage == "",
                            onClick = { selectedLanguage = "" }
                        )
                        Text(text = context.getString(R.string.theme_system))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Apply language change
                        LanguageManager.setLanguage(context, selectedLanguage)
                        showLanguageDialog = false
                        
                        // Restart the activity to apply changes
                        val activity = context as? Activity
                        activity?.let {
                            val intent = it.intent
                            it.finish()
                            it.startActivity(intent)
                        }
                    }
                ) {
                    Text(text = context.getString(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(text = context.getString(R.string.cancel))
                }
            }
        )
    }

        Column(
            modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Profile Card
        ProfileCard(navController = navController)

        // Premium Card
        if (!isPremium) {
            PremiumCard()
        }

        // Connection Requests Section - Her zaman göster
        ConnectionRequestsSection(navController = navController)

        // Promo Code Redeem Card
        PromoCodeRedeemCard(
            onRedeemCode = { code ->
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    // Promosyon kodunu kontrol et
                    firestore.collection("promoCodes")
                        .whereEqualTo("code", code)
                        .whereEqualTo("isActive", true)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            if (querySnapshot.isEmpty) {
                                Toast.makeText(context, "Geçersiz veya kullanılmış promosyon kodu", Toast.LENGTH_SHORT).show()
                                return@addOnSuccessListener
                            }
                            
                            val promoDoc = querySnapshot.documents.first()
                            val promoId = promoDoc.id
                            val usageLimit = (promoDoc.get("usageLimit") as? Long)?.toInt() ?: 0
                            val usedCount = (promoDoc.get("usedCount") as? Long)?.toInt() ?: 0
                            val usedBy = (promoDoc.get("usedBy") as? List<String>) ?: emptyList()
                            
                            // Kullanıcı daha önce bu kodu kullanmış mı kontrol et
                            if (currentUser.uid in usedBy) {
                                Toast.makeText(context, "Bu promosyon kodunu daha önce kullanmışsınız", Toast.LENGTH_SHORT).show()
                                return@addOnSuccessListener
                            }
                            
                            if (usedCount >= usageLimit) {
                                Toast.makeText(context, "Promosyon kodu kullanım limiti dolmuş", Toast.LENGTH_SHORT).show()
                                return@addOnSuccessListener
                            }
                            
                            // Kullanıcının daha önce premium olup olmadığını kontrol et
                            firestore.collection("users")
                                .document(currentUser.uid)
                                .get()
                                .addOnSuccessListener { userDoc ->
                                    val currentEndTime = userDoc.getLong("premiumEndTime") ?: 0L
                                    val currentTime = System.currentTimeMillis()
                                    val isCurrentlyPremium = userDoc.getBoolean("premium") ?: false
                                    
                                    val success = if (isCurrentlyPremium && currentEndTime > currentTime) {
                                        // Mevcut premium süresine ekle
                                        billingManager.extendPremiumWithPromoCode(currentUser.uid, 7 * 24 * 60 * 60 * 1000L)
                                    } else {
                                        // Yeni premium başlat
                                        billingManager.setPremiumWithPromoCode(currentUser.uid, 7 * 24 * 60 * 60 * 1000L)
                                    }
                                    
                                    if (success) {
                                        // Promosyon kodunun kullanım sayısını artır ve kullanıcıyı listeye ekle
                                        val newUsedCount = usedCount + 1
                                        val newUsedBy = usedBy + currentUser.uid
                                        
                                        promoDoc.reference.update(
                                            mapOf(
                                                "usedCount" to newUsedCount,
                                                "usedBy" to newUsedBy
                                            )
                                        )
                                        
                                        // Kullanıcının promosyon kodu kullanım geçmişini kaydet
                                        val userPromoUsage = com.cangzr.neocard.data.model.UserPromoUsage(
                                            userId = currentUser.uid,
                                            promoCodeId = promoId,
                                            promoCode = code,
                                            usedAt = System.currentTimeMillis(),
                                            premiumDays = 7
                                        )
                                        
                                        firestore.collection("userPromoUsage")
                                            .add(userPromoUsage)
                                            .addOnSuccessListener {
                                                val message = if (isCurrentlyPremium && currentEndTime > currentTime) {
                                                    "Promosyon kodu başarıyla kullanıldı! Premium süreniz 7 gün uzatıldı."
                                                } else {
                                                    "Promosyon kodu başarıyla kullanıldı! 7 günlük premium üyelik aktif."
                                                }
                                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                            }
                                            .addOnFailureListener {
                                                val message = if (isCurrentlyPremium && currentEndTime > currentTime) {
                                                    "Premium süreniz uzatıldı ancak kullanım geçmişi kaydedilemedi"
                                                } else {
                                                    "Premium üyelik aktif edildi ancak kullanım geçmişi kaydedilemedi"
                                                }
                                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                            }
                                    } else {
                                        Toast.makeText(context, "Premium üyelik aktif edilemedi", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Kullanıcı bilgileri alınamadı", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Promosyon kodu kontrol edilemedi: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(context, "Kullanıcı giriş yapmamış", Toast.LENGTH_SHORT).show()
                }
            }
        )

        // Promo Code Management Card - Sadece admin kullanıcılara görünür
        if (isAdmin) {
            PromoCodeCard(
                promoCodeList = promoCodeList,
                onAddCode = { code, limit ->
                    val currentUser = auth.currentUser
                    if (currentUser != null) {
                        val promoCodeData = hashMapOf(
                            "code" to code,
                            "usageLimit" to limit,
                            "usedCount" to 0,
                            "createdAt" to System.currentTimeMillis().toString(),
                            "isActive" to true,
                            "userId" to currentUser.uid,
                            "usedBy" to emptyList<String>() // Yeni alan eklendi
                        )
                        
                        firestore.collection("promoCodes").add(promoCodeData)
                                            .addOnSuccessListener {
                                Toast.makeText(context, "Promosyon kodu eklendi", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Hata: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                },
                onDeleteCode = { codeId ->
                    firestore.collection("promoCodes").document(codeId).delete()
                                    .addOnSuccessListener {
                            Toast.makeText(context, "Promosyon kodu silindi", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Hata: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            )
        }

        // Settings and Actions Card
        SettingsAndActionsCard(
            navController = navController,
            isSpecialUser = false
        )
    }
}