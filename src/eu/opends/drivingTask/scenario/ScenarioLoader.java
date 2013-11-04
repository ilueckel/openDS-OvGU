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

package eu.opends.drivingTask.scenario;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

import eu.opends.basics.MapObject;
import eu.opends.basics.SimulationBasics;
import eu.opends.cameraFlight.CameraFlightSettings;
import eu.opends.car.ResetPosition;
import eu.opends.drivingTask.DrivingTask;
import eu.opends.drivingTask.DrivingTaskDataQuery;
import eu.opends.drivingTask.DrivingTaskDataQuery.Layer;
import eu.opends.drivingTask.scene.SceneLoader;
import eu.opends.effects.WeatherSettings;
import eu.opends.environment.LaneLimit;
import eu.opends.environment.TrafficLight;
import eu.opends.main.DriveAnalyzer;
import eu.opends.main.Simulator;
import eu.opends.steeringTask.SteeringTaskSettings;
import eu.opends.traffic.FollowBoxSettings;
import eu.opends.traffic.PhysicalTraffic;
import eu.opends.traffic.TrafficCarData;
import eu.opends.traffic.Waypoint;


/**
 * 
 * @author Rafael Math
 */
@SuppressWarnings("unchecked")
public class ScenarioLoader 
{
	private DrivingTaskDataQuery dtData;
	private SimulationBasics sim;
	private SceneLoader sceneLoader;
	private float driverCarMass;
	private Vector3f driverCarStartLocation;
	private Quaternion driverCarStartRotation;
	private String driverCarModelPath;
	private CameraFlightSettings cameraFlightSettings;
	private SteeringTaskSettings steeringTaskSettings;
	private Map<String, LaneLimit> laneList = new HashMap<String, LaneLimit>();
	
	
	public enum CarProperty
	{
		tires_type,
		tires_size,
		engine_engineOn,
		engine_minSpeed,
		engine_maxSpeed,
		engine_acceleration,
		suspension_stiffness, 
		suspension_compression, 
		suspension_damping,
		brake_decelerationFreeWheel,
		brake_decelerationBrake, 
		wheel_frictionSlip, 
		engine_minRPM,
		engine_maxRPM;

		public String getXPathQuery()
		{
			String[] array = this.toString().split("_");
			if(array.length >= 2)
				return "/scenario:scenario/scenario:driver/scenario:car/scenario:"+array[0]+"/scenario:"+array[1];
			else
				return "/scenario:scenario/scenario:driver/scenario:car/scenario:"+array[0];
		}
	}
	

	public ScenarioLoader(DrivingTaskDataQuery dtData, SimulationBasics sim, DrivingTask drivingTask) 
	{
		this.dtData = dtData;
		this.sim = sim;
		this.sceneLoader = drivingTask.getSceneLoader();
		processSceneCar();
		extractTraffic();
		extractCameraFlight();
		extractSteeringTaskSettings();
		
		if(sim instanceof DriveAnalyzer)
			extractIdealLine();
		
		extractRoadInformation();
	}


	private void processSceneCar() 
	{
		String driverCarRef = dtData.getValue(Layer.SCENARIO, 
					"/scenario:scenario/scenario:driver/scenario:car/@ref", String.class);
		
		MapObject sceneCar = null;
		
		for(MapObject mapObject : sceneLoader.getMapObjects())
		{
			if(mapObject.getName().equals(driverCarRef))
			{
				sceneCar = mapObject;
				driverCarMass = sceneCar.getMass();
				driverCarStartLocation = sceneCar.getLocation();
				driverCarStartRotation = sceneCar.getRotation();
				driverCarModelPath = sceneCar.getModelPath();
			}
		}
		
		if(sceneCar != null)
			sceneLoader.getMapObjects().remove(sceneCar);
		
		extractResetPoints();
	}
	
	
	private void extractResetPoints() 
	{
		String path = "/scenario:scenario/scenario:driver/scenario:car/scenario:resetPoints/scenario:resetPoint";
		try {
			NodeList positionNodes = (NodeList) dtData.xPathQuery(Layer.SCENARIO, 
					path, XPathConstants.NODESET);

			for (int k = 1; k <= positionNodes.getLength(); k++) 
			{
				ResetPosition resetPosition = createResetPosition(path + "["+k+"]");
			
				String resetPositionRef = dtData.getValue(Layer.SCENARIO, 
						path + "["+k+"]/@ref", String.class);

				Map<String, ResetPosition> resetPositionMap = sceneLoader.getResetPositionMap();
				
				if(resetPosition != null)
				{
					Simulator.getResetPositionList().add(resetPosition);
				}
				else if((resetPositionRef != null) && (resetPositionMap.containsKey(resetPositionRef)))
				{
					ResetPosition refPosition = resetPositionMap.get(resetPositionRef);
					Simulator.getResetPositionList().add(refPosition);
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	private void extractRoadInformation() 
	{
		String path = "/scenario:scenario/scenario:road/scenario:lane";
		
		try {
			NodeList laneNodes = (NodeList) dtData.xPathQuery(Layer.SCENARIO, 
					path, XPathConstants.NODESET);

			for (int k = 1; k <= laneNodes.getLength(); k++) 
			{
				String laneID = dtData.getValue(Layer.SCENARIO, path + "["+k+"]/@id", String.class);
				
				Float xMin = dtData.getValue(Layer.SCENARIO, path + "["+k+"]/scenario:xMin", Float.class);
				
				Float xMax = dtData.getValue(Layer.SCENARIO, path + "["+k+"]/scenario:xMax", Float.class);
				
				
				if(laneID != null && !laneID.isEmpty() && xMin != null && xMax != null)
				{
					LaneLimit laneLimit = new LaneLimit(xMin, xMax);
					laneList.put(laneID, laneLimit);
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	private ResetPosition createResetPosition(String path) 
	{
		String id = dtData.getValue(Layer.SCENARIO, path + "/@id", String.class);
		Vector3f translation = dtData.getVector3f(Layer.SCENARIO, path + "/scenario:translation");
		Quaternion rotation = dtData.getQuaternion(Layer.SCENARIO, path + "/scenario:rotation");

		if((id != null) && (translation != null) && (rotation != null))
			return new ResetPosition(id, translation, rotation);
		
		return null;
	}


	private void extractCameraFlight() 
	{
		List<Vector3f> cameraFlightWayPointList = new ArrayList<Vector3f>();
		
		try {

			Float cameraFlightSpeed = dtData.getValue(Layer.SCENARIO, 
					"/scenario:scenario/scenario:driver/scenario:cameraFlight/scenario:speed", Float.class);
			
			Boolean automaticStart = dtData.getValue(Layer.SCENARIO, 
					"/scenario:scenario/scenario:driver/scenario:cameraFlight/scenario:automaticStart", Boolean.class);
			
			Boolean automaticStop = dtData.getValue(Layer.SCENARIO, 
					"/scenario:scenario/scenario:driver/scenario:cameraFlight/scenario:automaticStop", Boolean.class);
			
			NodeList pointNodes = (NodeList) dtData.xPathQuery(Layer.SCENARIO, 
					"/scenario:scenario/scenario:driver/scenario:cameraFlight/scenario:track/scenario:point", XPathConstants.NODESET);

			for (int k = 1; k <= pointNodes.getLength(); k++) 
			{
				Vector3f point = dtData.getVector3f(Layer.SCENARIO, 
						"/scenario:scenario/scenario:driver/scenario:cameraFlight/scenario:track/scenario:point["+k+"]/scenario:translation");
				
				String pointRef = dtData.getValue(Layer.SCENARIO, 
						"/scenario:scenario/scenario:driver/scenario:cameraFlight/scenario:track/scenario:point["+k+"]/@ref", String.class);

				Map<String, Vector3f> pointMap = sceneLoader.getPointMap();
				
				if(point != null)
				{
					cameraFlightWayPointList.add(point);
				}
				else if((pointRef != null) && (pointMap.containsKey(pointRef)))
				{
					Vector3f translation = pointMap.get(pointRef);
					cameraFlightWayPointList.add(translation);
				}
				else 
					throw new Exception("Error in camera flight way point list");
			}
			
			cameraFlightSettings = new CameraFlightSettings(cameraFlightSpeed, automaticStart, 
					automaticStop, cameraFlightWayPointList);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public CameraFlightSettings getCameraFlightSettings() 
	{
		return cameraFlightSettings;
	}
	
	
	private Vector3f getPointRef(String pointRef)
	{
		Map<String, Vector3f> pointMap = sceneLoader.getPointMap();
		
		if((pointRef != null) && (pointMap.containsKey(pointRef)))
			return pointMap.get(pointRef);
		else 
			return null;
	}
	
	private void extractSteeringTaskSettings() 
	{		
		try {

			String steeringTaskPath = "/scenario:scenario/scenario:driver/scenario:steeringTask";
			
			String startPointLoggingId = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:startPoint/@ref", String.class);
			Vector3f startPointLogging = getPointRef(startPointLoggingId);
			
			String endPointLoggingId = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:endPoint/@ref", String.class);
			Vector3f endPointLogging = getPointRef(endPointLoggingId);
			
			String steeringTaskType = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:steeringTaskType", String.class);
			
			Float distanceToObjects = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:distanceToObjects", Float.class);
			
			Float objectOffset = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:objectOffset", Float.class);
			
			Float heightOffset = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:heightOffset", Float.class);
			
			String targetObjectId = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:targetObject/@id", String.class);
			
			Float targetObjectSpeed = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:targetObject/@speed", Float.class);
			
			Float targetObjectMaxLeft = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:targetObject/@maxLeft", Float.class);
			
			Float targetObjectMaxRight = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:targetObject/@maxRight", Float.class);
			
			String steeringObjectId = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:steeringObject/@id", String.class);
			
			Float steeringObjectSpeed = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:steeringObject/@speed", Float.class);
			
			Float steeringObjectMaxLeft = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:steeringObject/@maxLeft", Float.class);
			
			Float steeringObjectMaxRight = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:steeringObject/@maxRight", Float.class);
			
			String trafficLightObjectId = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:trafficLightObject/@id", String.class);
			
			Integer pauseAfterTargetSet = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:pauseAfterTargetSet", Integer.class);
			
			Integer blinkingInterval = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:blinkingInterval", Integer.class);
			
			
			String databaseUrl = "";
			String databaseUser = "";
			String databasePassword = "";
			String databaseTable = "";
			Boolean writeToDB = false;
			
			// check whether DB node exists
			Node databaseNode = (Node) dtData.xPathQuery(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:database", XPathConstants.NODE);

			if(databaseNode != null)
			{
				databaseUrl = dtData.getValue(Layer.SCENARIO, 
						steeringTaskPath + "/scenario:database/@url", String.class);
				
				databaseUser = dtData.getValue(Layer.SCENARIO, 
						steeringTaskPath + "/scenario:database/@user", String.class);
				
				databasePassword = dtData.getValue(Layer.SCENARIO, 
						steeringTaskPath + "/scenario:database/@password", String.class);
				
				databaseTable = dtData.getValue(Layer.SCENARIO, 
						steeringTaskPath + "/scenario:database/@table", String.class);
				
				writeToDB = true;
			}
			
			String conditionName = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:conditionName", String.class);
			
			Long conditionNumber = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:conditionNumber", Long.class);
			
			String ptStartPointId = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:primaryTask/scenario:startPoint/@ref", String.class);
			Vector3f ptStartPoint = getPointRef(ptStartPointId);
			
			String ptEndPointId = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:primaryTask/scenario:endPoint/@ref", String.class);
			Vector3f ptEndPoint = getPointRef(ptEndPointId);

			Boolean isPeripheralMode = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:primaryTask/scenario:isPeripheralMode", Boolean.class);
			
			Integer ptIconWidth = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:primaryTask/scenario:iconWidth", Integer.class);
			
			Integer ptIconHeight = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:primaryTask/scenario:iconHeight", Integer.class);
			
			Integer ptIconDistFromLeftFrameBorder = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:primaryTask/scenario:iconDistFromLeftFrameBorder", Integer.class);
			
			Integer ptIconDistFromRightFrameBorder = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:primaryTask/scenario:iconDistFromRightFrameBorder", Integer.class);
			
			Integer ptLightMinPause = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:primaryTask/scenario:lightMinPause", Integer.class);
			
			Integer ptLightMaxPause = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:primaryTask/scenario:lightMaxPause", Integer.class);

			Integer ptLightDuration = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:primaryTask/scenario:lightDuration", Integer.class);
			
			Float ptBlinkingThreshold = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:primaryTask/scenario:blinkingThreshold", Float.class);
			
			Integer ptMinimumBlinkingDuration = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:primaryTask/scenario:minBlinkingDuration", Integer.class);
			
			String stStartPointId = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:secondaryTask/scenario:startPoint/@ref", String.class);
			Vector3f stStartPoint = getPointRef(stStartPointId);
			
			String stEndPointId = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:secondaryTask/scenario:endPoint/@ref", String.class);
			Vector3f stEndPoint = getPointRef(stEndPointId);
			
			Integer stWaitForNextLandmark = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:secondaryTask/scenario:waitForNextLandmark", Integer.class);
			
			Integer stMinTimeOfAppearance = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:secondaryTask/scenario:minTimeOfAppearance", Integer.class);
			
			Float stMaxVisibilityDistance = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:secondaryTask/scenario:maxVisibilityDistance", Float.class);
			
			Float stMaxSelectionDistance = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:secondaryTask/scenario:maxSelectionDistance", Float.class);
			
			Float stMaxAngle = dtData.getValue(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:secondaryTask/scenario:maxAngle", Float.class);
			
			
			List<String> stLandmarkObjectsList = new ArrayList<String>();
			NodeList landmarkObjectNodes = (NodeList) dtData.xPathQuery(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:secondaryTask/scenario:landmarkObjects/scenario:landmarkObject", XPathConstants.NODESET);

			for (int k = 1; k <= landmarkObjectNodes.getLength(); k++) 
			{
				String landmarkObjectId = dtData.getValue(Layer.SCENARIO, 
						steeringTaskPath + "/scenario:secondaryTask/scenario:landmarkObjects/scenario:landmarkObject["+k+"]/@id", String.class);
				
				if(landmarkObjectId != null)
					stLandmarkObjectsList.add(landmarkObjectId);
				else 
					throw new Exception("Error in landmark objects list");
			}
			
			
			List<String> stLandmarkTexturesList = new ArrayList<String>();
			NodeList landmarkTextureNodes = (NodeList) dtData.xPathQuery(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:secondaryTask/scenario:landmarkTextures/scenario:landmarkTexture", XPathConstants.NODESET);

			for (int k = 1; k <= landmarkTextureNodes.getLength(); k++) 
			{
				String landmarkTexturesUrl = dtData.getValue(Layer.SCENARIO, 
						steeringTaskPath + "/scenario:secondaryTask/scenario:landmarkTextures/scenario:landmarkTexture["+k+"]/@url", String.class);
				
				if(landmarkTexturesUrl != null)
					stLandmarkTexturesList.add(landmarkTexturesUrl);
				else 
					throw new Exception("Error in landmark textures list");
			}
			
			
			List<String> stDistractorTexturesList = new ArrayList<String>();
			NodeList distractorTextureNodes = (NodeList) dtData.xPathQuery(Layer.SCENARIO, 
					steeringTaskPath + "/scenario:secondaryTask/scenario:distractorTextures/scenario:distractorTexture", XPathConstants.NODESET);

			for (int k = 1; k <= distractorTextureNodes.getLength(); k++) 
			{
				String distractorTexturesUrl = dtData.getValue(Layer.SCENARIO, 
						steeringTaskPath + "/scenario:secondaryTask/scenario:distractorTextures/scenario:distractorTexture["+k+"]/@url", String.class);
				
				if(distractorTexturesUrl != null)
					stDistractorTexturesList.add(distractorTexturesUrl);
				else 
					throw new Exception("Error in distractor textures list");
			}
			
			steeringTaskSettings = new SteeringTaskSettings(startPointLogging, endPointLogging, steeringTaskType, 
					distanceToObjects, objectOffset, heightOffset, targetObjectId, targetObjectSpeed, 
					targetObjectMaxLeft, targetObjectMaxRight, steeringObjectId, steeringObjectSpeed, 
					steeringObjectMaxLeft, steeringObjectMaxRight, trafficLightObjectId, pauseAfterTargetSet, 
					blinkingInterval, writeToDB, databaseUrl, databaseUser, databasePassword, databaseTable, 
					conditionName, conditionNumber, ptStartPoint, ptEndPoint, isPeripheralMode, ptIconWidth, ptIconHeight, 
					ptIconDistFromLeftFrameBorder, ptIconDistFromRightFrameBorder, ptLightMinPause, ptLightMaxPause, 
					ptLightDuration, ptBlinkingThreshold, ptMinimumBlinkingDuration, stStartPoint, stEndPoint, 
					stWaitForNextLandmark, stMinTimeOfAppearance, stMaxVisibilityDistance, stMaxSelectionDistance, 
					stMaxAngle, stLandmarkObjectsList, stLandmarkTexturesList, stDistractorTexturesList);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	
	public SteeringTaskSettings getSteeringTaskSettings() 
	{
		return steeringTaskSettings;
	}
	
	
	public void extractIdealLine()
	{
		List<Vector3f> idealPoints = new ArrayList<Vector3f>();
		
		try {
			
			NodeList pointNodes = (NodeList) dtData.xPathQuery(Layer.SCENARIO, 
					"/scenario:scenario/scenario:driver/scenario:idealTrack/scenario:point", XPathConstants.NODESET);

			for (int k = 1; k <= pointNodes.getLength(); k++) 
			{
				Vector3f point = dtData.getVector3f(Layer.SCENARIO, 
						"/scenario:scenario/scenario:driver/scenario:idealTrack/scenario:point["+k+"]/scenario:translation");
				
				String pointRef = dtData.getValue(Layer.SCENARIO, 
						"/scenario:scenario/scenario:driver/scenario:idealTrack/scenario:point["+k+"]/@ref", String.class);

				Map<String, Vector3f> pointMap = sceneLoader.getPointMap();
				
				if(point != null)
				{
					idealPoints.add(point);
				}
				else if((pointRef != null) && (pointMap.containsKey(pointRef)))
				{
					Vector3f translation = pointMap.get(pointRef);
					idealPoints.add(translation);
				}
				else 
					throw new Exception("Error in ideal point list");
			}
			
			for(Vector3f idealPoint : idealPoints)
			{
				Vector2f idealPoint2f = new Vector2f(idealPoint.getX(), idealPoint.getZ());
				((DriveAnalyzer) sim).getDeviationComputer().addIdealPoint(idealPoint2f);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public WeatherSettings getWeatherSettings()
	{
		Float snowingPercentage = dtData.getValue(Layer.SCENARIO, 
				"/scenario:scenario/scenario:environment/scenario:weather/scenario:snowingPercentage", Float.class);
		if(snowingPercentage == null)
			snowingPercentage = 0f;
		
		Float rainingPercentage = dtData.getValue(Layer.SCENARIO, 
				"/scenario:scenario/scenario:environment/scenario:weather/scenario:rainingPercentage", Float.class);
		if(rainingPercentage == null)
			rainingPercentage = 0f;
		
		Float fogPercentage = dtData.getValue(Layer.SCENARIO, 
				"/scenario:scenario/scenario:environment/scenario:weather/scenario:fogPercentage", Float.class);
		if(fogPercentage == null)
			fogPercentage = 0f;
		
		return new WeatherSettings(snowingPercentage, rainingPercentage, fogPercentage);
	}
	

	/**
	 * Looks up the node "startPosition" for the initial location 
	 * of the car at the beginning of the simulation.
	 * 
	 */
	public Vector3f getStartLocation() 
	{
		return driverCarStartLocation;
	}
	
	
	/**
	 * Looks up the node "startPosition" for the initial rotation 
	 * of the car at the beginning of the simulation.
	 * 
	 */
	public Quaternion getStartRotation() 
	{
		return driverCarStartRotation;
	}
	
	
	public float getChassisMass() 
	{
		return driverCarMass;
	}
	
	
	public String getModelPath() 
	{
		return driverCarModelPath;
	}
	
	
	public Map<String, LaneLimit> getLaneList()
	{
		return laneList;
	}
	
	
	/**
	 * Looks up the sub node (specified in parameter name) of the given element node
	 * and writes the data to the global variable with the same name. If this was 
	 * successful, the global variable "isSet_&lt;name&gt;" will be set to true.
	 */
	public <T> T getCarProperty(CarProperty carProperty, T defaultValue)
	{		
		try {
			
			Class<T> cast = (Class<T>) defaultValue.getClass();
			return (T) dtData.getValue(Layer.SCENARIO, carProperty.getXPathQuery(), cast);
			
		} catch (Exception e2) {
			dtData.reportInvalidValueError(carProperty.toString(), dtData.getScenarioPath());
		}
		
		return (T) defaultValue;
	}
	
	
	private void extractTraffic()
	{
		try {
			NodeList pointNodes = (NodeList) dtData.xPathQuery(Layer.SCENARIO, 
					"/scenario:scenario/scenario:traffic/scenario:vehicle", XPathConstants.NODESET);

			for (int k = 1; k <= pointNodes.getLength(); k++) 
			{
				/*
				String modelRef = dtData.getValue(Layer.SCENARIO, 
						"/scenario:scenario/scenario:traffic/scenario:vehicle["+k+"]/@ref", String.class);
				
				Float simulationRate = dtData.getValue(Layer.SCENARIO, 
						"/scenario:scenario/scenario:traffic/scenario:vehicle["+k+"]/scenario:simulationRate", Float.class);
		
				Float pivotPosition = dtData.getValue(Layer.SCENARIO, 
						"/scenario:scenario/scenario:traffic/scenario:vehicle["+k+"]/scenario:pivotPosition", Float.class);
				
				Float frontSide = dtData.getValue(Layer.SCENARIO, 
						"/scenario:scenario/scenario:traffic/scenario:vehicle["+k+"]/scenario:frontSide", Float.class);
				
				Float acceleration = dtData.getValue(Layer.SCENARIO, 
						"/scenario:scenario/scenario:traffic/scenario:vehicle["+k+"]/scenario:acceleration", Float.class);
				
				Float yRotationSpeed = dtData.getValue(Layer.SCENARIO, 
						"/scenario:scenario/scenario:traffic/scenario:vehicle["+k+"]/scenario:yRotationSpeed", Float.class);
				
				Float zRotationSpeed = dtData.getValue(Layer.SCENARIO, 
						"/scenario:scenario/scenario:traffic/scenario:vehicle["+k+"]/scenario:zRotationSpeed", Float.class);
				*/
				

				String name = dtData.getValue(Layer.SCENARIO, 
						"/scenario:scenario/scenario:traffic/scenario:vehicle["+k+"]/@id", String.class);
				
				Float mass = dtData.getValue(Layer.SCENARIO, 
						"/scenario:scenario/scenario:traffic/scenario:vehicle["+k+"]/scenario:mass", Float.class);

				Float acceleration = dtData.getValue(Layer.SCENARIO, 
						"/scenario:scenario/scenario:traffic/scenario:vehicle["+k+"]/scenario:acceleration", Float.class);
				
				Float decelerationBrake = dtData.getValue(Layer.SCENARIO, 
						"/scenario:scenario/scenario:traffic/scenario:vehicle["+k+"]/scenario:decelerationBrake", Float.class);
				
				Float decelerationFreeWheel = dtData.getValue(Layer.SCENARIO, 
						"/scenario:scenario/scenario:traffic/scenario:vehicle["+k+"]/scenario:decelerationFreeWheel", Float.class);
				
				Boolean engineOn = dtData.getValue(Layer.SCENARIO, 
						"/scenario:scenario/scenario:traffic/scenario:vehicle["+k+"]/scenario:engineOn", Boolean.class);
				
				String modelPath = dtData.getValue(Layer.SCENARIO, 
						"/scenario:scenario/scenario:traffic/scenario:vehicle["+k+"]/scenario:modelPath", String.class);
				
				ArrayList<Waypoint> wayPoints = extractWayPoints(
						"/scenario:scenario/scenario:traffic/scenario:vehicle["+k+"]/scenario:wayPoints/scenario:wayPoint");
				
				Float curveTension = dtData.getValue(Layer.SCENARIO, 
						"/scenario:scenario/scenario:traffic/scenario:vehicle["+k+"]/scenario:curveTension", Float.class);
				
				Float maxDistance = dtData.getValue(Layer.SCENARIO, 
						"/scenario:scenario/scenario:traffic/scenario:vehicle["+k+"]/scenario:maxDistanceFromPath", Float.class);
		
				Boolean pathIsCycle = dtData.getValue(Layer.SCENARIO, 
						"/scenario:scenario/scenario:traffic/scenario:vehicle["+k+"]/scenario:pathIsCycle", Boolean.class);
				
				Boolean pathIsVisible = dtData.getValue(Layer.SCENARIO, 
						"/scenario:scenario/scenario:traffic/scenario:vehicle["+k+"]/scenario:pathIsVisible", Boolean.class);
				
				String startWayPoint = dtData.getValue(Layer.SCENARIO, 
						"/scenario:scenario/scenario:traffic/scenario:vehicle["+k+"]/scenario:startWayPoint", String.class);

				
				TrafficCarData trafficCarData = new TrafficCarData(name, mass, acceleration, decelerationBrake, 
						decelerationFreeWheel, engineOn, modelPath, new FollowBoxSettings(wayPoints, maxDistance, 
						curveTension, pathIsCycle, pathIsVisible, startWayPoint));
				PhysicalTraffic.getVehicleDataList().add(trafficCarData);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	

	public ArrayList<Waypoint> extractWayPoints(String path)
	{
		ArrayList<Waypoint> wayPoints = new ArrayList<Waypoint>();
		
		try {
			NodeList pointNodes = (NodeList) dtData.xPathQuery(Layer.SCENARIO, 
					path, XPathConstants.NODESET);

			for (int k = 1; k <= pointNodes.getLength(); k++) 
			{
				Waypoint wayPoint = dtData.getWayPoint(Layer.SCENARIO, path + "["+k+"]");
			
				String wayPointRef = dtData.getValue(Layer.SCENARIO, 
						path + "["+k+"]/@ref", String.class);

				Map<String, Vector3f> pointMap = sceneLoader.getPointMap();
				
				if(wayPoint != null)
				{
					wayPoints.add(wayPoint);
				}
				else if((wayPointRef != null) && (pointMap.containsKey(wayPointRef)))
				{
					Vector3f translation = pointMap.get(wayPointRef);
					Float speed = dtData.getValue(Layer.SCENARIO, path + "["+k+"]/scenario:speed", Float.class);
					String trafficLightID = dtData.getValue(Layer.SCENARIO, path + "["+k+"]/scenario:trafficLight", String.class);
					
					if((translation != null) && (speed != null))
					{
						Waypoint point = new Waypoint(wayPointRef, translation, speed, trafficLightID);
						wayPoints.add(point);
					}
				}
				else 
					throw new Exception("Error in way point list");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return new ArrayList<Waypoint>();
		}
		
		return wayPoints;
	}


	public boolean isAutomaticTransmission(boolean defaultValue) 
	{
		Boolean isAutomatic = dtData.getValue(Layer.SCENARIO, 
				"/scenario:scenario/scenario:driver/scenario:car/scenario:transmission/scenario:automatic", Boolean.class);
		
		if(isAutomatic != null)
			return isAutomatic;
		else
			return defaultValue;
	}


	public float getReverseGear(float defaultValue) 
	{
		Float transmission = dtData.getValue(Layer.SCENARIO, 
				"/scenario:scenario/scenario:driver/scenario:car/scenario:transmission/scenario:reverse", Float.class);
		
		if(transmission != null)
			return transmission;
		else
			return defaultValue;
	}
	
	
	public float getEngineSoundIntensity(float defaultValue) 
	{
		Float soundIntensity = dtData.getValue(Layer.SCENARIO, 
				"/scenario:scenario/scenario:driver/scenario:car/scenario:engine/scenario:soundIntensity", Float.class);
		
		if(soundIntensity != null)
			return soundIntensity;
		else
			return defaultValue;
	}


	public Float[] getForwardGears(Float[] defaultValue) 
	{
		List<Float> transmission = dtData.getArray(Layer.SCENARIO, 
				"/scenario:scenario/scenario:driver/scenario:car/scenario:transmission/scenario:forward", Float.class);
		
		Float[] transmissionArray = new Float[transmission.size()];
		
		for(int i=0; i<transmission.size(); i++)
			transmissionArray[i] = transmission.get(i);
		
		if((transmissionArray != null) && (transmissionArray.length >= 1))
			return transmissionArray;
		else
			return defaultValue;
	}


	public List<TrafficLight> getTrafficLights() 
	{
		//TODO finish method
		List<TrafficLight> trafficLightList = new ArrayList<TrafficLight>();
		//List<Spatial> objectList = sim.getSceneNode().getChildren();
		
		// ...
		
		return trafficLightList;
	}

	
}
