package android.util;
public class Log {
    public static int failures;
    public static int i(String tag, String message) { return 0; }
    public static int w(String tag, String message, Throwable e) { failures++; return 0; }
}
