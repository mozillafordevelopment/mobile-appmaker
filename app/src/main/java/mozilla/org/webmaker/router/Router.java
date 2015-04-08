/*
    Routable for Android
    Copyright (c) 2013 Turboprop, Inc. <clay@usepropeller.com>
    http://usepropeller.com

    Licensed under the MIT License.

    Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
	THE SOFTWARE.
*/

package mozilla.org.webmaker.router;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import mozilla.org.webmaker.router.exception.NoContextProvidedException;
import mozilla.org.webmaker.router.exception.RouteNotFoundException;
import mozilla.org.webmaker.router.helper.RouterCallback;
import mozilla.org.webmaker.router.helper.RouterOptions;
import mozilla.org.webmaker.router.helper.RouterParams;

public class Router {

    private final Map<String, RouterOptions> routes = new HashMap<>();
    private String rootUrl;
    private final Map<String, RouterParams> cachedRoutes = new HashMap<>();
    private Context context;
    private static final Router router = new Router();

    /**
     * Creates a new instance of the Router, with and sets the parent context.
     * @param context {@link Context} that all {@link Intent}s generated by the router will use
     */
    public Router(Context context) {
        this.context = context;
    }

    /**
     * Empty constructor stud, for globally accessible router instances.
     */
    public Router() {}

    /**
     * A globally accessible Router instance that will work for most use cases.
     * @return A globally accessible instance of Router
     */
    public static Router sharedRouter() {
        return router;
    }

    /**
     * Gets the parent context for this instance of the router.
     * @return The parent context for the router
     */
    public Context getContext() {
        return context;
    }

    /**
     * Sets the parent context for this instance of the router.
     * @param context Sets the parent context for the router
     */
    public void setContext(Context context) {
        this.context = context;
    }

    /**
     * Map a URL to a callback
     * @param format The URL being mapped; for example, "users/:id" or "groups/:id/topics/:topic_id"
     * @param callback {@link mozilla.org.webmaker.router.helper.RouterCallback} instance which contains the code to execute when the URL is opened
     */
    public void map(String format, RouterCallback callback) {
        RouterOptions options = new RouterOptions();
        options.setCallback(callback);
        this.map(format, null, options);
    }

    /**
     * Map a URL to open an {@link Activity}
     * @param format The URL being mapped; for example, "users/:id" or "groups/:id/topics/:topic_id"
     * @param clazz The {@link Activity} class to be opened with the URL
     */
    public void map(String format, Class<? extends Activity> clazz) {
        this.map(format, clazz, null);
    }

    /**
     * Map a URL to open an {@link Activity}
     * @param format The URL being mapped; for example, "users/:id" or "groups/:id/topics/:topic_id"
     * @param clazz The {@link Activity} class to be opened with the URL
     * @param options The {@link RouterOptions} to be used for more granular and customized options for when the URL is opened
     */
    public void map(String format, Class<? extends Activity> clazz, RouterOptions options) {
        if (options == null) { options = new RouterOptions(clazz); }
        this.routes.put(format, options);
    }

    /**
     * Set the root url; used when opening an activity or callback via RouterActivity
     * @param rootUrl The URL format to use as the root
     */
    public void setRootUrl(String rootUrl) {
        this.rootUrl = rootUrl;
    }

    /**
     * @return The router's root URL, or null.
     */
    public String getRootUrl() {
        return rootUrl;
    }

    /**
     * Open a URL using the operating system's configuration (such as opening a link to Chrome or a video to YouTube)
     * @param url The URL; for example, "http://www.youtube.com/watch?v=oHg5SJYRHA0"
     */
    public void openExternal(String url) {
        this.openExternal(url, null);
    }


    /**
     * Open a URL using the operating system's configuration (such as opening a link to Chrome or a video to YouTube)
     * @param url The URL; for example, "http://www.youtube.com/watch?v=oHg5SJYRHA0"
     * @param extras The {@link Bundle} which contains the extras to be assigned to the generated {@link Intent}
     */
    public void openExternal(String url, Bundle extras) {
        if (context == null) throw new NoContextProvidedException("You need to supply context for Router " + this.toString());
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (extras != null) intent.putExtras(extras);
        context.startActivity(intent);
    }

    /**
     * Open a mapped URL set using {@link #map(String, Class)} or {@link #map(String, RouterCallback)}
     * @param url The URL; for example, "users/16" or "groups/5/topics/20"
     */
    public void open(String url) {
        this.open(url, null);
    }

    /**
     * Open a mapped URL set using {@link #map(String, Class)} or {@link #map(String, RouterCallback)}
     * @param url The URL; for example, "users/16" or "groups/5/topics/20"
     * @param extras The {@link Bundle} which contains the extras to be assigned to the generated {@link Intent}
     */
    public void open(String url, Bundle extras) {
        if (context == null) throw new NoContextProvidedException("You need to supply context for Router " + this.toString());
        RouterParams params = this.paramsForUrl(url);
        RouterOptions options = params.routerOptions;

        if (options.getCallback() != null) {
            options.getCallback().run(params.openParams);
            return;
        }

        Intent intent = this.intentFor(context, url);
        if (intent == null) return;
        if (extras != null) intent.putExtras(extras);
        context.startActivity(intent);
    }

    /**
     * @param url The URL; for example, "users/16" or "groups/5/topics/20"
     * @return The {@link Intent} for the url
     */
    public Intent intentFor(String url) {
        RouterParams params = this.paramsForUrl(url);
        RouterOptions options = params.routerOptions;
        Intent intent = new Intent();

        if (options.getDefaultParams() != null) {
            for (Entry<String, String> entry : options.getDefaultParams().entrySet()) {
                intent.putExtra(entry.getKey(), entry.getValue());
            }
        }

        for (Entry<String, String> entry : params.openParams.entrySet()) {
            intent.putExtra(entry.getKey(), entry.getValue());
        }
        return intent;
    }

    /**
     * @param url The URL to check
     * @return Whether or not the URL refers to an anonymous callback function
     */
    public boolean isCallbackUrl(String url) {
        RouterParams params = this.paramsForUrl(url);
        RouterOptions options = params.routerOptions;
        return options.getCallback() != null;
    }

    /**
     *
     * @param context The context which is spawning the intent
     * @param url The URL; for example, "users/16" or "groups/5/topics/20"
     * @return The {@link Intent} for the url, with the correct {@link Activity} set, or null.
     */
    public Intent intentFor(Context context, String url) {
        RouterParams params = this.paramsForUrl(url);
        RouterOptions options = params.routerOptions;
        if (options.getCallback() != null) return null;
        Intent intent = intentFor(url);
        intent.setClass(context, options.getOpenClass());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    /*
     * Takes a url (i.e. "/users/16/hello") and breaks it into a {@link RouterParams} instance where
     * each of the parameters (like ":id") has been parsed.
     */
    private RouterParams paramsForUrl(String url) {
        final String cleanedUrl = cleanUrl(url);
        if (this.cachedRoutes.get(url) != null) return this.cachedRoutes.get(cleanedUrl);
        String[] givenParts = cleanedUrl.split("/");
        RouterOptions openOptions = null;
        RouterParams openParams = null;

        for (Entry<String, RouterOptions> entry : this.routes.entrySet()) {
            String routerUrl = cleanUrl(entry.getKey());
            RouterOptions routerOptions = entry.getValue();
            String[] routerParts = routerUrl.split("/");

            if (routerParts.length != givenParts.length) continue;
            Map<String, String> givenParams = urlToParamsMap(givenParts, routerParts);
            if (givenParams == null) continue;

            openOptions = routerOptions;
            openParams = new RouterParams();
            openParams.openParams = givenParams;
            openParams.routerOptions = routerOptions;
            break;
        }

        if (openOptions == null) throw new RouteNotFoundException("No route found for url " + url);

        this.cachedRoutes.put(cleanedUrl, openParams);
        return openParams;
    }

    /**
     *
     * @param givenUrlSegments An array representing the URL path attempting to be opened (i.e. ["users", "42"])
     * @param routerUrlSegments An array representing a possible URL match for the router (i.e. ["users", ":id"])
     * @return A map of URL parameters if it's a match (i.e. {"id" => "42"}) or null if there is no match
     */
    private Map<String, String> urlToParamsMap(String[] givenUrlSegments, String[] routerUrlSegments) {
        Map<String, String> formatParams = new HashMap<>();
        for (int index = 0; index < routerUrlSegments.length; index++) {
            String routerPart = routerUrlSegments[index];
            String givenPart = givenUrlSegments[index];

            if (routerPart.charAt(0) == ':') {
                String key = routerPart.substring(1, routerPart.length());
                formatParams.put(key, givenPart);
                continue;
            }

            if (!routerPart.equals(givenPart)) return null;
        }
        return formatParams;
    }


    /**
     * Cleans a URL
     * @param url The URL to clean
     * @return cleaned url
     */
    private String cleanUrl(String url) {
        if (url.startsWith("/")) return url.substring(1, url.length());
        return url;
    }
}
