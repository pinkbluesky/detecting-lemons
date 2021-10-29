package frc.calibration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.opencv.calib3d.*;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point3;
import org.opencv.core.CvType;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;

/**
 * Calibrates the camera using given calibration images and generates a camera
 * matrix and distortion coefficients.
 */
public class CameraCalibration {

    // stores config values for the calibration images (ie. width and height of
    // chessboard)
    public static final String CALIB_CONFIG_FILE_PATH = "src/main/java/frc/calibration/cc_evelyn/calibration_config.json";
    // stores calculated camera matrix
    public static final String CAMERA_MATRIX_FILE_PATH = "src/main/java/frc/calibration/camera_matrices.json";
    // stores calculated distortion coefficients
    public static final String DIST_COEFFS_FILE_PATH = "src/main/java/frc/calibration/dist_coeffs.json";

    // stores the resulting calibrated mats
    private static List<Mat> calibratedMats = new ArrayList<Mat>();

    // these values are read from the config file and describe the calibration image
    private static Size boardSize;
    private static int nrFrames;
    private static JSONArray calibImages;
    private static String patternType;

    // used for graphics
    private static JLabel currentImage;
    private static JFrame frame;

    /**
     * Runs the camera calibration.
     */
    public static void run() {
        // if already calibrated, exit
        if ((new File(CAMERA_MATRIX_FILE_PATH)).length() != 0 && (new File(DIST_COEFFS_FILE_PATH)).length() != 0) {
            return;
        }

        readConfig();
        calibrate();
        paintImages(); // comment out if you don't want to see the calibrated images

    }

    /**
     * Reads the config file and instantiates the corresponding variables.
     */
    private static void readConfig() {
        try {
            // read the config file
            FileReader reader = new FileReader(new File(CALIB_CONFIG_FILE_PATH));
            JSONParser parser = new JSONParser();
            JSONObject calibConfig = (JSONObject) parser.parse(reader);

            // initialize the chessboard size
            boardSize = new Size(Integer.parseInt(calibConfig.get("board_width").toString()),
                    Integer.parseInt(calibConfig.get("board_height").toString()));

            // initialize number of frames
            nrFrames = Integer.parseInt(calibConfig.get("nr_frames").toString());

            // get the list of calibration images
            calibImages = (JSONArray) calibConfig.get("calib_images");

            // get the calibration pattern type (ie. chessboard)
            patternType = calibConfig.get("calib_pattern").toString();
        } catch (IOException e) {
            System.out.println("Error while reading calibration config file.");
            e.printStackTrace();
        } catch (ParseException e) {
            System.out.println("Error while parsing calibration config file.");
            e.printStackTrace();
        }
    }

    /**
     * Uses calibration images to calibrate and generate matrices.
     */
    private static void calibrate() {
        Size imageSize = new Size();

        // stores the corners of the images
        List<Mat> imagePoints = new ArrayList<Mat>();

        Mat mat = new Mat();
        // for each image
        for (int i = 0; i < nrFrames; i++) {

            try {
                // get the image
                BufferedImage image = ImageIO.read(new File(calibImages.get(i).toString()));

                // store image size for later use
                if (i == 0) {
                    imageSize = new Size(image.getWidth(), image.getHeight());
                }

                MatOfPoint2f imageCorners = new MatOfPoint2f();
                mat = fromBufferedImage(image);

                // find the calibration pattern
                boolean found;
                switch (patternType) {
                case "CHESSBOARD":
                    found = Calib3d.findChessboardCorners(mat, boardSize, imageCorners);
                    break;
                default:
                    found = false;
                    System.out.println("Other calibration pattern types not supported.");
                    break;
                }

                // if pattern is found, draw the pattern on the image
                if (found) {

                    switch (patternType) {
                    case "CHESSBOARD":
                        Calib3d.drawChessboardCorners(mat, boardSize, imageCorners, found);
                        break;
                    default:
                        System.out.println("Other calibration pattern types not supported.");
                        break;
                    }
                    imagePoints.add(imageCorners);

                } else {
                    System.out.println("Failed: could not find chessboard corners.");
                    System.out.println(i);
                }

                calibratedMats.add(mat);
            } catch (IOException e) {
                System.out.println("Error while reading calibration image.");
                e.printStackTrace();
            }

        }

        // creates lists of Mats that represent each chessboard (used by the
        // calibratecamera function call)
        List<Mat> objectPoints = new ArrayList<Mat>();
        Mat obj = new Mat();
        for (int i = 0; i < boardSize.area(); i++) {
            obj.push_back(new MatOfPoint3f(new Point3(i / boardSize.width, i % boardSize.height, 0.0f)));
        }
        for (int i = 0; i < imagePoints.size(); i++) {
            objectPoints.add(obj);
        }

        // calibrate! obtain matrices
        Mat cameraMatrix = new Mat();
        Mat distCoeffs = new Mat();
        cameraMatrix.put(0, 0, 1);
        cameraMatrix.put(1, 1, 1);
        List<Mat> rvecs = new ArrayList<>();
        List<Mat> tvecs = new ArrayList<>();

        Calib3d.calibrateCamera(objectPoints, imagePoints, imageSize, cameraMatrix, distCoeffs, rvecs, tvecs);

        // write to the output json files
        StoreMat.storeMat(CAMERA_MATRIX_FILE_PATH, cameraMatrix);
        StoreMat.storeMat(DIST_COEFFS_FILE_PATH, distCoeffs);
    }

    /**
     * Displays each calibrated image.
     */
    private static void paintImages() {
        // creates a new jframe
        frame = new JFrame("Calibration Images");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(640, 480);
        frame.setVisible(true);

        JPanel panel = new JPanel();
        frame.setContentPane(panel);

        // creates a new slider for controlling the calibrated image gallery
        JSlider imageSlider = new JSlider(JSlider.HORIZONTAL, 1, calibratedMats.size(), 1);
        imageSlider.setMajorTickSpacing(1);
        imageSlider.setPaintTicks(true);
        imageSlider.setPaintLabels(true);
        imageSlider.addChangeListener(new ImageSliderListener());

        panel.add(imageSlider);

        // draw the first image
        currentImage = new JLabel(new ImageIcon(fromMat(calibratedMats.get(0))));
        panel.add(currentImage);
        frame.pack();
        frame.repaint();
    }

    /**
     * SliderListener that allows the user to adjust the slider and view the
     * calibrated image gallery.
     */
    static class ImageSliderListener implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {
            // draws the image based on slider value
            int currentI = ((JSlider) e.getSource()).getValue() - 1;
            currentImage.setIcon(new ImageIcon(fromMat(calibratedMats.get(currentI))));
            frame.pack();
            frame.repaint();
        }

    }

    /**
     * Utility function that converts a BufferedImage to Mat.
     * 
     * @param img the buffered image
     * @return the converted mat
     */
    public static Mat fromBufferedImage(BufferedImage img) {
        byte[] pixels = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        Mat mat = new Mat(img.getHeight(), img.getWidth(), CvType.CV_8UC3);
        mat.put(0, 0, pixels);
        return mat;
    }

    /**
     * Utility function that converts a Mat to a BufferedImage.
     * 
     * @param mat the mat
     * @return the converted buffered image
     */
    public static BufferedImage fromMat(Mat mat) {

        // Encoding the image
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".jpg", mat, matOfByte);
        // Storing the encoded Mat in a byte array
        byte[] byteArray = matOfByte.toArray();

        // Preparing the Buffered Image
        InputStream in = new ByteArrayInputStream(byteArray);
        BufferedImage bufImage = null;

        try {
            bufImage = ImageIO.read(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bufImage;
    }

}
