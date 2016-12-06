package net.yukunix.lrpc.example;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import net.yukunix.lrpc.client.ClientProperties;
import net.yukunix.lrpc.client.RPCClient;
import net.yukunix.lrpc.client.proxy.AsyncRPCCallback;
import net.yukunix.lrpc.client.proxy.IAsyncObjectProxy;
import net.yukunix.lrpc.example.obj.IHelloWordObj;

public class Benchmark {


    public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException {
    	final ClientProperties props = new ClientProperties() {
			
			@Override
			public long syncCallTimeOut() {
				// TODO Auto-generated method stub
				return 0;
			}
			
			@Override
			public long reconnInterval() {
				// TODO Auto-generated method stub
				return 0;
			}
			
			@Override
			public int ioThreadNum() {
				// TODO Auto-generated method stub
				return 0;
			}
			
			@Override
			public long connectTimeout() {
				// TODO Auto-generated method stub
				return 0;
			}
			
			@Override
			public int asyncThreadPoolSize() {
				// TODO Auto-generated method stub
				return 0;
			}
		};
    	
        final String host = "localhost";
        final int port = 9090;

        final IAsyncObjectProxy asyncClient = RPCClient.proxyBuilder(IHelloWordObj.class).buildAsyncObjPrx(props);
        final IHelloWordObj syncClient = RPCClient.proxyBuilder(IHelloWordObj.class).build(props);

        int threadNum = 100;
        final int requestNum = 1000;
        Thread[] threads = new Thread[threadNum];

        long startTime = System.currentTimeMillis();
        //benchmark for sync call
        final AtomicLong totalTimeCosted = new AtomicLong(0);
        for(int i =0;i< threadNum;i++){    
            threads[i] = new Thread(new Runnable(){
                @Override
                public void run() {
                    long start = System.currentTimeMillis();
                    for (int i = 0; i < requestNum; i++) {
                        String result = syncClient.hello("hello world!" + i);
                        if (!result.equals("hello world!" + i))
                            System.out.print("error=" + result);
                    }
                    totalTimeCosted.addAndGet(System.currentTimeMillis() - start);
                }
            });
            threads[i].start();
        }
        for(int i=0; i<threads.length;i++){
            threads[i].join();
        }
        
        long timeCosted = (System.currentTimeMillis() - startTime);
        System.out.println("sync call|total time costed:" + timeCosted + "|req/s=" + ((double)(requestNum * threadNum)) / timeCosted * 1000);


        //benchmark for async call
        final AsyncHelloWorldCallback callback = new AsyncHelloWorldCallback(requestNum*threadNum);
        for (int i = 0; i < threadNum; i++) {
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < requestNum; i++) {
                        asyncClient.call("hello",  "hello world!" + i ).addCallback(callback);
                    }
                }
            });
            threads[i].start();
        }
        for (int i = 0; i < threads.length; i++){
            threads[i].join();
        }


        synchronized (Benchmark.class) {
            Benchmark.class.wait();
        }

        System.out.print("shutdownGracefully");
        RPCClient.getInstance(props).shutdown();
    }


    public static class AsyncHelloWorldCallback implements AsyncRPCCallback {

        AtomicLong requestCount = new AtomicLong(0);
        private long requestNum;
        private long startTime;

        public AsyncHelloWorldCallback(long requestNum){
            this.requestNum = requestNum;
            this.startTime = System.currentTimeMillis();
        }

        @Override
        public void fail(Exception e) {
            System.out.println("fail"+e.getMessage());
        }

        @Override
        public void success(Object result) {
            if(requestNum ==requestCount.addAndGet(1)){
                long timeCosted= System.currentTimeMillis() - startTime;
                System.out.println("total time costed:" + timeCosted + "|total requests="+requestNum+"|req/s=" + requestNum  / (double) (timeCosted / 1000));
                synchronized (Benchmark.class) {
                    Benchmark.class.notify();
                }
            }
        }
    }

}
