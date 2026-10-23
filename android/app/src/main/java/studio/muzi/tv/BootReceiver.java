package studio.muzi.tv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import java.io.File;

/* loaded from: classes3.dex */
public class BootReceiver extends BroadcastReceiver {
    @Override // android.content.BroadcastReceiver
    public void onReceive(Context c, Intent i) {
        if (!"android.intent.action.BOOT_COMPLETED".equals(i.getAction()) || new File(c.getFilesDir(), "disable-home-redirect").exists()) {
            return;
        }
        c.startActivity(new Intent(c, (Class<?>) MainActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }
}
