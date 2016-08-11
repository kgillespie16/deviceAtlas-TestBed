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

		// Parse options to get commandLine
		try {
			commandLine = parser.parse(options, args);
		} catch (ParseException e) {
			printUsage("ERROR " + e.getMessage());
		}

		// To print usage information if the user selects help option
		if( commandLine.hasOption('h') ) {
			printUsage();
			return;
		}
		// Error if the two required arguments are not provided
		else if (commandLine.getOptions().length < 2) {
			printUsage("ERROR: Arguments missing");
		}

		try {
			readLimit = Integer.parseInt(commandLine.getOptionValue("readLimit","0"));
		} catch(NumberFormatException e) {
			System.out.println("Limit should be in integer" + e.getMessage());
		}
		inputFilename = commandLine.getOptionValue("inputFileName");
		outputFilename = commandLine.getOptionValue("outputFileName");

		// Read data from the input file
		List<UserAgentData> userAgents = readData(inputFilename);

		if(readLimit==0)
			readLimit=userAgents.size();

		// Location of json files for device atlas. They are located in the deviceAtlas folder.
		String atlasV1Filename = "./deviceAtlas/deviceAtlas1_7.json";
		String atlasV2Filename = "./deviceAtlas/deviceAtlas2_1.json";

		// Creates DeviceAtlas object
		DeviceAtlas atlas = new DeviceAtlas(atlasV1Filename, atlasV2Filename);

		// Creates PrintWriter objects
		PrintWriter pwForAttributes = null;
		PrintWriter pwForDifferences = null;
		PrintWriter pwForUnknowns = null;
		try{
			// Contains values for attributes vendor, manufacturer and model for both versions
			pwForAttributes = new PrintWriter(outputFilename);

			// Contains differences between vendor of DA1.7 and manufacturer of DA2.1
			pwForDifferences = new PrintWriter("outputDiff.csv");

			// Contains the null/unknown values for make/madel of older version
			pwForUnknowns = new PrintWriter("OldVersionNullUnknowns.csv");

			// Headers for output files
			pwForAttributes.println("UserAgentString,"+"Version1.7 Vendor,"+"Version1.7 Manufacturer,"+"Version1.7 Model,"+
					"Version2.1 Vendor,"+"Version2.1 Manufacturer,"+"Version2.1 Model");
			pwForDifferences.println("UserAgentString,"+"Key,"+"Version 1.7,"+"Version 2.1");
			pwForUnknowns.println("UserAgentString,"+"Version1.7 Vendor,"+"Version1.7 Manufacturer,"+"Version1.7 Model,"+
					"Version2.1 Vendor,"+"Version2.1 Manufacturer,"+"Version2.1 Model");

			// Loop to populate the output csv files
			for(int i=0; i< readLimit; i++){
				HashMap <String, Map> diff = findDiff(atlas,userAgents.get(i).getUserAgent());
				// Returns map of both device atlas attributes
				// Access the same as diff map but contains both equal and different values. keys manufacturer & vendor are now seperate
				HashMap <String, Map> attributes = getAttributes(atlas,userAgents.get(i).getUserAgent());
				Object vendorValueV1=null, manuValueV1 = null, modelValueV1 = null;
				Object vendorValueV2 = null, manuValueV2 = null, modelValueV2 = null;
				for(String key : attributes.keySet()){
					if(key.equals("vendor")){
						vendorValueV1 = attributes.get(key).get("v1");
						vendorValueV2 = attributes.get(key).get("v2");
					}
					if(key.equals("manufacturer")){
						manuValueV1 = attributes.get(key).get("v1");
						manuValueV2 = attributes.get(key).get("v2");
					}
					if(key.equals("model")){
						modelValueV1 = attributes.get(key).get("v1");
						modelValueV2 = attributes.get(key).get("v2");
					}
				}
				if(vendorValueV1==null || (vendorValueV1==null && modelValueV1=="UnKnown")){
					pwForUnknowns.println("\""+userAgents.get(i).getUserAgent() + "\"" + "," + vendorValueV1+ ","+ manuValueV1 + ","+modelValueV1+","+
							vendorValueV2+","+manuValueV2+","+modelValueV2);
				}else{
					pwForAttributes.println("\""+userAgents.get(i).getUserAgent() + "\"" + "," + vendorValueV1+ ","+ manuValueV1 + ","+modelValueV1+","+
							vendorValueV2+","+manuValueV2+","+modelValueV2);
				}
				
				for (String key : diff.keySet()){
					Object valueV1 = diff.get(key).get("v1");
					Object valueV2 = diff.get(key).get("v2");
					if(valueV1==null || valueV1=="UnKnown"){
						//pwForUnknowns.println("\""+userAgents.get(i).getUserAgent() + "\"" + "," + key + "," + valueV1 + "," + valueV2);
					}else{
						pwForDifferences.println("\""+userAgents.get(i).getUserAgent()+"\"" + "," + key + ","+ valueV1 + "," + valueV2 );
					}
				}
			}
			pwForAttributes.close();
			pwForDifferences.close();
			pwForUnknowns.close();
		}
		catch (FileNotFoundException e){
			System.out.println("File not found");
			e.printStackTrace();
		}
	}

	/** findDiff : DeviceAtlas, String -> HashMap<String,Map>
	 *  Given    : an object of DeviceAtlas class and an userAgent string
	 *  Returns  : the differences in attributes of deviceAtlas version
	 *             1.7 and version 2.1 for a given userAgent string.
	 *  Strategy : Using function getDif(String, Map<String, Object>, Properties)
	 *             from DeviceAtlas class to get the differences in attributes.*/
	public static HashMap <String, Map> findDiff(DeviceAtlas atlas,String userAgent){
		// Get Attributes from deviceAtlas1_7 api
		Map<String, Object> attributesV1 = atlas.getV1DeviceAttributes(userAgent);
		// Get Attributes from deviceAtlas2.1 api
		Properties attributesV2 = atlas.getV2DeviceAttributes(userAgent);
		// Returns difference from the two attributes
		HashMap <String, Map> diff = atlas.getDif(userAgent, attributesV1, attributesV2);
		return diff;
	}

	/* Similar to findDiff but doesn't remove any equal values from map.  */
	public static HashMap <String, Map> getAttributes(DeviceAtlas atlas,String userAgent){
		// Gets Attributes from deviceAtlas1_7 api
		Map<String, Object> attributesV1 = atlas.getV1DeviceAttributes(userAgent);
		// Gets Attributes from deviceAtlas2.1 api
		Properties attributesV2 = atlas.getV2DeviceAttributes(userAgent);
		// Returns difference from the two attributes
		HashMap <String, Map> attributes = atlas.combineAttributes(userAgent, attributesV1, attributesV2); 
		return attributes;
	}

	/** readData : String -> List<UserAgentData>
	 *  Given    : Input file to be read
	 *  Returns  : A list of type UserAgentData containing the number of occurences
	 *             and user agent strings. */
	private static List<UserAgentData> readData(String filename) {
		BufferedReader br = null;
		String line = "";
		// regex to split given user agent string
		String splitBy = "(\\\"user-agent\\\")(:)";
		// Contains all the given user agent strings and their occurences
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

	/** createOptions : void -> void
	 *  Used to populate options to be provided to the command line.
	 *  Examples: --inputFileName (The name of the input file to be read) */
	private static void createOptions() {
		options.addOption(getOption("inputFileName", true, "Input Data Filename", "String"));
		options.addOption(getOption("outputFileName", true, "Output Filename", "String"));
		options.addOption(getOption("readLimit", true, "Lines of Input File To be Read", "Integer"));
		options.addOption("h", "help", false, "Print Usage Information");
	}

	/** getOption : String, boolean, String, String -> Option
	 *  Given     : a string representing the long name of the
	 *              option, a boolean, a description string, a
	 *              string representing the type of the option.
	 *  Returns   : an object of Option class created according
	 *              to the given arguments. */
	private static Option getOption(String longOpt, boolean isRequired, String description, String argType) {
		Option o = new Option(null, longOpt, isRequired, description);
		o.setArgName(argType);
		o.setArgs(1);
		o.setValueSeparator('=');
		return o;
	}

	/** printUsage : String -> void
	 *  Prints the given message alongwith the usage options.
	 *  Example: "Error: Arguments missing"
	 *            (If the arguments provided on the command
	 *            line are less than required.)*/
	private static void printUsage(String message) {
		System.out.println(message + "\n");
		printUsage();
		System.exit(-1);
	}

	/** printUsage : void -> void
	 *  Prints the usage information mentioning the name, description
	 *  and argument type of all arguments*/
	private static void printUsage() {
		HelpFormatter helpFormatter = new HelpFormatter();
		helpFormatter.setWidth(80);
		helpFormatter.setSyntaxPrefix("USAGE: ");
		helpFormatter.printHelp("java Demo", options);
	}
}
