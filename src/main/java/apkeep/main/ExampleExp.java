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
package apkeep.main;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import apkeep.core.Network;
import apkeep.utils.Evaluator;

public class ExampleExp {
	
	private static final String currentPath = System.getProperty("user.dir");
	public static String workingPath;
	public static String outputPath;
	public static String updateFile;
	
	public static String name;
	public static Network net;
	public static Evaluator eva;
	
	public static ArrayList<String> topo;
	public static ArrayList<String> devices = null;
	public static Map<String, Set<String>> device_acls = null;
	public static Map<String, Map<String, Set<String>>> vlan_ports = null;
	public static Map<String, Set<String>> device_nats = null;
	
	private static void initialize() throws IOException {
		workingPath = Paths.get(currentPath, "networks", name).toString();
		outputPath = Paths.get(currentPath, "results", name+"_result.txt").toString();
		updateFile = Paths.get(workingPath, "updates").toString();
		
		topo = APKeep.readFile(Paths.get(workingPath, "topo.txt").toString());
		
		net = new Network(name);
		eva = new Evaluator(name,outputPath);
	}
	
	private static void runUpdates() throws Exception {
		net.initializeNetwork(topo, devices, device_acls, vlan_ports, device_nats);
		net.run(eva, updateFile);
		eva.printExpResults();
	}
	
	private static void runLinkFailure() throws Exception {
		net.checkLinkFailure(eva);
		eva.printLinkFailureResults();
	}
	
	private static void run4SwitchExp() throws Exception {
		
		name = "4switch";
		initialize();
		runUpdates();
	}
	
	private static void runAirtel1Exp() throws Exception {
		
		name = "airtel1";
		initialize();
		runUpdates();
		
	}
	private static void runAirtel2Exp() throws Exception {
		
		name = "airtel2";
		initialize();
		runUpdates();
	}
	private static void runInternet2Exp() throws Exception {
		
		name = "internet2";
		initialize();
		runUpdates();
	}
	private static void runStanfordNoACLExp() throws Exception {
		
		name = "stanford-noacl";
		initialize();
		vlan_ports = APKeep.readVlans(Paths.get(workingPath, "vlan.txt").toString());
		runUpdates();
	}
	private static void runPurdueNoACLExp() throws Exception {
		
		name = "purdue-noacl";
		initialize();
		devices = APKeep.readFile(Paths.get(workingPath, "devices.txt").toString());
		runUpdates();
	}
	private static void runStanfordExp() throws Exception {
		
		name = "stanford";
		initialize();
		device_acls = APKeep.readACLs(Paths.get(workingPath, "acls").toString());
		vlan_ports = APKeep.readVlans(Paths.get(workingPath, "vlan.txt").toString());
		runUpdates();
	}
	private static void runPurdueExp() throws Exception {
		
		name = "purdue";
		initialize();
		devices = APKeep.readFile(Paths.get(workingPath, "devices.txt").toString());
		device_acls = APKeep.readACLs(Paths.get(workingPath, "acls").toString());
		runUpdates();
	}
	private static void runInternet2NATExp(int num) throws Exception {
		
		name = "internet2";
		initialize();
		updateFile = Paths.get(workingPath, "update+nat").toString();
		device_nats = APKeep.readNATs(Paths.get(workingPath, "update+nat").toString(), num);
		runUpdates();
	}
	private static void runStanfordNATExp(int num) throws Exception {
		
		name = "stanford-noacl";
		initialize();
		updateFile = Paths.get(workingPath, "update+nat").toString();
		vlan_ports = APKeep.readVlans(Paths.get(workingPath, "vlan.txt").toString());
		device_nats = APKeep.readNATs(Paths.get(workingPath, "update+nat").toString());
		runUpdates();
	}
	
	private static void run4SwitchLinkFailureExp() throws Exception {
		
		name = "4switch";
		initialize();
		runUpdates();
		runLinkFailure();
	}
	
	private static void runAirtelLinkFailureExp() throws Exception {
		
		name = "airtel1-only-inserts";
		initialize();
		runUpdates();
		runLinkFailure();
	}

	public static void main(String[] args) throws Exception {
		
		run4SwitchExp();
		runAirtel1Exp();
		runAirtel2Exp();
		runInternet2Exp();
		runStanfordNoACLExp();
		runStanfordExp();
		runPurdueNoACLExp();
		runPurdueExp();
		
		runInternet2NATExp(20);
		runStanfordNATExp(20);
		
		run4SwitchLinkFailureExp();
		runAirtelLinkFailureExp();
	}
}
