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

import java.util.List;

import com.jme3.animation.LoopMode;
import com.jme3.cinematic.MotionPath;
import com.jme3.cinematic.MotionPathListener;
import com.jme3.cinematic.events.MotionTrack;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.math.Spline.SplineType;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.shape.Box;

import eu.opends.main.Simulator;

/**
 * 
 * @author Rafael Math
 */
public class FollowBox 
{
	private Simulator sim;
	private TrafficCar vehicle;
	private FollowBoxSettings settings;
	private List<Waypoint> waypointList;
	private float maxDistance;
    private MotionPath motionPath;
    private MotionTrack motionControl;
    private Spatial followBox;
	private boolean setWayPoint = false;
	private int setToWayPointIndex = 0;

	
	public FollowBox(Simulator sim, final TrafficCar vehicle, FollowBoxSettings settings)
	{
		this.sim = sim;
		this.vehicle = vehicle;
		this.settings = settings;
		
		waypointList = settings.getWayPoints();
		maxDistance = settings.getMaxDistance();
		
		motionPath = new MotionPath();

		motionPath.setCycle(settings.isPathCyclic());
		
		for(Waypoint wayPoint : waypointList)
			motionPath.addWayPoint(wayPoint.getPosition());

	    motionPath.setPathSplineType(SplineType.CatmullRom); // --> default: CatmullRom
	    motionPath.setCurveTension(settings.getCurveTension());
	    
	    if(settings.isPathVisible())
	    	motionPath.enableDebugShape(sim.getAssetManager(), sim.getSceneNode());

        motionPath.addListener(new MotionPathListener() 
        {
            public void onWayPointReach(MotionTrack control, int wayPointIndex) 
            {
            	// set speed limit for next way point
            	int index = wayPointIndex % waypointList.size();
            	float speed = waypointList.get(index).getSpeed();
            	setSpeed(speed);
            	
            	// if last way point reached
                if (motionPath.getNbWayPoints() == wayPointIndex + 1) 
                {
                	// reset vehicle to first way point if not cyclic
                	if(!motionPath.isCycle())
                		setToWayPoint(0);
                }
            }
        });

	    followBox = createFollowBox() ;
	    motionControl = new MotionTrack(followBox,motionPath);
	    
	    // get start way point
	    int startWayPointIndex = settings.getStartWayPointIndex();
        
        // set start speed
	    float initialSpeed = waypointList.get(startWayPointIndex).getSpeed();
	    setSpeed(initialSpeed);
	    
	    // set start position
        setToWayPoint(startWayPointIndex);
	    
	    // move object along path considering rotation
        motionControl.setDirectionType(MotionTrack.Direction.PathAndRotation);
        
        // loop movement of object
        motionControl.setLoopMode(LoopMode.Loop);
        
        // rotate moving object
        //motionControl.setRotation(new Quaternion().fromAngleNormalAxis(-FastMath.HALF_PI, Vector3f.UNIT_Y));
        
        // set moving object to position "20 seconds"
        //motionPath.interpolatePath(20, motionControl);

        // start movement
        //motionControl.play(); // already contained in update method
	}
    
	
	private int frameCounter = 0;
	public void update(Vector3f vehiclePos)
	{
		if(setWayPoint)
		{
			// first iteration --> set control to play in order to make 
			// position change have effect
			if(frameCounter <= 0)
			{
				motionControl.play();
				frameCounter ++;
			}
			
			// second iteration --> set position of follow box
			else if(frameCounter == 1)
			{
				// set follow box to way point
				motionControl.setCurrentWayPoint(setToWayPointIndex);
				
				// place follow box at beginning of line currentWP-nextWP
				motionControl.setCurrentValue(0);
				
				frameCounter ++;
			}
			
			// third iteration --> reset counter & exit branch
			else if(frameCounter >= 2)
			{
				frameCounter = 0;
				setWayPoint = false;
			}
		}
		else
		{
			// pause movement of follower box if vehicle's distance
			// has exceeded maximum
			if(maxDistanceExceeded(vehiclePos) || sim.isPause())
				motionControl.pause();
			else
				motionControl.play();
		}
	}

	
	public void setToWayPoint(int index)
	{
		// set follow box to WP (performed in update())
		setWayPoint = true;
		setToWayPointIndex = index;
		
		// set position to vehicle
		Vector3f position = waypointList.get(index).getPosition();
		vehicle.setPosition(position);
		
		// set heading to vehicle
		float heading = getHeadingAtWP(index);
		Quaternion quaternion = new Quaternion().fromAngles(0, heading, 0);
		vehicle.setRotation(quaternion);
	}
	
	
	public int getIndexOfWP(String wayPointID) 
	{
		for(int i=0; i<waypointList.size(); i++)
			if(waypointList.get(i).getName().equals(wayPointID))
				return i;
		return -1;
	}

	
	public float getHeadingAtWP(int index) 
	{
		float heading = 0;
		Waypoint nextWayPoint = getNextWayPoint(index);
		
		// if next way point available, compute heading towards it
		if(nextWayPoint != null)
		{
			// compute driving direction by looking at next way point from current position 
			Vector3f targetPosition = nextWayPoint.getPosition().clone();
			targetPosition.setY(0);
			
			Vector3f currentPosition = waypointList.get(index).getPosition().clone();
			currentPosition.setY(0);
			
			Vector3f drivingDirection = targetPosition.subtract(currentPosition).normalize();

			// compute heading (orientation) from driving direction vector for
			// angle between driving direction and heading "0"
			float angle0  = drivingDirection.angleBetween(new Vector3f(0,0,-1));
			// angle between driving direction and heading "90"
			float angle90 = drivingDirection.angleBetween(new Vector3f(1,0,0));
			
			// get all candidates for heading
			// find the value from {heading1,heading2} which matches with one of {heading3,heading4}
			float heading1 = (2.0f * FastMath.PI + angle0)  % FastMath.TWO_PI;
			float heading2 = (2.0f * FastMath.PI - angle0)  % FastMath.TWO_PI;
			float heading3 = (2.5f * FastMath.PI + angle90) % FastMath.TWO_PI;
			float heading4 = (2.5f * FastMath.PI - angle90) % FastMath.TWO_PI;
			
			float diff_1_3 = FastMath.abs(heading1-heading3);
			float diff_1_4 = FastMath.abs(heading1-heading4);
			float diff_2_3 = FastMath.abs(heading2-heading3);
			float diff_2_4 = FastMath.abs(heading2-heading4);
			
			if((diff_1_3 < diff_1_4 && diff_1_3 < diff_2_3 && diff_1_3 < diff_2_4) ||
				(diff_1_4 < diff_1_3 && diff_1_4 < diff_2_3 && diff_1_4 < diff_2_4))
			{
				// if diff_1_3 or diff_1_4 are smallest --> the correct heading is heading1
				heading = heading1;
			}
			else
			{
				// if diff_2_3 or diff_2_4 are smallest --> the correct heading is heading2
				heading = heading2;
			}
		}
		return heading;
	}
	

	public Waypoint getCurrentWayPoint() 
	{
		int currentIndex = motionControl.getCurrentWayPoint();
		return waypointList.get(currentIndex);
	}


	public Waypoint getNextWayPoint() 
	{
		int currentIndex = motionControl.getCurrentWayPoint();
		return getNextWayPoint(currentIndex);
	}
	

	public Waypoint getNextWayPoint(int index) 
	{
		Waypoint nextWayPoint = null;
		
		if(motionPath.isCycle())
		{
			// if path is cyclic, the successor of the last WP will be the first WP
			nextWayPoint = waypointList.get((index+1) % waypointList.size());
		}
		else if(motionPath.getNbWayPoints() > index+1)
		{
			// if not cyclic, only successors for way points 0 .. n-1 exist
			nextWayPoint = waypointList.get(index+1);
		}
		
		return nextWayPoint;
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


	public Vector3f getPosition() 
	{
		return followBox.getWorldTranslation();
	}


	public MotionTrack getMotionControl() 
	{
		return motionControl;
	}

    
    private Spatial createFollowBox() 
    {
		// add spatial representing the position the driving car is steering towards
		Box box = new Box(new Vector3f(0, 0, 0), 1f, 1f, 1f);
		Geometry followBox = new Geometry("followBox", box);
		followBox.setLocalTranslation(0, 0, 0);
		Material followBoxMaterial = new Material(sim.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		followBoxMaterial.setColor("Color", ColorRGBA.Green);
		followBox.setMaterial(followBoxMaterial);
        followBox.setLocalScale(0.4f);
        sim.getSceneNode().attachChild(followBox);
        
        if(!settings.isPathVisible())
        	followBox.setCullHint(CullHint.Always);
        	
        return followBox;
    }
    
    
	private boolean maxDistanceExceeded(Vector3f vehiclePos) 
	{
		// get box's position on xz-plane (ignore y component)
		Vector3f followBoxPosition = getPosition();
		followBoxPosition.setY(0);
		
		// get vehicle's position on xz-plane (ignore y component)
		Vector3f vehiclePosition = vehiclePos;
		vehiclePosition.setY(0);
		
		// distance between box and vehicle
		float currentDistance = followBoxPosition.distance(vehiclePosition);
		
		// report whether maximum distance is exceeded 
		return currentDistance > maxDistance;
	}


	public float getReducedSpeed()
	{
		// return a temporarily reduced speed for the traffic car
		// in order to reach next (lower) speed limit in time
		float reducedSpeedInKmh = Float.POSITIVE_INFINITY;
		
		// if next way point with lower speed comes closer --> reduce speed
		int currentIndex = motionControl.getCurrentWayPoint();
		Waypoint nextWP = getNextWayPoint(currentIndex);
		if(nextWP != null)
		{
			// current way point (already passed)
			Waypoint curentWP = waypointList.get(currentIndex);
			
			// speed at current way point
			float currentSpeedInKmh = curentWP.getSpeed();
			float currentSpeed = currentSpeedInKmh / 3.6f;
			
			// speed at next way point
			float targetSpeedInKmh = nextWP.getSpeed();
			float targetSpeed = targetSpeedInKmh / 3.6f;
			
			// if speed at the next WP is lower than at the current WP --> brake vehicle
			if(targetSpeed < currentSpeed)
			{
				// % of traveled distance between current and next way point
				float wayPercentage = motionControl.getCurrentValue();
				
				// distance between current and next way point
				Vector3f currentPos = curentWP.getPosition().clone();
				currentPos.setY(0);
				Vector3f nextPos = nextWP.getPosition().clone();
				nextPos.setY(0);
				float distance = currentPos.distance(nextPos);
				
				// distance (in meters) between follow box and next way point
				float distanceToNextWP = (1 - wayPercentage) * distance;
			
				// speed difference in m/s between current WP's speed and next WP's speed
				float speedDifference = currentSpeed - targetSpeed;
				
				// compute the distance in front of the next WP at what the vehicle has to start 
				// braking with 50% brake force in order to reach the next WP's (lower) speed in time.
				float deceleration50Percent = 50f * vehicle.getMaxBrakeForce()/vehicle.getMass();
				
				// time in seconds needed for braking process
				float time = speedDifference / deceleration50Percent;
				
				// distance covered during braking process
				float coveredDistance = 0.5f * -deceleration50Percent * time * time + currentSpeed * time;

				// start braking in x meters
				float distanceToBrakingPoint = distanceToNextWP - coveredDistance;
				
				if(distanceToBrakingPoint < 0)
				{
					// reduce speed linearly beginning from braking point
					
					// % of traveled distance between braking point and next way point
					float speedPercentage = -distanceToBrakingPoint/coveredDistance;
					
					//   0% traveled: reduced speed = currentSpeed
					//  50% traveled: reduced speed = (currentSpeed+targetSpeed)/2
					// 100% traveled: reduced speed = targetSpeed
					float reducedSpeed = currentSpeed - (speedPercentage * speedDifference);
					reducedSpeedInKmh = reducedSpeed * 3.6f;
					
					/*
					if(vehicle.getName().equals("car1"))
					{
						float vehicleSpeedInKmh = vehicle.getLinearSpeedInKmh();
						System.out.println(curentWP.getName() + " : " + speedPercentage + " : " + 
								reducedSpeedInKmh + " : " + vehicleSpeedInKmh + " : " + targetSpeedInKmh);
					}
					*/
				}
			}
		}
		return reducedSpeedInKmh;
	}

}
