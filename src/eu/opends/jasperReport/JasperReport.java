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

package eu.opends.jasperReport;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.math.FastMath;

import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;

import eu.opends.drivingTask.settings.SettingsLoader.Setting;
import eu.opends.main.SimulationDefaults;
import eu.opends.main.Simulator;
import eu.opends.steeringTask.SteeringTaskSettings;
import eu.opends.steeringTask.SteeringTask.SteeringTaskType;
import eu.opends.tools.Util;

/**
 * Class for handling database connection and logging to database
 * 
 * @author Rafael Math
 */
public class JasperReport 
{
	private SteeringTaskType steeringTaskType;
    private Connection connection;
    private PreparedStatement statement;
    private PreparedStatement additionalStatement;
    private boolean useAdditionalTable;
    
    // TODO: load from settings.xml
    private String outputFolder;
    private String fileName = "report.pdf";
	private String driverReportTemplate = "assets/JasperReports/templates/driver.jasper";
	private String passengerReportTemplate = "assets/JasperReports/templates/passenger.jasper";
	private boolean createReport = true;
	private boolean openReport = true;
	private float maxDeviation = 1; // --> blinking threshold

    
    /**
     * Constructor, that creates database connection and prepared statement for fast query execution
     */
    public JasperReport(SteeringTaskType steeringTaskType) 
    {
    	this.steeringTaskType = steeringTaskType;
    	this.outputFolder = Simulator.getOutputFolder();
    	
    	boolean suppressOpen = Simulator.getSettingsLoader().getSetting(Setting.Analyzer_suppressPDFPopup, 
				SimulationDefaults.Analyzer_suppressPDFPopup);
    	
    	openReport = !suppressOpen;
    	
        try {
        	
            // Loading database connection driver for MySQL server connection
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            
            // load settings from driving task
    		SteeringTaskSettings steeringTaskSettings = Simulator.getDrivingTask().
    				getScenarioLoader().getSteeringTaskSettings();
    		String url = steeringTaskSettings.getDatabaseUrl();
    		String user = steeringTaskSettings.getDatabaseUser();
    		String pass = steeringTaskSettings.getDatabasePassword();
    		String table = steeringTaskSettings.getDatabaseTable();
    		
    		// TODO load from settings.xml
    		useAdditionalTable = true; //steeringTaskSettings.getClearTable();
    		
            // Creating connection to local database
            connection = DriverManager.getConnection(url, user, pass);

            if(!connection.isClosed())
            	System.out.println("Successfully connected to MySQL server using TCP/IP...");
            
            // Creating prepared statement for faster query execution all "?" then have to be assigned some value using statement.set[Float,Int,Long,String,etc]
            statement = connection.prepareStatement("INSERT INTO `" + table + "` (subject_name, is_main_driver, condition_name, condition_number, " +
            			"lateral_target_pos, lateral_steering_pos, steering_deviation, light_state, co_driver_reaction, co_driver_reaction_time, brake_reaction, " +
            			"brake_reaction_time_driver, brake_reaction_time_co_driver, acceleration_reaction, acceleration_reaction_time_driver, " +
            			"acceleration_reaction_time_co_driver, absolute_time, gesture_reaction, gesture_reaction_time, lat_relevant_building, lon_relevant_building, " +
            			"x_screen_coordinate, y_screen_coordinate, non_relevant_buildings, experimentTime) VALUES (?, ?, ?, ?,   ?, ?, ?, ?, ?, ?, ?,   ?, ?, ?, ?,  " +
            			" ?, ?, ?, ?, ?,   ?, ?, ?, ?,?);");


            PreparedStatement clearStatement = connection.prepareStatement("TRUNCATE TABLE " + table);
            clearStatement.executeUpdate();
            
            if(useAdditionalTable)
            {
            	String additionalTable = table + "_" + Simulator.getOutputFolder().replace("analyzerData/", "");
            	
            	// create new table
            	PreparedStatement newTableStatement = connection.prepareStatement(
            			"CREATE TABLE IF NOT EXISTS `" + additionalTable + "` (" +
            			"`subject_name` varchar(100) default NULL," +
            			"`is_main_driver` tinyint(1) default NULL," +
            			"`condition_name` varchar(20) default NULL," +
            			"`condition_number` bigint(20) default NULL," +
            			"`lateral_target_pos` float default NULL," +
            			"`lateral_steering_pos` float default NULL," +
            			"`steering_deviation` float default NULL," +
            			"`light_state` varchar(20) default NULL," +
            			"`co_driver_reaction` int(11) default NULL," +
            			"`co_driver_reaction_time` bigint(20) default NULL," +
            			"`brake_reaction` int(11) default NULL," +
            			"`brake_reaction_time_driver` bigint(20) default NULL," +
            			"`brake_reaction_time_co_driver` bigint(20) default NULL," +
            			"`acceleration_reaction` int(11) default NULL," +
            			"`acceleration_reaction_time_driver` bigint(20) default NULL," +
            			"`acceleration_reaction_time_co_driver` bigint(20) default NULL," +
            			"`absolute_time` bigint(20) default NULL," +
            			"`gesture_reaction` int(11) default NULL," +
            			"`gesture_reaction_time` bigint(20) default NULL," +
            			"`lat_relevant_building` float default NULL," +
            			"`lon_relevant_building` float default NULL," +
            			"`x_screen_coordinate` float default NULL," +
            			"`y_screen_coordinate` float default NULL," +
            			"`non_relevant_buildings` int(11) default NULL," +
            			"`experimentTime` bigint(20) default NULL) ENGINE=MyISAM DEFAULT CHARSET=ascii;");
            	newTableStatement.executeUpdate();
            	
            	additionalStatement = connection.prepareStatement("INSERT INTO `" + additionalTable + "` (subject_name, is_main_driver, condition_name, condition_number, " +
            			"lateral_target_pos, lateral_steering_pos, steering_deviation, light_state, co_driver_reaction, co_driver_reaction_time, brake_reaction, " +
            			"brake_reaction_time_driver, brake_reaction_time_co_driver, acceleration_reaction, acceleration_reaction_time_driver, " +
            			"acceleration_reaction_time_co_driver, absolute_time, gesture_reaction, gesture_reaction_time, lat_relevant_building, lon_relevant_building, " +
            			"x_screen_coordinate, y_screen_coordinate, non_relevant_buildings, experimentTime) VALUES (?, ?, ?, ?,   ?, ?, ?, ?, ?, ?, ?,   ?, ?, ?, ?,  " +
            			" ?, ?, ?, ?, ?,   ?, ?, ?, ?,?);");
            }
            	
        } catch(Exception e) {

        	e.getStackTrace();
        }
    }


    // Method, which writes record to database. It assigns to each "?" in prepared statement definite value and then executes update inserting record in database
    public void addDataSet(String subjectName, boolean isMainDriver, String conditionName, long conditionNumber,
    							float lateralTargetPos, float lateralSteeringPos, float steeringDeviation, 
    							String lightState, int coDriverReaction, long coDriverReactionTime, int brakeReaction,
                                long brakeReactionTimeDriver, long brakeReactionTimeCoDriver, int accelReaction, 
                                long accelReactionTimeDriver, long accelReactionTimeCoDriver, long absoluteTime, 
                                int gestureReaction, long gestureReactionTime, float latRelBuilding, float lonRelBuilding, 
                                float xCoordinate, float yCoordinate, int nonRelBuildings, long experimentTime)
    {
        try {
            statement.setString(1, subjectName);
            statement.setBoolean(2, isMainDriver);
            statement.setString(3, conditionName);
            statement.setLong(4, conditionNumber);
            
            statement.setFloat(5, lateralTargetPos);
            statement.setFloat(6, lateralSteeringPos);
            statement.setFloat(7, steeringDeviation);
            statement.setString(8, lightState);
            statement.setInt(9, coDriverReaction);
            statement.setLong(10, coDriverReactionTime);
            
            statement.setInt(11, brakeReaction);
            statement.setLong(12, brakeReactionTimeDriver);
            statement.setLong(13, brakeReactionTimeCoDriver);
            
            statement.setInt(14, accelReaction);
            statement.setLong(15, accelReactionTimeDriver);
            statement.setLong(16, accelReactionTimeCoDriver);
            
            statement.setLong(17, absoluteTime);
            statement.setInt(18, gestureReaction);
            statement.setLong(19, gestureReactionTime);
            
            statement.setFloat(20, latRelBuilding);            
            statement.setFloat(21, lonRelBuilding);
            statement.setFloat(22, xCoordinate);
            statement.setFloat(23, yCoordinate);
            
            statement.setInt(24, nonRelBuildings);
            statement.setLong(25, experimentTime);
            
            statement.executeUpdate();
            
            if(useAdditionalTable)
            {
                additionalStatement.setString(1, subjectName);
                additionalStatement.setBoolean(2, isMainDriver);
                additionalStatement.setString(3, conditionName);
                additionalStatement.setLong(4, conditionNumber);
                
                additionalStatement.setFloat(5, lateralTargetPos);
                additionalStatement.setFloat(6, lateralSteeringPos);
                additionalStatement.setFloat(7, steeringDeviation);
                additionalStatement.setString(8, lightState);
                additionalStatement.setInt(9, coDriverReaction);
                additionalStatement.setLong(10, coDriverReactionTime);
                
                additionalStatement.setInt(11, brakeReaction);
                additionalStatement.setLong(12, brakeReactionTimeDriver);
                additionalStatement.setLong(13, brakeReactionTimeCoDriver);
                
                additionalStatement.setInt(14, accelReaction);
                additionalStatement.setLong(15, accelReactionTimeDriver);
                additionalStatement.setLong(16, accelReactionTimeCoDriver);
                
                additionalStatement.setLong(17, absoluteTime);
                additionalStatement.setInt(18, gestureReaction);
                additionalStatement.setLong(19, gestureReactionTime);
                
                additionalStatement.setFloat(20, latRelBuilding);            
                additionalStatement.setFloat(21, lonRelBuilding);
                additionalStatement.setFloat(22, xCoordinate);
                additionalStatement.setFloat(23, yCoordinate);
                
                additionalStatement.setInt(24, nonRelBuildings);
                additionalStatement.setLong(25, experimentTime);
                
                additionalStatement.executeUpdate();
            }

        } catch (SQLException ex) {
        	
            Logger.getLogger(JasperReport.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    
    /**
     * Closes database connection after logging ended
     */
    public void createPDF()
    {
        try {
        	
        	boolean reportCreated = false;
        	
        	if(createReport)
        		reportCreated = createReport();

        	if(reportCreated && openReport)
				Util.open(outputFolder + "/" + fileName);
			
			if(statement != null)
				statement.close();
			
			if(additionalStatement != null)
				additionalStatement.close();
			
			if(connection != null)
				connection.close();
            
        } catch (SQLException ex) {
            Logger.getLogger(JasperReport.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


	private boolean createReport()
	{
		boolean success = false;
		
		try{
			
			/*
			// used for source file
			InputStream inputStream = new FileInputStream("assets/JasperReports/templates/driver.jrxml");
			JasperDesign reportDesign = JRXmlLoader.load(inputStream);
			JasperReport report = JasperCompileManager.compileReport(reportDesign);
			*/
			
			// maybe try XML data source instead of database connection
			//JRDataSource dataSource = new JaxenXmlDataSource(new File("input.xml"));
			
			//get report template for driver or passenger task
			InputStream reportStream;
			if(steeringTaskType == SteeringTaskType.DRIVER)
				reportStream = new FileInputStream(driverReportTemplate);
			else
				reportStream = new FileInputStream(passengerReportTemplate);
			
			// fill report with parameters and data from database
			Map<String, Object> parameters = new HashMap<String, Object>();
			parameters.put("maxDeviation", new Float(FastMath.abs(maxDeviation)));
			JasperPrint print = JasperFillManager.fillReport(reportStream, parameters, connection);
			
			// create PDF file
			long start = System.currentTimeMillis();
			JasperExportManager.exportReportToPdfFile(print, outputFolder + "/" + fileName);
			System.out.println("PDF creation time : " + (System.currentTimeMillis() - start) + " ms");
			
			success = true;
			
		} catch (Exception e) {

			//System.err.println("Could not create report. Maybe PDF is still open?");
			e.printStackTrace();
		}
		
		return success;
	}

}
