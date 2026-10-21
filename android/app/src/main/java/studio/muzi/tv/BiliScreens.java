package studio.muzi.tv;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.CookieManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/* loaded from: classes3.dex */
final class BiliScreens {
    static final String HOME = "https://www.bilibili.com";
    static final String MOBILE = "https://m.bilibili.com";
    static final String MSEARCH = "https://m.bilibili.com/search?keyword=";
    static final String SEARCH = "https://search.bilibili.com/all?keyword=";

    private BiliScreens() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void show(final MainActivity a) {
        a.page("哔哩哔哩");
        a.screen = "bili";
        LinearLayout shelf = new LinearLayout(a);
        shelf.setOrientation(1);
        a.content.addView(shelf);
        int mark = a.content.getChildCount();
        int apps = addInstalled(a);
        View firstLauncher = a.content.getChildCount() > mark ? a.content.getChildAt(mark) : null;
        a.addButton(a.content, "电视浏览器打开 B站首页", new Runnable() { // from class: studio.muzi.tv.BiliScreens$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                a.openExternal(BiliScreens.HOME);
            }
        });
        a.addButton(a.content, "应用内打开首页（经常不能播放）", new Runnable() { // from class: studio.muzi.tv.BiliScreens$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                BiliScreens.openWeb(a, BiliScreens.MOBILE);
            }
        });
        LinearLayout row = new LinearLayout(a);
        final EditText input = new EditText(a);
        input.setSingleLine(true);
        input.setTextColor(-1);
        input.setHintTextColor(-6444612);
        input.setHint("搜索 B站，例如 纪录片");
        row.addView(input, new LinearLayout.LayoutParams(0, a.dp(60.0f), 1.0f));
        row.addView(a.button("浏览器搜索", new Runnable() { // from class: studio.muzi.tv.BiliScreens$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                BiliScreens.search(a, input, false);
            }
        }));
        row.addView(a.button("应用内搜索", new Runnable() { // from class: studio.muzi.tv.BiliScreens$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                BiliScreens.search(a, input, true);
            }
        }));
        a.content.addView(row);
        a.content.addView(a.text("Android 6 / 米家激光投影的系统网页组件太旧，应用内页面通常打不开视频。要看 B站，请安装「云视听小电视」，或用电视浏览器。应用内页面只适合试试能不能打开，菜单键可回到顶部按钮。", 16));
        DiscoverScreens.fillBili(a, shelf, firstLauncher);
        a.status.setText(apps > 0 ? "已发现 " + apps + " 个 B站客户端 · 确认打开 · 返回上一页" : "未安装 B站客户端 · 可用电视浏览器打开网站");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void openWeb(final MainActivity mainActivity, final String url) {
        mainActivity.stopVideo();
        mainActivity.screen = "biliweb";
        LinearLayout root = new LinearLayout(mainActivity);
        root.setOrientation(1);
        root.setBackgroundColor(0xFF000000);
        LinearLayout bar = new LinearLayout(mainActivity);
        bar.setOrientation(0);
        bar.setPadding(mainActivity.dp(6.0f), mainActivity.dp(4.0f), mainActivity.dp(6.0f), mainActivity.dp(4.0f));
        bar.setBackgroundColor(-15722457);
        bar.addView(mainActivity.button("返回", new Runnable() { // from class: studio.muzi.tv.BiliScreens$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                BiliScreens.show(mainActivity);
            }
        }));
        bar.addView(mainActivity.button("后退", new Runnable() { // from class: studio.muzi.tv.BiliScreens$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                BiliScreens.lambda$openWeb$5(mainActivity);
            }
        }));
        bar.addView(mainActivity.button("前进", new Runnable() { // from class: studio.muzi.tv.BiliScreens$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                BiliScreens.lambda$openWeb$6(mainActivity);
            }
        }));
        bar.addView(mainActivity.button("刷新", new Runnable() { // from class: studio.muzi.tv.BiliScreens$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                BiliScreens.lambda$openWeb$7(mainActivity);
            }
        }));
        bar.addView(mainActivity.button("电视浏览器", new Runnable() { // from class: studio.muzi.tv.BiliScreens$$ExternalSyntheticLambda9
            @Override // java.lang.Runnable
            public final void run() {
                mainActivity.openExternal(url);
            }
        }));
        HorizontalScrollView scroller = new HorizontalScrollView(mainActivity);
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.setFocusable(false);
        scroller.addView(bar);
        final TextView status = mainActivity.text("正在打开…", 16);
        root.addView(scroller);
        root.addView(status);
        WebView web = new WebView(mainActivity);
        web.setLayerType(1, null);
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setMixedContentMode(2);
        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(web, true);
        web.setWebViewClient(new WebViewClient() { // from class: studio.muzi.tv.BiliScreens.1
            @Override // android.webkit.WebViewClient
            public boolean shouldOverrideUrlLoading(WebView view, String next) {
                if (next == null) {
                    return true;
                }
                if (next.startsWith("http://") || next.startsWith("https://")) {
                    return false;
                }
                if (next.startsWith("bilibili://")) {
                    try {
                        mainActivity.startActivity(new Intent("android.intent.action.VIEW", Uri.parse(next)));
                    } catch (Exception e) {
                        mainActivity.toast("这个链接需要 B站客户端");
                    }
                }
                return true;
            }

            @Override // android.webkit.WebViewClient
            public void onPageFinished(WebView view, String finished) {
                String title = view.getTitle();
                status.setText((title == null || title.length() == 0) ? String.valueOf(finished) : title);
            }

            @Override // android.webkit.WebViewClient
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request == null || !request.isForMainFrame() || error == null) {
                    return;
                }
                status.setText("打开失败：" + ((Object) error.getDescription()));
            }
        });
        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        mainActivity.biliBar = bar;
        mainActivity.biliWeb = web;
        mainActivity.setContentView(root);
        web.loadUrl(url);
        bar.getChildAt(0).requestFocus();
    }

    static /* synthetic */ void lambda$openWeb$5(MainActivity a) {
        if (a.biliWeb == null || !a.biliWeb.canGoBack()) {
            return;
        }
        a.biliWeb.goBack();
    }

    static /* synthetic */ void lambda$openWeb$6(MainActivity a) {
        if (a.biliWeb == null || !a.biliWeb.canGoForward()) {
            return;
        }
        a.biliWeb.goForward();
    }

    static /* synthetic */ void lambda$openWeb$7(MainActivity a) {
        if (a.biliWeb != null) {
            a.biliWeb.reload();
        }
    }

    static boolean onKey(MainActivity a, KeyEvent e) {
        if (!"biliweb".equals(a.screen) || a.biliBar == null || a.biliBar.getChildCount() == 0) {
            return false;
        }
        int key = e.getKeyCode();
        if (key != 82 && key != 165) {
            return false;
        }
        a.biliBar.getChildAt(0).requestFocus();
        return true;
    }

    static boolean onBack(MainActivity a) {
        if (!"biliweb".equals(a.screen)) {
            return false;
        }
        if (a.biliWeb != null && a.biliWeb.canGoBack()) {
            a.biliWeb.goBack();
            return true;
        }
        show(a);
        return true;
    }

    static void destroy(MainActivity a) {
        WebView web = a.biliWeb;
        a.biliWeb = null;
        a.biliBar = null;
        if (web == null) {
            return;
        }
        web.stopLoading();
        web.setWebViewClient(null);
        ViewParent parent = web.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(web);
        }
        web.destroy();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void search(MainActivity a, EditText input, boolean inApp) {
        String q = input.getText().toString().trim();
        if (q.isEmpty()) {
            a.toast("请输入关键词");
            return;
        }
        String url = (inApp ? MSEARCH : SEARCH) + a.enc(q);
        if (!inApp) {
            a.openExternal(url);
        } else {
            openWeb(a, url);
        }
    }

    private static int addInstalled(MainActivity a) {
        int count = 0;
        Set<String> seen = new HashSet<>();
        String[] known = {"com.xiaodianshi.tv.yst", "tv.danmaku.bili"};
        for (String str : known) {
            if (addLaunch(a, str, null, seen)) {
                count++;
            }
        }
        String[] categories = {"android.intent.category.LEANBACK_LAUNCHER", "android.intent.category.LAUNCHER"};
        for (String str2 : categories) {
            Intent query = new Intent("android.intent.action.MAIN").addCategory(str2);
            List<ResolveInfo> list = a.getPackageManager().queryIntentActivities(query, 0);
            for (int i = 0; i < list.size(); i++) {
                ResolveInfo info = list.get(i);
                if (info.activityInfo != null) {
                    CharSequence label = info.loadLabel(a.getPackageManager());
                    String name = label == null ? "" : label.toString();
                    String blob = (name + " " + info.activityInfo.packageName).toLowerCase(Locale.ROOT);
                    if ((blob.contains("bilibili") || name.contains("哔哩") || name.contains("云视听")) && addLaunch(a, info.activityInfo.packageName, name, seen)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static boolean addLaunch(final MainActivity a, final String pkg, String name, Set<String> seen) {
        Intent launch = a.getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
        if (launch == null) {
            launch = a.getPackageManager().getLaunchIntentForPackage(pkg);
        }
        if (launch == null || !seen.add(pkg)) {
            return false;
        }
        if (name == null || name.trim().isEmpty()) {
            name = labelOf(a, pkg);
        }
        String caption = name;
        a.addButton(a.content, "打开 " + caption, new Runnable() { // from class: studio.muzi.tv.BiliScreens$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                a.launch(pkg);
            }
        });
        return true;
    }

    private static String labelOf(MainActivity a, String pkg) {
        try {
            CharSequence label = a.getPackageManager().getApplicationLabel(a.getPackageManager().getApplicationInfo(pkg, 0));
            if (label != null && label.length() > 0) {
                return label.toString();
            }
        } catch (Exception e) {
        }
        return pkg;
    }
}
