package frc.robot.commands.vision;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
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
            Imgproc.cvtColor(image, imageHSV, Imgproc.COLOR_BGR2HSV);
            Mat colorThresh = new Mat();
            // filter yellow color
            Core.inRange(imageHSV, hsvTab.getLowScalar(), hsvTab.getHighScalar(), colorThresh);

            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

            Imgproc.findContours(colorThresh, contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_NONE);

            Mat contourImg = new Mat(colorThresh.size(), colorThresh.type());
            Imgproc.drawContours(contourImg, contours, -1, new Scalar(255, 0, 0), 5);

            visionSubsystem.getOutputStream().putFrame(contourImg);
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
