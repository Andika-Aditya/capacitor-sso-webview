package com.telkomsel.ssowebview;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SSOWebView {

  private static final String TAG = "SSOWebView";

  /* Single background thread — cukup untuk satu exchange per login attempt */
  private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

  public interface Listener {
    void onRedirectIntercepted(String url, String code, String state, String body);

    void onCancelled();
  }

  private Dialog dialog;
  private WebView webView;
  private boolean alreadyRedirected = false;

  public void open(
    final Activity activity,
    final String url,
    final String redirectHost,
    final String redirectPathContains,
    final String title,
    final Listener listener) {
    alreadyRedirected = false;

    dialog = new Dialog(activity, android.R.style.Theme_Light_NoTitleBar_Fullscreen);

    LinearLayout root = new LinearLayout(activity);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setLayoutParams(new LinearLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT));

    /* ── Header bar ── */
    LinearLayout header = new LinearLayout(activity);
    header.setOrientation(LinearLayout.HORIZONTAL);
    header.setBackgroundColor(Color.parseColor("#1A2B4A"));
    header.setPadding(48, 72, 48, 48);
    header.setGravity(Gravity.CENTER_VERTICAL);
    header.setLayoutParams(new LinearLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT));

    TextView tvTitle = new TextView(activity);
    tvTitle.setText(title != null ? title : "Login");
    tvTitle.setTextColor(Color.WHITE);
    tvTitle.setTextSize(16);
    tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

    TextView btnClose = new TextView(activity);
    btnClose.setText("Tutup");
    btnClose.setTextColor(Color.WHITE);
    btnClose.setTextSize(14);
    btnClose.setPadding(32, 16, 16, 16);
    btnClose.setOnClickListener(v -> {
      boolean wasRedirected = alreadyRedirected;
      dismiss();
      if (!wasRedirected && listener != null)
        listener.onCancelled();
    });

    header.addView(tvTitle);
    header.addView(btnClose);

    /* ── WebView ── */
    webView = new WebView(activity);
    WebSettings s = webView.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setDatabaseEnabled(true);
    s.setUseWideViewPort(true);
    s.setLoadWithOverviewMode(true);
    s.setSupportZoom(false);
    s.setJavaScriptCanOpenWindowsAutomatically(true);

    CookieManager cm = CookieManager.getInstance();
    cm.setAcceptCookie(true);
    cm.setAcceptThirdPartyCookies(webView, true);

    webView.setWebViewClient(new WebViewClient() {

      @Override
      public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        Uri u = request.getUrl();
        String host = u.getHost();
        String path = u.getPath();

        boolean isRedirect = host != null && host.equalsIgnoreCase(redirectHost) &&
          path != null && path.toLowerCase()
          .contains(redirectPathContains.toLowerCase());

        if (isRedirect && !alreadyRedirected) {
          alreadyRedirected = true;

          final String finalUrl   = u.toString();
          final String finalCode  = u.getQueryParameter("code")  != null
                                    ? u.getQueryParameter("code")  : "";
          final String finalState = u.getQueryParameter("state") != null
                                    ? u.getQueryParameter("state") : "";

          /*
           * PENTING: BATALKAN navigasi (return true), jangan biarkan WebView
           * merender URL ini. Endpoint redirect mengembalikan JSON, bukan
           * HTML — jika WebView mencoba menampilkannya sebagai halaman, hasil
           * yang didapat adalah halaman error native "Webpage not available",
           * bukan body JSON, sehingga JSON.parse di sisi JS selalu gagal.
           *
           * Sebagai gantinya, ambil body via HTTP request native di background
           * thread — pola yang sama seperti fetchJsonFromUrl() pada
           * LoginIdamActivity, yang sudah terbukti bekerja untuk flow login
           * berbasis WebView redirect lainnya di aplikasi ini.
           */
          dismiss();
          fetchRedirectBody(finalUrl, finalCode, finalState, listener);
          return true;
        }
        return false;
      }
    });

    webView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

    root.addView(header);
    root.addView(webView);

    dialog.setContentView(root);
    dialog.setCancelable(true);
    dialog.setOnCancelListener(d -> {
      if (!alreadyRedirected && listener != null)
        listener.onCancelled();
    });
    dialog.show();

    webView.loadUrl(url);
  }

  private void fetchRedirectBody(
      final String url,
      final String code,
      final String state,
      final Listener listener) {

    EXECUTOR.execute(() -> {
      String body;
      HttpURLConnection conn = null;
      try {
        conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(20000);
        conn.setRequestProperty("Accept", "application/json");

        int status = conn.getResponseCode();
        InputStream is = (status >= 200 && status < 400)
            ? conn.getInputStream() : conn.getErrorStream();
        body = readStream(is);

        Log.d(TAG, "fetchRedirectBody status=" + status + " len=" + body.length());
      } catch (Exception e) {
        Log.e(TAG, "fetchRedirectBody failed", e);
        /*
         * Bungkus error sebagai JSON agar konsumen JS (yang selalu mem-parse
         * body sebagai JSON dan memeriksa `status`) mendapat pesan yang jelas,
         * alih-alih body kosong atau string mentah yang gagal di-parse.
         */
        body = "{\"status\":0,\"status_message\":\"" + escapeJson(e.getMessage()) + "\"}";
      } finally {
        if (conn != null) conn.disconnect();
      }

      final String finalBody = body;
      new Handler(Looper.getMainLooper()).post(() -> {
        if (listener != null) {
          listener.onRedirectIntercepted(url, code, state, finalBody);
        }
      });
    });
  }

  private static String readStream(InputStream is) throws IOException {
    if (is == null) return "";
    StringBuilder sb = new StringBuilder();
    try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
      String line;
      while ((line = br.readLine()) != null) sb.append(line);
    }
    return sb.toString();
  }

  private static String escapeJson(String s) {
    if (s == null) return "Unknown network error";
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  public void dismiss() {
    try {
      if (webView != null) {
        webView.stopLoading();
        webView.destroy();
        webView = null;
      }
      if (dialog != null && dialog.isShowing()) {
        dialog.dismiss();
      }
      dialog = null;
    } catch (Exception ignored) {}
  }
}
