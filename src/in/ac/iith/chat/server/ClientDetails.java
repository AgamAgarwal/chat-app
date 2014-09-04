package in.ac.iith.chat.server;

import java.net.InetAddress;
import java.util.Calendar;

public class ClientDetails {
	String nickname;
	InetAddress ipAddress;
	int port;
	long lastHeartbeat;
	public ClientDetails(String nn, InetAddress ip, int p) {
		nickname=nn;
		ipAddress=ip;
		port=p;
		updateHeartbeat();
	}
	
	public InetAddress getIP() {
		return ipAddress;
	}
	
	public void updateHeartbeat() {
		lastHeartbeat=System.currentTimeMillis();
	}
	
	@Override
	public String toString() {
		return nickname+"@"+ipAddress+":"+port;
	}
}
