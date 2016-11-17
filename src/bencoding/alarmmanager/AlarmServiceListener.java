/**
 * benCoding.AlarmManager Project
 * Copyright (c) 2009-2012 by Ben Bahrenburg. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package bencoding.alarmmanager;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.text.SimpleDateFormat;

import org.appcelerator.titanium.TiApplication;

import android.app.AlarmManager;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.PendingIntent;

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

    utils.debugLog("Received alarm service listener event");
    
    Bundle bundle = intent.getExtras();
    
    String fullServiceName = bundle.getString("alarm_service_name");
    
    boolean forceRestart = bundle.getBoolean("alarm_service_force_restart",false);
    boolean hasInterval = bundle.getBoolean("alarm_service_has_interval",false);
    boolean shouldWakeUp = bundle.getBoolean("alarm_service_should_wake_up", false);
    boolean deliverExact = bundle.getBoolean("alarm_service_deliver_exact", false);

    utils.debugLog("Request code: " + bundle.getInt("alarm_service_request_code"));
    utils.debugLog("Should wake up: " + shouldWakeUp);
    utils.debugLog("Deliver exact: " + deliverExact);
    utils.debugLog("Full Service Name: " + fullServiceName);

    if (this.isServiceRunning(context,fullServiceName)) {
          
      if (forceRestart) {
        
        utils.infoLog("Service is already running, we will stop it then restart");

        Intent tempIntent = new Intent();

        tempIntent.setClassName(TiApplication.getInstance().getApplicationContext(), fullServiceName);
        
        context.stopService(tempIntent);

        utils.infoLog("Service has been stopped");
      }
      else {

        utils.infoLog("Service is already running not need for us to start");

        return;
      }
    }

    Intent serviceIntent = new Intent();

    serviceIntent.setClassName(TiApplication.getInstance().getApplicationContext(), fullServiceName);    

    utils.debugLog("Is this an interval service? " + new Boolean(hasInterval).toString());

    if (hasInterval) {

      utils.debugLog("Is this an interval amount " + bundle.getLong("alarm_service_interval", 45*60*1000L));

      serviceIntent.putExtra("interval", bundle.getLong("alarm_service_interval", 45*60*1000L)); // Default to 45mins
    }
    
    serviceIntent.putExtra("alarm_service_request_code", bundle.getInt("alarm_service_request_code"));
    serviceIntent.putExtra("customData",bundle.getString("customData","[]"));
    
    context.startService(serviceIntent);
        
    if (shouldWakeUp) {
          
      PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
          
      // could use bright instead so we actually wake the device and we keep it on for a bit after release
      PowerManager.WakeLock wl = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "bencoding.AlarmManager");
        
      wl.acquire();
      wl.release();
    }
        
    utils.infoLog("Alarm service should now be running");

    if (bundle.getLong("alarm_service_repeat_ms", 0) > 0) {
        
      createRepeatService(bundle);
    }
  }

  private void createRepeatService(Bundle bundle) {

    Intent intent = new Intent(TiApplication.getInstance().getApplicationContext(), AlarmServiceListener.class);
      
    // use the same extras as the original service
    intent.putExtras(bundle);

    // update date and time by repeat interval (in milliseconds)
    int day = bundle.getInt("alarm_service_day");
    int month = bundle.getInt("alarm_service_month");
    int year = bundle.getInt("alarm_service_year");
    int hour = bundle.getInt("alarm_service_hour");
    int minute = bundle.getInt("alarm_service_minute");
    int second = bundle.getInt("alarm_service_second");
    
    boolean deliverExact = bundle.getBoolean("alarm_service_deliver_exact", false);

    Calendar cal = new GregorianCalendar(year, month, day);
    
    cal.add(Calendar.HOUR_OF_DAY, hour);
    cal.add(Calendar.MINUTE, minute);
    cal.add(Calendar.SECOND, second);

    Calendar now = Calendar.getInstance();
        
    long repeatInMs = bundle.getLong("alarm_service_repeat_ms", 0);
    int repeatInS = (int)repeatInMs / 1000;

    // add frequence until cal > now
    while (now.getTimeInMillis() > cal.getTimeInMillis()) {
      
      cal.add(Calendar.SECOND, repeatInS);
    }

    int requestCode = bundle.getInt("alarm_service_request_code", AlarmmanagerModule.DEFAULT_REQUEST_CODE);

    long triggerAtMillis = cal.getTimeInMillis();

    Date date = new Date(triggerAtMillis);
    String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
    
    utils.infoLog("Creating alarm service repeat for: "  + sdf.format(date));

    // create the Alarm Manager
    AlarmManager alarmManager = (AlarmManager) TiApplication.getInstance().getApplicationContext().getSystemService(TiApplication.ALARM_SERVICE);

    PendingIntent sender = PendingIntent.getBroadcast(TiApplication.getInstance().getApplicationContext(), requestCode, intent,  PendingIntent.FLAG_UPDATE_CURRENT);

    if (android.os.Build.VERSION.SDK_INT >= 19 && deliverExact) {

        utils.debugLog("Setting EXACT alarm service repeat");

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, sender);
    }
    else {

        utils.debugLog("Setting alarm service repeat");

        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, sender);    
    }
  }
}
