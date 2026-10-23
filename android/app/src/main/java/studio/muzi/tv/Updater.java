package studio.muzi.tv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import org.json.JSONObject;

/**
 * Update check for the family launcher: silent once at start (auto) and on demand (check).
 * Default manifest: https://zhishixingqiu.top/tv/version.json (studio key, package studio.muzi.tv).
 * Override with config.json "updateUrl", or Gradle -PmuziUpdateUrl / -PmuziBaseUrl.
 * Android 6: versionCode field only (no API 28 getLongVersionCode), file:// install URI.
 */
public final class Updater {
    public static final String VERSION_URL = "https://zhishixingqiu.top/tv/version.json";
    private static final String TAG = "MuziUpdater";

    private static boolean autoChecked;

    /** Silent check once per process at launcher start; only a newer version shows a dialog. */
    public static void auto(final Activity a) {
        if (autoChecked) return;
        autoChecked = true;
        run(a, false);
    }

    /** Manual check from All apps; reports up-to-date and failures with a toast. */
    public static void check(final Activity a) {
        run(a, true);
    }

    private static void run(final Activity a, final boolean manual) {
        final String manifest = versionUrl(a);
        new Thread(() -> {
            try {
                JSONObject v = new JSONObject(new String(get(manifest), "UTF-8"));
                offer(a, v.optJSONObject("studio"), a.getPackageName(), "muzi Launcher", manual);
                // FongMi updates require their own API/ABI and signature compatibility review.
            } catch (Throwable t) {
                Log.w(TAG, "check failed " + manifest, t);
                if (manual) a.runOnUiThread(() -> Toast.makeText(a, "暂时无法检查更新，请稍后重试", Toast.LENGTH_LONG).show());
            }
        }, "muzi-updater").start();
    }

    /** config.json updateUrl, else the URL baked in at build time, else VERSION_URL. */
    static String versionUrl(Activity a) {
        try {
            File f = new File(a.getFilesDir(), "config.json");
            if (f.isFile()) {
                byte[] buf = new byte[(int) Math.min(f.length(), 65536)];
                FileInputStream in = new FileInputStream(f);
                int n = in.read(buf);
                in.close();
                if (n > 0) {
                    String u = new JSONObject(new String(buf, 0, n, "UTF-8")).optString("updateUrl", "").trim();
                    if (u.startsWith("http://") || u.startsWith("https://")) return u;
                }
            }
        } catch (Throwable ignored) { }
        try {
            String built = BuildConfig.UPDATE_URL;
            if (built != null) {
                String u = built.trim();
                if (u.startsWith("http://") || u.startsWith("https://")) return u;
            }
        } catch (Throwable ignored) { }
        return VERSION_URL;
    }

    private static long installedCode(Activity a, String pkg) {
        try {
            PackageInfo p = a.getPackageManager().getPackageInfo(pkg, 0);
            // Keep this on the versionCode field. getLongVersionCode() is API 28 and a direct
            // call fails verification on Android 6, which would swallow the whole update check.
            return p.versionCode & 0xffffffffL;
        } catch (Exception e) {
            return -1;
        }
    }

    private static void offer(final Activity a, final JSONObject o, String pkg, final String label, final boolean manual) {
        if (o == null) return;
        long remote = o.optLong("versionCode", 0), local = installedCode(a, pkg);
        if (remote <= local) { if (manual) a.runOnUiThread(() -> Toast.makeText(a, "当前已是最新版本", Toast.LENGTH_SHORT).show()); return; }
        // Older manifests have no minSdk; treat a missing value as compatible (the APK itself is still checked before install).
        if (o.optInt("minSdk", 0) > Build.VERSION.SDK_INT) { if (manual) a.runOnUiThread(() -> Toast.makeText(a, "新版本不兼容当前系统", Toast.LENGTH_LONG).show()); return; }
        final String url = o.optString("url"), sha = o.optString("sha256"), name = o.optString("versionName");
        a.runOnUiThread(() -> {
            if (a.isFinishing()) return;
            new AlertDialog.Builder(a)
                .setTitle(label + (local < 0 ? " 未安装" : " 有新版本"))
                .setMessage("版本 " + name + "，现在下载并安装？")
                .setPositiveButton("更新", (d, w) -> download(a, url, sha, pkg))
                .setNegativeButton("以后", null).show();
        });
    }

    private static void download(final Activity a, final String url, final String sha, final String pkg) {
        Toast.makeText(a, "正在下载…", Toast.LENGTH_LONG).show();
        new Thread(() -> {
            try {
                if (!url.startsWith("https://") || !sha.matches("(?i)[0-9a-f]{64}"))
                    throw new Exception("更新地址或校验信息无效");
                File dir = Build.VERSION.SDK_INT >= 24 ? new File(a.getCacheDir(), "update") : new File(a.getExternalCacheDir() != null ? a.getExternalCacheDir() : a.getCacheDir(), "update");
                if (!dir.exists() && !dir.mkdirs()) throw new Exception("无法保存安装包");
                File f = new File(dir, pkg + ".apk");
                File partial = new File(dir, pkg + ".download.apk");
                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                c.setConnectTimeout(10000); c.setReadTimeout(30000);
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                try {
                    try (InputStream in = c.getInputStream(); FileOutputStream out = new FileOutputStream(partial)) {
                        byte[] buf = new byte[65536]; int n; long total = 0;
                        while ((n = in.read(buf)) != -1) {
                            total += n;
                            if (total > 100L * 1024 * 1024) throw new Exception("安装包超过大小限制");
                            digest.update(buf, 0, n); out.write(buf, 0, n);
                        }
                    }
                    if (!sha.equalsIgnoreCase(hex(digest.digest()))) throw new Exception("校验失败");
                    PackageInfo candidate = a.getPackageManager().getPackageArchiveInfo(partial.getPath(), android.content.pm.PackageManager.GET_SIGNATURES);
                    PackageInfo current = a.getPackageManager().getPackageInfo(pkg, android.content.pm.PackageManager.GET_SIGNATURES);
                    if (candidate == null || !pkg.equals(candidate.packageName) || candidate.versionCode <= current.versionCode)
                        throw new Exception("安装包身份或版本不匹配");
                    if (!java.util.Arrays.equals(candidate.signatures, current.signatures)) throw new Exception("签名不同，已阻止覆盖安装");
                    if (Build.VERSION.SDK_INT >= 24 && candidate.applicationInfo.minSdkVersion > Build.VERSION.SDK_INT)
                        throw new Exception("新版本不兼容当前系统");
                    if (f.exists() && !f.delete()) throw new Exception("无法替换缓存文件");
                    if (!partial.renameTo(f)) throw new Exception("无法保存下载结果");
                } finally { c.disconnect(); partial.delete(); }
                dir.setReadable(true, false); dir.setExecutable(true, false); f.setReadable(true, false);
                Uri uri;
                if (Build.VERSION.SDK_INT >= 24) {
                    uri = Uri.parse("content://" + a.getPackageName() + ".apk/" + f.getName());
                } else {
                    uri = Uri.fromFile(f);
                }
                Intent i = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                a.runOnUiThread(() -> {
                    try { a.startActivity(i); }
                    catch (Throwable e) { Toast.makeText(a, "更新失败：" + e.getMessage(), Toast.LENGTH_LONG).show(); }
                });
            } catch (final Throwable e) {
                a.runOnUiThread(() -> Toast.makeText(a, "更新失败：" + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }, "muzi-download").start();
    }

    static byte[] get(String u) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
        c.setConnectTimeout(10000);
        c.setReadTimeout(120000);
        c.setInstanceFollowRedirects(true);
        try {
            InputStream in = c.getInputStream();
            try {
                ByteArrayOutputStream o = new ByteArrayOutputStream();
                byte[] buf = new byte[65536];
                int n;
                while ((n = in.read(buf)) > 0) o.write(buf, 0, n);
                return o.toByteArray();
            } finally { in.close(); }
        } finally { c.disconnect(); }
    }

    private static String hex(byte[] d) {
        StringBuilder s = new StringBuilder();
        for (byte x : d) s.append(String.format("%02x", x));
        return s.toString();
    }
}
