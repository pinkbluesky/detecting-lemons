package frc.robot.commands.vision;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.calibration.CameraCalibration;
import frc.calibration.StoreMat;
import frc.robot.subsystems.VisionSubsystem;

public class TrackTargetCommand extends CommandBase {

    private final VisionSubsystem visionSubsystem;

    private Mat image;
    private Mat imageHSV;

    public static final String HSV_CONFIG_FILE_PATH = "src/main/java/frc/robot/commands/vision/lemon_config.json";

    private HSVConfigTab hsvTab;

    public TrackTargetCommand(VisionSubsystem visionSubsystem) {
        this.visionSubsystem = visionSubsystem;
        addRequirements(visionSubsystem);

        image = new Mat();
        imageHSV = new Mat();

        hsvTab = new HSVConfigTab(HSV_CONFIG_FILE_PATH, "Lemon Detection");

    }

    @Override
    public void initialize() {
        hsvTab.init();
    }

    @Override
    public void execute() {
        // grab image from camera stream
        visionSubsystem.getCvSink().grabFrame(image);

        // check that image is not null; sometimes the camera stream takes time to load
        if (!image.empty()) {

            // undistort the image
            Mat undistImg = new Mat();
            // obtain camera matrix and dist coefficients
            Mat cameraMatrix = StoreMat.readMat(CameraCalibration.CAMERA_MATRIX_FILE_PATH);
            Mat distCoeffs = StoreMat.readMat(CameraCalibration.DIST_COEFFS_FILE_PATH);
            Imgproc.undistort(image, undistImg, cameraMatrix, distCoeffs);

            // colored object detection using hough circles
            // (https://docs.opencv.org/4.5.3/da/d22/tutorial_py_canny.html) and
            // (https://stackoverflow.com/questions/38827505/detecting-colored-circle-and-its-center-using-opencv)

            // first: gaussian blur
            Mat blurImg = new Mat();
            Imgproc.GaussianBlur(undistImg, blurImg, new Size(3, 3), 0);

            // second: convert from RGB to HSV and filter for yellow
            Mat hsvImg = new Mat();
            Imgproc.cvtColor(blurImg, hsvImg, Imgproc.COLOR_BGR2HSV);

            Mat colorThreshImg = new Mat();
            Core.inRange(hsvImg, hsvTab.getLowScalar(), hsvTab.getHighScalar(), colorThreshImg);

            // third: color mask
            Mat colorMaskedImg = new Mat();
            // uses the color thresh image as a mask over the original frame from video
            Core.bitwise_and(blurImg, blurImg, colorMaskedImg, colorThreshImg);

            // fourth: convert to grayscale (hough circles is meant to be used on grayscale
            // images)
            Mat grayscaleImg = new Mat();
            Imgproc.cvtColor(colorMaskedImg, grayscaleImg, Imgproc.COLOR_BGR2GRAY);

            // fifth: edge detection
            Mat cannyEdgeImg = new Mat();
            Imgproc.Canny(grayscaleImg, cannyEdgeImg, 5, 100, 3);

            // sixth: find circles and draw them on the image
            Mat circles = new Mat();
            Imgproc.HoughCircles(cannyEdgeImg, circles, Imgproc.HOUGH_GRADIENT, 1.0, (double) cannyEdgeImg.rows() / 10,
                    100.0, 30.0, 70, 150);

            for (int x = 0; x < circles.cols(); x++) {
                double[] c = circles.get(0, x);
                Point center = new Point(Math.round(c[0]), Math.round(c[1]));
                // draw circle center
                Imgproc.circle(undistImg, center, 1, new Scalar(255, 0, 255), 3, 8, 0);
                // draw circle outline
                int radius = (int) Math.round(c[2]);
                Imgproc.circle(undistImg, center, radius, new Scalar(255, 0, 255), 3, 8, 0);
            }

            // put images on output stream
            visionSubsystem.getOutputStream("Original Stream").putFrame(image);
            visionSubsystem.getOutputStream("Canny Edge Stream").putFrame(cannyEdgeImg);
            visionSubsystem.getOutputStream("Undistorted Stream").putFrame(undistImg);
        }
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    /**
     * 
     */
    @Override
    public void end(boolean interrupted) {

        // save HSV values from Shuffleboard slider into config file
        hsvTab.save();

    }
}
