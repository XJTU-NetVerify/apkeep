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
import java.util.HashSet;
import java.util.Set;

import apkeep.core.Network;
import apkeep.elements.ACLElement;
import apkeep.elements.Element;
import apkeep.elements.ForwardElement;
import apkeep.exception.ElementNotFoundException;
import apkeep.utils.Logger;
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

public class LoopChecker extends Checker {
	
	private static Set<Loop> loops = new HashSet<>();
	
	public static int detectLoop(Network net,String device, Set<Integer> moved_aps) throws Exception {
		
		loops.clear();
		
		ForwardingGraph g = constructForwardingGraph(net, moved_aps);
		
		String element_name = net.getForwardElement(device);
		
		Set<PositionTuple> start_pts = g.getStartPorts(element_name);
		
		for(PositionTuple pt : start_pts) {
			HashSet<Integer> aps = new HashSet<>(g.getAPs(pt));
			ArrayList<PositionTuple> history = new ArrayList<>();
			traverseForwardingGraph(g, pt, aps, history);
		}	
		return loops.size();
	}
	
	/*
	 * Detect loops of packet moved APs, starting from element_name
	 * No forwarding graph is constructed, directly traversing PPM
	 */
	public static int detectLoopDirect(Network net, String device, 
			Set<Integer> moved_aps) throws Exception 
	{	
		loops.clear();
		
		String element_name = net.getForwardElement(device);
		Element element = net.getElement(element_name);
		
		// start from each virtual output ports (including vlan)
		for (String output_port: element.getPorts()) {
			// compute the intersection of moved aps and port pas
			HashSet<Integer> aps = element.forwardAPs(output_port, moved_aps);
			if(aps.isEmpty()) continue;
			
			Set<PositionTuple> start_pts = net.mapVlanIntoPhyPorts(new PositionTuple(element_name, output_port));
			if(start_pts.isEmpty()) start_pts.add(new PositionTuple(element_name, output_port));
			
			for(PositionTuple pt: start_pts) {
				ArrayList<PositionTuple> history = new ArrayList<PositionTuple>();
				traversePPM(net, pt, aps, history);
			}
		}
		
		return loops.size();
	}
	
	/*
	 * Detect loops of packet moved APs, starting from element_name
	 * Forward/NAT and ACL elements are divided
	 * No forwarding graph is constructed, directly traversing the elements
	 */
	public static int detectLoopDivisionDirect(Network net, String device, 
			Set<Integer> moved_aps) throws Exception 
	{	
		loops.clear();
		
		String element_name = net.getForwardElement(device);
		Element element = net.getElement(element_name);
		
		for(String port : element.getPorts()) {
			if(port.equals("default")) continue;
			
			HashSet<Integer> aps = null;
			HashSet<Integer> aclaps = null;
			
			// Step 1. initialize fwdaps and aclaps with moved_aps
			if(net.getElement(device) instanceof ACLElement) {
				// moved_aps are aclaps if device is ACLElement
				aps = new HashSet<Integer>(element.getPortAPs(port));
				aclaps = new HashSet<Integer>(moved_aps);
			}
			else {
				// moved_aps are fwdaps if device not ACLElement
				aps = new HashSet<Integer>(moved_aps);
				aps.retainAll(element.getPortAPs(port));
				aclaps = new HashSet<Integer>();
				aclaps.add(BDDACLWrapper.BDDTrue);
			}
			
			if(aps.isEmpty() || aclaps.isEmpty()) continue;
			
			// Step 2. start traversing
			
			Set<PositionTuple> start_pts = net.mapVlanIntoPhyPorts(new PositionTuple(element_name, port));
			if(start_pts.isEmpty()) start_pts.add(new PositionTuple(element_name, port));
			
			for(PositionTuple pt: start_pts) {
				ArrayList<PositionTuple> history = new ArrayList<PositionTuple>();
				traversePPM(net, pt, aps, aclaps, history);
			}
		}
		return loops.size();
	}
	
	/*
	 * Traverse the forwarding graph with only forward APs
	 */
	public static void traverseForwardingGraph(ForwardingGraph g, PositionTuple cur_hop, HashSet<Integer> aps, ArrayList<PositionTuple> history) 
	{
		Logger.logInfo("Traversing hop " + cur_hop + ":" + aps);
		
		aps.retainAll(g.getAPs(cur_hop));
		if (aps.size()==0){
			return;
		}
		
		if (history.contains(cur_hop)) {
			// find a loop
			history.add(cur_hop);
			Loop loop = new Loop(aps, history, cur_hop);
			loops.add(loop);
			return;
		}
		
		history.add(cur_hop);
		
		Set<PositionTuple> connected_pts = g.getConnectedPort(cur_hop);
		if (connected_pts == null) return;
		
		for(PositionTuple connected_pt : connected_pts) {
			for(PositionTuple next_pt : g.getPorts(connected_pt.getDeviceName())) {
				if(connected_pts.contains(next_pt)) continue;
				HashSet<Integer> new_aps = g.forwardAPs(cur_hop, aps);
				ArrayList<PositionTuple> new_history = new ArrayList<PositionTuple>(history);
				new_history.add(connected_pt);
				traverseForwardingGraph(g, next_pt, new_aps, new_history); 
			}
		}
	}
	
	/*
	 * Traverse the PPM model with only forward APs
	 */
	public static void traversePPM(Network net, PositionTuple cur_hop, 
			HashSet<Integer> cur_aps, ArrayList<PositionTuple> history) throws Exception 
	{	
		// check whether there is a loop
		if (history.contains(cur_hop)) {
			history.add(cur_hop);
			Loop loop = new Loop(cur_aps, history, cur_hop);
			loops.add(loop);
			return;
		}

		history.add(cur_hop);
		
		if (net.getConnectedPorts(cur_hop) == null) {
			return;
		}
		
		// one output port may map to multiple next hops
		for (PositionTuple connected_pt: net.getConnectedPorts(cur_hop)) {
			
			Element next_element = null;
			// get element by name
			String next_node_name = connected_pt.getDeviceName();
			if(!net.isACLNode(next_node_name)) {
				next_element = net.getElement(next_node_name);
			}
			else {
				next_element = net.getACLElement(next_node_name);
			}
			
			if (next_element == null) {
				throw new ElementNotFoundException(next_node_name);
			}
			
			// forward from next element
			for (String output_port : next_element.getPorts()) {
				// intersect current aps with port aps
				HashSet<Integer> filtered_aps = next_element.forwardAPs(output_port, cur_aps);
				if(filtered_aps.isEmpty()) continue;
				
				// get the output in next_element
				Set<PositionTuple> next_pts = new HashSet<>();
				if(next_element instanceof ForwardElement) {
					next_pts = net.mapVlanIntoPhyPorts(new PositionTuple(next_node_name, output_port));
				}
				if(next_pts.isEmpty()) next_pts.add(new PositionTuple(next_node_name, output_port));
						
				for(PositionTuple pt: next_pts) {
					ArrayList<PositionTuple> new_history = new ArrayList<PositionTuple>(history);
					new_history.add(connected_pt);
					traversePPM(net, pt, filtered_aps, new_history);
				}
			}
		}
	}
	
	/*
	 * Traverse the PPM model with both forward and ACL APs
	 */
	public static void traversePPM(Network net, PositionTuple cur_hop, 
			HashSet<Integer> fwdaps, HashSet<Integer> aclaps, 
			ArrayList<PositionTuple> history) throws Exception 
	{			
		if (history.contains(cur_hop)) {
			history.add(cur_hop);
			Loop loop = new Loop(fwdaps, history, cur_hop);
			loops.add(loop);
			return;
		}
		
		history.add(cur_hop);

		if (net.getConnectedPorts(cur_hop) == null) {
			return;
		}
		
		// one output port may map to multiple next hops
		for (PositionTuple connected_pt: net.getConnectedPorts(cur_hop)) {
			
			Element next_element = null;
			// get element by name
			String next_node_name = connected_pt.getDeviceName();
			if(!net.isACLNode(next_node_name)) {
				net.getElement(next_node_name);
			}
			else {
				next_element = net.getACLElement(next_node_name);
			}
			
			if (next_element == null) {
				throw new ElementNotFoundException(next_node_name);
			}
			
			// forward from next element
			for (String output_port : next_element.getPorts()) {
				// intersect current aps with port aps
				HashSet<Integer> filtered_aps = new HashSet<Integer>(fwdaps);
				HashSet<Integer> filtered_aclaps = new HashSet<Integer>(aclaps);
				
				if(next_element instanceof ACLElement) {
					filtered_aclaps = next_element.forwardAPs(output_port, aclaps);
				}
				else {
					filtered_aps = next_element.forwardAPs(output_port, fwdaps);
				}
				
				if(filtered_aps.isEmpty() || filtered_aclaps.isEmpty()) continue;
				if(!Element.hasOverlap(filtered_aps, filtered_aclaps)) continue;
				
				// get the output in next_element
				Set<PositionTuple> next_pts = new HashSet<>();
				if(next_element instanceof ForwardElement) {
					next_pts = net.mapVlanIntoPhyPorts(new PositionTuple(next_node_name, output_port));
				}
				if(next_pts.isEmpty()) next_pts.add(new PositionTuple(next_node_name, output_port));
						
				for(PositionTuple pt: next_pts) {
					ArrayList<PositionTuple> new_history = new ArrayList<PositionTuple>(history);
					new_history.add(connected_pt);
					traversePPM(net, pt, filtered_aps, filtered_aclaps, new_history);
				}
			}
		}
	}
}
