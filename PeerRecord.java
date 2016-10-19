import java.net.*;

/*
Class that will hold information about the local processes's Peers
*/
public class PeerRecord {
	
	private final int ID;
	private final Socket downSocket;
	
	public PeerRecord(int ID, Socket downSocket) {
		
		this.ID = ID;
		this.downSocket = downSocket;		
	}
	
	public int getID() {
		return ID;
	}
	
	public Socket getDownSocket() {
		return downSocket;
	}
}