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

import org.jline.builtins.Completers;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class Main {

	public static void main(String[] args) throws IOException {
		 Terminal terminal = TerminalBuilder.terminal();
	     Completers.FileNameCompleter completer = new Completers.FileNameCompleter();
	     LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(completer).build();
	     usage();
	     
	     while(true) {
	    	 String line = reader.readLine("APKeep>");
             line = line.trim();
             
             if (line.equalsIgnoreCase("exit")) break;
             ParsedLine pl = reader.getParser().parse(line, 0);

             if ("init".equals(pl.word()) && pl.words().size() == 2) {
                 APKeep.init(pl.words().get(1));
             }
             else if("update".equals(pl.word())) {
                 if(pl.words().size() == 1)
                     APKeep.update();
                 else if(pl.words().size() == 2)
                	 APKeep.update(pl.words().get(1));
                 else
                     usage();
             }
             else if("check".equals(pl.word()) && pl.words().size() == 2) {
                 String content = pl.words().get(1);
                 switch (content) {
                     case "whatif" : APKeep.checkLinkFailure(); break;
                     default: usage();
                 }
             }
             else if("dump".equals(pl.word()) && pl.words().size() == 2) {
                 String content = pl.words().get(1);
                 switch (content) {
                     case "loops" : APKeep.dumpLoops(System.out); break;
                     default: usage();
                 }
             }
             else {
                 usage();
             }
	     }
	}

	private static void usage() {
		String[] usage = {
                "APKeep",
                "  Usage: ",
                "    help                               show this message",
                "    init <snapshot>                    initialize with a network snapshot and operation parameters",
                "    update [<changes>]                 push rule changes on the init",
                "    check whatif                       answer \"what if\" questions for each possible link failure",
                "    dump loops                         dump loops",
                "    exit                               exit apkeep"
        };

        for (String u: usage) System.out.println(u);
        System.out.println();
	}
}
