package frc.robot.commands.vision;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.opencv.core.Scalar;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInWidgets;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;

// make sure to refresh the shuffleboard window to view the changes!!
public class HSVConfigTab {

    private NetworkTableEntry hLow;
    private NetworkTableEntry sLow;
    private NetworkTableEntry vLow;
    private NetworkTableEntry hHigh;
    private NetworkTableEntry sHigh;
    private NetworkTableEntry vHigh;

    private String filepath;
    private String tabName;

    // json object representing the config file (used in both reading and writing)
    private JSONObject jsonObj;

    /**
     * Creates a Shuffleboard config tab with sliders for low and high HSV values.
     * JSON structure: { h_low: {val: #, min: #, max: #}, s_low, v_low, h_high,
     * s_high, v_high}
     * 
     * @param filepath path to json config file
     * @param tabName  name of the shuffleboard tab
     */
    public HSVConfigTab(String filepath, String tabName) {
        this.tabName = tabName;
        this.filepath = filepath;
    }

    /**
     * Initializes the config tab with the six sliders.
     */
    public void init() {

        JSONParser parser = new JSONParser();

        try {
            // read the HSV config file
            FileReader reader = new FileReader(new File(filepath));
            jsonObj = (JSONObject) parser.parse(reader);

            // create sliders for each of the six values
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
        // retrieve json object associated with key
        JSONObject obj = (JSONObject) jsonObj.get(key);

        // get the current, min, and max value
        double val = ((Long) obj.get("val")).doubleValue();
        double min = ((Long) obj.get("min")).doubleValue();
        double max = ((Long) obj.get("max")).doubleValue();

        // add the slider to the shuffleboard tab
        return Shuffleboard.getTab(tabName).add(key, val).withWidget(BuiltInWidgets.kNumberSlider)
                .withProperties(Map.of("min", min, "max", max)).getEntry();
    }

    /**
     * Returns a Scalar object containing the low HSV values.
     * 
     * @return low HSV values
     */
    public Scalar getLowScalar() {
        return new Scalar(ntToDouble(hLow), ntToDouble(sLow), ntToDouble(vLow));
    }

    /**
     * Returns a Scalar object containing the high HSV values.
     * 
     * @return high HSV values
     */
    public Scalar getHighScalar() {
        return new Scalar(ntToDouble(hHigh), ntToDouble(sHigh), ntToDouble(vHigh));
    }

    private static double ntToDouble(NetworkTableEntry entry) {
        return entry.getValue().getDouble();
    }

}
