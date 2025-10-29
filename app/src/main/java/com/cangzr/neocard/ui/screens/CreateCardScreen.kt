package com.cangzr.neocard.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cangzr.neocard.R
import com.cangzr.neocard.data.CardType
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.cangzr.neocard.ui.screens.createcard.components.CardTypeSelector
import com.cangzr.neocard.ui.screens.createcard.components.ColorButton
import com.cangzr.neocard.ui.screens.createcard.components.FormCardContent
import com.cangzr.neocard.ui.screens.createcard.components.FormCardWithSwitch
import com.cangzr.neocard.ui.screens.createcard.components.GradientButton
import com.cangzr.neocard.ui.screens.createcard.components.PremiumInfoCard
import com.cangzr.neocard.ui.screens.createcard.components.SocialMediaIcon
import com.cangzr.neocard.ui.screens.createcard.utils.CardCreationUtils
import com.cangzr.neocard.ui.screens.createcard.viewmodels.CreateCardViewModel
import com.cangzr.neocard.ui.screens.createcard.viewmodels.BackgroundType
import com.cangzr.neocard.ui.screens.createcard.viewmodels.TextType
import com.cangzr.neocard.utils.ValidationUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCardScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: CreateCardViewModel = hiltViewModel()

    // ViewModel state
    val name by viewModel.name.collectAsState()
    val surname by viewModel.surname.collectAsState()
    val phone by viewModel.phone.collectAsState()
    val email by viewModel.email.collectAsState()
    val company by viewModel.company.collectAsState()
    val title by viewModel.title.collectAsState()
    val website by viewModel.website.collectAsState()
    val linkedin by viewModel.linkedin.collectAsState()
    val instagram by viewModel.instagram.collectAsState()
    val twitter by viewModel.twitter.collectAsState()
    val facebook by viewModel.facebook.collectAsState()
    val github by viewModel.github.collectAsState()
    val backgroundColor by viewModel.backgroundColor.collectAsState()
    val backgroundType by viewModel.backgroundType.collectAsState()
    val selectedGradient by viewModel.selectedGradient.collectAsState()
    val textStyles by viewModel.textStyles.collectAsState()
    val selectedCardType by viewModel.selectedCardType.collectAsState()
    val profileImageUri by viewModel.profileImageUri.collectAsState()
    val selectedImageBitmap by viewModel.selectedImageBitmap.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val showPremiumDialog by viewModel.showPremiumDialog.collectAsState()
    val isPublic by viewModel.isPublic.collectAsState()
    
    // Validation error states
    val nameError by viewModel.nameError.collectAsState()
    val surnameError by viewModel.surnameError.collectAsState()
    val emailError by viewModel.emailError.collectAsState()
    val phoneError by viewModel.phoneError.collectAsState()
    val websiteError by viewModel.websiteError.collectAsState()
    val linkedinError by viewModel.linkedinError.collectAsState()
    val githubError by viewModel.githubError.collectAsState()
    val twitterError by viewModel.twitterError.collectAsState()
    val instagramError by viewModel.instagramError.collectAsState()
    val facebookError by viewModel.facebookError.collectAsState()
    
    // Resource state
    val uiStateResource by viewModel.uiState.collectAsState()
    val isLoading = uiStateResource is com.cangzr.neocard.common.Resource.Loading

    // Local state
    var selectedText by remember { mutableStateOf<TextType?>(null) }
    var showImageOptions by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle UI state changes
    LaunchedEffect(uiStateResource) {
        when (val state = uiStateResource) {
            is com.cangzr.neocard.common.Resource.Success -> {
                if (state.data.isSaved) {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.card_saved),
                        duration = SnackbarDuration.Short
                    )
                    navController.popBackStack()
                    viewModel.resetState()
                }
            }
            is com.cangzr.neocard.common.Resource.Error -> {
                // Use userMessage instead of technical message
                snackbarHostState.showSnackbar(
                    message = state.userMessage,
                    duration = SnackbarDuration.Long
                )
                viewModel.resetState()
            }
            is com.cangzr.neocard.common.Resource.Loading -> {
                // Loading indicator handled via isLoading
            }
        }
    }

    fun saveCard() {
        viewModel.saveCard(
            context = context,
            onSuccess = {
                // Success handled in LaunchedEffect
            }
        )
    }

    // Galeri launcher'ı
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.updateProfileImageUri(it)
            val bitmap = CardCreationUtils.uriToBitmap(it, context)
            bitmap?.let { bmp ->
                viewModel.updateSelectedImageBitmap(bmp)
            }
            showEditDialog = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Başlık ve Kaydet Butonu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = context.getString(R.string.create_card),
                    style = MaterialTheme.typography.headlineSmall
                )

                Button(
                    onClick = { saveCard() },
                    modifier = Modifier.wrapContentWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(context.getString(R.string.save))
                }
            }

            // Önizleme Alanı
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            when (backgroundType) {
                                BackgroundType.SOLID -> Modifier.background(backgroundColor)
                                BackgroundType.GRADIENT -> Modifier.background(selectedGradient.second)
                            }
                        )
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Profil Fotoğrafı
                            if (selectedImageBitmap != null || profileImageUri != null) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                        .clickable { if (profileImageUri != null) showImageOptions = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        selectedImageBitmap != null -> {
                                            Image(
                                                bitmap = selectedImageBitmap!!.asImageBitmap(),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                        profileImageUri != null -> {
                                            AsyncImage(
                                                model = profileImageUri,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                }
                            }

                            // Kişisel Bilgiler
                            Column {
                                Text(
                                    text = "$name $surname",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = textStyles[TextType.NAME_SURNAME]?.fontSize?.sp ?: 18.sp,
                                        fontWeight = if (textStyles[TextType.NAME_SURNAME]?.isBold == true) FontWeight.Bold else FontWeight.Normal,
                                        fontStyle = if (textStyles[TextType.NAME_SURNAME]?.isItalic == true) FontStyle.Italic else FontStyle.Normal,
                                        textDecoration = if (textStyles[TextType.NAME_SURNAME]?.isUnderlined == true) TextDecoration.Underline else TextDecoration.None
                                    ),
                                    color = textStyles[TextType.NAME_SURNAME]?.color ?: Color.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clickable { selectedText = TextType.NAME_SURNAME }
                                )
                                if (title.isNotEmpty()) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = textStyles[TextType.TITLE]?.fontSize?.sp ?: 16.sp,
                                            fontWeight = if (textStyles[TextType.TITLE]?.isBold == true) FontWeight.Bold else FontWeight.Normal,
                                            fontStyle = if (textStyles[TextType.TITLE]?.isItalic == true) FontStyle.Italic else FontStyle.Normal,
                                            textDecoration = if (textStyles[TextType.TITLE]?.isUnderlined == true) TextDecoration.Underline else TextDecoration.None
                                        ),
                                        color = textStyles[TextType.TITLE]?.color ?: Color.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.clickable { selectedText = TextType.TITLE }
                                    )
                                }
                                if (company.isNotEmpty()) {
                                    Text(
                                        text = company,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = textStyles[TextType.COMPANY]?.fontSize?.sp ?: 14.sp,
                                            fontWeight = if (textStyles[TextType.COMPANY]?.isBold == true) FontWeight.Bold else FontWeight.Normal,
                                            fontStyle = if (textStyles[TextType.COMPANY]?.isItalic == true) FontStyle.Italic else FontStyle.Normal,
                                            textDecoration = if (textStyles[TextType.COMPANY]?.isUnderlined == true) TextDecoration.Underline else TextDecoration.None
                                        ),
                                        color = textStyles[TextType.COMPANY]?.color ?: Color.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.clickable { selectedText = TextType.COMPANY }
                                    )
                                }
                                if (email.isNotEmpty()) {
                                    Text(
                                        text = email,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = textStyles[TextType.EMAIL]?.fontSize?.sp ?: 14.sp,
                                            fontWeight = if (textStyles[TextType.EMAIL]?.isBold == true) FontWeight.Bold else FontWeight.Normal,
                                            fontStyle = if (textStyles[TextType.EMAIL]?.isItalic == true) FontStyle.Italic else FontStyle.Normal,
                                            textDecoration = if (textStyles[TextType.EMAIL]?.isUnderlined == true) TextDecoration.Underline else TextDecoration.None
                                        ),
                                        color = textStyles[TextType.EMAIL]?.color ?: Color.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.clickable { selectedText = TextType.EMAIL }
                                    )
                                }
                                if (phone.isNotEmpty()) {
                                    Text(
                                        text = phone,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = textStyles[TextType.PHONE]?.fontSize?.sp ?: 14.sp,
                                            fontWeight = if (textStyles[TextType.PHONE]?.isBold == true) FontWeight.Bold else FontWeight.Normal,
                                            fontStyle = if (textStyles[TextType.PHONE]?.isItalic == true) FontStyle.Italic else FontStyle.Normal,
                                            textDecoration = if (textStyles[TextType.PHONE]?.isUnderlined == true) TextDecoration.Underline else TextDecoration.None
                                        ),
                                        color = textStyles[TextType.PHONE]?.color ?: Color.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.clickable { selectedText = TextType.PHONE }
                                    )
                                }
                            }
                        }

                        // Sosyal Medya İkonları
                        Row(
                            modifier = Modifier
                                .padding(start = 16.dp, top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (linkedin.isNotEmpty()) {
                                SocialMediaIcon(
                                    iconRes = R.drawable.linkedin,
                                    contentDescription = "LinkedIn",
                                    tint = textStyles[TextType.NAME_SURNAME]?.color ?: Color.Black
                                )
                            }
                            if (github.isNotEmpty()) {
                                SocialMediaIcon(
                                    iconRes = R.drawable.github,
                                    contentDescription = "GitHub",
                                    tint = textStyles[TextType.NAME_SURNAME]?.color ?: Color.Black
                                )
                            }
                            if (twitter.isNotEmpty()) {
                                SocialMediaIcon(
                                    iconRes = R.drawable.twitt,
                                    contentDescription = "Twitter (X)",
                                    tint = textStyles[TextType.NAME_SURNAME]?.color ?: Color.Black
                                )
                            }
                            if (instagram.isNotEmpty()) {
                                SocialMediaIcon(
                                    iconRes = R.drawable.insta,
                                    contentDescription = "Instagram",
                                    tint = textStyles[TextType.NAME_SURNAME]?.color ?: Color.Black
                                )
                            }
                            if (facebook.isNotEmpty()) {
                                SocialMediaIcon(
                                    iconRes = R.drawable.face,
                                    contentDescription = "Facebook",
                                    tint = textStyles[TextType.NAME_SURNAME]?.color ?: Color.Black
                                )
                            }
                            if (website.isNotEmpty()) {
                                SocialMediaIcon(
                                    iconRes = R.drawable.web,
                                    contentDescription = "Web",
                                    tint = textStyles[TextType.NAME_SURNAME]?.color ?: Color.Black
                                )
                            }
                        }
                    }
                }
            }

            // Premium avantajlarını gösteren dialog
            if (showPremiumDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.updateShowPremiumDialog(false) },
                    title = {
                        Text(
                                            text = context.getString(R.string.premium_benefits),
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = context.getString(R.string.premium_advantages_description),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = context.getString(R.string.premium_unlimited_cards),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = context.getString(R.string.premium_gradient_backgrounds),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = context.getString(R.string.premium_custom_text_styles),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = context.getString(R.string.premium_ad_free),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.updateShowPremiumDialog(false) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(context.getString(R.string.ok))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { navController.navigate("profile") {
                                popUpTo("createcard") { inclusive = true }
                            } }
                        ) {
                            Text(context.getString(R.string.get_premium))
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Form Alanı
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profil Fotoğrafı
                FormCardContent(title = context.getString(R.string.profile_picture_logo)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.gallery),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(context.getString(R.string.choose_from_gallery))
                            }

                            if (profileImageUri != null) {
                                Button(
                                    onClick = {
                                        viewModel.updateProfileImageUri(null)
                                        viewModel.updateSelectedImageBitmap(null)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(context.getString(R.string.remove_profile_picture))
                                }
                            }
                        }
                    }
                }

                // Kişisel Bilgiler Kartı
                FormCardContent(title = context.getString(R.string.personal_info)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { viewModel.updateName(it) },
                        label = { Text(context.getString(R.string.name)) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = nameError != null,
                        supportingText = nameError?.let { 
                            { Text(it, color = MaterialTheme.colorScheme.error) }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = surname,
                        onValueChange = { viewModel.updateSurname(it) },
                        label = { Text(context.getString(R.string.surname)) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = surnameError != null,
                        supportingText = surnameError?.let { 
                            { Text(it, color = MaterialTheme.colorScheme.error) }
                        }
                    )
                }

                // İletişim Bilgileri Kartı
                FormCardContent(title = context.getString(R.string.contact_info)) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { 
                            val filteredInput = ValidationUtils.filterPhoneInput(it)
                            viewModel.updatePhone(filteredInput)
                        },
                        label = { Text(context.getString(R.string.phone)) },
                        leadingIcon = { Icon(Icons.Default.Phone, null) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = phoneError != null,
                        supportingText = phoneError?.let { 
                            { Text(it, color = MaterialTheme.colorScheme.error) }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { 
                            val filteredInput = ValidationUtils.filterEmailInput(it)
                            viewModel.updateEmail(filteredInput)
                        },
                        label = { Text(context.getString(R.string.email)) },
                        leadingIcon = { Icon(Icons.Default.Email, null) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = emailError != null,
                        supportingText = emailError?.let { 
                            { Text(it, color = MaterialTheme.colorScheme.error) }
                        }
                    )
                }

                // İş Bilgileri Kartı
                FormCardContent(title = context.getString(R.string.business_info)) {
                    OutlinedTextField(
                        value = company,
                        onValueChange = { viewModel.updateCompany(it) },
                        label = { Text(context.getString(R.string.company)) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.company),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { viewModel.updateTitle(it) },
                        label = { Text(context.getString(R.string.title)) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.statue),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = website,
                        onValueChange = { 
                            val filteredInput = ValidationUtils.filterWebsiteInput(it)
                            viewModel.updateWebsite(filteredInput)
                        },
                        label = { Text(context.getString(R.string.website)) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.web),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isError = website.isNotEmpty() && !ValidationUtils.isValidWebsite(website),
                        supportingText = if (website.isNotEmpty() && !ValidationUtils.isValidWebsite(website)) {
                            { Text("Geçerli bir website adresi girin (örn: example.com)", color = MaterialTheme.colorScheme.error) }
                        } else null
                    )
                }

                // Sosyal Medya Kartı
                FormCardContent(title = context.getString(R.string.social_media)) {
                    OutlinedTextField(
                        value = linkedin,
                        onValueChange = { 
                            val filteredInput = ValidationUtils.filterSocialInput(it)
                            viewModel.updateLinkedin(filteredInput)
                        },
                        label = { Text(context.getString(R.string.linkedin)) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = linkedin.isNotEmpty() && !ValidationUtils.isValidLinkedIn(linkedin),
                        supportingText = if (linkedin.isNotEmpty() && !ValidationUtils.isValidLinkedIn(linkedin)) {
                            { Text("Geçerli bir LinkedIn profili girin (örn: linkedin.com/in/username)", color = MaterialTheme.colorScheme.error) }
                        } else null
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = github,
                        onValueChange = { 
                            val filteredInput = ValidationUtils.filterSocialInput(it)
                            viewModel.updateGithub(filteredInput)
                        },
                        label = { Text(context.getString(R.string.github)) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = github.isNotEmpty() && !ValidationUtils.isValidGitHub(github),
                        supportingText = if (github.isNotEmpty() && !ValidationUtils.isValidGitHub(github)) {
                            { Text("Geçerli bir GitHub profili girin (örn: github.com/username)", color = MaterialTheme.colorScheme.error) }
                        } else null
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = twitter,
                        onValueChange = { 
                            val filteredInput = ValidationUtils.filterSocialInput(it)
                            viewModel.updateTwitter(filteredInput)
                        },
                        label = { Text(context.getString(R.string.twitter)) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = twitter.isNotEmpty() && !ValidationUtils.isValidTwitter(twitter),
                        supportingText = if (twitter.isNotEmpty() && !ValidationUtils.isValidTwitter(twitter)) {
                            { Text("Geçerli bir Twitter profili girin (örn: twitter.com/username)", color = MaterialTheme.colorScheme.error) }
                        } else null
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = instagram,
                        onValueChange = { 
                            val filteredInput = ValidationUtils.filterSocialInput(it)
                            viewModel.updateInstagram(filteredInput)
                        },
                        label = { Text(context.getString(R.string.instagram)) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = instagram.isNotEmpty() && !ValidationUtils.isValidInstagram(instagram),
                        supportingText = if (instagram.isNotEmpty() && !ValidationUtils.isValidInstagram(instagram)) {
                            { Text("Geçerli bir Instagram profili girin (örn: instagram.com/username)", color = MaterialTheme.colorScheme.error) }
                        } else null
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = facebook,
                        onValueChange = { 
                            val filteredInput = ValidationUtils.filterSocialInput(it)
                            viewModel.updateFacebook(filteredInput)
                        },
                        label = { Text(context.getString(R.string.facebook)) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = facebook.isNotEmpty() && !ValidationUtils.isValidFacebook(facebook),
                        supportingText = if (facebook.isNotEmpty() && !ValidationUtils.isValidFacebook(facebook)) {
                            { Text("Geçerli bir Facebook profili girin (örn: facebook.com/username)", color = MaterialTheme.colorScheme.error) }
                        } else null
                    )
                }

                // Tasarım Kartı
                FormCardContent(title = context.getString(R.string.design)) {
                    // Arkaplan Tipi Seçimi
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = backgroundType == BackgroundType.SOLID,
                            onClick = { viewModel.updateBackgroundType(BackgroundType.SOLID) },
                            label = { Text(context.getString(R.string.solid_color)) },
                            enabled = true
                        )
                        FilterChip(
                            selected = backgroundType == BackgroundType.GRADIENT,
                            onClick = { if (isPremium) viewModel.updateBackgroundType(BackgroundType.GRADIENT) },
                            label = { Text(context.getString(R.string.gradient)) },
                            enabled = isPremium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    when (backgroundType) {
                        BackgroundType.SOLID -> {
                            Text(context.getString(R.string.background_color))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                val colors = listOf(
                                    Color.White to context.getString(R.string.color_white),
                                    Color.Black to context.getString(R.string.color_black),
                                    Color(0xFFFFD700) to context.getString(R.string.color_gold),
                                    Color(0xFF40E0D0) to context.getString(R.string.color_turquoise),
                                    Color(0xFF4CAF50) to context.getString(R.string.color_green),
                                    Color(0xFFFF9800) to context.getString(R.string.color_orange),
                                    Color(0xFF2196F3) to context.getString(R.string.color_blue),
                                    Color(0xFFE91E63) to context.getString(R.string.color_pink),
                                    Color(0xFF9C27B0) to context.getString(R.string.color_purple),
                                    Color(0xFF795548) to context.getString(R.string.color_brown),
                                    Color(0xFF607D8B) to context.getString(R.string.color_gray),
                                    Color(0xFFF44336) to context.getString(R.string.color_red)
                                )
                                items(colors) { (color, name) ->
                                    ColorButton(
                                        color = color,
                                        selectedColor = backgroundColor,
                                        name = name,
                                        onColorSelected = { viewModel.updateBackgroundColor(it) }
                                    )
                                }
                            }
                        }
                        BackgroundType.GRADIENT -> {
                            Text(context.getString(R.string.select_gradient))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                items(CardCreationUtils.getPredefinedGradients(context)) { (name, brush) ->
                                    GradientButton(
                                        name = name,
                                        brush = brush,
                                        isSelected = selectedGradient.first == name,
                                        onSelect = { if (isPremium) viewModel.updateSelectedGradient(Pair(name, brush)) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Yazı Stilleri Kartı
                if (isPremium) {
                    FormCardContent(title = context.getString(R.string.text_styles)) {
                        if (selectedText != null) {
                            val style = textStyles[selectedText] ?: return@FormCardContent

                            // Stil seçenekleri
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = style.isBold,
                                    onClick = { viewModel.updateTextStyle(selectedText!!, style.copy(isBold = !style.isBold)) },
                                    label = { Text("B", fontWeight = FontWeight.Bold) }
                                )
                                FilterChip(
                                    selected = style.isItalic,
                                    onClick = { viewModel.updateTextStyle(selectedText!!, style.copy(isItalic = !style.isItalic)) },
                                    label = { Text("I", fontStyle = FontStyle.Italic) }
                                )
                                FilterChip(
                                    selected = style.isUnderlined,
                                    onClick = { viewModel.updateTextStyle(selectedText!!, style.copy(isUnderlined = !style.isUnderlined)) },
                                    label = { Text("U", textDecoration = TextDecoration.Underline) }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Font boyutu Slider
                                Text(context.getString(R.string.font_size, style.fontSize.toInt()))
                            Slider(
                                value = style.fontSize,
                                onValueChange = { newSize ->
                                    viewModel.updateTextStyle(selectedText!!, style.copy(fontSize = newSize))
                                },
                                valueRange = 12f..24f,
                                steps = 11,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Renk seçici
                            Text(context.getString(R.string.text_color))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                val colors = listOf(
                                    Color.White to context.getString(R.string.color_white),
                                    Color.Black to context.getString(R.string.color_black),
                                    Color(0xFFFFD700) to context.getString(R.string.color_gold),
                                    Color(0xFF40E0D0) to context.getString(R.string.color_turquoise),
                                    Color(0xFF4CAF50) to context.getString(R.string.color_green),
                                    Color(0xFFFF9800) to context.getString(R.string.color_orange),
                                    Color(0xFF2196F3) to context.getString(R.string.color_blue),
                                    Color(0xFFE91E63) to context.getString(R.string.color_pink),
                                    Color(0xFF9C27B0) to context.getString(R.string.color_purple),
                                    Color(0xFF795548) to context.getString(R.string.color_brown),
                                    Color(0xFF607D8B) to context.getString(R.string.color_gray),
                                    Color(0xFFF44336) to context.getString(R.string.color_red)
                                )
                                items(colors) { (color, name) ->
                                    ColorButton(
                                        color = color,
                                        selectedColor = style.color,
                                        name = name,
                                        onColorSelected = { newColor ->
                                            viewModel.updateTextStyle(selectedText!!, style.copy(color = newColor))
                                        }
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = context.getString(R.string.no_text_selected),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Kart Tipi Seçimi
                CardTypeSelector(
                    selectedType = selectedCardType,
                    onTypeSelected = { viewModel.updateSelectedCardType(it) }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Gizlilik Ayarları
                FormCardWithSwitch(
                    title = context.getString(R.string.visibility),
                    isChecked = isPublic,
                    onCheckedChange = { viewModel.updateIsPublic(it) }
                )
                
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Yükleme göstergesi
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .width(200.dp)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                                text = context.getString(R.string.saving_card),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        
        // Snackbar host for error messages
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
    // Premium değilse küçük bir bilgi kartı göster
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        if (!isPremium) {
            PremiumInfoCard(onClick = { viewModel.updateShowPremiumDialog(true) })
        }
    }
}

@Composable
fun PremiumInfoCard(onClick: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                painter = painterResource(id = R.drawable.info), // Bilgi ikonu
                contentDescription = context.getString(R.string.premium_benefits),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = context.getString(R.string.get_premium),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}