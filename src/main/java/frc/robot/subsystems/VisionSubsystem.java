package frc.robot.subsystems;

import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

import java.util.HashMap;
import java.util.Map;

import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.CvSource;

public class VisionSubsystem extends SubsystemBase {
    private CvSink cvSink;
    private Map<String, CvSource> outputStreamMap;

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

    public CvSource getOutputStream(String name) {

        // if no camera stream with specified name exists
        if (outputStreamMap.get(name) == null) {
            // create a new stream and update the map
            CvSource newStream = CameraServer.getInstance().putVideo(name, 640, 480);
            outputStreamMap.put(name, newStream);

            return newStream;
        }

        // if exists, return the specified output stream
        return outputStreamMap.get(name);
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

        // initialize output stream map, which will contain all created output streams
        outputStreamMap = new HashMap<String, CvSource>();

    }

}
