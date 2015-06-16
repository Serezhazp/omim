package com.mapswithme.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mapswithme.maps.MWMApplication;
import com.mapswithme.util.statistics.AlohaHelper;

import ru.mail.mrgservice.MRGSBroadcastReceiver;

/**
 * Custom broadcast receiver to send intent to MRGS & Alohalytics at the same time
 */
public class MultipleTrackerReferrerReceiver extends BroadcastReceiver
{
  @Override
  public void onReceive(Context context, Intent intent)
  {
    MWMApplication.get().initStats();
    // parse & send referrer to Aloha
    try
    {
      if (intent.hasExtra("referrer"))
      {
        final String referrer = intent.getStringExtra("referrer");
        final String referrerSplitted[] = referrer.split("&");
        if (referrerSplitted.length != 0)
        {
          final String[] parsedValues = new String[referrerSplitted.length * 2];
          int i = 0;
          for (String referrerValue : referrerSplitted)
          {
            String keyValue[] = referrerValue.split("=");
            parsedValues[i++] = keyValue[0];
            parsedValues[i++] = keyValue.length == 2 ? keyValue[1] : "";
          }

          org.alohalytics.Statistics.logEvent(AlohaHelper.GPLAY_INSTALL_REFERRER, parsedValues);
        }
        else
          org.alohalytics.Statistics.logEvent(AlohaHelper.GPLAY_INSTALL_REFERRER, referrer);
      }
    } catch (Exception e)
    {
      e.printStackTrace();
    }
    intent.setComponent(null);
    // now send intent to MRGS tools
    MRGSBroadcastReceiver receiver = new MRGSBroadcastReceiver();
    receiver.onReceive(context, intent);
  }
}