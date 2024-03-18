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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import apkeep.core.APKeeper;
import apkeep.core.ChangeItem;
import apkeep.exception.APNotFoundException;
import apkeep.exception.APSetNotFoundException;
import apkeep.exception.BDDNotFalseException;
import apkeep.rules.Rule;
import apkeep.utils.Logger;
import common.BDDACLWrapper;
import common.PositionTuple;

public abstract class Element {
	
	protected String name;
	
	protected static BDDACLWrapper bdd;
	protected APKeeper apk;
	
	protected Map<String, Set<Integer>> port_aps_raw;
	
	public Element(String ename) {
		name = ename;
		port_aps_raw = new HashMap<>();
	}

	public static void setBDDWrapper(BDDACLWrapper bdd_engine) {
		bdd = bdd_engine;
		
	}
	
	public void setAPC(APKeeper theapk) {
		apk = theapk;
		apk.addElement(name, this);
	}
	
	public abstract void initialize();
	public abstract Rule encodeOneRule(String rule);
	public abstract List<ChangeItem> insertOneRule(Rule rule) throws Exception;
	public abstract List<ChangeItem> removeOneRule(Rule rule) throws Exception;

	protected abstract int tryMergeIfNATElement(int delta);
	
	public String getName() {
		return name;
	}
	
	protected List<ChangeItem> identifyChangesInsert(Rule rule, ArrayList<Rule> affected_rules) throws Exception {
		// set bdd for the inserted rule
		List<ChangeItem> change_set = new ArrayList<>();
		
		int hit_bdd = bdd.ref(rule.getMatch_bdd());
		int bdd_to_change = BDDACLWrapper.BDDFalse;
		boolean inserted = false;
		Iterator<Rule> it2 = affected_rules.iterator();

		while (it2.hasNext()) {
			Rule item = it2.next();
			if (item.getPriority() > rule.getPriority()) {
				hit_bdd = bdd.diffTo(hit_bdd, item.getMatch_bdd());
				if (hit_bdd == BDDACLWrapper.BDDFalse) {
					return change_set;
				}
			}
			else {
				if(!inserted) {
					bdd_to_change = bdd.ref(hit_bdd);
					inserted = true;
				}

				if(bdd_to_change == BDDACLWrapper.BDDFalse) {
					break;
				}

				int delta = bdd.and(item.getHit_bdd(), bdd_to_change);
				if (delta == BDDACLWrapper.BDDFalse) {
					continue;
				}
				item.setHit_bdd(bdd.diffTo(item.getHit_bdd(), delta));
				bdd_to_change = bdd.diffTo(bdd_to_change, delta);
				if (!item.getPort().equals(rule.getPort())) {
					ChangeItem change_item = new ChangeItem(item.getPort(), rule.getPort(), delta);
					change_set.add(change_item);
				}
			}
		}
		if (bdd_to_change != BDDACLWrapper.BDDFalse) {
			throw new BDDNotFalseException(bdd_to_change);
		}
		
		rule.setHit_bdd(hit_bdd);
		
		return change_set;
	}
	protected List<ChangeItem> identifyChangesInsert(Rule rule, LinkedList<Rule> affected_rules) throws Exception {
		// set bdd for the inserted rule
		List<ChangeItem> change_set = new ArrayList<>();
		
		int hit_bdd = bdd.ref(rule.getMatch_bdd());
		int bdd_to_change = BDDACLWrapper.BDDFalse;
		Rule default_rule = affected_rules.getLast();
		boolean inserted = false;

		int cur_position = 0;
		Iterator<Rule> it2 = affected_rules.iterator();
		while (it2.hasNext()) {
			Rule item = it2.next();
			if (item.getPriority() > rule.getPriority()) {
				if (hit_bdd != BDDACLWrapper.BDDFalse) {
					hit_bdd = bdd.diffTo(hit_bdd, item.getMatch_bdd());
				}
				cur_position++;
			}
			else {
				if(!inserted) {
					// fast check whether the default rule is the only rule affected
					int temp = bdd.diff(hit_bdd, default_rule.getHit_bdd());
//					System.out.println(hit_bdd +" - "+default_rule.getHit_bdd()+" = "+temp);
					if (temp == BDDACLWrapper.BDDFalse) {
						default_rule.setHit_bdd(bdd.diffTo(default_rule.getHit_bdd(), hit_bdd));
						if (!default_rule.getPort().equals(rule.getPort())) {
							ChangeItem change_item = new ChangeItem(default_rule.getPort(), rule.getPort(), hit_bdd);
							change_set.add(change_item);
						}
						break;
					}
					bdd.deref(temp);
					
					bdd_to_change = bdd.ref(hit_bdd);
					inserted = true;
				}

				if(bdd_to_change == BDDACLWrapper.BDDFalse) {
					break;
				}

				int delta = bdd.and(item.getHit_bdd(), bdd_to_change);
				if (delta == BDDACLWrapper.BDDFalse) {
					continue;
				}
				item.setHit_bdd(bdd.diffTo(item.getHit_bdd(), delta));
				bdd_to_change = bdd.diffTo(bdd_to_change, delta);
				if (!item.getPort().equals(rule.getPort())) {
					ChangeItem change_item = new ChangeItem(item.getPort(), rule.getPort(), delta);
					change_set.add(change_item);
				}
			}
		}
		
		if (bdd_to_change != BDDACLWrapper.BDDFalse) {
			throw new BDDNotFalseException(bdd_to_change);
		}
		
		rule.setHit_bdd(hit_bdd);
		affected_rules.add(cur_position, rule);
		
		return change_set;
	}
	
	protected List<ChangeItem> identifyChangesRemove(Rule rule, ArrayList<Rule> affected_rules) throws Exception{
		ArrayList<ChangeItem> change_set = new ArrayList<ChangeItem>();
		affected_rules.remove(rule);
		
		int hit_bdd = bdd.ref(rule.getHit_bdd());
		Iterator<Rule> it2 = affected_rules.iterator();
		while (it2.hasNext() && hit_bdd != BDDACLWrapper.BDDFalse){
			Rule item = it2.next();
			if (item.getPriority() >= rule.getPriority()){
				continue;
			}
			int delta = bdd.and(hit_bdd, item.getMatch_bdd());
			if (delta != BDDACLWrapper.BDDFalse) {
				item.setHit_bdd(bdd.orTo(item.getHit_bdd(), delta));
				hit_bdd = bdd.diffTo(hit_bdd, delta);
				
				if (!item.getPort().equals(rule.getPort())) {
					ChangeItem change_item = new ChangeItem(rule.getPort(), item.getPort(), delta);
					change_set.add(change_item);
				}
			}
		}
	
		if (hit_bdd != BDDACLWrapper.BDDFalse) {
			throw new BDDNotFalseException(hit_bdd);
		}
		
		bdd.deref(rule.getHit_bdd());
		return change_set;
	}
	
	protected List<ChangeItem> identifyChangesRemove(Rule rule, LinkedList<Rule> affected_rules) throws Exception {
		List<ChangeItem> change_set = new ArrayList<ChangeItem>();
		int hit_bdd = bdd.ref(rule.getHit_bdd());
		
		Iterator<Rule> it = affected_rules.iterator();
		while (it.hasNext() && hit_bdd != BDDACLWrapper.BDDFalse){
			Rule item = it.next();
			if (item.getPriority() >= rule.getPriority()){
				continue;
			}
			int delta = bdd.and(hit_bdd, item.getMatch_bdd());
			if (delta != BDDACLWrapper.BDDFalse) {
				item.setHit_bdd(bdd.orTo(item.getHit_bdd(), delta));
				hit_bdd = bdd.diffTo(hit_bdd, delta);
				
				if (!item.getPort().equals(rule.getPort())) {
					ChangeItem change_item = new ChangeItem(rule.getPort(), item.getPort(), delta);
					change_set.add(change_item);
				}
			}
		}
		
		if (hit_bdd != BDDACLWrapper.BDDFalse) {
			throw new BDDNotFalseException(hit_bdd);
		}
		
		bdd.deref(rule.getHit_bdd());
		return change_set;
	}

	public Set<Integer> updatePortPredicateMap(List<ChangeItem> change_set) throws Exception{
		HashSet<Integer> moved_aps = new HashSet<Integer>();
		if(change_set.isEmpty()) return moved_aps;
		
		for(ChangeItem item : change_set) {
			
			String from_port = item.getFrom_port();
			String to_port = item.getTo_port();
			int delta = bdd.getBDD().ref(item.getDelta());
			
			// fast track: delta is one AP kept by from_port
			if(port_aps_raw.get(from_port).contains(delta)) {
				transferOneAP(from_port, to_port, delta);
				int merged_ap = tryMergeIfNATElement(delta);
				if (merged_ap != delta) {
					moved_aps.remove(delta);
					int another_ap = bdd.diff(merged_ap, delta);
					moved_aps.remove(another_ap);
					bdd.deref(another_ap);
				}
				moved_aps.add(merged_ap);
				bdd.getBDD().deref(merged_ap);
				continue;
			}
			
			// split AP when intersect
			Set<Integer> apset = new HashSet<Integer>(port_aps_raw.get(from_port));
			for(int ap : apset) {
				int intersect = bdd.and(delta, ap);
				if(intersect != BDDACLWrapper.BDDFalse) {
					if(intersect != ap) {
						int dif = bdd.diff(ap, intersect);
						apk.updateSplitAP(ap, dif, intersect);
						if (moved_aps.contains(ap)) {
							moved_aps.remove(ap);
							moved_aps.add(dif);
							moved_aps.add(intersect);
							Logger.logInfo("updated a moved AP");
						}
						bdd.deref(dif);
					}
					transferOneAP(from_port, to_port, intersect);
					int merged_ap = tryMergeIfNATElement(intersect); 
					if (merged_ap != intersect) {
						moved_aps.remove(intersect);
						int another_ap = bdd.diff(merged_ap, intersect);
						moved_aps.remove(another_ap);
						bdd.deref(another_ap);
					}
					moved_aps.add(merged_ap);
  					delta = bdd.diffTo(delta, intersect);
					bdd.getBDD().deref(merged_ap);
				}
				if(delta == BDDACLWrapper.BDDFalse) break;
			}
			
			apset = null;
			bdd.getBDD().deref(delta);
			if(delta != BDDACLWrapper.BDDFalse) {
				throw new BDDNotFalseException(delta);
			}
		}
		
		updateRewriteTableIfPresent();
		return moved_aps;
	}

	protected void transferOneAP(String from_port, String to_port, int delta) {
		port_aps_raw.get(from_port).remove(delta);
		port_aps_raw.get(to_port).add(delta);
		
		// update the AP edge reference		 
		try {
			apk.updateTransferAP(new PositionTuple(name, from_port), new PositionTuple(name, to_port), delta);
		} catch (APNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public void updateAPSplit(String portname, int origin, int parta, int partb) throws Exception {
		Set<Integer> apset = port_aps_raw.get(portname);
		if(!apset.contains(origin)) {
			throw new APNotFoundException(origin);
		}
		apset.remove(origin);
		apset.add(parta);
		apset.add(partb);
	}
	
	public void updateAPSetMerge(String port, int merged_ap, int ap1, int ap2) throws Exception {
		Set<Integer> apset = port_aps_raw.get(port);
		if(!apset.contains(ap1)) {
			throw new APNotFoundException(ap1);
		}
		if(!apset.contains(ap2)) {
			throw new APNotFoundException(ap2);
		}
		apset.remove(ap1);
		apset.remove(ap2);
		apset.add(merged_ap);
	}
	
	public void updateAPSetMergeBatch(String port, int merged_ap, HashSet<Integer> aps) throws Exception {
		Set<Integer> apset = port_aps_raw.get(port);
		if(!apset.containsAll(aps)) {
			throw new APSetNotFoundException(aps);
		}
		apset.removeAll(aps);
		apset.add(merged_ap);
	}
	
	protected void updateRewriteTableIfPresent() {
		// NAT Element should override this method
	}

	public Set<String> getPorts() {
		// TODO Auto-generated method stub
		return port_aps_raw.keySet();
	}
	
	public Set<Integer> getPortAPs(String port){
		return port_aps_raw.get(port);
	}
	
	public HashSet<Integer> forwardAPs(String port, Set<Integer> aps){
		HashSet<Integer> new_aps = new HashSet<>(aps);
		if(new_aps.remove(BDDACLWrapper.BDDTrue)) {
			return new HashSet<>(getPortAPs(port));
		}
//		System.out.println("*******");
//		System.out.println(name+" "+port);
//		System.out.println(port_aps_raw.keySet());
//		System.out.println(new_aps);
//		System.out.println(getPortAPs(port));
//		new_aps.retainAll(getPortAPs(port));
		return new_aps;
	}

	public static boolean hasOverlap(HashSet<Integer> aps1, HashSet<Integer> aps2) {
		if (aps1.isEmpty() || aps2.isEmpty()) {
			return false;
		}
		for (int ap1 : aps1) {
			for (int ap2: aps2) {
				int intersect = bdd.and(ap1, ap2);
				if (intersect != BDDACLWrapper.BDDFalse) {
					return true;
				}
			}
		}
		return false;
	}
}
