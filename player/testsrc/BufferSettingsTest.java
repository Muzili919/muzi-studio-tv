package studio.muzi.fongmi;
import org.junit.*;import static org.junit.Assert.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import android.content.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=23,manifest=Config.NONE)
public class BufferSettingsTest {
 @Test public void freshInstallGetsThree(){SharedPreferences p=RuntimeEnvironment.getApplication().getSharedPreferences("fresh",0);BufferSettings.migrate(p);assertEquals(3,p.getInt("buffer",0));}
 @Test public void existingPreferenceMigrates(){SharedPreferences p=RuntimeEnvironment.getApplication().getSharedPreferences("old",0);p.edit().putInt("buffer",1).commit();BufferSettings.migrate(p);assertEquals(3,p.getInt("buffer",0));}
 @Test public void laterUserChangesRemainEditable(){SharedPreferences p=RuntimeEnvironment.getApplication().getSharedPreferences("editable",0);BufferSettings.migrate(p);p.edit().putInt("buffer",4).commit();BufferSettings.migrate(p);assertEquals(4,p.getInt("buffer",0));}
}
