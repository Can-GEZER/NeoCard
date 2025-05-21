package com.cangzr.neocard.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.cangzr.neocard.R
import com.cangzr.neocard.ads.AdManager
import com.cangzr.neocard.ads.InlineAdView
import com.cangzr.neocard.ads.withAdItems
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class CardWithTags(
    val card: UserCard,
    val tags: List<String> = emptyList()
)

// Önceden tanımlanmış etiketler
data class TagCategory(
    val name: String,
    val tags: List<String>
)

val PREDEFINED_TAG_CATEGORIES = listOf(
    TagCategory(
        "İş İlişkileri",
        listOf(
            "Müşteri",
            "VIP Müşteri",
            "Potansiyel Müşteri",
            "Tedarikçi",
            "Stratejik Tedarikçi",
            "İş Ortağı",
            "Stratejik İş Ortağı",
            "Çalışan",
            "Yönetici",
            "Yatırımcı",
            "Potansiyel Yatırımcı",
            "Danışman",
            "Stratejik Danışman",
            "Mentor",
            "Mentee"
        )
    ),
    TagCategory(
        "Meslek Grupları",
        listOf(
            "Yazılım Geliştirici",
            "Sistem Mühendisi",
            "Veri Mühendisi",
            "UI/UX Tasarımcı",
            "Grafik Tasarımcı",
            "Proje Yöneticisi",
            "Ürün Yöneticisi",
            "Pazarlama Uzmanı",
            "Satış Temsilcisi",
            "İK Uzmanı",
            "Finans Uzmanı",
            "Muhasebeci",
            "Avukat",
            "Doktor",
            "Öğretmen",
            "Akademisyen",
            "Mimar",
            "İnşaat Mühendisi",
            "Makine Mühendisi",
            "Elektrik Mühendisi"
        )
    ),
    TagCategory(
        "Diğer İlişkiler",
        listOf(
            "Aile",
            "Arkadaş",
            "Komşu",
            "Öğrenci",
            "Diğer"
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessCardListScreen(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    var userCards by remember { mutableStateOf<List<CardWithTags>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var showTagSelectionDialog by remember { mutableStateOf<CardWithTags?>(null) }
    var selectedTags by remember { mutableStateOf<Set<String>>(emptySet()) }
    val context = LocalContext.current
    val adManager = remember { AdManager.getInstance(context) }

    // Tüm etiketleri ve kullanım sayılarını topla
    val allTagsWithCount = remember(userCards) {
        userCards.flatMap { it.tags }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedBy { it.first }
    }

    // Filtrelenmiş kartları hesapla
    val filteredCards = remember(userCards, searchQuery, selectedTags) {
        var filtered = userCards

        // Metin araması
        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter { cardWithTags ->
                val card = cardWithTags.card
                card.name.contains(searchQuery, ignoreCase = true) ||
                card.title.contains(searchQuery, ignoreCase = true) ||
                card.company.contains(searchQuery, ignoreCase = true) ||
                card.email.contains(searchQuery, ignoreCase = true) ||
                card.phone.contains(searchQuery, ignoreCase = true) ||
                cardWithTags.tags.any { it.contains(searchQuery, ignoreCase = true) }
            }
        }

        // Etiket filtreleme
        if (selectedTags.isNotEmpty()) {
            filtered = filtered.filter { cardWithTags ->
                selectedTags.all { tag -> cardWithTags.tags.contains(tag) }
            }
        }

        filtered
    }

    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            userCards = emptyList()
            isLoading = false
        } else {
            firestore.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    val connectedList = document.get("connected") as? List<Map<String, String>> ?: emptyList()
                    val userTags = document.get("cardTags") as? Map<String, List<String>> ?: emptyMap()

                    if (connectedList.isEmpty()) {
                        userCards = emptyList()
                        isLoading = false
                    } else {
                        fetchConnectedCards(connectedList, userTags) { cardsWithTags ->
                            userCards = cardsWithTags
                            isLoading = false
                        }
                    }
                }
                .addOnFailureListener {
                    userCards = emptyList()
                    isLoading = false
                }
        }
    }

    // Etiket seçim dialog'u
    if (showTagSelectionDialog != null) {
        var tagSearchQuery by remember { mutableStateOf("") }
        val filteredCategories = remember(tagSearchQuery) {
            if (tagSearchQuery.isEmpty()) {
                PREDEFINED_TAG_CATEGORIES
            } else {
                PREDEFINED_TAG_CATEGORIES.map { category ->
                    category.copy(
                        tags = category.tags.filter { it.contains(tagSearchQuery, ignoreCase = true) }
                    )
                }.filter { it.tags.isNotEmpty() }
            }
        }

        AlertDialog(
            onDismissRequest = { showTagSelectionDialog = null },
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Etiket Seç",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${showTagSelectionDialog!!.tags.size}/3",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "En fazla 3 etiket seçebilirsiniz",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    OutlinedTextField(
                        value = tagSearchQuery,
                        onValueChange = { tagSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Etiket ara...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Ara",
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = MaterialTheme.shapes.medium
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredCategories) { category ->
                            if (category.tags.isNotEmpty()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = category.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        category.tags.forEach { tag ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        val currentCard = showTagSelectionDialog!!
                                                        val updatedTags = if (currentCard.tags.contains(tag)) {
                                                            currentCard.tags - tag
                                                        } else {
                                                            if (currentCard.tags.size < 3) {
                                                                currentCard.tags + tag
                                                            } else {
                                                                return@clickable
                                                            }
                                                        }
                                                        userCards = userCards.map { 
                                                            if (it.card.id == currentCard.card.id) 
                                                                it.copy(tags = updatedTags) 
                                                            else it 
                                                        }
                                                        currentUser?.let { user ->
                                                            firestore.collection("users").document(user.uid)
                                                                .update("cardTags.${currentCard.card.id}", updatedTags)
                                                        }
                                                        showTagSelectionDialog = showTagSelectionDialog!!.copy(tags = updatedTags)
                                                    }
                                                    .padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = showTagSelectionDialog!!.tags.contains(tag),
                                                    onCheckedChange = null,
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = MaterialTheme.colorScheme.primary,
                                                        uncheckedColor = MaterialTheme.colorScheme.outline
                                                    ),
                                                    enabled = showTagSelectionDialog!!.tags.contains(tag) || 
                                                             showTagSelectionDialog!!.tags.size < 3
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = tag,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = if (showTagSelectionDialog!!.tags.contains(tag) || 
                                                              showTagSelectionDialog!!.tags.size < 3) {
                                                        MaterialTheme.colorScheme.onSurface
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
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
            },
            confirmButton = {
                TextButton(
                    onClick = { showTagSelectionDialog = null },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Tamam")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.large
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Text(
                text = "Bağlantılarım",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Arama çubuğu
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                placeholder = { Text("Kartvizit ara...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Ara"
                    )
                },
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Etiket filtreleme
            if (allTagsWithCount.isNotEmpty()) {
                Text(
                    text = "Etiketlere Göre Filtrele",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allTagsWithCount) { (tag, count) ->
                        FilterChip(
                            selected = selectedTags.contains(tag),
                            onClick = {
                                selectedTags = if (selectedTags.contains(tag)) {
                                    selectedTags - tag
                                } else {
                                    selectedTags + tag
                                }
                            },
                            label = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(tag)
                                    Text(
                                        text = "($count)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            },
                            leadingIcon = if (selectedTags.contains(tag)) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Seçili",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null
                        )
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (currentUser == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Bağlantılarınızı görmek için giriş yapmalısınız.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            } else if (filteredCards.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isEmpty()) 
                            "Bağlantılarınızda kayıtlı bir kart bulunamadı."
                        else 
                            "Aramanızla eşleşen kart bulunamadı.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // withAdItems kullanarak reklamlı öğeleri oluşturalım
                val adInterval = AdManager.BUSINESS_CARD_AD_INTERVAL
                val itemsWithAds = withAdItems(
                    items = filteredCards,
                    adInterval = adInterval,
                    itemContent = { cardWithTags ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            UserCardItem(card = cardWithTags.card) {
                                navController.navigate("sharedCardDetail/${cardWithTags.card.id}")
                            }
                            
                            // Etiket rozeti
                            if (cardWithTags.tags.isNotEmpty()) {
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp),
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    tonalElevation = 2.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_label),
                                            contentDescription = "Etiket",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "${cardWithTags.tags.size}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Etiket ekleme butonu
                        TextButton(
                            onClick = { showTagSelectionDialog = cardWithTags },
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_label),
                                contentDescription = "Etiket Seç",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Etiket Seç")
                        }
                    },
                    adContent = {
                        InlineAdView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }
                )
                
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(itemsWithAds.size) { index ->
                        itemsWithAds[index]()
                    }
                }
            }
        }
    }
}

fun fetchConnectedCards(
    connectedList: List<Map<String, String>>,
    userTags: Map<String, List<String>>,
    onComplete: (List<CardWithTags>) -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val allCards = mutableListOf<CardWithTags>()
    var fetchedCount = 0

    if (connectedList.isEmpty()) {
        onComplete(emptyList())
        return
    }

    connectedList.forEach { connection ->
        val userId = connection["userId"]
        val cardId = connection["cardId"]

        if (userId.isNullOrEmpty() || cardId.isNullOrEmpty()) {
            fetchedCount++
            if (fetchedCount == connectedList.size) {
                onComplete(allCards)
            }
            return@forEach
        }

        firestore.collection("users").document(userId)
            .collection("cards").document(cardId).get()
            .addOnSuccessListener { cardDoc ->
                val card = cardDoc.toObject(UserCard::class.java)?.copy(id = cardDoc.id)
                if (card != null) {
                    val tags = userTags[cardId] ?: emptyList()
                    allCards.add(CardWithTags(card, tags))
                }
                fetchedCount++
                if (fetchedCount == connectedList.size) {
                    onComplete(allCards)
                }
            }
            .addOnFailureListener {
                fetchedCount++
                if (fetchedCount == connectedList.size) {
                    onComplete(allCards)
                }
            }
    }
}
