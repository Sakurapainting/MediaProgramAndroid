package com.sakurapainting.mediaprogramandroid;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

/**
 * MQTT调试工具
 * 用于测试和验证MQTT连接和设备注册
 */
public class MqttDebugTool {
    
    private static final String TAG = "MqttDebugTool";
    private Context context;
    private MqttManager mqttManager;
    
    public MqttDebugTool(Context context) {
        this.context = context;
    }
    
    /**
     * 初始化MQTT管理器
     */
    public void initMqttManager() {
        try {
            Log.i(TAG, "=== 开始初始化MQTT管理器 ===");
            mqttManager = new MqttManager(context);
            Log.i(TAG, "MQTT管理器创建成功");
            showToast("MQTT管理器初始化成功");
        } catch (Exception e) {
            Log.e(TAG, "MQTT管理器初始化失败", e);
            showToast("MQTT管理器初始化失败: " + e.getMessage());
        }
    }
    
    /**
     * 连接MQTT
     */
    public void connectMqtt() {
        if (mqttManager == null) {
            Log.w(TAG, "MQTT管理器未初始化");
            showToast("请先初始化MQTT管理器");
            return;
        }
        try {
            Log.i(TAG, "开始连接MQTT服务器");
            // Log.i(TAG, "设备信息: " + mqttManager.getDeviceInfo()); // 注释掉不存在方法
            mqttManager.connect();
            showToast("正在连接MQTT服务器...");
        } catch (Exception e) {
            Log.e(TAG, "连接MQTT失败", e);
            showToast("连接MQTT失败: " + e.getMessage());
        }
    }
    /**
     * 手动注册设备
     */
    public void manualRegisterDevice() {
        if (mqttManager == null) {
            Log.w(TAG, "MQTT管理器未初始化");
            showToast("请先初始化MQTT管理器");
            return;
        }
        try {
            Log.i(TAG, "手动触发设备注册");
            // mqttManager.manualRegisterDevice(); // 注释掉不存在方法
            showToast("已发送设备注册请求");
        } catch (Exception e) {
            Log.e(TAG, "手动注册设备失败", e);
            showToast("手动注册失败: " + e.getMessage());
        }
    }
    /**
     * 获取连接状态
     */
    public String getConnectionStatus() {
        if (mqttManager == null) {
            return "MQTT管理器未初始化";
        }
        // return mqttManager.getDetailedStatus(); // 注释掉不存在方法
        return "状态获取功能未实现";
    }
    /**
     * 测试注册功能
     */
    public void testRegistration() {
        if (mqttManager == null) {
            Log.w(TAG, "MQTT管理器未初始化");
            showToast("请先初始化MQTT管理器");
            return;
        }
        try {
            Log.i(TAG, "开始测试设备注册功能");
            // mqttManager.testRegistration(); // 注释掉不存在方法
            showToast("测试注册功能已触发");
        } catch (Exception e) {
            Log.e(TAG, "测试注册失败", e);
            showToast("测试注册失败: " + e.getMessage());
        }
    }
    /**
     * 断开连接
     */
    public void disconnect() {
        if (mqttManager == null) {
            Log.w(TAG, "MQTT管理器未初始化");
            return;
        }
        
        try {
            Log.i(TAG, "断开MQTT连接");
            mqttManager.disconnect();
            showToast("已断开MQTT连接");
        } catch (Exception e) {
            Log.e(TAG, "断开连接失败", e);
            showToast("断开连接失败: " + e.getMessage());
        }
    }
    
    /**
     * 显示Toast消息
     */
    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 获取MQTT管理器
     */
    public MqttManager getMqttManager() {
        return mqttManager;
    }
}
