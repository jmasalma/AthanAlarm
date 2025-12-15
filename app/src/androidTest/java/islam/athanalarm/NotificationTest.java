package islam.athanalarm;

import android.app.NotificationManager;
import android.service.notification.StatusBarNotification;
import android.content.Context;
import android.content.Intent;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import islam.athanalarm.receiver.StartNotificationReceiver;
import islam.athanalarm.util.NotificationHelper;

@RunWith(AndroidJUnit4.class)
public class NotificationTest {

    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(android.Manifest.permission.POST_NOTIFICATIONS);

    private Context context;
    private NotificationManager notificationManager;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationHelper.createNotificationChannel(context);
    }

    @Test
    public void testNotificationIsPosted() throws InterruptedException {
        // Ensure the notification channel is created
        assertNotNull(notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_ID));

        // Create an intent to trigger the notification
        Intent intent = new Intent(context, StartNotificationReceiver.class);
        intent.setAction(CONSTANT.ACTION_NOTIFY_PRAYER_TIME);
        intent.putExtra("timeIndex", CONSTANT.FAJR);

        // Send the broadcast to trigger the notification
        context.sendBroadcast(intent);

        // Wait a moment for the notification to be posted
        Thread.sleep(2000);

        // Check if the notification is active
        boolean isNotificationVisible = false;
        for (StatusBarNotification sbn : notificationManager.getActiveNotifications()) {
            if (sbn.getId() == 1651) { // NOTIFICATION_ID
                isNotificationVisible = true;
                break;
            }
        }
        assertTrue("Notification was not posted", isNotificationVisible);
    }
}
