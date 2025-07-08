package com.sakurapainting.mediaprogramandroid;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 内容管理器
 * 负责处理服务器推送的内容显示
 */
public class ContentManager {
    
    private static final String TAG = "ContentManager";
    
    private Context context;
    private Handler mainHandler;
    
    public ContentManager(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * 内容处理回调接口
     */
    public interface ContentCallback {
        void onResult(String contentId, String status, String error);
    }
    
    /**
     * 处理内容推送
     */
    public void handleContentPush(JSONObject message, ContentCallback callback) {
        try {
            JSONObject data = message.getJSONObject("data");
            String contentId = data.getString("contentId");
            String url = data.getString("url");
            String type = data.getString("type");
            int duration = data.optInt("duration", 10);
            int priority = data.optInt("priority", 1);
            
            Log.i(TAG, String.format("处理内容推送 - ID: %s, 类型: %s, URL: %s", contentId, type, url));
            
            // 验证内容类型
            if (!isSupportedContentType(type)) {
                callback.onResult(contentId, "error", "不支持的内容类型: " + type);
                return;
            }
            
            // 根据内容类型处理
            switch (type.toLowerCase()) {
                case "image":
                    displayImage(contentId, url, duration, callback);
                    break;
                case "video":
                    displayVideo(contentId, url, duration, callback);
                    break;
                case "text":
                    displayText(contentId, data, duration, callback);
                    break;
                case "webpage":
                    displayWebpage(contentId, url, duration, callback);
                    break;
                default:
                    callback.onResult(contentId, "error", "未实现的内容类型: " + type);
            }
            
        } catch (JSONException e) {
            Log.e(TAG, "解析内容推送消息失败", e);
            String contentId = "unknown";
            try {
                contentId = message.getJSONObject("data").getString("contentId");
            } catch (JSONException ignored) {}
            
            callback.onResult(contentId, "error", "消息格式错误");
        }
    }
    
    /**
     * 检查是否支持的内容类型
     */
    private boolean isSupportedContentType(String type) {
        String[] supportedTypes = {"image", "video", "text", "webpage"};
        for (String supportedType : supportedTypes) {
            if (supportedType.equalsIgnoreCase(type)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 显示图片内容
     */
    private void displayImage(String contentId, String url, int duration, ContentCallback callback) {
        Log.i(TAG, "显示图片: " + url);
        
        mainHandler.post(() -> {
            try {
                // 启动内容显示Activity
                Intent intent = new Intent(context, ContentDisplayActivity.class);
                intent.putExtra("contentType", "image");
                intent.putExtra("contentId", contentId);
                intent.putExtra("url", url);
                intent.putExtra("duration", duration);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                
                context.startActivity(intent);
                
                // 报告开始播放
                callback.onResult(contentId, "playing", null);
                
                // 设置完成回调
                mainHandler.postDelayed(() -> {
                    callback.onResult(contentId, "completed", null);
                }, duration * 1000);
                
            } catch (Exception e) {
                Log.e(TAG, "显示图片失败", e);
                callback.onResult(contentId, "error", e.getMessage());
            }
        });
    }
    
    /**
     * 显示视频内容
     */
    private void displayVideo(String contentId, String url, int duration, ContentCallback callback) {
        Log.i(TAG, "显示视频: " + url);
        
        mainHandler.post(() -> {
            try {
                Intent intent = new Intent(context, ContentDisplayActivity.class);
                intent.putExtra("contentType", "video");
                intent.putExtra("contentId", contentId);
                intent.putExtra("url", url);
                intent.putExtra("duration", duration);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                
                context.startActivity(intent);
                
                callback.onResult(contentId, "playing", null);
                
                // 视频播放完成的回调需要在ContentDisplayActivity中处理
                
            } catch (Exception e) {
                Log.e(TAG, "显示视频失败", e);
                callback.onResult(contentId, "error", e.getMessage());
            }
        });
    }
    
    /**
     * 显示文本内容
     */
    private void displayText(String contentId, JSONObject data, int duration, ContentCallback callback) {
        try {
            String text = data.optString("text", "");
            String title = data.optString("title", "");
            
            Log.i(TAG, "显示文本: " + title);
            
            mainHandler.post(() -> {
                try {
                    Intent intent = new Intent(context, ContentDisplayActivity.class);
                    intent.putExtra("contentType", "text");
                    intent.putExtra("contentId", contentId);
                    intent.putExtra("title", title);
                    intent.putExtra("text", text);
                    intent.putExtra("duration", duration);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    
                    context.startActivity(intent);
                    
                    callback.onResult(contentId, "playing", null);
                    
                    mainHandler.postDelayed(() -> {
                        callback.onResult(contentId, "completed", null);
                    }, duration * 1000);
                    
                } catch (Exception e) {
                    Log.e(TAG, "显示文本失败", e);
                    callback.onResult(contentId, "error", e.getMessage());
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "处理文本内容失败", e);
            callback.onResult(contentId, "error", e.getMessage());
        }
    }
    
    /**
     * 显示网页内容
     */
    private void displayWebpage(String contentId, String url, int duration, ContentCallback callback) {
        Log.i(TAG, "显示网页: " + url);
        
        mainHandler.post(() -> {
            try {
                Intent intent = new Intent(context, ContentDisplayActivity.class);
                intent.putExtra("contentType", "webpage");
                intent.putExtra("contentId", contentId);
                intent.putExtra("url", url);
                intent.putExtra("duration", duration);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                
                context.startActivity(intent);
                
                callback.onResult(contentId, "playing", null);
                
                mainHandler.postDelayed(() -> {
                    callback.onResult(contentId, "completed", null);
                }, duration * 1000);
                
            } catch (Exception e) {
                Log.e(TAG, "显示网页失败", e);
                callback.onResult(contentId, "error", e.getMessage());
            }
        });
    }
    
    /**
     * 停止当前内容播放
     */
    public void stopCurrentContent() {
        Log.i(TAG, "停止当前内容播放");
        
        mainHandler.post(() -> {
            // 发送广播或通知ContentDisplayActivity关闭
            Intent intent = new Intent("com.sakurapainting.mediaprogramandroid.STOP_CONTENT");
            context.sendBroadcast(intent);
        });
    }
    
    /**
     * 获取当前播放状态
     */
    public String getCurrentContentStatus() {
        // 这里可以维护一个状态变量
        return "idle"; // idle, playing, paused
    }
}
