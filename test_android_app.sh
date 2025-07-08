#!/bin/bash

# 智慧融媒体终端 Android App 测试脚本
# 用于测试MQTT连接和内容推送功能

echo "🚀 智慧融媒体终端测试脚本"
echo "=================================="

# 检查ADB连接
echo "📱 检查设备连接..."
if ! adb devices | grep -q "device$"; then
    echo "❌ 未发现Android设备，请确保设备已连接并开启USB调试"
    exit 1
fi
echo "✅ 设备连接正常"

# 检查应用是否安装
echo "📦 检查应用安装状态..."
if adb shell pm list packages | grep -q "com.sakurapainting.mediaprogramandroid"; then
    echo "✅ 应用已安装"
else
    echo "❌ 应用未安装，请先安装APK文件"
    exit 1
fi

# 启动应用
echo "🎯 启动应用..."
adb shell am start -n com.sakurapainting.mediaprogramandroid/.MainActivity
sleep 3
echo "✅ 应用已启动"

# 检查MQTT服务器状态
echo "🌐 检查MQTT服务器状态..."
if curl -s http://localhost:5001/health > /dev/null; then
    echo "✅ 后端服务运行正常"
else
    echo "⚠️  后端服务未运行，请先启动云平台服务"
fi

# 获取认证Token (需要在实际环境中替换)
echo "🔐 获取认证Token..."
TOKEN=$(curl -s -X POST http://localhost:5001/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"admin@example.com","password":"123456"}' | \
    grep -o '"token":"[^"]*"' | sed 's/"token":"\([^"]*\)"/\1/')

if [ -n "$TOKEN" ]; then
    echo "✅ 认证成功"
else
    echo "⚠️  认证失败，请检查服务器状态"
    TOKEN="dummy_token"
fi

# 等待设备连接
echo "⏳ 等待设备MQTT连接..."
sleep 10

# 检查设备列表
echo "📋 查询设备列表..."
curl -s -H "Authorization: Bearer $TOKEN" \
    http://localhost:5001/api/mqtt/devices | \
    python3 -m json.tool 2>/dev/null || echo "设备列表查询失败或无JSON数据"

# 测试内容推送
echo "🎨 测试图片内容推送..."
curl -s -X POST http://localhost:5001/api/mqtt/push \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d '{
        "deviceId": "android_001",
        "contentId": "test_image_001",
        "url": "https://picsum.photos/800/600",
        "type": "image",
        "duration": 5
    }' > /dev/null

echo "📺 测试文本内容推送..."
curl -s -X POST http://localhost:5001/api/mqtt/push \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d '{
        "deviceId": "android_001",
        "contentId": "test_text_001",
        "type": "text",
        "duration": 5,
        "data": {
            "title": "测试标题",
            "text": "这是一条测试文本消息，用于验证内容推送功能。"
        }
    }' > /dev/null

echo "🌐 测试网页内容推送..."
curl -s -X POST http://localhost:5001/api/mqtt/push \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d '{
        "deviceId": "android_001",
        "contentId": "test_webpage_001",
        "url": "https://www.baidu.com",
        "type": "webpage",
        "duration": 8
    }' > /dev/null

# 测试命令下发
echo "⚡ 测试命令下发..."
curl -s -X POST http://localhost:5001/api/mqtt/command \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d '{
        "deviceId": "android_001",
        "command": "get_status"
    }' > /dev/null

# 测试广播消息
echo "📢 测试广播消息..."
curl -s -X POST http://localhost:5001/api/mqtt/broadcast \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d '{
        "message": "系统测试广播消息",
        "level": "info"
    }' > /dev/null

# 监控应用日志
echo "📊 监控应用日志 (10秒)..."
echo "按 Ctrl+C 停止日志监控"
timeout 10 adb logcat | grep -E "(MqttManager|ContentDisplay|MediaProgram)" || true

echo ""
echo "🎉 测试完成！"
echo "=================================="
echo "请检查设备屏幕确认内容是否正常显示"
echo "如有问题，请查看完整日志：adb logcat | grep MediaProgram"
