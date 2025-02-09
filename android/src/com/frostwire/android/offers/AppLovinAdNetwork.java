/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2016, FrostWire(R). All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.frostwire.android.offers;

import static com.frostwire.android.offers.Offers.DEBUG_MODE;

import android.app.Activity;
import android.content.Context;

import com.andrew.apollo.utils.MusicUtils;
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinSdk;
import com.frostwire.android.core.Constants;
import com.frostwire.android.gui.services.Engine;
import com.frostwire.util.Logger;
import com.frostwire.util.Ref;

import java.lang.ref.WeakReference;

public class AppLovinAdNetwork extends AbstractAdNetwork {

    private static final Logger LOG = Logger.getLogger(AppLovinAdNetwork.class);
    private static AppLovinAdNetwork APP_LOVIN_ADNETWORK = null;
    private AppLovinInterstitialAdapter interstitialAdapter = null;

    private AppLovinAdNetwork() {
    }

    @Override
    public void initialize(final Activity activity) {
        if (abortInitialize(activity)) {
            return;
        }

        final Context applicationContext = activity.getApplicationContext();
        WeakReference<Activity> activityRef = Ref.weak(activity);
        WeakReference<Context> appContextRef = Ref.weak(applicationContext);
        Engine.instance().getThreadPool().execute(() -> {
            try {
                if (!started() && Ref.alive(appContextRef) && Ref.alive(activityRef)) {
                    Context appContext = appContextRef.get();
                    Activity activity1 = activityRef.get();
                    AppLovinSdk.initializeSdk(appContext);
                    AppLovinSdk.getInstance(activity1).getSettings().setMuted(!DEBUG_MODE);
                    AppLovinSdk.getInstance(appContext).getSettings().setVerboseLogging(DEBUG_MODE);
                    LOG.info("AppLovin initialized.");
                    start();
                    loadNewInterstitial(activity1);
                }
            } catch (Throwable e) {
                LOG.error(e.getMessage(), e);
            }
        });
    }

    @Override
    public void loadNewInterstitial(Activity activity) {
        interstitialAdapter = new AppLovinInterstitialAdapter(this, activity);
        AppLovinSdk.getInstance(activity).getAdService().loadNextAd(AppLovinAdSize.INTERSTITIAL, interstitialAdapter);
    }

    @Override
    public String getShortCode() {
        return Constants.AD_NETWORK_SHORTCODE_APPLOVIN;
    }

    @Override
    public String getInUsePreferenceKey() {
        return Constants.PREF_KEY_GUI_USE_APPLOVIN;
    }

    @Override
    public boolean isDebugOn() {
        return DEBUG_MODE;
    }


    @Override
    public boolean showInterstitial(Activity activity,
                                    String placement,
                                    final boolean shutdownAfterwards,
                                    final boolean dismissAfterward) {
        boolean result = false;
        if (enabled() && started()) {
            // make sure video ads are always muted, it's very annoying (regardless of playback status)
            AppLovinSdk.getInstance(activity).getSettings().setMuted(true);
            interstitialAdapter.shutdownAppAfter(shutdownAfterwards);
            interstitialAdapter.dismissActivityAfterwards(dismissAfterward);
            try {
                interstitialAdapter.wasPlayingMusic(MusicUtils.isPlaying());
                result = interstitialAdapter.isAdReadyToDisplay() &&
                        // do not show applovin interstitials on exit
                        (!shutdownAfterwards || !interstitialAdapter.isVideoAd()) &&
                        interstitialAdapter.show(activity, placement);
            } catch (Throwable e) {
                e.printStackTrace();
                result = false;
            }
        }
        return result;
    }

    public static AppLovinAdNetwork getInstance() {
        if (APP_LOVIN_ADNETWORK == null) {
            APP_LOVIN_ADNETWORK = new AppLovinAdNetwork();
        }
        return APP_LOVIN_ADNETWORK;
    }
}
