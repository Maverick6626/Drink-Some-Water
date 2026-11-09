package com.example.drinksomewater

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

class WaterWidgetWideProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            WaterWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetId, isWide = true)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == WaterWidgetProvider.ACTION_ADD_WATER) {
            val repo = WaterRepository(context)
            repo.addWater(repo.getWidgetAmount())
            WaterWidgetProvider.updateAllWidgets(context)
        }
    }
}
