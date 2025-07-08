package com.sakurapainting.mediaprogramandroid;

import android.app.Application;
import android.content.Context;
import android.util.Log;

/**
 * 应用程序主类
 * 用于管理全局应用状态和配置
 */
public class MediaApplication extends Application {
    
    private static MediaApplication instance;
    private MqttManager mqttManager;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        try {
            instance = this;
            
            Log.i("MediaApplication", "应用启动 - Android " + android.os.Build.VERSION.RELEASE);
            
            // 延迟初始化MQTT管理器，避免启动时崩溃
            // mqttManager = new MqttManager(this);
            
            Log.i("MediaApplication", "应用基础初始化完成");
        } catch (Exception e) {
            Log.e("MediaApplication", "应用初始化异常", e);
            // 即使出错也要设置instance，避免空指针
            if (instance == null) {
                instance = this;
            }
        }
    }
    
    public static MediaApplication getInstance() {
        return instance;
    }
    
    public static Context getContext() {
        return instance.getApplicationContext();
    }
    
    public MqttManager getMqttManager() {
        if (mqttManager == null) {
            try {
                Log.i("MediaApplication", "延迟初始化MQTT管理器");
                mqttManager = new MqttManager(this);
                Log.i("MediaApplication", "MQTT管理器初始化完成");
            } catch (Exception e) {
                Log.e("MediaApplication", "MQTT管理器初始化失败", e);
                return null;
            }
        }
        return mqttManager;
    }
}
