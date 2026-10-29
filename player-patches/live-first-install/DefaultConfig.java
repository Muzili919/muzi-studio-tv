package studio.muzi.fongmi;

import android.util.Log;
import java.lang.reflect.Method;

/** Seed public subscriptions only when that subscription slot has no saved URL. */
public final class DefaultConfig {
    static final String URL = "https://zhishixingqiu.top/tv/fongmi.json";

    public static void ensure() {
        seed("vod", 0, "木子公共点播");
        seed("live", 1, "木子公共直播");
    }

    private static void seed(String slot, int type, String name) {
        try {
            Class<?> config = Class.forName("com.fongmi.android.tv.bean.Config");
            Object saved = config.getMethod(slot).invoke(null);
            String url = (String) config.getMethod("getUrl").invoke(saved);
            if (url != null && !url.trim().isEmpty()) return;
            Method create = config.getMethod("create", int.class, String.class, String.class);
            Object initial = create.invoke(null, type, URL, name);
            config.getMethod("update").invoke(initial);
            Log.i("MuziDefaults", "Initialized empty " + slot + " subscription");
        } catch (Exception e) {
            Log.w("MuziDefaults", "Cannot initialize " + slot + " subscription", e);
        }
    }
}
