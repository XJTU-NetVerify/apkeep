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
