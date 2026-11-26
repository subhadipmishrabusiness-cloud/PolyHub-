
package com.polyhub.app;

import android.os.Bundle;
import android.graphics.Color;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.os.Environment;
import android.app.DownloadManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebResourceRequest;
import android.webkit.ValueCallback;
import android.widget.FrameLayout;
import android.view.ViewGroup;
import android.view.View;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.view.ViewCompat;

public class MainActivity extends ComponentActivity {

    private WebView webView;
    private ValueCallback<Uri[]> fileUploadCallback;

    private ActivityResultLauncher<Intent> filePickerLauncher;

    private static final String HOME_URL =
            "https://polyhub-in.vercel.app";

    private static final String HOME_HOST =
            "polyhub-in.vercel.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // File picker result handler
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> handleFilePickerResult(result)
        );

        // Status bar and navigation bar handling
        WindowCompat.setDecorFitsSystemWindows(
                getWindow(), false
        );

        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.BLACK);

        FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                );

        root.addView(webView, params);
        setContentView(root);

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(
                        getWindow(), root
                );

        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);

        ViewCompat.setOnApplyWindowInsetsListener(
                root, (view, insets) -> {

            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
            );

            view.setPadding(
                    0,
                    bars.top,
                    0,
                    bars.bottom
            );

            return insets;
        });

        ViewCompat.requestApplyInsets(root);

        setupWebView();
    }

    private void setupWebView() {

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest request) {

                if (!request.isForMainFrame()) {
                    return false;
                }

                return handleNavigation(
                        request.getUrl().toString()
                );
            }

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view, String url) {

                return handleNavigation(url);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onShowFileChooser(
                    WebView webView,
                    ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams) {

                if (fileUploadCallback != null) {
                    fileUploadCallback.onReceiveValue(null);
                }

                fileUploadCallback = filePathCallback;

                Intent intent = new Intent(
                        Intent.ACTION_GET_CONTENT
                );

                intent.addCategory(Intent.CATEGORY_OPENABLE);

                intent.setType(getFileType(fileChooserParams));

                intent.putExtra(
                        Intent.EXTRA_ALLOW_MULTIPLE,
                        fileChooserParams.getMode()
                        == FileChooserParams.MODE_OPEN_MULTIPLE
                );

                try {
                    filePickerLauncher.launch(
                            Intent.createChooser(
                                    intent, "Select a file"
                            )
                    );
                } catch (ActivityNotFoundException e) {
                    fileUploadCallback = null;
                    filePathCallback.onReceiveValue(null);
                    Toast.makeText(
                            MainActivity.this,
                            "No file picker found",
                            Toast.LENGTH_SHORT
                    ).show();
                    return false;
                }

                return true;
            }
        });

        // Direct file downloads
        webView.setDownloadListener(
                (url, userAgent, contentDisposition,
                 mimeType, contentLength) -> {

            try {
                DownloadManager.Request request =
                        new DownloadManager.Request(
                                Uri.parse(url)
                        );

                String cookies =
                        CookieManager.getInstance()
                        .getCookie(url);

                if (cookies != null) {
                    request.addRequestHeader(
                            "Cookie", cookies
                    );
                }

                request.addRequestHeader(
                        "User-Agent", userAgent
                );

                request.setMimeType(mimeType);

                request.setTitle(
                        URLUtil.guessFileName(
                                url, contentDisposition, mimeType
                        )
                );

                request.setDescription(
                        "Downloading from PolyHub"
                );

                request.setNotificationVisibility(
                        DownloadManager.Request
                        .VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                );

                request.setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        URLUtil.guessFileName(
                                url, contentDisposition, mimeType
                        )
                );

                DownloadManager manager =
                        (DownloadManager) getSystemService(
                                DOWNLOAD_SERVICE
                        );

                if (manager != null) {
                    manager.enqueue(request);

                    Toast.makeText(
                            MainActivity.this,
                            "Download started",
                            Toast.LENGTH_SHORT
                    ).show();
                }

            } catch (Exception e) {
                openExternal(url);
            }
        });

        webView.loadUrl(HOME_URL);
    }

    private String getFileType(
            WebChromeClient.FileChooserParams params) {

        String[] types = params.getAcceptTypes();

        if (types != null && types.length == 1
                && types[0] != null
                && !types[0].isEmpty()) {
            return types[0];
        }

        return "*/*";
    }

    private void handleFilePickerResult(
            ActivityResult result) {

        if (fileUploadCallback == null) {
            return;
        }

        Uri[] results = null;

        if (result.getResultCode() == RESULT_OK
                && result.getData() != null) {

            Intent data = result.getData();

            if (data.getClipData() != null) {

                int count = data.getClipData().getItemCount();

                results = new Uri[count];

                for (int i = 0; i < count; i++) {
                    results[i] =
                            data.getClipData()
                            .getItemAt(i).getUri();
                }

            } else if (data.getData() != null) {
                results = new Uri[]{
                        data.getData()
                };
            }
        }

        fileUploadCallback.onReceiveValue(results);
        fileUploadCallback = null;
    }

    private boolean handleNavigation(String url) {

        Uri uri = Uri.parse(url);

        String scheme = uri.getScheme();
        String host = uri.getHost();

        if (scheme == null) {
            return false;
        }

        if (scheme.equals("http") || scheme.equals("https")) {

            if (host != null &&
                    (host.equalsIgnoreCase(HOME_HOST)
                    || host.toLowerCase().endsWith(
                            "." + HOME_HOST))) {
                return false;
            }

            openExternal(url);
            return true;
        }

        openExternal(url);
        return true;
    }

    private void openExternal(String url) {

        try {
            Intent intent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
            );

            startActivity(intent);

        } catch (ActivityNotFoundException e) {

            Toast.makeText(
                    this,
                    "No app available to open this link",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    @Override
    public void onBackPressed() {

        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {

        if (fileUploadCallback != null) {
            fileUploadCallback.onReceiveValue(null);
            fileUploadCallback = null;
        }

        if (webView != null) {
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }
}
