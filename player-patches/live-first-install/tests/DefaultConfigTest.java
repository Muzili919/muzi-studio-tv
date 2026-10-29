import studio.muzi.fongmi.DefaultConfig;
import com.fongmi.android.tv.bean.Config;
public class DefaultConfigTest {
    static void check(boolean value, String name) { if(!value)throw new AssertionError(name);System.out.println("PASS: "+name); }
    public static void main(String[] args) {
        DefaultConfig.ensure();
        check(Config.writes==2 && Config.saved[0].url.startsWith("https://") && Config.saved[1].url.equals(Config.saved[0].url), "fresh install initializes both slots");
        DefaultConfig.ensure();
        check(Config.writes==2, "repeat startup makes no duplicate writes");
        Config customVod=new Config(0,"https://customer.example/vod"), customLive=new Config(1,"file:///sdcard/customer.m3u");
        Config.saved=new Config[]{customVod,customLive}; DefaultConfig.ensure();
        check(Config.saved[0]==customVod && Config.saved[1]==customLive && Config.writes==2,"custom and local subscriptions remain intact");
        check(Config.saved[0].historyToken.equals("existing-history"),"existing object is retained");
        Config.saved[1]=new Config(1,""); DefaultConfig.ensure();
        check(Config.saved[0]==customVod && Config.writes==3,"only missing live slot is initialized");
        Config.saved[1]=new Config(1,"");Config.failVod=true;DefaultConfig.ensure();
        check(Config.writes==4 && android.util.Log.failures==1,"one slot error neither crashes nor blocks the other");
    }
}
