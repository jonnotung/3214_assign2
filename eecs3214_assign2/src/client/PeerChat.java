package client;

import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.Socket;


/**
 * Class PeerChat
 * For EECS3214
 * @Jonathan Tung
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
	//String to temporarily store user input from command line
	String userInput;
	//switch to determine whether chat session is finished or not
	boolean looping = true;
	//String to store user's name
	String myName;
	
	
	/**
	 * Creates a thread to handle peer to peer chat, to be handled over the given socket
	 * @param chatSocket
	 */
	public PeerChat(Socket socket, String myname, PrintStream out, BufferedReader in) {
		chatSocket = socket;
		outStream = out;
		inStream = in;
		myName = myname;
	}
	
	/**
	 * Thread to receive responses from chat peer 
	 * A separate thread was used as chat between users may not be a synchronized 1 for 1 message
	 */
	Thread peerResponse = new Thread(new Runnable() {
		public void run() {
			// String to store peer response
			String response;
			try {
				//While the response from peer is not null and the continue flag is true, continue waiting for responses
				while ((response = inStream.readLine()) != null && looping) {
					// if "***EXITCHAT" is received then break and end chat
					if (response.indexOf("***EXITCHAT") != -1) {
						//Respond with "***EXITCHAT" to end chat on peer
						System.out.println("Peer ended chat session. Exiting.");
						looping = false;
						break;
					}
					// While the peer is responding print response to console
					//System.out.print(peerName + ": ");
					System.out.println(response);
				}
				
			} catch (IOException e) {
			}

		}
	});
	
	//Main chat thread, takes input from user and sends to peer
	@Override
	public void run() {
		try {
			// Create user input stream
			inputLine = new BufferedReader(new InputStreamReader(System.in));
			// Creates output stream to peer
			outStream = new PrintStream(chatSocket.getOutputStream());
			// Creates input stream from peer
			inStream = new BufferedReader(new InputStreamReader(chatSocket.getInputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		//if socket and in/out streams were configured correctly
		if (chatSocket != null && outStream != null && inStream != null) {
			try {
				
				//If chat session was not rejected, print message saying it has started

				System.out.println("Chat session started. Type ***EXITCHAT to exit.");
				//Start a thread to receive chat from peer
				new Thread(peerResponse).start();
				
				//While the continue flag is true, take input from the user and send it to the peer
				while(looping) {
					//System.out.print(myName + ": ");
					userInput = inputLine.readLine().trim();
					outStream.println(myName + ": " + userInput);
					//exit chat if ***EXITCHAT is entered
					if (userInput.indexOf("***EXITCHAT") != -1){
						looping = false;
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


