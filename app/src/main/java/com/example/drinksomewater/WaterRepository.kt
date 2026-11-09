package com.example.drinksomewater

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
        private const val KEY_GOAL = "water_goal"
        private const val KEY_WIDGET_AMOUNT = "widget_amount"
        private const val KEY_THEME_MODE = "theme_mode"   // 0 system, 1 light, 2 dark
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

    fun getThemeMode(): Int = prefs.getInt(KEY_THEME_MODE, 0)
    fun setThemeMode(mode: Int) = prefs.edit().putInt(KEY_THEME_MODE, mode).apply()

    // ---------- today ----------
    fun getConsumed(): Int = prefs.getInt(DAY_PREFIX + keyFor(0), 0)

    fun addWater(amount: Int) {
        val key = DAY_PREFIX + keyFor(0)
        prefs.edit().putInt(key, prefs.getInt(key, 0) + amount).apply()
    }

    fun resetConsumed() {
        prefs.edit().remove(DAY_PREFIX + keyFor(0)).apply()
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
        val old = prefs.all.keys.filter { it.startsWith(DAY_PREFIX) && it < cutoff }
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
