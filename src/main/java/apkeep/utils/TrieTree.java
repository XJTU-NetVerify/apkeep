package apkeep.utils;

import apkeep.rules.ForwardingRule;
import common.BDDACLWrapper;

public class TrieTree {
	TrieTreeNode root;
	
	public TrieTree() {
		root = new TrieTreeNode(0, -1);
		TrieTreeNode child = new TrieTreeNode(1, 2);
		child.addRule(new ForwardingRule(BDDACLWrapper.BDDTrue,BDDACLWrapper.BDDTrue,0,0,"default",-1));
		
		child.setParent(root);
		root.setChild(2, child);
	}

	public TrieTreeNode insert(ForwardingRule rule) {
		long prefix = rule.getDstIP();
		int prefixlen = rule.getMaskLen();
		int[] prefixbin = PrefixLongToBin(prefix, prefixlen);
		return root.insert(prefixbin);
	}

	public TrieTreeNode search(ForwardingRule rule) throws Exception {
		long prefix = rule.getDstIP();
		int prefixlen = rule.getMaskLen();
		int[] prefixbin = PrefixLongToBin(prefix, prefixlen);
		return root.search(prefixbin);
	}
	
	public int[] PrefixLongToBin(long prefix, int prefixlen) 
	{
    	int[] bin = new int[32];
    	for (int i=0; i<32; i++) {
    		if (i >= prefixlen) {
    			bin[i] = 2;
    		}
    		else if((prefix & (1 << (32-i-1))) == 0) {
    			bin[i] = 0;
    		}
    		else {
    			bin[i] = 1;
    		}
    	}    
    	return bin;
	}
}
