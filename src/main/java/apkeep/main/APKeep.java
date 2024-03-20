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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.alibaba.fastjson.JSONObject;
import apkeep.core.Network;
import apkeep.utils.Evaluator;
import apkeep.utils.Parameters;

public class APKeep {
	
    private static final String currentPath = System.getProperty("user.dir");
    public static String workingPath;
    public static String outputPath;
	public static String name = "unknown";
	public static Network net;
	public static Evaluator eva;

	public static void init(String configPath) throws IOException {
		workingPath = Paths.get(configPath).toRealPath().toString();
		String paraFile = new String(Files.readAllBytes(Paths.get(workingPath, "parameters.json")));
		JSONObject paras = JSONObject.parseObject(paraFile);
		
		parseParameters(paras);
		outputPath = Paths.get(currentPath, "results", name+"_result.txt").toString();
		
		net = new Network(name);
		eva = new Evaluator(name, outputPath);
		
		ArrayList<String> topo = readFile(Paths.get(workingPath, "topo.txt").toString());
		ArrayList<String> devices = readFile(Paths.get(workingPath, "devices.txt").toString());
		Map<String, Set<String>> device_acls = readACLs(Paths.get(workingPath, "acls").toString());
		Map<String, Map<String, Set<String>>> vlan_ports = readVlans(Paths.get(workingPath, "vlan.txt").toString());
		Map<String, Set<String>> device_nats = readNATs(Paths.get(workingPath, "nat.txt").toString());
		
		net.initializeNetwork(topo, devices, device_acls, vlan_ports, device_nats);
	}

	private static void parseParameters(JSONObject paras) {
		if(paras.containsKey("NAME")) 
			name = paras.getString("NAME");
		if(paras.containsKey("BDD_TABLE_SIZE")) 
			Parameters.BDD_TABLE_SIZE = paras.getIntValue("BDD_TABLE_SIZE");
		if(paras.containsKey("GC_INTERVAL")) 
			Parameters.GC_INTERVAL = paras.getIntValue("GC_INTERVAL");
		if(paras.containsKey("TOTAL_AP_THRESHOLD")) 
			Parameters.TOTAL_AP_THRESHOLD = paras.getIntValue("TOTAL_AP_THRESHOLD");
		if(paras.containsKey("LOW_MERGEABLE_AP_THRESHOLD")) 
			Parameters.LOW_MERGEABLE_AP_THRESHOLD = paras.getIntValue("LOW_MERGEABLE_AP_THRESHOLD");
		if(paras.containsKey("HIGH_MERGEABLE_AP_THRESHOLD")) 
			Parameters.HIGH_MERGEABLE_AP_THRESHOLD = paras.getIntValue("HIGH_MERGEABLE_AP_THRESHOLD");
		if(paras.containsKey("FAST_UPDATE_THRESHOLD")) 
			Parameters.FAST_UPDATE_THRESHOLD = paras.getDoubleValue("FAST_UPDATE_THRESHOLD");
		if(paras.containsKey("PRINT_RESULT_INTERVAL")) 
			Parameters.PRINT_RESULT_INTERVAL = paras.getIntValue("PRINT_RESULT_INTERVAL");
		if(paras.containsKey("WRITE_RESULT_INTERVAL")) 
			Parameters.WRITE_RESULT_INTERVAL = paras.getIntValue("WRITE_RESULT_INTERVAL");
	}

	public static void update() {
		try {
			net.run(eva, Paths.get(workingPath, "updates").toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
		eva.printExpResults();
	}

	public static void update(String inputFile) {
		try {
			net.run(eva, Paths.get(inputFile).toRealPath().toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
		eva.printExpResults();
	}
	
	public static void checkLinkFailure() {
		try {
			net.checkLinkFailure(eva);
		} catch (IOException e) {
			e.printStackTrace();
		}
		eva.printLinkFailureResults();
	}

	public static void dumpLoops(PrintStream printer) {
		eva.printLoop(printer);
	}
	
	public static ArrayList<String> readFile(String inputFile) throws IOException{
		File file = new File(inputFile);
		if(!file.exists()) return null;
		
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(inputFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		ArrayList<String> contents = new ArrayList<String>();
		
		String OneLine;
		while((OneLine = br.readLine()) != null) {
			String linestr = OneLine.trim();
			contents.add(linestr);
		}
		
		return contents;
	}
	
	public static Map<String, Set<String>> readACLs(String inputFile) {
		File file = new File(inputFile);
		if(!file.exists()) return null;

		Map<String, Set<String>> device_acls = new HashMap<>();
		File[] files = file.listFiles();
		for(int i = 0; i < files.length; i ++){
			String acl_name = files[i].getName();
			if (acl_name.endsWith("_usage")) continue;
			String device = acl_name.split("_")[0];
			int index = 1;
			if(name.equals("stanford")) {
				device = device +"_"+acl_name.split("_")[1];
				index++;
			}
			String acl = acl_name.split("_")[index];
			
			device_acls.putIfAbsent(device, new HashSet<>());
			device_acls.get(device).add(acl);
		}
		
		return device_acls;
	}
	
	public static Map<String, Map<String, Set<String>>> readVlans(String inputFile) throws IOException {
		File file = new File(inputFile);
		if(!file.exists()) return null;
		
		Map<String, Map<String, Set<String>>> vlan_ports = new HashMap<>();
		ArrayList<String> rules = readFile(inputFile);
		for(String rule : rules) {
			String[] tokens = rule.split(" ");
			String device = tokens[0];
			String vlan = tokens[1];
			HashSet<String> ports = new HashSet<String>();
			for (int i=2; i<tokens.length; i++) {
				ports.add(tokens[i]);
			}
			vlan_ports.putIfAbsent(device, new HashMap<>());
			vlan_ports.get(device).put(vlan, ports);
		}
		
		return vlan_ports;
	}
	
	public static Map<String, Set<String>> readNATs(String inputFile) {
		File file = new File(inputFile);
		if(!file.exists()) return null;
		
		ArrayList<String> rules = null;
		try {
			rules = readFile(inputFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Map<String, Set<String>> device_nats = new HashMap<>();
		for(String rule : rules) {
			String[] tokens = rule.split(" ");
			String device = tokens[0];
			String port = tokens[1];
			device_nats.putIfAbsent(device, new HashSet<>());
			if(!device_nats.get(device).contains(port)) {
				device_nats.get(device).add(port);
			}
		}
		
		return device_nats;
	}
	
	public static Map<String, Set<String>> readNATs(String inputFile, int num) {
		File file = new File(inputFile);
		if(!file.exists()) return null;
		
		ArrayList<String> rules = null;
		try {
			rules = readFile(inputFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Map<String, Set<String>> device_nats = new HashMap<>();
		int nat_count = 0;
		device_nats = new HashMap<>();
		for(String rule : rules) {
			String[] tokens = rule.split(" ");
			String op = tokens[0];
			String type = tokens[1];
			if (op.equals("-")) break;
			if (!type.equals("nat")) continue;
			String device = tokens[2];
			String port = tokens[3];
			device_nats.putIfAbsent(device, new HashSet<>());
			if(!device_nats.get(device).contains(port)) {
				device_nats.get(device).add(port);
				nat_count++;
				if(nat_count>=num) break;
			}
		}
		
		return device_nats;
	}
	
	public static void main(String[] args) throws IOException {
		init("F:/Experiments/apkeep-opensource/stanford-noacl");
	}
}
