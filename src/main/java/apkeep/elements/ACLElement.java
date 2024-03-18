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
package apkeep.elements;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import apkeep.core.ChangeItem;
import apkeep.rules.FilterRule;
import apkeep.rules.Rule;
import apkeep.utils.Logger;
import common.ACLRule;
import common.BDDACLWrapper;

public class ACLElement extends Element {
	
	private LinkedList<Rule> acl_rule;
	
	public ACLElement(String ename) {
		super(ename);
		acl_rule = new LinkedList<>();
	}

	@Override
	public void initialize() {
		// initialize the rule list with a default deny rule
		FilterRule rule = new FilterRule(BDDACLWrapper.BDDTrue,BDDACLWrapper.BDDTrue,"deny", -1);
		acl_rule.add(rule);
		
		// initialize the AP set for port deny
		String deny_port = "deny";
		HashSet<Integer> alltrue = new HashSet<Integer>();
		alltrue.add(BDDACLWrapper.BDDTrue);
		port_aps_raw.put(deny_port, alltrue);
		
		// initialize the AP set for port permit
		String permit_port = "permit";
		HashSet<Integer> allfalse = new HashSet<Integer>();
		port_aps_raw.put(permit_port, allfalse);
	}

	@Override
	public Rule encodeOneRule(String rule) {
		String[] tokens = rule.split(" ");
		ACLRule r = new ACLRule(rule.substring(tokens[0].length() + tokens[1].length() + tokens[2].length() + 3));
		
		int match_bdd = apk.encodeACLBDD(r);
		return new FilterRule(match_bdd, r);
	}

	@Override
	public List<ChangeItem> insertOneRule(Rule rule) throws Exception {
		List<ChangeItem> change_set = identifyChangesInsert(rule, acl_rule);
		port_aps_raw.putIfAbsent(rule.getPort(), new HashSet<Integer>());
		return change_set;
	}

	@Override
	public List<ChangeItem> removeOneRule(Rule rule) throws Exception {
		int index = findRule(rule);
		if(index == acl_rule.size()) {
			Logger.logInfo("Rule not found " + rule.toString());
			return new ArrayList<ChangeItem>();
		}
		Rule rule_to_remove = acl_rule.get(index);
		// remove if rule hits no packets
		if(rule_to_remove.getHit_bdd() == BDDACLWrapper.BDDFalse) {
			removeRule(index);
			Logger.logInfo("hidden rule deleted");
			return new ArrayList<ChangeItem>();
		}
		
		List<ChangeItem> change_set = identifyChangesRemove(rule_to_remove, acl_rule);
		removeRule(index);
		return change_set;
	}

	private int findRule(Rule rule) {
		int index = 0;
		for(Rule r : acl_rule) {
			if(r.equals(rule)) return index;
			index++;
		}
		return index;
	}
	
	private void removeRule(int index) {
		bdd.deref(acl_rule.get(index).getMatch_bdd());
		acl_rule.remove(index);
	}
	@Override
	protected int tryMergeIfNATElement(int delta) {
		return delta;
	}
}
