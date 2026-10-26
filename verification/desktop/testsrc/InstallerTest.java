package studio.muzi.tv;
import org.junit.*;import static org.junit.Assert.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;import android.content.*;import android.net.Uri;import java.io.*;import android.database.Cursor;
@RunWith(RobolectricTestRunner.class) @Config(sdk=23,manifest=Config.NONE)
public class InstallerTest{
 MainActivity a;ApkProvider p;Uri uri;
 @Before public void setup()throws Exception{a=Robolectric.buildActivity(MainActivity.class).setup().get();p=Robolectric.buildContentProvider(ApkProvider.class).create().get();File dir=new File(a.getCacheDir(),"update");dir.mkdirs();try(FileOutputStream o=new FileOutputStream(new File(dir,"test.apk"))){o.write(new byte[]{1,2,3});}uri=Uri.parse("content://"+a.getPackageName()+".apk/test.apk");}
 @Test public void installerCanReadPrivateApk()throws Exception{assertEquals(3,p.openFile(uri,"r").getStatSize());}
 @Test public void installerReceivesFileMetadata(){Cursor c=p.query(uri,null,null,null,null);assertNotNull(c);assertTrue(c.moveToFirst());assertEquals("test.apk",c.getString(0));assertEquals(3,c.getLong(1));c.close();}
 @Test(expected=FileNotFoundException.class)public void noWriteAccess()throws Exception{p.openFile(uri,"w");}
 @Test(expected=FileNotFoundException.class)public void noPathTraversal()throws Exception{p.openFile(Uri.parse("content://"+a.getPackageName()+".apk/../secret.apk"),"r");}
 @Test public void android6UsesLegacyInstallerFileUri(){Intent i=Updater.installerIntent(a,new File("test.apk"));assertEquals("file",i.getData().getScheme());assertEquals(Intent.ACTION_INSTALL_PACKAGE,i.getAction());assertTrue(i.getBooleanExtra(Intent.EXTRA_RETURN_RESULT,false));assertNotNull(i.getClipData());assertTrue((i.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0);assertFalse((i.getFlags()&Intent.FLAG_ACTIVITY_NEW_TASK)!=0);}
 @Test public void android6UsesExternalCache()throws Exception{assertEquals(new File(a.getExternalCacheDir(),"update"),Updater.downloadDir(a));}
 @Test public void updateHostDoesNotDependOnComputerProxy()throws Exception{
 java.net.ProxySelector previous=java.net.ProxySelector.getDefault();final int[] calls={0};java.net.ProxySelector.setDefault(new java.net.ProxySelector(){public java.util.List<java.net.Proxy> select(java.net.URI u){calls[0]++;return java.util.Collections.singletonList(new java.net.Proxy(java.net.Proxy.Type.HTTP,new java.net.InetSocketAddress("127.0.0.1",1)));}public void connectFailed(java.net.URI u,java.net.SocketAddress a,java.io.IOException e){}});
 try{java.net.HttpURLConnection c=LegacyTls.open("https://zhishixingqiu.top/tv/version.json");assertFalse(c.usingProxy());assertEquals(0,calls[0]);c.disconnect();}finally{java.net.ProxySelector.setDefault(previous);}
 }
}
