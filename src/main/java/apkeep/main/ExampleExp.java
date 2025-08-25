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
