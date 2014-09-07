package in.ac.iith.chat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
	public static void main(String[] args)throws IOException {
		BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
		String nickname;
		Client client=null;
		do {
			System.out.print("Enter you nickname: ");
			nickname=br.readLine();	//TODO: introduce max length of nickname, and take input in loop, until correct
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
