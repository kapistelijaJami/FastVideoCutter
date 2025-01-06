package fastvideocutter;

public class Arguments {
	public boolean accurateMode = false;
	public String inputFile;
	public String outputFilename;
	public String startTime;
	public String endTime;
	
	public static Arguments parseArguments(String[] args) {
		Arguments arguments = new Arguments();
		
		boolean encounteredAFlag = false;
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			
			if (arg.startsWith("-")) {
				encounteredAFlag = true;
				
				if (arg.equals("-i") || arg.equals("-input")) {
					arg = args[++i];
					arguments.inputFile = arg;
				} else if (arg.equals("-s") || arg.equals("-start")) {
					arg = args[++i];
					arguments.startTime = arg;
				} else if (arg.equals("-e") || arg.equals("-end")) {
					arg = args[++i];
					arguments.endTime = arg;
				} else if (arg.equals("-d") || arg.equals("-duration")) {
					arg = args[++i];
					arguments.endTime = "d" + arg;
				} else if (arg.equals("-o") || arg.equals("-output")) {
					arg = args[++i];
					arguments.outputFilename = arg;
				} else if (arg.equals("-a") || arg.equals("-accurate")) {
					arguments.accurateMode = true;
				}
				
			} else if (!encounteredAFlag) {
				if (i == 0) {
					arguments.inputFile = arg;
				} else if (i == 1) {
					arguments.startTime = arg;
				} else if (i == 2) {
					arguments.endTime = arg;
				} else if (i == 3) {
					arguments.outputFilename = arg;
				}
			} else if (i == args.length - 1) {
				arguments.outputFilename = arg;
			}
		}
		
		return arguments;
	}
}
