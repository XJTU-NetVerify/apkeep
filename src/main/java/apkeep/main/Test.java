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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import apkeep.core.Network;
import apkeep.utils.Evaluator;
import apkeep.utils.Parameters;

public class Test {
	
	public static ArrayList<String> readFile(String inputFile) throws IOException{
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
	public static void writeToFile(String outputFile, List<String> contents) throws IOException {
		File file = new File(outputFile);
		if(!file.getParentFile().exists()) file.getParentFile().mkdirs();
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		for(String cont : contents) {
			writer.write(cont+"\n");
		}
		writer.close();
	}

	public static void main(String[] args) throws Exception {
		String name = "4switch";
//		String name = "airtel1";
//		String name = "airtel2";
//		String name = "internet2";
//		String name = "stanford-noacl";
//		String name = "purdue-noacl";
//		String name = "stanford";
//		String name = "purdue";
		int nat_num = 0;
		String datasetFolder = Parameters.root_path+name;
		String outFolder = datasetFolder+"/out.txt";
		
		String op_mode = "normal";
		ArrayList<String> topo = readFile(datasetFolder+"/topo.txt");
		Set<String> devices = null;
		Map<String, Set<String>> device_acls = null;
		Map<String, Map<String, Set<String>>> vlan_ports = null;
		Map<String, Set<String>> device_nats = null;
		String updateFile = "/updates";
//		ArrayList<String> rules = readFile(datasetFolder+"/updates");
		
		if(name.startsWith("purdue")) {
			devices = new HashSet<>();
			for(int i=1;i<=1646;i++) devices.add("config"+i);
		}
		if(name.equals("purdue") || name.equals("stanford")) {
			op_mode = "division";
			device_acls = new HashMap<>();
			String aclFolder = datasetFolder+"/acls";
			File file = new File(aclFolder);
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
		}
		if(name.startsWith("stanford")) {
			vlan_ports = new HashMap<>();
			ArrayList<String> rules = readFile(datasetFolder+"/vlan.txt");
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
		}
		if(name.equals("internet2") || name.equals("stanford-noacl")) {
			if(nat_num > 0) {
				updateFile = "/update+nat";
				ArrayList<String> rules = readFile(datasetFolder+updateFile);
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
						if(nat_count>=nat_num) break;
					}
				}
			}
		}
		
		Network net = new Network(name, op_mode);
		Evaluator eva = new Evaluator(name,outFolder);
		net.initializeNetwork(topo, devices, device_acls, vlan_ports, device_nats);
		net.run(eva, datasetFolder+updateFile);
		eva.printExpResults();
	}
}
