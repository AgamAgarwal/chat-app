package in.ac.iith.chat.client;

import in.ac.iith.chat.common.Constants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

public class Client {
	
	/**
	 * Client side sockets
	 */
	DatagramSocket serverSocket;
	
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
	 * Object to get input from user
	 */
	BufferedReader br;
	
	Thread serverReceiver;
	
	/**
	 * Will be set to true if the client is waiting for the list from the server.
	 * It is set as volatile because its value will be changed by different threads
	 */
	volatile boolean waitForList;
	
	/**
	 * Initializes the client with the given nickname
	 * @param nn nickname
	 */
	public Client(String nn) {
		nickname=nn;
		try {
			serverSocket=new DatagramSocket();	//try to bind the port. Note this is required as it will be needed for if any other client wants to send a message
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
		br=new BufferedReader(new InputStreamReader(System.in));
		heartbeatTimer=new Timer();	//initialize Timer object
	}
	
	/**
	 * Starts all the threads for the client
	 */
	public void start() {
		heartbeatTimer.scheduleAtFixedRate(new HeartbeatTask(), 0, Constants.Client.HEARTBEAT_RATE);	//schedule heartbeat at regular intervals
		serverReceiver=new Thread(new ServerReceiver());
		serverReceiver.start();
		promptLoop();
	}
	
	/**
	 * Loop for the terminal prompt
	 */
	private void promptLoop() {
		String command;
		while(true) {
			System.out.print("> ");
			try {
				command=br.readLine();
			} catch (IOException e) {
				System.err.println("Error reading command");
				continue;
			}
			if(command.equals(Constants.Client.LIST_COMMAND))
				requestForList();
			else if(command.equals(Constants.Client.CONNECT_COMMAND))
				;	//TODO: connect to given client
		}
	}
	
	/**
	 * Sends a request to the server to get the list of currently online clients
	 */
	private void requestForList() {
		waitForList=true;
		sendThroughSocket(Constants.REQUEST_ID, serverSocket);
		while(waitForList);
	}
	
	public boolean sendThroughSocket(String msg, DatagramSocket socket) {
		byte[] data=msg.getBytes();
		try {
			socket.send(new DatagramPacket(data, data.length, serverIP, Constants.Server.PORT));	//send the heartbeat
		} catch (IOException e) {
			System.err.println("Unable to send message");
			return false;
		}
		return true;
	}
	
	/**
	 * Class send a single heartbeat to the server
	 */
	private class HeartbeatTask extends TimerTask {
		
		@Override
		public void run() {
			sendThroughSocket(Constants.HEARTBEAT_ID+" "+nickname, serverSocket);	//heartbeat data comprises of its nickname
		}
	}
	
	private class ServerReceiver implements Runnable {

		@Override
		public void run() {
			while(true) {
				byte[] data=new byte[1024*1024];
				DatagramPacket packet=new DatagramPacket(data, data.length);
				try {
					serverSocket.receive(packet);
				} catch(IOException e) {
					System.err.println("Error while receiving message from server");
					continue;
				}
				System.out.print("Message from server:\n"+new String(data));
				waitForList=false;
			}
		}
		
	}
}
