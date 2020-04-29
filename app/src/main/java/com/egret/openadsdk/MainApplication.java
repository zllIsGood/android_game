package com.egret.openadsdk;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.egret.openadsdk.sdk.TTAdManagerHolder;
import com.meituan.android.walle.WalleChannelReader;
import com.umeng.analytics.MobclickAgent;
import com.umeng.commonsdk.UMConfigure;

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        //穿山甲SDK初始化
        //强烈建议在应用对应的Application#onCreate()方法中调用，避免出现content为null的异常
        TTAdManagerHolder.init(this);
        //如果明确某个进程不会使用到广告SDK，可以只针对特定进程初始化广告SDK的content
        //if (PROCESS_NAME_XXXX.equals(processName)) {
        //   TTAdManagerHolder.init(this)
        //}

        String channel = WalleChannelReader.getChannel(this.getApplicationContext());
/**
 * 注意: 即使您已经在AndroidManifest.xml中配置过appkey和channel值，也需要在App代码中调
 * 用初始化接口（如需要使用AndroidManifest.xml中配置好的appkey和channel值，
 * UMConfigure.init调用中appkey和channel参数请置为null）。
 */
        UMConfigure.init(this, "5ea93d780cafb248140004fe", channel, 0, "");
        initActivity();
    }
    private void initActivity(){
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(Activity activity) {

            }

            @Override
            public void onActivityResumed(Activity activity) {
                MobclickAgent.onResume(activity);
            }

            @Override
            public void onActivityPaused(Activity activity) {
                MobclickAgent.onPause(activity);
            }

            @Override
            public void onActivityStopped(Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        });
    }
}
