package studio.muzi.tv;

import java.net.URL;
import java.util.Locale;

/* loaded from: classes3.dex */
public final class PosterUrls {
    private PosterUrls() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) {
            return "";
        }
        if (s.startsWith("//")) {
            s = "https:" + s;
        }
        if (!s.startsWith("http://") && !s.startsWith("https://")) {
            return "";
        }
        return s;
    }

    public static String resolve(String raw, String base) {
        String ready = normalize(raw);
        if (!ready.isEmpty()) {
            return ready;
        }
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s) || base == null || base.isEmpty()) {
            return "";
        }
        try {
            return normalize(new URL(new URL(base), s).toString());
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean isYouku(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        try {
            return new URL(url).getHost().toLowerCase(Locale.ROOT).contains("youkupic.com");
        } catch (Exception e) {
            return url.toLowerCase(Locale.ROOT).contains("youkupic.com");
        }
    }

    public static boolean isImage(byte[] bytes) {
        return start(bytes) >= 0;
    }

    public static byte[] payload(byte[] bytes) {
        int at = start(bytes);
        if (at <= 0) {
            return bytes;
        }
        byte[] cut = new byte[bytes.length - at];
        System.arraycopy(bytes, at, cut, 0, cut.length);
        return cut;
    }

    private static int start(byte[] bytes) {
        if (bytes == null) {
            return -1;
        }
        int i = 0;
        if (bytes.length >= 3 && (bytes[0] & 255) == 239 && (bytes[1] & 255) == 187 && (bytes[2] & 255) == 191) {
            i = 3;
        }
        while (i < bytes.length && (bytes[i] == 32 || bytes[i] == 10 || bytes[i] == 13 || bytes[i] == 9)) {
            i++;
        }
        int n = bytes.length - i;
        if (n < 12) {
            return -1;
        }
        if ((bytes[i] & 255) == 255 && (bytes[i + 1] & 255) == 216) {
            return i;
        }
        if (bytes[i] == -119 && bytes[i + 1] == 80 && bytes[i + 2] == 78 && bytes[i + 3] == 71) {
            return i;
        }
        if (bytes[i] == 71 && bytes[i + 1] == 73 && bytes[i + 2] == 70) {
            return i;
        }
        if (bytes[i] != 82 || bytes[i + 1] != 73 || bytes[i + 2] != 70 || bytes[i + 3] != 70 || bytes[i + 8] != 87 || bytes[i + 9] != 69 || bytes[i + 10] != 66 || bytes[i + 11] != 80) {
            return -1;
        }
        return i;
    }
}
