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

package eu.opends.environment;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.jme3.scene.Spatial;



import eu.opends.environment.TrafficLight.*;
import eu.opends.environment.TrafficLightException.NoInternalProgramException;
import eu.opends.main.Simulator;
import eu.opends.visualization.*;

/**
 * This class represents the management of all traffic lights within a model. It 
 * contains a complete list of all traffic lights and a list of traffic light 
 * programs for each intersection
 * 
 * @author Rafael Math
 */
public class TrafficLightCenter 
{
	/**
	 * TrafficLightMode indicates which traffic light program is applied for 
	 * all intersections.
	 */
	public enum TrafficLightMode
	{
		TRIGGER,PROGRAM,EXTERNAL,BLINKING,OFF;
	}
	
	
	private static List<TrafficLight> trafficLightsList;
	private static Simulator sim;
	private static TrafficLightMode mode;
	private static TrafficLightExternalConnector trafficLightExternalConnector;
	private static List<TrafficLightInternalProgram> trafficLightProgramList = 
		new LinkedList<TrafficLightInternalProgram>();
	
	
	/**
	 * Setup initializes the traffic light center. Traffic light rules will 
	 * be loaded from a *-tlr.xml file, traffic lights will be loaded from the 
	 * map model, rules will be attached to traffic lights and traffic light
	 * programs will be started
	 * 
	 * @param _sim
	 * 			Simulator for map data
	 */
	public static void setup(Simulator _sim)
	{
		sim = _sim;
		
		// load traffic light rules from file
		TrafficLightRules.loadTrafficLightRules(Simulator.getDrivingTask().getPath());
		
		// create traffic lights and add them to the traffic lights list
		List<Spatial> objectList = sim.getSceneNode().getChildren();
		trafficLightsList = filterTrafficLights(objectList);
		
		trafficLightsList.addAll(Simulator.getDrivingTask().getScenarioLoader().getTrafficLights());
		
		// set internal traffic light program as default
		mode = TrafficLightMode.TRIGGER;

		List<String> intersectionIDList = new LinkedList<String>();
		for(TrafficLight trafficLight : trafficLightsList)
		{
			String trafficLightName = trafficLight.getName();
			
			// add individual traffic light rules to current traffic light
			ArrayList<String> currentTrafficLightsRules = 
				TrafficLightRules.getTrafficLightRules(trafficLightName);
			if(currentTrafficLightsRules != null)
				trafficLight.setTrafficLightRules(currentTrafficLightsRules);
			
			// add individual traffic light position data to current traffic light
			TrafficLightPositionData currentTrafficLightsPositionData = 
				TrafficLightRules.getTrafficLightPositionData(trafficLightName);
			if(currentTrafficLightsPositionData != null)
				trafficLight.setPositionData(currentTrafficLightsPositionData);
			
			// create a list containing all intersections
			String intersectionID = trafficLight.getIntersectionID();
			if(!intersectionIDList.contains(intersectionID))
				intersectionIDList.add(intersectionID);
			
			// print position and orientation of a traffic light
			//LightningData.printTrafficLightInfos(trafficLight, 100f);
		}

		// create a new traffic light program for each intersection
		for(String intersectionID : intersectionIDList)
		{
			// get traffic light phases for current intersection
			LinkedList<TrafficLightPhase> currentTrafficLightsPhases = 
				TrafficLightRules.getTrafficLightPhases(intersectionID);

			// create and start program
			TrafficLightInternalProgram trafficLightInternalProgram = 
				new TrafficLightInternalProgram(sim,intersectionID,trafficLightsList,currentTrafficLightsPhases);
			trafficLightInternalProgram.start();
			trafficLightProgramList.add(trafficLightInternalProgram);
		}	
		
		// start trafficLight-thread
		trafficLightExternalConnector = new TrafficLightExternalConnector(sim,2001,2048);
		trafficLightExternalConnector.start();
	}
	
	
	/**
	 * Evaluates an XML-string containing traffic light circuits from 
	 * external sources
	 * 
	 * @param datastring
	 * 			XML-file to be processed
	 */
	public static void evaluateInstructionString(String datastring)
	{
		if(mode == TrafficLightMode.EXTERNAL)
		{
			XMLParser parser = new XMLParser(datastring);
			parser.evalTrafficLightInstructions();
		}
	}
	
	
	/**
	 * When the traffic light trigger was hit and the internal traffic light 
	 * program is running in TRIGGER mode, green light will be requested at 
	 * the given traffic light 
	 * 
	 * @param trafficLightName
	 * 			Name of traffic light requested to switch to green
	 */
	public static void reportCollision(String trafficLightName, String type)
	{
		if((mode == TrafficLightMode.TRIGGER) && (type.equals("TrafficLightTrigger")))
		{
			TrafficLight trafficLight = getTrafficLightByName(trafficLightName);
			for(TrafficLightInternalProgram trafficLightInternalProgram : trafficLightProgramList)
			{
				if(trafficLightInternalProgram.getIntersectionID().equals(trafficLight.getIntersectionID()))
				{
					trafficLightInternalProgram.requestGreen(trafficLight);
				}
			}
		}
	}

	
	/**
	 * Returns current mode of traffic light center
	 * 
	 * @return
	 * 			mode of traffic light center
	 */
	public static TrafficLightMode getMode()
	{
		return mode;
	}
	
	
	/**
	 * Switch off all traffic lights and restart programs in the given mode
	 * 
	 * @param _mode
	 * 			new traffic light mode
	 */
	public static void setMode(TrafficLightMode _mode)
	{
		if(mode != _mode)
		{
			setStateAll(TrafficLightState.OFF);
			mode = _mode;
			System.out.println("Switched mode to " + _mode.toString());
		}
	}
	
	
	/**
	 * Switches to next traffic light mode. If last one was reached, continue 
	 * with first. Order: TRIGGER --> PROGRAM --> EXTERNAL --> BLINKING --> OFF
	 */
	public static void toggleMode()
	{
		if(getMode() == TrafficLightMode.TRIGGER)
			setMode(TrafficLightMode.PROGRAM);
		
		else if(getMode() == TrafficLightMode.PROGRAM)
			setMode(TrafficLightMode.EXTERNAL);
		
		else if(getMode() == TrafficLightMode.EXTERNAL)
			setMode(TrafficLightMode.BLINKING);
		
		else if(getMode() == TrafficLightMode.BLINKING)
			setMode(TrafficLightMode.OFF);
		
		else if(getMode() == TrafficLightMode.OFF)
			setMode(TrafficLightMode.TRIGGER);
	}
	
	
	/**
	 * Looks up the traffic light object with the given name. If no object found,
	 * null will be returned
	 * 
	 * @param trafficLightName
	 * 			name of the traffic light to look up (i.e. "TrafficLight.06_04" 
	 * 			without arrow description)
	 * 
	 * @return
	 * 			traffic light object for given input string
	 */
	public static TrafficLight getTrafficLightByName(String trafficLightName)
	{		
		for(TrafficLight trafficLight : trafficLightsList)
		{
			if(trafficLight.getName().equals(trafficLightName))
				return trafficLight;
		}
		return null;
	}
	
	
	/**
	 * Looks up the traffic light object with the given ID. If no object found,
	 * null will be returned
	 * 
	 * @param trafficLightID
	 * 			ID of the traffic light to look up (i.e. "TrafficLight.06_04.R")
	 * 
	 * @return
	 * 			traffic light object for given input string
	 */
	public static TrafficLight getTrafficLightByID(String trafficLightID)
	{		
		for(TrafficLight trafficLight : trafficLightsList)
		{
			if(trafficLight.getObjectID().equals(trafficLightID))
				return trafficLight;
		}
		return null;
	}
	
	
	/**
	 * Looks up the traffic light object with the given intersectionID, roadID and 
	 * lane. If no object found, null will be returned.
	 * 
	 * @param intersectionID
	 * 			Intersection, where the wanted traffic light is located
	 * 
	 * @param roadID
	 * 			Road leading to the given intersection
	 * 	
	 * @param lane
	 * 			Lane at given road
	 * 
	 * @return
	 * 			Traffic light at given intersection, road and lane (returns null,
	 *			if not available)
	 */
	public static TrafficLight getTrafficLightByLocation(String intersectionID, String roadID, int lane) 
	{
		try{
			
			// go through traffic light list
			for(TrafficLight trafficLight : trafficLightsList)
			{
				if(
						   trafficLight.getIntersectionID().equals(intersectionID)
						&& trafficLight.getPositionData().getRoadID().equals(roadID)
						&& trafficLight.getPositionData().getLane() == lane
				   )
					// return traffic light that matches with the given intersectionID, 
					// roadID and lane
					return trafficLight;
			}
			
		}catch(NullPointerException e){
			return null;
		}
		
		return null;
	}
	
	
	/**
	 * Returns the internal program a given intersection is assigned to.
	 * 
	 * @param IntersectionID
	 * 			ID of the traffic light intersection
	 * 
	 * @return
	 * 			Internal program for given intersectionID
	 * 
	 * @throws NoInternalProgramException
	 * 			If no matching internal program could be found
	 */
	public static TrafficLightInternalProgram getInternalProgram(String IntersectionID) 
			throws NoInternalProgramException
	{
		// go through all internal programs (normally one program for each intersection)
		for(TrafficLightInternalProgram trafficLightInternalProgram : trafficLightProgramList)
		{
			// return internal program if it matches the given intersectionID
			if(trafficLightInternalProgram.getIntersectionID().equals(IntersectionID))
				return trafficLightInternalProgram;
		}
		
		// if no internal program found --> throw exception
		throw new NoInternalProgramException("Traffic light not assigned to any internal program!");
	}
	

	/**
	 * Returns the internal program a given traffic light (and thus 
	 * intersection) is assigned to.
	 * 
	 * @param trafficLight
	 * 			traffic light object
	 * 
	 * @return
	 * 			Internal program for given traffic light
	 * 
	 * @throws Exception 
	 * 			If no matching internal program could be found
	 */
	public static TrafficLightInternalProgram getInternalProgram(TrafficLight trafficLight) 
			throws NoInternalProgramException
	{
		return getInternalProgram(trafficLight.getIntersectionID());
	}
	

	/**
	 * Sets all traffic lights to the given state
	 * 
	 * @param state
	 * 			state (e.g. red, yellow, green, ...) to set to all traffic lights
	 */
	private static void setStateAll(TrafficLightState state)
	{
		for (TrafficLight trafficLight : trafficLightsList)
		{
			trafficLight.setState(state);
		}
	}
	
	
	/**
	 * Creates the list of traffic lights from the list of all spatial objects 
	 * in the model
	 * 
	 * @param blenderObjectsList
	 * 			List containing all spatial objects of the model
	 * 
	 * @return
	 * 			List containing all traffic lights
	 */
	private static List<TrafficLight> filterTrafficLights(List<Spatial> blenderObjectsList) 
	{
		List<TrafficLight> trafficLightsList = new LinkedList<TrafficLight>();

		for (Spatial spatial : blenderObjectsList) 
		{
			if (spatial.getName() != null && spatial.getName().startsWith("TrafficLight")) 
			{
				trafficLightsList.add(new TrafficLight(sim,spatial));
			}
		}
		return trafficLightsList;
	}


	/**
	 * Creates a string containing all current traffic light states and sends 
	 * this string to Lightning
	 */
	public static void updateGlobalStatesString() 
	{
		String direction = "";
		String state = "";
		LightningClient lightningClient = sim.getLightningClient();
		
		// if simulator is connected to Lightning
		if(lightningClient != null)
		{
			String globalStates = "ltupdate .remotemotionsensor -trafficlightmodesIn {";
			
			// go through the list of all traffic lights and build string
			for (TrafficLight trafficLight : trafficLightsList)
			{
				direction = "";
				state = trafficLight.getState().toString().toLowerCase();
				
				if((!state.equals("init")) && (!state.equals("off")))
				{
					direction = "_" + trafficLight.getDirection().toString().toLowerCase();
					if(direction.equals("_none"))
						direction = "";
				}				
				
				globalStates += "\"" + state + direction + "\" ";
			}
			
			globalStates += "}\n";
			
			// send string to Lightning
			lightningClient.sendTrafficLightData(globalStates);
		}
	}
	
	
	/**
	 * Closes all internal threads
	 */
	public static void close()
	{
		for(TrafficLightInternalProgram tlip : trafficLightProgramList)
			tlip.requestStop();
		
		trafficLightExternalConnector.requestStop();
	}
	
}


