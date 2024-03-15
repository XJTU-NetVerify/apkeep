/*
 * Atomic Predicates Verifier
 * 
 * Copyright (c) 2013 UNIVERSITY OF TEXAS AUSTIN. All rights reserved. Developed
 * by: HONGKUN YANG and SIMON S. LAM http://www.cs.utexas.edu/users/lam/NRL/
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
 * 3. Neither the name of the UNIVERSITY OF TEXAS AUSTIN nor the names of the
 * developers may be used to endorse or promote products derived from this
 * Software without specific prior written permission.
 * 
 * 4. Any report or paper describing results derived from using any part of this
 * Software must cite the following publication of the developers: Hongkun Yang
 * and Simon S. Lam, Real-time Verification of Network Properties using Atomic
 * Predicates, IEEE/ACM Transactions on Networking, April 2016, Volume 24, No.
 * 2, pages 887-900 (first published March 2015, Digital Object Identifier:
 * 10.1109/TNET.2015.2398197).
 * 
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS WITH
 * THE SOFTWARE.
 */

package common;

import java.io.Serializable;
import java.util.HashSet;

/**
 * APSet1 optimizes set operations with complement of set whenever possible
 */

public class APSet1 implements Serializable {

	private static final long serialVersionUID = -1777981416095857486L;

	static int totalap;
	static HashSet<Integer> universeset;
	static double compoptThreshold = 0.5;
	boolean iscomplement;
	HashSet<Integer> apset;

	public int hashCode() {
		return apset.hashCode() ^ Boolean.toString(iscomplement).hashCode();
	}

	static void setcompThreshold(double thr) {
		compoptThreshold = thr;
	}

	public boolean contains(int predicate) {
		return iscomplement ^ apset.contains(predicate);
	}

	public boolean isempty() {
		if (!iscomplement && apset.isEmpty()) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isfull() {
		if (iscomplement && apset.isEmpty()) {
			return true;
		} else {
			return false;
		}
	}

	// directly set
	public static void setUniverse(HashSet<Integer> unv) {
		universeset = unv;
		totalap = universeset.size();
	}

	private void tocomplement() {
		HashSet<Integer> news = new HashSet<Integer>(universeset);
		news.removeAll(apset);
		apset = news;
		iscomplement = !iscomplement;
	}

	/**
	 * change the set to its complement, note that this is different than
	 * tocomplement()
	 */
	public void complement() {
		iscomplement = !iscomplement;
	}

	public void union(APSet1 apset2) {
		if (!iscomplement && !apset2.iscomplement) {
			unionNN(apset2.apset);
			return;
		}
		if (!iscomplement && apset2.iscomplement) {
			unionNC(apset2.apset);
			return;
		}
		if (iscomplement && !apset2.iscomplement) {
			unionCN(apset2.apset);
			return;
		}
		if (iscomplement && apset2.iscomplement) {
			unionCC(apset2.apset);
			return;
		}
	}

	public void intersect(APSet1 apset2) {
		if (!iscomplement && !apset2.iscomplement) {
			intersectNN(apset2.apset);
			return;
		}
		if (!iscomplement && apset2.iscomplement) {
			intersectNC(apset2.apset);
			return;
		}
		if (iscomplement && !apset2.iscomplement) {
			intersectCN(apset2.apset);
			return;
		}
		if (iscomplement && apset2.iscomplement) {
			intersectCC(apset2.apset);
			return;
		}
	}

	public void remove(int predicate) {
		if (iscomplement) {
			apset.add(predicate);
			complementopt();
		} else {
			apset.remove(predicate);
		}
	}

	/**
	 * copy an ap set
	 */
	public APSet1(APSet1 aps2) {
		iscomplement = aps2.iscomplement;
		apset = new HashSet<Integer>(aps2.apset);
	}

	public APSet1(HashSet<Integer> hs) {
		if (hs.size() == totalap) {
			SetAPSet(BDDACLWrapper.BDDTrue);
		} else if (hs.size() == 0) {
			SetAPSet(BDDACLWrapper.BDDFalse);
		} else {
			SetAPSet(hs);
		}
	}

	/**
	 * BDDACLFalse BDDACLTrue
	 * 
	 * @param settype
	 */
	public APSet1(int settype) {
		SetAPSet(settype);
	}

	// make sure hs is not allowall or denyall
	// directly set it
	private void SetAPSet(HashSet<Integer> hs) {
		apset = hs;
		iscomplement = false;
		complementopt();
	}

	private void complementopt() {
		if (apset.size() > compoptThreshold * totalap) {
			tocomplement();
		}
	}

	/**
	 * 1 - bddtrue 0 - bddfalse
	 * 
	 * @param settype
	 */
	private void SetAPSet(int settype) {
		apset = new HashSet<Integer>();
		if (settype == BDDACLWrapper.BDDTrue) {
			iscomplement = true;
		} else if (settype == BDDACLWrapper.BDDFalse) {
			iscomplement = false;
		} else {
			System.err.println("unsupported ap kind!");
		}
	}

	/**
	 * not complement
	 */
	private void unionNN(HashSet<Integer> hs) {
		apset.addAll(hs);
		complementopt();
	}

	private void intersectNN(HashSet<Integer> hs) {
		apset.retainAll(hs);
		// no need to do complementopt, since it is shrinking
	}

	/**
	 * comp(union(s1, comp(s2))) = comp(s2) - s1
	 * 
	 * @param hs
	 */
	private void unionNC(HashSet<Integer> comphs) {
		iscomplement = true;
		HashSet<Integer> news = new HashSet<Integer>(comphs);
		news.removeAll(apset);
		apset = news;
		// no need to do complementopt, it is shrinking
	}

	/**
	 * intersect(s1, s2) = s1 - comp(s2)
	 * 
	 * @param hs
	 */
	private void intersectNC(HashSet<Integer> comphs) {
		apset.removeAll(comphs);
		// no need to do complemntopt, since it is shrinking
	}

	/**
	 * comp(union(s1, s2)) = comp(s1) - s2
	 * 
	 * @param hs
	 */
	private void unionCN(HashSet<Integer> hs) {
		apset.removeAll(hs);
		// no need to do complementopt, it is shrinking
	}

	/**
	 * intersect(s1, s2) = s2 - comp(s1)
	 * 
	 * @param hs
	 */
	private void intersectCN(HashSet<Integer> hs) {
		iscomplement = false;
		HashSet<Integer> news = new HashSet<Integer>(hs);
		news.removeAll(apset);
		apset = news;
	}

	/**
	 * union(s1, s2) = comp(intersect(comp(s1), comp(s2)))
	 * 
	 * @param comphs
	 */
	private void unionCC(HashSet<Integer> comphs) {
		apset.retainAll(comphs);
	}

	/**
	 * comp(intersect(s1, s2)) = union(comp(s1), comp(s2))
	 * 
	 * @param comphs
	 */
	private void intersectCC(HashSet<Integer> comphs) {
		apset.addAll(comphs);
		complementopt();
	}

	public String toString() {
		String tos = "size: " + apset.size();
		// String tos = apset.toString();
		if (iscomplement) {
			tos = tos + " complement";
		}
		return tos;
	}

	/**
	 * 
	 * @return size of apset
	 */
	public int getSize() {
		return apset.size();
	}

}
