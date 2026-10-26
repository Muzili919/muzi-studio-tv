package studio.muzi.tv;

import android.app.AlertDialog;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.JSONArray;
import java.util.*;

/** Native API23 desktop. Only package shortcuts are saved; never uninstalls applications. */
public final class DesktopView extends android.widget.FrameLayout {
 final MainActivity a; final android.content.SharedPreferences prefs;
 final LinkedHashMap<String,ResolveInfo> apps=new LinkedHashMap<>();
 final ArrayList<String> pins=new ArrayList<>();
 final LinearLayout dock;final Sky sky;final TextView clock;final Button motion;
 boolean active=false;int heldKey=-1;boolean held=false;
 final Handler handler=new Handler(Looper.getMainLooper());
 final Runnable tick=new Runnable(){public void run(){if(!active||!isShown())return;clock.setText(new java.text.SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date()));Updater.onHome(a);handler.postDelayed(this,60000);}};
 public DesktopView(MainActivity owner){super(owner);a=owner;prefs=a.getSharedPreferences("desktop-v2",0);sky=new Sky();addView(sky,new LayoutParams(-1,-1));
 LinearLayout page=new LinearLayout(a);page.setOrientation(1);page.setPadding(d(40),d(30),d(40),d(24));addView(page,new LayoutParams(-1,-1));
 LinearLayout top=new LinearLayout(a);top.setGravity(Gravity.CENTER_VERTICAL);top.addView(new View(a),new LinearLayout.LayoutParams(0,1,1));clock=text("",34);top.addView(clock);page.addView(top);
 page.addView(new View(a),new LinearLayout.LayoutParams(1,0,1));
 HorizontalScrollView scroll=new HorizontalScrollView(a);scroll.setClipToPadding(false);scroll.setHorizontalScrollBarEnabled(false);dock=new LinearLayout(a);dock.setPadding(d(7),d(12),d(7),d(12));scroll.addView(dock);page.addView(scroll,new LinearLayout.LayoutParams(-1,d(145)));
 motion=button("",()->{prefs.edit().putBoolean("motion",!prefs.getBoolean("motion",true)).apply();updateMotion();});
 scan();load();render(null);updateMotion(); }
 int d(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 TextView text(String s,int size){TextView t=new TextView(a);t.setText(s);t.setTextSize(size);t.setTextColor(Color.WHITE);return t;}
 GradientDrawable bg(boolean focus){GradientDrawable g=new GradientDrawable();g.setColor(focus?0xff34576b:0xdc122533);g.setCornerRadius(d(12));g.setStroke(d(focus?2:1),focus?0xffd5f1ff:0x224a7188);return g;}
 Button button(String s,Runnable r){Button b=new Button(a);b.setText(s);b.setTextSize(14);b.setTextColor(Color.WHITE);b.setAllCaps(false);b.setBackground(bg(false));b.setPadding(d(16),d(8),d(16),d(8));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,d(44));lp.setMargins(d(5),0,d(5),0);b.setLayoutParams(lp);b.setOnFocusChangeListener((v,f)->v.setBackground(bg(f)));b.setOnClickListener(v->r.run());return b;}
 void scan(){apps.clear();PackageManager pm=a.getPackageManager();for(String cat:new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER,Intent.CATEGORY_LAUNCHER})for(ResolveInfo r:pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(cat),0)){String pkg=r.activityInfo.packageName;if(!pkg.equals(a.getPackageName())&&!apps.containsKey(pkg))apps.put(pkg,r);}}
 String label(String pkg){if(pkg.equals("com.fongmi.android.tv"))return "木子影界";try{return apps.containsKey(pkg)?apps.get(pkg).loadLabel(a.getPackageManager()).toString():a.getPackageManager().getApplicationLabel(a.getPackageManager().getApplicationInfo(pkg,0)).toString();}catch(Exception e){return "未安装 · "+pkg;}}
 void load(){pins.clear();String raw=prefs.getString("pins",null);if(raw!=null)try{JSONArray j=new JSONArray(raw);for(int i=0;i<j.length();i++){String p=j.getString(i);if(!pins.contains(p))pins.add(p);}return;}catch(Exception ignored){}
 save();}

 void save(){prefs.edit().putString("pins",new JSONArray(pins).toString()).commit();}
 static final String PLAYER="com.fongmi.android.tv";
 LinearLayout tile(String title,String glyph,Runnable action){LinearLayout v=new LinearLayout(a);v.setOrientation(1);v.setGravity(Gravity.CENTER);v.setFocusable(true);v.setClickable(true);v.setContentDescription(title);v.setBackground(bg(false));v.setPadding(d(10),d(12),d(10),d(10));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(d(142),d(117));lp.setMargins(d(7),0,d(7),0);dock.addView(v,lp);if(glyph!=null){v.addView(new ShortcutIcon(glyph),new LinearLayout.LayoutParams(d(48),d(48)));}TextView name=text(title,15);name.setSingleLine(true);name.setEllipsize(android.text.TextUtils.TruncateAt.END);name.setGravity(Gravity.CENTER);name.setPadding(0,d(10),0,0);v.addView(name,new LinearLayout.LayoutParams(-1,-2));v.setOnFocusChangeListener((view,f)->view.setBackground(bg(f)));v.setOnClickListener(view->action.run());return v;}
 final class ShortcutIcon extends View {final String kind;final Paint ink=new Paint(Paint.ANTI_ALIAS_FLAG);final RectF box=new RectF();final Path shape=new Path();ShortcutIcon(String kind){super(a);this.kind=kind;}@Override protected void onDraw(Canvas canvas){super.onDraw(canvas);float unit=Math.min(getWidth(),getHeight())/64f;canvas.save();canvas.scale(unit,unit);ink.setStyle(Paint.Style.FILL);ink.setColor(kind.equals("▷")?0xff337079:kind.equals("▣")?0xff436ca3:0xff506378);box.set(1,1,63,63);canvas.drawRoundRect(box,16,16,ink);ink.setColor(Color.WHITE);ink.setStyle(Paint.Style.STROKE);ink.setStrokeWidth(2.5f);ink.setStrokeCap(Paint.Cap.ROUND);if(kind.equals("▷")){shape.reset();shape.moveTo(24,17);shape.lineTo(45,32);shape.lineTo(24,47);shape.close();canvas.drawPath(shape,ink);}else if(kind.equals("▣")){box.set(15,22,49,45);canvas.drawRoundRect(box,4,4,ink);canvas.drawLine(25,14,32,21,ink);canvas.drawLine(39,14,32,21,ink);canvas.drawLine(25,50,39,50,ink);}else if(kind.equals("＋")){canvas.drawLine(20,32,44,32,ink);canvas.drawLine(32,20,32,44,ink);}else{canvas.drawCircle(32,32,11,ink);canvas.drawCircle(32,32,4,ink);for(int i=0;i<8;i++){canvas.save();canvas.rotate(i*45,32,32);canvas.drawLine(32,14,32,20,ink);canvas.restore();}}canvas.restore();}}
 static Intent playerIntent(boolean live){return new Intent(Intent.ACTION_MAIN).setClassName(PLAYER,"com.fongmi.android.tv.ui.activity.HomeActivity").putExtra("muzi_entry",live?"live":"vod").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);}
 void player(boolean live){try{if(live){android.content.pm.ApplicationInfo info=a.getPackageManager().getApplicationInfo(PLAYER,android.content.pm.PackageManager.GET_META_DATA);if(info.metaData==null||info.metaData.getInt("studio.muzi.entry.version",0)<1){a.toast("直播入口需要升级木子影界，正在检查更新");Updater.check(a);return;}}a.startActivity(playerIntent(live));}catch(Exception e){a.toast("请先安装支持独立直播入口的木子影界新版");}}
 void render(String focus){dock.removeAllViews();tile("木子影界","▷",()->player(false));tile("直播电视","▣",()->player(true));for(String pkg:pins){if(PLAYER.equals(pkg))continue;LinearLayout v=tile(label(pkg),null,()->launch(pkg));ImageView icon=new ImageView(a);try{icon.setImageDrawable(a.getPackageManager().getApplicationIcon(pkg));}catch(Exception e){icon.setImageResource(android.R.drawable.sym_def_app_icon);}v.addView(icon,0,new LinearLayout.LayoutParams(d(48),d(48)));v.setOnLongClickListener(view->{edit(pkg);return true;});v.setOnKeyListener((view,k,e)->{if(k==KeyEvent.KEYCODE_DPAD_CENTER||k==KeyEvent.KEYCODE_ENTER){if(e.getAction()==0){if(e.getRepeatCount()==0)held=false;if(e.getRepeatCount()>0&&!held){held=true;edit(pkg);}return true;}if(e.getAction()==1){if(!held)launch(pkg);return true;}}return false;});if(pkg.equals(focus))v.requestFocus();}tile("自定义应用","＋",()->picker(true));tile("设置","☰",this::settings);if(focus==null)dock.getChildAt(0).requestFocus();}
 void settings(){new AlertDialog.Builder(a).setTitle("桌面设置").setItems(new String[]{"全部应用","添加应用","管理桌面","动态壁纸 · "+(prefs.getBoolean("motion",true)?"开":"关"),"系统设置","检查桌面更新"},(dialog,n)->{if(n==0)picker(false);else if(n==1)picker(true);else if(n==2)manage();else if(n==3){motion.performClick();settings();}else if(n==4)a.openSystemSettings();else Updater.check(a);}).setNegativeButton("返回",null).show();}
 @Override public boolean dispatchKeyEvent(KeyEvent e){if(e.getKeyCode()==KeyEvent.KEYCODE_MENU){if(e.getAction()==KeyEvent.ACTION_UP)settings();return true;}return super.dispatchKeyEvent(e);}

 void launch(String pkg){if(!apps.containsKey(pkg)){a.toast("此应用已卸载或没有可启动入口，可从桌面移除");return;}a.launch(pkg);}
 void picker(boolean add){scan();ArrayList<String> ids=new ArrayList<>(apps.keySet());Collections.sort(ids,(x,y)->label(x).compareToIgnoreCase(label(y)));ids.remove(PLAYER);if(add)ids.removeAll(pins);String[] names=new String[ids.size()];for(int i=0;i<ids.size();i++)names[i]=label(ids.get(i));if(ids.isEmpty()){a.toast(add?"所有可启动应用已在桌面":"没有可启动的应用");return;}new AlertDialog.Builder(a).setTitle(add?"添加到桌面":"全部应用").setItems(names,(dlg,n)->{String pkg=ids.get(n);if(add){if(!pins.contains(pkg))pins.add(pkg);save();render(pkg);}else new AlertDialog.Builder(a).setTitle(label(pkg)).setItems(new String[]{"打开",pins.contains(pkg)?"已在桌面 · 管理":"添加到桌面"},(d,k)->{if(k==0)launch(pkg);else if(pins.contains(pkg))edit(pkg);else{pins.add(pkg);save();render(pkg);}}).setNegativeButton("返回",null).show();}).setNegativeButton("返回",null).show();}
 void manage(){ArrayList<String> custom=new ArrayList<>(pins);custom.remove(PLAYER);if(custom.isEmpty()){picker(true);return;}String[] names=new String[custom.size()];for(int i=0;i<names.length;i++)names[i]=label(custom.get(i));new AlertDialog.Builder(a).setTitle("管理桌面 · 选择应用").setItems(names,(dlg,n)->edit(custom.get(n))).setNegativeButton("返回",null).show();}
 void edit(String pkg){final int i=pins.indexOf(pkg);if(i<0)return;ArrayList<String> names=new ArrayList<>();ArrayList<Integer> actions=new ArrayList<>();if(i>0){names.add("向左移动");actions.add(-1);}if(i<pins.size()-1){names.add("向右移动");actions.add(1);}names.add("从桌面移除（不卸载应用）");actions.add(0);new AlertDialog.Builder(a).setTitle(label(pkg)).setItems(names.toArray(new String[0]),(dlg,n)->{int action=actions.get(n);int now=pins.indexOf(pkg);if(now<0)return;if(action==0)pins.remove(pkg);else Collections.swap(pins,now,now+action);save();render(action==0?null:pkg);}).setNegativeButton("返回",null).show();}
 void updateMotion(){motion.setText("动态壁纸 · "+(prefs.getBoolean("motion",true)?"开":"关"));sky.removeCallbacks(sky.redraw);sky.invalidate();}
 public void resume(){active=true;scan();render(null);handler.removeCallbacks(tick);handler.post(tick);sky.invalidate();}
 public void pause(){active=false;handler.removeCallbacks(tick);sky.removeCallbacks(sky.redraw);}
 @Override protected void onDetachedFromWindow(){pause();super.onDetachedFromWindow();}
 final class Sky extends View {
 final Paint paint=new Paint(3); final Bitmap wall,water;final float waterTop,waterBottom;LakeMotion lake;final Rect src;final Rect dst=new Rect();final Runnable redraw=()->invalidate();
 final Path skyMask=new Path(),waterMask=new Path();
 long elapsed=0,lastFrame=0;
 // Normalized shoreline and skyline keep mountains, meadow and app icons completely still.
 final float[] ridge={0,.45f,.04f,.456f,.065f,.436f,.09f,.459f,.13f,.429f,.16f,.455f,.205f,.435f,.253f,.431f,.28f,.46f,.325f,.471f,.375f,.427f,.404f,.444f,.431f,.404f,.48f,.362f,.506f,.388f,.542f,.416f,.567f,.411f,.608f,.448f,.636f,.452f,.673f,.474f,.704f,.493f,.754f,.47f,.78f,.49f,.82f,.49f,.86f,.508f,.90f,.499f,.94f,.512f,1,.48f};
 final float[] shore={.30f,.599f,.60f,.596f,.76f,.617f,1,.632f,1,.86f,.91f,.85f,.84f,.854f,.77f,.837f,.69f,.837f,.60f,.813f,.54f,.832f,.46f,.822f,.37f,.816f,.29f,.796f,.38f,.743f,.31f,.709f,.19f,.696f,.24f,.671f,.16f,.654f,.24f,.634f,.32f,.615f};
 Sky(){super(a);Bitmap b=null;try(java.io.InputStream in=a.getAssets().open("galaxy.jpg")){BitmapFactory.Options o=new BitmapFactory.Options();o.inSampleSize=1;b=BitmapFactory.decodeStream(in,null,o);}catch(Exception ignored){}wall=b;src=b==null?new Rect():new Rect(0,0,b.getWidth(),b.getHeight());int top=b==null?0:(int)(b.getHeight()*.595f),bottom=b==null?0:(int)Math.ceil(b.getHeight()*.87f);water=b==null?null:Bitmap.createBitmap(b,0,top,b.getWidth(),bottom-top);waterTop=b==null?.595f:top/(float)b.getHeight();waterBottom=b==null?.87f:bottom/(float)b.getHeight();}

 void polygon(Path p,float[] points,int w,int h,boolean sky){p.reset();if(sky){p.moveTo(0,0);p.lineTo(w,0);for(int i=points.length-2;i>=0;i-=2)p.lineTo(points[i]*w,points[i+1]*h);}else{p.moveTo(points[0]*w,points[1]*h);for(int i=2;i<points.length;i+=2)p.lineTo(points[i]*w,points[i+1]*h);}p.close();}
 @Override protected void onSizeChanged(int w,int h,int ow,int oh){polygon(skyMask,ridge,w,h,true);polygon(waterMask,shore,w,h,false);lake=w>0&&h>0?new LakeMotion(shore,80,48,waterTop,waterBottom,w,h):null;}
 @Override protected void onDraw(Canvas c){super.onDraw(c);int w=getWidth(),h=getHeight();c.drawColor(0xff08121e);dst.set(0,0,w,h);paint.setShader(null);paint.setAlpha(255);if(wall!=null)c.drawBitmap(wall,src,dst,paint);
 boolean moving=active&&prefs.getBoolean("motion",true)&&isShown();long now=SystemClock.uptimeMillis();if(moving){if(lastFrame>0)elapsed+=Math.min(now-lastFrame,100);lastFrame=now;}else lastFrame=0;float t=elapsed/1000f;
 if(prefs.getBoolean("motion",true)&&wall!=null){
 c.save();c.clipPath(skyMask);
 for(int i=0;i<2;i++){float q=((t+i*3.7f)%9)/1.5f;if(q<1){float x=w*(.83f-i*.24f-q*.25f),y=h*(.07f+i*.07f+q*.21f);paint.setStrokeWidth(Math.max(1,w/1100f));for(int j=0;j<16;j++){paint.setColor(0xffe1f5ff);paint.setAlpha((int)(220*Math.sin(q*Math.PI)*(1-j/16f)));c.drawLine(x+w*j*.004f,y-h*j*.0035f,x+w*(j+1)*.004f,y-h*(j+1)*.0035f,paint);}}}c.restore();
 if(water!=null&&lake!=null){c.save();c.clipPath(waterMask);paint.setAlpha(255);c.drawBitmapMesh(water,lake.cols,lake.rows,lake.frame(t),0,null,0,paint);c.restore();}
 }paint.setShader(null);paint.setAlpha(255);c.drawColor(0x16000a16);if(moving)postDelayed(redraw,50);}
 }
}
