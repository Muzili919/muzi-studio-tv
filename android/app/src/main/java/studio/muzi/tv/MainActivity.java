package studio.muzi.tv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.Timeline;
import androidx.media3.common.Tracks;
import androidx.media3.common.VideoSize;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;
import studio.muzi.tv.HighlightMap;
import studio.muzi.tv.HighlightStore;
import studio.muzi.tv.MainActivity;

/* loaded from: classes3.dex */
@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public class MainActivity extends Activity {
    static final String TAG = "MuziTV";
    LinearLayout biliBar;
    WebView biliWeb;
    LinearLayout content;
    LinearLayout controls;
    JSONObject currentDetail;
    Runnable hideBlurb;
    LinearLayout highlightActions;
    TextView highlightBlurb;
    DesktopView home;
    LinearLayout markerChips;
    HorizontalScrollView markerScroller;
    Button pauseButton;
    TextView playStatus;
    LinearLayout posters;
    long previewDur;
    boolean previewPlaying;
    long previewPos;
    Runnable previewTick;
    EditText searchBox;
    MarkerSeekBar seek;
    LinearLayout shelfNav;
    Button speedButton;
    TextView status;
    Runnable ticker;
    TextView timeLabel;
    ExoPlayer video;
    Handler ui = new Handler();
    ExecutorService io = Executors.newSingleThreadExecutor();
    String server = "";
    String password = "";
    String cookie = "";
    String cornerLabel = "";
    String screen = "home";
    String query = "";
    JSONArray sources = new JSONArray();
    JSONArray results = new JSONArray();
    JSONArray episodes = new JSONArray();
    int currentEpisode = 0;
    int selected = 0;
    static final int REQ_PICK_APK = 4101;
    float playbackSpeed = 1.0f;
    ExecutorService images = Executors.newFixedThreadPool(4);
    int shelfGeneration = 0;
    int libraryTab = 0;
    boolean liveMode = false;
    boolean switching = false;
    boolean directLive = false;
    long resumeAt = 0;
    String activeTitle = "";
    String liveRegion = "cn";
    int resumeEpisode = 0;
    JSONArray channels = new JSONArray();
    List<Highlight> activeHighlights = new ArrayList();
    int[] markerTimes = new int[0];
    int pinnedHighlight = -1;
    ExecutorService side = Executors.newSingleThreadExecutor();
    ExecutorService cms = Executors.newFixedThreadPool(4);
    int duration = 0;
    int position = 0;
    boolean playing = false;
    long loadGeneration = 0;

    @Override // android.app.Activity
    public void onCreate(Bundle b) {
        super.onCreate(b);
        // Update checks run while the native desktop is foreground.
        getWindow().setFlags(1024, 1024);
        getWindow().getDecorView().setSystemUiVisibility(5894);
        readConfig();
        initSources();
        new Thread(MultiSource::applyRank).start();
        this.libraryTab = -1;
        showHome();
    }

    /** 客厅 = 米家激光投影 MiProjA1；其他（MiTV4A）= 卧室。 */
    String roomName() {
        String model = android.os.Build.MODEL == null ? "" : android.os.Build.MODEL;
        return model.toLowerCase(Locale.ROOT).contains("proj") ? "客厅" : "卧室";
    }

    String defaultCornerLabel() {
        return "muzi.studio";
    }

    void readConfig() {
        this.cornerLabel = defaultCornerLabel();
        try {
            JSONObject c = new JSONObject(read(new FileInputStream(new File(getFilesDir(), "config.json"))));
            this.cornerLabel = c.optString("cornerLabel", "").trim();
            if (!"muzi.studio".equals(this.cornerLabel)) {
                this.cornerLabel = defaultCornerLabel();
            }
            this.server = c.getString("server").replaceAll("/+$", "");
            this.password = c.getString("password");
        } catch (Exception e) {
            Log.w(TAG, "Server configuration not provisioned");
        }
    }

    @Override // android.app.Activity
    public void setContentView(View view) {
        String label = this.cornerLabel == null ? "" : this.cornerLabel.trim();
        // 主页画布自己画品牌，避免重复角标
        if (label.isEmpty() || view instanceof Home) {
            super.setContentView(view);
            return;
        }
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.addView(view, new FrameLayout.LayoutParams(-1, -1));
        TextView badge = new TextView(this);
        badge.setText(label);
        badge.setTextSize(13.0f);
        badge.setTextColor(-1);
        badge.setSingleLine(true);
        badge.setMaxWidth(dp(480.0f));
        GradientDrawable plate = new GradientDrawable();
        plate.setColor(-871360473);
        plate.setCornerRadius(dp(6.0f));
        badge.setBackground(plate);
        badge.setPadding(dp(10.0f), dp(5.0f), dp(10.0f), dp(5.0f));
        badge.setFocusable(false);
        badge.setImportantForAccessibility(2);
        // 右下角，避免压住页面标题和右上角按钮
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, 8388693);
        lp.setMargins(0, 0, dp(20.0f), dp(10.0f));
        frameLayout.addView(badge, lp);
        super.setContentView(frameLayout);
    }

    void initSources() {
        this.sources = new JSONArray();
        String[][] list = {new String[]{"bf", "暴风资源", "https://bfzyapi.com/api.php/provide/vod"}, new String[]{"ff", "非凡资源", "https://api.ffzyapi.com/api.php/provide/vod"}, new String[]{"lz", "量子资源", "https://cj.lziapi.com/api.php/provide/vod"}};
        for (String[] v : list) {
            try {
                this.sources.put(new JSONObject().put("key", v[0]).put("name", v[1]).put("url", v[2]));
            } catch (Exception e) {
            }
        }
    }

    int dp(float v) {
        return (int) ((getResources().getDisplayMetrics().density * v) + 0.5f);
    }

    TextView text(String s, int size) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(-1);
        t.setPadding(dp(8.0f), dp(8.0f), dp(8.0f), dp(8.0f));
        return t;
    }

    Drawable bg(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(8.0f));
        return d;
    }

    Button button(String label, final Runnable action) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(label);
        b.setTextSize(19.0f);
        b.setTextColor(-1);
        b.setPadding(dp(18.0f), dp(12.0f), dp(18.0f), dp(12.0f));
        b.setBackground(bg(Color.rgb(34, 46, 65)));
        b.setOnFocusChangeListener(new View.OnFocusChangeListener() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda40
            @Override // android.view.View.OnFocusChangeListener
            public final void onFocusChange(View view, boolean z) {
                MainActivity.this.m1652lambda$button$0$studiomuzitvMainActivity(view, z);
            }
        });
        b.setOnClickListener(new View.OnClickListener() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda41
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                action.run();
            }
        });
        return b;
    }

    /* renamed from: lambda$button$0$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1652lambda$button$0$studiomuzitvMainActivity(View v, boolean f) {
        int i;
        int i2;
        int i3;
        if (f) {
            i = 109;
            i2 = 141;
            i3 = 86;
        } else {
            i = 46;
            i2 = 65;
            i3 = 34;
        }
        v.setBackground(bg(Color.rgb(i3, i, i2)));
        v.setScaleX(f ? 1.02f : 1.0f);
        v.setScaleY(f ? 1.02f : 1.0f);
    }

    void addButton(LinearLayout p, String s, Runnable r) {
        Button b = button(s, r);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(6.0f), dp(5.0f), dp(6.0f), dp(5.0f));
        p.addView(b, lp);
    }

    static android.graphics.Bitmap wallpaper;

    LinearLayout page(String title) {
        stopVideo();
        this.loadGeneration++;
        this.screen = "page";
        LinearLayout base = new LinearLayout(this);
        base.setOrientation(android.widget.LinearLayout.VERTICAL);
        base.setPadding(dp(40.0f), dp(16.0f), dp(40.0f), dp(12.0f));
        base.setBackgroundColor(Color.rgb(10, 17, 28));
        try {
            if (wallpaper == null) wallpaper = BitmapFactory.decodeStream(getAssets().open("galaxy.jpg"));
            android.graphics.drawable.BitmapDrawable wd = new android.graphics.drawable.BitmapDrawable(getResources(), wallpaper);
            base.setBackground(wd);
        } catch (Exception ignored) { }
        LinearLayout top = new LinearLayout(this);
        TextView heading = text(title, 28);
        top.addView(heading, new LinearLayout.LayoutParams(0, -2, 1.0f));
        top.addView(button("返回", new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda48
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.onBackPressed();
            }
        }));
        base.addView(top);
        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        scroll.setClipChildren(false);
        this.content = new LinearLayout(this);
        this.content.setOrientation(android.widget.LinearLayout.VERTICAL);
        this.content.setClipChildren(false);
        scroll.addView(this.content);
        base.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        this.status = text("", 15);
        base.addView(this.status);
        setContentView(base);
        return base;
    }

    void showHome() {
        stopVideo();
        this.loadGeneration++;
        this.screen = "home";
        if (this.selected < 0 || this.selected > 3) this.selected = 0;
        this.home = new DesktopView(this);
        setContentView(this.home);
        this.home.requestFocus();
        this.home.resume();
    }

    @Override // android.app.Activity
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        showHome();
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (Updater.onInstallResult(this, request, result, data)) return;
        if (request != REQ_PICK_APK || result != RESULT_OK || data == null || data.getData() == null) return;
        installPickedApk(data.getData());
    }

    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){
        super.onRequestPermissionsResult(request,permissions,results);
        if(request==Updater.REQUEST_STORAGE&&results.length>0&&results[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)Updater.check(this);
    }

    void openLibreWebsite() {
        openLibreWebsite("/");
    }

    void openLibreWebsite(String path) {
        try {
            Intent i = new Intent("android.intent.action.VIEW", Uri.parse("https://tv.bagiinlink.eu.org" + path));
            i.setPackage("com.phlox.tvwebbrowser");
            startActivity(i);
        } catch (Exception e) {
            toast("LibreTV 网页浏览器暂时无法打开。");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void launch(String pkg) {
        Intent i = getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) {
            i = getPackageManager().getLaunchIntentForPackage(pkg);
        }
        if (i == null) {
            toast("尚未安装或此应用没有可用入口");
            return;
        }
        try {
            startActivity(i);
        } catch (Exception e) {
            toast("无法打开此应用");
        }
    }

    void openFongMi() {
        try {
            Intent i = getPackageManager().getLeanbackLaunchIntentForPackage("com.fongmi.android.tv");
            if (i == null) {
                Intent explicit = new Intent("android.intent.action.MAIN");
                explicit.setClassName("com.fongmi.android.tv", "com.fongmi.android.tv.ui.activity.HomeActivity");
                explicit.addCategory("android.intent.category.LEANBACK_LAUNCHER");
                i = getPackageManager().resolveActivity(explicit, 0) != null ? explicit : getPackageManager().getLaunchIntentForPackage("com.fongmi.android.tv");
            }
            if (i == null) {
                Toast.makeText(this, "尚未安装木子映界", Toast.LENGTH_SHORT).show();
            } else {
                i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            }
        } catch (Exception e) {
            Toast.makeText(this, "尚未安装木子映界", Toast.LENGTH_SHORT).show();
        }
    }

    void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    /** Family dock: 0 蜂蜜, 1 YouTube, 2 全部应用, 3 设置. Live / HDMI / Tencent / Bilibili are not slots. */
    void openHomeItem(int n) {
        if (n < 0 || n > 3) n = 0;
        this.selected = n;
        switch (n) {
            case 0:
                openFongMi();
                break;
            case 1:
                chooseYoutube();
                break;
            case 2:
                showApps();
                break;
            case 3:
                openSystemSettings();
                break;
            default:
                break;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void openExternal(String url) {
        try {
            Intent i = new Intent("android.intent.action.VIEW", Uri.parse(url));
            i.setPackage("com.phlox.tvwebbrowser");
            startActivity(i);
        } catch (Exception e) {
            try {
                startActivity(new Intent("android.intent.action.VIEW", Uri.parse(url)));
            } catch (Exception e2) {
                toast("没有可用的浏览器。可安装云视听小电视，或使用电视自带浏览器。");
            }
        }
    }

    void chooseYoutube() {
        String[] pkgs = {
            "com.google.android.youtube.tv",
            "com.google.android.youtube",
            "com.teamsmart.videomanager.tv",
            "org.smarttube.stable",
            "com.liskovsoft.smartyoutubetv2",
            "com.liskovsoft.smarttubetv"
        };
        for (String p : pkgs) {
            try {
                getPackageManager().getPackageInfo(p, 0);
                launch(p);
                return;
            } catch (Exception e) {
            }
        }
        String[] cats = {"android.intent.category.LEANBACK_LAUNCHER", "android.intent.category.LAUNCHER"};
        for (String cat : cats) {
            Intent q = new Intent("android.intent.action.MAIN").addCategory(cat);
            for (ResolveInfo r : getPackageManager().queryIntentActivities(q, 0)) {
                String pkg = r.activityInfo == null ? "" : r.activityInfo.packageName;
                if (pkg.toLowerCase(Locale.ROOT).contains("youtube")) {
                    launch(pkg);
                    return;
                }
            }
        }
        toast("尚未安装 YouTube");
    }

    /** Opens a file manager when one is installed, otherwise the system package picker. */
    void openInstallApps() {
        String[] pkgs = {
            "com.xiaomi.mitv.mediaexplorer",
            "com.xiaomi.mitv.filemanager",
            "com.android.fileexplorer",
            "com.estrongs.android.pop",
            "com.estrongs.android.pop.pro",
            "com.cxinventor.file.explorer",
            "com.alphainventor.filemanager",
            "com.amaze.filemanager",
            "com.lonelycatgames.Xplore",
            "com.android.documentsui",
            "com.google.android.documentsui",
            "com.shafa.market",
            "com.softwinner.TvdFileManager"
        };
        for (String p : pkgs) {
            if (tryLaunch(p)) return;
        }
        String[] cats = {"android.intent.category.LEANBACK_LAUNCHER", "android.intent.category.LAUNCHER"};
        for (String cat : cats) {
            Intent q = new Intent("android.intent.action.MAIN").addCategory(cat);
            for (ResolveInfo r : getPackageManager().queryIntentActivities(q, 0)) {
                if (r.activityInfo == null) continue;
                String pkg = r.activityInfo.packageName;
                String label = "";
                try { label = r.loadLabel(getPackageManager()).toString(); } catch (Exception ignored) { }
                String blob = (pkg + " " + label).toLowerCase(Locale.ROOT);
                boolean fileApp = blob.contains("filemanager") || blob.contains("fileexplorer") || blob.contains("documentsui")
                        || label.contains("文件管理") || label.contains("文件浏览") || label.contains("沙发") || label.contains("安装器");
                if (fileApp && tryLaunch(pkg)) return;
            }
        }
        try {
            Intent pick = new Intent("android.intent.action.GET_CONTENT");
            pick.addCategory("android.intent.category.OPENABLE");
            pick.setType("application/vnd.android.package-archive");
            startActivityForResult(pick, REQ_PICK_APK);
        } catch (Exception e) {
            try {
                Intent pick = new Intent("android.intent.action.GET_CONTENT");
                pick.addCategory("android.intent.category.OPENABLE");
                pick.setType("*/*");
                startActivityForResult(Intent.createChooser(pick, "选择安装包"), REQ_PICK_APK);
            } catch (Exception e2) {
                toast("没有可用的文件管理或安装器");
            }
        }
    }

    boolean tryLaunch(String pkg) {
        if (pkg == null || pkg.equals(getPackageName())) return false;
        Intent i = getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i == null) return false;
        try {
            startActivity(i);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    void installPickedApk(Uri uri) {
        try {
            if (Build.VERSION.SDK_INT >= 24) {
                Intent view = new Intent("android.intent.action.VIEW");
                view.setDataAndType(uri, "application/vnd.android.package-archive");
                view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(view);
                return;
            }
            File dir = getExternalCacheDir();
            if (dir == null) dir = getCacheDir();
            File out = new File(dir, "picked-install.apk");
            InputStream in = getContentResolver().openInputStream(uri);
            if (in == null) throw new IOException("empty");
            FileOutputStream fos = new FileOutputStream(out);
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) fos.write(buf, 0, n);
            fos.close();
            in.close();
            out.setReadable(true, false);
            dir.setReadable(true, false);
            dir.setExecutable(true, false);
            Intent view = new Intent("android.intent.action.VIEW");
            view.setDataAndType(Uri.fromFile(out), "application/vnd.android.package-archive");
            view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(view);
        } catch (Exception e) {
            toast("无法打开安装器");
        }
    }

    /* renamed from: lambda$showApps$3$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ int m1701lambda$showApps$3$studiomuzitvMainActivity(ResolveInfo a, ResolveInfo b) {
        return a.loadLabel(getPackageManager()).toString().compareTo(b.loadLabel(getPackageManager()).toString());
    }

    /* renamed from: lambda$showApps$4$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1702lambda$showApps$4$studiomuzitvMainActivity(ResolveInfo r) {
        launch(r.activityInfo.packageName);
    }

    void showApps() {
        page("全部应用");
        this.screen = "apps";
        addButton(this.content, "当贝市场", () -> {
            Intent market = getPackageManager().getLaunchIntentForPackage("com.dangbeimarket");
            if (market != null) startActivity(market);
            else openExternal("https://www.dangbei.com/");
        });
        addButton(this.content, "检查木子桌面更新", () -> Updater.check(this));
        addButton(this.content, "开发者设置 / ADB", () -> {
            try { startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)); }
            catch (Exception e) { toast("此电视未开放开发者设置，请按品牌说明开启；当贝市场不能直接解锁 ADB。"); }
        });
        addButton(this.content, "安装应用", new Runnable() {
            @Override
            public void run() {
                openInstallApps();
            }
        });
        this.status.setText("「安装应用」固定在最上方，下面是已安装的应用。");
        ArrayList<ResolveInfo> all = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        String[] strArr = {"android.intent.category.LEANBACK_LAUNCHER", "android.intent.category.LAUNCHER"};
        for (int i = 0; i < 2; i++) {
            String category = strArr[i];
            Intent q = new Intent("android.intent.action.MAIN").addCategory(category);
            for (ResolveInfo r : getPackageManager().queryIntentActivities(q, 0)) {
                if (seen.add(r.activityInfo.packageName)) {
                    all.add(r);
                }
            }
        }
        Collections.sort(all, new Comparator() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda33
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return MainActivity.this.m1701lambda$showApps$3$studiomuzitvMainActivity((ResolveInfo) obj, (ResolveInfo) obj2);
            }
        });
        Iterator<ResolveInfo> it = all.iterator();
        while (it.hasNext()) {
            final ResolveInfo r2 = it.next();
            addButton(this.content, r2.loadLabel(getPackageManager()).toString(), new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda44
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m1702lambda$showApps$4$studiomuzitvMainActivity(r2);
                }
            });
        }
    }

    void openSystemSettings() {
        try {
            startActivity(new Intent("android.settings.SETTINGS"));
        } catch (Exception e) {
            launch("com.xiaomi.mitv.settings");
        }
    }

    /* renamed from: lambda$showSettings$5$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1706lambda$showSettings$5$studiomuzitvMainActivity() {
        openSystemSettings();
    }

    /* renamed from: lambda$showSettings$9$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1710lambda$showSettings$9$studiomuzitvMainActivity() {
        BiliScreens.show(this);
    }

    void showSettings() {
        page("设置");
        this.screen = "settings";
        addButton(this.content, "电视系统设置", new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda49
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m1706lambda$showSettings$5$studiomuzitvMainActivity();
            }
        });
        addButton(this.content, "LibreTV 连接设置", new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda50
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m1707lambda$showSettings$6$studiomuzitvMainActivity();
            }
        });
        addButton(this.content, "角落标签" + (this.cornerLabel.isEmpty() ? "" : " · " + this.cornerLabel), new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda51
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m1708lambda$showSettings$7$studiomuzitvMainActivity();
            }
        });
        addButton(this.content, "精彩标记", new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda52
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m1709lambda$showSettings$8$studiomuzitvMainActivity();
            }
        });
        addButton(this.content, "哔哩哔哩", new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda53
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m1710lambda$showSettings$9$studiomuzitvMainActivity();
            }
        });
        this.content.addView(text("星空桌面 · 主页键返回这里。旧桌面入口可通过电脑恢复。", 18));
        this.content.addView(text("银河照片：ESO/S. Brunier · CC BY 4.0\nwww.eso.org/public/images/eso0932a/\n显示裁切及暗部遮罩", 15));
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            this.content.addView(text("版本 " + info.versionName + " (" + info.versionCode + ")", 15));
        } catch (Exception e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: configure, reason: merged with bridge method [inline-methods] */
    public void m1707lambda$showSettings$6$studiomuzitvMainActivity() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(android.widget.LinearLayout.VERTICAL);
        final EditText host = new EditText(this);
        host.setSingleLine(true);
        host.setText(this.server);
        host.setHint("LibreTV 服务地址");
        final EditText pass = new EditText(this);
        pass.setSingleLine(true);
        pass.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        pass.setHint("访问密码（留空保留）");
        l.addView(host);
        l.addView(pass);
        new AlertDialog.Builder(this).setTitle("LibreTV 连接").setView(l).setNegativeButton("取消", (DialogInterface.OnClickListener) null).setPositiveButton("保存", new DialogInterface.OnClickListener() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda55
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.m1655lambda$configure$10$studiomuzitvMainActivity(host, pass, dialogInterface, i);
            }
        }).show();
    }

    /* renamed from: lambda$configure$10$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1655lambda$configure$10$studiomuzitvMainActivity(EditText host, EditText pass, DialogInterface d, int w) {
        String h = host.getText().toString().trim();
        if (!h.startsWith("http://") && !h.startsWith("https://")) {
            toast("请输入 http 或 https 地址");
            return;
        }
        this.server = h.replaceAll("/+$", "");
        if (pass.length() > 0) {
            this.password = pass.getText().toString();
        }
        this.cookie = "";
        try {
            JSONObject j = new JSONObject();
            try {
                j = new JSONObject(read(openFileInput("config.json")));
            } catch (Exception e) {
            }
            j.put("server", this.server);
            j.put("password", this.password);
            FileOutputStream f = openFileOutput("config.json", 0);
            try {
                f.write(j.toString().getBytes("UTF-8"));
                if (f != null) {
                    f.close();
                }
                toast("连接设置已保存");
            } finally {
            }
        } catch (Exception e2) {
            toast("保存失败");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: configureCorner, reason: merged with bridge method [inline-methods] */
    public void m1708lambda$showSettings$7$studiomuzitvMainActivity() {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(this.cornerLabel);
        input.setHint("muzi.studio");
        input.setSelection(input.getText().length());
        new AlertDialog.Builder(this).setTitle("角落标签").setMessage("角落一直显示品牌。默认 muzi.studio。留空则不显示。").setView(input).setNegativeButton("取消", (DialogInterface.OnClickListener) null).setPositiveButton("保存", new DialogInterface.OnClickListener() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda26
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.m1657lambda$configureCorner$12$studiomuzitvMainActivity(input, dialogInterface, i);
            }
        }).show();
    }

    /* renamed from: lambda$configureCorner$11$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1656lambda$configureCorner$11$studiomuzitvMainActivity() {
        if ("settings".equals(this.screen)) {
            showSettings();
        }
    }

    /* renamed from: lambda$configureCorner$12$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1657lambda$configureCorner$12$studiomuzitvMainActivity(EditText input, DialogInterface d, int w) {
        this.cornerLabel = input.getText().toString().trim();
        try {
            JSONObject j = new JSONObject();
            try {
                j = new JSONObject(read(openFileInput("config.json")));
            } catch (Exception e) {
            }
            if (this.cornerLabel.isEmpty()) {
                j.remove("cornerLabel");
            } else {
                j.put("cornerLabel", this.cornerLabel);
            }
            FileOutputStream f = openFileOutput("config.json", 0);
            try {
                f.write(j.toString().getBytes("UTF-8"));
                if (f != null) {
                    f.close();
                }
                toast(this.cornerLabel.isEmpty() ? "已隐藏角落标签" : "角落标签已保存");
                this.ui.post(new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda12
                    @Override // java.lang.Runnable
                    public final void run() {
                        MainActivity.this.m1656lambda$configureCorner$11$studiomuzitvMainActivity();
                    }
                });
            } finally {
            }
        } catch (Exception e2) {
            toast("保存失败");
        }
    }

    /* renamed from: lambda$showInputs$13$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1704lambda$showInputs$13$studiomuzitvMainActivity(TvInputInfo x) {
        try {
            startActivity(new Intent("android.intent.action.VIEW", TvContract.buildChannelUriForPassthroughInput(x.getId())));
        } catch (Exception e) {
            toast("此输入需要从小米原桌面切换");
        }
    }

    /* renamed from: lambda$showInputs$14$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1705lambda$showInputs$14$studiomuzitvMainActivity() {
        launch("com.mitv.tvhome");
    }

    void showInputs() {
        page("外接设备");
        this.screen = "inputs";
        TvInputManager m = (TvInputManager) getSystemService(android.content.Context.TV_INPUT_SERVICE);
        int count = 0;
        if (m != null) {
            for (final TvInputInfo x : m.getTvInputList()) {
                if (x.isPassthroughInput()) {
                    count++;
                    addButton(this.content, x.loadLabel(this).toString(), new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda13
                        @Override // java.lang.Runnable
                        public final void run() {
                            MainActivity.this.m1704lambda$showInputs$13$studiomuzitvMainActivity(x);
                        }
                    });
                }
            }
        }
        if (count == 0) {
            this.content.addView(text("系统未公开输入源列表，请从原小米桌面选择信号源。", 19));
        }
        addButton(this.content, "打开小米原桌面", new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda14
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m1705lambda$showInputs$14$studiomuzitvMainActivity();
            }
        });
    }

    void showLive() {
        page("电视直播");
        this.screen = "live";
        this.directLive = false;
        this.content.addView(text("央视和主要卫视。地址来自安装包里的精选表，不是 iptv-org 全表。", 16));
        DiscoverScreens.fillLive(this, null);
        this.status.setText("左右选频道，上下换行，确认播放");
    }

    /* renamed from: lambda$loadLive$15$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1665lambda$loadLive$15$studiomuzitvMainActivity(long gen, JSONArray list) {
        if (gen != this.loadGeneration) {
            return;
        }
        this.channels = list;
        renderChannels("");
    }

    /* renamed from: lambda$loadLive$16$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1666lambda$loadLive$16$studiomuzitvMainActivity(String region, final long gen) {
        try {
            ensureAuth();
            JSONObject out = request("GET", "/api/live/playlist?url=" + enc("https://iptv-org.github.io/iptv/countries/" + region + ".m3u"), null);
            final JSONArray list = out.getJSONArray("channels");
            this.ui.post(new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m1665lambda$loadLive$15$studiomuzitvMainActivity(gen, list);
                }
            });
        } catch (Exception e) {
            report(gen, "频道加载失败：" + safe(e));
        }
    }

    void loadLive(final String region) {
        this.liveRegion = region;
        this.directLive = false;
        page("电视直播 · 频道");
        this.screen = "channels";
        this.status.setText("正在加载频道…");
        final long gen = this.loadGeneration;
        this.io.execute(new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda16
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m1666lambda$loadLive$16$studiomuzitvMainActivity(region, gen);
            }
        });
    }

    /* renamed from: lambda$renderChannels$17$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1693lambda$renderChannels$17$studiomuzitvMainActivity(EditText input) {
        ((InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(input.getWindowToken(), 0);
        renderChannels(input.getText().toString());
    }

    /* renamed from: lambda$renderChannels$18$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1694lambda$renderChannels$18$studiomuzitvMainActivity(JSONObject ch) {
        this.directLive = false;
        this.liveMode = true;
        this.resumeAt = 0L;
        play(ch.optString("url"), ch.optString("name"));
    }

    void renderChannels(String filter) {
        this.content.removeAllViews();
        LinearLayout row = new LinearLayout(this);
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setTextColor(-1);
        input.setHintTextColor(-6444612);
        input.setHint("搜索频道，例如 CCTV");
        input.setText(filter);
        row.addView(input, new LinearLayout.LayoutParams(0, dp(60.0f), 1.0f));
        row.addView(button("筛选", new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m1693lambda$renderChannels$17$studiomuzitvMainActivity(input);
            }
        }));
        this.content.addView(row);
        int count = 0;
        for (int i = 0; i < this.channels.length(); i++) {
            final JSONObject ch = this.channels.optJSONObject(i);
            if (ch != null && ch.optString("name").toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT))) {
                count++;
                addButton(this.content, ch.optString("name") + " · " + ch.optString("group"), new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        MainActivity.this.m1694lambda$renderChannels$18$studiomuzitvMainActivity(ch);
                    }
                });
            }
        }
        this.status.setText("显示 " + count + " 个频道 · 确认播放 · 返回切换地区");
        if (this.content.getChildCount() > 1) {
            this.content.getChildAt(1).requestFocus();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: showLibrary, reason: merged with bridge method [inline-methods] */
    public void m1698lambda$search$28$studiomuzitvMainActivity() {
        DiscoverScreens.openLibrary(this);
    }

    /* renamed from: lambda$loadShelf$19$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1669lambda$loadShelf$19$studiomuzitvMainActivity(View v, boolean f) {
        v.setBackground(bg(f ? -11375728 : -14931653));
    }

    /* renamed from: lambda$loadShelf$20$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1670lambda$loadShelf$20$studiomuzitvMainActivity(String title, View v) {
        search(title);
    }

    /* renamed from: lambda$loadShelf$21$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1671lambda$loadShelf$21$studiomuzitvMainActivity(long gen, int shelf, JSONArray list) {
        if (gen == this.loadGeneration && shelf == this.shelfGeneration) {
            LinearLayout cards = null;
            int i = 0;
            while (i < list.length()) {
                if (i % 6 == 0) {
                    cards = new LinearLayout(this);
                    this.posters.addView(cards);
                }
                LinearLayout cards2 = cards;
                JSONObject item = list.optJSONObject(i);
                final String title = item.optString("title");
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(android.widget.LinearLayout.VERTICAL);
                card.setPadding(dp(5.0f), dp(5.0f), dp(5.0f), dp(5.0f));
                card.setBackground(bg(-14931653));
                card.setFocusable(true);
                card.setOnFocusChangeListener(new View.OnFocusChangeListener() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda42
                    @Override // android.view.View.OnFocusChangeListener
                    public final void onFocusChange(View view, boolean z) {
                        MainActivity.this.m1669lambda$loadShelf$19$studiomuzitvMainActivity(view, z);
                    }
                });
                card.setOnClickListener(new View.OnClickListener() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda43
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view) {
                        MainActivity.this.m1670lambda$loadShelf$20$studiomuzitvMainActivity(title, view);
                    }
                });
                ImageView image = new ImageView(this);
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                image.setBackgroundColor(-14273206);
                card.addView(image, new LinearLayout.LayoutParams(-1, dp(145.0f)));
                TextView label = text(title, 16);
                label.setMaxLines(2);
                card.addView(label);
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dp(215.0f), 1.0f);
                cp.setMargins(dp(4.0f), dp(4.0f), dp(4.0f), dp(4.0f));
                cards2.addView(card, cp);
                loadPoster(item.optString("cover"), image, gen, shelf);
                i++;
                cards = cards2;
            }
            this.status.setText("选择海报搜索片源 · 遥控器方向键选择，确认打开");
        }
    }

    /* renamed from: lambda$loadShelf$22$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1672lambda$loadShelf$22$studiomuzitvMainActivity(String type, String tag, final long gen, final int shelf) {
        try {
            ensureAuth();
            final JSONArray list = request("GET", "/api/douban?type=" + type + "&tag=" + enc(tag) + "&pageSize=12", null).getJSONArray("items");
            this.ui.post(new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda28
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m1671lambda$loadShelf$21$studiomuzitvMainActivity(gen, shelf, list);
                }
            });
        } catch (Exception e) {
            report(gen, "推荐加载失败，仍可使用顶部搜索：" + safe(e));
        }
    }

    void loadShelf(int category) {
        final int shelf = this.shelfGeneration + 1;
        this.shelfGeneration = shelf;
        final long gen = this.loadGeneration;
        this.posters.removeAllViews();
        String[] titles = {"热门电影", "热门电视剧", "综艺精选", "动漫精选"};
        this.posters.addView(text(titles[category], 22));
        this.status.setText("正在加载海报…");
        final String type = category == 0 ? CmsCatalog.MOVIE : "tv";
        final String tag = category == 2 ? "综艺" : category == 3 ? "动画" : "热门";
        this.io.execute(new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m1672lambda$loadShelf$22$studiomuzitvMainActivity(type, tag, gen, shelf);
            }
        });
    }

    static final class Grab {
        final byte[] body;
        final int code;

        Grab(int code, byte[] body) {
            this.code = code;
            this.body = body == null ? new byte[0] : body;
        }
    }

    /* renamed from: lambda$loadPoster$23$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1667lambda$loadPoster$23$studiomuzitvMainActivity(long gen, int shelf, ImageView image, Bitmap bitmap) {
        if (gen == this.loadGeneration && shelf == this.shelfGeneration && image.isAttachedToWindow()) {
            if (bitmap != null) {
                image.setImageBitmap(bitmap);
            }
        } else if (bitmap != null) {
            bitmap.recycle();
        }
    }

    /* renamed from: lambda$loadPoster$24$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1668lambda$loadPoster$24$studiomuzitvMainActivity(String pic, final long gen, final int shelf, final ImageView image) {
        byte[] bytes;
        String reason;
        String str;
        String str2 = "";
        boolean useProxy = (this.server == null || this.server.isEmpty()) ? false : true;
        byte[] bytes2 = null;
        String reason2 = "empty";
        if (useProxy) {
            for (int attempt = 0; attempt < 3 && !PosterUrls.isImage(bytes2); attempt++) {
                try {
                    ensureAuth();
                    Grab g = grab(this.server + "/api/proxy?url=" + enc(pic), true);
                    if (g.code == 401) {
                        this.cookie = str2;
                        reason2 = "proxy-401";
                    } else if (PosterUrls.isImage(g.body)) {
                        bytes2 = PosterUrls.payload(g.body);
                        str2 = "proxy-ok";
                        reason2 = "proxy-ok";
                        break;
                    } else {
                        reason2 = "proxy-http-" + g.code;
                        if (g.code == 404 || (g.code >= 500 && attempt >= 1)) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    reason2 = "proxy-" + e.getClass().getSimpleName() + (e.getMessage() == null ? str2 : ":" + e.getMessage());
                }
            }
            bytes = bytes2;
            reason = reason2;
        } else {
            try {
                Grab g2 = grab(pic, false);
                if (PosterUrls.isImage(g2.body)) {
                    bytes2 = PosterUrls.payload(g2.body);
                    str = "direct-ok";
                } else {
                    str = "direct-http-" + g2.code;
                }
                String reason3 = str;
                bytes = bytes2;
                reason = reason3;
            } catch (Exception e2) {
                String reason4 = "direct-" + e2.getClass().getSimpleName();
                bytes = null;
                reason = reason4;
            }
        }
        if (!PosterUrls.isImage(bytes)) {
            String host = posterHost(pic);
            Log.w(TAG, "poster skip host=" + host + " reason=" + reason + (useProxy ? " direct-fallback=no" : " server-unset"));
            if (useProxy && host.contains("youkupic")) {
                Log.w(TAG, "poster youkupic: LibreTV /api/proxy cannot fetch pic.youkupic.com (often HTTP 502). Cover skipped. 无尽 has no other picture field. Direct HTTPS is not used.");
            }
        }
        final Bitmap bitmap = decodePoster(bytes);
        if (bitmap == null && PosterUrls.isImage(bytes)) {
            Log.w(TAG, "poster undecoded host=" + posterHost(pic) + " reason=" + reason + " bytes=" + bytes.length);
        }
        this.ui.post(new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda54
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m1667lambda$loadPoster$23$studiomuzitvMainActivity(gen, shelf, image, bitmap);
            }
        });
    }

    void loadPoster(String url, final ImageView image, final long gen, final int shelf) {
        final String pic = PosterUrls.normalize(url);
        if (pic.isEmpty()) {
            return;
        }
        this.images.execute(new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda60
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m1668lambda$loadPoster$24$studiomuzitvMainActivity(pic, gen, shelf, image);
            }
        });
    }

    Grab grab(String url, boolean withCookie) throws IOException {
        return grab(url, withCookie, 0);
    }

    Grab grab(String url, boolean withCookie, int hops) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        try {
            c.setConnectTimeout(8000);
            c.setReadTimeout(12000);
            c.setInstanceFollowRedirects(false);
            c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 6.0.1; TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.74 Mobile Safari/537.36");
            c.setRequestProperty("Accept", "image/webp,image/*,*/*;q=0.8");
            if (withCookie && this.cookie != null && !this.cookie.isEmpty()) {
                c.setRequestProperty("Cookie", this.cookie);
            }
            int code = c.getResponseCode();
            if (code >= 300 && code < 400 && hops < 2) {
                String loc = c.getHeaderField("Location");
                return (loc == null || !sameHost(url, loc)) ? new Grab(code, new byte[0]) : grab(new URL(new URL(url), loc).toString(), withCookie, hops + 1);
            }
            if (code >= 400) {
                return new Grab(code, new byte[0]);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int total = 0;
            boolean cut = false;
            InputStream in = c.getInputStream();
            while (true) {
                try {
                    int n = in.read(buf);
                    if (n == -1) {
                        break;
                    }
                    total += n;
                    if (total > 3145728) {
                        cut = true;
                        break;
                    }
                    out.write(buf, 0, n);
                } finally {
                }
            }
            if (in != null) {
                in.close();
            }
            return cut ? new Grab(200, new byte[0]) : new Grab(code, out.toByteArray());
        } finally {
            c.disconnect();
        }
    }

    static boolean sameHost(String a, String b) {
        try {
            return new URL(a).getHost().equalsIgnoreCase(new URL(new URL(a), b).getHost());
        } catch (Exception e) {
            return false;
        }
    }

    static Bitmap decodePoster(byte[] bytes) {
        if (!PosterUrls.isImage(bytes)) {
            return null;
        }
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
        int sample = 1;
        int width = bounds.outWidth;
        while (width > 0 && width / sample > 480 && sample < 8) {
            sample *= 2;
        }
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        opts.inSampleSize = sample;
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
        if (bitmap != null || sample == 1) {
            return bitmap;
        }
        opts.inSampleSize = 1;
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
    }

    static String posterHost(String url) {
        try {
            return new URL(url).getHost();
        } catch (Exception e) {
            return "";
        }
    }

    static String posterOrigin(String url) {
        try {
            URL u = new URL(url);
            return u.getProtocol() + "://" + u.getHost() + "/";
        } catch (Exception e) {
            return "";
        }
    }

    void saveProgress() {
        if (this.video == null || this.liveMode || this.currentDetail == null || this.video.getCurrentPosition() < 1000) {
            return;
        }
        try {
            JSONObject h = new JSONObject().put("title", this.activeTitle).put("detail", this.currentDetail).put("episode", this.currentEpisode).put("position", this.video.getCurrentPosition());
            getPreferences(0).edit().putString("continue", h.toString()).apply();
        } catch (Exception e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: chooseSpeed, reason: merged with bridge method [inline-methods] */
    public void m1684lambda$play$43$studiomuzitvMainActivity() {
        final float[] speeds = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f};
        String[] names = {"0.5 倍", "0.75 倍", "1 倍 · 正常", "1.25 倍", "1.5 倍", "1.75 倍", "2 倍"};
        int selectedSpeed = 2;
        for (int i = 0; i < speeds.length; i++) {
            if (speeds[i] == this.playbackSpeed) {
                selectedSpeed = i;
            }
        }
        new AlertDialog.Builder(this).setTitle("播放速度").setSingleChoiceItems(names, selectedSpeed, new DialogInterface.OnClickListener() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda27
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i2) {
                MainActivity.this.m1654lambda$chooseSpeed$25$studiomuzitvMainActivity(speeds, dialogInterface, i2);
            }
        }).setNegativeButton("关闭", (DialogInterface.OnClickListener) null).show();
    }

    /* renamed from: lambda$chooseSpeed$25$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1654lambda$chooseSpeed$25$studiomuzitvMainActivity(float[] speeds, DialogInterface d, int n) {
        this.playbackSpeed = speeds[n];
        if (this.video != null) {
            this.video.setPlaybackParameters(new PlaybackParameters(this.playbackSpeed, 1.0f));
        }
        this.speedButton.setText("倍速 " + this.playbackSpeed + "×");
        d.dismiss();
    }

    String safe(Exception e) {
        String s = e.getMessage();
        if (s == null) {
            return e.getClass().getSimpleName();
        }
        return s.replace(this.password.isEmpty() ? "!NEVER_MATCH!" : this.password, "[隐藏]");
    }

    /* renamed from: lambda$report$26$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1696lambda$report$26$studiomuzitvMainActivity(long gen, String s) {
        if (gen == this.loadGeneration) {
            this.status.setText(s);
        }
    }

    void report(final long gen, final String s) {
        this.ui.post(new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m1696lambda$report$26$studiomuzitvMainActivity(gen, s);
            }
        });
    }

    /* renamed from: lambda$search$29$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1699lambda$search$29$studiomuzitvMainActivity(long gen, JSONArray found, JSONObject out) {
        if (gen != this.loadGeneration) {
            return;
        }
        this.results = found;
        this.status.setText("找到 " + found.length() + " 项结果" + ((out.optJSONArray("failures") == null || out.optJSONArray("failures").length() <= 0) ? "" : " · 部分源未响应"));
        for (int k = 0; k < found.length(); k++) {
            final JSONObject item = found.optJSONObject(k);
            addButton(this.content, item.optString("name") + "  ·  " + item.optString("year") + "  ·  " + item.optString("sourceName"), new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda8
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m1697lambda$search$27$studiomuzitvMainActivity(item);
                }
            });
        }
        int k2 = found.length();
        if (k2 == 0) {
            addButton(this.content, "重新搜索", new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda9
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m1698lambda$search$28$studiomuzitvMainActivity();
                }
            });
        } else {
            this.content.getChildAt(0).requestFocus();
        }
    }

    /* renamed from: lambda$search$30$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1700lambda$search$30$studiomuzitvMainActivity(final long gen) {
        try {
            ensureAuth();
            if (this.sources.length() == 0) {
                this.sources = request("GET", "/api/status", null).getJSONArray("defaultSources");
            }
            JSONObject body = new JSONObject();
            body.put("wd", this.query);
            body.put("sources", this.sources);
            body.put("filterAdult", true);
            final JSONObject out = request("POST", "/api/search", body);
            final JSONArray found = out.getJSONArray("list");
            this.ui.post(new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda59
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m1699lambda$search$29$studiomuzitvMainActivity(gen, found, out);
                }
            });
        } catch (Exception e) {
            report(gen, "搜索失败：" + safe(e));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void search(String q) {
        String q2 = q.trim();
        if (q2.isEmpty()) {
            toast("请输入片名");
            return;
        }
        this.query = q2;
        if (this.searchBox != null) {
            ((InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(this.searchBox.getWindowToken(), 0);
        }
        page("LibreTV · " + this.query);
        this.screen = "results";
        this.status.setText("正在搜索…");
        final long gen = this.loadGeneration;
        this.io.execute(new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda19
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m1700lambda$search$30$studiomuzitvMainActivity(gen);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: detail, reason: merged with bridge method [inline-methods] */
    public void m1697lambda$search$27$studiomuzitvMainActivity(final JSONObject item) {
        page(item.optString("name"));
        this.screen = "detail";
        this.status.setText("正在获取剧集…");
        final long gen = this.loadGeneration;
        this.io.execute(new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda63
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m1662lambda$detail$32$studiomuzitvMainActivity(item, gen);
            }
        });
    }

    /* renamed from: lambda$detail$31$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1661lambda$detail$31$studiomuzitvMainActivity(long gen, JSONObject out) {
        if (gen != this.loadGeneration) {
            return;
        }
        this.currentDetail = out;
        this.episodes = out.optJSONArray("episodes");
        if (this.switching && this.episodes != null && this.episodes.length() > 0) {
            this.switching = false;
            m1695lambda$renderEpisodes$33$studiomuzitvMainActivity(Math.min(this.resumeEpisode, this.episodes.length() - 1));
        } else {
            this.switching = false;
            this.resumeAt = 0L;
            renderEpisodes();
        }
    }

    /* renamed from: lambda$detail$32$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1662lambda$detail$32$studiomuzitvMainActivity(JSONObject item, final long gen) {
        try {
            ensureAuth();
            JSONObject src = new JSONObject();
            src.put("key", item.getString("sourceKey"));
            src.put("name", item.getString("sourceName"));
            src.put("url", item.getString("sourceUrl"));
            final JSONObject out = request("GET", "/api/detail?id=" + enc(item.getString("vodId")) + "&source=" + enc(src.toString()), null);
            this.ui.post(new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda15
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m1661lambda$detail$31$studiomuzitvMainActivity(gen, out);
                }
            });
        } catch (Exception e) {
            report(gen, "剧集获取失败：" + safe(e));
        }
    }

    void renderEpisodes() {
        this.content.removeAllViews();
        if (this.episodes == null) {
            this.episodes = new JSONArray();
        }
        this.status.setText("共 " + this.episodes.length() + " 集 · 确认键播放");
        for (int k = 0; k < this.episodes.length(); k++) {
            final int index = k;
            String ep = this.episodes.optString(k);
            String label = ep.contains("$") ? ep.substring(0, ep.indexOf(36)) : "第 " + (k + 1) + " 集";
            addButton(this.content, label, new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda46
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m1695lambda$renderEpisodes$33$studiomuzitvMainActivity(index);
                }
            });
        }
        if (this.episodes.length() > 0) {
            this.content.getChildAt(0).requestFocus();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: playEpisode, reason: merged with bridge method [inline-methods] */
    public void m1695lambda$renderEpisodes$33$studiomuzitvMainActivity(int index) {
        stopVideo();
        this.liveMode = false;
        this.currentEpisode = index;
        String ep = this.episodes.optString(index);
        String url = ep.contains("$") ? ep.substring(ep.indexOf(36) + 1) : ep;
        final String title = this.currentDetail.optJSONObject("videoInfo").optString("title", "muzi.studio");
        if (this.server == null || this.server.isEmpty()) {
            page(title);
            this.status.setText("正在为你挑选最快线路…");
            final long gen = this.loadGeneration;
            final int epi = index;
            final String first = url;
            this.io.execute(new Runnable() {
                @Override public void run() {
                    final java.util.List<MultiSource.Line> ls = MultiSource.rank(title, epi, first);
                    ui.post(new Runnable() {
                        @Override public void run() {
                            if (gen != loadGeneration) return;
                            MultiSource.lines = ls;
                            MultiSource.current = 0;
                            MultiSource.Line best = ls.isEmpty() ? null : ls.get(0);
                            if (best != null) toast("线路「" + best.name + "」" + String.format(Locale.ROOT, "%.1f", best.mbps) + " Mbps，共 " + ls.size() + " 条");
                            play(best == null ? first : best.url, title);
                        }
                    });
                }
            });
            return;
        }
        play(url, title);
    }

    String clock(long ms) {
        if (ms < 0 || ms == C.TIME_UNSET) {
            return "--:--";
        }
        long t = ms / 1000;
        return String.format(Locale.ROOT, "%02d:%02d:%02d", Long.valueOf(t / 3600), Long.valueOf((t / 60) % 60), Long.valueOf(t % 60));
    }

    void showControls() {
        if (this.controls != null) {
            this.controls.setVisibility(android.view.View.VISIBLE);
            this.pauseButton.requestFocus();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: chooseEpisodes, reason: merged with bridge method [inline-methods] */
    public void m1682lambda$play$41$studiomuzitvMainActivity() {
        if (this.liveMode) {
            stopVideo();
            showLive();
            return;
        }
        String[] names = new String[this.episodes.length()];
        for (int i = 0; i < names.length; i++) {
            String ep = this.episodes.optString(i);
            names[i] = ep.contains("$") ? ep.substring(0, ep.indexOf(36)) : "第 " + (i + 1) + " 集";
        }
        new AlertDialog.Builder(this).setTitle("选集").setSingleChoiceItems(names, this.currentEpisode, new DialogInterface.OnClickListener() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda38
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i2) {
                MainActivity.this.m1653lambda$chooseEpisodes$34$studiomuzitvMainActivity(dialogInterface, i2);
            }
        }).setNegativeButton("关闭", (DialogInterface.OnClickListener) null).show();
    }

    /* renamed from: lambda$chooseEpisodes$34$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1653lambda$chooseEpisodes$34$studiomuzitvMainActivity(DialogInterface d, int n) {
        d.dismiss();
        this.resumeAt = 0L;
        m1695lambda$renderEpisodes$33$studiomuzitvMainActivity(n);
    }

    /* renamed from: lambda$switchSource$35$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1711lambda$switchSource$35$studiomuzitvMainActivity(JSONObject item) {
        this.switching = true;
        m1697lambda$search$27$studiomuzitvMainActivity(item);
    }

    /* renamed from: lambda$switchSource$36$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1712lambda$switchSource$36$studiomuzitvMainActivity(long gen, JSONArray found) {
        if (gen != this.loadGeneration) {
            return;
        }
        for (int i = 0; i < found.length(); i++) {
            final JSONObject item = found.optJSONObject(i);
            addButton(this.content, item.optString("name") + " · " + item.optString("sourceName"), new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda18
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m1711lambda$switchSource$35$studiomuzitvMainActivity(item);
                }
            });
        }
        this.status.setText("找到 " + found.length() + " 项 · 请核对片名和集数");
        if (found.length() > 0) {
            this.content.getChildAt(0).requestFocus();
        }
    }

    /* renamed from: lambda$switchSource$37$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1713lambda$switchSource$37$studiomuzitvMainActivity(String title, final long gen) {
        try {
            ensureAuth();
            JSONObject out = request("POST", "/api/search", new JSONObject().put("wd", title).put("sources", this.sources).put("filterAdult", true));
            final JSONArray found = out.getJSONArray("list");
            this.ui.post(new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda47
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m1712lambda$switchSource$36$studiomuzitvMainActivity(gen, found);
                }
            });
        } catch (Exception e) {
            report(gen, "换源搜索失败：" + safe(e));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: switchSource, reason: merged with bridge method [inline-methods] */
    public void m1683lambda$play$42$studiomuzitvMainActivity() {
        if (this.liveMode) {
            stopVideo();
            showLive();
            return;
        }
        this.resumeAt = this.video == null ? 0L : this.video.getCurrentPosition();
        this.resumeEpisode = this.currentEpisode;
        final String title = this.activeTitle;
        page("换源 · " + title);
        this.screen = "switch";
        this.status.setText("搜索其它来源，选择后从原进度继续…");
        final long gen = this.loadGeneration;
        this.io.execute(new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda17
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m1713lambda$switchSource$37$studiomuzitvMainActivity(title, gen);
            }
        });
    }

    /* renamed from: lambda$play$38$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1679lambda$play$38$studiomuzitvMainActivity(long gen, boolean live, boolean direct, String url, String name) {
        if (gen != this.loadGeneration) {
            return;
        }
        this.liveMode = live;
        this.directLive = direct;
        if (!this.cookie.isEmpty()) {
            play(url, name);
            return;
        }
        if (live) {
            this.status.setText("直播要走 LibreTV 代理，登录没有成功。请检查设置里的密码。没有改走直连。");
            return;
        }
        String saved = this.server;
        this.server = "";
        play(url, name);
        this.server = saved;
    }

    /* renamed from: lambda$play$39$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1680lambda$play$39$studiomuzitvMainActivity(final long gen, final boolean live, final boolean direct, final String url, final String name) {
        try {
            ensureAuth();
        } catch (Exception e) {
        }
        this.ui.post(new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda57
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m1679lambda$play$38$studiomuzitvMainActivity(gen, live, direct, url, name);
            }
        });
    }

    /* JADX WARN: Removed duplicated region for block: B:38:0x0356  */
    /* JADX WARN: Removed duplicated region for block: B:41:0x0388  */
    /* JADX WARN: Removed duplicated region for block: B:43:? A[RETURN, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:44:0x0358  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    void play(final String sourceUrl, final String title) {
        String mediaUrl;
        String str;
        if (this.server != null && !this.server.isEmpty() && this.cookie.isEmpty()) {
            final boolean live = this.liveMode;
            final boolean direct = this.directLive;
            page(title);
            this.status.setText("正在连接 LibreTV…");
            final long gen = this.loadGeneration;
            this.io.execute(new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda29
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m1680lambda$play$39$studiomuzitvMainActivity(gen, live, direct, sourceUrl, title);
                }
            });
            return;
        }
        stopVideo();
        this.loadGeneration++;
        this.screen = "player";
        this.activeTitle = title;
        final FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(0xFF000000);
        final TextureView surface = new TextureView(this);
        frame.addView(surface, new FrameLayout.LayoutParams(-1, -1, 17));
        this.playStatus = text(title + " · 正在缓冲…", 18);
        this.playStatus.setBackgroundColor(-1342177280);
        frame.addView(this.playStatus, new FrameLayout.LayoutParams(-1, -2, 48));
        this.controls = new LinearLayout(this);
        this.controls.setOrientation(android.widget.LinearLayout.VERTICAL);
        this.controls.setPadding(dp(24.0f), dp(8.0f), dp(24.0f), dp(12.0f));
        this.controls.setBackgroundColor(-586147801);
        this.timeLabel = text("正在加载…", 18);
        this.controls.addView(this.timeLabel);
        this.highlightBlurb = text("", 16);
        this.highlightBlurb.setTextColor(-7261);
        this.highlightBlurb.setVisibility(android.view.View.GONE);
        this.controls.addView(this.highlightBlurb);
        this.seek = new MarkerSeekBar(this);
        this.seek.setMax(1000);
        this.seek.setKeyProgressIncrement(5);
        this.controls.addView(this.seek, new LinearLayout.LayoutParams(-1, dp(38.0f)));
        this.seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { // from class: studio.muzi.tv.MainActivity.1
            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onProgressChanged(SeekBar s, int p, boolean user) {
                if (!user || MainActivity.this.video == null || !MainActivity.this.video.isCurrentMediaItemSeekable() || MainActivity.this.video.getDuration() <= 0) {
                    return;
                }
                MainActivity.this.video.seekTo((MainActivity.this.video.getDuration() * p) / 1000);
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStartTrackingTouch(SeekBar s) {
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStopTrackingTouch(SeekBar s) {
            }
        });
        this.markerScroller = new HorizontalScrollView(this);
        this.markerScroller.setHorizontalScrollBarEnabled(false);
        this.markerScroller.setFocusable(false);
        this.markerScroller.setDescendantFocusability(262144);
        this.markerScroller.setVisibility(android.view.View.GONE);
        this.markerChips = new LinearLayout(this);
        this.markerChips.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        this.markerScroller.addView(this.markerChips);
        this.controls.addView(this.markerScroller);
        LinearLayout row = new LinearLayout(this);
        this.pauseButton = button("暂停 / 播放", new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda30
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m1681lambda$play$40$studiomuzitvMainActivity();
            }
        });
        row.addView(this.pauseButton);
        row.addView(button(this.liveMode ? "频道" : "选集", new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda31
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m1682lambda$play$41$studiomuzitvMainActivity();
            }
        }));
        row.addView(button(this.liveMode ? "换频道" : "换源", new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda32
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m1683lambda$play$42$studiomuzitvMainActivity();
            }
        }));
        if (!this.liveMode) {
            this.speedButton = button("倍速 " + this.playbackSpeed + "×", new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda34
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m1684lambda$play$43$studiomuzitvMainActivity();
                }
            });
            row.addView(this.speedButton);
        }
        row.addView(button("收起控制栏", new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda35
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m1685lambda$play$44$studiomuzitvMainActivity(surface);
            }
        }));
        this.controls.addView(row);
        this.highlightActions = new LinearLayout(this);
        this.highlightActions.setVisibility(android.view.View.GONE);
        this.highlightActions.addView(button("上一精彩", new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda36
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m1686lambda$play$45$studiomuzitvMainActivity();
            }
        }));
        this.highlightActions.addView(button("下一精彩", new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda37
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m1687lambda$play$46$studiomuzitvMainActivity();
            }
        }));
        TextView highlightHint = text("频道键 / 快进键也可跳转", 15);
        highlightHint.setTextColor(-3813152);
        this.highlightActions.addView(highlightHint);
        this.controls.addView(this.highlightActions);
        frame.addView(this.controls, new FrameLayout.LayoutParams(-1, -2, 80));
        setContentView(frame);
        getWindow().addFlags(128);
        Map<String, String> headers = new HashMap<>();
        headers.put("Cookie", this.cookie);
        DefaultHttpDataSource.Factory http = new DefaultHttpDataSource.Factory().setUserAgent("MuziTV/0.10").setDefaultRequestProperties(headers).setConnectTimeoutMs(15000).setReadTimeoutMs(25000);
        this.video = new ExoPlayer.Builder(this).setMediaSourceFactory(new DefaultMediaSourceFactory(http)).setLoadControl(new DefaultLoadControl.Builder().setBufferDurationsMs(60000, 180000, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS, 10000).setTargetBufferBytes(67108864).setPrioritizeTimeOverSizeThresholds(false).build()).build();
        this.video.setVideoTextureView(surface);
        MultiSource.watch(this, title);
        this.video.setAudioAttributes(new AudioAttributes.Builder().setUsage(androidx.media3.common.C.USAGE_MEDIA).setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE).build(), true);
        this.video.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (MainActivity.this.video == null) {
                    return;
                }
                Log.i(MainActivity.TAG, "PLAYER_STATE " + state + " duration=" + MainActivity.this.video.getDuration());
                if (state == 3) {
                    MainActivity.this.playStatus.setVisibility(android.view.View.GONE);
                }
                if (state == 2) {
                    MainActivity.this.playStatus.setVisibility(android.view.View.VISIBLE);
                    MainActivity.this.playStatus.setText(title + " · 正在缓冲…");
                }
                if (state == 4) {
                    MainActivity.this.playStatus.setVisibility(android.view.View.VISIBLE);
                    MainActivity.this.playStatus.setText("播放结束 · 可选集或换源");
                    MainActivity.this.showControls();
                }
            }

            @Override
            public void onRenderedFirstFrame() {
                Log.i(MainActivity.TAG, "FIRST_VIDEO_FRAME");
            }

            @Override
            public void onVideoSizeChanged(VideoSize size) {
                Log.i(MainActivity.TAG, "VIDEO_SIZE " + size.width + "x" + size.height);
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                MainActivity.this.playStatus.setVisibility(android.view.View.VISIBLE);
                MainActivity.this.playStatus.setText("播放失败：" + error.getErrorCodeName() + " · 可换源重试");
                MainActivity.this.showControls();
                Log.e(MainActivity.TAG, "PLAY_ERROR " + error.getErrorCodeName(), error);
            }
        });
        if (this.server == null || this.server.isEmpty()) {
            mediaUrl = sourceUrl;
        } else {
            mediaUrl = this.server + (this.liveMode ? "/api/live/stream?url=" : "/api/proxy?url=") + enc(sourceUrl);
        }
        MediaItem.Builder media = new MediaItem.Builder().setUri(mediaUrl);
        if (!sourceUrl.toLowerCase(Locale.ROOT).contains("m3u8")) {
            if (sourceUrl.toLowerCase(Locale.ROOT).contains(".mp4")) {
                str = MimeTypes.VIDEO_MP4;
            }
            this.video.setMediaItem(media.build());
            if (!this.liveMode && this.resumeAt > 0) {
                this.video.seekTo(this.resumeAt);
            }
            this.resumeAt = 0L;
            this.video.setPlaybackParameters(new PlaybackParameters(!this.liveMode ? 1.0f : this.playbackSpeed, 1.0f));
            this.video.prepare();
            this.video.play();
            surface.setFocusable(true);
            surface.setFocusableInTouchMode(true);
            showControls();
            this.ticker = new Runnable() { // from class: studio.muzi.tv.MainActivity.3
                int ticks = 0;

                @Override // java.lang.Runnable
                public void run() {
                    if (MainActivity.this.video == null) {
                        return;
                    }
                    long pos = MainActivity.this.video.getCurrentPosition();
                    long len = MainActivity.this.video.getDuration();
                    MainActivity.this.timeLabel.setText(title + "   " + MainActivity.this.clock(pos) + " / " + (MainActivity.this.liveMode ? "直播" : MainActivity.this.clock(len)) + "   " + (MainActivity.this.video.isPlaying() ? "播放中" : "暂停 / 缓冲"));
                    MainActivity.this.seek.setEnabled(MainActivity.this.video.isCurrentMediaItemSeekable() && len > 0);
                    if (len > 0) {
                        MainActivity.this.seek.setProgress((int) ((pos * 1000) / len));
                        MainActivity.this.seek.setSecondaryProgress(MainActivity.this.video.getBufferedPercentage() * 10);
                    }
                    MainActivity.this.refreshHighlightDots(pos, len);
                    MainActivity.this.pauseButton.setText(MainActivity.this.video.isPlaying() ? "暂停" : "播放");
                    if (this.ticks % 5 == 0) {
                        MainActivity.this.saveProgress();
                    }
                    int i = this.ticks;
                    this.ticks = i + 1;
                    if (i % 5 == 0) {
                        Log.i(MainActivity.TAG, "PLAY_TICK pos=" + pos + " duration=" + len + " playing=" + MainActivity.this.video.isPlaying() + " bufferedMs=" + MainActivity.this.video.getTotalBufferedDuration() + " speed=" + MainActivity.this.video.getPlaybackParameters().speed + " audio=" + (MainActivity.this.video.getAudioFormat() != null ? MainActivity.this.video.getAudioFormat().sampleMimeType : "none"));
                    }
                    MainActivity.this.ui.postDelayed(this, 1000L);
                }
            };
            this.ui.postDelayed(this.ticker, 1000L);
            if (this.liveMode) {
                loadHighlightsForPlayback(title);
                return;
            }
            return;
        }
        str = MimeTypes.APPLICATION_M3U8;
        media.setMimeType(str);
        this.video.setMediaItem(media.build());
        if (!this.liveMode) {
            this.video.seekTo(this.resumeAt);
        }
        this.resumeAt = 0L;
        this.video.setPlaybackParameters(new PlaybackParameters(!this.liveMode ? 1.0f : this.playbackSpeed, 1.0f));
        this.video.prepare();
        this.video.play();
        surface.setFocusable(true);
        surface.setFocusableInTouchMode(true);
        showControls();
        this.ticker = new Runnable() { // from class: studio.muzi.tv.MainActivity.3
            int ticks = 0;

            @Override // java.lang.Runnable
            public void run() {
                if (MainActivity.this.video == null) {
                    return;
                }
                long pos = MainActivity.this.video.getCurrentPosition();
                long len = MainActivity.this.video.getDuration();
                MainActivity.this.timeLabel.setText(title + "   " + MainActivity.this.clock(pos) + " / " + (MainActivity.this.liveMode ? "直播" : MainActivity.this.clock(len)) + "   " + (MainActivity.this.video.isPlaying() ? "播放中" : "暂停 / 缓冲"));
                MainActivity.this.seek.setEnabled(MainActivity.this.video.isCurrentMediaItemSeekable() && len > 0);
                if (len > 0) {
                    MainActivity.this.seek.setProgress((int) ((pos * 1000) / len));
                    MainActivity.this.seek.setSecondaryProgress(MainActivity.this.video.getBufferedPercentage() * 10);
                }
                MainActivity.this.refreshHighlightDots(pos, len);
                MainActivity.this.pauseButton.setText(MainActivity.this.video.isPlaying() ? "暂停" : "播放");
                if (this.ticks % 5 == 0) {
                    MainActivity.this.saveProgress();
                }
                int i = this.ticks;
                this.ticks = i + 1;
                if (i % 5 == 0) {
                    Log.i(MainActivity.TAG, "PLAY_TICK pos=" + pos + " duration=" + len + " playing=" + MainActivity.this.video.isPlaying() + " bufferedMs=" + MainActivity.this.video.getTotalBufferedDuration() + " speed=" + MainActivity.this.video.getPlaybackParameters().speed + " audio=" + (MainActivity.this.video.getAudioFormat() != null ? MainActivity.this.video.getAudioFormat().sampleMimeType : "none"));
                }
                MainActivity.this.ui.postDelayed(this, 1000L);
            }
        };
        this.ui.postDelayed(this.ticker, 1000L);
        if (this.liveMode) {
        }
    }

    /* renamed from: lambda$play$40$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1681lambda$play$40$studiomuzitvMainActivity() {
        if (this.video != null) {
            if (this.video.isPlaying()) {
                this.video.pause();
            } else {
                this.video.play();
            }
        }
    }

    /* renamed from: lambda$play$44$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1685lambda$play$44$studiomuzitvMainActivity(TextureView surface) {
        this.controls.setVisibility(android.view.View.GONE);
        surface.requestFocus();
    }

    /* renamed from: lambda$play$45$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1686lambda$play$45$studiomuzitvMainActivity() {
        jumpHighlight(false);
    }

    /* renamed from: lambda$play$46$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1687lambda$play$46$studiomuzitvMainActivity() {
        jumpHighlight(true);
    }

    void stopVideo() {
        saveProgress();
        if (this.ticker != null) {
            this.ui.removeCallbacks(this.ticker);
        }
        if (this.hideBlurb != null) {
            this.ui.removeCallbacks(this.hideBlurb);
        }
        if (this.previewTick != null) {
            this.ui.removeCallbacks(this.previewTick);
        }
        this.previewTick = null;
        if (this.video != null) {
            this.video.release();
            this.video = null;
        }
        this.playing = false;
        this.previewPlaying = false;
        this.activeHighlights.clear();
        this.markerTimes = new int[0];
        this.pinnedHighlight = -1;
        BiliScreens.destroy(this);
        getWindow().clearFlags(128);
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public boolean dispatchKeyEvent(KeyEvent e) {
        if ("home".equals(this.screen) && this.home != null && this.home.navigationKey(e)) return true;
        if (e.getAction() == 0 && e.getRepeatCount() == 0 && BiliScreens.onKey(this, e)) {
            return true;
        }
        if (this.screen.equals("preview") && e.getAction() == 0 && e.getRepeatCount() == 0) {
            int k = e.getKeyCode();
            if (isNextHighlightKey(k)) {
                jumpHighlight(true);
                return true;
            }
            if (isPrevHighlightKey(k)) {
                jumpHighlight(false);
                return true;
            }
        }
        if (this.screen.equals("player") && this.video != null && e.getAction() == 0) {
            int k2 = e.getKeyCode();
            if (e.getRepeatCount() == 0 && !this.activeHighlights.isEmpty() && (isNextHighlightKey(k2) || isPrevHighlightKey(k2))) {
                jumpHighlight(isNextHighlightKey(k2));
                return true;
            }
            if (k2 == 82 || ((k2 == 19 && this.controls.getVisibility() != android.view.View.VISIBLE) || (k2 == 20 && this.controls.getVisibility() != android.view.View.VISIBLE))) {
                showControls();
                return true;
            }
            if (k2 == 85 || (this.controls.getVisibility() != android.view.View.VISIBLE && (k2 == 23 || k2 == 66))) {
                if (this.video.isPlaying()) {
                    this.video.pause();
                } else {
                    this.video.play();
                }
                showControls();
                return true;
            }
            if (this.controls.getVisibility() != android.view.View.VISIBLE && (k2 == 21 || k2 == 22)) {
                if (this.video.isCurrentMediaItemSeekable() && this.video.getDuration() > 0) {
                    this.video.seekTo(Math.max(0L, Math.min(this.video.getDuration() - 1000, this.video.getCurrentPosition() + (k2 == 22 ? 10000 : -10000))));
                }
                showControls();
                return true;
            }
        }
        return super.dispatchKeyEvent(e);
    }

    @Override
    public void onBackPressed() {
        if (BiliScreens.onBack(this)) {
            return;
        }
        if (this.screen.equals("preview") || this.screen.equals("preview-list")) {
            stopVideo();
            showSettings();
            return;
        }
        if (this.screen.equals("home")) {
            return;
        }
        if (this.screen.equals("search")) {
            m1698lambda$search$28$studiomuzitvMainActivity();
            return;
        }
        if (this.screen.equals("player")) {
            if (this.liveMode) {
                this.directLive = false;
                showLive();
                return;
            } else {
                String title = this.activeTitle;
                page(title);
                this.screen = "detail";
                renderEpisodes();
                return;
            }
        }
        String title2 = this.screen;
        if (title2.equals("channels")) {
            showLive();
            return;
        }
        if (!this.screen.equals("detail") && !this.screen.equals("results") && !this.screen.equals("switch")) {
            showHome();
            return;
        }
        this.switching = false;
        this.resumeAt = 0L;
        m1698lambda$search$28$studiomuzitvMainActivity();
    }

    @Override // android.app.Activity
    protected void onPause() {
        super.onPause();
        if (home != null) home.pause();
        Updater.background(this);
        if (this.video != null) {
            this.video.pause();
        }
        if (this.biliWeb != null) {
            this.biliWeb.onPause();
        }
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        if (home != null && "home".equals(screen)) home.resume();
        if (this.biliWeb != null) {
            this.biliWeb.onResume();
        }
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        stopVideo();
        this.io.shutdownNow();
        this.images.shutdownNow();
        this.side.shutdownNow();
        this.cms.shutdownNow();
        super.onDestroy();
    }

    String enc(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    static String read(InputStream in) throws IOException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                byte[] b = new byte[8192];
                do {
                    int n = in.read(b);
                    if (n == -1) {
                        String byteArrayOutputStream = out.toString("UTF-8");
                        out.close();
                        if (in != null) {
                            in.close();
                        }
                        return byteArrayOutputStream;
                    }
                    out.write(b, 0, n);
                } while (out.size() <= 8388608);
                throw new IOException("响应过大");
            } finally {
            }
        } catch (Throwable th) {
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    synchronized void ensureAuth() throws Exception {
        if (this.cookie.isEmpty()) {
            request("POST", "/api/auth", new JSONObject().put("password", this.password));
        }
    }

    synchronized JSONObject request(String method, String path, JSONObject body) throws Exception {
        JSONObject json;
        HttpURLConnection c = (HttpURLConnection) new URL(this.server + path).openConnection();
        try {
            c.setConnectTimeout(10000);
            c.setReadTimeout(35000);
            c.setRequestMethod(method);
            if (!this.cookie.isEmpty()) {
                try {
                    c.setRequestProperty("Cookie", this.cookie);
                } catch (Throwable th) {
                    th = th;
                    c.disconnect();
                    throw th;
                }
            }
            if (body != null) {
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json");
                OutputStream o = c.getOutputStream();
                try {
                    o.write(body.toString().getBytes("UTF-8"));
                    if (o != null) {
                        o.close();
                    }
                } finally {
                }
            }
            int code = c.getResponseCode();
            String set = c.getHeaderField("Set-Cookie");
            if (set != null) {
                this.cookie = set.split(";", 2)[0];
            }
            String raw = read(code < 400 ? c.getInputStream() : c.getErrorStream());
            if (code == 401) {
                this.cookie = "";
                throw new IOException("会话已过期，请返回后重试");
            }
            json = new JSONObject(raw);
            if (code >= 400) {
                throw new IOException(json.optString("error", "HTTP " + code));
            }
            c.disconnect();
        } catch (Throwable th2) {
            throw th2 instanceof RuntimeException ? (RuntimeException) th2 : new RuntimeException(th2);
        }
        return json;
    }

    /* renamed from: lambda$loadHighlightsForPlayback$47$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1663lambda$loadHighlightsForPlayback$47$studiomuzitvMainActivity(long gen, List hits) {
        if (gen == this.loadGeneration && "player".equals(this.screen)) {
            applyHighlights(hits);
        }
    }

    /* renamed from: lambda$loadHighlightsForPlayback$48$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1664lambda$loadHighlightsForPlayback$48$studiomuzitvMainActivity(String title, int episode, final long gen) {
        HighlightStore.Loaded loaded = HighlightStore.load(this);
        final List<Highlight> hits = loaded.map.match(title, episode);
        Log.i(TAG, "highlights " + hits.size() + " origin=" + loaded.origin + " title=" + title + " ep=" + (episode + 1));
        this.ui.post(new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda56
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m1663lambda$loadHighlightsForPlayback$47$studiomuzitvMainActivity(gen, hits);
            }
        });
    }

    void loadHighlightsForPlayback(final String title) {
        final long gen = this.loadGeneration;
        final int episode = this.currentEpisode;
        if (this.liveMode) {
            return;
        }
        this.side.execute(new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda45
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m1664lambda$loadHighlightsForPlayback$48$studiomuzitvMainActivity(title, episode, gen);
            }
        });
    }

    void applyHighlights(List<Highlight> hits) {
        if (this.seek == null || this.markerChips == null || this.markerScroller == null || this.highlightActions == null) {
            return;
        }
        this.activeHighlights = new ArrayList();
        if (hits != null) {
            this.activeHighlights.addAll(hits);
        }
        this.markerTimes = new int[this.activeHighlights.size()];
        for (int i = 0; i < this.activeHighlights.size(); i++) {
            this.markerTimes[i] = (int) this.activeHighlights.get(i).timeMs();
        }
        this.pinnedHighlight = -1;
        boolean on = !this.activeHighlights.isEmpty();
        this.markerScroller.setVisibility(on ? android.view.View.VISIBLE : android.view.View.GONE);
        this.highlightActions.setVisibility(on ? android.view.View.VISIBLE : android.view.View.GONE);
        this.markerChips.removeAllViews();
        if (on) {
            populateMarkerChips(this.markerChips, this.activeHighlights);
            showBlurb("本片有 " + this.activeHighlights.size() + " 个精彩标记 · 选择标记跳转", true);
            refreshHighlightDots(currentPos(), currentDur());
        } else {
            if (this.highlightBlurb != null) {
                this.highlightBlurb.setVisibility(android.view.View.GONE);
            }
            this.seek.setMarkers(new int[0], 0L, -1);
        }
    }

    /* renamed from: lambda$populateMarkerChips$50$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1689lambda$populateMarkerChips$50$studiomuzitvMainActivity(int index, View v, boolean f) {
        int i;
        int i2;
        int i3;
        if (f) {
            i = 109;
            i2 = 141;
            i3 = 86;
        } else {
            i = 46;
            i2 = 65;
            i3 = 34;
        }
        v.setBackground(bg(Color.rgb(i3, i, i2)));
        v.setScaleX(f ? 1.02f : 1.0f);
        v.setScaleY(f ? 1.02f : 1.0f);
        if (!f) {
            if (this.pinnedHighlight == index) {
                this.pinnedHighlight = -1;
            }
        } else {
            this.pinnedHighlight = index;
            if (index < this.activeHighlights.size()) {
                showBlurb(this.activeHighlights.get(index).text, false);
            }
            refreshHighlightDots(currentPos(), currentDur());
        }
    }

    void populateMarkerChips(LinearLayout host, List<Highlight> items) {
        host.removeAllViews();
        for (int i = 0; i < items.size(); i++) {
            final int index = i;
            Highlight h = items.get(i);
            Button chip = button(shortClock(h.timeMs()) + "  " + h.text, new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda61
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m1688lambda$populateMarkerChips$49$studiomuzitvMainActivity(index);
                }
            });
            chip.setTextSize(15.0f);
            chip.setSingleLine(true);
            chip.setEllipsize(TextUtils.TruncateAt.END);
            chip.setMaxWidth(dp(300.0f));
            chip.setOnFocusChangeListener(new View.OnFocusChangeListener() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda62
                @Override // android.view.View.OnFocusChangeListener
                public final void onFocusChange(View view, boolean z) {
                    MainActivity.this.m1689lambda$populateMarkerChips$50$studiomuzitvMainActivity(index, view, z);
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.setMargins(dp(4.0f), dp(2.0f), dp(4.0f), dp(2.0f));
            host.addView(chip, lp);
        }
    }

    long currentPos() {
        if ("preview".equals(this.screen)) {
            return this.previewPos;
        }
        if (this.video == null) {
            return 0L;
        }
        return this.video.getCurrentPosition();
    }

    long currentDur() {
        long len = "preview".equals(this.screen) ? this.previewDur : this.video == null ? 0L : this.video.getDuration();
        if (len < 0) {
            return 0L;
        }
        return len;
    }

    void refreshHighlightDots(long pos, long len) {
        if (this.seek == null) {
            return;
        }
        int active = this.pinnedHighlight;
        if (active < 0) {
            active = chapterIndex(pos);
        }
        this.seek.setMarkers(this.markerTimes, len, active);
    }

    int chapterIndex(long pos) {
        int idx = -1;
        for (int i = 0; i < this.activeHighlights.size(); i++) {
            if (this.activeHighlights.get(i).timeMs() <= 400 + pos) {
                idx = i;
            }
        }
        return idx;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: seekToHighlight, reason: merged with bridge method [inline-methods] */
    public void m1688lambda$populateMarkerChips$49$studiomuzitvMainActivity(int index) {
        if (index < 0 || index >= this.activeHighlights.size()) {
            return;
        }
        Highlight h = this.activeHighlights.get(index);
        long ms = h.timeMs();
        String blurb = h.text;
        if ("preview".equals(this.screen)) {
            this.previewPos = this.previewDur > 0 ? Math.max(0L, Math.min(ms, Math.max(0L, this.previewDur - 500))) : ms;
            showBlurb(blurb, true);
            refreshPreviewUi();
            focusChip(index);
            return;
        }
        if (this.video == null || !this.video.isCurrentMediaItemSeekable()) {
            toast("当前内容不能跳转");
            return;
        }
        long len = this.video.getDuration();
        if (len > 0 && ms > len) {
            ms = Math.max(0L, len - 1000);
            blurb = h.text + " · 已超过片长";
        }
        this.video.seekTo(ms);
        showBlurb(blurb, true);
        if (this.controls != null && this.controls.getVisibility() != android.view.View.VISIBLE) {
            showControls();
        }
        refreshHighlightDots(ms, len);
        focusChip(index);
    }

    void focusChip(int index) {
        if (this.markerChips == null || index < 0 || index >= this.markerChips.getChildCount()) {
            return;
        }
        this.markerChips.getChildAt(index).requestFocus();
    }

    boolean jumpHighlight(boolean forward) {
        if (this.activeHighlights.isEmpty()) {
            return false;
        }
        long pos = currentPos();
        int target = -1;
        if (forward) {
            int i = 0;
            while (true) {
                if (i >= this.activeHighlights.size()) {
                    break;
                }
                if (this.activeHighlights.get(i).timeMs() > pos + 1500) {
                    target = i;
                    break;
                }
                i++;
            }
        } else {
            int i2 = this.activeHighlights.size() - 1;
            while (true) {
                if (i2 < 0) {
                    break;
                }
                if (this.activeHighlights.get(i2).timeMs() < pos - 1500) {
                    target = i2;
                    break;
                }
                i2--;
            }
        }
        if (target < 0) {
            toast(forward ? "后面没有精彩标记" : "前面没有精彩标记");
            return true;
        }
        this.pinnedHighlight = -1;
        m1688lambda$populateMarkerChips$49$studiomuzitvMainActivity(target);
        return true;
    }

    /* renamed from: lambda$showBlurb$51$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1703lambda$showBlurb$51$studiomuzitvMainActivity() {
        if (this.highlightBlurb == null || this.pinnedHighlight >= 0) {
            return;
        }
        this.highlightBlurb.setVisibility(android.view.View.GONE);
    }

    void showBlurb(String text, boolean autoHide) {
        if (this.highlightBlurb == null) {
            return;
        }
        this.highlightBlurb.setText(text);
        this.highlightBlurb.setVisibility(android.view.View.VISIBLE);
        if (this.hideBlurb != null) {
            this.ui.removeCallbacks(this.hideBlurb);
        }
        if (autoHide) {
            this.hideBlurb = new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda22
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m1703lambda$showBlurb$51$studiomuzitvMainActivity();
                }
            };
            this.ui.postDelayed(this.hideBlurb, 4500L);
        }
    }

    String shortClock(long ms) {
        long t = Math.max(0L, ms / 1000);
        long h = t / 3600;
        long m = (t / 60) % 60;
        long s = t % 60;
        return h > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", Long.valueOf(h), Long.valueOf(m), Long.valueOf(s)) : String.format(Locale.ROOT, "%02d:%02d", Long.valueOf(m), Long.valueOf(s));
    }

    boolean isNextHighlightKey(int k) {
        return k == 166 || k == 87 || k == 90;
    }

    boolean isPrevHighlightKey(int k) {
        return k == 167 || k == 88 || k == 89;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: configureHighlights, reason: merged with bridge method [inline-methods] */
    public void m1709lambda$showSettings$8$studiomuzitvMainActivity() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        box.setPadding(dp(8.0f), dp(8.0f), dp(8.0f), dp(8.0f));
        TextView help = new TextView(this);
        help.setTextColor(0xFF000000);
        help.setTextSize(16.0f);
        help.setText("精彩标记不来自片源。按片名匹配，可选集数。\n\n优先级：\n1. 网络地址\n2. 上次成功的网络缓存\n3. 侧载文件（整份替换，不与示例合并）\n4. 安装包内 assets/highlights.json\n\n" + HighlightStore.describe(this) + "\n\n集数从 1 开始，和「第 N 集」一致。不写集数则电影和每一集都用。留空后保存会停用网络地址。");
        box.addView(help, new LinearLayout.LayoutParams(-1, -2));
        final EditText url = new EditText(this);
        url.setSingleLine(true);
        url.setHint("http://192.168.1.8:8765/highlights.json");
        url.setText(HighlightStore.readUrl(this));
        box.addView(url, new LinearLayout.LayoutParams(-1, -2));
        scrollView.addView(box);
        final AlertDialog dialog = new AlertDialog.Builder(this).setTitle("精彩标记").setView(scrollView).setNegativeButton("关闭", (DialogInterface.OnClickListener) null).setNeutralButton("预览", new DialogInterface.OnClickListener() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda3
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.m1658lambda$configureHighlights$52$studiomuzitvMainActivity(dialogInterface, i);
            }
        }).setPositiveButton("保存", (DialogInterface.OnClickListener) null).create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda4
            @Override // android.content.DialogInterface.OnShowListener
            public final void onShow(DialogInterface dialogInterface) {
                MainActivity.this.m1660lambda$configureHighlights$54$studiomuzitvMainActivity(dialog, url, dialogInterface);
            }
        });
        dialog.show();
    }

    /* renamed from: lambda$configureHighlights$52$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1658lambda$configureHighlights$52$studiomuzitvMainActivity(DialogInterface d, int w) {
        this.ui.post(new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda39
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.previewHighlights();
            }
        });
    }

    /* renamed from: lambda$configureHighlights$53$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1659lambda$configureHighlights$53$studiomuzitvMainActivity(EditText url, AlertDialog dialog, View v) {
        String value = url.getText().toString().trim();
        if (!value.isEmpty() && !value.startsWith("http://") && !value.startsWith("https://")) {
            toast("请输入 http 或 https 地址");
            return;
        }
        try {
            HighlightStore.writeUrl(this, value);
            toast(value.isEmpty() ? "已改用本地标记" : "网络标记地址已保存");
            dialog.dismiss();
        } catch (Exception e) {
            toast("保存失败");
        }
    }

    /* renamed from: lambda$configureHighlights$54$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1660lambda$configureHighlights$54$studiomuzitvMainActivity(final AlertDialog dialog, final EditText url, DialogInterface d) {
        dialog.getButton(-1).setOnClickListener(new View.OnClickListener() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda11
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m1659lambda$configureHighlights$53$studiomuzitvMainActivity(url, dialog, view);
            }
        });
    }

    /* renamed from: lambda$previewHighlights$56$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1691lambda$previewHighlights$56$studiomuzitvMainActivity(HighlightStore.Loaded loaded) {
        if ("preview-list".equals(this.screen)) {
            if (loaded.map.entries.isEmpty()) {
                this.status.setText("没有可用标记 · " + loaded.origin);
                return;
            }
            this.status.setText(loaded.map.entries.size() + " 条 · " + loaded.origin + " · 确认后预览圆点，不会播放片源");
            for (int i = 0; i < loaded.map.entries.size(); i++) {
                final HighlightMap.Entry entry = loaded.map.entries.get(i);
                addButton(this.content, entry.titleContains + (entry.episode == null ? " · 电影/每一集" : " · 第 " + entry.episode + " 集") + " · " + entry.highlights.size() + " 个", new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda58
                    @Override // java.lang.Runnable
                    public final void run() {
                        MainActivity.this.m1690lambda$previewHighlights$55$studiomuzitvMainActivity(entry);
                    }
                });
            }
            if (this.content.getChildCount() > 0) {
                this.content.getChildAt(0).requestFocus();
            }
        }
    }

    /* renamed from: lambda$previewHighlights$57$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1692lambda$previewHighlights$57$studiomuzitvMainActivity() {
        final HighlightStore.Loaded loaded = HighlightStore.load(this);
        this.ui.post(new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda10
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m1691lambda$previewHighlights$56$studiomuzitvMainActivity(loaded);
            }
        });
    }

    void previewHighlights() {
        page("精彩标记预览");
        this.screen = "preview-list";
        this.status.setText("正在读取标记…");
        this.side.execute(new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m1692lambda$previewHighlights$57$studiomuzitvMainActivity();
            }
        });
    }

    /* renamed from: lambda$openPreview$58$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1673lambda$openPreview$58$studiomuzitvMainActivity() {
        this.previewPlaying = !this.previewPlaying;
        this.pauseButton.setText(this.previewPlaying ? "暂停" : "播放");
    }

    /* renamed from: lambda$openPreview$59$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1674lambda$openPreview$59$studiomuzitvMainActivity() {
        jumpHighlight(false);
    }

    /* renamed from: lambda$openPreview$60$studio-muzi-tv-MainActivity, reason: not valid java name */

    /* synthetic */ void m1676lambda$openPreview$61$studiomuzitvMainActivity() {
        m1675lambda$openPreview$60$studiomuzitvMainActivity();
    }
    /* synthetic */ void m1675lambda$openPreview$60$studiomuzitvMainActivity() {
        jumpHighlight(true);
    }

    /* renamed from: lambda$openPreview$62$studio-muzi-tv-MainActivity, reason: not valid java name */
    /* synthetic */ void m1677lambda$openPreview$62$studiomuzitvMainActivity() {
        if ("preview".equals(this.screen)) {
            if (this.previewPlaying) {
                this.previewPos = Math.min(this.previewDur, this.previewPos + 1000);
            }
            refreshPreviewUi();
            this.ui.postDelayed(this.previewTick, 1000L);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: openPreview, reason: merged with bridge method [inline-methods] */
    public void m1690lambda$previewHighlights$55$studiomuzitvMainActivity(HighlightMap.Entry entry) {
        stopVideo();
        this.screen = "preview";
        this.activeHighlights = new ArrayList(entry.highlights);
        this.markerTimes = new int[this.activeHighlights.size()];
        for (int i = 0; i < this.activeHighlights.size(); i++) {
            this.markerTimes[i] = (int) this.activeHighlights.get(i).timeMs();
        }
        this.pinnedHighlight = -1;
        this.previewDur = 2700000L;
        for (int i2 = 0; i2 < this.activeHighlights.size(); i2++) {
            this.previewDur = Math.max(this.previewDur, this.activeHighlights.get(i2).timeMs() + 30000);
        }
        this.previewPos = 0L;
        this.previewPlaying = true;
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(0xFF000000);
        TextView banner = text(entry.titleContains + " · 标记预览（不播放片源）", 18);
        banner.setBackgroundColor(-1342177280);
        frame.addView(banner, new FrameLayout.LayoutParams(-1, -2, 48));
        this.controls = new LinearLayout(this);
        this.controls.setOrientation(android.widget.LinearLayout.VERTICAL);
        this.controls.setPadding(dp(24.0f), dp(8.0f), dp(24.0f), dp(12.0f));
        this.controls.setBackgroundColor(-586147801);
        this.timeLabel = text("", 18);
        this.controls.addView(this.timeLabel);
        this.highlightBlurb = text("", 16);
        this.highlightBlurb.setTextColor(-7261);
        this.controls.addView(this.highlightBlurb);
        this.seek = new MarkerSeekBar(this);
        this.seek.setMax(1000);
        this.seek.setKeyProgressIncrement(10);
        this.controls.addView(this.seek, new LinearLayout.LayoutParams(-1, dp(38.0f)));
        this.seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { // from class: studio.muzi.tv.MainActivity.4
            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onProgressChanged(SeekBar s, int p, boolean user) {
                if (!user || MainActivity.this.previewDur <= 0) {
                    return;
                }
                MainActivity.this.previewPos = (MainActivity.this.previewDur * p) / 1000;
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStartTrackingTouch(SeekBar s) {
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStopTrackingTouch(SeekBar s) {
            }
        });
        this.markerScroller = new HorizontalScrollView(this);
        this.markerScroller.setFocusable(false);
        this.markerScroller.setHorizontalScrollBarEnabled(false);
        this.markerChips = new LinearLayout(this);
        this.markerChips.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        this.markerScroller.addView(this.markerChips);
        this.controls.addView(this.markerScroller);
        populateMarkerChips(this.markerChips, this.activeHighlights);
        this.highlightActions = new LinearLayout(this);
        LinearLayout row = this.highlightActions;
        this.pauseButton = button("暂停", new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda20
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m1673lambda$openPreview$58$studiomuzitvMainActivity();
            }
        });
        row.addView(this.pauseButton);
        row.addView(button("上一精彩", new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda21
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m1674lambda$openPreview$59$studiomuzitvMainActivity();
            }
        }));
        row.addView(button("下一精彩", new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda23
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m1675lambda$openPreview$60$studiomuzitvMainActivity();
            }
        }));
        row.addView(button("返回", new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda24
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m1676lambda$openPreview$61$studiomuzitvMainActivity();
            }
        }));
        this.controls.addView(row);
        frame.addView(this.controls, new FrameLayout.LayoutParams(-1, -2, 80));
        setContentView(frame);
        showBlurb("方向键选择标记，确认跳转", true);
        refreshPreviewUi();
        showControls();
        this.previewTick = new Runnable() { // from class: studio.muzi.tv.MainActivity$$ExternalSyntheticLambda25
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m1677lambda$openPreview$62$studiomuzitvMainActivity();
            }
        };
        this.ui.postDelayed(this.previewTick, 1000L);
    }

    void refreshPreviewUi() {
        if (this.timeLabel == null || this.seek == null) {
            return;
        }
        this.timeLabel.setText("预览   " + clock(this.previewPos) + " / " + clock(this.previewDur) + (this.previewPlaying ? "   播放中" : "   暂停"));
        this.seek.setProgress(this.previewDur <= 0 ? 0 : (int) ((this.previewPos * 1000) / this.previewDur));
        refreshHighlightDots(this.previewPos, this.previewDur);
        if (this.pauseButton != null) {
            this.pauseButton.setText(this.previewPlaying ? "暂停" : "播放");
        }
    }

    class Home extends View {
        Runnable clockTick;
        RectF[] hit;
        String[] labels;
        Typeface medium;
        Paint p;
        Typeface regular;
        float sx;
        float sy;
        Bitmap wall;

        Home() {
            super(MainActivity.this);
            this.p = new Paint(3);
            // Fixed family dock (Gavin 2026-10-22). Do not add Tencent, Bilibili, LibreTV, live, or HDMI tiles.
            this.labels = new String[]{"木子映界", "YouTube", "全部应用", "设置"};
            this.hit = new RectF[4];
            this.sx = 1.0f;
            this.sy = 1.0f;
            this.regular = Typeface.create("sans-serif", android.graphics.Typeface.NORMAL);
            this.medium = Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL);
            setFocusable(true);
            setFocusableInTouchMode(true);
            try {
                this.wall = BitmapFactory.decodeStream(MainActivity.this.getAssets().open("galaxy.jpg"));
            } catch (Exception e) {
            }
            this.clockTick = new Runnable() { // from class: studio.muzi.tv.MainActivity$Home$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.Home.this.m1714lambda$new$0$studiomuzitvMainActivity$Home();
                }
            };
            postDelayed(this.clockTick, 30000L);
        }

        /* renamed from: lambda$new$0$studio-muzi-tv-MainActivity$Home, reason: not valid java name */
        /* synthetic */ void m1714lambda$new$0$studiomuzitvMainActivity$Home() {
            invalidate();
            postDelayed(this.clockTick, 30000L);
        }

        @Override // android.view.View
        protected void onDetachedFromWindow() {
            removeCallbacks(this.clockTick);
            super.onDetachedFromWindow();
        }

        void txt(Canvas c, String s, float x, float y, float size, int color, boolean bold) {
            this.p.setShader(null);
            this.p.setStyle(Paint.Style.FILL);
            this.p.setColor(color);
            this.p.setTypeface(bold ? this.medium : this.regular);
            this.p.setTextSize(size);
            c.drawText(s, x, y, this.p);
        }

        void box(Canvas c, RectF r, int color, float round) {
            this.p.setShader(null);
            this.p.setStyle(Paint.Style.FILL);
            this.p.setColor(color);
            c.drawRoundRect(r, round, round, this.p);
        }

        /* JADX WARN: Removed duplicated region for block: B:28:0x030c  */
        /* JADX WARN: Removed duplicated region for block: B:31:0x030e  */
        @Override // android.view.View
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        protected void onDraw(Canvas c) {
            float x;
            int i;
            int i2;
            float f;
            float f2;
            int i3;
            boolean z;
            String str;
            float f3;
            DesktopView home;
            Canvas canvas;
            float f4;
            float f5;
            int i4;
            boolean z2;
            String str2;
            float f6;
            Home home2;
            Canvas canvas2;
            float f7;
            int i5;
            Home home3;
            Canvas canvas3;
            float f8;
            float f9;
            float f10;
            super.onDraw(c);
            this.sx = getWidth() / 1920.0f;
            this.sy = getHeight() / 1080.0f;
            c.save();
            c.scale(this.sx, this.sy);
            if (this.wall != null) {
                float ratio = Math.max(1920.0f / this.wall.getWidth(), 1080.0f / this.wall.getHeight());
                float w = this.wall.getWidth() * ratio;
                float h = this.wall.getHeight() * ratio;
                this.p.setColor(-1);
                this.p.setShader(null);
                c.drawBitmap(this.wall, (Rect) null, new RectF((1920.0f - w) / 2.0f, (1080.0f - h) * 0.42f, (1920.0f + w) / 2.0f, ((1080.0f - h) * 0.42f) + h), this.p);
            } else {
                c.drawColor(-15657182);
            }
            this.p.setShader(new LinearGradient(0.0f, 0.0f, 0.0f, 1080.0f, new int[]{1074138134, 0, 0, -1207695085, -234616557}, new float[]{0.0f, 0.26f, 0.52f, 0.85f, 1.0f}, Shader.TileMode.CLAMP));
            int i6 = 5;
            c.drawRect(0.0f, 0.0f, 1920.0f, 1080.0f, this.p);
            this.p.setShader(null);
            boolean z3 = false;
            txt(c, "muzi.studio", 114.0f, 109.0f, 40.0f, -1, true);
            String now = new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date());
            txt(c, now, 1634.0f, 128.0f, 66.0f, -1, false);
            txt(c, new SimpleDateFormat("M月d日 EEEE", Locale.CHINA).format(new Date()), 1647.0f, 166.0f, 19.0f, -3092008, false);
            box(c, new RectF(80.0f, 760.0f, 1840.0f, 1030.0f), -1206839518, 28.0f);
            int slots = this.labels.length;
            float tileW = 300.0f;
            float gap = 48.0f;
            float total = slots * tileW + (slots - 1) * gap;
            float start = (1920.0f - total) / 2.0f;
            for (int i7 = 0; i7 < slots; i7++) {
                float x2 = start + i7 * (tileW + gap);
                boolean focus = MainActivity.this.selected == i7;
                RectF tile = focus
                        ? new RectF(x2 - 8.0f, 792.0f, x2 + tileW + 8.0f, 968.0f)
                        : new RectF(x2, 804.0f, x2 + tileW, 956.0f);
                this.hit[i7] = new RectF(x2 - 8.0f, 784.0f, x2 + tileW + 8.0f, 1020.0f);
                int color = i7 == 0 ? Color.rgb(176, 116, 32)
                        : i7 == 1 ? Color.rgb(168, 42, 42)
                        : i7 == 2 ? Color.rgb(36, 74, 116)
                        : Color.rgb(54, 62, 76);
                box(c, tile, color, 20.0f);
                if (focus) {
                    this.p.setColor(-789249);
                    this.p.setStyle(Paint.Style.STROKE);
                    this.p.setStrokeWidth(4.0f);
                    c.drawRoundRect(new RectF(tile.left - 5.0f, tile.top - 5.0f, tile.right + 5.0f, tile.bottom + 5.0f), 24.0f, 24.0f, this.p);
                    this.p.setStyle(Paint.Style.FILL);
                }
                float cx = x2 + tileW / 2.0f;
                float cy = focus ? 860.0f : 872.0f;
                if (i7 == 0) drawFongMiMark(c, cx, cy);
                else if (i7 == 1) drawYoutubeMark(c, cx, cy);
                else if (i7 == 2) drawAppsMark(c, cx, cy);
                else drawGear(c, cx, cy);
                this.p.setTextSize(26.0f);
                this.p.setTypeface(this.medium);
                float tw = this.p.measureText(this.labels[i7]);
                txt(c, this.labels[i7], x2 + ((tileW - tw) / 2.0f), 1004.0f, 26.0f, focus ? -1 : -2039584, true);
            }
            this.p.setTextSize(18.0f);
            this.p.setTypeface(this.regular);
            String hint = "← → 选择    确认打开";
            float hw = this.p.measureText(hint);
            txt(c, hint, (1920.0f - hw) / 2.0f, 1064.0f, 18.0f, -5788229, false);
            c.restore();
        }

        /** 蜂蜜：圆角底板 + 播放三角。名字只出现在坞标签。 */
        void drawFongMiMark(Canvas c, float cx, float cy) {
            RectF plate = new RectF(cx - 46.0f, cy - 36.0f, cx + 46.0f, cy + 20.0f);
            box(c, plate, -15231982, 16.0f);
            this.p.setColor(-1182206);
            this.p.setStyle(Paint.Style.STROKE);
            this.p.setStrokeWidth(3.0f);
            c.drawRoundRect(plate, 16.0f, 16.0f, this.p);
            this.p.setStyle(Paint.Style.FILL);
            this.p.setColor(-1);
            Path play = new Path();
            play.moveTo(cx - 10.0f, cy - 22.0f);
            play.lineTo(cx - 10.0f, cy + 4.0f);
            play.lineTo(cx + 16.0f, cy - 9.0f);
            play.close();
            c.drawPath(play, this.p);
        }

        void drawYoutubeMark(Canvas c, float cx, float cy) {
            box(c, new RectF(cx - 48.0f, cy - 30.0f, cx + 48.0f, cy + 30.0f), Color.rgb(245, 245, 245), 14.0f);
            tri(c, cx - 12.0f, cy - 16.0f, 28.0f, 32.0f, Color.rgb(200, 32, 32));
        }

        void drawAppsMark(Canvas c, float cx, float cy) {
            float s = 22.0f;
            float g = 10.0f;
            float left = cx - s - (g / 2.0f);
            float top = cy - s - (g / 2.0f);
            for (int row = 0; row < 2; row++) {
                for (int col = 0; col < 2; col++) {
                    float l = left + col * (s + g);
                    float t = top + row * (s + g);
                    box(c, new RectF(l, t, l + s, t + s), -1, 4.0f);
                }
            }
        }

        void drawGear(Canvas c, float cx, float cy) {
            this.p.setColor(-1);
            this.p.setStyle(Paint.Style.STROKE);
            this.p.setStrokeWidth(6.0f);
            c.drawCircle(cx, cy, 22.0f, this.p);
            this.p.setStyle(Paint.Style.FILL);
            c.drawCircle(cx, cy, 6.0f, this.p);
            this.p.setStrokeWidth(5.0f);
            this.p.setStyle(Paint.Style.STROKE);
            for (int i = 0; i < 4; i++) {
                double a = i * Math.PI / 2.0;
                c.drawLine(cx + (float) Math.cos(a) * 18.0f, cy + (float) Math.sin(a) * 18.0f,
                        cx + (float) Math.cos(a) * 34.0f, cy + (float) Math.sin(a) * 34.0f, this.p);
            }
            this.p.setStyle(Paint.Style.FILL);
        }

        void tri(Canvas c, float x, float y, float w, float h, int color) {
            this.p.setColor(color);
            this.p.setStyle(Paint.Style.FILL);
            Path path = new Path();
            path.moveTo(x, y);
            path.lineTo(x + w, (h / 2.0f) + y);
            path.lineTo(x, y + h);
            path.close();
            c.drawPath(path, this.p);
        }
        @Override // android.view.View, android.view.KeyEvent.Callback
        public boolean onKeyDown(int key, KeyEvent e) {
            int n = this.labels.length;
            int s = MainActivity.this.selected;
            if (s < 0 || s >= n) s = 0;
            if (key == 22) {
                s = Math.min(n - 1, s + 1);
            } else if (key == 21) {
                s = Math.max(0, s - 1);
            } else if (key == 19 || key == 20) {
                return true;
            } else if (key == 23 || key == 66) {
                MainActivity.this.openHomeItem(s);
                return true;
            } else {
                return super.onKeyDown(key, e);
            }
            MainActivity.this.selected = s;
            invalidate();
            return true;
        }


        @Override // android.view.View
        public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction() == 1) {
                for (int i = 0; i < this.hit.length; i++) {
                    if (this.hit[i] != null && this.hit[i].contains(e.getX() / this.sx, e.getY() / this.sy)) {
                        MainActivity.this.selected = i;
                        invalidate();
                        MainActivity.this.openHomeItem(i);
                        return true;
                    }
                }
            }
            return true;
        }
    }
}
