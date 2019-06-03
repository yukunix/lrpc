package net.yukunix.lrpc.example.client;

import java.util.concurrent.atomic.AtomicLong;

import net.yukunix.lrpc.client.ClientProperties;
import net.yukunix.lrpc.client.RPCClient;
import net.yukunix.lrpc.example.obj.IHelloWordObj;
import net.yukunix.lrpc.example.obj.IHelloWordObj.HellMsg;
import net.yukunix.lrpc.example.obj.IHelloWordObj.Msg;

public class HelloWorldClient {
	

    public static void main(String[] args) throws Exception {
    	final ClientProperties props = new ClientProperties() {
			
			@Override
			public long syncCallTimeOut() {
				return 300;
			}
			
			@Override
			public long reconnInterval() {
				return 1000;
			}
			
			@Override
			public int ioThreadNum() {
				return 1;
			}
			
			@Override
			public long connectTimeout() {
				return 300;
			}
			
			@Override
			public int asyncThreadPoolSize() {
				return 4;
			}

			@Override
			public String host() {
				return "localhost";
			}

			@Override
			public int port() {
				return 9090;
			}
		};
    	
    	 final String host ="127.0.0.1";
    	 final int port = 9090;

         final AtomicLong totalTimeCosted = new AtomicLong(0);
         int threadNum = 1;
         final int requestNum = 100_000;
         Thread[] threads = new Thread[threadNum];
         
         for(int i =0;i< threadNum;i++){	
        	 threads[i] = new Thread(new Runnable(){
			 @Override
			 public void run() {
				 
					IHelloWordObj client = RPCClient.proxyBuilder(IHelloWordObj.class).withServerNode(host, port).build(props);
					long start = System.currentTimeMillis();
					for (int i = 0; i < requestNum; i++) {
						String result = client.hello("hello world!" + i);
						if (!result.equals("hello world!" + i))
							System.out.print("error=" + result);
					}
					totalTimeCosted.addAndGet(System.currentTimeMillis() - start);
				}
        	 });
        	 threads[i].start();
         }
         
         for(int i=0; i<threads.length;i++)
        	 threads[i].join();

		System.out.println("total time costed:" + totalTimeCosted.get()	+ "|req/s=" + requestNum * threadNum / (double) (totalTimeCosted.get() / 1000));

		IHelloWordObj client = RPCClient.proxyBuilder(IHelloWordObj.class).withServerNode(host, port).build(props);

		HellMsg msg =new HellMsg();
		msg.setI(1);
		msg.setL(2L);
		msg.setS("hello1");
		msg.setMsg(new Msg());
		msg.getMsg().setI(2);
		msg.getMsg().setL(3L);
		msg.getMsg().setS("hello2");
		
		System.out.println("test server list:" + client.testMst(msg));
		Msg res = client.testMst(msg);
		if(!(res.getI() == msg.getI() + msg.getMsg().getI()) 
			|| !(res.getL() == msg.getL() + msg.getMsg().getL())
			||!(res.getS() .equals(msg.getS() + msg.getMsg().getS())))
		{
			System.out.println("tesg Msg got error!");
		}
		
		String ret = client.test(1, "2", 3);
		System.out.println("test: " + ret);
		ret = client.testB(new Integer(1), "2", new Long(3));
		System.out.println("testB: " + ret);

		RPCClient.getInstance(props).getEventLoopGroup().shutdownGracefully();
    }
}
