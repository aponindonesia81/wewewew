package com.tradeguard

import android.content.Context
import kotlin.math.max

object GuardPrefs {
    private const val P = "tradeguard"
    private const val LOCK_UNTIL = "lockUntil"
    private const val STREAK = "streak"
    private const val TOTAL = "total"
    private const val WINS = "wins"
    private const val LOSSES = "losses"
    private const val LIMIT = "limit"
    private const val MINUTES = "minutes"
    private const val ZERO_LOSS = "zeroLoss"
    private const val L = "roiL"; private const val T = "roiT"; private const val R = "roiR"; private const val B = "roiB"
    private const val FG = "foregroundPackage"

    private fun p(c: Context) = c.getSharedPreferences(P, Context.MODE_PRIVATE)
    fun lockUntil(c: Context) = p(c).getLong(LOCK_UNTIL, 0L)
    fun locked(c: Context) = lockUntil(c) > System.currentTimeMillis()
    fun setLock(c: Context, until: Long) = p(c).edit().putLong(LOCK_UNTIL, until).apply()
    fun streak(c: Context) = p(c).getInt(STREAK, 0)
    fun total(c: Context) = p(c).getInt(TOTAL, 0)
    fun wins(c: Context) = p(c).getInt(WINS, 0)
    fun losses(c: Context) = p(c).getInt(LOSSES, 0)
    fun limit(c: Context) = max(1, p(c).getInt(LIMIT, 4))
    fun minutes(c: Context) = max(1, p(c).getInt(MINUTES, 3))
    fun zeroIsLoss(c: Context) = p(c).getBoolean(ZERO_LOSS, true)
    fun saveSettings(c: Context, limit: Int, minutes: Int, zero: Boolean, l: Float, t: Float, r: Float, b: Float) = p(c).edit()
        .putInt(LIMIT, max(1, limit)).putInt(MINUTES, max(1, minutes)).putBoolean(ZERO_LOSS, zero)
        .putFloat(L, l.coerceIn(0f,100f)).putFloat(T, t.coerceIn(0f,100f)).putFloat(R, r.coerceIn(0f,100f)).putFloat(B, b.coerceIn(0f,100f)).apply()
    fun setForegroundPackage(c: Context, pkg:String) = p(c).edit().putString(FG,pkg).apply()
    fun foregroundPackage(c: Context) = p(c).getString(FG,"") ?: ""
    fun roi(c: Context): FloatArray = floatArrayOf(p(c).getFloat(L,45f),p(c).getFloat(T,35f),p(c).getFloat(R,100f),p(c).getFloat(B,75f))
    fun record(c: Context, profit: Boolean, loss: Boolean) {
        val e=p(c).edit().putInt(TOTAL,total(c)+1)
        if(profit) e.putInt(WINS,wins(c)+1).putInt(STREAK,0)
        if(loss) e.putInt(LOSSES,losses(c)+1).putInt(STREAK,streak(c)+1)
        e.apply()
    }
    fun reset(c: Context) = p(c).edit().clear().putInt(LIMIT,4).putInt(MINUTES,3).putBoolean(ZERO_LOSS,true).apply()
}
