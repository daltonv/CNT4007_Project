import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class PeerProcess {
	private Config config;
	private int myID;

	public PeerProcess(int myID) throws UnknownHostException, IOException {
		this.myID = myID;
		this.config = new Config("common.cfg", "peerinfo.cfg");
		//TODO bitfield class init
		//TODO record class init
	}

	public void run() {
		for (int i=0; i<config.getPeerCount(); i++) {
			Socket = new Socket(config.getHosts(i),config.getPorts().get(i));

		}
	}

	public static void main(String args[]) throws Exception {
		PeerProcess peer = new PeerProcess(Integer.parseInt(args[0]));
	}

}
