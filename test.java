class test {
	public static void main(String args[]) throws Exception {
		PeerProcess p1 = new PeerProcess(1001);
		PeerProcess p2 = new PeerProcess(1002);
		Thread t1 = new Thread(p1);
		Thread t2 = new Thread(p2);
		t1.start();
		t2.start();
	}
}