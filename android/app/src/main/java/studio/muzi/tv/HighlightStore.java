package studio.muzi.tv;

import android.content.Context;
import android.util.Log;
import androidx.media3.common.PlaybackException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes3.dex */
public final class HighlightStore {
    static final String CACHE_FILE = "highlights-remote.json";
    static final String CACHE_URL_FILE = "highlights-remote.url";
    static final long REMOTE_TTL_MS = 300000;
    static final String SIDE_FILE = "highlights.json";
    static final String TAG = "MuziTV";
    private static HighlightMap memory;
    private static long memoryAt;
    private static String memoryUrl = "";

    public static final class Loaded {
        public final HighlightMap map;
        public final String origin;

        Loaded(HighlightMap map, String origin) {
            this.map = map;
            this.origin = origin;
        }
    }

    private HighlightStore() {
    }

    public static Loaded load(Context ctx) {
        Context app = ctx.getApplicationContext();
        String url = readUrl(app);
        if (!url.isEmpty()) {
            synchronized (HighlightStore.class) {
                if (memory != null && url.equals(memoryUrl) && System.currentTimeMillis() - memoryAt < 300000) {
                    return new Loaded(memory, "网络地址");
                }
                try {
                    String body = httpGet(url);
                    HighlightMap map = HighlightMap.parse(body);
                    writeCache(app, url, body);
                    synchronized (HighlightStore.class) {
                        remember(url, map);
                    }
                    logWarnings(map, "网络地址");
                    Log.i(TAG, "highlights from url entries=" + map.entries.size());
                    return new Loaded(map, "网络地址");
                } catch (Exception e) {
                    Log.w(TAG, "highlight url failed: " + e.getMessage());
                    HighlightMap cached = readCache(app, url);
                    if (cached != null) {
                        return new Loaded(cached, "网络缓存");
                    }
                }
            }
        }
        return loadLocal(app);
    }

    public static synchronized Loaded loadLocal(Context ctx) {
        synchronized (HighlightStore.class) {
            Context app = ctx.getApplicationContext();
            File side = new File(app.getFilesDir(), SIDE_FILE);
            if (side.isFile() && side.length() > 0) {
                try {
                    HighlightMap map = HighlightMap.parse(readUtf8(new FileInputStream(side)));
                    logWarnings(map, "侧载文件");
                    Log.i(TAG, "highlights from file entries=" + map.entries.size());
                    return new Loaded(map, "侧载文件");
                } catch (Exception e) {
                    Log.w(TAG, "side-loaded highlights ignored", e);
                }
            }
            try {
                HighlightMap map2 = HighlightMap.parse(readUtf8(app.getAssets().open(SIDE_FILE)));
                logWarnings(map2, "安装包示例");
                Log.i(TAG, "highlights from assets entries=" + map2.entries.size());
                return new Loaded(map2, "安装包示例");
            } catch (Exception e2) {
                Log.w(TAG, "bundled highlights unreadable", e2);
                return new Loaded(HighlightMap.empty(), "无");
            }
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:14:0x0044, code lost:
    
        if (r3.startsWith("https://") != false) goto L15;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static synchronized String readUrl(Context ctx) {
        synchronized (HighlightStore.class) {
            File file = new File(ctx.getApplicationContext().getFilesDir(), "config.json");
            if (!file.isFile()) {
                return "";
            }
            try {
                JSONObject json = new JSONObject(readUtf8(new FileInputStream(file)));
                String url = json.optString("highlightsUrl", "").trim();
                if (!url.startsWith("http://")) {
                }
                return url;
            } catch (Exception e) {
            }
        }
        return "";
    }

    public static synchronized void writeUrl(Context ctx, String url) throws IOException {
        synchronized (HighlightStore.class) {
            Context app = ctx.getApplicationContext();
            String url2 = url == null ? "" : url.trim();
            if (!url2.isEmpty() && !url2.startsWith("http://") && !url2.startsWith("https://")) {
                throw new IOException("地址必须以 http:// 或 https:// 开头");
            }
            File file = new File(app.getFilesDir(), "config.json");
            JSONObject json = new JSONObject();
            if (file.isFile()) {
                try {
                    json = new JSONObject(readUtf8(new FileInputStream(file)));
                } catch (JSONException e) {
                    json = new JSONObject();
                }
            }
            try {
                if (url2.isEmpty()) {
                    json.remove("highlightsUrl");
                } else {
                    json.put("highlightsUrl", url2);
                }
                FileOutputStream out = app.openFileOutput("config.json", 0);
                try {
                    out.write(json.toString().getBytes("UTF-8"));
                    out.close();
                    memory = null;
                    memoryUrl = "";
                    memoryAt = 0L;
                } catch (Throwable th) {
                    out.close();
                    throw th;
                }
            } catch (JSONException e2) {
                throw new IOException(e2);
            }
        }
    }

    public static String sideLoadPath(Context ctx) {
        return new File(ctx.getApplicationContext().getFilesDir(), SIDE_FILE).getAbsolutePath();
    }

    public static String describe(Context ctx) {
        String url = readUrl(ctx);
        String path = sideLoadPath(ctx);
        if (!url.isEmpty()) {
            return "当前优先使用网络：\n" + url + "\n\n侧载文件：\n" + path;
        }
        File side = new File(path);
        return side.isFile() ? "当前使用侧载文件：\n" + path : "当前使用安装包内示例。\n\n侧载文件路径：\n" + path;
    }

    private static void remember(String url, HighlightMap map) {
        memory = map;
        memoryUrl = url;
        memoryAt = System.currentTimeMillis();
    }

    private static void logWarnings(HighlightMap map, String origin) {
        for (int i = 0; i < map.warnings.size(); i++) {
            Log.w(TAG, origin + " " + map.warnings.get(i));
        }
    }

    private static HighlightMap readCache(Context ctx, String url) {
        File pointer = new File(ctx.getFilesDir(), CACHE_URL_FILE);
        File body = new File(ctx.getFilesDir(), CACHE_FILE);
        if (!pointer.isFile() || !body.isFile()) {
            return null;
        }
        try {
            String cachedUrl = readUtf8(new FileInputStream(pointer)).trim();
            if (!url.equals(cachedUrl)) {
                return null;
            }
            HighlightMap map = HighlightMap.parse(readUtf8(new FileInputStream(body)));
            Log.i(TAG, "highlights from cache entries=" + map.entries.size());
            return map;
        } catch (Exception e) {
            Log.w(TAG, "highlight cache ignored", e);
            return null;
        }
    }

    private static void writeCache(Context ctx, String url, String body) {
        try {
            FileOutputStream bodyOut = ctx.openFileOutput(CACHE_URL_FILE, 0);
            try {
                bodyOut.write(url.getBytes("UTF-8"));
                bodyOut.close();
                bodyOut = ctx.openFileOutput(CACHE_FILE, 0);
                try {
                    bodyOut.write(body.getBytes("UTF-8"));
                    bodyOut.close();
                } finally {
                }
            } finally {
            }
        } catch (IOException e) {
            Log.w(TAG, "highlight cache not saved", e);
        }
    }

    private static String httpGet(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setConnectTimeout(PlaybackException.ERROR_CODE_DRM_UNSPECIFIED);
            conn.setReadTimeout(8000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("Accept", "application/json,text/plain");
            conn.setRequestProperty("User-Agent", "MuziTV/0.9");
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IOException("HTTP " + code);
            }
            return readLimited(conn.getInputStream());
        } finally {
            conn.disconnect();
        }
    }

    private static String readLimited(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int total = 0;
        while (true) {
            int n = in.read(buf);
            if (n != -1) {
                total += n;
                if (total > 1048576) {
                    throw new IOException("标记文件超过 1MB");
                }
                out.write(buf, 0, n);
            } else {
                String text = out.toString("UTF-8");
                return text.startsWith("\ufeff") ? text.substring(1) : text;
            }
        }
    }

    private static String readUtf8(InputStream in) throws IOException {
        try {
            return readLimited(in);
        } finally {
            in.close();
        }
    }
}
