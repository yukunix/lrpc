package net.yukunix.lrpc.example.obj;


public class HelloWorldObj implements IHelloWordObj {

	@Override
	public String hello(String msg) {
//		System.out.print("msg"+msg);
		return msg;
	}

	@Override
	public String test(int i, String s, long l) {
		System.out.print("test:"+i+s+l);
		return i+s+l;
	}
	
	@Override
	public String testB(Integer i, String s, Long l) {
		System.out.print("testB:"+i+s+l+1);
		return i+s+l+1;
	}

	@Override
	public Msg testMst(HellMsg helloMsg) {
		Msg _msg = new Msg();
		_msg.setI(helloMsg.getI()+helloMsg.getMsg().getI());
		_msg.setL(helloMsg.getL()+helloMsg.getMsg().getL());
		_msg.setS(helloMsg.getS()+helloMsg.getMsg().getS());
		return _msg;
	}
}
