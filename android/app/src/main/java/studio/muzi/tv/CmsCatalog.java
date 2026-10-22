package studio.muzi.tv;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/* loaded from: classes3.dex */
public final class CmsCatalog {
    public static final String ANIME = "anime";
    public static final String MOVIE = "movie";
    public static final String SERIES = "series";
    public static final String VARIETY = "variety";
    private static final Comparator<Item> ANIME_SCORE = new Comparator<Item>() { // from class: studio.muzi.tv.CmsCatalog.1
        @Override // java.util.Comparator
        public int compare(Item a, Item b) {
            double as = CmsCatalog.animeSortScore(a);
            double bs = CmsCatalog.animeSortScore(b);
            if (as != bs) {
                return as > bs ? -1 : 1;
            }
            if (a.hits != b.hits) {
                return a.hits > b.hits ? -1 : 1;
            }
            return CmsCatalog.TIME.compare(a, b);
        }
    };
    private static final Comparator<Item> ANIME_TIME = new Comparator<Item>() { // from class: studio.muzi.tv.CmsCatalog.2
        @Override // java.util.Comparator
        public int compare(Item a, Item b) {
            if (CmsCatalog.animeRomance(a) != CmsCatalog.animeRomance(b)) {
                return CmsCatalog.animeRomance(a) ? 1 : -1;
            }
            if (a.updatedAt != b.updatedAt) {
                return a.updatedAt > b.updatedAt ? -1 : 1;
            }
            double as = CmsCatalog.animeSortScore(a);
            double bs = CmsCatalog.animeSortScore(b);
            if (as != bs) {
                return as > bs ? -1 : 1;
            }
            if (a.hits != b.hits) {
                return a.hits > b.hits ? -1 : 1;
            }
            return 0;
        }
    };
    /** 综合：有评分优先 → 分高在前 → 同档看最新更新 → hits。无评分排后按最新。 */
    private static final Comparator<Item> SCORE = new Comparator<Item>() { // from class: studio.muzi.tv.CmsCatalog.3
        @Override // java.util.Comparator
        public int compare(Item a, Item b) {
            if (a.scored() != b.scored()) {
                return a.scored() ? -1 : 1;
            }
            if (a.score != b.score) {
                return a.score > b.score ? -1 : 1;
            }
            if (a.updatedAt != b.updatedAt) {
                return a.updatedAt > b.updatedAt ? -1 : 1;
            }
            if (a.hits != b.hits) {
                return a.hits > b.hits ? -1 : 1;
            }
            return 0;
        }
    };
    private static final Comparator<Item> TIME = new Comparator<Item>() { // from class: studio.muzi.tv.CmsCatalog.4
        @Override // java.util.Comparator
        public int compare(Item a, Item b) {
            if (a.updatedAt != b.updatedAt) {
                return a.updatedAt > b.updatedAt ? -1 : 1;
            }
            if (a.hits != b.hits) {
                return a.hits > b.hits ? -1 : 1;
            }
            return 0;
        }
    };

    public static final class Item {
        public final String area;
        public final long hits;
        public final String id;
        public final String name;
        public final String pic;
        public final String playUrl;
        public final String playFrom;
        public final int region;
        public final String remarks;
        public final double score;
        public final String sourceKey;
        public final String sourceName;
        public final String sourceUrl;
        public final String typeName;
        public final long updatedAt;
        public final String year;

        public Item(String sourceKey, String sourceName, String sourceUrl, String id, String name, String typeName, String area, String year, String remarks, String pic, String playUrl, double score, long hits, long updatedAt, int region) {
            this(sourceKey, sourceName, sourceUrl, id, name, typeName, area, year, remarks, pic, playUrl, "", score, hits, updatedAt, region);
        }

        public Item(String sourceKey, String sourceName, String sourceUrl, String id, String name, String typeName, String area, String year, String remarks, String pic, String playUrl, String playFrom, double score, long hits, long updatedAt, int region) {
            this.sourceKey = sourceKey;
            this.sourceName = sourceName;
            this.sourceUrl = sourceUrl;
            this.id = id;
            this.name = name;
            this.typeName = typeName;
            this.area = area;
            this.year = year;
            this.remarks = remarks;
            this.pic = pic;
            this.playUrl = playUrl;
            this.playFrom = playFrom == null ? "" : playFrom;
            this.score = score;
            this.hits = hits;
            this.updatedAt = updatedAt;
            this.region = region;
        }

        public boolean scored() {
            return this.score >= 1.0d;
        }

        public Item withPic(String nextPic) {
            return new Item(this.sourceKey, this.sourceName, this.sourceUrl, this.id, this.name, this.typeName, this.area, this.year, this.remarks, nextPic == null ? "" : nextPic, this.playUrl, this.playFrom, this.score, this.hits, this.updatedAt, this.region);
        }
    }

    public static final class Row {
        public final List<Item> items;
        public final String title;

        public Row(String title, List<Item> items) {
            this.title = title;
            this.items = items;
        }
    }

    private CmsCatalog() {
    }

    public static String kindOf(String typeName) {
        String n = typeName == null ? "" : typeName.trim();
        if (n.isEmpty()) {
            return "other";
        }
        if (containsAny(n, "伦理", "福利", "写真", "理论", "公告", "头条", "资讯")) {
            return "skip";
        }
        if (n.contains("综艺")) {
            return VARIETY;
        }
        if (n.contains("动漫") || n.contains("动画")) {
            return ANIME;
        }
        if (n.contains("纪录")) {
            return MOVIE;
        }
        if (n.endsWith("剧") || n.contains("连续剧") || n.contains("短剧")) {
            return SERIES;
        }
        return (n.contains("片") || n.contains("电影")) ? MOVIE : "other";
    }

    public static int regionOf(String area) {
        if (area == null) {
            return 0;
        }
        String s = area.replace(" ", "").replace("\u3000", "");
        if (s.isEmpty() || "null".equals(s)) {
            return 0;
        }
        return containsAny(s, "大陆", "内地", "中国", "国产", "香港", "台湾", "澳门", "华语", "港台") ? 1 : 2;
    }

    public static boolean biliTagged(String name, String typeName) {
        String blob = ((name == null ? "" : name) + " " + (typeName != null ? typeName : "")).toLowerCase(Locale.ROOT);
        return blob.contains("哔哩") || blob.contains("bilibili") || blob.contains("b站");
    }

    public static int typeLimit(String kind) {
        if (ANIME.equals(kind) || VARIETY.equals(kind)) {
            return 3;
        }
        return 2;
    }

    public static List<String> typeIds(JSONArray classes, String kind, int limit) {
        List<String> ids = new ArrayList<>();
        if (classes == null || limit <= 0) {
            return ids;
        }
        String[] keys = keysFor(kind);
        for (int k = 0; k < keys.length && ids.size() < limit; k++) {
            for (int i = 0; i < classes.length() && ids.size() < limit; i++) {
                JSONObject c = classes.optJSONObject(i);
                if (c != null) {
                    String name = c.optString("type_name", "");
                    if (!romanceClass(name) && kind.equals(kindOf(name)) && name.contains(keys[k])) {
                        String id = idOf(c);
                        if (!id.isEmpty() && !ids.contains(id)) {
                            ids.add(id);
                        }
                    }
                }
            }
        }
        int k2 = ids.size();
        if (k2 < 2) {
            for (int i2 = 0; i2 < classes.length() && ids.size() < limit; i2++) {
                JSONObject c2 = classes.optJSONObject(i2);
                if (c2 != null) {
                    String name2 = c2.optString("type_name", "");
                    if (!romanceClass(name2) && kind.equals(kindOf(name2))) {
                        String id2 = idOf(c2);
                        if (!id2.isEmpty() && !ids.contains(id2)) {
                            ids.add(id2);
                        }
                    }
                }
            }
        }
        return ids;
    }

    public static String parentTypeId(JSONArray classes, String kind) {
        if (classes == null) {
            return "";
        }
        String[] exact = parentNames(kind);
        for (String str : exact) {
            for (int i = 0; i < classes.length(); i++) {
                JSONObject c = classes.optJSONObject(i);
                if (c != null && str.equals(c.optString("type_name", "").trim())) {
                    return idOf(c);
                }
            }
        }
        List<String> any = typeIds(classes, kind, 1);
        return any.isEmpty() ? "" : any.get(0);
    }

    public static List<Item> parse(String json, String sourceKey, String sourceName, String sourceUrl) {
        JSONArray list = null;
        String str;
        String str2 = "vod_id";
        List<Item> out = new ArrayList<>();
        if (json != null && !json.isEmpty()) {
            try {
                JSONObject root = new JSONObject(json.startsWith("\ufeff") ? json.substring(1) : json);
                list = root.optJSONArray("list");
            } catch (Exception e) {
            }
            if (list == null) {
                return out;
            }
            int i = 0;
            while (i < list.length()) {
                JSONObject o = list.optJSONObject(i);
                if (o == null) {
                    str = str2;
                } else {
                    String typeName = unescape(o.optString("type_name", ""));
                    if ("skip".equals(kindOf(typeName))) {
                        str = str2;
                    } else {
                        String name = unescape(o.optString("vod_name", "")).trim();
                        if (name.isEmpty()) {
                            str = str2;
                        } else {
                            String id = String.valueOf(o.opt(str2) == null ? "" : o.opt(str2));
                            if ("null".equals(id)) {
                                id = "";
                            }
                            // 豆瓣分优先；vod_score 若是整数（暴风等站内投票常见 6/8/10.0）视为不可信
                            double douban = num(o, "vod_douban_score");
                            double site = num(o, "vod_score");
                            double score = douban >= 1.0d ? douban : (site >= 1.0d && site != Math.floor(site) ? site : 0.0d);
                            if (score < 1.0d || score > 10.0d) {
                                score = 0.0d;
                            }
                            double score2 = score;
                            str = str2;
                            long hits = Math.max(lng(o, "vod_hits"), Math.max(lng(o, "vod_hits_week"), lng(o, "vod_hits_month")));
                            String area = unescape(o.optString("vod_area", "")).trim();
                            try {
                                out.add(new Item(sourceKey, sourceName, sourceUrl, id, name, typeName, area, o.optString("vod_year", "").trim(), unescape(o.optString("vod_remarks", "")).trim(), picture(o, sourceUrl), o.optString("vod_play_url", ""), o.optString("vod_play_from", ""), score2, hits, parseTime(o.optString("vod_time", "")), regionOf(area)));
                            } catch (Exception e2) {
                            }
                        }
                    }
                }
                i++;
                str2 = str;
            }
            return out;
        }
        return out;
    }

    public static List<Item> dedupe(List<Item> items) {
        LinkedHashMap<String, Item> map = new LinkedHashMap<>();
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            String key = norm(item.name);
            Item old = map.get(key);
            if (old == null) {
                map.put(key, item);
            } else {
                Item winner = better(item, old) ? item : old;
                Item other = winner == item ? old : item;
                if (PosterUrls.isYouku(winner.pic) && other.pic != null && !other.pic.isEmpty() && !PosterUrls.isYouku(other.pic)) {
                    winner = winner.withPic(other.pic);
                }
                map.put(key, winner);
            }
        }
        return new ArrayList(map.values());
    }

    static String picture(JSONObject o, String sourceUrl) {
        String[] fields = {"vod_pic", "vod_pic_thumb", "vod_pic_slide"};
        String youku = "";
        for (String str : fields) {
            String resolved = PosterUrls.resolve(unescape(o.optString(str, "")).trim(), sourceUrl);
            if (!resolved.isEmpty()) {
                if (!PosterUrls.isYouku(resolved)) {
                    return resolved;
                }
                if (youku.isEmpty()) {
                    youku = resolved;
                }
            }
        }
        return youku;
    }

    public static List<Row> categoryRows(String label, List<Item> raw) {
        return "综艺".equals(label) ? varietyCategoryRows(raw) : "动漫".equals(label) ? animeCategoryRows(raw) : "电影".equals(label) ? movieCategoryRows(raw) : splitRows(label, dedupe(raw), SCORE, TIME);
    }

    private static List<Row> varietyCategoryRows(List<Item> raw) {
        List<Item> shaped = shapeVariety(raw);
        List<Row> rows = splitRows("综艺", shaped, SCORE, TIME);
        pin(rows, othersIn(shaped));
        return rows;
    }

    static List<Item> shapeVariety(List<Item> raw) {
        List<Item> items = dedupe(raw);
        List<Item> main = new ArrayList<>();
        List<Item> other = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            if (varietyMain(items.get(i))) {
                main.add(items.get(i));
            } else {
                other.add(items.get(i));
            }
        }
        main.addAll(top(other, SCORE, 2));
        return main;
    }

    static boolean varietyMain(Item item) {
        if (item.region == 1 || varietyKorea(item)) {
            return true;
        }
        String area = item.area == null ? "" : item.area.replace(" ", "").replace("\u3000", "");
        if (!area.isEmpty() && !"null".equalsIgnoreCase(area)) {
            return false;
        }
        String type = item.typeName != null ? item.typeName : "";
        return type.contains("大陆") || type.contains("日韩") || type.contains("韩国") || type.contains("港台");
    }

    static boolean varietyKorea(Item item) {
        String area = item.area == null ? "" : item.area;
        if (area.contains("日本")) {
            return false;
        }
        String type = item.typeName != null ? item.typeName : "";
        return area.contains("韩国") || area.contains("韩") || type.contains("韩国");
    }

    private static List<Row> movieCategoryRows(List<Item> raw) {
        List<Item> items = dedupe(raw);
        List<Row> rows = new ArrayList<>();
        // 推荐：评分优先 + 同档最新（SCORE）；最新行：纯更新时间
        add(rows, "推荐", top(items, SCORE, 12));
        add(rows, "最新", top(items, TIME, 12));
        add(rows, "国内", top(filter(items, false, 1), SCORE, 12));
        List<Item> foreign = filter(items, false, 2);
        List<Item> unknown = filter(items, false, 0);
        if (foreign.isEmpty()) {
            add(rows, "其他", top(unknown, SCORE, 12));
        } else {
            add(rows, "国外", top(concat(foreign, unknown), SCORE, 12));
        }
        return rows;
    }

    private static List<Row> splitRows(String label, List<Item> items, Comparator<Item> scoreOrder, Comparator<Item> timeOrder) {
        List<Row> rows = new ArrayList<>();
        add(rows, "高分" + label + " · 国内", top(filter(items, true, 1), scoreOrder, 12));
        add(rows, "高分" + label + " · 国外", top(concat(filter(items, true, 2), filter(items, true, 0)), scoreOrder, 12));
        List<Item> homeNew = filter(items, false, 1);
        List<Item> foreignNew = filter(items, false, 2);
        List<Item> unknownNew = filter(items, false, 0);
        add(rows, "最新" + label + " · 国内", top(homeNew, timeOrder, 12));
        if (foreignNew.isEmpty()) {
            add(rows, "其他", top(unknownNew, timeOrder, 12));
        } else {
            add(rows, "最新" + label + " · 国外", top(concat(foreignNew, unknownNew), timeOrder, 12));
        }
        return rows;
    }

    private static List<Item> concat(List<Item> a, List<Item> b) {
        List<Item> out = new ArrayList<>(a);
        out.addAll(b);
        return out;
    }

    public static List<Row> taggedRows(List<Item> raw) {
        List<Item> items = dedupe(raw);
        List<Row> rows = new ArrayList<>();
        add(rows, "名称里带哔哩 · 有评分", top(filter(items, true, -1), SCORE, 12));
        add(rows, "名称里带哔哩 · 最近更新", top(items, TIME, 12));
        return rows;
    }

    public static List<Row> homeRows(String label, List<Item> raw) {
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < raw.size(); i++) {
            Item item = raw.get(i);
            String kind = kindOf(item.typeName);
            if (!"skip".equals(kind) && !"other".equals(kind)) {
                items.add(item);
            }
        }
        List<Item> items2 = dedupe(items);
        if ("综艺".equals(label)) {
            items2 = shapeVariety(items2);
        }
        if ("动漫".equals(label)) {
            items2 = softenRomance(items2);
        }
        List<Row> rows = new ArrayList<>();
        Comparator<Item> scoreOrder = "动漫".equals(label) ? ANIME_SCORE : SCORE;
        Comparator<Item> timeOrder = "动漫".equals(label) ? ANIME_TIME : TIME;
        add(rows, "高分" + label, top(filter(items2, true, -1), scoreOrder, 12));
        add(rows, "最新" + label, top(items2, timeOrder, 12));
        if ("综艺".equals(label)) {
            pin(rows, othersIn(items2));
        }
        return rows;
    }

    private static List<Item> othersIn(List<Item> items) {
        List<Item> other = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            if (!varietyMain(items.get(i))) {
                other.add(items.get(i));
            }
        }
        return other;
    }

    private static void pin(List<Row> rows, List<Item> pinned) {
        if (pinned.isEmpty() || rows.isEmpty()) {
            return;
        }
        List<Item> scored = new ArrayList<>();
        List<Item> rest = new ArrayList<>();
        for (int i = 0; i < pinned.size(); i++) {
            if (pinned.get(i).scored()) {
                scored.add(pinned.get(i));
            } else {
                rest.add(pinned.get(i));
            }
        }
        place(rows, scored, "高分");
        place(rows, rest, "最新");
    }

    private static void place(List<Row> rows, List<Item> pinned, String prefer) {
        if (pinned.isEmpty()) {
            return;
        }
        int target = -1;
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).title.contains("国外") && rows.get(i).title.contains(prefer)) {
                target = i;
            }
        }
        if (target < 0) {
            for (int i2 = 0; i2 < rows.size(); i2++) {
                if (rows.get(i2).title.contains(prefer)) {
                    target = i2;
                }
            }
        }
        if (target < 0) {
            for (int i3 = 0; i3 < rows.size(); i3++) {
                if (rows.get(i3).title.contains("国外")) {
                    target = i3;
                }
            }
        }
        if (target < 0) {
            target = rows.size() - 1;
        }
        Row row = rows.get(target);
        List<Item> items = new ArrayList<>(row.items);
        int placed = 0;
        for (int p = 0; p < pinned.size(); p++) {
            Item extra = pinned.get(p);
            boolean seen = false;
            for (int i4 = 0; i4 < items.size(); i4++) {
                if (extra.name.equals(items.get(i4).name)) {
                    seen = true;
                }
            }
            if (!seen) {
                if (items.size() >= 12) {
                    items.remove(items.size() - 1);
                }
                items.add(Math.min(items.size(), (placed * 4) + 3), extra);
                placed++;
            }
        }
        rows.set(target, new Row(row.title, items));
    }

    private static List<Row> animeCategoryRows(List<Item> raw) {
        List<Item> items = dedupe(raw);
        List<Row> rows = new ArrayList<>();
        add(rows, "高分动漫 · 国内", top(softenRomance(filter(items, true, 1)), ANIME_SCORE, 12));
        add(rows, "高分动漫 · 国外", top(softenRomance(concat(filter(items, true, 2), filter(items, true, 0))), ANIME_SCORE, 12));
        List<Item> homeNew = softenRomance(filter(items, false, 1));
        List<Item> foreignNew = softenRomance(filter(items, false, 2));
        List<Item> unknownNew = softenRomance(filter(items, false, 0));
        add(rows, "最新动漫 · 国内", top(homeNew, ANIME_TIME, 12));
        if (foreignNew.isEmpty()) {
            add(rows, "其他", top(unknownNew, ANIME_TIME, 12));
        } else {
            add(rows, "最新动漫 · 国外", top(concat(foreignNew, unknownNew), ANIME_TIME, 12));
        }
        return rows;
    }

    static boolean animeRomance(Item item) {
        return containsAny(blob(item), "恋爱", "爱情", "甜宠", "甜蜜", "约会", "言情", "纯爱", "乙女", "少女恋爱", "校园恋爱");
    }

    static boolean animeHype(Item item) {
        return containsAny(blob(item), "热血", "战斗", "奇幻", "冒险", "科幻");
    }

    static boolean animeJapanese(Item item) {
        String area = item.area == null ? "" : item.area;
        String type = item.typeName != null ? item.typeName : "";
        return containsAny(area + " " + type, "日本", "日漫", "日韩");
    }

    static List<Item> softenRomance(List<Item> items) {
        List<Item> keep = new ArrayList<>();
        List<Item> romance = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            if (animeRomance(items.get(i))) {
                romance.add(items.get(i));
            } else {
                keep.add(items.get(i));
            }
        }
        int i2 = keep.size();
        if (i2 >= 4) {
            return keep;
        }
        keep.addAll(romance);
        return keep;
    }

    private static String blob(Item item) {
        String name = item.name == null ? "" : item.name;
        String type = item.typeName != null ? item.typeName : "";
        return name + " " + type;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static double animeSortScore(Item item) {
        double score = item.score;
        return animeRomance(item) ? score - 4.0d : (animeJapanese(item) && animeHype(item)) ? 0.6d + score : animeHype(item) ? 0.4d + score : animeJapanese(item) ? 0.2d + score : score;
    }

    public static final class PlayGroup {
        public final String playFrom;
        public final JSONArray episodes;

        public PlayGroup(String playFrom, JSONArray episodes) {
            this.playFrom = playFrom == null ? "" : playFrom;
            this.episodes = episodes == null ? new JSONArray() : episodes;
        }
    }

    /** 按 $$$ 拆开 vod_play_from / vod_play_url，保留每一条线路。 */
    public static List<PlayGroup> playGroups(String playFrom, String playUrl) {
        List<PlayGroup> out = new ArrayList<>();
        if (playUrl == null || playUrl.trim().isEmpty()) {
            return out;
        }
        String[] urls = playUrl.trim().split("\\$\\$\\$", -1);
        String[] froms = playFrom == null ? new String[0] : playFrom.split("\\$\\$\\$", -1);
        for (int i = 0; i < urls.length; i++) {
            String name = i < froms.length ? froms[i].trim() : "";
            if (name.isEmpty()) {
                name = "线路" + (i + 1);
            }
            JSONArray eps = episodes(urls[i]);
            if (eps.length() > 0) {
                out.add(new PlayGroup(name, eps));
            }
        }
        return out;
    }

    public static JSONArray episodes(String playUrl) {
        JSONArray out = new JSONArray();
        if (playUrl == null) {
            return out;
        }
        String trimmed = playUrl.trim();
        if (trimmed.isEmpty()) {
            return out;
        }
        String group = trimmed.split("\\$\\$\\$", -1)[0];
        String[] parts = group.split("#");
        for (String str : parts) {
            String part = str.trim();
            if (!part.isEmpty()) {
                int dollar = part.indexOf(36);
                String label = dollar >= 0 ? part.substring(0, dollar).trim() : "播放";
                String link = dollar >= 0 ? part.substring(dollar + 1).trim() : part;
                if (link.startsWith("http://") || link.startsWith("https://")) {
                    if (label.isEmpty()) {
                        label = "播放";
                    }
                    out.put(unescape(label) + "$" + link);
                }
            }
        }
        return out;
    }

    private static void add(List<Row> rows, String title, List<Item> items) {
        if (!items.isEmpty()) {
            rows.add(new Row(title, items));
        }
    }

    private static List<Item> filter(List<Item> items, boolean scoredOnly, int region) {
        List<Item> out = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            if ((!scoredOnly || item.scored()) && (region < 0 || item.region == region)) {
                out.add(item);
            }
        }
        return out;
    }

    private static List<Item> top(List<Item> items, Comparator<Item> order, int limit) {
        List<Item> copy = new ArrayList<>(items);
        Collections.sort(copy, order);
        return copy.size() > limit ? new ArrayList(copy.subList(0, limit)) : copy;
    }

    private static boolean better(Item item, Item old) {
        return item.scored() != old.scored() ? item.scored() : item.score != old.score ? item.score > old.score : item.updatedAt != old.updatedAt ? item.updatedAt > old.updatedAt : item.hits > old.hits;
    }

    static String norm(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!Character.isWhitespace(c) && c != 183 && c != '-' && c != 8212 && c != '_' && c != ':' && c != 65306) {
                b.append(Character.toLowerCase(c));
            }
        }
        return b.toString();
    }

    private static String[] keysFor(String kind) {
        return MOVIE.equals(kind) ? new String[]{"剧情", "动作", "喜剧", "科幻", "纪录"} : SERIES.equals(kind) ? new String[]{"国产剧", "大陆剧", "欧美剧", "美国剧", "韩国剧", "日本剧", "香港剧"} : VARIETY.equals(kind) ? new String[]{"大陆综艺", "日韩综艺", "欧美综艺", "韩国综艺", "日本综艺", "港台综艺", "海外综艺"} : ANIME.equals(kind) ? new String[]{"日韩动漫", "日本动漫", "日漫", "热血", "战斗", "奇幻", "冒险", "科幻", "国产动漫", "欧美动漫", "海外动漫", "港台动漫"} : new String[0];
    }

    private static boolean romanceClass(String name) {
        return name != null && containsAny(name, "恋爱", "爱情", "甜宠", "言情", "纯爱", "乙女");
    }

    private static String[] parentNames(String kind) {
        return MOVIE.equals(kind) ? new String[]{"电影", "电影片"} : SERIES.equals(kind) ? new String[]{"连续剧", "电视剧"} : VARIETY.equals(kind) ? new String[]{"综艺", "综艺片"} : ANIME.equals(kind) ? new String[]{"动漫", "动漫片"} : new String[0];
    }

    private static String idOf(JSONObject c) {
        Object raw = c.opt("type_id");
        if (raw == null || raw == JSONObject.NULL) {
            return "";
        }
        String id = String.valueOf(raw).trim();
        return id.endsWith(".0") ? id.substring(0, id.length() - 2) : id;
    }

    private static boolean containsAny(String s, String... keys) {
        for (String str : keys) {
            if (s.contains(str)) {
                return true;
            }
        }
        return false;
    }

    private static double num(JSONObject o, String key) {
        Object v = o.opt(key);
        if (v == null || v == JSONObject.NULL) {
            return 0.0d;
        }
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(v).trim());
        } catch (Exception e) {
            return 0.0d;
        }
    }

    private static long lng(JSONObject o, String key) {
        Object v = o.opt(key);
        if (v == null || v == JSONObject.NULL) {
            return 0L;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v).trim());
        } catch (Exception e) {
            return 0L;
        }
    }

    private static long parseTime(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0L;
        }
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(text.trim());
            if (date == null) {
                return 0L;
            }
            return date.getTime();
        } catch (ParseException e) {
            return 0L;
        }
    }

    static String unescape(String s) {
        if (s == null || s.indexOf(38) < 0) {
            return s == null ? "" : s;
        }
        return s.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#039;", "'").replace("&nbsp;", " ");
    }
}
