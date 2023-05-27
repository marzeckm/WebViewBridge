package com.example.webviewbridge;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.example.webviewbridge.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private WebViewBridge js_con;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        com.example.webviewbridge.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Own code can be added here
        js_con = new WebViewBridge(findViewById(R.id.webView1), this);
        js_con.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onBackPressed() {
        if (!js_con.goBack()) super.onBackPressed();
    }
}