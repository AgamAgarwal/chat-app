package in.ac.iith.chat.server;

import in.ac.iith.chat.common.ClientDetails;
import in.ac.iith.chat.common.Constants;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class Server {
	
	/**
	 * Server Socket object to receive heartbeats from clients
	 */
	DatagramSocket serverSocket;
	
	/**
	 * HashMap to store nickname to IP Address mapping
	 */
	HashMap<String, ClientDetails> onlineClients;
	
	/**
	 * A separate thread to receive all the heartbeats
	 */
	Thread heartbeatReceiver;
	
	Timer clientListCheckerTimer;
	
	/**
	 * Constructor to open the Datagram Socket and initialize the clients hashmap
	 */
	public Server() {
		
		try {
			serverSocket=new DatagramSocket(Constants.Server.PORT);	//try to bind the port to socket
		} catch (SocketException e) {
			System.err.println("Can't bind port "+Constants.Server.PORT);	//if bind fails
			e.printStackTrace();
		}
		onlineClients=new HashMap<String, ClientDetails>();	//initialize hashmap
		clientListCheckerTimer=new Timer();
	}
	
	/**
	 * Starts all the required threads of the server
	 */
	public void start() {
		System.out.println("The server is running now.");
		heartbeatReceiver=new Thread(new HeartbeatReceiver());
		heartbeatReceiver.start();
		clientListCheckerTimer.scheduleAtFixedRate(new ClientListChecker(), 0, Constants.Server.CLIENT_CHECK_RATE);
	}
	
	/**
	 * Takes in details of a client and its IP Address and updates in the table
	 * @param clientName
	 * @param clientIP
	 * @return false if the nickname is already taken by some other client
	 */
	public boolean updateClient(String clientName, InetAddress clientIP, int clientPort, int serverPort) {
		if(onlineClients.containsKey(clientName)) {
			ClientDetails cd=onlineClients.get(clientName);
			if(clientIP.equals(cd.getIP()) && clientPort==cd.getPort()) {	//if IP address matches
				cd.updateHeartbeat();
				//System.out.println(cd.toString());
				return true;
			} else {	//nickname already taken by some other client(checked using IP address)
				System.out.println("Duplicate nickname found.");
				return false;
			}
		} else {	//if new client
			onlineClients.put(clientName, new ClientDetails(clientName, clientIP, clientPort));
			System.out.println("Added new client:"+clientName);

			String reply = Constants.ACCEPTED_NICKNAME;
			byte data[] = reply.getBytes();
			try {
				serverSocket.send(new DatagramPacket(data, data.length, clientIP, serverPort));
				System.out.println("Data Sent.");
			} catch (IOException e) {
				System.err.println("Unable to send accepted message to client at "+clientIP.getHostAddress()+":"+clientPort);
			}


			return true;
		}
	}
	
	public void sendList(InetAddress clientIP, int clientPort) {
		StringBuilder sb=new StringBuilder();
		Iterator<ClientDetails> it=onlineClients.values().iterator();
		sb.append(Constants.Server.LIST_HEADER).append("\n");	//header
		while(it.hasNext())
			sb.append(it.next().toString()).append("\n");
		String reply=sb.toString();
		byte[] data=reply.getBytes();
		try {
			serverSocket.send(new DatagramPacket(data, data.length, clientIP, clientPort));
		} catch (IOException e) {
			System.err.println("Unable to send list to client at "+clientIP.getHostAddress()+":"+clientPort);
		}
	}
	
	/**
	 * Class to be run as a separate thread to receive all the heartbeats
	 */
	private class HeartbeatReceiver implements Runnable {
		
		@Override
		public void run() {
			while(true) {
				byte[] data=new byte[1024];
				DatagramPacket packet=new DatagramPacket(data, data.length);
				try {
					serverSocket.receive(packet);	//receive the next packet. Note that this is a blocking call
				} catch (IOException e) {
					System.err.println("Error while receiving packet.");
					continue;	//try to receive another packet
				}
				String[] dataString=(new String(data)).split(" ");
				try {
					if(dataString[0].startsWith(Constants.REQUEST_ID)) {
						sendList(packet.getAddress(), packet.getPort());
					} else if(dataString[0].startsWith(Constants.HEARTBEAT_ID)) {	//if it's a heartbeat

						/* Changes here. */
						boolean success = updateClient(dataString[2], packet.getAddress(), Integer.parseInt(dataString[1]), packet.getPort());	//update the time of last heartbeat of the client

						if(!success){	// in case of duplicate nickname.
							System.out.print("Sending error message to "+packet.getAddress());
							String reply = Constants.DUPLICATE_NICKNAME;
							reply += "\nThe nickname has already been taken. Try another nickname.";

							data=reply.getBytes();
							try {
								serverSocket.send(new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort()));
							} catch (IOException e) {
								System.err.println("Unable to send duplicate nickname error to "+packet.getAddress().getHostAddress()+":"+packet.getPort());
							}
						}

						/* Changes end */
					}
				} catch(Exception e) {
					System.err.println("Invalid request from client at "+packet.getAddress().getHostAddress());
				}
				//TODO: use return value of updateClient() to respond to the client if the nickname has already been taken
			}
		}
	}
	
	private class ClientListChecker extends TimerTask {
		@Override
		public void run() {
			Iterator<Map.Entry<String, ClientDetails>> it=onlineClients.entrySet().iterator();
			long currentTime=System.currentTimeMillis();
			while(it.hasNext()) {
				Map.Entry<String, ClientDetails> entry=it.next();
				if(currentTime-entry.getValue().getLastHeartbeat()>Constants.Server.CLIENT_EXPIRE_TIME) {
					System.out.println(entry.getValue().getName()+" timed out.");
					it.remove();
				}
			}
		}
		
	}
}
