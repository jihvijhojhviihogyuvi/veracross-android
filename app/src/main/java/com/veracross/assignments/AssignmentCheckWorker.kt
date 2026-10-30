package com.veracross.assignments

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.json.JSONArray
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AssignmentCheckWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val cookie = Prefs.getCookie(applicationContext) ?: return Result.success()
        Notifier.ensureChannels(applicationContext)
        val assignments = try { Scraper.fetchAssignments(cookie) } catch (e: Exception) { return Result.retry() }
        val known = Prefs.getKnownIds(applicationContext)
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        for (a in assignments) {
            val key = "${a.itemId}|${a.title}|${a.due}"
            if (key !in known) {
                // new item appeared
                Notifier.notifyNewItem(applicationContext, a)
                known.add(key)
            }
            // due tomorrow?
            if (a.due == tomorrow.format(fmt)) {
                val isTest = a.type.contains("Test", ignoreCase = true) || a.type.contains("Quiz", ignoreCase = true)
                if (isTest) Notifier.notifyTestDue(applicationContext, a)
                else Notifier.notifyHwDue(applicationContext, a)
            }
        }
        Prefs.saveKnownIds(applicationContext, known)
        // save last items
        val arr = JSONArray()
        for (a in assignments) {
            val o = org.json.JSONObject()
            o.put("itemId", a.itemId); o.put("title", a.title); o.put("course", a.course)
            o.put("type", a.type); o.put("due", a.due); o.put("status", a.status)
            arr.put(o)
        }
        Prefs.saveLastItems(applicationContext, arr.toString())
        return Result.success()
    }
}
