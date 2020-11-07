package httpcomunication;

import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DebugUtil {
    private static SimpleDateFormat  format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    public static void info(String message) {
        System.out.println(format.format(new Date()) + ": " + message);
    }
}
