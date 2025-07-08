package com.sakurapainting.mediaprogramandroid;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 主Activity - Android 4.4兼容版本
 * 显示MQTT连接状态和基本控制界面
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
        
        try {
            setContentView(R.layout.activity_main);
            Log.i(TAG, "智慧融媒体终端启动 - Android " + Build.VERSION.RELEASE);
            
            initViews();
            
            // 获取MQTT管理器
            mqttManager = MediaApplication.getInstance().getMqttManager();
            
            updateUI();
            
            // 延迟启动连接
            statusTextView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mqttManager != null) {
                        mqttManager.connect();
                    }
                }
            }, 2000);
            
        } catch (Exception e) {
            Log.e(TAG, "onCreate 异常", e);
            Toast.makeText(this, "应用启动异常: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * 初始化视图
     */
    private void initViews() {
        try {
            statusTextView = findViewById(R.id.statusTextView);
            deviceInfoTextView = findViewById(R.id.deviceInfoTextView);
            connectButton = findViewById(R.id.connectButton);
            disconnectButton = findViewById(R.id.disconnectButton);
            
            if (connectButton != null) {
                connectButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            if (mqttManager != null) {
                                mqttManager.connect();
                                updateUI();
                                Toast.makeText(MainActivity.this, "正在连接MQTT服务器...", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "连接异常", e);
                            Toast.makeText(MainActivity.this, "连接失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
            
            if (disconnectButton != null) {
                disconnectButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            if (mqttManager != null) {
                                mqttManager.disconnect();
                                updateUI();
                                Toast.makeText(MainActivity.this, "已断开MQTT连接", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "断开连接异常", e);
                        }
                    }
                });
            }
            
            // 定期更新UI
            startUIUpdateTimer();
            
        } catch (Exception e) {
            Log.e(TAG, "初始化视图异常", e);
        }
    }
    
    /**
     * 启动UI更新定时器
     */
    private void startUIUpdateTimer() {
        if (statusTextView != null) {
            statusTextView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        updateUI();
                        if (statusTextView != null) {
                            statusTextView.postDelayed(this, 3000); // 每3秒更新一次
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "UI更新异常", e);
                    }
                }
            }, 3000);
        }
    }
    
    /**
     * 更新UI显示
     */
    private void updateUI() {
        try {
            if (mqttManager == null) {
                Log.w(TAG, "MqttManager为空，跳过UI更新");
                return;
            }
            
            // 更新连接状态
            boolean isConnected = mqttManager.isConnected();
            String statusText = String.format("MQTT连接状态: %s\n设备ID: %s\n客户端ID: %s\nAndroid版本: %s",
                    isConnected ? "✅ 已连接" : "❌ 未连接",
                    mqttManager.getDeviceId() != null ? mqttManager.getDeviceId() : "未知",
                    mqttManager.getClientId() != null ? mqttManager.getClientId() : "未知",
                    Build.VERSION.RELEASE);
            
            if (statusTextView != null) {
                statusTextView.setText(statusText);
            }
            
            // 更新按钮状态
            if (connectButton != null) {
                connectButton.setEnabled(!isConnected);
            }
            if (disconnectButton != null) {
                disconnectButton.setEnabled(isConnected);
            }
            
            // 更新设备信息
            updateDeviceInfo();
            
        } catch (Exception e) {
            Log.e(TAG, "更新UI异常", e);
        }
    }
    
    /**
     * 更新设备信息
     */
    private void updateDeviceInfo() {
        try {
            String deviceInfo = String.format("设备信息:\n品牌: %s\n型号: %s\nAPI级别: %d\n应用状态: 运行中",
                    Build.BRAND != null ? Build.BRAND : "未知",
                    Build.MODEL != null ? Build.MODEL : "未知",
                    Build.VERSION.SDK_INT);
            
            if (deviceInfoTextView != null) {
                deviceInfoTextView.setText(deviceInfo);
            }
        } catch (Exception e) {
            Log.w(TAG, "更新设备信息失败", e);
            if (deviceInfoTextView != null) {
                deviceInfoTextView.setText("设备信息获取失败");
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        try {
            // 断开MQTT连接
            if (mqttManager != null) {
                mqttManager.disconnect();
            }
            
            Log.i(TAG, "主Activity已销毁");
        } catch (Exception e) {
            Log.e(TAG, "销毁异常", e);
        }
        
        super.onDestroy();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        try {
            updateUI();
        } catch (Exception e) {
            Log.e(TAG, "onResume异常", e);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        try {
            Log.d(TAG, "Activity暂停");
        } catch (Exception e) {
            Log.e(TAG, "onPause异常", e);
        }
    }
}
