import java.io.*;
import java.net.*;
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

	public void readMessage(PeerRecord peer) throws IOException {
		byte[] lengthByte = new byte[4];
		int bytesRcvd;
		int totalBytesRcvd = 0;		
		while(totalBytesRcvd < 4){
			bytesRcvd = peer.inStream.read(lengthByte, totalBytesRcvd, 4 - totalBytesRcvd);
			totalBytesRcvd += bytesRcvd;
		}
		for(int i = 0; i<4; i++) {
			System.out.println(lengthByte[i]+", "+(char)lengthByte[i]);
		}
		if(lengthByte[0] == (byte)'P' && lengthByte[1] == (byte)'2' && lengthByte[2] == (byte)'P' && lengthByte[3] =='F') {
			byte[] garbage = new byte[28];
			peer.inStream.read(garbage,0,28);
			this.type = HANDSHAKE;
			System.out.println("Peer: " + peer.peerID + " received handshake");
			return;
		}
		length = lengthByte[0] & 0xff;


		byte[] typebyte = new byte[4];
		totalBytesRcvd = 0;
		while(totalBytesRcvd < 4){
			bytesRcvd = peer.inStream.read(typebyte, totalBytesRcvd, 4 - totalBytesRcvd);
			totalBytesRcvd += bytesRcvd;
		}
		type = typebyte[0] & 0xff;
		
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
		}
	}

	public void sendMessage(Socket s) throws IOException {
		if(payload == null){
			length = 4;
		}
		else{
			length = payload.length + 4;
		}
		OutputStream out = s.getOutputStream();
		out.write((byte)length);
		out.write((byte)type);
		if(payload != null){
			out.write(payload);
		}
		out.flush();
	}

	public int getType(){
		return type;
	}

	public int setType(int type){
		return type;
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