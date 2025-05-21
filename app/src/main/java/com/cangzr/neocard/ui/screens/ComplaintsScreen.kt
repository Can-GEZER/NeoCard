package com.cangzr.neocard.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintsScreen(navController: NavHostController) {
    val firestore = FirebaseFirestore.getInstance()
    var groupedReports by remember { mutableStateOf<Map<String, List<Report>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        firestore.collection("reports")
            .orderBy("reportedAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    isLoading = false
                    errorMessage = "Veriler yüklenirken hata oluştu: ${error.message}"
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val reportList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Report::class.java)?.copy(id = doc.id)
                    }
                    groupedReports = reportList.groupBy { it.jobPostId }
                    isLoading = false
                }
            }
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (groupedReports.isEmpty()) {
                Text(
                    text = "Hiç şikayet bulunmamaktadır.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedReports.forEach { (jobPostId, reports) ->
                        item {
                            ComplaintCard(reports = reports, firestore = firestore, context = context)
                        }
                    }
                }
            }

            // Hata mesajı Snackbar
            errorMessage?.let { message ->
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    action = {
                        TextButton(onClick = { errorMessage = null }) {
                            Text("Tamam")
                        }
                    }
                ) {
                    Text(message)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComplaintCard(reports: List<Report>, firestore: FirebaseFirestore, context: android.content.Context) {
    var jobPost by remember { mutableStateOf<JobPost?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showIgnoreDialog by remember { mutableStateOf(false) }

    val firstReport = reports.first()
    val groupedByUser = reports.groupBy { it.reporterUserId }

    LaunchedEffect(firstReport.jobPostId) {
        firestore.collection("jobPosts").document(firstReport.jobPostId).get()
            .addOnSuccessListener { document ->
                jobPost = document.toObject<JobPost>()
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "📌 Şikayet Sayısı: ${reports.size}",
                style = MaterialTheme.typography.titleMedium
            )

            groupedByUser.forEach { (userId, userReports) ->
                Text(text = "👤 Kullanıcı: $userId", style = MaterialTheme.typography.bodyMedium)
                Text(text = "📅 Tarih: ${formatTimestamp(userReports.first().reportedAt)}", style = MaterialTheme.typography.bodyMedium)
                
                // Şikayet nedenleri chip şeklinde gösteriliyor
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    userReports.forEach { report ->
                        ReportReasonItem(report.reason)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (isLoading) {
                CircularProgressIndicator()
            } else if (jobPost != null) {
                Text(text = "📝 İlan Başlığı: ${jobPost!!.title}", style = MaterialTheme.typography.titleMedium)
                Text(text = "🏢 Şirket: ${jobPost!!.company}", style = MaterialTheme.typography.bodyMedium)

                val maxChars = 100
                val description = jobPost!!.description
                val shortenedDescription = if (description.length > maxChars) {
                    "${description.take(maxChars)}..."
                } else {
                    description
                }
                Text(text = "📜 Açıklama: $shortenedDescription", style = MaterialTheme.typography.bodyMedium)
                Text(text = "📂 Kategori: ${jobPost!!.category}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "🟢 Aktif Mi?: ${if (jobPost!!.isActive) "Evet" else "Hayır"}", style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { showDeleteDialog = true },
                        enabled = !isProcessing,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Sil")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("İlanı Sil")
                        }
                    }

                    Button(
                        onClick = { showIgnoreDialog = true },
                        enabled = !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Şikayetleri Yoksay")
                        }
                    }
                }
            } else {
                Text(text = "⚠ İlgili ilan bulunamadı.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            onConfirm = {
                isProcessing = true
                deleteJobPost(firstReport.jobPostId, firestore, context) {
                    isProcessing = false
                    showDeleteDialog = false
                }
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showIgnoreDialog) {
        IgnoreConfirmationDialog(
            onConfirm = {
                isProcessing = true
                ignoreAllReports(firstReport.jobPostId, firestore, context) {
                    isProcessing = false
                    showIgnoreDialog = false
                }
            },
            onDismiss = { showIgnoreDialog = false }
        )
    }
}

@Composable
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("İlanı Sil") },
        text = { Text("Bu ilanı silmek istediğinizden emin misiniz?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Evet, Sil")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

@Composable
fun IgnoreConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Şikayetleri Yoksay") },
        text = { Text("Bu ilanla ilgili tüm şikayetleri yok saymak istediğinizden emin misiniz?") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Evet, Yoksay")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

@Composable
fun ReportReasonItem(reason: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = reason,
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

fun deleteJobPost(jobPostId: String, firestore: FirebaseFirestore, context: android.content.Context, onComplete: () -> Unit) {
    // Önce ilanı ve şikayetleri al
    firestore.collection("jobPosts").document(jobPostId).get()
        .addOnSuccessListener { jobDoc ->
            val jobPost = jobDoc.toObject<JobPost>()
            
            // Şikayet sayısını al
            firestore.collection("reports")
                .whereEqualTo("jobPostId", jobPostId)
                .get()
                .addOnSuccessListener { reportsSnapshot ->
                    val reportCount = reportsSnapshot.size()
                    val reportReasons = reportsSnapshot.documents.map { it.getString("reason") ?: "" }.distinct()
                    
                    // Batch işlemi başlat
                    val batch = firestore.batch()
                    
                    // İlanı sil
                    batch.delete(firestore.collection("jobPosts").document(jobPostId))
                    
                    // İlan sahibinin jobPostIds listesinden kaldır
                    jobPost?.userId?.let { userId ->
                        batch.update(
                            firestore.collection("users").document(userId),
                            "jobPostIds", FieldValue.arrayRemove(jobPostId)
                        )
                    }
                    
                    // Şikayetleri sil
                    reportsSnapshot.documents.forEach { doc ->
                        batch.delete(doc.reference)
                    }
                    
                    // Batch işlemini uygula
                    batch.commit()
                        .addOnSuccessListener {

                            Toast.makeText(context, "İlan başarıyla silindi", Toast.LENGTH_SHORT).show()
                            onComplete()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "İlan silinirken hata oluştu", Toast.LENGTH_SHORT).show()
                            onComplete()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Şikayetler alınırken hata oluştu", Toast.LENGTH_SHORT).show()
                    onComplete()
                }
        }
        .addOnFailureListener {
            Toast.makeText(context, "İlan bilgileri alınırken hata oluştu", Toast.LENGTH_SHORT).show()
            onComplete()
        }
}

fun ignoreAllReports(jobPostId: String, firestore: FirebaseFirestore, context: android.content.Context, onComplete: () -> Unit) {
    firestore.collection("reports")
        .whereEqualTo("jobPostId", jobPostId)
        .get()
        .addOnSuccessListener { snapshot ->
            val batch = firestore.batch()
            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit()
                .addOnSuccessListener {
                    Toast.makeText(context, "Şikayetler başarıyla yok sayıldı", Toast.LENGTH_SHORT).show()
                    onComplete()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Şikayetler yok sayılırken hata oluştu", Toast.LENGTH_SHORT).show()
                    onComplete()
                }
        }
        .addOnFailureListener {
            Toast.makeText(context, "Şikayetler alınırken hata oluştu", Toast.LENGTH_SHORT).show()
            onComplete()
        }
}

fun formatTimestamp(timestamp: Timestamp?): String {
    return timestamp?.let {
        val date = it.toDate()
        val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        sdf.format(date)
    } ?: "Bilinmiyor"
}

data class Report(
    val id: String = "",
    val jobPostId: String = "",
    val reason: String = "",
    val reportedAt: Timestamp? = null,
    val reporterUserId: String = ""
)
