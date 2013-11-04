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

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;



/**
 * This class is a representation of traffic light rules. Rules can be loaded 
 * from external files and be forwarded to the internal traffic light objects.
 * 
 * @author Rafael Math
 */
public class TrafficLightRules{
	
	private static Map<String, ArrayList<String>> trafficLightRulesList = null;
	private static Map<String, LinkedList<TrafficLightPhase>> trafficLightPhaseList = null;
	private static Map<String, TrafficLightPositionData> trafficLightPositionDataList = null;
	
	
	/**
	 * Loads traffic light rules from XML-file, parses them and stores them 
	 * to the traffic light rules list. The XML-file (extension: *-tlr.xml) 
	 * is expected to be saved in the same directory with the same file name 
	 * as the driving task file (extension: *.xml).
	 * E.g. citymodel_001.xml  --->  citymodel_001-tlr.xml
	 * 
	 * @param drivingTaskFileName
	 * 			name of the map file
	 */
	public static void loadTrafficLightRules(String drivingTaskFileName)
	{
		// A traffic light rules file is expected to fit with originalname-tlr.xml
		String rulesFileName = drivingTaskFileName.replace(".xml", "-tlr.xml");
		File rulesFile = new File(rulesFileName);
		
		if(rulesFile.isFile())
		{
			XMLParser parser = new XMLParser(rulesFile);
			trafficLightRulesList = parser.getTrafficLightRules();
			trafficLightPhaseList = parser.getTrafficLightPhases();
			trafficLightPositionDataList = parser.getTrafficLightPositionData();
		}
		else
			System.err.println("Traffic light rules file '" + rulesFileName + "' not found!");
	}
	
	
	/**
	 * Returns the traffic light rules for the given traffic light
	 * 
	 * @param trafficLightName
	 * 			name of the traffic light
	 * 
	 * @return
	 * 			list of rules for the given traffic light
	 */
	public static ArrayList<String> getTrafficLightRules(String trafficLightName)
	{
		if(trafficLightRulesList != null)
			return trafficLightRulesList.get(trafficLightName);
		else
			return null;
	}
	
	
	/**
	 * Returns the traffic light phases for the given intersection
	 * 
	 * @param intersectionID
	 * 			ID of the intersection
	 * 
	 * @return
	 * 			list of phases for the given intersection
	 */
	public static LinkedList<TrafficLightPhase> getTrafficLightPhases(String intersectionID)
	{
		if(trafficLightPhaseList != null)
			return trafficLightPhaseList.get(intersectionID);
		else
			return null;
	}
	
	
	/**
	 * Returns the traffic light position data for the given traffic light
	 * 
	 * @param trafficLightName
	 * 			name of the traffic light
	 * 
	 * @return
	 * 			position data for the given traffic light
	 */
	public static TrafficLightPositionData getTrafficLightPositionData(String trafficLightName)
	{
		if(trafficLightPositionDataList != null)
			return trafficLightPositionDataList.get(trafficLightName);
		else
			return null;
	}
}