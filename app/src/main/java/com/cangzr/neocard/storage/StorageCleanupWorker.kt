package com.cangzr.neocard.storage

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cangzr.neocard.storage.FirebaseStorageManager

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
            Result.retry()
        }
    }
} 
