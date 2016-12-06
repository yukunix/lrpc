package net.yukunix.lrpc.client.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import net.yukunix.lrpc.client.ClientProperties;
import net.yukunix.lrpc.client.DefaultClientHandler;
import net.yukunix.lrpc.client.RPCFuture;
import net.yukunix.lrpc.stub.Constants;
import net.yukunix.lrpc.stub.RPCContext;

public class ObjectProxy<T> extends BaseObjectProxy<T> implements InvocationHandler,IAsyncObjectProxy {

	public ObjectProxy(ClientProperties props, InetSocketAddress server, Class<T> clazz) {
		super(props, server, clazz);
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		
		   if(Object.class  == method.getDeclaringClass()) {
			   
		       String name = method.getName();
		       if("equals".equals(name)) {
		           return proxy == args[0];
		       } else if("hashCode".equals(name)) {
		           return System.identityHashCode(proxy);
		       } else if("toString".equals(name)) {
		           return proxy.getClass().getName() + "@" +
		               Integer.toHexString(System.identityHashCode(proxy)) +
		               ", with InvocationHandler " + this;
		       } else {
		           throw new IllegalStateException(String.valueOf(method));
		       }
		   }

		   DefaultClientHandler handler = chooseHandler();		
		   long seqNum = handler.getNextSequentNumber();
		   RPCContext rpcCtx = createRequest(method.getName(), args, seqNum, Constants.RPCType.normal);

		   RPCFuture rpcFuture = handler.doRPC(rpcCtx);
		   return rpcFuture.get(this.syncCallTimeOutMillis, TimeUnit.MILLISECONDS);
	}

	@Override
	public RPCFuture call(String funcName, Object... args){
		
		DefaultClientHandler handler = chooseHandler();
		long seqNum = handler.getNextSequentNumber();
		RPCContext rpcCtx = createRequest(funcName, args, seqNum, Constants.RPCType.async);

		RPCFuture rpcFuture = handler.doRPC(rpcCtx);
		return rpcFuture;
	}
	
	@Override
	public void notify(String funcName, Object[] args) {
		
		DefaultClientHandler handler = chooseHandler();
		long seqNum = handler.getNextSequentNumber();
		RPCContext rpcCtx = createRequest(funcName, args, seqNum, Constants.RPCType.oneway);

		handler.doNotify(rpcCtx);
	}
	
}
