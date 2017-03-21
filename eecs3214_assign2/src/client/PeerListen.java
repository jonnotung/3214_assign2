package client;

import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Class listenChat
 * For: EECS3214 Assignment 2
 * @author Jonathan Tung 
 * 
 * Thread to set up a listening port for a peer to peer chat request
 * The peer chat listening port is set to 27459
 * Will start a peer to peer chat thread if a connection request is received
 */
public class PeerListen extends Thread {
	private ServerSocket chatListenSocket;
	private Socket chatSocket = null;
	private final int portNumber = 27459;
	private boolean closed;
	private PrintStream outStream;
	private BufferedReader inStream;
	private PrintStream serverOut;
	String myName;
	PeerChat peerChat;
	
	/**
	 * Creates a thread to listen for peer to peer chat requests
	 * @param myName the user's entered name
	 * 		out output stream to the server, needed to close server connection
	 */
	public PeerListen (String myname, PrintStream out) {
		myName = myname;
		serverOut = out;
	}
	
	/**
	 * Main PeerListen thread. Listens for a peer to contact at chatListenSocket
	 * If a connection is received, set up chatSocket to conduct the peer to peer chat and
	 * start a PeerChat thread to handle the chat.
	 * 
	 * If a PeerChat thread is successfully started, the client's connections to the server are closed
	 * which is to reduce confusion and text on screen due to the limitations of having only a single console 
	 * for input and output.
	 */
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
				// Creates output stream to peer
				outStream = new PrintStream(chatSocket.getOutputStream());
				// Creates input stream from peer
				inStream = new BufferedReader(new InputStreamReader(chatSocket.getInputStream()));
				
				if(chatSocket != null){
					//Start a peer to peer chat thread with chatSocket if connection is received
					peerChat = new PeerChat(chatSocket, myName, outStream, inStream);
					peerChat.start();
					serverOut.println("LEAVE");
					closed = true;
					break;
				}
			}
			chatListenSocket.close();
			
		} catch (IOException e) {
			//System.out.println(e);
		} 
		
	}
	
	/**
	 * Method to start a PeerListen thread
	 */
	public void start() {
		//ensures the while loop will enter
		closed = false;
		//calls run()
		super.start();
	}
	
	/**
	 * Method to stop a PeerListen thread
	 */
	public void stopListening() {
		try {
			closed = true;
			//closing chatListenSocket stops it from blocking the thread while waiting for a connection
			chatListenSocket.close();
		} catch (IOException e) {
			
		}
		
	}

}
