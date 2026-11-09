package com.example.drinksomewater

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** One drink logged today. */
data class LogEntry(val ml: Int, val time: String)

/** One day's total, used by the weekly chart. */
data class DayTotal(
    val dateKey: String,
    val millis: Long,
    val label: String,
    val ml: Int,
    val isToday: Boolean,
    val isFuture: Boolean
)

class WaterRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("water_prefs", Context.MODE_PRIVATE)

    companion object {
        const val MAX_WEEKS_BACK = 26          // ~6 months
        private const val KEEP_DAYS = 26 * 7 + 7
        private const val DAY_PREFIX = "day_"
        private const val LOG_PREFIX = "log_"
        private const val KEEP_LOG_DAYS = 14
        private const val KEY_GOAL = "water_goal"
        private const val KEY_WIDGET_AMOUNT = "widget_amount"
        private const val KEY_WIDGET_DISPLAY = "widget_display" // 0 percentage, 1 amount (ml)
        private const val KEY_THEME_MODE = "theme_mode"   // 0 system, 1 light, 2 dark
        private const val KEY_WIDGET_THEME = "widget_theme" // 0 light, 1 dark
    }

    init {
        migrateLegacy()
        prune()
    }

    // ---------- settings ----------
    fun getGoal(): Int = prefs.getInt(KEY_GOAL, 2000)
    fun setGoal(goal: Int) = prefs.edit().putInt(KEY_GOAL, goal).apply()

    fun getWidgetAmount(): Int = prefs.getInt(KEY_WIDGET_AMOUNT, 250)
    fun setWidgetAmount(amount: Int) = prefs.edit().putInt(KEY_WIDGET_AMOUNT, amount).apply()

    fun getWidgetDisplay(): Int = prefs.getInt(KEY_WIDGET_DISPLAY, 0)
    fun setWidgetDisplay(mode: Int) = prefs.edit().putInt(KEY_WIDGET_DISPLAY, mode).apply()

    fun getThemeMode(): Int = prefs.getInt(KEY_THEME_MODE, 0)
    fun setThemeMode(mode: Int) = prefs.edit().putInt(KEY_THEME_MODE, mode).apply()
    
    fun getWidgetTheme(): Int = prefs.getInt(KEY_WIDGET_THEME, 0)
    fun setWidgetTheme(mode: Int) = prefs.edit().putInt(KEY_WIDGET_THEME, mode).apply()

    // ---------- today ----------
    fun getConsumed(): Int = prefs.getInt(DAY_PREFIX + keyFor(0), 0)

    fun addWater(amount: Int) {
        val today = keyFor(0)
        val total = prefs.getInt(DAY_PREFIX + today, 0) + amount
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Calendar.getInstance().time)
        val log = prefs.getString(LOG_PREFIX + today, "").orEmpty()
        val entry = "$amount,$time"
        prefs.edit()
            .putInt(DAY_PREFIX + today, total)
            .putString(LOG_PREFIX + today, if (log.isEmpty()) entry else "$log;$entry")
            .apply()
    }

    /** Today's drinks, oldest first. */
    fun getTodayLog(): List<LogEntry> {
        val raw = prefs.getString(LOG_PREFIX + keyFor(0), "").orEmpty()
        if (raw.isEmpty()) return emptyList()
        return raw.split(";").mapNotNull {
            val parts = it.split(",")
            val ml = parts.getOrNull(0)?.toIntOrNull()
            if (ml == null) null else LogEntry(ml, parts.getOrNull(1).orEmpty())
        }
    }

    /** Removes one logged drink and takes it off today's total. */
    fun removeLogEntry(index: Int) {
        val today = keyFor(0)
        val log = getTodayLog().toMutableList()
        if (index !in log.indices) return
        val removed = log.removeAt(index)
        val total = (getConsumed() - removed.ml).coerceAtLeast(0)
        prefs.edit()
            .putInt(DAY_PREFIX + today, total)
            .putString(LOG_PREFIX + today, log.joinToString(";") { "${it.ml},${it.time}" })
            .apply()
    }

    fun resetConsumed() {
        val today = keyFor(0)
        prefs.edit().remove(DAY_PREFIX + today).remove(LOG_PREFIX + today).apply()
    }

    /** Days in a row (ending today, or yesterday if today isn't done yet) that hit the goal. */
    fun getStreak(): Int {
        val goal = getGoal()
        if (goal <= 0) return 0
        var offset = if (getConsumed() >= goal) 0 else -1
        var streak = 0
        while (offset >= -KEEP_DAYS) {
            if (prefs.getInt(DAY_PREFIX + keyFor(offset), 0) >= goal) {
                streak++
                offset--
            } else break
        }
        return streak
    }

    // ---------- history ----------
    /** weekOffset 0 = this week (Mon–Sun), -1 = last week, ... */
    fun getWeek(weekOffset: Int): List<DayTotal> {
        val todayKey = keyFor(0)
        val start = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            val sinceMonday = (get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
            add(Calendar.DAY_OF_YEAR, -sinceMonday + weekOffset * 7)
        }
        val labelFmt = SimpleDateFormat("EEE", Locale.getDefault())
        return (0..6).map { i ->
            val c = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, i) }
            val key = dateFmt().format(c.time)
            DayTotal(
                dateKey = key,
                millis = c.timeInMillis,
                label = labelFmt.format(c.time),
                ml = prefs.getInt(DAY_PREFIX + key, 0),
                isToday = key == todayKey,
                isFuture = key > todayKey
            )
        }
    }

    // ---------- helpers ----------
    private fun dateFmt() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun keyFor(offsetDays: Int): String {
        val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offsetDays) }
        return dateFmt().format(c.time)
    }

    /** Drops anything older than ~6 months. */
    private fun prune() {
        val cutoff = DAY_PREFIX + keyFor(-KEEP_DAYS)
        val logCutoff = LOG_PREFIX + keyFor(-KEEP_LOG_DAYS)
        val old = prefs.all.keys.filter {
            (it.startsWith(DAY_PREFIX) && it < cutoff) ||
                (it.startsWith(LOG_PREFIX) && it < logCutoff)
        }
        if (old.isNotEmpty()) {
            prefs.edit().apply { old.forEach { remove(it) } }.apply()
        }
    }

    /** Moves the old single "consumed" value into the new per-day storage. */
    private fun migrateLegacy() {
        val lastDate = prefs.getString("last_date", null) ?: return
        val old = prefs.getInt("water_consumed", 0)
        val e = prefs.edit()
        if (old > 0 && !prefs.contains(DAY_PREFIX + lastDate)) e.putInt(DAY_PREFIX + lastDate, old)
        e.remove("last_date").remove("water_consumed").apply()
    }
}
