
package com.polyhub.app;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.graphics.Color;

import androidx.core.graphics.Insets;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.ViewCompat;

public class MainActivity extends Activity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable edge-to-edge and handle system bars manually
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.BLACK);

        setContentView(webView);

        // Keep website content below status bar and above navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(webView, (view, windowInsets) -> {

            Insets systemBars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            );

            view.setPadding(
                0,
                systemBars.top,
                0,
                systemBars.bottom
            );

            return windowInsets;
        });

        ViewCompat.requestApplyInsets(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        webView.loadUrl("https://polyhub-in.vercel.app");
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
