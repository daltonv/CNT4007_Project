import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class PeerProcess implements Runnable{
	private Config config;
	private int myID;
	private int myIndex;
	private PeerRecord[] neighbors;
	private int neighborsCount;

	public PeerProcess(int myID) throws UnknownHostException, IOException {
		this.myID = myID;
		this.config = new Config("common.cfg", "peerinfo.cfg");
		this.neighborsCount = config.getPeerCount();
		neighbors = new PeerRecord[neighborsCount];
		//TODO bitfield class init
	}

	public void handshake(PeerRecord peer) throws Exception {
		Socket s = peer.getDownSocket(); //get peers socket
		
		Message shake = new Message(); //create a message for handshacking
		shake.setID(myID); //set the message ID to myID
		shake.sendHandShake(s); //send the message out

		shake.readHandShake(s); //read the handshake message

		if(shake.getID() != peer.getID()) {
			throw new Exception("Failed to handshake!");
		}

		System.out.println(this.myID + " receives hand shake message");
	}

	public void run() {
		try {
			for (int i=0; i<config.getPeerCount(); i++) {
				if(config.getIDs().get(i) == myID) {
					int myIndex = i;
					break;
				}
			}

			for (int i=0; i<config.getPeerCount(); i++) {
				if(i != myIndex) {
					Socket downSocket = new Socket(config.getHosts().get(i),config.getPorts(i));
					
					neighbors[i] = new PeerRecord(config.getIDs().get(i),downSocket);
				}
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String args[]) throws Exception {
		PeerProcess p = new PeerProcess(Integer.parseInt(args[0]));
		Thread t = new Thread(p);
		t.start();
	}

}
