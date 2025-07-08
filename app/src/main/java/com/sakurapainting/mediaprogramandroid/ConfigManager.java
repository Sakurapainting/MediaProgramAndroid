package com.sakurapainting.mediaprogramandroid;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 配置管理器
 * 管理应用配置和MQTT连接参数
 */
public class ConfigManager {
    
    private static final String PREFS_NAME = "mqtt_config";
    
    // 配置键
    private static final String KEY_MQTT_SERVER = "mqtt_server";
    private static final String KEY_MQTT_PORT = "mqtt_port";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_CLIENT_ID = "client_id";
    private static final String KEY_AUTO_CONNECT = "auto_connect";
    private static final String KEY_HEARTBEAT_INTERVAL = "heartbeat_interval";
    private static final String KEY_RECONNECT_DELAY = "reconnect_delay";
    
    // 默认值
    private static final String DEFAULT_MQTT_SERVER = "10.0.2.2"; // 模拟器默认主机IP
    private static final int DEFAULT_MQTT_PORT = 1884;
    private static final boolean DEFAULT_AUTO_CONNECT = true;
    private static final int DEFAULT_HEARTBEAT_INTERVAL = 30; // 秒
    private static final int DEFAULT_RECONNECT_DELAY = 5; // 秒
    
    private SharedPreferences prefs;
    
    public ConfigManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    // MQTT服务器地址
    public String getMqttServer() {
        return prefs.getString(KEY_MQTT_SERVER, DEFAULT_MQTT_SERVER);
    }
    
    public void setMqttServer(String server) {
        prefs.edit().putString(KEY_MQTT_SERVER, server).apply();
    }
    
    // MQTT端口
    public int getMqttPort() {
        return prefs.getInt(KEY_MQTT_PORT, DEFAULT_MQTT_PORT);
    }
    
    public void setMqttPort(int port) {
        prefs.edit().putInt(KEY_MQTT_PORT, port).apply();
    }
    
    // 完整MQTT URL
    public String getMqttUrl() {
        return String.format("tcp://%s:%d", getMqttServer(), getMqttPort());
    }
    
    // 设备ID
    public String getDeviceId() {
        return prefs.getString(KEY_DEVICE_ID, null);
    }
    
    public void setDeviceId(String deviceId) {
        prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply();
    }
    
    // 客户端ID
    public String getClientId() {
        return prefs.getString(KEY_CLIENT_ID, null);
    }
    
    public void setClientId(String clientId) {
        prefs.edit().putString(KEY_CLIENT_ID, clientId).apply();
    }
    
    // 自动连接
    public boolean isAutoConnect() {
        return prefs.getBoolean(KEY_AUTO_CONNECT, DEFAULT_AUTO_CONNECT);
    }
    
    public void setAutoConnect(boolean autoConnect) {
        prefs.edit().putBoolean(KEY_AUTO_CONNECT, autoConnect).apply();
    }
    
    // 心跳间隔
    public int getHeartbeatInterval() {
        return prefs.getInt(KEY_HEARTBEAT_INTERVAL, DEFAULT_HEARTBEAT_INTERVAL);
    }
    
    public void setHeartbeatInterval(int interval) {
        prefs.edit().putInt(KEY_HEARTBEAT_INTERVAL, interval).apply();
    }
    
    // 重连延迟
    public int getReconnectDelay() {
        return prefs.getInt(KEY_RECONNECT_DELAY, DEFAULT_RECONNECT_DELAY);
    }
    
    public void setReconnectDelay(int delay) {
        prefs.edit().putInt(KEY_RECONNECT_DELAY, delay).apply();
    }
    
    /**
     * 重置所有配置为默认值
     */
    public void resetToDefaults() {
        prefs.edit().clear().apply();
    }
    
    /**
     * 检查配置是否完整
     */
    public boolean isConfigComplete() {
        return getDeviceId() != null && getClientId() != null;
    }
    
    /**
     * 获取配置摘要信息
     */
    public String getConfigSummary() {
        return String.format("服务器: %s:%d\n设备ID: %s\n客户端ID: %s\n自动连接: %s\n心跳间隔: %d秒",
                getMqttServer(),
                getMqttPort(),
                getDeviceId() != null ? getDeviceId() : "未设置",
                getClientId() != null ? getClientId() : "未设置",
                isAutoConnect() ? "是" : "否",
                getHeartbeatInterval());
    }
}
