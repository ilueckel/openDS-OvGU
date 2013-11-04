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


import com.jme3.input.ChaseCamera;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Node;
import com.jme3.scene.control.CameraControl.ControlDirection;

import eu.opends.basics.SimulationBasics;
import eu.opends.drivingTask.settings.SettingsLoader;
import eu.opends.drivingTask.settings.SettingsLoader.Setting;


/**
 * 
 * @author Rafael Math
 */
public abstract class CameraFactory 
{
	protected SimulationBasics sim;
	protected SettingsLoader settingsLoader;
	protected CameraMode camMode = CameraMode.EGO;
	protected ChaseCamera chaseCam;
	protected boolean showBackViewMirror;
	protected CameraNode mainCameraNode = new CameraNode();
	
	protected Node targetNode;
	protected Camera cam;
	protected ViewPort backViewPort;

	private float angleBetweenAdjacentCameras;
    
	private int width;
	private int height;	
	private float aspectRatio;
	private float frameOfView;
	
	
	/**
	 * Get main camera node which contains all scene cameras.
	 * 
	 * @return
	 * 		Node containing all scene cameras.
	 */
	public CameraNode getMainCameraNode()
	{
		return mainCameraNode;
	}
	
	
	/**
	 * Camera views that can be activated while driving 
	 */
	public enum CameraMode 
	{
		CHASE, TOP, EGO, STATIC_BACK, OFF
	}

	
	/**
	 * Get the current camera view.
	 * 
	 * @return
	 * 		Current camera view.
	 */
	public CameraMode getCamMode() 
	{
		return camMode;
	}

	
	/**
	 * Setup all scene cameras.
	 * 
	 * @param sim
	 *
	 * @param targetNode
	 */
	public void initCamera(SimulationBasics sim, Node targetNode) 
	{
		this.sim = sim;
		this.targetNode = targetNode;
		this.cam = sim.getCamera();
		this.settingsLoader = SimulationBasics.getSettingsLoader();
		
    	this.width = sim.getSettings().getWidth();
    	this.height = sim.getSettings().getHeight();
    	this.aspectRatio = (float)width/(float)height;
    	this.frameOfView = settingsLoader.getSetting(Setting.General_frameOfView, 30.5f);
    	//this.frameOfView = 30.5f; //62.5f; //25.63f; //(40*3.0f)/aspectRatio;  //25; //13.2f; //30.5f; // 23.2f; // 40/aspectRatio;
	    
    	// set initial rear view mirror state
    	this.showBackViewMirror = settingsLoader.getSetting(Setting.General_showRearviewMirror, false);
    	
    	angleBetweenAdjacentCameras = settingsLoader.getSetting(Setting.General_angleBetweenAdjacentCameras, 40);
    	if(angleBetweenAdjacentCameras > 90 || angleBetweenAdjacentCameras < 0)
    	{
    		System.err.println("Angle between adjacent cameras must be within 0 to 90 degrees. Set to default: 40 degrees.");
    		angleBetweenAdjacentCameras = 40;
    	}
    	
    	int numberOfScreens = sim.getNumberOfScreens();
	    if(numberOfScreens > 1)
	    {
	    	// clear default cam
	    	sim.getRenderManager().getMainView("Default").clearScenes();

	    	// add one camera for each screen
	    	for(int i = 1; i<=numberOfScreens; i++)
	    		setupCamera(i,numberOfScreens);
	    }
	    else
	    	setupCenterCamera();

		setupBackCamera();
    	
		setupChaseCamera();
	}
	
	
	/**
	 * Returns whether back view mirror is visible or not
	 * 
	 * @return
	 * 		Visibility of rear view mirror
	 */
	public boolean getShowBackViewMirror() 
	{
		return showBackViewMirror;
	}
	
	
	/**
	 * Set whether back view mirror is visible or not
	 * 
	 * @param showMirror
	 * 		Boolean indicating visibility of rear view mirror
	 */
	public void setShowBackViewMirror(boolean showMirror)
	{
		// user may only change state of mirror in ego camera mode
		if(camMode == CameraMode.EGO)
			showBackViewMirror = showMirror;
	}
	
	
	public abstract void setCamMode(CameraMode mode);
	
	
	public abstract void changeCamera();

	
	public abstract void updateCamera();

	
	private void setupCamera(int index, int totalInt) 
	{
		float total = totalInt;
		Camera cam = new Camera(width, height);
		cam.setFrustumPerspective(frameOfView, aspectRatio/total, 1f, 2000);
		
		float additionalPixel = 1f/width;
		float viewPortLeft = (index-1)/total;
		float viewPortRight = (index)/total + additionalPixel;
		cam.setViewPort(viewPortLeft, viewPortRight, 0f, 1f);
		
		ViewPort viewPort = sim.getRenderManager().createMainView("View"+index, cam);
		viewPort.attachScene(sim.getRootNode());
		viewPort.setBackgroundColor(ColorRGBA.Black);
		
		// add camera to main camera node
		CameraNode camNode = new CameraNode("CamNode"+index, cam);
		camNode.setControlDir(ControlDirection.SpatialToCamera);
		mainCameraNode.attachChild(camNode);
		camNode.setLocalTranslation(new Vector3f(0, 0, 0));
		
		float angle = (((totalInt+1)/2)-index) * angleBetweenAdjacentCameras;
		camNode.setLocalRotation(new Quaternion().fromAngles(0, (180+angle)*FastMath.DEG_TO_RAD, 0));
	}

	
	/**
	 * 	Setup center camera (always on)
	 */
	private void setupCenterCamera() 
	{
		// add center camera to main camera node
		CameraNode centerCamNode = new CameraNode("CamNode1", cam);	
		centerCamNode.setControlDir(ControlDirection.SpatialToCamera);
		mainCameraNode.attachChild(centerCamNode);
		centerCamNode.setLocalTranslation(new Vector3f(0, 0, 0));
		centerCamNode.setLocalRotation(new Quaternion().fromAngles(0, 180*FastMath.DEG_TO_RAD, 0));
	}


	/**
	 *	Setup rear view mirror
	 */
	private void setupBackCamera() 
	{
		Camera backCam = cam.clone();
		
		float left = settingsLoader.getSetting(Setting.General_rearviewMirrorViewPortLeft, 0.3f);
		float right = settingsLoader.getSetting(Setting.General_rearviewMirrorViewPortRight, 0.7f);
		float bottom = settingsLoader.getSetting(Setting.General_rearviewMirrorViewPortBottom, 0.78f);
		float top = settingsLoader.getSetting(Setting.General_rearviewMirrorViewPortTop, 0.98f);
		
		if(sim.getNumberOfScreens() > 1)
		{
			left = 0.4f;
			right = 0.6f;
			bottom = 0.78f;
			top = 0.98f;
		}
		
		float aspect = ((right-left)*width)/((top-bottom)*height);
		
		backCam.setFrustumPerspective(30.0f, aspect, 1, 2000);
		backCam.setViewPort(left, right, bottom, top);
		
		// inverse back view cam (=> back view mirror)
		Matrix4f matrix = backCam.getProjectionMatrix().clone();
		matrix.m00 = - matrix.m00;
		backCam.setProjectionMatrix(matrix);
		
		// set view port (needed to show/hide mirror)
	    backViewPort = sim.getRenderManager().createMainView("BackView", backCam);
	    backViewPort.attachScene(sim.getRootNode());
	    backViewPort.setEnabled(false);
	    
	    // add back camera to main camera node
    	CameraNode backCamNode = new CameraNode("BackCamNode", backCam);
    	backCamNode.setControlDir(ControlDirection.SpatialToCamera);
    	mainCameraNode.attachChild(backCamNode);
	}
	

	/**
	 *	Setup free camera (can be controlled with mouse)
	 */
	private void setupChaseCamera() 
	{
		chaseCam = new ChaseCamera(cam, targetNode, sim.getInputManager());
        chaseCam.setUpVector(new Vector3f(0, 1, 0));
        chaseCam.setEnabled(false);
        
        // set visual parameters        
        float minDistance = settingsLoader.getSetting(Setting.Mouse_minScrollZoom, 1f);
        chaseCam.setMinDistance(minDistance);
        
        float maxDistance = settingsLoader.getSetting(Setting.Mouse_maxScrollZoom, 40f);
        chaseCam.setMaxDistance(maxDistance);
        
        float zoomSensitivity = settingsLoader.getSetting(Setting.Mouse_scrollSensitivityFactor, 5f);
        chaseCam.setZoomSensitivity(zoomSensitivity);
	}
}
