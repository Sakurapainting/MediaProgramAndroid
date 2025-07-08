# 智慧融媒体终端 Android App

## 🎯 项目简介

这是一个为Android 4.4（API Level 19）设计的MQTT客户端应用，用于与智慧融媒体云平台进行数据通信。应用支持接收服务器推送的图片、视频、文本和网页内容，并能够上报设备状态信息。

## 📱 主要功能

### 1. MQTT通信
- ✅ 自动连接到云平台MQTT服务器
- ✅ 设备注册和身份认证
- ✅ 定时心跳和状态上报
- ✅ 自动重连机制
- ✅ 消息队列和离线缓存

### 2. 内容显示
- ✅ 图片内容全屏显示
- ✅ 视频内容播放
- ✅ 文本内容展示
- ✅ 网页内容浏览
- ✅ 自动播放时长控制

### 3. 设备管理
- ✅ 实时系统状态监控
- ✅ 设备信息收集和上报
- ✅ 远程命令执行
- ✅ 广播消息接收

### 4. 用户界面
- ✅ 简洁的主控制界面
- ✅ 实时连接状态显示
- ✅ 设备信息展示
- ✅ 手动连接控制

## 🔧 技术规格

### 系统要求
- **最低Android版本**: Android 4.4 (API Level 19)
- **目标Android版本**: Android 5.1 (API Level 22)
- **屏幕方向**: 横屏显示
- **网络要求**: WiFi或移动数据连接

### 核心依赖
```gradle
// MQTT客户端库
implementation 'org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5'
implementation 'org.eclipse.paho:org.eclipse.paho.android.service:1.1.1'

// AndroidX兼容库
implementation 'androidx.appcompat:appcompat:1.4.2'
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'

// 网络和JSON处理
implementation 'com.squareup.okhttp3:okhttp:4.9.3'
implementation 'org.json:json:20210307'
```

## 📋 MQTT消息协议

### 设备注册消息
```json
{
  "type": "register",
  "deviceId": "android_001",
  "clientId": "mqtt_client_001",
  "timestamp": 1640995200000,
  "data": {
    "deviceId": "android_001",
    "name": "安卓屏幕终端_001",
    "type": "android_screen",
    "location": {
      "name": "移动显示终端",
      "address": "位置待设定"
    },
    "specifications": {
      "resolution": "1920x1080",
      "brand": "Samsung",
      "model": "Galaxy Tab",
      "androidVersion": "4.4.2"
    }
  }
}
```

### 心跳消息
```json
{
  "type": "heartbeat",
  "deviceId": "android_001", 
  "clientId": "mqtt_client_001",
  "timestamp": 1640995200000,
  "data": {
    "uptime": 12345678,
    "memoryUsage": 45.6,
    "cpuUsage": 23.1,
    "temperature": 35
  }
}
```

### 内容推送响应
```json
{
  "type": "content_response",
  "deviceId": "android_001",
  "clientId": "mqtt_client_001", 
  "timestamp": 1640995200000,
  "data": {
    "contentId": "content_001",
    "status": "playing", // playing, completed, error
    "error": null
  }
}
```

## 🚀 快速开始

### 1. 环境准备
```bash
# 1. 确保Android SDK已安装
# 2. 目标设备或模拟器运行Android 4.4+
# 3. 云平台MQTT服务已启动（端口1884）
```

### 2. 构建应用
```bash
# 克隆项目
git clone <repository-url>
cd MediaProgramAndroid

# 使用Android Studio打开项目或命令行构建
./gradlew assembleDebug
```

### 3. 安装部署
```bash
# 安装到设备
adb install app/build/outputs/apk/debug/app-debug.apk

# 启动应用
adb shell am start -n com.sakurapainting.mediaprogramandroid/.MainActivity
```

### 4. 配置连接
```java
// 默认配置（可在ConfigManager中修改）
MQTT服务器: 10.0.2.2  // 模拟器使用，实际设备需要改为服务器IP
MQTT端口: 1884
自动连接: 开启
心跳间隔: 30秒
```

## 📁 项目结构

```
app/src/main/java/com/sakurapainting/mediaprogramandroid/
├── MediaApplication.java          # 应用程序主类
├── MainActivity.java              # 主Activity
├── MqttManager.java              # MQTT连接管理器
├── DeviceStatusManager.java      # 设备状态管理器
├── ContentManager.java           # 内容管理器
├── ContentDisplayActivity.java   # 内容显示Activity
└── ConfigManager.java            # 配置管理器

app/src/main/res/
├── layout/
│   └── activity_main.xml         # 主界面布局
├── values/
│   ├── strings.xml               # 字符串资源
│   ├── colors.xml                # 颜色资源
│   └── themes.xml                # 主题样式
└── AndroidManifest.xml           # 应用清单文件
```

## 🔗 与云平台连接

### 1. 服务器端准备
```bash
# 启动云平台后端服务
cd server
npm run dev

# 确认MQTT服务运行在1884端口
curl http://localhost:5001/api/mqtt/status
```

### 2. 网络配置
```xml
<!-- 对于模拟器测试 -->
<string name="mqtt_server">10.0.2.2</string>

<!-- 对于实际设备部署 -->
<string name="mqtt_server">192.168.1.100</string> <!-- 替换为实际服务器IP -->
```

### 3. 防火墙设置
```bash
# 确保1884端口对设备开放
sudo ufw allow 1884
# 或在Windows防火墙中允许端口1884
```

## 📱 使用说明

### 1. 应用启动
1. 打开应用后会自动请求必要权限
2. 授予权限后点击"连接MQTT"按钮
3. 观察连接状态显示

### 2. 内容推送测试
使用云平台管理界面或API推送测试内容：

```bash
# 推送图片内容
curl -X POST http://localhost:5001/api/mqtt/push \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "deviceId": "android_001",
    "contentId": "test_image_001", 
    "url": "https://example.com/image.jpg",
    "type": "image",
    "duration": 10
  }'
```

### 3. 设备管理
- 在云平台管理界面查看设备状态
- 发送远程命令（重启、截图等）
- 监控设备性能数据

## 🐛 故障排除

### 常见问题

1. **MQTT连接失败**
   - 检查网络连接
   - 确认服务器IP和端口
   - 查看应用日志：`adb logcat | grep MqttManager`

2. **权限被拒绝**
   - 在设备设置中手动授予应用权限
   - 重启应用重新请求权限

3. **内容显示异常**
   - 检查内容URL是否可访问
   - 确认设备支持内容格式
   - 查看ContentDisplayActivity日志

4. **心跳断开**
   - 检查网络稳定性
   - 调整心跳间隔（在ConfigManager中）
   - 确认设备电源管理设置

### 调试命令
```bash
# 查看应用日志
adb logcat | grep "MediaProgram"

# 检查MQTT连接
adb logcat | grep "MqttManager"

# 监控内容播放
adb logcat | grep "ContentDisplay"

# 查看设备状态
adb logcat | grep "DeviceStatus"
```

## 🔒 安全说明

### 权限使用
- `INTERNET`: MQTT网络通信
- `ACCESS_NETWORK_STATE`: 网络状态检查
- `WAKE_LOCK`: 保持连接活跃
- `READ_PHONE_STATE`: 获取设备唯一标识

### 数据安全
- 所有MQTT通信使用QoS 1确保消息传递
- 设备ID基于Android ID生成，保护隐私
- 建议生产环境启用SSL/TLS加密

## 📈 性能优化

### 内存管理
- 图片加载使用异步任务避免OOM
- 及时释放WebView和VideoView资源
- 定期清理MQTT消息缓存

### 电池优化
- 合理设置心跳间隔
- 屏幕关闭时降低活动频率
- 使用后台服务保持连接

### 网络优化
- 自动重连机制
- 消息队列和离线缓存
- 网络状态变化监听

## 🔄 版本更新

### v1.0.0 (当前版本)
- ✅ 基础MQTT通信功能
- ✅ 多媒体内容显示
- ✅ 设备状态监控
- ✅ Android 4.4兼容性

### 计划更新
- 🔲 SSL/TLS加密支持
- 🔲 离线内容缓存
- 🔲 设备分组管理
- 🔲 实时音视频流
- 🔲 固件OTA更新

## 📞 技术支持

如有问题或建议，请通过以下方式联系：

- 项目文档：参考 `MQTT_ANDROID_INTEGRATION.md`
- 服务器API：http://localhost:5001/api/mqtt/status
- 日志分析：使用 `adb logcat` 查看详细日志

---

**🎉 智慧融媒体终端 Android 应用已就绪！**

现在您可以：
- 部署到Android 4.4+设备
- 与云平台建立MQTT连接
- 接收和显示推送内容
- 实时监控设备状态
