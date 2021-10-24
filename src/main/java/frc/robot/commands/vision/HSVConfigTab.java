package frc.robot.commands.vision;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.opencv.core.Scalar;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInWidgets;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;

public class HSVConfigTab {

    private NetworkTableEntry hLow;
    private NetworkTableEntry sLow;
    private NetworkTableEntry vLow;
    private NetworkTableEntry hHigh;
    private NetworkTableEntry sHigh;
    private NetworkTableEntry vHigh;

    private String filepath;
    private ShuffleboardTab tab;
    // json object representing the config file (used in both reading and writing)
    private JSONObject jsonObj;

    /**
     * Creates a Shuffleboard config tab with sliders for low and high HSV values.
     * The JSON config file must have keys (h_low, s_low, v_low, h_high, s_high,
     * v_high) which each correspond to a JSON object that has keys val, min, and
     * max.
     * 
     * @param filepath path to json config file
     * @param tabName  name of the config tab
     */
    public HSVConfigTab(String filepath, String tabName) {
        tab = Shuffleboard.getTab(tabName);
        this.filepath = filepath;
    }

    /**
     * Initializes the config tab with the six sliders.
     */
    public void init() {

        JSONParser parser = new JSONParser();

        try {
            FileReader reader = new FileReader(new File(filepath));
            jsonObj = (JSONObject) parser.parse(reader);

            hLow = createSliderEntry("h_low");
            sLow = createSliderEntry("s_low");
            vLow = createSliderEntry("v_low");
            hHigh = createSliderEntry("h_high");
            sHigh = createSliderEntry("s_high");
            vHigh = createSliderEntry("v_high");

        } catch (IOException e) {
            System.out.println("IO error while reading HSV config file.");
            e.printStackTrace();
        } catch (ParseException e) {
            System.out.println("Could not parse HSV config file.");
            e.printStackTrace();
        }
    }

    /**
     * Creates and adds a slider widget to the ShuffleBoard, and returns the
     * corresponding NetworkTableEntry. Sets name, min, max, and initial value of
     * slider based on key.
     */
    private NetworkTableEntry createSliderEntry(String key) {
        // retrieve nested json object associated with key
        JSONObject obj = (JSONObject) jsonObj.get(key);

        String name = obj.get("name").toString();
        double val = ((Double) obj.get("val")).doubleValue();
        double min = ((Double) obj.get("min")).doubleValue();
        double max = ((Double) obj.get("max")).doubleValue();

        return tab.add(name, val).withWidget(BuiltInWidgets.kNumberSlider)
                .withProperties(Map.of("min", min, "max", max)).getEntry();
    }

    /**
     * Returns a Scalar object representing the low HSV values.
     * 
     * @return low HSV values
     */
    public Scalar getLowScalar() {
        return new Scalar(ntToDouble(hLow), ntToDouble(sLow), ntToDouble(vLow));
    }

    /**
     * Returns a Scalar object representing the high HSV values.
     * 
     * @return high HSV values
     */
    public Scalar getHighScalar() {
        return new Scalar(ntToDouble(hHigh), ntToDouble(sHigh), ntToDouble(vHigh));
    }

    private static double ntToDouble(NetworkTableEntry entry) {
        return entry.getValue().getDouble();
    }

    /**
     * Saves the current network table entries into the config file. Useful for
     * testing HSV values through the GUI and saving them for future runs.
     */
    public void save() {
        // creating this parameterized Hashmap avoids type safety warnings
        HashMap<String, Double> hsvObj = new HashMap<String, Double>();

        hsvObj.put("h_low", ntToDouble(hLow));
        hsvObj.put("s_low", ntToDouble(sLow));
        hsvObj.put("v_low", ntToDouble(vLow));
        hsvObj.put("h_high", ntToDouble(hHigh));
        hsvObj.put("s_high", ntToDouble(sHigh));
        hsvObj.put("v_high", ntToDouble(vHigh));

        jsonObj = new JSONObject(hsvObj);

        FileWriter file;
        try {
            file = new FileWriter(filepath);
            file.write(jsonObj.toJSONString());
            file.close();
        } catch (IOException e) {
            System.out.println("IO error while writing to HSV config file.");
            e.printStackTrace();
        }

    }

}
