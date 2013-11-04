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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

import eu.opends.drivingTask.DrivingTask;

/**
 * 
 * @author Saied, Rafael Math
 */
public class DataReader 
{
	private File inFile;
	private BufferedReader inputReader;
	private String nameOfDrivingTaskFile;
	private String nameOfDriver;
	private Date fileDate;
	private String inputLineNext;

	
	public boolean initReader(String filePath, boolean verbose) 
	{
		String inputLine;
		String[] splittedLineArray;

		inFile = new File(filePath);
		if (!inFile.isFile()) {
			System.err.println("File " + inFile.toString()
					+ " could not be found.");
		}
		try {
			inputReader = new BufferedReader(new FileReader(inFile));

			// Read in the name of the driving task
			inputLine = inputReader.readLine();
			splittedLineArray = inputLine.split(": ");

			nameOfDrivingTaskFile = splittedLineArray[1];
			if(verbose)
				System.out.println("Driving Task: " + splittedLineArray[1]);


			
			// Read in the date and time, at which the data-file has been
			// created.
			inputLine = inputReader.readLine();
			splittedLineArray = inputLine.split(": ");
			try {
				// Save the date
				fileDate = new SimpleDateFormat("yyyy_mm_dd-hh_mm_ss")
						.parse(splittedLineArray[1]);
				if(verbose)
					System.out.println("Creation Time: " + fileDate);

			} catch (ParseException e) {
				System.err.println("The date could not be read: " + inputLine
						+ " is no valid date.");
				fileDate = null;
			}

			// Read in name of the driver
			inputLine = inputReader.readLine();
			splittedLineArray = inputLine.split(": ");
			nameOfDriver = splittedLineArray[1];
			if(verbose)
				System.out.println("Driver: " + nameOfDriver);

			// Read in the used format, so it can be skipped.
			inputLine = inputReader.readLine();

		} catch (IOException e) {
			//e.printStackTrace();
			return false;
		}
		
		return true;
	}

	
	public String readInNextDataLine() 
	{
		try {
			inputLineNext = inputReader.readLine();
		} catch (IOException e) {

			e.printStackTrace();
		}

		return inputLineNext;
	}
	
	
	public long getTimeStamp(String inputLine) 
	{
		String[] splittedLineArray = inputLine.split(":");
		return Long.parseLong(splittedLineArray[0]);
	}
	

	public Vector3f getCarPositionFromDataLine(String inputLine) 
	{
		String[] splittedLineArray = inputLine.split(":");

		return new Vector3f(Float.parseFloat(splittedLineArray[1]), Float
				.parseFloat(splittedLineArray[2]), Float
				.parseFloat(splittedLineArray[3]));
	}

	
	public Quaternion getCarRotationFromDataLine(String inputLine)
	{
		String[] splittedLineArray = inputLine.split(":");

		return new Quaternion(Float.parseFloat(splittedLineArray[4]), Float
				.parseFloat(splittedLineArray[5]), Float
				.parseFloat(splittedLineArray[6]), Float
				.parseFloat(splittedLineArray[7]));
	}
	
	
	public double getSpeed(String inputLine) 
	{
		String[] splittedLineArray = inputLine.split(":");
		return Double.parseDouble(splittedLineArray[8]);

	}
	
	
	public double getSteeringWheelPosition(String inputLine) 
	{
		String[] splittedLineArray = inputLine.split(":");
		return Double.parseDouble(splittedLineArray[9]);
	}
	
	
	public double getPedalPosition(String inputLine) 
	{
		String[] splittedLineArray = inputLine.split(":");
		return Double.parseDouble(splittedLineArray[10]);
	}

	
	public int getMarker(String inputLine) 
	{
		String[] splittedLineArray = inputLine.split(":");
		return Integer.parseInt(splittedLineArray[12]);
	}
	
	
	public String getNameOfDriver() 
	{
		return nameOfDriver;
	}


	public Date getFileDate() 
	{
		return fileDate;
	}

	public String getNameOfDrivingTaskFile() 
	{
		return nameOfDrivingTaskFile;
	}
	
	
	public boolean isValidAnalyzerFile(File analyzerFile) 
	{
		String analyzerFilePath = analyzerFile.getPath();
		
		try {
			
			boolean errorOccured = !initReader(analyzerFilePath, false);
			if(errorOccured)
			{
				System.err.println("File is not a valid analyzer file: " + analyzerFilePath);
				return false;
			}
			
		} catch (Exception e) {
			
			System.err.println("File is not a valid analyzer file: " + analyzerFilePath);
			return false;
		}
		
		try {
			
			// check whether specified driving task is valid
			String drivingTaskFileName = getNameOfDrivingTaskFile();
			File drivingTaskFile = new File(drivingTaskFileName);				
			if(!DrivingTask.isValidDrivingTask(drivingTaskFile))
			{
				System.err.println("File '" + analyzerFilePath + 
						"'\npoints to an invalid driving task file : " + drivingTaskFileName);
				return false;
			}
			
		} catch (Exception e) {
			
			System.err.println("File '" + analyzerFilePath + "'\npoints to an invalid driving task file");
			return false;
		}
		
		return true;
	}

}
