package net.yukunix.lrpc.server;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import net.yukunix.lrpc.client.proxy.BaseObjectProxy;

public class RPCServer {
    
    private final Logger logger = LogManager.getLogger(RPCServer.class);
	
	private static HashMap<String,Object> objects =new HashMap<String,Object>();

    private int port;
    private int backlog = 1000;
    private int ioThreadNum;
    
	public static Object getObject(String objName){
		return objects.get(objName);
	}

	private static ExecutorService threadPool;

	public static void submit(Runnable task){
		if(threadPool == null){
			synchronized (BaseObjectProxy.class) {
				if(threadPool==null){
					LinkedBlockingDeque<Runnable> linkedBlockingDeque = new LinkedBlockingDeque<Runnable>();
					threadPool = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 600L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
//					threadPool = new BetterExecutorService(linkedBlockingDeque, executor,"Client async thread pool",RPCServer.getConfig().getInt("server.asyncThreadPoolSize"));
				}
			}
		}
		
		threadPool.submit(task);
	}

	ServerProperties props;
	
	public RPCServer(ServerProperties props) throws Exception {
		this.props = props;
	    this.port = props.port();
	    this.ioThreadNum = props.ioThreadNum();
		List<String> objClassList = props.objClassList();
		logger.info("Object list:");
		
		for( String objClass : objClassList){
			Object obj = RPCServer.class.forName(objClass).newInstance();
			Class[] interfaces= obj.getClass().getInterfaces();
			
			for(int i =0;i<interfaces.length;i++){
				objects.put(interfaces[i].getName(), obj);
				logger.info("   " + interfaces[i].getName());
			}
		}
	}

	public void run() throws Exception {
		final EventLoopGroup bossGroup = new NioEventLoopGroup();
		final EventLoopGroup workerGroup = new NioEventLoopGroup(this.ioThreadNum);
		try {

			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
					.channel(NioServerSocketChannel.class)
					.childHandler(new DefaultServerInitializer(props))
					.option(ChannelOption.SO_BACKLOG, this.backlog)
					.option(ChannelOption.SO_REUSEADDR, true)
					.option(ChannelOption.SO_KEEPALIVE, true);

			Channel ch = b.bind(port).sync().channel();
			
			logger.info("NettyRPC server listening on port "+ port + " and ready for connections...");
			
	         Runtime.getRuntime().addShutdownHook(new Thread(){
	                @Override
	                public void run(){
	                    bossGroup.shutdownGracefully();
	                    workerGroup.shutdownGracefully();
	                }
	            });
			ch.closeFuture().sync();

		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	public static void main(String[] args) throws Exception {
		new RPCServer(new ServerProperties() {
			
			@Override
			public int port() {
				// TODO Auto-generated method stub
				return 0;
			}
			
			@Override
			public List<String> objClassList() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public int ioThreadNum() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public boolean async() {
				// TODO Auto-generated method stub
				return false;
			}
		}).run();
	}
}
