package islam.athanalarm

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Html
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import islam.athanalarm.ui.QiblaScreen
import islam.athanalarm.ui.TodayScreen

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                viewModel.updateLocation()
                val intent = Intent(this, PrayerTimeReceiver::class.java)
                intent.action = CONSTANT.ACTION_UPDATE_PRAYER_TIMES
                sendBroadcast(intent)
            } else {
                viewModel.loadLocationFromSettings()
            }
            requestNotificationPermission()
        }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission granted, no special action needed
            } else {
                // Permission denied, no special action needed
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen(viewModel)
        }

        supportActionBar?.title = Html.fromHtml("<font face='monospace'>" + supportActionBar?.title + "</font>", Html.FROM_HTML_MODE_LEGACY)

        requestLocationPermission()
        requestExactAlarmPermission()
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                intent.data = Uri.fromParts("package", packageName, null)
                startActivity(intent)
            }
        }
    }

    private fun requestLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                viewModel.updateLocation()
            }
            else -> {
                viewModel.loadLocationFromSettings()
                requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED -> {
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.startCompass()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopCompass()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    private val settingsResultProvider = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.loadLocationFromSettings()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_settings -> {
                val i = Intent(applicationContext, SettingsActivity::class.java)
                settingsResultProvider.launch(i)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

sealed class Screen(val route: String, val resourceId: Int, val icon: @Composable () -> Unit) {
    object Today : Screen("today", R.string.today, { Icon(Icons.Filled.DateRange, contentDescription = null) })
    object Qibla : Screen("qibla", R.string.qibla, { Icon(Icons.Filled.LocationOn, contentDescription = null) })
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val items = listOf(Screen.Today, Screen.Qibla)
    Scaffold(
        bottomBar = {
            BottomNavigation(backgroundColor = colorResource(R.color.colorPrimary)) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    BottomNavigationItem(
                        icon = { screen.icon() },
                        label = { Text(stringResource(screen.resourceId)) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = Screen.Today.route, Modifier.padding(innerPadding)) {
            composable(Screen.Today.route) { TodayScreen(viewModel = viewModel) }
            composable(Screen.Qibla.route) { QiblaScreen(viewModel = viewModel) }
        }
    }
}
