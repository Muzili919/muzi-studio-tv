package studio.muzi.tv;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.SeekBar;

/* loaded from: classes3.dex */
public class MarkerSeekBar extends SeekBar {
    private int active;
    private long durationMs;
    private final Paint fill;
    private final Paint stroke;
    private int[] timesMs;

    public MarkerSeekBar(Context context) {
        this(context, null);
    }

    public MarkerSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.timesMs = new int[0];
        this.active = -1;
        this.fill = new Paint(1);
        this.stroke = new Paint(1);
        setSplitTrack(false);
        this.stroke.setStyle(Paint.Style.STROKE);
    }

    public void setMarkers(int[] timesMs, long durationMs, int active) {
        this.timesMs = timesMs == null ? new int[0] : timesMs;
        this.durationMs = durationMs;
        this.active = active;
        if (this.timesMs.length == 0) {
            setContentDescription(null);
        } else {
            setContentDescription(this.timesMs.length + " 个精彩标记");
        }
        invalidate();
    }

    @Override // android.widget.AbsSeekBar, android.widget.ProgressBar, android.view.View
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.timesMs.length != 0 && this.durationMs > 0 && getWidth() > 0) {
            float density = getResources().getDisplayMetrics().density;
            this.stroke.setStrokeWidth(2.0f * density);
            float cy = trackCenterY();
            for (int pass = 0; pass < 2; pass++) {
                int i = 0;
                while (i < this.timesMs.length) {
                    boolean on = i == this.active;
                    if ((pass != 0 || !on) && ((pass != 1 || on) && this.timesMs[i] >= 0 && this.timesMs[i] <= this.durationMs)) {
                        float scale = this.timesMs[i] / this.durationMs;
                        float x = HighlightGeometry.markerX(getWidth(), getPaddingLeft(), getPaddingRight(), thumbWidth(), getThumbOffset(), scale, isRtl());
                        float radius = (on ? 6.5f : 4.5f) * density;
                        this.fill.setColor(on ? -14249 : -527900);
                        this.stroke.setColor(-15722457);
                        canvas.drawCircle(x, cy, radius, this.fill);
                        canvas.drawCircle(x, cy, radius, this.stroke);
                    }
                    i++;
                }
            }
        }
    }

    private float trackCenterY() {
        Drawable track = getProgressDrawable();
        return track != null ? getPaddingTop() + track.getBounds().centerY() : getHeight() / 2.0f;
    }

    private int thumbWidth() {
        Drawable thumb = getThumb();
        if (thumb == null) {
            return 0;
        }
        return Math.max(thumb.getIntrinsicWidth(), thumb.getBounds().width());
    }

    private boolean isRtl() {
        return getLayoutDirection() == 1;
    }
}
