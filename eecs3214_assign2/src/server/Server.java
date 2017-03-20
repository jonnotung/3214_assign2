package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;

/*
 * Multi threaded server for EECS3214 assignment 1
 *
 * Code adapted from example code by Anatol Ursu from http://www.di.ase.md/~aursu/ClientServerThreads.html
 * Multithreading is used to handle connections to multiple clients concurrently
 */
public class Server {

	// The server socket.
	private static ServerSocket serverSocket = null;
	// The client socket.
	private static Socket clientSocket = null;

	// This chat server can accept up to maxClientsCount clients' connections.
	private static final int maxClientsCount = 15;
	// Array to store connected client threads, shared among all client threads
	private static final clientThread[] threads = new clientThread[maxClientsCount];
	// Array to store names of connected clients, shared among all running
	// client threads
	private static final ArrayList<String> clientNames = new ArrayList<String>(maxClientsCount);

	public static void main(String args[]) {

		// Set the default port number.
		int portNumber = 27459;

		/*
		 * Open a server socket on the portNumber (set to 27459). Note that we
		 * can not choose a port less than 1023 if we are not privileged users
		 * (root).
		 */
		try {
			serverSocket = new ServerSocket(portNumber);
		} catch (IOException e) {
			System.out.println(e);
		}
		System.out.println("Server listening at port " + portNumber);
		/*
		 * Create a client socket for each connection and pass it to a new
		 * client thread.
		 */
		while (true) {
			try {
				clientSocket = serverSocket.accept();
				System.out.println(clientSocket.getInetAddress().toString() + " connected");
				int i = 0;
				for (i = 0; i < maxClientsCount; i++) {
					if (threads[i] == null) {
						(threads[i] = new clientThread(clientSocket, threads, clientNames)).start();
						break;
					}
				}
				// If more than maxClientsCount clients try to access the
				// server, tell them the server is full and close client socket
				if (i >= maxClientsCount) {
					PrintStream os = new PrintStream(clientSocket.getOutputStream());
					os.println("Server too busy. Try later.");
					os.close();
					clientSocket.close();
				}
			} catch (IOException e) {
				System.out.println(e);
			}
		}
	}
}

/*
 * The chat client thread, so the server can handle multiple clients
 * concurrently. This client thread opens the input and the output streams for a
 * particular client, and presents the client with JOIN, LEAVE and LIST command
 * options
 * 
 */
class clientThread extends Thread {
	private String name = "";
	// input stream from client to server
	private BufferedReader is = null;
	// output stream to client from server
	private PrintStream os = null;
	// client socket
	private Socket clientSocket = null;
	// array of connected clients
	private final clientThread[] threads;
	// array of names of connected clients
	private final ArrayList<String> clientNames;
	private int maxClientsCount;

	// Constructor for clientThread
	public clientThread(Socket clientSocket, clientThread[] threads, ArrayList<String> names) {
		this.clientSocket = clientSocket;
		this.threads = threads;
		this.clientNames = names;
		// ensures maxClientCount remains consistent between threads
		maxClientsCount = threads.length;
	}

	public void run() {
		int maxClientsCount = this.maxClientsCount;
		//Array of clients
		clientThread[] threads = this.threads;
		boolean looping = true;
		//String variable to store input from client
		String line;
		try {
			/*
			 * Create input and output streams for this client.
			 */
			is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			os = new PrintStream(clientSocket.getOutputStream());

			// Asks the client to join, list the list of players, or to leave
			while (looping) {
				os.println("To join players enter JOIN. To leave enter LEAVE.");
				line = is.readLine();
				// If LEAVE is entered break the loop and close the connection
				if (line.startsWith("LEAVE")) {
					looping = false;
				}  else if (line.trim().startsWith("JOIN")) {
					// Asks client to enter their unique name					
					os.println("Please enter your name: ");
					name = is.readLine();
					os.println("Welcome " + name);
					break;
				}
			}
			
			//once client has joined they cannot join again
			while (looping) {
				os.println("To see a list of other online players enter LIST.\nCHAT to start a chat session.\nTo leave enter LEAVE.");
				line = is.readLine();
				// If LEAVE is entered break the loop and close the connection
				if (line.startsWith("LEAVE")) {
					looping = false;
				} else if (line.startsWith("LIST")) {
					os.println("Current players are: ");
					// ensure threads are synchronized when accessing shared data
					
					synchronized (this) {
						// iterates through all concurrently running client
						// threads and prints their names with their associated IP addresses
						for (int i = 0; i < maxClientsCount; i++) {
							if (threads[i] != null) {
								os.println(threads[i].name + " at IP: " + threads[i].clientSocket.getInetAddress().toString() + " Port: " + threads[i].clientSocket.getPort());
							}
						}
					}
				}
			}
			//Client thread has finished. 
			os.println("*** Bye " + name + " ***");

			/*
			 * Clean up. Set the current thread variable to null so that a new
			 * client could be accepted by the server.
			 */
			synchronized (this) {
				for (int i = 0; i < maxClientsCount; i++) {
					if (threads[i] == this) {
						threads[i] = null;
					}
				}
			}
			/*
			 * Close the output stream, close the input stream, close the
			 * socket.
			 */
			System.out.println("Client disconnected");
			is.close();
			os.close();
			clientSocket.close();
		} catch (IOException e) {
		}
	}
}