import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;

public class FileManager {
	private RandomAccessFile file;
	private int numberOfPieces;
	private int pieceSize;
	private int fileSize;

	public FileManager(int numberOfPieces, int pieceSize, int fileSize, String fileName, int peerID) throws FileNotFoundException {
		String directory = "peer_" + peerID + "/";
		File dir = new File(directory);
		if(!dir.exists()) {
			dir.mkdirs();
			System.out.println("Peer:" + peerID + " creating directory " + directory);
		}
		file = new RandomAccessFile(directory + fileName, "rw");
		
		this.numberOfPieces = numberOfPieces;
		this.pieceSize = pieceSize;
		this.fileSize = fileSize;
	}

	public Pieces getPiece(int index) throws IOException {
		int length = 0;
		if(index == numberOfPieces - 1) {
			length = fileSize - pieceSize*index; //make the length the size of the remaing bytes if this is the last index
		}
		else {
			length = pieceSize;
		}

		int offSet = index*pieceSize; //get the location in the file to start reading from
		byte[] bytes = new byte[length]; 

		file.seek(offSet);
		for(int i = 0; i < length; i++) {
			bytes[i] = file.readByte(); //read all the bytes for the piece into the bytes array
		}

		Pieces piece = new Pieces(index, bytes); //create a piece from index and bytes array
		return piece;
	}

	public void writePiece(Pieces piece) throws IOException {
		int offSet = piece.getPieceIndex()*pieceSize; //get the location in the file to read too
		int length = piece.getPieceBytes().length; //get the length of the piece

		byte[] bytes = piece.getPieceBytes(); 
		file.seek(offSet);
		for(int i = 0; i < length; i++) {
			file.writeByte(bytes[i]); //write to the file's bytes
		}
	}
}