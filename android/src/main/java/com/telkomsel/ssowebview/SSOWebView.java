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
        void onRedirectIntercepted(String url, String code, String state);
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
        final Listener listener
    ) {
        alreadyRedirected = false;

        dialog = new Dialog(activity, android.R.style.Theme_Light_NoTitleBar_Fullscreen);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));

        /* ── Header bar ── */
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setBackgroundColor(Color.parseColor("#1A2B4A"));
        header.setPadding(48, 72, 48, 48);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView tvTitle = new TextView(activity);
        tvTitle.setText(title != null ? title : "Login");
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(16);
        tvTitle.setLayoutParams(new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        ));

        TextView btnClose = new TextView(activity);
        btnClose.setText("Tutup");
        btnClose.setTextColor(Color.WHITE);
        btnClose.setTextSize(14);
        btnClose.setPadding(32, 16, 16, 16);
        btnClose.setOnClickListener(v -> {
            boolean wasRedirected = alreadyRedirected;
            dismiss();
            if (!wasRedirected && listener != null) listener.onCancelled();
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

                boolean isRedirect =
                    host != null && host.equalsIgnoreCase(redirectHost) &&
                    path != null && path.toLowerCase()
                        .contains(redirectPathContains.toLowerCase());

                if (isRedirect && !alreadyRedirected) {
                    alreadyRedirected = true;

                    String code  = u.getQueryParameter("code");
                    String state = u.getQueryParameter("state");

                    dismiss();

                    if (listener != null) {
                        listener.onRedirectIntercepted(
                            u.toString(),
                            code  != null ? code  : "",
                            state != null ? state : ""
                        );
                    }
                    return true;   /* BLOKIR navigasi — code TIDAK dikonsumsi */
                }
                return false;
            }
        });

        webView.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ));

        root.addView(header);
        root.addView(webView);

        dialog.setContentView(root);
        dialog.setCancelable(true);
        dialog.setOnCancelListener(d -> {
            if (!alreadyRedirected && listener != null) listener.onCancelled();
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
