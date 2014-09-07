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
import java.util.LinkedList;
import java.util.Queue;
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
	
	Thread serverReceiver, clientReceiver, msgQueueManager;
	
	HashMap<String, ClientDetails> otherClients;
	
	/**
	 * Will be set to true if the client is waiting for the list from the server.
	 * It is set as volatile because its value will be changed by different threads
	 */
	volatile boolean waitForList;
	
	/**
	 * Identifies if there is currently an active chat session or not
	 */
	volatile boolean currentlyChatting;
	
	volatile boolean terminalLock;
	
	volatile boolean pendingChatRequest, pendingChatAccept;
	
	volatile String pendingChatRequestPartner;
	
	volatile ClientDetails currentChatPartner, pendingAcceptClient;
	
	Queue<String> msgQueue;
	
	/**
	 * Initializes the client with the given nickname
	 * @param nn nickname
	 */
	public Client(String nn) {
		if(!nn.matches("[A-Za-z0-9]+"))
			throw new IllegalArgumentException("Invalid nickname. A nickname can only contain letters and numbers.");
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
		currentlyChatting=false;	//initially not chatting with anyone
		currentChatPartner=null;
		msgQueue=new LinkedList<String>();
	}
	
	/**
	 * Starts all the threads for the client
	 */
	public void start() {
		//heartbeatTimer.scheduleAtFixedRate(new HeartbeatTask(), 0, Constants.Client.HEARTBEAT_RATE);	//schedule heartbeat at regular intervals
		serverReceiver=new Thread(new ServerReceiver());
		serverReceiver.start();
		clientReceiver=new Thread(new ClientReceiver());
		clientReceiver.start();
		msgQueueManager=new Thread(new MsgQueueManager());
		msgQueueManager.start();
		//promptLoop();

		// changed
		sendThroughSocket(Constants.HEARTBEAT_ID+" "+clientSocket.getLocalPort()+" "+nickname, serverSocket, serverIP, Constants.Server.PORT);

	}
	
	/**
	 * Loop for the terminal prompt
	 */
	private void promptLoop() {
		String command;
		while(true) {
			try {
				command=br.readLine();
			} catch (IOException e) {
				System.err.println("Error reading command");
				if(pendingChatAccept)
					sendThroughSocket(Constants.Client.CHAT_DENY+" "+nickname, clientSocket, pendingAcceptClient.getIP(), pendingAcceptClient.getPort());	//deny the request
				continue;
			}
			
			if(pendingChatAccept) {
				char c=command.charAt(0);
				if(c=='y') {
					System.out.println("Accepting request");
					sendThroughSocket(Constants.Client.CHAT_ACCEPT+" "+nickname, clientSocket, pendingAcceptClient.getIP(), pendingAcceptClient.getPort());	//accept the request
					currentChatPartner=pendingAcceptClient;
					currentlyChatting=true;
					pendingChatAccept=false;
					msgQueue.clear();	//clear message queue
				} else {
					sendThroughSocket(Constants.Client.CHAT_DENY+" "+nickname, clientSocket, pendingAcceptClient.getIP(), pendingAcceptClient.getPort());	//deny the request
				}
			} else if(command.equals(Constants.Client.LIST_COMMAND))
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
			} else if(command.startsWith(Constants.Client.MESSAGE_COMMAND)) {
				if(!isCurrentlyChatting()) {	//if not chatting with anyone currently
					System.out.println("Not chatting with anyone currently. Please connect to someone first.");
					continue;
				}
				int spaceIndex=command.indexOf(' ');
				String msg;
				if(spaceIndex==-1) {	//if name is not given with the command 
					lockTerminal();
					System.out.println("Type in the message you want to send:");
					try {
						msg=br.readLine();
					} catch (IOException e) {
						releaseTerminal();
						continue;
					}
					releaseTerminal();
				} else
					msg=command.substring(spaceIndex+1);
				sendMessageToChatPartner(msg);
			}
		}
	}
	
	private void lockTerminal() {
		terminalLock=true;
	}
	
	private void releaseTerminal() {
		terminalLock=false;
	}
	
	private boolean terminalIsLocked() {
		return terminalLock;
	}
	
	private void connectToClient(String name) {
		ClientDetails cd=otherClients.get(name);
		if(cd==null)
			return;
		sendThroughSocket(Constants.Client.CHAT_REQUEST+" "+nickname, clientSocket, cd.getIP(), cd.getPort());	//send chat request
		pendingChatRequestPartner=name;
		pendingChatRequest=true;
		//TODO start timer for request timeout
	}
	
	private void sendMessageToChatPartner(String msg) {
		if(currentChatPartner==null)
			return;
		sendThroughSocket(Constants.Client.MESSAGE_COMMAND+" "+nickname+" "+msg, clientSocket, currentChatPartner.getIP(), currentChatPartner.getPort());	//send message
	}
	
	public boolean isCurrentlyChatting() {
		return currentlyChatting;
	}
	
	/**
	 * Sends a request to the server to get the list of currently online clients
	 */
	private void requestForList() {
		waitForList=true;
		sendThroughSocket(Constants.REQUEST_ID, serverSocket, serverIP, Constants.Server.PORT);
		while(waitForList);
	}
	
	public boolean sendThroughSocket(String msg, DatagramSocket socket, InetAddress ip, int port) {
		byte[] data=msg.getBytes();
		try {
			socket.send(new DatagramPacket(data, data.length, ip, port));	//send the heartbeat
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
			sendThroughSocket(Constants.HEARTBEAT_ID+" "+clientSocket.getLocalPort()+" "+nickname, serverSocket, serverIP, Constants.Server.PORT);	//heartbeat data comprises of its nickname
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
					waitForList=false;
				}
				else if(reply[0].equals(Constants.DUPLICATE_NICKNAME)){
					System.out.println(reply[1]);

					String nn = "$";
					while(true)
					{
						System.out.print("Enter another nick name: ");
						try{
							nn = br.readLine();
						} catch(Exception e) {
							System.out.println("Error reading input from the console.");
						}
						if(!nn.matches("[A-Za-z0-9]+"))
							throw new IllegalArgumentException("Invalid nickname. A nickname can only contain letters and numbers.");
						else
						{
							nickname = nn;
							break;
						}
					}

					sendThroughSocket(Constants.HEARTBEAT_ID+" "+clientSocket.getLocalPort()+" "+nickname, serverSocket, serverIP, Constants.Server.PORT);
					
					//heartbeatTimer = new Timer();
					//heartbeatTimer.scheduleAtFixedRate(new HeartbeatTask(), 0, Constants.Client.HEARTBEAT_RATE);
				}
				else if(reply[0].trim().equals(Constants.ACCEPTED_NICKNAME)){
					System.out.println("Nickname Accepted.");
					heartbeatTimer.scheduleAtFixedRate(new HeartbeatTask(), 0, Constants.Client.HEARTBEAT_RATE);
					promptLoop();
				}
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
					clientSocket.receive(packet);
				} catch(IOException e) {
					System.err.println("Error while receiving message from server");
					continue;
				}
				String reply=(new String(data)).trim();
				if(reply.startsWith(Constants.Client.CHAT_REQUEST)) {
					if(isCurrentlyChatting()) {	//if already chatting with someone right now
						sendThroughSocket(Constants.Client.CHAT_DENY+" "+nickname, clientSocket, packet.getAddress(), packet.getPort());	//deny the request
					} else {
						String[] parts=reply.split(" ", 2);
						if(parts.length<2) {	//if name is not present
							sendThroughSocket(Constants.Client.CHAT_DENY+" "+nickname, clientSocket, packet.getAddress(), packet.getPort());	//deny the request
							continue;
						}
						System.out.println("User '"+parts[1]+"' wants to chat with you. Accept(y/n)?");
						pendingAcceptClient=new ClientDetails(parts[1], packet.getAddress(), packet.getPort());
						pendingChatAccept=true;
					}
				} else if(reply.startsWith(Constants.Client.CHAT_ACCEPT)) {
					if(!pendingChatRequest)	//ignore if no pending chat request
						continue;
					String[] parts=reply.split(" ", 2);
					if(!parts[1].trim().equals(pendingChatRequestPartner))
						continue;
					ClientDetails cd=otherClients.get(pendingChatRequestPartner);
					currentChatPartner=new ClientDetails(pendingChatRequestPartner, cd.getIP(), cd.getPort());
					System.out.println("Request accepted by "+pendingChatRequestPartner);
					msgQueue.clear();	//clear message queue
					currentlyChatting=true;
					pendingChatRequest=false;
				} else if(reply.startsWith(Constants.Client.CHAT_DENY)) {
					if(!pendingChatRequest)	//ignore if no pending chat request
						continue;
					String[] parts=reply.split(" ", 2);
					if(!parts[1].trim().equals(pendingChatRequestPartner))
						continue;
					System.out.println("Request denied by "+pendingChatRequestPartner);
					pendingChatRequest=false;
				} else if(reply.startsWith(Constants.Client.MESSAGE_COMMAND)) {
					if(!isCurrentlyChatting())
						continue;	//discard if not currently chatting with anyone
					String[] parts=reply.split(" ", 3);
					if(parts.length<3)	//if invalid
						continue;
					if(!parts[1].trim().equals(currentChatPartner.getName()))	//if wrong sender
						continue;
					msgQueue.add(parts[2].trim());
				}
			}
		}
	}
	
	private class MsgQueueManager implements Runnable {

		@Override
		public void run() {
			while(true) {
				while(terminalIsLocked() || msgQueue.isEmpty());	//wait till terminal lock is released or any msg arrives
				String msg=msgQueue.poll();
				
				/*
				 * Checking if msg is null.
				 * This case may happen due to many threads using the same queue.
				 * Note that, in the currently implementation, only this thread is
				 * polling from the queue, so such an error may not occur.
				 */
				if(msg==null)
					continue;
				
				System.out.println(currentChatPartner.getName()+" says: "+msg);
			}
		}
		
	}
}
