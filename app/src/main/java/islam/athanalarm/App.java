package islam.athanalarm;

import android.app.Application;

public class App extends Application {

    private static App sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        NotificationHelper.createNotificationChannel(this);
    }
}
