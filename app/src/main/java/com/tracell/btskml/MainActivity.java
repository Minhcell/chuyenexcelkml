package com.tracell.btskml;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private WebView web;
    private ValueCallback<Uri[]> filePathCallback;
    private static final int REQ_FILE = 1;
    private static final int REQ_SAVE = 2;
    private byte[] pendingBytes;
    private String pendingName = "file";
    private String pendingMime = "application/octet-stream";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        setContentView(web);

        WebSettings ws = web.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setAllowFileAccess(true);
        ws.setAllowContentAccess(true);

        web.addJavascriptInterface(new Bridge(), "AndroidSave");

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                filePathCallback = callback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                try {
                    startActivityForResult(Intent.createChooser(intent, "Chon file"), REQ_FILE);
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }
        });

        web.loadUrl("file:///android_asset/index.html");
    }

    public class Bridge {
        @JavascriptInterface
        public void saveFile(final String name, final String mime, final String base64) {
            try {
                pendingBytes = Base64.decode(base64, Base64.DEFAULT);
            } catch (Exception e) {
                pendingBytes = null;
            }
            pendingName = (name == null || name.isEmpty()) ? "file" : name;
            pendingMime = (mime == null || mime.isEmpty()) ? "application/octet-stream" : mime;
            runOnUiThread(new Runnable() {
                public void run() {
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType(pendingMime);
                    intent.putExtra(Intent.EXTRA_TITLE, pendingName);
                    try {
                        startActivityForResult(intent, REQ_SAVE);
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Khong mo duoc hop luu", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_FILE) {
            Uri[] result = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                result = new Uri[]{ data.getData() };
            }
            if (filePathCallback != null) {
                filePathCallback.onReceiveValue(result);
                filePathCallback = null;
            }
        } else if (requestCode == REQ_SAVE) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null && pendingBytes != null) {
                try {
                    OutputStream os = getContentResolver().openOutputStream(data.getData());
                    os.write(pendingBytes);
                    os.flush();
                    os.close();
                    Toast.makeText(this, "Da luu " + pendingName, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Luu loi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
            pendingBytes = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
