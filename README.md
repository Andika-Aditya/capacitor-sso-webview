# capacitor-sso-webview

SSO WebView with URL intercept

## Install

To use npm

```bash
npm install capacitor-sso-webview
````

To use yarn

```bash
yarn add capacitor-sso-webview
```

Sync native files

```bash
npx cap sync
```

## API

<docgen-index>

* [`openSSO(...)`](#opensso)
* [`close()`](#close)
* [`addListener('ssoRedirect', ...)`](#addlistenerssoredirect-)
* [`addListener('ssoCancelled', ...)`](#addlistenerssocancelled-)
* [`removeAllListeners()`](#removealllisteners)
* [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### openSSO(...)

```typescript
openSSO(options: SSOOpenOptions) => Promise<void>
```

Buka WebView SSO. Navigasi ke redirectHost akan di-intercept
dan DIBATALKAN sehingga authorization code tidak dikonsumsi.

| Param         | Type                                                      |
| ------------- | --------------------------------------------------------- |
| **`options`** | <code><a href="#ssoopenoptions">SSOOpenOptions</a></code> |

--------------------


### close()

```typescript
close() => Promise<void>
```

Tutup WebView secara manual

--------------------


### addListener('ssoRedirect', ...)

```typescript
addListener(eventName: 'ssoRedirect', listenerFunc: (event: SSORedirectEvent) => void) => Promise<PluginListenerHandle>
```

Fired saat redirect ter-intercept — code masih valid

| Param              | Type                                                                              |
| ------------------ | --------------------------------------------------------------------------------- |
| **`eventName`**    | <code>'ssoRedirect'</code>                                                        |
| **`listenerFunc`** | <code>(event: <a href="#ssoredirectevent">SSORedirectEvent</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

--------------------


### addListener('ssoCancelled', ...)

```typescript
addListener(eventName: 'ssoCancelled', listenerFunc: () => void) => Promise<PluginListenerHandle>
```

Fired saat user menutup WebView tanpa menyelesaikan login

| Param              | Type                        |
| ------------------ | --------------------------- |
| **`eventName`**    | <code>'ssoCancelled'</code> |
| **`listenerFunc`** | <code>() =&gt; void</code>  |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

--------------------


### removeAllListeners()

```typescript
removeAllListeners() => Promise<void>
```

--------------------


### Interfaces


#### SSOOpenOptions

| Prop                       | Type                | Description                                                      |
| -------------------------- | ------------------- | ---------------------------------------------------------------- |
| **`url`**                  | <code>string</code> | URL SSO authorize endpoint                                       |
| **`redirectHost`**         | <code>string</code> | Host yang menandakan redirect, mis. "tdirectmbl.telkomsel.co.id" |
| **`redirectPathContains`** | <code>string</code> | Substring path redirect, mis. "mobilelogon"                      |
| **`title`**                | <code>string</code> | Judul header WebView                                             |


#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |


#### SSORedirectEvent

| Prop        | Type                | Description                           |
| ----------- | ------------------- | ------------------------------------- |
| **`url`**   | <code>string</code> | Full redirect URL yang ter-intercept  |
| **`code`**  | <code>string</code> | Authorization code (belum dikonsumsi) |
| **`state`** | <code>string</code> | State parameter dari OAuth flow       |

</docgen-api>
