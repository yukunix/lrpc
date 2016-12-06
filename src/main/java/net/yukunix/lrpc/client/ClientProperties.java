package net.yukunix.lrpc.client;

public interface ClientProperties {

	default String host() {
		return "127.0.0.1";
	};
	
	default int port() {
		return 9090;
	};
	
	long syncCallTimeOut();
    long connectTimeout();
    long reconnInterval();
    int ioThreadNum();
    int asyncThreadPoolSize();
    
}
