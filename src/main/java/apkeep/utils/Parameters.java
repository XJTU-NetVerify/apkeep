package apkeep.utils;

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
	public static double FAST_UPDATE_THRESHOLD = 0.25;

	public static int PRINT_RESULT_INTERVAL = 100000;
//	public static int PRINT_RESULT_INTERVAL = 10000;
//	public static int PRINT_RESULT_INTERVAL = 1;
	public static int WRITE_RESULT_INTERVAL = 1;
}
