import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;


public class PeerProcess implements Runnable{
	private Config config; //my configuration info
	private int myID; //myID
	private int myPortNumber;
	private String myHost;
	private boolean myHasFile;
	private BitField myBitField; //my own BitField
	private FileManager myFileManager;

	private HashMap<Integer, PeerRecord> peerMap;

	private HashMap<Integer, PeerRecord> interestedList = new HashMap<Integer ,PeerRecord>(); //list of peers with interesting pieces
	private HashMap<Integer, PeerRecord> interestingList = new HashMap<Integer, PeerRecord>(); //list of peers interested in my pieces
	private HashMap<Integer, PeerRecord> senderList = new HashMap<Integer, PeerRecord>(); //list of peers sending me pieces
	private HashMap<Integer, PeerRecord> neighborList = new HashMap<Integer, PeerRecord>(); //list of peers I am sending data to

	public PeerProcess(int myID) throws UnknownHostException, IOException {
		this.myID = myID;
		this.config = new Config("common.cfg", "peerinfo.cfg", myID); //read config files
		this.peerMap = config.getPeerMap(); //get the peer map
		this.myBitField = config.getMyBitField(); //get my BitField
		this.myHasFile = config.getMyHasFile();
		this.myFileManager = config.getMyFileManager();
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
				peer.sentHandShake = true;
				System.out.println("Peer:" + myID + " sent handshake to Peer:" + peer.peerID);
			}
		}
	}

	public void handleMessage(PeerRecord peer, Message gotMessage) throws Exception {
		int interestStatus = 0;

		switch (gotMessage.getType()) {
			case Message.HANDSHAKE:
				System.out.println("Peer:" + myID + " got handshake from Peer:" + peer.peerID);
				if(peer.sentHandShake) {
					System.out.println("Peer:" + myID + " sending bitfield to Peer:" + peer.peerID);
					Message sendBitField = new Message(); //create message for sending bitfield
					sendBitField.setType(Message.BITFIELD); //set message type to bitfield
					sendBitField.setPayLoad(myBitField.toBytes()); //set the payload bitfield bytes
					sendBitField.sendMessage(peer); //send bitfield
				}
				else {
					System.out.println("Peer:" + myID + " sending handshake 2 to Peer:" + peer.peerID);
					gotMessage.sendHandShake(peer);
				}
				break;

			case Message.BITFIELD:
				peer.bitField.setBitField(gotMessage.getPayLoad()); //update the bitfield to match
				System.out.println("Peer:" + peer.peerID + " bitfield is " + peer.bitField.getText());
				if(!peer.sentHandShake) {
					System.out.println("Peer:" + myID + " sending bitfield to Peer:" + peer.peerID);
					Message sendBitField = new Message(); //create message for sending bitfield
					sendBitField.setType(Message.BITFIELD); //set message type to bitfield
					sendBitField.setPayLoad(myBitField.toBytes()); //set the payload bitfield bytes
					sendBitField.sendMessage(peer); //send bitfield
				}
				else {
					int interestingIndex = myBitField.getInterestingIndex(peer.bitField); //get interesting index compared to my bitfield
					Message interest = new Message(); //create message for interest
					if (interestingIndex != -1) {
						System.out.println("Peer:" + myID + " is interested in Peer:" + peer.peerID);
						
						peer.isInterested = true;	//set peer to be interested
						interestedList.put(peer.peerID,peer);	//add to interestedList

						interest.setType(Message.INTERESTED); //if interested set type to interested
					}
					else {
						System.out.println("Peer:" + myID + " is not interested in Peer:" + peer.peerID);

						peer.isInterested = false; //set peer to be not interested
						interestedList.remove(peer.peerID);

						interest.setType(Message.NOTINTERESTED); //set type to uninterested
					}
					interest.sendMessage(peer); //send message
				}
				
				break;

			case Message.INTERESTED:
				peer.isInterested = true;
				
				break;

			case Message.NOTINTERESTED:
				peer.isInterested = false;
				
				break;

			case Message.CHOKE:
				//remove sender
				break;
			
			case Message.UNCHOKE:
				int pieceIndex = myBitField.getInterestingIndex(peer.bitField); //get a pieceIndex I need
				ByteBuffer b = ByteBuffer.allocate(4); //setup byte buffer for payload
				byte[] msg = b.putInt(pieceIndex).array(); //put pieceIndex in byte[]

				Message requestMsg = new Message(); //create message object
				requestMsg.setType(Message.REQUEST); //set message type to REQUEST
				requestMsg.setPayLoad(msg); //set payload to byte[] of pieceIndex
				requestMsg.sendMessage(peer); //send the message

				System.out.println("Peer:" + myID + " sent request for piece " + pieceIndex + " to Peer:" + peer.peerID);

				break;

			case Message.REQUEST:
				//ByteBuffer b = ByteBuffer.wrap(gotMessage.getPayload());
				//int pieceIndex = buf.getInt(0);
				//System.out.println("Peer:" + myID + " received request for piece " + pieceIndec + " from Peer:" + peer.peerID);

				//send piece of pieceIndex to peer

				break;

			default:
				break;
		}
	}

	public void unchokingUpdate() throws Exception {
		if(myHasFile) {
			waitToExit();
		}

		System.out.println("Peer:" + myID + " entered unchoking update");
		List<PeerRecord> sortedPeers = new ArrayList<PeerRecord>(peerMap.values()); //create arrayList of peers
		
		//sort peers by piecesSinceLastRound value
		Collections.sort(sortedPeers, new Comparator<PeerRecord>() {
			public int compare(PeerRecord peer1, PeerRecord peer2) {
				return peer1.piecesSinceLastRound - peer2.piecesSinceLastRound;
			}
		});

		//remove any peers that are not interesting
		for(int i = 0; i < sortedPeers.size(); i++) {
			if(!sortedPeers.get(i).isInterested) {
				sortedPeers.remove(i);
			}
		}
		if(sortedPeers.size()>0){
			//send unchoke for the first numberofprefferedneighbors peers
			for(int i = 0; i<config.getNumberPreferredNeighbors(); i++) {
				if(sortedPeers.get(i).isChoked) {
					neighborList.put(sortedPeers.get(i).peerID,sortedPeers.get(i)); //add to neighbor list

					Message unchokeMsg = new Message(); //create message object 
					unchokeMsg.setType(Message.UNCHOKE); //set message to UNCHOKE type
					unchokeMsg.sendMessage(sortedPeers.get(i)); //send message

					sortedPeers.get(i).isChoked = false; //update peer to be unchoked

					System.out.println("Peer:" + myID + " sending unchoke message to Peer:" + sortedPeers.get(i).peerID);
				}
				if(sortedPeers.get(i).isOptimisticallyUnchoked) {
					sortedPeers.get(i).isOptimisticallyUnchoked = false;
				}
			}

			//send choke to the rest of the peers
			for(int i = config.getNumberPreferredNeighbors(); i < sortedPeers.size(); i++) {
				if(!sortedPeers.get(i).isChoked && !sortedPeers.get(i).isOptimisticallyUnchoked) {
					neighborList.remove(sortedPeers.get(i).peerID); //remove from neighbor list
					sortedPeers.get(i).isInterested = false; //set isinterested to false

					Message chokeMsg = new Message(); //create message object
					chokeMsg.setType(Message.CHOKE); //set message type to choke
					chokeMsg.sendMessage(sortedPeers.get(i));

					System.out.println("Peer:" + myID + " sending choke message to Peer:" + sortedPeers.get(i).peerID);

					sortedPeers.get(i).isChoked = true;
				}
			}
		}

	}

	public void optomisticUnchokingUpdate() throws Exception {
		if(myHasFile) {
			waitToExit();
		}

		System.out.println("Peer:" + myID + " entering optimistic update");

		List<PeerRecord> peers = new ArrayList<PeerRecord>(peerMap.values());
		long seed = System.nanoTime(); //get a seed for random number with nano time;
		Collections.shuffle(peers, new Random(seed)); //shuffle peers order randomly
		
		for (int i = 0; i < peers.size(); i++) {
			if(peers.get(i).isChoked && peers.get(i).isInterested) {
				neighborList.put(peers.get(i).peerID,peers.get(i)); //add to neighbor list

				Message unchokeMsg = new Message(); //create message object 
				unchokeMsg.setType(Message.UNCHOKE); //set message to UNCHOKE type
				unchokeMsg.sendMessage(peers.get(i)); //send message
				
				System.out.println("Peer:" + myID + " sending unchoke message to Peer:" + peers.get(i).peerID);	

				peers.get(i).isOptimisticallyUnchoked = true;
			}
		}
	}

	public void waitToExit() {
		//System.out.println("Peer:" + myID + " has the file and is now waiting for all peers to finish");
		boolean finished = true;

		List<PeerRecord> peerList = new ArrayList<PeerRecord>(peerMap.values());
		for(PeerRecord peer: peerList) {
			if(!peer.bitField.isFinished()) {
				finished = false;
			}
		}
		if(finished) {
			System.exit(0);
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
					if(peer.inStream.available() >= 5) {
						Message gotMessage = new Message(); //create message object for received message
						gotMessage.readMessage(peer,myID); //read message
						handleMessage(peer,gotMessage);  //handle the message
					}
				}

				if(System.currentTimeMillis() > unchokeTime + 1000*config.getUnchokingInterval()) {
					unchokingUpdate();
					unchokeTime = System.currentTimeMillis();
				}

				if(System.currentTimeMillis() > optTime + 1000*config.getOptomisticUnChokingInterval()) {
					optomisticUnchokingUpdate();
					optTime = System.currentTimeMillis();
				}
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String args[]) throws Exception {
		PeerProcess p = new PeerProcess(Integer.parseInt(args[0]));
		Thread t = new Thread(p);
		t.start();
	}

}
