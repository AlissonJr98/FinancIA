package com.financia.app.core.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.financia.app.MainActivity
import com.financia.app.R
import java.text.NumberFormat
import java.util.Locale

class FinancIAWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        private const val PREFS_WIDGET = "financia_widget_prefs"
        private const val KEY_BALANCE_TEXT = "latest_balance_text"
        private const val KEY_SUBTITLE_TEXT = "latest_subtitle_text"

        fun saveAndRefreshWidget(
            context: Context,
            balance: Double,
            income: Double,
            expense: Double
        ) {
            val brl = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
            val balanceText = brl.format(balance)
            val subText = "Rec: ${brl.format(income)} • Desp: ${brl.format(expense)}"

            context.getSharedPreferences(PREFS_WIDGET, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_BALANCE_TEXT, balanceText)
                .putString(KEY_SUBTITLE_TEXT, subText)
                .apply()

            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, FinancIAWidgetProvider::class.java))
            for (id in ids) {
                updateAppWidget(context, manager, id)
            }
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = context.getSharedPreferences(PREFS_WIDGET, Context.MODE_PRIVATE)
            val balanceText = prefs.getString(KEY_BALANCE_TEXT, "R$ 0,00") ?: "R$ 0,00"
            val subtitleText = prefs.getString(KEY_SUBTITLE_TEXT, "Toque para lançar gasto com IA")
                ?: "Toque para lançar gasto com IA"

            val views = RemoteViews(context.packageName, R.layout.financia_widget)
            views.setTextViewText(R.id.widget_balance, balanceText)
            views.setTextViewText(R.id.widget_subtitle, subtitleText)

            val openSmartAddIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_tab", "SMART_ADD")
            }
            val pendingSmartAdd = PendingIntent.getActivity(
                context,
                201,
                openSmartAddIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(R.id.widget_root, pendingSmartAdd)
            views.setOnClickPendingIntent(R.id.widget_action_btn, pendingSmartAdd)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
