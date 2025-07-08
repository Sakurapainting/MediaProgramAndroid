package com.sakurapainting.mediaprogramandroid;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 主Activity - 最简化的Android 4.4兼容版本
 * 为了排查崩溃问题，极度简化启动逻辑
 */
public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "MainActivity";
    
    private TextView statusTextView;
    private TextView deviceInfoTextView;
    private Button connectButton;
    private Button disconnectButton;
    
    private MqttManager mqttManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.i(TAG, "=== MainActivity onCreate 开始 ===");
        
        try {
            // 步骤1：设置布局
            Log.i(TAG, "步骤1：设置布局");
            setContentView(R.layout.activity_main);
            Log.i(TAG, "布局设置成功");
            
            // 步骤2：显示基本信息
            Log.i(TAG, "步骤2：显示基本信息");
            Log.i(TAG, "智慧融媒体终端启动 - Android " + Build.VERSION.RELEASE);
            Log.i(TAG, "API Level: " + Build.VERSION.SDK_INT);
            
            // 步骤3：初始化基本视图
            Log.i(TAG, "步骤3：初始化基本视图");
            initBasicViews();
            Log.i(TAG, "基本视图初始化成功");
            
            // 步骤4：显示启动状态
            if (statusTextView != null) {
                statusTextView.setText("应用启动成功\nAndroid " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
            }
            
            if (deviceInfoTextView != null) {
                String deviceInfo = "设备信息:\n" +
                        "品牌: " + Build.BRAND + "\n" +
                        "型号: " + Build.MODEL + "\n" +
                        "制造商: " + Build.MANUFACTURER;
                deviceInfoTextView.setText(deviceInfo);
            }
            
            // 步骤5：延迟初始化MQTT（非关键）
            Log.i(TAG, "步骤5：计划延迟初始化MQTT");
            scheduleDelayedMqttInit();
            
            Log.i(TAG, "=== MainActivity onCreate 完成 ===");
            
        } catch (Exception e) {
            Log.e(TAG, "onCreate 发生异常", e);
            
            // 尝试显示错误信息
            try {
                setTitle("启动失败");
                Toast.makeText(this, "启动异常: " + e.getMessage(), Toast.LENGTH_LONG).show();
                
                // 显示一个简单的错误界面
                TextView errorView = new TextView(this);
                errorView.setText("应用启动失败\n\n错误: " + e.getMessage() + 
                                "\n\nAndroid版本: " + Build.VERSION.RELEASE +
                                "\nAPI级别: " + Build.VERSION.SDK_INT);
                errorView.setTextSize(14);
                errorView.setPadding(20, 20, 20, 20);
                setContentView(errorView);
                
            } catch (Exception e2) {
                Log.e(TAG, "连错误界面都设置失败了", e2);
            }
        }
    }
    
    /**
     * 初始化基本视图（不涉及复杂逻辑）
     */
    private void initBasicViews() {
        try {
            Log.i(TAG, "查找视图组件...");
            
            statusTextView = findViewById(R.id.statusTextView);
            if (statusTextView != null) {
                Log.i(TAG, "statusTextView 找到");
            } else {
                Log.w(TAG, "statusTextView 未找到");
            }
            
            deviceInfoTextView = findViewById(R.id.deviceInfoTextView);
            if (deviceInfoTextView != null) {
                Log.i(TAG, "deviceInfoTextView 找到");
            } else {
                Log.w(TAG, "deviceInfoTextView 未找到");
            }
            
            connectButton = findViewById(R.id.connectButton);
            if (connectButton != null) {
                Log.i(TAG, "connectButton 找到");
                connectButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onConnectButtonClick();
                    }
                });
            } else {
                Log.w(TAG, "connectButton 未找到");
            }
            
            disconnectButton = findViewById(R.id.disconnectButton);
            if (disconnectButton != null) {
                Log.i(TAG, "disconnectButton 找到");
                disconnectButton.setEnabled(false);
                disconnectButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onDisconnectButtonClick();
                    }
                });
            } else {
                Log.w(TAG, "disconnectButton 未找到");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "初始化基本视图异常", e);
            throw e; // 重新抛出异常
        }
    }
    
    /**
     * 延迟初始化MQTT管理器
     */
    private void scheduleDelayedMqttInit() {
        android.os.Handler handler = new android.os.Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.i(TAG, "开始延迟初始化MQTT管理器");
                    
                    MediaApplication app = MediaApplication.getInstance();
                    if (app != null) {
                        mqttManager = app.getMqttManager();
                        if (mqttManager != null) {
                            Log.i(TAG, "MQTT管理器获取成功");
                            updateConnectionUI();
                        } else {
                            Log.w(TAG, "MQTT管理器获取失败");
                            showMqttError("MQTT管理器初始化失败");
                        }
                    } else {
                        Log.w(TAG, "MediaApplication实例为空");
                        showMqttError("应用实例获取失败");
                    }
                    
                } catch (Exception e) {
                    Log.e(TAG, "延迟初始化MQTT异常", e);
                    showMqttError("MQTT初始化异常: " + e.getMessage());
                }
            }
        }, 2000); // 延迟2秒
    }
    
    /**
     * 连接按钮点击
     */
    private void onConnectButtonClick() {
        try {
            Log.i(TAG, "连接按钮被点击");
            
            if (mqttManager != null) {
                mqttManager.connect();
                Toast.makeText(this, "正在连接MQTT服务器...", Toast.LENGTH_SHORT).show();
                updateConnectionUI();
            } else {
                Toast.makeText(this, "MQTT管理器未初始化", Toast.LENGTH_SHORT).show();
                scheduleDelayedMqttInit(); // 重试初始化
            }
            
        } catch (Exception e) {
            Log.e(TAG, "连接异常", e);
            Toast.makeText(this, "连接失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 断开连接按钮点击
     */
    private void onDisconnectButtonClick() {
        try {
            Log.i(TAG, "断开连接按钮被点击");
            
            if (mqttManager != null) {
                mqttManager.disconnect();
                Toast.makeText(this, "已断开MQTT连接", Toast.LENGTH_SHORT).show();
                updateConnectionUI();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "断开连接异常", e);
            Toast.makeText(this, "断开连接失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 更新连接状态UI
     */
    private void updateConnectionUI() {
        try {
            if (mqttManager == null) {
                return;
            }
            
            boolean isConnected = mqttManager.isConnected();
            String statusText = String.format("MQTT连接状态: %s\n设备ID: %s\n客户端ID: %s",
                    isConnected ? "✅ 已连接" : "❌ 未连接",
                    mqttManager.getDeviceId() != null ? mqttManager.getDeviceId() : "未知",
                    mqttManager.getClientId() != null ? mqttManager.getClientId() : "未知");
            
            if (statusTextView != null) {
                statusTextView.setText(statusText);
            }
            
            if (connectButton != null) {
                connectButton.setEnabled(!isConnected);
            }
            if (disconnectButton != null) {
                disconnectButton.setEnabled(isConnected);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "更新连接UI异常", e);
        }
    }
    
    /**
     * 显示MQTT错误信息
     */
    private void showMqttError(String message) {
        try {
            if (statusTextView != null) {
                statusTextView.setText("MQTT状态: 初始化失败\n" + message);
            }
            
            if (connectButton != null) {
                connectButton.setText("重试初始化");
                connectButton.setEnabled(true);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "显示MQTT错误异常", e);
        }
    }
    
    @Override
    protected void onDestroy() {
        try {
            Log.i(TAG, "MainActivity onDestroy");
            
            if (mqttManager != null) {
                mqttManager.disconnect();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "onDestroy异常", e);
        }
        
        super.onDestroy();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        try {
            Log.i(TAG, "MainActivity onResume");
            updateConnectionUI();
        } catch (Exception e) {
            Log.e(TAG, "onResume异常", e);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        try {
            Log.d(TAG, "MainActivity onPause");
        } catch (Exception e) {
            Log.e(TAG, "onPause异常", e);
        }
    }
}
