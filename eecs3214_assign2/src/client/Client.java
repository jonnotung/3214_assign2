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

public class Client {

	// Client socket
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


	
	public static void main(String[] args) {
		// server listen port set to 27359
		portNumber = 27459;
		// default host is local machine
		host = "localhost";

		/*
		 * Create separate thread to read from the server Keep on reading until
		 * "** BYE" is received from the server then break and stop thread
		 */
		Thread fromServer = new Thread(new Runnable() {
			public void run() {
				// String to store server response
				String response;
				try {
					while ((response = inStream.readLine()) != null) {
						// While the server is responding print response to console
						System.out.println(response);
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
		});
		
		/*
		 * Thread to contact the server and to send user input to the server
		 */
		Thread contactServer = new Thread(new Runnable() {
			public void run() {
				/**
				 * Open a socket to the given or default host at port 27459 Open input
				 * and output streams from server, and input stream from user
				 */
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
						fromServer.start();
						while (!closed) {
							
							// read input from user and send it to the server as long as client thread is running
							userInput = inputLine.readLine().trim();
							if(userInput.equals("CHAT")) {
								chatting = true;
							}
							
							//If the user enters "CHAT", suspend this thread and launch the peer to peer chat thread
							//Only suspend the thread that sends to the server, as the server should not send to this 
							//client if nothing is sent to it
							if(chatting) {
								wait();
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
		});

		Thread peerChat = new Thread(new Runnable() {
			public void run() {
				//Server socket to receive from chat partner
				ServerSocket serverSocket = null;
				//Client socket to send to chat partner
				Socket clientSocket = null;
			}
		});
		
		//run thread to contact server
		contactServer.start();
	}

}