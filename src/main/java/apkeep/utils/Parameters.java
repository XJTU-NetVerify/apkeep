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
package apkeep.utils;

import java.util.HashSet;

import apkeep.checker.Property;

public class Parameters {

	public static boolean MergeAP = true;

	public static int BDD_TABLE_SIZE = 100000000;
//	public static int BDD_TABLE_SIZE = 100000000; // works well for airtel
//	public static int BDD_TABLE_SIZE = 10000000; // works well for 4Switch, 27us
//	public static int BDD_TABLE_SIZE = 1000000; // works well for stanford-noacl, 142us
//	public static int BDD_TABLE_SIZE = 1000; // works well for internet2, 22us
	public static int GC_INTERVAL = 100000;
	public static int TOTAL_AP_THRESHOLD = 500;
	public static int LOW_MERGEABLE_AP_THRESHOLD = 10;
	public static int HIGH_MERGEABLE_AP_THRESHOLD = 50;
	public static final double FAST_UPDATE_THRESHOLD = 0.25;

	public static int PRINT_RESULT_INTERVAL = 100000;
//	public static int PRINT_RESULT_INTERVAL = 10000;
//	public static int PRINT_RESULT_INTERVAL = 1;
	public static int WRITE_RESULT_INTERVAL = 1;

//	public static HashSet<Property> PROPERTIES_TO_CHECK = new HashSet<Property>(){{add(Property.LOOP);add(Property.BLACKHOLE);}};
	public static HashSet<Property> PROPERTIES_TO_CHECK = new HashSet<Property>(){{add(Property.LOOP);}};
	
	public static String root_path = "F:/Experiments/apkeep-opensource/";	
}
