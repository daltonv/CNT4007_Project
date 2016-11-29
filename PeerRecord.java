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
	public Socket socket;

	public BitField bitField;
	public int piecesSinceLastRound;
	
	public boolean sentHandShake;
	public boolean isInterested;
	public boolean interestedIn;
	public boolean isChoked;
	public boolean isOptimisticallyUnchoked;
	
	public PeerRecord(int peerID, String host, int portNumber, boolean hasFile, int pieceCount) {
		this.peerID = peerID;
		this.host = host;
		this.portNumber = portNumber;
		this.hasFile = hasFile;
		this.sentHandShake = false;
		this.bitField = new BitField(pieceCount);
		this.isInterested = false;
		this.piecesSinceLastRound = 0;
		this.isChoked = true;
		this.isOptimisticallyUnchoked = false;
		this.interestedIn = false;		
	}
}