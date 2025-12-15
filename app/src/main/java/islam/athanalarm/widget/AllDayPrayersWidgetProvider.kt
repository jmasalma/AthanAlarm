package islam.athanalarm.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import islam.athanalarm.MainActivity
import islam.athanalarm.R
import islam.athanalarm.handler.ScheduleHandler
import islam.athanalarm.repo.PrayerTimesRepository

class AllDayPrayersWidgetProvider : BaseWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.all_day_prayers_widget)

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.prayers_layout, pendingIntent)

        views.removeAllViews(R.id.prayers_layout)
        val repository = PrayerTimesRepository(context)
        val schedule = repository.getTodaysSchedule()

        if (schedule != null) {
            val prayerNames = context.resources.getStringArray(R.array.prayer_names)
            for (i in prayerNames.indices) {
                val prayerRow = RemoteViews(context.packageName, R.layout.prayer_row)
                val prayerName = prayerNames[i]
                val prayerTime = ScheduleHandler.getFormattedTime(schedule.schedule, schedule.extremes, i.toShort(), "0")
                prayerRow.setTextViewText(R.id.prayer_name, prayerName)
                prayerRow.setTextViewText(R.id.prayer_time, prayerTime)

                if (i.toShort() == schedule.nextTimeIndex) {
                    prayerRow.setInt(R.id.prayer_row_layout, "setBackgroundColor", context.getColor(R.color.colorAccent))
                }

                views.addView(R.id.prayers_layout, prayerRow)
            }
            scheduleNextUpdate(context, appWidgetId, schedule.schedule[schedule.nextTimeIndex.toInt()].timeInMillis, AllDayPrayersWidgetProvider::class.java)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
