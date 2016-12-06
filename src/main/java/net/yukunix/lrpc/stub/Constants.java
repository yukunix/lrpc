package net.yukunix.lrpc.stub;

public class Constants {
	
	public interface RPCStatus {
		char ok = 0;
		char exception = 1;
		char unknownError = 2;
	}
	
	public interface RPCType {
	    byte normal = 0;
	    byte oneway = 1;
	    byte async = 2;
	}
	
	
	
}
