
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.logging.*;


/*
	Logger for P2P project
	writes to log_peer_<peerID>.log
*/
public class P2PLogger{

	private int peerID;
	private Logger logger;
	private FileHandler fileHandler;
	private SimpleFormatter formatter;

	public P2PLogger(int peerID, FileHandler fileHandler) throws IOException {
		this.peerID = peerID;
		logger = Logger.getLogger("Peer" + this.peerID);
		logger.setLevel(Level.INFO);
		this.fileHandler = fileHandler;
		formatter = new SimpleFormatter();
		fileHandler.setFormatter(formatter);
		logger.addHandler(fileHandler);
	}

	
	// TCP Connection Logging

	// Log Connection To
	public void logTCPConnTo(int peerID){
		logger.info(": Peer " + this.peerID + " makes a connection to Peer " + peerID + ".\n");
	}

	//Log Connection From
	public void logTCPConnFrom(int peerID){
		logger.info(": Peer " + this.peerID + " is connected from Peer " + peerID + ".\n");
	}
	
	public void changePrefLog(ArrayList<Integer> prefNeighbors) {
			
			StringBuffer s = new StringBuffer();
			s.append(":Peer " + this.peerID + " has the preferred neighbores ");
			
			for (int i = 0; i < prefNeighbors.size(); i++) {
				if (i != (prefNeighbors.size() - 1)) {
					s.append(prefNeighbors.get(i) + ", ");
				} else {
					s.append(prefNeighbors.get(i) + "\n");
				}
			}
			
			logger.info(s.toString());
	}

	//Log change of optimistically unchoked neighbor
	public void logChangeOpt(int peerID){
		logger.info(" : Peer " + this.peerID + " has the optimistically unchoked neighbor " + peerID + ".\n");
	}

	//Log unchoking
	public void logUnchoking(int peerID){
		logger.info(" : Peer " + this.peerID + " is unchoked by " + peerID + ".\n");
	}

	//Log Choking
	public void logChoking(int peerID){
		logger.info(" : Peer " + this.peerID + " is choked by "  + peerID + ".\n");
	}

	
	//Log receiving 'have' message
	public void logReceiveHave(int peerID, int pieceIndex){
		logger.info(" : Peer " + this.peerID + " received the 'have' message from " + peerID + " for the piece " + pieceIndex + ".\n");
	}

	//Log receiving 'interested' message
	public void logReceiveInterested(int peerID){
		logger.info(" : Peer " + this.peerID + " received the 'interested' message from " + peerID + ".\n");
	}
	
	//Log receiving 'not interested' message
	public void logNotInterested(int peerID){
		logger.info(" : Peer " + this.peerID + " received the 'not interested' message from " + peerID + ".\n");
	}

	//Log downloading a piece
	public void logPieceDownload(int peerID, int pieceIndex, int numPieces){
		logger.info(" : Peer " + this.peerID + " has downloaded the piece " + pieceIndex + " from "  + peerID + ".\n" 
			+ "INFO:  : Now the number of pieces it has is " + numPieces + ".\n");
	}

	//Log completion of download
	public void logDownloadComp(){
		logger.info(" : Peer " + this.peerID + " has downloaded the complete file.\n");
	}


}