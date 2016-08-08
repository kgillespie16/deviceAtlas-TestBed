package deviceAtlas;

import java.io.InputStream;
import java.lang.ClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;

import deviceAtlas.V1_7.mobi.mtld.da.Api;

import deviceAtlas.V2_1.mobi.mtld.da.Properties;
import deviceAtlas.V2_1.mobi.mtld.da.device.DeviceApi;

public class DeviceAtlas {

    private String atlasV1Filename, atlasV2Filename;
    private DeviceApi deviceApi;
    private HashMap<String, Object> deviceTree;
    private static final Splitter SPLIT_ON_DOT = Splitter.on(".").omitEmptyStrings().trimResults().omitEmptyStrings();
    private static final Joiner JOIN_ON_DOT=Joiner.on(".").skipNulls();

    public DeviceAtlas(String v1Filename, String v2Filename){
        atlasV1Filename = v1Filename;
        atlasV2Filename = v2Filename;
        deviceTree = getDeviceTree(atlasV1Filename);
        deviceApi = initDevice(atlasV2Filename);
    }

    // Gets device attributes for deviceAtlas1.7
    public Map<String, Object> getV1DeviceAttributes(String userAgent) {
        if(deviceTree != null){
            Map<String, Object> props = Api.getProperties(deviceTree, userAgent);
            return props;
        }
        else return null;
    }

    // Gets device attributes for deviceAtlas2.1
    public Properties getV2DeviceAttributes(String userAgent){
        if (deviceApi != null){
            try{
                Properties properties = deviceApi.getProperties(userAgent);
                return properties;
            }
            catch(Exception e){ e.printStackTrace(); }
        }
        return null;
    }

    // Gets deviceTree for use in deviceAtlas1.7
    private HashMap<String, Object> getDeviceTree(String filename){
        HashMap<String, Object> deviceTree = null;
        try {
            ClassLoader classLoader = DeviceAtlas.class.getClassLoader();
            InputStream in = classLoader.getResourceAsStream(filename);
            String asString = IOUtils.toString(in, CharEncoding.UTF_8);
            in.close();
            deviceTree = Api.getTreeFromString(asString);
        }
        catch(Exception e){ e.printStackTrace(); }
        return deviceTree;
    }

    // Initialises DeviceApi Object for deviceAtlas2.1
    private DeviceApi initDevice(String filename){
        try{
            DeviceApi deviceApi = new DeviceApi();
            ClassLoader classLoader = DeviceAtlas.class.getClassLoader();
            deviceApi.loadDataFromStream(classLoader.getResourceAsStream(filename));
            return deviceApi;
        }
        catch(Exception e){ e.printStackTrace(); }
        return null;
    }

    // Combines model, vendor/manufacturer, osName, osVersion from both deviceAtlas API's
    private HashMap <String, Map> combineAttributes(String userAgent, Map<String, Object> attributesV1, Properties attributesV2){
            String v1 = "v1";
            String v2 = "v2";
            HashMap <String, Map>  attributes = new HashMap<String, Map>();
            HashMap <String, String> versions = new HashMap<String, String>(2);
            HashMap <String, String> tmp;

            // MODEL
            versions.put(v1, determineName(attributesV1));
            versions.put(v2, determineName(attributesV2));
            tmp = new HashMap<String, String>(versions);
            attributes.put("model", tmp);
            versions.clear();

            // VENDOR/MANU
            versions.put(v1, Attributes.vendor.get(attributesV1));
            if(attributesV2.get("manufacturer")!=null)
            versions.put(v2, attributesV2.get("manufacturer").asString());
            tmp = new HashMap<String, String>(versions);
            attributes.put("vendor", tmp);
            versions.clear();

            // OSNAME
            String osNameV1 = queryDeviceOs(userAgent, attributesV1);
            String osNameV2 = null;
            if(attributesV2.get("osName")!=null)
            osNameV2 = attributesV2.get("osName").asString();
            versions.put(v1, osNameV1);
            versions.put(v2, osNameV2);
            tmp = new HashMap<String, String>(versions);
            attributes.put("osName",tmp);
            versions.clear();

            // OSVERSION
            versions.put(v1, getOsVersion(osNameV1, Attributes.osVersion.get(attributesV1)));
            versions.put(v2, getOsVersion(osNameV2, Attributes.osVersion.get(attributesV2)));
            tmp = new HashMap<String, String>(versions);
            attributes.put("osVersions", tmp);
            versions.clear();

            // deviceWidth
            String widthV1 = Integer.toString(getScreenWidth(attributesV1));
            String widthV2 = Integer.toString(getScreenWidth(attributesV2));
            versions.put(v1, widthV1);
            versions.put(v2, widthV2);
            tmp = new HashMap<String,String>(versions);
            attributes.put("deviceWidth",tmp);
            versions.clear();

            // deviceHeight
            String heightV1 = Integer.toString(getScreenHeight(attributesV1));
            String heightV2 = Integer.toString(getScreenHeight(attributesV2));
            versions.put(v1, heightV1);
            versions.put(v2, heightV2);
            tmp = new HashMap<String,String>(versions);
            attributes.put("deviceHeight",tmp);
            versions.clear();

            // isRobot
            String isRobotV1 = String.valueOf(Attributes.isRobot.hasAttribute(attributesV1));
            String isRobotV2 = String.valueOf(Attributes.isRobot.hasAttribute(attributesV2));
            versions.put(v1, isRobotV1);
            versions.put(v2, isRobotV2 );
            tmp = new HashMap<String,String>(versions);
            attributes.put("isRobot",tmp);
            versions.clear();

            // javaScriptSupported
            versions.put(v1, Attributes.javaScriptSupported.get(attributesV1));
            versions.put(v2, Attributes.javaScriptSupported.get(attributesV2));
            tmp = new HashMap<String,String>(versions);
            attributes.put("javaScriptSupported",tmp);
            versions.clear();

            //.setTablet(Attributes.isTablet.hasAttribute(attributes))
            String isTabletV1 = String.valueOf(Attributes.isTablet.hasAttribute(attributesV1));
            String isTabletV2 = String.valueOf(Attributes.isTablet.hasAttribute(attributesV2));
            versions.put(v1, isTabletV1);
            versions.put(v2, isTabletV2);
            tmp = new HashMap<String,String>(versions);
            attributes.put("isTablet",tmp);
            versions.clear();


            return attributes;
    }

    // Returns Hashmap with values that are different in combined attributes Hashmap
    private HashMap <String, Map> findDif(HashMap <String, Map> attributes){
        String v1 = "v1";
        String v2 = "v2";
        HashMap <String, Map>  dif = new HashMap<String, Map>();
        HashMap <String, String> versions;

        for(String key : attributes.keySet()){
            String valueV1 = Objects.toString(attributes.get(key).get("v1"));
            String valueV2 = Objects.toString(attributes.get(key).get("v2"));
            if (valueV1 != null && valueV2 != null){
                try {
                    if (valueV1.compareTo(valueV2) != 0 ){
                        dif.put(key, attributes.get(key));
                    }
                }
                catch(Exception e){e.printStackTrace();}
            }
            else if ( (valueV1 == null && valueV2 != null) ||  (valueV2 == null && valueV1 != null) ){
                dif.put(key, attributes.get(key));
            }

        }
        return dif;
    }

    // Used to combine and find difference in attributes
    public HashMap <String, Map> getDif(String userAgent, Map<String, Object> attributesV1, Properties attributesV2 ) {
        return findDif(combineAttributes(userAgent, attributesV1, attributesV2));
    }

    // Prints values in attributes hashmap in readable format (Used for debugging)
    public static void printAttributes(HashMap<String, Map> attributes){
        for (String key : attributes.keySet()){
            Object valueV1 = attributes.get(key).get("v1");
            Object valueV2 = attributes.get(key).get("v2");
            System.out.println("Key: " + key + " - " + "Version: 1.7, Value: " + valueV1 + " -- Version: 2.1, Value: " + valueV2 );
        }
    }

    public int getScreenHeight( Map<String, Object> attributes) {
        int height= 144;
        try {
            String res = (String) attributes.get(Attributes.displayHeight.toString());
           height= Integer.parseInt(res);
        } catch(Exception e){}
        return height;
    }

    public int getScreenHeight(Properties attributes) {
        int height= 144;
        try {
            height =  attributes.get(Attributes.displayHeight.toString()).asInteger();
        } catch(Exception e){}
        return height;
    }

    public int getScreenWidth( Map<String, Object> attributes) {
        int width = 176;
        try {
            String res = (String) attributes.get(Attributes.displayWidth.toString());
            width = Integer.parseInt(res);
        } catch(Exception e){}
        return width;
    }

    public int getScreenWidth( Properties attributes) {
        int width = 176;
        try {
            width =  attributes.get(Attributes.displayWidth.toString()).asInteger();
        } catch(Exception e){}
        return width;
    }

    private String determineName(Map<String, Object> attributes) {
        String name = Attributes.model.get(attributes);
        if (StringUtils.isBlank(name)) {
            String mobile = Attributes.mobileDevice.get(attributes);
            String isBrowser = Attributes.isBrowser.get(attributes);
            if ("0".equals(mobile) && "1".equals(isBrowser)) {
                name = "Non-Mobile";
            } else {
                name = "UnKnown";
            }
        }
        return name;
    }

    private String determineName(Properties attributes) {
        String name = Attributes.model.get(attributes);
        if (StringUtils.isBlank(name)) {
            String mobile = Attributes.mobileDevice.get(attributes);
            String isBrowser = Attributes.isBrowser.get(attributes);
            if ("0".equals(mobile) && "1".equals(isBrowser)) {
                name = "Non-Mobile";
            } else {
                name = "UnKnown";
            }
        }
        return name;
    }

    private String queryDeviceOs(String userAgent, Map<String, Object> attributes) {

        String additionalInfo = Attributes.osProprietary.get(attributes);

        DeviceOs deviceOs = checkBooleanAttributeHits(attributes, additionalInfo);
        if (deviceOs != DeviceOs.Unknown) return deviceOs.toString();
        deviceOs = checkProprietaryString(additionalInfo);
        if (deviceOs != DeviceOs.Unknown) return deviceOs.toString();
        deviceOs = checkUserAgentDirectly(userAgent);
        if (deviceOs != DeviceOs.Unknown) return deviceOs.toString();
        return null;
    }

    private DeviceOs checkBooleanAttributeHits(final Map<String, Object> attributes, final String additionalInfo) {
        if (Attributes.osAndroid.hasAttribute(attributes)) return DeviceOs.Android;

        if (Attributes.osOsx.hasAttribute(attributes)) return DeviceOs.Ios;

        if (Attributes.osRim.hasAttribute(attributes)) return DeviceOs.Rim;

        if (Attributes.osSymbian.hasAttribute(attributes)) return DeviceOs.Symbian;

        if (Attributes.osWindows.hasAttribute(attributes)) return DeviceOs.Windows;

        if (Attributes.osBada.hasAttribute(attributes)) return DeviceOs.Bada;

        if (Attributes.osLinux.hasAttribute(attributes)) {
            if (DeviceOs.WebOS.compareOsString(additionalInfo)) {
                return DeviceOs.WebOS;
            } else {
                return DeviceOs.Linux;
            }
        }

        return DeviceOs.Unknown;
    }

    private DeviceOs checkProprietaryString(final String proprietary) {
        if (DeviceOs.Android.compareOsString(proprietary)) return DeviceOs.Android;

        if (DeviceOs.Bada.compareOsString(proprietary)) return DeviceOs.Bada;

        if (DeviceOs.Brew.compareOsString(proprietary)) return DeviceOs.Brew;

        if (DeviceOs.Hiptop.compareOsString(proprietary)) return DeviceOs.Hiptop;

        if (DeviceOs.L4.compareOsString(proprietary)) return DeviceOs.L4;

        if (DeviceOs.Motorola.compareOsString(proprietary)) return DeviceOs.Motorola;

        if (DeviceOs.NokiaOS.compareOsString(proprietary)) return DeviceOs.NokiaOS;

        if (DeviceOs.Nucleus.compareOsString(proprietary)) return DeviceOs.Nucleus;

        if (DeviceOs.PalmOS.compareOsString(proprietary)) return DeviceOs.PalmOS;

        if (DeviceOs.Rex.compareOsString(proprietary)) return DeviceOs.Rex;

        if (DeviceOs.Symbian.compareOsString(proprietary)) return DeviceOs.Symbian;

        if (DeviceOs.Vrtx.compareOsString(proprietary)) return DeviceOs.Vrtx;

        return DeviceOs.Unknown;
    }

    private DeviceOs checkUserAgentDirectly(final String userAgent) {
        String agentToLower = userAgent.toLowerCase();

        if (agentToLower.contains(DeviceOs.Android.toString().toLowerCase())) return DeviceOs.Android;

        if (agentToLower.contains("iphone")) return DeviceOs.Ios;

        if (agentToLower.contains(DeviceOs.Windows.toString().toLowerCase())) return DeviceOs.Windows;

        if (agentToLower.contains("blackberry")) return DeviceOs.Rim;

        if (agentToLower.contains(DeviceOs.Symbian.toString().toLowerCase())) return DeviceOs.Symbian;

        if (agentToLower.contains(DeviceOs.Brew.toString().toLowerCase())) return DeviceOs.Brew;

        if (agentToLower.contains("nucleus")) return DeviceOs.Nucleus;

        if (agentToLower.contains(DeviceOs.Linux.toString().toLowerCase())) return DeviceOs.Linux;

        if (agentToLower.contains(DeviceOs.Bada.toString().toLowerCase())) return DeviceOs.Bada;

        return DeviceOs.Unknown;
    }

    protected static String getOsVersion(String osName, String rawVersion) {
    	String osVersion = rawVersion;
    	if (DeviceOs.Rim.toString().equalsIgnoreCase(osName)) {
    		if (StringUtils.isNotEmpty(rawVersion)) {
	    		// Crunch RIM version numbers, convert aa.bb.cc.dd to aa.bb.cc
	    		List<String> segments = ImmutableList.copyOf(SPLIT_ON_DOT.split(rawVersion));
	    		if (segments.size() > 3) {
	    			osVersion=JOIN_ON_DOT.join(segments.get(0), segments.get(1), segments.get(2));
	    		}
    		}
    	}
    	return osVersion;
    }

    private enum Attributes {
        model,
        mobileDevice,
        displayWidth,
        displayHeight,
        isBrowser,
        vendor,
        osVersion,
        isRobot,
        osProprietary,
        osRim,
        osAndroid,
        osBada,
        osWindows,
        osOsx,
        osSymbian,
        osLinux,
        isTablet,
        javaScriptSupported("js.supportBasicJavaScript");

        private final String string;
        Attributes() {
            this.string = name();
        }

        Attributes(String string) {
            this.string = string;
        }

        public String toString() {
            return string;
        }

        public final String get(final Map<String, Object> attributes) {
            return (String) attributes.get(this.toString());
        }

        public final String get(final Properties attributes) {
            if(attributes.containsKey(this.toString()))
                return attributes.get(this.toString()).asString();
            else return null;
        }

        public final boolean hasAttribute(final Map<String, Object> attributes) {
            String attr = get(attributes);
            if ("1".equals(attr)) return true;
            return false;
        }

        public final boolean hasAttribute(final Properties attributes) {
            if(attributes.containsKey(this.toString())){
                String attr = get(attributes);
                if ("1".equals(attr)) return true;
            }
            return false;
        }


    }

}
