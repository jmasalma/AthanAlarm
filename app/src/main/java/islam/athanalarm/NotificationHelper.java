package islam.athanalarm;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;

/**
 * Helper class for creating and managing notifications.
 */
public class NotificationHelper {

    private static final String CHANNEL_ID = "prayer_time_channel";
    private static final String CHANNEL_NAME = "Prayer Time Notifications";
    private static final String CHANNEL_DESC = "Notifications for prayer times";

    /**
     * Creates a notification channel for prayer time notifications.
     *
     * @param context The context.
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(CHANNEL_DESC);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Shows a notification.
     *
     * @param context The context.
     * @param title The title of the notification.
     * @param message The message of the notification.
     * @param notificationId The ID of the notification.
     */
    @SuppressLint("MissingPermission")
    public static void showNotification(Context context, String title, String message, int notificationId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.icon)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        notificationManager.notify(notificationId, builder.build());
    }

    @SuppressLint("MissingPermission")
    public static void showCountdownNotification(Context context, String title, long prayerTimeMillis, int notificationId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.icon)
                .setContentTitle(title)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(prayerTimeMillis)
                .setTimeoutAfter(prayerTimeMillis - System.currentTimeMillis())
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        notificationManager.notify(notificationId, builder.build());
    }

    /**
     * Cancels a notification.
     *
     * @param context The context.
     * @param notificationId The ID of the notification to cancel.
     */
    public static void cancelNotification(Context context, int notificationId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);
    }
}
