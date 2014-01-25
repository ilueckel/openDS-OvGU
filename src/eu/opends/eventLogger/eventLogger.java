package eu.opends.eventLogger;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.swing.*;

import eu.opends.analyzer.DataUnit;
import eu.opends.car.Car;
import eu.opends.tools.Util;
import eu.opends.webcam.Grabber;
import eu.opends.webcam.SoundRecorder;

public class eventLogger {

	private Calendar startTime = new GregorianCalendar();

	/**
	 * An array list for not having to write every row directly to file.
	 */
	private ArrayList<DataUnit> arrayDataList;
	private BufferedWriter out;
	private File outFile;
	private String newLine = System.getProperty("line.separator");
	private String outputFolder;
	private File analyzerDataFile;
	private boolean dataWriterEnabled = false;
	private String driverName = "";
	private String drivingTaskFileName;

	public eventLogger(String outputFolder, Car car, String driverName,
			String drivingTaskFileName) {
		this.outputFolder = outputFolder;
		this.driverName = driverName;
		this.drivingTaskFileName = drivingTaskFileName;

		Util.makeDirectory(outputFolder);

		analyzerDataFile = new File(outputFolder + "/eventData.txt");

		initWriter();
	}

	public void initWriter() {

		if (analyzerDataFile.getAbsolutePath() == null) {
			System.err.println("Parameter not accepted at method initWriter.");
			return;
		}

		outFile = new File(analyzerDataFile.getAbsolutePath());

		int i = 2;
		while (outFile.exists()) {
			analyzerDataFile = new File(outputFolder + "/eventData(" + i
					+ ").txt");
			outFile = new File(analyzerDataFile.getAbsolutePath());
			i++;
		}

		try {
			out = new BufferedWriter(new FileWriter(outFile));
			out.write("Driving Task: " + drivingTaskFileName + newLine);
			out.write("Date-Time: "
					+ new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss")
							.format(new Date()) + newLine);
			out.write("Driver: " + driverName + newLine);
			out.write("Used Format = Time (ms): Description");
			out.newLine();

		} catch (IOException e) {
			e.printStackTrace();
		}
		arrayDataList = new ArrayList<DataUnit>();
	}

	public void logEvent() {
		Runnable r = new Runnable() {
			
			@Override
			public void run() {
				Date date = new Date();
				 String str = JOptionPane.showInputDialog(null,
						"Enter your event text : ", "Event logger", 1);
				write(date, str);
			}
		};
		
		 Thread t = new Thread(r);
         t.start();
		
	}

	/**
	 * 
	 * see eu.opends.analyzer.IAnalyzationDataWriter#write(float, float, float,
	 * float, java.util.Date, float, float, boolean, float)
	 */
	public void write(Date curDate, String description) {
		DataUnit row = new DataUnit(curDate, description);
		this.write(row);
	}

	/**
	 * Write data to the data pool. After 50 data sets, the pool is flushed to
	 * the file.
	 */
	public void write(DataUnit row) {
		arrayDataList.add(row);
		flush();
	}

	public void flush() {
		try {
			StringBuffer sb = new StringBuffer();
			for (DataUnit r : arrayDataList) {
				sb.append(r.getDate().getTime() + ":" + r.getDescription()
						+ newLine);
			}
			out.write(sb.toString());
			arrayDataList.clear();
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void quit() {
		dataWriterEnabled = false;
		flush();
		try {
			if (out != null)
				out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean isDataWriterEnabled() {
		return dataWriterEnabled;
	}

	public void setDataWriterEnabled(boolean dataWriterEnabled) {
		this.dataWriterEnabled = dataWriterEnabled;
	}

	public void setStartTime() {
		this.startTime = new GregorianCalendar();
	}

	public String getElapsedTime() {
		Calendar now = new GregorianCalendar();

		long milliseconds1 = startTime.getTimeInMillis();
		long milliseconds2 = now.getTimeInMillis();

		long elapsedMilliseconds = milliseconds2 - milliseconds1;

		return "Time elapsed: "
				+ new SimpleDateFormat("mm:ss.SSS").format(elapsedMilliseconds);
	}
}