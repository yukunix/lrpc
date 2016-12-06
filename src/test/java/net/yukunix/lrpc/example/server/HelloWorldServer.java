package net.yukunix.lrpc.example.server;

import java.util.Arrays;
import java.util.List;

import net.yukunix.lrpc.example.obj.HelloWorldObj;
import net.yukunix.lrpc.server.RPCServer;
import net.yukunix.lrpc.server.ServerProperties;

public class HelloWorldServer {

	public static void main(String[] args) throws Exception {
		final ServerProperties props = new ServerProperties() {
			
			@Override
			public int port() {
				return 9090;
			}
			
			@Override
			public List<String> objClassList() {
				return Arrays.asList(HelloWorldObj.class.getName());
			}
			
			@Override
			public int ioThreadNum() {
				return 1;
			}
			
			@Override
			public boolean async() {
				return false;
			}
		};
		new RPCServer(props).run();
	}
}
