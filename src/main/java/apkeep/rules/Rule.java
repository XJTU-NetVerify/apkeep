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

import common.BDDACLWrapper;

public abstract class Rule implements Comparable<Rule>{
	
	protected int match_bdd;
	protected int hit_bdd;

	protected int priority;
	protected String port;
	
	public Rule(int match_bdd, int priority, String port) {
		this.priority = priority;
		this.port = port;
		this.match_bdd = match_bdd;
		this.hit_bdd = BDDACLWrapper.BDDFalse;
	}
	
	protected Rule(int match_bdd, int hit_bdd, int priority, String port) {
		this.priority = priority;
		this.port = port;
		this.match_bdd = match_bdd;
		this.hit_bdd = hit_bdd;
	}

	public void setHit_bdd(int hit_bdd) {
		this.hit_bdd = hit_bdd;
	}
	
	public int getMatch_bdd() {
		return match_bdd;
	}

	public int getHit_bdd() {
		return hit_bdd;
	}
	
	public int getPriority() {
		return priority;
	}
	
	public String getPort() {
		return port;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof Rule) {
			Rule another = (Rule) o;
			return another.priority == priority 
					&& another.port.equals(port);
		}
		return false;
	}
	
	@Override
	public int compareTo(Rule a) {
		return a.priority - priority;
	}
}
