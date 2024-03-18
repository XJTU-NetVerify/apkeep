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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import apkeep.core.Network;
import apkeep.elements.ACLElement;
import apkeep.elements.Element;
import apkeep.exception.APNullReferenceException;
import common.PositionTuple;

public class Checker {

	protected static ForwardingGraph constructForwardingGraph(Network net, Set<Integer> moved_aps) throws Exception{
		if(moved_aps.size() == 0) return null;
		
		Set<Integer> rewrited_aps = net.rewriteAllAPs(moved_aps);
		
		Map<PositionTuple, Set<Integer>> port_aps = new HashMap<>();
		Map<String, Set<PositionTuple>> node_ports = new HashMap<>();
		Map<String, Element> elements = new HashMap<>();
		
		for(int ap : rewrited_aps) {
			Set<PositionTuple> hold_ports = net.getHoldPorts(ap);
			if(hold_ports == null) {
				throw new APNullReferenceException(ap);
			}
			
			for(PositionTuple pt : hold_ports) {
				elements.putIfAbsent(pt.getDeviceName(), net.getElement(pt.getDeviceName()));
			}
			
			Set<PositionTuple> topo_ports = net.mapElementPortIntoTopoPort(hold_ports);
			
			for(PositionTuple pt : topo_ports) {
				port_aps.putIfAbsent(pt, new HashSet<>());
				port_aps.get(pt).add(ap);
				
				node_ports.putIfAbsent(pt.getDeviceName(), new HashSet<>());
				node_ports.get(pt.getDeviceName()).add(pt);
			}
		}
		
		Map<PositionTuple, HashSet<PositionTuple>> topo = new HashMap<>();
		for(PositionTuple pt : port_aps.keySet()) {
			if(net.containsPort(pt)) {
				topo.put(pt, net.getConnectedPorts(pt));
			}
		}
		
		ForwardingGraph g = new ForwardingGraph(node_ports, port_aps, topo, elements);
		return g;
	}
	
	protected static ForwardingGraph constructForwardingGraphFWD(Network net, Set<Integer> moved_aps) throws Exception {
		if(moved_aps.size() == 0) return null;
		
		Map<PositionTuple, Set<Integer>> port_aps = new HashMap<>();
		Map<String, Set<PositionTuple>> node_ports = new HashMap<>();
		Map<String, Element> elements = new HashMap<>();
		
		// add forwarding elements using fwd_apk
		for(int ap : moved_aps) {
			Set<PositionTuple> hold_ports = net.getHoldPorts(ap);
			if(hold_ports == null) {
				throw new APNullReferenceException(ap);
			}
			
			for(PositionTuple pt : hold_ports) {
				elements.putIfAbsent(pt.getDeviceName(), net.getElement(pt.getDeviceName()));
			}
			
			Set<PositionTuple> topo_ports = net.mapElementPortIntoTopoPort(hold_ports);
			
			for(PositionTuple pt : topo_ports) {
				port_aps.putIfAbsent(pt, new HashSet<>());
				port_aps.get(pt).add(ap);
				
				node_ports.putIfAbsent(pt.getDeviceName(), new HashSet<>());
				node_ports.get(pt.getDeviceName()).add(pt);
			}
		}
		
		// add all acl nodes
		for(String acl_node_name : net.getACLNodes()) {
			Element acl_element = net.getACLElement(acl_node_name);
			elements.putIfAbsent(acl_element.getName(), acl_element);
			PositionTuple permitport = new PositionTuple(acl_node_name, "permit");
			port_aps.put(permitport, acl_element.getPortAPs("permit"));
			HashSet<PositionTuple> new_ports = new HashSet<PositionTuple>();
			new_ports.add(permitport);
			node_ports.put(acl_node_name, new_ports);
		}
		
		// build topo using port_aps
		Map<PositionTuple, HashSet<PositionTuple>> topo = new HashMap<>();
		for(PositionTuple pt : port_aps.keySet()) {
			if(net.containsPort(pt)) {
				topo.put(pt, net.getConnectedPorts(pt));
			}
		}
		
		ForwardingGraph g = new ForwardingGraph(node_ports, port_aps, topo, elements);
		return g;
	}
	
	protected static ForwardingGraph constructForwardingGraphACL(Network net, Set<Integer> moved_aps) throws Exception {
		if(moved_aps.size() == 0) return null;
		
		Map<PositionTuple, Set<Integer>> port_aps = new HashMap<>();
		Map<String, Set<PositionTuple>> node_ports = new HashMap<>();
		Map<String, Element> elements = new HashMap<>();
		
		// add acl elements using acl_apk
		for(int ap : moved_aps) {
			Set<PositionTuple> hold_ports = net.getACLHoldPorts(ap);
			if(hold_ports == null) {
				throw new APNullReferenceException(ap);
			}
			
			for(PositionTuple pt : hold_ports) {
				elements.putIfAbsent(pt.getDeviceName(), net.getElement(pt.getDeviceName()));
			}
			
			Set<PositionTuple> topo_ports = net.mapElementPortIntoTopoPort(hold_ports);
			
			for(PositionTuple pt : topo_ports) {
				port_aps.putIfAbsent(pt, new HashSet<>());
				port_aps.get(pt).add(ap);
				
				node_ports.putIfAbsent(pt.getDeviceName(), new HashSet<>());
				node_ports.get(pt.getDeviceName()).add(pt);
			}
		}
		
		// add all forwarding element
		for(Element e : net.getAllElements()) {
			if(e instanceof ACLElement) continue;
			elements.putIfAbsent(e.getName(), e);
			node_ports.putIfAbsent(e.getName(), new HashSet<>());
			for(String port : e.getPorts()) {
				PositionTuple pt = new PositionTuple(e.getName(), port);
				Set<PositionTuple> hold_ports = new HashSet<>();
				hold_ports.add(pt);
				
				Set<PositionTuple> topo_ports = net.mapElementPortIntoTopoPort(hold_ports);
				
				node_ports.get(e.getName()).addAll(topo_ports);
				
				for(PositionTuple pt1 : topo_ports) {
					port_aps.putIfAbsent(pt1, new HashSet<>());
					port_aps.get(pt1).addAll(e.getPortAPs(port));
				}
			}
		}
		
		// build topo using port_aps
		Map<PositionTuple, HashSet<PositionTuple>> topo = new HashMap<>();
		for(PositionTuple pt : port_aps.keySet()) {
			if(net.containsPort(pt)) {
				topo.put(pt, net.getConnectedPorts(pt));
			}
		}
		
		ForwardingGraph g = new ForwardingGraph(node_ports, port_aps, topo, elements);
		return g;
	}
}
