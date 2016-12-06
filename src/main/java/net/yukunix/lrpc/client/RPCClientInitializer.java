
package net.yukunix.lrpc.client;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import net.yukunix.lrpc.client.proxy.BaseObjectProxy;
import net.yukunix.lrpc.codec.Decoder;
import net.yukunix.lrpc.codec.Encoder;

public class RPCClientInitializer extends ChannelInitializer<SocketChannel> {

	private BaseObjectProxy objProxy;
	ClientProperties props;
	
	public  RPCClientInitializer(BaseObjectProxy objProxy, ClientProperties props){
		super();
		this.objProxy = objProxy;
		this.props = props;
	}
	
    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();
        p.addLast("decoder", new Decoder());

        p.addLast("encoder", new Encoder());
 
        p.addLast("handler", new DefaultClientHandler(props, objProxy));
    }
}
