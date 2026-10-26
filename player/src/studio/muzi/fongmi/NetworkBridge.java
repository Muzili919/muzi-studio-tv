package studio.muzi.fongmi;
/** Only the existing Muzi service uses the Android6 trust/proxy compatibility path. */
public final class NetworkBridge {
 public static void installProxyFallback(){java.net.ProxySelector.setDefault(ProxyFallback.wrap(java.net.ProxySelector.getDefault()));}
 public static String fetchOwnHost(String value){try{java.net.URL url=new java.net.URL(value);if(!"zhishixingqiu.top".equalsIgnoreCase(url.getHost()))return null;String protocol=url.getProtocol();if(!"https".equals(protocol)&&!"http".equals(protocol))return null;return HomeSurface.fetch(value);}catch(Exception e){android.util.Log.w("muzi-network","own service fetch failed",e);return null;}}
}
