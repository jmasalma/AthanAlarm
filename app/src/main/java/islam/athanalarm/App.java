package islam.athanalarm;

import android.app.Application;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.MediaPlayer;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class App extends Application {

    private static App sInstance;

    private MediaPlayer mPlayer;

    public static void startMedia(int resid) {
        sInstance.mPlayer.stop();
        sInstance.mPlayer = MediaPlayer.create(sInstance, resid);
        sInstance.mPlayer.setScreenOnWhilePlaying(true);
        sInstance.mPlayer.start();
    }

    public static void stopMedia() {
        sInstance.mPlayer.stop();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        mPlayer = MediaPlayer.create(this, R.raw.bismillah);
        NotificationHelper.createNotificationChannel(this);
    }

    @Override
    public void onTerminate() {
        sInstance.mPlayer.stop();
        super.onTerminate();
    }
}
