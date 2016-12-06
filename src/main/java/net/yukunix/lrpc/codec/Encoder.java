package net.yukunix.lrpc.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.yukunix.lrpc.stub.RPCContext;


public class Encoder extends ChannelOutboundHandlerAdapter {

    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    	
    	if(msg instanceof RPCContext){
    		RPCContext rpcContext = (RPCContext)msg;
    		Object objToEncode;
    		if(rpcContext.getResponse() !=null){
    			objToEncode = rpcContext.getResponse();
    		}else{
    			objToEncode = rpcContext.getRequest();
    		}
    		
    		byte[] bytes = KryoSerializer.write(objToEncode);
    		
    		ByteBuf byteBuf = ctx.alloc().buffer(4 + bytes.length);
    		//header
    		byteBuf.writeInt(bytes.length);
    		//body
    		byteBuf.writeBytes(bytes);
    		ctx.writeAndFlush(byteBuf, promise); 
    	}
    }
}
