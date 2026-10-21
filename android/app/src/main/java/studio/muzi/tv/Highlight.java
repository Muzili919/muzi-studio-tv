package studio.muzi.tv;

/* loaded from: classes3.dex */
public final class Highlight {
    public final String text;
    public final int timeSec;

    public Highlight(int timeSec, String text) {
        this.timeSec = timeSec;
        this.text = (text == null || text.trim().isEmpty()) ? "精彩" : text.trim();
    }

    public long timeMs() {
        return this.timeSec * 1000;
    }
}
