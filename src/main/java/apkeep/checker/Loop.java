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
