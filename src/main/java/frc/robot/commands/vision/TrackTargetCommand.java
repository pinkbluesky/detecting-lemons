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
        // use camera stream from vision subsystem to find lemon coordinates
        visionSubsystem.getCvSink().grabFrame(image);

        // check that image is not null; sometimes the camera stream takes time to load
        if (!image.empty()) {

            Mat blurImg = new Mat();
            Imgproc.GaussianBlur(image, blurImg, new Size(3, 3), 0);

            Mat hsvImg = new Mat();
            Imgproc.cvtColor(blurImg, hsvImg, Imgproc.COLOR_BGR2HSV);

            Mat colorThreshImg = new Mat();
            Core.inRange(hsvImg, hsvTab.getLowScalar(), hsvTab.getHighScalar(), colorThreshImg);

            Mat colorMaskedImg = new Mat();
            // uses the color thresh image as a mask over the original frame from video
            Core.bitwise_and(blurImg, blurImg, colorMaskedImg, colorThreshImg);

            Mat grayscaleImg = new Mat();
            Imgproc.cvtColor(colorMaskedImg, grayscaleImg, Imgproc.COLOR_BGR2GRAY);

            Mat cannyEdgeImg = new Mat();
            Imgproc.Canny(grayscaleImg, cannyEdgeImg, 50, 200, 3);

            Mat circles = new Mat();
            Imgproc.HoughCircles(cannyEdgeImg, circles, Imgproc.HOUGH_GRADIENT, 1.0, (double) cannyEdgeImg.rows() / 10,
                    100.0, 30.0, 70, 150); // change the last two parameters
            // (min_radius & max_radius) to detect larger circles

            for (int x = 0; x < circles.cols(); x++) {
                double[] c = circles.get(0, x);
                Point center = new Point(Math.round(c[0]), Math.round(c[1]));
                // circle center
                Imgproc.circle(image, center, 1, new Scalar(255, 0, 255), 3, 8, 0);
                // circle outline
                int radius = (int) Math.round(c[2]);
                Imgproc.circle(image, center, radius, new Scalar(255, 0, 255), 3, 8, 0);
            }

            /*
             * Mat temp = new Mat();
             * 
             * // Imgproc.cvtColor(image, imageHSV, Imgproc.COLOR_BGR2HSV); Mat colorThresh
             * = new Mat(); // filter yellow color // Core.inRange(imageHSV,
             * hsvTab.getLowScalar(), hsvTab.getHighScalar(), // colorThresh);
             * 
             * // Imgproc.cvtColor(colorThresh, temp, Imgproc.COLOR_HSV2BGR);
             * Imgproc.cvtColor(image, colorThresh, Imgproc.COLOR_BGR2GRAY);
             * 
             * Imgproc.medianBlur(colorThresh, colorThresh, 5); Mat circles = new Mat();
             * Imgproc.HoughCircles(colorThresh, circles, Imgproc.HOUGH_GRADIENT, 1.0,
             * (double) colorThresh.rows() / 3, 100.0, 30.0, 70, 150); // change the last
             * two parameters // (min_radius & max_radius) to detect larger circles
             * 
             * for (int x = 0; x < circles.cols(); x++) { double[] c = circles.get(0, x);
             * Point center = new Point(Math.round(c[0]), Math.round(c[1])); // circle
             * center Imgproc.circle(colorThresh, center, 1, new Scalar(255, 0, 255), 3, 8,
             * 0); // circle outline int radius = (int) Math.round(c[2]);
             * Imgproc.circle(image, center, radius, new Scalar(255, 0, 255), 3, 8, 0); }
             */
            /*
             * List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
             * 
             * Imgproc.findContours(colorThresh, contours, new Mat(), Imgproc.RETR_TREE,
             * Imgproc.CHAIN_APPROX_NONE);
             * 
             * Mat contourImg = new Mat(colorThresh.size(), colorThresh.type());
             * 
             * for (int i = 0; i < contours.size(); i++) { MatOfPoint c = contours.get(i);
             * 
             * MatOfPoint2f c2f = new MatOfPoint2f(); c.convertTo(c2f, CvType.CV_32S);
             * 
             * MatOfPoint2f approx = new MatOfPoint2f();
             * 
             * Imgproc.approxPolyDP(c2f, approx, 0.01 * Imgproc.arcLength(c2f, true), true);
             * 
             * if (approx.total() > 10) { Imgproc.drawContours(contourImg, contours, i, new
             * Scalar(255, 0, 0), 5); } }
             * 
             * // Imgproc.drawContours(contourImg, contours, -1, new Scalar(255, 0, 0), 5);
             */
            visionSubsystem.getOutputStream().putFrame(image);
            visionSubsystem.getOutputStream2().putFrame(cannyEdgeImg);
            visionSubsystem.getOutputStream3().putFrame(grayscaleImg);
        }
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    @Override
    /**
     * 
     */
    public void end(boolean interrupted) {

        // save HSV values from Shuffleboard slider into config file
        hsvTab.save();

    }
}
