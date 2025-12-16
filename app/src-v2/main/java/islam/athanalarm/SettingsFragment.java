package islam.athanalarm;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import islam.athanalarm.handler.SensorData;

public class SettingsFragment extends PreferenceFragmentCompat {
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mEncryptedSharedPreferences = mViewModel.getSettings();

        mViewModel.getLocation().observe(getViewLifecycleOwner(), new Observer<Location>() {
            @Override
            public void onChanged(@Nullable Location currentLocation) {
                if (currentLocation == null || !isAdded() || getActivity() == null) return;

                final String latitude = Double.toString(currentLocation.getLatitude());
                final String longitude = Double.toString(currentLocation.getLongitude());

                // Update the UI preferences
                EditTextPreference latitudePref = findPreference("latitude");
                if (latitudePref != null) {
                    latitudePref.setText(latitude);
                    updateSummary(latitudePref);
                }

                EditTextPreference longitudePref = findPreference("longitude");
                if (longitudePref != null) {
                    longitudePref.setText(longitude);
                    updateSummary(longitudePref);
                }
            }
        });

        mViewModel.getSensorReadings().observe(getViewLifecycleOwner(), new Observer<SensorData>() {
            @Override
            public void onChanged(@Nullable SensorData sensorReadings) {
                if (sensorReadings == null || !isAdded() || getActivity() == null) return;

                final String altitude = Float.toString(sensorReadings.getAltitude());
                EditTextPreference altitudePref = findPreference("altitude");
                if (altitudePref != null) {
                    altitudePref.setText(altitude);
                    updateSummary(altitudePref);
                }

                final String pressure = Float.toString(sensorReadings.getPressure());
                EditTextPreference pressurePref = findPreference("pressure");
                if (pressurePref != null) {
                    pressurePref.setText(pressure);
                    updateSummary(pressurePref);
                }
            }
        });

        mViewModel.getCalculationMethodIndex().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String calculationMethodIndex) {
                if (!isAdded() || getActivity() == null) return;
                ListPreference calculationMethodPref = findPreference("calculationMethodsIndex");
                if (calculationMethodPref != null) {
                    calculationMethodPref.setValue(calculationMethodIndex);
                    updateListSummary(calculationMethodPref);
                }
            }
        });

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

        updateCalculationMethodSummary();
    }

    private void updateCalculationMethodSummary() {
        ListPreference calculationMethodPref = findPreference("calculationMethodsIndex");
        if (calculationMethodPref != null && calculationMethodPref.getValue() == null) {
            calculationMethodPref.setSummary("Detecting...");
            mViewModel.updateCalculationMethod();
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
