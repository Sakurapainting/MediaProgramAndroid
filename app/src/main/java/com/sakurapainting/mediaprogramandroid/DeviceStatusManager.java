package com.sakurapainting.mediaprogramandroid;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * 设备状态管理器
 * 负责获取设备信息和系统状态
 */
public class DeviceStatusManager {
    
    private static final String TAG = "DeviceStatusManager";
    
    private Context context;
    
    public DeviceStatusManager(Context context) {
        this.context = context;
    }
    
    /**
     * 获取设备规格信息
     */
    public JSONObject getDeviceSpecifications() {
        JSONObject specs = new JSONObject();
        
        try {
            // 屏幕分辨率
            DisplayMetrics displayMetrics = new DisplayMetrics();
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
            
            String resolution = displayMetrics.widthPixels + "x" + displayMetrics.heightPixels;
            specs.put("resolution", resolution);
            
            // 屏幕密度
            specs.put("density", displayMetrics.density);
            specs.put("densityDpi", displayMetrics.densityDpi);
            
            // 屏幕尺寸计算（英寸）
            double x = Math.pow(displayMetrics.widthPixels / displayMetrics.xdpi, 2);
            double y = Math.pow(displayMetrics.heightPixels / displayMetrics.ydpi, 2);
            double screenInches = Math.sqrt(x + y);
            specs.put("size", String.format("%.1f寸", screenInches));
            
            // 屏幕方向
            int orientation = context.getResources().getConfiguration().orientation;
            specs.put("orientation", orientation == 1 ? "vertical" : "horizontal");
            
            // 设备信息
            specs.put("brand", Build.BRAND);
            specs.put("model", Build.MODEL);
            specs.put("manufacturer", Build.MANUFACTURER);
            specs.put("androidVersion", Build.VERSION.RELEASE);
            specs.put("apiLevel", Build.VERSION.SDK_INT);
            
            // 内存信息 - Android 4.4兼容处理
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);
            
            // Android 4.4 (API 19) 不支持 totalMem 字段，使用兼容方法
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                try {
                    // 使用反射来安全访问totalMem字段
                    java.lang.reflect.Field totalMemField = memoryInfo.getClass().getField("totalMem");
                    long totalMemoryMB = totalMemField.getLong(memoryInfo) / (1024 * 1024);
                    specs.put("totalMemoryMB", totalMemoryMB);
                } catch (Exception e) {
                    // Android 4.4 fallback - 使用可用内存估算
                    long availMemoryMB = memoryInfo.availMem / (1024 * 1024);
                    specs.put("totalMemoryMB", availMemoryMB * 3); // 粗略估算总内存为可用内存的3倍
                    specs.put("note", "totalMem estimated for Android 4.4 compatibility");
                    Log.w(TAG, "使用估算方式获取总内存: " + e.getMessage());
                }
            } else {
                // 更老版本的Android，只记录可用内存
                long availMemoryMB = memoryInfo.availMem / (1024 * 1024);
                specs.put("availableMemoryMB", availMemoryMB);
                specs.put("totalMemoryMB", "unknown_old_android");
            }
            
        } catch (JSONException e) {
            Log.e(TAG, "创建设备规格信息失败", e);
        }
        
        return specs;
    }
    
    /**
     * 获取系统状态信息（用于心跳）
     */
    public JSONObject getSystemStatus() {
        JSONObject status = new JSONObject();
        
        try {
            // 系统运行时间
            status.put("uptime", System.currentTimeMillis());
            
            // 内存使用情况 - Android 4.4兼容处理
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);
            
            long availableMemory = memoryInfo.availMem;
            status.put("availableMemoryMB", availableMemory / (1024 * 1024));
            
            // Android 4.4兼容处理 totalMem
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                try {
                    long totalMemory = memoryInfo.totalMem;
                    double memoryUsage = ((double)(totalMemory - availableMemory) / totalMemory) * 100;
                    status.put("memoryUsage", Math.round(memoryUsage * 10.0) / 10.0);
                    status.put("totalMemoryMB", totalMemory / (1024 * 1024));
                } catch (NoSuchFieldError e) {
                    // Android 4.4 fallback
                    status.put("memoryUsage", "unknown");
                    status.put("totalMemoryMB", "unknown");
                    Log.w(TAG, "totalMem not available on this Android version");
                }
            } else {
                status.put("memoryUsage", "unknown");
                status.put("totalMemoryMB", "unknown");
            }
            
            // CPU使用率（简化版本）
            double cpuUsage = getCpuUsage();
            status.put("cpuUsage", cpuUsage);
            
            // 设备温度（如果可用）
            float temperature = getDeviceTemperature();
            if (temperature > 0) {
                status.put("temperature", temperature);
            }
            
            // 应用版本
            try {
                PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                status.put("appVersion", pInfo.versionName);
                status.put("appVersionCode", pInfo.versionCode);
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "无法获取应用版本信息");
            }
            
            // 屏幕亮度（固定值，实际获取需要特殊权限）
            status.put("brightness", 80);
            
        } catch (JSONException e) {
            Log.e(TAG, "创建系统状态信息失败", e);
        }
        
        return status;
    }
    
    /**
     * 获取设备详细信息
     */
    public JSONObject getDeviceInfo() {
        JSONObject info = new JSONObject();
        
        try {
            // 电池信息（模拟，实际需要BatteryManager）
            info.put("battery", 85);
            
            // 设备温度
            float temp = getDeviceTemperature();
            if (temp > 0) {
                info.put("temperature", temp);
            } else {
                info.put("temperature", 32); // 模拟值
            }
            
            // 屏幕亮度
            info.put("brightness", 80);
            
            // 音量
            info.put("volume", 60);
            
            // 网络状态
            info.put("networkType", getNetworkType());
            
            // 存储空间
            JSONObject storage = getStorageInfo();
            info.put("storage", storage);
            
        } catch (JSONException e) {
            Log.e(TAG, "创建设备信息失败", e);
        }
        
        return info;
    }
    
    /**
     * 获取CPU使用率（简化实现）
     */
    private double getCpuUsage() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/stat"));
            String line = reader.readLine();
            reader.close();
            
            if (line != null) {
                String[] tokens = line.split(" +");
                long idle = Long.parseLong(tokens[4]);
                long total = 0;
                for (int i = 1; i < tokens.length; i++) {
                    total += Long.parseLong(tokens[i]);
                }
                
                // 简化计算，返回一个估计值
                return Math.min(100.0, Math.max(0.0, 100.0 - (idle * 100.0 / total)));
            }
        } catch (IOException | NumberFormatException e) {
            Log.w(TAG, "无法读取CPU使用率");
        }
        
        // 返回模拟值
        return 15.0 + Math.random() * 20.0;
    }
    
    /**
     * 获取设备温度
     */
    private float getDeviceTemperature() {
        try {
            // 尝试读取温度文件（需要root权限或特定设备）
            BufferedReader reader = new BufferedReader(new FileReader("/sys/class/thermal/thermal_zone0/temp"));
            String tempString = reader.readLine();
            reader.close();
            
            if (tempString != null) {
                float temp = Float.parseFloat(tempString) / 1000.0f; // 转换为摄氏度
                return temp;
            }
        } catch (IOException | NumberFormatException e) {
            Log.w(TAG, "无法读取设备温度");
        }
        
        return -1; // 无法获取
    }
    
    /**
     * 获取网络类型
     */
    private String getNetworkType() {
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) 
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
            android.net.NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            
            if (networkInfo != null && networkInfo.isConnected()) {
                if (networkInfo.getType() == android.net.ConnectivityManager.TYPE_WIFI) {
                    return "WIFI";
                } else if (networkInfo.getType() == android.net.ConnectivityManager.TYPE_MOBILE) {
                    return "MOBILE";
                } else {
                    return "OTHER";
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "无法获取网络类型", e);
        }
        
        return "UNKNOWN";
    }
    
    /**
     * 获取存储信息
     */
    private JSONObject getStorageInfo() {
        JSONObject storage = new JSONObject();
        
        try {
            // 内部存储
            android.os.StatFs internal = new android.os.StatFs(android.os.Environment.getDataDirectory().getPath());
            long internalTotal = (long) internal.getBlockCount() * internal.getBlockSize();
            long internalAvailable = (long) internal.getAvailableBlocks() * internal.getBlockSize();
            
            storage.put("internalTotalMB", internalTotal / (1024 * 1024));
            storage.put("internalAvailableMB", internalAvailable / (1024 * 1024));
            
            // 外部存储（如果存在）
            if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
                android.os.StatFs external = new android.os.StatFs(android.os.Environment.getExternalStorageDirectory().getPath());
                long externalTotal = (long) external.getBlockCount() * external.getBlockSize();
                long externalAvailable = (long) external.getAvailableBlocks() * external.getBlockSize();
                
                storage.put("externalTotalMB", externalTotal / (1024 * 1024));
                storage.put("externalAvailableMB", externalAvailable / (1024 * 1024));
            }
            
        } catch (Exception e) {
            Log.w(TAG, "无法获取存储信息", e);
        }
        
        return storage;
    }
}
