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
		
	/**
	 * Creates a thread to handle peer to peer chat, to be handled over the given socket
	 * @param chatSocket
	 */
	public PeerChat(Socket socket, boolean iStarted) {
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
	
	@Override
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


