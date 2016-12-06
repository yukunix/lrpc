package net.yukunix.lrpc.codec;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import net.yukunix.lrpc.stub.RPCContext;
import net.yukunix.lrpc.stub.Request;
import net.yukunix.lrpc.stub.Response;

public class Decoder extends ByteToMessageDecoder  {
	
	@Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) { 
    	
    	int packetLength = 0;
    	int bodyLength = 0;
    	int readerIndex = in.readerIndex();
    	
    	int intLength = 4;
    	if (in.readableBytes() >= intLength) {
    		bodyLength = in.getInt(readerIndex);
    		packetLength = intLength + bodyLength ;
    		
    		if (bodyLength > 0 && in.readableBytes() >= packetLength) {
				ByteBuf bodyByteBuf = in.slice(readerIndex + intLength, bodyLength);
				Object bodyObj =  KryoSerializer.read(bodyByteBuf);  
				
				RPCContext context = new RPCContext();
				if(bodyObj instanceof Request){
					context.setRequest((Request) bodyObj);
				}else if(bodyObj instanceof Response){
					context.setResponse( (Response) bodyObj);
				}else{//decoder got error
					ctx.close();
				}
				
				out.add(context);
				in.skipBytes(packetLength);
				
				return; // continue to next netty handler
    		}
    	}
    	
    	ctx.close(); // stop netty processing if it didn't get any message body.
    }


}
