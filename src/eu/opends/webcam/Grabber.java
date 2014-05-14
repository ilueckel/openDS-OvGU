package eu.opends.webcam;

import java.io.File;

import static com.googlecode.javacv.cpp.opencv_highgui.cvSaveImage;
import static com.googlecode.javacv.cpp.videoInputLib.*;

import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.VideoInputFrameGrabber;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.CanvasFrame;


/*
 * 
 */
public class Grabber {
	private FrameGrabber[] grabber;
	private String OutputFolder;
	
	
	//IplImage image;
    //CanvasFrame canvas = new CanvasFrame("Web Cam");
	

	public Grabber(String outputFolder) {
		OutputFolder = outputFolder;
	}

	private void captureImage(final int camId, final String timeStamp) {
		try {
			final IplImage img;
			img = grabber[camId].grab();
			if (img != null) {
				
				Runnable r = new Runnable()
				{
				    @Override
				    public void run()
				    {
				    	cvSaveImage(OutputFolder + "/" + camId + "/" + timeStamp + "-capture.jpg", img);
				    }
				};

				Thread t = new Thread(r);
				t.start();
				
				
				//canvas.showImage(img);
				//canvas.setTitle(timeStamp);
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
		
		/*FrameGrabber grabber = new VideoInputFrameGrabber(0); 
        int i=0;
        try {
            grabber.start();
            IplImage img;
            while (true) {
                img = grabber.grab();
                if (img != null) {
                    //cvFlip(img, img, 1);// l-r = 90_degrees_steps_anti_clockwise
                    //cvSaveImage((i++)+"-capture.jpg", img);
                    // show image on window
                    canvas.showImage(img);
                }
                 //Thread.sleep(INTERVAL);
            }
        } catch (Exception e) {
        }*/
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
