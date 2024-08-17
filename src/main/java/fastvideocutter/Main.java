package fastvideocutter;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.filechooser.FileSystemView;
import processes.Execute;
import processes.StreamGobbler;
import processes.StreamGobbler.Type;
import timer.DurationFormat;

public class Main {
	private static Process PROCESS = null;
	
	public static void main(String[] args) {
		boolean startedWithoutArgs = false;
		if (args.length == 0) {
			startedWithoutArgs = true;
			
			printHelp();
			
			Scanner scan = new Scanner(System.in);
			System.out.print("Input file (drag and drop works, URLs work. 'm' for merge mode): ");
			args = new String[4];
			args[0] = scan.nextLine();
			args[0] = args[0].replaceAll("\"", "");
			
			if (args[0].toLowerCase().equals("m") || args[0].toLowerCase().equals("merge")) {
				merge();
				return;
			}
			
			System.out.print("Start time: ");
			args[1] = scan.nextLine();
			
			if (args[1].isBlank()) {
				args[1] = "0";
			}
			
			System.out.print("End time: ");
			args[2] = scan.nextLine();
			
			System.out.println("New name (default is \"out\"): ");
			System.out.println("(For absolute path put drive letter and slash first, and end it with file ext)");
			if (isURL(args[0])) {
				System.out.println("(One empty enter adds downloads folder automatically as a path)");
			}
			args[3] = scan.nextLine();
			
			if (args[3].isEmpty() && isURL(args[0])) {
				System.out.println("Output filename (include extension):");
				args[3] = getDownloadsFolder().getAbsolutePath() + "\\";
				System.out.print(args[3]);
				args[3] += scan.nextLine();
			}
			
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
		
		//If file is m3u8, it might be master file with many qualities, we don't want to download all of them.
		//FFmpeg will choose the highest quality by default.
		String mapStreams = "-map 0:v -map 0:a ";
		if (type.equals("m3u8")) {
			mapStreams = "";
		}
		
		int idx = Math.max(input.lastIndexOf("\\"), input.lastIndexOf("/"));
		if (idx >= 0) {
			outPath = input.substring(0, idx + 1);
		}
		
		if (args.length == 4 && !args[3].isBlank()) {
			outName = args[3];
			if (outName.endsWith(".mp4") || outName.endsWith(".mkv")) {
				type = outName.substring(outName.lastIndexOf(".") + 1);
				outName = outName.substring(0, outName.lastIndexOf("."));
			}
		}
		
		String startDot = ".\\";
		if (Execute.programExists("ffmpeg")) {
			startDot = "";
		}
		
		String command;
		if (isWindowsAbsolutePath(outName)) { //Doesn't include the extension
			command = startDot + "ffmpeg -protocol_whitelist file,http,https,tcp,tls" + startString + " -i \"" + input + "\"" + endString + " -c copy " + mapStreams + "\"" + outName + "." + type + "\"";
		} else {
			command = startDot + "ffmpeg -protocol_whitelist file,http,https,tcp,tls" + startString + " -i \"" + input + "\"" + endString + " -c copy " + mapStreams + "\"" + outPath + outName + "." + type + "\"";
		}
		
		executeCommand(command);
		
		if (startedWithoutArgs) {
			System.out.println("\nDone.");
			System.out.println("Press enter to exit.");
			new Scanner(System.in).nextLine(); //Keeps the console open until user presses enter.
		}
	}
	
	private static void printHelp() {
		System.out.println("Command format:");
		System.out.println("FastVideoCutter.exe \"path to the mp4 file.mp4\" startTime(ss / mm:ss / hh:mm:ss.lll etc) endTime outputName\n");
		
		System.out.println("Like this: FastVideoCutter.exe \"D:\\Videot\\OBS\\Recordings\\Alchemy glithcless.mp4\" 30:00 35:00\n");
		
		System.out.println("Start time and end time are in seconds (neither is required). OutputName is also optional (defaults to \"out\").");
		System.out.println("For absolute path put drive letter and slash first, and end it with file ext.");
		System.out.println("End time can be duration too if you put d (for duration) right before the time.");
		System.out.println("Like so: d5:00 (means 5 minutes of video).");
		System.out.println("Time format always includes seconds. You can add others intuitively.");
		System.out.println("':' separates hours, minutes and seconds. '.' separates seconds from milliseconds.");
		System.out.println("Doesn't override a file, so make sure the outputName doesn't already exist in the directory.\n");
	}
	
	private static boolean isWindowsAbsolutePath(String path) {
        //Regular expression to match Windows absolute path
        Pattern pattern = Pattern.compile("^[a-zA-Z]:\\\\.+$");
        Pattern pattern2 = Pattern.compile("^[a-zA-Z]:/.+$");
		
		Matcher matcher = pattern.matcher(path);
        Matcher matcher2 = pattern2.matcher(path);
		
        return matcher.matches() || matcher2.matches();
    }
	
	private static boolean isURL(String url) {
        Pattern pattern = Pattern.compile("^https?:.*$", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(url);
        return matcher.matches();
    }
	
	private static File getDefaultDirectory() {
        FileSystemView fileSystemView = FileSystemView.getFileSystemView();
        return fileSystemView.getDefaultDirectory();
    }
	
	private static File getDownloadsFolder() {
		String userHome = System.getProperty("user.home");
        File downloads = new File(userHome, "Downloads");
		if (downloads.exists() && downloads.isDirectory()) {
            return downloads;
        } else {
            return getDefaultDirectory();
        }
    }

	private static void merge() {
		System.out.println("\nMODE SET TO MERGE. ENTER AT LEAST 2 FILES:\n");
		
		Scanner scan = new Scanner(System.in);
		List<String> files = new ArrayList<>();
		
		while (true) {
			System.out.print("Input file (drag and drop works, URLs work. Video codecs must be same. Empty input stops.): ");
			String line = scan.nextLine();
			if (line.isBlank()) {
				break;
			}
			line = line.replaceAll("\"", "");
			files.add(line);
		}
		
		if (files.size() <= 1) {
			System.out.println("At least 2 files needed!");
			System.out.println("Press enter to exit.");
			scan.nextLine();
			return;
		}
		
		System.out.println("Output filename (include extension):");
		String output = getDownloadsFolder().getAbsolutePath() + "\\";
		System.out.print(output);
		output += scan.nextLine();
		
		String dot = ".\\";
		if (Execute.programExists("ffmpeg")) {
			dot = "";
		}
		
		String command;
		if (isWindowsAbsolutePath(output)) {
			String filesString = "";
			
			for (int i = 0; i < files.size(); i++) {
				String file = files.get(i);
				if (i != 0) {
					filesString += " & ";
				}
				filesString += "echo file '" + file + "'";
			}
			
			command = "(" + filesString + ") | " + dot + "ffmpeg -protocol_whitelist file,pipe,http,https,tcp,tls -f concat -safe 0 -i pipe:0 -c copy \"" + output + "\"";
		} else {
			System.out.println("Error! File path is not absolute, even though it should be.");
			return;
		}
		
		executeCommand(command);
		
		System.out.println("\nDone.");
		System.out.println("Press enter to exit.");
		scan.nextLine(); //Keeps the console open until user presses enter.
	}
	
	private static void executeCommand(String command) {
		PROCESS = Execute.executeCommandGetProcess(command);
		Execute.gobbleStream(PROCESS, true, Type.OUTPUT);
		Execute.gobbleStream(PROCESS, true, Type.ERROR);
		
		System.out.println("Command \'" + command + "\' executed");
		
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if (PROCESS != null && PROCESS.isAlive()) {
				System.out.println("Shutdown hook triggered. Terminating ffmpeg process.");
				Execute.cancelProcess(PROCESS);
				System.out.println("\nProcess terminated.");
			}
		}));
		
		System.out.println("Waiting for command to end.");
		
		try {
			PROCESS.waitFor();
			if (PROCESS.isAlive()) {
				Execute.cancelProcess(PROCESS);
			}
		} catch (InterruptedException e) {
			e.printStackTrace(System.err);
		}
	}
}
