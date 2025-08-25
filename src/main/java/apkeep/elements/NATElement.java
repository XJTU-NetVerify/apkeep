package apkeep.elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import apkeep.core.ChangeItem;
import apkeep.exception.APNotFoundException;
import apkeep.rules.RewriteRule;
import apkeep.rules.Rule;
import apkeep.utils.Logger;
import common.BDDACLWrapper;
import common.PositionTuple;
import common.Utility;

public class NATElement extends Element {

	LinkedList<Rule> rewrite_rules;
	HashMap<Integer, HashSet<Integer>> rewrite_table;
	HashSet<Integer> output_aps;
	HashMap<String, Rule> rule_map;

	public NATElement(String ename) {
		super(ename);
		rewrite_rules = new LinkedList<>();
		rewrite_table = new HashMap<Integer, HashSet<Integer>>();
		output_aps = new HashSet<Integer>();
		rule_map = new HashMap<>();
	}

	@Override
	public void initialize() {
		// initialize the AP set for default rewrite rule
		RewriteRule default_rule = new RewriteRule(BDDACLWrapper.BDDTrue, BDDACLWrapper.BDDTrue, 
				BDDACLWrapper.BDDTrue, "default", 0);
		rewrite_rules.add(default_rule);
		rule_map.put(default_rule.getPort(), default_rule);
		
		HashSet<Integer> alltrue = new HashSet<Integer>();
		alltrue.add(BDDACLWrapper.BDDTrue);
		port_aps_raw.put(default_rule.getPort(), alltrue);
	}
	
	public HashMap<Integer, HashSet<Integer>> getRewrite_table() {
		return rewrite_table;
	}
	
	@Override
	public HashSet<Integer> forwardAPs(String port, Set<Integer> aps){
		HashSet<Integer> new_aps = new HashSet<>(aps);
		if(new_aps.remove(BDDACLWrapper.BDDTrue)) {
			new_aps = new HashSet<>(getPortAPs(port));
		}
		else {
			new_aps.retainAll(getPortAPs(port));
		}
		return rewriteAPs(new_aps);
	}
	
	public HashSet<Integer> rewriteAPs(HashSet<Integer> old_aps) {
		HashSet<Integer> new_aps = new HashSet<Integer>();
		for (int ap : old_aps) {
			if (rewrite_table.containsKey(ap)) {
				new_aps.addAll(rewrite_table.get(ap));
			}
			else {
				new_aps.add(ap);
			}
		}
		return new_aps;
	}

	public boolean isMergable(int ap1, int ap2) {
		if (!output_aps.contains(ap1) && !output_aps.contains(ap2)) 
			return true;
		if (output_aps.contains(ap1) && !output_aps.contains(ap2)) 
			return false;
		if (!output_aps.contains(ap1) && output_aps.contains(ap2)) 
			return false;
		
		for (HashSet<Integer> aps : rewrite_table.values()) {
			if (aps.contains(ap1) && !aps.contains(ap2)) {
				return false;
			}
			if (!aps.contains(ap1) && aps.contains(ap2)) {
				return false;
			}
		}
		return true;
	}

	public boolean isMergable(HashSet<Integer> aps) {
		if(checkIntersect(aps, output_aps)) return false;
		
		for (HashSet<Integer> rewrited_aps : rewrite_table.values()) {
			if(checkIntersect(aps, rewrited_aps)) return false;
		}
		return true;
	}
	
	private boolean checkIntersect(Set<Integer> a, Set<Integer> b) {
		Set<Integer> copy = new HashSet<Integer>(a);
		copy.removeAll(b);
		return !copy.equals(a) && !copy.isEmpty();
	}

	@Override
	public Rule encodeOneRule(String rule) {
		String[] tokens = rule.split(" ");
		long old_prefix = Utility.IPStringToLong(tokens[4]);
		int old_prefixlen = Integer.valueOf(tokens[5]);
		String new_ip = tokens[6];
		long new_prefix = Utility.IPStringToLong(tokens[6]);
		int new_prefixlen = Integer.valueOf(tokens[7]);
		
		int old_bdd = apk.encodePrefixBDD(old_prefix, old_prefixlen);
		int new_bdd = apk.encodePrefixBDD(new_prefix, new_prefixlen);
		
		return new RewriteRule(old_bdd, new_bdd, new_ip, 65535);
	}

	@Override
	public List<ChangeItem> insertOneRule(Rule rule) throws Exception {
		List<ChangeItem> change_set = identifyChangesInsert(rule, rewrite_rules);
		rule_map.put(rule.getPort(), rule);
		port_aps_raw.putIfAbsent(rule.getPort(), new HashSet<Integer>());
		return change_set;
	}

	@Override
	public List<ChangeItem> removeOneRule(Rule rule) throws Exception {
		int index = findRule(rule);
		if(index == rewrite_rules.size()) {
			Logger.logInfo("Rule not found " + rule.toString());
			return new ArrayList<ChangeItem>();
		}
		Rule rule_to_remove = rewrite_rules.get(index);
		// remove if rule hits no packets
		if(rule_to_remove.getHit_bdd() == BDDACLWrapper.BDDFalse) {
			removeRule(index);
			Logger.logInfo("hidden rule deleted");
			return new ArrayList<ChangeItem>();
		}
		
		List<ChangeItem> change_set = identifyChangesRemove(rule_to_remove, rewrite_rules);
		removeRule(index);
		return change_set;
	}
	
	private int findRule(Rule rule) {
		int index = 0;
		for(Rule r : rewrite_rules) {
			if(r.equals(rule)) return index;
			index++;
		}
		return index;
	}
	
	private void removeRule(int index) {
		bdd.deref(rewrite_rules.get(index).getMatch_bdd());
		rewrite_rules.remove(index);
	}
	
	@Override
	protected void transferOneAP(String from_port, String to_port, int delta) {
		port_aps_raw.get(from_port).remove(delta);
		port_aps_raw.get(to_port).add(delta);
		
		if (rewrite_table.containsKey(delta)) {
			HashSet<Integer> old_aps = rewrite_table.get(delta);
			output_aps.removeAll(old_aps);
			while (true) {
				HashSet<Integer> new_aps = new HashSet<Integer>(old_aps); 
				for (int one_ap : new_aps) {
					if (apk.hasAP(one_ap)) {
						try {
							apk.tryMergeAP(one_ap);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				if (old_aps.size() == new_aps.size())
					break;
			}
				
			old_aps.clear();
		}
		else {
			rewrite_table.put(delta, new HashSet<Integer>());
		}
		
		if (to_port.equals("default")) {
			rewrite_table.remove(delta);
		}
		else {
			RewriteRule rule = (RewriteRule) rule_map.get(to_port);
			int delta_rewrite = bdd.nat(delta, rule.getField_bdd(), rule.getNew_pkt_bdd());
			rewrite_table.get(delta).add(delta_rewrite);
		}
		
		// update the AP edge reference		 
		try {
			apk.updateTransferAP(new PositionTuple(name, from_port), new PositionTuple(name, to_port), delta);
		} catch (APNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void updateAPSplit(String portname, int origin, int parta, int partb) throws Exception {
		Set<Integer> apset = port_aps_raw.get(portname);
		if(!apset.contains(origin)) {
			throw new APNotFoundException(origin);
		}
		apset.remove(origin);
		apset.add(parta);
		apset.add(partb);
		
		for (HashSet<Integer> aps : rewrite_table.values()) {
			if (aps.contains(origin)) {
				aps.remove(origin);
				aps.add(parta);
				aps.add(partb);
				output_aps.remove(origin);
				output_aps.add(parta);
				output_aps.add(partb);
			}
		}
		
		if (!rewrite_table.containsKey(origin))
			return;
		// update the rewrite table
		if (rewrite_table.get(origin) != null) {
			output_aps.removeAll(rewrite_table.get(origin));
		}
		rewrite_table.remove(origin);
		RewriteRule rule = (RewriteRule) rule_map.get(portname);
		int parta_rewrite = bdd.nat(parta, rule.getField_bdd(), rule.getNew_pkt_bdd());
		int partb_rewrite = bdd.nat(partb, rule.getField_bdd(), rule.getNew_pkt_bdd());
		HashSet<Integer> parta_apset = new HashSet<Integer>();
		parta_apset.add(parta_rewrite);
		rewrite_table.put(parta, parta_apset);
		output_aps.add(parta_rewrite);
		HashSet<Integer> partb_apset = new HashSet<Integer>();
		partb_apset.add(partb_rewrite);
		rewrite_table.put(partb, partb_apset);
		output_aps.add(partb_rewrite);
	}
	
	@Override
	public void updateAPSetMerge(String port, int merged_ap, int ap1, int ap2) throws Exception {
		Set<Integer> apset = port_aps_raw.get(port);
		if(!apset.contains(ap1)) {
			throw new APNotFoundException(ap1);
		}
		if(!apset.contains(ap2)) {
			throw new APNotFoundException(ap2);
		}
		apset.remove(ap1);
		apset.remove(ap2);
		apset.add(merged_ap);
		
		for (HashSet<Integer> aps : rewrite_table.values()) {
			if (aps.contains(ap1) && aps.contains(ap2)) {
				aps.remove(ap1);
				aps.remove(ap2);
				aps.add(merged_ap);
				output_aps.remove(ap1);
				output_aps.remove(ap2);
				output_aps.add(merged_ap);
			}
		}
			
		if (!rewrite_table.containsKey(ap1) && !rewrite_table.containsKey(ap2))
			return;
		
		// update the rewrite table
		if (rewrite_table.get(ap1) != null) {
			output_aps.removeAll(rewrite_table.get(ap1));
		}
		if (rewrite_table.get(ap2) != null) {
			output_aps.removeAll(rewrite_table.get(ap2));
		}
			
		rewrite_table.remove(ap1);
		rewrite_table.remove(ap2);
		RewriteRule rule = (RewriteRule) rule_map.get(port);
		int merged_rewrite = bdd.nat(merged_ap, rule.getField_bdd(), rule.getNew_pkt_bdd());
		HashSet<Integer> merged_apset = new HashSet<Integer>();
		merged_apset.add(merged_rewrite);
		rewrite_table.put(merged_ap, merged_apset);
		output_aps.add(merged_rewrite);
	}
	
	public boolean updateRewriteTable() throws Exception {
		HashMap<Integer, HashSet<Integer>> old_rewrite_table = new HashMap<Integer, HashSet<Integer>>(rewrite_table);
		for (int ap : old_rewrite_table.keySet()) {
			HashSet<Integer> rewrited_aps = rewrite_table.get(ap);
			for (int rewrited_ap : rewrited_aps) {
				if (!apk.hasAP(rewrited_ap)) {
					apk.addPredicate(rewrited_ap);
					rewrited_aps.remove(rewrited_ap);
					output_aps.remove(rewrited_ap);
					HashSet<Integer> new_rewited_ap = apk.getAPExp(rewrited_ap);
					rewrited_aps.addAll(new_rewited_ap);
					output_aps.addAll(new_rewited_ap);
					return true;
				}
			}
		}
		
		return false;
	}
	
	@Override
	protected void updateRewriteTableIfPresent() {
		boolean updated = true;
		int update_round = 1;
		while(updated) {
			try {
				updated = updateRewriteTable();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Logger.logInfo("Update rewrite table round " + update_round++);
		}
	}

	@Override
	protected int tryMergeIfNATElement(int delta) {
		try {
			return apk.tryMergeAP(delta);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return delta;
	}
}
