package apkeep.utils;

import java.util.ArrayList;
import java.util.List;
import apkeep.exception.LimitExceededException;
import apkeep.rules.Rule;

public class TrieTreeNode {
	
	final static int ipBits = 32;
	
	private List<Rule> matched_rules;
	
	int node_level;
	int node_value;
	
	// children [0]: bit 0; children[1]: bit 1; children[2]: bit *
	TrieTreeNode parent;
	TrieTreeNode[] children;
    
    public TrieTreeNode(int level, int value)
	{
		children = new TrieTreeNode[3];
		matched_rules = new ArrayList<>();
		node_level = level;
		node_value = value;
	}
    
	public TrieTreeNode insert(int[] prefixbin) {
		int index = prefixbin[node_level];	
		
		if (children[index] == null) {
			children[index] = new TrieTreeNode(node_level+1, index);
		}
		children[index].parent = this;
		
		// stop recursion when reaching wildcard or the bottom level
		if (index == 2 || node_level == 31) {
			return children[index];
		}
		
		return children[index].insert(prefixbin);
	}
	
	public TrieTreeNode search(int[] prefixbin) throws Exception {
		// should not reach here
		if (node_level > 31) {
			throw new LimitExceededException(node_level, 31);
		}
		
		int index = prefixbin[node_level];
		if (children[index] !=  null) {
			// stop recursion when reaching wildcard or the bottom level
			if (index == 2 || node_level == 31)
				return children[index];
			return children[index].search(prefixbin);
		}
		
		return null;
	}
	
    private List<TrieTreeNode> getDescendant() {
    	ArrayList<TrieTreeNode> descendants = new ArrayList<TrieTreeNode>();
		
		if (node_level == 32 && node_value != 2) {
			return descendants;
		}
		
		if (parent == null) {
			throw new NullPointerException();
		}
		
		if (parent.children[0] != null) {
			parent.children[0].getDescendantRecur(descendants);
		}
		
		if (parent.children[1] != null) {
			parent.children[1].getDescendantRecur(descendants);
		}
		
		return descendants;
	}
	
	private void getDescendantRecur(ArrayList<TrieTreeNode> descendants) {
		if (node_level == 32) {
			descendants.add(this);
			return;
		}
		
		if (children[2] != null) {
			descendants.add(children[2]);
			//return;
		}
		
		if (children[0] != null) {
			children[0].getDescendantRecur(descendants);
		}
		
		if (children[1] != null) {
			children[1].getDescendantRecur(descendants);
		}
		
		return;
	}
	
	private List<TrieTreeNode> getAncestor() {
		ArrayList<TrieTreeNode> ancestors = new ArrayList<TrieTreeNode>();
		
		// should not reach here
		if (parent == null) {
			throw new NullPointerException();
		}
		
		// search for all ancestors recursively
		getAncestorRecur(ancestors);
		if(ancestors.contains(this)) ancestors.remove(this);
		
		return ancestors;
	}

	private void getAncestorRecur(ArrayList<TrieTreeNode> ancestors) {
		if (parent != null) {
			if (parent.children[2] != null)
				ancestors.add(parent.children[2]);
			parent.getAncestorRecur(ancestors);
		}
	}

	public List<Rule> getDescendantRules() {
		List<Rule> rules = new ArrayList<>();
		List<TrieTreeNode> descendant = getDescendant();
		if(descendant == null) return rules;
		for(TrieTreeNode node : descendant) {
			rules.addAll(node.getRules());
		}
		return rules;
	}
	
	public List<Rule> getAncestorRules() {
		List<Rule> rules = new ArrayList<>();
		List<TrieTreeNode> ancestor = getAncestor();
		if(ancestor == null) return rules;
		for(TrieTreeNode node : ancestor) {
			rules.addAll(node.getRules());
		}
		return rules;
	}

	public void setParent(TrieTreeNode parent) {
		this.parent = parent;
	}
    public void setChild(int bit, TrieTreeNode child) {
    	children[bit] = child;
    }

	public void addRule(Rule rule) {
		if(!matched_rules.contains(rule)) matched_rules.add(rule);
	}
	
	public void removeRule(Rule rule) {
		matched_rules.remove(rule);
	}
	
	public void delete() {
		if (parent != null){
			parent.children[node_value] = null;
			if (!parent.hasChild()) {
				parent.delete();
			}
		}
	}
	
	public boolean noRules() {
		return matched_rules.isEmpty();
	}
	
	private boolean hasChild() {
		for (int i=0; i<3; i++) {
			if (children[i] != null) {
				return true;
			}
		}
		return false;
	}
	
	public Rule findRule(Rule rule) {
		for(Rule r : matched_rules) {
			if(r.equals(rule)) return r;
		}
		return null;
	}
	
	public boolean hasRule(Rule rule) {
		return matched_rules.contains(rule);
	}
	
	public List<Rule> getRules(){
		return matched_rules;
	}
}
