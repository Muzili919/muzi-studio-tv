package studio.muzi.tv;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import org.json.JSONArray;
import org.json.JSONObject;
import studio.muzi.tv.CmsCatalog;
import studio.muzi.tv.LiveGuide;

/* loaded from: classes3.dex */
final class DiscoverScreens {
    static final long CACHE_MS = 600000;
    static final String CN_M3U = BuildConfig.BASE_URL + "/live-example.m3u";
    static final String HK_M3U = "https://iptv-org.github.io/iptv/countries/hk.m3u";
    static final String[][] SOURCES = {
        new String[]{"lz", "量子", "https://cms-c.example.com/api.php/provide/vod"},
        new String[]{"ms", "木子体育", BuildConfig.BASE_URL + "/sports/"}
    };
    /** 顶栏六入口：按钮位置 -> 内部 tab（电影0 电视剧1 综艺2 动漫3 体育4 直播5）；-1 = 推荐。 */
    static final int[] TAB_IDS = {0, 1, 3, 2, 4, 5};
    /** 子分类与公网 FongMi 配置一致（量子类名）。 */
    static final String[][] SUBS = {
        {"动作片", "喜剧片", "恐怖片", "科幻片", "爱情片", "剧情片", "战争片", "记录片"},
        {"国产剧", "欧美剧", "香港剧", "韩国剧", "台湾剧", "日本剧", "海外剧", "泰国剧"},
        {"大陆综艺", "港台综艺", "日韩综艺", "欧美综艺"},
        {"日韩动漫", "国产动漫", "海外动漫", "欧美动漫", "港台动漫", "动画片"}
    };
    /** 点播内部体育子类；默认斯诺克（源站台球类常含斯诺克）。 */
    static final String[] SPORTS_CHIPS = {"足球", "篮球", "乒乓球", "斯诺克", "羽毛球", "网球", "台球"};
    static final String SPORTS_DEFAULT = "斯诺克";
    static final String[] KINDS = {CmsCatalog.MOVIE, CmsCatalog.SERIES, CmsCatalog.VARIETY, CmsCatalog.ANIME};
    static final String[] LABELS = {"电影", "电视剧", "综艺", "动漫"};
    static final Map<String, Hit> CACHE = Collections.synchronizedMap(new LinkedHashMap<String, Hit>(32, 0.75f, true) { // from class: studio.muzi.tv.DiscoverScreens.1
        @Override // java.util.LinkedHashMap
        protected boolean removeEldestEntry(Map.Entry<String, Hit> eldest) {
            return size() > 64;
        }
    });

    static final class Hit {
        final long at;
        final String body;

        Hit(String body, long at) {
            this.body = body;
            this.at = at;
        }
    }

    private DiscoverScreens() {
    }

    static void openLibrary(final MainActivity a) {
        // 0-4 点播分类（含体育）；直播/B站不落库为 libraryTab
        if (a.libraryTab < -1 || a.libraryTab > 4) {
            a.libraryTab = -1;
        }
        a.page("muzi.studio");
        a.screen = "library";
        addLibraryChrome(a);
        HorizontalScrollView horizontalScrollView = new HorizontalScrollView(a);
        horizontalScrollView.setHorizontalScrollBarEnabled(false);
        horizontalScrollView.setFocusable(false);
        horizontalScrollView.setDescendantFocusability(262144);
        LinearLayout nav = new LinearLayout(a);
        horizontalScrollView.addView(nav);
        a.content.addView(horizontalScrollView);
        // 点播页内部切换分类（主页星空不放这些）；直播/B站仍挂在点播顶栏末尾
        String[] tabs = {"电影", "电视剧", "动漫", "综艺", "体育", "直播"};
        int restore = a.libraryTab;
        for (int i = 0; i < tabs.length; i++) {
            final int n = i;
            Button tab = a.button(tabs[i], new Runnable() { // from class: studio.muzi.tv.DiscoverScreens$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    DiscoverScreens.openTab(a, TAB_IDS[n]);
                }
            });
            tab.setTextSize(15.0f);
            tab.setSingleLine(true);
            tab.setEllipsize(TextUtils.TruncateAt.END);
            tab.setPadding(a.dp(10.0f), a.dp(6.0f), a.dp(10.0f), a.dp(6.0f));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, a.dp(48.0f));
            lp.setMargins(a.dp(3.0f), a.dp(6.0f), a.dp(3.0f), a.dp(6.0f));
            nav.addView(tab, lp);
        }
        String saved = a.getPreferences(0).getString("continue", "");
        if (!saved.isEmpty()) {
            try {
                final JSONObject history = new JSONObject(saved);
                a.content.addView(a.text("继续观看", 20));
                a.addButton(a.content, history.optString("title") + " · 第 " + (history.optInt("episode") + 1) + " 集 · " + a.clock(history.optLong("position")), new Runnable() { // from class: studio.muzi.tv.DiscoverScreens$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        DiscoverScreens.lambda$openLibrary$1(a, history);
                    }
                });
            } catch (Exception e) {
            }
        }
        a.posters = new LinearLayout(a);
        a.posters.setOrientation(1);
        a.posters.setClipChildren(false);
        a.content.addView(a.posters);
        a.shelfNav = nav;
        markTabs(nav, restore);
        openTab(a, restore);
        int pos = 0;
        for (int k = 0; k < TAB_IDS.length; k++) if (TAB_IDS[k] == restore) pos = k;
        if (nav.getChildCount() > pos) {
            nav.getChildAt(pos).requestFocus();
        }
    }

    static /* synthetic */ void lambda$openLibrary$1(MainActivity a, JSONObject history) {
        try {
            a.currentDetail = history.getJSONObject("detail");
            a.episodes = a.currentDetail.getJSONArray("episodes");
            a.resumeAt = history.optLong("position");
            a.m1695lambda$renderEpisodes$33$studiomuzitvMainActivity(Math.min(history.optInt("episode"), a.episodes.length() - 1));
        } catch (Exception e) {
            a.toast("记录不可用，请重新搜索");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void openSearch(final MainActivity a) {
        a.page("搜索");
        a.screen = "search";
        LinearLayout row = new LinearLayout(a);
        a.searchBox = new EditText(a);
        a.searchBox.setSingleLine(true);
        a.searchBox.setTextColor(-1);
        a.searchBox.setTextSize(19.0f);
        a.searchBox.setHintTextColor(-6444612);
        a.searchBox.setHint("电影、电视剧、综艺");
        row.addView(a.searchBox, new LinearLayout.LayoutParams(0, a.dp(52.0f), 1.0f));
        row.addView(a.button("搜索", new Runnable() { // from class: studio.muzi.tv.DiscoverScreens$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                a.search(a.searchBox.getText().toString());
            }
        }));
        a.content.addView(row);
        a.searchBox.setOnEditorActionListener(new TextView.OnEditorActionListener() { // from class: studio.muzi.tv.DiscoverScreens$$ExternalSyntheticLambda5
            @Override // android.widget.TextView.OnEditorActionListener
            public final boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                return DiscoverScreens.lambda$openSearch$3(a, textView, i, keyEvent);
            }
        });
        a.status.setText("输入片名后确认");
        a.searchBox.requestFocus();
    }

    static /* synthetic */ boolean lambda$openSearch$3(MainActivity a, TextView v, int action, KeyEvent event) {
        a.search(a.searchBox.getText().toString());
        return true;
    }

    static void addLibraryChrome(final MainActivity a) {
        Object parent = a.content.getParent();
        if (parent instanceof View) {
            ViewParent base = ((View) parent).getParent();
            if (base instanceof ViewGroup) {
                ViewGroup root = (ViewGroup) base;
                if (root.getChildCount() == 0 || !(root.getChildAt(0) instanceof LinearLayout)) {
                    return;
                }
                LinearLayout top = (LinearLayout) root.getChildAt(0);
                top.setGravity(16);
                View search = iconButton(a, "search", new Runnable() { // from class: studio.muzi.tv.DiscoverScreens$$ExternalSyntheticLambda7
                    @Override // java.lang.Runnable
                    public final void run() {
                        DiscoverScreens.openSearch(a);
                    }
                });
                View gear = iconButton(a, "gear", new Runnable() { // from class: studio.muzi.tv.DiscoverScreens$$ExternalSyntheticLambda8
                    @Override // java.lang.Runnable
                    public final void run() {
                        a.m1707lambda$showSettings$6$studiomuzitvMainActivity();
                    }
                });
                int back = top.getChildCount() - 1;
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(a.dp(48.0f), a.dp(48.0f));
                lp.setMargins(a.dp(6.0f), 0, a.dp(4.0f), 0);
                top.addView(search, back, lp);
                LinearLayout.LayoutParams gearLp = new LinearLayout.LayoutParams(a.dp(48.0f), a.dp(48.0f));
                gearLp.setMargins(0, 0, a.dp(8.0f), 0);
                top.addView(gear, back + 1, gearLp);
            }
        }
    }

    static View iconButton(MainActivity a, final String kind, final Runnable action) {
        View v = new View(a) { // from class: studio.muzi.tv.DiscoverScreens.2
            final Paint paint = new Paint(1);

            @Override // android.view.View
            protected void onDraw(Canvas canvas) {
                float w = getWidth();
                float h = getHeight();
                this.paint.setStyle(Paint.Style.FILL);
                this.paint.setColor(isFocused() ? -12956568 : -14405564);
                canvas.drawCircle(w / 2.0f, h / 2.0f, (Math.min(w, h) / 2.0f) - 2.0f, this.paint);
                this.paint.setStyle(Paint.Style.STROKE);
                this.paint.setStrokeCap(Paint.Cap.ROUND);
                this.paint.setStrokeWidth(isFocused() ? 3.4f : 2.4f);
                this.paint.setColor(isFocused() ? -670639 : -1577482);
                if (!"gear".equals(kind)) {
                    DiscoverScreens.drawGlass(canvas, this.paint, w, h);
                } else {
                    DiscoverScreens.drawGear(canvas, this.paint, w, h);
                }
            }
        };
        v.setFocusable(true);
        v.setClickable(true);
        v.setContentDescription("gear".equals(kind) ? "设置" : "搜索");
        v.setOnClickListener(new View.OnClickListener() { // from class: studio.muzi.tv.DiscoverScreens$$ExternalSyntheticLambda14
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                action.run();
            }
        });
        v.setOnFocusChangeListener(new View.OnFocusChangeListener() { // from class: studio.muzi.tv.DiscoverScreens$$ExternalSyntheticLambda15
            @Override // android.view.View.OnFocusChangeListener
            public final void onFocusChange(View view, boolean z) {
                view.invalidate();
            }
        });
        return v;
    }

    static void drawGlass(Canvas canvas, Paint paint, float w, float h) {
        float r = Math.min(w, h) * 0.16f;
        canvas.drawCircle(w * 0.42f, h * 0.42f, r, paint);
        canvas.drawLine((w * 0.42f) + (r * 0.72f), (0.42f * h) + (0.72f * r), w * 0.74f, h * 0.74f, paint);
    }

    static void drawGear(Canvas canvas, Paint paint, float w, float h) {
        float cx = w / 2.0f;
        float cy = h / 2.0f;
        float r = Math.min(w, h) * 0.14f;
        canvas.drawCircle(cx, cy, r, paint);
        for (int i = 0; i < 8; i++) {
            double ang = (i * 3.141592653589793d) / 4.0d;
            canvas.drawLine(cx + (((float) Math.cos(ang)) * (r + 1.0f)), cy + (((float) Math.sin(ang)) * (1.0f + r)), cx + (((float) Math.cos(ang)) * (r + 7.0f)), cy + (((float) Math.sin(ang)) * (7.0f + r)), paint);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void openTab(MainActivity a, int tab) {
        // 电影0 电视剧1 综艺2 动漫3 体育4 直播5 B站6
        if (tab == 5) {
            a.showLive();
            return;
        }
        if (tab == 6) {
            BiliScreens.show(a);
            return;
        }
        a.libraryTab = tab;
        if (a.shelfNav != null) {
            markTabs(a.shelfNav, tab);
        }
        loadTab(a, tab);
    }

    private static void markTabs(LinearLayout nav, int selected) {
        int i = 0;
        while (i < nav.getChildCount()) {
            if (nav.getChildAt(i) instanceof Button) {
                Button tab = (Button) nav.getChildAt(i);
                tab.setTextColor((i < TAB_IDS.length && TAB_IDS[i] == selected) ? -670639 : -1);
            }
            i++;
        }
    }


    private static void loadSports(final MainActivity a, final long gen, final int shelf, String initialChip) {
        final LinearLayout chips = new LinearLayout(a);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        a.posters.addView(chips);
        final LinearLayout host = new LinearLayout(a);
        host.setOrientation(LinearLayout.VERTICAL);
        host.setClipChildren(false);
        a.posters.addView(host);
        final String[] selected = {initialChip == null ? SPORTS_DEFAULT : initialChip};
        for (int i = 0; i < SPORTS_CHIPS.length; i++) {
            final String chip = SPORTS_CHIPS[i];
            Button b = a.button(chip, new Runnable() {
                @Override public void run() {
                    selected[0] = chip;
                    markSportChips(chips, chip);
                    fetchSports(a, gen, shelf, host, chip);
                }
            });
            b.setTextSize(14.0f);
            b.setPadding(a.dp(12.0f), a.dp(6.0f), a.dp(12.0f), a.dp(6.0f));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, a.dp(44.0f));
            lp.setMargins(a.dp(4.0f), a.dp(8.0f), a.dp(4.0f), a.dp(8.0f));
            chips.addView(b, lp);
        }
        markSportChips(chips, selected[0]);
        fetchSports(a, gen, shelf, host, selected[0]);
    }

    private static void markSportChips(LinearLayout chips, String selected) {
        for (int i = 0; i < chips.getChildCount(); i++) {
            if (chips.getChildAt(i) instanceof Button) {
                Button b = (Button) chips.getChildAt(i);
                boolean on = selected.equals(b.getText().toString());
                b.setTextColor(on ? -670639 : -1);
            }
        }
    }

    private static void fetchSports(final MainActivity a, final long gen, final int shelf, final LinearLayout host, final String chip) {
        host.removeAllViews();
        final TextView note = a.text("正在检索「" + chip + "」… 斯诺克优先量子/暴风（新赛季列表），年份看标题与 vod_year。", 15);
        host.addView(note);
        final LinearLayout rows = new LinearLayout(a);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setClipChildren(false);
        host.addView(rows);
        final List<CmsCatalog.Item> items = new ArrayList<>();
        final int[] left = {SOURCES.length};
        final List<String> failed = new ArrayList<>();
        final String[] queries = sportQueries(chip);
        final int[] order = sportsSourceOrder(chip);
        left[0] = order.length;
        for (int s = 0; s < order.length; s++) {
            final int index = order[s];
            run(a, new Runnable() {
                @Override public void run() {
                    List<CmsCatalog.Item> batch = new ArrayList<>();
                    String err = null;
                    int errors = 0;
                    for (String q : queries) {
                        try {
                            String body = cachedGet(query(SOURCES[index][2], "ac=videolist&wd=" + a.enc(q) + "&pg=1"));
                            List<CmsCatalog.Item> parsed = CmsCatalog.parse(body, SOURCES[index][0], SOURCES[index][1], SOURCES[index][2]);
                            for (CmsCatalog.Item it : parsed) {
                                if (keepSportItem(it, chip)) {
                                    batch.add(it);
                                }
                            }
                        } catch (Exception e) {
                            errors++;
                        }
                    }
                    if (batch.isEmpty() && errors > 0) {
                        err = SOURCES[index][1];
                    }
                    final String failedName = err;
                    final List<CmsCatalog.Item> found = batch;
                    a.ui.post(new Runnable() {
                        @Override public void run() {
                            if (gen != a.loadGeneration || shelf != a.shelfGeneration) {
                                return;
                            }
                            if (failedName != null && !failed.contains(failedName)) {
                                failed.add(failedName);
                            }
                            items.addAll(found);
                            left[0] = left[0] - 1;
                            if (left[0] > 0) {
                                return;
                            }
                            List<CmsCatalog.Item> ranked = rankSports(CmsCatalog.dedupe(items), chip);
                            rows.removeAllViews();
                            if (ranked.isEmpty()) {
                                note.setText("没有找到可播的「" + chip + "」。斯诺克源站多为 2023–2024 旧赛；若没有 2025–2026 不会伪造年份。"
                                        + (failed.isEmpty() ? "" : (" 失败源：" + failed)));
                                a.status.setText("体育 · 无结果");
                                return;
                            }
                            int y26 = 0, y25 = 0, yOther = 0;
                            for (CmsCatalog.Item it : ranked) {
                                int y = titleYear(it);
                                if (y >= 2026) y26++;
                                else if (y >= 2025) y25++;
                                else yOther++;
                            }
                            note.setText("共 " + ranked.size() + " 条（标题年 2026:" + y26 + " / 2025:" + y25 + " / 更早或未知:" + yOther
                                    + "）。按年份优先再按更新时间。斯诺克也会搜「台球」。"
                                    + (failed.isEmpty() ? "" : (" 失败：" + failed)));
                            a.status.setText("体育 · " + chip + " · " + ranked.size());
                            // 简单一行海报
                            LinearLayout row = new LinearLayout(a);
                            row.setOrientation(LinearLayout.HORIZONTAL);
                            row.setClipChildren(false);
                            HorizontalScrollView sc = new HorizontalScrollView(a);
                            sc.setHorizontalScrollBarEnabled(false);
                            sc.addView(row);
                            rows.addView(a.text(chip + " · 最新优先", 18));
                            rows.addView(sc);
                            int n = Math.min(ranked.size(), 24);
                            for (int i = 0; i < n; i++) {
                                addSportCard(a, row, ranked.get(i), gen);
                            }
                        }
                    });
                }
            });
        }
    }

    private static String[] sportQueries(String chip) {
        if ("斯诺克".equals(chip)) {
            return new String[]{"斯诺克", "台球"};
        }
        if ("台球".equals(chip)) {
            return new String[]{"台球", "斯诺克"};
        }
        if ("羽毛球".equals(chip)) {
            return new String[]{"羽毛球", "羽毛球公开赛", "羽联", "苏迪曼杯", "尤伯杯"};
        }
        return new String[]{chip};
    }

    /** 斯诺克/台球：量子、暴风列表新；红牛台球几乎全是 2023。足球篮球：红牛优先。 */
    private static int[] sportsSourceOrder(String chip) {
        String[] prefer;
        if ("斯诺克".equals(chip) || "台球".equals(chip)) {
            prefer = new String[]{"lz", "bf", "360", "wj", "hn", "ff"};
        } else if ("羽毛球".equals(chip)) {
            prefer = new String[]{"lz", "bf", "wj", "hn", "360", "ff"};
        } else if ("足球".equals(chip) || "篮球".equals(chip)) {
            prefer = new String[]{"hn", "lz", "bf", "360", "wj", "ff"};
        } else {
            prefer = new String[]{"lz", "bf", "hn", "360", "wj", "ff"};
        }
        int[] order = new int[SOURCES.length];
        boolean[] used = new boolean[SOURCES.length];
        int n = 0;
        for (String key : prefer) {
            for (int i = 0; i < SOURCES.length; i++) {
                if (!used[i] && key.equals(SOURCES[i][0])) {
                    order[n++] = i;
                    used[i] = true;
                }
            }
        }
        for (int i = 0; i < SOURCES.length; i++) {
            if (!used[i]) {
                order[n++] = i;
            }
        }
        return order;
    }

    private static boolean keepSportItem(CmsCatalog.Item it, String chip) {
        if (it == null || it.name == null) return false;
        String name = it.name;
        String type = it.typeName == null ? "" : it.typeName;
        // 过滤动漫/动画误伤（如排球少年一类）
        if (type.contains("动漫") || type.contains("动画") || name.contains("少年") || name.contains("动漫")) {
            return false;
        }
        if ("斯诺克".equals(chip) || "台球".equals(chip)) {
            return name.contains("斯诺克") || name.contains("台球") || type.contains("台球") || type.contains("斯诺克") || type.contains("体育");
        }
        if ("羽毛球".equals(chip)) {
            // 排除明显影视剧；保留公开赛/羽联/杯赛等
            if (name.contains("横扫羽毛球场") || name.contains("老爸") || type.contains("剧") || type.contains("电影")) {
                return false;
            }
            return name.contains("羽毛球") || name.contains("羽联") || name.contains("苏迪曼") || name.contains("尤伯")
                    || name.contains("汤姆斯") || type.contains("羽毛球") || type.contains("体育");
        }
        return name.contains(chip) || type.contains(chip) || type.contains("体育");
    }

    private static int titleYear(CmsCatalog.Item it) {
        int best = 0;
        if (it.year != null) {
            try {
                String y = it.year.trim();
                if (y.length() >= 4) best = Integer.parseInt(y.substring(0, 4));
            } catch (Exception ignored) {}
        }
        if (it.name != null) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(20[0-3][0-9])").matcher(it.name);
            while (m.find()) {
                try {
                    int y = Integer.parseInt(m.group(1));
                    if (y > best) best = y;
                } catch (Exception ignored) {}
            }
        }
        return best;
    }

    private static int sportSourcePriority(String key) {
        if ("lz".equals(key)) return 0;
        if ("bf".equals(key)) return 1;
        if ("360".equals(key) || "wj".equals(key)) return 2;
        if ("hn".equals(key)) return 3;
        return 4;
    }

    private static List<CmsCatalog.Item> rankSports(List<CmsCatalog.Item> items, String chip) {
        List<CmsCatalog.Item> copy = new ArrayList<>(items);
        Collections.sort(copy, new java.util.Comparator<CmsCatalog.Item>() {
            @Override public int compare(CmsCatalog.Item a, CmsCatalog.Item b) {
                int ya = titleYear(a);
                int yb = titleYear(b);
                // 2025/2026 优先桶
                int ba = ya >= 2026 ? 3 : ya >= 2025 ? 2 : ya >= 2024 ? 1 : 0;
                int bb = yb >= 2026 ? 3 : yb >= 2025 ? 2 : yb >= 2024 ? 1 : 0;
                if (ba != bb) return bb - ba;
                if (ya != yb) return yb - ya;
                if (a.updatedAt != b.updatedAt) return a.updatedAt > b.updatedAt ? -1 : 1;
                // 列表新源优先（量子/暴风）；播放可用性另议
                int pa = sportSourcePriority(a.sourceKey);
                int pb = sportSourcePriority(b.sourceKey);
                if (pa != pb) return pa - pb;
                return 0;
            }
        });
        return copy;
    }

    private static void addSportCard(final MainActivity a, LinearLayout row, final CmsCatalog.Item item, final long gen) {
        View v = card(a, item, gen, a.shelfGeneration);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(a.dp(148.0f), a.dp(220.0f));
        lp.setMargins(a.dp(6.0f), a.dp(6.0f), a.dp(6.0f), a.dp(6.0f));
        row.addView(v, lp);
    }

    static void fillLive(final MainActivity a, final View firstRegion) {
        final long gen = a.loadGeneration;
        LinearLayout block = new LinearLayout(a);
        block.setOrientation(1);
        final LinearLayout list = new LinearLayout(a);
        list.setOrientation(1);
        list.setClipChildren(false);
        block.addView(list);
        final TextView note = a.text("正在读取频道…", 15);
        block.addView(note);
        a.content.addView(block, 0);
        run(a, new Runnable() { // from class: studio.muzi.tv.DiscoverScreens$$ExternalSyntheticLambda11
            @Override // java.lang.Runnable
            public final void run() {
                DiscoverScreens.lambda$fillLive$10(a, gen, list, note, firstRegion);
            }
        });
    }

    static /* synthetic */ void lambda$fillLive$10(final MainActivity a, final long gen, final LinearLayout list, final TextView note, final View firstRegion) {
        try {
            String body = readAsset(a, "live-curated.m3u");
            final List<LiveGuide.Channel> rec = LiveGuide.parseM3u(body);
            a.ui.post(new Runnable() { // from class: studio.muzi.tv.DiscoverScreens$$ExternalSyntheticLambda12
                @Override // java.lang.Runnable
                public final void run() {
                    DiscoverScreens.showLivePicks(a, gen, list, note, rec, firstRegion);
                }
            });
        } catch (Exception e) {
            final String msg = a.safe(e);
            a.ui.post(new Runnable() { // from class: studio.muzi.tv.DiscoverScreens$$ExternalSyntheticLambda13
                @Override // java.lang.Runnable
                public final void run() {
                    DiscoverScreens.lambda$fillLive$9(gen, a, note, msg);
                }
            });
        }
    }

    static /* synthetic */ void lambda$fillLive$9(long gen, MainActivity a, TextView note, String msg) {
        if (gen != a.loadGeneration || !"live".equals(a.screen)) {
            return;
        }
        note.setText("频道表读取失败：" + msg);
    }

    static String readAsset(MainActivity a, String name) throws IOException {
        return MainActivity.read(a.getAssets().open(name));
    }

    static void fillBili(final MainActivity a, LinearLayout host, final View firstLauncher) {
        final long gen = a.loadGeneration;
        host.addView(sectionTitle(a, "片源名称里带哔哩的条目"));
        final TextView note = a.text("正在查找… 这不是 B站 排行。红牛 / 暴风 / 量子 / 360 / 无尽 / 非凡没有哔哩分类。", 15);
        host.addView(note);
        LinearLayout rows = new LinearLayout(a);
        rows.setOrientation(1);
        host.addView(rows);
        final List<CmsCatalog.Item> items = new ArrayList<>();
        final int[] left = {SOURCES.length};
        final List<String> failed = new ArrayList<>();
        int s = 0;
        while (s < SOURCES.length) {
            final int index = s;
            final LinearLayout linearLayout = rows;
            run(a, new Runnable() { // from class: studio.muzi.tv.DiscoverScreens$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    DiscoverScreens.lambda$fillBili$12(index, a, gen, failed, items, left, linearLayout, note, firstLauncher);
                }
            });
            s++;
            rows = rows;
        }
    }

    /* JADX WARN: Incorrect condition in loop: B:3:0x001e */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    static /* synthetic */ void lambda$fillBili$12(int index, final MainActivity a, final long gen, final List failed, final List items, final int[] left, final LinearLayout rows, final TextView note, final View firstLauncher) {
        String name = SOURCES[index][1];
        final List<CmsCatalog.Item> batch = new ArrayList<>();
        String[] words = {"哔哩", "bilibili"};
        int wordErrors = 0;
        for (String str : words) {
            try {
                String body = cachedGet(query(SOURCES[index][2], "ac=videolist&wd=" + a.enc(str) + "&pg=1"));
                List<CmsCatalog.Item> parsed = CmsCatalog.parse(body, SOURCES[index][0], name, SOURCES[index][2]);
                for (int i = 0; i < parsed.size(); i++) {
                    CmsCatalog.Item item = parsed.get(i);
                    if (CmsCatalog.biliTagged(item.name, item.typeName)) {
                        batch.add(item);
                    }
                }
            } catch (Exception e) {
                wordErrors++;
            }
        }
        String err = (!batch.isEmpty() || wordErrors <= 0) ? null : name;
        final String failedName = err;
        a.ui.post(new Runnable() { // from class: studio.muzi.tv.DiscoverScreens$$ExternalSyntheticLambda26
            @Override // java.lang.Runnable
            public final void run() {
                DiscoverScreens.lambda$fillBili$11(gen, a, failedName, failed, items, batch, left, rows, note, firstLauncher);
            }
        });
    }

    static /* synthetic */ void lambda$fillBili$11(long gen, MainActivity a, String failedName, List failed, List items, List found, int[] left, LinearLayout rows, TextView note, View firstLauncher) {
        if (gen != a.loadGeneration || !"bili".equals(a.screen)) {
            return;
        }
        if (failedName != null && !failed.contains(failedName)) {
            failed.add(failedName);
        }
        items.addAll(found);
        left[0] = left[0] - 1;
        paintTagged(a, rows, items, gen);
        if (left[0] > 0) {
            return;
        }
        List<CmsCatalog.Row> ranked = CmsCatalog.taggedRows(items);
        if (ranked.isEmpty()) {
            note.setText(emptyBili(failed));
            focusIfIdle(a, firstLauncher);
        } else {
            note.setText("这些标题的名字里带哔哩或 bilibili，按评分和更新时间排列。不是哔哩哔哩热榜。要看 B站 请用下面的客户端或电视浏览器。");
            focusIfIdle(a, firstCard(rows));
        }
    }

    private static void loadTab(MainActivity a, int tab) {
        int shelf = a.shelfGeneration + 1;
        a.shelfGeneration = shelf;
        long gen = a.loadGeneration;
        a.posters.removeAllViews();
        if (tab < 0) {
            a.posters.addView(a.text("推荐", 22));
            loadHome(a, gen, shelf);
            return;
        }
        if (tab == 4) {
            a.posters.addView(a.text("体育", 22));
            a.posters.addView(a.text("点播内体育：斯诺克/台球优先量子+暴风（列表有 2025–2026）；足球篮球仍可用红牛。按标题年份再按更新时间。注意：量子/暴风斯诺克播放链偶发 404。", 15));
            loadSports(a, gen, shelf, SPORTS_DEFAULT);
            return;
        }
        String blurb = "高分行只收录有评分的节目。没有评分时按更新时间排列。没标地区的并进国外。推荐行综合评分+最新。";
        if (tab == 0) {
            blurb = "推荐综合评分优先、同档看最新更新。国内、国外分开排。没标地区的并进国外。";
        } else if (tab == 2) {
            blurb = "以大陆和韩国为主，再加一两部其他地区里评分最高的。";
        } else if (tab == 3) {
            blurb = "优先日韩动漫里的热血、战斗、奇幻、冒险、科幻。恋爱、甜宠这类名字会靠后或省略。高分仍按真实评分排列。";
        }
        a.posters.addView(a.text(LABELS[tab], 22));
        a.posters.addView(a.text(blurb, 15));
        addSubChips(a, tab, gen, shelf);
        loadCategory(a, tab, gen, shelf);
    }

    /** 栏目内子分类：点「全部」看综合推荐，点子类直接拉量子该类最新。 */
    private static void addSubChips(final MainActivity a, final int tab, final long gen, final int shelf) {
        HorizontalScrollView sc = new HorizontalScrollView(a);
        sc.setHorizontalScrollBarEnabled(false);
        sc.setFocusable(false);
        sc.setDescendantFocusability(262144);
        final LinearLayout chips = new LinearLayout(a);
        sc.addView(chips);
        a.posters.addView(sc);
        final LinearLayout host = column(a);
        String[] subs = SUBS[tab];
        String[] all = new String[subs.length + 1];
        all[0] = "全部";
        System.arraycopy(subs, 0, all, 1, subs.length);
        for (int i = 0; i < all.length; i++) {
            final String name = all[i];
            Button b = a.button(name, new Runnable() {
                @Override public void run() {
                    for (int j = 0; j < chips.getChildCount(); j++) ((Button) chips.getChildAt(j)).setTextColor(-1);
                    if ("全部".equals(name)) { loadTab(a, tab); return; }
                    markChip(chips, name);
                    loadSub(a, tab, name, gen);
                }
            });
            b.setTextSize(14.0f);
            b.setPadding(a.dp(12.0f), a.dp(6.0f), a.dp(12.0f), a.dp(6.0f));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, a.dp(44.0f));
            lp.setMargins(a.dp(3.0f), a.dp(4.0f), a.dp(3.0f), a.dp(4.0f));
            chips.addView(b, lp);
        }
        markChip(chips, "全部");
    }

    private static void markChip(LinearLayout chips, String name) {
        for (int j = 0; j < chips.getChildCount(); j++) {
            Button b = (Button) chips.getChildAt(j);
            b.setTextColor(name.equals(b.getText().toString()) ? -670639 : -1);
        }
    }

    private static void loadSub(final MainActivity a, final int tab, final String typeName, final long gen) {
        final int shelf = a.shelfGeneration + 1;
        a.shelfGeneration = shelf;
        // 保留标题/说明/子类行（前 3 个），替换下面的内容
        while (a.posters.getChildCount() > 3) a.posters.removeViewAt(3);
        final LinearLayout host = column(a);
        a.posters.addView(host);
        a.status.setText("正在读取" + typeName + "…");
        final String base = SOURCES[0][2];
        run(a, new Runnable() {
            @Override public void run() {
                List<CmsCatalog.Item> items = new ArrayList<>();
                String err = null;
                try {
                    JSONArray cls = classes(base);
                    String tid = null;
                    for (int i = 0; i < cls.length(); i++) {
                        JSONObject c = cls.getJSONObject(i);
                        if (typeName.equals(c.optString("type_name"))) { tid = c.optString("type_id"); break; }
                    }
                    if (tid == null) throw new IOException("无此分类");
                    for (int pg = 1; pg <= 2; pg++) {
                        items.addAll(CmsCatalog.parse(cachedGet(query(base, "ac=videolist&t=" + tid + "&pg=" + pg)), SOURCES[0][0], SOURCES[0][1], base));
                    }
                } catch (Exception e) { err = e.getMessage(); }
                final List<CmsCatalog.Item> got = items;
                final String fail = err;
                a.ui.post(new Runnable() {
                    @Override public void run() {
                        if (stale(a, gen, shelf, "library")) return;
                        if (got.isEmpty()) { a.status.setText(typeName + " 暂无内容" + (fail == null ? "" : "：" + fail)); return; }
                        paintCategory(a, host, got, typeName, gen, shelf);
                        a.status.setText(typeName + " · " + got.size() + " 部");
                    }
                });
            }
        });
    }

    private static void loadHome(final MainActivity a, final long gen, final int shelf) {
        List<CmsCatalog.Item>[] buckets = new ArrayList[KINDS.length];
        final LinearLayout[] high = new LinearLayout[KINDS.length];
        final LinearLayout[] fresh = new LinearLayout[KINDS.length];
        final boolean[] dirty = new boolean[KINDS.length];
        for (int i = 0; i < KINDS.length; i++) {
            buckets[i] = new ArrayList<>();
            high[i] = column(a);
            fresh[i] = column(a);
            a.posters.addView(high[i]);
            a.posters.addView(fresh[i]);
        }
        final int[] left = {SOURCES.length};
        final List<String> failed = new ArrayList<>();
        a.status.setText(loadingText());
        int s = 0;
        while (s < SOURCES.length) {
            final int index = s;
            final List<CmsCatalog.Item>[] listArr = buckets;
            run(a, new Runnable() { // from class: studio.muzi.tv.DiscoverScreens$$ExternalSyntheticLambda22
                @Override // java.lang.Runnable
                public final void run() {
                    DiscoverScreens.lambda$loadHome$15(index, a, gen, shelf, listArr, high, fresh, dirty, failed, left);
                }
            });
            s++;
            buckets = buckets;
        }
    }

    static void lambda$loadHome$15(int index, final MainActivity a, final long gen, final int shelf, final List[] buckets, final LinearLayout[] high, final LinearLayout[] fresh, final boolean[] dirty, final List failed, final int[] left) {
        String err = null;
        try {
            JSONArray classes = classes(SOURCES[index][2]);
            int got = 0;
            int errors = 0;
            for (int k = 0; k < KINDS.length; k++) {
                List<String> ids = CmsCatalog.typeIds(classes, KINDS[k], CmsCatalog.typeLimit(KINDS[k]));
                final List<CmsCatalog.Item> kept = new ArrayList<>();
                for (int i = 0; i < ids.size(); i++) {
                    try {
                        String body = cachedGet(query(SOURCES[index][2], "ac=videolist&t=" + ids.get(i) + "&pg=1"));
                        kept.addAll(keepKind(CmsCatalog.parse(body, SOURCES[index][0], SOURCES[index][1], SOURCES[index][2]), KINDS[k]));
                    } catch (Exception e) {
                        errors++;
                    }
                }
                got += kept.size();
                if (!kept.isEmpty()) {
                    final int ki = k;
                    a.ui.post(new Runnable() {
                        @Override
                        public void run() {
                            DiscoverScreens.lambda$loadHome$13(a, gen, shelf, buckets, ki, kept, high, fresh, dirty, failed, left);
                        }
                    });
                }
            }
            if (got == 0 && errors > 0) {
                err = SOURCES[index][1];
            }
        } catch (Exception e) {
            err = SOURCES[index][1];
        }
        final String failedName = err;
        a.ui.post(new Runnable() {
            @Override
            public void run() {
                DiscoverScreens.lambda$loadHome$14(a, gen, shelf, failedName, failed, left, buckets, dirty, high, fresh);
            }
        });
    }

    static /* synthetic */ void lambda$loadHome$13(MainActivity a, long gen, int shelf, List[] buckets, int ki, List batch, LinearLayout[] high, LinearLayout[] fresh, boolean[] dirty, List failed, int[] left) {
        if (stale(a, gen, shelf, "library")) {
            return;
        }
        buckets[ki].addAll(batch);
        paintHomeKind(a, high[ki], fresh[ki], buckets[ki], LABELS[ki], gen, shelf, dirty, ki);
        finishHome(a, buckets, failed, left[0], dirty, high, fresh, gen, shelf);
    }

    static /* synthetic */ void lambda$loadHome$14(MainActivity a, long gen, int shelf, String failedName, List failed, int[] left, List[] buckets, boolean[] dirty, LinearLayout[] high, LinearLayout[] fresh) {
        if (stale(a, gen, shelf, "library")) {
            return;
        }
        if (failedName != null && !failed.contains(failedName)) {
            failed.add(failedName);
        }
        left[0] = left[0] - 1;
        finishHome(a, buckets, failed, left[0], dirty, high, fresh, gen, shelf);
    }

    private static void loadCategory(final MainActivity a, final int kindIndex, final long gen, final int shelf) {
        final List<CmsCatalog.Item> items = new ArrayList<>();
        final LinearLayout host = column(a);
        a.posters.addView(host);
        final int[] left = {SOURCES.length};
        final List<String> failed = new ArrayList<>();
        final boolean[] dirty = {false};
        a.status.setText("正在读取" + LABELS[kindIndex] + "…");
        for (int s = 0; s < SOURCES.length; s++) {
            final int index = s;
            run(a, new Runnable() { // from class: studio.muzi.tv.DiscoverScreens$$ExternalSyntheticLambda21
                @Override // java.lang.Runnable
                public final void run() {
                    DiscoverScreens.lambda$loadCategory$18(index, kindIndex, a, gen, shelf, failed, items, left, host, dirty);
                }
            });
        }
    }

    static void lambda$loadCategory$18(int index, final int kindIndex, final MainActivity a, final long gen, final int shelf, final List failed, final List items, final int[] left, final LinearLayout host, final boolean[] dirty) {
        String err = null;
        final List<CmsCatalog.Item> batch = new ArrayList<>();
        try {
            JSONArray classes = classes(SOURCES[index][2]);
            List<String> ids = CmsCatalog.typeIds(classes, KINDS[kindIndex], CmsCatalog.typeLimit(KINDS[kindIndex]));
            int errors = 0;
            for (int i = 0; i < ids.size(); i++) {
                try {
                    String body = cachedGet(query(SOURCES[index][2], "ac=videolist&t=" + ids.get(i) + "&pg=1"));
                    batch.addAll(keepKind(CmsCatalog.parse(body, SOURCES[index][0], SOURCES[index][1], SOURCES[index][2]), KINDS[kindIndex]));
                } catch (Exception e) {
                    errors++;
                }
            }
            if (batch.isEmpty() && (ids.isEmpty() || errors > 0)) {
                err = SOURCES[index][1];
            }
        } catch (Exception e2) {
            err = SOURCES[index][1];
        }
        final String failedName = err;
        a.ui.post(new Runnable() {
            @Override
            public void run() {
                DiscoverScreens.lambda$loadCategory$17(a, gen, shelf, failedName, failed, items, batch, left, host, kindIndex, dirty);
            }
        });
    }

    static /* synthetic */ void lambda$loadCategory$17(final MainActivity a, final long gen, final int shelf, String failedName, List failed, List items, List found, int[] left, LinearLayout host, final int kindIndex, boolean[] dirty) {
        if (stale(a, gen, shelf, "library")) {
            return;
        }
        if (failedName != null && !failed.contains(failedName)) {
            failed.add(failedName);
        }
        items.addAll(found);
        left[0] = left[0] - 1;
        if (!hosted(a.getCurrentFocus(), host)) {
            paintCategory(a, host, items, LABELS[kindIndex], gen, shelf);
            dirty[0] = false;
        } else {
            dirty[0] = true;
        }
        if (left[0] == 0 && dirty[0] && !hosted(a.getCurrentFocus(), host)) {
            paintCategory(a, host, items, LABELS[kindIndex], gen, shelf);
            dirty[0] = false;
        }
        List<CmsCatalog.Item> unique = CmsCatalog.dedupe(items);
        int shown = unique.size();
        a.status.setText(categoryStatus(LABELS[kindIndex], shown, failed, left[0], scored(unique)));
        if (left[0] == 0 && shown == 0) {
            a.addButton(a.posters, "重试", new Runnable() { // from class: studio.muzi.tv.DiscoverScreens$$ExternalSyntheticLambda6
                @Override // java.lang.Runnable
                public final void run() {
                    DiscoverScreens.lambda$loadCategory$16(gen, a, shelf, kindIndex);
                }
            });
        }
    }

    static /* synthetic */ void lambda$loadCategory$16(long gen, MainActivity a, int shelf, int kindIndex) {
        if ("library".equals(a.screen)) {
            CACHE.clear();
            loadTab(a, kindIndex);
        }
    }

    private static void paintHomeKind(MainActivity a, LinearLayout high, LinearLayout fresh, List<CmsCatalog.Item> items, String label, long gen, int shelf, boolean[] dirty, int index) {
        if (!hosted(a.getCurrentFocus(), high) && !hosted(a.getCurrentFocus(), fresh)) {
            dirty[index] = false;
            high.removeAllViews();
            fresh.removeAllViews();
            List<CmsCatalog.Row> rows = CmsCatalog.homeRows(label, items);
            for (int i = 0; i < rows.size(); i++) {
                LinearLayout dest = rows.get(i).title.startsWith("高分") ? high : fresh;
                renderRow(a, dest, rows.get(i), gen, shelf);
            }
            wireFocus(a.posters);
            return;
        }
        dirty[index] = true;
    }

    private static void finishHome(final MainActivity a, List<CmsCatalog.Item>[] buckets, List<String> failed, int left, boolean[] dirty, LinearLayout[] high, LinearLayout[] fresh, final long gen, final int shelf) {
        if (left == 0) {
            for (int k = 0; k < dirty.length; k++) {
                if (dirty[k] && !hosted(a.getCurrentFocus(), high[k]) && !hosted(a.getCurrentFocus(), fresh[k])) {
                    paintHomeKind(a, high[k], fresh[k], buckets[k], LABELS[k], gen, shelf, dirty, k);
                }
            }
        }
        int shown = 0;
        int scored = 0;
        for (List<CmsCatalog.Item> list : buckets) {
            List<CmsCatalog.Item> unique = CmsCatalog.dedupe(list);
            shown += unique.size();
            scored += scored(unique);
        }
        a.status.setText(homeStatus(shown, scored, failed, left));
        if (left == 0 && shown == 0) {
            a.addButton(a.posters, "重试", new Runnable() { // from class: studio.muzi.tv.DiscoverScreens$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    DiscoverScreens.lambda$finishHome$19(gen, a, shelf);
                }
            });
        }
    }

    static /* synthetic */ void lambda$finishHome$19(long gen, MainActivity a, int shelf) {
        if ("library".equals(a.screen)) {
            CACHE.clear();
            loadTab(a, 0);
        }
    }

    private static void paintCategory(MainActivity a, LinearLayout host, List<CmsCatalog.Item> items, String label, long gen, int shelf) {
        host.removeAllViews();
        List<CmsCatalog.Row> rows = CmsCatalog.categoryRows(label, items);
        for (int i = 0; i < rows.size(); i++) {
            renderRow(a, host, rows.get(i), gen, shelf);
        }
        wireFocus((a.posters == null || !a.posters.isAttachedToWindow()) ? host : a.posters);
    }

    private static void paintTagged(MainActivity a, LinearLayout host, List<CmsCatalog.Item> items, long gen) {
        if (hosted(a.getCurrentFocus(), host)) {
            return;
        }
        host.removeAllViews();
        List<CmsCatalog.Row> rows = CmsCatalog.taggedRows(items);
        for (int i = 0; i < rows.size(); i++) {
            renderRow(a, host, rows.get(i), gen, a.shelfGeneration);
        }
        wireFocus(host);
    }

    private static void renderRow(MainActivity a, LinearLayout linearLayout, CmsCatalog.Row row, long gen, int shelf) {
        linearLayout.addView(sectionTitle(a, row.title));
        HorizontalScrollView scroller = rowScroller(a);
        LinearLayout strip = new LinearLayout(a);
        strip.setOrientation(0);
        strip.setClipChildren(false);
        for (int i = 0; i < row.items.size(); i++) {
            strip.addView(card(a, row.items.get(i), gen, shelf));
        }
        scroller.addView(strip);
        linearLayout.addView(scroller, new LinearLayout.LayoutParams(-1, -2));
    }

    private static View sectionTitle(MainActivity a, String title) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(0);
        row.setGravity(16);
        row.setPadding(0, a.dp(14.0f), 0, a.dp(4.0f));
        View mark = new View(a);
        GradientDrawable bar = new GradientDrawable();
        bar.setColor(-670639);
        bar.setCornerRadius(a.dp(2.0f));
        mark.setBackground(bar);
        LinearLayout.LayoutParams markLp = new LinearLayout.LayoutParams(a.dp(4.0f), a.dp(18.0f));
        markLp.setMargins(0, 0, a.dp(8.0f), 0);
        row.addView(mark, markLp);
        TextView label = a.text(title, 20);
        label.setPadding(0, 0, 0, 0);
        label.setTypeface(Typeface.create("sans-serif-medium", 0));
        row.addView(label);
        return row;
    }

    private static HorizontalScrollView rowScroller(MainActivity a) {
        HorizontalScrollView scroller = new HorizontalScrollView(a);
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.setFocusable(false);
        scroller.setDescendantFocusability(262144);
        scroller.setClipToPadding(false);
        scroller.setClipChildren(false);
        scroller.setPadding(a.dp(2.0f), a.dp(8.0f), a.dp(28.0f), a.dp(12.0f));
        return scroller;
    }

    private static View card(final MainActivity a, final CmsCatalog.Item item, long gen, int shelf) {
        LinearLayout linearLayout = new LinearLayout(a);
        linearLayout.setOrientation(1);
        linearLayout.setClipChildren(false);
        linearLayout.setFocusable(true);
        linearLayout.setFocusableInTouchMode(true);
        linearLayout.setClickable(true);
        linearLayout.setContentDescription(item.name);
        FrameLayout frameLayout = new FrameLayout(a);
        GradientDrawable plate = new GradientDrawable();
        plate.setColor(-15064269);
        plate.setCornerRadius(a.dp(10.0f));
        frameLayout.setBackground(plate);
        frameLayout.setClipToOutline(true);
        ImageView image = new ImageView(a);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        frameLayout.addView(image, new FrameLayout.LayoutParams(-1, -1));
        TextView letter = a.text(item.name.isEmpty() ? "片" : item.name.substring(0, 1), 28);
        letter.setTextColor(1728053247);
        letter.setGravity(17);
        frameLayout.addView(letter, new FrameLayout.LayoutParams(-1, -1));
        image.bringToFront();
        if (item.scored()) {
            TextView badge = a.text(String.format(Locale.US, "%.1f", Double.valueOf(item.score)), 13);
            badge.setTextColor(-15068668);
            badge.setTypeface(Typeface.create("sans-serif-medium", 0));
            GradientDrawable pill = new GradientDrawable();
            pill.setColor(-670639);
            pill.setCornerRadius(a.dp(8.0f));
            badge.setBackground(pill);
            badge.setPadding(a.dp(7.0f), a.dp(2.0f), a.dp(7.0f), a.dp(2.0f));
            FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(-2, -2, 8388659);
            bp.setMargins(a.dp(8.0f), a.dp(8.0f), 0, 0);
            frameLayout.addView(badge, bp);
        }
        linearLayout.addView(frameLayout, new LinearLayout.LayoutParams(-1, a.dp(236.0f)));
        TextView label = a.text(item.name, 15);
        label.setMaxLines(2);
        label.setMinLines(2);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setPadding(a.dp(2.0f), a.dp(6.0f), a.dp(2.0f), 0);
        linearLayout.addView(label);
        String meta = (item.area == null || item.area.isEmpty()) ? item.sourceName : item.area + " · " + item.sourceName;
        TextView sub = a.text(meta, 12);
        sub.setTextColor(-6641992);
        sub.setMaxLines(1);
        sub.setEllipsize(TextUtils.TruncateAt.END);
        sub.setPadding(a.dp(2.0f), 0, a.dp(2.0f), 0);
        linearLayout.addView(sub);
        final GradientDrawable focusRing = new GradientDrawable();
        focusRing.setColor(0);
        focusRing.setCornerRadius(a.dp(12.0f));
        focusRing.setStroke(a.dp(3.0f), -1);
        linearLayout.setOnFocusChangeListener(new View.OnFocusChangeListener() { // from class: studio.muzi.tv.DiscoverScreens$$ExternalSyntheticLambda23
            @Override // android.view.View.OnFocusChangeListener
            public final void onFocusChange(View view, boolean z) {
                DiscoverScreens.lambda$card$20(a, focusRing, view, z);
            }
        });
        linearLayout.setOnClickListener(new View.OnClickListener() { // from class: studio.muzi.tv.DiscoverScreens$$ExternalSyntheticLambda24
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                DiscoverScreens.openItem(a, item);
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(a.dp(168.0f), -2);
        lp.setMargins(0, a.dp(4.0f), a.dp(14.0f), a.dp(4.0f));
        linearLayout.setLayoutParams(lp);
        a.loadPoster(item.pic, image, gen, shelf);
        return linearLayout;
    }

    static /* synthetic */ void lambda$card$20(MainActivity a, GradientDrawable focusRing, View v, boolean focused) {
        v.animate().scaleX(focused ? 1.06f : 1.0f).scaleY(focused ? 1.06f : 1.0f).setDuration(120L).start();
        v.setTranslationZ(focused ? a.dp(12.0f) : 0.0f);
        v.setForeground(focused ? focusRing : null);
    }

    private static View channelCard(final MainActivity a, final LiveGuide.Channel channel) {
        LinearLayout card = new LinearLayout(a);
        card.setOrientation(1);
        card.setGravity(16);
        card.setPadding(a.dp(16.0f), a.dp(14.0f), a.dp(16.0f), a.dp(14.0f));
        card.setFocusable(true);
        card.setFocusableInTouchMode(true);
        card.setClickable(true);
        card.setContentDescription(channel.name);
        final GradientDrawable plate = new GradientDrawable();
        plate.setColor(-15064269);
        plate.setCornerRadius(a.dp(12.0f));
        card.setBackground(plate);
        TextView name = a.text(channel.name, 18);
        name.setMaxLines(2);
        name.setEllipsize(TextUtils.TruncateAt.END);
        name.setPadding(0, 0, 0, 0);
        card.addView(name);
        if (channel.group != null && !channel.group.isEmpty()) {
            TextView group = a.text(channel.group, 13);
            group.setTextColor(-6641992);
            group.setMaxLines(1);
            group.setPadding(0, a.dp(4.0f), 0, 0);
            card.addView(group);
        }
        final GradientDrawable focusRing = new GradientDrawable();
        focusRing.setColor(0);
        focusRing.setCornerRadius(a.dp(12.0f));
        focusRing.setStroke(a.dp(3.0f), -1);
        card.setOnFocusChangeListener(new View.OnFocusChangeListener() { // from class: studio.muzi.tv.DiscoverScreens$$ExternalSyntheticLambda17
            @Override // android.view.View.OnFocusChangeListener
            public final void onFocusChange(View view, boolean z) {
                DiscoverScreens.lambda$channelCard$22(a, focusRing, plate, view, z);
            }
        });
        card.setOnClickListener(new View.OnClickListener() { // from class: studio.muzi.tv.DiscoverScreens$$ExternalSyntheticLambda18
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                DiscoverScreens.lambda$channelCard$23(a, channel, view);
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(a.dp(280.0f), a.dp(132.0f));
        lp.setMargins(0, a.dp(4.0f), a.dp(14.0f), a.dp(4.0f));
        card.setLayoutParams(lp);
        return card;
    }

    static /* synthetic */ void lambda$channelCard$22(MainActivity a, GradientDrawable focusRing, GradientDrawable rest, View v, boolean focused) {
        v.animate().scaleX(focused ? 1.05f : 1.0f).scaleY(focused ? 1.05f : 1.0f).setDuration(120L).start();
        v.setTranslationZ(focused ? a.dp(10.0f) : 0.0f);
        v.setForeground(focused ? focusRing : null);
        v.setBackground(rest);
    }

    static /* synthetic */ void lambda$channelCard$23(MainActivity a, LiveGuide.Channel channel, View v) {
        a.directLive = true;
        a.liveMode = true;
        a.resumeAt = 0L;
        a.play(channel.url, channel.name);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void openItem(final MainActivity a, final CmsCatalog.Item item) {
        JSONArray eps = CmsCatalog.episodes(item.playUrl);
        a.page(item.name);
        a.screen = "detail";
        try {
            JSONObject info = new JSONObject();
            info.put("title", item.name);
            JSONObject detail = new JSONObject();
            detail.put("videoInfo", info);
            detail.put("episodes", eps);
            a.currentDetail = detail;
            a.episodes = eps;
            if (eps.length() == 0) {
                a.status.setText("这个条目没有可播放地址 · 搜索需要已配置的 LibreTV");
                a.content.addView(a.text(caption(item), 16));
                a.addButton(a.content, "用 LibreTV 搜索「" + item.name + "」", new Runnable() { // from class: studio.muzi.tv.DiscoverScreens$$ExternalSyntheticLambda9
                    @Override // java.lang.Runnable
                    public final void run() {
                        a.search(item.name);
                    }
                });
                if (a.content.getChildCount() > 0) {
                    a.content.getChildAt(a.content.getChildCount() - 1).requestFocus();
                    return;
                }
                return;
            }
            a.renderEpisodes();
            a.content.addView(a.text(caption(item), 15), 0);
            a.addButton(a.content, "用 LibreTV 搜索其它源", new Runnable() { // from class: studio.muzi.tv.DiscoverScreens$$ExternalSyntheticLambda10
                @Override // java.lang.Runnable
                public final void run() {
                    a.search(item.name);
                }
            });
            a.status.setText(((Object) a.status.getText()) + " · " + item.sourceName + " · 返回上一页");
        } catch (Exception e) {
            a.toast("无法打开");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void showLivePicks(MainActivity a, long gen, LinearLayout linearLayout, TextView note, List<LiveGuide.Channel> rec, View firstRegion) {
        if (gen != a.loadGeneration || !"live".equals(a.screen)) {
            return;
        }
        linearLayout.removeAllViews();
        if (rec.isEmpty()) {
            note.setText("频道表是空的。");
            return;
        }
        LinkedHashMap<String, List<LiveGuide.Channel>> groups = new LinkedHashMap<>();
        for (int i = 0; i < rec.size(); i++) {
            LiveGuide.Channel channel = rec.get(i);
            String key = (channel.group == null || channel.group.isEmpty()) ? "频道" : channel.group;
            List<LiveGuide.Channel> bucket = groups.get(key);
            if (bucket == null) {
                bucket = new ArrayList<>();
                groups.put(key, bucket);
            }
            bucket.add(channel);
        }
        View firstCard = null;
        Iterator<Map.Entry<String, List<LiveGuide.Channel>>> it = groups.entrySet().iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            Map.Entry<String, List<LiveGuide.Channel>> entry = it.next();
            linearLayout.addView(sectionTitle(a, entry.getKey()));
            HorizontalScrollView scroller = rowScroller(a);
            LinearLayout strip = new LinearLayout(a);
            strip.setOrientation(0);
            strip.setClipChildren(false);
            for (int i2 = 0; i2 < entry.getValue().size(); i2++) {
                View card = channelCard(a, entry.getValue().get(i2));
                if (firstCard == null) {
                    firstCard = card;
                }
                strip.addView(card);
            }
            scroller.addView(strip);
            linearLayout.addView(scroller, new LinearLayout.LayoutParams(-1, -2));
        }
        wireLive(linearLayout, firstRegion);
        int foreign = groups.containsKey("海外") ? groups.get("海外").size() : 0;
        note.setText("左右选频道，上下换行。央视和卫视是测过的地址。海外只留了能打开的 " + foreign + " 个，完整 iptv-org 表不再铺开。");
        View focus = a.getCurrentFocus();
        if (firstCard != null) {
            if (focus == null || focus == firstRegion) {
                firstCard.requestFocus();
            }
        }
    }

    private static void wireLive(View host, View below) {
        ArrayList<List<View>> rows = new ArrayList<>();
        collectRows(host, rows);
        int r = 0;
        while (r < rows.size()) {
            List<View> row = rows.get(r);
            List<View> above = r > 0 ? rows.get(r - 1) : null;
            List<View> under = r + 1 < rows.size() ? rows.get(r + 1) : null;
            for (int i = 0; i < row.size(); i++) {
                View up = above == null ? null : above.get(Math.min(i, above.size() - 1));
                View down = under == null ? below : under.get(Math.min(i, under.size() - 1));
                bindVertical(row.get(i), up, down);
            }
            r++;
        }
        if (below != null && !rows.isEmpty()) {
            List<View> last = rows.get(rows.size() - 1);
            bindVertical(below, last.get(0), null);
        }
    }

    private static void bindVertical(View view, final View up, final View down) {
        if (view.getId() == -1) {
            view.setId(View.generateViewId());
        }
        if (up != null) {
            if (up.getId() == -1) {
                up.setId(View.generateViewId());
            }
            view.setNextFocusUpId(up.getId());
        }
        if (down != null) {
            if (down.getId() == -1) {
                down.setId(View.generateViewId());
            }
            view.setNextFocusDownId(down.getId());
        }
        view.setOnKeyListener(new View.OnKeyListener() { // from class: studio.muzi.tv.DiscoverScreens$$ExternalSyntheticLambda16
            @Override // android.view.View.OnKeyListener
            public final boolean onKey(View view2, int i, KeyEvent keyEvent) {
                return DiscoverScreens.lambda$bindVertical$26(down, up, view2, i, keyEvent);
            }
        });
    }

    static /* synthetic */ boolean lambda$bindVertical$26(View down, View up, View v, int keyCode, KeyEvent event) {
        if (event.getAction() != 0) {
            return false;
        }
        if (keyCode == 20 && down != null) {
            return down.requestFocus();
        }
        if (keyCode != 19 || up == null) {
            return false;
        }
        return up.requestFocus();
    }

    static String caption(CmsCatalog.Item item) {
        StringBuilder b = new StringBuilder();
        b.append(item.scored() ? String.format(Locale.US, "%.1f 分", Double.valueOf(item.score)) : "无评分");
        if (item.hits > 0) {
            b.append(" · 热度 ").append(item.hits);
        }
        if (item.area != null && !item.area.isEmpty()) {
            b.append(" · ").append(item.area);
        }
        if (item.year != null && !item.year.isEmpty()) {
            b.append(" · ").append(item.year);
        }
        if (item.remarks != null && !item.remarks.isEmpty()) {
            b.append(" · ").append(item.remarks.replace('\n', ' '));
        }
        b.append(" · ").append(item.sourceName);
        return b.toString();
    }

    static List<CmsCatalog.Item> keepKind(List<CmsCatalog.Item> items, String kind) {
        List<CmsCatalog.Item> out = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            if (kind.equals(CmsCatalog.kindOf(items.get(i).typeName))) {
                out.add(items.get(i));
            }
        }
        return out;
    }

    static JSONArray classes(String base) throws Exception {
        String body = cachedGet(query(base, "ac=list"));
        if (body.startsWith("\ufeff")) {
            body = body.substring(1);
        }
        JSONArray classes = new JSONObject(body).optJSONArray("class");
        if (classes == null) {
            throw new IOException("没有分类");
        }
        return classes;
    }

    static String cachedGet(String url) throws IOException {
        long now = System.currentTimeMillis();
        Hit hit = CACHE.get(url);
        if (hit != null && now - hit.at < CACHE_MS && hit.body != null && !hit.body.isEmpty()) {
            return hit.body;
        }
        String body = httpGet(url);
        CACHE.put(url, new Hit(body, now));
        return body;
    }

    static String httpGet(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        try {
            c.setConnectTimeout(8000);
            c.setReadTimeout(12000);
            c.setInstanceFollowRedirects(true);
            c.setRequestProperty("User-Agent", "MuziTV/0.10");
            c.setRequestProperty("Accept", "application/json,text/plain,*/*");
            int code = c.getResponseCode();
            if (code >= 400) {
                throw new IOException("HTTP " + code);
            }
            return MainActivity.read(c.getInputStream());
        } finally {
            c.disconnect();
        }
    }

    static String query(String base, String q) {
        return base + (base.indexOf(63) >= 0 ? "&" : "?") + q;
    }

    static List<LiveGuide.Channel> withPhoenix(List<LiveGuide.Channel> cn, String hkBody) {
        List<LiveGuide.Channel> out = new ArrayList<>();
        if (cn != null) {
            out.addAll(cn);
        }
        if (containsName(out, "凤凰") || hkBody == null) {
            return out;
        }
        List<LiveGuide.Channel> hk = LiveGuide.recommend(LiveGuide.parseM3u(hkBody));
        int i = 0;
        while (true) {
            if (i < hk.size()) {
                if (hk.get(i).name != null && hk.get(i).name.contains("凤凰")) {
                    out.add(hk.get(i));
                    break;
                }
                i++;
            } else {
                break;
            }
        }
        return out;
    }

    private static boolean containsName(List<LiveGuide.Channel> list, String key) {
        if (list == null) {
            return false;
        }
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).name != null && list.get(i).name.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private static void run(MainActivity a, Runnable job) {
        try {
            a.cms.execute(job);
        } catch (RejectedExecutionException e) {
        }
    }

    private static boolean stale(MainActivity a, long gen, int shelf, String screen) {
        return (gen == a.loadGeneration && shelf == a.shelfGeneration && screen.equals(a.screen)) ? false : true;
    }

    private static boolean hosted(View focus, View host) {
        View v = focus;
        while (v != null) {
            if (v == host) {
                return true;
            }
            Object parent = v.getParent();
            v = parent instanceof View ? (View) parent : null;
        }
        return false;
    }

    private static void focusIfIdle(MainActivity a, View target) {
        if (target == null || !target.isFocusable()) {
            return;
        }
        View focus = a.getCurrentFocus();
        if (focus == null || !hosted(focus, a.content)) {
            target.requestFocus();
        }
    }

    private static View firstCard(ViewGroup rows) {
        return firstFocusable(rows);
    }

    private static void wireFocus(View root) {
        if (root == null) {
            return;
        }
        ArrayList<List<View>> rows = new ArrayList<>();
        collectRows(root, rows);
        for (int r = 0; r < rows.size(); r++) {
            List<View> row = rows.get(r);
            for (int i = 0; i < row.size(); i++) {
                View card = row.get(i);
                if (card.getId() == -1) {
                    card.setId(View.generateViewId());
                }
                if (r > 0) {
                    List<View> above = rows.get(r - 1);
                    card.setNextFocusUpId(above.get(Math.min(i, above.size() - 1)).getId());
                } else {
                    card.setNextFocusUpId(-1);
                }
                if (r + 1 < rows.size()) {
                    List<View> below = rows.get(r + 1);
                    card.setNextFocusDownId(below.get(Math.min(i, below.size() - 1)).getId());
                } else {
                    card.setNextFocusDownId(-1);
                }
            }
        }
    }

    private static void collectRows(View node, List<List<View>> out) {
        if (node instanceof HorizontalScrollView) {
            HorizontalScrollView scroll = (HorizontalScrollView) node;
            if (scroll.getChildCount() == 0) {
                return;
            }
            View inner = scroll.getChildAt(0);
            if (inner instanceof ViewGroup) {
                ViewGroup strip = (ViewGroup) inner;
                ArrayList<View> cards = new ArrayList<>();
                for (int i = 0; i < strip.getChildCount(); i++) {
                    View child = strip.getChildAt(i);
                    if (child.isFocusable()) {
                        cards.add(child);
                    }
                }
                if (!cards.isEmpty()) {
                    out.add(cards);
                    return;
                }
                return;
            }
            return;
        }
        if (node instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) node;
            for (int i2 = 0; i2 < group.getChildCount(); i2++) {
                collectRows(group.getChildAt(i2), out);
            }
        }
    }

    private static View firstFocusable(View root) {
        if (root.isFocusable()) {
            return root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = firstFocusable(group.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
            return null;
        }
        return null;
    }

    private static LinearLayout column(MainActivity a) {
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(1);
        return box;
    }

    private static int scored(List<CmsCatalog.Item> items) {
        int n = 0;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).scored()) {
                n++;
            }
        }
        return n;
    }

    private static String loadingText() {
        StringBuilder b = new StringBuilder("正在读取");
        for (int i = 0; i < SOURCES.length; i++) {
            if (i > 0) {
                b.append("、");
            }
            b.append(SOURCES[i][1]);
        }
        b.append("的最近一页…");
        return b.toString();
    }

    private static String homeStatus(int shown, int scored, List<String> failed, int left) {
        if (left > 0) {
            return "已整理 " + shown + " 部 · 还在读取其它源";
        }
        StringBuilder b = new StringBuilder();
        if (shown == 0) {
            b.append("这一页没有返回节目");
        } else {
            b.append("已整理 ").append(shown).append(" 部");
        }
        if (shown > 0 && scored == 0) {
            b.append(" · 这些源大多没有评分，已按更新时间排列");
        } else if (shown > 0) {
            b.append(" · 高分只含有评分的条目");
        }
        appendFailed(b, failed);
        if (shown > 0) {
            b.append(" · 方向键选择，确认播放");
        } else {
            b.append(" · 可点重试，或用左上角搜索");
        }
        return b.toString();
    }

    private static String categoryStatus(String label, int shown, List<String> failed, int left, int scored) {
        if (left > 0) {
            return label + " 已整理 " + shown + " 部 · 还在读取其它源";
        }
        StringBuilder b = new StringBuilder();
        if (shown == 0) {
            b.append(label).append(" 没有返回节目");
        } else {
            b.append(label).append(" 已整理 ").append(shown).append(" 部");
        }
        if (shown > 0 && scored == 0) {
            b.append(" · 这一页没有评分，已按更新时间分成国内 / 国外");
        }
        appendFailed(b, failed);
        if (shown > 0) {
            b.append(" · 方向键选择，确认播放");
        } else {
            b.append(" · 可点重试，或用左上角搜索");
        }
        return b.toString();
    }

    private static void appendFailed(StringBuilder b, List<String> failed) {
        if (failed == null || failed.isEmpty()) {
            return;
        }
        b.append(" · 未响应：");
        for (int i = 0; i < failed.size(); i++) {
            if (i > 0) {
                b.append("、");
            }
            b.append(failed.get(i));
        }
    }

    private static String emptyBili(List<String> failed) {
        StringBuilder b = new StringBuilder("这几个片源没有哔哩分类，按名称也没搜到带「哔哩」或 bilibili 的条目。下面仍可打开云视听小电视或电视浏览器。应用内网页在 Android 6 上通常不能播放。");
        appendFailed(b, failed);
        return b.toString();
    }
}
