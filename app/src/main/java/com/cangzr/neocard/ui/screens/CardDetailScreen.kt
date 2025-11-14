package com.cangzr.neocard.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.cangzr.neocard.R
import com.cangzr.neocard.billing.BillingManager
import com.cangzr.neocard.data.CardType
import com.cangzr.neocard.data.model.UserCard
import com.cangzr.neocard.Screen
import com.cangzr.neocard.ui.screens.carddetail.components.ExpandableStatisticsHeader
import com.cangzr.neocard.ui.screens.carddetail.components.InfoDisplayColumn
import com.cangzr.neocard.ui.screens.carddetail.components.StatisticsCard
import com.cangzr.neocard.ui.screens.carddetail.utils.CardDetailRepository
import com.cangzr.neocard.ui.screens.carddetail.viewmodels.CardDetailViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import coil.size.Size
import com.cangzr.neocard.ads.BottomBannerAd

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    cardId: String,
    onBackClick: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val billingManager = remember { BillingManager.getInstance(context) }
    val isPremium by billingManager.isPremium.collectAsState()
    val viewModel: CardDetailViewModel = hiltViewModel()

    val userCard by viewModel.userCard.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val isDeleting by viewModel.isDeleting.collectAsState()
    val cardStatistics by viewModel.cardStatistics.collectAsState()
    val isLoadingStats by viewModel.isLoadingStats.collectAsState()
    
    val name by viewModel.name.collectAsState()
    val surname by viewModel.surname.collectAsState()
    val title by viewModel.title.collectAsState()
    val company by viewModel.company.collectAsState()
    val phone by viewModel.phone.collectAsState()
    val email by viewModel.email.collectAsState()
    val website by viewModel.website.collectAsState()
    val linkedin by viewModel.linkedin.collectAsState()
    val github by viewModel.github.collectAsState()
    val twitter by viewModel.twitter.collectAsState()
    val instagram by viewModel.instagram.collectAsState()
    val facebook by viewModel.facebook.collectAsState()
    val bio by viewModel.bio.collectAsState()
    val cv by viewModel.cv.collectAsState()
    
    val nameError by viewModel.nameError.collectAsState()
    val surnameError by viewModel.surnameError.collectAsState()
    val phoneError by viewModel.phoneError.collectAsState()
    val emailError by viewModel.emailError.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    var showErrorMessage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showStatistics by remember { mutableStateOf(false) }

    LaunchedEffect(cardId) {
        viewModel.loadCard(cardId, context)
                    if (isPremium) {
            viewModel.loadCardStatistics(cardId, context)
        }
    }

if (showSuccessMessage) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000)
            showSuccessMessage = false
        }
        Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
    }

    if (showErrorMessage) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000)
            showErrorMessage = false
        }
        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(context.getString(R.string.loading_card))
            }
        }
        return
    }

    if (userCard == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(id = R.drawable.info),
                    contentDescription = context.getString(R.string.error),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(context.getString(R.string.card_not_found), style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { onBackClick() }) {
                    Text(context.getString(R.string.go_back))
                }
            }
        }
        return
    }

    val cardType = CardType.entries.find { it.name == userCard?.cardType } ?: CardType.FREELANCE

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(context.getString(R.string.delete_card)) },
            text = { Text(context.getString(R.string.confirm_delete)) },
            confirmButton = {
                Button(
                    onClick = {
                        val profileImageUrl = userCard?.profileImageUrl ?: ""
                        CardDetailRepository.deleteCardWithImage(
                            cardId = cardId,
                            profileImageUrl = profileImageUrl,
                            context = context,
                            onSuccess = {
                                            showDeleteDialog = false
                                            successMessage = context.getString(R.string.card_deleted)
                                            showSuccessMessage = true
                                            onBackClick()
                            },
                            onError = { error ->
                                errorMessage = error
                                            showErrorMessage = true
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onError
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(context.getString(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(context.getString(R.string.cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(context.getString(R.string.card_detail)) },
                navigationIcon = {
                    IconButton(onClick = { onBackClick() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = context.getString(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }, enabled = !isSaving) {
                        Icon(Icons.Default.Delete, contentDescription = context.getString(R.string.delete), tint = MaterialTheme.colorScheme.error)
                    }
                    IconButton(onClick = {
                        navController.navigate(Screen.EditCard.createRoute(cardId))
                    }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = context.getString(R.string.edit)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isPremium) {
                ExpandableStatisticsHeader(
                    isExpanded = showStatistics,
                    onToggle = { showStatistics = !showStatistics }
                ) {
                    StatisticsCard(
                        statistics = cardStatistics,
                        isLoading = isLoadingStats,
                        onRefresh = { viewModel.loadCardStatistics(cardId, context) }
                    )
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = context.getString(R.string.statistics),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = context.getString(R.string.statistics_premium_only),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(onClick = { 
                            navController.navigate(Screen.Profile.route)
                            {
                                popUpTo(Screen.Profile.route) { inclusive = true }}
                        }) {
                            Text(context.getString(R.string.get_premium))
                        }
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .padding(vertical = 24.dp)
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (userCard?.profileImageUrl != null && userCard?.profileImageUrl?.isNotEmpty() == true) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(userCard?.profileImageUrl)
                            .crossfade(true)
                            .size(Size.ORIGINAL)
                            .placeholder(R.drawable.logo3)
                            .error(R.drawable.logo3)
                            .transformations(CircleCropTransformation())
                            .build(),
                            contentDescription = context.getString(R.string.profile_picture),
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.AccountCircle, null, Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }

            InfoDisplayColumn(
                name = name,
                surname = surname,
                title = title,
                company = company,
                phone = phone,
                email = email,
                website = website,
                linkedin = linkedin,
                github = github,
                twitter = twitter,
                instagram = instagram,
                facebook = facebook,
                cardType = cardType,
                bio = bio,
                cv = cv,
                skills = userCard?.skills ?: emptyList(),
                isPremium = isPremium
            )
        }
        
        BottomBannerAd(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

