package net.yukunix.lrpc.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import net.yukunix.lrpc.codec.Decoder;
import net.yukunix.lrpc.codec.Encoder;

public class DefaultServerInitializer extends ChannelInitializer<SocketChannel> {

	ServerProperties props;

	public DefaultServerInitializer(ServerProperties props) {
		this.props = props;
	}

	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		// Create a default pipeline implementation
		final ChannelPipeline p = ch.pipeline();

		p.addLast("decoder", new Decoder());

		p.addLast("encoder", new Encoder());

		p.addLast("handler", new DefaultHandler(props));
		
		p.addLast("httpExceptionHandler", new DefaultExceptionHandler());
	}
}
