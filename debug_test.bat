@echo off
echo 智慧融媒体终端 Android 4.4 调试脚本
echo =====================================

echo 1. 安装最新APK...
adb install -r app\build\outputs\apk\debug\app-debug.apk

echo.
echo 2. 启动应用...
adb shell am start -n com.sakurapainting.mediaprogramandroid/.MainActivity

echo.
echo 3. 等待5秒...
timeout /t 5 /nobreak > nul

echo.
echo 4. 获取应用日志（最近100行）...
adb logcat -d | findstr /i "MediaProgramAndroid MainActivity MediaApplication MQTT DeviceStatus AndroidRuntime FATAL"

echo.
echo 5. 清理日志缓存...
adb logcat -c

echo.
echo 调试完成！如果应用崩溃，上面会显示相关错误日志。
pause
