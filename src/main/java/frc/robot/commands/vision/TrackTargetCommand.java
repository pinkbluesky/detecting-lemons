package frc.robot.commands.vision;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.robot.subsystems.VisionSubsystem;

public class TrackTargetCommand extends CommandBase {

    private final VisionSubsystem visionSubsystem;

    private Mat image;
    private Mat imageHSV;

    public static final String HSV_CONFIG_FILE_PATH = "/src/main/java/frc/robot/commands/vision/lemon_config.json";

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

        if (!image.empty()) {
            Imgproc.cvtColor(image, imageHSV, Imgproc.COLOR_BGR2HSV);
            Mat thresh = new Mat();
            Core.inRange(imageHSV, hsvTab.getLowScalar(), hsvTab.getHighScalar(), thresh);
            visionSubsystem.getOutputStream().putFrame(thresh);
        }
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    @Override
    public void end(boolean interrupted) {

        // save HSV values from Shuffleboard slider into config file
        hsvTab.save();

    }
}
