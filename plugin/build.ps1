# MusicJoy 插件构建脚本（Windows PowerShell）
# 用法：先设置服务端目录，再运行本脚本
#   方式一：设置环境变量  MUSICJOY_SERVER_DIR=D:\path\to\你的Mohist或Spigot服务端
#   方式二：直接改下面的 $serverDir 默认值
#
# 编译需要服务端目录里包含 java21.jar（或相应 JDK）与 Forge/Spigot 依赖库。

$scriptDir = $PSScriptRoot
$serverDir = if ($env:MUSICJOY_SERVER_DIR) { $env:MUSICJOY_SERVER_DIR } else { Join-Path (Split-Path $scriptDir -Parent) "server" }
$projectDir = $scriptDir
$srcDir = "$projectDir\src\main\java"
$resourcesDir = "$projectDir\src\main\resources"
$tmpDir = "$env:TEMP\musicjoy-build"
$classesDir = "$tmpDir\classes"
$jarFile = "$tmpDir\MusicJoy.jar"
$pluginTarget = "$serverDir\plugins\MusicJoy.jar"

if (-not (Test-Path $serverDir)) {
    Write-Host "[错误] 服务端目录不存在: $serverDir"
    Write-Host "请设置环境变量 MUSICJOY_SERVER_DIR 指向你的服务端目录。"
    exit 1
}

if (Test-Path $tmpDir) { Remove-Item -Recurse -Force $tmpDir }
New-Item -ItemType Directory -Path $classesDir -Force | Out-Null

# 依赖类路径：优先 java21.jar（服务端自带），再补齐 forge/kyori/httpclient/gson 等
$cpItems = @(
    "$serverDir\java21.jar",
    "$serverDir\libraries\net\minecraftforge\forge\1.20.1-47.3.22\forge-1.20.1-47.3.22-universal.jar",
    "$serverDir\libraries\net\kyori\adventure-api\4.14.0\adventure-api-4.14.0.jar",
    "$serverDir\libraries\net\kyori\adventure-api\4.14.0\adventure-api-4.14.0.jar",
    "$serverDir\libraries\org\apache\httpcomponents\httpclient\4.5.13\httpclient-4.5.13.jar",
    "$serverDir\libraries\org\apache\httpcomponents\httpcore\4.4.14\httpcore-4.4.14.jar",
    "$serverDir\libraries\commons-codec\commons-codec\1.16.0\commons-codec-1.16.0.jar",
    "$serverDir\libraries\commons-logging\commons-logging\1.2\commons-logging-1.2.jar",
    "$serverDir\libraries\com\google\code\gson\gson\2.10\gson-2.10.jar"
)
$cp = ($cpItems | ForEach-Object { "`"$_`"" }) -join ";"

$javaFiles = Get-ChildItem -Path $srcDir -Filter "*.java" -Recurse | ForEach-Object { "`"$($_.FullName)`"" }

Write-Host "=== Compiling ==="
$javacArgs = @("-cp", $cp, "-d", "`"$classesDir`"", "-source", "8", "-target", "8", "-proc:none") + $javaFiles
$process = Start-Process -FilePath "javac" -ArgumentList $javacArgs -NoNewWindow -Wait -PassThru

if ($process.ExitCode -ne 0) { Write-Host "=== COMPILATION FAILED ==="; exit 1 }
Write-Host "=== Compilation OK ==="

Copy-Item -Path "$resourcesDir\*" -Destination $classesDir -Force

Write-Host "=== Creating JAR ==="
Push-Location $classesDir
jar cf "`"$jarFile`"" .
Pop-Location

Write-Host "=== JAR created: $jarFile ==="
if (Test-Path "$serverDir\plugins") {
    Copy-Item -Path $jarFile -Destination $pluginTarget -Force
    Write-Host "=== Copied to server: $pluginTarget ==="
} else {
    Write-Host "（未找到 $serverDir\plugins，跳过复制）"
}
Write-Host "=== DONE ==="
