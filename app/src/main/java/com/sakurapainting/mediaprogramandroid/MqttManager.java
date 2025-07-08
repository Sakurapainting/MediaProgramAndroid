package com.sakurapainting.mediaprogramandroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * MQTT连接管理器
 * 负责与云平台的MQTT通信
 */
public class MqttManager {
    
    private static final String TAG = "MqttManager";
    
    // MQTT配置
    private ConfigManager configManager;
    
    // 主题定义
    private static final String TOPIC_REGISTER = "device/register";
    private static final String TOPIC_HEARTBEAT = "device/heartbeat";
    private static final String TOPIC_STATUS = "device/status";
    private static final String TOPIC_DATA = "device/data";
    private static final String TOPIC_CONTENT_RESPONSE = "device/content_response";
    private static final String TOPIC_CONTENT = "device/%s/content";
    private static final String TOPIC_COMMANDS = "device/%s/commands";
    private static final String TOPIC_BROADCAST = "broadcast/all";
    
    private Context context;
    private MqttAndroidClient mqttClient;
    private String deviceId;
    private String clientId;
    private boolean isConnected = false;
    private Handler heartbeatHandler;
    private Runnable heartbeatRunnable;
    private DeviceStatusManager statusManager;
    private ContentManager contentManager;
    
    public MqttManager(Context context) {
        this.context = context;
        this.configManager = new ConfigManager(context);
        this.statusManager = new DeviceStatusManager(context);
        this.contentManager = new ContentManager(context);
        initializeDevice();
        setupHeartbeat();
    }
    
    /**
     * 初始化设备信息 - Android 4.4兼容版本
     */
    private void initializeDevice() {
        try {
            // 生成或获取设备ID
            deviceId = configManager.getDeviceId();
            if (deviceId == null) {
                String androidId = null;
                try {
                    androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
                } catch (Exception e) {
                    Log.w(TAG, "无法获取ANDROID_ID", e);
                }
                
                if (androidId != null && androidId.length() > 0 && !"9774d56d682e549c".equals(androidId)) {
                    // 正常的ANDROID_ID
                    deviceId = "android_" + androidId.substring(Math.max(0, androidId.length() - 8));
                } else {
                    // 使用时间戳作为后备方案
                    long timestamp = System.currentTimeMillis();
                    deviceId = "android_" + String.valueOf(timestamp).substring(5);
                    Log.i(TAG, "使用时间戳生成设备ID");
                }
                configManager.setDeviceId(deviceId);
            }
            
            // 生成客户端ID
            clientId = configManager.getClientId();
            if (clientId == null) {
                clientId = "mqtt_client_" + deviceId.substring("android_".length());
                configManager.setClientId(clientId);
            }
            
            Log.i(TAG, "Device ID: " + deviceId + ", Client ID: " + clientId);
        } catch (Exception e) {
            Log.e(TAG, "初始化设备信息异常", e);
            // 使用默认值
            deviceId = "android_default";
            clientId = "mqtt_client_default";
        }
    }
    
    /**
     * 连接到MQTT服务器
     */
    public void connect() {
        if (mqttClient != null && mqttClient.isConnected()) {
            Log.w(TAG, "MQTT already connected");
            return;
        }
        
        if (!isNetworkAvailable()) {
            Log.w(TAG, "Network not available");
            return;
        }
        
        try {
            // 创建MQTT客户端
            String mqttUrl = configManager.getMqttUrl();
            mqttClient = new MqttAndroidClient(context, mqttUrl, clientId);
            mqttClient.setCallback(new MqttCallbackHandler());
            
            Log.i(TAG, "连接到MQTT服务器: " + mqttUrl);
            
            // 连接选项
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(false);
            options.setConnectionTimeout(30);
            options.setKeepAliveInterval(60);
            
            // 连接
            mqttClient.connect(options, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "MQTT连接成功");
                    isConnected = true;
                    subscribeToTopics();
                    registerDevice();
                    startHeartbeat();
                    updateStatus("online");
                }
                
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "MQTT连接失败", exception);
                    isConnected = false;
                    // 延迟重试
                    int delay = configManager.getReconnectDelay() * 1000;
                    new Handler(Looper.getMainLooper()).postDelayed(() -> connect(), delay);
                }
            });
            
        } catch (MqttException e) {
            Log.e(TAG, "创建MQTT客户端失败", e);
        }
    }
    
    /**
     * 订阅相关主题
     */
    private void subscribeToTopics() {
        try {
            // 订阅内容推送主题
            String contentTopic = String.format(TOPIC_CONTENT, clientId);
            mqttClient.subscribe(contentTopic, 1);
            Log.i(TAG, "订阅主题: " + contentTopic);
            
            // 订阅命令主题
            String commandTopic = String.format(TOPIC_COMMANDS, clientId);
            mqttClient.subscribe(commandTopic, 1);
            Log.i(TAG, "订阅主题: " + commandTopic);
            
            // 订阅广播主题
            mqttClient.subscribe(TOPIC_BROADCAST, 1);
            Log.i(TAG, "订阅主题: " + TOPIC_BROADCAST);
            
        } catch (MqttException e) {
            Log.e(TAG, "订阅主题失败", e);
        }
    }
    
    /**
     * 注册设备
     */
    private void registerDevice() {
        try {
            JSONObject message = new JSONObject();
            message.put("type", "register");
            message.put("deviceId", deviceId);
            message.put("clientId", clientId);
            message.put("timestamp", System.currentTimeMillis());
            
            JSONObject data = new JSONObject();
            data.put("deviceId", deviceId);
            data.put("name", "安卓屏幕终端_" + deviceId.substring("android_".length()));
            data.put("type", "android_screen");
            
            // 位置信息
            JSONObject location = new JSONObject();
            location.put("name", "移动显示终端");
            location.put("address", "位置待设定");
            JSONObject coordinates = new JSONObject();
            coordinates.put("latitude", 0.0);
            coordinates.put("longitude", 0.0);
            location.put("coordinates", coordinates);
            data.put("location", location);
            
            // 设备规格
            JSONObject specs = statusManager.getDeviceSpecifications();
            data.put("specifications", specs);
            
            data.put("version", "1.0.0");
            data.put("capabilities", new String[]{"display", "audio", "touch"});
            
            message.put("data", data);
            
            publishMessage(TOPIC_REGISTER, message.toString());
            Log.i(TAG, "设备注册消息已发送");
            
        } catch (JSONException e) {
            Log.e(TAG, "创建注册消息失败", e);
        }
    }
    
    /**
     * 发送心跳消息
     */
    private void sendHeartbeat() {
        if (!isConnected) return;
        
        try {
            JSONObject message = new JSONObject();
            message.put("type", "heartbeat");
            message.put("deviceId", deviceId);
            message.put("clientId", clientId);
            message.put("timestamp", System.currentTimeMillis());
            
            JSONObject data = statusManager.getSystemStatus();
            message.put("data", data);
            
            publishMessage(TOPIC_HEARTBEAT, message.toString());
            Log.d(TAG, "心跳消息已发送");
            
        } catch (JSONException e) {
            Log.e(TAG, "创建心跳消息失败", e);
        }
    }
    
    /**
     * 更新设备状态
     */
    private void updateStatus(String status) {
        try {
            JSONObject message = new JSONObject();
            message.put("type", "status");
            message.put("deviceId", deviceId);
            message.put("clientId", clientId);
            message.put("timestamp", System.currentTimeMillis());
            
            JSONObject data = new JSONObject();
            data.put("status", status);
            data.put("deviceInfo", statusManager.getDeviceInfo());
            message.put("data", data);
            
            publishMessage(TOPIC_STATUS, message.toString());
            Log.i(TAG, "状态更新消息已发送: " + status);
            
        } catch (JSONException e) {
            Log.e(TAG, "创建状态消息失败", e);
        }
    }
    
    /**
     * 发布消息
     */
    private void publishMessage(String topic, String message) {
        if (!isConnected || mqttClient == null) {
            Log.w(TAG, "MQTT未连接，无法发送消息");
            return;
        }
        
        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(1);
            mqttMessage.setRetained(false);
            
            mqttClient.publish(topic, mqttMessage);
            Log.d(TAG, "消息已发布到 " + topic + ": " + message);
            
        } catch (MqttException e) {
            Log.e(TAG, "发布消息失败", e);
        }
    }
    
    /**
     * 设置心跳
     */
    private void setupHeartbeat() {
        heartbeatHandler = new Handler(Looper.getMainLooper());
        heartbeatRunnable = new Runnable() {
            @Override
            public void run() {
                sendHeartbeat();
                int interval = configManager.getHeartbeatInterval() * 1000; // 转换为毫秒
                heartbeatHandler.postDelayed(this, interval);
            }
        };
    }
    
    /**
     * 开始心跳
     */
    private void startHeartbeat() {
        stopHeartbeat();
        heartbeatHandler.post(heartbeatRunnable);
    }
    
    /**
     * 停止心跳
     */
    private void stopHeartbeat() {
        if (heartbeatHandler != null && heartbeatRunnable != null) {
            heartbeatHandler.removeCallbacks(heartbeatRunnable);
        }
    }
    
    /**
     * 检查网络连接
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
    
    /**
     * 断开连接
     */
    public void disconnect() {
        stopHeartbeat();
        
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                updateStatus("offline");
                mqttClient.disconnect();
                Log.i(TAG, "MQTT已断开连接");
            } catch (MqttException e) {
                Log.e(TAG, "断开MQTT连接失败", e);
            }
        }
        
        isConnected = false;
    }
    
    /**
     * MQTT回调处理器
     */
    private class MqttCallbackHandler implements MqttCallback {
        
        @Override
        public void connectionLost(Throwable cause) {
            Log.w(TAG, "MQTT连接丢失", cause);
            isConnected = false;
            stopHeartbeat();
            
            // 延迟重连
            int delay = configManager.getReconnectDelay() * 1000;
            new Handler(Looper.getMainLooper()).postDelayed(() -> connect(), delay);
        }
        
        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            String payload = new String(message.getPayload());
            Log.i(TAG, "收到消息 - 主题: " + topic + ", 内容: " + payload);
            
            try {
                JSONObject jsonMessage = new JSONObject(payload);
                String type = jsonMessage.optString("type");
                
                if (topic.endsWith("/content")) {
                    // 处理内容推送
                    handleContentPush(jsonMessage);
                } else if (topic.endsWith("/commands")) {
                    // 处理命令
                    handleCommand(jsonMessage);
                } else if (topic.equals(TOPIC_BROADCAST)) {
                    // 处理广播消息
                    handleBroadcast(jsonMessage);
                }
                
            } catch (JSONException e) {
                Log.e(TAG, "解析消息失败", e);
            }
        }
        
        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            Log.d(TAG, "消息发送完成");
        }
    }
    
    /**
     * 处理内容推送
     */
    private void handleContentPush(JSONObject message) {
        Log.i(TAG, "处理内容推送");
        contentManager.handleContentPush(message, new ContentManager.ContentCallback() {
            @Override
            public void onResult(String contentId, String status, String error) {
                sendContentResponse(contentId, status, error);
            }
        });
    }
    
    /**
     * 处理命令
     */
    private void handleCommand(JSONObject message) {
        try {
            JSONObject data = message.getJSONObject("data");
            String command = data.getString("command");
            
            Log.i(TAG, "处理命令: " + command);
            
            switch (command) {
                case "screenshot":
                    handleScreenshotCommand(data);
                    break;
                case "restart":
                    handleRestartCommand();
                    break;
                case "get_status":
                    updateStatus("online");
                    break;
                default:
                    Log.w(TAG, "未知命令: " + command);
            }
            
        } catch (JSONException e) {
            Log.e(TAG, "处理命令失败", e);
        }
    }
    
    /**
     * 处理广播消息
     */
    private void handleBroadcast(JSONObject message) {
        try {
            JSONObject data = message.getJSONObject("data");
            String broadcastMessage = data.getString("message");
            String level = data.optString("level", "info");
            
            Log.i(TAG, "收到广播消息: " + broadcastMessage + " (级别: " + level + ")");
            
            // 在主活动中显示广播消息（如果需要）
            // 这里可以发送广播Intent给MainActivity
            
        } catch (JSONException e) {
            Log.e(TAG, "处理广播消息失败", e);
        }
    }
    
    /**
     * 处理截图命令
     */
    private void handleScreenshotCommand(JSONObject params) {
        // 这里实现截图功能
        // 由于Android 4.4的限制，需要root权限或特殊方法
        Log.i(TAG, "截图功能需要特殊权限，当前版本暂不支持");
    }
    
    /**
     * 处理重启命令
     */
    private void handleRestartCommand() {
        Log.i(TAG, "收到重启命令");
        // 重启应用
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            android.os.Process.killProcess(android.os.Process.myPid());
        }, 1000);
    }
    
    /**
     * 发送内容响应
     */
    private void sendContentResponse(String contentId, String status, String error) {
        try {
            JSONObject message = new JSONObject();
            message.put("type", "content_response");
            message.put("deviceId", deviceId);
            message.put("clientId", clientId);
            message.put("timestamp", System.currentTimeMillis());
            
            JSONObject data = new JSONObject();
            data.put("contentId", contentId);
            data.put("status", status);
            if (error != null) {
                data.put("error", error);
            } else {
                data.put("error", JSONObject.NULL);
            }
            message.put("data", data);
            
            publishMessage(TOPIC_CONTENT_RESPONSE, message.toString());
            Log.i(TAG, "内容响应已发送: " + status);
            
        } catch (JSONException e) {
            Log.e(TAG, "创建内容响应失败", e);
        }
    }
    
    // Getter方法
    public boolean isConnected() {
        return isConnected && mqttClient != null && mqttClient.isConnected();
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public String getClientId() {
        return clientId;
    }
}
