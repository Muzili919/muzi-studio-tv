package studio.muzi.tv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import org.json.JSONObject;

/** Checks https://zhishixingqiu.top/tv/version.json and offers to install newer studio / FongMi APKs. */
public final class Updater {
    public static final String VERSION_URL = "https://zhishixingqiu.top/tv/version.json";
    private static boolean checked;

    public static void check(final Activity a) {
        if (checked) return;
        checked = true;
        new Thread(() -> {
            try {
                JSONObject v = new JSONObject(new String(get(VERSION_URL), "UTF-8"));
                offer(a, v.optJSONObject("studio"), a.getPackageName(), "木子电视");
                offer(a, v.optJSONObject("fongmi"), "com.fongmi.android.tv", "FongMi 影视");
            } catch (Throwable ignored) { }
        }, "muzi-updater").start();
    }

    private static long installedCode(Activity a, String pkg) {
        try {
            PackageInfo p = a.getPackageManager().getPackageInfo(pkg, 0);
            return Build.VERSION.SDK_INT >= 28 ? p.getLongVersionCode() : p.versionCode;
        } catch (Exception e) { return -1; }
    }

    private static void offer(final Activity a, final JSONObject o, String pkg, final String label) {
        if (o == null) return;
        long remote = o.optLong("versionCode", 0), local = installedCode(a, pkg);
        if (remote <= local) return;
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
                byte[] b = get(url);
                if (!sha.isEmpty() && !sha.equalsIgnoreCase(hex(MessageDigest.getInstance("SHA-256").digest(b))))
                    throw new Exception("校验失败");
                File dir = Build.VERSION.SDK_INT >= 24 ? new File(a.getCacheDir(), "update") : a.getExternalCacheDir();
                dir.mkdirs();
                File f = new File(dir, pkg + ".apk");
                try (FileOutputStream out = new FileOutputStream(f)) { out.write(b); }
                Uri uri;
                if (Build.VERSION.SDK_INT >= 24) uri = Uri.parse("content://" + a.getPackageName() + ".apk/" + f.getName());
                else { f.setReadable(true, false); uri = Uri.fromFile(f); }
                Intent i = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                a.runOnUiThread(() -> a.startActivity(i));
            } catch (final Throwable e) {
                a.runOnUiThread(() -> Toast.makeText(a, "更新失败：" + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }, "muzi-download").start();
    }

    static byte[] get(String u) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
        c.setConnectTimeout(10000); c.setReadTimeout(60000); c.setInstanceFollowRedirects(true);
        try (InputStream in = c.getInputStream()) {
            ByteArrayOutputStream o = new ByteArrayOutputStream(); byte[] buf = new byte[65536]; int n;
            while ((n = in.read(buf)) > 0) o.write(buf, 0, n);
            return o.toByteArray();
        } finally { c.disconnect(); }
    }

    private static String hex(byte[] d) { StringBuilder s = new StringBuilder(); for (byte x : d) s.append(String.format("%02x", x)); return s.toString(); }
}
