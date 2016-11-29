class test {
	public static void main(String args[]) throws Exception {
		PeerProcess p1 = new PeerProcess(1002);
		PeerProcess p2 = new PeerProcess(1003);
		//PeerProcess p3 = new PeerProcess(1003);
		Thread t1 = new Thread(p1);
		Thread t2 = new Thread(p2);
		//Thread t3 = new Thread(p3);
		t1.start();
		t2.start();
		//t3.start();
	}
}