package studio.muzi.tv;

/* loaded from: classes3.dex */
public final class HighlightGeometry {
    private HighlightGeometry() {
    }

    public static float markerX(int width, int padLeft, int padRight, int thumbWidth, int thumbOffset, float scale, boolean rtl) {
        if (rtl) {
            scale = 1.0f - scale;
        }
        int available = (((width - padLeft) - padRight) - thumbWidth) + (thumbOffset * 2);
        if (available < 1) {
            available = Math.max(1, (width - padLeft) - padRight);
        }
        return (padLeft - thumbOffset) + (available * scale) + (thumbWidth / 2.0f);
    }
}
