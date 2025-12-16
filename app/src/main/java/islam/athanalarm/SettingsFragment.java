package islam.athanalarm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import islam.athanalarm.handler.SensorData;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    final Set<String> PREFS_TO_UPDATE_SUMMARY = new HashSet<>(Arrays.asList(
            "latitude",
            "longitude",
            "beforePrayerNotification",
            "altitude",
            "pressure",
            "calculationMethodsIndex"
    ));
    private SharedPreferences mEncryptedSharedPreferences;
    private MainViewModel mViewModel;
    private Observer<Location> mLocationObserver;
    private Observer<SensorData> mSensorReadingsObserver;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);

        mViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        try {
            MasterKey masterKey = new MasterKey.Builder(requireActivity(), MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            mEncryptedSharedPreferences = EncryptedSharedPreferences.create(
                    requireActivity(),
                    "secret_shared_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            Log.e("SettingsFragment", "Failed to create encrypted shared preferences", e);
            requireActivity().finish(); // Can't work without preferences
            return;
        }

        mLocationObserver = new Observer<Location>() {
            @Override
            public void onChanged(@Nullable Location currentLocation) {
                if (currentLocation == null) return;

                final String latitude = Double.toString(currentLocation.getLatitude());
                final String longitude = Double.toString(currentLocation.getLongitude());

                // Save to encrypted preferences
                SharedPreferences.Editor editor = mEncryptedSharedPreferences.edit();
                editor.putString("latitude", latitude);
                editor.putString("longitude", longitude);
                editor.apply();

                // Update the UI preferences
                EditTextPreference latitudePref = (EditTextPreference) findPreference("latitude");
                latitudePref.setText(latitude);
                updateSummary(latitudePref);

                EditTextPreference longitudePref = (EditTextPreference) findPreference("longitude");
                longitudePref.setText(longitude);
                updateSummary(longitudePref);

                syncEncryptedToUi();
                updateSummaries();
            }
        };
        mViewModel.getLocation().observeForever(mLocationObserver);

        mSensorReadingsObserver = new Observer<SensorData>() {
            @Override
            public void onChanged(@Nullable SensorData sensorReadings) {
                if (sensorReadings == null) return;

                final String altitude = Float.toString(sensorReadings.getAltitude());
                EditTextPreference altitudePref = (EditTextPreference) findPreference("altitude");
                if (altitudePref != null) {
                    altitudePref.setText(altitude);
                    updateSummary(altitudePref);
                }

                final String pressure = Float.toString(sensorReadings.getPressure());
                EditTextPreference pressurePref = (EditTextPreference) findPreference("pressure");
                if (pressurePref != null) {
                    pressurePref.setText(pressure);
                    updateSummary(pressurePref);
                }
            }
        };
        mViewModel.getSensorReadings().observeForever(mSensorReadingsObserver);

        findPreference("lookupGPS").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                mViewModel.updateLocation();
                mViewModel.updateSensorValues();
                return true;
            }
        });

        findPreference("information").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                return true;
            }
        });

        try {
            String versionName = requireActivity().getPackageManager().getPackageInfo(requireActivity().getPackageName(), 0).versionName;
            String summary = getText(R.string.information_text).toString().replace("#", versionName);
            summary += "\n" + BuildConfig.GIT_VERSION + " (" + BuildConfig.BUILD_DATE + ")";
            findPreference("information").setSummary(summary);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mViewModel != null) {
            if (mLocationObserver != null) {
                mViewModel.getLocation().removeObserver(mLocationObserver);
            }
            if (mSensorReadingsObserver != null) {
                mViewModel.getSensorReadings().removeObserver(mSensorReadingsObserver);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register listener on the UI (default) preferences
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        // Sync from encrypted to UI preferences
        syncEncryptedToUi();

        updateSummaries();
    }

    private void syncEncryptedToUi() {
        SharedPreferences uiPrefs = getPreferenceManager().getSharedPreferences();
        SharedPreferences.Editor uiEditor = uiPrefs.edit();
        for (String key : PREFS_TO_UPDATE_SUMMARY) {
            String value = mEncryptedSharedPreferences.getString(key, null);
            if (value != null) {
                uiEditor.putString(key, value);
            }
        }
        uiEditor.apply();
    }

    private void updateSummaries() {
        // Summaries are based on the UI preferences
        Map<String, ?> preferencesMap = getPreferenceManager().getSharedPreferences().getAll();
        for (Map.Entry<String, ?> preferenceEntry : preferencesMap.entrySet()) {
            if (PREFS_TO_UPDATE_SUMMARY.contains(preferenceEntry.getKey())) {
                Preference pref = findPreference(preferenceEntry.getKey());
                if (pref instanceof EditTextPreference) {
                    updateSummary((EditTextPreference) pref);
                }
            }
        }

        // Update calculation method summary
        updateCalculationMethodSummary();
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // This is called when UI (default) preferences change
        if (PREFS_TO_UPDATE_SUMMARY.contains(key)) {
            Preference pref = findPreference(key);
            if (pref instanceof EditTextPreference) {
                updateSummary((EditTextPreference) pref);
            } else if (pref instanceof ListPreference) {
                updateListSummary((ListPreference) pref);
            }
            // Sync the change to encrypted preferences
            SharedPreferences.Editor encryptedEditor = mEncryptedSharedPreferences.edit();
            encryptedEditor.putString(key, sharedPreferences.getString(key, ""));
            encryptedEditor.apply();

            if (key.equals("latitude") || key.equals("longitude")) {
                updateCalculationMethodSummary();
            }

        // Broadcast intent to update widget
        Intent intent = new Intent(getActivity(), PrayerTimeReceiver.class);
        intent.setAction(CONSTANT.ACTION_UPDATE_WIDGET);
        getActivity().sendBroadcast(intent);
        }
    }

    private void updateCalculationMethodSummary() {
        ListPreference calculationMethodPref = (ListPreference) findPreference("calculationMethodsIndex");
        if (calculationMethodPref.getValue() == null) {
            calculationMethodPref.setSummary("Detecting...");
            PrayerTimeScheduler.getCountryCode(getActivity(), Double.parseDouble(mEncryptedSharedPreferences.getString("latitude", "0")), Double.parseDouble(mEncryptedSharedPreferences.getString("longitude", "0"))).thenAccept(countryCode -> {
                String calculationMethodIndex = PrayerTimeScheduler.getCalculationMethodIndex(countryCode);
                requireActivity().runOnUiThread(() -> {
                    calculationMethodPref.setValue(calculationMethodIndex);
                    updateListSummary(calculationMethodPref);
                });
            });
        }
    }

    private void updateSummary(EditTextPreference preference) {
        if (preference != null) {
            preference.setSummary(preference.getText());
        }
    }

    private void updateListSummary(ListPreference preference) {
        if (preference != null) {
            preference.setSummary(preference.getEntry());
        }
    }
}
