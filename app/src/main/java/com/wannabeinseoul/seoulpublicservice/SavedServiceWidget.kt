package com.wannabeinseoul.seoulpublicservice

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.wannabeinseoul.seoulpublicservice.ui.SplashActivity

/**
 * Implementation of App Widget functionality.
 */
class SavedServiceWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {


        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val pendingIntent: PendingIntent = Intent(context, SplashActivity::class.java)
        .let { intent ->
            PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

    val view: RemoteViews = RemoteViews(context.packageName, R.layout.saved_service_widget)
        .apply {
            setOnClickPendingIntent(R.id.btn_widget, pendingIntent)
//            setImageViewResource(R.id.iv_widget, )
            setTextViewText(R.id.tv_widget, "what is it???????????????????????????????")
        }

    appWidgetManager.updateAppWidget(appWidgetId, view)
}