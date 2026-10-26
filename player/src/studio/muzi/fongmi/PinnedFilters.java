package studio.muzi.fongmi;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** API 23-compatible, scoped to the reviewed muzi_catalog_v2 provider.
 * The original Exo/IJK player and source switching paths are not replaced.
 */
public final class PinnedFilters extends LinearLayout {
    private static final String TAG = "MuziPinnedFilters";
    private static final String OWNER = "muzi_catalog_v2";
    private final Object fragment;
    private final View grid;
    private final Map<String,String> values;
    private final SharedPreferences prefs;
    private final String kind;
    private final List<List<Button>> rows = new ArrayList<List<Button>>();
    private final List<String> keys = new ArrayList<String>();
    private final List<List<String>> choices = new ArrayList<List<String>>();

    private static Object field(Object object, String name) throws Exception {
        for (Class<?> c = object.getClass(); c != null; c = c.getSuperclass()) {
            try { Field f=c.getDeclaredField(name); f.setAccessible(true); return f.get(object); }
            catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name);
    }
    private static Object call(Object obj, String method) throws Exception {
        return obj.getClass().getMethod(method).invoke(obj);
    }
    private static String string(Object obj, String method) throws Exception {
        Object v=call(obj,method); return v==null?"":v.toString();
    }
    private int dp(int n) { return Math.round(n*getResources().getDisplayMetrics().density); }

    @SuppressWarnings("unchecked")
    private PinnedFilters(Object fragment, View grid, List<?> filters, String kind) throws Exception {
        super(grid.getContext()); this.fragment=fragment; this.grid=grid; this.kind=kind;
        this.values=(Map<String,String>)field(fragment,"j0");
        this.prefs=getContext().getSharedPreferences("muzi-pinned-filters",Context.MODE_PRIVATE);
        setOrientation(VERTICAL); setClipChildren(false); setClipToPadding(false);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        for (Object filter: filters) {
            String key=string(filter,"getKey");
            if (!key.equals("order") && !key.equals("area")) continue;
            List<?> options=(List<?>)call(filter,"getValue");
            if(options.isEmpty()) continue;
            final int rowIndex=rows.size(); final String filterKey=key;
            List<Button> buttons=new ArrayList<Button>(); List<String> optionValues=new ArrayList<String>();
            keys.add(key); rows.add(buttons); choices.add(optionValues);
            HorizontalScrollView scroll=new HorizontalScrollView(getContext());
            scroll.setHorizontalScrollBarEnabled(false); scroll.setClipToPadding(false);
            scroll.setPadding(dp(24),dp(4),dp(24),dp(4)); scroll.setFocusable(false);
            LinearLayout row=new LinearLayout(getContext()); row.setOrientation(HORIZONTAL);
            row.setClipChildren(false); row.setClipToPadding(false);
            for (Object option: options) {
                final String value=string(option,"getV");
                Button button=new Button(getContext()); button.setId(View.generateViewId());
                button.setText(string(option,"getN")); button.setTextSize(17);button.setAllCaps(false);
                button.setMinHeight(0); button.setMinimumHeight(0);button.setMinWidth(dp(68));
                button.setPadding(dp(15),0,dp(15),0);button.setFocusable(true);
                button.setOnFocusChangeListener(new View.OnFocusChangeListener(){public void onFocusChange(View v,boolean f){paint();}});
                button.setOnClickListener(new View.OnClickListener(){public void onClick(View v){
                    if(value.equals(values.get(filterKey))) return;
                    String before=values.get(filterKey); values.put(filterKey,value);
                    try {
                        fragment.getClass().getMethod("Y").invoke(fragment);
                        prefs.edit().putString(kind+":"+filterKey,value).apply();
                    } catch(Exception e){
                        if(before==null)values.remove(filterKey);else values.put(filterKey,before);
                        android.widget.Toast.makeText(getContext(),"筛选更新失败，请重试",android.widget.Toast.LENGTH_SHORT).show();
                        Log.e(TAG,"refresh failed",e);
                    }
                    paint();
                }});
                button.setOnKeyListener(new View.OnKeyListener(){public boolean onKey(View v,int code,KeyEvent e){
                    if(e.getAction()!=KeyEvent.ACTION_DOWN)return false;
                    if(code==KeyEvent.KEYCODE_DPAD_DOWN){
                        if(rowIndex+1<rows.size())selected(rowIndex+1).requestFocus();else grid.requestFocus();
                        return true;
                    }
                    if(code==KeyEvent.KEYCODE_DPAD_UP && rowIndex>0){selected(rowIndex-1).requestFocus();return true;}
                    return false;
                }});
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,dp(40));lp.setMarginEnd(dp(8));
                row.addView(button,lp);buttons.add(button);optionValues.add(value);
            }
            String fallback=values.get(key);
            if(fallback==null || !optionValues.contains(fallback))fallback=string(filter,"getInit");
            if(!optionValues.contains(fallback))fallback=optionValues.get(0);
            String saved=prefs.getString(kind+":"+key,fallback);
            values.put(key,optionValues.contains(saved)?saved:fallback);
            scroll.addView(row,new HorizontalScrollView.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.MATCH_PARENT));
            addView(scroll,new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,dp(50)));
        }
        if(rows.size()!=2)throw new IllegalArgumentException("Expected order and area filter rows");
        paint();
    }
    private Button selected(int r){int i=choices.get(r).indexOf(values.get(keys.get(r)));return rows.get(r).get(Math.max(i,0));}
    private void paint(){
        for(int r=0;r<rows.size();r++)for(int i=0;i<rows.get(r).size();i++){
            Button b=rows.get(r).get(i); boolean active=choices.get(r).get(i).equals(values.get(keys.get(r)));
            GradientDrawable bg=new GradientDrawable();bg.setCornerRadius(dp(8));
            bg.setColor(active?Color.rgb(214,233,237):Color.argb(b.hasFocus()?110:32,40,63,82));
            if(b.hasFocus())bg.setStroke(dp(2),Color.rgb(190,233,243));
            b.setBackground(bg);b.setTextColor(active?Color.rgb(18,39,53):Color.rgb(234,242,248));
        }
    }
    @Override public View focusSearch(View focused,int direction){
        if(direction==View.FOCUS_UP && insideGrid(focused)){
            try{if(((Number)call(grid,"getSelectedPosition")).intValue()==0)return selected(rows.size()-1);}catch(Exception e){Log.w(TAG,"focus position",e);}
        }
        return super.focusSearch(focused,direction);
    }
    private boolean insideGrid(View v){
        while(v!=null){if(v==grid)return true;v=v.getParent() instanceof View?(View)v.getParent():null;}return false;
    }

    public static void attach(Object fragment){
        View grid=null;ViewGroup parent=null;PinnedFilters panel=null;ViewGroup.LayoutParams original=null;int index=-1;
        try{
            Bundle args=(Bundle)field(fragment,"s");
            if(args==null || !OWNER.equals(args.getString("key")))return;
            Object binding=field(fragment,"k0");grid=(View)field(binding,"p");
            if(grid.getParent() instanceof PinnedFilters)return;
            List<?> filters=(List<?>)field(fragment,"p0");
            parent=(ViewGroup)grid.getParent();if(parent==null)return;
            panel=new PinnedFilters(fragment,grid,filters,args.getString("typeId","1"));
            original=grid.getLayoutParams();index=parent.indexOfChild(grid);
            // Prevent the original scroll listener from hiding the category header.
            grid.getClass().getMethod("setHeader",View[].class).invoke(grid,(Object)new View[0]);
            parent.removeView(grid);
            panel.addView(grid,new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,0,1));
            parent.addView(panel,index,original);
            Log.i(TAG,"attached two fixed filter rows, kind="+panel.kind);
        }catch(Exception e){
            if(parent!=null && grid!=null && index>=0 && grid.getParent()!=parent){
                if(grid.getParent() instanceof ViewGroup)((ViewGroup)grid.getParent()).removeView(grid);
                if(panel!=null && panel.getParent()==parent)parent.removeView(panel);
                parent.addView(grid,Math.min(index,parent.getChildCount()),original);
            }
            Log.e(TAG,"keeping original category UI",e);
        }
    }
    /** Intercept the old collapse action only after our panel has been attached. */
    public static boolean handleToggle(Activity activity){
        if(!OWNER.equals(activity.getIntent().getStringExtra("key")))return false;
        try{
            Object fragment=call(activity,"H");View grid=(View)field(field(fragment,"k0"),"p");
            if(!(grid.getParent() instanceof PinnedFilters))return false;
            ((PinnedFilters)grid.getParent()).selected(0).requestFocus();return true;
        }catch(Exception e){Log.w(TAG,"toggle fallback",e);return false;}
    }
}
