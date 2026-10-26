import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import java.util.*;
import studio.muzi.fongmi.PinnedFilters;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=23,manifest=Config.NONE)
public class PinnedFiltersTest {
 public static class Opt {String n,v;Opt(String n,String v){this.n=n;this.v=v;}public String getN(){return n;}public String getV(){return v;}}
 public static class Filter {String key;List<Opt> opts;Filter(String k,Opt...o){key=k;opts=Arrays.asList(o);}public String getKey(){return key;}public String getInit(){return opts.get(0).v;}public List<Opt> getValue(){return opts;}}
 public static class Grid extends LinearLayout {public View[] headers;public Grid(Activity a){super(a);setFocusable(true);}public void setHeader(View...v){headers=v;}public int getSelectedPosition(){return 0;}}
 public static class Binding {public View p;Binding(View v){p=v;}}
 public static class Fragment {public Bundle s=new Bundle();public Binding k0;public Map<String,String> j0=new HashMap<>();public List<Filter> p0;public int refreshes;public void Y(){refreshes++;}}
 Fragment setup(String key,String kind){Activity a=Robolectric.buildActivity(Activity.class).setup().get();FrameLayout root=new FrameLayout(a);Grid grid=new Grid(a);root.addView(grid,new FrameLayout.LayoutParams(-1,-1));a.setContentView(root);Fragment f=new Fragment();f.s.putString("key",key);f.s.putString("typeId",kind);f.k0=new Binding(grid);f.p0=Arrays.asList(new Filter("order",new Opt("推荐","recommend"),new Opt("最新","latest"),new Opt("热门","hot")),new Filter("area",new Opt("全部","全部"),new Opt("日韩","日韩")));return f;}
 Button button(View v,String text){if(v instanceof Button && text.contentEquals(((Button)v).getText()))return (Button)v;if(v instanceof android.view.ViewGroup){android.view.ViewGroup p=(android.view.ViewGroup)v;for(int i=0;i<p.getChildCount();i++){Button b=button(p.getChildAt(i),text);if(b!=null)return b;}}return null;}
 @Test public void twoRowsSeparateFromScrollingGrid(){Fragment f=setup("muzi_catalog_v2","1");PinnedFilters.attach(f);assertTrue(f.k0.p.getParent() instanceof PinnedFilters);LinearLayout panel=(LinearLayout)f.k0.p.getParent();assertEquals(3,panel.getChildCount());assertTrue(panel.getChildAt(0) instanceof HorizontalScrollView);assertTrue(panel.getChildAt(1) instanceof HorizontalScrollView);assertSame(f.k0.p,panel.getChildAt(2));assertEquals(0,((Grid)f.k0.p).headers.length);}
 @Test public void filterUpdatesAndRemainsVisible(){Fragment f=setup("muzi_catalog_v2","1");PinnedFilters.attach(f);View panel=(View)f.k0.p.getParent();button(panel,"最新").performClick();assertEquals("latest",f.j0.get("order"));assertEquals(1,f.refreshes);assertEquals(View.VISIBLE,panel.getVisibility());button(panel,"日韩").performClick();assertEquals("日韩",f.j0.get("area"));assertEquals("latest",f.j0.get("order"));assertEquals(2,f.refreshes);}
 @Test public void attachIsIdempotent(){Fragment f=setup("muzi_catalog_v2","1");PinnedFilters.attach(f);Object p=f.k0.p.getParent();PinnedFilters.attach(f);assertSame(p,f.k0.p.getParent());assertEquals(3,((LinearLayout)p).getChildCount());}
 @Test public void otherProvidersStayOriginal(){Fragment f=setup("backup_bf","1");Object p=f.k0.p.getParent();PinnedFilters.attach(f);assertSame(p,f.k0.p.getParent());}
 @Test public void invalidFiltersKeepOriginalGrid(){Fragment f=setup("muzi_catalog_v2","1");f.p0=Collections.emptyList();Object p=f.k0.p.getParent();PinnedFilters.attach(f);assertSame(p,f.k0.p.getParent());}
 @Test public void remembersSelectionsByCategory(){Fragment a=setup("muzi_catalog_v2","1");PinnedFilters.attach(a);button((View)a.k0.p.getParent(),"最新").performClick();Fragment b=setup("muzi_catalog_v2","1");PinnedFilters.attach(b);assertEquals("latest",b.j0.get("order"));Fragment tv=setup("muzi_catalog_v2","2");PinnedFilters.attach(tv);assertEquals("recommend",tv.j0.get("order"));}
 @Test public void gridUpReturnsToSecondRow(){Fragment f=setup("muzi_catalog_v2","1");PinnedFilters.attach(f);PinnedFilters panel=(PinnedFilters)f.k0.p.getParent();assertSame(button(panel,"全部"),panel.focusSearch(f.k0.p,View.FOCUS_UP));}
 @Test public void downMovesThroughBothRows(){Fragment f=setup("muzi_catalog_v2","1");PinnedFilters.attach(f);View panel=(View)f.k0.p.getParent();Button first=button(panel,"推荐"),second=button(panel,"全部");first.requestFocus();first.dispatchKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,android.view.KeyEvent.KEYCODE_DPAD_DOWN));assertTrue(second.hasFocus());second.dispatchKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,android.view.KeyEvent.KEYCODE_DPAD_DOWN));assertTrue(f.k0.p.hasFocus());}

}
