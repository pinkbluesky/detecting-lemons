package frc.calibration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
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

public class CameraCalibration {

    public static final String CALIB_CONFIG_FILE_PATH = "src/main/java/frc/calibration/calibration_config.json";
    public static final String CAMERA_MATRIX_FILE_PATH = "src/main/java/frc/calibration/camera_matrices.json";
    public static final String DIST_COEFFS_FILE_PATH = "src/main/java/frc/calibration/dist_coeffs.json";

    private static List<Mat> calibratedMats = new ArrayList<Mat>();
    private static Size boardSize;
    private static int nrFrames;
    private static JSONArray calibImages;
    private static String patternType;

    private static JLabel currentImage;
    private static JFrame frame;

    public static void run() {

        // if already calibrated, exit
        if ((new File(CAMERA_MATRIX_FILE_PATH)).length() != 0 && (new File(DIST_COEFFS_FILE_PATH)).length() != 0) {
            return;
        }

        readConfig();
        calibrate();
        paintImages(); // comment out if you don't want to see the calibrated images

    }

    private static void readConfig() {
        try {

            FileReader reader = new FileReader(new File(CALIB_CONFIG_FILE_PATH));
            JSONParser parser = new JSONParser();
            JSONObject calibConfig = (JSONObject) parser.parse(reader);

            boardSize = new Size(Integer.parseInt(calibConfig.get("board_width").toString()),
                    Integer.parseInt(calibConfig.get("board_height").toString()));

            // iterate through images
            nrFrames = Integer.parseInt(calibConfig.get("nr_frames").toString());

            calibImages = (JSONArray) calibConfig.get("calib_images");

            patternType = calibConfig.get("calib_pattern").toString();
        } catch (IOException e) {
            System.out.println("Error while reading calibration config file.");
            e.printStackTrace();
        } catch (ParseException e) {
            System.out.println("Error while parsing calibration config file.");
            e.printStackTrace();
        }
    }

    private static void calibrate() {
        Size imageSize = new Size();

        // create a list to store all n images' corners
        List<Mat> imagePoints = new ArrayList<Mat>();

        Mat mat = new Mat();
        for (int i = 0; i < nrFrames; i++) {

            try {
                // find the chessboard within each image
                BufferedImage image = ImageIO.read(new File(calibImages.get(i).toString()));

                // store image size for later use
                if (i == 0) {
                    imageSize = new Size(image.getWidth(), image.getHeight());
                }

                MatOfPoint2f imageCorners = new MatOfPoint2f();
                mat = fromBufferedImage(image);

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

                if (found) {
                    Calib3d.drawChessboardCorners(mat, boardSize, imageCorners, found);

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

        // create a list of Mats corresponding to each image's chessboard pattern
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

        StoreMat.storeMat(CAMERA_MATRIX_FILE_PATH, cameraMatrix);
        StoreMat.storeMat(DIST_COEFFS_FILE_PATH, distCoeffs);
    }

    private static void paintImages() {
        frame = new JFrame("Calibration Images");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(640, 480);
        frame.setVisible(true);

        JPanel panel = new JPanel();
        frame.setContentPane(panel);

        JSlider imageSlider = new JSlider(JSlider.HORIZONTAL, 1, calibratedMats.size(), 1);
        imageSlider.setMajorTickSpacing(1);
        imageSlider.setPaintTicks(true);
        imageSlider.setPaintLabels(true);
        imageSlider.addChangeListener(new ImageSliderListener());

        panel.add(imageSlider);

        currentImage = new JLabel(new ImageIcon(fromMat(calibratedMats.get(0))));
        panel.add(currentImage);
        frame.pack();
        frame.repaint();
    }

    static class ImageSliderListener implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {
            int currentI = ((JSlider) e.getSource()).getValue() - 1;
            currentImage.setIcon(new ImageIcon(fromMat(calibratedMats.get(currentI))));
            frame.pack();
            frame.repaint();
        }

    }

    public static Mat fromBufferedImage(BufferedImage img) {
        byte[] pixels = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        Mat mat = new Mat(img.getHeight(), img.getWidth(), CvType.CV_8UC3);
        mat.put(0, 0, pixels);
        return mat;
    }

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
