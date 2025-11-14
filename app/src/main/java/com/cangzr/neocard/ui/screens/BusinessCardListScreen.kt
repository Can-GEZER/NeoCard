package com.cangzr.neocard.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import coil.transform.CircleCropTransformation
import androidx.navigation.NavHostController
import com.cangzr.neocard.R
import com.cangzr.neocard.Screen
import com.cangzr.neocard.ads.AdManager
import com.cangzr.neocard.ads.InlineAdView
import com.cangzr.neocard.ads.withAdItems
import com.cangzr.neocard.data.model.UserCard
import com.cangzr.neocard.data.model.TextStyleDTO
import com.cangzr.neocard.ui.screens.createcard.utils.CardCreationUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class CardWithTags(
    val card: UserCard,
    val tags: List<String> = emptyList()
)

data class TagCategory(
    val name: String,
    val tags: List<String>
)

@Composable
fun getPredefinedTagCategories(context: Context): List<TagCategory> {
    return listOf(
        TagCategory(
            context.getString(R.string.tag_category_business_relations),
            listOf(
                context.getString(R.string.tag_customer),
                context.getString(R.string.tag_vip_customer),
                context.getString(R.string.tag_potential_customer),
                context.getString(R.string.tag_supplier),
                context.getString(R.string.tag_strategic_supplier),
                context.getString(R.string.tag_business_partner),
                context.getString(R.string.tag_strategic_partner),
                context.getString(R.string.tag_employee),
                context.getString(R.string.tag_manager),
                context.getString(R.string.tag_investor),
                context.getString(R.string.tag_potential_investor),
                context.getString(R.string.tag_consultant),
                context.getString(R.string.tag_strategic_consultant),
                context.getString(R.string.tag_mentor),
                context.getString(R.string.tag_mentee)
            )
        ),
        TagCategory(
            context.getString(R.string.tag_category_professions),
            listOf(
                context.getString(R.string.tag_software_developer),
                context.getString(R.string.tag_system_engineer),
                context.getString(R.string.tag_data_engineer),
                context.getString(R.string.tag_ui_ux_designer),
                context.getString(R.string.tag_graphic_designer),
                context.getString(R.string.tag_project_manager),
                context.getString(R.string.tag_product_manager),
                context.getString(R.string.tag_marketing_specialist),
                context.getString(R.string.tag_sales_representative),
                context.getString(R.string.tag_hr_specialist),
                context.getString(R.string.tag_finance_specialist),
                context.getString(R.string.tag_accountant),
                context.getString(R.string.tag_lawyer),
                context.getString(R.string.tag_doctor),
                context.getString(R.string.tag_teacher),
                context.getString(R.string.tag_academic),
                context.getString(R.string.tag_architect),
                context.getString(R.string.tag_civil_engineer),
                context.getString(R.string.tag_mechanical_engineer),
                context.getString(R.string.tag_electrical_engineer)
            )
        ),
        TagCategory(
            context.getString(R.string.tag_category_other_relations),
            listOf(
                context.getString(R.string.tag_family),
                context.getString(R.string.tag_friend),
                context.getString(R.string.tag_neighbor),
                context.getString(R.string.tag_student),
                context.getString(R.string.tag_other)
            )
        )
    )
}

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

    val allTagsWithCount = remember(userCards) {
        userCards.flatMap { it.tags }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedBy { it.first }
    }

    val filteredCards = remember(userCards, searchQuery, selectedTags) {
        var filtered = userCards

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

        if (selectedTags.isNotEmpty()) {
            filtered = filtered.filter { cardWithTags ->
                selectedTags.all { tag -> cardWithTags.tags.contains(tag) }
            }
        }

        filtered
    }

    var allConnectedList by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
    var currentPage by remember { mutableStateOf(0) }
    val pageSize = 10
    var hasMoreCards by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var userTagsMap by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    
    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            userCards = emptyList()
            isLoading = false
        } else {
            firestore.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    val connectedList = document.get("connected") as? List<Map<String, String>> ?: emptyList()
                    val userTags = document.get("cardTags") as? Map<String, List<String>> ?: emptyMap()
                    
                    allConnectedList = connectedList
                    userTagsMap = userTags

                    if (connectedList.isEmpty()) {
                        userCards = emptyList()
                        isLoading = false
                        hasMoreCards = false
                    } else {
                        fetchConnectedCards(
                            connectedList = connectedList, 
                            userTags = userTags, 
                            onComplete = { cardsWithTags ->
                                userCards = cardsWithTags
                                isLoading = false
                                hasMoreCards = (pageSize * (currentPage + 1)) < connectedList.size
                                currentPage = 1
                            },
                            pageSize = pageSize, 
                            startIndex = 0
                        )
                    }
                }
                .addOnFailureListener {
                    userCards = emptyList()
                    isLoading = false
                    hasMoreCards = false
                }
        }
    }
    
    fun loadMoreCards() {
        if (!hasMoreCards || isLoadingMore || currentUser == null) return
        
        isLoadingMore = true
        val startIndex = currentPage * pageSize
        
        fetchConnectedCards(
            connectedList = allConnectedList,
            userTags = userTagsMap,
            onComplete = { newCards ->
                userCards = userCards + newCards
                isLoadingMore = false
            },
            pageSize = pageSize,
            startIndex = startIndex
        )
        currentPage++
        hasMoreCards = (pageSize * currentPage) < allConnectedList.size
    }

    if (showTagSelectionDialog != null) {
        var tagSearchQuery by remember { mutableStateOf("") }
        val predefinedCategories = getPredefinedTagCategories(context)
        val filteredCategories = remember(tagSearchQuery) {
            if (tagSearchQuery.isEmpty()) {
                predefinedCategories
            } else {
                predefinedCategories.map { category ->
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
                            text = context.getString(R.string.select_tags),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${showTagSelectionDialog!!.tags.size}/3",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = context.getString(R.string.max_three_tags),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    OutlinedTextField(
                        value = tagSearchQuery,
                        onValueChange = { tagSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(context.getString(R.string.search_tags)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = context.getString(R.string.search),
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
                    Text(context.getString(R.string.ok))
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
                text = context.getString(R.string.my_connections),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                placeholder = { Text(context.getString(R.string.search_cards)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = context.getString(R.string.search)
                    )
                },
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            if (allTagsWithCount.isNotEmpty()) {
                Text(
                    text = context.getString(R.string.filter_by_tags),
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
                                        contentDescription = context.getString(R.string.selected),
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
                        text = context.getString(R.string.login_to_see_connections),
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
                            context.getString(R.string.no_cards_in_connections)
                        else 
                            context.getString(R.string.no_search_results),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val adInterval = AdManager.BUSINESS_CARD_AD_INTERVAL
                val itemsWithAds = withAdItems(
                    items = filteredCards,
                    adInterval = adInterval,
                    itemContent = { cardWithTags ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            UserCardItem(card = cardWithTags.card) {
                                navController.navigate(Screen.SharedCardDetail.createRoute(cardWithTags.card.id))
                            }
                            
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
                                            contentDescription = context.getString(R.string.tag),
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
                        
                        TextButton(
                            onClick = { showTagSelectionDialog = cardWithTags },
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_label),
                                contentDescription = context.getString(R.string.select_tags),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(context.getString(R.string.select_tags))
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
                        
                        if (index == itemsWithAds.size - 3 && hasMoreCards && !isLoadingMore) {
                            LaunchedEffect(index) {
                                loadMoreCards()
                            }
                        }
                    }
                    
                    if (isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(30.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun fetchConnectedCards(
    connectedList: List<Map<String, String>>,
    userTags: Map<String, List<String>>,
    onComplete: (List<CardWithTags>) -> Unit,
    pageSize: Int = 10,
    startIndex: Int = 0
) {
    val firestore = FirebaseFirestore.getInstance()
    val allCards = mutableListOf<CardWithTags>()
    var fetchedCount = 0

    if (connectedList.isEmpty()) {
        onComplete(emptyList())
        return
    }

    val endIndex = minOf(startIndex + pageSize, connectedList.size)
    val currentPageConnections = connectedList.subList(startIndex, endIndex)

    currentPageConnections.forEach { connection ->
        val userId = connection["userId"]
        val cardId = connection["cardId"]

        if (userId.isNullOrEmpty() || cardId.isNullOrEmpty()) {
            fetchedCount++
            if (fetchedCount == currentPageConnections.size) {
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
                if (fetchedCount == currentPageConnections.size) {
                    onComplete(allCards)
                }
            }
            .addOnFailureListener {
                fetchedCount++
                if (fetchedCount == currentPageConnections.size) {
                    onComplete(allCards)
                }
            }
    }
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
                .fillMaxWidth()
                .fillMaxHeight()
                .background(parseBusinessBackground(card, context))
                .padding(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().fillMaxSize()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    if (card.profileImageUrl!!.isNotEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
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

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("${card.name} ${card.surname}", style = parseBusinessTextStyle(card.textStyles["NAME_SURNAME"]))
                        Text(card.title, style = parseBusinessTextStyle(card.textStyles["TITLE"]))
                        Text(card.company, style = parseBusinessTextStyle(card.textStyles["COMPANY"]))
                        Text(card.email, style = parseBusinessTextStyle(card.textStyles["EMAIL"]))
                        Text(card.phone, style = parseBusinessTextStyle(card.textStyles["PHONE"]))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (card.website.isNotEmpty()) BusinessSocialIcon(R.drawable.web, nameSurnameColor)
                    if (card.linkedin.isNotEmpty()) BusinessSocialIcon(R.drawable.linkedin, nameSurnameColor)
                    if (card.github.isNotEmpty()) BusinessSocialIcon(R.drawable.github, nameSurnameColor)
                    if (card.twitter.isNotEmpty()) BusinessSocialIcon(R.drawable.twitt, nameSurnameColor)
                    if (card.instagram.isNotEmpty()) BusinessSocialIcon(R.drawable.insta, nameSurnameColor)
                    if (card.facebook.isNotEmpty()) BusinessSocialIcon(R.drawable.face, nameSurnameColor)
                }
            }
        }
    }
}

@Composable
fun BusinessSocialIcon(iconRes: Int, color: Color) {
    Image(
        painter = painterResource(id = iconRes),
        contentDescription = null,
        modifier = Modifier.size(24.dp),
        colorFilter = ColorFilter.tint(color)
    )
}

fun parseBusinessBackground(card: UserCard, context: Context): Brush {
    return if (card.backgroundType == "GRADIENT") {
        var gradient = CardCreationUtils.getPredefinedGradients(context).firstOrNull { it.first == card.selectedGradient }
        
        if (gradient == null) {
            val allGradients = listOf(
                Pair("Gün Batımı", Brush.horizontalGradient(listOf(Color(0xFFFE6B8B), Color(0xFFFF8E53)))),
                Pair("Sunset", Brush.horizontalGradient(listOf(Color(0xFFFE6B8B), Color(0xFFFF8E53)))),
                Pair("Okyanus", Brush.horizontalGradient(listOf(Color(0xFF2196F3), Color(0xFF00BCD4)))),
                Pair("Ocean", Brush.horizontalGradient(listOf(Color(0xFF2196F3), Color(0xFF00BCD4)))),
                Pair("Orman", Brush.horizontalGradient(listOf(Color(0xFF4CAF50), Color(0xFF8BC34A)))),
                Pair("Forest", Brush.horizontalGradient(listOf(Color(0xFF4CAF50), Color(0xFF8BC34A)))),
                Pair("Gece", Brush.verticalGradient(listOf(Color(0xFF2C3E50), Color(0xFF3498DB)))),
                Pair("Night", Brush.verticalGradient(listOf(Color(0xFF2C3E50), Color(0xFF3498DB)))),
                Pair("Mor Sis", Brush.verticalGradient(listOf(Color(0xFF9C27B0), Color(0xFFE91E63)))),
                Pair("Purple Mist", Brush.verticalGradient(listOf(Color(0xFF9C27B0), Color(0xFFE91E63))))
            )
            gradient = allGradients.firstOrNull { it.first == card.selectedGradient }
        }
        
        gradient?.second ?: Brush.verticalGradient(listOf(Color.Gray, Color.LightGray))
    } else {
        Brush.verticalGradient(
            listOf(
                Color(android.graphics.Color.parseColor(card.backgroundColor)),
                Color(android.graphics.Color.parseColor(card.backgroundColor))
            )
        )
    }
}

fun parseBusinessTextStyle(dto: TextStyleDTO?): androidx.compose.ui.text.TextStyle {
    return androidx.compose.ui.text.TextStyle(
        fontSize = (dto?.fontSize?.sp ?: 16.sp),
        fontWeight = if (dto?.isBold == true) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
        fontStyle = if (dto?.isItalic == true) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
        textDecoration = if (dto?.isUnderlined == true) androidx.compose.ui.text.style.TextDecoration.Underline else androidx.compose.ui.text.style.TextDecoration.None,
        color = Color(android.graphics.Color.parseColor(dto?.color ?: "#000000"))
    )
}
