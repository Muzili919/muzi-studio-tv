package studio.muzi.tv;

import android.content.Context;
import android.graphics.*;
import android.opengl.*;
import android.os.SystemClock;
import android.util.Log;
import android.view.*;
import android.widget.FrameLayout;
import java.io.*;
import java.nio.*;
import org.json.*;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** API23 backdrop: the approved water shader, static photo and independent meteor events. */
final class DesktopBackdrop extends FrameLayout {
    private static final String TAG="MuziBackdrop";
    private final Bitmap wall;
    private final GLSurfaceView surface;
    private final WaterRenderer renderer;
    private final View fallback;
    private final MeteorField meteors=new MeteorField();
    private final Path skyline=new Path();
    private final float[] ridge={0,.45f,.04f,.456f,.065f,.436f,.09f,.459f,.13f,.429f,.16f,.455f,.205f,.435f,.253f,.431f,.28f,.46f,.325f,.471f,.375f,.427f,.404f,.444f,.431f,.404f,.48f,.362f,.506f,.388f,.542f,.416f,.567f,.411f,.608f,.448f,.636f,.452f,.673f,.474f,.704f,.493f,.754f,.47f,.78f,.49f,.82f,.49f,.86f,.508f,.90f,.499f,.94f,.512f,1,.48f};

    private boolean active, motion=true, surfacePaused;
    private long elapsed,last;
    private final Runnable redraw=()->invalidate();
    DesktopBackdrop(Context context) {
        super(context); setWillNotDraw(false);
        Bitmap photo=null;
        try(InputStream in=context.getAssets().open("galaxy.jpg")){photo=BitmapFactory.decodeStream(in);}catch(Exception e){Log.e(TAG,"wallpaper unavailable",e);}
        wall=photo; renderer=new WaterRenderer();
        surface=new GLSurfaceView(context); surface.setFocusable(false);surface.setFocusableInTouchMode(false);surface.setEGLContextClientVersion(2);
        surface.setPreserveEGLContextOnPause(true); surface.setRenderer(renderer);
        surface.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        addView(surface,new LayoutParams(-1,-1));
        fallback=new View(context){final Paint paint=new Paint(Paint.FILTER_BITMAP_FLAG);@Override protected void onDraw(Canvas canvas){canvas.drawColor(0xff08121e);if(wall!=null)canvas.drawBitmap(wall,null,new Rect(0,0,getWidth(),getHeight()),paint);}};
        addView(fallback,new LayoutParams(-1,-1));
    }
    void setMotion(boolean enabled){motion=enabled;renderer.motion=enabled;surface.requestRender();invalidate();}
    void resume(){active=true;last=0;if(surfacePaused){surface.onResume();surfacePaused=false;}invalidate();}
    void pause(){active=false;last=0;removeCallbacks(redraw);if(!surfacePaused){surface.onPause();surfacePaused=true;}}
    @Override protected void onDetachedFromWindow(){pause();super.onDetachedFromWindow();}
    @Override protected void onSizeChanged(int width,int height,int oldWidth,int oldHeight){super.onSizeChanged(width,height,oldWidth,oldHeight);skyline.reset();skyline.moveTo(0,0);skyline.lineTo(width,0);for(int i=ridge.length-2;i>=0;i-=2)skyline.lineTo(ridge[i]*width,ridge[i+1]*height);skyline.close();}
    @Override protected void dispatchDraw(Canvas canvas){
        super.dispatchDraw(canvas);
        long now=SystemClock.uptimeMillis();boolean moving=active&&motion&&isShown();
        if(moving){if(last>0)elapsed+=Math.min(100,now-last);last=now;}else last=0;
        float time=elapsed/1000f;renderer.time=time;renderer.motion=motion;
        if(motion)meteors.draw(canvas,time,getWidth(),getHeight(),skyline);
        canvas.drawColor(0x16000a16);
        if(moving){surface.requestRender();removeCallbacks(redraw);postDelayed(redraw,50);}
    }
    private final class WaterRenderer implements GLSurfaceView.Renderer {
        volatile float time;volatile boolean motion=true;
        int program,timeLocation,motionLocation;boolean failed,reported;
        final int[] textures=new int[2];
        final FloatBuffer quad=ByteBuffer.allocateDirect(8*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        WaterRenderer(){quad.put(new float[]{-1,-1,1,-1,-1,1,1,1}).position(0);}
        int compile(int type,String source){int shader=GLES20.glCreateShader(type);GLES20.glShaderSource(shader,source);GLES20.glCompileShader(shader);int[] ok=new int[1];GLES20.glGetShaderiv(shader,GLES20.GL_COMPILE_STATUS,ok,0);if(ok[0]==0)throw new IllegalStateException(GLES20.glGetShaderInfoLog(shader));return shader;}
        String readAsset(String name)throws IOException{try(InputStream in=getContext().getAssets().open(name);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))>0)out.write(buffer,0,n);return out.toString("UTF-8");}}
        Path maskPath(JSONArray points,float iw,float ih)throws JSONException{Path path=new Path();for(int i=0;i<points.length();i++){JSONArray point=points.getJSONArray(i);float x=(float)point.getDouble(0)/iw*960,y=(float)point.getDouble(1)/ih*540;if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}path.close();return path;}
        void texture(int unit,Bitmap bitmap,String uniform){GLES20.glActiveTexture(GLES20.GL_TEXTURE0+unit);GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textures[unit]);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,bitmap,0);GLES20.glUniform1i(GLES20.glGetUniformLocation(program,uniform),unit);}
        @Override public void onSurfaceCreated(GL10 gl,EGLConfig config){
            failed=false;reported=false;
            try{
                if(wall==null)throw new IllegalStateException("No wallpaper bitmap");
                int vertex=compile(GLES20.GL_VERTEX_SHADER,"attribute vec2 pos;varying vec2 uv;void main(){uv=vec2((pos.x+1.0)*0.5,(1.0-pos.y)*0.5);gl_Position=vec4(pos,0.,1.);}");
                int fragment=compile(GLES20.GL_FRAGMENT_SHADER,readAsset("water.frag"));program=GLES20.glCreateProgram();GLES20.glAttachShader(program,vertex);GLES20.glAttachShader(program,fragment);GLES20.glLinkProgram(program);int[] linked=new int[1];GLES20.glGetProgramiv(program,GLES20.GL_LINK_STATUS,linked,0);if(linked[0]==0)throw new IllegalStateException(GLES20.glGetProgramInfoLog(program));GLES20.glDeleteShader(vertex);GLES20.glDeleteShader(fragment);GLES20.glUseProgram(program);
                int pos=GLES20.glGetAttribLocation(program,"pos");quad.position(0);GLES20.glEnableVertexAttribArray(pos);GLES20.glVertexAttribPointer(pos,2,GLES20.GL_FLOAT,false,0,quad);
                GLES20.glGenTextures(2,textures,0);texture(0,wall,"wall");
                JSONObject region=new JSONObject(readAsset("water-mask.json"));float iw=(float)region.getDouble("imageWidth"),ih=(float)region.getDouble("imageHeight");
                Bitmap mask=Bitmap.createBitmap(960,540,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(mask);Path polygon=maskPath(region.getJSONArray("outer"),iw,ih);canvas.save();canvas.clipPath(polygon);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(Color.WHITE);paint.setMaskFilter(new BlurMaskFilter(3.5f,BlurMaskFilter.Blur.NORMAL));canvas.drawPath(polygon,paint);canvas.restore();
                Paint erase=new Paint(Paint.ANTI_ALIAS_FLAG);erase.setColor(Color.WHITE);erase.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));erase.setStyle(Paint.Style.STROKE);erase.setStrokeWidth(12);erase.setMaskFilter(new BlurMaskFilter(3,BlurMaskFilter.Blur.NORMAL));canvas.drawPath(polygon,erase);erase.setMaskFilter(null);canvas.drawPath(polygon,erase);erase.setStyle(Paint.Style.FILL);JSONArray holes=region.getJSONArray("holes");for(int i=0;i<holes.length();i++){Path hole=maskPath(holes.getJSONObject(i).getJSONArray("points"),iw,ih);erase.setMaskFilter(new BlurMaskFilter(3.5f,BlurMaskFilter.Blur.NORMAL));canvas.drawPath(hole,erase);erase.setMaskFilter(null);canvas.drawPath(hole,erase);}texture(1,mask,"lakeMask");mask.recycle();

                timeLocation=GLES20.glGetUniformLocation(program,"time");motionLocation=GLES20.glGetUniformLocation(program,"animate");
                int error=GLES20.glGetError();if(error!=GLES20.GL_NO_ERROR)throw new IllegalStateException("GL setup error "+error);
                Log.i(TAG,"Water renderer ready: "+GLES20.glGetString(GLES20.GL_RENDERER));
            }catch(Throwable e){failed=true;Log.e(TAG,"Water renderer unavailable; preserving static wallpaper",e);post(()->{surface.setVisibility(View.INVISIBLE);fallback.setVisibility(View.VISIBLE);});}
        }
        @Override public void onSurfaceChanged(GL10 gl,int width,int height){GLES20.glViewport(0,0,width,height);}
        @Override public void onDrawFrame(GL10 gl){if(failed||program==0)return;GLES20.glUseProgram(program);GLES20.glUniform1f(timeLocation,time);GLES20.glUniform1f(motionLocation,motion?1:0);GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);if(!reported){reported=true;post(()->fallback.setVisibility(View.GONE));}}
    }
}
