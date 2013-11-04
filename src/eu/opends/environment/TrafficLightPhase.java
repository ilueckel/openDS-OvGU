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

import eu.opends.environment.TrafficLight.TrafficLightState;
import eu.opends.environment.TrafficLightException.InvalidStateCharacterException;


/**
 * This class represents a traffic light phase. Each phase consists of a unique ID, 
 * a number of steps the phase endures (duration) and a string containing a character
 * representation of traffic light states. Every time a phase is used it has to be 
 * activated in order to set an expiration time.<br>
 * Moreover, this class contains a method to read a traffic light's state from the 
 * state string ("getState()")
 * 
 * @author Rafael Math
 */
public class TrafficLightPhase 
{
	private String ID;
	private int    duration;
	private String stateString;
	private int    expirationTime;
	

	/**
	 * Creates a new traffic light phase. Needed parameters are the unique phaseID, 
	 * the number of steps a phase endures (usually in seconds) and a state string 
	 * of length 16 representing the states of the involved traffic lights 
	 * (e.g. "yyggrrrryyggrrrr").
	 * 
	 * @param phaseID
	 * 			Unique phaseID
	 * 
	 * @param phaseDuration
	 * 			number of steps a phase endures
	 * 
	 * @param phaseState
	 * 			state string
	 */
	public TrafficLightPhase(String phaseID, int phaseDuration, String phaseState)
	{
		this.ID = phaseID;
		this.duration = phaseDuration;
		this.stateString = phaseState;
	}
	
	
	/**
	 * Returns the ID of this traffic light phase, which can be set in the 
	 * external traffic light rules file. Can be used to check if a phase
	 * has already been passed, since phases will be arranged in cycles.
	 * 
	 * @return
	 * 			ID of this traffic light phase
	 */
	public String getID()
	{
		return ID;
	}
	
	
	/**
	 * Returns the duration (usually time in seconds) of a traffic light phase.
	 * 
	 * @return
	 * 			Number of steps (usually 1 step = 1 second) a phase will endure
	 */
	public int getDuration()
	{
		return duration;
	}
	
	
	/**
	 * Returns the string representation of a phase.<br>
	 * E.g. "ID: 01, Duration: 12, State: yyggrrrryyggrrrr"
	 * 
	 * @return
	 * 			String representation of a phase
	 */
	@Override
	public String toString()
	{
		return "ID: "+ID+", Duration: "+duration+", State: "+stateString;
	}
	
	
	/**
	 * Returns the number of steps (usually in seconds) until the phase expires. This 
	 * value can be computed by subtracting the current time from the expiration time.
	 * 
	 * @param currentTime
	 * 			Current time stamp (number of steps passed so far)
	 * 
	 * @return
	 * 			Number of steps until the phase expires
	 */
	public int timeToExpiration(int currentTime)
	{
		int timeToExpiration = expirationTime - currentTime;
		return Math.max(0, timeToExpiration);
	}
	
	
	/**
	 * Activates a phase by setting the expiration time. Expiration time is the sum 
	 * of steps passed so far and the number of steps the phase endures.
	 * 
	 * @param currentTime
	 * 			Current time stamp (number of steps passed so far)
	 */
	public void activate(int currentTime)
	{
		expirationTime = currentTime + duration;
	}
	
	
	/**
	 * Checks whether a phase has expired.
	 * 
	 * @param currentTime
	 * 			Current time stamp (number of steps passed so far)
	 * @return
	 * 			true, if phase has expired
	 */
	public boolean hasExpired(int currentTime)
	{
		return (expirationTime <= currentTime);
	}
	
	
	/**
	 * Reads a traffic light's state from this phase's state string.
	 * 
	 * @param trafficLight
	 * 			Traffic light to look up in state string
	 * 
	 * @return
	 * 			Traffic light state according to this phase's state string
	 */
	public TrafficLightState getState(TrafficLight trafficLight)
	{
		// get position in state string from traffic light name 
		// e.g. "TrafficLight.11_06" --> position 6
		String trafficLightName = trafficLight.getName();
		String trafficLightID = TrafficLight.parseTrafficLightID(trafficLightName);
		int position = Integer.parseInt(trafficLightID);
		
		// get SUMO state character from state string
		char stateCharacter = stateString.charAt(position);
		
		try 
		{	
			// converts a SUMO state character to a traffic light state (e.g. 'r' --> "RED")
			return XMLParser.parseSUMOStateCharacter(stateCharacter);
		}
		catch (InvalidStateCharacterException e) 
		{	
			e.printStackTrace();
			return TrafficLightState.OFF;
		}	
	}

}
