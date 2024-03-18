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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import apkeep.elements.Element;
import apkeep.elements.NATElement;
import common.PositionTuple;

public class ForwardingGraph {

	Map<PositionTuple, HashSet<PositionTuple>> topology;
	Map<String, Set<PositionTuple>> node_ports;
	Map<PositionTuple, Set<Integer>> port_aps;
	
	Map<String, Element> elements;
	
	public ForwardingGraph(Map<String, Set<PositionTuple>> node_ports, 
			Map<PositionTuple, Set<Integer>> port_aps, 
			Map<PositionTuple, HashSet<PositionTuple>> topo,
			Map<String, Element> elements) {
		this.node_ports = node_ports;
		this.port_aps = port_aps;
		topology = topo;
		this.elements = elements;
	}

	public Set<PositionTuple> getStartPorts(String element_name) {
		Set<PositionTuple> pts = new HashSet<>();
		if(!node_ports.containsKey(element_name)) return pts;
		for(PositionTuple pt : node_ports.get(element_name)) {
			if(!pt.getPortName().equals("default")) pts.add(pt);
		}
		return pts;
	}

	public Set<Integer> getAPs(PositionTuple pt) {
		return port_aps.get(pt);
	}

	public Set<PositionTuple> getConnectedPort(PositionTuple cur_hop) {
		return topology.get(cur_hop);
	}
	
	public Set<PositionTuple> getPorts(String node) {
		if(node_ports.containsKey(node)) return node_ports.get(node);
		return new HashSet<>();
	}

	public HashSet<Integer> forwardAPs(PositionTuple cur_hop, HashSet<Integer> aps) {
		Element e = elements.get(cur_hop.getDeviceName());
		if(e == null) return new HashSet<>(aps);
		if(e instanceof NATElement) return e.forwardAPs(cur_hop.getPortName(), aps);
		return new HashSet<>(port_aps.get(cur_hop));
	}
}
