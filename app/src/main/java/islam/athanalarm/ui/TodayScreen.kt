package islam.athanalarm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aminography.primecalendar.hijri.HijriCalendar
import islam.athanalarm.CONSTANT
import islam.athanalarm.MainViewModel
import islam.athanalarm.R
import islam.athanalarm.handler.ScheduleData
import islam.athanalarm.handler.ScheduleHandler
import java.util.Locale
import java.util.TimeZone

/**
 * Composable screen that displays the prayer times for today.
 *
 * @param viewModel The view model for the main screen.
 */
@Composable
fun TodayScreen(viewModel: MainViewModel) {
    val scheduleData by viewModel.scheduleData.observeAsState()
    scheduleData?.let {
        PrayerTimeList(scheduleData = it)
    }
}

/**
 * Composable that displays a list of prayer times.
 *
 * @param scheduleData The prayer time schedule.
 */
@Composable
fun PrayerTimeList(scheduleData: ScheduleData) {
    val prayerNames = listOf(
        R.string.fajr,
        R.string.sunrise,
        R.string.dhuhr,
        R.string.asr,
        R.string.maghrib,
        R.string.ishaa,
        R.string.next_fajr
    )

    LazyColumn {
        item {
            val hijriCalendar = HijriCalendar(TimeZone.getDefault(), Locale.ENGLISH)
            Text(
                text = hijriCalendar.longDateString,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
        }
        itemsIndexed(prayerNames) { index, prayerNameResId ->
            PrayerTimeRow(
                prayerName = stringResource(id = prayerNameResId),
                prayerTime = ScheduleHandler.getFormattedTime(
                    scheduleData.schedule,
                    scheduleData.extremes,
                    index.toShort(),
                    "0" // Assuming default time format
                ),
                isNextPrayer = index.toShort() == scheduleData.nextTimeIndex
            )
        }
    }
}

@Composable
fun PrayerTimeRow(prayerName: String, prayerTime: String, isNextPrayer: Boolean) {
    val backgroundColor = if (isNextPrayer) MaterialTheme.colors.primary.copy(alpha = 0.1f) else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = prayerName, fontFamily = FontFamily.Monospace)
        Text(text = prayerTime, fontFamily = FontFamily.Monospace)
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    // This is a dummy ScheduleData for preview purposes
    val dummyScheduleData = ScheduleData(
        Array(7) { java.util.GregorianCalendar() },
        BooleanArray(7) { false },
        CONSTANT.DHUHR.toShort()
    )
    PrayerTimeList(scheduleData = dummyScheduleData)
}
