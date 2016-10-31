import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Config {
	//common config vara
	private int numberPreferredNeighbors;
	private int unchokingInterval;
	private int optomisticUnchokingInterval;
	private String fileName;
	private int fileSize;
	private int pieceSize;

	private int pieceCount;

	//peer config vars
	private HashMap<Integer, PeerRecord> peerMap = new HashMap<Integer,PeerRecord>();

	int myPortNumber;
	String myHost;
	boolean myHasFile;

	private int peerCount;

	public Config(String commonInfo, String peerInfo, int myID) throws FileNotFoundException {
		//read common config
		Scanner in1= new Scanner(new FileReader(commonInfo));
		this.numberPreferredNeighbors = Integer.parseInt(in1.nextLine().trim());
		this.unchokingInterval = Integer.parseInt(in1.nextLine().trim());
		this.optomisticUnchokingInterval = Integer.parseInt(in1.nextLine().trim());
		this.fileName = in1.nextLine().trim();
		this.fileSize = Integer.parseInt(in1.nextLine().trim());
		this.pieceSize = Integer.parseInt(in1.nextLine().trim());

		//calculate pieceCount
		this.pieceCount = this.fileSize/this.pieceSize;
		if(this.fileSize%this.pieceSize != 0) {
			this.pieceCount++;
		}

		//read peer config
		Scanner in2= new Scanner(new FileReader(peerInfo));

		int newPeerID = 0;
		String newHost = null;
		int newPort = 0;
		boolean newHasFile = false;

		int count = 0;
		while(in2.hasNextLine()) {
			String info = in2.nextLine();
			String[] split = info.split(" ");
			
			newPeerID = Integer.parseInt(split[0].trim());
			newHost = split[1].trim();
			newPort = Integer.parseInt(split[2].trim());
			if (split[3].trim().equals("1")) {
				newHasFile = true;
			} else {
				newHasFile = false;
			}

			if(newPeerID != myID) {
				PeerRecord newPeer = new PeerRecord(newPeerID,newHost,newPort,newHasFile); //create a new peer record
				this.peerMap.put(newPeerID, newPeer);	//store the peer record
			}
			else {
				this.myPortNumber = newPort;
				this.myHost = newHost;
				this.myHasFile = newHasFile;
			}
			count++;
		}
		
		this.peerCount = count; 
	}

	public int getFileSize() {
		return fileSize;
	}

	public int getPieceCount() {
		return pieceCount;
	}

	public int getPieceSize() {
		return pieceSize;
	}

	public int getNumberPreferredNeighbors() {
		return numberPreferredNeighbors;
	}

	public int getUnchokingInterval() {
		return unchokingInterval;
	}

	public int getOptomisticUnChokingInterval() {
		return optomisticUnchokingInterval;
	}

	public String getFileName() {
		return fileName;
	}

	public int getPeerCount() {
		return peerCount;
	}

	public int getMyPortNumber() {
		return myPortNumber;
	}

	public String getMyHost() {
		return myHost;
	}

	public boolean getHasFile() {
		return myHasFile;
	}

	public HashMap<Integer, PeerRecord> getPeerMap() {
		return peerMap;
	}
}
