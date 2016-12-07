import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;


public class PeerProcess implements Runnable{
	private Config config; //my configuration info
	private int myID; //myID
	private BitField myBitField; //my own BitField
	private FileManager myFileManager;
	private P2PLogger myLogger;

	private HashMap<Integer, PeerRecord> peerMap;

	public PeerProcess(int myID) throws UnknownHostException, IOException {
		this.myID = myID;
		this.config = new Config("common.cfg", "peerinfo.cfg", myID); //read config files
		this.peerMap = config.getPeerMap(); //get the peer map
		this.myBitField = config.getMyBitField(); //get my BitField
		this.myFileManager = config.getMyFileManager();
		this.myLogger = new P2PLogger(myID,myFileManager.getFileHandler());
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
				System.out.println("Peer:" + myID + " listening for hostname " + peer.host + " via socket " + config.getMyPortNumber());
				ServerSocket serv = new ServerSocket(config.getMyPortNumber(),0,InetAddress.getByName(peer.host)); //create server socket
				Socket socket = serv.accept();	//now listen for requests
				serv.close(); //close the server socket now that it is not needed
				System.out.println("Peer:" + myID + " heard news from " + socket.getInetAddress().toString());

				//create input and output data streams, and save them in the peer
				DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
				outStream.flush();
				DataInputStream inStream = new DataInputStream(socket.getInputStream());
				
				Message shake = new Message(config.getPieceSize());
				shake.readHandShake(inStream);
				PeerRecord cPeer = peerMap.get(shake.getID());
				
				cPeer.inStream = inStream;
				cPeer.outStream = outStream;
				cPeer.socket = socket;
			}
			//if we appear second we are a client
			else if(myID > peer.peerID) {
				System.out.println("Peer:" + myID + " trying to connect to " + peer.host + " via socket " + peer.portNumber);
				Socket socket = new Socket(peer.host,peer.portNumber);
				
				DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
				outStream.flush();
				DataInputStream inStream = new DataInputStream(socket.getInputStream());
				
				peer.inStream = inStream;
				peer.outStream = outStream;
				peer.socket = socket;

				//create input and output data streams, and save them in the peer
				Message shake = new Message(config.getPieceSize());
				shake.sendHandShake(peer, myID);
				peer.sentHandShake = true;
				System.out.println("Peer:" + myID + " sent handshake to Peer:" + peer.peerID);
			}
		}
	}

	public synchronized void handleMessages(List<PeerRecord> peerList) throws Exception {
		for(PeerRecord peer: peerList) {
			if(peer.inStream.available() >= 5) {
				Message gotMessage = new Message(config.getPieceSize()); //create message object for received message
				gotMessage.readMessage(peer,myID); //read message
		
				switch (gotMessage.getType()) {
					case Message.HANDSHAKE:
						handleHandshake(peer,gotMessage);
						break;
		
					case Message.BITFIELD:
						handleBitfield(peer,gotMessage);
						break;
		
					case Message.INTERESTED:
						peer.isInterested = true; //update my record of the peer saying it's interested in my
						myLogger.logReceiveInterested(peer.peerID); //log the received interested message
						break;
		
					case Message.NOTINTERESTED:
						peer.isInterested = false; //update my record of the peer saying it's not interested in me
						myLogger.logNotInterested(peer.peerID); //log the received notinterested message
						break;
		
					case Message.CHOKE:
						myLogger.logChoking(peer.peerID); //log the received notinterested message
						break;
					
					case Message.UNCHOKE:
						handleUnchoke(peer,gotMessage);
						break;
		
					case Message.REQUEST:
						handleRequest(peer,gotMessage);
						break;
		
					case Message.PIECE:
						handlePiece(peer,gotMessage);
						break;
						
					case Message.HAVE:
						handleHave(peer,gotMessage);
						break;
						
					default:
						break;
				}
			}
		}
	}

	public void handleHandshake(PeerRecord peer, Message gotMessage) throws Exception {
		System.out.println("Peer:" + myID + " got handshake from Peer:" + peer.peerID);
		if(peer.sentHandShake) {
			System.out.println("Peer:" + myID + " sending bitfield to Peer:" + peer.peerID);
			myLogger.logTCPConnTo(peer.peerID);
			gotMessage.clear();
			gotMessage.sendBitField(peer,myBitField);
		}
		else {
			System.out.println("Peer:" + myID + " sending handshake 2 to Peer:" + peer.peerID);
			myLogger.logTCPConnFrom(peer.peerID);
			gotMessage.clear();
			gotMessage.sendHandShake(peer, myID);
		}
	}

	public void handleBitfield(PeerRecord peer, Message gotMessage) throws Exception {
		peer.bitField.setBitField(gotMessage.getPayLoad()); //update the bitfield to match
		System.out.println("Peer:" + peer.peerID + " bitfield is " + peer.bitField.getText());
		
		if(!peer.sentHandShake) {
			System.out.println("Peer:" + myID + " sending bitfield to Peer:" + peer.peerID);
			gotMessage.clear();
			gotMessage.sendBitField(peer,myBitField);
		}
		else {
			int interestingIndex = myBitField.getInterestingIndex(peer.bitField); //get interesting index compared to my bitfield
			gotMessage.clear();
			if (interestingIndex != -1) {
				System.out.println("Peer:" + myID + " is interested in Peer:" + peer.peerID);
				
				peer.interestedIn = true; //update my record of the peer to show my interest

				gotMessage.sendInterested(peer);
			}
			else {
				System.out.println("Peer:" + myID + " is not interested in Peer:" + peer.peerID);

				peer.interestedIn = false; //update my record of the peer to show my disinterest

				gotMessage.sendNotInterested(peer);
			}
		}
	}

	public void handleUnchoke(PeerRecord peer, Message gotMessage) throws Exception {
		myLogger.logUnchoking(peer.peerID);

		int pieceIndex = myBitField.getRandomNeededIndex(peer.bitField); //get a pieceIndex I need
		
		if(pieceIndex != -1) {

			gotMessage.clear();
			gotMessage.sendRequest(peer,pieceIndex);

			System.out.println("Peer:" + myID + " sent request for piece " + pieceIndex + " to Peer:" + peer.peerID);
		}
	}

	public void handleRequest(PeerRecord peer, Message gotMessage) throws Exception {
		ByteBuffer b = ByteBuffer.wrap(gotMessage.getPayLoad());
		int pieceIndex = b.getInt(0);
		
		if(pieceIndex != -1) {
			System.out.println("Peer:" + myID + " received request for piece " + pieceIndex + " from Peer:" + peer.peerID);
			
			Pieces piece = myFileManager.getPiece(pieceIndex); //get the piece at the index
			gotMessage.clear();
			gotMessage.sendPiece(peer, piece);

			System.out.println("Peer:" + myID + " sent piece " + pieceIndex + " to Peer:" + peer.peerID);
		}
		else {
			System.out.println("Peer:" + myID + " received a request from Peer:" + peer.peerID + " but they already have that piece");
		}
	}

	public void handlePiece(PeerRecord peer, Message gotMessage) throws Exception {
		byte[] payload = gotMessage.getPayLoad(); //create byte array for the message payload
				
		ByteBuffer b = ByteBuffer.wrap(payload); //create bytebuffer for payload
		int pieceIndex = b.getInt(0); //get the pieceIndex
		byte[] pieceBytes = new byte[payload.length - 4]; //create array for the actual pieceBytes
		System.arraycopy(payload,4,pieceBytes,0,payload.length-4); //copy the piece stuff into the pieceBytes
		Pieces piece = new Pieces(pieceIndex,pieceBytes); //create piece from the payload information

		System.out.println("Peer:" + myID + " received and downloaded piece " + pieceIndex + " from Peer:" + peer.peerID);

		myFileManager.putPiece(piece); //place piece in file
		myBitField.turnOnBit(pieceIndex); //update my bitfield to reflect new piece

		myLogger.logPieceDownload(peer.peerID,pieceIndex,myBitField.getPiecesCountDowned()); //log the piece download

		/* Send have message to all my neighbors so they can update their bitfields */
		List<PeerRecord> peerList = new ArrayList<PeerRecord>(peerMap.values());
		for(PeerRecord entry : peerList) {
			gotMessage.clear();
			gotMessage.sendHave(entry, pieceIndex);
		}
		
		if(myBitField.isFinished()) {
			System.out.println("Peer:" + myID + " is finished");
			myLogger.logDownloadComp(); //log that i am finished
			waitToExit();
		}
		else {
			int newPieceIndex = myBitField.getRandomNeededIndex(peer.bitField);	
			b = ByteBuffer.allocate(4); //setup byte buffer for payload
			byte[] msg = b.putInt(newPieceIndex).array(); //put pieceIndex in byte[]
			Message requestMsg = new Message(config.getPieceSize());
			requestMsg.setType(Message.REQUEST);
			requestMsg.setPayLoad(msg);
			requestMsg.sendMessage(peer);
			
			System.out.println("Peer:" + myID + " sent request for piece " + newPieceIndex + " to Peer:" + peer.peerID);		
		}
		peer.piecesSinceLastRound++; //updated how many pieces i got from last unchoking round	
	}
	
	public void handleHave(PeerRecord peer, Message gotMessage) throws IOException {
		byte[] payload = gotMessage.getPayLoad(); //create byte array for the message payload
		ByteBuffer b = ByteBuffer.wrap(payload); //create bytebuffer for payload
		int pieceIndex = b.getInt(0); //get the pieceIndex

		myLogger.logReceiveHave(peer.peerID,pieceIndex); //log receiving the have message
		
		peer.bitField.turnOnBit(pieceIndex); //update this peer's bitfield in my records

		int interest = myBitField.getInterestingIndex(peer.bitField);
		gotMessage.clear();

		/* only send interested or not interested if something changed */
		if(interest == -1 && peer.interestedIn) {
			peer.interestedIn = false; //update my record of the peer to show my disinterest
			gotMessage.sendNotInterested(peer); //send a not interested message
		}
		else if (interest > -1 && !peer.interestedIn) {
			peer.interestedIn = true; //update my record of the peer to show my interest
			gotMessage.sendInterested(peer);
		}

	}

	public void unchokingUpdate() throws Exception {
		if(myBitField.isFinished()) {
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
		
		ArrayList<Integer> prefNeighbors = new ArrayList<Integer>();
		if(sortedPeers.size()>0){
			//send unchoke for the first number of prefferedneighbors peers
			for(int i = 0; i<config.getNumberPreferredNeighbors(); i++) {
				if(sortedPeers.get(i).isChoked) {
					//neighborList.put(sortedPeers.get(i).peerID,sortedPeers.get(i)); //add to neighbor list

					Message unchokeMsg = new Message(config.getPieceSize()); //create message object 
					unchokeMsg.setType(Message.UNCHOKE); //set message to UNCHOKE type
					unchokeMsg.sendMessage(sortedPeers.get(i)); //send message

					sortedPeers.get(i).isChoked = false; //update peer to be unchoked

					//System.out.println("Peer:" + myID + " sending unchoke message to Peer:" + sortedPeers.get(i).peerID);
				}
				if(sortedPeers.get(i).isOptimisticallyUnchoked) {
					sortedPeers.get(i).isOptimisticallyUnchoked = false;
				}
				sortedPeers.get(i).piecesSinceLastRound = 0;
				prefNeighbors.add(sortedPeers.get(i).peerID);	
			}
			myLogger.changePrefLog(prefNeighbors);

			//send choke to the rest of the peers
			for(int i = config.getNumberPreferredNeighbors(); i < sortedPeers.size(); i++) {
				if(!sortedPeers.get(i).isChoked && !sortedPeers.get(i).isOptimisticallyUnchoked) {
					//neighborList.remove(sortedPeers.get(i).peerID); //remove from neighbor list
					//sortedPeers.get(i).isInterested = false; //set isinterested to false

					Message chokeMsg = new Message(config.getPieceSize()); //create message object
					chokeMsg.setType(Message.CHOKE); //set message type to choke
					chokeMsg.sendMessage(sortedPeers.get(i));

					System.out.println("Peer:" + myID + " sending choke message to Peer:" + sortedPeers.get(i).peerID);

					sortedPeers.get(i).isChoked = true;
				}
				sortedPeers.get(i).piecesSinceLastRound = 0;
			}
		}

	}

	public void optomisticUnchokingUpdate() throws Exception {
		if(myBitField.isFinished()) {
			waitToExit();
		}

		System.out.println("Peer:" + myID + " entering optimistic update");

		List<PeerRecord> peers = new ArrayList<PeerRecord>(peerMap.values());
		long seed = System.nanoTime(); //get a seed for random number with nano time;
		Collections.shuffle(peers, new Random(seed)); //shuffle peers order randomly
		
		for (int i = 0; i < peers.size(); i++) {
			if(peers.get(i).isChoked && peers.get(i).isInterested) {
				//neighborList.put(peers.get(i).peerID,peers.get(i)); //add to neighbor list

				Message unchokeMsg = new Message(config.getPieceSize()); //create message object 
				unchokeMsg.setType(Message.UNCHOKE); //set message to UNCHOKE type
				unchokeMsg.sendMessage(peers.get(i)); //send message
				
				//System.out.println("Peer:" + myID + " sending unchoke message to Peer:" + peers.get(i).peerID);	
				peers.get(i).isOptimisticallyUnchoked = true;
				myLogger.logChangeOpt(peers.get(i).peerID); //log the change of optimistic neighbor
			}
		}
	}

	public void waitToExit() throws IOException {
		//System.out.println("Peer:" + myID + " has the file and is now waiting for all peers to finish");
		boolean finished = true;

		List<PeerRecord> peerList = new ArrayList<PeerRecord>(peerMap.values());
		for(PeerRecord peer: peerList) {
			if(!peer.bitField.isFinished()) {
				finished = false;
			}
		}
		if(finished) {
			System.out.println("All peers are finished. Exiting program");
			for(PeerRecord peer: peerList) {
				peer.socket.close();
			}
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
				handleMessages(peerList);

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
