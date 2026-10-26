package studio.muzi.fongmi;
import org.junit.*;import static org.junit.Assert.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import android.app.Activity;import android.content.Intent;import android.widget.EditText;import android.speech.RecognizerIntent;import android.view.KeyEvent;import java.util.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=23,manifest=Config.NONE)
public class VoiceSearchTest{
 public static class Search extends Activity{public int searches;public void I(){searches++;}}
 @Test public void resultFillsTextAndUsesExistingSearch(){Search a=Robolectric.buildActivity(Search.class).setup().get();EditText edit=new EditText(a);a.setContentView(edit);Intent data=new Intent().putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS,new ArrayList<String>(Arrays.asList("西游记")));assertTrue(VoiceSearch.result(a,VoiceSearch.REQUEST,Activity.RESULT_OK,data));assertEquals("西游记",edit.getText().toString());assertEquals(1,a.searches);a.finish();}
 @Test public void cancellationKeepsTypedSearch(){Search a=Robolectric.buildActivity(Search.class).setup().get();EditText edit=new EditText(a);edit.setText("原片名");a.setContentView(edit);VoiceSearch.result(a,VoiceSearch.REQUEST,Activity.RESULT_CANCELED,null);assertEquals("原片名",edit.getText().toString());assertEquals(0,a.searches);a.finish();}
 @Test public void remoteMicKeysAreSupported(){assertTrue(VoiceSearch.voiceKey(KeyEvent.KEYCODE_VOICE_ASSIST));assertTrue(VoiceSearch.voiceKey(KeyEvent.KEYCODE_ASSIST));assertTrue(VoiceSearch.voiceKey(KeyEvent.KEYCODE_SEARCH));assertFalse(VoiceSearch.voiceKey(KeyEvent.KEYCODE_HOME));}
 @Test public void recognitionRequestsChineseFreeForm(){assertEquals("zh-CN",VoiceSearch.intent().getStringExtra(RecognizerIntent.EXTRA_LANGUAGE));assertEquals(RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,VoiceSearch.intent().getStringExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL));}
}
