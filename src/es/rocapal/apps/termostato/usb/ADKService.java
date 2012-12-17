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

import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ADKService extends IntentService {

	
	public ADKService ()
	{
		super("ADKService");
	}
	
	public ADKService(String name) {
		super(name);		
	}
	

	private static Context mActivityContext;
		
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	
	@Override
    public void onCreate() {
		super.onCreate(); 
				
    }
	
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {		
		super.onStartCommand(intent, flags, startId);	  
		
		initService();
		Log.d("ADKService", "onStartCommand");
		return Service.START_STICKY;
	}
	
	@Override
	public void onDestroy() {		
		super.onDestroy();

		Log.d("ADKService", "onDestroy");
		
		//ThermAccessory.getInstance().unRegister();
    	ThermAccessory.getInstance().closeAccessory();
	}
	
	public static void setListener (IADKService listener)
	{
		ThermAccessory.getInstance().setListener(listener);
	}
	
	public static void onResume ()
	{
		ThermAccessory.getInstance().onResumeAccessory();
	}
	
	public static void setContext (Context context)
	{
		mActivityContext = context;
	}
	
	public static void updateProgrammedTemp (float temp)
	{
		ThermAccessory.getInstance().setProgrammedTemp (temp);
	}

	public void initService ()
	{
		ThermAccessory.getInstance().initUSBManager(mActivityContext);
	}

	@Override
	protected void onHandleIntent(Intent arg0) {
		// TODO Auto-generated method stub
		
	}

	

}
