package apkeep.elements;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import apkeep.core.ChangeItem;
import apkeep.rules.FilterRule;
import apkeep.rules.Rule;
import apkeep.utils.Logger;
import common.ACLRule;
import common.BDDACLWrapper;

public class ACLElement extends Element {
	
	private LinkedList<Rule> acl_rule;
	
	public ACLElement(String ename) {
		super(ename);
		acl_rule = new LinkedList<>();
	}

	@Override
	public void initialize() {
		// initialize the rule list with a default deny rule
		FilterRule rule = new FilterRule(BDDACLWrapper.BDDTrue,BDDACLWrapper.BDDTrue,"deny", -1);
		acl_rule.add(rule);
		
		// initialize the AP set for port deny
		String deny_port = "deny";
		HashSet<Integer> alltrue = new HashSet<Integer>();
		alltrue.add(BDDACLWrapper.BDDTrue);
		port_aps_raw.put(deny_port, alltrue);
		
		// initialize the AP set for port permit
		String permit_port = "permit";
		HashSet<Integer> allfalse = new HashSet<Integer>();
		port_aps_raw.put(permit_port, allfalse);
	}

	@Override
	public Rule encodeOneRule(String rule) {
		String[] tokens = rule.split(" ");
		ACLRule r = new ACLRule(rule.substring(tokens[0].length() + tokens[1].length() + tokens[2].length() + 3));
		
		int match_bdd = apk.encodeACLBDD(r);
		return new FilterRule(match_bdd, r);
	}

	@Override
	public List<ChangeItem> insertOneRule(Rule rule) throws Exception {
		List<ChangeItem> change_set = identifyChangesInsert(rule, acl_rule);
		port_aps_raw.putIfAbsent(rule.getPort(), new HashSet<Integer>());
		return change_set;
	}

	@Override
	public List<ChangeItem> removeOneRule(Rule rule) throws Exception {
		int index = findRule(rule);
		if(index == acl_rule.size()) {
			Logger.logInfo("Rule not found " + rule.toString());
			return new ArrayList<ChangeItem>();
		}
		Rule rule_to_remove = acl_rule.get(index);
		// remove if rule hits no packets
		if(rule_to_remove.getHit_bdd() == BDDACLWrapper.BDDFalse) {
			removeRule(index);
			Logger.logInfo("hidden rule deleted");
			return new ArrayList<ChangeItem>();
		}
		
		List<ChangeItem> change_set = identifyChangesRemove(rule_to_remove, acl_rule);
		removeRule(index);
		return change_set;
	}

	private int findRule(Rule rule) {
		int index = 0;
		for(Rule r : acl_rule) {
			if(r.equals(rule)) return index;
			index++;
		}
		return index;
	}
	
	private void removeRule(int index) {
		bdd.deref(acl_rule.get(index).getMatch_bdd());
		acl_rule.remove(index);
	}
	@Override
	protected int tryMergeIfNATElement(int delta) {
		return delta;
	}
}
