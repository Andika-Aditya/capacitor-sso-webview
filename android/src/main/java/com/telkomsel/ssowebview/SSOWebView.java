package com.telkomsel.ssowebview;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.net.Uri;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SSOWebView {

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
    tvTitle.setLayoutParams(new LinearLayout.LayoutParams(
      0, ViewGroup.LayoutParams.WRAP_CONTENT, 1 f));

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
          /* Biarkan WebView navigate — server akan return JSON body */
          return false;
        }
        return false;
      }

      @Override
      public void onPageFinished(WebView view, String pageUrl) {
        super.onPageFinished(view, pageUrl);

        Uri u = Uri.parse(pageUrl);
        String host = u.getHost();
        String path = u.getPath();

        boolean isRedirectPage = host != null && host.equalsIgnoreCase(redirectHost) &&
          path != null && path.toLowerCase()
          .contains(redirectPathContains.toLowerCase());

        if (!isRedirectPage)
          return;

        final String finalUrl = pageUrl;
        final String finalCode = u.getQueryParameter("code") != null ?
          u.getQueryParameter("code") :
          "";
        final String finalState = u.getQueryParameter("state") != null ?
          u.getQueryParameter("state") :
          "";

        /* Delay 150ms — beri waktu body JSON ter-render ke DOM */
        view.postDelayed(new Runnable() {
          @Override
          public void run() {
            if (webView == null) return;
            webView.evaluateJavascript(
              "(function(){" +
              "  var t = document.body" +
              "    ? (document.body.innerText || document.body.textContent || '')" +
              "    : '';" +
              "  return t.trim();" +
              "})()",
              value -> {
                String body = value;
                if (body != null) {
                  if (body.startsWith("\"") && body.endsWith("\""))
                    body = body.substring(1, body.length() - 1);
                  body = body.replace("\\\"", "\"")
                    .replace("\\n", "\n")
                    .replace("\\r", "")
                    .replace("\\/", "/");
                }
                final String finalBody = (body != null) ? body : "";
                android.util.Log.d("SSOWebView",
                  "body len=" + finalBody.length() +
                  " preview=" + finalBody.substring(
                    0, Math.min(120, finalBody.length())));

                /* FIX: gunakan outer class reference */
                activity.runOnUiThread(() -> SSOWebView.this.dismiss());

                if (listener != null)
                  listener.onRedirectIntercepted(
                    finalUrl, finalCode, finalState, finalBody);
              }
            );
          }
        }, 150);
      }

      private void dismiss() {
        SSOWebView.this.dismiss();
      }
    });

    webView.setLayoutParams(new LinearLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT, 0, 1 f));

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
