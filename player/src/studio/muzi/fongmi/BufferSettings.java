package studio.muzi.fongmi;
import android.content.SharedPreferences;
public final class BufferSettings {
 public static void migrate(SharedPreferences prefs){
  if(!prefs.getBoolean("muzi.buffer3.migrated.v1",false))
   prefs.edit().putInt("buffer",3).putBoolean("muzi.buffer3.migrated.v1",true).commit();
 }
}
