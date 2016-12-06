package net.yukunix.lrpc.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.yukunix.lrpc.exception.RPCException;
import net.yukunix.lrpc.stub.Constants;
import net.yukunix.lrpc.stub.Request;
import net.yukunix.lrpc.stub.Response;

public class DefaultExceptionHandler extends ChannelInboundHandlerAdapter {

	private final Logger logger = LogManager.getLogger(DefaultExceptionHandler.class);

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		
		logger.error("Exception caught", cause);

		if(cause instanceof RPCException){	//application layer exception.
			RPCException exc = (RPCException)cause;
			Request req = exc.context.getRequest();
			Response res= new Response();
			//copy properties
			res.setSeqNum(req.getSeqNum());
			res.setVersion(req.getVersion());
			res.setType(req.getType());
			res.setObjName(req.getObjName());
			res.setFuncName(req.getFuncName());
			
			//pass exception message to client
			res.setStatus(Constants.RPCStatus.exception);
			res.setMsg(exc.getMessage());
			
			ctx.writeAndFlush(res);
		}else{	// unknow error
			ctx.close();
		}
	}
}
