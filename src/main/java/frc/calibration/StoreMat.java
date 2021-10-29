package frc.calibration;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import org.apache.commons.lang3.SerializationUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Base64;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Utility class for storing or reading mat objects in/from json files.
 * https://stackoverflow.com/questions/27065062/opencv-mat-object-serialization-in-java
 * 
 */
public class StoreMat {

    /**
     * Writes mat to json file.
     * 
     * @param filepath the output file
     * @param mat      the mat object to store
     */
    public static void storeMat(String filepath, Mat mat) {
        FileWriter file;
        try {
            // writes to the file
            file = new FileWriter(filepath);
            file.write(matToJSON(mat));
            file.close();
        } catch (IOException e) {
            System.out.println("Failed to write json file.");
            e.printStackTrace();
        }
    }

    /**
     * Converts a mat object to a json string. Final JSON structure: { rows: #,
     * cols: #, type: #, data: "___" }
     * 
     * @param mat the mat object
     * @return the json string
     */
    private static String matToJSON(Mat mat) {

        JSONObject obj = new JSONObject();

        if (mat.isContinuous()) {
            int cols = mat.cols();
            int rows = mat.rows();
            int elemSize = (int) mat.elemSize();
            int type = mat.type();

            // store rows, columns, type in json object
            obj.put("rows", rows);
            obj.put("cols", cols);
            obj.put("type", type);

            String dataString;

            // encode underlying mat array to base 64, which becomes our datastring
            // array type depends on mat type
            if (type == CvType.CV_32S || type == CvType.CV_32SC2 || type == CvType.CV_32SC3 || type == CvType.CV_16S) {
                int[] data = new int[cols * rows * elemSize];
                mat.get(0, 0, data);
                dataString = new String(Base64.getEncoder().encode(SerializationUtils.serialize(data)));
            } else if (type == CvType.CV_32F || type == CvType.CV_32FC2) {
                float[] data = new float[cols * rows * elemSize];
                mat.get(0, 0, data);
                dataString = new String(Base64.getEncoder().encode(SerializationUtils.serialize(data)));
            } else if (type == CvType.CV_64F || type == CvType.CV_64FC2) {
                double[] data = new double[cols * rows * elemSize];
                mat.get(0, 0, data);
                dataString = new String(Base64.getEncoder().encode(SerializationUtils.serialize(data)));
            } else if (type == CvType.CV_8U) {
                byte[] data = new byte[cols * rows * elemSize];
                mat.get(0, 0, data);
                dataString = new String(Base64.getEncoder().encode(SerializationUtils.serialize(data)));
            } else {

                throw new UnsupportedOperationException("Serializing Mat failed: unknown type.");
            }

            // store final data string in json object
            obj.put("data", dataString);

            // return the json object
            return obj.toJSONString();
        } else {
            System.out.println("Mat not continuous.");
        }

        // return empty json object if cannot convert
        return "{}";
    }

    /**
     * Reads a mat from a given json file.
     * 
     * @param filepath the json file
     * @return the mat object
     */
    public static Mat readMat(String filepath) {
        JSONParser parser = new JSONParser();

        FileReader reader;
        try {
            // read the file
            reader = new FileReader(filepath);

            // parse the json
            JSONObject jsonObject = (JSONObject) parser.parse(reader);

            // get the rows, columns, and type from the parsed json
            int rows = ((Long) jsonObject.get("rows")).intValue();
            int cols = ((Long) jsonObject.get("cols")).intValue();
            int type = ((Long) jsonObject.get("type")).intValue();

            // get the serialized mat from the parsed json
            String dataString = jsonObject.get("data").toString();

            // returns the created mat object based on these values
            return matFromJson(dataString, rows, cols, type);

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Converts JSON parameters to a mat object.
     * 
     * @param dataString the serialized mat string
     * @param rows       the number of rows
     * @param cols       the number of columns
     * @param type       the type of mat
     * @return the mat object
     */
    private static Mat matFromJson(String dataString, int rows, int cols, int type) {

        // create a new mat
        Mat mat = new Mat(rows, cols, type);

        // deserialize the data string and put into the empty mat object
        if (type == CvType.CV_32S || type == CvType.CV_32SC2 || type == CvType.CV_32SC3 || type == CvType.CV_16S) {
            int[] data = SerializationUtils.deserialize(Base64.getDecoder().decode(dataString.getBytes()));
            mat.put(0, 0, data);
        } else if (type == CvType.CV_32F || type == CvType.CV_32FC2) {
            float[] data = SerializationUtils.deserialize(Base64.getDecoder().decode(dataString.getBytes()));
            mat.put(0, 0, data);
        } else if (type == CvType.CV_64F || type == CvType.CV_64FC2) {
            double[] data = SerializationUtils.deserialize(Base64.getDecoder().decode(dataString.getBytes()));
            mat.put(0, 0, data);
        } else if (type == CvType.CV_8U) {
            byte[] data = Base64.getDecoder().decode(dataString.getBytes());
            mat.put(0, 0, data);
        } else {
            throw new UnsupportedOperationException("Deserializing Mat failed: unknown type.");
        }

        return mat;

    }

}
