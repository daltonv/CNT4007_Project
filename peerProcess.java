import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;

public class PeerProcess implements Runnable{
	private Config config;
	private int myID;
	private int myPortNumber;
	private String myHost;
	private boolean myHaveFile;

	private HashMap<Integer, PeerRecord> peerMap;

	public PeerProcess(int myID) throws UnknownHostException, IOException {
		this.myID = myID;
		this.config = new Config("common.cfg", "peerinfo.cfg", myID);
		this.peerMap = config.getPeerMap();
		//TODO bitfield class init
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
				System.out.println("Peer:" + peer.peerID + " listening for hostname " + peer.host + " via socket " + peer.portNumber);
				ServerSocket serv = new ServerSocket(peer.portNumber); //create server socket
				Socket socket = serv.accept();	//now listen for requests
				System.out.println("Peer:" + peer.peerID + " heard news");

				//create input and output data streams, and save them in the peer
				DataInputStream inStream = new DataInputStream(socket.getInputStream());
				DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
				peer.inStream = inStream;
				peer.outStream = outStream;

			}
			//if we appear second we are a client
			else if(myID > peer.peerID) {
				System.out.println("Peer:" + peer.peerID + " trying to connect to " + peer.host + " via socket " + peer.portNumber);
				Socket socket = new Socket(peer.host,config.getMyPortNumber());
				
				DataInputStream inStream = new DataInputStream(socket.getInputStream());
				DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
				peer.inStream = inStream;
				peer.outStream = outStream;

				//create input and output data streams, and save them in the peer
				Message shake = new Message();
				shake.sendHandShake(peer);
			}
		}
	}

	public void handleMessage(PeerRecord peer, Message gotMessage) throws Exception {
		int interestStatus = 0;

		int stub = 0;
		int index;
		ByteBuffer buf;

		switch (gotMessage.getType()) {
			case 8:
				if(peer.sentHandShake) {
					System.out.println("Peer:" + peer.peerID + " sending bitfield");
					//sendbitfield
				}
				else {
					System.out.println("Peer:" + peer.peerID + "sending handshake 2");
					gotMessage.sendHandShake(peer);
				}
				break;
			default:
				break;
		}
	}

	public void run() {
		try {
			initConnections();
			System.out.println("Connections started");

			long unchokeTime = System.currentTimeMillis();
			long optTime = System.currentTimeMillis();

			List<PeerRecord> peerList = new ArrayList<PeerRecord>(peerMap.values());

			while(true){
				for(PeerRecord peer: peerList) {
					Message gotMessage = new Message();
					gotMessage.readMessage(peer);
					handleMessage(peer,gotMessage); 
				}
			}
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
