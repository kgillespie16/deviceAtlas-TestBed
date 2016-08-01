/*********************************************************************************
 * This is the main program to test the two different versions of Device Atlas   *
 * for differences in device attributes for same user agent string.              *
 * The program takes the user agent file as an input and processes it to convert *
 * into user agent strings. The user agent strings are then passed to the        *
 * two different versions of DeviceAtlas and the output device attributes are    *
 * then compared. The differences are returned in an output csv file.            *
 ********************************************************************************/    

import deviceAtlas.DeviceAtlas;
import deviceAtlas.V2_1.mobi.mtld.da.Properties;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Demo {

	private static final Logger logger = LoggerFactory.getLogger(Demo.class);
	private static DefaultParser parser = new DefaultParser();
	private static Options options = new Options();
	private static CommandLine commandLine;

	// Private UserAgentData class to store the attributes from input file
	private static class UserAgentData {
		private Integer noOfOccurences;
		private String userAgent;
		public UserAgentData(Integer noOfOccurences, String userAgent) {
			this.noOfOccurences = noOfOccurences;
			this.userAgent = userAgent;
		}
		public Integer getNoOfOccurences() {
			return noOfOccurences;
		}
		public String getUserAgent() {
			return userAgent;
		}
		public String toString() {
			return "No of occurences: " + noOfOccurences + " User Agent String: " + userAgent;
		}
	}

	// Main Method
	public static void main(String [] args){
		Demo.createOptions();
		String inputFilename, outputFilename = null;
		Integer readLimit = null;
		
		try {
			commandLine = parser.parse(options, args);
		} catch (ParseException e) {
			printUsage("ERROR " + e.getMessage());
		}
		
		if( commandLine.hasOption('h') ) { 
			printUsage(); 
			return;
		} else if (commandLine.getOptions().length < 2) {
			printUsage("ERROR: Arguments missing");
		}
		
		try {
			readLimit = Integer.parseInt(commandLine.getOptionValue("readLimit","0"));
		} catch(NumberFormatException e) {
			System.out.println("Limit should be in integer" + e.getMessage());
		}

		inputFilename = commandLine.getOptionValue("inputFileName");
		outputFilename = commandLine.getOptionValue("outputFileName");

		System.out.println("<--------- Starting file read ------------->");
		List<UserAgentData> userAgents = readData(inputFilename);
		System.out.println("<--------- File read complete ------------->");

		if(readLimit==0)
			readLimit=userAgents.size();

		// Location of json files for device atlas. They are located in the deviceAtlas folder.
		String atlasV1Filename = "./deviceAtlas/deviceAtlas1_7.json";
		String atlasV2Filename = "./deviceAtlas/deviceAtlas2_1.json";

		// Sample userAgent
		// String userAgent = "Mozilla/5.0 (Linux; Android 6.0.1; HTC Desire EYE Build/MMB29M; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/48.0.2564.106 Mobile Safari/537.36";
		// String userAgent = "Mozilla/5.0 (Linux; Android 4.4.4; XT1025 Build/KXC21.5-40) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/33.0.0.0 Mobile Safari/537.36";

		// Creates DeviceAtlas object
		DeviceAtlas atlas = new DeviceAtlas(atlasV1Filename, atlasV2Filename);
		PrintWriter pwForDifferences = null;
		PrintWriter pwForUnknowns = null;
		try{
			pwForDifferences = new PrintWriter(outputFilename);
			pwForUnknowns = new PrintWriter("OldVersionNullUnknowns.csv"); // Contains the null/unknown make/madel for older version
		
			pwForDifferences.println("UserAgentString,"+"Key,"+"Version 1.7 Value,"+"Version 2.1 Value");
			pwForUnknowns.println("UserAgentString,"+"Key,"+"Version 1.7 Value,"+"Version 2.1 Value");
			
			for(int i=0; i< readLimit; i++){
				HashMap <String, Map> diff = findDiff(atlas,userAgents.get(i).getUserAgent());
				for (String key : diff.keySet()){
					Object valueV1 = diff.get(key).get("v1");
					Object valueV2 = diff.get(key).get("v2");
					if(valueV1==null || valueV1=="UnKnown"){
						pwForUnknowns.println("\""+userAgents.get(i).getUserAgent() + "\"" + "," + key + "," + valueV1 + "," + valueV2);
					}else{
						pwForDifferences.println("\""+userAgents.get(i).getUserAgent()+"\"" + "," + key + ","+ valueV1 + "," + valueV2 );
					}
				}
			}
			pwForDifferences.close();
			pwForUnknowns.close();
		}
		catch (FileNotFoundException e){
			System.out.println("File not found");
			e.printStackTrace();
		}
	}

	public static HashMap <String, Map> findDiff(DeviceAtlas atlas,String userAgent){
		Map<String, Object> attributesV1 = atlas.getV1DeviceAttributes(userAgent); // Gets Attributes from deviceAtlas1_7 api
		Properties attributesV2 = atlas.getV2DeviceAttributes(userAgent); // Gets Attributes from deviceAtlas2.1 api
		HashMap <String, Map> diff = atlas.getDif(userAgent, attributesV1, attributesV2); // Returns difference from the two attributes
		return diff;
	}

	/** readData : String -> List<UserAgentData>
	 *  Given    : Input file to be read
	 *  Returns  : A list of type UserAgentData containing the number of occurences
	 *  and user agent strings. */
	private static List<UserAgentData> readData(String filename) {
		BufferedReader br = null;
		String line = "";
		String splitBy = "(\\\"user-agent\\\")(:)";
		List<UserAgentData> userAgents = new ArrayList<UserAgentData>();
		try {
			br = new BufferedReader(new FileReader(filename));
			while ((line = br.readLine()) != null) {
				String[] data = line.split(splitBy);
				if (data.length < 2) {
					continue;
				}
				Integer noOfOccurences = Integer.getInteger(data[0].trim());
				String userAgentString = data[1].replaceAll("\"", " ").trim();
				UserAgentData ua = new UserAgentData(noOfOccurences,userAgentString);
				userAgents.add(ua);
			}
		} catch (Exception e) {
			logger.error("error processing input user agents file: " + e.getMessage());
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return userAgents;
	}

	private static void createOptions() {
		options.addOption(getOption("inputFileName", true, "Input Data Filename", "String"));
		options.addOption(getOption("outputFileName", true, "Output Filename", "String"));
		options.addOption(getOption("readLimit", true, "Lines of Input File To be Read", "Integer"));
		options.addOption("h", "help", false, "Print Usage Information");
	}

	private static Option getOption(String longOpt, boolean isRequired, String description, String argType) {
		Option o = new Option(null, longOpt, isRequired, description);
		o.setArgName(argType);
		o.setArgs(1);
		o.setValueSeparator('=');
		return o;
	}

	private static void printUsage(String message) {
		System.out.println(message + "\n");
		printUsage();
		System.exit(-1);
	}

	private static void printUsage() {
		HelpFormatter helpFormatter = new HelpFormatter();
		helpFormatter.setWidth(80);
		helpFormatter.setSyntaxPrefix("USAGE: ");
		helpFormatter.printHelp("java Demo", options);
	}
}
