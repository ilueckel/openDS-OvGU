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

package eu.opends.camera;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;

import eu.opends.car.Car;
import eu.opends.main.Simulator;

/**
 * 
 * @author Rafael Math
 */
public class SimulatorCam extends CameraFactory 
{	
	private Car car;
	private Node carNode;
	
	
	public SimulatorCam(Simulator sim, Car car) 
	{	    
		this.car = car;
		carNode = car.getCarNode();
		
		initCamera(sim, carNode);		
		setCamMode(CameraMode.EGO);
	}

	
	public void setCamMode(CameraMode mode)
	{
		switch (mode)
		{
			case EGO:
				camMode = CameraMode.EGO;
				chaseCam.setEnabled(false);
				setCarVisible(false);
				mainCameraNode.getChild("CamNode1").getControl(0).setEnabled(true);
				mainCameraNode.setLocalTranslation(car.getEgoCamPos());
				mainCameraNode.setLocalRotation(new Quaternion().fromAngles(0, 0, 0));
				break;
	
			case CHASE:
				camMode = CameraMode.CHASE;
				chaseCam.setEnabled(true);
				chaseCam.setDragToRotate(false);
				setCarVisible(true);
				mainCameraNode.getChild("CamNode1").getControl(0).setEnabled(false);
				break;
	
			case TOP:
				camMode = CameraMode.TOP;
				chaseCam.setEnabled(false);
				setCarVisible(true);
				
				if(sim.getNumberOfScreens() == 1)
					mainCameraNode.getChild("CamNode1").getControl(0).setEnabled(false);
				else
				{
					// camera detached from car node in TOP-mode to make the camera movement more stable
					mainCameraNode.getChild("CamNode1").getControl(0).setEnabled(true);
					mainCameraNode.setLocalTranslation(0,30,0);
					mainCameraNode.setLocalRotation(new Quaternion().fromAngles(-FastMath.HALF_PI, 0, 0));
				}
				break;
	
			case STATIC_BACK:
				camMode = CameraMode.STATIC_BACK;
				chaseCam.setEnabled(false);
				setCarVisible(true);
				mainCameraNode.getChild("CamNode1").getControl(0).setEnabled(true);
				mainCameraNode.setLocalTranslation(car.getStaticBackCamPos());
				mainCameraNode.setLocalRotation(new Quaternion().fromAngles(0, 0, 0));
				break;
				
			case OFF:
				camMode = CameraMode.OFF;
				chaseCam.setEnabled(false);
				setCarVisible(false);
				break;
		}
	}
	
	
	public void changeCamera() 
	{
		// STATIC_BACK --> EGO (--> CHASE, only if 1 screen) --> TOP --> STATIC_BACK --> ...
		switch (camMode)
		{
			case STATIC_BACK: setCamMode(CameraMode.EGO); break;
			case EGO: 
					if(sim.getNumberOfScreens() == 1)
						setCamMode(CameraMode.CHASE);
					else
						setCamMode(CameraMode.TOP);
					break;
			case CHASE: setCamMode(CameraMode.TOP); break;
			case TOP: setCamMode(CameraMode.STATIC_BACK); break;	
		}
	}
	
	
	public void updateCamera()
	{
		if(camMode == CameraMode.EGO)
			backViewPort.setEnabled(showBackViewMirror);
		else
			backViewPort.setEnabled(false);
		
		if(camMode == CameraMode.TOP && sim.getNumberOfScreens() == 1)
		{
			// camera detached from car node in TOP-mode to make the camera movement more stable
			Vector3f targetPosition = carNode.localToWorld(new Vector3f(0, 0, 0), null);
			Vector3f camPos = new Vector3f(targetPosition.x, targetPosition.y + 30, targetPosition.z);
			cam.setLocation(camPos);

			Vector3f left = new Vector3f(-1, 0, 0);
			Vector3f up = new Vector3f(0, 0, -1);
			Vector3f direction = new Vector3f(0, -1f, 0);
			cam.setAxes(left, up, direction);
		}
	}
	
	
	public void setCarVisible(boolean setVisible) 
	{
		if(setVisible)
		{
			if (carNode.getCullHint() == CullHint.Always)
				carNode.setCullHint(CullHint.Dynamic);
		}
		else
		{
			if (carNode.getCullHint() != CullHint.Always)
				carNode.setCullHint(CullHint.Always);
		}
	}
}
