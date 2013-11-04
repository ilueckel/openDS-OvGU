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

package eu.opends.effects;

import com.jme3.math.ColorRGBA;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.FogFilter;
import com.jme3.post.filters.BloomFilter.GlowMode;

import eu.opends.main.Simulator;

/**
 * 
 * @author Rafael Math
 */
public class EffectCenter 
{
	private Simulator sim;
	private SnowParticleEmitter snowParticleEmitter;
	private RainParticleEmitter rainParticleEmitter;
	private boolean isSnowing;
	private boolean isRaining;
	private boolean isFog;
	private boolean isBloom;

	
	public EffectCenter(Simulator sim) 
	{
		this.sim = sim;
		
		WeatherSettings weatherSettings = Simulator.getDrivingTask().getScenarioLoader().getWeatherSettings();
		isSnowing = (weatherSettings.getSnowingPercentage() > 0);
		isRaining = (weatherSettings.getRainingPercentage() > 0);
		isFog = (weatherSettings.getFogPercentage() > 0);
		isBloom = false;
		
		if(isSnowing)
		{
			// init snow
			float percentage = Math.max(weatherSettings.getSnowingPercentage(),0);
			snowParticleEmitter = new SnowParticleEmitter(sim.getAssetManager(), percentage);
			sim.getRootNode().attachChild(snowParticleEmitter);
		}
		
		if(isRaining)
		{
			// init snow
			float percentage = Math.max(weatherSettings.getRainingPercentage(),0);
			rainParticleEmitter = new RainParticleEmitter(sim.getAssetManager(), percentage);
			sim.getRootNode().attachChild(rainParticleEmitter);
		}
		
		if(isFog || isBloom)
		{
		    FilterPostProcessor processor = new FilterPostProcessor(sim.getAssetManager());
		    
		    if(isFog)
		    {
		    	float percentage = Math.max(weatherSettings.getFogPercentage(),0);
			    FogFilter fog = new FogFilter();
		        fog.setFogColor(new ColorRGBA(0.9f, 0.9f, 0.9f, 1.0f));
		        fog.setFogDistance(155);
		        fog.setFogDensity(2.0f * (percentage/100f));
		        processor.addFilter(fog);
		    }
		    
		    if(isBloom)
		    {
		    	// ensure any object is set to glow, e.g. car chassis:
		    	// chassis.getMaterial().setColor("GlowColor", ColorRGBA.Orange);
		    	
		    	BloomFilter bloom = new BloomFilter(GlowMode.Objects);
		    	processor.addFilter(bloom);
		    }
		    
	        sim.getViewPort().addProcessor(processor);
		}
	}

	
	public void update(float tpf)
	{
		if(isSnowing)
			snowParticleEmitter.setLocalTranslation(sim.getCar().getPosition());
		
		if(isRaining)
			rainParticleEmitter.setLocalTranslation(sim.getCar().getPosition());
	}

}
