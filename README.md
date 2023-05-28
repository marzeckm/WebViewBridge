# WebViewBridge
The WebViewBridge project is a framework for developing Android hybrid apps using Java, HTML, CSS and JavaScript.

## Installation
To install WebViewBridge, simply download the repository and open the project in `Android Studio`.
You can rename the project to fit your intended application. To rename the project in Android Studio, follow these steps:

1. Close the project: Close the `WebViewBridge` project in Android Studio to ensure a smooth renaming process.

2. Rename the project directory: Locate the root directory of the `WebViewBridge` project in the file system. Rename the directory to the new project name, for example, `NewWebViewBridge`.

3. Open the project in Android Studio: Launch Android Studio and click on `Open an Existing Project`. Navigate to the newly renamed project directory, `NewWebViewBridge`, and select the appropriate `build.gradle` file. Android Studio will detect the project and open it.

4. Update the project name in `settings.gradle`: In the Project pane (usually located on the left side of the Android Studio window), expand the Gradle Scripts folder, and double-click on settings.gradle. Inside the file, you will find a line similar to `rootProject.name = 'WebViewBridge'`. Change `WebViewBridge` to `NewWebViewBridge` and save the file.

5. Refactor package names: Right-click on the main package of your project (usually located in the `java` folder) and select `Refactor` > `Rename`. Enter the new project name, `NewWebViewBridge`, and proceed. Android Studio will update all the references to the package name throughout the project.

6. Update application ID (optional): If you want to change the application ID (package name) of your project, open the `build.gradle` file for the app module (usually located inside the app folder). Locate the applicationId property and change it to the desired new package name.

7. Sync and rebuild the project: Click on the "Sync Now" button that appears in the toolbar to sync the changes made to the Gradle files. After syncing, rebuild your project by clicking on `Build` > `Rebuild Project` or pressing `Ctrl`/`Cmd` + `F9`.

Alternatively, you can also copy only the `WebViewBridge.java` file from the `java` > `main` folder to your project's corresponding folder. Then, create a assets folder in the `java` > `main` directory. Copy the `hybrid-app.js` file from the `assets` folder in this repository to the newly created `assets` folder in your project. To access the native functions in the `hybrid-app.js` file, include the following tag `<script src="hybrid-app.js"/>` in your HTML files.

## Configuration
If you want to use the project as is and incorporate your changes, simply place your HTML files in the assets folder. It is recommended to name the initially called HTML file as `index.html`. If you only want to use the `WebViewBridge.java` and `hybrid-app.js`, you need to make a few adjustments. First, add the following lines to your `AndroidManifest.xml` file:

`<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />`
`<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />`
`<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />`
`<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>`
`<uses-permission android:name="android.permission.INTERNET"/>`
`<uses-permission android:name="android.permission.VIBRATE" />`
`<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />`
`<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28" />`
`<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />`
`<uses-feature android:name="android.hardware.camera" android:required="false" />`

If you also want to access websites or resources in the WebView over the HTTP protocol (without encryption), you need to add the following attribute in the Application tag:

`android:usesCleartextTraffic="true"`

Next, add a WebView to your `activity_main.xml` and name it, for example, `webView1`. Then, go to your `ActivityMain.java` and add the following lines:

`private WebViewBridge js_con;`
`js_con = new WebViewBridge(findViewById(R.id.webView1), this);`
`js_con.loadUrl("file:///android_asset/index.html");`

If you want to enable navigation within the WebView using the Back button, add the following function to the `MainActivity.java` file:

`@Override`
`public void onBackPressed() {`
`    if (!js_con.goBack()) super.onBackPressed();`
`}`

## Usage
If you have followed the steps, you can now access some native functions from your HTML code.

`Native.takePhoto()` - Opens the camera and saves the image to the gallery. This function also returns the location of the image as a string.
`Native.flashlight(boolean)` - Turns the flashlight on or off based on the provided boolean value.
`Native.getCurrentLocation()` - Returns the current GPS location as a string.
`Native.showToast(string)` - Displays a native toast message with the content of the string.

For more functions, feel free to explore the example project in this repository.

## Requirements
- Android Studio 2022.1.1 or higher
- Android SDK 21 (Android 5.0)
- A computer running Windows 7 (or higher), MacOS 10.11 (or higher), Linux Ubuntu 18.10 (or higher), Chrome OS 100 (or higher)

## Contribute
If you want to contribute to the development of this project, feel free to submit pull requests or open issues. Let's make WebViewBridge even better together!

## License
This project is licensed under the [MIT License](LICENSE).
