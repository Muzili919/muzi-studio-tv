package studio.muzi.fongmi;
import org.junit.*;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import android.app.Activity;
import android.view.*;
import android.widget.*;
import org.json.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=23,manifest=Config.NONE,assetDir="decoded/assets")
public class HomeSurfaceTest {
 Activity activity;HomeSurface surface;
 @Before public void setup(){activity=Robolectric.buildActivity(Activity.class).setup().get();FrameLayout root=new FrameLayout(activity);activity.setContentView(root);HomeSurface.install(activity);surface=HomeSurface.find(activity);}
 @After public void tearDown(){activity.finish();if(surface!=null){surface.worker.shutdownNow();surface.images.shutdownNow();}}
 @Test public void defaultIsMoviesLatest(){assertNotNull(surface);assertEquals(0,surface.category);assertEquals("latest",surface.order);assertEquals("最新",((Button)surface.sort.getChildAt(0)).getText().toString());assertTrue(surface.heading.getText().toString().contains("电影 · 最新"));}
 @Test public void allNavigationEntriesPresent(){assertEquals(6,surface.nav.getChildCount());assertEquals("搜索",((Button)surface.nav.getChildAt(5)).getText().toString());}
 @Test public void categoryResetsInvalidFilters(){surface.genre="恐怖";surface.area="欧美";surface.order="hot";surface.choose(4);assertEquals("全部",surface.genre);assertEquals("全部",surface.area);assertEquals("latest",surface.order);assertEquals(7,surface.regions.getChildCount());assertEquals("乒乓球",((Button)surface.regions.getChildAt(5)).getText().toString());}
 @Test public void originalHomeHiddenWithoutDestroyingIt(){ViewGroup content=activity.findViewById(android.R.id.content);assertEquals(2,content.getChildCount());assertEquals(View.GONE,content.getChildAt(0).getVisibility());HomeSurface.install(activity);assertEquals(2,content.getChildCount());}
 @Test public void searchUsesOriginalActivity(){surface.nav.getChildAt(5).performClick();assertTrue(Shadows.shadowOf(activity).getNextStartedActivity().getComponent().getClassName().endsWith("SearchActivity"));}
 @Test public void fixedRowsOutsidePosterGrid(){assertSame(surface,surface.grid.getParent());assertNotSame(surface.grid,surface.regions.getParent());assertNotSame(surface.grid,surface.sort.getParent());}
 @Test public void genreAreaOrderCompose()throws Exception{JSONArray rows=new JSONArray();rows.put(row("a","欧美","科幻",8));rows.put(row("b","日韩","科幻",9));rows.put(row("c","欧美","恐怖",9));JSONObject result=HomeSurface.localPage(rows,"1","recommend","欧美","科幻",1);assertEquals(1,result.getInt("total"));assertEquals("a",result.getJSONArray("list").getJSONObject(0).getString("vod_name"));}
 @Test public void missingGenreIsNotInvented()throws Exception{JSONObject x=row("a","欧美","",8);assertEquals(0,HomeSurface.localPage(new JSONArray().put(x),"1","latest","全部","恐怖",1).getInt("total"));assertEquals(1,HomeSurface.localPage(new JSONArray().put(x),"1","latest","全部","全部",1).getInt("total"));}
 @Test public void liveEntryIsConsumedAndWaitsForOriginalConfig(){android.content.Intent intent=new android.content.Intent().putExtra("muzi_entry","live");HomeSurface.entry(activity,intent);assertFalse(intent.hasExtra("muzi_entry"));assertTrue(surface.livePending);assertTrue(surface.heading.getText().toString().contains("直播电视"));HomeSurface.entry(activity,new android.content.Intent().putExtra("muzi_entry","vod"));assertFalse(surface.livePending);}
 @Test public void pingPongLocalFilterKeepsExistingRecord()throws Exception{JSONObject x=row("乒乓球决赛","", "",0).put("_kind","17").put("_sport","乒乓球");JSONObject y=row("足球","", "",0).put("_kind","17").put("_sport","足球");JSONObject result=HomeSurface.localPage(new JSONArray().put(x).put(y),"17","latest","乒乓球","全部",1);assertEquals(1,result.getInt("total"));assertEquals("乒乓球决赛",result.getJSONArray("list").getJSONObject(0).getString("vod_name"));}
 JSONObject row(String name,String area,String genre,int score)throws Exception{return new JSONObject().put("_kind","1").put("_region",area).put("_genres",new JSONArray().put(genre)).put("vod_name",name).put("vod_douban_score",score).put("vod_time","2026-10-23");}
}
