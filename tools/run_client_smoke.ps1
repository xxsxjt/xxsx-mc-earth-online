param(
    [string]$TemplateWorld = '',
    [int]$TimeoutSeconds = 180
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$module = Join-Path $repo 'neoforge-26.2'
if ([string]::IsNullOrWhiteSpace($TemplateWorld)) {
    $TemplateWorld = Join-Path $module 'runs\client\saves\ui-test'
}
$runRoot = Join-Path $module 'runs\client-smoke'
$worldName = 'ui-test'
$worldTarget = Join-Path $runRoot "saves\$worldName"
$outputRoot = Join-Path $repo 'output\quality'
$archiveRoot = Join-Path $repo ('tmp\client-smoke-backups\' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
$latestLog = Join-Path $runRoot 'logs\latest.log'
$crashRoot = Join-Path $runRoot 'crash-reports'

$templatePath = (Resolve-Path -LiteralPath $TemplateWorld).Path
$templateLevel = Join-Path $templatePath 'level.dat'
if (-not (Test-Path -LiteralPath $templateLevel)) {
    throw "Client smoke template has no level.dat: $templatePath"
}
$templateHashBefore = (Get-FileHash -LiteralPath $templateLevel -Algorithm SHA256).Hash

New-Item -ItemType Directory -Path $runRoot, $outputRoot, $archiveRoot -Force | Out-Null
if (Test-Path -LiteralPath $worldTarget) {
    Move-Item -LiteralPath $worldTarget -Destination (Join-Path $archiveRoot $worldName)
}
New-Item -ItemType Directory -Path (Split-Path -Parent $worldTarget) -Force | Out-Null
Copy-Item -LiteralPath $templatePath -Destination $worldTarget -Recurse

$sourceOptions = Join-Path $module 'runs\client\options.txt'
$targetOptions = Join-Path $runRoot 'options.txt'
if (Test-Path -LiteralPath $sourceOptions) {
    Copy-Item -LiteralPath $sourceOptions -Destination $targetOptions -Force
}
if (Test-Path -LiteralPath $latestLog) {
    Move-Item -LiteralPath $latestLog -Destination (Join-Path $archiveRoot 'latest.log')
}
$crashesBefore = if (Test-Path -LiteralPath $crashRoot) {
    @(Get-ChildItem -LiteralPath $crashRoot -File | Select-Object -ExpandProperty FullName)
} else { @() }

$stdoutPath = Join-Path $outputRoot 'client-smoke-stdout.log'
$stderrPath = Join-Path $outputRoot 'client-smoke-stderr.log'
$psi = [System.Diagnostics.ProcessStartInfo]::new()
$psi.FileName = 'cmd.exe'
$psi.Arguments = '/d /c gradlew.bat runClientSmoke --console=plain --no-daemon'
$psi.WorkingDirectory = $module
$psi.UseShellExecute = $false
$psi.CreateNoWindow = $true
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true
$process = [System.Diagnostics.Process]::new()
$process.StartInfo = $psi
if (-not $process.Start()) {
    throw 'Failed to start standalone client smoke process'
}
$stdoutTask = $process.StandardOutput.ReadToEndAsync()
$stderrTask = $process.StandardError.ReadToEndAsync()

function Get-SmokeGameProcess {
    Get-CimInstance Win32_Process | Where-Object {
        $_.Name -in @('java.exe', 'javaw.exe') -and
        $_.CommandLine -match 'clientSmokeRunVmArgs|runs[\\/]client-smoke'
    } | Select-Object -First 1
}

$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
$resourceLoaded = $false
$soundLoaded = $false
$atlasLoaded = $false
$worldStarted = $false
$playerJoined = $false
while ((Get-Date) -lt $deadline -and -not $process.HasExited) {
    Start-Sleep -Seconds 1
    if (-not (Test-Path -LiteralPath $latestLog)) {
        continue
    }
    $logText = Get-Content -LiteralPath $latestLog -Raw -ErrorAction SilentlyContinue
    $resourceLoaded = $logText -match 'Reloading ResourceManager:.*mod/earth_on_minecraft'
    $soundLoaded = $logText -match 'Sound engine started'
    $atlasLoaded = $logText -match 'minecraft:textures/atlas/blocks\.png-atlas'
    $worldStarted = $logText -match 'Starting integrated minecraft server version 26\.2'
    $playerJoined = $logText -match 'logged in with entity id'
    if ($resourceLoaded -and $soundLoaded -and $atlasLoaded -and $worldStarted -and $playerJoined) {
        break
    }
}

if (-not $process.HasExited) {
    if ($playerJoined) {
        Start-Sleep -Seconds 5
    }
    $game = Get-SmokeGameProcess
    if ($null -ne $game) {
        $gameProcess = Get-Process -Id $game.ProcessId -ErrorAction SilentlyContinue
        if ($null -ne $gameProcess) {
            [void]$gameProcess.CloseMainWindow()
        }
    }
}

if (-not $process.WaitForExit(60000)) {
    $game = Get-SmokeGameProcess
    if ($null -ne $game) {
        Stop-Process -Id $game.ProcessId -Force -ErrorAction SilentlyContinue
    }
    if (-not $process.WaitForExit(30000)) {
        $process.Kill($true)
        $process.WaitForExit()
    }
}

$stdout = $stdoutTask.GetAwaiter().GetResult()
$stderr = $stderrTask.GetAwaiter().GetResult()
Set-Content -LiteralPath $stdoutPath -Encoding utf8 -Value $stdout
Set-Content -LiteralPath $stderrPath -Encoding utf8 -Value $stderr

$templateHashAfter = (Get-FileHash -LiteralPath $templateLevel -Algorithm SHA256).Hash
$finalLog = if (Test-Path -LiteralPath $latestLog) { Get-Content -LiteralPath $latestLog -Raw } else { '' }
$earthErrors = @($finalLog -split "`r?`n" | Where-Object {
    $_ -match '(?i)earth[_ ]on[_ ]minecraft' -and $_ -match '(?i)(ERROR|Exception|Failed|Missing|Unable)'
})
$crashesAfter = if (Test-Path -LiteralPath $crashRoot) {
    @(Get-ChildItem -LiteralPath $crashRoot -File | Select-Object -ExpandProperty FullName)
} else { @() }
$newCrashes = @($crashesAfter | Where-Object { $_ -notin $crashesBefore })
$gracefulStop = $finalLog -match 'Stopping!' -and $finalLog -match 'All dimensions are saved'

$report = [ordered]@{
    schema_version = 1
    template_world = $templatePath
    template_level_hash_unchanged = ($templateHashBefore -eq $templateHashAfter)
    resource_reload_observed = $resourceLoaded
    sound_engine_observed = $soundLoaded
    block_atlas_observed = $atlasLoaded
    integrated_server_observed = $worldStarted
    player_join_observed = $playerJoined
    graceful_stop_observed = $gracefulStop
    exit_code = $process.ExitCode
    new_crash_reports = $newCrashes.Count
    earth_error_lines = $earthErrors.Count
    latest_log = if (Test-Path -LiteralPath $latestLog) { $latestLog } else { $null }
    stdout = $stdoutPath
    stderr = $stderrPath
}
$reportPath = Join-Path $outputRoot 'client-smoke-report.json'
$report | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $reportPath -Encoding utf8

if (-not $resourceLoaded -or -not $soundLoaded -or -not $atlasLoaded -or -not $worldStarted -or
        -not $playerJoined -or -not $gracefulStop -or $process.ExitCode -ne 0 -or
        $newCrashes.Count -ne 0 -or $earthErrors.Count -ne 0 -or $templateHashBefore -ne $templateHashAfter) {
    Get-Content -LiteralPath $reportPath
    throw "CLIENT_SMOKE_FAILED report=$reportPath"
}
Write-Output "CLIENT_SMOKE_OK report=$reportPath"
