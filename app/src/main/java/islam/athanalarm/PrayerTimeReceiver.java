package islam.athanalarm;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import islam.athanalarm.widget.AllDayPrayersWidgetProvider;
import islam.athanalarm.widget.NextPrayerWidgetProvider;

/**
 * BroadcastReceiver for handling prayer time notifications and widget updates.
 */
public class PrayerTimeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case CONSTANT.ACTION_UPDATE_PRAYER_TIMES:
                    PrayerTimeScheduler.scheduleAlarms(context, scheduleData -> {});
                    break;
                case CONSTANT.ACTION_UPDATE_WIDGET:
                    updateWidgets(context);
                    break;
                case CONSTANT.ACTION_LOCATION_UPDATED:
                    // Do nothing
                    break;
                default:
                    showPrayerTimeNotification(context, intent);
                    break;
            }
        } else {
            showPrayerTimeNotification(context, intent);
        }
    }

    /**
     * Shows a notification for the prayer time.
     * If the notification is a regular prayer time notification, it will dismiss the corresponding "before" notification.
     *
     * @param context The context.
     * @param intent The intent containing the prayer name and notification ID.
     */
    private void showPrayerTimeNotification(Context context, Intent intent) {
        String prayerName = intent.getStringExtra("prayer_name");
        int notificationId = intent.getIntExtra("notification_id", 1);

        if (notificationId < CONSTANT.NOTIFICATION_ID_OFFSET) {
            NotificationHelper.cancelNotification(context, notificationId + CONSTANT.NOTIFICATION_ID_OFFSET);
            int previousNotificationId = (notificationId == CONSTANT.FAJR) ? CONSTANT.ISHAA : notificationId - 1;
            NotificationHelper.cancelNotification(context, previousNotificationId);
            NotificationHelper.showNotification(context, "Prayer Time", "It's time for " + prayerName, notificationId);
        } else {
            long prayerTimeMillis = intent.getLongExtra("prayer_time_millis", 0);
            if (prayerTimeMillis > 0) {
                NotificationHelper.showCountdownNotification(context, prayerName, prayerTimeMillis, notificationId);
            }
        }
    }

    private void updateWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, AllDayPrayersWidgetProvider.class));
        if (appWidgetIds.length > 0) {
            new AllDayPrayersWidgetProvider().onUpdate(context, appWidgetManager, appWidgetIds);
        }
        appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, NextPrayerWidgetProvider.class));
        if (appWidgetIds.length > 0) {
            new NextPrayerWidgetProvider().onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }
}
