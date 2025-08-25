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
