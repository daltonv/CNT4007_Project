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
	private BitField myBitField;

	private HashMap<Integer, PeerRecord> peerMap;

	public PeerProcess(int myID) throws UnknownHostException, IOException {
		this.myID = myID;
		this.config = new Config("common.cfg", "peerinfo.cfg", myID); //read config files
		this.peerMap = config.getPeerMap(); //get the peer map
		this.myBitField = config.getMyBitField(); //get my BitField
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
				System.out.println("Peer:" + myID + " listening for hostname " + peer.host + " via socket " + peer.portNumber);
				ServerSocket serv = new ServerSocket(peer.portNumber); //create server socket
				Socket socket = serv.accept();	//now listen for requests
				System.out.println("Peer:" + myID + " heard news");

				//create input and output data streams, and save them in the peer
				DataInputStream inStream = new DataInputStream(socket.getInputStream());
				DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
				peer.inStream = inStream;
				peer.outStream = outStream;

			}
			//if we appear second we are a client
			else if(myID > peer.peerID) {
				System.out.println("Peer:" + myID + " trying to connect to " + peer.host + " via socket " + config.getMyPortNumber());
				Socket socket = new Socket(peer.host,config.getMyPortNumber());
				
				DataInputStream inStream = new DataInputStream(socket.getInputStream());
				DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
				peer.inStream = inStream;
				peer.outStream = outStream;

				//create input and output data streams, and save them in the peer
				Message shake = new Message();
				shake.sendHandShake(peer);
				System.out.println("Peer:" + myID + " sent handshake to Peer:" + peer.peerID);
			}
		}
	}

	public void handleMessage(PeerRecord peer, Message gotMessage) throws Exception {
		int interestStatus = 0;

		int stub = 0;
		int index;
		ByteBuffer buf;

		switch (gotMessage.getType()) {
			case Message.HANDSHAKE:
				System.out.println("Peer:" + myID + " got handshake from Peer:" + peer.peerID);
				if(peer.sentHandShake) {
					System.out.println("Peer:" + myID + " sending bitfield to Peer:" + peer.peerID);
					Message sendBitField = new Message(); //create message for sending bitfield
					sendBitField.setType(Message.BITFIELD); //set message type to bitfield
					sendBitField.setPayLoad(peer.bitField.toBytes()); //set the payload bitfield bytes
					sendBitField.sendMessage(peer); //send bitfield
				}
				else {
					System.out.println("Peer:" + myID + " sending handshake 2 to Peer:" + peer.peerID);
					gotMessage.sendHandShake(peer);
				}
				break;
			case Message.BITFIELD:
				peer.bitField.setBitField(gotMessage.getPayLoad()); //update the bitfield to match
				if(!peer.sentHandShake) {
					System.out.println("Peer:" + myID + " sending bitfield to Peer:" + peer.peerID);
					Message sendBitField = new Message(); //create message for sending bitfield
					sendBitField.setType(Message.BITFIELD); //set message type to bitfield
					sendBitField.setPayLoad(peer.bitField.toBytes()); //set the payload bitfield bytes
					sendBitField.sendMessage(peer); //send bitfield
				}
				else {
					int interestingIndex = myBitField.getInterestingIndex(peer.bitField); //get interesting index compared to my bitfield
					Message interest = new Message(); //create message for interest
					if (interestingIndex != -1) {
						System.out.println("Peer:" + myID + " is interested in Peer:" + peer.peerID);
						interest.setType(Message.INTERESTED); //if interested set type to interested
					}
					else {
						System.out.println("Peer:" + myID + " is not interested in Peer:" + peer.peerID);
						interest.setType(Message.NOTINTERESTED); //set type to uninterested
					}
					interest.sendMessage(peer); //send message
				}
				break;
			case Message.CHOKE:
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
					gotMessage.readMessage(peer,myID);
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
