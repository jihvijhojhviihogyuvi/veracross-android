package com.veracross.assignments

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val NAME = "vx_prefs"
    private fun sp(ctx: Context): SharedPreferences = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun saveCookie(ctx: Context, cookie: String) = sp(ctx).edit().putString("cookie", cookie).apply()
    fun getCookie(ctx: Context): String? = sp(ctx).getString("cookie", null)

    fun saveChecksPerDay(ctx: Context, n: Int) = sp(ctx).edit().putInt("checks", n).apply()
    fun getChecksPerDay(ctx: Context): Int = sp(ctx).getInt("checks", 4)

    fun saveKnownIds(ctx: Context, ids: Set<String>) = sp(ctx).edit().putStringSet("known_ids", ids).apply()
    fun getKnownIds(ctx: Context): MutableSet<String> = sp(ctx).getStringSet("known_ids", emptySet())?.toMutableSet() ?: mutableSetOf()

    fun saveLastItems(ctx: Context, json: String) = sp(ctx).edit().putString("last_items", json).apply()
    fun getLastItems(ctx: Context): String? = sp(ctx).getString("last_items", null)

    fun saveBackendUrl(ctx: Context, url: String) = sp(ctx).edit().putString("backend_url", url).apply()
    fun getBackendUrl(ctx: Context): String = sp(ctx).getString("backend_url", "http://localhost:3000") ?: "http://localhost:3000"
}
