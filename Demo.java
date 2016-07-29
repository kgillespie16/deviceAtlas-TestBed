import deviceAtlas.DeviceAtlas;
import deviceAtlas.V2_1.mobi.mtld.da.Properties;
import java.util.HashMap;
import java.util.Map;


public class Demo {
    public static HashMap <String, Map> findDiff(DeviceAtlas atlas,String userAgent){
        Map<String, Object> attributesV1 = atlas.getV1DeviceAttributes(userAgent); // Gets Attributes from deviceAtlas1_7 api
        Properties attributesV2 = atlas.getV2DeviceAttributes(userAgent); // Gets Attributes from deviceAtlas2.1 api
        HashMap <String, Map> diff = atlas.getDif(userAgent, attributesV1, attributesV2); // Returns difference from the two attributes
        return diff;
    }


    public static void main(String [] args){
        // Location of json files for device atlas. They are located in the deviceAtlas folder.
        String atlasV1Filename = "./deviceAtlas/deviceAtlas1_7.json";
        String atlasV2Filename = "./deviceAtlas/deviceAtlas2_1.json";

        // Sample userAgent
        String userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 9_3_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Mobile/13E238";

        // Creates DeviceAtlas object
        DeviceAtlas atlas = new DeviceAtlas(atlasV1Filename, atlasV2Filename);
        HashMap <String, Map> diff = findDiff(atlas,userAgent);

        // print differences in readable format, was used for tessting
        atlas.printAttributes(diff);

    }
}
