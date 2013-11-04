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


import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.InputSource;

import eu.opends.canbus.CANClient;
import eu.opends.car.Car;
import eu.opends.environment.TrafficLight.*;
import eu.opends.environment.TrafficLightException.InvalidStateCharacterException;
import eu.opends.main.Simulator;
import eu.opends.tools.PanelCenter;



/**
 * XMLParser allows XML parsing of strings and files in order to evaluate 
 * traffic light instructions either manually generated or by the external 
 * SUMO traffic simulator. Furthermore this class provides a method to parse 
 * files that contain traffic light rules, traffic light phases and traffic 
 * light position data.
 * 
 * @author Rafael Math
 */
public class XMLParser
{
	private Document doc;
	private String xmlstring;
	private boolean errorOccured = false;
	
	
	/**
	 * Creates a DOM-object from the given input string. If the input string 
	 * is not a valid XML string, a warning message will be returned.
	 * 
	 * @param xmlstring
	 * 			XML input string to parse
	 */
	public XMLParser(String xmlstring)
	{		
		this.xmlstring = xmlstring;
		
		try {
	        InputSource xmlsource = new InputSource();
	        xmlsource.setCharacterStream(new StringReader(xmlstring));

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();

			doc = db.parse(xmlsource);

		} catch (Exception e) {
			System.err.println("[WARNING]: Malformed XML input (XMLParser.java): " + xmlstring);
			errorOccured = true;
		}
	}
	
	
	/**
	 * Creates a DOM-object from the given input file. If the input file 
	 * does not contain a valid XML string, a warning message will be returned.
	 * 
	 * @param xmlfile
	 * 			XML input file to parse
	 */
	public XMLParser(File xmlfile) 
	{
		this.xmlstring = xmlfile.getPath();
		
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();

			doc = db.parse(xmlfile);

		} catch (Exception e) {
			System.err.println("[WARNING]: Malformed XML input (XMLParser.java): " + xmlstring);
			errorOccured = true;
		}
	}
	

	/**
	 * Searches the given XML string for traffic light instructions. External
	 * instructions from the SUMO traffic simulator and manual instructions
	 * will be distinguished.
	 */
	public void evalTrafficLightInstructions()
	{
		// write instruction to console
		System.out.println("XML-String: " + xmlstring);
		
		if(!errorOccured)
		{		
			// go down to 2nd level of the hierarchy to distinguish between SUMO or manual input
			// <TrafficLightControl><tlsstate .../></TrafficLightControl>            --> SUMO
			// <TrafficLightControl><TrafficLight.xx_xx ...></TrafficLightControl>   --> manual
			
			NodeList nodeLst = doc.getElementsByTagName("TrafficLightControl");
			Node node = nodeLst.item(0);
			NodeList trafficLightInstructionList = node.getChildNodes();
	
			for(int i=0; i<trafficLightInstructionList.getLength(); i++)
			{
				Element trafficLightInstruction = (Element) trafficLightInstructionList.item(i);
				
				// if <tlsstate .../> was found --> SUMO instruction
				if(trafficLightInstruction.getNodeName().equals("tlsstate"))				
					evalSUMOInstruction(trafficLightInstruction);
				else
					evalManualInstruction(trafficLightInstruction);
			}
		}
	}
	
	
	/**
	 * Searches the given XML string for CAN-bus instructions like steering 
	 * wheel angle, status of brake and gas pedal. Settings of the car will
	 * be adjusted
	 */
	public void evalCANInstruction(Simulator sim, CANClient canClient) 
	{
		if(!errorOccured)
		{		
			// example CAN-bus instructions:
			//<message><action name="steering">-92</action></message>
			//<message><action name="acceleration">0.5</action></message>
			//<message><action name="brake">0.3</action></message>
			//<message><action name="button">cs</action></message>
			//<message><action name="button">return</action></message>
			
			Car car = sim.getCar();
			
			NodeList nodeLst = doc.getElementsByTagName("message");
			for(int i=0; i<nodeLst.getLength(); i++)
			{
				Node currentNode = nodeLst.item(i);			
				NodeList actionList = ((Element) currentNode).getElementsByTagName("action");
		
				for(int j=0; j<actionList.getLength(); j++)
				{
					try{
						
						Element currentAction = (Element) actionList.item(j);
						
						String actionID = currentAction.getAttribute("name");	
						String valueString = getCharacterDataFromElement(currentAction);

						// performs a steering input
						if(actionID.equals("steering"))
						{
							float value = Float.parseFloat(valueString);
							System.out.println("Steering: " + value);
							
							// for Sim-TD Smart
							canClient.setSteeringAngle(-value);
							
							// for Mercedes R-class
							//canClient.setSteeringAngle(value);
							
							sim.getSteeringTask().setSteeringIntensity(-0.02f*value);
						}
						
						// performs "cruise forward"-button
						else if(actionID.equals("MFLplus_State") || actionID.equals("MFLtelefoneEnd_State"))	
						{
							int value = Integer.parseInt(valueString);
							System.out.println("Gas: " + value);
							if(value == 0)
								car.setGasPedalIntensity(0);
								//car.releaseAccel();
							else
								car.setGasPedalIntensity(-1);
						}
						
						// performs "cruise forward"-button
						else if(actionID.equals("acceleration"))	
						{
							float value = Float.parseFloat(valueString);
							System.out.println("Gas: " + value);
							value = value*6;
							if(value <= 0)
								car.setGasPedalIntensity(0);
								//car.releaseAccel();
							else
							{
								car.setGasPedalIntensity(Math.max(-value,-1.0f));
								sim.getSteeringTask().getPrimaryTask().reportGreenLight();
							}
						}
						
						// performs "cruise backward"-button
						else if(actionID.equals("MFLminus_State"))	
						{
							int value = Integer.parseInt(valueString);
							System.out.println("Back: " + value);
							if(value == 0)
								car.setGasPedalIntensity(0);
								//car.releaseAccel();
							else
								car.setGasPedalIntensity(1);
						}
						
						// performs brake pedal
						else if(actionID.equals("KL54_RM_State"))	
						{
							int value = Integer.parseInt(valueString);
							System.out.println("Brake: " + value);
							if(value == 0)
								//car.setGasPedalIntensity(0);
								car.setBrakePedalPressIntensity(0);
								//car.releaseAccel();
							else
								car.setBrakePedalPressIntensity(1); // 1 --> full braking
						}
						
						// performs brake pedal
						else if(actionID.equals("brake"))	
						{
							float value = Float.parseFloat(valueString);
							System.out.println("Brake: " + value);
							if(value <= 0)
								//car.setGasPedalIntensity(0);
								car.setBrakePedalPressIntensity(0);
								//car.releaseAccel();
							else
							{
								car.setBrakePedalPressIntensity(Math.min(value,1.0f)); // 1 --> full braking
								sim.getSteeringTask().getPrimaryTask().reportRedLight();
							}
						}
						
						// performs "change view"-button
						else if(actionID.equals("button") && valueString.equals("cs"))	
						{
							System.out.println("Change view");
							sim.getCameraFactory().changeCamera();
						}

						// performs "reset car"-button
						else if(actionID.equals("button") && valueString.equals("return"))	
						{
							System.out.println("Reset car");
							car.setToNextResetPosition();
						}
						
						// shows message on display
						else if(actionID.equals("display"))	
						{
							int duration;
							try{
								duration = Integer.parseInt(currentAction.getAttribute("duration"));
							} catch(Exception e){
								duration = 0;
							}
							PanelCenter.getMessageBox().addMessage(valueString,duration);
						}
						
					} catch(Exception e){
						e.printStackTrace();
					}
				}
			}
		}
		
	}
	
	
	/**
	 * Parses the given XML input for traffic light rules and returns them as Map.
	 * In this context, traffic light rules means an individual list for every 
	 * traffic light containing all those traffic lights which have to be red 
	 * before this one may be switched to green.
	 * 
	 * @return
	 * 			Map of traffic light names associated with a list of traffic lights 
	 * 			that have to be red, before this traffic light may be switched to green.
	 */
	public Map<String, ArrayList<String>> getTrafficLightRules()
	{	
		// Structure:
		// <TrafficLightRules>
		//		<Intersection ID="08">
		//			<TrafficLight ID="05">
		//				<RequiresRed>01</RequiresRed>
		
		if(errorOccured)
			return null;

		try {
			
			Map<String, ArrayList<String>> trafficLightRulesList = new HashMap<String, ArrayList<String>>(100);
			
			NodeList nodeLst = doc.getElementsByTagName("TrafficLightRules");
			Node node = nodeLst.item(0);
			
			// if no traffic light rules given
			if(node == null)
				return null;
			
			NodeList intersectionList = ((Element) node).getElementsByTagName("Intersection");
	
			for(int i=0; i<intersectionList.getLength(); i++)
			{
				Element currentIntersection = (Element) intersectionList.item(i);
				
				String intersectionID = currentIntersection.getAttribute("ID");				
				NodeList trafficLightList = ((Element) currentIntersection).getElementsByTagName("TrafficLight");

				for(int j=0; j<trafficLightList.getLength(); j++)
				{
					Element currentTrafficLight = (Element) trafficLightList.item(j);
					
					String trafficLightID = currentTrafficLight.getAttribute("ID");
					NodeList requiresRedList = ((Element) currentTrafficLight).getElementsByTagName("RequiresRed");

					ArrayList<String> returnList = new ArrayList<String>(10);
					for(int k=0; k<requiresRedList.getLength(); k++)
					{
						Element currentRequiresRed = (Element) requiresRedList.item(k);
						
						String requiresRedID = getCharacterDataFromElement(currentRequiresRed);

						returnList.add("TrafficLight." + intersectionID + "_" + requiresRedID);
					}
					trafficLightRulesList.put("TrafficLight." + intersectionID + "_" + trafficLightID, returnList);
				}
			}
			
			return trafficLightRulesList;
			
			
		} catch (Exception e) {
			System.err.println(e.toString());
			return null;
		}

	}
	
	
	/**
	 * Parses the given XML input for traffic light phases and returns them as Map.
	 * Each intersection has its own list of phases. Each phase of an intersection
	 * should be named with an individual ID, which will be used to detect whether
	 * a phase has just been passed in the past. Furthermore each phase has a duration
	 * time (usually in seconds) and a state string representing the traffic light 
	 * configuration of an intersection when this phase is active.
	 * 
	 * @return
	 * 			Map of intersectionIDs associated with the corresponding list of 
	 * 			traffic light phases.
	 */
	public Map<String, LinkedList<TrafficLightPhase>> getTrafficLightPhases() 
	{
		// Structure:
		// <TrafficLightPhases>
		//		<Intersection ID="08">
		//			<Phase ID="01" duration="31" state="GGggrrrrGGggrrrr"/>
		
		if(errorOccured)
			return null;

		try {
			
			Map<String, LinkedList<TrafficLightPhase>> trafficLightPhasesList = 
				new HashMap<String, LinkedList<TrafficLightPhase>>(100);
			
			NodeList nodeLst = doc.getElementsByTagName("TrafficLightPhases");
			Node node = nodeLst.item(0);
			
			// if no traffic light phases given
			if(node == null)
				return null;
			
			NodeList intersectionList = ((Element) node).getElementsByTagName("Intersection");
	
			for(int i=0; i<intersectionList.getLength(); i++)
			{
				Element currentIntersection = (Element) intersectionList.item(i);
				
				String intersectionID = currentIntersection.getAttribute("ID");				
				NodeList phaseList = ((Element) currentIntersection).getElementsByTagName("Phase");

				LinkedList<TrafficLightPhase> returnList = new LinkedList<TrafficLightPhase>();
				for(int j=0; j<phaseList.getLength(); j++)
				{
					Element currentPhase = (Element) phaseList.item(j);
					
					String phaseID = currentPhase.getAttribute("ID");
					int phaseDuration = Integer.parseInt(currentPhase.getAttribute("duration"));
					String phaseState = currentPhase.getAttribute("state");
					
					returnList.add(new TrafficLightPhase(phaseID, phaseDuration, phaseState));
				}
				trafficLightPhasesList.put(intersectionID, returnList);
			}
			
			return trafficLightPhasesList;
			
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	/**
	 * Parses the given XML input for traffic light position data and returns them 
	 * as Map. Each intersection has its own configuration for traffic light positioning 
	 * data, where an arbitrary number of roads leading to this intersection may be 
	 * defined. Each road is assigned to an individual roadID, a type of crossing and
	 * an arrow configuration (integer values according to the SIM-TD specification).
	 * The lane information will be extracted from the TrafficLight0, TrafficLight1 
	 * and TrafficLight2 values which contain the ID of the corresponding traffic light
	 * at lane 0,1 or 2 respectively
	 * 
	 * @return
	 * 			Map of traffic light IDs associated with the corresponding traffic light
	 * 			position data.
	 */
	public Map<String, TrafficLightPositionData> getTrafficLightPositionData()
	{	
		// Structure:
		// <TrafficLightPosition>
		//		<Intersection ID="08">
		//			<Road ID ="00" type="4" arrow="9">
		//				<TrafficLight0 ID="02">
		//				<TrafficLight1 ID="04">
		//			</Road>
		
		if(errorOccured)
			return null;

		try {
			
			Map<String, TrafficLightPositionData> trafficLightPositionList = new HashMap<String, TrafficLightPositionData>(100);
			
			NodeList nodeLst = doc.getElementsByTagName("TrafficLightPosition");
			Node node = nodeLst.item(0);
			
			// if no traffic light positions given
			if(node == null)
				return null;
			
			NodeList intersectionList = ((Element) node).getElementsByTagName("Intersection");
	
			for(int i=0; i<intersectionList.getLength(); i++)
			{
				Element currentIntersection = (Element) intersectionList.item(i);
				
				String intersectionID = currentIntersection.getAttribute("ID");				
				NodeList roadList = ((Element) currentIntersection).getElementsByTagName("Road");

				for(int j=0; j<roadList.getLength(); j++)
				{
					Element currentRoad = (Element) roadList.item(j);
					
					String roadID = currentRoad.getAttribute("ID");
					int crossingType  = Integer.parseInt(currentRoad.getAttribute("type"));
					int arrowType = Integer.parseInt(currentRoad.getAttribute("arrow"));
					
					for(int position=0; position<=2; position++)
					{
						NodeList trafficLightList = ((Element) currentRoad).getElementsByTagName("TrafficLight" + position);
						for(int k=0; k<trafficLightList.getLength(); k++)
						{
							Element currentTrafficLight = (Element) trafficLightList.item(k);
							String trafficLightID = currentTrafficLight.getAttribute("ID");
							
							TrafficLightPositionData positionData = new TrafficLightPositionData(roadID,crossingType,arrowType,position);

							trafficLightPositionList.put("TrafficLight." + intersectionID + "_" + trafficLightID, positionData);
						}
					}
				}
			}
			
			return trafficLightPositionList;
			
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}
	
	
	/**
	 * Evaluates a SUMO instruction and sets the model's traffic light states 
	 * according to the given intersection ID (attribute "id"), the traffic 
	 * light ID (position in attribute "state") and state (value at position 
	 * in attribute "state").
	 * 
	 * <pre> &lt;TrafficLightControl&gt;
	 *     &lt;tlsstate timeR="178.00" id="0" programID="0" phase="6" state="rrrryyggrrrryygg"/&gt;
	 * &lt;/TrafficLightControl&gt;</pre>
	 * 
	 * @param trafficLightInstruction
	 * 			instruction string from SUMO (must match to the string above)
	 */
	private void evalSUMOInstruction(Element trafficLightInstruction)
	{
		try{
			
			// read state string and id string from SUMO instruction
			String stateString = trafficLightInstruction.getAttribute("state");
			String idString    = trafficLightInstruction.getAttribute("id");
			String intersectionID = String.format("%2s", idString).replace(' ', '0');
	
			for(int i=0; i<stateString.length(); i++)
			{
				// get traffic light object from intersection ID and traffic light ID
				String trafficlightID     = String.format("%2s", i).replace(' ', '0');
				String trafficLightName   = "TrafficLight." + intersectionID + "_" + trafficlightID;
				TrafficLight trafficLight = TrafficLightCenter.getTrafficLightByName(trafficLightName);
	
				if(trafficLight != null)
				{
					// assign state to traffic light object
					TrafficLightState state = parseSUMOStateCharacter(stateString.charAt(i));
					trafficLight.setState(state);
				}
			}
			
		} catch (InvalidStateCharacterException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Evaluates a (manual) XML instruction and sets the given traffic lights' 
	 * states to the given state values.
	 * 
	 * <pre> &lt;TrafficLightControl&gt;
	 *     &lt;TrafficLight.00_12&gt;
	 *         &lt;status>GREEN&lt;/status&gt;
	 *     &lt;/TrafficLight.00_12&gt;
	 *     ...
	 * &lt;/TrafficLightControl&gt;</pre>
	 * 
	 * 
	 * @param trafficLightInstruction
	 * 			manual instruction string (must match to the string above)
	 */
	private void evalManualInstruction(Element trafficLightInstruction)
	{
		try{
			
			// get traffic light object from XML
			String trafficLightName = trafficLightInstruction.getNodeName();
			TrafficLight trafficLight = TrafficLightCenter.getTrafficLightByName(trafficLightName);

			if(trafficLight != null)
			{
				// get traffic light state from XML
				NodeList stateList = trafficLightInstruction.getElementsByTagName("status");
				Element stateElement = (Element) stateList.item(0);
				String stateString = getCharacterDataFromElement(stateElement);
				TrafficLightState state = parseManualStateString(stateString);
				
				// assign state to traffic light object
				trafficLight.setState(state);
			}
			
		} catch (Exception e) {
			System.err.println(e.toString());
		}
	}
	
	
	/**
	 * Transforms SUMO's character representation of states to a value of
	 * TrafficLightState. E.g.  'r' --> TrafficLightState.RED
	 * 
	 * @param stateChar
	 * 			A SUMO state character
	 * 
	 * @return
	 * 			The corresponding traffic light state representation
	 * 
	 * @throws Exception 
	 * 			Exception will be thrown on invalid character input
	 */
	public static TrafficLightState parseSUMOStateCharacter(char stateChar) throws InvalidStateCharacterException
	{
		switch (stateChar){
			case 'G' : return TrafficLightState.GREEN;
			case 'g' : return TrafficLightState.GREEN;
			case 'y' : return TrafficLightState.YELLOW;
			case 'r' : return TrafficLightState.RED;
			case 'x' : return TrafficLightState.YELLOWRED;
			case 'o' : return TrafficLightState.OFF;
			case 'a' : return TrafficLightState.ALL;			
		}
		
		throw new InvalidStateCharacterException("Invalid character data: '" + stateChar + "'");
	}
	
	
	/**
	 * Returns character data from XML element.
	 * E.g. &lt;elem&gt;abc123&lt;/elem&gt;  --> "abc123"
	 * 
	 * @param elem
	 * 			XML Element
	 * 
	 * @return
	 * 			string representation of the given element
	 * 
	 * @throws Exception
	 * 			Exception will be thrown if no character data available
	 */
	public static String getCharacterDataFromElement(Element elem) throws Exception 
	{
		Node child = elem.getFirstChild();
		if (child instanceof CharacterData) 
		{
			CharacterData cd = (CharacterData) child;
			return cd.getData();
		}
		
		throw new Exception("No character data given");
	}
	
	
	/**
	 * Transforms the string representation of states to a value of
	 * TrafficLightState. E.g.  "green" --> TrafficLightState.GREEN
	 * 
	 * @param stateString
	 * 			The string representation of the state
	 * 
	 * @return
	 * 			The corresponding traffic light state representation
	 * 
	 * @throws Exception 
	 * 			Exception will be thrown on invalid string input
	 */
	private TrafficLightState parseManualStateString(String stateString) throws Exception
	{
		try{
			return TrafficLightState.valueOf(stateString.toUpperCase());
		} catch (IllegalArgumentException e){
			throw new Exception("Invalid character data: '" + stateString + "'");
		}
	}


}