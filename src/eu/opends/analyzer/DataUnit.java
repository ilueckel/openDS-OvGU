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

package eu.opends.analyzer;

import java.io.Serializable;
import java.util.Date;

/**
 * Data object containing position and speed of the car, as well as the current
 * date. Based on <code>AnalyzationData.java</code> from the CARS-project.
 * 
 * @author Marco Mueller
 * 			Anpassungen Martin Michael Kalbitz
 */
public class DataUnit implements Serializable 
{
	private static final long serialVersionUID = -8293989514037755782L;
	private double xpos, ypos, zpos, speed, steeringWheelPos, gasPedalPos, brakePedalPos,
			xrot, yrot, zrot, wrot;
	private Date date;
	private int lightIntensity;
	private boolean enginOn, blinkerLeft, blinkerRight;
	

	/**
	 * The default constructor.
	 * 
	 * @param xpos
	 *            The position of the car on the x axis.
	 * @param ypos
	 *            The position of the car on the y axis.
	 * @param zpos
	 *            The position of the car on the z axis.
	 * @param speed
	 *            The current speed of the car in kilometers per hour.
	 * @param date
	 *            The date, when the data set was taken.
	 * @param steeringWheelPos
	 *            The position of the steering wheel: -1 full left, 0 centered,
	 *            1 full right.
	 * @param gasPedalPos
	 *            The position of the gas pedal: 0 no acceleration, 1 full acceleration
	 * @param brakePedalPos
	 *            The position of the brake pedal: -1 full break/negative acceleration, 0 no acceleration
	 */

	public DataUnit(Date date, double xpos, double ypos, double zpos,
			double xrot, double yrot, double zrot, double wrot, double speed,
			double steeringWheelPos, double gasPedalPos, double brakePedalPos,
			boolean enginOn, int lightIntensity, boolean blinkerLeft, boolean blinkerRight) 
	{
		setDate(date);
		setSpeed(speed);
		setXpos(xpos);
		setYpos(ypos);
		setZpos(zpos);
		setXrot(xrot);
		setYrot(yrot);
		setZrot(zrot);
		setWrot(wrot);
		setSteeringWheelPos(steeringWheelPos);
		setGasPedalPos(gasPedalPos);
		setBrakePedalPos(brakePedalPos);
		setEnginOn(enginOn);
		setLightIntensity(lightIntensity);
		setBlinkerLeft(blinkerLeft);
		setBlinkerRight(blinkerRight);
	}


	public double getXrot() {
		return xrot;
	}

	private void setXrot(double xrot) {
		this.xrot = xrot;
	}

	public double getYrot() {
		return yrot;
	}

	private void setYrot(double yrot) {
		this.yrot = yrot;
	}

	public double getZrot() {
		return zrot;
	}

	private void setZrot(double zrot) {
		this.zrot = zrot;
	}

	public double getWrot() {
		return wrot;
	}

	private void setWrot(double wrot) {
		this.wrot = wrot;
	}

	/**
	 * 
	 * @return The position of the car on the x axis.
	 */
	public double getXpos() {
		return xpos;
	}

	/**
	 * @param xpos
	 *            The position of the car on the x axis.
	 */
	private void setXpos(double xpos) {
		this.xpos = xpos;
	}

	/**
	 * 
	 * @return The position of the car on the y axis.
	 */
	public double getYpos() {
		return ypos;
	}

	/**
	 * 
	 * @param ypos
	 *            The position of the car on the y axis.
	 */
	private void setYpos(double ypos) {
		this.ypos = ypos;
	}

	/**
	 * 
	 * @return The speed of the car in kilometers per hour.
	 */
	public double getSpeed() {
		return speed;
	}

	/**
	 * 
	 * @param speed
	 *            The speed of the car in kilometers per hour.
	 */
	private void setSpeed(double speed) {
		this.speed = speed;
	}

	/**
	 * @return The date set for the data set.
	 */
	public Date getDate() {
		return date;
	}

	/**
	 * 
	 * @param date
	 *            The date of the data set.
	 */
	private void setDate(Date date) {
		this.date = date;
	}

	/**
	 * 
	 * @return The position of the car on the z axis.
	 */
	public double getZpos() {
		return zpos;
	}

	/**
	 * 
	 * @param zpos
	 *            The position of the car on the z axis.
	 */
	private void setZpos(double zpos) {
		this.zpos = zpos;
	}

	/**
	 * @return The position of the steering wheel: -1 full left, 0 centered, 1
	 *         full right.
	 */
	public double getSteeringWheelPos() {
		return steeringWheelPos;
	}

	/**
	 * 
	 * @param steeringWheelPos
	 *            The position of the steering wheel: -1 full left, 0 centered,
	 *            1 full right.
	 */
	private void setSteeringWheelPos(double steeringWheelPos) {
		this.steeringWheelPos = steeringWheelPos;
	}

	/**
	 * 
	 * @return The position of the pedals: -1 full break/negative acceleration,
	 *         0 no acceleration, 1 full acceleration
	 */
	public double getPedalPos() {
		return gasPedalPos;
	}

	/**
	 * 
	 * @param pedalPos
	 *            The position of the pedals: -1 full break/negative
	 *            acceleration, 0 no acceleration, 1 full acceleration
	 */
	private void setGasPedalPos(double pedalPos) {
		this.gasPedalPos = pedalPos;
	}


	/**
	 * 
	 * @return true if the car is breaking, else false
	 */
	public double isBreaking() {
		return brakePedalPos;
	}

	/**
	 * 
	 * @param brakePedalPos
	 *            true if the car is breaking, else false
	 */
	private void setBrakePedalPos(double brakePedalPos) {
		this.brakePedalPos = brakePedalPos;
	}


	public boolean isEnginOn() {
		return enginOn;
	}


	public void setEnginOn(boolean enginOn) {
		this.enginOn = enginOn;
	}


	public int getLightIntensity() {
		return lightIntensity;
	}


	public void setLightIntensity(int lightIntensity) {
		this.lightIntensity = lightIntensity;
	}


	public boolean isBlinkerLeft() {
		return blinkerLeft;
	}


	public void setBlinkerLeft(boolean blinkerLeft) {
		this.blinkerLeft = blinkerLeft;
	}


	public boolean isBlinkerRight() {
		return blinkerRight;
	}


	public void setBlinkerRight(boolean blinkerRight) {
		this.blinkerRight = blinkerRight;
	}


}
