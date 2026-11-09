package com.example.drinksomewater

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class WaterWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_ADD_WATER) {
            val repo = WaterRepository(context)
            repo.addWater(repo.getWidgetAmount())
            updateAllWidgets(context)
        }
    }

    companion object {
        const val ACTION_ADD_WATER = "com.example.drinksomewater.ADD_WATER"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            isWide: Boolean = false
        ) {
            val repo = WaterRepository(context)
            val consumed = repo.getConsumed()
            val goal = repo.getGoal()
            val isDark = repo.getWidgetTheme() == 1

            val layoutId = if (isWide) {
                if (isDark) R.layout.water_widget_wide_dark else R.layout.water_widget_wide_light
            } else {
                if (isDark) R.layout.water_widget_dark else R.layout.water_widget_light
            }

            val views = RemoteViews(context.packageName, layoutId)

            if (isWide) {
                val emoteFile = java.io.File(context.filesDir, "emote_image")
                if (emoteFile.exists()) {
                    val bitmap = android.graphics.BitmapFactory.decodeFile(emoteFile.absolutePath)
                    if (bitmap != null) {
                        views.setImageViewBitmap(R.id.widget_emote, bitmap)
                    }
                }
            }

            val percent = if (goal > 0) (consumed.toFloat() / goal * 100).toInt() else 0
            views.setProgressBar(R.id.widget_progress_bar, 100, percent.coerceIn(0, 100), false)
            val mainText = if (repo.getWidgetDisplay() == 1) {
                String.format(java.util.Locale.getDefault(), "%,d ml", consumed)
            } else {
                "$percent%"
            }
            views.setTextViewText(R.id.widget_text_main, mainText)

            // "+" button adds water
            val addIntent = Intent(context, WaterWidgetProvider::class.java).apply {
                action = ACTION_ADD_WATER
            }
            val addPending = PendingIntent.getBroadcast(
                context, 0, addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_add, addPending)

            // Tapping anywhere else opens the app
            val openPending = PendingIntent.getActivity(
                context, 1, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openPending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WaterWidgetProvider::class.java))
            for (id in ids) updateAppWidget(context, manager, id, false)
            
            val idsWide = manager.getAppWidgetIds(ComponentName(context, WaterWidgetWideProvider::class.java))
            for (id in idsWide) updateAppWidget(context, manager, id, true)
        }
    }
}
