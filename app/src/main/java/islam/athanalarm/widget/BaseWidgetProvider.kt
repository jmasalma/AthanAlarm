package islam.athanalarm.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

abstract class BaseWidgetProvider : AppWidgetProvider() {

    protected fun scheduleNextUpdate(context: Context, appWidgetId: Int, updateTime: Long, javaClass: Class<*>) {
        val intent = Intent(context, javaClass)
        intent.action = "android.appwidget.action.APPWIDGET_UPDATE"
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
        val pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.RTC, updateTime, pendingIntent)
    }
}
