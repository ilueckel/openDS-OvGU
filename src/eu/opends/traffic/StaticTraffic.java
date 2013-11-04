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

package eu.opends.traffic;

import java.util.ArrayList;
import java.util.List;

import com.jme3.animation.LoopMode;
import com.jme3.cinematic.MotionPath;
import com.jme3.cinematic.MotionPathListener;
import com.jme3.cinematic.events.MotionTrack;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.math.Spline.SplineType;
import com.jme3.scene.Spatial;

import eu.opends.car.Car;
import eu.opends.main.Simulator;

/**
 * 
 * @author Rafael Math
 */
public class StaticTraffic 
{
	private Simulator sim;
    private ArrayList<TrafficCar> vehicleList = new ArrayList<TrafficCar>();
    private float steer = 0;
    private Spatial teapot;
    private MotionTrack motionControl;
    
    
    public ArrayList<TrafficCar> getVehicleList()
    {
    	return vehicleList;
    }


    public float getSpeed()
    {
    	float duration = motionControl.getInitialDuration();
    	float distanceMeters = motionPath.getLength();
    	float speed = distanceMeters / duration;
    	return 3.6f * speed;
    }
    
    
    public void setSpeed(float speedKmh)
    {
    	float distanceMeters = motionPath.getLength();
        float speed = speedKmh / 3.6f;
        float duration = distanceMeters / speed;
        motionControl.setInitialDuration(duration);
    }
    
    
    private MotionPath motionPath;
    
	public StaticTraffic(Simulator sim)
	{
		this.sim = sim;
		
		//---------------------------------------

		List<Vector3f> wayPoints = new ArrayList<Vector3f> ();
		wayPoints.add(new Vector3f(-57.553032f, 0f, -50.641f));
		wayPoints.add(new Vector3f(-58.458622f, 0f, 74.62811f));
		wayPoints.add(new Vector3f(-58.458633f, 0f, 84.24828f));
		wayPoints.add(new Vector3f(-182.1062f, 0f, 84.49099f));
		wayPoints.add(new Vector3f(-188.93419f, 0f, 83.57963f));
		wayPoints.add(new Vector3f(-193.49127f, 0f, 77.97089f));
		wayPoints.add(new Vector3f(-192.26537f, 0f, -42.02943f));
		wayPoints.add(new Vector3f(-192.26535f, 0f, -51.122475f));
		wayPoints.add(new Vector3f(-70.96055f, 0f, -50.526424f));

		
		motionPath = new MotionPath();

		motionPath.setCycle(false);
			
	       for(Vector3f wayPoint : wayPoints)
	    	   motionPath.addWayPoint(wayPoint);

	    motionPath.setPathSplineType(SplineType.CatmullRom); // --> default: CatmullRom
	    motionPath.setCurveTension(0.05f);
	    
	    motionPath.enableDebugShape(sim.getAssetManager(), sim.getSceneNode());

        motionPath.addListener(new MotionPathListener() 
        {
            public void onWayPointReach(MotionTrack control, int wayPointIndex) {
                if (motionPath.getNbWayPoints() == wayPointIndex + 1) {
                	System.err.println(control.getSpatial().getName() + "Finished!!! ");
                	setSpeed(20);
                } else {
                	System.err.println(control.getSpatial().getName() + " Reached way point " + wayPointIndex);
                	setSpeed(getSpeed() + 5f);
                }
            }
        });
	    

	    teapot = createTeapot() ;
	    motionControl = new MotionTrack(teapot,motionPath);
	    
	    float speedKmPerHour = 20;
	    setSpeed(speedKmPerHour);
	    
	    // move object along path considering rotation
        motionControl.setDirectionType(MotionTrack.Direction.PathAndRotation);
        
        // loop movement of object
        motionControl.setLoopMode(LoopMode.Loop);
        
        // rotate moving object
        motionControl.setRotation(new Quaternion().fromAngleNormalAxis(-FastMath.HALF_PI, Vector3f.UNIT_Y));
        
        // set moving object to position "20 seconds"
        //motionPath.interpolatePath(20, motionControl);

        // start movement
        motionControl.play();

        //---------------------------------------
		
		TrafficCarData trafficCarData = new TrafficCarData("car1", 800, 3.3f, 8.7f, 2.0f, true, 
				"Models/Ferrari_DFKI/Car.scene", null);
		vehicleList.add(new TrafficCar(sim, trafficCarData));
		
		

		sim.getInputManager().addMapping("play_stop", new KeyTrigger(KeyInput.KEY_Y));
        ActionListener acl = new ActionListener() {

            public void onAction(String name, boolean keyPressed, float tpf) {

                if (name.equals("play_stop") && keyPressed) {
                    if (playing) {
                        playing = false;
                        motionControl.pause();
                    } else {
                        playing = true;
                        motionControl.play();
                    }
                }
            }
        };

        sim.getInputManager().addListener(acl, "play_stop");

	}
boolean playing = true;
	
    private Spatial createTeapot() 
    {
        Material mat = new Material(sim.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
        mat.setFloat("Shininess", 1f);
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.Black);
        mat.setColor("Diffuse", ColorRGBA.DarkGray);
        mat.setColor("Specular", ColorRGBA.White.mult(0.6f));
        //Spatial teapot = sim.getAssetManager().loadModel("Models/Teapot/Teapot.obj");
        Spatial teapot = sim.getAssetManager().loadModel("Models/Truck/Truck.scene");
        teapot.setName("Teapot");
        teapot.setLocalScale(0.4f);
        //teapot.setMaterial(mat);
        sim.getSceneNode().attachChild(teapot);
        
        return teapot;
    }


    
	
	public void update(float tpf) 
	{
		for(TrafficCar vehicle : vehicleList)
		{
			controlSpeed(vehicle);
			
			controlSteering(vehicle);
			
			vehicle.update(vehicleList);
		}
			System.out.println(getSpeed());
	}

	
	private void controlSpeed(Car vehicle) 
	{
		if(vehicle.getCurrentSpeedKmh()<25)
			vehicle.setGasPedalIntensity(-1);
		else if(vehicle.getCurrentSpeedKmh()>30)
			vehicle.setBrakePedalPressIntensity(1);
		else
		{
			vehicle.setGasPedalIntensity(0);
			vehicle.setBrakePedalPressIntensity(0);
		}
	}
	

	private void controlSteering(Car vehicle) 
	{
		// TODO: replace joystick input with method following waypoints
		vehicle.steer(steer);		
		
		


		
	}


	public void steer(float f) 
	{
		steer = f;
	}

}
