package net.yukunix.lrpc.server;

import java.util.List;

public interface ServerProperties {

	int port();
    int ioThreadNum();
	List<String> objClassList();
	boolean async();
	
	
}
