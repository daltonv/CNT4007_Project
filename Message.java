import java.io,*;
import java.net,*;

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
	private byte[] payload;

	public Message(){
		type = 0;
		length = 0;
		payload = null;
	}

	public void read(Socket s) throws IOException{
		InputStream in = s.getInputStream();
		byte[] lengthByte = new byte[4];
		int bytesRcvd;
		int totalBytesRcvd = 0;		
		while(totalBytesRcvd < 4){
			bytesRcvd = in.read(lengthByte, totalBytesRcvd, 4 - totalBytesRcvd);
			totalBytesRcvd += bytesRcvd;
		}
		length = Conversion.BytesToInt(lengthByte);

		byte[] typebyte = new byte[4];
		totalBytesRcvd = 0;
		while(totalBytesRcvd < 4){
			bytesRcvd = in.read(typebyte, totalBytesRcvd, 4 - totalBytesRcvd);
			totalBytesRcvd += bytesRcvd;
		}
		type = Conversion.BytesToInt(typebyte);
	}
}
