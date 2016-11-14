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

	public Message(){
		type = 0;
		length = 0;
		payload = null;
	}

	/* Helper function to convert byte[] to an integer value */
	private static int bytesToInt(byte[] bytes, int length) {
		int b = 0;
		for(int i=0;i<length;i++) {
			b = (b << 8 | bytes[i] & 0xFF);
		}
		return b;
	}

	/* Helper function to convert integer value to a byte[] */
	private static byte[] intToBytes(int d) {
		byte[] bytes = new byte[4];
		bytes[0] = (byte) ((d & 0xFF000000) >> 24);
		bytes[1] = (byte) ((d & 0xFF0000) >> 16);
		bytes[2] = (byte) ((d & 0xFF00) >> 8);
		bytes[3] = (byte) (d & 0xFF);
		return bytes;
	}

	public void readMessage(PeerRecord peer, int myID) throws IOException {
		byte[] temp = new byte[5];
		peer.inStream.read(temp,0,5);

		if(temp[0] == (byte)'P' && temp[1] == (byte)'2' && temp[2] == (byte)'P' && temp[3] =='F' && temp[4] == (byte)'I') {
			byte[] garbage = new byte[28];
			peer.inStream.read(garbage,0,28);
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

		/*//length segment
		byte[] lengthByte = new byte[4];
		int bytesRcvd;
		int totalBytesRcvd = 0;		
		while(totalBytesRcvd < 4){
			bytesRcvd = peer.inStream.read(lengthByte, totalBytesRcvd, 4 - totalBytesRcvd);
			totalBytesRcvd += bytesRcvd;
		}
		length = bytesToInt(lengthByte,4); //convert to int

		//type segment
		byte[] typebyte = new byte[1];
		totalBytesRcvd = 0;
		while(totalBytesRcvd < 1){
			bytesRcvd = peer.inStream.read(typebyte, totalBytesRcvd, 1 - totalBytesRcvd);
			totalBytesRcvd += bytesRcvd;
		}
		type = bytesToInt(typebyte,1); //convert to int
		
		//type segment
		if(length > 4){
			payload = new byte[length - 4]; 
		}
		else{
			payload = null;
		}
		totalBytesRcvd = 0;
		while(totalBytesRcvd < length - 4){
			bytesRcvd = peer.inStream.read(payload, totalBytesRcvd, length - 4 - totalBytesRcvd);
			totalBytesRcvd += bytesRcvd;
		}*/
	}

	public void sendMessage(PeerRecord peer) throws IOException {
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

		peer.outStream.write(msg,0,msg.length);
		if(payload != null){
			peer.outStream.write(payload);
		}
		peer.outStream.flush();
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

	public int readHandShake(Socket s) throws IOException {
		Scanner in = new Scanner(s.getInputStream());
		int readID = in.nextInt();

		return readID;
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
			peer.sentHandShake = true;
		}
		catch(IOException e) {
			System.out.println("Error with sending handshake. Could not write to stream");
		}
	}

	public int getID(){
		return ID;
	}
}