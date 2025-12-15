package islam.athanalarm.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.RemoteViews
import islam.athanalarm.MainActivity
import islam.athanalarm.R
import islam.athanalarm.handler.ScheduleHandler
import islam.athanalarm.repo.PrayerTimesRepository
import java.util.Calendar

/**
 * App widget provider for the "Next Prayer" widget.
 */
class NextPrayerWidgetProvider : BaseWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    /**
     * Updates the widget with the next prayer time and a countdown.
     *
     * @param context The context.
     * @param appWidgetManager The app widget manager.
     * @param appWidgetId The app widget ID.
     */
    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.next_prayer_widget)

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.next_prayer_widget_layout, pendingIntent)

        val repository = PrayerTimesRepository(context)
        val schedule = repository.getTodaysSchedule()

        if (schedule != null) {
            val nextPrayer = schedule.nextTimeIndex
            val prayerName = getPrayerName(context, nextPrayer)
            val prayerTime = ScheduleHandler.getFormattedTime(schedule.schedule, schedule.extremes, nextPrayer, "0")
            val prayerTimeMillis = schedule.schedule[nextPrayer.toInt()].timeInMillis
            val now = System.currentTimeMillis()
            val diff = prayerTimeMillis - now

            views.setTextViewText(R.id.prayer_name, prayerName)
            views.setTextViewText(R.id.prayer_time, prayerTime)
            views.setChronometer(R.id.countdown, SystemClock.elapsedRealtime() + diff, null, true)

            scheduleNextUpdate(context, appWidgetId, prayerTimeMillis, NextPrayerWidgetProvider::class.java)
        } else {
            views.setTextViewText(R.id.prayer_name, "Error")
            views.setTextViewText(R.id.prayer_time, " ")
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun getPrayerName(context: Context, prayerIndex: Short): String {
        val prayerNames = context.resources.getStringArray(R.array.prayer_names)
        return prayerNames[prayerIndex.toInt()]
    }
}
