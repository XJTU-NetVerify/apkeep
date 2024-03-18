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
import common.BDDACLWrapper;
import common.PositionTuple;

public class BlackholeChecker extends Checker {
	
	static int blackhole_num = 0;

	public static int detectBlackhole(Network net, String device, Set<Integer> moved_aps) throws Exception {
		
		blackhole_num = 0;

		ForwardingGraph g = constructForwardingGraph(net, moved_aps);
				
		String element_name = net.getForwardElement(device);

		Set<PositionTuple> start_pts = g.getStartPorts(element_name);
		
		for(PositionTuple pt : start_pts) {
			HashSet<Integer> aps = new HashSet<>(g.getAPs(pt));
			ArrayList<PositionTuple> history = new ArrayList<>();
			traverseForwardingGraph(g, pt, aps, history);
		}
		
		return blackhole_num;
	}
	
	/*
	 * Detect blackholes due to moved APs, starting from element_name
	 * Forward/NAT and ACL elements are divided
	 */
	public static int detectBlackholeDivision(Network net, String device, Set<Integer> moved_aps) throws Exception {
		
		blackhole_num = 0;
		
		// construct the forwarding graph
		ForwardingGraph g = null;
		if(net.getElement(device) instanceof ACLElement) {
			g = constructForwardingGraphACL(net, moved_aps);
		}
		else {
			g = constructForwardingGraphFWD(net, moved_aps);
		}
		
		String element_name = net.getForwardElement(device);

		Set<PositionTuple> start_pts = g.getStartPorts(element_name);
		
		for(PositionTuple pt : start_pts) {
			
			HashSet<Integer> aps = null;
			HashSet<Integer> aclaps = null;
			
			if(net.getElement(device) instanceof ACLElement) {
				// moved_aps are aclaps if device is ACLElement
				aps = new HashSet<Integer>(g.getAPs(pt));
				aclaps = new HashSet<Integer>(moved_aps);
			}
			else {
				// moved_aps are fwdaps if device not ACLElement
				aps = new HashSet<Integer>(moved_aps);
				aclaps = new HashSet<Integer>();
				aclaps.add(BDDACLWrapper.BDDTrue);
			}
			
			ArrayList<PositionTuple> history = new ArrayList<>();
			traverseForwardingGraph(g, pt, aps, aclaps, history);
		}
		
		return blackhole_num;
	}

	private static void traverseForwardingGraph(ForwardingGraph g, PositionTuple cur_hop, HashSet<Integer> aps,
			ArrayList<PositionTuple> history) {
		
//		aps.retainAll(g.getAPs(cur_hop));
		if (aps.size()==0){
			return;
		}
		
		if (history.contains(cur_hop)) {
			return;
		}
		
		history.add(cur_hop);
		
		Set<PositionTuple> connected_pts = g.getConnectedPort(cur_hop);
		if (connected_pts == null) {
			if(cur_hop.getPortName().equals("default")) {
				blackhole_num++;
			}
			return;
		}
		
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
	
	private static void traverseForwardingGraph(ForwardingGraph g, PositionTuple cur_hop, HashSet<Integer> fwdaps,
			HashSet<Integer> aclaps, ArrayList<PositionTuple> history) {
		
		if (history.contains(cur_hop)) {
			return;
		}
		
		history.add(cur_hop);
		
		Set<PositionTuple> connected_pts = g.getConnectedPort(cur_hop);
		if (connected_pts == null) {
			if(cur_hop.getPortName().equals("default")) {
				blackhole_num++;
			}
			return;
		}
		
		for(PositionTuple connected_pt : connected_pts) {
			for(PositionTuple next_pt : g.getPorts(connected_pt.getDeviceName())) {
				if(connected_pts.contains(next_pt)) continue;
				
				HashSet<Integer> filtered_aps = new HashSet<Integer>(fwdaps);
				HashSet<Integer> filtered_aclaps = new HashSet<Integer>(aclaps);
				
				if(cur_hop.getPortName().equals("permit") || cur_hop.getPortName().equals("deny")) {
					filtered_aclaps = g.forwardAPs(cur_hop, aclaps);
				}
				else {
					filtered_aps = g.forwardAPs(cur_hop, fwdaps);
				}
				
				if(filtered_aps.isEmpty() || filtered_aclaps.isEmpty()) continue;
				if(!Element.hasOverlap(filtered_aps, filtered_aclaps)) continue;
				
				ArrayList<PositionTuple> new_history = new ArrayList<PositionTuple>(history);
				new_history.add(connected_pt);
				traverseForwardingGraph(g, next_pt, filtered_aps, filtered_aclaps, new_history); 
			}
		}
	}
}
