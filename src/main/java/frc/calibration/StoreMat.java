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

// utility class for storing or reading mat objects in json files
// used to store camera calibration matrices (camera matrix and distortion coefficients)
public class StoreMat {

    // writes serialized mat to json file
    public static void storeMat(String filepath, Mat mat) {
        FileWriter file;
        try {
            file = new FileWriter(filepath);
            file.write(matToJSON(mat));
            file.close();
        } catch (IOException e) {
            System.out.println("Failed to write json file.");
            e.printStackTrace();
        }
    }

    // converts a Mat object to a JSON string
    private static String matToJSON(Mat mat) {
        JSONObject obj = new JSONObject();

        if (mat.isContinuous()) {
            int cols = mat.cols();
            int rows = mat.rows();
            int elemSize = (int) mat.elemSize();
            int type = mat.type();

            obj.put("rows", rows);
            obj.put("cols", cols);
            obj.put("type", type);

            // We cannot set binary data to a json object, so:
            // Encoding data byte array to Base64.
            String dataString;

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
            obj.put("data", dataString);

            return obj.toJSONString();
        } else {
            System.out.println("Mat not continuous.");
        }
        return "{}";
    }

    // reads mat from json file
    public static Mat readMat(String filepath) {
        JSONParser parser = new JSONParser();

        FileReader reader;
        try {
            reader = new FileReader(filepath);

            JSONObject jsonObject = (JSONObject) parser.parse(reader);

            int rows = ((Integer) jsonObject.get("rows")).intValue();
            int cols = ((Integer) jsonObject.get("cols")).intValue();
            int type = ((Integer) jsonObject.get("type")).intValue();

            String dataString = jsonObject.get("data").toString();

            return matFromJson(dataString, rows, cols, type);

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return null;
    }

    // converts json string to mat object
    private static Mat matFromJson(String dataString, int rows, int cols, int type) {

        Mat mat = new Mat(rows, cols, type);

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
