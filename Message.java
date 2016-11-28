import java.io.*;
import java.net.*;
import java.nio.*;
import java.lang.*;
import java.util.Scanner;


public class Message{
	final static int CHOKE = 0;
	final static int UNCHOKE = 1;
	final static int INTERESTED = 2;
	final static int NOTINTERESTED = 3;
	final static int HAVE = 4;
	final static int BITFIELD = 5;
	final static int REQUEST = 6;
	final static int PIECE = 7;
	final static int HANDSHAKE = 8;

	private int type;
	private int length;
	private int ID;
	private byte[] payload;
	private int pieceSize;

	public Message(int pieceSize){
		type = 0;
		length = 0;
		payload = null;
		this.pieceSize = pieceSize;
	}

	public synchronized void readMessage(PeerRecord peer, int myID) throws IOException {
		byte[] temp = new byte[5];
		peer.inStream.read(temp,0,5);

		if(temp[0] == (byte)'P' && temp[1] == (byte)'2' && temp[2] == (byte)'P' && temp[3] =='F' && temp[4] == (byte)'I') {
			byte[] garbage = new byte[27];
			peer.inStream.read(garbage,0,27);
			this.type = HANDSHAKE;
		}
		else {
			ByteBuffer b = ByteBuffer.wrap(temp);
			this.length = b.getInt(0); //converts first 4 bytes to an int
			this.type = (int)temp[4]; //converts last byte to int
			if(this.length > 1){
				payload = new byte[this.length - 1];
				peer.inStream.read(this.payload,0,this.length-1);
			}
			
			System.out.println("Peer:" + myID + " got message of type " + this.type + " and length " + this.length + " from Peer:" + peer.peerID);
		}
	}

	public synchronized void sendMessage(PeerRecord peer) throws IOException {
		if(payload == null){
			length = 1; //set length to 4 if no payload
		}
		else{
			length = payload.length + 1; //set length to 1 + payload size
		}

		byte[] msg = new byte[5];
		ByteBuffer b = ByteBuffer.wrap(msg);
		b.putInt(length);
		msg[4] = (byte)type;
		
		try {
			peer.outStream.write(msg,0,msg.length);
			if(payload != null){
				peer.outStream.write(payload,0,payload.length);
			}
			
			
		}
		catch(IOException ex) {
			System.out.println(ex.toString());
		}
		peer.outStream.flush();
	}

	/*This function handles sending pieces since their payloads need some special configuration */
	public synchronized void sendPiece(PeerRecord peer, Pieces piece) throws IOException {
		byte[] msg = new byte[piece.getPieceBytes().length + 4]; //create byte array for payload
		ByteBuffer b = ByteBuffer.wrap(msg); //create byte buffer for payload
		byte[] index = b.putInt(piece.getPieceIndex()).array(); //put pieceIndex in the the first 4 bytes of the payload
		//System.out.println("Are you sure " + piece.getPieceIndex());
		System.arraycopy(index,0,msg,0,index.length);
		System.arraycopy(piece.getPieceBytes(),0,msg,4,piece.getPieceBytes().length); //copy the piece byte array to the payload array 
		
		this.type = Message.PIECE; //set type to piece
		this.payload = msg; //set the payload to msg
		this.sendMessage(peer); //send the message
	}

	public synchronized void sendBitField(PeerRecord peer, BitField bitfield) throws IOException {
		this.type = Message.BITFIELD; //set message type to bitfield
		this.payload = bitfield.toBytes(); //set the payload bitfield bytes
		sendMessage(peer); //send bitfield
	}

	public synchronized void sendInterested(PeerRecord peer) throws IOException {
		this.type = Message.INTERESTED;
		sendMessage(peer);
	}

	public synchronized void sendNotInterested(PeerRecord peer) throws IOException {
		this.type = Message.NOTINTERESTED;
		sendMessage(peer);
	}

	public synchronized void sendRequest(PeerRecord peer, int pieceIndex) throws IOException {
		this.type = Message.REQUEST;
		
		ByteBuffer b = ByteBuffer.allocate(4); //setup byte buffer for payload
		this.payload = b.putInt(pieceIndex).array(); //put pieceIndex in byte[]

		sendMessage(peer);
	}

	public synchronized void sendHave(PeerRecord peer, int pieceIndex) throws IOException {
		this.type = Message.HAVE;
		
		ByteBuffer b = ByteBuffer.allocate(4); //setup byte buffer for payload
		this.payload = b.putInt(pieceIndex).array(); //put pieceIndex in byte[]

		sendMessage(peer);
	}

	public void clear() {
		this.type = 0;
		this.payload = null;
		this.length = 0;
	}

	public void sendHandShake(PeerRecord peer) throws IOException {
		
		try {
			byte[] handshake = new byte[32];
			
			String header = "P2PFILESHARINGPROJ";
			String zerobits = "0000000000";
			String id = String.valueOf(peer.peerID);
			while(id.length() != 4){
				id = "0" + id;
			}
			String hsString = header + zerobits + id;
			handshake = hsString.getBytes();

			peer.outStream.write(handshake,0,handshake.length);
		}
		catch(IOException e) {
			System.out.println("Error with sending handshake. Could not write to stream");
		}
	}

	public int getType(){
		return type;
	}

	public void setType(int type){
		this.type = type ;
	}

	public byte[] getPayLoad(){
		return payload;
	}

	public void setPayLoad(byte[] payload){
		this.payload = payload;
	}

	public void setID(int ID){
		this.ID = ID;
	}

	public int getID(){
		return ID;
	}
	
	public int getLength() {
		return length;
	}
}