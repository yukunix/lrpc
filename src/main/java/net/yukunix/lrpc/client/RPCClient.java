package net.yukunix.lrpc.client;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import net.yukunix.lrpc.client.proxy.BaseObjectProxy;
import net.yukunix.lrpc.client.proxy.IAsyncObjectProxy;
import net.yukunix.lrpc.client.proxy.ObjectProxy;

import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RPCClient {

	EventLoopGroup eventLoopGroup;
	
	ExecutorService threadPool;
	
	ClientProperties props;
	
	static RPCClient instane;;
	public RPCClient(ClientProperties props){
		this.props = props;
		
	    eventLoopGroup = new NioEventLoopGroup(props.ioThreadNum());
	    
	    LinkedBlockingDeque<Runnable> linkedBlockingDeque = new LinkedBlockingDeque<>();
        threadPool = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 600L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
//        threadPool = new BetterExecutorService(linkedBlockingDeque, executor,"Client async thread pool", props.asyncThreadPoolSize());
//        Executors.newFixedThreadPool(props.asyncThreadPoolSize());
	
	}
	
	public static RPCClient getInstance(ClientProperties props){
	    if(instane == null){
            synchronized (RPCClient.class) {
                if(instane == null){
                    instane= new RPCClient(props);
                }
            }
        }
        return instane;
	}

	
	public static <T> ObjProxyBuilder<T> proxyBuilder(Class<T> clazz){
	   return new ObjProxyBuilder<T>(clazz);
	}
	
	public static class ObjProxyBuilder<T> {
	    private Class<T> clazz;
        private String host;
        private int port;
        
	    public ObjProxyBuilder(Class<T> clazz) {
            this.clazz = clazz;
        }
        public ObjProxyBuilder<T> withServerNode(String host, int port){
	        this.host = host;
	        this.port = port;
	        return this;
	    }
	    
	    public T build(ClientProperties props){
	        InetSocketAddress server = new InetSocketAddress(host, port);
            T t = (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz }, new ObjectProxy<T>(props, server, clazz));
            return t;
	    }
	    
	    public IAsyncObjectProxy buildAsyncObjPrx(ClientProperties props){
            return new ObjectProxy<T>(props, new InetSocketAddress(host, port), clazz);
        }
	}

	
	public void submit(Runnable task){
		if(threadPool == null){
			synchronized (BaseObjectProxy.class) {
				if(threadPool== null){
					
				}
			}
		}
		
		threadPool.submit(task);
	}

	public EventLoopGroup getEventLoopGroup() {
		return eventLoopGroup;
	}
	
	public void shutdown(){
		eventLoopGroup.shutdownGracefully();
		threadPool.shutdown();
	}
}


