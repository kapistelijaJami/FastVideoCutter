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

/*TODO: Might want to implement faster download for m3u8 files, like in TwitchRecover, see TwitchRecover.Core.Downloader.Download.TSDownload()
It downloads them concurrently and then combines (literally just merges bytes back to back). But since this can set the times we wouldn't need to download all the .ts files.
Not sure how to get the correct video length and start time then. Maybe if the downloaded length is long, download all and then combine and cut with ffmpeg,
but otherwise let ffmpeg do it all the way. Or figure out a way to do it better.*/
public class Main {
	private static Process PROCESS = null;
	
	public static void main(String[] args) {
		boolean askForExitWhenDone = false;
		
		Arguments arguments;
		
		if (args.length == 0) {
			arguments = new Arguments();
			askForExitWhenDone = true;
			
			printShortHelp();
			
			Scanner scan = new Scanner(System.in);
			System.out.print("Input file (drag and drop works, URLs work. 'm' for merge mode, 'a' for accurate timings (slow)): ");
			String arg = scan.nextLine();
			
			if (arg.toLowerCase().equals("h") || arg.toLowerCase().equals("help")) {
				printFullHelp();
				waitForEnter();
				return;
			} else if (arg.toLowerCase().equals("m") || arg.toLowerCase().equals("merge")) {
				merge();
				return;
			} else if (arg.toLowerCase().equals("a") || arg.toLowerCase().equals("accurate")) {
				arguments.accurateMode = true;
				System.out.print("Input file (drag and drop works, URLs work): ");
				arguments.inputFile = scan.nextLine();
				arguments.inputFile = arguments.inputFile.replaceAll("\"", "");
			} else {
				arguments.inputFile = arg.replaceAll("\"", "");
			}
			
			System.out.print("Start time: ");
			arguments.startTime = scan.nextLine();
			
			if (arguments.startTime.isBlank()) {
				arguments.startTime = "0";
			}
			
			System.out.print("End time: ");
			arguments.endTime = scan.nextLine();
			
			System.out.println("New name (default is \"out\"): ");
			System.out.println("(For absolute path put drive letter and slash first, and end it with file ext)");
			if (isURL(arguments.inputFile)) {
				System.out.println("(One empty enter adds downloads folder automatically as a path)");
			}
			arguments.outputFilename = scan.nextLine();
			
			if (arguments.outputFilename.isEmpty() && isURL(arguments.inputFile)) {
				System.out.println("Output filename (include extension):");
				arguments.outputFilename = getDownloadsFolder().getAbsolutePath() + "\\";
				System.out.print(arguments.outputFilename);
				arguments.outputFilename += scan.nextLine();
			}
			
		} else if (args.length < 2 || args[0].equals("-h")) {
			printFullHelp();
			return;
		} else {
			arguments = Arguments.parseArguments(args);
		}
		
		
		
		String startString = "";
		Duration start = DurationFormat.parseSimple(arguments.startTime);
		if (start.toMillis() != 0) {
			startString = " -ss " + (start.toMillis() / 1000.0);
		}
		
		String endString = "";
		
		if (!arguments.endTime.isBlank()) {
			if (arguments.endTime.startsWith("d")) {
				Duration e = DurationFormat.parseSimple(arguments.endTime.substring(1));
				endString = " -t " + (e.toMillis() / 1000.0);
			} else {
				Duration e = DurationFormat.parseSimple(arguments.endTime);
				endString = " -t " + (e.minus(start).toMillis() / 1000.0);
			}
		}
		
		String outPath = "";
		String outName = "out";
		
		String input = arguments.inputFile;
		String type = input.substring(input.lastIndexOf(".") + 1);
		
		//If file is m3u8, it might be master file with many qualities, we don't want to download all of them.
		//FFmpeg will choose the highest quality by default.
		String mapStreams = "-map 0:v -map 0:a "; //This picks all streams
		if (type.equals("m3u8")) {
			mapStreams = "";
		}
		
		int idx = Math.max(input.lastIndexOf("\\"), input.lastIndexOf("/"));
		if (idx >= 0) {
			outPath = input.substring(0, idx + 1);
		}
		
		if (!arguments.outputFilename.isBlank()) {
			outName = arguments.outputFilename;
			if (outName.endsWith(".mp4") || outName.endsWith(".mkv")) {
				type = outName.substring(outName.lastIndexOf(".") + 1);
				outName = outName.substring(0, outName.lastIndexOf("."));
			}
		}
		
		String startDot = ".\\";
		if (Execute.programExists("ffmpeg")) {
			startDot = "";
		}
		
		String codec = " -c copy ";
		if (arguments.accurateMode) {
			codec = " -c:v libx264 -crf 18 -c:a aac ";
		}
		
		String command;
		if (isWindowsAbsolutePath(outName)) { //Doesn't include the extension
			command = startDot + "ffmpeg -protocol_whitelist file,http,https,tcp,tls" + startString + " -i \"" + input + "\"" + endString + codec + mapStreams + "\"" + outName + "." + type + "\"";
		} else {
			command = startDot + "ffmpeg -protocol_whitelist file,http,https,tcp,tls" + startString + " -i \"" + input + "\"" + endString + codec + mapStreams + "\"" + outPath + outName + "." + type + "\"";
		}
		
		executeCommand(command);
		
		if (askForExitWhenDone) {
			System.out.println("\nDone.");
			waitForEnter();
		}
	}
	
	private static void waitForEnter() {
		System.out.println("Press enter to exit.");
		new Scanner(System.in).nextLine(); //Keeps the console open until user presses enter.
	}
	
	private static void printShortHelp() {
		System.out.println("Type 'h' or run the exe with '-h' flag for help.\n");
	}
	
	private static void printFullHelp() {
		System.out.println("\nCommand format:");
		System.out.println("FastVideoCutter.exe \"path to the video file.mp4\" startTime endTime outputName\n");
		
		System.out.println("Like this: FastVideoCutter.exe \"D:\\Videot\\OBS\\Recordings\\Video.mp4\" 30:00 35:00\n");
		
		System.out.println("Or you can use flags. If you want to mix flags with the default format, put them last.");
		
		System.out.println("Start time and end time are optional (defaults to video start and end times respectively).");
		System.out.println("Time format always includes seconds. You can add others intuitively.");
		System.out.println("':' separates hours, minutes and seconds. '.' separates seconds from milliseconds.");
		System.out.println("OutputName is also optional (defaults to \"out\" and same type as input file).");
		System.out.println("For absolute path put drive letter, colon and slash first, and end it with file ext.");
		System.out.println("End time can be duration too if you put d (for duration) right before the time.");
		System.out.println("Like so: d5:00 (means 5 minutes of video). (You can also use flags as program arguments)");
		System.out.println("Doesn't override a file, so make sure the outputName doesn't already exist in the directory.");
		System.out.println("You can change format by giving type extension at the end of the outputName.\n");
		
		System.out.println("Flags:");
		System.out.println("-i or --input\t\tInput file location.");
		System.out.println("-s or --start\t\tStart time.");
		System.out.println("-e or --end\t\tEnd time.");
		System.out.println("-d or --duration\tDuration (Use either this or End time).");
		System.out.println("-o or --output\t\tOutput file name.");
		System.out.println("-a or --accurate\tAccurate timings. Won't use I-frames, so is accurate but slow. Will re-encode the video.\n");
		
		System.out.println("Flag usage: FastVideoCutter.exe input.mp4 -s 15:35 -e 21:13 -o outputName.mp4");
		System.out.println("If -o is last flag it can be omitted and have only outputName for convenience.\n");
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
