package eu.opends.webcam;

import java.io.File;

import static com.googlecode.javacv.cpp.opencv_highgui.*;

import com.googlecode.javacv.CanvasFrame;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class CapturedImageViewer {
	private CanvasFrame[] canvasframes;
	private String outputFolder;
	
	public CapturedImageViewer(String OutputFolder){
		// save outputFolder
		this.outputFolder = OutputFolder;
		
		// check how many webcams were recorded
		int i = 0;
		while(new File(OutputFolder + "/" + i + "/").isDirectory()){
			i++;
		}
		
		// initialize canvas-frames for each recorded webcam
		if(i > 0){
			canvasframes = new CanvasFrame[i];
			
			for(i=i; i > 0; i--){
				canvasframes[i-1] = new CanvasFrame("Webcam "+i, 1);
				System.out.println("Created canvasFrame " + i);
			}
		}
		
	}
	
	public void showImage(long timestamp){
		// update canvas-frames
		int i = 0;
		for(CanvasFrame canvas : canvasframes){
			boolean imageexists = new File(this.outputFolder + "/" + i + "/" + timestamp
					+ "-capture.jpg").exists();
			System.out.println("File "+this.outputFolder + "/" + i + "/" + timestamp
					+ "-capture.jpg" + " exists: " +imageexists);
			if (imageexists)
			{
				IplImage image;
				image = cvLoadImage(this.outputFolder + "/" + i + "/" + timestamp + "-capture.jpg");
				canvas.showImage(image);
				com.googlecode.javacv.cpp.opencv_core.cvReleaseImage(image);
			}
			
			i++;
		}
	}
}
