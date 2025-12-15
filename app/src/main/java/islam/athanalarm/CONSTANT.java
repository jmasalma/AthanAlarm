package islam.athanalarm;

import net.sourceforge.jitl.Method;
import net.sourceforge.jitl.Rounding;

public class CONSTANT {
    public static final String ACTION_UPDATE_PRAYER_TIMES = "islam.athanalarm.ACTION_UPDATE_PRAYER_TIMES";
    public static final String ACTION_UPDATE_WIDGET = "islam.athanalarm.ACTION_UPDATE_WIDGET";
    public static final String ACTION_LOCATION_UPDATED = "islam.athanalarm.ACTION_LOCATION_UPDATED";
    public static final short FAJR = 0;
    public static final short SUNRISE = 1;
    public static final short DHUHR = 2;
    public static final short ASR = 3;
    public static final short MAGHRIB = 4;
    public static final short ISHAA = 5;
    public static final short NEXT_FAJR = 6;
    public static final String[][] CALCULATION_METHOD_COUNTRY_CODES = {
            {}, // Jafari
            {"US", "CA"}, // ISNA
            {"GB", "FR", "DE", "IT", "ES", "NL", "BE", "SE", "NO", "DK", "CH", "AT", "IE", "FI", "PT", "LU", "IS", "GR", "CY"}, // MWL
            {"SA"},       // Umm al-Qura
            {"EG", "SY", "IQ", "JO", "LB", "PS", "TR", "MY", "SG", "BN"}, // Egypt
            {"PK", "BD", "IN", "AF"}, // Karachi
            {}  // Dubai
    };
    public static final String DEFAULT_CALCULATION_METHOD = "1";
    public static final String DEFAULT_ROUNDING_TYPE = "2";
    public static final int DEFAULT_TIME_FORMAT = 0;
    public static final int NOTIFICATION_ID_OFFSET = 10;
    public static final int REQUEST_CODE_OFFSET = 10;
    public static final Method[] CALCULATION_METHODS = {
            new Method(16, 14, 4, 0, 0, 0, Rounding.SPECIAL, net.sourceforge.jitl.Mathhab.SHAAFI, 48.5, net.sourceforge.jitl.ExtremeLatitude.GOOD_INVALID, true, 0, 0, 0, 0, 0, 0), // Jafari
            Method.ISNA,
            Method.MUSLIM_LEAGUE,
            Method.UMM_ALQURRA,
            Method.EGYPT_SURVEY,
            new Method(18, 18, 1.5, 0, 0, 0, Rounding.SPECIAL, net.sourceforge.jitl.Mathhab.HANAFI, 48.5, net.sourceforge.jitl.ExtremeLatitude.GOOD_INVALID, false, 0, 0, 0, 0, 0, 0), // Karachi Hanafi
            new Method(18.2, 18.2, 1.5, 0, 0, 0, Rounding.SPECIAL, net.sourceforge.jitl.Mathhab.SHAAFI, 48.5, net.sourceforge.jitl.ExtremeLatitude.GOOD_INVALID, false, 0, 0, 0, 0, 0, 0) // Dubai
    };
    public static final Rounding[] ROUNDING_TYPES = {
            Rounding.NONE,
            Rounding.NORMAL,
            Rounding.SPECIAL,
            Rounding.AGRESSIVE
    };
}
