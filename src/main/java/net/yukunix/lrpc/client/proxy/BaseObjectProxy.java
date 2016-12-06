package net.yukunix.lrpc.client.proxy;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.socket.nio.NioSocketChannel;
import net.yukunix.lrpc.client.ClientProperties;
import net.yukunix.lrpc.client.DefaultClientHandler;
import net.yukunix.lrpc.client.RPCClient;
import net.yukunix.lrpc.client.RPCClientInitializer;
import net.yukunix.lrpc.stub.Constants;
import net.yukunix.lrpc.stub.RPCContext;
import net.yukunix.lrpc.stub.Request;

public class BaseObjectProxy<T> {

	private final Logger logger = LogManager.getLogger(BaseObjectProxy.class);
	 
	protected Class<T> clazz;
	private String objName;
	
	private ReentrantLock lock = new ReentrantLock();
	private Condition connected  = lock.newCondition(); 
	
	
	protected CopyOnWriteArrayList<DefaultClientHandler> connectedHandlers = new CopyOnWriteArrayList<DefaultClientHandler>();
	private Map<InetSocketAddress, DefaultClientHandler> connectedServerNodes = new ConcurrentHashMap<>();
	
	private AtomicInteger roundRobin = new AtomicInteger(0);

	ClientProperties props;
	protected long syncCallTimeOutMillis;
    protected long connectTimeoutMillis;
    protected long reconnIntervalMillis;
    
	public void setClazz(Class<T> clazz) {
		this.clazz = clazz;
	}

	public Class<T> getClazz() {
		return clazz;
	}
	
	public ClientProperties getProps() {
		return props;
	}
	
	public BaseObjectProxy(ClientProperties props, InetSocketAddress server, Class<T> clazz) {
		this.props = props;
	    this.clazz = clazz;
	    this.objName = clazz.getName();
	    this.connectTimeoutMillis = props.connectTimeout();
	    this.reconnIntervalMillis = props.reconnInterval();
	    this.syncCallTimeOutMillis = props.syncCallTimeOut();

        connect(server);
	}
	
	private void addHandler(DefaultClientHandler handler){
	    connectedHandlers.add(handler);
	    connectedServerNodes.put((InetSocketAddress)handler.getChannel().remoteAddress(), handler);
	    signalAvailableHandler();
	}
	
	private void connect(final InetSocketAddress remotePeer) {
	    doConnect(null, remotePeer, 0);
	}
	
	public void  reconnect(final Channel channel, final SocketAddress remotePeer){
	    doConnect(channel, remotePeer , reconnIntervalMillis);
	}

	private void doConnect(final Channel channel, final SocketAddress remotePeer, long delay) {
	    if(channel != null){
	        connectedHandlers.remove(channel.pipeline().get(DefaultClientHandler.class));
	    }
		
		RPCClient.getInstance(props).getEventLoopGroup().schedule(new Runnable(){
			@Override
			public void run() {
				try {
				 	 Bootstrap b = new Bootstrap();
				 	 b.group(RPCClient.getInstance(props).getEventLoopGroup())
				 	     .channel(NioSocketChannel.class)
				 	     .handler(new RPCClientInitializer( BaseObjectProxy.this, props));
				 	 
					 ChannelFuture channelFuture = b.connect(remotePeer);
					 
					 channelFuture.addListener(new ChannelFutureListener(){
						@Override
						public void operationComplete(final ChannelFuture channelFuture) throws Exception {
							if(!channelFuture.isSuccess()){
							    logger.info("Can't connect to remote server. objName=" + objName + "|remote peer=" + remotePeer.toString());
								reconnect(channelFuture.channel(), remotePeer );
							}else{
							    logger.info("Successfully connect to remote server. objName=" + objName + "|remote peer=" + remotePeer);
								DefaultClientHandler handler = channelFuture.channel().pipeline().get(DefaultClientHandler.class);
								addHandler(handler);
							}
						}
					 });
	
				} catch (Exception e) {
					logger.warn("doConnect got exception|msg="+e.getMessage(),e);
					reconnect(channel, remotePeer);
				}
			}
		}, delay, TimeUnit.MILLISECONDS);
	}
	
	
	private boolean waitingForHandler() throws InterruptedException{
	    lock.lock();
	    try{
	        return connected.await(this.connectTimeoutMillis, TimeUnit.MILLISECONDS);
	    }finally{
	        lock.unlock();
	    }
	}
	
	private void signalAvailableHandler() {
        lock.lock();
        try{
            connected.signalAll();
        }finally{
            lock.unlock();
        }
    }
	
	DefaultClientHandler chooseHandler(){
		
		CopyOnWriteArrayList<DefaultClientHandler> handlers = (CopyOnWriteArrayList<DefaultClientHandler>) this.connectedHandlers.clone();
		int size = handlers.size();
		if(size <= 0){
		    try {
		        boolean available = waitingForHandler();
		        if(available){
		            handlers = (CopyOnWriteArrayList<DefaultClientHandler>) this.connectedHandlers.clone();
		            size = handlers.size();
		        }
		       
                if(size <= 0){
                    throw new RuntimeException("Cann't connect any servers!");
                }
                
            } catch (InterruptedException e) {
                logger.error("chooseHandler|msg=" + e.getMessage(), e);
                throw new RuntimeException("Cann't connect any servers!", e);
            }
		}
		int index = (roundRobin.getAndAdd(1) + size)%size;
		return handlers.get(index);
	}

	RPCContext createRequest(String funcName, Object[] args, long seqNum, byte type) {
		try{
			Request req = new Request();
			req.setSeqNum(seqNum);
			req.setObjName(clazz.getName());
			req.setFuncName(funcName);
			req.setArgs(args);
			   
			Class[] parameterTypes = new Class[args.length];
			for(int i=0; i<args.length;i++){
				parameterTypes[i] = args[i].getClass();
			}
		   
		    Method method = clazz.getMethod(funcName, parameterTypes);
		    if( method.getReturnType().equals(Void.TYPE) && Constants.RPCType.oneway == type){
			   req.setType(Constants.RPCType.oneway);
		    }else if( method.getReturnType().equals(Void.TYPE) && Constants.RPCType.normal == type){
		    	req.setType(Constants.RPCType.oneway);
		    }else if( method.getReturnType().equals(Void.TYPE) && Constants.RPCType.async == type){
		    	new RuntimeException("this method will not return, please use notify() to call this method.");
		    }else{
		    	req.setType(type);
		    }
		    
		    RPCContext rpcCtx = new RPCContext();
			rpcCtx.setRequest(req);
		    return rpcCtx ;
		}catch (Exception e) {
			throw new RuntimeException("BaseObjectProxy.createRequest got exception|",e);
		}
	}
	
}
