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

	private int type;
	private int length;
	private int ID;
	private byte[] payload;

	public Message(){
		type = 0;
		length = 0;
		payload = null;
	}

	public void readMessage(Socket s) throws IOException {
		InputStream in = s.getInputStream();
		byte[] lengthByte = new byte[4];
		int bytesRcvd;
		int totalBytesRcvd = 0;		
		while(totalBytesRcvd < 4){
			bytesRcvd = in.read(lengthByte, totalBytesRcvd, 4 - totalBytesRcvd);
			totalBytesRcvd += bytesRcvd;
		}
		length = lengthByte[0] & 0xff;

		byte[] typebyte = new byte[4];
		totalBytesRcvd = 0;
		while(totalBytesRcvd < 4){
			bytesRcvd = in.read(typebyte, totalBytesRcvd, 4 - totalBytesRcvd);
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
			bytesRcvd = in.read(payload, totalBytesRcvd, length - 4 - totalBytesRcvd);
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

	public void readHandShake(Socket s) throws IOException {
		Scanner in = new Scanner(s.getInputStream());
		ID = in.nextInt();
	}

	public void sendHandShake(Socket s) throws IOException {
		PrintWriter out = new PrintWriter(s.getOutputStream());
		out.println(ID);
		out.flush();
	}

	public int getID(){
		return ID;
	}
}