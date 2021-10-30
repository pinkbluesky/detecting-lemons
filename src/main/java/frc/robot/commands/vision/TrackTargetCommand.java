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
import org.opencv.imgproc.Moments;

import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.calibration.CameraCalibration;
import frc.calibration.StoreMat;
import frc.robot.subsystems.VisionSubsystem;

public class TrackTargetCommand extends CommandBase {

    private final VisionSubsystem visionSubsystem;

    // stores the image from the camera stream
    private Mat image;

    // filepath to configured HSV range values
    public static final String HSV_CONFIG_FILE_PATH = "src/main/java/frc/robot/commands/vision/lemon_config.json";

    // Shuffleboard tab for adjusting HSV values
    private HSVConfigTab hsvTab;

    // kernel used when reducing image noise (stored here to save memory)
    private static final Mat kernel = new Mat(3, 3, CvType.CV_8U);

    // camera matrix and distortion coefficients
    // stored here so we don't have to get them over and over again for every frame
    private final Mat cameraMatrix;
    private final Mat distCoeffs;

    /**
     * Command that tracks a lemon target and writes coordinates on the screen. Can
     * adjust HSV values through the Shuffleboard GUI for fine-tuning.
     * 
     * @param visionSubsystem the vision subsystem
     */
    public TrackTargetCommand(VisionSubsystem visionSubsystem) {
        this.visionSubsystem = visionSubsystem;
        addRequirements(visionSubsystem);

        image = new Mat();

        // create shuffleboard tab for changing hsv values
        hsvTab = new HSVConfigTab(HSV_CONFIG_FILE_PATH, "Lemon Detection");

        // get camera matrix and dist coefficients
        this.cameraMatrix = StoreMat.readMat(CameraCalibration.CAMERA_MATRIX_FILE_PATH);
        this.distCoeffs = StoreMat.readMat(CameraCalibration.DIST_COEFFS_FILE_PATH);

    }

    @Override
    public void initialize() {
        // initialize the HSV config tab
        hsvTab.init();
    }

    @Override
    public void execute() {
        // grab image from camera stream
        visionSubsystem.getCvSink().grabFrame(image);

        // check that image is not null; sometimes the camera stream takes time to load
        if (!image.empty()) {

            // undistort the image
            // Mat undistImg = new Mat();
            // Imgproc.undistort(image, undistImg, cameraMatrix, distCoeffs);

            // gaussian blur
            Mat blurImg = new Mat();
            Imgproc.GaussianBlur(image, blurImg, new Size(3, 3), 0);

            // convert from RGB to HSV and filter for yellow
            Mat hsvImg = new Mat();
            Imgproc.cvtColor(blurImg, hsvImg, Imgproc.COLOR_BGR2HSV);

            Mat colorThreshImg = new Mat();
            Core.inRange(hsvImg, hsvTab.getLowScalar(), hsvTab.getHighScalar(), colorThreshImg);

            // color mask
            Mat colorMaskedImg = new Mat();
            Core.bitwise_and(blurImg, blurImg, colorMaskedImg, colorThreshImg);

            // dilate then erode to remove tiny blobs (thanks kepler)
            Mat temp = new Mat();
            Imgproc.dilate(colorMaskedImg, temp, kernel, new Point(-1, -1), 1, Core.BORDER_DEFAULT);
            Imgproc.erode(temp, colorMaskedImg, kernel, new Point(-1, -1), 6);
            Imgproc.dilate(colorMaskedImg, temp, kernel, new Point(-1, -1), 1, Core.BORDER_DEFAULT);

            // edge detection
            Mat cannyEdgeImg = new Mat();
            Imgproc.Canny(temp, cannyEdgeImg, 200, 300, 3);

            // find contours
            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            Imgproc.findContours(cannyEdgeImg, contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

            // approximates polygons from contours
            for (MatOfPoint contour : contours) {
                MatOfPoint2f approx = new MatOfPoint2f();
                MatOfPoint2f c = new MatOfPoint2f();
                contour.convertTo(c, CvType.CV_32FC2);
                Imgproc.approxPolyDP(c, approx, Imgproc.arcLength(c, true) * 0.02, true);

                // if polygon has enough vertices and area to be considered a lemon
                if (approx.total() >= 10 && Imgproc.contourArea(contour) > 300) {
                    // calculate center
                    // https://www.pyimagesearch.com/2016/02/01/opencv-center-of-contour/
                    Moments moments = Imgproc.moments(contour);
                    Point center = new Point(moments.get_m10() / moments.get_m00(),
                            moments.get_m01() / moments.get_m00());

                    // draw center point
                    Imgproc.circle(image, center, 5, new Scalar(255, 0, 255), 3, 8, 0);
                    // draw all contours
                    Imgproc.drawContours(image, contours, -1, new Scalar(0, 255, 0));

                    // calculate world coordinates of center point
                    double[] cameraPoints = { center.x, center.y, 1 };
                    Mat cameraXYZ = new Mat(3, 1, cameraMatrix.type());
                    cameraXYZ.put(0, 0, cameraPoints);

                    Mat worldXYZ = new Mat();
                    Core.gemm(cameraMatrix, cameraXYZ, 1, new Mat(), 0, worldXYZ);

                    // write coordinates on output stream
                    String coordText = "(" + worldXYZ.get(0, 0)[0] / 1000 + ", " + worldXYZ.get(1, 0)[0] / 1000 + ", "
                            + worldXYZ.get(2, 0)[0] + ")";
                    Imgproc.putText(image, coordText, center, Core.FONT_HERSHEY_PLAIN, 1, new Scalar(255, 0, 255));

                }
            }

            // put images on output stream
            visionSubsystem.getOutputStream("Original Stream").putFrame(colorMaskedImg);
            visionSubsystem.getOutputStream("Canny Edge Stream").putFrame(cannyEdgeImg);
            visionSubsystem.getOutputStream("Undistorted Stream").putFrame(image); // the stream with annotated
                                                                                   // coordinates
        }
    }

    @Override
    public boolean isFinished() {
        return false;

    }

    @Override
    public void end(boolean interrupted) {
    }
}
