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
    private SimpleMqttManager simpleMqttManager; // 调试用的简化版本
    
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
    
    public Object getMqttManager() {
        try {
            // 尝试使用真实的MQTT管理器
            if (mqttManager == null) {
                Log.i("MediaApplication", "创建真实的MqttManager");
                mqttManager = new MqttManager(this);
            }
            return mqttManager;
        } catch (Exception e) {
            Log.e("MediaApplication", "创建真实MqttManager失败，使用简化版本", e);
            // 如果真实的失败了，使用简化版本
            if (simpleMqttManager == null) {
                simpleMqttManager = new SimpleMqttManager(this);
            }
            return simpleMqttManager;
        }
    }
    
    /**
     * 模拟的MQTT管理器类
     */
    public static class MockMqttManager {
        private boolean connected = false;
        
        public void connect() {
            Log.i("MediaApplication", "模拟MQTT连接");
            connected = true;
        }
        
        public void disconnect() {
            Log.i("MediaApplication", "模拟MQTT断开");
            connected = false;
        }
        
        public boolean isConnected() {
            return connected;
        }
        
        public String getDeviceId() {
            return "debug_device_001";
        }
        
        public String getClientId() {
            return "debug_client_001";
        }
    }
}
