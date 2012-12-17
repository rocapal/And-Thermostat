/*
 *    Copyright (C) 2012 - Roberto Calvo Palomino
 * 
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *    
 *    
 *    Authors: Roberto Calvo <rocapal [at] gmail [dot] es>
 */


package es.rocapal.apps.termostato.usb;

import android.util.Log;

public class Controller {
			
	private final Double MAX_RANGE = 0.3;
	private final float INIT_VALUE = (float) -40.0;
	
	private final String TAG = getClass().getSimpleName();
	
	private float mRoomTemp;
	private float mProgrammedTemp;
	
	public Controller()
	{
		mRoomTemp = INIT_VALUE;
		mProgrammedTemp = INIT_VALUE;
	}

	public void setRoomTemp ( float temp )
	{
		mRoomTemp = temp;
	}
	
	public void setProgrammedTemp (float temp)
	{
		mProgrammedTemp = temp;
	}

	
	/**
	 * Calculate the status of the heat
	 * @return True if the heat must be power on. False if the heat must be power off.
	 */
	public boolean calcHeatStatus ()
	{
		if (mRoomTemp == INIT_VALUE || mProgrammedTemp == INIT_VALUE)
		{
			Log.e(TAG, "mRoomTemp or mProgrammedTemp not initialized!");
			return false;
		}
		
		float diff = mRoomTemp - mProgrammedTemp;
		
		if ( Math.abs(diff) >=  MAX_RANGE)
		{
			if (diff <= 0.0)
				return true;
			else
				return false;
		}
		
		return false;
	}
}
