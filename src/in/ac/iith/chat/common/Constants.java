package in.ac.iith.chat.common;

public final class Constants {
	public final class Server {
		public final static int PORT=12340;
		public final static String LIST_HEADER="list";
	}
	public final class Client {
		public final static int HEARTBEAT_RATE=1000;	//1000 milliseconds
		public final static String LIST_COMMAND="l";
		public final static String CONNECT_COMMAND="c";
		public final static String DISCONNECT_COMMAND="bye";
		public final static String MESSAGE_COMMAND="m";
		
		public final static String CHAT_REQUEST="crq";
	}
	public final static String HEARTBEAT_ID="hb";
	public final static String REQUEST_ID="rq";
}
