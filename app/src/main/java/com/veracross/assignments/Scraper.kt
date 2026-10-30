package com.veracross.assignments

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class Assignment(
    val itemId: Long,
    val title: String,
    val course: String,
    val teacher: String?,
    val type: String,
    val status: String,
    val due: String?,
    val color: String?
)

object Scraper {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    const val SCHOOL = "maimonides"
    const val PERSON_ID = "100412"
    val PLANNER_URL = "https://portals-embed.veracross.com/$SCHOOL/student/planner?p=$PERSON_ID&school_year=2026"

    fun fetchAssignments(cookie: String): List<Assignment> {
        val req = Request.Builder()
            .url(PLANNER_URL)
            .header("Cookie", cookie)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code}")
            val html = resp.body?.string() ?: ""
            return parse(html)
        }
    }

    fun parse(html: String): List<Assignment> {
        val marker = "AssignmentPlanner.App("
        val idx = html.indexOf(marker)
        if (idx < 0) return emptyList()
        val braceIdx = html.indexOf('{', idx)
        if (braceIdx < 0) return emptyList()
        val objStr = extractBalanced(html, braceIdx) ?: return emptyList()
        val quoted = objStr.replace(Regex("""([{,]\s*)([A-Za-z_]\w*)\s*:"""), "$1\"$2\":")
            .replace(Regex(""",\s*([}\]])"""), "$1")
        val data = try { JSONObject(quoted) } catch (e: Exception) { return emptyList() }
        val rowsArr = data.optJSONArray("rows") ?: JSONArray()
        val rows = mutableMapOf<Long, JSONObject>()
        for (i in 0 until rowsArr.length()) {
            val r = rowsArr.getJSONObject(i)
            val id = r.optLong("id", r.optLong("class_pk", -1))
            if (id >= 0) rows[id] = r
        }
        val itemsArr = data.optJSONArray("items") ?: JSONArray()
        val seen = mutableSetOf<String>()
        val result = mutableListOf<Assignment>()
        for (i in 0 until itemsArr.length()) {
            val it = itemsArr.getJSONObject(i)
            val rowId = it.optLong("row", -1)
            val row = rows[rowId]
            val course = row?.optString("description") ?: ""
            val teacher = row?.optString("teacher_full_name")
            val title = it.optString("notes", "")
            val type = it.optString("description", "")
            val status = it.optString("type", "")
            val due = it.optString("formatted_date").takeIf { it.isNotBlank() }
                ?: it.optString("date").takeIf { it.isNotBlank() }
            // normalize due to yyyy-MM-dd if it's a date string
            val dueNorm = normalizeDue(due)
            val key = "$title|$dueNorm|$course"
            if (key in seen) continue
            seen.add(key)
            result.add(Assignment(
                itemId = it.optLong("item_id", -1),
                title = title,
                course = course,
                teacher = teacher,
                type = type,
                status = status,
                due = dueNorm,
                color = it.optString("color")
            ))
        }
        return result.sortedBy { it.due ?: "9999" }
    }

    private fun extractBalanced(s: String, start: Int): String? {
        var depth = 0
        var i = start
        var inStr = false
        var escape = false
        while (i < s.length) {
            val ch = s[i]
            if (inStr) {
                if (escape) escape = false
                else if (ch == '\\') escape = true
                else if (ch == '"') inStr = false
            } else {
                if (ch == '"') inStr = true
                else if (ch == '{') depth++
                else if (ch == '}') {
                    depth--
                    if (depth == 0) return s.substring(start, i + 1)
                }
            }
            i++
        }
        return null
    }

    private fun normalizeDue(v: String?): String? {
        if (v.isNullOrBlank()) return null
        // ISO
        Regex("""(\d{4})-(\d{2})-(\d{2})""").find(v)?.let { return it.value }
        // "Oct 27 2026" or "Oct 27"
        val months = mapOf("Jan" to "01", "Feb" to "02", "Mar" to "03", "Apr" to "04", "May" to "05", "Jun" to "06", "Jul" to "07", "Aug" to "08", "Sep" to "09", "Oct" to "10", "Nov" to "11", "Dec" to "12")
        val m = Regex("""([A-Z][a-z]{2})\s+(\d{1,2})(?:\s+(\d{4}))?""").find(v)
        if (m != null) {
            val mon = months[m.groupValues[1]] ?: return v
            val day = m.groupValues[2].padStart(2, '0')
            val year = m.groupValues[3].ifBlank { java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString() }
            return "$year-$mon-$day"
        }
        return v
    }
}
