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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import apkeep.checker.BlackholeChecker;
import apkeep.checker.LoopChecker;
import apkeep.checker.Property;
import apkeep.elements.ACLElement;
import apkeep.elements.Element;
import apkeep.elements.ForwardElement;
import apkeep.elements.NATElement;
import apkeep.exception.ElementNotFoundException;
import apkeep.rules.Rule;
import apkeep.utils.Evaluator;
import apkeep.utils.Logger;
import apkeep.utils.Parameters;
import common.BDDACLWrapper;
import common.PositionTuple;

public class Network {

	protected String name;
	protected boolean division_activated = false;
	protected HashMap<PositionTuple, HashSet<PositionTuple>> topology;
	
	/*
	 * The Port Predicate Map
	 * Each element has a set of ports, each of which is guarded by a set of predicates
	 */
	protected HashMap<String, Element> elements;
	
	/*
	 * The set of acl nodes, each corresponds to an application of an ACLElement
	 */
	protected HashSet<String> acl_node_names;
	private HashSet<String> nat_element_names;
	
	private Map<Integer, Set<Integer>> rewrite_table;
	
	/*
	 * The BDD data structure for encoding packet sets with Boolean formula
	 */
	protected BDDACLWrapper bdd_engine;
	
	/*
	 * The key data structure that handles the split and merge of predicates
	 */
	protected APKeeper fwd_apk; // the APKeeper for forwarding devices
	protected APKeeper acl_apk; // the APKeeper for ACL devices
	
	public Network(String network_name, String op_mode) {
		name = network_name;
		if(op_mode.equals("division")) division_activated = true;
		
		topology = new HashMap<>();
		elements = new HashMap<>();
		bdd_engine = new BDDACLWrapper();
		
		fwd_apk = null;
		acl_apk = null;
		
		acl_node_names = new HashSet<>();
		nat_element_names = new HashSet<>();
		
		rewrite_table = new HashMap<>();

		Element.setBDDWrapper(bdd_engine);
	}
	
	public void initializeNetwork(ArrayList<String> l1_links, 
			Set<String> devices,
			Map<String, Set<String>> device_acls,
			Map<String, Map<String, Set<String>>> vlan_ports,
			Map<String, Set<String>> device_nats) {
		constructTopology(l1_links); // create ForwardElements, if any
		addFWDElement(devices); // create ForwardElement not in topo, if any
		addNATs(device_nats); // create NATElement and insert it to topology, if any
		addACLs(device_acls); // create ACLElements, if any
		addVLANs(vlan_ports); // create VLAN to physic port mapping, if any
		initializeAPK();
	}
	
	/*
	 * initialize one/two instance(s) of APKeeper according to the operation mode
	 */
	private void initializeAPK()
	{
		fwd_apk = new APKeeper(bdd_engine);
		if(division_activated) acl_apk = new APKeeper(bdd_engine);

		for(Element e : elements.values()) {
			
			if(division_activated && e instanceof ACLElement) {
				e.setAPC(acl_apk);
			}
			else {
				e.setAPC(fwd_apk);
			}
			
			e.initialize();
		}
		
		fwd_apk.initialize();
		if(division_activated) acl_apk.initialize();
	}
	
	private void addElementOrNode(String name) {
		if(isACLNode(name)) {
			acl_node_names.add(name);
		}
		else if(!elements.containsKey(name)){
			ForwardElement e = new ForwardElement(name);
			elements.put(name, e);
		}
	}
	public boolean isACLNode(String name) {
		return name.endsWith("_in") || name.endsWith("_out");
	}
	private void addDirectedEdge(String d1, String p1, String d2, String p2) {
		PositionTuple pt1 = new PositionTuple(d1, p1);
		PositionTuple pt2 = new PositionTuple(d2, p2);
		// links are one way
		if(topology.containsKey(pt1))
		{
			topology.get(pt1).add(pt2);
		}else
		{
			HashSet<PositionTuple> newset = new HashSet<PositionTuple>();
			newset.add(pt2);
			topology.put(pt1, newset);
		}
	}
	
	public boolean containsPort(PositionTuple pt) {
		return topology.containsKey(pt);
	}
	
	public HashSet<PositionTuple> getConnectedPorts(PositionTuple pt){
		return topology.get(pt);
	}
	
	/*
	 * add ForwardElement from layer ONE topology
	 */
	private void constructTopology(ArrayList<String> l1_link)
	{	
		for(String linestr : l1_link)
		{
			String[] tokens = linestr.split(" ");
			addElementOrNode(tokens[0]);
			addElementOrNode(tokens[2]);
			addDirectedEdge(tokens[0], tokens[1], tokens[2], tokens[3]);
		}
	}
	
	private void addFWDElement(Set<String> devices) {
		if(devices == null) return;
		for(String element : devices) {
//			System.out.println(element);
			if(elements.containsKey(element)) continue;
			Element e = new ForwardElement(element);
			elements.put(element, e);
		}
	}
	
	private void addNATs(Map<String, Set<String>> device_nats) {
		if(device_nats == null) return;
		for(String device : device_nats.keySet()) {
			for(String port : device_nats.get(device)) {
				String nat_name = device+"_"+port;
				if(elements.containsKey(nat_name)) continue;
				NATElement nat = new NATElement(nat_name);
				elements.put(nat_name, nat);
				nat_element_names.add(nat_name);
				
				addDirectedEdge(device, port, nat_name, "inport");
			}
		}
	}
	
	private void addACLs(Map<String, Set<String>> device_acls) {
		if(device_acls == null) return;
		for(String device : device_acls.keySet()) {
			for(String aclname : device_acls.get(device)) {
				String element = device+"_"+aclname;
				Element e = new ACLElement(element);
				elements.put(element, e);
			}
		}
	}
	
	private void addVLANs(Map<String, Map<String, Set<String>>> vlan_ports) {
		if(vlan_ports == null) return;
		for(String device : vlan_ports.keySet()) {
			ForwardElement e = (ForwardElement) elements.get(device);
			e.addVlanPorts(vlan_ports.get(device));
		}
	}
	
	public Collection<Element> getAllElements() {
		return elements.values();
	}
	
	public Element getElement(String deviceName) {
		return elements.get(deviceName);
	}
	
	public Element getACLElement(String acl_node_name) {
		String[] tokens = acl_node_name.split("_");
		String acl_element_name = null;
		if (name.equals("stanford")) {
			acl_element_name = tokens[0]+"_"+tokens[1]+"_"+tokens[2];
		}
		else {
			acl_element_name = tokens[0]+"_"+tokens[1];
		}
		return elements.get(acl_element_name);
	}
	
	public String getForwardElement(String device) {
		if(elements.get(device) instanceof ACLElement) {
			return getForwardElementFromACL(device);
		}
		return device;
	}
	
	public String getForwardElementFromACL(String acl_name) {
		String[] tokens = acl_name.split("_");
		if (name.equals("stanford")) {
			return tokens[0]+"_"+tokens[1];
		}
		else {
			return tokens[0];
		}
	}
	
	public Set<String> getACLNodes() {
		return acl_node_names;
	}

	public Set<PositionTuple> getHoldPorts(int ap) throws Exception {
		return fwd_apk.getHoldPorts(ap);
	}
	
	public Set<PositionTuple> getACLHoldPorts(int ap) throws Exception {
		return acl_apk.getHoldPorts(ap);
	}
	
	public int getAPNum() {
		if(division_activated) {
			return fwd_apk.getAPNum()+acl_apk.getAPNum();
		}
		else {
			return fwd_apk.getAPNum();
		}
	}

	public Set<PositionTuple> mapElementPortIntoTopoPort(Set<PositionTuple> hold_ports) {
		Set<PositionTuple> pts = new HashSet<>();
		for(PositionTuple pt : hold_ports) {
			if(elements.get(pt.getDeviceName()) instanceof ACLElement) {
				pts.addAll(mapACLElementIntoNodes(pt));
			}
			else if(pt.getPortName().toLowerCase().startsWith("vlan")) {
				pts.addAll(mapVlanIntoPhyPorts(pt));
			}
			else {
				pts.add(pt);
			}
		}
		return pts;
	}

	private Set<PositionTuple> mapACLElementIntoNodes(PositionTuple pt) {
		Set<PositionTuple> pts = new HashSet<>();
		for(String aclname : acl_node_names) {
			if(aclname.startsWith(pt.getDeviceName())) {
				pts.add(new PositionTuple(aclname, pt.getPortName()));
			}
		}
		return pts;
	}
	public Set<PositionTuple> mapVlanIntoPhyPorts(PositionTuple pt) {
		Set<PositionTuple> pts = new HashSet<>();
		ForwardElement e = (ForwardElement) elements.get(pt.getDeviceName());
		for(String port : e.getVlanPorts(pt.getPortName())) {
			pts.add(new PositionTuple(pt.getDeviceName(), port));
		}
		return pts;
	}
	
	public Set<Integer> rewriteAllAPs(Set<Integer> origin_aps) {
		if(nat_element_names.isEmpty()) return origin_aps;
		if(!isRewritable(origin_aps)) return origin_aps;
		
		Set<Integer> rewrited_aps = new HashSet<>();
		while(true) {
			for(int ap : origin_aps) {
				rewrited_aps.addAll(rewriteAP(ap));
			}
			rewrited_aps.removeAll(origin_aps);
			if(rewrited_aps.isEmpty()) break;
			origin_aps.addAll(rewrited_aps);
			rewrited_aps.clear();
		}
		return origin_aps;
	}
	
	public boolean isRewritable(Set<Integer> aps) {
		rewrite_table.clear();
		for(String nat_name : nat_element_names) {
			NATElement nat = (NATElement) elements.get(nat_name);
			rewrite_table.putAll(nat.getRewrite_table());
		}
		
		HashSet<Integer> rewitable_aps = new HashSet<Integer>(rewrite_table.keySet());
		rewitable_aps.retainAll(aps);
		
		return !rewitable_aps.isEmpty();
	}
	
	public Set<Integer> rewriteAP(int ap){
		if(!rewrite_table.containsKey(ap)) return new HashSet<>();
		return rewrite_table.get(ap);
	}
	
	public void run(Evaluator eva, List<String> rules) throws Exception {
		
		eva.startExp();
		
		for(String rule : rules) {
			updateRule(eva, rule);
		}
		
		hardMergeAPBatch();
		
		eva.endExp(getAPNum());
	}
	
	public void run(Evaluator eva, String ruleFile) throws Exception {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(ruleFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		eva.startExp();
		
		String OneLine;
		while((OneLine = br.readLine()) != null) {
			String linestr = OneLine.trim();
			updateRule(eva, linestr);
		}

		hardMergeAPBatch();

		eva.endExp(getAPNum());
	}
	
	public void updateRule(Evaluator eva, String rule) throws Exception {
		Logger.logDebugInfo(rule);
		String[] tokens = rule.split(" ");
		String op = tokens[0];
		String type = tokens[1];
		String device = tokens[2];
		
		if(op.equals("-") && !eva.isInsertFinish()) {
			eva.setInsertFlag(true);
			hardMergeAPBatch();
			eva.setInsertAP(getAPNum());
		}
		/*
		 * Updating PPM
		 */
		eva.startUpdate();
		Set<Integer> moved_aps = updateRule(op, type, device, rule);
		if (moved_aps == null) return;
		eva.midUpdate();
		
		/*
		 * Verifying properties
		 */
		if (!moved_aps.isEmpty()) {
			int loops = checkLoop(type, device, moved_aps);
			int blackholes = checkBlackHole(type, device, moved_aps);

			eva.addLoops(loops);
			eva.addBlackholes(blackholes);
		}
		
		softMergeAPBatch();
		
		eva.endUpdate();
		eva.printUpdateResults(getAPNum());
	}
	
	private Set<Integer> updateRule(String op, String type, String device, String rule) throws Exception{
		String element_name = null;
		if(type.equals("nat")) {
			element_name = device+"_"+rule.split(" ")[3];
		}
		else{
			element_name = device;
		}
		Element e = elements.get(element_name);
		if (e == null) {
			throw new ElementNotFoundException(element_name);
		}
		
		List<ChangeItem> change_set = new ArrayList<>();
		
		/*
		 * Step 1. Encoding match fields
		 */
		Rule r = e.encodeOneRule(rule);

		/*
		 * Step 2. Identifying changes
		 */
		if (op.equals("+")){
			change_set = e.insertOneRule(r);
		}
		else if (op.equals("-")){
			change_set = e.removeOneRule(r);
		}
		
		/*
		 * Step 3. Updating predicates
		 */
		Set<Integer> moved_aps = e.updatePortPredicateMap(change_set);
		
		return moved_aps;
	}
	
	public int checkLoop(String type, String device, Set<Integer> moved_aps) throws Exception {
		if(!Parameters.PROPERTIES_TO_CHECK.contains(Property.LOOP)) return 0;
		if(division_activated) {
			return LoopChecker.detectLoopDivisionDirect(this, device, moved_aps);
		}
		if(name.startsWith("purdue")) {
			return LoopChecker.detectLoopDirect(this, device, moved_aps);
		}
		return LoopChecker.detectLoop(this, device, moved_aps);
	}
	
	public int checkBlackHole(String type, String device, Set<Integer> moved_aps) throws Exception {
		if(!Parameters.PROPERTIES_TO_CHECK.contains(Property.BLACKHOLE)) return 0;
		if(division_activated) {
			return BlackholeChecker.detectBlackholeDivision(this, device, moved_aps);
		}
		return BlackholeChecker.detectBlackhole(this, device, moved_aps);
	}
	
	private void softMergeAPBatch() throws Exception {
		if(fwd_apk.isMergeable()) {
			fwd_apk.tryMergeAPBatch();
		}
		if (acl_apk != null) {
			acl_apk.tryMergeAPBatch();
		}
	}
	
	private void hardMergeAPBatch() throws Exception {
		fwd_apk.tryMergeAPBatch();
		if (acl_apk != null) {
			acl_apk.tryMergeAPBatch();
		}
	}
}
