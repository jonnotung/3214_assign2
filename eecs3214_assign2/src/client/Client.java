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
	//boolean to keep track of when a chat session is started
	private static boolean chatting = false;
	//The current user's name
	private static String myName;
	//The name of person the current user is chatting to
	private static String buddyName;
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
				new Thread(new Client()).start();
				while (!closed) {
					// read input from user and send it to the server as long as client thread is running
					userInput = inputLine.readLine().trim();
					if(userInput.equals("CHAT")) {
						chatting = true;
						System.out.println("Enter the IP of the peer you wish to chat with: ");
						String chatPeerAddress = inputLine.readLine().trim();
						chatSocket = new Socket(chatPeerAddress, portNumber);
						//Start a peer to peer chat thread with chatSocket
						new PeerChat(chatSocket, true).start();
					}

					//If the user starts a peer chat session, suspend this thread that sends to the server 
					if(chatting) {
						Thread.sleep(1);
					}
					outStream.println(userInput);
				}

				// Close I/O streams and socket when done
				outStream.close();
				inStream.close();
				clientSocket.close();
			} catch (IOException e) {
				e.printStackTrace(System.out);
			} catch (InterruptedException e) {
				e.printStackTrace(System.out);
			}
		}

	}

	/**
	 * Thread listenChat 
	 * 
	 * Thread to set up a listening port for a peer to peer chat request
	 * The peer chat listening port is set to 27459
	 * Will start a peer to peer chat thread if a connection request is received
	 */
	Thread listenChat = new Thread(new Runnable() {
		public void run(){
			try {
				//Set up a socket to listen for chat requests at 27459
				chatListenSocket = new ServerSocket(portNumber);

				//Listen until a peer to peer chat session has been initiated
				//either by sending a request to a peer, or a peer sending a request 
				while(true) {
					//if a chat session is started, prevent listening for any more connections
					while(chatting) {
						Thread.sleep(1);
					}

					chatSocket = chatListenSocket.accept();
					chatting = true;
					//Start a peer to peer chat thread with chatSocket if connection is received
					new PeerChat(chatSocket, false).start();

				}

			} catch (IOException e) {
				System.out.println(e);
			} catch (InterruptedException e) {
				e.printStackTrace(System.out);
			}
		}
	});

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
			while ((response = inStream.readLine()) != null) {
				// While the server is responding print response to console
				System.out.println(response);

				// if "Welcome" is received, user has joined, start start listening for chat requests
				if(response.indexOf("Welcome") != -1) {
					new Thread(listenChat).start();
				}

				// if "** BYE" is received then break
				if (response.indexOf("*** Bye") != -1)
					break;
			}
			// close the client process
			closed = true;
		} catch (IOException e) {
			e.printStackTrace(System.out);
		}

	}
}

/**
 * Class PeerChat
 *  
 *  This class contains a thread that will handle peer to peer chatting
 *  The thread is run when the client sends a request to a peer to chat or
 *  the client receives a chat request from a peer.
 */
class PeerChat extends Thread {
	//chat socket to peer
	private Socket chatSocket;
	// output stream to chat peer
	private PrintStream outStream;
	// input stream from chat peer
	private BufferedReader inStream;
	// input stream from command line from user
	private  BufferedReader inputLine;
	String userInput;
	//switch to identify whether user initiated peer chat, or they were contacted by a peer
	private boolean iStartedChat;

	boolean looping = true;
	
	/**
	 * Constructor
	 * @param chatSocket
	 */
	PeerChat(Socket socket, boolean iStarted) {
		chatSocket = socket;
		iStartedChat = iStarted;
	}
	
	/**
	 * Thread to receive responses from chat peer
	 */
	Thread peerResponse = new Thread(new Runnable() {
		public void run() {
			// String to store server response
			String response;
			try {
				while ((response = inStream.readLine()) != null) {
					// if "EXITCHAT" is received then break
					if (response.indexOf("EXITCHAT") != -1) {
						outStream.println("EXITCHAT");
						break;
					}
					
					// While the peer is responding print response to console
					System.out.println("Peer: " + response);
				}
				// close the client process
				looping = false;
			} catch (IOException e) {
				e.printStackTrace(System.out);
			}

		}
	});
	
	public void run() {
		String response;
		try {
			// Create user input stream
			inputLine = new BufferedReader(new InputStreamReader(System.in));
			// Creates output stream to server
			outStream = new PrintStream(chatSocket.getOutputStream());
			// Creates input stream from server
			inStream = new BufferedReader(new InputStreamReader(chatSocket.getInputStream()));

		} catch (IOException ioe) {
			System.out.println("Could not get I/O from host: \n");
			ioe.printStackTrace(System.out);
		}

		if (chatSocket != null && outStream != null && inStream != null) {
			try {
				//If user did not initiate chat, ask them if they want to accept the chat
				if(!iStartedChat) {
					while(looping) {
						System.out.println("Chat request received, accept? YES/NO");
						userInput = inputLine.readLine().trim();
						//If user does not want to accept chat
						if(userInput.equals("NO")) {
							//skip all subsequent loops and close sockets
							looping = false;
							outStream.println("***REJECTED");
						}
						else if (userInput.equals("YES")){
							outStream.println("***ACCEPTED");
							break;
						}
					}
				}
				//if user initiated chat, wait for accept or reject response
				else {
					//Get response from peer
					while ((response = inStream.readLine()) != null) {
						
						// if "***ACCEPTED" is received then break and continue
						if (response.indexOf("***ACCEPTED") != -1) {
							break;
						}
						// if ***REJECTED is received break and skip to end where socket and connections are closed
						else if (response.indexOf("***REJECTED") != -1) {
							System.out.println("Chat request rejected.");
							looping = false;
							break;
						}
					}
				}
				
				//If chat session was not rejected, print message saying it has started
				if(looping) {
					System.out.println("Chat session started. Type EXITCHAT to exit.");
					//Start a thread to receive chat from peer
					new Thread(peerResponse).start();
				}
				
				while(looping) {
					System.out.print("Me: ");
					userInput = inputLine.readLine().trim();
					outStream.println(userInput);
					//exit chat if EXITCHAT is entered
					if (userInput.equals("EXITCHAT")){
						break;
					}
				}
				
				//End of session, close socket and connections
				outStream.close();
				inStream.close();
				chatSocket.close();
			}catch (IOException ioe) {
				System.out.println("Could not get I/O from host: \n");
				ioe.printStackTrace(System.out);
			}
		}
	}
}


