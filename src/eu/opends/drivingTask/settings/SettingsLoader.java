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

package eu.opends.drivingTask.settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathConstants;

import org.w3c.dom.NodeList;

import eu.opends.drivingTask.DrivingTaskDataQuery;
import eu.opends.drivingTask.DrivingTaskDataQuery.Layer;
import eu.opends.input.KeyMapping;

/**
 * 
 * @author Rafael Math
 */
@SuppressWarnings("unchecked")
public class SettingsLoader
{
	private DrivingTaskDataQuery dtData;
	private Map<String,String[]> keyAssignmentMap = new HashMap<String,String[]>();

	public enum Setting
	{
		General_driverName("settings:general/settings:driverName"),
		General_showRearviewMirror("settings:general/settings:showRearviewMirror"),
		General_rearviewMirrorViewPortLeft("settings:general/settings:rearviewMirror/settings:viewPortLeft"),
		General_rearviewMirrorViewPortRight("settings:general/settings:rearviewMirror/settings:viewPortRight"),
		General_rearviewMirrorViewPortTop("settings:general/settings:rearviewMirror/settings:viewPortTop"),
		General_rearviewMirrorViewPortBottom("settings:general/settings:rearviewMirror/settings:viewPortBottom"),
		General_numberOfScreens("settings:general/settings:numberOfScreens"),
		General_angleBetweenAdjacentCameras("settings:general/settings:angleBetweenAdjacentCameras"),
		General_frameOfView("settings:general/settings:frameOfView"),
		General_showStats("settings:general/settings:showStats"),
		General_showAnalogIndicators("settings:general/settings:showAnalogIndicators"),
		General_showDigitalIndicators("settings:general/settings:showDigitalIndicators"),
		General_showFuelConsumption("settings:general/settings:showFuelConsumption"),
		Analyzer_suppressPDFPopup("settings:analyzer/settings:suppressPDFPopup"),
		SIMTD_sendDataToHmi("settings:SIMTD/settings:sendDataToHmi"),
		SIMTD_startGui("settings:SIMTD/settings:startGui"),
		SIMTD_hmiNativePath("settings:SIMTD/settings:hmiNativePath"),
		SIMTD_hmiNativeExecutable("settings:SIMTD/settings:hmiNativeExecutable"),
		SIMTD_ip("settings:SIMTD/settings:ip"),
		SIMTD_port("settings:SIMTD/settings:port"),
		ExternalVisualization_enableConnection("settings:externalVisualization/settings:enableConnection"),
		ExternalVisualization_ip("settings:externalVisualization/settings:ip"),
		ExternalVisualization_port("settings:externalVisualization/settings:port"),
		ExternalVisualization_updateRate("settings:externalVisualization/settings:updateRate"),
		ExternalVisualization_scalingFactor("settings:externalVisualization/settings:scalingFactor"),
		ExternalVisualization_sendPosOriAsOneString("settings:externalVisualization/settings:sendPosOriAsOneString"),
		CANInterface_enableConnection("settings:CANInterface/settings:enableConnection"),
		CANInterface_ip("settings:CANInterface/settings:ip"),
		CANInterface_port("settings:CANInterface/settings:port"),
		CANInterface_updateRate("settings:CANInterface/settings:updateRate"),
		CANInterface_maxSteeringAngle("settings:CANInterface/settings:maxSteeringAngle"),
		VsimrtiServer_startServer("settings:vsimrtiServer/settings:startServer"),
		VsimrtiServer_port("settings:vsimrtiServer/settings:port"),
		SettingsControllerServer_startServer("settings:settingsControllerServer/settings:startServer"),
		SettingsControllerServer_port("settings:settingsControllerServer/settings:port"),
		ReactionMeasurement_groupRed("settings:reactionMeasurement/settings:groupRed"),
		ReactionMeasurement_groupYellow("settings:reactionMeasurement/settings:groupYellow"),
		ReactionMeasurement_groupGreen("settings:reactionMeasurement/settings:groupGreen"),
		ReactionMeasurement_groupCyan("settings:reactionMeasurement/settings:groupCyan"),
		ReactionMeasurement_groupBlue("settings:reactionMeasurement/settings:groupBlue"),
		ReactionMeasurement_groupMagenta("settings:reactionMeasurement/settings:groupMagenta"),
		Joystick_controllerID("settings:controllers/settings:joystick/settings:controllerID"),
		Joystick_steeringAxis("settings:controllers/settings:joystick/settings:steeringAxis"),
		Joystick_invertSteeringAxis("settings:controllers/settings:joystick/settings:invertSteeringAxis"),
		Joystick_pedalAxis("settings:controllers/settings:joystick/settings:pedalAxis"),
		Joystick_invertPedalAxis("settings:controllers/settings:joystick/settings:invertPedalAxis"),
		Joystick_steeringSensitivityFactor("settings:controllers/settings:joystick/settings:steeringSensitivityFactor"),
		Joystick_pedalSensitivityFactor("settings:controllers/settings:joystick/settings:pedalSensitivityFactor"),
		Mouse_scrollSensitivityFactor("settings:controllers/settings:mouse/settings:scrollSensitivityFactor"),
		Mouse_minScrollZoom("settings:controllers/settings:mouse/settings:minScrollZoom"),
		Mouse_maxScrollZoom("settings:controllers/settings:mouse/settings:maxScrollZoom");
		
		private String path;
		
		Setting(){
			path = null;
		}
		
		Setting(String p){
			path = p;
		}
		
		public String getXPathQuery()
		{
			if(path!=null)
			{
				return "/settings:settings/"+path;
			}
			else
			{
				String[] array = this.toString().split("_");
				return "/settings:settings/settings:"+array[0]+"/settings:"+array[1];	
			}
		}
	}

	
	public SettingsLoader(DrivingTaskDataQuery dtData) 
	{
		this.dtData = dtData;
		loadKeyAssignments();
	}
	

	private void loadKeyAssignments() 
	{
		String path = "/settings:settings/settings:controllers/settings:keyboard/settings:keyAssignments/settings:keyAssignment";
		NodeList keyAssignmentNodes = (NodeList) dtData.xPathQuery(Layer.SETTINGS, 
				path, XPathConstants.NODESET);

		for (int k = 1; k <= keyAssignmentNodes.getLength(); k++) 
		{
			String function = dtData.getValue(Layer.SETTINGS, 
					path + "["+k+"]/@function", String.class);
			
			String keyList = dtData.getValue(Layer.SETTINGS, 
					path + "["+k+"]/@key", String.class).toUpperCase();
			
			if(!function.isEmpty())
			{
				if(!keyAssignmentMap.containsKey(function))
				{
					// insert key pair to keyAssignmentMap
					if(keyList.isEmpty())
					{
						// do not assign any key and remove default assignment 
						keyAssignmentMap.put(function, new String[]{});
						//System.err.println("A:" + function);
					}
					else
					{
						// assign a comma-separated list of keys
						String[] newKeys = keyList.split(",");
						
						for(int i = 0; i<newKeys.length; i++)
							newKeys[i] = newKeys[i].replace("KEY_", "");
						
						keyAssignmentMap.put(function, newKeys);
					}
				}
				else
				{
					// append key pair to keyAssignmentMap
					if(!keyList.isEmpty())
					{
						// assign a comma-separated list of keys
						String[] originalKeys = keyAssignmentMap.get(function);
						String[] newKeys = keyList.split(",");
						
						for(int i = 0; i<newKeys.length; i++)
							newKeys[i] = newKeys[i].replace("KEY_", "");
						
						String[] allKeys = joinArrays(originalKeys, newKeys);					    
						keyAssignmentMap.put(function, allKeys);
					}
				}
			}
		}		
	}

	
	private static String[] joinArrays(String [] ... arrays) 
	{
		// calculate size of target array
		int size = 0;
		for (String[] array : arrays) 
		  size += array.length;
		
		String[] result = new String[size];
		
		int j = 0;
		for (String[] array : arrays) 
		{
			for (String s : array)
				result[j++] = s;
		}
		
		return result;
	}
	

	/**
	 * Looks up the sub node (specified in parameter name) of the given element node
	 * and writes the data to the global variable with the same name. If this was 
	 * successful, the global variable "isSet_&lt;name&gt;" will be set to true. 
	 * 
	 */
	public <T> T getSetting(Setting setting, T defaultValue)
	{		
		try {
			
			Class<T> cast = (Class<T>) defaultValue.getClass();
			T returnvalue = (T) dtData.getValue(Layer.SETTINGS, setting.getXPathQuery(), cast);
			
			if(returnvalue == null)
				returnvalue = defaultValue;
			
			return returnvalue;

		} catch (Exception e2) {
			dtData.reportInvalidValueError(setting.toString(), dtData.getSettingsPath());
		}
		
		return defaultValue;
	}
	
	
	public List<KeyMapping> lookUpKeyMappings(ArrayList<KeyMapping> keyMappingList)
	{
		for(KeyMapping keyMapping : keyMappingList)
		{
			String function = keyMapping.getID();
			if(keyAssignmentMap.containsKey(function))
				keyMapping.setKeys(keyAssignmentMap.get(function));
		}
		
		return keyMappingList;
	}

}
