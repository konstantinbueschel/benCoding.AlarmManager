/**
 * benCoding.AlarmManager Project
 * Copyright (c) 2009-2012 by Ben Bahrenburg. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package bencoding.alarmmanager;

import org.appcelerator.titanium.TiApplication;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;

public class AlarmServiceListener  extends BroadcastReceiver {

	private boolean isServiceRunning(Context context, String serviceName) {

		ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
			if (serviceName.equals(service.service.getClassName()))
				return true;
		return false;
	}
	@Override
    public void onReceive(Context context, Intent intent) {
		utils.debugLog("In Alarm Service Listener");
    	Bundle bundle = intent.getExtras();
        String fullServiceName = bundle.getString("alarm_service_name");
        boolean forceRestart = bundle.getBoolean("alarm_service_force_restart",false);
        boolean hasInterval = bundle.getBoolean("alarm_service_has_interval",false);
        boolean shouldWakeUp = bundle.getBoolean("should_wake_up", false);

        utils.debugLog("RequestCode: " + bundle.getInt("notification_request_code"));
        utils.debugLog("Full Service Name: " + fullServiceName);

        if (this.isServiceRunning(context,fullServiceName)) {
        	if(forceRestart){
        		utils.infoLog("Service is already running, we will stop it then restart");
              	Intent tempIntent = new Intent();
              	tempIntent.setClassName(TiApplication.getInstance().getApplicationContext(), fullServiceName);
              	context.stopService(tempIntent);
              	utils.infoLog("Service has been stopped");
        	}else{
        		utils.infoLog("Service is already running not need for us to start");
        		return;
        	}
        }

      	Intent serviceIntent = new Intent();

      	serviceIntent.setClassName(TiApplication.getInstance().getApplicationContext(), fullServiceName);
      	serviceIntent.putExtra("notification_request_code", bundle.getInt("notification_request_code"));

      	utils.debugLog("Is this an interval service? " + new Boolean(hasInterval).toString());

      	if(hasInterval){

      		utils.debugLog("Is this an interval amount " + bundle.getLong("alarm_service_interval", 45*60*1000L));
      		serviceIntent.putExtra("interval", bundle.getLong("alarm_service_interval", 45*60*1000L)); // Default to 45mins
      	}

        context.startService(serviceIntent);
        
        if (shouldWakeUp) {
        	
        	PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        	
    		PowerManager.WakeLock wl = powerManager.newWakeLock(
    				PowerManager.SCREEN_DIM_WAKE_LOCK | // could use bright instead
    				PowerManager.ACQUIRE_CAUSES_WAKEUP | // so we actually
    																// wake the
    																// device
    				PowerManager.ON_AFTER_RELEASE // and we keep it on for a
    														// bit after release
    				, "My Tag");
    		
    		wl.acquire();
    		wl.release();
        }
        
        utils.infoLog("Alarm Service Started");
    }
}
