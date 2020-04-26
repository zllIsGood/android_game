package com.egret.openadsdk;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdDislike;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTBannerAd;
import com.bytedance.sdk.openadsdk.TTInteractionAd;
import com.bytedance.sdk.openadsdk.TTNativeExpressAd;
import com.egret.openadsdk.sdk.AdCode;
//import com.egret.openadsdk.sdk.BannerExpressActivity;
import com.egret.openadsdk.sdk.BannerExpressActivity;
import com.egret.openadsdk.sdk.DeviceActivity;
import com.egret.openadsdk.sdk.FullScreenVideoActivity;
import com.egret.openadsdk.sdk.RewardVideoActivity;
import com.egret.openadsdk.sdk.SplashActivity;
import com.egret.openadsdk.sdk.TTAdManagerHolder;
import com.egret.openadsdk.sdk.TToast;
import com.google.gson.JsonObject;

import org.egret.runtime.launcherInterface.INativePlayer;
import org.egret.egretnativeandroid.EgretNativeAndroid;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
//Android项目发布设置详见doc目录下的README_ANDROID.md

public class MainActivity extends Activity {
    private final String TAG = "MainActivity";
    private EgretNativeAndroid nativeAndroid;
    private TTAdNative mTTAdNative;
    private FrameLayout mBannerContainer;
    public static MainActivity instance = null;
    private String  DeviceID;
    View bannerView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        instance = this;
        nativeAndroid = new EgretNativeAndroid(this);
        if (!nativeAndroid.checkGlEsVersion()) {
            Toast.makeText(this, "This device does not support OpenGL ES 2.0.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        nativeAndroid.config.showFPS = false;
        nativeAndroid.config.fpsLogTime = 30;
        nativeAndroid.config.disableNativeRender = false;
        nativeAndroid.config.clearCache = false;
        nativeAndroid.config.loadingTimeout = 0;

        setExternalInterfaces();
        
        if (!nativeAndroid.initialize("https://zmg.zmfamily.cn/word/game/index.html")) {
            Toast.makeText(this, "Initialize native failed.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        setContentView(nativeAndroid.getRootFrameLayout());

        this.initJSEvent();

        //step2:创建TTAdNative对象，createAdNative(Context context) banner广告context需要传入Activity对象
        mTTAdNative = TTAdManagerHolder.get().createAdNative(this);
        // 申请部分权限,建议在sdk初始化前申请,如：READ_PHONE_STATE、ACCESS_COARSE_LOCATION及ACCESS_FINE_LOCATION权限，
        // 以获取更好的广告推荐效果，如read_phone_state,防止获取不了imei时候，下载类广告没有填充的问题。
        TTAdManagerHolder.get().requestPermissionIfNecessary(this);

        try {
            //获取设备id
            //TelephonyManager TelephonyMgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            final TelephonyManager manager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                DeviceID = wm.getConnectionInfo().getMacAddress();
                if (DeviceID!=null) return;;
            }
            DeviceID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            if (DeviceID!=null) return;;
            if(manager.getDeviceId() == null || manager.getDeviceId().equals("")) {
                if (Build.VERSION.SDK_INT >= 23) {
                    DeviceID = manager.getDeviceId(0);
                    if (DeviceID!=null) return;;
                }
            }else{
                DeviceID = manager.getDeviceId();
            }
            //DeviceID = TelephonyMgr.getDeviceId();
        }
        catch(Exception e) {
            e.printStackTrace();
        }

    }

    public  void initJSEvent(){
        //监听来自JS的设备消息
        nativeAndroid.setExternalInterface("TTGetDeviceID", new INativePlayer.INativeInterface() {
            @Override
            public void callback(String dataFromJs) {
                MainActivity.jsEvent(AdCode.DeviceID, DeviceID);
//                MainActivity.jsEvent(AdCode.DeviceID, "id=1");
//                Log.d(TAG, DeviceID);
            }
        });

        //监听来自JS的开屏视频消息
        nativeAndroid.setExternalInterface("TTSplashAd", new INativePlayer.INativeInterface() {
            @Override
            public void callback(String dataFromJs) {
                Intent intent = new Intent(MainActivity.this, SplashActivity.class);
                intent.putExtra("splash_code",AdCode.splash_code);
                intent.putExtra("is_express", false);
                startActivityForResult(intent, AdCode.OPENADSDK);
            }
        });
        //监听来自JS的激励视频消息
        nativeAndroid.setExternalInterface("TTRewardVideoAd", new INativePlayer.INativeInterface() {
            @Override
            public void callback(String dataFromJs) {
                Intent intent = new Intent(MainActivity.this, RewardVideoActivity.class);
                intent.putExtra("horizontal_rit",AdCode.reward_horizontal_code);
                intent.putExtra("vertical_rit",AdCode.reward_vertical_code);

                JSONObject object = null;
                try {
                    object = new JSONObject(dataFromJs);
                    Boolean is_horizontal = object.getBoolean("is_horizontal");
                    intent.putExtra("is_horizontal",is_horizontal);
                    String userID = object.getString("userID");
                    intent.putExtra("userID",userID);
                    int rewardAmount = object.getInt("rewardAmount");
                    intent.putExtra("rewardAmount",rewardAmount);
                    String rewardName = object.getString("rewardName");
                    intent.putExtra("rewardName",rewardName);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                startActivityForResult(intent, AdCode.OPENADSDK);
            }
        });
        //监听来自JS的全屏视频消息
        nativeAndroid.setExternalInterface("TTFullScreenVideoAd", new INativePlayer.INativeInterface() {
            @Override
            public void callback(String dataFromJs) {
                Intent intent = new Intent(MainActivity.this, FullScreenVideoActivity.class);
                intent.putExtra("horizontal_rit",AdCode.full_horizontal_code);
                intent.putExtra("vertical_rit",AdCode.full_vertical_code);

                JSONObject object = null;
                try {
                    object = new JSONObject(dataFromJs);
                    Boolean is_horizontal = object.getBoolean("is_horizontal");
                    intent.putExtra("is_horizontal",is_horizontal);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                startActivityForResult(intent, AdCode.OPENADSDK);
            }
        });
        //监听来自JS的Banner广告消息
        nativeAndroid.setExternalInterface("TTBannerExpressAd", new INativePlayer.INativeInterface() {
            @Override
            public void callback(String dataFromJs) {
                String code = AdCode.banner_code;
                if(mBannerContainer==null){
                    mBannerContainer =  nativeAndroid.getRootFrameLayout();
                }
                JSONObject object = null;
                try {
                    object = new JSONObject(dataFromJs);
                    Boolean is_top = object.getBoolean("is_top");
                    int width;
                    int height;
                    width = object.getInt("width");
                    height = object.getInt("height");

//                    MainActivity.instance.loadBannerAd(code,is_top,width,height );
//                    MainActivity.instance.loadNewBannerAd(code,is_top,width,height );
                    Intent intent = new Intent(MainActivity.this, BannerExpressActivity.class);
                    intent.putExtra("splash_code",AdCode.reward_vertical_code);
                    intent.putExtra("is_express", true);
                    startActivityForResult(intent, AdCode.OPENADSDK);
                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }
        });
        //监听来自JS的插屏广告消息
        nativeAndroid.setExternalInterface("TTInteractionAd", new INativePlayer.INativeInterface() {
            @Override
            public void callback(String dataFromJs) {
                String code = AdCode.interaction_code;
                JSONObject object = null;
                try {
                    int width;
                    int height;
                    object = new JSONObject(dataFromJs);
                    width = object.getInt("width");
                    height = object.getInt("height");
                    MainActivity.instance.loadInteractionAd(code,width,height);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

    }




    @Override
    protected void onPause() {
        super.onPause();
        nativeAndroid.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        nativeAndroid.resume();
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent keyEvent) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            nativeAndroid.exitGame();
        }

        return super.onKeyDown(keyCode, keyEvent);
    }

    private void setExternalInterfaces() {
        nativeAndroid.setExternalInterface("sendToNative", new INativePlayer.INativeInterface() {
            @Override
            public void callback(String message) {
                String str = "Native get message: ";
                str += message;
                Log.d(TAG, str);
                nativeAndroid.callExternalInterface("sendToJS", str);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == AdCode.OPENADSDK&&resultCode == AdCode.SplashAd) {
//        }else if(requestCode == AdCode.OPENADSDK&&resultCode == AdCode.RewardVideoAd) {
//        }
    }
    public static void jsEvent(int adcode,String json){
        String tag = "";
        if(adcode == AdCode.RewardVideoAd){
            tag = "TTRewardVideoAd-js";
        }else if(adcode == AdCode.SplashAd){
            tag = "TTSplashAd-js";
        }else if(adcode == AdCode.FullScreenVideoAd){
            tag = "TTFullScreenVideoAd-js";
        }else if(adcode == AdCode.BannerExpressAd){
            tag = "TTBannerExpressAd-js";
        }else if(adcode == AdCode.InteractionAd){
            tag = "TTInteractionAd-js";
        }else if(adcode == AdCode.DeviceID){
            tag = "TTGetDeviceID-js";
        }
        MainActivity.instance.send2JS(tag,json);
        Log.d(tag, json);
    }

    public  void send2JS(String tag ,String json){
        nativeAndroid.callExternalInterface(tag, json);
    }
    private TTNativeExpressAd mTTAd;
//    private void loadNewBannerAd(String codeId, final boolean isTop, final int width, final int height) {
//        //step4:创建广告请求参数AdSlot,具体参数含义参考文档
//        AdSlot adSlot = new AdSlot.Builder()
//                .setCodeId(codeId) //广告位id
//                .setSupportDeepLink(true)
////                .setImageAcceptedSize(600, 150)
////                .setImageAcceptedSize(width,height)
//                .setAdCount(1) //请求广告数量为1到3条
//                .setExpressViewAcceptedSize(width, height) //期望模板广告view的size,单位dp
//                .build();
//        //step5:请求广告，对请求回调的广告作渲染处理
//        mTTAdNative.loadBannerExpressAd(adSlot, new TTAdNative.NativeExpressAdListener() {
//            @Override
//            public void onError(int code, String message) {
//                TToast.show(MainActivity.this, "load error : " + code + ", " + message);
//                if(bannerView!=null){
//                    mBannerContainer.removeView(bannerView);
//                }
//                JsonObject json = new JsonObject();
//                json.addProperty("event","onError" );
//                MainActivity.jsEvent(AdCode.BannerExpressAd,json.toString());
//            }
//
//            @Override
//            public void onNativeExpressAdLoad(List<TTNativeExpressAd> ads) {
//                Log.i(TAG, "onNativeExpressAdLoad:1 ");
//                Log.i(TAG, String.valueOf(ads));
//                mTTAd = ads.get(0);
//                Log.i(TAG, String.valueOf(mTTAd));
//                if (mTTAd == null) {
//                    return;
//                }
//                if(bannerView!=null){
//                    mBannerContainer.removeView(bannerView);
//                    bannerView = null;
//                }
//                mTTAd.render();
////                bannerView = mTTAd.getBannerView();
////                if (bannerView == null) {
////                    return;
////                }
//                Rect rect = new Rect();
//                getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
//                FrameLayout.LayoutParams params =  new FrameLayout.LayoutParams(rect.width(), (int)((((double)height)/((double) width))*rect.width()));
//                if(!isTop){
//                    params.gravity = Gravity.BOTTOM;
//                }else{
//                    params.gravity = Gravity.TOP;
//                }
////                bannerView.setLayoutParams(params);
//
//                //设置轮播的时间间隔  间隔在30s到120秒之间的值，不设置默认不轮播
//                mTTAd.setSlideIntervalTime(30 * 1000);
////                mBannerContainer.addView(bannerView);
//                //设置广告互动监听回调
//                mTTAd.setExpressInteractionListener(new TTNativeExpressAd.ExpressAdInteractionListener() {
//                    @Override
//                    public void onAdClicked(View view, int type) {
//                        TToast.show(MainActivity.this, "广告被点击");
//                        JsonObject json = new JsonObject();
//                        json.addProperty("event","onAdClicked" );
//                        MainActivity.jsEvent(AdCode.BannerExpressAd,json.toString());
//                    }
//
//                    @Override
//                    public void onAdShow(View view, int type) {
//                        TToast.show(MainActivity.this, "广告展示");
//                        JsonObject json = new JsonObject();
//                        json.addProperty("event","onAdShow" );
//                        MainActivity.jsEvent(AdCode.BannerExpressAd,json.toString());
//                    }
//
//                    @Override
//                    public void onRenderFail(View view, String s, int i) {
//
//                    }
//
//                    @Override
//                    public void onRenderSuccess(View view, float v, float v1) {
//
//                    }
//                });
//                //在banner中显示网盟提供的dislike icon，有助于广告投放精准度提升
////                mTTAd.setShowDislikeIcon(new TTAdDislike.DislikeInteractionCallback() {
////                    @Override
////                    public void onSelected(int position, String value) {
////                        TToast.show(MainActivity.this, "点击 " + value);
////                        JsonObject json = new JsonObject();
////                        json.addProperty("event","onSelected" );
////                        json.addProperty("value",value);
////                        MainActivity.jsEvent(AdCode.BannerExpressAd,json.toString());
////                        //用户选择不喜欢原因后，移除广告展示
//////                        mBannerContainer.removeAllViews();
////                        if(bannerView!=null){
////                            mBannerContainer.removeView(bannerView);
////                            bannerView = null;
////                        }
////                    }
////
////                    @Override
////                    public void onCancel() {
////                        TToast.show(MainActivity.this, "点击取消 ");
////                        JsonObject json = new JsonObject();
////                        json.addProperty("event","onCancel" );
////                        MainActivity.jsEvent(AdCode.BannerExpressAd,json.toString());
////                    }
////                });
//
//
//            }
//        });
//    }
    private void loadBannerAd(String codeId, final boolean isTop, final int width, final int height) {
        //step4:创建广告请求参数AdSlot,具体参数含义参考文档
        AdSlot adSlot = new AdSlot.Builder()
                .setCodeId(codeId) //广告位id
                .setSupportDeepLink(true)
//                .setImageAcceptedSize(600, 150)
                .setImageAcceptedSize(width,height)
                .build();
        //step5:请求广告，对请求回调的广告作渲染处理
        mTTAdNative.loadBannerAd(adSlot, new TTAdNative.BannerAdListener() {
            @Override
            public void onError(int code, String message) {
                TToast.show(MainActivity.this, "load error : " + code + ", " + message);
                if(bannerView!=null){
                    mBannerContainer.removeView(bannerView);
                }
                JsonObject json = new JsonObject();
                json.addProperty("event","onError" );
                MainActivity.jsEvent(AdCode.BannerExpressAd,json.toString());
            }

            @Override
            public void onBannerAdLoad(final TTBannerAd ad) {
                if (ad == null) {
                    return;
                }
                if(bannerView!=null){
                    mBannerContainer.removeView(bannerView);
                    bannerView = null;
                }
                bannerView = ad.getBannerView();
                if (bannerView == null) {
                    return;
                }
                Rect rect = new Rect();
                getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
                FrameLayout.LayoutParams params =  new FrameLayout.LayoutParams(rect.width(), (int)((((double)height)/((double) width))*rect.width()));
                if(!isTop){
                    params.gravity = Gravity.BOTTOM;
                }else{
                    params.gravity = Gravity.TOP;
                }
                bannerView.setLayoutParams(params);

                //设置轮播的时间间隔  间隔在30s到120秒之间的值，不设置默认不轮播
                ad.setSlideIntervalTime(30 * 1000);
                mBannerContainer.addView(bannerView);
                //设置广告互动监听回调
                ad.setBannerInteractionListener(new TTBannerAd.AdInteractionListener() {
                    @Override
                    public void onAdClicked(View view, int type) {
                        TToast.show(MainActivity.this, "广告被点击");
                        JsonObject json = new JsonObject();
                        json.addProperty("event","onAdClicked" );
                        MainActivity.jsEvent(AdCode.BannerExpressAd,json.toString());
                    }

                    @Override
                    public void onAdShow(View view, int type) {
                        TToast.show(MainActivity.this, "广告展示");
                        JsonObject json = new JsonObject();
                        json.addProperty("event","onAdShow" );
                        MainActivity.jsEvent(AdCode.BannerExpressAd,json.toString());
                    }
                });
                //在banner中显示网盟提供的dislike icon，有助于广告投放精准度提升
                ad.setShowDislikeIcon(new TTAdDislike.DislikeInteractionCallback() {
                    @Override
                    public void onSelected(int position, String value) {
                        TToast.show(MainActivity.this, "点击 " + value);
                        JsonObject json = new JsonObject();
                        json.addProperty("event","onSelected" );
                        json.addProperty("value",value);
                        MainActivity.jsEvent(AdCode.BannerExpressAd,json.toString());
                        //用户选择不喜欢原因后，移除广告展示
//                        mBannerContainer.removeAllViews();
                        if(bannerView!=null){
                            mBannerContainer.removeView(bannerView);
                            bannerView = null;
                        }
                    }

                    @Override
                    public void onCancel() {
                        TToast.show(MainActivity.this, "点击取消 ");
                        JsonObject json = new JsonObject();
                        json.addProperty("event","onCancel" );
                        MainActivity.jsEvent(AdCode.BannerExpressAd,json.toString());
                    }
                });


            }
        });
    }
    private void loadInteractionAd(String codeId ,final int width, final int height) {
        //step4:创建插屏广告请求参数AdSlot,具体参数含义参考文档
        AdSlot adSlot = new AdSlot.Builder()
                .setCodeId(codeId)
                .setSupportDeepLink(true)
                .setImageAcceptedSize(width, height) //根据广告平台选择的尺寸，传入同比例尺寸
                .build();
        //step5:请求广告，调用插屏广告异步请求接口
        mTTAdNative.loadInteractionAd(adSlot, new TTAdNative.InteractionAdListener() {
            @Override
            public void onError(int code, String message) {
                TToast.show(getApplicationContext(), "code: " + code + "  message: " + message);
                JsonObject json = new JsonObject();
                json.addProperty("event","onError" );
                MainActivity.jsEvent(AdCode.InteractionAd,json.toString());
            }

            @Override
            public void onInteractionAdLoad(TTInteractionAd ttInteractionAd) {
                TToast.show(getApplicationContext(), "type:  " + ttInteractionAd.getInteractionType());
                ttInteractionAd.setAdInteractionListener(new TTInteractionAd.AdInteractionListener() {
                    @Override
                    public void onAdClicked() {
                        Log.d(TAG, "被点击");
                        TToast.show(MainActivity.this, "广告被点击");
                        JsonObject json = new JsonObject();
                        json.addProperty("event","onAdClicked" );
                        MainActivity.jsEvent(AdCode.InteractionAd,json.toString());
                    }

                    @Override
                    public void onAdShow() {
                        Log.d(TAG, "被展示");
                        TToast.show(MainActivity.this, "广告被展示");
                        JsonObject json = new JsonObject();
                        json.addProperty("event","onAdShow" );
                        MainActivity.jsEvent(AdCode.InteractionAd,json.toString());
                    }

                    @Override
                    public void onAdDismiss() {
                        Log.d(TAG, "插屏广告消失");
                        TToast.show(MainActivity.this, "广告消失");
                        JsonObject json = new JsonObject();
                        json.addProperty("event","onAdDismiss" );
                        MainActivity.jsEvent(AdCode.InteractionAd,json.toString());
                    }
                });
                //弹出插屏广告
                ttInteractionAd.showInteractionAd(MainActivity.this);
            }
        });
    }
}
