package apkeep.rules;

import apkeep.core.APKeeper;
import common.Fields;

public class RewriteRule extends Rule{
	
	private int field_bdd;
	private int new_pkt_bdd;
	
	public RewriteRule(int old_bdd, int new_bdd, String rname, int p)
	{
		super(old_bdd, p, rname);
		new_pkt_bdd = new_bdd;
		field_bdd = APKeeper.bddengine.get_field_bdd(Fields.dst_ip);
	}
	
	public RewriteRule(int old_bdd, int hit_bdd, int new_bdd, String rname, int p)
	{
		super(old_bdd, hit_bdd, p, rname);
		new_pkt_bdd = new_bdd;
		field_bdd = APKeeper.bddengine.get_field_bdd(Fields.dst_ip);
	}
	
	public int getField_bdd() {
		return field_bdd;
	}

	public int getNew_pkt_bdd() {
		return new_pkt_bdd;
	}

	public String toString()
	{
		return "";
	}
}
