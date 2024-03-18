package apkeep.checker2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import apkeep.core.Network;
import apkeep.elements.ACLElement;
import apkeep.elements.Element;
import apkeep.elements.ForwardElement;
import apkeep.utils.Parameters;
import common.BDDACLWrapper;
import common.PositionTuple;

public abstract class Checker {
	
	Network net;
	HashMap<PositionTuple, HashSet<Integer>> port_aps;
	HashMap<String, HashSet<PositionTuple>> node_ports;
	
	public Checker(Network n) {
		net = n;
		port_aps = new HashMap<PositionTuple, HashSet<Integer>>();
		node_ports = new HashMap<String, HashSet<PositionTuple>>();	
	}
	
	public abstract void TraverseFowardingGraph(PositionTuple cur_hop, HashSet<Integer> aps, ArrayList<PositionTuple> history); 
	public abstract void TraverseForwardingGraph(PositionTuple cur_hop, HashSet<Integer> fwd_aps, HashSet<Integer> acl_aps, ArrayList<PositionTuple> history);
//	public abstract void Check();
	
	public void ConstructFowardingGraph(HashSet<Integer> moved_aps)
	{
		port_aps.clear();
		node_ports.clear();
		
		if (moved_aps.size() == 0) {
			return;
		}
		
		if (net.GetNATNumber() > 0) {
			RewriteMovedAPS(moved_aps);
		}
		
		for (Integer ap: moved_aps) {
			HashSet<PositionTuple> pts = net.fwd_apk.GetReferedEdges(ap);
			if (pts == null) {
				System.err.println("AP not found");
				continue;
			}
			
			HashSet<PositionTuple> new_pts = new HashSet<PositionTuple>();
			for (PositionTuple pt: pts) {
				// add ACL ports
				if (pt.getPortName().equals("permit") || pt.getPortName().equals("deny")) {
					for (String aclname : net.GetACLNames()) {
						if (aclname.startsWith(pt.getDeviceName())){
							new_pts.add(new PositionTuple(aclname, pt.getPortName()));
						}
					}
				}
				// translate VLAN ports
				else if (pt.getPortName().startsWith("vlan")) {
					HashSet<String> ports = net.GetForwardElement(pt.getDeviceName()).VlanToPhy(pt.getPortName());
					for (String port : ports) {
						new_pts.add(new PositionTuple(pt.getDeviceName(), port));
					}
				}
				// normal ports
				else {
					new_pts.add(pt);
				}
			}
			
			pts = new_pts;
			
			for (PositionTuple pt: pts) {
				if (port_aps.containsKey(pt)) {
					port_aps.get(pt).add(ap);
				}
				else {
					HashSet<Integer> new_aps = new HashSet<Integer>();
					new_aps.add(ap);
					port_aps.put(pt, new_aps);
					String dname = pt.getDeviceName();
					if(node_ports.containsKey(dname)) {
						node_ports.get(dname).add(pt);
					}
					else {
						HashSet<PositionTuple> new_ports = new HashSet<PositionTuple>();
						new_ports.add(pt);
						node_ports.put(dname, new_ports);
					}
				}
			}
		}
	}
	

	public void ConstructFowardingGraphFW(HashSet<Integer> moved_aps)
	{
		port_aps.clear();
		node_ports.clear();
		
		if (moved_aps.size() == 0) {
			return;
		}
		
		for (Integer ap: moved_aps) {
			HashSet<PositionTuple> pts = net.fwd_apk.GetReferedEdges(ap);
			if (pts == null) {
				System.err.println("AP not found");
				continue;
			}
			
			HashSet<PositionTuple> new_pts = new HashSet<PositionTuple>();
			for (PositionTuple pt: pts) {
				// translate VLAN ports
				if (pt.getPortName().startsWith("vlan")) {
					HashSet<String> ports = net.GetForwardElement(pt.getDeviceName()).VlanToPhy(pt.getPortName());
					for (String port : ports) {
						new_pts.add(new PositionTuple(pt.getDeviceName(), port));
					}
				}
				// normal ports
				else {
					new_pts.add(pt);
				}
			}
			
			pts = new_pts;
			
			for (PositionTuple pt: pts) {
				if (port_aps.containsKey(pt)) {
					port_aps.get(pt).add(ap);
				}
				else {
					HashSet<Integer> new_aps = new HashSet<Integer>();
					new_aps.add(ap);
					port_aps.put(pt, new_aps);
					String dname = pt.getDeviceName();
					if(node_ports.containsKey(dname)) {
						node_ports.get(dname).add(pt);
					}
					else {
						HashSet<PositionTuple> new_ports = new HashSet<PositionTuple>();
						new_ports.add(pt);
						node_ports.put(dname, new_ports);
					}
				}
			}
		}
		
		// add all ACL ports
		for (String acl_element_name : net.GetACLNames()) {
			ACLElement acl_element = net.GetACLElement(acl_element_name);
			PositionTuple permitport = new PositionTuple(acl_element_name, "permit");
			port_aps.put(permitport, acl_element.get_port_aps("permit"));
			HashSet<PositionTuple> new_ports = new HashSet<PositionTuple>();
			new_ports.add(permitport);
			node_ports.put(acl_element_name, new_ports);
		}
	}
	
	public void ConstructFowardingGraphACL(HashSet<Integer> moved_aps)
	{
		port_aps.clear();
		node_ports.clear();
	
		if (moved_aps.size() == 0) {
			return;
		}
	
		if (net.fwd_apk.getAPNum() <= 1) {
			return;
		}
			
		// add ACL ports
		for (Integer ap: moved_aps) {
			HashSet<PositionTuple> pts = net.acl_apk.GetReferedEdges(ap);
			if (pts == null) {
				System.err.println("AP not found");
				continue;
			}
			
			// translate logical ACL ports to physical ACL ports
			HashSet<PositionTuple> new_pts = new HashSet<PositionTuple>();
			for (PositionTuple pt: pts) {
				for (String acl_name : net.GetACLNames()) {
					if (acl_name.startsWith(pt.getDeviceName())) {
						new_pts.add(new PositionTuple(acl_name, pt.getPortName()));
					}
				}
			}
			
			for (PositionTuple pt: new_pts) {
				if (port_aps.containsKey(pt)) {
					port_aps.get(pt).add(ap);
				}
				else {
					HashSet<Integer> new_aps = new HashSet<Integer>();
					new_aps.add(ap);
					port_aps.put(pt, new_aps);
					String dname = pt.getDeviceName();
					if(node_ports.containsKey(dname)) {
						node_ports.get(dname).add(pt);
					}
					else {
						HashSet<PositionTuple> new_ports = new HashSet<PositionTuple>();
						new_ports.add(pt);
						node_ports.put(dname, new_ports);
					}
				}
			}
		}
		

		// add all forwarding ports
		for (Element e : net.getAllElements()) {
			if (e instanceof ForwardElement){
				ForwardElement fe = (ForwardElement) e;
				HashSet<PositionTuple> new_ports = new HashSet<PositionTuple>();
				for (String port : fe.port_aps_raw.keySet()) {
					if (port.startsWith("vlan")) {
						HashSet<String> vlan_ports = fe.VlanToPhy(port);
						for (String one_vlan_port : vlan_ports) {
							PositionTuple pt = new PositionTuple(fe.name, one_vlan_port);
							if (port_aps.containsKey(pt)) {
								port_aps.get(pt).addAll(fe.port_aps_raw.get(port));
							}
							else {
								port_aps.put(pt,new HashSet<Integer>(fe.port_aps_raw.get(port)));
								new_ports.add(pt);
							}
						}
					}
					else {
						PositionTuple pt = new PositionTuple(fe.name, port);
						if (port_aps.containsKey(pt)) {
							port_aps.get(pt).addAll(fe.port_aps_raw.get(port));
						}
						else {
							port_aps.put(pt,new HashSet<Integer>(fe.port_aps_raw.get(port)));
							new_ports.add(pt);
						}
					}
				}
				node_ports.put(fe.name, new_ports);
			}
		}
	}
	
	public void RewriteMovedAPS(HashSet<Integer> moved_aps)
	{
		HashMap<Integer, HashSet<Integer>> global_rewrite_table = net.GetGlobalRewriteTable();
		if (global_rewrite_table.size() == 0) {
			return;
		}
		HashSet<Integer> all_rewited_aps = new HashSet<Integer>(global_rewrite_table.keySet());
		all_rewited_aps.retainAll(moved_aps);
		if (all_rewited_aps.size() == 0) {
			return;
		}
			
		HashSet<Integer> new_moved_aps = new HashSet<Integer>();
		while (true) {
			for (int one_ap : moved_aps) {
				if (global_rewrite_table.containsKey(one_ap)) {
					new_moved_aps.addAll(global_rewrite_table.get(one_ap));
				}
			}
			new_moved_aps.removeAll(moved_aps);
			if (new_moved_aps.isEmpty()) {
				break;
			}
			moved_aps.addAll(new_moved_aps);
			new_moved_aps.clear();
		}
	}
	
	public boolean HasOverlap(HashSet<Integer> aps1, HashSet<Integer> aps2) {
//		if (aps2.size() == net.aclapk.getAPNum()) {
//			System.out.println("all acl aps");
//			return true;
//		}		
		if (aps1.size()==0 || aps2.size() == 0) {
			return false;
		}
		
		//System.out.println("checking " + aps1 + " vs " + aps2);
		for (int ap1 : aps1) {
			for (int ap2: aps2) {
				int intersect = net.GetBDDEngine().getBDD().and(ap1, ap2);
				if (intersect != BDDACLWrapper.BDDFalse) {
					//System.out.println(ap1 + " intersects " + ap2);
					return true;
				}
			}
		}
		
		return false;
	}

}
