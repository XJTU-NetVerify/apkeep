package apkeep.core;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import apkeep.checker.Checker;
import apkeep.checker.ForwardingGraph;
import apkeep.elements.ACLElement;
import apkeep.elements.Element;
import apkeep.elements.ForwardElement;
import apkeep.elements.NATElement;
import apkeep.exception.ElementNotFoundException;
import apkeep.rules.Rule;
import apkeep.utils.Evaluator;
import apkeep.utils.Logger;
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
	
	/*
	 * The BDD data structure for encoding packet sets with Boolean formula
	 */
	public BDDACLWrapper bdd_engine;
	
	/*
	 * The key data structure that handles the split and merge of predicates
	 */
	protected APKeeper fwd_apk; // the APKeeper for forwarding devices
	protected APKeeper acl_apk; // the APKeeper for ACL devices

	private Checker checker;
	
	public Network(String network_name) {
		name = network_name;
		division_activated = false;
		
		topology = new HashMap<>();
		elements = new HashMap<>();
		bdd_engine = new BDDACLWrapper();
		
		fwd_apk = null;
		acl_apk = null;
		
		acl_node_names = new HashSet<>();
		nat_element_names = new HashSet<>();
		
		new HashMap<>();

		Element.setBDDWrapper(bdd_engine);
		
		checker = new Checker(this);
	}
	
	public void initializeNetwork(ArrayList<String> l1_links, 
			List<String> devices,
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
	
	private void addFWDElement(List<String> devices) {
		if(devices == null) return;
		for(String element : devices) {
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
		division_activated = true;
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

	public Set<PositionTuple> getHoldPorts(int ap) throws Exception {
		return fwd_apk.getHoldPorts(ap);
	}
	
	public int getAPNum() {
		if(division_activated) {
			return fwd_apk.getAPNum()+acl_apk.getAPNum();
		}
		else {
			return fwd_apk.getAPNum();
		}
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
		Set<Integer> moved_aps = updateRule(eva, op, type, device, rule);
		if (moved_aps == null) return;
		eva.midUpdate();
		
		/*
		 * Verifying properties
		 */
		if (!moved_aps.isEmpty()) {
			checkProperty(eva, device, moved_aps);
		}
		
		softMergeAPBatch();
		
		eva.endUpdate();
		eva.printUpdateResults(getAPNum());
	}
	
	private Set<Integer> updateRule(Evaluator eva, String op, String type, String device, String rule) throws Exception{
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
		eva.startUpdate();
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
	
	public void checkProperty(Evaluator eva, String device, Set<Integer> moved_aps) throws Exception {
		if(division_activated) {
			checker.checkPropertyDivision(device, moved_aps);
		}
		else {
			checker.checkProperty(device, moved_aps);
		}
		
		eva.addLoops(checker.getLoops());
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
	
	/* answer "what if" questions for each possible link failure
	 * for each link, and for each 
	 */
	public void checkLinkFailure(Evaluator eva) throws IOException
	{
		Checker checker = new Checker(this);

		int total_links = 0;
		long construction_time = 0;
		long detection_time = 0;
		for (PositionTuple pt1 : topology.keySet()) {
			for (PositionTuple pt2 : topology.get(pt1)) {
				long t1 = System.nanoTime();
				ForwardingGraph g = checker.constructFowardingGraph(pt1);
				if (g == null) continue;
				long t2 = System.nanoTime();
				construction_time += (t2-t1);
				checker.checkProperty(g);
				long t3 = System.nanoTime();
				detection_time += (t3-t2);
				total_links ++;
				System.out.println("Link " + pt1 + "->" + pt2 + ":" + (t3-t1)/1000000.0 + "ms");
			}
		}
		
		eva.addLinkFailure(total_links, construction_time, detection_time);
	}
}
