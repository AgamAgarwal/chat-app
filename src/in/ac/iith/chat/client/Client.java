package in.ac.iith.chat.client;

import in.ac.iith.chat.common.ClientDetails;
import in.ac.iith.chat.common.Constants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class Client {
	
	/**
	 * Client side sockets
	 */
	DatagramSocket serverSocket, clientSocket;
	
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
	
	Thread serverReceiver, clientReceiver;
	
	HashMap<String, ClientDetails> otherClients;
	
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
			clientSocket=new DatagramSocket();
		} catch (SocketException e) {
			System.err.println("Error binding any port");
			System.exit(-1);
		}
		try {
			serverIP=InetAddress.getByName("localhost");	//currently using localhost, will have to change later
		} catch (UnknownHostException e) {
			System.out.println("Unknown server");
			System.exit(-1);
		}
		br=new BufferedReader(new InputStreamReader(System.in));
		otherClients=null;
		heartbeatTimer=new Timer();	//initialize Timer object
	}
	
	/**
	 * Starts all the threads for the client
	 */
	public void start() {
		heartbeatTimer.scheduleAtFixedRate(new HeartbeatTask(), 0, Constants.Client.HEARTBEAT_RATE);	//schedule heartbeat at regular intervals
		serverReceiver=new Thread(new ServerReceiver());
		serverReceiver.start();
		clientReceiver=new Thread(new ClientReceiver());
		clientReceiver.start();
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
			else if(command.startsWith(Constants.Client.CONNECT_COMMAND)) {
				int spaceIndex=command.indexOf(' ');
				String name;
				if(spaceIndex==-1) {	//if name is not given with the command 
					System.out.print("Whom do you want to chat with? ");
					try {
						name=br.readLine();
					} catch (IOException e) {
						continue;
					}
				} else
					name=command.substring(spaceIndex+1);
				
				//checking if the name exists in the client list
				if(otherClients.containsKey(name))
					connectToClient(name);
				else
					System.err.println("Invalid client name, try again!!!");
			}
		}
	}
	
	private void connectToClient(String name) {
		System.out.println("Connecting to "+name);
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
			sendThroughSocket(Constants.HEARTBEAT_ID+" "+clientSocket.getLocalPort()+" "+nickname, serverSocket);	//heartbeat data comprises of its nickname
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
				String[] reply=new String(data).split("\n");
				if(reply[0].equals(Constants.Server.LIST_HEADER)) {
					otherClients=new HashMap<String, ClientDetails>();
					for(int i=1;i<reply.length;i++) {
						String[] line=reply[i].split(":");
						if(line.length<3) continue;
						String name=line[0].trim();
						if(name.equals(nickname)) continue;	//skip self
						InetAddress ip;
						try {
							ip=InetAddress.getByName(line[1].trim());
						} catch (UnknownHostException e) {
							continue;
						}
						int port=Integer.parseInt(line[2].trim());
						System.out.println(name);
						otherClients.put(name, new ClientDetails(name, ip, port));
					}
				}
				waitForList=false;
			}
		}
	}
	
	private class ClientReceiver implements Runnable {

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
				String reply=(new String(data)).trim();
				if(reply.startsWith(Constants.Client.CHAT_REQUEST)) {
					//TODO: Check if user is not chatting with someone currently, then directly accept request or ask user for confirmation
				}
			}
		}
		
	}
}
