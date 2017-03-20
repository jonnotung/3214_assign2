package client;

import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/*
 * Multithreaded client for EECS3214 assignment 1
 * Code adapted from example code by Anatol Ursu at http://www.di.ase.md/~aursu/ClientServerThreads.html
 * Multithreading is used to concurrently handle I/O with the user and from the server.
 */

public class Client implements Runnable {

	// Client socket to connect to chat server
	private static Socket clientSocket;
	// output stream to server
	private static PrintStream outStream;
	// input stream from server
	private static BufferedReader inStream;
	// input stream from command line from user
	private static BufferedReader inputLine;
	//boolean to keep track of when to close connection to server
	private static boolean closed = false;
	
	//The current user's name
	private static String myName = null;
	//The name of person the current user is chatting to
	private static String peerName = null;
	//Port of server to connect to
	private static int portNumber;
	//Address of server to connect to
	private static String host;
	//String to store user input for checking
	private static String userInput;
	//Server socket for client to listen to peer chat requests with
	private static ServerSocket chatListenSocket;
	//Socket to communicate peer to peer chat over
	private static Socket chatSocket;
	
	//Thread to listen for peer to peer chat requests
	static PeerListen peerListener;
	//Thread to conduct peer to peer chat 
	static PeerChat peerChat;
	//Thread to get response from server
	static Thread serverResponse;
	
	public static void main(String[] args) {
		/**
		 * Open a socket to the given or default host at port 27459 Open input
		 * and output streams from server, and input stream from user
		 */
		// server listen port set to 27459
		portNumber = 27459;
		// default host is local machine
		host = "localhost";

		try {
			// Create user input stream
			inputLine = new BufferedReader(new InputStreamReader(System.in));

			// enter address of host to connect to
			System.out.println("Enter host address: ");
			host = inputLine.readLine().trim();

			// creates a new socket to connect to specified host and port
			clientSocket = new Socket(host, portNumber);

			// Creates output stream to server
			outStream = new PrintStream(clientSocket.getOutputStream());
			// Creates input stream from server
			inStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

			System.out.println("Now using host: " + host + "at port " + portNumber);

		} catch (UnknownHostException e) {
			System.out.println("Unknown host: \n");
			e.printStackTrace(System.out);
		} catch (IOException ioe) {
			System.out.println("Could not get I/O from host: \n");
			ioe.printStackTrace(System.out);
		}

		/**
		 * If sockets and I/O streams have been initialized properly Write some
		 * data to the opened port
		 */
		if (clientSocket != null && outStream != null && inStream != null) {
			try {
				// create a thread to read from the server
				serverResponse = new Thread(new Client());
				serverResponse.start();
				
				while (!closed) {
					// read input from user and send it to the server as long as client thread is running
					userInput = inputLine.readLine().trim();
					
					//If user wants to chat
					if(userInput.equals("CHAT")) {
						
						System.out.println("Enter the IP address of the peer you wish to chat with: ");
						//ask for the IP address of the chat peer
						String chatPeerAddress = inputLine.readLine().trim();
						
						//Create a socket to conduct chat over
						chatSocket = new Socket(chatPeerAddress, portNumber);
						//if the socket successfully connects
						if(chatSocket != null) {
							
							System.out.println("Peer connecteded. Disconnecting from server.");
							//Start a peer to peer chat thread using chatSocket
							peerChat = new PeerChat(chatSocket, true, myName);
							peerChat.start();
							//chat session started, only 1 at a time, stop listening for chat requests
							peerListener.stopListening();
							//stop threads communicating with server, since we want command line input to go to chat, not the server
							closed = true;
						}
						else {
							System.out.println("Could not connect to peer.");
						}
					}
					else {
					//send user input to server
					outStream.println(userInput);
					}
				}

				// Close I/O streams and socket when done
				outStream.close();
				inStream.close();
				clientSocket.close();
			} catch (IOException e) {
				//e.printStackTrace();
			} 
		}

	}

	
	/**
	 * Thread to receive from server
	 * 
	 * Create separate thread to read from the server Keep on reading until
	 * "** BYE" is received from the server then break and stop thread
	 * Will start a listenChat thread to listen for chat requests when the user JOINs the server
	 */
	@Override
	public void run() {
		// String to store server response
		String response;
		try {
			while ((response = inStream.readLine()) != null && !closed) {
				// While the server is responding print response to console
				

				// if "Welcome" is received, user has joined, start start listening for chat requests
				if(response.indexOf("Welcome") != -1) {
					//parse user's name and store in myName
					myName = response.split(" ", 2)[1];
					System.out.println(response);
					peerListener = new PeerListen(myName);
					peerListener.start();
				}
				
				// if "** BYE" is received then break
				if (response.indexOf("*** Bye") != -1) {
					System.out.println(response);
					break;
				}
					
			}
			// close the client process
			closed = true;
		} catch (IOException e) {
			//e.printStackTrace(System.out);
		}

	}
}

