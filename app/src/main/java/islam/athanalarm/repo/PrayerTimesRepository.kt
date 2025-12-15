package islam.athanalarm.repo

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import islam.athanalarm.CONSTANT
import islam.athanalarm.handler.ScheduleData
import islam.athanalarm.handler.ScheduleHandler
import net.sourceforge.jitl.astro.Location
import java.io.IOException
import java.security.GeneralSecurityException

class PrayerTimesRepository(private val context: Context) {

    fun getTodaysSchedule(): ScheduleData? {
        try {
            val masterKey = MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val settings = EncryptedSharedPreferences.create(
                context,
                "secret_shared_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            val latitude = settings.getString("latitude", "0")
            val longitude = settings.getString("longitude", "0")
            val altitude = settings.getString("altitude", "0")
            val pressure = settings.getString("pressure", "1010")
            val temperature = settings.getString("temperature", "10")
            val calculationMethodIndex = settings.getString("calculationMethodsIndex", CONSTANT.DEFAULT_CALCULATION_METHOD)
            val roundingTypeIndex = settings.getString("roundingTypesIndex", CONSTANT.DEFAULT_ROUNDING_TYPE)
            val offsetMinutes = settings.getInt("offsetMinutes", 0)

            val location = ScheduleHandler.getLocation(latitude, longitude, altitude, pressure, temperature)
            return ScheduleHandler.calculate(location, calculationMethodIndex, roundingTypeIndex, offsetMinutes)
        } catch (e: GeneralSecurityException) {
            e.printStackTrace()
            return null
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }
}
