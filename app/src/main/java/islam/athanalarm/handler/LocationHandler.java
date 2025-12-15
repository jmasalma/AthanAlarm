package islam.athanalarm.handler;

import android.annotation.SuppressLint;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class LocationHandler {

    private final LocationManager mLocationManager;
    private final MutableLiveData<Location> mLocation = new MutableLiveData<>();

    public LocationHandler(LocationManager locationManager) {
        mLocationManager = locationManager;
    }

    public LiveData<Location> getLocation() {
        return mLocation;
    }

    @SuppressLint("MissingPermission")
    public void update() {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setCostAllowed(true);

        Location currentLocation = null;
        try {
            currentLocation = mLocationManager.getLastKnownLocation(mLocationManager.getBestProvider(criteria, true));

            if (currentLocation == null) {
                criteria.setAccuracy(Criteria.ACCURACY_COARSE);
                currentLocation = mLocationManager.getLastKnownLocation(mLocationManager.getBestProvider(criteria, true));
            }

            if (currentLocation != null) {
                mLocation.postValue(currentLocation);
            }
        } catch (Exception ex) {
            // GPS and wireless networks might be disabled
        }
    }
}