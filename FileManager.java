public class FileManager() {
	
	public FileManager(int peerID) {
		String directory = "./peer_" + String.valueOf(peerID) + "/";
		File root = new File(directory);
		if (!root.exists()) {
			root.mkdirs();
		}
	}
}