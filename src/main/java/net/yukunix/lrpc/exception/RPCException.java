package net.yukunix.lrpc.exception;

import net.yukunix.lrpc.stub.RPCContext;

public class RPCException extends Exception {

	private static final long serialVersionUID = 8166629097983704842L;
	
	public RPCContext context;

	public RPCException(RPCContext context, Throwable cause) {
		super(cause);
		this.context = context;
	}
}
