package studio.muzi.tv;
import org.junit.*;import static org.junit.Assert.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;import android.content.*;import android.content.pm.*;import android.app.*;import android.widget.*;import java.lang.reflect.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=23,manifest=Config.NONE) @LooperMode(LooperMode.Mode.PAUSED)
public class DesktopTest {
 MainActivity a;DesktopView v;
 @Before public void setUp()throws Exception{a=Robolectric.buildActivity(MainActivity.class).setup().get();a.getSharedPreferences("desktop-v2",0).edit().clear().commit();Field f=Updater.class.getDeclaredField("lastCheck");f.setAccessible(true);f.setLong(null,System.currentTimeMillis());for(String p:new String[]{"com.fongmi.android.tv","example.app"}){ResolveInfo r=new ResolveInfo();r.activityInfo=new ActivityInfo();r.activityInfo.packageName=p;r.activityInfo.name=p+".Main";r.activityInfo.applicationInfo=new ApplicationInfo();r.activityInfo.applicationInfo.packageName=p;r.nonLocalizedLabel=p;Shadows.shadowOf(a.getPackageManager()).addResolveInfoForIntent(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER),r);}v=new DesktopView(a);a.setContentView(v);}
 @After public void tearDown(){v.pause();a.finish();}
 @Test public void desktopHasSeparateEntries(){assertEquals("木子影界",v.dock.getChildAt(0).getContentDescription());assertEquals("直播电视",v.dock.getChildAt(1).getContentDescription());assertEquals("设置",v.dock.getChildAt(v.dock.getChildCount()-1).getContentDescription());}
 @Test public void liveAndVodHaveDistinctRoutes(){assertEquals("live",DesktopView.playerIntent(true).getStringExtra("muzi_entry"));assertEquals("vod",DesktopView.playerIntent(false).getStringExtra("muzi_entry"));assertEquals("com.fongmi.android.tv",DesktopView.playerIntent(true).getComponent().getPackageName());}
 @Test public void enumeratesInstalledApplications(){assertTrue(v.apps.containsKey("example.app"));assertFalse(v.apps.containsKey(a.getPackageName()));}
 @Test public void addAndPersistActualPackage(){v.picker(true);AlertDialog dlg=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();dlg.getListView().performItemClick(null,0,0);assertTrue(v.pins.contains("example.app"));DesktopView again=new DesktopView(a);assertEquals(v.pins,again.pins);again.pause();}
 @Test public void reorderPersists(){v.pins.clear();v.pins.add("com.fongmi.android.tv");v.pins.add("example.app");v.edit("example.app");org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().getListView().performItemClick(null,0,0);assertEquals("example.app",v.pins.get(0));assertEquals(v.pins,new DesktopView(a).pins);}
 @Test public void removeShortcutKeepsApplication(){v.pins.clear();v.pins.add("example.app");v.edit("example.app");org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().getListView().performItemClick(null,0,0);assertFalse(v.pins.contains("example.app"));assertTrue(v.apps.containsKey("example.app"));}
 @Test public void pauseDisablesAnimation(){v.resume();assertTrue(v.active);v.pause();assertFalse(v.active);}
 @Test public void wallpaperSwitchPersists(){v.motion.performClick();assertFalse(v.prefs.getBoolean("motion",true));assertTrue(new DesktopView(a).motion.getText().toString().endsWith("关"));}
 @Test public void launchUsesSelectedApplication(){v.launch("example.app");assertEquals("example.app",a.launched);}
 @Test public void absentApplicationNotLaunched(){v.launch("missing.app");assertNull(a.launched);}
 PackageInfo info(String pkg,int version,String signature){PackageInfo p=new PackageInfo();p.packageName=pkg;p.versionCode=version;p.signatures=new Signature[]{new Signature(signature)};return p;}
 @Test public void validSignedUpgradeAccepted()throws Exception{Updater.validateCandidate(info("studio.muzi.tv",120,"abcd"),info("studio.muzi.tv",118,"abcd"),"studio.muzi.tv",120);}
 @Test(expected=Exception.class)public void wrongSignatureRejected()throws Exception{Updater.validateCandidate(info("studio.muzi.tv",120,"ffff"),info("studio.muzi.tv",118,"abcd"),"studio.muzi.tv",120);}
 @Test(expected=Exception.class)public void downgradeRejected()throws Exception{Updater.validateCandidate(info("studio.muzi.tv",117,"abcd"),info("studio.muzi.tv",118,"abcd"),"studio.muzi.tv",117);}
 @Test(expected=Exception.class)public void wrongPackageRejected()throws Exception{Updater.validateCandidate(info("other",120,"abcd"),info("studio.muzi.tv",118,"abcd"),"studio.muzi.tv",120);}
 @Test(expected=Exception.class)public void wrongManifestVersionRejected()throws Exception{Updater.validateCandidate(info("studio.muzi.tv",119,"abcd"),info("studio.muzi.tv",118,"abcd"),"studio.muzi.tv",120);}
}
