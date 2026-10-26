package studio.muzi.tv;
import org.junit.Test;import static org.junit.Assert.*;import org.json.JSONObject;import org.junit.runner.RunWith;import org.robolectric.RobolectricTestRunner;import org.robolectric.annotation.Config;
@RunWith(RobolectricTestRunner.class) @Config(sdk=23,manifest=Config.NONE)
public class ReleaseTest{
 JSONObject versions()throws Exception{return new JSONObject().put("studio",new JSONObject().put("versionCode",125).put("minSdk",23)).put("fongmi",new JSONObject().put("versionCode",414).put("minSdk",23));}
 @Test public void playerUpdatesBeforeDesktop()throws Exception{assertEquals("fongmi",Updater.nextTarget(versions(),123,412,23));assertEquals("studio",Updater.nextTarget(versions(),123,414,23));assertEquals("",Updater.nextTarget(versions(),125,414,23));}
 @Test public void missingPlayerIsNotInstalledAutomatically()throws Exception{assertEquals("studio",Updater.nextTarget(versions(),123,-1,23));}
 @Test public void incompatiblePlayerDoesNotBlockDesktop()throws Exception{JSONObject v=versions();v.getJSONObject("fongmi").put("minSdk",24);assertEquals("studio",Updater.nextTarget(v,123,412,23));}
 @Test public void waterPreservesShoreAndVariesAcrossSurface(){float[] shore={.2f,.6f,.9f,.6f,.9f,.85f,.2f,.85f};LakeMotion m=new LakeMotion(shore,80,48,.595f,.87f,1920,1080);float[] first=m.frame(2).clone();float[] second=m.frame(4);int moving=0,fixed=0;for(int i=0;i<m.weights.length;i++){float dx=Math.abs(first[i*2]-second[i*2]),dy=Math.abs(first[i*2+1]-second[i*2+1]);if(m.weights[i]==0){assertEquals(0f,dx,0f);assertEquals(0f,dy,0f);fixed++;}else if(dx+dy>.01f)moving++;assertTrue(Math.abs(second[i*2]-m.base[i*2])<1);assertTrue(Math.abs(second[i*2+1]-m.base[i*2+1])<1.5);}assertTrue(moving>100);assertTrue(fixed>100);}
}
