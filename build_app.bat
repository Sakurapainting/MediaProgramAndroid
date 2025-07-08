@echo off
REM 智慧融媒体终端 Android App 构建脚本
REM 适用于Windows环境

echo ===========================================
echo    智慧融媒体终端 Android App 构建
echo ===========================================
echo.

REM 检查Java环境
echo 检查Java环境...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Java环境未配置，请安装JDK
    pause
    exit /b 1
)
echo ✅ Java环境正常

REM 检查Android SDK
echo 检查Android SDK...
if not defined ANDROID_HOME (
    echo ❌ ANDROID_HOME环境变量未设置
    echo 请设置ANDROID_HOME指向Android SDK路径
    pause
    exit /b 1
)
echo ✅ Android SDK路径: %ANDROID_HOME%

REM 清理项目
echo.
echo 清理项目...
call gradlew clean
if %errorlevel% neq 0 (
    echo ❌ 项目清理失败
    pause
    exit /b 1
)
echo ✅ 项目清理完成

REM 构建Debug版本
echo.
echo 构建Debug版本...
call gradlew assembleDebug
if %errorlevel% neq 0 (
    echo ❌ 构建失败
    pause
    exit /b 1
)
echo ✅ Debug版本构建完成

REM 构建Release版本
echo.
echo 构建Release版本...
call gradlew assembleRelease
if %errorlevel% neq 0 (
    echo ⚠️  Release版本构建失败（可能需要签名配置）
) else (
    echo ✅ Release版本构建完成
)

REM 显示APK文件位置
echo.
echo ===========================================
echo 构建完成！APK文件位置：
echo.
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo 📱 Debug版本: app\build\outputs\apk\debug\app-debug.apk
)
if exist "app\build\outputs\apk\release\app-release-unsigned.apk" (
    echo 📱 Release版本: app\build\outputs\apk\release\app-release-unsigned.apk
)
echo.

REM 检查设备连接
echo 检查设备连接...
adb devices
echo.

REM 询问是否安装
set /p install="是否要安装到连接的设备？(y/n): "
if /i "%install%"=="y" (
    if exist "app\build\outputs\apk\debug\app-debug.apk" (
        echo 正在安装Debug版本...
        adb install -r "app\build\outputs\apk\debug\app-debug.apk"
        if %errorlevel% equ 0 (
            echo ✅ 安装成功
            echo.
            set /p launch="是否要启动应用？(y/n): "
            if /i "!launch!"=="y" (
                adb shell am start -n com.sakurapainting.mediaprogramandroid/.MainActivity
                echo ✅ 应用已启动
            )
        ) else (
            echo ❌ 安装失败
        )
    ) else (
        echo ❌ 找不到APK文件
    )
)

echo.
echo ===========================================
echo 构建脚本执行完成
echo.
echo 接下来您可以：
echo 1. 手动安装APK文件到设备
echo 2. 运行测试脚本验证功能
echo 3. 启动云平台服务进行联调
echo ===========================================

pause
