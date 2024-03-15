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
package apkeep.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import apkeep.elements.ACLElement;
import apkeep.elements.Element;
import apkeep.elements.NATElement;
import apkeep.utils.Logger;
import apkeep.utils.Parameters;
import common.ACLRule;
import common.BDDACLWrapper;
import common.PositionTuple;
import common.Utility;
import jdd.bdd.BDD;

/**
 * Computes Atomic Predicates using BDDs.
 */
public class APKeeper {
	private final static boolean MergeAP = Parameters.MergeAP;
	
	public static BDDACLWrapper bddengine;
	private Set<Integer> AP;
	
	private Map<String, Element> elements;
	int element_number = 0;
	HashMap<String, Integer> element_ids;
	HashMap<Integer, String> id_element;
	HashSet<String> nat_names;
	
	HashMap<Integer, ArrayList<String>> ap_ports;
	HashMap<ArrayList<String>, HashSet<Integer>> ports_aps;	
	HashSet<ArrayList<String>> ports_to_merge;
	
	private int mergeable_aps = 0;
	HashMap<String,Integer> cachePrefixBDD;
	
	public APKeeper(BDDACLWrapper bdd_engine) {
		bddengine = bdd_engine;
		AP = new HashSet<Integer>();
		
		element_ids = new HashMap<String, Integer>();
		id_element = new HashMap<>();
		elements = new HashMap<String, Element>();
		nat_names = new HashSet<String>();
		
		ap_ports = new HashMap<Integer, ArrayList<String>>();
		ports_aps = new HashMap<ArrayList<String>, HashSet<Integer>>();	
		ports_to_merge = new HashSet<ArrayList<String>>();
		
		cachePrefixBDD = new HashMap<>();
	}
	
	public void addElement(String ename, Element e) {
		elements.put(ename, e);
		element_ids.put(ename, element_number);
		id_element.put(element_number, ename);
		element_number ++;
	}
	
	public void initialize() {
		int element_number = elements.keySet().size();
		ArrayList<String> ports = new ArrayList<String>(element_number);
		HashSet<Integer> aps = new HashSet<Integer>();
		
		for (int i=0; i<element_number; i++) {
			ports.add("default");
		}
		for(String ename : elements.keySet()) {
			Element e = elements.get(ename);
			int element_id = element_ids.get(ename);
			if (e instanceof ACLElement) {
				ports.set(element_id, "deny");
			}
			else if (e instanceof NATElement) {
				nat_names.add(ename);
			}
		}
		
		ap_ports.put(BDDACLWrapper.BDDTrue, ports);
		
		aps.add(BDDACLWrapper.BDDTrue);
		ports_aps.put(ports, aps);
		
		AP.add(BDDACLWrapper.BDDTrue);
	}
	
	public boolean hasAP(int ap){
		return AP.contains(ap);
	}
	
	public int getAPNum() {
		return AP.size();
	}
	
	public Set<PositionTuple> getHoldPorts(int ap) {
		if(!AP.contains(ap)) Logger.logError("Error in finding edges for ap " + ap);
		
		Set<PositionTuple> pts = new HashSet<>();
		for(int index=0; index < ap_ports.get(ap).size();index++) {
			pts.add(new PositionTuple(id_element.get(index), ap_ports.get(ap).get(index)));
		}
		
		return pts;
	}
	
	/**
	 * 
	 * @param predicatebdd
	 * @return if the acl is true or force, return the set containing the acl itself;
	 *         otherwise, return an ap expression
	 */
	public HashSet<Integer> getAPExp(int PredicateBDD){
		HashSet<Integer> apexp = new HashSet<Integer> ();
		// get the expression
		if(PredicateBDD == BDDACLWrapper.BDDFalse)
		{
			return apexp;
		}
		else if ( PredicateBDD == BDDACLWrapper.BDDTrue)
		{
			return new HashSet<Integer>(AP);
		}

		for(int oneap : AP)
		{
			if(bddengine.getBDD().and(oneap, PredicateBDD) != BDDACLWrapper.BDDFalse)
			{
				apexp.add(oneap);
			}
		}
		return apexp;
	}
	
	/**
	 * add one predicate and recompute APs
	 */
	public void addPredicate (int pred){

		BDD thebdd = bddengine.getBDD();

		int predneg = thebdd.not(pred);
		thebdd.ref(predneg);

		HashSet<Integer> oldList = new HashSet<Integer>(AP);

		for (int oldap : oldList) {
			int parta = thebdd.and(pred, oldap);
			if(parta != BDDACLWrapper.BDDFalse) {
				int partb = thebdd.and(predneg, oldap);
				if (partb != BDDACLWrapper.BDDFalse) {
					updateSplitAP(oldap, parta, partb);
				}
			}
		}
	}
	
	public int encodePrefixBDD(long destip, int prefixlen) {
		String prefix = destip + " " + prefixlen;
		if (cachePrefixBDD.containsKey(prefix)) {
			return cachePrefixBDD.get(prefix);
		}
		else {
			int prefixbdd = bddengine.encodeDstIPPrefix(destip, prefixlen);
			cachePrefixBDD.put(prefix, prefixbdd);
			return prefixbdd;
		}
	}
	
	public void removePrefixBDD(long destip, int prefixlen) {
		String prefix = destip + " " + prefixlen;
		if (cachePrefixBDD.containsKey(prefix)) {
			cachePrefixBDD.remove(prefix);
		}
	}
	
	public int encodeACLBDD(ACLRule rule) {
		return bddengine.ConvertACLRule(rule);
	}
	
	public void updateSplitAP(int origin, int parta, int partb) {
		Logger.logDebugInfo("Splitting "+origin+" -> " +parta+" + "+partb);
		if(!AP.contains(origin)) {
			Logger.logError("Error: origin AP " + origin + " not found");
		}

		AP.remove(origin);
		AP.add(parta);
		AP.add(partb);
		
		if(ap_ports.containsKey(origin)){
			ArrayList<String> ports = ap_ports.get(origin);
			ap_ports.put(parta, ports);
			ap_ports.put(partb, (ArrayList<String>)ports.clone());
			
			// update each element's AP set
			for(String elementname : elements.keySet()){
				String port = ports.get(element_ids.get(elementname));
				elements.get(elementname).updateAPSplit(port, origin, parta, partb);
			}
			
			ap_ports.remove(origin);
			
			if (MergeAP) {
				ports_aps.get(ports).remove(origin);
				ports_aps.get(ports).add(parta);
				ports_aps.get(ports).add(partb);
				mergeable_aps ++;
			}
		}
		
		bddengine.ref(parta);
		bddengine.ref(partb);
		bddengine.deref(origin);
		
		if(!ap_ports.keySet().equals(AP)) {
			Logger.logError("AP Not equalvalent!");
		}
	}
	
	public void updateTransferAP(PositionTuple pt1, PositionTuple pt2, int ap) {
		if(!ap_ports.containsKey(ap)){
			Logger.logError("Error: AP edge reference not found " + ap);
		}
		
		ArrayList<String> ports = ap_ports.get(ap);
//		System.out.println(ports);

		if (!MergeAP) {
			ports.set(element_ids.get(pt2.getDeviceName()), pt2.getPortName());
		}
		else {
			HashSet<Integer> aps = ports_aps.get(ports);
			aps.remove(ap);
			
			// the ap set becomes empty, then remove the ports entry
			if (aps.isEmpty()) { 
				ports_aps.remove(ports);
			}
			// the ap set is non-empty, then clone the ports
			else {
				mergeable_aps --;
				
				// the ap set has one ap, then do not merge it
				if (aps.size() == 1) {
					ports_to_merge.remove(ports);
				}
				
				ports = new ArrayList<String>(ports);
			}
			
			ports.set(element_ids.get(pt2.getDeviceName()), pt2.getPortName());
			ap_ports.put(ap, ports);
			
			if(!ports_aps.containsKey(ports)) {
				ports_aps.put(ports, new HashSet<Integer>());
			}
			aps = ports_aps.get(ports);
			if (!aps.isEmpty()) {
				mergeable_aps ++;
			}
			aps.add(ap);
			if (aps.size() == 2) {
				ports_to_merge.add(ports);
			}
		}
//		System.out.println("Transfer "+ap+ ": "+pt1+" -> "+pt2);
//		for(ArrayList<String> ports1 : ports_to_merge) {
//			Set<Integer> aps = ports_aps.get(ports1);
//			System.out.println(ports1+":");
//			System.out.println(aps);
//			if(aps.size() < 2) Logger.logError("Find Singleton!");
//		}
//		if(!ports_aps.keySet().containsAll(ports_to_merge)) {
//			System.out.println("---------");
//			for(ArrayList<String> ports1 : ports_aps.keySet()) System.out.println(ports1);
//			System.out.println("---------");
//			for(ArrayList<String> ports1 : ports_to_merge) System.out.println(ports1);
//			System.out.println("---------");
//			Logger.logError("Ports Not equalvalent!");
//		}
	}
	
	public boolean checkRWMergable(int ap1, int ap2) {
		if (nat_names.isEmpty()) return true;
		for (String nat_name : nat_names) {
			NATElement nat = (NATElement) elements.get(nat_name);
			if (!nat.isMergable(ap1, ap2)) return false;
		}
		return true;
	}
	
	public boolean checkRWMergable(HashSet<Integer> aps) {
		if (nat_names.isEmpty()) return true;
		for (String nat_name : nat_names) {
			NATElement nat = (NATElement) elements.get(nat_name);
			if (!nat.isMergable(aps)) return false;
		}
		return true;
	}
	
	public boolean isMergeable() {
		if(AP.size() > Parameters.TOTAL_AP_THRESHOLD 
				&& mergeable_aps > Parameters.LOW_MERGEABLE_AP_THRESHOLD) return true;
		if(mergeable_aps > Parameters.HIGH_MERGEABLE_AP_THRESHOLD) return true;
		return false;
	}
	
	public int tryMergeAP(int ap) {
		if (!MergeAP) return ap;
		
		ArrayList<String> ports = ap_ports.get(ap);
		HashSet<Integer> aps = ports_aps.get(ports);
		if (aps.size()>1) {
			for(int one_ap: aps) {
				if (one_ap == ap) continue;
				if (!checkRWMergable(one_ap, ap)) continue;
				int merged_ap = bddengine.or(ap, one_ap);
				updateMergeAP(one_ap, ap, merged_ap);
				return merged_ap;
			}
		}
		return ap;
	}
	
	public void tryMergeAPBatch() {
		if (ports_to_merge.isEmpty()) return;
		
		for (ArrayList<String> ports : new ArrayList<>(ports_to_merge)) {
			HashSet<Integer> aps = ports_aps.get(ports);
			if(aps.size()<2) Logger.logError("Merge singleton!");
			if (!checkRWMergable(aps)) continue;
			
			int[] apsarr = aps.stream().mapToInt(Number::intValue).toArray();
			int merged_ap = bddengine.OrInBatch(apsarr);
			mergeable_aps = mergeable_aps - aps.size() + 1;
			updateMergeAPBatch(merged_ap, aps);
			ports_to_merge.remove(ports);
		}
	}
	
	public void updateMergeAP(int ap1, int ap2, int merged_ap) {
		Logger.logDebugInfo("Merging "+ap1+" + "+ap2+" -> " +merged_ap);
		if(!AP.contains(ap1) || !AP.contains(ap2)) {
			Logger.logError("Error: origin APs " + ap1 + " or "+ ap2 + " not found");
		}
		AP.remove(ap1);
		AP.remove(ap2);
		AP.add(merged_ap);
		
		ArrayList<String> ports = ap_ports.get(ap1);
		for(String elementname : elements.keySet()){
			String port = ports.get(element_ids.get(elementname));
			elements.get(elementname).updateAPSetMerge(port, merged_ap, ap1, ap2);
		}
		ap_ports.remove(ap1);
		ap_ports.remove(ap2);
		ap_ports.put(merged_ap, ports);
		
		HashSet<Integer> aps = ports_aps.get(ports);
		aps.remove(ap1);
		aps.remove(ap2);
		aps.add(merged_ap);
		
		bddengine.deref(ap1);
		bddengine.deref(ap2);
		
		if(!ap_ports.keySet().equals(AP)) {
			Logger.logError("Not equalvalent!");
		}
	}
	
	public void updateMergeAPBatch (int merged_ap, HashSet<Integer> aps)
	{
		Logger.logDebugInfo("Merging "+aps+" -> " +merged_ap);
		if(!AP.containsAll(aps)) {
			Logger.logError("Error: origin APs " + aps + " not found");
		}
		
		AP.removeAll(aps);
		AP.add(merged_ap);
		
		ArrayList<String> ports = ap_ports.get(aps.toArray()[0]);
		ap_ports.put(merged_ap, ports);
		for(String elementname : elements.keySet()){
			String port = ports.get(element_ids.get(elementname));
			elements.get(elementname).updateAPSetMergeBatch(port, merged_ap, aps);
		}
		for (int ap : aps) {
			bddengine.deref(ap);
			ap_ports.remove(ap);
		}
		
		aps.clear();
		aps.add(merged_ap);
		
		if(!ap_ports.keySet().equals(AP)) {
			Logger.logError("Not equalvalent!");
		}
	}
}
