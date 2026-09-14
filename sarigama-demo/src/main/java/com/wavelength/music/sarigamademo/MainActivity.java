package com.wavelength.music.sarigamademo;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String HOME = "https://sarigama.lk/";
    private static final String PLAYER = "https://sarigama.lk/player";
    private WebView web;
    private TextView status;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(11,11,15));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(11,11,15));

        status = new TextView(this);
        status.setText("SARIGAMA EMBED TEST • loading…");
        status.setTextColor(Color.WHITE);
        status.setTextSize(12);
        status.setGravity(Gravity.CENTER);
        status.setPadding(12,12,12,12);
        root.addView(status, new LinearLayout.LayoutParams(-1,-2));

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView v, String url) {
                status.setText("CONNECTED • " + (url.contains("/player") ? "NOW PLAYING" : "SARIGAMA"));
            }
        });
        root.addView(web, new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(6,6,6,8);
        nav.addView(tab("Home", v -> web.loadUrl(HOME)), weight());
        nav.addView(tab("Search", v -> openSearch()), weight());
        nav.addView(tab("Now Playing", v -> web.loadUrl(PLAYER)), weight());
        root.addView(nav, new LinearLayout.LayoutParams(-1,-2));
        setContentView(root);
        web.loadUrl(HOME);
    }

    private LinearLayout.LayoutParams weight() { return new LinearLayout.LayoutParams(0, -2, 1); }

    private Button tab(String label, View.OnClickListener click) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(12);
        b.setAllCaps(false);
        b.setOnClickListener(click);
        return b;
    }

    private void openSearch() {
        web.loadUrl(HOME);
        web.postDelayed(() -> web.evaluateJavascript(
            "(function(){var e=document.querySelector('input[placeholder*=Browse],input[type=search],input[type=text]');if(e){e.scrollIntoView({behavior:'smooth',block:'center'});e.focus();e.click();return 'focused';}return 'not-found';})()",
            r -> status.setText(r != null && r.contains("focused") ? "SEARCH READY • type a Sinhala song/artist" : "SEARCH • use Sarigama search box")
        ), 1800);
    }

    @Override public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
}
