package studio.muzi.tv;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import studio.muzi.tv.HomeRedirectService;

/* loaded from: classes3.dex */
public class HomeRedirectService extends AccessibilityService {
    private BroadcastReceiver homeKey;
    private long last = 0;

    /* renamed from: studio.muzi.tv.HomeRedirectService$1, reason: invalid class name */
    class AnonymousClass1 extends BroadcastReceiver {
        AnonymousClass1() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context c, Intent i) {
            String reason = i.getStringExtra("reason");
            Log.i("MuziHome", "close dialogs: " + reason);
            if ("homekey".equals(reason)) {
                new Handler().postDelayed(new Runnable() { // from class: studio.muzi.tv.HomeRedirectService$1$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        HomeRedirectService.AnonymousClass1.this.m1651lambda$onReceive$0$studiomuzitvHomeRedirectService$1();
                    }
                }, 180L);
            }
        }

        /* renamed from: lambda$onReceive$0$studio-muzi-tv-HomeRedirectService$1, reason: not valid java name */
        /* synthetic */ void m1651lambda$onReceive$0$studiomuzitvHomeRedirectService$1() {
            HomeRedirectService.this.startActivity(new Intent(HomeRedirectService.this, (Class<?>) MainActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }
    }

    @Override // android.accessibilityservice.AccessibilityService
    @android.annotation.SuppressLint("UnspecifiedRegisterReceiverFlag") // Pre-33 overload; API 33+ branch supplies the flag.
    protected void onServiceConnected() {
        super.onServiceConnected();
        this.homeKey = new AnonymousClass1();
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(this.homeKey, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"), Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(this.homeKey, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
        }
    }

    @Override // android.app.Service
    public void onDestroy() {
        if (this.homeKey != null) {
            unregisterReceiver(this.homeKey);
        }
        super.onDestroy();
    }

    @Override // android.accessibilityservice.AccessibilityService
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }
        String pkg = String.valueOf(event.getPackageName());
        String name = String.valueOf(event.getClassName());
        Log.i("MuziHome", pkg + " / " + name);
        if (pkg.equals("com.mitv.tvhome")) {
            if (name.equals("com.mitv.tvhome.MainActivity") || name.equals("com.mitv.tvhome.MainActivityUserMode")) {
                long now = SystemClock.elapsedRealtime();
                if (now - this.last < 800) {
                    return;
                }
                this.last = now;
                startActivity(new Intent(this, (Class<?>) MainActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        }
    }

    @Override // android.accessibilityservice.AccessibilityService
    public void onInterrupt() {
    }
}
