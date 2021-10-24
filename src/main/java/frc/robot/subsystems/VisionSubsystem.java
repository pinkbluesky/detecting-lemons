package frc.robot.subsystems;

import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.CvSource;

public class VisionSubsystem extends SubsystemBase {
    private CvSink cvSink;
    private CvSource outputStream;
    private CvSource outputStream2;
    private CvSource outputStream3;

    public VisionSubsystem() {
        CommandScheduler.getInstance().registerSubsystem(this); // allows periodic
        // function to be called by scheduler
        startStreams();
    }

    @Override
    public void periodic() {
        // This method will be called once per scheduler run

    }

    public CvSink getCvSink() {
        return cvSink;
    }

    public CvSource getOutputStream() {
        return outputStream;
    }

    public CvSource getOutputStream2() {
        return outputStream2;
    }

    public CvSource getOutputStream3() {
        return outputStream3;
    }

    @Override
    public void simulationPeriodic() {
        // This method will be called once per scheduler run during simulation
        System.out.print("");
    }

    public void startStreams() {

        // final String cameraName =
        // "\\\\?\\usb#vid_05a3&pid_9230&mi_00#7&2cb608c4&0&0000#{e5323777-f976-4f5b-9b55-b94699c46e44}\\global";

        CameraServer.getInstance().startAutomaticCapture(); // webcam
        CameraServer.getInstance().startAutomaticCapture(1); // leftside usb

        // Creates the CvSink and connects it to the UsbCamera
        cvSink = CameraServer.getInstance().getVideo();

        // Creates the CvSource and MjpegServer [2] and connects them
        outputStream = CameraServer.getInstance().putVideo("Output Stream", 640, 480);
        outputStream2 = CameraServer.getInstance().putVideo("Output Stream 2", 640, 480);
        outputStream3 = CameraServer.getInstance().putVideo("Output Stream 3", 640, 480);
    }

}
