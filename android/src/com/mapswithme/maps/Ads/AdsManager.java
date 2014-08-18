package com.mapswithme.maps.Ads;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.mapswithme.maps.MWMApplication;
import com.mapswithme.util.ConnectionState;
import com.mapswithme.util.Constants;
import com.mapswithme.util.UiUtils;
import com.mapswithme.util.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdsManager
{
  private static final String ROOT_MENU_ITEMS_KEY = "AppFeatureBottomMenuItems";
  private static final String MENU_ITEMS_KEY = "Items";
  private static final String DEFAULT_KEY = "*";
  private static final String ID_KEY = "Id";
  private static final String APP_URL_KEY = "AppURL";
  private static final String ICON_KEY = "IconURLs";
  private static final String TITLE_KEY = "Titles";
  private static final String COLOR_KEY = "Color";
  private static final String WEB_URL_KEY = "WebURLs";
  private static final String CACHE_FILE = "menu_ads.json";

  private static List<MenuAd> sMenuAds;

  public static List<MenuAd> getMenuAds()
  {
    return sMenuAds;
  }

  public static void updateMenuAds()
  {
    String menuAdsString;
    if (ConnectionState.isConnected(MWMApplication.get()))
    {
      menuAdsString = getJsonAdsFromServer();
      cacheMenuAds(menuAdsString);
    }
    else
      menuAdsString = getCachedJsonString();

    if (menuAdsString == null)
      return;

    final JSONObject menuAdsJson;
    try
    {
      menuAdsJson = new JSONObject(menuAdsString);
      sMenuAds = parseMenuAds(menuAdsJson);
    } catch (JSONException e)
    {
      e.printStackTrace();
    }
  }

  private static void cacheMenuAds(String menuAdsString)
  {
    if (menuAdsString == null)
      return;

    final File cacheFile = new File(MWMApplication.get().getDataStoragePath(), CACHE_FILE);
    try (FileOutputStream fileOutputStream = new FileOutputStream(cacheFile))
    {
      fileOutputStream.write(menuAdsString.getBytes());
      fileOutputStream.close();
    } catch (IOException e)
    {
      e.printStackTrace();
    }
  }

  private static String getCachedJsonString()
  {
    String menuAdsString = null;
    final File cacheFile = new File(MWMApplication.get().getDataStoragePath(), CACHE_FILE);
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(cacheFile))))
    {
      final StringBuilder stringBuilder = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null)
        stringBuilder.append(line);

      menuAdsString = stringBuilder.toString();
    } catch (IOException e)
    {
      e.printStackTrace();
    }

    return menuAdsString;
  }

  private static List<MenuAd> parseMenuAds(JSONObject adsJson) throws JSONException
  {
    final List<MenuAd> ads = new ArrayList<>();

    final String countryCode = Locale.getDefault().getCountry();
    final JSONArray menuItemsJson = getJsonObjectByKeyOrDefault(adsJson.getJSONObject(ROOT_MENU_ITEMS_KEY), countryCode).
        getJSONArray(MENU_ITEMS_KEY);

    final String localeKey = Locale.getDefault().getLanguage();
    final String density = UiUtils.getDisplayDensityString();
    for (int i = 0; i < menuItemsJson.length(); i++)
    {
      final JSONObject menuItemJson = menuItemsJson.getJSONObject(i);
      final String icon = getStringByKeyOrDefault(menuItemJson.getJSONObject(ICON_KEY), density);
      final String webUrl = getStringByKeyOrDefault(menuItemJson.getJSONObject(WEB_URL_KEY), localeKey);
      final String title = getStringByKeyOrDefault(menuItemJson.getJSONObject(TITLE_KEY), localeKey);
      final String id = menuItemJson.getString(ID_KEY);
      final Bitmap bitmap = loadAdIcon(icon, id);

      ads.add(new MenuAd(icon,
          title,
          menuItemJson.getString(COLOR_KEY),
          id,
          menuItemJson.getString(APP_URL_KEY),
          webUrl,
          bitmap));
    }

    return ads;
  }

  /**
   * Loads and caches ad icon. If internet isnt connected tries to reuse cached icon.
   *
   * @param urlString url of the icon
   * @param adId      name of cachefile
   * @return
   */
  private static Bitmap loadAdIcon(String urlString, String adId)
  {
    if (!ConnectionState.isConnected(MWMApplication.get()))
      return loadCachedBitmap(adId);

    try
    {
      final URL url = new java.net.URL(urlString);
      final HttpURLConnection connection = (HttpURLConnection) url
          .openConnection();
      final int timeout = 5000;
      connection.setReadTimeout(timeout);
      connection.setConnectTimeout(timeout);
      connection.setDoInput(true);
      connection.connect();
      InputStream input = connection.getInputStream();
      final Bitmap bitmap = BitmapFactory.decodeStream(input);
      cacheBitmap(bitmap, adId);
      return bitmap;
    } catch (IOException e)
    {
      e.printStackTrace();
    }

    return null;
  }

  private static void cacheBitmap(Bitmap bitmap, String fileName)
  {
    final File cacheFile = new File(MWMApplication.get().getDataStoragePath(), fileName);
    try (FileOutputStream fileOutputStream = new FileOutputStream(cacheFile))
    {
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
      fileOutputStream.close();
    } catch (IOException e)
    {
      e.printStackTrace();
    }
  }

  private static Bitmap loadCachedBitmap(String fileName)
  {
    final File cacheFile = new File(MWMApplication.get().getDataStoragePath(), fileName);
    if (!cacheFile.exists())
      return null;

    try (InputStream inputStream = new FileInputStream(cacheFile))
    {
      return BitmapFactory.decodeStream(inputStream);
    } catch (IOException e)
    {
      e.printStackTrace();
    }

    return null;
  }

  private static String getStringByKeyOrDefault(JSONObject json, String key) throws JSONException
  {
    String res = json.optString(key);
    if (res.isEmpty())
      res = json.optString(DEFAULT_KEY);

    return res;
  }

  private static JSONObject getJsonObjectByKeyOrDefault(JSONObject json, String key) throws JSONException
  {
    JSONObject res = json.optJSONObject(key);
    if (res == null)
      res = json.getJSONObject(DEFAULT_KEY);

    return res;
  }

  private static String getJsonAdsFromServer()
  {
    BufferedReader reader = null;
    HttpURLConnection connection = null;
    try
    {
      final URL url = new URL(Constants.Url.MENU_ADS_JSON);
      connection = (HttpURLConnection) url.openConnection();
      final int timeout = 10000;
      connection.setReadTimeout(timeout);
      connection.setConnectTimeout(timeout);
      connection.setRequestMethod("GET");
      connection.setDoInput(true);
      // Starts the query
      connection.connect();
      final int response = connection.getResponseCode();
      if (response == HttpURLConnection.HTTP_OK)
      {
        reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        final StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null)
          builder.append(line).append("\n");

        return builder.toString();
      }
    } catch (java.io.IOException e)
    {
      e.printStackTrace();
    } finally
    {
      Utils.closeStream(reader);
      if (connection != null)
        connection.disconnect();
    }

    return null;
  }
}