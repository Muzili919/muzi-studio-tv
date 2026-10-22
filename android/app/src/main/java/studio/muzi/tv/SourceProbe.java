package studio.muzi.tv;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Measures the first K candidates (playlist + first segment) and prefers the lowest latency.
 * Does not download a full title and does not touch the ExoPlayer buffer.
 */
final class SourceProbe {
    static final int FIRST_K = 4;
    static final int TIMEOUT_MS = 2500;
    static final int MAX_EXTRA = 8;

    private SourceProbe() {
    }

    static void probeFirst(List<PlayCandidate> list, int k) {
        if (list == null || list.isEmpty() || k <= 0) {
            return;
        }
        int n = Math.min(k, list.size());
        ExecutorService pool = Executors.newFixedThreadPool(n);
        List<Future<?>> jobs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            final PlayCandidate c = list.get(i);
            c.status = PlayCandidate.PROBING;
            jobs.add(pool.submit(new Runnable() {
                @Override
                public void run() {
                    long ms = measure(c.url);
                    if (ms >= 0) {
                        c.status = PlayCandidate.OK;
                        c.latencyMs = ms;
                    } else {
                        c.status = PlayCandidate.FAIL;
                        c.latencyMs = -1;
                    }
                }
            }));
        }
        for (int i = 0; i < jobs.size(); i++) {
            try {
                jobs.get(i).get(TIMEOUT_MS + 800L, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                jobs.get(i).cancel(true);
            }
        }
        pool.shutdownNow();
        for (int i = 0; i < n; i++) {
            if (PlayCandidate.PROBING.equals(list.get(i).status)) {
                list.get(i).status = PlayCandidate.FAIL;
            }
        }
    }

    /** Sort the probed prefix so the fastest ok line is first. Later lines stay put. */
    static void preferFastest(List<PlayCandidate> list, int k) {
        if (list == null || list.size() < 2 || k <= 0) {
            return;
        }
        int n = Math.min(k, list.size());
        List<PlayCandidate> head = new ArrayList<>(list.subList(0, n));
        Collections.sort(head, new Comparator<PlayCandidate>() {
            @Override
            public int compare(PlayCandidate a, PlayCandidate b) {
                int ra = rank(a);
                int rb = rank(b);
                if (ra != rb) {
                    return ra - rb;
                }
                long la = a.latencyMs < 0 ? Long.MAX_VALUE : a.latencyMs;
                long lb = b.latencyMs < 0 ? Long.MAX_VALUE : b.latencyMs;
                return la < lb ? -1 : la > lb ? 1 : 0;
            }
        });
        for (int i = 0; i < n; i++) {
            list.set(i, head.get(i));
        }
    }

    private static int rank(PlayCandidate c) {
        if (PlayCandidate.OK.equals(c.status)) {
            return 0;
        }
        if (PlayCandidate.PROBING.equals(c.status) || PlayCandidate.PENDING.equals(c.status)) {
            return 1;
        }
        return 2;
    }

    /** Same-title lines from the other configured CMS endpoints. */
    static List<PlayCandidate> searchCms(String title, int episode, List<PlayCandidate> already) {
        List<PlayCandidate> extra = new ArrayList<>();
        if (title == null || title.trim().isEmpty()) {
            return extra;
        }
        String want = CmsCatalog.norm(title);
        String[][] sources = DiscoverScreens.SOURCES;
        for (int s = 0; s < sources.length && extra.size() < MAX_EXTRA; s++) {
            if ("ms".equals(sources[s][0])) {
                continue;
            }
            try {
                String body = DiscoverScreens.httpGet(DiscoverScreens.query(sources[s][2], "ac=detail&wd=" + URLEncoder.encode(title, "UTF-8")));
                JSONArray list = new JSONObject(body).optJSONArray("list");
                if (list == null) {
                    continue;
                }
                for (int i = 0; i < list.length() && extra.size() < MAX_EXTRA; i++) {
                    JSONObject v = list.optJSONObject(i);
                    if (v == null) {
                        continue;
                    }
                    String typeName = v.optString("type_name", "");
                    if ("skip".equals(CmsCatalog.kindOf(typeName))) {
                        continue;
                    }
                    if (!want.equals(CmsCatalog.norm(v.optString("vod_name", "")))) {
                        continue;
                    }
                    List<CmsCatalog.PlayGroup> groups = CmsCatalog.playGroups(v.optString("vod_play_from", ""), v.optString("vod_play_url", ""));
                    for (int g = 0; g < groups.size() && extra.size() < MAX_EXTRA; g++) {
                        CmsCatalog.PlayGroup group = groups.get(g);
                        if (group.episodes.length() == 0) {
                            continue;
                        }
                        String ep = group.episodes.optString(Math.min(Math.max(episode, 0), group.episodes.length() - 1));
                        String url = MainActivity.episodeUrl(ep);
                        if (!url.startsWith("http") || seen(already, extra, url)) {
                            continue;
                        }
                        extra.add(new PlayCandidate(sources[s][1], group.playFrom, url));
                    }
                    break;
                }
            } catch (Throwable ignored) {
            }
        }
        return extra;
    }

    private static boolean seen(List<PlayCandidate> a, List<PlayCandidate> b, String url) {
        if (a != null) {
            for (int i = 0; i < a.size(); i++) {
                if (url.equals(a.get(i).url)) {
                    return true;
                }
            }
        }
        for (int i = 0; i < b.size(); i++) {
            if (url.equals(b.get(i).url)) {
                return true;
            }
        }
        return false;
    }

    static long measure(String url) {
        try {
            if (url == null || !(url.startsWith("http://") || url.startsWith("https://"))) {
                return -1;
            }
            long t0 = System.nanoTime();
            String current = url;
            for (int hop = 0; hop < 3; hop++) {
                byte[] body = readSome(current, hop == 0 ? 48 * 1024 : 16 * 1024);
                if (body == null || body.length == 0) {
                    return -1;
                }
                String text = new String(body, "UTF-8");
                if (!text.contains("#EXTM3U")) {
                    break;
                }
                String ref = firstUri(text);
                if (ref == null) {
                    return -1;
                }
                current = new URL(new URL(current), ref).toString();
                if (text.contains("#EXTINF")) {
                    if (readSome(current, 16 * 1024) == null) {
                        return -1;
                    }
                    break;
                }
            }
            long ms = (System.nanoTime() - t0) / 1000000L;
            return Math.max(ms, 1L);
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static String firstUri(String text) {
        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String ln = lines[i].trim();
            if (ln.isEmpty() || ln.startsWith("#")) {
                continue;
            }
            return ln;
        }
        return null;
    }

    private static byte[] readSome(String url, int max) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setInstanceFollowRedirects(true);
            c.setConnectTimeout(TIMEOUT_MS);
            c.setReadTimeout(TIMEOUT_MS);
            c.setRequestProperty("User-Agent", "MuziTV/1.1");
            c.setRequestProperty("Range", "bytes=0-" + Math.max(1023, max - 1));
            int code = c.getResponseCode();
            if (code >= 400) {
                return null;
            }
            InputStream in = c.getInputStream();
            try {
                byte[] buf = new byte[8192];
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                int n;
                while ((n = in.read(buf)) > 0 && out.size() < max) {
                    out.write(buf, 0, n);
                }
                return out.toByteArray();
            } finally {
                in.close();
            }
        } catch (Throwable ignored) {
            return null;
        } finally {
            if (c != null) {
                c.disconnect();
            }
        }
    }
}
