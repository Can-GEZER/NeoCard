package com.cangzr.neocard.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.cangzr.neocard.R
import com.cangzr.neocard.data.CardType
import com.cangzr.neocard.ui.screens.home.components.ExploreCardsSection
import com.cangzr.neocard.ui.screens.home.components.UserCardGallery
import com.cangzr.neocard.ui.screens.home.components.UserCardDialog
import com.cangzr.neocard.ui.screens.home.components.ShareBottomSheet
import com.cangzr.neocard.ui.screens.home.viewmodels.HomeViewModel
import com.cangzr.neocard.data.model.UserCard
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel()

    var showCardTypeDropdown by remember { mutableStateOf(false) }
    val allFilterText = context.getString(R.string.all)
    var selectedCardType by remember { mutableStateOf(allFilterText) }
    
    // Dialog state
    var showCardDialog by remember { mutableStateOf(false) }
    var dialogCard by remember { mutableStateOf<UserCard?>(null) }
    
    // Bottom sheet state
    val bottomSheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    
    // Kart tipleri listesi - "Tümü" ve CardType enum değerleri
    val cardTypes = listOf(allFilterText) + CardType.entries.map { it.getTitle() }
    
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
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
                        Text(
                text = context.getString(R.string.my_cards),
                style = MaterialTheme.typography.titleLarge
            )
            
            // Kart Tipi Filtresi
            Box {
                FilterChip(
                    onClick = { showCardTypeDropdown = true },
                    label = { Text(selectedCardType) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    selected = false,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                
                    DropdownMenu(
                        expanded = showCardTypeDropdown,
                    onDismissRequest = { showCardTypeDropdown = false }
                    ) {
                    cardTypes.forEach { cardType ->
                            DropdownMenuItem(
                            text = { Text(cardType) },
                                onClick = {
                                selectedCardType = cardType
                                    showCardTypeDropdown = false
                                viewModel.updateSelectedCardType(cardType)
                                }
                            )
                        }
                    }
                }
            }

            // Kartvizit Galerisi
            Box(
                modifier = Modifier.height(200.dp)
            ) {
                UserCardGallery(
                    navController = navController,
                    filterType = selectedCardType,
                    onCardSelected = { card ->
                        dialogCard = card
                        showCardDialog = true
                    }
                )
            }
        
        // Keşfet Kartları - Ayrı scrollable container
                ExploreCardsSection(navController = navController)
    }
    
    // Card Detail Dialog - Ekran yüzeyinde göster
    if (showCardDialog && dialogCard != null) {
        Box(
            modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
            // Koyu arka plan
            Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                    .background(
                        color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f)
                    )
            )
            
            // Dialog içeriği
            UserCardDialog(
                onViewDetails = {
                    navController.navigate("card_detail/${dialogCard?.id}")
                    showCardDialog = false
                    dialogCard = null
                },
                onShare = {
                    showCardDialog = false
                    showBottomSheet = true
                },
                onDismiss = {
                    showCardDialog = false
                    dialogCard = null
                }
            )
        }
    }
    
    // Bottom Sheet for sharing
    if (showBottomSheet && dialogCard != null) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = bottomSheetState
        ) {
            ShareBottomSheet(
                card = dialogCard!!,
                onDismiss = { showBottomSheet = false }
            )
        }
    }
}