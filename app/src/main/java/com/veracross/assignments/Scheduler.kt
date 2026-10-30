package com.veracross.assignments

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object Scheduler {
    private const val WORK_NAME = "vx_check"

    fun schedule(ctx: Context) {
        val checks = Prefs.getChecksPerDay(ctx).coerceAtLeast(1)
        val intervalHours = (24 / checks).coerceAtLeast(1).toLong()
        val req = PeriodicWorkRequestBuilder<AssignmentCheckWorker>(intervalHours, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    fun cancel(ctx: Context) {
        WorkManager.getInstance(ctx).cancelUniqueWork(WORK_NAME)
    }
}
