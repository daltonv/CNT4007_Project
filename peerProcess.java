import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class PeerProcess implements Runnable{
	private Config config;
	private int myID;
	private int myIndex;
	private PeerRecord[] neighbors;
	private int neighborsCount;

	public PeerProcess(int myID) throws UnknownHostException, IOException {
		this.myID = myID;
		this.config = new Config("common.cfg", "peerinfo.cfg");
		this.neighborsCount = config.getPeerCount();
		neighbors = new PeerRecord[neighborsCount];
		//TODO bitfield class init
	}

	public void sendConnection(PeerRecord peer) throws Exception {
		Socket s = peer.getDownSocket(); //get peers socket

		System.out.println("Test 2");
		
		Message shake = new Message(); //create a message for handshacking
		shake.setID(myID); //set the message ID to myID

		shake.sendHandShake(s); //send the message out

		shake.readHandShake(s); //read the handshake message

		if(shake.getID() != peer.getID()) {
			throw new Exception("Failed to handshake!");
		}

		System.out.println(this.myID + " receives hand shake message");
	}

	public void getConnection(ServerSocket serv) throws Exception {
		Socket s = serv.accept(); //get peers socket

		System.out.println("Test 3");
		
		Message shake = new Message(); //create a message for handshacking
		shake.setID(myID); //set the message ID to myID
		shake.sendHandShake(s); //send the message out

		shake.readHandShake(s); //read the handshake message
	}

	public void initConnections() throws Exception {

		/*This right here is a great example of why we should switch to using dictionaries for 
		storing config data*/
		for (int i=0; i<config.getPeerCount(); i++) {

			if(config.getIDs().get(i) == this.myID) {
				this.myIndex = i;
				break;
			}
		}

		/*This assumes the peerIDs are ordered. Should probably add code to ensure they are ordered before hand*/
		for (int i=0; i<config.getPeerCount(); i++) {
			if(i != myIndex) {
				
				//if we appear first we are a server
				if(myID < config.getIDs().get(i)) {
					System.out.println("Test 2");
					ServerSocket serv = new ServerSocket(config.getPorts(i));
					getConnection(serv);
				}
				//if we appear second we are a client
				else if(myID > config.getIDs().get(i)) {
					System.out.println("Test 1");
					Socket downSocket = new Socket(config.getHosts().get(i),config.getPorts(myIndex));
					neighbors[i] = new PeerRecord(config.getIDs().get(i),downSocket);

					sendConnection(neighbors[i]);
				}
			}
		}
	} 

	public void run() {
		try {
			initConnections();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*public static void main(String args[]) throws Exception {
		PeerProcess p = new PeerProcess(Integer.parseInt(args[0]));
		Thread t = new Thread(p);
		t.start();
	}*/

}
