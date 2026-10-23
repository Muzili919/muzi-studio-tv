package studio.muzi.tv;

import androidx.media3.extractor.AacUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes3.dex */
@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public final class HighlightMap {
    public final List<Entry> entries;
    public final List<String> warnings;

    public static final class Entry {
        public final Integer episode;
        public final List<Highlight> highlights;
        public final String titleContains;

        Entry(String titleContains, Integer episode, List<Highlight> highlights) {
            this.titleContains = titleContains;
            this.episode = episode;
            this.highlights = Collections.unmodifiableList(highlights);
        }
    }

    private HighlightMap(List<Entry> entries, List<String> warnings) {
        this.entries = Collections.unmodifiableList(entries);
        this.warnings = Collections.unmodifiableList(warnings);
    }

    public static HighlightMap empty() {
        return new HighlightMap(new ArrayList(), new ArrayList());
    }

    public static HighlightMap parse(String json) throws JSONException {
        String json2;
        JSONObject root;
        JSONArray rawEntries;
        String json3;
        JSONObject root2;
        JSONArray rawEntries2;
        String json4 = json;
        if (json4 == null) {
            throw new JSONException("empty");
        }
        int i = 1;
        if (json4.startsWith("\ufeff")) {
            json4 = json4.substring(1);
        }
        JSONObject root3 = new JSONObject(json4);
        JSONArray rawEntries3 = root3.optJSONArray("entries");
        if (rawEntries3 == null) {
            throw new JSONException("缺少 entries");
        }
        List<Entry> entries = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int i2 = 0;
        while (i2 < rawEntries3.length()) {
            JSONObject item = rawEntries3.optJSONObject(i2);
            if (item == null) {
                warnings.add("第 " + (i2 + 1) + " 条不是对象，已跳过");
                json2 = json4;
                root = root3;
                rawEntries = rawEntries3;
            } else {
                JSONObject match = item.optJSONObject("match");
                if (match == null) {
                    warnings.add("第 " + (i2 + 1) + " 条缺少 match，已跳过");
                    json2 = json4;
                    root = root3;
                    rawEntries = rawEntries3;
                } else {
                    String title = match.optString("titleContains", "").trim();
                    if (title.isEmpty()) {
                        warnings.add("第 " + (i2 + 1) + " 条缺少 titleContains，已跳过");
                        json2 = json4;
                        root = root3;
                        rawEntries = rawEntries3;
                    } else {
                        Integer episode = null;
                        if (match.has("episode") && !match.isNull("episode")) {
                            Object rawEpisode = match.get("episode");
                            if (!(rawEpisode instanceof Number)) {
                                warnings.add("「" + title + "」的 episode 不是数字，已跳过");
                                json2 = json4;
                                root = root3;
                                rawEntries = rawEntries3;
                            } else {
                                int ep = ((Number) rawEpisode).intValue();
                                if (ep < i) {
                                    warnings.add("「" + title + "」的 episode 必须从 1 开始，已跳过");
                                    json2 = json4;
                                    root = root3;
                                    rawEntries = rawEntries3;
                                } else {
                                    episode = Integer.valueOf(ep);
                                }
                            }
                        }
                        JSONArray rawMarks = item.optJSONArray("highlights");
                        if (rawMarks == null) {
                            warnings.add("「" + title + "」缺少 highlights，已跳过");
                            json2 = json4;
                            root = root3;
                            rawEntries = rawEntries3;
                        } else {
                            List<Highlight> marks = new ArrayList<>();
                            int h = 0;
                            while (h < rawMarks.length()) {
                                JSONObject mark = rawMarks.optJSONObject(h);
                                if (mark != null) {
                                    json3 = json4;
                                    if (!mark.has("timeSec")) {
                                        root2 = root3;
                                        rawEntries2 = rawEntries3;
                                    } else if (mark.isNull("timeSec")) {
                                        root2 = root3;
                                        rawEntries2 = rawEntries3;
                                    } else {
                                        Object rawTime = mark.get("timeSec");
                                        root2 = root3;
                                        if (!(rawTime instanceof Number)) {
                                            rawEntries2 = rawEntries3;
                                            warnings.add("「" + title + "」的 timeSec 不是数字，已跳过");
                                        } else {
                                            rawEntries2 = rawEntries3;
                                            int sec = ((Number) rawTime).intValue();
                                            if (sec >= 0 && sec <= 1000000) {
                                                marks.add(new Highlight(sec, mark.optString("text", "")));
                                            }
                                            warnings.add("「" + title + "」的 timeSec=" + sec + " 无效，已跳过");
                                        }
                                        h++;
                                        json4 = json3;
                                        root3 = root2;
                                        rawEntries3 = rawEntries2;
                                    }
                                } else {
                                    json3 = json4;
                                    root2 = root3;
                                    rawEntries2 = rawEntries3;
                                }
                                warnings.add("「" + title + "」第 " + (h + 1) + " 个标记缺少 timeSec，已跳过");
                                h++;
                                json4 = json3;
                                root3 = root2;
                                rawEntries3 = rawEntries2;
                            }
                            json2 = json4;
                            root = root3;
                            rawEntries = rawEntries3;
                            if (marks.isEmpty()) {
                                warnings.add("「" + title + "」没有可用标记，已跳过");
                            } else {
                                Collections.sort(marks, new Comparator<Highlight>() { // from class: studio.muzi.tv.HighlightMap.1
                                    @Override // java.util.Comparator
                                    public int compare(Highlight a, Highlight b) {
                                        return a.timeSec - b.timeSec;
                                    }
                                });
                                entries.add(new Entry(title, episode, marks));
                            }
                        }
                    }
                }
            }
            i2++;
            json4 = json2;
            root3 = root;
            rawEntries3 = rawEntries;
            i = 1;
        }
        return new HighlightMap(entries, warnings);
    }

    public List<Highlight> match(String title, int zeroBasedEpisode) {
        if (title == null || title.trim().isEmpty() || zeroBasedEpisode < 0) {
            return new ArrayList();
        }
        String wanted = norm(title);
        int episodeNumber = zeroBasedEpisode + 1;
        Entry best = null;
        int bestScore = Integer.MIN_VALUE;
        for (int i = 0; i < this.entries.size(); i++) {
            Entry entry = this.entries.get(i);
            String key = norm(entry.titleContains);
            if (!key.isEmpty() && wanted.contains(key) && (entry.episode == null || entry.episode.intValue() == episodeNumber)) {
                int score = key.length() + (entry.episode != null ? AacUtil.AAC_LC_MAX_RATE_BYTES_PER_SECOND : 0);
                if (score > bestScore) {
                    best = entry;
                    bestScore = score;
                }
            }
        }
        return best == null ? new ArrayList() : new ArrayList(best.highlights);
    }

    static String norm(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!Character.isWhitespace(c) && c != 183 && c != 12539 && c != 8226 && c != '-' && c != 8212 && c != '_' && c != ':' && c != 65306) {
                b.append(Character.toLowerCase(c));
            }
        }
        return b.toString();
    }
}
