package net.yukunix.lrpc.server;

import java.lang.reflect.Method;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.yukunix.lrpc.stub.Constants;
import net.yukunix.lrpc.stub.RPCContext;
import net.yukunix.lrpc.stub.Request;
import net.yukunix.lrpc.stub.Response;



public class DefaultHandler extends SimpleChannelInboundHandler<RPCContext> {

	private final Logger logger = LogManager.getLogger(DefaultHandler.class);
	
	ServerProperties props;
	
	public DefaultHandler(ServerProperties props) {
		super(false);
		this.props = props;
	}

	@Override
	protected void channelRead0(final ChannelHandlerContext ctx, final RPCContext rpcContext) throws Exception {
		if(props.async()){
			RPCServer.submit(new Runnable(){
				@Override
				public void run() {
					processRequest(ctx,rpcContext);
				}
			});
		}else{
			processRequest(ctx,rpcContext);
		}
	}	

	public void processRequest(ChannelHandlerContext ctx, RPCContext rpcContext){
		Request req = rpcContext.getRequest();
		Response res= new Response();
		
		//copy properties
		res.setSeqNum(req.getSeqNum());
		res.setVersion(req.getVersion());
		res.setType(req.getType());
		res.setObjName(req.getObjName());
		res.setFuncName(req.getFuncName());

		try{
		    
			Object[] args = req.getArgs();
			Class[] argTypes = new Class[args.length];
			String methodKey ="";
			for(int i=0;i<args.length;i++){
				argTypes[i] = args[i].getClass();
				methodKey+=argTypes[i].getSimpleName();
			}
	
			Object obj= RPCServer.getObject(req.getObjName());
			Class clazz= obj.getClass();
			Method func = clazz.getMethod(req.getFuncName(), argTypes);
			Object result= func.invoke(obj, req.getArgs());
			
			if(req.getType() != Constants.RPCType.oneway){
				res.setResult(result);
				res.setStatus(Constants.RPCStatus.ok);
				res.setMsg("ok");

				rpcContext.setResponse(res);
				ctx.writeAndFlush(rpcContext);
			}
			
		} catch (Exception e) {
			
			//pass exception message to client
			if(req.getType() != Constants.RPCType.oneway){
				res.setStatus(Constants.RPCStatus.exception);
				res.setMsg("excepton="+e.getClass().getSimpleName()+"|msg="+e.getMessage());
				
				rpcContext.setResponse(res);
				ctx.writeAndFlush(rpcContext);
			}
			
			logger.error("DefaultHandler|processRequest got error",e);
		}
		
	}
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		// TODO(adolgarev): cancel submitted tasks,
		// that works only for not in progress tasks
		// if (future != null && !future.isDone()) {
		// future.cancel(true);
		// }
	}

}
