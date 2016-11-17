import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;

public class Pieces {
	private byte[][] piecesStruct;

	public Pieces(int numberOfPieces, int pieceSize, String fileName) {
		piecesStruct = new byte[numberOfPieces][piecesSize + 4]; //array holding payloads plus an index byte
		File tempFile = new File(fileName);

		FileInputStream fs = new FileInputStream(tempFile);
		for(int i = 0; i<numberOfPieces; i++) {
			ByteBuffer b = ByteBuffer.allocate(4); //allocate bytebuffer of index byte
			b.putInt(0,i) //make index into a byte
			System.arraycopy(b.array(),0,pieces[i],0,4); //place byte array into piecesStruct

			//fill the rest of the bytes with file contents, if any
			for(int j = 4; j<pieceSize+4; j++) {
				pieces[i][j] = (byte)fs.read();
			}
		}
		fs.close(); //close the file stream
	} 
}