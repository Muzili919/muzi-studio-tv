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
 * Startup update check for the family launcher.
 * Default manifest: https://zhishixingqiu.top/tv/version.json (studio key, package studio.muzi.tv).
 * Override with config.json "updateUrl", or Gradle -PmuziUpdateUrl / -PmuziBaseUrl.
 * Android 6: external-cache file URI for legacy Xiaomi installer; API24+ uses read-only content URI.
 */
public final class Updater {
    public static final String VERSION_URL = "https://zhishixingqiu.top/tv/version.json";
    private static final String TAG = "MuziUpdater";
    public static final int REQUEST_INSTALL=7901, REQUEST_STORAGE=7902;
    private static boolean storagePrompted;

    private static volatile boolean working;
    private static volatile boolean downloading;
    private static long lastCheck;
    private static java.lang.ref.WeakReference<Activity> foreground=new java.lang.ref.WeakReference<>(null);
    private static Intent ready; private static String readyName,readyLabel,readyPackage; private static long readyCode;
    private static boolean showing;
    public static void background(Activity a){if(foreground.get()==a)foreground.clear();}
    public static void onHome(Activity a){foreground=new java.lang.ref.WeakReference<>(a);showReady(a);if(System.currentTimeMillis()-lastCheck>3600000L)checkInternal(a,false);}
    public static void check(Activity a){foreground=new java.lang.ref.WeakReference<>(a);checkInternal(a,true);}
    private static synchronized void checkInternal(final Activity a,final boolean manual){
        if(working||downloading)return;
        if(Build.VERSION.SDK_INT==23&&a.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)!=android.content.pm.PackageManager.PERMISSION_GRANTED){
            if(manual||!storagePrompted){storagePrompted=true;a.runOnUiThread(()->a.requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_STORAGE));}return;
        }
        working=true;lastCheck=System.currentTimeMillis();
        final String manifest=versionUrl(a);
        new Thread(()->{try{JSONObject v=new JSONObject(new String(get(manifest),"UTF-8"));offerUpdates(a,v,manual);}
        catch(Throwable t){Log.w(TAG,"update check failed",t);lastCheck=System.currentTimeMillis()-3540000L;if(manual)a.runOnUiThread(()->Toast.makeText(a,"暂时无法检查更新，请稍后重试",Toast.LENGTH_LONG).show());}
        finally{working=false;}},"muzi-updater").start();
    }
    private static void showReady(Activity a){
        if(ready==null||showing||foreground.get()!=a||a.isFinishing()||a.isDestroyed())return;
        if(a instanceof MainActivity && !"home".equals(((MainActivity)a).screen))return;
        showing=true;final Intent install=ready;
        new AlertDialog.Builder(a).setTitle(readyLabel+"更新已准备好")
         .setMessage("版本 "+readyName+" 已下载并校验。保留桌面布局和应用数据，系统可能需要确认安装。")
         .setPositiveButton("安装更新",(d,w)->{ready=null;try{if(Build.VERSION.SDK_INT>=26&&!a.getPackageManager().canRequestPackageInstalls()){ready=install;a.startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+a.getPackageName())));return;}a.getSharedPreferences("updater",0).edit().putLong("pendingVersion",readyCode).putString("pendingPackage",readyPackage).apply();a.startActivityForResult(install,REQUEST_INSTALL);}catch(Exception e){Log.w(TAG,"cannot open installer",e);ready=install;Toast.makeText(a,"无法打开安装器，请稍后重试",Toast.LENGTH_LONG).show();}})
         .setNegativeButton("明天提醒",(d,w)->{a.getSharedPreferences("updater",0).edit().putLong("deferCode",readyCode).putLong("deferUntil",System.currentTimeMillis()+86400000L).apply();ready=null;})
         .setOnCancelListener(d->{a.getSharedPreferences("updater",0).edit().putLong("deferCode",readyCode).putLong("deferUntil",System.currentTimeMillis()+86400000L).apply();ready=null;}).setOnDismissListener(d->showing=false).show();
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

    static String nextTarget(JSONObject v,int studio,int player,int sdk){
        JSONObject f=v.optJSONObject("fongmi"),s=v.optJSONObject("studio");
        if(player>=0&&f!=null&&f.optLong("versionCode",0)>player&&f.optInt("minSdk",Integer.MAX_VALUE)<=sdk)return "fongmi";
        if(s!=null&&s.optLong("versionCode",0)>studio&&s.optInt("minSdk",Integer.MAX_VALUE)<=sdk)return "studio";
        return "";
    }
    private static void offerUpdates(Activity a,JSONObject v,boolean manual){String target=nextTarget(v,(int)installedCode(a,a.getPackageName()),(int)installedCode(a,"com.fongmi.android.tv"),Build.VERSION.SDK_INT);if(target.isEmpty()){if(manual)a.runOnUiThread(()->Toast.makeText(a,"桌面与播放器已是当前系统的最新版本",Toast.LENGTH_SHORT).show());return;}boolean player=target.equals("fongmi");offer(a,v.optJSONObject(target),player?"com.fongmi.android.tv":a.getPackageName(),player?"木子影界":"木子桌面",manual);}

    private static void offer(final Activity a, final JSONObject o, String pkg, final String label, final boolean manual) {
        if (o == null) return;
        long remote = o.optLong("versionCode", 0), local = installedCode(a, pkg);
        if (remote <= local) { if(manual) a.runOnUiThread(() -> Toast.makeText(a, "当前已是最新版本", Toast.LENGTH_SHORT).show()); return; }
        if (o.optInt("minSdk", Integer.MAX_VALUE) > Build.VERSION.SDK_INT) { if(manual) a.runOnUiThread(() -> Toast.makeText(a, "新版本不兼容当前系统", Toast.LENGTH_LONG).show()); return; }
        final String url = o.optString("url"), sha = o.optString("sha256"), name = o.optString("versionName");
        if(!pkg.equals(o.optString("package",pkg)))throw new IllegalArgumentException("更新包名不匹配");
        android.content.SharedPreferences prefs=a.getSharedPreferences("updater",0);
        if(!manual&&prefs.getLong("deferCode",0)==remote&&prefs.getLong("deferUntil",0)>System.currentTimeMillis())return;
        download(a,url,sha,pkg,name,remote,manual);
    }

    private static void download(final Activity a, final String url, final String sha, final String pkg,final String name,final long remote,final boolean manual) {
        downloading=true;
        if(manual)a.runOnUiThread(()->Toast.makeText(a, "正在后台下载…", Toast.LENGTH_LONG).show());
        new Thread(() -> {
            try {
                if (!url.startsWith("https://") || !sha.matches("(?i)[0-9a-f]{64}"))
                    throw new Exception("更新地址或校验信息无效");
                File dir = downloadDir(a);
                if (!dir.exists() && !dir.mkdirs()) throw new Exception("无法保存安装包");
                File f = new File(dir, pkg + ".apk");
                File partial = new File(dir, pkg + ".download.apk");
                HttpURLConnection c = LegacyTls.open(url);
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
                    validateCandidate(candidate,current,pkg,remote);
                    if (f.exists() && !f.delete()) throw new Exception("无法替换缓存文件");
                    if (!partial.renameTo(f)) throw new Exception("无法保存下载结果");
                } finally { c.disconnect(); partial.delete(); }
                Intent i = installerIntent(a,f);
                a.runOnUiThread(() -> {
                    ready=i;readyName=name;readyCode=remote;readyPackage=pkg;readyLabel=pkg.equals(a.getPackageName())?"木子桌面":"木子影界";Activity currentActivity=foreground.get();if(currentActivity!=null)showReady(currentActivity);
                });
            } catch (final Throwable e) {
                Log.w(TAG,"download failed",e);lastCheck=System.currentTimeMillis()-3540000L;if(manual)a.runOnUiThread(() -> Toast.makeText(a, "更新失败：" + e.getMessage(), Toast.LENGTH_LONG).show());
            } finally { downloading=false; }
        }, "muzi-download").start();
    }

    static File downloadDir(Activity a)throws Exception{
        File base=Build.VERSION.SDK_INT<24?a.getExternalCacheDir():a.getCacheDir();
        if(base==null)throw new Exception("外部存储不可用，暂时无法安装更新");
        return new File(base,"update");
    }
    static Intent installerIntent(Activity a,File f){
        Uri uri=Build.VERSION.SDK_INT<24?Uri.fromFile(f):Uri.parse("content://"+a.getPackageName()+".apk/"+f.getName());
        if(Build.VERSION.SDK_INT<24)f.setReadable(true,false);
        Intent intent=new Intent(Intent.ACTION_INSTALL_PACKAGE).setDataAndType(uri,"application/vnd.android.package-archive")
          .putExtra(Intent.EXTRA_RETURN_RESULT,true).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setClipData(android.content.ClipData.newRawUri("安装包",uri));return intent;
    }
    public static boolean onInstallResult(Activity a,int request,int result,Intent data){
        if(request!=REQUEST_INSTALL)return false;
        android.content.SharedPreferences p=a.getSharedPreferences("updater",0);long expected=p.getLong("pendingVersion",0);
        boolean success=expected>0&&installedCode(a,p.getString("pendingPackage",a.getPackageName()))>=expected;
        String message=success?"更新已安装":result==Activity.RESULT_CANCELED?"安装未完成，可在桌面设置中重新检查更新":"系统未完成安装，请检查安装权限和可用空间";
        int detail=data==null?0:data.getIntExtra("android.intent.extra.INSTALL_RESULT",0);
        p.edit().putString("lastInstallResult",message).putInt("lastInstallCode",detail).remove("pendingVersion").remove("pendingPackage").apply();
        Log.i(TAG,"install result="+result+" detail="+detail+" versionVerified="+success);
        Toast.makeText(a,message,Toast.LENGTH_LONG).show();lastCheck=0;if(success)checkInternal(a,false);return true;
    }
    static void validateCandidate(PackageInfo candidate,PackageInfo current,String pkg,long remote)throws Exception {
        if(candidate==null||!pkg.equals(candidate.packageName)||candidate.versionCode<=current.versionCode||candidate.versionCode!=remote)throw new Exception("安装包身份或版本不匹配");
        if(candidate.signatures==null||candidate.signatures.length==0||!java.util.Arrays.equals(candidate.signatures,current.signatures))throw new Exception("签名不同，已阻止覆盖安装");
        if(Build.VERSION.SDK_INT>=24&&candidate.applicationInfo.minSdkVersion>Build.VERSION.SDK_INT)throw new Exception("新版本不兼容当前系统");
    }
    static byte[] get(String u) throws Exception {
        if(!u.startsWith("https://"))throw new Exception("更新信息必须使用HTTPS");
        HttpURLConnection c = LegacyTls.open(u);
        c.setConnectTimeout(10000);
        c.setReadTimeout(120000);
        c.setInstanceFollowRedirects(true);
        try {
            InputStream in = c.getInputStream();
            try {
                ByteArrayOutputStream o = new ByteArrayOutputStream();
                byte[] buf = new byte[65536];
                int n;
                while ((n = in.read(buf)) > 0) {o.write(buf, 0, n);if(o.size()>1048576)throw new Exception("更新信息过大");}
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
