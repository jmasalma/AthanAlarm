package islam.athanalarm.handler;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.format.DateFormat;

import androidx.security.crypto.EncryptedSharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Locale;
import androidx.security.crypto.MasterKey;

import net.sourceforge.jitl.Jitl;
import net.sourceforge.jitl.Method;
import net.sourceforge.jitl.Prayer;
import net.sourceforge.jitl.astro.Location;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import islam.athanalarm.CONSTANT;

public class ScheduleHandler {

    public static ScheduleData calculate(Location location, String calculationMethodIndex, String roundingTypeIndex, int offsetMinutes) {
        Method method = CONSTANT.CALCULATION_METHODS[Integer.parseInt(calculationMethodIndex)].copy();
        method.setRound(CONSTANT.ROUNDING_TYPES[Integer.parseInt(roundingTypeIndex)]);

        GregorianCalendar day = new GregorianCalendar();
        Jitl itl = new Jitl(location, method);
        Prayer[] dayPrayers = itl.getPrayerTimes(day).getPrayers();
        Prayer[] allTimes = new Prayer[]{dayPrayers[0], dayPrayers[1], dayPrayers[2], dayPrayers[3], dayPrayers[4], dayPrayers[5], itl.getNextDayFajr(day)};

        GregorianCalendar[] schedule = new GregorianCalendar[7];
        boolean[] extremes = new boolean[7];
        for (short i = CONSTANT.FAJR; i <= CONSTANT.NEXT_FAJR; i++) {
            schedule[i] = new GregorianCalendar(day.get(Calendar.YEAR), day.get(Calendar.MONTH), day.get(Calendar.DAY_OF_MONTH), allTimes[i].getHour(), allTimes[i].getMinute(), allTimes[i].getSecond());
            schedule[i].add(Calendar.MINUTE, offsetMinutes);
            extremes[i] = allTimes[i].isExtreme();
        }
        schedule[CONSTANT.NEXT_FAJR].add(Calendar.DAY_OF_MONTH, 1); // Next fajr is tomorrow

        return new ScheduleData(schedule, extremes, getNextTimeIndex(schedule));
    }

    public static String getFormattedTime(GregorianCalendar[] schedule, boolean[] extremes, short i, String timeFormatIndex) {
        boolean isAMPM = Integer.parseInt(timeFormatIndex) == CONSTANT.DEFAULT_TIME_FORMAT;
        if (schedule[i] == null) {
            return "";
        }
        Date time = schedule[i].getTime();
        if (time == null) {
            return "";
        }
        String pattern = isAMPM ? "h:mm a" : "HH:mm";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
        String formattedTime = sdf.format(time);
        if (extremes[i]) {
            formattedTime += " *";
        }
        return formattedTime;
    }

    public static short getNextTimeIndex(GregorianCalendar[] schedule) {
        return getNextTimeIndex(schedule, new GregorianCalendar());
    }

    public static short getNextTimeIndex(GregorianCalendar[] schedule, Calendar now) {
        if (now.before(schedule[CONSTANT.FAJR])) return CONSTANT.FAJR;
        for (short i = CONSTANT.FAJR; i < CONSTANT.NEXT_FAJR; i++) {
            if (now.after(schedule[i]) && now.before(schedule[i + 1])) {
                return ++i;
            }
        }
        return CONSTANT.NEXT_FAJR;
    }

    public static Location getLocation(String latitude, String longitude, String altitude, String pressure, String temperature) {
        Location location = new Location(
                Float.parseFloat(latitude),
                Float.parseFloat(longitude),
                getGMTOffset(),
                0
        );
        location.setSeaLevel(Float.parseFloat(altitude) < 0 ? 0 : Float.parseFloat(altitude));
        location.setPressure(Float.parseFloat(pressure));
        location.setTemperature(Float.parseFloat(temperature));
        return location;
    }

    private static double getGMTOffset() {
        Calendar now = new GregorianCalendar();
        int gmtOffset = now.getTimeZone().getOffset(now.getTimeInMillis());
        return gmtOffset / 3600000.0;
    }

    public static void scheduleAlarms(Context context, ScheduleData scheduleData) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        SharedPreferences settings;
        try {
            MasterKey masterKey = new MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            settings = EncryptedSharedPreferences.create(
                    context,
                    "secret_shared_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }


        String[] prayerNames = context.getResources().getStringArray(islam.athanalarm.R.array.prayer_names);

        for (int i = 0; i < scheduleData.schedule.length; i++) {
            if (i == CONSTANT.SUNRISE) continue; // Don't notify for sunrise

            GregorianCalendar prayerTime = scheduleData.schedule[i];

            if (prayerTime.getTimeInMillis() < System.currentTimeMillis()) {
                continue;
            }

            // Schedule prayer time notification
            Intent intent = new Intent(context, islam.athanalarm.PrayerTimeReceiver.class);
            intent.putExtra("prayer_name", prayerNames[i]);
            intent.putExtra("notification_id", i);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, i, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, prayerTime.getTimeInMillis(), pendingIntent);

            // Schedule before prayer notification
            int beforePrayerNotificationTime = 0;
            try {
                beforePrayerNotificationTime = Integer.parseInt(settings.getString("beforePrayerNotification", "0"));
            } catch (NumberFormatException e) {
                // Ignore and use 0
            }

            if (beforePrayerNotificationTime > 0) {
                GregorianCalendar beforePrayerTime = (GregorianCalendar) prayerTime.clone();
                beforePrayerTime.add(Calendar.MINUTE, -beforePrayerNotificationTime);
                Intent beforeIntent = new Intent(context, islam.athanalarm.PrayerTimeReceiver.class);
                beforeIntent.putExtra("prayer_name", prayerNames[i] + " (in " + beforePrayerNotificationTime + " minutes)");
                beforeIntent.putExtra("notification_id", i + CONSTANT.NOTIFICATION_ID_OFFSET);
                beforeIntent.putExtra("prayer_time_millis", prayerTime.getTimeInMillis());
                PendingIntent beforePendingIntent = PendingIntent.getBroadcast(context, i + CONSTANT.REQUEST_CODE_OFFSET, beforeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, beforePrayerTime.getTimeInMillis(), beforePendingIntent);
            }
        }
    }
}
