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

package eu.opends.canbus;

import java.net.Socket;
import java.net.SocketException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.io.*;

//import sun.net.www.protocol.http.HttpURLConnection.TunnelState;

import com.jme3.math.Vector3f;

import eu.opends.car.Car;
import eu.opends.car.LightTexturesContainer.TurnSignalState;
import eu.opends.drivingTask.settings.SettingsLoader;
import eu.opends.drivingTask.settings.SettingsLoader.Setting;
import eu.opends.environment.XMLParser;
import eu.opends.main.SimulationDefaults;
import eu.opends.main.Simulator;

/**
 * This class represents the connector to the CAN-Interface. Steering, gas, brake and 
 * control instructions from the real car will be forwarded to the simulator; heading,
 * geo coordinates and speed will be sent back to the CAN-Interface in order to display
 * the position and speed on a in-car display. Furthermore trigger collisions can be 
 * sent to the CAN-Interface.
 * 
 * @author Rafael Math
 */
public class CANClient extends Thread
{
	// angle the real car wheel must be rotated for full lock in simulator 
	private float maxSteeringAngle;	
	private Simulator sim;
	private Car car;
	private int framerate;
	private boolean stoprequested;
	private boolean errorOccurred;
	private float steeringAngle;
	private boolean doSteering;
	private Calendar timeOfLastFire;
	private PrintWriter printWriter;
	private Socket socket;
	
	//Neu hinzugefuegte Variablen
	private File outputFile;
	private File inputFile;
	private String lineSeperator;
	private FileWriter writer;
	private BufferedReader reader;
	private StringBuffer outputMessage;
	
	
	/**
	 * Creates a new TCP connection with the CAN-Interface at the given IP and port
	 * 
	 * @param sim
	 * 			The simulator
	 */
	public CANClient(Simulator sim)
    {
		super();
		
		this.sim = sim;
		this.car = sim.getCar();
		stoprequested = false;
		errorOccurred = false;
		steeringAngle = 0.0f;
		doSteering = false;
		timeOfLastFire = new GregorianCalendar();
		
		SettingsLoader settingsLoader = Simulator.getDrivingTask().getSettingsLoader();
		//String ip = settingsLoader.getSetting(Setting.CANInterface_ip, SimulationDefaults.CANInterface_ip);
		//int port = settingsLoader.getSetting(Setting.CANInterface_port, SimulationDefaults.CANInterface_port);
		framerate = settingsLoader.getSetting(Setting.CANInterface_updateRate, SimulationDefaults.CANInterface_updateRate);
		maxSteeringAngle = settingsLoader.getSetting(Setting.CANInterface_maxSteeringAngle, SimulationDefaults.CANInterface_maxSteeringAngle); 
		
		lineSeperator = System.getProperty("line.separator");
		outputFile = new File("outtest.txt");
		inputFile = new File("intest.txt");
		outputMessage = new StringBuffer(50);	//50 als erster Test wenn mehr Werte kommen sollte die Zahl groesser gewaehlt werden
    }
	
    
	/**
	 * Werten Parameter aus einer textdatei aus um den Simulator entsprechend anzupassen
	 */
	@Override
	public void run() 
	{
		// when loop is left, connection will be closed
		// loop will be left when requested or error occurred
		while(!stoprequested && !errorOccurred)
		{
			try {
				reader = new BufferedReader(new FileReader(inputFile));
				//Motor an oder aus
				String zeile = reader.readLine();
				if (zeile != null)
				{
					//Pruefen ob Motor an oder aus
					int enOn = Integer.parseInt(zeile);
					boolean engineOn = enOn == 0 ? false : true;
					if(engineOn != car.isEngineOn())
					{
						car.setEnginOn(engineOn);
					}
				}
				//Licht Status
				zeile = reader.readLine();
				if (zeile != null)
				{
					int lightIntensity = Integer.parseInt(zeile);
					//car.setLightState(lightIntensity);
					car.toggleLight();
					System.out.println(lightIntensity);
					
				}
				//Blinker links
				zeile = reader.readLine();
				int blkLeft;
				if (zeile != null)
				{
					blkLeft = Integer.parseInt(zeile);
					if (blkLeft != -1)
					{
						 if (blkLeft != 0)  car.setTurnSignal(TurnSignalState.LEFT);
						 else        car.setTurnSignal(TurnSignalState.OFF);
					}
					
				}
				//Blinker rechts
				zeile = reader.readLine();
				if (zeile != null)
				{
					int blkright = Integer.parseInt(zeile);
					if(blkright != 0)	car.setTurnSignal(TurnSignalState.RIGHT);
					
				}
				
				
				reader.close();				
			} catch (IOException e) {
				
				// will be thrown if e.g. server was shut down
				System.err.println("Fehler beim Auslesen von Input Daten");
				errorOccurred = true;
				
			} catch (Exception e) {
			}
			
			// set virtual car's steering angle to the given steering angle
			if(doSteering)
				updateSteeringAngle();
		}
		
	}

	
	/**
	 * Schreibt Parameter des Simulates in textdatei die dann von externen Programm auf den CAN geschrieben werden
	 */
	public synchronized void sendCarData()
	{
		// generate time stamp
		Calendar currentTime = new GregorianCalendar();
		
		// if enough time has passed by since last fire, the event will be forwarded
		if(forwardEvent(currentTime))
		{
			
			float speed = ((float) car.getCurrentSpeedKmhRounded());  // in km/h
			//float heading = car.getHeadingDegree();       // 0..360 degree
			Vector3f geoPosition = car.getGeoPosition();
			float latitude = geoPosition.getX();          // N-S position in model coordinates
			float longitude = geoPosition.getY();         // W-E position in model coordinates
			float tspeed = ((float) car.getCurrentSpeedKmh());	//TODO muss noch rausgefunden werden
			int blk_left = car.getTurnSignal() == TurnSignalState.LEFT || car.getTurnSignal() == TurnSignalState.BOTH ? 1 : 0;
			int blk_right = car.getTurnSignal() == TurnSignalState.RIGHT || car.getTurnSignal() == TurnSignalState.BOTH ? 1 : 0;
			
			outputMessage.delete(0, outputMessage.length());
			try { 	
				writer = new FileWriter(outputFile);
				outputMessage.append(speed).append(lineSeperator);			//Geschwindigkeit
				outputMessage.append(tspeed).append(lineSeperator);			//Drehzahl
				outputMessage.append(blk_left).append(lineSeperator);		//blinker links
				outputMessage.append(blk_right).append(lineSeperator);		//blinker rechts
				outputMessage.append(latitude).append(lineSeperator);		//Latitude
				outputMessage.append(longitude).append(lineSeperator);		//Longitude
				writer.write(outputMessage.toString());
				writer.flush();
				writer.close();
			} catch (IOException e) {
				System.err.println("CANClient_sendCarData(): " + e.toString());
			}

		}
	}
	
	
	/**
	 * Sends trigger reports to the CAN-Interface if the simulated car has hit a trigger.
	 * 
	 * @param triggerID
	 * 			ID of the CAN-Trigger that will be sent to the CAN-Interface
	 */
	public synchronized void sendTriggerData(String triggerID)
	{
		// break, if no connection established
		if(socket == null || errorOccurred)
			return;

		try { 	

		 	// send trigger data to CAN-bus and flush
			String triggerString = "$SimCarTrigger#" + triggerID + "%";
			printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
		 	printWriter.print(triggerString);
		 	printWriter.flush();
		 				 	
		} catch (IOException e) {
			System.err.println("CANClient_sendTriggerData(): " + e.toString());
		}
	}

	
	/**
	 * Sends the current deviation from the normative line to the CAN-Interface.
	 * 
	 * @param deviation
	 * 			Value representing the current deviation in meters from the 
	 * 			normative line.
	 */
	public synchronized void sendDeviationData(float deviation) 
	{
		// break, if no connection established
		if(socket == null || errorOccurred)
			return;

		try { 	

		 	// send deviation data to CAN-Interface and flush
			String positionString = "$SimDeviationState#" + deviation + "%";
			printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
		 	printWriter.print(positionString);
		 	printWriter.flush();
		 				 	
		} catch (IOException e) {
			System.err.println("CANClient_sendDeviationData(): " + e.toString());
		}
	}
	
	
	/**
	 * Sets the target steering angle as read from the CAN-Interface in order 
	 * to synchronize with the current steering angle of the simulator. Sets 
	 * "doSteering" to true.
	 * 
	 * @param steeringAngle
	 * 			Steering angle as read from the real car
	 */
	public synchronized void setSteeringAngle(float steeringAngle) 
	{
		// set doSteering to true in order to perform steering instructions 
		// from the real car; otherwise the keyboard will suppress car steering
		this.doSteering = true;
		this.steeringAngle = steeringAngle;
	}
	
	
	/**
	 * Sets "doSteering" to false in order to suppress the steering of the real car.
	 * E.g. if the keyboard steering has higher priority
	 */
	public synchronized void suppressSteering() 
	{
		this.doSteering = false;
	}
	
	
	/**
	 * Requests the connection to close after the current loop
	 */
	public synchronized void requestStop() 
	{
		stoprequested = true;
	}
	
	
	/**
	 * Compares the current steering angle (in the simulator) with the given 
	 * steering angle (of the real car). The bigger the difference, the faster
	 * the steering angle of the simulator will be changed to the wanted value 
	 */
	private void updateSteeringAngle() 
	{
		try {
			
			// get target steering angle from real car
			// maximum angle will be matched to -1 or 1, respectively
			float targetAngle = -Math.max(Math.min(steeringAngle/maxSteeringAngle,1),-1);
			
			// print target (real car) steering angle
			//System.out.println("target: " + targetAngle);
			
			// if target angle is close to straight ahead, steer straight ahead
			if((targetAngle >= -0.001f) && (targetAngle <= 0.001f))	
				targetAngle = 0;
			
			car.steer(targetAngle);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
    
	
	/**
	 * Reads an incoming message from the socket connection.
	 * 
	 * @param socket
	 * 			Socket connection
	 * 
	 * @return
	 * 			Message string of up to 10,000 characters
	 * 
	 * @throws IOException
	 */
	private String readMessage(Socket socket) throws IOException 
	{
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		char[] buffer = new char[10000];
		int nrOfChars = bufferedReader.read(buffer, 0, buffer.length);
		
		return new String(buffer, 0, nrOfChars);
	}
	
	
	/**
	 * This method checks whether the incoming camera information should 
	 * be sent to the server at the current time complying with the given 
	 * frame rate
	 * 
	 * @param now
	 * 			The current time stamp
	 * 
	 * @return true if enough time has passed by since last fire, false otherwise
	 */
    private boolean forwardEvent(Calendar now)
    {
        // fire an event every x milliseconds
    	int fireInterval = 1000 / framerate;

        // subtract time of last event from current time to get time elapsed since last fire
        long elapsedMillisecs = timeDiff(now,timeOfLastFire);
        
        if (elapsedMillisecs >= fireInterval)
        {
            // update time of last fire
            timeOfLastFire.add(Calendar.MILLISECOND, fireInterval);

            // fire
            return true;
        }
        else
            // do not fire
            return false;
    }
    
    
	/**
	 * This method computes the difference between two given time stamps
	 * 
	 * @param timestamp1
	 * 			First time stamp value to compare
	 * 
	 * @param timestamp2
	 * 			Second time stamp value to compare
	 * 
	 * @return difference between the given time stamps in milliseconds (long)
	 */
    private static long timeDiff(Calendar timestamp1, Calendar timestamp2)
    {
        return Math.abs(timestamp1.getTimeInMillis() - timestamp2.getTimeInMillis());
    }

   
}
