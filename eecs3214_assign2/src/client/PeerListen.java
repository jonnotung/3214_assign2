package client;

import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Thread listenChat 
 * 
 * Thread to set up a listening port for a peer to peer chat request
 * The peer chat listening port is set to 27459
 * Will start a peer to peer chat thread if a connection request is received
 */
public class PeerListen extends Thread {
	private ServerSocket chatListenSocket;
	private Socket chatSocket;
	private final int portNumber = 27459;
	private boolean closed, chatting;
	String myName;
	PeerChat peerChat;
	
	/**
	 * Creates a thread to listen for peer to peer chat requests
	 * @param chatSocket
	 */
	public PeerListen (String myname) {
		myName = myname;
	}

	@Override 
	public void run(){
		try {
			//Set up a socket to listen for chat requests at 27459
			chatListenSocket = new ServerSocket(portNumber);

			//Listen until a peer to peer chat session has been initiated
			//either by sending a request to a peer, or a peer sending a request 
			while(!closed) {
				//if a chat session is started, prevent listening for any more connections
				
				chatSocket = chatListenSocket.accept();
				
				//Start a peer to peer chat thread with chatSocket if connection is received
				peerChat = new PeerChat(new Socket(chatSocket.getInetAddress(), chatSocket.getPort()), false, myName);
				peerChat.start();
				closed = true;
				break;
			}
			chatListenSocket.close();
			chatSocket.close();
		} catch (IOException e) {
			//System.out.println(e);
		} 
		
	}
	
	public void start() {
		closed = false;
		chatting = false;
		super.start();
	}
	
	public void stopListening() {
		closed = true;
		
	}

}
