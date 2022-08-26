package com.reactnativesbppay

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.UiThreadUtil.runOnUiThread
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Base64


class SbpPayModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  override fun getName(): String {
    return "SbpPay"
  }

  // Example method
  // See https://reactnative.dev/docs/native-modules-android
  @ReactMethod
  fun multiply(a: Int, b: Int, promise: Promise) {
    promise.resolve(a * b)
  }

  @ReactMethod
  fun checkUrl(data: ReadableMap, promise: Promise) {
    val givenName: String? = if (data.hasKey("test")) data.getString("test") else null

    val urlconn = "https://qr.nspk.ru/.well-known/assetlinks.json"
    @SuppressLint("QueryPermissionsNeeded")
    fun drawableToBitmap(drawable: Drawable): String? {
      var bitmap: Bitmap? = null
      if (drawable is BitmapDrawable) {
        val bitmapDrawable = drawable
        if (bitmapDrawable.bitmap != null) {
          val byteArrayOutputStream = ByteArrayOutputStream()
          bitmapDrawable.bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
          val imageBytes: ByteArray = byteArrayOutputStream.toByteArray()
          return Base64.encodeToString(imageBytes, Base64.DEFAULT)
        }
      }
      bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
        Bitmap.createBitmap(
          1,
          1,
          Bitmap.Config.ARGB_8888
        ) // Single color bitmap will be created of 1x1 pixel
      } else {
        Bitmap.createBitmap(
          drawable.intrinsicWidth,
          drawable.intrinsicHeight,
          Bitmap.Config.ARGB_8888
        )
      }
      val byteArrayOutputStream = ByteArrayOutputStream()
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
      val imageBytes: ByteArray = byteArrayOutputStream.toByteArray()
      return Base64.encodeToString(imageBytes, Base64.DEFAULT)

    }
    fun getBankApps(link: String, banks: Set<Any?>): JsonObject {
      // get sbp packages
      val sbpIntent = Intent(Intent.ACTION_VIEW)
      val PackageManager: PackageManager = getReactApplicationContext().getPackageManager()
      sbpIntent.setDataAndNormalize(Uri.parse(link))
      val sbpPackages = PackageManager.queryIntentActivities(sbpIntent, 0)
        .map { it.activityInfo.packageName }
      val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://"))
      val browserPackages = PackageManager.queryIntentActivities(browserIntent, 0)
        .map { it.activityInfo.packageName }
      // filter out browsers
      val nonBrowserSbpPackages = sbpPackages.filter { it !in browserPackages }
      // get bank packages
      val bankPackages = PackageManager.getInstalledApplications(0)
        .map {it.packageName}.filter { it in banks }
      val Out = JsonObject()
      var i = 0;
      val mutable = mutableListOf<String>().apply {
        addAll(nonBrowserSbpPackages)
        addAll(bankPackages)
      }.distinct()
      mutable.map {
        i++;
        val obj = JsonObject()
        val title = PackageManager.getApplicationLabel(PackageManager.getApplicationInfo( it, 0))
        obj.addProperty("name", title.toString())
        obj.addProperty("packageName", it);
        obj.addProperty("icon", drawableToBitmap(PackageManager.getApplicationIcon(it)).toString());
        Out.add(i.toString(), obj);
      }
      return Out
    }

     fun connectToUrl(url: String): String {
      val targetUrl = URL(urlconn)
      val gson: Gson = GsonBuilder().create()
      val connection = targetUrl.openConnection() as HttpURLConnection
      connection.requestMethod = "GET"
      connection.connect()
      var responseReader: InputStreamReader? = null
      fun read(reader: InputStreamReader): String {
        val buffer = CharArray(4096)
        var read: Int = -1
        val result = StringBuilder()

        while ({ read = reader.read(buffer, 0, 4096); read }() != -1) {
          result.append(buffer, 0, read)
        }

        return result.toString()
      }
      val responseCode = connection.responseCode
      if (responseCode == HttpURLConnection.HTTP_OK) {
        responseReader = InputStreamReader(connection.inputStream)
        val response = read(responseReader)
        val banks: Set<Any?> = (gson.fromJson(response, List::class.java) as List).map {
          ((it as Map<*, *>)["target"] as Map<*, *>)["package_name"]
        }.toSet()
        val a = getBankApps(url,banks)
        return if(a.size() > 0) {
          a.toString();
        } else {
          "";
        }
      } else {
        return "";
      }
    }



    val url = "https://qr.nspk.ru/";
    val banksToString = connectToUrl(url)

    promise.resolve(banksToString)
  }
  @ReactMethod
  fun openSbpDeepLinkInBank(data: ReadableMap, promise: Promise) {
      val packageName: String? = if (data.hasKey("packageName")) data.getString("packageName") else null
      val url: String? = if (data.hasKey("url")) data.getString("url") else null
    val activity: Activity = getCurrentActivity() as Activity
    val PackageManager: PackageManager = getReactApplicationContext().getPackageManager()
    val intent = Intent(Intent.ACTION_VIEW)
    intent.setPackage(packageName)
    intent.data = Uri.parse(url)
    if (intent.resolveActivity(PackageManager) != null) {
      activity.startActivityForResult(intent, 113)
    }
    promise.resolve("ok")
  }

}
