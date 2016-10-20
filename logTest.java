import java.io.*;
public class logTest {
    public static void main(String[] args){
    	try{
    		int myID = 1001;
    		int theirID = 1002;
    		P2PLogger myLogger = new P2PLogger(myID);
    		myLogger.logTCPConnTo(theirID);
    		myLogger.logTCPConnFrom(theirID);
    		myLogger.logChangeOpt(theirID);
    		myLogger.logUnchoking(theirID);
    		myLogger.logChoking(theirID);
    		myLogger.logReceiveHave(theirID, 8);
    		myLogger.logReceiveInterested(theirID);
    		myLogger.logNotInterested(theirID);
    		myLogger.logPieceDownload(theirID, 8, 56);
    		myLogger.logDownloadComp();
    	} catch	(Exception e){

    	}
    }
}
