package eu.opends.webcam;

import java.io.File;

import static com.googlecode.javacv.cpp.opencv_highgui.cvSaveImage;
import static com.googlecode.javacv.cpp.videoInputLib.*;

import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.VideoInputFrameGrabber;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

/*
 * 
 */
public class Grabber {
	private FrameGrabber[] grabber;
	private String OutputFolder;

	public Grabber(String outputFolder) {
		OutputFolder = outputFolder;
	}

	private void captureImage(int camId, String timeStamp) {
		try {
			IplImage img;
			img = grabber[camId].grab();
			if (img != null) {
				cvSaveImage(OutputFolder + "/" + camId + "/" + timeStamp
						+ "-capture.jpg", img);
			}
		} catch (Exception e) {
		}
	}

	/*
	 * 
	 */
	public void captureImage(final String timeStamp) {
		int devices = videoInput.listDevices();
		for (int i = 0; i < devices; i++) {
			captureImage(i, timeStamp);
		}
	}

	private void stopCam(int camId) {
		try {
			grabber[camId].release();
			grabber[camId].stop();
		} catch (com.googlecode.javacv.FrameGrabber.Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * 
	 */
	public void initializeCam() {
		int devices = videoInput.listDevices();
		grabber = new FrameGrabber[devices];
		for (int i = 0; i < devices; i++) {
			grabber[i] = new VideoInputFrameGrabber(i);
			File dir = new File("./" + OutputFolder + "/" + i + "/");
			dir.mkdirs();
			try {
				grabber[i].start();
			} catch (com.googlecode.javacv.FrameGrabber.Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/*
	 * 
	 */
	public void stop() {
		int devices = videoInput.listDevices();
		for (int i = 0; i < devices; i++) {
			stopCam(i);
		}
	}
}
