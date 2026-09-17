package com.telkomsel.ssowebview;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "SSOWebView")
public class SSOWebViewPlugin extends Plugin {

    private final SSOWebView implementation = new SSOWebView();

    @PluginMethod
    public void openSSO(PluginCall call) {
        final String url          = call.getString("url");
        final String redirectHost = call.getString("redirectHost", "tdirectmbl.telkomsel.co.id");
        final String redirectPath = call.getString("redirectPathContains", "mobilelogon");
        final String title        = call.getString("title", "Login");

        if (url == null || url.isEmpty()) {
            call.reject("url is required");
            return;
        }

        getActivity().runOnUiThread(() -> {
            try {
                implementation.open(
                    getActivity(),
                    url,
                    redirectHost,
                    redirectPath,
                    title,
                    new SSOWebView.Listener() {
                        @Override
                        public void onRedirectIntercepted(String u, String code, String state) {
                            JSObject ret = new JSObject();
                            ret.put("url", u);
                            ret.put("code", code);
                            ret.put("state", state);
                            notifyListeners("ssoRedirect", ret);
                        }

                        @Override
                        public void onCancelled() {
                            notifyListeners("ssoCancelled", new JSObject());
                        }
                    }
                );
                call.resolve();
            } catch (Exception e) {
                call.reject("Failed to open SSO WebView: " + e.getMessage());
            }
        });
    }

    @PluginMethod
    public void close(PluginCall call) {
        getActivity().runOnUiThread(implementation::dismiss);
        call.resolve();
    }
}
