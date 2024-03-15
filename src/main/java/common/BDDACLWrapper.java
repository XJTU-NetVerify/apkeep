/*
 * Atomic Predicates for Transformers
 * 
 * Copyright (c) 2015 UNIVERSITY OF TEXAS AUSTIN. All rights reserved. Developed
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
 * and Simon S. Lam, Scalable Verification of Networks With Packet Transformers
 * Using Atomic Predicates, IEEE/ACM Transactions on Networking, October 2017,
 * Volume 25, No. 5, pages 2900-2915 (first published as IEEE Early Access
 * Article, July 2017, Digital Object Identifier: 10.1109/TNET.2017.2720172).
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

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import jdd.bdd.*;
import jdd.bdd.debug.DebugBDD;

/**
 * The true, false, bdd variables, the negation of bdd variables:
 * their reference count are already set to maximal, so they will never be 
 * garbage collected. And no need to worry about the reference count for them.
 * @author carmo
 *
 */
public class BDDACLWrapper implements Serializable{

      /**
       * 
       */
      private static final long serialVersionUID = 7284490986562707221L;

      BDD aclBDD;

      /**
       * the arrays store BDD variables.
       */
      final static int protocolBits = 8;
      int[] protocol; //protocol[0] is the lowest bit
      final static int portBits = 16;
      int[] srcPort;
      int[] dstPort;
      final static int ipBits = 32;
      int[] srcIP;
      int[] dstIP;
      int[] dstIPInner;
      final static int mplsBits = 20;
      // mplsLabel[0] - least significant bit
      int[] mplsLabel;
      final static int ip6Bits = 128;
      int[] dstIP6;

      public int mplsLabelField;
      public int mplsLabelFieldDecoration;
      int dstIPField;
      //int dstIPInnerField;

      // these bits (bdd variables) are used to indicate whether a particular field exists 
      // in a bdd representation
      int mplsLabelBit;
      int dstIPBit;
      int srcIPBit;
      int srcPortBit;
      int dstPortBit;
      int dstIP6Bit;
      int protocolBit;
      int dstIPInnerBit;

      Permutation push_perm;
      Permutation pop_perm;

      /**
       * for readability. In bdd:
       * 0 is the false node
       * 1 is the true node
       */
      public final static int BDDFalse = 0;
      public final static int BDDTrue = 1;

      public BDDACLWrapper()
      {
            // for debugging 
            //aclBDD = new DebugBDD(100000, 10000);
            
            // normal
            //aclBDD = new BDD(100000000, 1000000);
            aclBDD = new BDD(10000000, 10000000);
            //aclBDD = new BDD(10000000, 1000000);
            
            protocol = new int[protocolBits];
            srcPort = new int[portBits];
            dstPort = new int[portBits];
            srcIP = new int[ipBits];
            dstIP = new int[ipBits];
            dstIPInner = new int[ipBits];
            mplsLabel = new int[mplsBits];
            dstIP6 = new int[ip6Bits];

            /**
             * will try more orders of variables
             */
            DeclareSrcIP();
            DeclareDstIP();
            DeclareDstIPInner();
            DeclareSrcPort();
            DeclareDstPort();
            DeclareProtocol();
            DeclareMPLSLabel();
            DeclareDstIP6();

            mplsLabelField = AndInBatch(mplsLabel);
            mplsLabelFieldDecoration = 
                        aclBDD.ref(aclBDD.and(mplsLabelField, mplsLabelBit));
            dstIPField = AndInBatch(dstIP);
            //dstIPInnerField = AndInBatch(dstIPInner);
      }
      
      public void createIPinIPPermutation()
      {
            push_perm = aclBDD.createPermutation(dstIP, dstIPInner);
            pop_perm = aclBDD.createPermutation(dstIPInner, dstIP);
      }

      public int get_field_bdd(Fields field_name)
      {
            switch(field_name){
            case dst_ip: return dstIPField;
            default: return BDDFalse;
            }
      }

      public int push_inner_ipdst(int original_pkt, int new_header)
      {
            //int s = aclBDD.support(original_pkt);
            //int s_no_dst = aclBDD.ref(aclBDD.exists(original_pkt, dstIPField));
            //int s_no_bit = aclBDD.ref(aclBDD.exists(s_no_dst, dstIPInnerBit));
            //System.out.println("before encap:" + s_no_bit);
            //if(s_no_bit != 1)
            //{
             //     int dstIPInnerField = AndInBatch(dstIPInner);
            //      System.out.println(aclBDD.exists(s_no_bit, dstIPInnerField));
            //}
            //System.err.println("in pkt:" + original_pkt);
            
            int tmp1 = aclBDD.ref(aclBDD.replace(original_pkt, push_perm));
            int new_pkt_bit = aclBDD.ref(aclBDD.and(new_header, tmp1));
            int new_pkt = set_field_bit(Fields.dst_ip_inner, new_pkt_bit, true);
            
            aclBDD.deref(tmp1);
            aclBDD.deref(new_pkt_bit);
            
            return new_pkt;
      }

      public int pop_outer_ipdst(int encapsulated)
      {
            int tmp1 =   aclBDD.ref( aclBDD.exists(encapsulated, dstIPField) );
            int new_pkt_bit = aclBDD.ref(aclBDD.replace(tmp1, pop_perm));
            
            int new_pkt = set_field_bit(Fields.dst_ip_inner, new_pkt_bit, false);
            
            aclBDD.deref(tmp1);
            aclBDD.deref(new_pkt_bit);
            
           //int support = aclBDD.ref(aclBDD.exists(new_pkt, dstIPField));
            //System.out.println("after decap:" + aclBDD.exists(support, dstIPInnerBit));
            
            return new_pkt;
      }

      /**
       * change the field of old_pkt to new_val
       * @param old_pkt
       * @param new_pkt
       * @return
       */
      public int nat(int old_pkt, int exists_quant, int new_val)
      {
            int tmp1 = aclBDD.ref(aclBDD.exists(old_pkt, exists_quant));
            int res = aclBDD.ref(aclBDD.and(tmp1, new_val));
            aclBDD.deref(tmp1);
            return res;
      }

      /**
       * check whether the packet set has labels
       * @param pkt
       * @return
       */
      public boolean isMPLSPkt(int pkt)
      {
            int pkt_ex = aclBDD.and(pkt, mplsLabelBit);
            if(pkt_ex == pkt)
            {
                  return true;
            }else
            {
                  return false;
            }
      }

      public int separateNonMPLS(int pkt)
      {
            return aclBDD.ref(aclBDD.and(pkt, aclBDD.not(mplsLabelBit)));
      }
      
      public FWDAPSet get_mpls_pkts_ap(APComputer apc)
      {
          return new FWDAPSet(apc.getAPExp(mplsLabelBit));
      }
      
      public FWDAPSet get_nonmpls_pkts_ap(APComputer apc)
      {
          return new FWDAPSet(apc.getAPExp(aclBDD.not(mplsLabelBit)));
      }

      public int separateMPLS(int pkt)
      {
            return aclBDD.ref(aclBDD.and(pkt, mplsLabelBit));
      }
      
      public int get_all_mpls_pkts()
      {
          return mplsLabelBit;
      }

      /**
       * assume pkt already has a label
       * @param pkt
       * @param label_bdd
       * @return
       */
      public boolean hasMPLSLabel(int pkt, int label_bdd)
      {
            if(aclBDD.and(pkt, label_bdd) == BDDFalse)
            {
                  return false;
            }else
            {
                  return true;
            }
      }
      
      public boolean hasIPTunnel(int pkt)
      {
            if(aclBDD.and(pkt, dstIPInnerBit) == BDDFalse)
            {
                  return false;
            }else
            {
                  return true;
            }
      }

      /**
       * assume input pkt has no mpls label
       * @param pkt
       * @param label
       * @return
       */
      public int pushMPLSLabel(int pkt, int label)
      {
            int pkt_no_label = aclBDD.ref(aclBDD.exists(pkt, mplsLabelBit));
            int tmp = aclBDD.ref(aclBDD.and(pkt_no_label, mplsLabelBit));
            int pkt_out = aclBDD.ref(aclBDD.and(tmp, label));
            aclBDD.deref(pkt_no_label);
            aclBDD.deref(tmp);
            return pkt_out;
      }

      /**
       * remove the mpls label field
       * @param pkt_label
       * @return
       */
      public int popMPLSLabel(int pkt_label)
      {
            int pkt = aclBDD.ref(aclBDD.exists(pkt_label, mplsLabelFieldDecoration));
            int pkt_out = aclBDD.ref(aclBDD.and(pkt, aclBDD.not(mplsLabelBit)));
            aclBDD.deref(pkt);

            return pkt_out;
      }

      /**
       * pop particular labels
       * @param pkt
       * @param label_bdd
       * @return
       */
      public int popMPLSLabel(int pkt, int label_bdd)
      {
            int pkt_with_label = aclBDD.ref(aclBDD.and(pkt, label_bdd));
            if (pkt_with_label == BDDFalse)
            {
                  return BDDFalse;
            }
            int pkt_popped_label = popMPLSLabel(pkt_with_label);
            aclBDD.deref(pkt_with_label);
            return pkt_popped_label;
      }

      public int swapMPLSLabel(int in_pkt, int out_label_bdd)
      {


            int pkt_no_label = aclBDD.ref(aclBDD.exists(in_pkt, mplsLabelField));
            int pkt_out_label = aclBDD.ref(aclBDD.and(pkt_no_label, out_label_bdd));
            aclBDD.deref(pkt_no_label);

            return pkt_out_label;
      }

      public int swapMPLSLabel(int in_pkt, int in_label_bdd, int out_label_bdd)
      {
            int in_pkt_with_in_label = aclBDD.ref(aclBDD.and(in_pkt, in_label_bdd));
            if(in_pkt_with_in_label == BDDFalse)
            {
                  return BDDFalse;
            }
            int in_pkt_no_label = 
                        aclBDD.ref(aclBDD.exists(in_pkt_with_in_label, mplsLabelField));
            int out_pkt = aclBDD.ref(aclBDD.and(in_pkt_no_label, out_label_bdd));

            aclBDD.deref(in_pkt_with_in_label);
            aclBDD.deref(in_pkt_no_label);

            return out_pkt;
      }

      public int getMPLSReserveLabels()
      {
            int [] pop_labels_bdd = new int [4];

            for (int i = 0; i < pop_labels_bdd.length; i ++)
            {
                  pop_labels_bdd[i] = encodeMPLSLabel(i);
            }

            int popLabels_bdd = OrInBatch(pop_labels_bdd);
            DerefInBatch(pop_labels_bdd);
            return popLabels_bdd;
      }


      public BDD getBDD()
      {
            return aclBDD;
      }

      public HashMap<String, Integer> getfwdbdds(ArrayList<ForwardingRule> fws)
      {
            int alreadyfwded = BDDFalse;
            HashMap<String, Integer> fwdbdds = new HashMap<String, Integer>();
            int longestmatch = 32;

            //int prefixchk = encodeDstIPPrefix(2148270417L, 32);

            for(int i = longestmatch; i >=0; i --)
            {
                  for(int j = 0; j < fws.size(); j ++)
                  {
                        ForwardingRule onefw = fws.get(j);
                        if(onefw.getprefixlen() == i)
                        {

                              String iname = onefw.getiname();
                              //int[] ipbin = Utility.CalBinRep(onefw.getdestip(), ipBits);
                              //int[] ipbinprefix = new int[onefw.getprefixlen()];
                              //for(int k = 0; k < onefw.getprefixlen(); k ++)
                              //{
                              //	ipbinprefix[k] = ipbin[k + ipBits - onefw.getprefixlen()];
                              //}
                              //int entrybdd = EncodePrefix(ipbinprefix, dstIP, ipBits);
                              int entrybdd = encodeDstIPPrefix(onefw.getdestip(), onefw.getprefixlen());

                              int notalreadyfwded = aclBDD.not(alreadyfwded);
                              aclBDD.ref(notalreadyfwded);
                              int toadd = aclBDD.and(entrybdd, notalreadyfwded);
                              aclBDD.ref(toadd);
                              aclBDD.deref(notalreadyfwded);
                              int altmp = aclBDD.or(alreadyfwded, entrybdd);
                              aclBDD.ref(altmp);
                              aclBDD.deref(alreadyfwded);
                              alreadyfwded = altmp;
                              onefw.setBDDRep(entrybdd);
                              //aclBDD.deref(entrybdd);

                              /*
					if(aclBDD.and(prefixchk, toadd) > 0)
					{
						System.out.println(onefw);
					}*/

                              if(fwdbdds.containsKey(iname))
                              {
                                    int oldkey = fwdbdds.get(iname);
                                    int newkey = aclBDD.or(toadd, oldkey);
                                    aclBDD.ref(newkey);
                                    aclBDD.deref(toadd);
                                    aclBDD.deref(oldkey);
                                    fwdbdds.put(iname, newkey);
                              }else
                              {
                                    fwdbdds.put(iname, toadd);
                              }
                        }
                  }
            }
            aclBDD.deref(alreadyfwded);
            return fwdbdds;
      }

      public HashMap<String, Integer> getfwdbdds6(ArrayList<ForwardingRule6> fws)
      {
            int alreadyfwded = BDDFalse;
            HashMap<String, Integer> fwdbdds = new HashMap<String, Integer>();
            int longestmatch = 128;

            //int prefixchk = encodeDstIPPrefix(2148270417L, 32);

            for(int i = longestmatch; i >=0; i --)
            {
                  for(int j = 0; j < fws.size(); j ++)
                  {
                        ForwardingRule6 onefw = fws.get(j);
                        if(onefw.getprefixlen() == i)
                        {

                              String iname = onefw.getiname();
                              encodeFW6(onefw);
                              int entrybdd = onefw.getBDDRep();

                              int notalreadyfwded = aclBDD.not(alreadyfwded);
                              aclBDD.ref(notalreadyfwded);
                              int toadd = aclBDD.and(entrybdd, notalreadyfwded);
                              aclBDD.ref(toadd);
                              aclBDD.deref(notalreadyfwded);
                              int altmp = aclBDD.or(alreadyfwded, entrybdd);
                              aclBDD.ref(altmp);
                              aclBDD.deref(alreadyfwded);
                              alreadyfwded = altmp;
                              //aclBDD.deref(entrybdd);

                              /*
					if(aclBDD.and(prefixchk, toadd) > 0)
					{
						System.out.println(onefw);
					}*/

                              if(fwdbdds.containsKey(iname))
                              {
                                    int oldkey = fwdbdds.get(iname);
                                    int newkey = aclBDD.or(toadd, oldkey);
                                    aclBDD.ref(newkey);
                                    aclBDD.deref(toadd);
                                    aclBDD.deref(oldkey);
                                    fwdbdds.put(iname, newkey);
                              }else
                              {
                                    fwdbdds.put(iname, toadd);
                              }
                        }
                  }
            }
            aclBDD.deref(alreadyfwded);
            return fwdbdds;
      }

      private void encodeFW6(ForwardingRule6 onefw)
      {
            String prefix = onefw.getdestip();
            int prefix_len = onefw.getprefixlen();
            if(prefix_len == 0)
            {
                  onefw.setBDDRep(BDDTrue);
                  return;
            }

            int tempnode = BDDTrue;
            for(int i = 0; i < prefix_len; i ++)
            {
                  if(i == 0)
                  {
                        tempnode = EncodingVar(dstIP6[i], prefix.charAt(i));
                  }else
                  {
                        int tempnode2 = EncodingVar(dstIP6[i], prefix.charAt(i));
                        int tempnode3 = aclBDD.and(tempnode, tempnode2);
                        aclBDD.ref(tempnode3);
                        aclBDD.deref(tempnode);
                        tempnode = tempnode3;
                  }
            }

            onefw.setBDDRep(tempnode);
      }

      public HashMap<String, Integer> getfwdbdds_no_store(ArrayList<ForwardingRule> fws)
      {
            int alreadyfwded = BDDFalse;
            HashMap<String, Integer> fwdbdds = new HashMap<String, Integer>();
            int longestmatch = 32;

            //int prefixchk = encodeDstIPPrefix(2148270417L, 32);

            for(int i = longestmatch; i >=0; i --)
            {
                  for(int j = 0; j < fws.size(); j ++)
                  {
                        ForwardingRule onefw = fws.get(j);
                        if(onefw.getprefixlen() == i)
                        {

                              String iname = onefw.getiname();
                              //int[] ipbin = Utility.CalBinRep(onefw.getdestip(), ipBits);
                              //int[] ipbinprefix = new int[onefw.getprefixlen()];
                              //for(int k = 0; k < onefw.getprefixlen(); k ++)
                              //{
                              //	ipbinprefix[k] = ipbin[k + ipBits - onefw.getprefixlen()];
                              //}
                              //int entrybdd = EncodePrefix(ipbinprefix, dstIP, ipBits);
                              int entrybdd = encodeDstIPPrefix(onefw.getdestip(), onefw.getprefixlen());

                              int notalreadyfwded = aclBDD.not(alreadyfwded);
                              aclBDD.ref(notalreadyfwded);
                              int toadd = aclBDD.and(entrybdd, notalreadyfwded);
                              aclBDD.ref(toadd);
                              aclBDD.deref(notalreadyfwded);
                              int altmp = aclBDD.or(alreadyfwded, entrybdd);
                              aclBDD.ref(altmp);
                              aclBDD.deref(alreadyfwded);
                              alreadyfwded = altmp;
                              //onefw.setBDDRep(entrybdd);
                              aclBDD.deref(entrybdd);

                              /*
					if(aclBDD.and(prefixchk, toadd) > 0)
					{
						System.out.println(onefw);
					}*/

                              if(fwdbdds.containsKey(iname))
                              {
                                    int oldkey = fwdbdds.get(iname);
                                    int newkey = aclBDD.or(toadd, oldkey);
                                    aclBDD.ref(newkey);
                                    aclBDD.deref(toadd);
                                    aclBDD.deref(oldkey);
                                    fwdbdds.put(iname, newkey);
                              }else
                              {
                                    fwdbdds.put(iname, toadd);
                              }
                        }
                  }
            }
            aclBDD.deref(alreadyfwded);
            return fwdbdds;
      }

      /**
       * from shorted to longest
       * @param fws
       * @return
       */
      public HashMap<String, Integer> getfwdbdds_sorted_no_store(ArrayList<ForwardingRule> fws)
      {
            int alreadyfwded = BDDFalse;
            HashMap<String, Integer> fwdbdds = new HashMap<String, Integer>();

            //int prefixchk = encodeDstIPPrefix(2148270417L, 32);


            for(int j = fws.size() - 1; j >= 0; j --)
            {
                  ForwardingRule onefw = fws.get(j);
                  //System.out.println(j);


                  String iname = onefw.getiname();
                  //int[] ipbin = Utility.CalBinRep(onefw.getdestip(), ipBits);
                  //int[] ipbinprefix = new int[onefw.getprefixlen()];
                  //for(int k = 0; k < onefw.getprefixlen(); k ++)
                  //{
                  //	ipbinprefix[k] = ipbin[k + ipBits - onefw.getprefixlen()];
                  //}
                  //int entrybdd = EncodePrefix(ipbinprefix, dstIP, ipBits);
                  int entrybdd = encodeDstIPPrefix(onefw.getdestip(), onefw.getprefixlen());

                  int notalreadyfwded = aclBDD.not(alreadyfwded);
                  aclBDD.ref(notalreadyfwded);
                  int toadd = aclBDD.and(entrybdd, notalreadyfwded);
                  aclBDD.ref(toadd);
                  aclBDD.deref(notalreadyfwded);
                  int altmp = aclBDD.or(alreadyfwded, entrybdd);
                  aclBDD.ref(altmp);
                  aclBDD.deref(alreadyfwded);
                  alreadyfwded = altmp;
                  //onefw.setBDDRep(entrybdd);
                  aclBDD.deref(entrybdd);

                  /*
					if(aclBDD.and(prefixchk, toadd) > 0)
					{
						System.out.println(onefw);
					}*/

                  if(fwdbdds.containsKey(iname))
                  {
                        int oldkey = fwdbdds.get(iname);
                        int newkey = aclBDD.or(toadd, oldkey);
                        aclBDD.ref(newkey);
                        aclBDD.deref(toadd);
                        aclBDD.deref(oldkey);
                        fwdbdds.put(iname, newkey);
                  }else
                  {
                        fwdbdds.put(iname, toadd);
                  }

            }

            aclBDD.deref(alreadyfwded);
            return fwdbdds;
      }

      /**
       * bdd for each rule is computed
       * @param fws
       * @return
       */
      public HashMap<String, Integer> getfwdbdds2(ArrayList<ForwardingRule> fws)
      {
            int alreadyfwded = BDDFalse;

            HashMap<String, Integer> fwdbdds = new HashMap<String, Integer>();
            int longestmatch = 32;
            for(int i = longestmatch; i >=0; i --)
            {
                  for(int j = 0; j < fws.size(); j ++)
                  {
                        ForwardingRule onefw = fws.get(j);
                        if(onefw.getprefixlen() == i)
                        {
                              String iname = onefw.getiname();
                              //int[] ipbin = Utility.CalBinRep(onefw.getdestip(), ipBits);
                              //int[] ipbinprefix = new int[onefw.getprefixlen()];
                              //for(int k = 0; k < onefw.getprefixlen(); k ++)
                              //{
                              //	ipbinprefix[k] = ipbin[k + ipBits - onefw.getprefixlen()];
                              //}
                              //int entrybdd = EncodePrefix(ipbinprefix, dstIP, ipBits);
                              int entrybdd = onefw.getBDDRep();
                              int notalreadyfwded = aclBDD.not(alreadyfwded);
                              aclBDD.ref(notalreadyfwded);
                              int toadd = aclBDD.and(entrybdd, notalreadyfwded);
                              aclBDD.ref(toadd);
                              aclBDD.deref(notalreadyfwded);
                              int altmp = aclBDD.or(alreadyfwded, entrybdd);
                              aclBDD.ref(altmp);
                              aclBDD.deref(alreadyfwded);
                              alreadyfwded = altmp;

                              if(fwdbdds.containsKey(iname))
                              {
                                    int oldkey = fwdbdds.get(iname);
                                    int newkey = aclBDD.or(toadd, oldkey);
                                    aclBDD.ref(newkey);
                                    aclBDD.deref(toadd);
                                    aclBDD.deref(oldkey);
                                    fwdbdds.put(iname, newkey);
                              }else
                              {
                                    fwdbdds.put(iname, toadd);
                              }
                        }
                  }
            }
            aclBDD.deref(alreadyfwded);
            return fwdbdds;
      }

      public int encodeSrcIPPrefix(long ipaddr, int prefixlen)
      {
            int[] ipbin = Utility.CalBinRep(ipaddr, ipBits);
            int[] ipbinprefix = new int[prefixlen];
            for(int k = 0; k < prefixlen; k ++)
            {
                  ipbinprefix[k] = ipbin[k + ipBits - prefixlen];
            }
            int entrybdd = EncodePrefix(ipbinprefix, srcIP, ipBits);
            return entrybdd;
      }

      public int encodeDstIPPrefix(long ipaddr, int prefixlen)
      {
            int[] ipbin = Utility.CalBinRep(ipaddr, ipBits);
            int[] ipbinprefix = new int[prefixlen];
            for(int k = 0; k < prefixlen; k ++)
            {
                  ipbinprefix[k] = ipbin[k + ipBits - prefixlen];
            }
            int entrybdd = EncodePrefix(ipbinprefix, dstIP, ipBits);
            return entrybdd;
      }

      public int encodeMPLSLabel(int label_int)
      {
            int [] label_bin = Utility.CalBinRep(label_int, mplsBits);
            int [] encodedVars = new int[mplsBits];
            for(int i = 0; i < mplsBits; i ++)
            {
                  encodedVars[i] = EncodingVar(mplsLabel[i], label_bin[i]);
            }

            return AndInBatch(encodedVars);
      }


      public void multipleref(int bddnode, int reftimes)
      {
            for(int i = 0; i < reftimes; i ++)
            {
                  aclBDD.ref(bddnode);
            }
      }

      /**
       * 
       * @param entrybdd
       * @return the set of fwdbdds which might be changed
       */
      public HashSet<String> getDependencySet(ForwardingRule fwdr, HashMap<String, Integer> fwdbdds)
      {
            HashSet<String> ports = new HashSet<String>();
            int entrybdd = fwdr.getBDDRep();
            if(fwdbdds.keySet().contains(fwdr.getiname()))
            {
                  int onebdd = fwdbdds.get(fwdr.getiname());
                  if(entrybdd == aclBDD.and(entrybdd, onebdd))
                  {
                        return ports;
                  }else
                  {
                        ports.add(fwdr.getiname());
                        for(String port : fwdbdds.keySet())
                        {
                              if(!port.equals(fwdr.getiname()))
                              {
                                    onebdd = fwdbdds.get(port);
                                    if(BDDFalse != aclBDD.and(entrybdd, onebdd))
                                    {
                                          ports.add(port);
                                    }
                              }
                        }
                  }
            }else
            {
                  ports.add(fwdr.getiname());
                  for(String port : fwdbdds.keySet())
                  {
                        int onebdd = fwdbdds.get(port);
                        if(BDDFalse != aclBDD.and(entrybdd, onebdd))
                        {
                              ports.add(port);
                        }
                  }
            }

            return ports;
      }

      public int getlongP(ForwardingRule onefw, ArrayList<ForwardingRule> fws)
      {
            int longP = BDDFalse;

            for(ForwardingRule of : fws)
            {
                  if(onefw.getprefixlen() <= of.getprefixlen())
                  {
                        int tmp = aclBDD.or(longP, of.getBDDRep());
                        aclBDD.deref(longP);
                        aclBDD.ref(tmp);
                        longP = tmp;
                  }
            }

            return longP;
      }

      /**
       * 
       * @param subs - has ip information
       * @param rawBDD
       * @param reftimes -  the res need to be referenced for several times
       * @return
       */
      public int encodeACLin(ArrayList<Subnet> subs, int rawBDD, int reftimes)
      {
            // dest ip
            if(subs == null)
            {
                  multipleref(rawBDD, reftimes);
                  return rawBDD;
            }
            int destipbdd = encodeDstIPPrefixs(subs);
            int notdestip = aclBDD.not(destipbdd);
            aclBDD.ref(notdestip);
            int res = aclBDD.or(notdestip, rawBDD);

            multipleref(res, reftimes);
            aclBDD.deref(destipbdd);
            aclBDD.deref(notdestip);
            return res;
      }


      public int encodeACLout(ArrayList<Subnet> subs, int rawBDD, int reftimes)
      {
            if(subs == null)
            {
                  multipleref(rawBDD, reftimes);
                  return rawBDD;
            }
            // src ip
            int srcipbdd = encodeSrcIPPrefixs(subs);
            int notsrctip = aclBDD.not(srcipbdd);
            aclBDD.ref(notsrctip);
            int res = aclBDD.or(notsrctip, rawBDD);

            multipleref(res, reftimes);
            aclBDD.deref(srcipbdd);
            aclBDD.deref(notsrctip);
            return res;
      }

      public int encodeDstIPPrefixs(ArrayList<Subnet> subs)
      {
            int res = BDDFalse;
            for(int i = 0; i < subs.size(); i ++)
            {
                  Subnet onesub = subs.get(i);
                  int dstipbdd = encodeDstIPPrefix(onesub.getipaddr(), onesub.getprefixlen());
                  int tmp = aclBDD.or(res, dstipbdd);
                  aclBDD.ref(tmp);
                  aclBDD.deref(res);
                  aclBDD.deref(dstipbdd);
                  res = tmp;
            }
            return res;
      }

      public int encodeSrcIPPrefixs(ArrayList<Subnet> subs)
      {
            int res = BDDFalse;
            for(int i = 0; i < subs.size(); i ++)
            {
                  Subnet onesub = subs.get(i);
                  int srcipbdd = encodeSrcIPPrefix(onesub.getipaddr(), onesub.getprefixlen());
                  int tmp = aclBDD.or(res, srcipbdd);
                  aclBDD.ref(tmp);
                  aclBDD.deref(res);
                  aclBDD.deref(srcipbdd);
                  res = tmp;
            }
            return res;
      }
      /**
       * 
       * @return the size of bdd (in bytes)
       */
      public long BDDSize()
      {
            return aclBDD.getMemoryUsage();
      }

      private void DeclareVars(int[] vars, int bits)
      {
            for(int i = bits - 1; i >=0; i --)
            {
                  vars[i] = aclBDD.createVar();
            }
      }

      //protocol is 8 bits
      private void DeclareProtocol()
      {
            DeclareVars(protocol, protocolBits);
            protocolBit = aclBDD.createVar();
      }

      private void DeclareSrcPort()
      {
            DeclareVars(srcPort, portBits);
            srcPortBit = aclBDD.createVar();
      }

      private void DeclareDstPort()
      {
            DeclareVars(dstPort, portBits);
            dstPortBit = aclBDD.createVar();
      }

      private void DeclareSrcIP()
      {
            DeclareVars(srcIP, ipBits);
            srcIPBit = aclBDD.createVar();
      }

      private void DeclareDstIP()
      {
            DeclareVars(dstIP, ipBits);
            dstIPBit = aclBDD.createVar();
      }

      private void DeclareDstIPInner()
      {
            DeclareVars(dstIPInner, ipBits);
            dstIPInnerBit = aclBDD.createVar();
      }

      private void DeclareDstIP6()
      {
            DeclareVars(dstIP6, ipBits);
            dstIP6Bit = aclBDD.createVar();
      }

      private void DeclareMPLSLabel()
      {
            DeclareVars(mplsLabel, mplsBits);
            mplsLabelBit = aclBDD.createVar();
      }

      /**
       * @param vars - a list of bdd nodes that we do not need anymore
       */
      public void DerefInBatch(int[] vars)
      {
            for(int i = 0; i < vars.length; i ++)
            {
                  aclBDD.deref(vars[i]);
            }
      }

      public void deref(int bdd)
      {
            aclBDD.deref(bdd);
      }

      public int ref(int bdd)
      {
            return aclBDD.ref(bdd);
      }

      /**
       * 
       * @param acls - the acl that needs to be transformed to bdd
       * @return a bdd node that represents the acl
       */
      public int ConvertACLs(LinkedList<ACLRule> acls)
      {
            if(acls.size() == 0)
            {
                  // no need to ref the false node
                  return BDDFalse;
            }
            int res = BDDFalse;
            int denyBuffer = BDDFalse;
            int denyBufferNot = BDDTrue;
            for(int i = 0; i < acls.size(); i ++)
            {
                  ACLRule acl = acls.get(i);
                  // g has been referenced
                  int g = ConvertACLRule(acl);

                  if(ACLRule.CheckPermit(acl))
                  {
                        if(res == BDDFalse)
                        {
                              if(denyBuffer == BDDFalse)
                              {
                                    res = g;
                              }else
                              {
                                    int tempnode = aclBDD.and(g, denyBufferNot);
                                    aclBDD.ref(tempnode);
                                    res = tempnode;
                                    aclBDD.deref(g);
                              }
                        }else
                        {
                              if(denyBuffer == BDDFalse)
                              {
                                    // just combine the current res and g
                                    int tempnode = aclBDD.or(res, g);
                                    aclBDD.ref(tempnode);
                                    DerefInBatch(new int[]{res, g});
                                    res = tempnode;
                              }else
                              {
                                    //the general case
                                    int tempnode = aclBDD.and(g, denyBufferNot);
                                    aclBDD.ref(tempnode);
                                    aclBDD.deref(g);

                                    int tempnode2 = aclBDD.or(res, tempnode);
                                    aclBDD.ref(tempnode2);
                                    DerefInBatch(new int[]{res, tempnode});
                                    res = tempnode2;
                              }
                        }

                  }else
                  {
                        /**
                         * combine to the denyBuffer
                         */
                        if(denyBuffer == BDDFalse)
                        {
                              denyBuffer = g;
                              denyBufferNot = aclBDD.not(g);
                              aclBDD.ref(denyBufferNot);
                        }else
                        {
                              int tempnode = aclBDD.or(denyBuffer, g);
                              aclBDD.ref(tempnode);
                              DerefInBatch(new int[]{denyBuffer, g});
                              denyBuffer = tempnode;

                              aclBDD.deref(denyBufferNot);
                              denyBufferNot = aclBDD.not(denyBuffer);
                              aclBDD.ref(denyBufferNot);
                        }
                  }
                  //System.out.println(acl);
                  //System.out.println(res);
            }
            /**
             * we need to de-ref denyBuffer, denyBufferNot
             */
            //DerefInBatch(new int[]{denyBuffer, denyBufferNot});
            aclBDD.deref(denyBufferNot);
            aclBDD.deref(denyBuffer);
            return res;
      }

      /**
       * 
       * @param acls - the acl that needs to be transformed to bdd
       * @return a bdd node that represents the acl
       * store intermediate results
       * ref_rule_id < start_rule_id
       */
      public int ConvertACLs_stored(LinkedList<ACLRule> acls)
      {
            if(acls.size() == 0)
            {
                  // no need to ref the false node
                  return BDDFalse;
            }
            int allowed = BDDFalse;
            int denied = BDDFalse;


            for(int i = 0; i < acls.size(); i ++)
            {
                  ACLRule acl = acls.get(i);
                  if (acl.is_visible())
                  {
                        // g has been referenced
                        int g = ConvertACLRule(acl);

                        if(ACLRule.CheckPermit(acl))
                        {
                              int denied_not = aclBDD.ref(aclBDD.not(denied));
                              int tmp = aclBDD.ref(aclBDD.and(g, denied_not));
                              allowed = aclBDD.ref(aclBDD.or(allowed, tmp));
                              aclBDD.ref(denied);
                              acl.insert_vals(g, allowed, denied);
                              aclBDD.deref(denied_not);
                              aclBDD.deref(tmp);

                        }else
                        {
                              denied = aclBDD.ref(aclBDD.or(g, denied));
                              aclBDD.ref(allowed);
                              acl.insert_vals(g, allowed, denied);
                        }
                        //System.out.println(acl);
                        //System.out.println(res);
                  }
            }
            /**
             * we need to de-ref denyBuffer, denyBufferNot
             */
            //DerefInBatch(new int[]{denyBuffer, denyBufferNot});
            return allowed;
      }

      /**
       * for rule update
       * @param acls
       * @param start_rule_id
       * @param ref_rule_id
       * @return
       */
      public int updateACLs(LinkedList<ACLRule> acls, int start_rule_id, int ref_rule_id)
      {
            if(acls.size() == 0)
            {
                  // no need to ref the false node
                  return BDDFalse;
            }
            int allowed = BDDFalse;
            int denied = BDDFalse;
            if(ref_rule_id >=0)
            {
                  allowed = acls.get(ref_rule_id).permit_bdd;
                  denied = acls.get(ref_rule_id).deny_bdd;
            }


            for(int i = start_rule_id; i < acls.size(); i ++)
            {
                  ACLRule acl = acls.get(i);
                  if (acl.is_visible())
                  {
                        // g has been referenced
                        int g = acl.get_val_bdd();
                        if(g == BDDFalse)
                        {
                              g = ConvertACLRule(acl);
                        }

                        if(ACLRule.CheckPermit(acl))
                        {
                              int denied_not = aclBDD.ref(aclBDD.not(denied));
                              int tmp = aclBDD.ref(aclBDD.and(g, denied_not));
                              allowed = aclBDD.ref(aclBDD.or(allowed, tmp));
                              aclBDD.ref(denied);
                              acl.update_bdds(allowed, denied, aclBDD);
                              aclBDD.deref(denied_not);
                              aclBDD.deref(tmp);

                        }else
                        {
                              denied = aclBDD.ref(aclBDD.or(g, denied));
                              aclBDD.ref(allowed);
                              acl.update_bdds(allowed, denied, aclBDD);
                        }
                        //System.out.println(acl);
                        //System.out.println(res);
                  }
            }
            /**
             * we need to de-ref denyBuffer, denyBufferNot
             */
            //DerefInBatch(new int[]{denyBuffer, denyBufferNot});
            return allowed;
      }


      /**
       * 
       * @param aclr - an acl rule
       * @return a bdd node representing this rule
       */
      public int ConvertACLRule(ACLRule aclr)
      {	
            /**
             *  protocol
             */
            // no need to ref the true node
            int protocolNode = BDDTrue;
            if(aclr.protocolLower == null || 
                        aclr.protocolLower.equalsIgnoreCase("any"))
            {
                  //do nothing, just a shortcut
            }else{
                  Range r = ACLRule.convertProtocolToRange
                              (aclr.protocolLower, aclr.protocolUpper);
                  protocolNode = ConvertProtocol(r);
            }

            /**
             * src port
             */
            int srcPortNode = BDDTrue;
            if(aclr.sourcePortLower == null ||
                        aclr.sourcePortLower.equalsIgnoreCase("any"))
            {
                  //do nothing, just a shortcut
            }else{
                  Range r = ACLRule.convertPortToRange(aclr.sourcePortLower, 
                              aclr.sourcePortUpper);
                  srcPortNode = ConvertSrcPort(r);
            }

            /**
             * dst port
             */
            int dstPortNode = BDDTrue;
            if(aclr.destinationPortLower == null ||
                        aclr.destinationPortLower.equalsIgnoreCase("any"))
            {
                  // do nothing, just a shortcut
            }else{
                  Range r = ACLRule.convertPortToRange(aclr.destinationPortLower, 
                              aclr.destinationPortUpper);
                  dstPortNode = ConvertDstPort(r);
            }

            /**
             * src IP
             */
            int srcIPNode = ConvertIPAddress(aclr.source, aclr.sourceWildcard, srcIP);

            /**
             * dst IP
             */
            int dstIPNode = ConvertIPAddress(aclr.destination, 
                        aclr.destinationWildcard, dstIP);

            //put them together
            int [] fiveFields = {protocolNode,srcPortNode,dstPortNode,
                        srcIPNode,dstIPNode};
            int tempnode = AndInBatch(fiveFields);
            //clean up internal nodes
            DerefInBatch(fiveFields);

            return tempnode;
      }

      /**
       * @param bddnodes - an array of bdd nodes
       * @return - the bdd node which is the AND of all input nodes
       * all temporary nodes are de-referenced. 
       * the input nodes are not de-referenced.
       */
      public int AndInBatch(int [] bddnodes)
      {
            int tempnode = BDDTrue;
            for(int i = 0; i < bddnodes.length; i ++)
            {
                  if(i == 0)
                  {
                        tempnode = bddnodes[i];
                        aclBDD.ref(tempnode);
                  }else
                  {
                        if(bddnodes[i] == BDDTrue)
                        {
                              // short cut, TRUE does not affect anything
                              continue;
                        }
                        if(bddnodes[i] == BDDFalse)
                        {
                              // short cut, once FALSE, the result is false
                              // the current tempnode is useless now
                              aclBDD.deref(tempnode);
                              tempnode = BDDFalse; 
                              break;
                        }
                        int tempnode2 = aclBDD.and(tempnode, bddnodes[i]);
                        aclBDD.ref(tempnode2);
                        // do not need current tempnode 
                        aclBDD.deref(tempnode);
                        //refresh
                        tempnode = tempnode2;
                  }
            }
            return tempnode;
      }

      /**
       * already reference the new bdd node
       * @param bdd1
       * @param bdd2
       * @return
       */
      public int and(int bdd1, int bdd2)
      {
            return aclBDD.ref(aclBDD.and(bdd1, bdd2));
      }

      /**
       * @param bddnodes - an array of bdd nodes
       * @return - the bdd node which is the OR of all input nodes
       * all temporary nodes are de-referenced. 
       * the input nodes are not de-referenced.
       */
      public int OrInBatch(int [] bddnodes)
      {
            int tempnode = BDDFalse;
            for(int i = 0; i < bddnodes.length; i ++)
            {
                  if(i == 0)
                  {
                        tempnode = bddnodes[i];
                        aclBDD.ref(tempnode);
                  }else
                  {
                        if(bddnodes[i] == BDDFalse)
                        {
                              // short cut, FALSE does not affect anything
                              continue;
                        }
                        if(bddnodes[i] == BDDTrue)
                        {
                              // short cut, once TRUE, the result is true
                              // the current tempnode is useless now
                              aclBDD.deref(tempnode);
                              tempnode = BDDTrue; 
                              break;
                        }
                        int tempnode2 = aclBDD.or(tempnode, bddnodes[i]);
                        aclBDD.ref(tempnode2);
                        // do not need current tempnode 
                        aclBDD.deref(tempnode);
                        //refresh
                        tempnode = tempnode2;
                  }
            }
            return tempnode;
      }


      /**
       * @param ip address and mask
       * @return the corresponding bdd node
       */
      protected int ConvertIPAddress(String IP, String Mask, int[] vars)
      {
            int tempnode = BDDTrue;
            // case 1 IP = any
            if(IP == null || IP.equalsIgnoreCase("any"))
            {
                  // return TRUE node
                  return tempnode;
            }

            // binary representation of IP address
            int[] ipbin = Utility.IPBinRep(IP);
            // case 2 Mask = null
            if(Mask == null)
            {
                  // no mask is working
                  return EncodePrefix(ipbin, vars, ipBits);
            }else{
                  int [] maskbin = Utility.IPBinRep(Mask);
                  int numMasked = Utility.NumofNonZeros(maskbin);

                  int [] prefix = new int[maskbin.length - numMasked];
                  int [] varsUsed = new int[prefix.length];
                  int ind = 0;
                  for(int i = 0; i < maskbin.length; i ++)
                  {
                        if(maskbin[i] == 0)
                        {
                              prefix[ind] = ipbin[i];
                              varsUsed[ind] = vars[i];
                              ind ++;
                        }
                  }

                  return EncodePrefix(prefix, varsUsed, prefix.length);
            }

      }

      /***
       * convert a range of protocol numbers to a bdd representation
       */
      public int ConvertProtocol(Range r)
      {
            return ConvertRange(r, protocol, protocolBits);

      }

      /**
       * convert a range of source port numbers to a bdd representation
       */
      public int ConvertSrcPort(Range r)
      {
            return ConvertRange(r, srcPort, portBits);
      }

      /**
       * convert a range of destination port numbers to a bdd representation
       */
      public int ConvertDstPort(Range r)
      {
            return ConvertRange(r, dstPort, portBits);
      }

      /**
       * 
       * @param r - the range
       * @param vars - bdd variables used
       * @param bits - number of bits in the representation
       * @return the corresponding bdd node
       */
      private int ConvertRange(Range r, int [] vars, int bits)
      {

            LinkedList<int []> prefix = Utility.DecomposeInterval(r, bits);
            //System.out.println(vars.length);
            if(prefix.size() == 0)
            {
                  return BDDTrue;
            }

            int tempnode = BDDTrue;
            for(int i = 0; i < prefix.size(); i ++)
            {
                  if(i == 0)
                  {
                        tempnode = EncodePrefix(prefix.get(i), vars, bits);
                  }else
                  {
                        int tempnode2 = EncodePrefix(prefix.get(i), vars, bits);
                        int tempnode3 = aclBDD.or(tempnode, tempnode2);
                        aclBDD.ref(tempnode3);
                        DerefInBatch(new int[]{tempnode, tempnode2});
                        tempnode = tempnode3;
                  }
            }
            return tempnode;
      }

      /**
       * 
       * @param prefix - 
       * @param vars - bdd variables used
       * @param bits - number of bits in the representation
       * @return a bdd node representing the predicate
       * e.g. for protocl, bits = 8, prefix = {1,0,1,0}, so the predicate is protocol[4] 
       * and (not protocol[5]) and protocol[6] and (not protocol[7])
       */
      private int EncodePrefix(int [] prefix, int[] vars, int bits)
      {
            if(prefix.length == 0)
            {
                  return BDDTrue;
            }

            int tempnode = BDDTrue;
            for(int i = 0; i < prefix.length; i ++)
            {
                  if(i == 0){
                        tempnode = EncodingVar(vars[bits - prefix.length + i], prefix[i]);
                  }else
                  {
                        int tempnode2 = EncodingVar(vars[bits - prefix.length + i], prefix[i]);
                        int tempnode3 = aclBDD.and(tempnode, tempnode2);
                        aclBDD.ref(tempnode3);
                        //do not need tempnode2, tempnode now
                        //aclBDD.deref(tempnode2);
                        //aclBDD.deref(tempnode);
                        DerefInBatch(new int[]{tempnode, tempnode2});
                        //refresh tempnode 3
                        tempnode = tempnode3;
                  }
            }
            return tempnode;
      }

      /***
       * return a bdd node representing the predicate on the protocol field
       */
      private int EncodeProtocolPrefix(int [] prefix)
      {
            return EncodePrefix(prefix, protocol, protocolBits);
      }

      /**
       * print out a graph for the bdd node var
       */
      public void PrintVar(int var)
      {
            if(aclBDD.isValid(var))
            {
                  aclBDD.printDot(Integer.toString(var), var);
                  System.out.println("BDD node " + var + " printed.");
            }else
            {
                  System.err.println(var + " is not a valid BDD node!");
            }
      }

      /**
       * return the size of the bdd tree
       */
      public int getNodeSize(int bddnode)
      {
            int size = aclBDD.nodeCount(bddnode);
            if(size == 0)
            {// this means that it is only a terminal node
                  size ++;
            }
            return size;
      }

      public int getNodeSize(Collection<Integer> nodes)
      {
            int size = 0;
            for(int n : nodes)
            {
                  size += getNodeSize(n);
            }
            return size;
      }

      /*
       * cleanup the bdd after usage
       */
      public void CleanUp()
      {
            aclBDD.cleanup();
      }

      /***
       * var is a BDD variable
       * if flag == 1, return var
       * if flag == 0, return not var, the new bdd node is referenced.
       */
      private int EncodingVar(int var, int flag)
      {
            if (flag == 0)
            {
                  int tempnode = aclBDD.not(var);
                  // no need to ref the negation of a variable.
                  // the ref count is already set to maximal
                  //aclBDD.ref(tempnode);
                  return tempnode;
            }
            if (flag == 1)
            {
                  return var;
            }

            //should not reach here
            System.err.println("flag can only be 0 or 1!");
            return -1;
      }

      private int EncodingVar(int var, char flag)
      {
            return EncodingVar(var, Character.getNumericValue(flag));
      }

      public static void main(String[] args) throws IOException
      {

            /****
             * it is to test the translation from an interval of port numbers to bdd
             */
            /*
		BDDPacketSet bps = new BDDPacketSet();
		int var = bps.ConvertProtocol(new Range(16,31));
		bps.PrintVar(var);
		bps.CleanUp();
             */

            /**
             * test whether translate IP address correctly
             */

            /*
		BDDPacketSet bps = new BDDPacketSet();
		int var = bps.ConvertIPAddress("192.192.200.2", "0.0.255.255", bps.srcIP);
		bps.PrintVar(var);
		bps.CleanUp();
             */

            /**
             * test conversion of an inverval
             */
            /*
		BDDACLWrapper bps = new BDDACLWrapper();
		Range r = new Range(123,123);
		int bddnode = bps.ConvertDstPort(r);
		bps.PrintVar(bddnode);
             */

            /**
             * compare packet set conversion and bdd conversion
             * this is the problem. actually you cannot convert the following things to a single range.
             */

            /*
		Range r1 = PacketSet.convertIPtoIntegerRange("0.0.0.0", "255.255.255.0");
		System.out.println(r1);

		Range r2 = PacketSet.convertIPtoIntegerRange("0.0.0.255", "255.255.255.0");
		System.out.println(r2);
             */

            /******************
             * test conversion for one rule
             */		

            /*
		NetworkConfig net = ParseTools.LoadNetwork("purdue.ser");
		Hashtable<String, RouterConfig> routers = net.tableOfRouters;
		RouterConfig aRouter = routers.get("config822");
		Hashtable<String, InterfaceConfig> interfaces = aRouter.tableOfInterfaceByNames;
		InterfaceConfig aInterface = interfaces.get("Vlan1000");
		String aclName = aInterface.outFilters.get(0);
		System.out.println(aclName);

		Hashtable<String, LinkedList<ACLRule>> tACLs = aRouter.tableOfACLs;
		LinkedList<ACLRule> acl = tACLs.get(aclName);
		acl.get(0).permitDeny = "deny";
		acl.get(3).permitDeny = "deny";
		acl.get(8).permitDeny = "deny";
		for(int i = 0; i < acl.size(); i ++)
		{
			System.out.println(acl.get(i));
		}

		BDDACLWrapper bps = new BDDACLWrapper();
		int var = bps.ConvertACLs(acl);
		bps.PrintVar(var);
		bps.CleanUp();
             */

            /***
             * check the size of a single acl rule
             */
            BDDACLWrapper bps = new BDDACLWrapper();
            int prefix = bps.encodeDstIPPrefix(128, 8);
            prefix = bps.popMPLSLabel(prefix);
            System.out.println(prefix);

            int prefix2 = bps.encodeDstIPPrefix(256, 8);
            prefix2 = bps.popMPLSLabel(prefix2);

            // this checks whether label field is all 1's
            System.out.println(bps.isMPLSPkt(prefix));

            int label_bdd = bps.encodeMPLSLabel(3);
            int prefix_label = bps.pushMPLSLabel(prefix, label_bdd);

            int label_bdd2 = bps.encodeMPLSLabel(4);
            int prefix_label2 = bps.pushMPLSLabel(prefix2, label_bdd2);

            int combined_pkt = bps.or(prefix_label, prefix_label2);

            System.out.println(bps.isMPLSPkt(prefix_label));

            int prefix_pop = bps.popMPLSLabel(prefix_label);

            System.out.println(bps.isMPLSPkt(prefix_pop));
            System.out.println(prefix_pop);

            int combined_pkt_pop = bps.popMPLSLabel(combined_pkt);
            System.out.println(combined_pkt_pop);

            /*
		StoreACL sa = StoreACL.LoadNetwork("purdue-ACLs.ser");
		LinkedList<LinkedList<ACLRule>> acllists = sa.ACLList;


		for(int i = 0; i < acllists.size(); i ++)
		{
			LinkedList<ACLRule> acls = acllists.get(i);
			for(int j = 0; j < acls.size(); j ++)
			{
				ACLRule aclr = acls.get(j);
				int aclrnode = bps.ConvertACLRule(aclr);
				System.out.println(bps.getNodeSize(aclrnode));
			}
		}
             */

            /**
             * test group function
             */
            /*
		ACLRule acl0 = new ACLRule();
		acl0.permitDeny = "deny";
		ACLRule acl1 = new ACLRule();
		acl1.permitDeny = "deny";
		ACLRule acl2 = new ACLRule();
		acl2.permitDeny = "deny";
		ACLRule acl3 = new ACLRule();
		acl3.permitDeny = "deny";
		ACLRule acl4 = new ACLRule();
		acl4.permitDeny = "deny";

		LinkedList<ACLRule> acls = new LinkedList<ACLRule>();
		acls.add(acl0);
		acls.add(acl1);
		acls.add(acl2);
		acls.add(acl3);
		acls.add(acl4);

		LinkedList<int[]> grouped = BDDPacketSet.GroupACLRules(acls);

		for(int i = 0; i < grouped.size(); i ++)
		{
			System.out.println(grouped.get(i)[0] + " " + grouped.get(i)[1]);
		}
             */

            /**
             * integer division...
             */
            //System.out.println(3/2);

            /**
             * test andinbatch and orinbatch
             */
            /*
		BDDPacketSet bps = new BDDPacketSet();
		int var1 = bps.OrInBatch(new int[]{0,1,0});
		bps.PrintVar(var1);
		int var2 = bps.AndInBatch(new int[]{1,1,1});
		bps.PrintVar(var2);
		bps.CleanUp();
             */
      }

      /**
       * handle ref-count
       * @param bdd1
       * @param bdd2
       * @return
       */
      public int or(int bdd1, int bdd2) {
            // TODO Auto-generated method stub
            return aclBDD.ref(aclBDD.or(bdd1, bdd2));
      }
      
      // bdd1 and (not bdd2)
      public int diff(int bdd1, int bdd2)
      {
            int not2 = aclBDD.ref(aclBDD.not(bdd2));
            int diff = aclBDD.ref(aclBDD.and(bdd1, not2));
            aclBDD.deref(not2);
            
            return diff;
            
      }
      /**
       * bdd1 <- bdd1 and (not bdd2)
       * @param bdd1
       * @param bdd2
       * @return
       */
      public int diffTo(int bdd1, int bdd2)
      {
          int res = diff(bdd1, bdd2);
          aclBDD.deref(bdd1);
          return res;
      }
      /**
       * wrapper of BDD's orTo
       * @param bdd1
       * @param bdd2
       * @return
       */
      public int orTo(int bdd1, int bdd2)
      {
          return aclBDD.orTo(bdd1, bdd2);
      }
      
      public boolean checkIPTunnelPktCorrectness(int pkt)
      {
            int nottunneled = aclBDD.ref(aclBDD.and(pkt, aclBDD.not(dstIPInnerBit)));
            int removed = aclBDD.ref(aclBDD.exists(nottunneled, dstIPInnerBit));
            int removed2 = aclBDD.ref(aclBDD.exists(removed, dstIPField));
            if(removed2 == BDDTrue)
            {
                  return true;
            }else
            {
                  return false;
            }
            
      }
      
      /**
       * 
       * @param field_name - e.g., mpls_label field, inner destination ip address field
       * @param pkt - packet set represented by BDD
       * @param positive - if true, set field bit to one, if false, set field bit to zero
       * @return
       */
      public int set_field_bit(Fields field_name, int pkt, boolean positive)
      {
            int field_bit = BDDFalse;
            switch(field_name){
            case dst_ip_inner: field_bit = dstIPInnerBit; 
            break;
            case mpls_label: field_bit = mplsLabelBit;
            break;
            default: System.err.println(field_name + " not supported."); 
            System.exit(-1);
            }
            
            
            if(positive)
            {
                  return set_field_bit(field_bit, pkt);
            }else
            {
                  return set_field_bit_not(field_bit, pkt);
            }
      }
      
      private int set_field_bit(int field_bit, int pkt)
      {
            int result = aclBDD.ref(aclBDD.exists(pkt, field_bit));
            result = aclBDD.andTo(result, field_bit);
            return result;
      }
      
      private int set_field_bit_not(int field_bit, int pkt)
      {
            int result = aclBDD.ref(aclBDD.exists(pkt, field_bit));
            result = aclBDD.andTo(result, aclBDD.not(field_bit));
            return result;
      }
}
