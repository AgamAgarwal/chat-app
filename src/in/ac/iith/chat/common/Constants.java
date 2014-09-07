package in.ac.iith.chat.common;

public final class Constants {
	public final class Server {
		public final static int PORT=12340;
		public final static String LIST_HEADER="list";
		public final static long CLIENT_EXPIRE_TIME=5000;	//5000 milliseconds
		public final static long CLIENT_CHECK_RATE=1000;	//1000 milliseconds
	}
	public final class Client {
		public final static long HEARTBEAT_RATE=1000;	//1000 milliseconds
		public final static long CHAT_REQUEST_TIMEOUT=10000;
		public final static long LIST_REQUEST_TIMER=1000;
		
		public final static String LIST_COMMAND="l";
		public final static String CONNECT_COMMAND="c";
		public final static String DISCONNECT_COMMAND="bye";
		public final static String MESSAGE_COMMAND="m";
		public final static String HELP_COMMAND="help";
		
		public final static String CHAT_REQUEST="crq";
		public final static String CHAT_ACCEPT="acc";
		public final static String CHAT_DENY="den";
		public final static String CHAT_DISCONNECT="fin";
		public final static String CHAT_TYPING="typ";
		
		public final static String HELP_TEXT="\nType '"+LIST_COMMAND+"' to get the list of online clients.\n" +
				"Type '"+CONNECT_COMMAND+" <client_name>' to connect to a client.\n" +
				"Type '"+MESSAGE_COMMAND+"' to send a message to the connected client.\n" +
				"Type '"+DISCONNECT_COMMAND+"' to disconnect from the connected client.\n" +
				"Type '"+HELP_COMMAND+"' to display this message.\n";
	}
	public final static String HEARTBEAT_ID="hb";
	public final static String REQUEST_ID="rq";
	
	public final static String DUPLICATE_NICKNAME = "Duplicate";
	public final static String ACCEPTED_NICKNAME = "Nickname accepted";
}
