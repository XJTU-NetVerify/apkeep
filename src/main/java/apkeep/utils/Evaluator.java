package apkeep.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import apkeep.checker.Loop;

public class Evaluator {
	
	String name;
	
	int update_num;
	int fast_update;
	
	Set<Loop> loops;
	
	int ap_insert_num;
	int ap_end_num;
	
	// nanotime
	long start_time;
	long mid_time;
	long end_time;
	long update_time;
	long ppm_time;
	long veri_time;
	long total_time;
	
	// bytes
	long initial_memory;
	long peak_memory;
	
	// stats vars
	boolean insert_finish;
	
	Runtime rt;
	String output_file;
	FileWriter output_writer;
	
	// link failure
	int total_links;
	long construction_time;
	long detection_time;
	
	public Evaluator(String net, String outputFile) {
		name = net;
		output_file = outputFile;
		loops = new HashSet<>();
	}
	
	public boolean isInsertFinish() {
		return insert_finish;
	}

	public void setInsertFlag(boolean flag) {
		insert_finish = flag;
	}
	
	public void setInsertAP(int num) {
		ap_insert_num = num;
	}
	
	public void addLoops(Set<Loop> loops) {
		this.loops.addAll(loops);
	}
	
	public void startUpdate() {
		update_num++;
		start_time = System.nanoTime();
	}
	
	public void midUpdate() {
		mid_time = System.nanoTime();
	}
	
	public void endUpdate() {
		end_time = System.nanoTime();
		update_time = end_time - start_time;
		total_time += update_time;
		ppm_time += mid_time - start_time;
		veri_time += end_time - mid_time;
		if (update_time/1000000.0 < Parameters.FAST_UPDATE_THRESHOLD) fast_update ++;
		/*
		 * Periodical garbage collection for BDD and JVM
		 */
		if (update_num % Parameters.GC_INTERVAL == 0) {
			rt.gc();
			rt.gc();
			long current_memory = rt.totalMemory() - rt.freeMemory();
			if (current_memory-initial_memory > peak_memory) {
				peak_memory = current_memory-initial_memory;
			}
		}
	}
	
	public void startExp() {
		rt = Runtime.getRuntime();
		rt.gc();
		rt.gc();
		initial_memory = rt.totalMemory() - rt.freeMemory();
		peak_memory = 0;
		
		insert_finish = false;
		update_num = 0;
		fast_update = 0;
		
		loops.clear();
		
		total_time = 0;
		ppm_time = 0;
		veri_time = 0;
		
		File file = new File(output_file);
		if(!file.getParentFile().exists()) file.getParentFile().mkdirs();
		try {
			output_writer = new FileWriter(file);
			output_writer.write("#rule_id\tAP_num\ttotal_time\tPPM_time\tcheck_time\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void endExp(int apNum) {
		ap_end_num = apNum;
		try {
			output_writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void printUpdateResults(int ap_num) {
		if (update_num % Parameters.PRINT_RESULT_INTERVAL == 0) {
			System.out.println(update_num + " " + ap_num + " " + update_time/1000);
		}
		if (update_num % Parameters.WRITE_RESULT_INTERVAL == 0) {
			try {
				output_writer.write(update_num + "\t" + ap_num + "\t" + update_time/1000
						+ "\t" + (mid_time - start_time)/1000
						+ "\t" + (end_time - mid_time)/1000
						+"\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			output_writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void printExpResults() {
		System.out.println("The " + name + " dataset");
		System.out.println("Number of updates: " + update_num);
		System.out.println("Total time: " + total_time/1000000 + "ms");
		System.out.println("Update PPM time: " + ppm_time/1000000 + "ms");
		System.out.println("Check property time: " + veri_time/1000000 + "ms");
		System.out.println("Number of APs after insert: " + ap_insert_num);
		System.out.println("Number of APs after update: " + ap_end_num);
		
		System.out.println("Number of loops: " + loops.size());

		System.out.println("Average update time: " + total_time/update_num/1000.0 + "us");
		System.out.println(fast_update*100.0/update_num + "% < " + Parameters.FAST_UPDATE_THRESHOLD + "ms");	
		System.out.println("Memory Usage: " + peak_memory/1000000 + "MB");
	}
	
	public void printLoop(PrintStream printer) {
		for(Loop loop : loops) {
			printer.println(loop);
		}
	}
	

	public void addLinkFailure(int total_links2, long construction_time2, long detection_time2) {
		total_links = total_links2;
		construction_time = construction_time2;
		detection_time = detection_time2;
	}

	public void printLinkFailureResults() {
		System.out.println("Number of links " + total_links);
		System.out.println("Average time to construct forwarding graph: " + construction_time / total_links / 1000000.0 + "ms");
		System.out.println("Average time to detect loops: " + detection_time / total_links / 1000000.0 + "ms");
		System.out.println("Average time in total: " + (construction_time + detection_time) / total_links / 1000000.0 + "ms");
	}
}
