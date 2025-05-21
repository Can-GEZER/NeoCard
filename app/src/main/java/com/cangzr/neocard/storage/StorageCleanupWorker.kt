package com.cangzr.neocard.storage

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cangzr.neocard.storage.FirebaseStorageManager

/**
 * Kullanılmayan resimleri periyodik olarak temizleyen WorkManager worker sınıfı.
 * Bu worker, 7 günden eski ve hiçbir kartvizitte veya iş ilanında kullanılmayan resimleri siler.
 */
class StorageCleanupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val storageManager = FirebaseStorageManager.getInstance()
            storageManager.cleanUnusedImages()
            Result.success()
        } catch (e: Exception) {
            // Hata durumunda tekrar dene
            Result.retry()
        }
    }
} 