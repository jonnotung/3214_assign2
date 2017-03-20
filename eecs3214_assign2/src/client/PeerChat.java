package client;

import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.Socket;


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
	//switch to determine whether chat session is finished or not
	boolean looping = true;
	String myName=" ";
	String peerName = " ";
		
	/**
	 * Creates a thread to handle peer to peer chat, to be handled over the given socket
	 * @param chatSocket
	 */
	public PeerChat(Socket socket, boolean iStarted, String myname) {
		chatSocket = socket;
		iStartedChat = iStarted;
		myName = myname;
	
	}
	
	/**
	 * Thread to receive responses from chat peer
	 */
	Thread peerResponse = new Thread(new Runnable() {
		public void run() {
			// String to store server response
			String response;
			try {
				while ((response = inStream.readLine()) != null && looping) {
					// if "EXITCHAT" is received then break
					if (response.indexOf("EXITCHAT") != -1) {
						outStream.println("EXITCHAT");
						break;
					}
					//If peer is querying for my name, send it with the flag ***NAME
					if(response.indexOf("***QUERYNAME") != -1) {
						outStream.println("***NAME " + myName);
					}
					//If peer is responding to a name query with ***NAME flag, parse the name and store in peerName
					if(response.indexOf("***NAME") != -1) {
						peerName = response.split(" ", 2)[1].trim();
					}
					// While the peer is responding print response to console
					System.out.println(peerName + ": " + response);
				}
				// close the client process
				looping = false;
			} catch (IOException e) {
				e.printStackTrace(System.out);
			}

		}
	});
	
	@Override
	public void run() {
		String response;
		try {
			// Create user input stream
			inputLine = new BufferedReader(new InputStreamReader(System.in));
			// Creates output stream to peer
			outStream = new PrintStream(chatSocket.getOutputStream());
			// Creates input stream from peer
			inStream = new BufferedReader(new InputStreamReader(chatSocket.getInputStream()));

		} catch (IOException ioe) {
			System.out.println("Could not get I/O from peer: \n");
			ioe.printStackTrace(System.out);
		}

		if (chatSocket != null && outStream != null && inStream != null) {
			try {
				
				//If chat session was not rejected, print message saying it has started

				System.out.println("Chat session started. Type EXITCHAT to exit.");
				//Start a thread to receive chat from peer
				new Thread(peerResponse).start();
				
				//ask peer for their name
				outStream.println("***QUERYNAME");
				
				while(looping) {
					System.out.print(myName + ": ");
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


