package studio.muzi.fongmi;
import org.junit.Test;import static org.junit.Assert.*;import java.net.*;import java.util.*;import java.io.*;
public class ProxyFallbackTest {
 ProxySelector delegate(final Proxy proxy){return new ProxySelector(){public List<Proxy> select(URI uri){return Collections.singletonList(proxy);}public void connectFailed(URI u,SocketAddress a,IOException e){}};}
 @Test public void unavailableLanProxyFallsBackToDirect()throws Exception{Proxy source=new Proxy(Proxy.Type.HTTP,new InetSocketAddress("127.0.0.1",1));assertEquals(Proxy.NO_PROXY,ProxyFallback.wrap(delegate(source)).select(new URI("http://example.com")).get(0));}
 @Test public void unresolvedPrivateProxyAlsoFallsBack()throws Exception{Proxy source=new Proxy(Proxy.Type.HTTP,InetSocketAddress.createUnresolved("127.0.0.1",1));assertEquals(Proxy.NO_PROXY,ProxyFallback.wrap(delegate(source)).select(new URI("http://example.com")).get(0));}
 @Test public void unresolvedWorkingLocalProxyIsKept()throws Exception{try(ServerSocket server=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"))){Proxy source=new Proxy(Proxy.Type.HTTP,InetSocketAddress.createUnresolved("127.0.0.1",server.getLocalPort()));assertEquals(source,ProxyFallback.wrap(delegate(source)).select(new URI("http://example.com")).get(0));}}
 @Test public void unresolvedLanAddressIsRecognized(){assertTrue(ProxyFallback.local(InetSocketAddress.createUnresolved("192.168.255.254",17890)));}
 @Test public void publicProxyIsPreserved()throws Exception{Proxy source=new Proxy(Proxy.Type.HTTP,new InetSocketAddress("203.0.113.1",8080));assertEquals(source,ProxyFallback.wrap(delegate(source)).select(new URI("http://example.com")).get(0));}
 @Test public void workingLanProxyIsPreserved()throws Exception{try(ServerSocket server=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"))){Proxy source=new Proxy(Proxy.Type.HTTP,new InetSocketAddress("127.0.0.1",server.getLocalPort()));assertEquals(source,ProxyFallback.wrap(delegate(source)).select(new URI("http://example.com")).get(0));}}
}
