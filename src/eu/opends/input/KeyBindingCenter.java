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

package eu.opends.input;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.jme3.input.InputManager;
import com.jme3.input.JoyInput;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.InputListener;
import com.jme3.input.controls.JoyAxisTrigger;
import com.jme3.input.controls.JoyButtonTrigger;
import com.jme3.input.controls.KeyTrigger;

import eu.opends.basics.SimulationBasics;
import eu.opends.drivingTask.settings.SettingsLoader;
import eu.opends.drivingTask.settings.SettingsLoader.Setting;
import eu.opends.main.DriveAnalyzer;
import eu.opends.main.Simulator;

/**
 * 
 * @author Rafael Math
 */
public class KeyBindingCenter 
{
	private List<KeyBindingEntry> keyBindingList = new ArrayList<KeyBindingEntry>();
	
	private InputManager inputManager;
	
    public KeyBindingCenter(SimulationBasics sim) 
    {
    	inputManager = sim.getInputManager();
    	
    	// disable shutdown by ESCAPE button
    	inputManager.deleteMapping("SIMPLEAPP_Exit");
    	
    	if(sim instanceof Simulator)
	    	assignSimulatorKeys((Simulator)sim);

    	else if(sim instanceof DriveAnalyzer)
    		assignDriveAnalyzerKeys((DriveAnalyzer)sim);
	}

    
    public List<KeyBindingEntry> getKeyBindingList()
    {
    	return keyBindingList;
    }
    
    
    private void addKeyMapping(KeyMapping keyMapping, InputListener inputListener)
    {
    	String[] keys = keyMapping.getKeys();
    	
    	if((keys != null) && (keys.length >= 1))
    	{
        	String returnString = null;
        	for(KeyBindingEntry entry : keyBindingList)
        	{
        		if(entry.getDescription().equals(keyMapping.getDescription()))
        		{
        			returnString = entry.getKeyList();
        			break;
        		}
        	}

	    	for(String key : keys)
	    	{
				try {
		
					String keyString = "KEY_" + key;
					Field field = KeyInput.class.getField(keyString);
					int keyNumber = field.getInt(KeyInput.class);
					inputManager.addMapping(keyMapping.getID(), new KeyTrigger(keyNumber));
					inputManager.addListener(inputListener, keyMapping.getID());
					if(returnString == null)
						returnString = key;
					else
						returnString += ", " + key;
					
				} catch (Exception e) {
					System.err.println("Invalid key '" + key + "' for key binding '" + keyMapping.getID() + "'");
				}
	    	}
	
	    	keyBindingList.add(new KeyBindingEntry(keyMapping.getDescription(), returnString));
    	}
    }
    

	private void assignSimulatorKeys(Simulator simulator) 
	{		
		// ACTION
		InputListener simulatorActionListener = new SimulatorActionListener(simulator);
		for(KeyMapping keyMapping : KeyMapping.getSimulatorActionKeyMappingList())
			addKeyMapping(keyMapping, simulatorActionListener);
		
		inputManager.addMapping(KeyMapping.TOGGLE_AUTOMATIC.getID(), new JoyButtonTrigger(0,1));
		inputManager.addMapping(KeyMapping.TOGGLE_CAM.getID(), new JoyButtonTrigger(0,3));
		inputManager.addMapping(KeyMapping.REPORT_LANDMARK.getID(), new JoyButtonTrigger(0,7));
		inputManager.addMapping(KeyMapping.SHIFT_DOWN.getID(), new JoyButtonTrigger(0,8));
		inputManager.addMapping(KeyMapping.SHIFT_UP.getID(), new JoyButtonTrigger(0,9));
		inputManager.addMapping(KeyMapping.CLOSE_INSTRUCTION_SCREEN.getID(), new JoyButtonTrigger(0,14));
		inputManager.addListener(simulatorActionListener, KeyMapping.TOGGLE_CAM.getID());

		
		// ANALOG
        inputManager.addMapping("DPAD Left", new JoyAxisTrigger(0, JoyInput.AXIS_POV_X, true));
        inputManager.addMapping("DPAD Right", new JoyAxisTrigger(0, JoyInput.AXIS_POV_X, false));
        inputManager.addMapping("DPAD Down", new JoyAxisTrigger(0, JoyInput.AXIS_POV_Y, true));
        inputManager.addMapping("DPAD Up", new JoyAxisTrigger(0, JoyInput.AXIS_POV_Y, false));
        
        
        if(inputManager.getJoysticks().length > 0 && inputManager.getJoysticks()[0].getName().equals("Logitech Driving Force GT USB"))
        {
        	inputManager.addMapping("Joy Up", new JoyAxisTrigger(0, 2, false));
        	inputManager.addMapping("Joy Down", new JoyAxisTrigger(0, 2, true));
        	inputManager.addMapping("Joy Right", new JoyAxisTrigger(0, 1, false));
        	inputManager.addMapping("Joy Left", new JoyAxisTrigger(0, 1, true));
        }
        else
        {
        	inputManager.addMapping("Joy Left", new JoyAxisTrigger(0, 0, true));
        	inputManager.addMapping("Joy Right", new JoyAxisTrigger(0, 0, false));
        	inputManager.addMapping("Joy Down", new JoyAxisTrigger(0, 1, true));
        	inputManager.addMapping("Joy Up", new JoyAxisTrigger(0, 1, false));
        }
        
        
        SettingsLoader settingsLoader = Simulator.getSettingsLoader();
        int controllerID = settingsLoader.getSetting(Setting.Joystick_controllerID, 0);
        int pedalAxis = settingsLoader.getSetting(Setting.Joystick_pedalAxis, 2);
        boolean invertPedalAxis = settingsLoader.getSetting(Setting.Joystick_invertPedalAxis, false);
        int steeringAxis = settingsLoader.getSetting(Setting.Joystick_steeringAxis, 1);
        boolean invertSteeringAxis = settingsLoader.getSetting(Setting.Joystick_invertSteeringAxis, false);
        
        inputManager.addMapping("Joy Up", new JoyAxisTrigger(controllerID, pedalAxis, invertPedalAxis));
    	inputManager.addMapping("Joy Down", new JoyAxisTrigger(controllerID, pedalAxis, !invertPedalAxis));
    	inputManager.addMapping("Joy Right", new JoyAxisTrigger(controllerID, steeringAxis, invertSteeringAxis));
    	inputManager.addMapping("Joy Left", new JoyAxisTrigger(controllerID, steeringAxis, !invertSteeringAxis));
    	
        inputManager.addListener(new SimulatorAnalogListener(simulator), "DPAD Left", "DPAD Right", 
        		"DPAD Down", "DPAD Up", "Joy Left", "Joy Right", "Joy Down", "Joy Up");
	}
	
	
	private void assignDriveAnalyzerKeys(DriveAnalyzer analyzer) 
	{
		//remove arrow key's mapping for chase camera
		inputManager.deleteTrigger("FLYCAM_Left", new KeyTrigger(KeyInput.KEY_LEFT));
		inputManager.deleteTrigger("FLYCAM_Right", new KeyTrigger(KeyInput.KEY_RIGHT));
		inputManager.deleteTrigger("FLYCAM_Up", new KeyTrigger(KeyInput.KEY_UP));
		inputManager.deleteTrigger("FLYCAM_Down", new KeyTrigger(KeyInput.KEY_DOWN));
		

		// ACTION
		InputListener driveAnalyzerActionListener = new DriveAnalyzerActionListener(analyzer);
		for(KeyMapping keyMapping : KeyMapping.getDriveAnalyzerActionKeyMappingList())
			addKeyMapping(keyMapping, driveAnalyzerActionListener);


		// ANALOG
		InputListener driveAnalyzerAnalogListener = new DriveAnalyzerAnalogListener(analyzer);
		for(KeyMapping keyMapping : KeyMapping.getDriveAnalyzerAnalogKeyMappingList())
			addKeyMapping(keyMapping, driveAnalyzerAnalogListener);
	}
}
