package islam.athanalarm;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import net.sourceforge.jitl.astro.Location;

import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import java.util.function.Consumer;

import islam.athanalarm.handler.ScheduleData;
import islam.athanalarm.handler.ScheduleHandler;

public class PrayerTimeScheduler {

    public static void scheduleAlarms(Context context, Consumer<ScheduleData> callback) {
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
            callback.accept(null);
            return;
        }

        String latitude = settings.getString("latitude", null);
        String longitude = settings.getString("longitude", null);

        if (latitude != null && longitude != null) {
            String calculationMethodIndex = settings.getString("calculationMethodsIndex", null);
            if (calculationMethodIndex == null) {
                getCountryCode(context, Double.parseDouble(latitude), Double.parseDouble(longitude)).thenAccept(countryCode -> {
                    String newCalculationMethodIndex = getCalculationMethodIndex(countryCode);
                    settings.edit().putString("calculationMethodsIndex", newCalculationMethodIndex).apply();
                    calculateAndSchedule(context, settings, latitude, longitude, newCalculationMethodIndex, callback);
                });
            } else {
                calculateAndSchedule(context, settings, latitude, longitude, calculationMethodIndex, callback);
            }
        } else {
            callback.accept(null);
        }
    }

    private static void calculateAndSchedule(Context context, SharedPreferences settings, String latitude, String longitude, String calculationMethodIndex, Consumer<ScheduleData> callback) {
        String altitude = settings.getString("altitude", "0");
        String pressure = settings.getString("pressure", "1010");
        String temperature = settings.getString("temperature", "10");
        Location locationAstro = ScheduleHandler.getLocation(latitude, longitude, altitude, pressure, temperature);
        String roundingTypeIndex = settings.getString("roundingTypesIndex", String.valueOf(CONSTANT.DEFAULT_ROUNDING_TYPE));
        int offsetMinutes = 0;
        try {
            offsetMinutes = Integer.parseInt(settings.getString("offsetMinutes", "0"));
        } catch (NumberFormatException e) {
            // Ignore and use 0
        }
        ScheduleData newScheduleData = ScheduleHandler.calculate(locationAstro, calculationMethodIndex, roundingTypeIndex, offsetMinutes);
        ScheduleHandler.scheduleAlarms(context, newScheduleData);
        callback.accept(newScheduleData);
    }

    public static CompletableFuture<String> getCountryCode(Context context, double latitude, double longitude) {
        return CompletableFuture.supplyAsync(() -> {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = null;
            try {
                addresses = geocoder.getFromLocation(latitude, longitude, 1);
            } catch (IOException e) {
                Log.e("PrayerTimeScheduler", "Failed to get country code", e);
            }
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getCountryCode();
            }
            return null;
        });
    }

    public static String getCalculationMethodIndex(String countryCode) {
        if (countryCode != null) {
            for (int i = 0; i < CONSTANT.CALCULATION_METHOD_COUNTRY_CODES.length; i++) {
                for (String code : CONSTANT.CALCULATION_METHOD_COUNTRY_CODES[i]) {
                    if (code.equals(countryCode)) {
                        return String.valueOf(i);
                    }
                }
            }
        }
        return CONSTANT.DEFAULT_CALCULATION_METHOD;
    }
}
