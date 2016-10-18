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
	private ArrayList<Integer> IDs;
	private ArrayList<String> hosts;
	private ArrayList<Integer> ports;
	private ArrayList<Boolean> hasFile;

	private int peerCount;

	public Config(String commonInfo, String peerInfo) throws FileNotFoundException {
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

		IDs = new ArrayList<Integer>();
		hosts = new ArrayList<String>();
		ports = new ArrayList<Integer>();
		hasFile = new ArrayList<Boolean>();

		int count = 0;
		while(in2.hasNextLine()) {
			String info = in2.nextLine();
			String[] split = info.split(" ");
			this.IDs.add(Integer.parseInt(split[0].trim()));
			this.hosts.add(split[1].trim());
			this.ports.add(Integer.parseInt(split[2].trim()));
			if (split[3].trim().equals("1")) {
				this.hasFile.add(true);
			} else {
				this.hasFile.add(false);
			}
			count++;
		}

		this.peerCount = count; 
	}
}
