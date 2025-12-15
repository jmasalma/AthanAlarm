package islam.athanalarm

import android.app.Application
import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var application: Application
    private lateinit var prefs: SharedPreferences

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        // Clear preferences before each test to ensure a clean state
        val masterKey = MasterKey.Builder(application, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        prefs = EncryptedSharedPreferences.create(
            application,
            "secret_shared_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        prefs.edit().clear().commit()
    }

    @Test
    fun `test default location is used when no settings are available`() {
        // When
        val viewModel = MainViewModel(application)

        // Then
        val location = viewModel.location.value
        assertNotNull("Location should not be null", location)
        assertEquals(43.467, location!!.latitude, 0.001)
        assertEquals(-80.517, location.longitude, 0.001)
    }

    @Test
    fun `test initial location is loaded from settings`() {
        // Given
        prefs.edit()
            .putString("latitude", "34.0522")
            .putString("longitude", "-118.2437")
            .commit()

        // When
        val viewModel = MainViewModel(application)

        // Then
        val location = viewModel.location.value
        assertNotNull("Location should not be null", location)
        assertEquals(34.0522, location!!.latitude, 0.001)
        assertEquals(-118.2437, location.longitude, 0.001)
    }
}
