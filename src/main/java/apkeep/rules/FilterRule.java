package apkeep.rules;

import common.ACLRule;

public class FilterRule extends Rule {
	
	public String accessList;
	public String accessListNumber;
	public String protocolLower;
	public String protocolUpper;
	public String source;
	public String sourceWildcard;
	public String sourcePortLower;
	public String sourcePortUpper;
	public String destination;
	public String destinationWildcard;
	public String destinationPortLower;
	public String destinationPortUpper;

	public FilterRule(int match_bdd, int hit_bdd, String port, int priority) {
		super(match_bdd, hit_bdd, priority, port);
		this.accessList = null;
		this.accessListNumber = null;
		this.protocolLower = null;
		this.protocolUpper = null;
		this.source = null;
		this.sourceWildcard = null;
		this.sourcePortLower = null;
		this.sourcePortUpper = null;
		this.destination = null;
		this.destinationWildcard = null;
		this.destinationPortLower = null;
		this.destinationPortUpper = null;
	}

	public FilterRule(int match_bdd, ACLRule rule) {
		super(match_bdd, rule.priority, rule.permitDeny);
		this.accessList = rule.accessList;
		this.accessListNumber = rule.accessListNumber;
		this.protocolLower = rule.protocolLower;
		this.protocolUpper = rule.protocolUpper;
		this.source = rule.source;
		this.sourceWildcard = rule.sourceWildcard;
		this.sourcePortLower = rule.sourcePortLower;
		this.sourcePortUpper = rule.sourcePortUpper;
		this.destination = rule.destination;
		this.destinationWildcard = rule.destinationWildcard;
		this.destinationPortLower = rule.destinationPortLower;
		this.destinationPortUpper = rule.destinationPortUpper;		
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof FilterRule) {
			FilterRule another = (FilterRule) o;
			if(this.toString().equals(another.toString()))
				return true;
		}
		return false;
	}
	
	public String toString() {
		return accessList
				+ " "
				+ accessListNumber
				+ " "
				+ port + " " + protocolLower + " " + protocolUpper + " "
				+ source + " " + sourceWildcard + " " + sourcePortLower + " "
				+ sourcePortUpper + " " + destination + " "
				+ destinationWildcard + " " + destinationPortLower + " "
				+ destinationPortUpper + " "
				+ priority
		;
	}
}
