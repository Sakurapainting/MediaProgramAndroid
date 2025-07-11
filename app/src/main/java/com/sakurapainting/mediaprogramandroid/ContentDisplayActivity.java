package com.sakurapainting.mediaprogramandroid;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 内容显示Activity
 * 用于全屏显示各种类型的内容
 */
public class ContentDisplayActivity extends Activity {
    
    private static final String TAG = "ContentDisplayActivity";
    
    private RelativeLayout rootLayout;
    private ImageView imageView;
    private VideoView videoView;
    private WebView webView;
    private TextView titleTextView;
    private TextView contentTextView;
    
    private String contentType;
    private String contentId;
    private String url;
    private String title;
    private String text;
    private int duration;
    
    private Handler autoCloseHandler;
    private Runnable autoCloseRunnable;
    
    private BroadcastReceiver stopContentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "收到停止内容播放指令");
            finish();
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 设置全屏无标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        // 保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        setupViews();
        getIntentData();
        registerStopReceiver();
        
        // 根据内容类型显示内容
        displayContent();
        
        // 设置自动关闭
        setupAutoClose();
    }
    
    /**
     * 设置视图
     */
    private void setupViews() {
        rootLayout = new RelativeLayout(this);
        rootLayout.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT));
        rootLayout.setBackgroundColor(0xFF000000); // 黑色背景
        
        // 图片视图
        imageView = new ImageView(this);
        imageView.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setVisibility(View.GONE);
        
        // 视频视图
        videoView = new VideoView(this);
        RelativeLayout.LayoutParams videoParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        videoParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        videoView.setLayoutParams(videoParams);
        videoView.setVisibility(View.GONE);
        
        // 网页视图
        webView = new WebView(this);
        webView.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT));
        webView.setVisibility(View.GONE);
        
        // 标题文本
        titleTextView = new TextView(this);
        RelativeLayout.LayoutParams titleParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        titleParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        titleParams.setMargins(50, 100, 50, 20);
        titleTextView.setLayoutParams(titleParams);
        titleTextView.setTextColor(0xFFFFFFFF);
        titleTextView.setTextSize(32);
        titleTextView.setGravity(android.view.Gravity.CENTER);
        titleTextView.setVisibility(View.GONE);
        
        // 内容文本
        contentTextView = new TextView(this);
        RelativeLayout.LayoutParams contentParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        contentParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        contentParams.setMargins(100, 0, 100, 0);
        contentTextView.setLayoutParams(contentParams);
        contentTextView.setTextColor(0xFFFFFFFF);
        contentTextView.setTextSize(24);
        contentTextView.setGravity(android.view.Gravity.CENTER);
        contentTextView.setVisibility(View.GONE);
        
        // 添加所有视图到根布局
        rootLayout.addView(imageView);
        rootLayout.addView(videoView);
        rootLayout.addView(webView);
        rootLayout.addView(titleTextView);
        rootLayout.addView(contentTextView);
        
        setContentView(rootLayout);
    }
    
    /**
     * 获取Intent数据
     */
    private void getIntentData() {
        Intent intent = getIntent();
        contentType = intent.getStringExtra("contentType");
        contentId = intent.getStringExtra("contentId");
        url = intent.getStringExtra("url");
        title = intent.getStringExtra("title");
        text = intent.getStringExtra("text");
        duration = intent.getIntExtra("duration", 10);
        
        Log.i(TAG, String.format("显示内容 - 类型: %s, ID: %s, 时长: %d秒", contentType, contentId, duration));
    }
    
    /**
     * 注册停止播放广播接收器
     */
    private void registerStopReceiver() {
        IntentFilter filter = new IntentFilter("com.sakurapainting.mediaprogramandroid.STOP_CONTENT");
        registerReceiver(stopContentReceiver, filter);
    }
    
    /**
     * 显示内容
     */
    private void displayContent() {
        if (contentType == null) {
            Log.e(TAG, "内容类型为空");
            finish();
            return;
        }
        
        switch (contentType.toLowerCase()) {
            case "image":
                displayImage();
                break;
            case "video":
                displayVideo();
                break;
            case "text":
                displayText();
                break;
            case "webpage":
                displayWebpage();
                break;
            default:
                Log.e(TAG, "不支持的内容类型: " + contentType);
                finish();
        }
    }
    
    /**
     * 显示图片
     */
    private void displayImage() {
        if (url == null || url.isEmpty()) {
            Log.e(TAG, "图片URL为空");
            finish();
            return;
        }
        
        imageView.setVisibility(View.VISIBLE);
        
        // 异步加载图片
        new LoadImageTask().execute(url);
    }
    
    /**
     * 显示视频
     */
    private void displayVideo() {
        if (url == null || url.isEmpty()) {
            Log.e(TAG, "视频URL为空");
            finish();
            return;
        }
        
        videoView.setVisibility(View.VISIBLE);
        
        try {
            Uri videoUri;
            
            // 判断是本地文件还是网络URL
            if (url.startsWith("http://") || url.startsWith("https://")) {
                // 网络URL
                videoUri = Uri.parse(url);
                Log.i(TAG, "播放网络视频: " + url);
            } else {
                // 本地文件路径
                java.io.File videoFile = new java.io.File(url);
                if (!videoFile.exists()) {
                    Log.e(TAG, "本地视频文件不存在: " + url);
                    finish();
                    return;
                }
                videoUri = Uri.fromFile(videoFile);
                Log.i(TAG, "播放本地视频: " + url);
            }
            
            videoView.setVideoURI(videoUri);
            
            // 设置媒体控制器（可选）
            MediaController mediaController = new MediaController(this);
            mediaController.setAnchorView(videoView);
            videoView.setMediaController(mediaController);
            
            // 设置播放监听器
            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    Log.i(TAG, "视频准备完成，开始播放");
                    videoView.start();
                    
                    // 设置视频尺寸适应
                    mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                }
            });
            
            videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    Log.i(TAG, "视频播放完成");
                    finish();
                }
            });
            
            videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    Log.e(TAG, "视频播放错误: what=" + what + ", extra=" + extra);
                    
                    // 提供更详细的错误信息
                    String errorMsg = "视频播放错误";
                    switch (what) {
                        case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                            errorMsg += " - 未知错误";
                            break;
                        case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                            errorMsg += " - 服务器错误";
                            break;
                        default:
                            errorMsg += " - 错误代码: " + what;
                    }
                    
                    Log.e(TAG, errorMsg);
                    finish();
                    return true;
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "设置视频播放失败", e);
            finish();
        }
    }
    
    /**
     * 显示文本
     */
    private void displayText() {
        if (title != null && !title.isEmpty()) {
            titleTextView.setText(title);
            titleTextView.setVisibility(View.VISIBLE);
        }
        
        if (text != null && !text.isEmpty()) {
            contentTextView.setText(text);
            contentTextView.setVisibility(View.VISIBLE);
        }
        
        if ((title == null || title.isEmpty()) && (text == null || text.isEmpty())) {
            Log.e(TAG, "文本内容为空");
            finish();
        }
    }
    
    /**
     * 显示网页
     */
    private void displayWebpage() {
        if (url == null || url.isEmpty()) {
            Log.e(TAG, "网页URL为空");
            finish();
            return;
        }
        
        webView.setVisibility(View.VISIBLE);
        
        // 配置WebView
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setBuiltInZoomControls(false);
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                Log.i(TAG, "网页加载完成: " + url);
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.e(TAG, "网页加载错误: " + description);
                finish();
            }
        });
        
        webView.loadUrl(url);
    }
    
    /**
     * 设置自动关闭
     */
    private void setupAutoClose() {
        if (duration > 0 && !contentType.equals("video")) { // 视频有自己的完成监听
            autoCloseHandler = new Handler();
            autoCloseRunnable = new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "内容显示时间到，自动关闭");
                    finish();
                }
            };
            
            autoCloseHandler.postDelayed(autoCloseRunnable, duration * 1000);
        }
    }
    
    /**
     * 异步图片加载任务
     */
    private class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
        
        @Override
        protected Bitmap doInBackground(String... urls) {
            String imageUrl = urls[0];
            
            try {
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                
                InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                input.close();
                connection.disconnect();
                
                return bitmap;
                
            } catch (Exception e) {
                Log.e(TAG, "加载图片失败: " + imageUrl, e);
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
                Log.i(TAG, "图片加载成功");
            } else {
                Log.e(TAG, "图片加载失败");
                finish();
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 取消自动关闭
        if (autoCloseHandler != null && autoCloseRunnable != null) {
            autoCloseHandler.removeCallbacks(autoCloseRunnable);
        }
        
        // 停止视频播放
        if (videoView != null && videoView.isPlaying()) {
            videoView.stopPlayback();
        }
        
        // 清理WebView
        if (webView != null) {
            webView.destroy();
        }
        
        // 注销广播接收器
        try {
            unregisterReceiver(stopContentReceiver);
        } catch (Exception e) {
            // 忽略注销错误
        }
        
        Log.i(TAG, "内容显示Activity已销毁");
    }
    
    @Override
    public void onBackPressed() {
        // 禁用返回键，防止意外退出
        // super.onBackPressed();
    }
}
