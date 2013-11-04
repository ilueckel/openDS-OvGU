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

//import com.jme.image.Texture;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
//import com.jme.scene.state.TextureState;
//import com.jme.util.TextureManager;

import eu.opends.main.Simulator;


/**
 * This class represents a single traffic light which can be switched between 
 * different states. A traffic light may be equipped with arrow lights or just
 * with a circular light.
 * 
 * @author Rafael Math
 */
public class TrafficLight
{
	/**
	 * TrafficLightState indicates the combination of illuminated lights.
	 */
	public enum TrafficLightState
	{
		RED,GREEN,YELLOW,YELLOWRED,OFF,ALL;
		
		// builds state-specific part of the texture file name
		public String getStateString()
		{
			return "_" + this.toString().toLowerCase();
		}
	}

	
	/**
	 * TrafficLightDirection indicates whether the the traffic light has 
	 * arrow-shaped lights and to which direction they are pointing. 
	 */
	public enum TrafficLightDirection
	{
		LEFT,RIGHT,UP,NONE;
		
		// builds direction-specific part of the texture file name
		public String getDirectionString()
		{
			if(this == NONE)
				return "";
			else
				return "_" + this.toString().toLowerCase();
		}
	}
	
	
	private Simulator sim;
	private Spatial trafficLightObject;
	private String trafficLightObjectID;
	private String name;
	private String intersectionID;
	private TrafficLightState state;
	private TrafficLightDirection direction;
	private ArrayList<TrafficLight> requiresRedList = null;
	private TrafficLightPositionData positionData;
	

	public TrafficLight(Simulator sim, Spatial trafficLightObject, String name, 
			String trafficLightGroupID, TrafficLightState initialState, TrafficLightDirection direction)
	{
		this.sim = sim;
		this.trafficLightObject = trafficLightObject;
		this.trafficLightObjectID = trafficLightObject.getName();
		this.name = name;
		this.intersectionID = trafficLightGroupID;
		setState(initialState);
		setDirection(direction);
	}
	
	
	/**
	 * Traffic light properties will be parsed from the name of the traffic 
	 * light object. A valid name is TrafficLight.11_06.R, which will be parsed
	 * as TrafficLight.11_06 (name), 11 (intersectionID), RIGHT (direction)
	 * 
	 * @param sim
	 * 			instance of the simulator
	 * @param trafficLightObject
	 * 			traffic light object (spatial) to be parsed from the model
	 */
	public TrafficLight(Simulator sim, Spatial trafficLightObject)
	{
		this.sim = sim;
		this.trafficLightObject = trafficLightObject;
		this.trafficLightObjectID = trafficLightObject.getName();
		this.name = parseName(trafficLightObjectID);
		this.intersectionID = parseIntersectionID(name);

		// switch off all lights when a new traffic light is created
		setState(TrafficLightState.OFF);
		
		// set direction as given in the traffic light object of the model
		setDirection(parseDirection(trafficLightObjectID));
	}
	
	
	/**
	 * Returns the ID of the traffic light's underlying Blender object, 
	 * e.g. "TrafficLight.11_06.R"
	 * 
	 * @return
	 * 			name of the traffic light's Blender object ID
	 */
	public String getObjectID()
	{
		return trafficLightObjectID;
	}
	
	
	/**
	 * Returns the name of the traffic light, e.g. "TrafficLight.11_06"
	 * 
	 * @return
	 * 			name of traffic light
	 */
	public String getName()
	{
		return name;
	}
	
	
	/**
	 * Returns the local position of the traffic light
	 * 
	 * @return
	 * 			Local position of traffic light
	 */
	public Vector3f getLocalPosition()
	{
		return trafficLightObject.getLocalTranslation();
	}
	
	
	/**
	 * Returns the world position of the traffic light
	 * 
	 * @return
	 * 			World position of traffic light
	 */
	public Vector3f getWorldPosition()
	{
		return trafficLightObject.getWorldTranslation();
	}
	

	/**
	 * Returns the rotation of the traffic light
	 * 
	 * @return
	 * 			rotation of traffic light
	 */
	public Quaternion getRotation() 
	{
		return trafficLightObject.getWorldRotation();
	}
	
	
	/**
	 * Returns the ID of the intersection the traffic light is associated to, e.g. "11"
	 * 
	 * @return
	 * 			ID of the intersection where the traffic light is located
	 */
	public String getIntersectionID()
	{
		return intersectionID;
	}
	
	
	/**
	 * Returns the traffic light state, e.g. TrafficLightState.RED
	 * 
	 * @return
	 * 			traffic light state
	 */
	public TrafficLightState getState()
	{
		return state;
	}
	
	
	/**
	 * Changes the traffic light state immediately to the given value 
	 * (e.g. TrafficLightState.RED) by loading the corresponding texture
	 * file for the traffic light. 
	 * 
	 * @param state
	 * 			state to change to immediately
	 */
	public void setState(TrafficLightState state)
	{
		if(this.state != state)
		{
			this.state = state;
			
			updateTexture();
			
			// creates a string containing all current traffic light states
			// this string will be sent to external programs, i.e. Lightning
			TrafficLightCenter.updateGlobalStatesString();
		}
	}
	
	
	/**
	 * Returns the traffic light direction, e.g. TrafficLightDirection.RIGHT
	 * 
	 * @return
	 * 			traffic light direction
	 */
	public TrafficLightDirection getDirection()
	{
		return direction;
	}
	
	
	/**
	 * Changes the traffic light direction immediately to the given value 
	 * (e.g. TrafficLightDirection.RIGHT) by loading the corresponding texture
	 * file for the traffic light. 
	 * 
	 * @param direction
	 * 			direction to change to immediately
	 */
	public void setDirection(TrafficLightDirection direction)
	{
		if(this.direction != direction)
		{
			this.direction = direction;

			updateTexture();
		}
	}
	
	
	/**
	 * Returns a list of traffic lights which have to be red before 
	 * the current traffic light may be switched to green
	 * 
	 * @return
	 * 			List of traffic lights that are required to be red
	 */
	public ArrayList<TrafficLight> getTrafficLightRules()
	{
		return requiresRedList;
	}
	
	
	/**
	 * Sets the list of all traffic lights which are required to be red, before 
	 * the current traffic light may be switched to green. This traffic light 
	 * list is created using a string list of traffic light names.
	 * 
	 * @param requiresRedList_String
	 * 			String list containing the names of traffic lights that have to be red
	 */
	public void setTrafficLightRules(ArrayList<String> requiresRedList_String)
	{
		requiresRedList = stringListToTrafficLightList(requiresRedList_String);
	}	
	
	
	/**
	 * Returns the traffic light position data
	 * 
	 * @return
	 * 			traffic light position data
	 */
	public TrafficLightPositionData getPositionData()
	{
		return positionData;
	}
	
	
	/**
	 * Sets the traffic light position data
	 * 
	 * @param positionData
	 * 			traffic light position data
	 */
	public void setPositionData(TrafficLightPositionData positionData) 
	{
		this.positionData = positionData; 
	}
	

	/**
	 * Returns name of traffic light from traffic light object ID as named in model 
	 * by pruning the direction indicator. 
	 * E.g. TrafficLight.11_06.R  -->  TrafficLight.11_06
	 * 
	 * @param trafficLightObjectID
	 * 			traffic light object ID as named in model
	 * @return
	 * 			name of traffic light (without direction indicator)
	 */
	public static String parseName(String trafficLightObjectID)
	{
		String[] array = trafficLightObjectID.split("\\.");
		
		if(array.length >= 2)
			return array[0] + "." + array[1];
		else
			return array[0];
		
		// alternative 1:
		//return trafficLightObjectID.replace(".L", "").replace(".U", "").replace(".R", "");
		
		// alternative 2:
		//return trafficLight_string.substring(0,18); 
	}
	
	
	/**
	 * Returns the intersection ID from traffic light name
	 * E.g. TrafficLight.11_06  -->  11
	 * 
	 * @param trafficLightName
	 * 			traffic light name
	 * @return
	 * 			ID of the intersection where the traffic light is located
	 */
	public static String parseIntersectionID(String trafficLightName)
	{
		String[] array1 = trafficLightName.split("\\.");
		String[] array2 = array1[1].split("_");
		return array2[0];
	}
	
	
	/**
	 * Returns the traffic light ID from traffic light name
	 * E.g. TrafficLight.11_06  -->  06
	 * 
	 * @param trafficLightName
	 * 			traffic light name
	 * @return
	 * 			ID of the traffic light in this intersection
	 */
	public static String parseTrafficLightID(String trafficLightName)
	{
		String[] array1 = trafficLightName.split("\\.");
		String[] array2 = array1[1].split("_");
		return array2[1];
	}
	
	
	/**
	 * Returns the direction of the arrow-shaped lights (if applicable) from the traffic 
	 * light object ID, e.g. TrafficLight.11_06.R  -->  RIGHT
	 * 
	 * @param trafficLightObjectID
	 * 			traffic light name
	 * @return
	 * 			direction of arrow-shaped lights (left, right, up) or none 
	 */
	private TrafficLightDirection parseDirection(String trafficLightObjectID){
		
		if(trafficLightObjectID.endsWith(".L"))
			return TrafficLightDirection.LEFT;
		
		else if(trafficLightObjectID.endsWith(".R"))
			return TrafficLightDirection.RIGHT;
		
		else if(trafficLightObjectID.endsWith(".U"))
			return TrafficLightDirection.UP;

		else
			return TrafficLightDirection.NONE;
	}
	
	
	/**
	 * Updates the texture of the current traffic light according to the given state 
	 * and direction.
	 */
	private void updateTexture()
	{
		Spatial textureSpatial = ((Node) trafficLightObject).getChild(0);
		Material mat = new Material(sim.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
	    mat.setTexture("ColorMap",sim.getAssetManager().loadTexture(getTrafficLightTexture()));
	    mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
	    textureSpatial.setMaterial(mat);
	}

	
	/**
	 * Returns texture of the current traffic light according to the given state
	 * and direction
	 * @return
	 * 			texture to use with current traffic light
	 */
	private String getTrafficLightTexture()
	{
		// basic file path
		String filepath = "Textures/TrafficLight/trafficlight";
		
		// state specific extension
		String stateString = state.getStateString();
		
		// direction specific extension
		String directionString = "";
		
		// if traffic light is switched off, no distinction between arrow-shaped textures necessary
		if(state != TrafficLightState.OFF)
			directionString = direction.getDirectionString();		

        return filepath + stateString + directionString + ".tga";
	}
	

	/**
	 * This method converts a string list of traffic light names into an object list 
	 * containing the corresponding traffic lights by iteratively looking up traffic 
	 * light objects by name.
	 * 
	 * @param stringList
	 * 			string list containing names of traffic lights to look up
	 *  
	 * @return
	 * 			traffic light list containing all traffic lights as specified in input list
	 */
	private ArrayList<TrafficLight> stringListToTrafficLightList(ArrayList<String> stringList)
	{
		ArrayList<TrafficLight> trafficLightList = new ArrayList<TrafficLight>(10);
		
		for(String string : stringList)
		{
			TrafficLight trafficLight = TrafficLightCenter.getTrafficLightByName(string);
			if(trafficLight != null)
				trafficLightList.add(trafficLight);
		}
		
		return trafficLightList;
	}

}