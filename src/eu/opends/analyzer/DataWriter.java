/*
*  This file is part of OpenDS (Open Source Driving Simulator).
*  Copyright (C) 2013 Rafael Math
*
*  OpenDS is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, either version 3 of the License, or
*  (at your option) any later version.
*
*  OpenDS is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU General Public License for more details.
*
*  You should have received a copy of the GNU General Public License
*  along with OpenDS. If not, see <http://www.gnu.org/licenses/>.
*/

package eu.opends.analyzer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import eu.opends.car.Car;
import eu.opends.car.LightTexturesContainer.TurnSignalState;
import eu.opends.tools.Util;

/**
 * 
 * That class is responsible for writing drive-data. At the moment it is a
 * ripped down version of similar classes used in CARS.
 * 
 * @author Saied
 * 		   Anpassungen Martin Michael Kalbitz
 */
public class DataWriter 
{
	private Calendar startTime = new GregorianCalendar();

	/**
	 * An array list for not having to write every row directly to file.
	 */
	private ArrayList<DataUnit> arrayDataList;
	private BufferedWriter out;
	private File outFile;
	private String newLine = System.getProperty("line.separator");
	private Date lastAnalyzerDataSave;
	private String outputFolder;
	private Car car;
	private File analyzerDataFile;
	private boolean dataWriterEnabled = true;
	private String driverName = "";
	private Date curDate;
	private String drivingTaskFileName;


	public DataWriter(String outputFolder, Car car, String driverName, String drivingTaskFileName) 
	{	
		this.outputFolder = outputFolder;
		this.car = car;
		this.driverName = driverName;
		this.drivingTaskFileName = drivingTaskFileName;

		Util.makeDirectory(outputFolder);

		analyzerDataFile = new File(outputFolder + "/carData.txt");

		initWriter();
	}


	public void initWriter() 
	{

		if (analyzerDataFile.getAbsolutePath() == null) 
		{
			System.err.println("Parameter not accepted at method initWriter.");
			return;
		}
		
		outFile = new File(analyzerDataFile.getAbsolutePath());
		
		int i = 2;
		while(outFile.exists()) 
		{
			analyzerDataFile = new File(outputFolder + "/carData(" + i + ").txt");
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
			out.write("Used Format = Time (ms): Position (x,y,z) : Rotation (x,y,z,w) :"
					+ " Speed (km/h) : Steering Wheel Position [-1,1] : Gas Pedal Position :"
					+ " Brake Pedal Position : Engine (On) : light Intensity : TurnSignalLeft :"
					+ " TurnSignalRight" + newLine);

		} catch (IOException e) {
			e.printStackTrace();
		}
		arrayDataList = new ArrayList<DataUnit>();

		lastAnalyzerDataSave = new Date();
	}


	/**
	 * Save the car data at a frequency of 20Hz. That class should be called in
	 * the update-method <code>Simulator.java</code>.
	 */
	public void saveAnalyzerData() 
	{
		curDate = new Date();

		if (curDate.getTime() - lastAnalyzerDataSave.getTime() >= 50) 
		{
			//boolean enginOn, int lightIntensity, boolean blinkerLeft, boolean blinkerRight
			write(
					curDate,
					Math.round(car.getPosition().x * 1000) / 1000.,
					Math.round(car.getPosition().y * 1000) / 1000.,
					Math.round(car.getPosition().z * 1000) / 1000.,
					Math.round(car.getRotation().getX() * 10000) / 10000.,
					Math.round(car.getRotation().getY() * 10000) / 10000.,
					Math.round(car.getRotation().getZ() * 10000) / 10000.,
					Math.round(car.getRotation().getW() * 10000) / 10000.,
					car.getCurrentSpeedKmhRounded(), Math.round(car
							.getSteeringWheelState() * 100000) / 100000., 
					car.getGasPedalPressIntensity(), car.getBrakePedalPressIntensity(),
					car.isEngineOn(),
					car.getLightIntensity(),
					car.getTurnSignal() == TurnSignalState.BOTH || car.getTurnSignal() == TurnSignalState.LEFT ? true : false,
					car.getTurnSignal() == TurnSignalState.BOTH || car.getTurnSignal() == TurnSignalState.RIGHT ? true : false	
					);

			lastAnalyzerDataSave = curDate;
		}

	}

	
	/**
	 * 
	 * see eu.opends.analyzer.IAnalyzationDataWriter#write(float,
	 *      float, float, float, java.util.Date, float, float, boolean, float)
	 */
	public void write(Date curDate, double x, double y, double z, double xRot,
			double yRot, double zRot, double wRot, double linearSpeed,
			double steeringWheelState, double gasPedalState, double brakePedalState,
			boolean enginOn, int lightIntensity, boolean blinkerLeft, boolean blinkerRight) 
	{
		DataUnit row = new DataUnit(curDate, x, y, z, xRot, yRot, zRot, wRot,
				linearSpeed, steeringWheelState, gasPedalState, brakePedalState,
				enginOn, lightIntensity, blinkerLeft, blinkerRight);
		this.write(row);

	}
	

	/**
	 * Write data to the data pool. After 50 data sets, the pool is flushed to
	 * the file.
	 */
	public void write(DataUnit row)
	{
		arrayDataList.add(row);
		if (arrayDataList.size() > 50)
			flush();
	}
	

	public void flush() 
	{
		try {
			StringBuffer sb = new StringBuffer();
			for (DataUnit r : arrayDataList) {
				sb.append(r.getDate().getTime() + ":" + r.getXpos() + ":"
						+ r.getYpos() + ":" + r.getZpos() + ":" + r.getXrot()
						+ ":" + r.getYrot() + ":" + r.getZrot() + ":"
						+ r.getWrot() + ":" + r.getSpeed() + ":"
						+ r.getSteeringWheelPos() + ":" + r.getPedalPos() + ":"
						+ r.isBreaking() + newLine
				// + r.getRacingLineDistance()
						// + ":"
						// + r.getFuelConsumption()
						// + ":" + r.isMarker1Set()
						// + ":"
						// + r.isMarker2Set() + ":" + r.isMarker3Set() + ":"
						// + r.getRound() + newLine
						);
			}
			out.write(sb.toString());
			arrayDataList.clear();
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	
	public void quit() 
	{
		dataWriterEnabled = false;
		flush();
		try {
			if (out != null)
				out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	public boolean isDataWriterEnabled() 
	{
		return dataWriterEnabled;
	}

	
	public void setDataWriterEnabled(boolean dataWriterEnabled) 
	{
		this.dataWriterEnabled = dataWriterEnabled;
	}

	
	public void setStartTime() 
	{
		this.startTime = new GregorianCalendar();
	}
	
	
	public String getElapsedTime()
	{
		Calendar now = new GregorianCalendar();
		
		long milliseconds1 = startTime.getTimeInMillis();
	    long milliseconds2 = now.getTimeInMillis();
	    
	    long elapsedMilliseconds = milliseconds2 - milliseconds1;
	    
	    return "Time elapsed: " + new SimpleDateFormat("mm:ss.SSS").format(elapsedMilliseconds);
	}

}
