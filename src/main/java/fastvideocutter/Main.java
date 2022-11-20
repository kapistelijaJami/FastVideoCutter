package fastvideocutter;

import java.time.Duration;
import java.util.Scanner;
import processes.Execute;
import timer.DurationFormat;

public class Main {
	public static void main(String[] args) {
		boolean startedWithoutArgs = false;
		if (args.length == 0) {
			startedWithoutArgs = true;
			
			printHelp();
			
			Scanner scan = new Scanner(System.in);
			System.out.print("Input file (drag and drop works): ");
			args = new String[4];
			args[0] = scan.nextLine();
			args[0] = args[0].replaceAll("\"", "");
			
			System.out.print("Start time: ");
			args[1] = scan.nextLine();
			
			if (args[1].isBlank()) {
				args[1] = "0";
			}
			
			System.out.print("End time: ");
			args[2] = scan.nextLine();
			
			System.out.print("New name (default is \"out\"): ");
			args[3] = scan.nextLine();
			
		} else if (args.length < 2 || args[0].equals("-h")) {
			printHelp();
			return;
		}
		
		
		
		String startString = "";
		Duration start = DurationFormat.parseSimple(args[1]);
		if (start.toMillis() != 0) {
			startString = " -ss " + (start.toMillis() / 1000.0);
		}
		
		String endString = "";
		
		if (args.length > 2 && !args[2].isBlank()) {
			if (args[2].startsWith("d")) {
				Duration e = DurationFormat.parseSimple(args[2].substring(1));
				endString = " -t " + (e.toMillis() / 1000.0);
			} else {
				Duration e = DurationFormat.parseSimple(args[2]);
				endString = " -t " + (e.minus(start).toMillis() / 1000.0);
			}
		}
		
		String outPath = "";
		String outName = "out";
		
		String input = args[0];
		String type = input.substring(input.lastIndexOf(".") + 1);
		
		int idx = Math.max(input.lastIndexOf("\\"), input.lastIndexOf("/"));
		if (idx >= 0) {
			outPath = input.substring(0, idx + 1);
		}
		
		if (args.length == 4 && !args[3].isBlank()) {
			outName = args[3];
		}
		
		String dot = ".\\";
		if (Execute.programExists("ffmpeg")) {
			dot = "";
		}
		
		String command = dot + "ffmpeg" + startString + " -i \"" + input + "\"" + endString + " -c copy \"" + outPath + outName + "." + type + "\"";
		
		Execute.executeCommand(command, true);
		
		if (startedWithoutArgs) {
			System.out.println("\nDone.");
			System.out.println("Press enter to exit.");
			new Scanner(System.in).nextLine(); //keeps the console open until user presses enter.
		}
	}
	
	private static void printHelp() {
		System.out.println("Command format:");
		System.out.println("FastVideoCutter.exe \"path to the mp4 file.mp4\" startTime(ss / mm:ss / hh:mm:ss.lll etc) endTime outputName\n");
		
		System.out.println("Like this: FastVideoCutter.exe \"D:\\Videot\\OBS\\Recordings\\Alchemy glithcless.mp4\" 30:00 35:00\n");
		
		System.out.println("Start time is required (can be 0), end time is not. OutputName is also optional (defaults to \"out\").");
		System.out.println("End time can be duration too if you put d (for duration) right before the time.");
		System.out.println("Like so: d5:00 (means 5 minutes of video).");
		System.out.println("Time format always includes seconds. You can add others intuitively.");
		System.out.println("':' separates hours, minutes and seconds. '.' separates seconds from milliseconds.");
		System.out.println("Doesn't override a file, so make sure the outputName doesn't already exist in the directory.\n");
	}
}
