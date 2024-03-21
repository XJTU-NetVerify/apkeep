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
import java.util.List;
import java.util.Set;

import apkeep.core.APKeeper;
import common.PositionTuple;

public class Loop {
	Set<Integer> apset;
	List<PositionTuple> path;
	
	public Loop(Set<Integer> rewrited_aps, List<PositionTuple> history, PositionTuple cur_hop)
	{
		apset = rewrited_aps;
		path = history;
		while(path.size() > 0) {
			if(!path.get(0).equals(cur_hop)) {
				path.remove(0);
			}
			else break;
		}
	}
	
	public String toString()
	{
		HashSet<String> prefixes = APKeeper.getAPPrefixes(apset);
		String loop = "loop found for " + prefixes + ":\n";
		for (int i=0; i<path.size(); i++) {
			loop += path.get(i) + " ";
		}
		return "++++++++++++++++++++++++++++++\n" 
				+ loop 
				+ "\n++++++++++++++++++++++++++++++"; 
	}
	
	@Override
	public int hashCode(){
		return 0;
	}
	
	@Override
	public boolean equals (Object o) 
	{
		Loop loop = (Loop) o;
		//if (!apset.equals(loop.apset)) return false;
		if (path.size() != loop.path.size()) return false;
		int i;
		PositionTuple start = loop.path.get(0);
		for (i=0; i< path.size(); i++) {
			if (path.get(i).equals(start)){
				break;
			}
		}
		for (int j=0; j<path.size()-1; j++) {
			if (!path.get((i+j)%(path.size()-1)).equals(loop.path.get(j))) {
				return false;
			}
		}
		// the loop already exists, update the AP set for this loop
		apset.addAll(loop.apset);
		return true;
	}
}
