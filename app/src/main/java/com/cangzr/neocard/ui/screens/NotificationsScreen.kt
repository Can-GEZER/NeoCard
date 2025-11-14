package com.cangzr.neocard.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import com.cangzr.neocard.R
import com.cangzr.neocard.Screen
import com.cangzr.neocard.notifications.NotificationManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

data class NotificationItem(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val timestamp: Timestamp?,
    val read: Boolean,
    val senderId: String?,
    val cardId: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    
    var notifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val currentUser = auth.currentUser
    
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            try {
                val snapshot = firestore.collection("users")
                    .document(currentUser.uid)
                    .collection("notifications")
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                notifications = snapshot.documents.mapNotNull { doc ->
                    try {
                        NotificationItem(
                            id = doc.id,
                            type = doc.getString("type") ?: "DEFAULT",
                            title = doc.getString("title") ?: "",
                            body = doc.getString("body") ?: "",
                            timestamp = doc.getTimestamp("timestamp"),
                            read = doc.getBoolean("read") ?: false,
                            senderId = doc.getString("senderId"),
                            cardId = doc.getString("cardId")
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(context.getString(R.string.notifications)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = context.getString(R.string.back)
                        )
                    }
                },
                actions = {
                    if (notifications.any { !it.read }) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    currentUser?.let { user ->
                                        notifications.forEach { notification ->
                                            if (!notification.read) {
                                                NotificationManager.markNotificationAsRead(
                                                    user.uid,
                                                    notification.id
                                                )
                                            }
                                        }
                                        notifications = notifications.map { it.copy(read = true) }
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = context.getString(R.string.mark_all_as_read)
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                notifications.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = context.getString(R.string.no_notifications),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = notifications,
                            key = { it.id }
                        ) { notification ->
                            var isVisible by remember { mutableStateOf(true) }
                            
                            AnimatedVisibility(
                                visible = isVisible,
                                exit = fadeOut(animationSpec = tween(300)) + 
                                       shrinkVertically(animationSpec = tween(300))
                            ) {
                                SwipeToDeleteNotificationItem(
                                    notification = notification,
                                    onDelete = {
                                        scope.launch {
                                            isVisible = false
                                            delay(300) // Animation süresi
                                            currentUser?.let { user ->
                                                firestore.collection("users")
                                                    .document(user.uid)
                                                    .collection("notifications")
                                                    .document(notification.id)
                                                    .delete()
                                                
                                                notifications = notifications.filter { it.id != notification.id }
                                            }
                                        }
                                    },
                                    onClick = {
                                        scope.launch {
                                            if (!notification.read) {
                                                currentUser?.let { user ->
                                                    NotificationManager.markNotificationAsRead(
                                                        user.uid,
                                                        notification.id
                                                    )
                                                }
                                            }
                                            
                                            when (notification.type) {
                                                "CONNECTION_REQUEST" -> {
                                                    navController.navigate(Screen.ConnectionRequests.route)
                                                }
                                                "CONNECTION_ACCEPTED" -> {
                                                    navController.navigate(Screen.Business.route)
                                                }
                                                "CARD_UPDATED" -> {
                                                    notification.cardId?.let { cardId ->
                                                        navController.navigate(Screen.SharedCardDetail.createRoute(cardId))
                                                    }
                                                }
                                            }
                                        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteNotificationItem(
    notification: NotificationItem,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )
    
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                else -> Color.Transparent
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, shape = RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Sil",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    ) {
        NotificationItemCard(
            notification = notification,
            onClick = onClick
        )
    }
}

@Composable
fun NotificationItemCard(
    notification: NotificationItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val notificationColor = getNotificationColor(notification.type)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notification.read) 2.dp else 4.dp)
    ) {
        Box {
            if (!notification.read) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    notificationColor,
                                    notificationColor.copy(alpha = 0.5f)
                                )
                            )
                        )
                        .align(Alignment.CenterStart)
                )
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = if (notification.read) 16.dp else 20.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 16.dp
                    ),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    notificationColor.copy(alpha = 0.15f),
                                    notificationColor.copy(alpha = 0.05f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getNotificationIcon(notification.type),
                        contentDescription = null,
                        tint = notificationColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = getNotificationTypeLabel(context, notification.type),
                            style = MaterialTheme.typography.labelLarge,
                            color = notificationColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            notification.timestamp?.let { timestamp ->
                                Text(
                                    text = getTimeAgo(context, timestamp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            
                            if (!notification.read) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(notificationColor)
                                )
                            }
                        }
                    }
                    
                    Text(
                        text = notification.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (notification.read) FontWeight.Normal else FontWeight.SemiBold,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun getNotificationIcon(type: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (type) {
        "CONNECTION_REQUEST" -> Icons.Default.PersonAdd
        "CONNECTION_ACCEPTED" -> Icons.Default.CheckCircle
        "CARD_UPDATED" -> Icons.Default.Update
        else -> Icons.Default.Notifications
    }
}

@Composable
fun getNotificationColor(type: String): androidx.compose.ui.graphics.Color {
    return when (type) {
        "CONNECTION_REQUEST" -> MaterialTheme.colorScheme.primary
        "CONNECTION_ACCEPTED" -> MaterialTheme.colorScheme.tertiary
        "CARD_UPDATED" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

fun getNotificationTypeLabel(context: android.content.Context, type: String): String {
    return when (type) {
        "CONNECTION_REQUEST" -> context.getString(R.string.notification_connection_request)
        "CONNECTION_ACCEPTED" -> context.getString(R.string.notification_connection_accepted)
        "CARD_UPDATED" -> context.getString(R.string.notification_card_updated)
        else -> context.getString(R.string.notifications)
    }
}

fun getTimeAgo(context: android.content.Context, timestamp: Timestamp): String {
    val now = System.currentTimeMillis()
    val time = timestamp.toDate().time
    val diff = now - time
    
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    val weeks = days / 7
    
    return when {
        minutes < 1 -> context.getString(R.string.just_now)
        minutes < 60 -> context.getString(R.string.minutes_ago, minutes.toInt())
        hours < 24 -> context.getString(R.string.hours_ago, hours.toInt())
        days < 7 -> context.getString(R.string.days_ago, days.toInt())
        else -> context.getString(R.string.weeks_ago, weeks.toInt())
    }
}

