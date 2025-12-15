package islam.athanalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TimeChangedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_TIME_CHANGED.equals(intent.getAction()) || Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
            PrayerTimeScheduler.scheduleAlarms(context, scheduleData -> {});
        }
    }
}
