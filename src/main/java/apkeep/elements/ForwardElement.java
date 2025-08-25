package apkeep.elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import apkeep.core.ChangeItem;
import apkeep.rules.ForwardingRule;
import apkeep.rules.Rule;
import apkeep.utils.Logger;
import apkeep.utils.TrieTree;
import apkeep.utils.TrieTreeNode;
import common.BDDACLWrapper;

public class ForwardElement extends Element {
	
	TrieTree trie;
	Map<String, Set<String>> vlan_ports;

	public ForwardElement(String ename) {
		super(ename);
		trie = new TrieTree();
		vlan_ports = new HashMap<>();
	}

	@Override
	public void initialize() {
		String default_port = "default";
		HashSet<Integer> alltrue = new HashSet<Integer>();
		alltrue.add(BDDACLWrapper.BDDTrue);
		port_aps_raw.put(default_port, alltrue);
	}

	public void addVlanPorts(Map<String, Set<String>> map) {
		vlan_ports.putAll(map);
	}
	
	public Set<String> getVlanPorts(String vlan){
		if(vlan_ports.containsKey(vlan)) return vlan_ports.get(vlan);
		return new HashSet<>();
	}

	@Override
	public Rule encodeOneRule(String rule) {
		String[] tokens = rule.split(" ");
		long prefix = Long.valueOf(tokens[3]);
		int prefixlen = Integer.valueOf(tokens[4]);
		String port = tokens[5];
		int priority = Integer.valueOf(tokens[6]);
		
		int match_bdd = apk.encodePrefixBDD(prefix, prefixlen);
		
		return new ForwardingRule(match_bdd, prefix, prefixlen, port, priority);
	}

	@Override
	public List<ChangeItem> insertOneRule(Rule rule) throws Exception {
		// find the node in the trie
		TrieTreeNode node = trie.insert((ForwardingRule) rule);
		
		// duplicate rules
		if (node.hasRule(rule)) {
			Logger.logInfo("duplicate rule " + rule.toString());
			return new ArrayList<ChangeItem>();
		}
		
		// get the affected rules
		ArrayList<Rule> affected_rules = getAffectedRules(node);
		List<ChangeItem> change_set = identifyChangesInsert(rule, affected_rules);
		
		// check whether the forwarding port exists, if not create it, 
		// and initialize the AP set of the port to empty
		port_aps_raw.putIfAbsent(rule.getPort(), new HashSet<Integer>());
		
		// insert the rule
		node.addRule(rule);
		return change_set;
	}

	@Override
	public List<ChangeItem> removeOneRule(Rule rule) throws Exception {
		// find the node in the trie
		TrieTreeNode node = trie.search((ForwardingRule) rule);
		if(node == null) {
			Logger.logInfo("Node not found " + rule.toString());
			return new ArrayList<ChangeItem>();
		}
		
		// find the rule in the node
		Rule rule_to_remove = node.findRule(rule);
		if(rule_to_remove == null) {
			Logger.logInfo("Rule not found " + rule.toString());
			return new ArrayList<ChangeItem>();
		}
		
		// remove if rule hits no packets
		if(rule_to_remove.getHit_bdd() == BDDACLWrapper.BDDFalse) {
			removeRule(node, rule_to_remove);
			Logger.logInfo("hidden rule deleted");
			return new ArrayList<ChangeItem>();
		}
		
		// get the affected rules
		ArrayList<Rule> affected_rules = getAffectedRules(node);
		List<ChangeItem> change_set = identifyChangesRemove(rule_to_remove, affected_rules);
		
		removeRule(node, rule_to_remove);
		return change_set;
	}
	
	private ArrayList<Rule> getAffectedRules(TrieTreeNode node) {
		ArrayList<Rule> affected_rules = new ArrayList<>();
		affected_rules.addAll(node.getDescendantRules());
		affected_rules.addAll(node.getAncestorRules());
		affected_rules.addAll(node.getRules());
		Arrays.sort(affected_rules.toArray());
		return affected_rules;
	}
	
	private void removeRule(TrieTreeNode node, Rule rule) {
		node.removeRule(rule);
		if(node.noRules()) {
			ForwardingRule r = (ForwardingRule) rule;
			apk.removePrefixBDD(r.getDstIP(), r.getMaskLen());
			bdd.deref(rule.getMatch_bdd());
        	node.delete();
		}
	}

	@Override
	protected int tryMergeIfNATElement(int delta) {
		return delta;
	}
}
