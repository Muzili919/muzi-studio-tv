package studio.muzi.tv;

import android.util.Log;
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** 同名多源聚合：首片测速选最快；播放中低于 2Mbps 或卡顿则自动切下一源并续播。 */
final class MultiSource {
    static final double MIN_MBPS = 2.0;
    static final String RANK_URL = "https://zhishixingqiu.top/tv/rank.json";

    static final class Line { final String name, url; double mbps; Line(String n, String u) { name = n; url = u; } }

    static List<Line> lines = new ArrayList<>();
    static int current = 0;

    /** 读服务器排名，按排名重排 DiscoverScreens.SOURCES（体育聚合保持在末尾）。 */
    static void applyRank() {
        try {
            JSONArray r = new JSONObject(DiscoverScreens.httpGet(RANK_URL)).getJSONArray("sources");
            List<String[]> out = new ArrayList<>();
            for (int i = 0; i < r.length(); i++) {
                JSONObject o = r.getJSONObject(i);
                if (o.optDouble("mbps", 0) < MIN_MBPS || out.size() >= 5) continue;
                out.add(new String[]{o.getString("key"), o.getString("name"), o.getString("api")});
            }
            for (String[] s : DiscoverScreens.SOURCES) if ("ms".equals(s[0])) out.add(s);
            if (out.size() >= 2) DiscoverScreens.SOURCES = out.toArray(new String[0][]);
        } catch (Throwable e) { Log.w("MuziTV", "rank: " + e); }
    }

    /** 后台线程调用：收集同名同集所有源地址并测速排序。 */
    static List<Line> rank(String title, int episode, String firstUrl) {
        List<Line> found = new ArrayList<>();
        if (firstUrl != null) found.add(new Line("当前", firstUrl));
        for (String[] s : DiscoverScreens.SOURCES) {
            if ("ms".equals(s[0])) continue;
            try {
                String body = DiscoverScreens.httpGet(DiscoverScreens.query(s[2], "ac=detail&wd=" + URLEncoder.encode(title, "UTF-8")));
                JSONArray list = new JSONObject(body).optJSONArray("list");
                if (list == null) continue;
                for (int i = 0; i < list.length(); i++) {
                    JSONObject v = list.getJSONObject(i);
                    if (!title.equals(v.optString("vod_name").trim())) continue;
                    JSONArray eps = CmsCatalog.episodes(v.optString("vod_play_url"));
                    if (eps.length() == 0) continue;
                    String ep = eps.optString(Math.min(episode, eps.length() - 1));
                    String u = ep.contains("$") ? ep.substring(ep.indexOf('$') + 1) : ep;
                    boolean dup = false;
                    for (Line l : found) dup |= l.url.equals(u);
                    if (!dup) found.add(new Line(s[1], u));
                    break;
                }
            } catch (Throwable ignored) { }
        }
        for (Line l : found) l.mbps = probe(l.url);
        Collections.sort(found, (a, b) -> Double.compare(b.mbps, a.mbps));
        return found;
    }

    /** 解析 m3u8（含一层主播放列表），下载前 2 个分片测速，返回 Mbps。 */
    static double probe(String m3u8) {
        try {
            String u = m3u8;
            for (int hop = 0; hop < 3; hop++) {
                String tx = new String(bytes(u, 8000, 64 * 1024), "UTF-8");
                List<String> refs = new ArrayList<>();
                for (String ln : tx.split("\n")) { ln = ln.trim(); if (!ln.isEmpty() && !ln.startsWith("#")) refs.add(ln); }
                if (refs.isEmpty()) return 0;
                if (tx.contains("#EXTINF")) {
                    long bytes = 0, t0 = System.nanoTime();
                    for (int i = 0; i < Math.min(2, refs.size()); i++) bytes += bytes(new URL(new URL(u), refs.get(i)).toString(), 10000, 8 << 20).length;
                    double s = (System.nanoTime() - t0) / 1e9;
                    return s > 0 ? bytes * 8 / s / 1e6 : 0;
                }
                u = new URL(new URL(u), refs.get(0)).toString();
            }
        } catch (Throwable ignored) { }
        return 0;
    }

    static byte[] bytes(String u, int timeout, int max) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
        c.setConnectTimeout(timeout); c.setReadTimeout(timeout); c.setRequestProperty("User-Agent", "Mozilla/5.0");
        try (java.io.InputStream in = c.getInputStream()) {
            java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream(); byte[] b = new byte[32768]; int n;
            while ((n = in.read(b)) > 0 && o.size() < max) o.write(b, 0, n);
            return o.toByteArray();
        } finally { c.disconnect(); }
    }

    /** 播放中监测：连续缓冲 >8s，或带宽估计 <2Mbps 且缓冲余量 <8s 持续 20s → 切下一源续播。 */
    static void watch(final MainActivity a, final String title) {
        final Object player = a.video;
        a.ui.postDelayed(new Runnable() {
            int stuck = 0, slow = 0;
            @Override public void run() {
                if (a.video == null || a.video != player || a.liveMode) return;
                int state = a.video.getPlaybackState();
                long pos = a.video.getCurrentPosition();
                stuck = (state == 2 && a.video.getPlayWhenReady()) ? stuck + 1 : 0;
                double est = DefaultBandwidthMeter.getSingletonInstance(a).getBitrateEstimate() / 1e6;
                slow = (est > 0 && est < MIN_MBPS && a.video.getTotalBufferedDuration() < 8000) ? slow + 1 : 0;
                if ((stuck >= 8 || slow >= 20) && current + 1 < lines.size()) {
                    current++;
                    Line next = lines.get(current);
                    a.toast("线路较慢，自动切换到「" + next.name + "」");
                    a.resumeAt = pos;
                    a.play(next.url, title);
                    return;
                }
                a.ui.postDelayed(this, 1000L);
            }
        }, 5000L);
    }
}
