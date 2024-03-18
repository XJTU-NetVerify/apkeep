package apkeep.checker2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import apkeep.core.Network;
import apkeep.elements.ACLElement;
import apkeep.elements.Element;
import apkeep.elements.ForwardElement;
import apkeep.elements.NATElement;
import common.BDDACLWrapper;
import common.PositionTuple;

class Loop {
	HashSet<Integer> apset;
	ArrayList<PositionTuple> path;
	
	public Loop(HashSet<Integer> aps, ArrayList<PositionTuple> history, PositionTuple cur_hop)
	{
		apset = aps;
		path = history;
		while(path.size() > 0) {
			if(!path.get(0).equals(cur_hop)) {
				path.remove(0);
			}
			else break;
		}
	}
	
	public String toString()
	{
		String loop = "loop found for " + apset + ": ";
		for (int i=0; i<path.size(); i++) {
			loop += path.get(i) + " ";
		}
		return "++++++++++++++++++++++++++++++\n" 
				+ loop 
				+ "\n++++++++++++++++++++++++++++++"; 
	}
	
	@Override
	public int hashCode(){
		return 0;
	}
	
	@Override
	public boolean equals (Object o) 
	{
		Loop loop = (Loop) o;
		//if (!apset.equals(loop.apset)) return false;
		if (path.size() != loop.path.size()) return false;
		int i;
		PositionTuple start = loop.path.get(0);
		for (i=0; i< path.size(); i++) {
			if (path.get(i).equals(start)){
				break;
			}
		}
		for (int j=0; j<path.size()-1; j++) {
			if (!path.get((i+j)%(path.size()-1)).equals(loop.path.get(j))) {
				return false;
			}
		}
		// the loop already exists, update the AP set for this loop
		apset.addAll(loop.apset);
		return true;
	}
}

public class LoopChecker extends Checker{
		
	HashSet<Loop> loops;
	
	public LoopChecker(Network n)
	{
		super(n);
		this.loops = new HashSet<Loop>();
	}
	
	/*
	 * Detect loops of packet moved APs, starting from element_name
	 */
	public int DetectLoop(String element_name, HashSet<Integer> moved_aps) 
	{	
		loops.clear();

		ConstructFowardingGraph(moved_aps);
				
		if (net.getElement(element_name) instanceof ACLElement){
			element_name = net.getForwardElementFromACL(element_name);
		}
		
		for(PositionTuple pt : port_aps.keySet()) {
			if (!pt.getDeviceName().equals(element_name) || pt.getPortName().equals("default"))
				continue;
			HashSet<Integer> aps = new HashSet<Integer>(port_aps.get(pt));
			ArrayList<PositionTuple> history = new ArrayList<PositionTuple>();
			TraverseFowardingGraph(pt, aps, history);
		}
		
		return loops.size();
	}
	
	/*
	 * Detect loops of packet moved APs, starting from element_name
	 * No forwarding graph is constructed, directly traversing PPM
	 */
	public int DetectLoopDirect(String element_name, HashSet<Integer> moved_aps) 
	{	
		loops.clear();
		
		Element element = net.getElement(element_name);
		
		// start from each virtual output ports (including vlan)
		for (String output_port: element.getPorts()) {
			// compute the intersection of moved aps and port pas
			HashSet<Integer> aps = new HashSet<Integer>(moved_aps);
			aps.retainAll(element.getPortAPs(output_port));
			if (aps.size()>0) {
				if (output_port.startsWith("vlan")) {
					// map virtual ports to physical ports
					Set<String> output_ports = ((ForwardElement) net.getElement(element_name)).getVlanPorts(output_port);
					// traverse from each physical output port
					for (String one_output_port : output_ports) {
						PositionTuple output_pt = new PositionTuple(element_name, one_output_port);
						ArrayList<PositionTuple> history = new ArrayList<PositionTuple>();
						TraversePPM(output_pt, new HashSet<Integer>(aps), history);
					}
				}
				else {
					// traverse from the output port
					PositionTuple output_pt = new PositionTuple(element_name, output_port);
					ArrayList<PositionTuple> history = new ArrayList<PositionTuple>();
					TraversePPM(output_pt, aps, history);
				}
			}
		}
		
		return loops.size();
	}
	
	/*
	 * Detect loops of packet moved APs, starting from element_name
	 * Forward/NAT and ACL elements are divided
	 * No forwarding graph is constructed, directly traversing the elements
	 */
	public int DetectLoopDivisionDirect(String element_name, HashSet<Integer> moved_aps) 
	{	
		loops.clear();
		
		boolean isACLDevice = false;
		if (net.getElement(element_name) instanceof ACLElement){
			if(net.getConnectedPorts(new PositionTuple(element_name, "permit"))==null)
				return 0;
			element_name = net.getForwardElementFromACL(element_name);
			isACLDevice = true;
		}
		
		Element element = net.getElement(element_name);
		for (String port: element.getPorts()) {
			if (port.equals("default") || element.getPortAPs(port).size()==0)
				continue;
			
			HashSet<Integer> aps = null;
			HashSet<Integer> aclaps = null;
			if (isACLDevice) {
				aps = new HashSet<Integer>(element.getPortAPs(port));
				aclaps = new HashSet<Integer>(moved_aps);
			}
			else {
				aps = new HashSet<Integer>(moved_aps);
				aps.retainAll(element.getPortAPs(port));
				aclaps = new HashSet<Integer>();
				aclaps.add(BDDACLWrapper.BDDTrue);
			}
			
			if (aps.size()>0 && aclaps.size()>0) {
				if (port.startsWith("vlan")) {
					// map virtual ports to physical ports
					Set<String> output_ports = ((ForwardElement) net.getElement(element_name)).getVlanPorts(port);
					// traverse from each physical output port
					for (String one_output_port : output_ports) {
						PositionTuple output_pt = new PositionTuple(element_name, one_output_port);
						ArrayList<PositionTuple> history = new ArrayList<PositionTuple>();
						TraversePPM(output_pt, new HashSet<Integer>(aps), new HashSet<Integer>(aclaps), history);
					}
				}
				else {
					// traverse from the output port
					PositionTuple output_pt = new PositionTuple(element_name, port);
					ArrayList<PositionTuple> history = new ArrayList<PositionTuple>();
					TraversePPM(output_pt, aps, aclaps, history);
				}
			}
		}
		return loops.size();
	}
	
	/*
	 * Traverse the forwarding graph with only forward APs
	 */
	public void TraverseFowardingGraph(PositionTuple cur_hop, HashSet<Integer> aps, ArrayList<PositionTuple> history) 
	{
		
		aps.retainAll(port_aps.get(cur_hop));
		if (aps.size()==0){
			return;
		}
		
		if (cur_hop.getPortName().equals("nat")) {
			NATElement r = (NATElement) net.getElement(cur_hop.getDeviceName());
			aps = r.forwardAPs(cur_hop.getPortName(),aps);
		}
		
		if (history.contains(cur_hop)) {
			history.add(cur_hop);
			Loop loop = new Loop(aps, history, cur_hop);
			if (loops.add(loop)){
			}
			return;
		}
		
		history.add(cur_hop);
		
		if (net.getConnectedPorts(cur_hop) == null) {
			return;
		}
		
		for (PositionTuple pt: net.getConnectedPorts(cur_hop)) {
			//System.out.println("next hop: " + pt);
			String dname = pt.getDeviceName();
			if (!node_ports.containsKey(dname))
				return;
			for (PositionTuple next_hop : node_ports.get(dname)) {
				if(next_hop.equals(pt)) {
					continue;
				}
				HashSet<Integer> new_aps = new HashSet<Integer>(aps);
				ArrayList<PositionTuple> new_history = new ArrayList<PositionTuple>(history);
				new_history.add(pt);
				TraverseFowardingGraph(next_hop, new_aps, new_history); 
				//Traverse(next_hop, aps, history);
			}
		}
	}
	
	/*
	 * Traverse the PPM model with only forward APs
	 */
	public void TraversePPM(PositionTuple cur_hop, HashSet<Integer> cur_aps, ArrayList<PositionTuple> history) 
	{

		// apply the rewrite table if it is an NAT element
		Element current_element = net.getElement(cur_hop.getDeviceName());
		String current_port = cur_hop.getPortName();
		if (current_element instanceof NATElement && !current_port.equals("default")) {
			NATElement nat = (NATElement) current_element;
//			LOG.LOOP("before rewrite: " + cur_aps);
			cur_aps = nat.forwardAPs(cur_hop.getPortName(),cur_aps);
//			LOG.LOOP("after rewrite: " + cur_aps);
			if (cur_aps.size()==0){
				return;
			}
		}
		
		// check whether there is a loop
		if (history.contains(cur_hop)) {
			history.add(cur_hop);
			Loop loop = new Loop(cur_aps, history, cur_hop);
			if (loops.add(loop)){
			}
			return;
		}

		history.add(cur_hop);
		
		if (net.getConnectedPorts(cur_hop) == null) {
//			LOG.LOOP("no next hops found");
			return;
		}
		
		// one output port may map to multiple next hops
		for (PositionTuple next_pt: net.getConnectedPorts(cur_hop)) {
			
//			LOG.LOOP("traversing input port " + next_pt + ": " + cur_aps);

			String next_intput_port = next_pt.getPortName();
			String next_element_name = next_pt.getDeviceName();
			Element next_element = net.getElement(next_element_name);
			if (next_element == null) {
//				LOG.ERROR("element not found");
				return;
			}
			
			for (String output_port : next_element.getPorts()) {
				// intersect current aps with port aps
				HashSet<Integer> filtered_aps = new HashSet<Integer>(cur_aps);
				Set<Integer> port_aps = next_element.getPortAPs(output_port);
//				LOG.LOOP("trying output port " + next_element_name + "," + output_port + ": " + port_aps.toString());
				filtered_aps.retainAll(port_aps);
				if (filtered_aps.size()==0){
					continue;
				}	
				
				if (output_port.startsWith("vlan")) {
					// map vlan ports to physical ports
					Set<String> output_ports = ((ForwardElement) net.getElement(next_element_name)).getVlanPorts(output_port);
					// traverse from each physical output port
					for (String one_output_port : output_ports) {
						// cannot output to the input port
						if(one_output_port.equals(next_intput_port)) {
							continue;
						}
						PositionTuple new_pt = new PositionTuple(next_element_name, one_output_port);
						HashSet<Integer> new_aps = new HashSet<Integer>(filtered_aps);
						ArrayList<PositionTuple> new_history = new ArrayList<PositionTuple>(history);
						new_history.add(next_pt);
						TraversePPM(new_pt, new_aps, new_history);
					}
				}
				else {
					// traverse from the output port
					if(output_port.equals(next_intput_port)) {
						continue;
					}
					PositionTuple new_pt = new PositionTuple(next_element_name, output_port);
					ArrayList<PositionTuple> new_history = new ArrayList<PositionTuple>(history);
					new_history.add(next_pt);
					TraversePPM(new_pt, filtered_aps, new_history);
				}
			}
		}
	}
	
	/*
	 * Traverse the PPM model with both forward and ACL APs
	 */
	public void TraversePPM(PositionTuple cur_hop, HashSet<Integer> aps, HashSet<Integer> aclaps, ArrayList<PositionTuple> history) 
	{			
		if (history.contains(cur_hop)) {
			//System.err.println("A possible loop found! " + aps + ": "+ history);		
			if (!HasOverlap(aps, aclaps)) {
				System.err.println("It is not a Loop!");			
				return;
			}
			history.add(cur_hop);
			Loop loop = new Loop(aps, history, cur_hop);
			if (loops.add(loop)){
//				System.exit(0);
			}
			return;
		}
		
		history.add(cur_hop);

		if (net.getConnectedPorts(cur_hop) == null) {
			return;
		}
		
		// one output port may map to multiple next hops
		for (PositionTuple next_pt: net.getConnectedPorts(cur_hop)) {

			String next_intput_port = next_pt.getPortName();
			String next_node_name = next_pt.getDeviceName();
			Element next_element = null;
			
			if (next_intput_port.equals("inport")) {
				next_element = net.getACLElement(next_node_name);
			}
			else {
				next_element = net.getElement(next_node_name);
			}
			
			if (next_element == null) {
				return;
			}
			
			for (String output_port : next_element.getPorts()) {
				// intersect current aps with port aps
				Set<Integer> port_aps = next_element.getPortAPs(output_port);
				HashSet<Integer> filtered_aps = new HashSet<Integer>(aps);
				HashSet<Integer> filtered_aclaps = new HashSet<Integer>(aclaps);

				if (output_port.equals("permit") || output_port.equals("deny")) {
					if (filtered_aclaps.remove(BDDACLWrapper.BDDTrue)) {
						filtered_aclaps.addAll(port_aps);
					}
					else {
						filtered_aclaps.retainAll(port_aps);
					}
					if (filtered_aclaps.size()==0){	
						continue;
					}
					if (output_port.equals("deny")) {
						if (HasOverlap(aps, aclaps)){
						}
						continue;
					}
				}
				else {
					filtered_aps.retainAll(port_aps);
					if (filtered_aps.size()==0){
						continue;
					}
				}
				
				if (output_port.startsWith("vlan")) {
					// map vlan ports to physical ports
					Set<String> output_ports = ((ForwardElement) net.getElement(next_node_name)).getVlanPorts(output_port);
					// traverse from each physical output port
					for (String one_output_port : output_ports) {
						// cannot output to the input port
						if(one_output_port.equals(next_intput_port)) {
//							LOG.LOOP("cannot output to the input port " + next_intput_port);
							continue;
						}
						PositionTuple new_pt = new PositionTuple(next_node_name, one_output_port);
						HashSet<Integer> new_aps = new HashSet<Integer>(filtered_aps);
						HashSet<Integer> new_aclaps = new HashSet<Integer>(filtered_aclaps);
						ArrayList<PositionTuple> new_history = new ArrayList<PositionTuple>(history);
						new_history.add(next_pt);
						TraversePPM(new_pt, new_aps, new_aclaps, new_history);
					}
				}
				else {
					// traverse from the output port
					if(output_port.equals(next_intput_port)) {
//						LOG.LOOP("cannot output to the input port " + next_intput_port);
						continue;
					}
					PositionTuple new_pt = new PositionTuple(next_node_name, output_port);
					ArrayList<PositionTuple> new_history = new ArrayList<PositionTuple>(history);
					new_history.add(next_pt);
					TraversePPM(new_pt, filtered_aps, filtered_aclaps, new_history);
				}
			}
		}
	}

	@Override
	public void TraverseForwardingGraph(PositionTuple cur_hop, HashSet<Integer> fwd_aps, HashSet<Integer> acl_aps,
			ArrayList<PositionTuple> history) {
		
	}
}


