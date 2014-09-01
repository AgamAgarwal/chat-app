package in.ac.iith.chat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
	public static void main(String[] args)throws IOException {
		BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
		String nickname;
		System.out.print("Enter you nickname: ");
		nickname=br.readLine();	//TODO: introduce max length of nickname, and take input in loop, until correct
		Client client=new Client(nickname);
		client.start();
	}
}
