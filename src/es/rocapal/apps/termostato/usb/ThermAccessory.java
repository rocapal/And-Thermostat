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

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;


public class ThermAccessory {
	
	public static final String ACTION_USB_PERMISSION = "es.rocapal.apps.termostato.USB_PERMISSION";
	
	private static final int ACTION_GET_TEMPERATURE = 0;
	private static final int ACTION_GET_HEAT_STATUS = 1;
	private static final int ACTION_SET_HEAT_STATUS = 2;
	
	private final String TAG = getClass().getSimpleName();
	private final Integer UPDATED_TIME = 1000;
	
	static ThermAccessory mInstance = null; 
	
	private UsbManager mUsbManager;
	private PendingIntent mPermissionIntent;
	private boolean mPermissionRequestPending;	
	private ParcelFileDescriptor mFileDescriptor;	
	private static UsbAccessory mAccessory = null;
	private FileInputStream mInputStream = null;
	private FileOutputStream mOutputStream = null;
	
	private Context mContext;	
	private Timer mTempTimer = null;
	private static ArrayList<IADKService> mIArray = new ArrayList<IADKService>();

	private Controller mController;
	
	public static ThermAccessory getInstance ()
	{
		if (mInstance == null)
			mInstance = new ThermAccessory();
		
		return mInstance;
	}
	
	private ThermAccessory ()
	{	
		mController = new Controller();
	}
	
	public void initUSBManager (Context ctx)
	{				
		
		mContext = ctx;
		mUsbManager = UsbManager.getInstance(mContext);
        
		mPermissionIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		ctx.registerReceiver(mUsbReceiver, filter);
        
		if (mAccessory != null )
		{			
			openAccessory(mAccessory);			
		}
		else
		{
			notifyAccesoryStatus(false);
			Log.e(TAG,"Error to openAccesory");
		}
		
		
	}
	
	
	private BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();						
			
			if (ACTION_USB_PERMISSION.equals(action)) {
				
				synchronized (this) {
					
					UsbAccessory accessory = UsbManager.getAccessory(intent);
					
					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) 
					{
						openAccessory(accessory);
					} else {
						Log.d("Main", "permission denied for accessory " + accessory);
					}
					mPermissionRequestPending = false;
				}
				
			} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				
				UsbAccessory accessory = UsbManager.getAccessory(intent);
				
				if (accessory != null && accessory.equals(mAccessory)) {
					closeAccessory();
				}
			}
		}

	};
	
	
	public void openAccessory(UsbAccessory accessory) {
		
		mFileDescriptor = mUsbManager.openAccessory(accessory);
		
		if (mFileDescriptor != null) {
			mAccessory = accessory;
			FileDescriptor fd = mFileDescriptor.getFileDescriptor();
			
			mInputStream = new FileInputStream(fd);
			mOutputStream = new FileOutputStream(fd);
						
			notifyAccesoryStatus(true);
			
			Log.d(TAG,"Creating Timer");
			
			mTempTimer = new Timer();
			mTempTimer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					sendAction(ACTION_GET_TEMPERATURE);
				}
			}, 0, UPDATED_TIME);
						
			
		} else {
			Log.d(TAG, "accessory open fail");
			notifyAccesoryStatus(false);
		}
	}
	
	public void unRegister ()
	{
		// Receiver
		if (mUsbReceiver != null)
		{
			mContext.unregisterReceiver(mUsbReceiver);
			mUsbReceiver = null;
		}
	}
	
	public void closeAccessory() {
		
		if (mTempTimer != null)
			mTempTimer.cancel();
		notifyAccesoryStatus(false);
		
		// Descriptor
		try {
			if (mFileDescriptor != null) {
				mFileDescriptor.close();
			}
		} catch (IOException e) {
		} finally {
			mFileDescriptor = null;			
		}
	}
	
	public void onResumeAccessory ()
	{
		
		if (mInputStream != null && mOutputStream != null) {
			return;
		}
		
		mUsbManager = UsbManager.getInstance(mContext);

		UsbAccessory[] accessories = mUsbManager.getAccessoryList();
		UsbAccessory accessory = (accessories == null ? null : accessories[0]);
		
		if (accessory != null) {
			if (mUsbManager.hasPermission(accessory)) {
				openAccessory(accessory);
			} else {
				synchronized (mUsbReceiver) {
					if (!mPermissionRequestPending) {
						mUsbManager.requestPermission(accessory, mPermissionIntent);
						mPermissionRequestPending = true;
					}
				}
			}
		} else {
			Log.d(TAG, "mAccessory is null");
			notifyAccesoryStatus(false);
		}
	}
	
	public void setListener (IADKService listener)
	{
		if (listener != null)
			mIArray.add(listener);
	}
	
	

	private float composeFloat (Byte b0, Byte b1, Byte b2, Byte b3)
	{
		int asInt = (b0 & 0xFF) 
	              | ((b1 & 0xFF) << 8) 
	              | ((b2 & 0xFF) << 16) 
	              | ((b3 & 0xFF) << 24);
	              
		return  Float.intBitsToFloat(asInt);	              
	}
	
	public void setProgrammedTemp (float temp)
	{
		mController.setProgrammedTemp(temp);
		notifyHeatStatus (mController.calcHeatStatus());
	}
	

	private synchronized void sendAction( int action ) {	
		
		int ret = 0;
		byte[] buffer = new byte[16384];
		int i;

		/* Temperature: 3 bytes = 0x1 0x0 0x0  -> Query for temperature
		 *     -> Query:    0x1 0x0 0x0
		 *     -> Response: 0x1 B0 B1 B2 B3   (4 bytes to float)
		 *          
		 *  Query heat state: 3 bytes
		 *  	-> Send:    0x2 0x1 0x0
		 *      -> Recv:    0x2 0x1 B0   (B0 = 0x1 -> Head ON ; B0 = 0x2 -> Head OFF)
		 *     
		 *  Set heat state: 3 bytes
		 *      -> Send:	0x2 0x2 0x0
		 *      -> Recv:    0x2 0x2 B0   (B0 = 0x1 -> Head ON ; B0 = 0x2 -> Head OFF)
		 */				
		
		
		switch (action) {
		case ACTION_GET_TEMPERATURE:
			sendCommand((byte)1, (byte)0, 0);
			break;
			
		case ACTION_GET_HEAT_STATUS:
			sendCommand((byte)2, (byte)1, 0);
			break;
			
		case ACTION_SET_HEAT_STATUS:
			sendCommand((byte)2, (byte)2, 0);
			break;
		default:
			break;
		}

		


		try {
			ret = mInputStream.read(buffer);
		} catch (IOException e) {
			Log.d(TAG, "IOException");
			return;
		}

		i = 0;
		while (i < ret) {
			int len = ret - i;

			switch (buffer[i]) {
			case 0x1:

				final float roomTemp = composeFloat(buffer[i + 1],buffer[i + 2], buffer[i+3], buffer[i+4]);

				// Update the controller
				mController.setRoomTemp(roomTemp);
				notifyHeatStatus (mController.calcHeatStatus());
				
				// Send data to listeners
				for (IADKService  listener : mIArray) {
					listener.notifyTemperature(roomTemp);
				}
				
				/*
				Main.mContext.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						Main.mContext.tvEnvTemp.setText( String.format("%.2f", temp ));								
					}
				});		
				*/

				i += 5;
				break;
				
			default:
				Log.d(TAG, "unknown msg: " + buffer[i]);
				i = len;
				break;
			}
		}		
	}
	
	private void sendCommand(byte command, byte target, int value) 
	{

		byte[] buffer = new byte[3];
		if (value > 255)
			value = 255;

		buffer[0] = command;
		buffer[1] = target;
		buffer[2] = (byte) value;
		
		if (mOutputStream != null && buffer[1] != -1) {
			try {
				mOutputStream.write(buffer);
			} catch (IOException e) {
				Log.e(TAG, "write failed", e);
			}
		}
	}
	
	private void notifyAccesoryStatus (Boolean status)
	{		
		for (IADKService  listener : mIArray) {
			listener.notifyAccessoryStatus(status);
		}
	}
	
	private void notifyHeatStatus (Boolean status)
	{
		for (IADKService  listener : mIArray) {
			listener.notifyHeatStatus (status);
		}
	}

	
	
}
