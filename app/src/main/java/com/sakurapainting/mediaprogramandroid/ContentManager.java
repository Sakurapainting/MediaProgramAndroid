package com.sakurapainting.mediaprogramandroid;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 内容管理器
 * 负责处理服务器推送的内容显示
 */
public class ContentManager {
    
    private static final String TAG = "ContentManager";
    
    private Context context;
    private Handler mainHandler;
    
    // 下载目录
    private static final String DOWNLOAD_DIR = "MediaProgram";
    
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
     * 视频下载监听接口
     */
    public interface DownloadProgressCallback {
        void onProgress(int progress);
        void onComplete(String localPath);
        void onError(String error);
    }
    
    /**
     * 处理内容推送
     */
    public void handleContentPush(JSONObject message, ContentCallback callback) {
        try {
            Log.i(TAG, "收到内容推送消息: " + message.toString());
            
            JSONObject data = message.getJSONObject("data");
            String contentId = data.getString("contentId");
            String fileUrl = data.getString("fileUrl");  // 从后端推送的完整URL
            String type = data.getString("type");
            String title = data.optString("title", "");
            String description = data.optString("description", "");
            String format = data.optString("format", "");
            int duration = data.optInt("duration", 0);
            
            Log.i(TAG, String.format("处理内容推送 - ID: %s, 类型: %s, 标题: %s, URL: %s", contentId, type, title, fileUrl));
            
            // 验证内容类型
            if (!isSupportedContentType(type)) {
                callback.onResult(contentId, "error", "不支持的内容类型: " + type);
                return;
            }
            
            // 根据内容类型处理
            switch (type.toLowerCase()) {
                case "image":
                    displayImage(contentId, fileUrl, duration, callback);
                    break;
                case "video":
                    // 对于视频，先下载到本地再播放
                    downloadAndDisplayVideo(contentId, title, fileUrl, format, duration, callback);
                    break;
                case "text":
                    displayText(contentId, data, duration, callback);
                    break;
                case "webpage":
                    displayWebpage(contentId, fileUrl, duration, callback);
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
     * 下载并显示视频内容
     */
    private void downloadAndDisplayVideo(String contentId, String title, String fileUrl, String format, int duration, ContentCallback callback) {
        Log.i(TAG, "开始下载视频: " + fileUrl);
        Log.i(TAG, "视频标题: " + title);
        Log.i(TAG, "内容ID: " + contentId);
        Log.i(TAG, "视频格式: " + format);
        
        // 先检查本地是否已有该文件
        String fileName = generateVideoFileName(contentId, title, format);
        File localFile = new File(getDownloadDirectory(), fileName);
        
        Log.i(TAG, "本地文件路径: " + localFile.getAbsolutePath());
        Log.i(TAG, "文件名: " + fileName);
        
        if (localFile.exists() && localFile.length() > 0) {
            Log.i(TAG, "本地文件已存在，直接播放: " + localFile.getAbsolutePath());
            displayVideo(contentId, localFile.getAbsolutePath(), duration, callback);
            return;
        }
        
        Log.i(TAG, "本地文件不存在，开始下载...");
        
        // 异步下载视频文件
        new VideoDownloadTask(contentId, fileUrl, localFile, new DownloadProgressCallback() {
            @Override
            public void onProgress(int progress) {
                Log.d(TAG, "下载进度: " + progress + "%");
                // 可以在这里通知UI更新下载进度
            }
            
            @Override
            public void onComplete(String localPath) {
                Log.i(TAG, "视频下载完成: " + localPath);
                File file = new File(localPath);
                Log.i(TAG, "下载文件大小: " + file.length() + " bytes");
                // 下载完成后立即播放
                displayVideo(contentId, localPath, duration, callback);
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "视频下载失败: " + error);
                callback.onResult(contentId, "error", "视频下载失败: " + error);
            }
        }).execute();
        
        // 立即报告开始下载状态
        callback.onResult(contentId, "downloading", null);
    }
    
    /**
     * 异步视频下载任务
     */
    private class VideoDownloadTask extends AsyncTask<Void, Integer, String> {
        private String contentId;
        private String fileUrl;
        private File localFile;
        private DownloadProgressCallback callback;
        private String errorMessage;
        
        public VideoDownloadTask(String contentId, String fileUrl, File localFile, DownloadProgressCallback callback) {
            this.contentId = contentId;
            this.fileUrl = fileUrl;
            this.localFile = localFile;
            this.callback = callback;
        }
        
        @Override
        protected String doInBackground(Void... params) {
            HttpURLConnection connection = null;
            InputStream input = null;
            FileOutputStream output = null;
            
            try {
                Log.i(TAG, "开始下载任务，URL: " + fileUrl);
                Log.i(TAG, "目标文件: " + localFile.getAbsolutePath());
                
                // 创建下载目录
                File parentDir = localFile.getParentFile();
                if (!parentDir.exists()) {
                    boolean created = parentDir.mkdirs();
                    Log.i(TAG, "创建父目录: " + parentDir.getAbsolutePath() + ", 结果: " + created);
                }
                
                URL url = new URL(fileUrl);
                Log.i(TAG, "建立HTTP连接到: " + url.toString());
                
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);
                
                Log.i(TAG, "开始连接...");
                connection.connect();
                
                int responseCode = connection.getResponseCode();
                Log.i(TAG, "HTTP响应码: " + responseCode);
                
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    errorMessage = "下载失败，HTTP响应码: " + responseCode;
                    Log.e(TAG, errorMessage);
                    return null;
                }
                
                int fileLength = connection.getContentLength();
                Log.i(TAG, "文件大小: " + fileLength + " bytes");
                
                input = connection.getInputStream();
                output = new FileOutputStream(localFile);
                
                byte[] buffer = new byte[4096];
                long total = 0;
                int count;
                
                Log.i(TAG, "开始读取数据...");
                while ((count = input.read(buffer)) != -1) {
                    if (isCancelled()) {
                        Log.w(TAG, "下载被取消");
                        return null;
                    }
                    
                    total += count;
                    output.write(buffer, 0, count);
                    
                    // 更新下载进度
                    if (fileLength > 0) {
                        int progress = (int) (total * 100 / fileLength);
                        publishProgress(progress);
                    }
                    
                    // 每下载1MB打印一次日志
                    if (total % (1024 * 1024) == 0) {
                        Log.d(TAG, "已下载: " + (total / 1024 / 1024) + "MB");
                    }
                }
                
                output.flush();
                Log.i(TAG, "下载完成，总共下载: " + total + " bytes");
                Log.i(TAG, "文件保存到: " + localFile.getAbsolutePath());
                Log.i(TAG, "文件实际大小: " + localFile.length() + " bytes");
                
                return localFile.getAbsolutePath();
                
            } catch (Exception e) {
                Log.e(TAG, "下载视频文件出错", e);
                errorMessage = e.getMessage();
                return null;
            } finally {
                try {
                    if (output != null) {
                        output.close();
                        Log.d(TAG, "关闭输出流");
                    }
                    if (input != null) {
                        input.close();
                        Log.d(TAG, "关闭输入流");
                    }
                    if (connection != null) {
                        connection.disconnect();
                        Log.d(TAG, "断开HTTP连接");
                    }
                } catch (IOException e) {
                    Log.e(TAG, "关闭流时出错", e);
                }
            }
        }
        
        @Override
        protected void onProgressUpdate(Integer... progress) {
            if (callback != null) {
                callback.onProgress(progress[0]);
            }
        }
        
        @Override
        protected void onPostExecute(String localPath) {
            if (localPath != null) {
                if (callback != null) {
                    callback.onComplete(localPath);
                }
            } else {
                if (callback != null) {
                    callback.onError(errorMessage != null ? errorMessage : "未知下载错误");
                }
            }
        }
    }
    
    /**
     * 生成视频文件名
     */
    private String generateVideoFileName(String contentId, String title, String format) {
        String extension = ".mp4"; // 默认扩展名
        
        if (format != null && !format.isEmpty()) {
            if (!format.startsWith(".")) {
                extension = "." + format;
            } else {
                extension = format;
            }
        }
        
        // 清理文件名中的非法字符
        String safeName = (title != null && !title.isEmpty()) ? 
            title.replaceAll("[^a-zA-Z0-9\\-_\\.]", "_") : 
            "video_" + contentId;
        
        return safeName + "_" + contentId + extension;
    }
    
    /**
     * 获取下载目录
     */
    private File getDownloadDirectory() {
        File dir;
        
        // 优先使用外部存储的下载目录
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            try {
                dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DOWNLOAD_DIR);
                Log.i(TAG, "使用外部存储下载目录: " + dir.getAbsolutePath());
            } catch (Exception e) {
                Log.w(TAG, "无法访问外部存储，使用应用私有目录", e);
                dir = new File(context.getFilesDir(), DOWNLOAD_DIR);
            }
        } else {
            Log.w(TAG, "外部存储不可用，使用应用私有目录");
            // 使用应用私有目录
            dir = new File(context.getFilesDir(), DOWNLOAD_DIR);
        }
        
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            Log.i(TAG, "创建下载目录: " + dir.getAbsolutePath() + ", 结果: " + created);
        }
        
        return dir;
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
