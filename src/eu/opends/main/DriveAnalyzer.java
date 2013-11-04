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

package eu.opends.main;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.math.ColorRGBA;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.font.BitmapText;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.shape.Curve;
import com.jme3.scene.shape.Cylinder;
import com.jme3.system.AppSettings;
import de.lessvoid.nifty.Nifty;
import eu.opends.analyzer.DeviationComputer;
import eu.opends.analyzer.DataReader;
import eu.opends.basics.InternalMapProcessing;
import eu.opends.basics.SimulationBasics;
import eu.opends.camera.AnalyzerCam;
import eu.opends.drivingTask.DrivingTask;
import eu.opends.input.KeyBindingCenter;
import eu.opends.niftyGui.AnalyzerFileSelectionGUIController;

/**
 * 
 * @author Saied Tehrani, Rafael Math
 */
public class DriveAnalyzer extends SimulationBasics 
{	
	private boolean showRelativeTime = true;
	private boolean pointsEnabled = false;
	private boolean lineEnabled = true;
	private boolean coneEnabled = true;

	private Nifty nifty;
    private boolean analyzerFileGiven = false;
    public String analyzerFilePath = "";
    private boolean initializationFinished = false;

	private Node pointNode = new Node();
	private Node lineNode = new Node();
	private Node coneNode = new Node();
	private Node target = new Node();
	private int targetIndex = 0;

	private BitmapText markerText, speedText, timeText;
	
	private float roadWidth = 10.0f;
	private DeviationComputer devComp = new DeviationComputer(roadWidth);
	public DeviationComputer getDeviationComputer() 
	{
		return devComp;
	}

	private LinkedList<Vector3f> carPositionList = new LinkedList<Vector3f>();
	public LinkedList<Vector3f> getCarPositionList() 
	{
		return carPositionList;
	}

	private LinkedList<Quaternion> carRotationList = new LinkedList<Quaternion>();
	public LinkedList<Quaternion> getCarRotationList() 
	{
		return carRotationList;
	}

	private LinkedList<Long> timeList = new LinkedList<Long>();
	private LinkedList<Double> speedList = new LinkedList<Double>();

	private DataReader dataReader = new DataReader();
	private Long initialTimeStamp = 0l;

	public enum VisualizationMode 
	{
		POINT, LINE, CONE;
	}

	
	@Override
	public void simpleInitApp() 
	{
		setDisplayFps(false);
		setDisplayStatView(false);
		
    	if(analyzerFileGiven)
    		simpleInitAnalyzerFile();
    	else
    		initAnalyzerFileSelectionGUI();
	}	
		
	
	private void initAnalyzerFileSelectionGUI() 
	{
		NiftyJmeDisplay niftyDisplay = new NiftyJmeDisplay(assetManager, inputManager, audioRenderer, guiViewPort);
    	
    	// Create a new NiftyGUI object
    	nifty = niftyDisplay.getNifty();
    		
    	String xmlPath = "Interface/AnalyzerFileSelectionGUI.xml";
    	
    	// Read XML and initialize custom ScreenController
    	nifty.fromXml(xmlPath, "start", new AnalyzerFileSelectionGUIController(this, nifty));
    		
    	// attach the Nifty display to the gui view port as a processor
    	guiViewPort.addProcessor(niftyDisplay);
    	
    	// disable fly cam
    	flyCam.setEnabled(false);
	}
	
	
	public void closeAnalyzerFileSelectionGUI() 
	{
		nifty.exit();
        inputManager.setCursorVisible(false);
        flyCam.setEnabled(true);
	}

	
	public boolean isValidAnalyzerFile(File analyzerFile) 
	{
		return dataReader.isValidAnalyzerFile(analyzerFile);
	}
	

	public void simpleInitAnalyzerFile() 
	{
		loadData();
		
		loadDrivingTask();
		
		super.simpleInitApp();	

		loadMap();
		
		// setup key binding
		keyBindingCenter = new KeyBindingCenter(this);
     
		//devComp.showAllIdealPoints();
		//devComp.showAllWayPoints();
		try {
			float area = devComp.getDeviation();
			float lengthOfIdealLine = devComp.getLengthOfIdealLine();
			System.out.println("Area between ideal line and driven line: " + area);
			System.out.println("Length of ideal line: " + lengthOfIdealLine);
			System.out.println("Mean deviation: " + (float)area/lengthOfIdealLine + "\n");
		} catch (Exception e) {
			System.out.println(e.getMessage() + "\n");
		}
		
		createText();
		
        // setup camera settings
		cameraFactory = new AnalyzerCam(this, target);
		//target.attachChild(cameraFactory.getMainCameraNode()); // TODO
        
        visualizeData();
        
        initializationFinished = true;
	}



	/**
	 * Loading the data from <code>path</code> and storing them in the
	 * appropriate data-structures.
	 * 
	 * @param analyzerFilePath
	 */
	private void loadData() 
	{
		dataReader.initReader(analyzerFilePath, true);

		//mapFileName = myDataReader.getNameOfMap();

		String inputLineNext = dataReader.readInNextDataLine();

		while (inputLineNext != null) 
		{	
			Vector3f carPos = dataReader.getCarPositionFromDataLine(inputLineNext);
			carPositionList.add(carPos);
			
			Quaternion carRotation = dataReader.getCarRotationFromDataLine(inputLineNext);
			carRotationList.add(carRotation);
		
			devComp.addWayPoint(carPos);
			
			timeList.add(dataReader.getTimeStamp(inputLineNext));

			speedList.add(dataReader.getSpeed(inputLineNext));

			inputLineNext = dataReader.readInNextDataLine();
		}

		if(timeList.size() > 0)
			initialTimeStamp = timeList.get(0);
		
		// System.out.println("Size carPositionList: "+ carPositionList.size());
		// System.out.println("Size timeList: "+ timeList.size());
		// System.out.println("Size speedList: "+ speedList.size());
		// System.out.println("Size markerList: "+ markerList.size());

	}

	
	private void loadDrivingTask() 
	{
		String drivingTaskName = dataReader.getNameOfDrivingTaskFile();
		File drivingTaskFile = new File(drivingTaskName);
		drivingTask = new DrivingTask(this,drivingTaskFile);
		
		sceneLoader = drivingTask.getSceneLoader();
		scenarioLoader = drivingTask.getScenarioLoader();
		interactionLoader = drivingTask.getInteractionLoader();
		settingsLoader = drivingTask.getSettingsLoader();
	}
	
	
	/**
	 * This method is used to generate the additional Text-elements.
	 */
	private void createText() 
	{
	    guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        
        markerText = new BitmapText(guiFont, false);
        markerText.setName("markerText");
        markerText.setText("");
        markerText.setCullHint(CullHint.Dynamic);
        markerText.setSize(guiFont.getCharSet().getRenderedSize());
        markerText.setColor(ColorRGBA.LightGray);
        markerText.setLocalTranslation(0, 20, 0);
        guiNode.attachChild(markerText);

        timeText = new BitmapText(guiFont, false);
        timeText.setName("timeText");
        timeText.setText("");
        timeText.setCullHint(CullHint.Dynamic);
        timeText.setSize(guiFont.getCharSet().getRenderedSize());
        timeText.setColor(ColorRGBA.LightGray);
        timeText.setLocalTranslation(settings.getWidth() / 2 - 125, 20,	0);
        guiNode.attachChild(timeText);
        
        speedText = new BitmapText(guiFont, false);
        speedText.setName("speedText");
        speedText.setText("");
        speedText.setCullHint(CullHint.Dynamic);
        speedText.setSize(guiFont.getCharSet().getRenderedSize());
        speedText.setColor(ColorRGBA.LightGray);
        speedText.setLocalTranslation(settings.getWidth() - 125, 20, 0);
        guiNode.attachChild(speedText);
	}

	
	private void visualizeData() 
	{
		if(devComp.getIdealPoints().size() >= 2)
		{
			/*
			 * Visualizing the distance between the car and the ideal line
			 */
			Material deviationMaterial = new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
			deviationMaterial.setColor("Color", ColorRGBA.Red);
			
			Curve deviationLine = new Curve(devComp.getDeviationPoints().toArray(new Vector3f[0]), 1);
			deviationLine.setMode(Mode.Lines);
			deviationLine.setLineWidth(4f);
			Geometry geoDeviationLine = new Geometry("deviationLine", deviationLine);
			geoDeviationLine.setMaterial(deviationMaterial);
			sceneNode.attachChild(geoDeviationLine);
			
			
			/*
			 * Drawing the ideal Line
			 */
			Material idealMaterial = new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
			idealMaterial.setColor("Color", ColorRGBA.Blue);
			
			Curve idealLine = new Curve(devComp.getIdealPoints().toArray(new Vector3f[0]), 1);
			idealLine.setMode(Mode.Lines);
			idealLine.setLineWidth(4f);
			Geometry geoIdealLine = new Geometry("idealLine", idealLine);
			geoIdealLine.setMaterial(idealMaterial);
			sceneNode.attachChild(geoIdealLine);
		}
		
		
		/*
		 * Drawing the driven Line
		 */
		Material drivenMaterial = new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
		drivenMaterial.setColor("Color", ColorRGBA.Yellow);
		
		// visualize points
		Curve points = new Curve(carPositionList.toArray(new Vector3f[0]), 1);
		points.setMode(Mode.Points);
		points.setPointSize(4f);
		Geometry geoPoints = new Geometry("drivenPoints", points);
		geoPoints.setMaterial(drivenMaterial);
		pointNode.attachChild(geoPoints);

		// visualize line
		Curve line = new Curve(carPositionList.toArray(new Vector3f[0]), 1);
		line.setMode(Mode.Lines);
		line.setLineWidth(4f);
		Geometry geoLine = new Geometry("drivenLine", line);
	    geoLine.setMaterial(drivenMaterial);
	    lineNode.attachChild(geoLine);

	
	    // visualize cones
	    Material coneMaterial = new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
	    coneMaterial.setColor("Color", ColorRGBA.Black);
		
		for (int i=0; i<carPositionList.size(); i++) 
		{
			Cylinder cone = new Cylinder(10, 10, 0.3f, 0.01f, 0.9f, true, false);
			cone.setLineWidth(4f);
			Geometry geoCone = new Geometry("cone_"+i, cone);
			geoCone.setLocalTranslation(carPositionList.get(i));
			geoCone.setLocalRotation(carRotationList.get(i));
			geoCone.setMaterial(coneMaterial);
			geoCone.setCullHint(CullHint.Always);
			coneNode.attachChild(geoCone);
		}

		if (pointsEnabled)
			sceneNode.attachChild(pointNode);
		
		if (lineEnabled)
			sceneNode.attachChild(lineNode);
		
		if (coneEnabled)
			sceneNode.attachChild(coneNode);
		
		// set camera view and time/speed texts
		updateView();
	}


	public void toggleVisualization(VisualizationMode vizMode) 
	{
		if(!isPause())
		{
			switch (vizMode) {
			case POINT:
	
				if (pointsEnabled) {
					sceneNode.detachChild(pointNode);
					pointsEnabled = false;
				} else {
					sceneNode.attachChild(pointNode);
					pointsEnabled = true;
				}
	
				break;
	
			case LINE:
	
				if (lineEnabled) {
					sceneNode.detachChild(lineNode);
					lineEnabled = false;
				} else {
					sceneNode.attachChild(lineNode);
					lineEnabled = true;
				}
	
				break;
	
			case CONE:
	
				if (coneEnabled) {
					sceneNode.detachChild(coneNode);
					coneEnabled = false;
				} else {
					sceneNode.attachChild(coneNode);
					coneEnabled = true;
				}
	
				break;
	
			default:
				break;
			}
		}

	}


	/**
	 * Does load a physics map. Here the elements of the map are also further
	 * processed by using the class <code>InternalMapProcessing</code>, e.g.
	 * replacing symbolic elements by a simulated counterpart.
	 */
	private void loadMap() 
	{
    	//load map model and setup car
		new InternalMapProcessing(this);
	}

	
	/**
	 * <code>moveFocus()</code> sets the position of the target. The target's
	 * position is equal to one of the data-points, whereas the direction
	 * specifies which of the neighbors in the data-point list should be taken.
	 * 
	 * @param direction
	 */
	public void moveFocus(int direction) 
	{
		if (!isPause() && direction == 1 && (targetIndex + 1) < carPositionList.size()) 
		{
			targetIndex++;
			updateView();
		}

		if (!isPause() && direction == -1 && (targetIndex - 1) >= 0)
		{
			targetIndex--;
			updateView();
		}
	}


	private void updateView() 
	{
		target.setLocalTranslation(carPositionList.get(targetIndex));
		target.setLocalRotation(carRotationList.get(targetIndex));
		cameraFactory.updateCamera();
		
		// update speed text
		DecimalFormat decimalFormat = new DecimalFormat("#0.00");
		speedText.setText(decimalFormat.format(speedList.get(targetIndex)) + " km/h");
		
		// update timestamp
		updateTimestamp();

		// make previous cone invisible (if exists)
		Spatial previousCone = coneNode.getChild("cone_" + (targetIndex-1));		
		if(previousCone != null)
			previousCone.setCullHint(CullHint.Always);
		
		// make current cone visible (if exists)
		Spatial currentCone = coneNode.getChild("cone_" + targetIndex);
		if(currentCone != null)
			currentCone.setCullHint(CullHint.Dynamic);
		
		// make next cone invisible (if exists)
		Spatial nextCone = coneNode.getChild("cone_" + (targetIndex+1));
		if(nextCone != null)
			nextCone.setCullHint(CullHint.Always);
	}


	private void updateTimestamp() 
	{
		Long currentTimeStamp = timeList.get(targetIndex);
		
		if(showRelativeTime)
		{
			Long elapsedTime = currentTimeStamp - initialTimeStamp;
			SimpleDateFormat dateFormat = new SimpleDateFormat("mm:ss.S");
			timeText.setText(dateFormat.format(elapsedTime));
		}
		else
		{
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
			timeText.setText(dateFormat.format(new Date(currentTimeStamp)));
		}
	}
	

    @Override
    public void simpleUpdate(float tpf) 
    {
    	if(initializationFinished)
    	{
			// updates camera
			super.simpleUpdate(tpf);
    	}
    }
    
    
	/**
	 * Cleanup after game loop was left
	 */
    /*
	@Override
    public void stop() 
    {
		if(initializationFinished)
			super.stop();

    	System.exit(0);
    }
	*/
	
	public static void main(String[] args) 
	{   	
		Logger.getLogger("").setLevel(Level.SEVERE);
		DriveAnalyzer analyzer = new DriveAnalyzer();

    	if(args.length >= 1)
    	{
    		analyzer.analyzerFilePath = args[0];
    		analyzer.analyzerFileGiven = true;
    		
    		if(!analyzer.isValidAnalyzerFile(new File(args[0])))
    			return;
    	}  	
    	
    	AppSettings settings = new AppSettings(false);
        
        settings.setUseJoysticks(true);
        settings.setSettingsDialogImage("OpenDS.png");
        settings.setTitle("OpenDS Analyzer");

		analyzer.setSettings(settings);
		
		analyzer.setPauseOnLostFocus(false);
		analyzer.start();
	}

}
