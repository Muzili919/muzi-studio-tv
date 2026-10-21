package studio.muzi.tv;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/* loaded from: classes3.dex */
public final class LiveGuide {
    static final String[][] KEYS = {new String[]{"CCTV-1"}, new String[]{"CCTV-5", "CCTV-5+"}, new String[]{"CCTV-6"}, new String[]{"CCTV-13"}, new String[]{"湖南卫视", "Hunan TV"}, new String[]{"浙江卫视", "Zhejiang Satellite"}, new String[]{"东方卫视", "Dragon TV"}, new String[]{"北京卫视", "Beijing Satellite"}, new String[]{"江苏卫视", "Jiangsu Satellite"}, new String[]{"凤凰卫视", "Phoenix Chinese"}};

    public static final class Channel {
        public final String group;
        public final String name;
        public final String url;

        public Channel(String name, String url, String group) {
            this.name = name;
            this.url = url;
            this.group = group == null ? "" : group;
        }
    }

    private LiveGuide() {
    }

    public static List<Channel> parseM3u(String text) {
        List<Channel> out = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return out;
        }
        String[] lines = text.split("\n");
        String pendingName = null;
        String pendingGroup = "";
        for (String str : lines) {
            String line = str.trim();
            if (line.startsWith("#EXTINF")) {
                int comma = line.lastIndexOf(44);
                pendingName = comma >= 0 ? line.substring(comma + 1).trim() : "频道";
                pendingGroup = attr(line, "group-title");
                if (pendingName.isEmpty()) {
                    pendingName = "频道";
                }
            } else if (!line.isEmpty() && !line.startsWith("#") && pendingName != null) {
                if (line.startsWith("http://") || line.startsWith("https://")) {
                    out.add(new Channel(pendingName, line, pendingGroup));
                }
                pendingName = null;
                pendingGroup = "";
            }
        }
        return out;
    }

    public static List<Channel> recommend(List<Channel> all) {
        List<Channel> picked = new ArrayList<>();
        if (all == null) {
            return picked;
        }
        for (int k = 0; k < KEYS.length; k++) {
            Channel fallback = null;
            Channel chosen = null;
            for (int i = 0; i < all.size() && chosen == null; i++) {
                Channel channel = all.get(i);
                if (matchesAny(channel.name, KEYS[k]) && !already(picked, channel)) {
                    if (usable(channel.name)) {
                        chosen = channel;
                    } else if (fallback == null) {
                        fallback = channel;
                    }
                }
            }
            if (chosen == null) {
                chosen = fallback;
            }
            if (chosen != null) {
                picked.add(chosen);
            }
        }
        return picked;
    }

    static boolean matches(String name, String key) {
        if (name == null || key == null) {
            return false;
        }
        String n = strip(name);
        String token = strip(key);
        if (token.toUpperCase(Locale.ROOT).startsWith("CCTV")) {
            String upper = n.toUpperCase(Locale.ROOT);
            String needle = token.toUpperCase(Locale.ROOT);
            int at = upper.indexOf(needle);
            if (at < 0) {
                return false;
            }
            int after = needle.length() + at;
            if (after >= upper.length()) {
                return true;
            }
            char next = upper.charAt(after);
            if (Character.isDigit(next) || next == '+') {
                return false;
            }
            return next < 'A' || next > 'Z' || (next == 'H' && upper.startsWith("HD", after));
        }
        return n.toUpperCase(Locale.ROOT).contains(token.toUpperCase(Locale.ROOT));
    }

    private static boolean matchesAny(String name, String[] keys) {
        for (String str : keys) {
            if (matches(name, str)) {
                return true;
            }
        }
        return false;
    }

    static boolean usable(String name) {
        if (name == null) {
            return false;
        }
        String n = name.toLowerCase(Locale.ROOT);
        return n.indexOf("geo-blocked") < 0 && n.indexOf("not 24/7") < 0;
    }

    private static String strip(String s) {
        return s.replace(" ", "").replace("－", "-").replace("—", "-");
    }

    private static boolean already(List<Channel> picked, Channel channel) {
        for (int i = 0; i < picked.size(); i++) {
            if (picked.get(i).url.equals(channel.url)) {
                return true;
            }
        }
        return false;
    }

    private static String attr(String line, String key) {
        int start;
        int end;
        String mark = key + "=\"";
        int at = line.indexOf(mark);
        if (at < 0 || (end = line.indexOf(34, (start = mark.length() + at))) < 0) {
            return "";
        }
        return line.substring(start, end);
    }
}
