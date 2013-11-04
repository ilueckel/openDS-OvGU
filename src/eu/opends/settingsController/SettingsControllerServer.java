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

package eu.opends.settingsController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import eu.opends.drivingTask.settings.SettingsLoader.Setting;
import eu.opends.main.SimulationDefaults;
import eu.opends.main.Simulator;

/**
 * 
 * @author Daniel Braun, Rafael Math
 */
public class SettingsControllerServer extends Thread
{
	private ServerSocket serverSocket = null;
	private Socket clientSocket = null;
	private PrintWriter out = null;
	private BufferedReader in = null;
	
	private Simulator sim;
	private int port = 0;
	private boolean connected = false;
	
	
	public SettingsControllerServer(Simulator sim)
	{
		this.sim = sim;
		this.port = Simulator.getSettingsLoader().getSetting(Setting.SettingsControllerServer_port, 
				SimulationDefaults.SettingsControllerServer_port);
	}
	
	
	public void run()
	{		
		String message = "";		
		
		try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.err.println("Could not listen on port:"+port);
            return;
        }
          
        System.out.println("settingsController Server started.");
        
        while(!isInterrupted())
        {
        	try {
        		
            	clientSocket = serverSocket.accept(); // wait for client	            
    	      	out = new PrintWriter(clientSocket.getOutputStream(), true);
    		  	in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    		  	connected = true;
    		  	
    	    } catch (IOException e) {
    	    	interrupt();
    	    }  
        	
	        while(!isInterrupted() && connected)
	        {
	        	message = "";
	        	String line = null;
	        	
	        	try{
	        		line = in.readLine();
	        		
	        		while(line != null){
	        			message = message + line;
	        			if(line.equals("</Message>")){
	        				while(in.ready())
	        					in.readLine(); //flush
	        				break;
	        			}
	        			message += "\n";
	        			line = in.readLine();
	        		}
	        		
	        	} catch(SocketException e){
	        		System.out.println("Client disconnected.");
	        		connected = false;
	        		break;
	        	} catch (Exception e){
	            	System.out.println(e);
	            	connected = false;
	            	break;
	        	}
       		
	        	if(message.equals("") || message.length() < 2){
	        		connected = false;
	        		break;
	        	}
	        	
	        	String[] splitMessage = message.split("\\n", 4);
	        	
	        	String sizeOfMsg = splitMessage[0];
	        	String status = splitMessage[1];
	        	String messageTag = splitMessage[2];
	        	
	        	parseXML(splitMessage[3]);
	        }
        }
        
        System.out.println("Server closed.");
		
	}
	
	private static Document loadXMLFromString(String xml) throws Exception
    {		
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        return builder.parse(is);
    }
	
	/*
	private static String getValue(String tag, Element element) {
		NodeList nodes = element.getElementsByTagName(tag).item(0).getChildNodes();
		Node node = (Node) nodes.item(0);
		return node.getNodeValue();
	}
	*/
	
	private void parseXML(String xml) {
		try {			
			Document doc = loadXMLFromString(xml);			
			doc.getDocumentElement().normalize();			
			String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
			
			
			NodeList nodes = doc.getElementsByTagName("Event");			
			String eventName = (((Element) nodes.item(0)).getAttribute("Name"));
			
			
			
			nodes = doc.getElementsByTagName("DataValue");
			DataValue[] dataValues = new DataValue[nodes.getLength()];
			for (int i = 0; i < nodes.getLength(); i++) {
				dataValues[i] = new DataValue((((Element) nodes.item(i)).getAttribute("Feature")), (((Element) nodes.item(i)).getAttribute("Value")));								
			}
			
			if(eventName.equals("EstablishConnection")){
				System.out.println("EstablishConnection"); //TODO
				
				response += "<Message><Event Name=\"ConnectionEstablished\"/>\n</Message>\n";			
			}
			else if(eventName.equals("DLChangeEvent")){
				System.out.println("DLChangeEvent"); //TODO
				
				response += "<Message><Event Name=\"DLStatusEvent\"/>\n<Data><DataEntry Type=\"StringList\">\n<!-- identifier for the task that initiated the dl-change -->\n<DataValue Feature=\"id\" Value=\""+dataValues[0].getValue()+"\"/>\n<!-- info if dl-status change was successful -->\n<DataValue Feature=\"status\" Value=\"DLC_SUCCESS | DLC_FAIL\"/>\n<!-- example: further information if the dl-status could not be changed -->\n<DataValue Feature=\"info\" Value=\"could not be changed because of ...\"/>\n</DataEntry></Data>\n</Message>";
			}
			else if(eventName.equals("MarkerEvent")){
				System.out.println("MarkerEvent"); //TODO
				
				response += "<Message><Event Name=\"MarkerStatusEvent\"/>\n<Data><DataEntry Type=\"StringList\">\n<!-- identifier for the task that controls the recording -->\n<DataValue Feature=\"id\" Value=\""+dataValues[0].getValue()+"\"/>\n<DataValue Feature=\"status\" Value=\"RECORDING_STARTED | RECORDING_STOPPED | RECORDING_START_FAILED | RECORDING_STOP_FAILED\"/>\n<!-- example: further information if the markers can't be set/removed-->\n<DataValue Feature=\"info\" Value=\"error because of ...\"/>\n</DataEntry></Data>\n</Message>";
			}
			else{
				System.err.println("Unknow event received!");
				return;
			}
			
			final byte[] utf8Bytes = response.getBytes("UTF-8");
			
			System.out.println(utf8Bytes.length);
        	
        	byte[] length = new byte[4];
        	ByteBuffer buff = ByteBuffer.allocate(4);
        	buff.order(ByteOrder.LITTLE_ENDIAN);
        	length = buff.putInt(utf8Bytes.length).array();			
			
			response = (length[0] & 0xff) + " " + (length[1] & 0xff) + " " + (length[2] & 0xff) + " " + (length[3] & 0xff) + "\n"+
						"0 0 0 0\n" +
						"100 0 0 0\n" +
						response;
			
			out.println(response);
			
		} catch (Exception e) {
			System.err.println("No valid XML data received!");
			e.printStackTrace();
		}
		
		
		
		
	}

	public void close(){		
		if(connected){
			try {
				out.close();
		        in.close();
		        clientSocket.close();			
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		interrupt();
	}	
        
        
}