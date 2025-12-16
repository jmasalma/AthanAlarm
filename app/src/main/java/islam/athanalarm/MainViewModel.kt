package islam.athanalarm

import android.app.Application
import android.content.Context
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import islam.athanalarm.handler.CompassHandler
import islam.athanalarm.handler.LocationHandler
import islam.athanalarm.handler.SensorHandler
import islam.athanalarm.handler.SensorData
import islam.athanalarm.handler.ScheduleData
import islam.athanalarm.handler.ScheduleHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.sourceforge.jitl.Jitl
import net.sourceforge.jitl.astro.Direction
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * ViewModel for the main screen, responsible for managing location, prayer times, and qibla direction.
 *
 * @param application The application context.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val KEY_LATITUDE = "latitude"
        private const val KEY_LONGITUDE = "longitude"
        private const val KEY_ALTITUDE = "altitude"
        private const val KEY_PRESSURE = "pressure"
    }

    private val compassHandler: CompassHandler
    private val locationHandler: LocationHandler
    private val sensorHandler: SensorHandler
    private val masterKey = MasterKey.Builder(application, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    val settings = EncryptedSharedPreferences.create(
        application,
        "secret_shared_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _scheduleData = MediatorLiveData<ScheduleData>()
    /**
     * LiveData holding the prayer time schedule.
     */
    val scheduleData: LiveData<ScheduleData> = _scheduleData

    private val _qiblaDirection = MediatorLiveData<Double>()
    /**
     * LiveData holding the qibla direction in degrees from North.
     */
    val qiblaDirection: LiveData<Double> = _qiblaDirection

    /**
     * LiveData holding the direction of North in degrees.
     */
    val northDirection: LiveData<Float>
    private val _location = MediatorLiveData<Location>()
    /**
     * LiveData holding the current location.
     */
    val location: LiveData<Location> = _location
    private val _sensorReadings = MutableLiveData<SensorData>()
    val sensorReadings: LiveData<SensorData> = _sensorReadings
    private val _calculationMethodIndex = MutableLiveData<String>()
    val calculationMethodIndex: LiveData<String> = _calculationMethodIndex

    private lateinit var sensorDataObserver: (SensorData) -> Unit
    init {
        compassHandler = CompassHandler(application.getSystemService(Context.SENSOR_SERVICE) as SensorManager)
        locationHandler = LocationHandler(application.getSystemService(Context.LOCATION_SERVICE) as LocationManager)
        sensorHandler = SensorHandler(application.getSystemService(Context.SENSOR_SERVICE) as SensorManager)
        northDirection = compassHandler.northDirection
        _location.addSource(locationHandler.location) {
            saveLocation(it)
            _location.postValue(it)
        }
        sensorDataObserver = {
            settings.edit()
                .putString(KEY_ALTITUDE, it.altitude.toString())
                .putString(KEY_PRESSURE, it.pressure.toString())
                .apply()
            _sensorReadings.postValue(it)
            sensorHandler.stop()
            sensorHandler.sensorData.removeObserver(sensorDataObserver)
        }

        _scheduleData.addSource(_location) { it?.let { loc -> updateData(loc) } }
        _qiblaDirection.addSource(_location) { it?.let { loc -> updateData(loc) } }

        loadLocationFromSettings()
    }

    override fun onCleared() {
        super.onCleared()
        if (::sensorDataObserver.isInitialized) {
            sensorHandler.sensorData.removeObserver(sensorDataObserver)
        }
    }

    fun updateCalculationMethod() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val latitude = settings.getString(KEY_LATITUDE, "0")?.toDouble() ?: 0.0
                val longitude = settings.getString(KEY_LONGITUDE, "0")?.toDouble() ?: 0.0
                PrayerTimeScheduler.getCountryCode(getApplication(), latitude, longitude).thenAccept { countryCode ->
                    val calculationMethodIndex = PrayerTimeScheduler.getCalculationMethodIndex(countryCode)
                    _calculationMethodIndex.postValue(calculationMethodIndex)
                }
            }
        }
    }

    private fun saveLocation(location: Location) {
        val editor = settings.edit()
        editor.putString(KEY_LATITUDE, location.latitude.toString())
        editor.putString(KEY_LONGITUDE, location.longitude.toString())
        if (location.hasAltitude() && !sensorHandler.hasSensor()) {
            editor.putString(KEY_ALTITUDE, location.altitude.toString())
        }
        editor.apply()
    }

    /**
     * Starts tracking the compass for qibla and north direction.
     */
    fun startCompass() {
        compassHandler.startTracking()
    }

    /**
     * Stops tracking the compass.
     */
    fun stopCompass() {
        compassHandler.stopTracking()
    }

    fun updateSensorValues() {
        if (sensorHandler.hasSensor()) {
            sensorHandler.sensorData.observeForever(sensorDataObserver)
            sensorHandler.start()
        }
    }

    /**
     * Requests a location update.
     */
    fun updateLocation() {
        viewModelScope.launch(Dispatchers.IO) {
            locationHandler.update()
        }
    }

    /**
     * Loads the location from settings, or uses a default location if none is saved.
     */
    fun loadLocationFromSettings() {
        val latitude = settings.getString(KEY_LATITUDE, null)
        val longitude = settings.getString(KEY_LONGITUDE, null)
        if (latitude != null && longitude != null) {
            val location = Location("settings")
            location.latitude = latitude.toDouble()
            location.longitude = longitude.toDouble()
            _location.postValue(location)
        } else {
            val location = Location("default")
            location.latitude = 43.467
            location.longitude = -80.517
            settings.edit()
                .putString("latitude", location.latitude.toString())
                .putString("longitude", location.longitude.toString())
                .apply()
            saveLocation(location)
            _location.postValue(location)
        }
    }

    /**
     * Updates the prayer time schedule and qibla direction based on the provided location.
     *
     * @param loc The location to use for the calculations.
     */
    fun updateData(loc: Location) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val latitude = loc.latitude.toString()
                val longitude = loc.longitude.toString()
                val altitude = settings.getString(KEY_ALTITUDE, "0")
                val pressure = settings.getString(KEY_PRESSURE, "1010")
                val temperature = settings.getString("temperature", "10")

                val locationAstro = ScheduleHandler.getLocation(latitude, longitude, altitude, pressure, temperature)

                    // Calculate and post schedule
                    PrayerTimeScheduler.scheduleAlarms(getApplication()) { newScheduleData ->
                        if (newScheduleData != null) {
                            _scheduleData.postValue(newScheduleData)
                        }
                    }
                    // Calculate and post qibla direction
                    val qibla = Jitl.getNorthQibla(locationAstro)
                    _qiblaDirection.postValue(qibla.getDecimalValue(Direction.NORTH))
                }
            }
    }

    private suspend fun awaitGetFromLocation(geocoder: android.location.Geocoder, latitude: Double, longitude: Double): List<android.location.Address>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCoroutine { continuation ->
                geocoder.getFromLocation(latitude, longitude, 1, object : android.location.Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<android.location.Address>) {
                        continuation.resume(addresses)
                    }
                    override fun onError(errorMessage: String?) {
                        continuation.resume(null)
                    }
                })
            }
        } else {
            try {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1)
            } catch (e: java.io.IOException) {
                null
            }
        }
    }
}