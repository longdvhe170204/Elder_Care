# Khởi động backend demo QuickstartDemoApplication trên schema elder_care_qs3, cổng 8083 (quickstart 007).
# Chạy từ bất kỳ đâu: powershell -File backend\tools\quickstart-007\start.ps1 ; log ở D:\eldercare-logs\qs-007-server.log
Set-Location (Join-Path $PSScriptRoot "..\..")
Get-Content .env | Where-Object { $_ -match '^[A-Z_]+=' } | ForEach-Object { $k, $v = $_ -split '=', 2; [Environment]::SetEnvironmentVariable($k, $v) }
$env:DB_URL = "jdbc:mysql://localhost:3306/elder_care_qs3"
$env:SERVER_PORT = "8083"
$env:ESMS_API_KEY = $null; $env:FIREBASE_CREDENTIALS = $null
$env:TEMP = "D:\eldercare-tmp"; $env:TMP = "D:\eldercare-tmp"
.\mvnw.cmd -q spring-boot:test-run "-Dspring-boot.run.main-class=com.eldercare.backend.support.demo.QuickstartDemoApplication" "-Dspring-boot.run.profiles=demo" "-Dspring-boot.run.jvmArguments=-Djdk.net.unixdomain.tmpdir=C:\eldercare-tmp -Djava.io.tmpdir=D:\eldercare-tmp" *> D:\eldercare-logs\qs-007-server.log
