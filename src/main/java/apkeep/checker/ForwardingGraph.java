package apkeep.checker;

import java.util.Map;
import java.util.Set;

import common.PositionTuple;

public class ForwardingGraph {

	Map<PositionTuple, Set<Integer>> port_aps;
	Map<String, Set<PositionTuple>> node_ports;

	public ForwardingGraph(Map<PositionTuple, Set<Integer>> port_aps, 
			Map<String, Set<PositionTuple>> node_ports) {
		
		this.port_aps = port_aps;
		this.node_ports = node_ports;
	}

}
