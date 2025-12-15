package islam.athanalarm.handler;

import android.app.Application;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.sourceforge.jitl.astro.Location;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import islam.athanalarm.CONSTANT;

@RunWith(AndroidJUnit4.class)
public class ScheduleHandlerTest {

    private Application application;
    private GregorianCalendar[] schedule;
    private boolean[] extremes;

    @Before
    public void setUp() {
        Locale.setDefault(Locale.US);
        application = ApplicationProvider.getApplicationContext();
        // Set up a fixed schedule for consistent testing
        schedule = new GregorianCalendar[7];
        extremes = new boolean[7];
        // Set timezone to UTC to avoid local timezone issues
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        // Fajr: 5:00 AM
        schedule[CONSTANT.FAJR] = new GregorianCalendar(2024, Calendar.JANUARY, 1, 5, 0, 0);
        // Sunrise: 6:30 AM
        schedule[CONSTANT.SUNRISE] = new GregorianCalendar(2024, Calendar.JANUARY, 1, 6, 30, 0);
        // Dhuhr: 12:00 PM
        schedule[CONSTANT.DHUHR] = new GregorianCalendar(2024, Calendar.JANUARY, 1, 12, 0, 0);
        // Asr: 3:00 PM
        schedule[CONSTANT.ASR] = new GregorianCalendar(2024, Calendar.JANUARY, 1, 15, 0, 0);
        // Maghrib: 5:30 PM
        schedule[CONSTANT.MAGHRIB] = new GregorianCalendar(2024, Calendar.JANUARY, 1, 17, 30, 0);
        // Isha: 7:00 PM
        schedule[CONSTANT.ISHAA] = new GregorianCalendar(2024, Calendar.JANUARY, 1, 19, 0, 0);
        // Next Fajr: 5:01 AM (next day)
        schedule[CONSTANT.NEXT_FAJR] = new GregorianCalendar(2024, Calendar.JANUARY, 2, 5, 1, 0);

        // Mark one time as extreme for testing
        extremes[CONSTANT.ASR] = true;
    }

    @Test
    public void testCalculate() {
        // Given
        Location location = new Location(34.0522, -118.2437, -8, 0); // Los Angeles
        String calculationMethodIndex = "0"; // ISNA
        String roundingTypeIndex = "0"; // No rounding
        int offsetMinutes = 0;

        // When
        ScheduleData scheduleData = ScheduleHandler.calculate(location, calculationMethodIndex, roundingTypeIndex, offsetMinutes);

        // Then
        assertNotNull(scheduleData);
        assertNotNull(scheduleData.schedule);
        assertEquals(7, scheduleData.schedule.length);

        // Verify that the prayer times are in the correct order
        for (int i = 0; i< scheduleData.schedule.length - 1; i++) {
            assertTrue("Prayer time " + i + " should be before " + (i+1),
                    scheduleData.schedule[i].before(scheduleData.schedule[i+1]));
        }

        // Verify that the next prayer index is calculated correctly
        assertTrue(scheduleData.nextTimeIndex >= 0 && scheduleData.nextTimeIndex < scheduleData.schedule.length);
    }

    @Test
    public void testGetFormattedTime() {
        // --- AM/PM format (12-hour) ---
        assertEquals("5:00 AM", ScheduleHandler.getFormattedTime(schedule, extremes, CONSTANT.FAJR, "0"));
        assertEquals("12:00 PM", ScheduleHandler.getFormattedTime(schedule, extremes, CONSTANT.DHUHR, "0"));
        assertEquals("7:00 PM", ScheduleHandler.getFormattedTime(schedule, extremes, CONSTANT.ISHAA, "0"));

        // Test extreme time formatting
        assertEquals("3:00 PM *", ScheduleHandler.getFormattedTime(schedule, extremes, CONSTANT.ASR, "0"));


        // --- 24-hour format ---
        assertEquals("05:00", ScheduleHandler.getFormattedTime(schedule, extremes, CONSTANT.FAJR, "1"));
        assertEquals("12:00", ScheduleHandler.getFormattedTime(schedule, extremes, CONSTANT.DHUHR, "1"));
        assertEquals("19:00", ScheduleHandler.getFormattedTime(schedule, extremes, CONSTANT.ISHAA, "1"));

        // Test extreme time formatting in 24h
        assertEquals("15:00 *", ScheduleHandler.getFormattedTime(schedule, extremes, CONSTANT.ASR, "1"));
    }

    @Test
    public void testGetNextTimeIndex() {
        // Mock current time to test different scenarios
        Calendar now = new GregorianCalendar(2024, Calendar.JANUARY, 1, 10, 0, 0); // 10:00 AM
        assertEquals(CONSTANT.DHUHR, ScheduleHandler.getNextTimeIndex(schedule, now));

        now.set(Calendar.HOUR_OF_DAY, 18); // 6:00 PM
        assertEquals(CONSTANT.ISHAA, ScheduleHandler.getNextTimeIndex(schedule, now));

        now.set(Calendar.HOUR_OF_DAY, 4); // 4:00 AM
        assertEquals(CONSTANT.FAJR, ScheduleHandler.getNextTimeIndex(schedule, now));
    }
}
