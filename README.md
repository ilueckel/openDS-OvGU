openDS-OvGU
===========

Webcam Capturing
-----------
There is a webcam capturing class implemented, that captures a picture every tick (about 50ms) and saves it to the HDD. The folder can be found under `analyzerData\{Capture date}\{0-9}` the subfolders are numeric and are indicating the different webcams (multiple webcam capturing is supported). Sound is recorded to the `RecordedAudio.wav` file


Self driving car emulator
-----------
For testing purposes there can be used a second steering wheel to emulate a self driving car. To enable this feature you need to add the following (adjustable) code to your driving task xml-file
```
<joystick2>
	<controllerID>1</controllerID>
	<steeringAxis>0</steeringAxis>
	<invertSteeringAxis>false</invertSteeringAxis>
	<pedalAxis>1</pedalAxis>
	<invertPedalAxis>false</invertPedalAxis>
	<steeringSensitivityFactor>0.5</steeringSensitivityFactor>
	<pedalSensitivityFactor>1.0</pedalSensitivityFactor>
	<keyAssignments>
	<keyAssignment function="start_engine" key="BUTTON_1" />
		<keyAssignment function="stop_engine" key="BUTTON_2" />
	</keyAssignments>
</joystick2>
```
 
 You can switch between steering wheel 1 and steering wheel 2 with the `B` keyboard key. A notification will be shown.