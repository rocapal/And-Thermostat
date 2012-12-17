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


package es.rocapal.apps.termostato;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import es.rocapal.apps.termostato.usb.ADKService;
import es.rocapal.apps.termostato.usb.IADKService;

public class Main extends Activity {

	TextView tvDate, tvTemp, tvEnvTemp, tvUsbStatus, tvHeatStatus;
	Timer timer;
	Button btUp, btDown;
	
	private final String TAG = getClass().getSimpleName();
				
	public static Main mContext;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mContext = this;
        setupWidgets();
                
        
        //ThermAccessory.getInstance().initUSBManager(this);       
        
        ADKService.setContext(this);
        
        ADKService.setListener(new IADKService() {
			
			@Override
			public void notifyTemperature(final float temperature) {
				
				Main.this.runOnUiThread(new Runnable() {					
					@Override
					public void run() {
						if (tvEnvTemp != null)
							tvEnvTemp.setText( String.format("%.2f", temperature ));
						else
							Log.e(TAG, "tvEnvTemp instance is null");
						
					}
				});
				
			}
			
			@Override
			public void notifyAccessoryStatus (final boolean status) {
				
				Main.this.runOnUiThread(new Runnable() {					
					@Override
					public void run() {
						if (tvUsbStatus != null)
							tvUsbStatus.setText(status ? "USB Connected" : "USB Disconnected");
						else
							Log.e(TAG, "tvStatus instance is null");						
					}
				});				
				
			}

			@Override
			public void notifyHeatStatus(final boolean status) {
				Main.this.runOnUiThread(new Runnable() {
					
					@Override
					public void run() {						
						tvHeatStatus.setText (status ? "HEAT: ON" : "HEAT: OFF");						
					}
				});
				
			}
		});
                
        startService(new Intent(this, ADKService.class));		
        
        // Init programmed Temp
        updateProgrammedTemp((float)(0.0));
        
    }
    
    @Override
	public void onPause() {
		super.onPause();
		Log.d(TAG, "onPause");
	}
    
    @Override
    protected void onDestroy() {    	
    	super.onDestroy();
    	
    	Log.d(TAG, "onDestroy");
    	if (timer != null)
    		timer.cancel();    	    	
    	
    	//ThermAccessory.getInstance().closeAccessory();
    	//ThermAccessory.getInstance().unRegister();
    }
	
    @Override
	public void onResume() {
		super.onResume();
		//ThermAccessory.getInstance().onResumeAccessory();
		ADKService.onResume();
	}
    
    private void setupWidgets()
    {
    	tvUsbStatus = (TextView) this.findViewById(R.id.tvUsbStatus);
    	tvHeatStatus = (TextView) this.findViewById(R.id.tvHeatStatus);
        tvDate = (TextView) this.findViewById(R.id.tvDate);
        tvTemp = (TextView) this.findViewById(R.id.tvTemp);
        tvEnvTemp = (TextView) this.findViewById(R.id.tvEnvTemp);
        btUp = (Button) this.findViewById(R.id.btUp);
        btDown = (Button) this.findViewById(R.id.btDown);
        
        // Timer for the clock
        timer = new Timer("DigitalClock"); 
        timer.scheduleAtFixedRate(new TimerTask() {
        	@Override
        	public void run() {
        		runOnUiThread(updateTimeTask);
        	}
        }, 1, 1000);
        
        // Listener for the buttons
        btUp.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				updateProgrammedTemp((float) +0.5);				
			}
		});
        
        btDown.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				updateProgrammedTemp((float) -0.5);		
			}
		});
        
    }
    
    private void updateProgrammedTemp (float change)
    {
    	float temp = Float.parseFloat((String) tvTemp.getText());
    	temp = temp + change;
    	tvTemp.setText (String.valueOf(temp));
    	
    	ADKService.updateProgrammedTemp (temp);
    }
	
	final Runnable updateTimeTask = new Runnable() {
		public void run() {
			
			String date = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
			String[] mydate = date.split(",");			
			tvDate.setText(mydate[1]);
		} 
	};

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
}
