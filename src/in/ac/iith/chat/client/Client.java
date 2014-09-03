package in.ac.iith.chat.client;

import in.ac.iith.chat.common.Constants;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

public class Client {
	
	/**
	 * Client side socket
	 */
	DatagramSocket socket;
	
	/**
	 * Server's IP Address
	 */
	InetAddress serverIP;
	
	/**
	 * Nickname of the client
	 */
	String nickname;
	
	/**
	 * Timer to send heartbeat at regular intervals
	 */
	Timer heartbeatTimer;
	
	/**
	 * Initializes the client with the given nickname
	 * @param nn nickname
	 */
	public Client(String nn) {
		nickname=nn;
		try {
			socket=new DatagramSocket();	//try to bind the port. Note this is required as it will be needed for if any other client wants to send a message
		} catch (SocketException e) {
			System.err.println("Error binding any port");
			e.printStackTrace();
		}
		try {
			serverIP=InetAddress.getByName("localhost");	//currently using localhost, will have to change later
		} catch (UnknownHostException e) {
			System.out.println("Unknown server");
			e.printStackTrace();
		}
		heartbeatTimer=new Timer();	//initialize Timer object
	}
	
	/**
	 * Starts all the threads for the client
	 */
	public void start() {
		heartbeatTimer.scheduleAtFixedRate(new HeartbeatTask(), 0, Constants.Client.HEARTBEAT_RATE);	//schedule heartbeat at regular intervals
	}
	
	/**
	 * Class send a single heartbeat to the server
	 */
	private class HeartbeatTask extends TimerTask {
		
		@Override
		public void run() {
			byte[] data=nickname.getBytes();	//heartbeat data comprises of its nickname
			try {
				socket.send(new DatagramPacket(data, data.length, serverIP, Constants.Server.PORT));	//send the heartbeat
			} catch (IOException e) {
				System.err.println("Unable to send heartbeat");
				e.printStackTrace();
			}
		}
	}
}
