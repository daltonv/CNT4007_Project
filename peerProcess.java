import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class PeerProcess implements Runnable{
	private Config config;
	private int myID;

	public PeerProcess(int myID) throws UnknownHostException, IOException {
		this.myID = myID;
		this.config = new Config("common.cfg", "peerinfo.cfg");
		//TODO bitfield class init
		//TODO record class init
	}

	public void run() {
		try {
			for (int i=0; i<config.getPeerCount(); i++) {
				Socket downSocket = new Socket(config.getHosts().get(i),config.getPorts(i));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String args[]) throws Exception {
		PeerProcess p = new PeerProcess(Integer.parseInt(args[0]));
		Thread t = new Thread(p);
		t.start();
	}

}
