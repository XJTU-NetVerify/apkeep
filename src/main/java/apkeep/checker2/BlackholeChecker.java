package apkeep.checker2;

import java.util.ArrayList;
import java.util.HashSet;

import apkeep.core.Network;
import apkeep.elements.ACLElement;
import apkeep.elements.ForwardElement;
import apkeep.elements.NATElement;
import common.BDDACLWrapper;
import common.PositionTuple;

public class BlackholeChecker extends Checker{

	int num_black_hole;
	
	public BlackholeChecker(Network n) {
		super(n);
	}

	public void TraverseFowardingGraph(PositionTuple cur_hop, HashSet<Integer> aps, ArrayList<PositionTuple> history)
	{
		LOG.CHECK("Traversing hop " + cur_hop + ":" + aps);
		
		aps.retainAll(port_aps.get(cur_hop));
		if (aps.size()==0){
			return;
		}
		
		if (cur_hop.getPortName().equals("nat")) {
			NATElement r = (NATElement) net.GetElements().get(cur_hop.getDeviceName());
			LOG.CHECK("rewrited from " + aps);
			aps = r.LookupRewriteTable(aps);
			LOG.CHECK(" to " + aps);
		}
		
		if (history.contains(cur_hop)) {
			return;
		}
		
		history.add(cur_hop);
		
		if (net.LinkTransfer(cur_hop) == null) {
			if (cur_hop.getPortName().equals("default")) {
				LOG.WARNING("Black hole detected " + aps + ": " + history);
				num_black_hole ++;
			}
			return;
		}
		
		for (PositionTuple pt: net.LinkTransfer(cur_hop)) {
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
			}
		}		
	}
	
	/*
	 * Traverse the forwarding graph with both the forward and ACL APs
	 */
	public void TraverseForwardingGraph(PositionTuple cur_hop, HashSet<Integer> fwd_aps, HashSet<Integer> acl_aps, ArrayList<PositionTuple> history) 
	{	
		String port_name = cur_hop.getPortName();
		HashSet<Integer> aps = port_aps.get(cur_hop);
				
		if (port_name.equals("permit") || port_name.equals("deny")) {
			LOG.CHECK(cur_hop + ":" + port_aps.get(cur_hop));
			if (acl_aps.remove(BDDACLWrapper.BDDTrue)) {
				acl_aps.addAll(aps);
			}
			else {
				acl_aps.retainAll(aps);
			}
			if (acl_aps.size()==0){
				LOG.CHECK("acl aps becomes empty");			
				return;
			}
			if (port_name.equals("deny")) {
				if (HasOverlap(aps, acl_aps)){
					LOG.CHECK("some packets are filtered");
				}
				return;
			}
		}
		else {
			fwd_aps.retainAll(port_aps.get(cur_hop));
			if (fwd_aps.size()==0){
				return;
			}
		}
		
		if (history.contains(cur_hop)) {
			return;
		}
		
		history.add(cur_hop);

		if (net.LinkTransfer(cur_hop) == null) {
			if (cur_hop.getPortName().equals("default")) {
				LOG.WARNING("Black hole detected " + aps + ": " + history);
				num_black_hole ++;
			}
			return;
		}
		
		for (PositionTuple pt: net.LinkTransfer(cur_hop)) {
			//System.out.println("next hop: " + pt);
			String dname = pt.getDeviceName();
			if (!node_ports.containsKey(dname))
				return;
			for (PositionTuple next_hop : node_ports.get(dname)) {
				if(next_hop.equals(pt)) {
					continue;
				}
				HashSet<Integer> new_aps = new HashSet<Integer>(fwd_aps);
				HashSet<Integer> new_aclaps = new HashSet<Integer>(acl_aps);
				ArrayList<PositionTuple> new_history = new ArrayList<PositionTuple>(history);
				new_history.add(pt);
				TraverseForwardingGraph(next_hop, new_aps, new_aclaps, new_history); 
			}
		}
	}
	
	/*
	 * Detect blackholes due to moved APs, starting from element_name
	 */
	public int DetectBlackhole(String element_name, HashSet<Integer> moved_aps) 
	{	
		num_black_hole = 0;

		ConstructFowardingGraph(moved_aps);
				
		if (net.GetElements().get(element_name) instanceof ACLElement){
			element_name = net.GetFWDNameByACLName(element_name);
		}
		
		for(PositionTuple pt : port_aps.keySet()) {
			if (!pt.getDeviceName().equals(element_name) || pt.getPortName().equals("default"))
				continue;
			HashSet<Integer> aps = new HashSet<Integer>(port_aps.get(pt));
			ArrayList<PositionTuple> history = new ArrayList<PositionTuple>();
			LOG.CHECK("start from " + pt + " " + aps);
			TraverseFowardingGraph(pt, aps, history);
		}
		
		return num_black_hole;
	}
	
	/*
	 * Detect blackholes due to moved APs, starting from element_name
	 * Forward/NAT and ACL elements are divided
	 */
	public int DetectBlackholeDivision(String element_name, HashSet<Integer> moved_aps) 
	{
		num_black_hole = 0;
		
		boolean isACLDevice = false;

		// construct the forwarding graph
		if (net.GetElements().get(element_name) instanceof ACLElement){
			ConstructFowardingGraphACL(moved_aps);
			element_name = net.GetFWDNameByACLName(element_name);
			isACLDevice = true;
		}
		else if (net.GetElements().get(element_name) instanceof ForwardElement) {
			ConstructFowardingGraphFW(moved_aps);
		}
		
		for(PositionTuple pt : port_aps.keySet()) {
			if (!pt.getDeviceName().equals(element_name) || pt.getPortName().equals("default"))
				continue;
			HashSet<Integer> aps = null;
			HashSet<Integer> allaclap = null;
			if (isACLDevice) {
				aps = new HashSet<Integer>(port_aps.get(pt));
				allaclap = new HashSet<Integer>(moved_aps);
			}
			else {
				aps = new HashSet<Integer>(moved_aps);
				allaclap = new HashSet<Integer>();
				allaclap.add(BDDACLWrapper.BDDTrue);
			}
	
			ArrayList<PositionTuple> history = new ArrayList<PositionTuple>();
			LOG.CHECK("start from " + pt + " " + aps + ", " + allaclap);
			TraverseForwardingGraph(pt, aps, allaclap, history);
		}
		
		return num_black_hole;
	}
}
