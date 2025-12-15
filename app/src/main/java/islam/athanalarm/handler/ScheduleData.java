package islam.athanalarm.handler;

import java.util.GregorianCalendar;

public class ScheduleData {
    public final GregorianCalendar[] schedule;
    public final boolean[] extremes;
    public final short nextTimeIndex;

    public ScheduleData(GregorianCalendar[] schedule, boolean[] extremes, short nextTimeIndex) {
        this.schedule = schedule;
        this.extremes = extremes;
        this.nextTimeIndex = nextTimeIndex;
    }
}