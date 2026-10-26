package studio.muzi.tv;

import android.graphics.*;
import java.util.*;

/** Time-based events, independent of the frame rate and without a repeating track. */
final class MeteorField {
    static final class Meteor {
        float start, duration, x, y, dx, dy, distance, tail, width, brightness;
        int color;
    }
    private final Random random;
    private final ArrayList<Meteor> events = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float next = .8f;
    MeteorField() { this(new Random()); }
    MeteorField(Random random) { this.random = random; paint.setStrokeCap(Paint.Cap.ROUND); }
    private float between(float a, float b) { return a + random.nextFloat() * (b - a); }
    private void add(float start) {
        Meteor m = new Meteor();
        float direction = random.nextFloat() < .73f ? -1 : 1;
        float angle = between(.40f, .84f);
        m.start=start; m.duration=between(.65f,1.65f);
        m.x=direction<0?between(.24f,.96f):between(.04f,.68f); m.y=between(.035f,.26f);
        m.dx=(float)Math.cos(angle)*direction; m.dy=(float)Math.sin(angle);
        m.distance=between(.14f,.34f); m.tail=between(.024f,.105f);
        m.width=between(.00045f,.0010f); m.brightness=between(.42f,.96f);
        int[] colors={0xffd2e9ff,0xffeff6ff,0xffffeed8}; m.color=colors[random.nextInt(colors.length)];
        events.add(m);
    }
    void draw(Canvas canvas, float time, int width, int height, Path skyline) {
        while(next<time+8) {
            float start=next; add(start);
            if(random.nextFloat()<.23f) {
                add(start+between(.28f,.72f));
                if(random.nextFloat()<.32f) add(start+between(.86f,1.28f));
            }
            next+=between(2.8f,7.4f);
        }
        for(Iterator<Meteor> it=events.iterator();it.hasNext();) {
            Meteor m=it.next(); if(m.start+m.duration<time-1) it.remove();
        }
        canvas.save(); canvas.clipPath(skyline);
        for(Meteor m:events) {
            float q=(time-m.start)/m.duration; if(q<=0||q>=1) continue;
            float alpha=(float)Math.pow(Math.sin(Math.PI*q),.7)*m.brightness;
            float x=(m.x+m.dx*m.distance*q)*width, y=(m.y+m.dy*m.distance*q)*height;
            float length=m.tail*Math.min(1,q*5);
            float tx=x-m.dx*length*width, ty=y-m.dy*length*height;
            int transparent=m.color&0x00ffffff;
            paint.setStyle(Paint.Style.STROKE);
            paint.setShader(new LinearGradient(tx,ty,x,y,new int[]{transparent,(m.color&0xffffff)|0x59000000,m.color},new float[]{0,.70f,1},Shader.TileMode.CLAMP));
            paint.setAlpha((int)(255*alpha)); paint.setStrokeWidth(Math.max(.7f,m.width*width));
            canvas.drawLine(tx,ty,x,y,paint);
            paint.setShader(null); paint.setColor(0xfff8fcff); paint.setAlpha((int)(242*alpha));
            paint.setStyle(Paint.Style.FILL); canvas.drawCircle(x,y,Math.max(.6f,m.width*width*.65f),paint);
        }
        canvas.restore(); paint.setShader(null); paint.setAlpha(255);
    }
}
