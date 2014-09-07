package in.ac.iith.chat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ClientMain {
	public static void main(String[] args)throws IOException {
		BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
		String nickname;
		Client client=null;
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
		client.start();
	}
}
