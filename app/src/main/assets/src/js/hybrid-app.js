const Native = {
    goBack: () => { Android.goBack(); },
    showToast: (text) => { Android.showToast(text); },
    showAlert: (title, text) => { Android.showWarning(title, text); },
    showError: (title, text) => { Android.showError(title, text); },

    systemOutPrintln: (text) => { Android.systemOutPrintln(text); },
    systemErrPrintln: (text) => { Android.systemErrPrintln(text); },
    systemOutPrint: (text) => { Android.systemOutPrint(text); },
    systemErrPrint: (text) => { Android.systemErrPrint(text); },

    getConnectivityStatus: () => { return Android.getConnectivityStatus(); },
    getCurrentLocation: () => { return Android.getCurrentLocation(); },

    takePhoto: () => { return Android.takePhoto(); },

    vibrate: (milliseconds) => { Android.vibrate(milliseconds); },
    vibrateShort: () => { Android.vibrate(100); },
    vibrateMedium: () => { Android.vibrate(200); },
    vibrateLong: () => { Android.vibrate(500); },

    nightModeEnabled: () => { return Android.nightModeEnabled(); },
    setStatusBarColor: (colorCode) => { Android.setStatusBarColor(colorCode); },

    flashlight: (value) => { Android.flashlight(value); },

    writeFile: (name, content) => { Android.writeTextToInternalStorage(name, content); },
    readFile: (name) => { return Android.readTextFromInternalStorage(name); },

    permissionGranted: (permissionName) => { return Android.checkPermission(permissionName); },
    permissionGrantedCamera: () => { return Native.permissionGranted("android.permission.CAMERA"); },
    permissionGrantedInternet: () => { return Native.permissionGranted("android.permission.INTERNET"); },
    permissionGrantedAccessNetworkState: () => { return Native.permissionGranted("android.permission.ACCESS_NETWORK_STATE"); },
    permissionGrantedGPS: () => { return Native.permissionGranted("android.permission.GPS"); },
    permissionGrantedAccessFineLocation: () => { return Native.permissionGranted("android.permission.ACCESS_FINE_LOCATION"); },
    permissionGrantedAccessCoarseLocation: () => { return Native.permissionGranted("android.permission.ACCESS_COARSE_LOCATION"); },
    permissionGrantedWriteExternalStorage: () => { return Native.permissionGranted("android.permission.WRITE_EXTERNAL_STORAGE"); },
    permissionGrantedReadExternalStorage: () => { return Native.permissionGranted("android.permission.READ_EXTERNAL_STORAGE"); },
    permissionGrantedVibrate: () => { return Native.permissionGranted("android.permission.VIBRATE"); },
    requestPermission: (permission, requestCode) => { Android.requestPermission(permission, requestCode); },
    requestPermissionCamera: () => { Native.requestPermission('android.permission.CAMERA'); },
    requestPermissionAccessFineLocation: () => { Native.requestPermission('android.permission.ACCESS_FINE_LOCATION'); },
    requestPermissionAccessCoarseLocation: () => { Native.requestPermission('android.permission.ACCESS_COARSE_LOCATION'); },
    requestPermissionWriteExternalStorage: () => { Native.requestPermission('android.permission.WRITE_EXTERNAL_STORAGE'); },
    requestPermissionReadExternalStorage: () => { Native.requestPermission('android.permission.READ_EXTERNAL_STORAGE'); },

    pushNotification: (notificationTitle, notificationContent) => { Android.pushNotification(notificationTitle, notificationContent); },

    // value = 0: SCREEN_ORIENTATION_PORTRAIT; 1: SCREEN_ORIENTATION_LANDSCAPE; Else: SCREEN_ORIENTATION_UNSPECIFIED
    displayRotationMode: (value) => { Android.displayRotationMode(value); },

    loadUrl: (newUrl) => { Native.callNativeFunction("loadUrl", newUrl); },
    loadData: (newData) => { Native.callNativeFunction("loadData", newData) },
    setPageNotFound: (newUrl) => { Native.callNativeFunction("setPageNotFoundUrl", newUrl); },

    callNativeFunction: (methodIdentifier) => { window.location.hash = methodIdentifier; },
    callNativeFunction: (methodIdentifier, parameters) => {
        var tempUrl = methodIdentifier + "=";

        if(Array.isArray(parameters)){
            for (var i = 0; i < parameters.length; i++){
                if (i < (parameters.length - 1)) tempUrl = tempUrl + parameters[i] + "&";
                else tempUrl = tempUrl + parameters[i];
            }
        }else tempUrl = tempUrl + parameters;

        window.location.hash = tempUrl;
    },
}