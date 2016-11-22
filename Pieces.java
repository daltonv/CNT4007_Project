/*
	This class is just used to contain the byteStream for each piece along with
	an integer representing the "index" of the piece relative to the other pieces
*/


public class Pieces {
	private int pieceIndex;
	private byte[] pieceBytes;

	public Pieces(int pieceIndex, byte[] pieceBytes) {
		this.pieceIndex = this.pieceIndex;
		this.pieceBytes = pieceBytes;
	}

	public byte[] getPieceBytes() {
		return pieceBytes;
	}

	public int getPieceIndex() {
		return pieceIndex;
	} 
}