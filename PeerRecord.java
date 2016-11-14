import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;

/*
Class that will hold information about the local processes's Peers
*/
public class PeerRecord {
	
	public int peerID;
	public String host;
	public int portNumber;
	public boolean hasFile;
	public DataInputStream inStream;
	public DataOutputStream outStream;
	public boolean sentHandShake;
	
	public PeerRecord(int peerID, String host, int portNumber, boolean hasFile) {
		this.peerID = peerID;
		this.host = host;
		this.portNumber = portNumber;
		this.hasFile = hasFile;
		sentHandShake = false;		
	}
}