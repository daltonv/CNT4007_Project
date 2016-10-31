import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;

public class PeerProcess {
public class PeerProcess implements Runnable{
	private Config config;
	private int myID;
	private int myIndex;
	private PeerRecord[] neighbors;
	private int neighborsCount;
	private HashMap<Integer, PeerRecord> peerMap;

	public PeerProcess(int myID) throws UnknownHostException, IOException {
		this.myID = myID;
		this.config = new Config("common.cfg", "peerinfo.cfg", myID);
		this.peerMap = config.getPeerMap();
		this.neighborsCount = config.getPeerCount();
		neighbors = new PeerRecord[neighborsCount];
		//TODO bitfield class init
	}

	public void sendConnection(Socket s, int peerID) throws Exception {
		Message shake = new Message(); //create a message for handshacking
		shake.setID(myID); //set the message ID to myID

		shake.sendHandShake(s); //send the message out

		shake.readHandShake(s); //read the handshake message

		if(shake.getID() != peerID) {
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
		//get the peerMap and sort it by peerID
		List<PeerRecord> sortedPeers = new ArrayList<PeerRecord>(peerMap.values());
		Collections.sort(sortedPeers, new Comparator<PeerRecord>() {
			public int compare(PeerRecord peer1, PeerRecord peer2) {
				return peer1.peerID - peer2.peerID;
			}
		});

		sortedPeers.remove(Integer.valueOf(this.myID)); //ensure my peer info isn't in the list
		
		for (PeerRecord peer: sortedPeers) {
			//if we appear first we are a server
			if(myID < peer.peerID) {
				ServerSocket serv = new ServerSocket(peer.portNumber);
				getConnection(serv);
			}
			//if we appear second we are a client
			else if(myID > peer.peerID) {
				Socket socket = new Socket(peer.host,config.getMyPortNumber());
				sendConnection(socket, peer.peerID);
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
