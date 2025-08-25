package apkeep.rules;

public class ForwardingRule extends Rule {
	long dstIP;
	int maskLen;

	public ForwardingRule(int match_bdd, long dstip, int len, String port, int priority) {
		super(match_bdd, priority, port);
		dstIP = dstip;
		maskLen = len;
	}
	
	public ForwardingRule(int match_bdd, int hit_bdd, long dstip, int len, String port, int priority) {
		super(match_bdd, hit_bdd, priority, port);
		dstIP = dstip;
		maskLen = len;
	}

	public long getDstIP() {
		return dstIP;
	}

	public int getMaskLen() {
		return maskLen;
	}
}
