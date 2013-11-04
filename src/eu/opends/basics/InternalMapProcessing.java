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

package eu.opends.basics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.shape.Box;

import eu.opends.main.Simulator;

/**
 * This class is used to further process the elements on the map.
 * 
 * @author Rafael Math
 */
public class InternalMapProcessing
{
	private SimulationBasics sim;
	private Node sceneNode;
	private PhysicsSpace physicsSpace;
	private List<Spatial> triggerList = new ArrayList<Spatial>();
	
	
	public InternalMapProcessing(SimulationBasics sim)
	{
		this.sim = sim;
		this.sceneNode = sim.getSceneNode();
		this.physicsSpace = sim.getPhysicsSpace();
		
		// get list of additional objects (generated from XML file)
		addMapObjectsToScene(Simulator.getDrivingTask().getSceneLoader().getMapObjects());

		System.out.println("MapModelList:  [" + listToString(sceneNode) + "]");

		// apply triggers to certain visible objects
		if (sim instanceof Simulator) 
		{
			generateTrafficLightTriggers();
			generateDrivingTaskTriggers();
			addTriggersToTriggerNode();
		}
	}

	
	private String listToString(Node sceneNode) 
	{
		String output = "";
        boolean isFirstChild = true;
        for(Spatial child : sceneNode.getChildren())
        {
        	if(isFirstChild)
        	{
        		output += child.getName();
        		isFirstChild = false;
        	}
        	else
        		output += ", " + child.getName();
        }
		return output;
	}


	private void addToPhysicsSpace(Spatial node) 
	{
		// We set up collision detection for the scene by creating a
        // compound collision shape and a static physics node with mass zero.
        CollisionShape sceneShape = CollisionShapeFactory.createMeshShape(node);
        RigidBodyControl sceneControl = new RigidBodyControl(sceneShape, 0);
        node.addControl(sceneControl);
        
        // attach the scene to the physics space
        physicsSpace.add(sceneControl);
	}

	
	
	/**
	 * Converts a list of map objects into a list of spatial objects which 
	 * can be added to the simulators scene graph.
	 * 
	 * @param mapObjects
	 * 			List of map objects to convert
	 * 
	 * @return
	 * 			List of spatial objects
	 */
	private void addMapObjectsToScene(List<MapObject> mapObjects)
	{			
		for(MapObject mapObject : mapObjects)
		{	
			Node node = new Node(mapObject.getName());
			
			Spatial spatial = mapObject.getSpatial();
			
        	// set FaceCullMode of spatial's geometries to off
			// no longer needed, as FaceCullMode.Off is default setting
			//Util.setFaceCullMode(spatial, FaceCullMode.Off);
			
	    	node.attachChild(spatial);
	    	
	    	
//*******************	TODO   REMOVE  	

	    	Spatial spat;
	    	if(spatial instanceof Node)
	    	{
	    		for (Iterator<Spatial> it = ((Node) spatial).getChildren().iterator(); it.hasNext();) 
	    		{
	    			spat = it.next();

	    			if(spat.getName().startsWith("TrafficLight."))
	    			{
	    				sceneNode.attachChild(spat);
	    				addToPhysicsSpace(spat);
	    				it.remove();
	    			}

	    			if (spat.getName().startsWith("Car.Driver") ||
	    				spat.getName().startsWith("Car.Reset") ||
	    				spat.getName().startsWith("Blind_") ||
	    				spat.getName().startsWith("Pos_") ||
	    				spat.getName().startsWith("Dyn.Cone") ||
	    				spat.getName().startsWith("Traffic.") ||
	    				spat.getName().startsWith("TrafficWP") ||
	    				spat.getName().startsWith("SpeedLimit_") ||
	    				spat.getName().startsWith("Caution_") ||
	    				spat.getName().startsWith("IdealPoint")
	    				) 
	    			{
	    				it.remove();
	    			}
	    		}
	    	}
	    	
//*******************	
	    	
	    	node.setLocalScale(mapObject.getScale());

	        node.updateModelBound();
	        
			// if marked as invisible then cull always else cull dynamic
			if(!mapObject.isVisible())
				node.setCullHint(CullHint.Always);
			
			String collisionShapeString = mapObject.getCollisionShape();
			if(collisionShapeString == null)
				collisionShapeString = "meshShape";
			
			// FIXME
			if(!mapObject.getName().startsWith("TrafficLight") && (collisionShapeString.equalsIgnoreCase("boxShape") ||
					collisionShapeString.equalsIgnoreCase("meshShape")))
			{
		        CollisionShape collisionShape;
		        float mass = mapObject.getMass();

		        if(mass == 0)
		        	// mesh shape for static objects
		        	collisionShape = CollisionShapeFactory.createMeshShape(node);
		        else
		        {
			        // set whether triangle accuracy should be applied
			        if(collisionShapeString.equalsIgnoreCase("meshShape"))
			        	collisionShape = CollisionShapeFactory.createDynamicMeshShape(node);
			        else
			        	collisionShape = CollisionShapeFactory.createBoxShape(node);
		        }		        
		        
		        RigidBodyControl physicsControl = new RigidBodyControl(collisionShape, mass);
		        node.addControl(physicsControl);

		        physicsControl.setPhysicsLocation(mapObject.getLocation());
		        physicsControl.setPhysicsRotation(mapObject.getRotation());
		        
		        //physicsControl.setFriction(100);
		        
		        // add additional map object to physics space
		        physicsSpace.add(physicsControl);
			}
			else
			{
				node.setLocalTranslation(mapObject.getLocation());
		        node.setLocalRotation(mapObject.getRotation());
			}
			
	        // attach additional map object to scene node
			sceneNode.attachChild(node);
		}
	}
	
	
	/**
	 * Generates traffic light triggers 20 meters before a traffic light.
	 * 
	 * @param blenderObjectsList
	 */
	private void generateTrafficLightTriggers() 
	{
		for(Spatial object : sceneNode.getChildren())
		{
			String objectName = object.getName();

			if (objectName.startsWith("TrafficLight")) 
			{
				Vector3f relativePos1,relativePos2;
				
				// get rotation of traffic light in origin (0,0,0) 
				Quaternion localRotation = object.getWorldRotation();
				
				// get translation from origin to traffic light
				Vector3f localTranslation = object.getLocalTranslation();
				
				
				// traffic light for green trigger
				// *******************************
				
				// set relative position of trigger to the traffic light
				if(objectName.endsWith(".L") || objectName.endsWith(".U"))
					relativePos1 = new Vector3f(-4f, 1.5f, 12f);
				else
					relativePos1 = new Vector3f(-2.7f, 1.5f, 12f);
				
				// create trigger in origin
				Box trigger1 = new Box(relativePos1, 0.4f, 0.2f, 12f);
				Spatial triggerBox1 = new Geometry("TrafficLightTrigger:" + objectName, trigger1);
				Material mat1 = new Material(sim.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
				mat1.setColor("Color", ColorRGBA.Green);
				triggerBox1.setMaterial(mat1);
				
				// rotate trigger in the same way as the traffic light in origin
				triggerBox1.setLocalRotation(localRotation);
				
				// move rotated trigger from origin to a location next to the traffic light
				triggerBox1.setLocalTranslation(localTranslation);

				// set properties of trigger
				triggerBox1.setModelBound(new BoundingBox());
				triggerBox1.setCullHint(CullHint.Always);
				triggerBox1.updateModelBound();

				// attach trigger to trigger list
				triggerList.add(triggerBox1);		

				
				// traffic light phase trigger
				// ***************************

				// set relative position of trigger to the traffic light
				if(objectName.endsWith(".L") || objectName.endsWith(".U"))
					relativePos2 = new Vector3f(-4f, 1.5f, 60f);
				else
					relativePos2 = new Vector3f(-2.7f, 1.5f, 60f);
				
				// create trigger in origin
				Box trigger2 = new Box(relativePos2, 0.4f, 0.2f, 50f);
				Spatial triggerBox2 = new Geometry("TrafficLightPhaseTrigger:" + objectName, trigger2);
				Material mat2 = new Material(sim.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
				mat2.setColor("Color", ColorRGBA.Red);
				triggerBox2.setMaterial(mat2);
				
				// rotate trigger in the same way as the traffic light in origin
				triggerBox2.setLocalRotation(localRotation);
				
				// move rotated trigger from origin to a location next to the traffic light
				triggerBox2.setLocalTranslation(localTranslation);
				
				// set properties of trigger
				triggerBox2.setModelBound(new BoundingBox());
				triggerBox2.setCullHint(CullHint.Always);
				triggerBox2.updateModelBound();
				
				// attach trigger to trigger list
				triggerList.add(triggerBox2);
			}
		}
	}
	
	
	/**
	 * Generates blind triggers which replace the original boxes.
	 * 
	 * @param blenderObjectsList
	 */
	private void generateDrivingTaskTriggers()
	{		
		for (Spatial object : sceneNode.getChildren()) 
		{
			if (SimulationBasics.getTriggerActionListMap().containsKey(object.getName())) 
			{
				// add trigger to trigger list
				triggerList.add(object);
			}
		}
	}
	
	
	private void addTriggersToTriggerNode()
	{
		for(Spatial object : triggerList)
		{
			// add trigger to trigger node
			//TODO use trigger node
			sim.getRootNode().attachChild(object);
		}
	}	
}
