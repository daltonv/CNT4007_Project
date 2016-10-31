import java.net.*;

/*
Class that will hold information about the local processes's Peers
*/
public class PeerRecord {
	
	public int peerID;
	public String host;
	public int portNumber;
	public boolean hasFile;
	
	public PeerRecord(int peerID, String host, int portNumber, boolean hasFile) {
		this.peerID = peerID;
		this.host = host;
		this.portNumber = portNumber;
		this.hasFile = hasFile;		
	}

}