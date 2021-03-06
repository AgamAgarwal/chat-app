package in.ac.iith.chat.common;

import java.net.InetAddress;

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
	
	public String getName() {
		return nickname;
	}
	
	public InetAddress getIP() {
		return ipAddress;
	}
	
	public int getPort() {
		return port;
	}
	
	public void updateHeartbeat() {
		lastHeartbeat=System.currentTimeMillis();
	}
	
	public long getLastHeartbeat() {
		return lastHeartbeat;
	}
	
	@Override
	public String toString() {
		return nickname+":"+ipAddress.getHostAddress()+":"+port;
	}
}

