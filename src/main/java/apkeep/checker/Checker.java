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
package apkeep.checker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import apkeep.core.Network;
import apkeep.elements.ACLElement;
import apkeep.elements.Element;
import apkeep.elements.ForwardElement;
import common.BDDACLWrapper;
import common.PositionTuple;

public class Checker {
	
	Network net;
	Set<Loop> loops;
	
	public Checker(Network net) {
		this.net = net;
		loops = new HashSet<>();
	}
	
	public int getLoops() {
		return loops.size();
	}
	
	public ForwardingGraph constructFowardingGraph(PositionTuple pt1) {
		Map<PositionTuple, Set<Integer>> port_aps = new HashMap<>();
		Map<String, Set<PositionTuple>> node_ports = new HashMap<>();
		
		Element e = getElement(pt1.getDeviceName());
		Set<Integer> aps = e.getPortAPs(pt1.getPortName());
		
		if (aps == null) return null;
			
		for (int ap : aps) {
			Set<PositionTuple> pts = null;
			try {
				pts = net.getHoldPorts(ap);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			for (PositionTuple pt: pts) {
				port_aps.putIfAbsent(pt, new HashSet<>());
				port_aps.get(pt).add(ap);
				
				node_ports.putIfAbsent(pt.getDeviceName(), new HashSet<>());
				node_ports.get(pt.getDeviceName()).add(pt);
			}
		}
		
		return new ForwardingGraph(port_aps, node_ports);
	}
	
	public int checkProperty(ForwardingGraph g) {
		loops.clear();
		
		for(PositionTuple pt : g.port_aps.keySet()) {
			Set<Integer> aps = new HashSet<Integer>(g.port_aps.get(pt));
			ArrayList<PositionTuple> history = new ArrayList<PositionTuple>();
			traverseFowardingGraph(pt, aps, history, g);
		}
		
		return loops.size();
	}
	
	private void traverseFowardingGraph(PositionTuple cur_hop, Set<Integer> fwd_aps, 
			ArrayList<PositionTuple> history,
			ForwardingGraph g) {
		if(fwd_aps.isEmpty()) return;
		/*
		 * check loops
		 */
		if(checkLoop(history, cur_hop, fwd_aps, null)) return;
		history.add(cur_hop);
		
		/*
		 * look up l1-topology for connected node
		 */
		if(net.getConnectedPorts(cur_hop) == null) return;
		for(PositionTuple connected_pt : net.getConnectedPorts(cur_hop)) {
			String next_node = connected_pt.getDeviceName();
			if(!g.node_ports.containsKey(next_node)) continue;
			for(PositionTuple next_hop : g.node_ports.get(next_node)) {
				if(next_hop.equals(connected_pt)) continue;
				Set<Integer> aps = new HashSet<>(g.port_aps.get(next_hop));
				aps.retainAll(fwd_aps);
				ArrayList<PositionTuple> new_history = new ArrayList<>(history);
				new_history.add(connected_pt);
				traverseFowardingGraph(next_hop, aps, new_history, g);
			}
		}
	}

	public void checkProperty(String element_name, Set<Integer> moved_aps) {
		loops.clear();
		
		Element e = net.getElement(element_name);
		for(String port : e.getPorts()) {
			if (port.equals("default") || e.getPortAPs(port).isEmpty()) continue;
			
			Set<Integer> aps = new HashSet<>(moved_aps);
			aps.retainAll(e.getPortAPs(port));
			
			if(aps.isEmpty()) continue;
			Set<String> ports = getPhysicalPorts(e,port);
			for(String next_port : ports) {
				PositionTuple next_hop = new PositionTuple(element_name, next_port);
				ArrayList<PositionTuple> history = new ArrayList<>();
				traversePPM(next_hop, aps, history);
			}
		}
	}
	
	public void checkPropertyDivision(String element_name, Set<Integer> moved_aps) {
		loops.clear();
		
		boolean isACL = false;
		if(net.getElement(element_name) instanceof ACLElement) {
			if(net.getConnectedPorts(new PositionTuple(element_name, "permit"))==null) return;
			isACL = true;
		}
		
		element_name = net.getForwardElement(element_name);
		Element e = net.getElement(element_name);
		
		for(String port : e.getPorts()) {
			if (port.equals("default") || e.getPortAPs(port).isEmpty()) continue;
			
			Set<Integer> fwd_aps = new HashSet<>();
			Set<Integer> acl_aps = new HashSet<>();
			if(isACL) {
				fwd_aps.addAll(e.getPortAPs(port));
				acl_aps.addAll(moved_aps);
			}
			else {
				fwd_aps.addAll(moved_aps);
				fwd_aps.retainAll(e.getPortAPs(port));
				acl_aps.add(BDDACLWrapper.BDDTrue);
			}
			
			if(fwd_aps.isEmpty() || acl_aps.isEmpty()) continue;
			Set<String> ports = getPhysicalPorts(e,port);
			for(String next_port : ports) {
				PositionTuple next_hop = new PositionTuple(element_name, next_port);
				ArrayList<PositionTuple> history = new ArrayList<>();
				traversePPMDivision(next_hop, fwd_aps, acl_aps, history);
			}
		}
	}

	private void traversePPM(PositionTuple cur_hop, Set<Integer> fwd_aps, 
			List<PositionTuple> history) {
		
		if(fwd_aps.isEmpty()) return;
		/*
		 * check loops
		 */
		if(checkLoop(history, cur_hop, fwd_aps, null)) return;
		history.add(cur_hop);
		
		/*
		 * look up l1-topology for connected node
		 */
		if(net.getConnectedPorts(cur_hop) == null) return;
		for(PositionTuple connected_pt : net.getConnectedPorts(cur_hop)) {
			String next_node = connected_pt.getDeviceName();
			Element e = getElement(next_node);
			for(String port : e.getPorts()) {
				if(port.equals(connected_pt.getPortName())) continue;
				Set<Integer> aps = e.forwardAPs(port, fwd_aps);
				Set<String> ports = getPhysicalPorts(e,port);
				for(String next_port : ports) {
					if(next_port.equals(connected_pt.getPortName())) continue;
					PositionTuple next_hop = new PositionTuple(next_node, next_port);
					ArrayList<PositionTuple> new_history = new ArrayList<>(history);
					new_history.add(connected_pt);
					traversePPM(next_hop, aps, new_history);
				}
			}
		}
	}
	
	private void traversePPMDivision(PositionTuple cur_hop, 
			Set<Integer> fwd_aps, Set<Integer> acl_aps,
			List<PositionTuple> history) {
		if(fwd_aps.isEmpty() || acl_aps.isEmpty()) return;
		if(cur_hop.getPortName().equals("deny")) return;
		
		/*
		 * check loops
		 */
		if(checkLoop(history, cur_hop, fwd_aps, acl_aps)) return;
		history.add(cur_hop);
		
		/*
		 * look up l1-topology for connected node
		 */
		if(net.getConnectedPorts(cur_hop) == null) return;
		for(PositionTuple connected_pt : net.getConnectedPorts(cur_hop)) {
			String next_node = connected_pt.getDeviceName();
			Element e = getElement(next_node);
			Set<Integer> filtered_fwd_aps = new HashSet<>(fwd_aps);
			Set<Integer> filtered_acl_aps = new HashSet<>(acl_aps);
			for(String port : e.getPorts()) {
				if(port.equals(connected_pt.getPortName())) continue;
				if(e instanceof ACLElement) {
					filtered_acl_aps = e.forwardAPs(port, acl_aps);
				}
				else {
					filtered_fwd_aps = e.forwardAPs(port, fwd_aps);
				}
				Set<String> ports = getPhysicalPorts(e,port);
				for(String next_port : ports) {
					if(next_port.equals(connected_pt.getPortName())) continue;
					PositionTuple next_hop = new PositionTuple(next_node, next_port);
					ArrayList<PositionTuple> new_history = new ArrayList<>(history);
					new_history.add(connected_pt);
					traversePPMDivision(next_hop, filtered_fwd_aps, filtered_acl_aps, new_history);
				}
			}
		}
	}

	private boolean checkLoop(List<PositionTuple> history, PositionTuple cur_hop,
			Set<Integer> fwd_aps, Set<Integer> acl_aps) {
		if(history.contains(cur_hop)) {
			if(acl_aps != null) {
				if(!Element.hasOverlap(fwd_aps, acl_aps)) {
					return true;
				}
			}
			history.add(cur_hop);
			Loop loop = new Loop(fwd_aps, history, cur_hop);
			loops.add(loop);
			return true;
		}
		return false;
	}
	
	private Element getElement(String node_name) {
		if(net.isACLNode(node_name)) return net.getACLElement(node_name);
		return net.getElement(node_name);
	}
	
	private Set<String> getPhysicalPorts(Element e, String port){
		if(e instanceof ForwardElement) {
			if(port.toLowerCase().startsWith("vlan")) {
				return ((ForwardElement) e).getVlanPorts(port);			
			}
		}
		Set<String> ports = new HashSet<>();
		ports.add(port);
		return ports;
	}
}
