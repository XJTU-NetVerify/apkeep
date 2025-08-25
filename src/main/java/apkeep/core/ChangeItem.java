package apkeep.core;

public class ChangeItem {
	private String from_port;
	private String to_port;
	private int delta;
	
	public ChangeItem(String port1, String port2, int packets) {
		from_port = port1;
		to_port = port2;
		delta = packets;
	}

	public String getFrom_port() {
		return from_port;
	}

	public String getTo_port() {
		return to_port;
	}

	public int getDelta() {
		return delta;
	}
	
	public String toString() {
		return "["+delta+"]"+from_port +" -> "+ to_port;
	}
}
