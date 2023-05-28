package com.example.webviewbridge;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.webkit.*;
import android.widget.Toast;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static android.content.Context.MODE_PRIVATE;

public class WebViewBridge {
    final WebView webView;
    final HashMap<String, CallableFunction> callableFunctions;
    private String currentUrl = "", pageNotFoundUrl = "", lastCallbackValue = "";
    final Context context;

    // Enum "NodePosition" is used to determine the position where a node should be added in HTML
    @SuppressWarnings("unused")
    enum NodePosition{
        beforeStart, afterStart, beforeEnd, afterEnd
    }

    // Gets an WebView and the App-Context and creates the WebViewBridge object
    @SuppressLint("SetJavaScriptEnabled")
    WebViewBridge(WebView wv, Context con){
        this.webView = wv;
        this.context = con;
        this.callableFunctions = new HashMap<>();
        this.webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (url.contains("#")) {
                    currentUrl = url.split("#")[1];
                    splitUrlToCallMethod();
                }
            }

            @Override
            public void onReceivedError (WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                //Loading the custom error page
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (error.getErrorCode() == -2) if (!pageNotFoundUrl.equals("")) view.loadUrl(pageNotFoundUrl);
                }
            }
        });

        setJavaScriptEnabled(true);

        // Adds functions to the callable functions, so they can be accessed from JavaScript
        this.addCallableFunction(this, "loadUrl", "loadUrl", new Object[] {"String"});
        this.addCallableFunction(this, "loadData", "loadData", new Object[] {"String"});
        this.addCallableFunction(this, "setPageNotFoundUrl", "setPageNotFoundUrl", new Object[] {"String"});

        // Adds an JavaScript-Interface to the WebView (access functions from JavaScript)
        webView.addJavascriptInterface(new MyJavaScriptInterface(con), "Android");
    }

    @SuppressWarnings("unused")
    public String getLastCallbackValue(){
        return this.lastCallbackValue;
    }


    // Options for the WebView ---------------------------------------------------------------------

    public void loadUrl(String url){
        this.webView.loadUrl(url);
    }

    @SuppressWarnings("unused")
    public void loadData(String data){
        this.webView.loadData(data, "text/html", "UTF-8");
    }

    @SuppressWarnings("unused")
    public void setDomStorageEnabled(Boolean value){
        this.webView.getSettings().setDomStorageEnabled(value);
    }

    public void setJavaScriptEnabled(Boolean value){
        this.webView.getSettings().setJavaScriptEnabled(value);
    }

    public void setAppCacheEnabled(Boolean value){
        this.webView.getSettings().setCacheMode(((value) ? WebSettings.LOAD_CACHE_ELSE_NETWORK : WebSettings.LOAD_NO_CACHE));
    }

    @SuppressWarnings("unused")
    public void setAllowFileAccess(Boolean value){
        this.webView.getSettings().setAllowFileAccess(value);
    }

    @SuppressLint("ObsoleteSdkInt")
    @SuppressWarnings("unused")
    public void allowAccessFromFileURLs(Boolean value){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            this.webView.getSettings().setAllowUniversalAccessFromFileURLs(value);
            this.webView.getSettings().setAllowFileAccessFromFileURLs(value);
        }
    }

    @SuppressWarnings("unused")
    public void loadCacheElseNetwork(){
        setAppCacheEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
    }

    public boolean goBack(){
        if (this.webView.canGoBack() && this.currentUrl.contains("#")) {
            this.webView.goBack();
        }

        if (this.webView.canGoBack()){
            this.webView.goBack();
            return true;
        } else return false;
    }


    // Functions that have access to the storage ---------------------------------------------------

    public void writeFile(Context context, String fileName, String value){
        try {
            FileOutputStream fileOutputStream = context.openFileOutput(fileName, MODE_PRIVATE);
            fileOutputStream.write(value.getBytes());
            fileOutputStream.close();
        } catch (Exception e) {
            System.out.println("An error occurred while writing the file: " + e);
        }
    }

    public String readFile(Context context, String fileName) {
        StringBuilder text = new StringBuilder();
        String line;

        try {
            FileInputStream fileInputStream = context.openFileInput(fileName);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);

            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            while ((line = bufferedReader.readLine()) != null) {
                text.append("\n").append(line);
            }
        } catch (Exception e) {
            System.err.println("An error occurred while reading the file: " + e);
        }
        return text.toString();
    }


    // Functions getting elements from WebView or execute Code in WebView --------------------------

    public void executeJavaScript(final String cmd){
        this.webView.setWebViewClient(new WebViewClient() {
            @SuppressLint("ObsoleteSdkInt")
            @Override
            public void onPageFinished(WebView view, String url) {
                if (Build.VERSION.SDK_INT >= 19) view.evaluateJavascript(cmd, s -> lastCallbackValue = s);
                else loadUrl("javascript:" + cmd + "; void(0);");

                if (url.contains("#")) {
                    currentUrl = url.split("#")[1];
                    splitUrlToCallMethod();
                }
            }
            public void onReceivedError (WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (error.getErrorCode() == -2) if (!pageNotFoundUrl.equals("")) view.loadUrl(pageNotFoundUrl);
                }
            }
        });
    }

    @SuppressWarnings("unused")
    public void setPageNotFoundUrl(String newPageNotFoundUrl){
        this.pageNotFoundUrl = newPageNotFoundUrl;
    }

    private String getHtmlElementById(String id){
        return ("document.getElementById('" + id + "')");
    }

    private String getHtmlElementsByClass(String classname){
        return ("document.getElementsByClassName('"+ classname + "')");
    }

    private String getHtmlElementsByTagName(String tagName){
        return ("document.getElementsByTagName('"+ tagName + "')");
    }

    private void doSomethingByClassName(String classname, String function){
        executeJavaScript("var availableClasses = " + getHtmlElementsByClass(classname) + "; [].forEach.call(availableClasses, function (availableClass) {availableClass." +  function + "})");
    }

    private void doSomethingByTagName(String tagName, String function){
        executeJavaScript("var availableTags = " + getHtmlElementsByTagName(tagName) + "; [].forEach.call(availableTags, function (availableTag) {availableTag." +  function + "})");
    }

    private String makeStyleAttributeValid(String attribute){
        int i = attribute.indexOf("-");

        if (i != -1)
            attribute = attribute.substring(0, i) + attribute.substring(i+1, i+2).toUpperCase() + attribute.substring(i+2);

        return attribute;
    }

    @SuppressWarnings("unused")
    public void setCssById(String id, String attribute, String value){
        executeJavaScript(getHtmlElementById(id) + ".style." + makeStyleAttributeValid(attribute) + " = '" + value + "';");
    }

    @SuppressWarnings("unused")
    public void setCssByClass(String classname, String attribute, String value){
        doSomethingByClassName(classname, "style." + makeStyleAttributeValid(attribute) + " = '" + value + "';");
    }

    @SuppressWarnings("unused")
    public void setCssByTagName(String tagName, String attribute, String value){
        doSomethingByTagName(tagName, "style." + makeStyleAttributeValid(attribute) + " = '" + value + "';");
    }

    public void setHtmlAttributeById(String id, String attributeName, String value){
        executeJavaScript(getHtmlElementById(id) + "." + attributeName + " = '" + value + "';");
    }

    public void setHtmlAttributeByClass(String classname, String attributeName,  String value){
        doSomethingByClassName(classname, attributeName + " = '" + value + "';");
    }

    public void setHtmlAttributeByTagName(String tagName, String attributeName,  String value){
        doSomethingByTagName(tagName, attributeName + " = '" + value + "';");
    }

    public void setInnerHtmlById(String id, String value){
        setHtmlAttributeById(id, "innerHTML", value);
    }

    public void setInnerHtmlByClass(String classname, String value){
        setHtmlAttributeByClass(classname, "innerHTML", value);
    }

    @SuppressWarnings("unused")
    public void setInnerHtmlByTagName(String tagName, String value){
        setHtmlAttributeByTagName(tagName, "innerHTML", value);    }

    @SuppressWarnings("unused")
    public void setImageSourceById(String id, String url){
        setHtmlAttributeById(id, "src", url);
    }

    @SuppressWarnings("unused")
    public void setImageSourceByClass(String classname, String url){
        setHtmlAttributeByClass(classname, "src", url);
    }

    @SuppressWarnings("unused")
    public void appendNodeById(String motherNodeId, HtmlNode htmlNode, NodePosition nodePosition){
        executeJavaScript(getHtmlElementById(motherNodeId) + ".insertAdjacentHTML('" + nodePosition + "', '" + htmlNode.get() + "');");
    }

    @SuppressWarnings("unused")
    public void appendNodeByClass(String motherNodeClassName, HtmlNode htmlNode, NodePosition nodePosition){
        doSomethingByClassName(motherNodeClassName, "insertAdjacentHTML('" + nodePosition + "', '" + htmlNode.get() + "');");
    }

    @SuppressWarnings("unused")
    public void appendNodeByTagName(String motherNodeTagName, HtmlNode htmlNode, NodePosition nodePosition){
        doSomethingByTagName(motherNodeTagName, "insertAdjacentHTML('" + nodePosition + "', '" + htmlNode.get() + "');");
    }

    @SuppressWarnings("unused")
    public void replaceNodeById(String nodeToReplaceId, HtmlNode htmlNode){
        setInnerHtmlById(nodeToReplaceId, htmlNode.get());
    }

    @SuppressWarnings("unused")
    public void replaceNodeByClass(String nodeToReplaceClass, HtmlNode htmlNode){
        setInnerHtmlByClass(nodeToReplaceClass, htmlNode.get());
    }

    @SuppressWarnings("unused")
    public void replaceNodeByTagName(String nodeToReplaceTagName, HtmlNode htmlNode){
        setInnerHtmlByClass(nodeToReplaceTagName, htmlNode.get());
    }

    @SuppressWarnings("unused")
    public void removeNodeById(String id){
        executeJavaScript(getHtmlElementById(id) + ".remove();");
    }

    @SuppressWarnings("unused")
    public void removeNodeByClass(String classname){
        executeJavaScript( "var availableClasses = " + getHtmlElementsByClass(classname) + "; while(availableClasses.length > 0){availableClasses[0].remove();}");
    }

    @SuppressWarnings("unused")
    public void removeNodeByTagName(String tagName){
        executeJavaScript( "var availableTags = " + getHtmlElementsByTagName(tagName) + "; while(availableTags.length > 0){availableTags[0].remove();}");
    }


    // Methods that give the WebView the opportunity to call native functions ----------------------

    @SuppressWarnings("unused")
    public void addCallableFunction(Object classObject, String methodName, String keyword){
        this.callableFunctions.put(keyword, new CallableFunction(classObject, methodName, keyword));
    }

    public void addCallableFunction(Object classObject, String methodName, String keyword, Object[] arguments){
        this.callableFunctions.put(keyword, new CallableFunction(classObject, methodName, keyword, arguments));
    }

    @SuppressWarnings("unused")
    public void removeCallableFunctions(String key){
        this.callableFunctions.remove(key);
    }

    private void proofCallableFunctions(String proofingKeyword, Object[] arguments){
        if (this.callableFunctions.containsKey(proofingKeyword) && arguments != null){
            Objects.requireNonNull(this.callableFunctions.get(proofingKeyword)).setArguments(arguments);
            Objects.requireNonNull(this.callableFunctions.get(proofingKeyword)).invokeMethod();
        }
    }

    private void splitUrlToCallMethod(){
        String callableFunctionIdentifier = this.currentUrl.split("=")[0];
        Object[] callableFunctionParameters = this.currentUrl.split("=")[1].split("&");

        proofCallableFunctions(callableFunctionIdentifier, callableFunctionParameters);
    }

    private Object[] proofTypes(Object[] callableFunctionsParameters){
        Object[] tempParameters = new Object[callableFunctionsParameters.length];
        for (int i = 0; i < callableFunctionsParameters.length; i++ ){
            Object object = callableFunctionsParameters[i];
            if (object.equals("true") || object.equals("false")){
                tempParameters[i] = (Boolean.parseBoolean((String) object));
            }else{
                try{
                    tempParameters[i] = Integer.parseInt((String)object);
                }catch(Exception e){
                    try{
                        tempParameters[i] = Float.parseFloat((String)object);
                    }catch(Exception e2){
                        tempParameters[i] = object;
                    }
                }
            }
        }
        return tempParameters;
    }


    // The JavaScript-Interface provides native functions to the JavaScript-Code -------------------

    class MyJavaScriptInterface {
        final Context context;

        MyJavaScriptInterface(Context context){
            this.context = context;
        }

        @SuppressWarnings("unused")
        @JavascriptInterface
        public void showToast(String message) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }

        @SuppressWarnings("unused")
        @JavascriptInterface
        public void systemOutPrintln(String message) {
            System.out.println(message);
        }

        @SuppressWarnings("unused")
        @JavascriptInterface
        public void systemErrPrintln(String message) {
            System.err.println(message);
        }

        @SuppressWarnings("unused")
        @JavascriptInterface
        public void systemOutPrint(String message) {
            System.out.print(message);
        }

        @SuppressWarnings("unused")
        @JavascriptInterface
        public void systemErrPrint(String message) {
            System.err.print(message);
        }

        @SuppressWarnings("unused")
        @JavascriptInterface
        public void showWarning(String warningTitle, String warningText){
            new AlertDialog.Builder(context)
                    .setTitle(warningTitle)
                    .setMessage(warningText)
                    .setPositiveButton(android.R.string.yes, null)
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .show();
        }

        @SuppressWarnings("unused")
        @JavascriptInterface
        public void showError(String errorTitle, String errorText){
            new AlertDialog.Builder(context)
                    .setTitle(errorTitle)
                    .setMessage(errorText)
                    .setPositiveButton(android.R.string.yes, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }

        @SuppressWarnings("unused")
        @JavascriptInterface
        public String nightModeEnabled(){
            int nightModeFlags = this.context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

            switch (nightModeFlags) {
                case Configuration.UI_MODE_NIGHT_YES:
                    return "UI_MODE_NIGHT_YES";
                case Configuration.UI_MODE_NIGHT_NO:
                    return "UI_MODE_NIGHT_NO";
                case Configuration.UI_MODE_NIGHT_UNDEFINED:
                    return "UI_MODE_NIGHT_UNDEFINED";
            }
            return "null";
        }

        @SuppressLint("SourceLockedOrientationActivity")
        @SuppressWarnings("unused")
        @JavascriptInterface
        public void displayRotationMode(int value){
            if (value == 0) ((Activity) this.context).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            else if (value == 1) ((Activity) this.context).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            else ((Activity) this.context).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        }

        @SuppressWarnings("unused")
        @JavascriptInterface
        public String getConnectivityStatus() {
            ConnectivityManager cm = (ConnectivityManager) this.context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);

            if (cm != null) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if (null != activeNetwork) {
                    if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI)
                        return "TYPE_WIFI";

                    if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE)
                        return "TYPE_MOBILE";
                }
                return "TYPE_NOT_CONNECTED";
            }
            return "Type_Error";
        }

        @JavascriptInterface
        public boolean checkPermission(String permission){
            return ContextCompat.checkSelfPermission(this.context, permission) == PackageManager.PERMISSION_GRANTED;
        }

        @JavascriptInterface
        public boolean requestPermission(String permission){
            if (!checkPermission(permission))
                ActivityCompat.requestPermissions((Activity) this.context, new String[]{permission}, 100);
            return checkPermission(permission);
        }

        @SuppressWarnings("unused")
        @JavascriptInterface
        public String getCurrentLocation(){
            Toast.makeText(this.context, "The application wants to get your location.", Toast.LENGTH_SHORT).show();
            requestPermission(Manifest.permission.ACCESS_FINE_LOCATION);

            if (requestPermission(Manifest.permission.ACCESS_COARSE_LOCATION) && checkPermission(Manifest.permission.ACCESS_FINE_LOCATION )) {
                try {
                    LocationManager lm = (LocationManager) this.context.getSystemService(Context.LOCATION_SERVICE);
                    Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new MyLocationListener(this.context));
                    System.out.println(location.getLatitude() + "," + location.getLongitude());
                    return (location.getLatitude() + "," + location.getLongitude());
                }catch(Exception e){
                    return "null";
                }
            }
            return "null";
        }

        @SuppressWarnings("unused")
        @JavascriptInterface
        public String takePhoto(){

            if( this.context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)){
                String[] requestedPermissions;

                if(Build.VERSION.SDK_INT < 28)
                    requestedPermissions = new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE};
                else if(Build.VERSION.SDK_INT <= 32)
                    requestedPermissions = new String[] {Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE};
                else requestedPermissions = new String[] {Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES};

                ActivityCompat.requestPermissions((Activity) this.context, requestedPermissions, 1001);

                try {
                    StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                    StrictMode.setVmPolicy(builder.build());
                    String photoPathAndFileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/Camera/" + "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".jpg";
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(photoPathAndFileName)));

                    ((Activity) this.context).startActivityForResult(intent, 100);
                    return photoPathAndFileName;
                } catch (Exception e) {
                    System.err.println("Camera could not be started");
                    return null;
                }
            }

            return "null";
        }

        @SuppressWarnings("unused")
        @JavascriptInterface
        public void vibrate(int milliseconds){
            Vibrator v = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(milliseconds);
            }
        }

        @SuppressLint("UnspecifiedImmutableFlag")
        @SuppressWarnings("unused")
        @JavascriptInterface
        public void pushNotification(String notificationTitle, String notificationContent){
            if (Build.VERSION.SDK_INT >= 33)
                requestPermission(Manifest.permission.POST_NOTIFICATIONS);

            if(Build.VERSION.SDK_INT < 33 || checkPermission(Manifest.permission.POST_NOTIFICATIONS)) {
                NotificationManager notificationManager = (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel notificationChannel = new NotificationChannel("NativeWebApp_ID", "NativeWebApp", NotificationManager.IMPORTANCE_DEFAULT);
                    notificationChannel.setDescription("NativeWebApp-Notification");
                    notificationManager.createNotificationChannel(notificationChannel);
                }
                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this.context.getApplicationContext(), "NativeWebApp_ID")
                        .setSmallIcon(R.mipmap.ic_launcher) // Icon of the notification
                        .setContentTitle(notificationTitle) // Title of the notification
                        .setContentText(notificationContent)// Content of the notification
                        .setAutoCancel(true); // Notification will be removed after clicking on it
                Intent intent = new Intent(this.context.getApplicationContext(), ((Activity) this.context).getClass());

                PendingIntent pi;
                if (Build.VERSION.SDK_INT >= 31)
                    pi = PendingIntent.getActivity(this.context, 0, intent, PendingIntent.FLAG_MUTABLE);
                else
                    pi = PendingIntent.getActivity(this.context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                notificationBuilder.setContentIntent(pi);
                notificationManager.notify(0, notificationBuilder.build());
            }else{
                showToast("Could not show Notification");
            }
        }

        @SuppressWarnings("unused")
        @JavascriptInterface
        public void flashlight(boolean value) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                CameraManager cameraManager = (CameraManager) this.context.getSystemService(Context.CAMERA_SERVICE);

                try {
                    String cameraId = cameraManager.getCameraIdList()[0];
                    cameraManager.setTorchMode(cameraId, value);
                } catch (CameraAccessException e) {
                    System.err.println("Error: " + e);
                }
            }else{
                Toast.makeText(this.context, "Feature not supported in this version of Android.", Toast.LENGTH_SHORT).show();
            }
        }

        @SuppressWarnings("unused")
        @JavascriptInterface
        public void writeTextToInternalStorage(String fileName, String content){
            writeFile(this.context, fileName, content);
        }

        @SuppressWarnings("unused")
        @JavascriptInterface
        public String readTextFromInternalStorage(String fileName){
            return readFile(this.context, fileName);
        }

        @SuppressLint("ObsoleteSdkInt")
        @SuppressWarnings("unused")
        @JavascriptInterface
        public void setStatusBarColor(String color){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                ((Activity)this.context).getWindow().setStatusBarColor(Color.parseColor(color));
        }

    }

    private static class MyLocationListener implements LocationListener {
        final Context context;

        MyLocationListener(Context context){
            this.context = context;
        }

        public void onLocationChanged(Location location) {
            if (location != null) {
                System.out.println("Location was determined!");
            }
        }

        public void onProviderDisabled(String provider) {
            Toast.makeText(this.context, "Error onProviderDisabled", Toast.LENGTH_SHORT).show();
        }

        public void onProviderEnabled(String provider) {
            Toast.makeText(this.context, "onProviderEnabled", Toast.LENGTH_SHORT).show();
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            Toast.makeText(this.context, "onStatusChanged", Toast.LENGTH_LONG).show();
        }
    }
}


// Class for creating functions that can be called from HTTP fragments (#Fragment) -----------------

class CallableFunction{

    Object classObject;
    String methodName, keyword;
    Object[] arguments = new Object[0];

    CallableFunction(Object classObject, String methodName, String keyword){
        this.classObject = classObject;
        this.methodName = methodName;
        this.keyword = keyword;
    }

    CallableFunction(Object classObject, String methodName, String keyword, Object[] arguments){
        this.classObject = classObject;
        this.methodName = methodName;
        this.keyword = keyword;
        this.arguments = arguments;
    }

    @SuppressWarnings("unused")
    public String getKeyword() {
        return this.keyword;
    }

    public void setArguments(Object[] newArguments){
        this.arguments = newArguments;
    }

    public void invokeMethod() {
        try {
            java.lang.reflect.Method method = this.classObject.getClass().getDeclaredMethod(this.methodName, getArgumentClasses());
            method.invoke(classObject, this.arguments);
        } catch (Exception e) {
            System.err.println("Error: " + e);
        }
    }

    private Class<?>[] getArgumentClasses(){
        Class<?>[] argumentClasses = new Class<?>[this.arguments.length];
        for(int i = 0; i < argumentClasses.length; i++){
            argumentClasses[i] = this.arguments[i].getClass();
        }

        return argumentClasses;
    }
}


// Class for the creation of HTML nodes from the Java-Code.

class HtmlNode {
    private String tagName, innerHtml;
    private HashMap<String, String> cssRules, attributes;
    private ArrayList<HtmlNode> childNodes;

    @SuppressWarnings("unused")
    public HtmlNode(String tagName) {
        standardNode(tagName);
    }

    @SuppressWarnings("unused")
    public HtmlNode(String tagName, String innerHtml) {
        standardNode(tagName);
        this.innerHtml = innerHtml;
    }

    public String get() {
        String temp = "<" + this.tagName;

        for (Map.Entry<String, String> entry : this.attributes.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            temp = temp.concat(" " + key + "=\"" + value + "\"");
        }

        if (this.cssRules.size() > 0) {
            temp = temp.concat(" style=\"");

            for (Map.Entry<String, String> entry : this.cssRules.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                temp = temp.concat(key + ": " + value + "; ");
            }

            temp = temp.concat("\"");
        }

        if (this.innerHtml.contentEquals("") && this.getChildNodes().size() < 1) {
            temp = temp.concat("/>");
        } else {
            temp = temp.concat(">" + this.innerHtml);
            for (HtmlNode tempNode : this.childNodes)
                temp = temp.concat(tempNode.get());
            temp = temp.concat("</" + this.tagName + ">");
        }

        return temp;
    }

    private void standardNode(String tagName) {
        this.tagName = tagName;
        this.innerHtml = "";
        this.attributes = new HashMap<>();
        this.cssRules = new HashMap<>();
        this.childNodes = new ArrayList<>();
    }

    @SuppressWarnings("unused")
    public void setTagName(String newTagName) {
        this.tagName = newTagName;
    }

    @SuppressWarnings("unused")
    public String getTagName() {
        return this.tagName;
    }

    @SuppressWarnings("unused")
    public void setInnerHTML(String newInnerHtml) {
        this.innerHtml = newInnerHtml;
    }

    @SuppressWarnings("unused")
    public String getInnerHtml() {
        return this.innerHtml;
    }

    public void setAttribute(String attributeName, String attributeContent) {
        this.attributes.put(attributeName, attributeContent);
    }

    public String getAttribute(String attributeName) {
        return this.attributes.get(attributeName);
    }

    @SuppressWarnings("unused")
    public void setCssAttribute(String cssAttributeName, String cssAttributeContent) {
        this.cssRules.put(cssAttributeName, cssAttributeContent);
    }

    @SuppressWarnings("unused")
    public String getCssAttribute(String cssAttributeName) {
        return this.cssRules.get(cssAttributeName);
    }

    @SuppressWarnings("unused")
    public void appendChild(HtmlNode childNode) {
        this.childNodes.add(childNode);
    }

    @SuppressWarnings("unused")
    public void removeChild(int id) {
        this.childNodes.remove(id);
    }

    @SuppressWarnings("unused")
    public void removeChild(HtmlNode childNode) {
        this.childNodes.remove(childNode);
    }

    public ArrayList<HtmlNode> getChildNodes() {
        return this.childNodes;
    }

    public void setId(String id) {
        this.setAttribute("id", id);
    }

    public String getId() {
        return this.getAttribute("id");
    }

    @SuppressWarnings("unused")
    public void setHtmlClass(String classname) {
        this.setAttribute("class", classname);
    }

    @SuppressWarnings("unused")
    public String getHtmlClass() {
        return this.getAttribute("class");
    }
}