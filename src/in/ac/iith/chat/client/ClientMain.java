package in.ac.iith.chat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ClientMain {
	static Client client;
	public static void main(String[] args)throws IOException {
		BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
		String nickname;
		do {
			System.out.print("Enter you nickname: ");
			nickname=br.readLine();
			try {
				client=new Client(nickname);
				if(client!=null)
					break;
			} catch(Exception e) {
				System.err.println(e.getMessage());
			}
		}while(true);
		Runtime.getRuntime().addShutdownHook(new ShutdownHook());
		client.start();
	}
	
	private static class ShutdownHook extends Thread {
		@Override
		public void run() {
			client.shutdown();
		}
	}
}
