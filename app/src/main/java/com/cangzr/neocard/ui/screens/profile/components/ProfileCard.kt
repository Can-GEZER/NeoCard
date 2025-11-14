package com.cangzr.neocard.ui.screens.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.cangzr.neocard.R
import com.cangzr.neocard.Screen
import com.cangzr.neocard.billing.BillingManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.runtime.collectAsState

@Composable
fun ProfileCard(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val billingManager = remember { BillingManager.getInstance(context) }
    val isPremium by billingManager.isPremium.collectAsState()

    var userDisplayName by remember { mutableStateOf("") }
    var userPhotoUrl by remember { mutableStateOf<String?>(null) }
    val currentUser = remember { mutableStateOf(auth.currentUser) }
    var unreadNotificationCount by remember { mutableStateOf(0) }

    val isLoggedIn = currentUser.value != null

    LaunchedEffect(currentUser.value) {
        currentUser.value?.let { user ->
            userDisplayName = user.displayName ?: "Bilinmeyen Kullanıcı"
            userPhotoUrl = user.photoUrl?.toString()
            
            firestore.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        if (document.getString("displayName").isNullOrEmpty() && !userDisplayName.isNullOrEmpty()) {
                            firestore.collection("users").document(user.uid)
                                .update("displayName", userDisplayName)
                        }
                    }
                }
            
            firestore.collection("users")
                .document(user.uid)
                .collection("notifications")
                .whereEqualTo("read", false)
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null) {
                        unreadNotificationCount = snapshot.size()
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
            .then(
                if (isPremium && isLoggedIn) {
                    Modifier.border(
                        width = 3.dp,
                        color = androidx.compose.ui.graphics.Color(0xFFFFD700), // Altın rengi
                        shape = RoundedCornerShape(16.dp)
                    )
                } else {
                    Modifier
                }
            )
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
                                text = userDisplayName.ifEmpty { context.getString(R.string.user) },
                                style = MaterialTheme.typography.titleLarge,
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = currentUser.value?.email ?: context.getString(R.string.no_email),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 18.sp
                            )
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.BadgedBox(
                            badge = {
                                if (unreadNotificationCount > 0) {
                                    androidx.compose.material3.Badge(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ) {
                                        Text(
                                            text = if (unreadNotificationCount > 99) "99+" else unreadNotificationCount.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
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
                        text = context.getString(R.string.login_prompt),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Button(
                        onClick = { navController.navigate(Screen.Auth.route) }
                        ) {
                        Text(context.getString(R.string.login))
                    }
                }
            }
        }
    }
}
