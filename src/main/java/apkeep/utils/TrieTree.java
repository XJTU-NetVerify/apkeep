/**
 * APKeep
 * 
 * Copyright (c) 2020 ANTS Lab, Xi'an Jiaotong University. All rights reserved.
 * Developed by: PENG ZHANG and XU LIU.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * with the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimers.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimers in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the Xi'an Jiaotong University nor the names of the
 * developers may be used to endorse or promote products derived from this
 * Software without specific prior written permission.
 * 
 * 4. Any report or paper describing results derived from using any part of this
 * Software must cite the following publication of the developers: Peng Zhang,
 * Xu Liu, Hongkun Yang, Ning Kang, Zhengchang Gu, and Hao Li, APKeep: Realtime 
 * Verification for Real Networks, In 17th USENIX Symposium on Networked Systems
 * Design and Implementation (NSDI 20), pp. 241-255. 2020.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS WITH
 * THE SOFTWARE.
 */
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

	public TrieTreeNode search(ForwardingRule rule) {
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
