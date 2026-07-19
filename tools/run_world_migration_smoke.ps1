param(
    [Parameter(Mandatory = $true)]
    [string]$SourceWorld,
    [Parameter(Mandatory = $true)]
    [string]$InstanceMods,
    [int]$TimeoutSeconds = 240
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$module = Join-Path $repo 'neoforge-26.2'
$runRoot = Join-Path $module 'runs\migration'
$worldName = 'migration-world'
$worldTarget = Join-Path $runRoot $worldName
$modsTarget = Join-Path $runRoot 'mods'
$outputRoot = Join-Path $repo 'output\quality'
$archiveRoot = Join-Path $repo ('tmp\migration-smoke-backups\' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
$rconClient = Join-Path $repo 'tools\minecraft_rcon.py'

$sourceWorldPath = (Resolve-Path -LiteralPath $SourceWorld).Path
$instanceModsPath = (Resolve-Path -LiteralPath $InstanceMods).Path
$sourceLevel = Join-Path $sourceWorldPath 'level.dat'
if (-not (Test-Path -LiteralPath $sourceLevel)) {
    throw "Source world has no level.dat: $sourceWorldPath"
}
$sourceHashBefore = (Get-FileHash -LiteralPath $sourceLevel -Algorithm SHA256).Hash
$clientOnlyPatterns = @(
    '*dynamic-fps*', '*sodium*', '*appleskin*', '*Controlling*', '*MouseTweaks*',
    '*drippyloadingscreen*', '*autofish*', '*IMBlocker*', '*JustEnoughProfessions*',
    '*jei-*', '*LegendaryTooltips*', '*EnchantmentDescriptions*', 'EnchantmentInsights*',
    'Searchables*', 'fancymenu*', 'konkrete*', 'melody*', '*lambdynamiclights*',
    '*xaerominimap*', '*xaeroworldmap*', '*Jade*'
)

function Test-ClientOnlyMod([string]$Name) {
    foreach ($pattern in $clientOnlyPatterns) {
        if ($Name -like $pattern) {
            return $true
        }
    }
    return $false
}

New-Item -ItemType Directory -Path $runRoot, $outputRoot, $archiveRoot -Force | Out-Null
foreach ($path in @($worldTarget, $modsTarget)) {
    if (Test-Path -LiteralPath $path) {
        Move-Item -LiteralPath $path -Destination (Join-Path $archiveRoot (Split-Path $path -Leaf))
    }
}

Copy-Item -LiteralPath $sourceWorldPath -Destination $worldTarget -Recurse
New-Item -ItemType Directory -Path $modsTarget -Force | Out-Null
$activeMods = @(Get-ChildItem -LiteralPath $instanceModsPath -File -Filter '*.jar' |
    Where-Object { $_.Name -notlike 'earth-on-minecraft-neoforge-26.2-*.jar' })
$excludedMods = @($activeMods | Where-Object { Test-ClientOnlyMod $_.Name })
$copiedMods = @($activeMods | Where-Object { -not (Test-ClientOnlyMod $_.Name) })
foreach ($mod in $copiedMods) {
    Copy-Item -LiteralPath $mod.FullName -Destination (Join-Path $modsTarget $mod.Name)
}

Set-Content -LiteralPath (Join-Path $runRoot 'eula.txt') -Encoding ascii -Value 'eula=true'
@'
level-name=migration-world
online-mode=false
server-port=25567
motd=Earth on Minecraft migration smoke
view-distance=4
simulation-distance=4
max-tick-time=120000
sync-chunk-writes=true
enable-rcon=true
rcon.port=25577
rcon.password=earth-migration-smoke-local
broadcast-rcon-to-ops=false
'@ | Set-Content -LiteralPath (Join-Path $runRoot 'server.properties') -Encoding ascii

$latestLog = Join-Path $runRoot 'logs\latest.log'
$crashRoot = Join-Path $runRoot 'crash-reports'
if (Test-Path -LiteralPath $latestLog) {
    Move-Item -LiteralPath $latestLog -Destination (Join-Path $archiveRoot 'latest.log')
}
$crashesBefore = if (Test-Path -LiteralPath $crashRoot) {
    @(Get-ChildItem -LiteralPath $crashRoot -File | Select-Object -ExpandProperty FullName)
} else { @() }

$psi = [System.Diagnostics.ProcessStartInfo]::new()
$psi.FileName = 'cmd.exe'
$psi.Arguments = '/d /c gradlew.bat runMigrationServer --console=plain'
$psi.WorkingDirectory = $module
$psi.UseShellExecute = $false
$psi.CreateNoWindow = $true
$psi.RedirectStandardInput = $true
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true
$process = [System.Diagnostics.Process]::new()
$process.StartInfo = $psi
if (-not $process.Start()) {
    throw 'Failed to start migration server process'
}
$stdoutTask = $process.StandardOutput.ReadToEndAsync()
$stderrTask = $process.StandardError.ReadToEndAsync()

function Stop-MigrationServerViaRcon {
    $previousPassword = $env:EOM_MIGRATION_RCON_PASSWORD
    try {
        $env:EOM_MIGRATION_RCON_PASSWORD = 'earth-migration-smoke-local'
        $response = @(& python $rconClient --host 127.0.0.1 --port 25577 --timeout 10 `
            --password-env EOM_MIGRATION_RCON_PASSWORD stop 2>&1)
        if ($LASTEXITCODE -ne 0) {
            throw "RCON stop failed: $($response -join [Environment]::NewLine)"
        }
        if ($response.Count -gt 0) {
            Write-Output ($response -join [Environment]::NewLine)
        }
    } finally {
        $env:EOM_MIGRATION_RCON_PASSWORD = $previousPassword
    }
}

$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
$started = $false
while ((Get-Date) -lt $deadline -and -not $process.HasExited) {
    Start-Sleep -Seconds 1
    if (Test-Path -LiteralPath $latestLog) {
        $logText = Get-Content -LiteralPath $latestLog -Raw -ErrorAction SilentlyContinue
        if ($logText -match 'Done \([0-9.]+s\)!' -or $logText -match 'For help, type "help"') {
            $started = $true
            break
        }
    }
}

if ($started -and -not $process.HasExited) {
    Start-Sleep -Seconds 1
    Stop-MigrationServerViaRcon
}
if (-not $process.WaitForExit(120000)) {
    $process.Kill($true)
    $process.WaitForExit()
}

$stdout = $stdoutTask.GetAwaiter().GetResult()
$stderr = $stderrTask.GetAwaiter().GetResult()
$stdoutPath = Join-Path $outputRoot 'migration-server-stdout.log'
$stderrPath = Join-Path $outputRoot 'migration-server-stderr.log'
Set-Content -LiteralPath $stdoutPath -Encoding utf8 -Value $stdout
Set-Content -LiteralPath $stderrPath -Encoding utf8 -Value $stderr

$sourceHashAfter = (Get-FileHash -LiteralPath $sourceLevel -Algorithm SHA256).Hash
$crashesAfter = if (Test-Path -LiteralPath $crashRoot) {
    @(Get-ChildItem -LiteralPath $crashRoot -File | Select-Object -ExpandProperty FullName)
} else { @() }
$newCrashes = @($crashesAfter | Where-Object { $_ -notin $crashesBefore })
$finalLog = if (Test-Path -LiteralPath $latestLog) { Get-Content -LiteralPath $latestLog -Raw } else { '' }
$saved = $finalLog -match 'Saving chunks for level' -or $finalLog -match 'All dimensions are saved'
$stopped = $finalLog -match 'Stopping server'
$earthErrors = @($finalLog -split "`r?`n" | Where-Object {
    $_ -match '(?i)earth[_ ]on[_ ]minecraft' -and $_ -match '(?i)(ERROR|Exception|Failed)'
})

$report = [ordered]@{
    schema_version = 1
    source_world = $sourceWorldPath
    source_level_hash_unchanged = ($sourceHashBefore -eq $sourceHashAfter)
    copied_mods = $copiedMods.Count
    excluded_client_mods = $excludedMods.Count
    server_started = $started
    graceful_save_observed = $saved
    graceful_stop_observed = $stopped
    exit_code = $process.ExitCode
    new_crash_reports = $newCrashes.Count
    earth_error_lines = $earthErrors.Count
    latest_log = if (Test-Path -LiteralPath $latestLog) { $latestLog } else { $null }
    stdout = $stdoutPath
    stderr = $stderrPath
}
$reportPath = Join-Path $outputRoot 'world-migration-report.json'
$report | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $reportPath -Encoding utf8

if (-not $started -or -not $saved -or -not $stopped -or $process.ExitCode -ne 0 -or $newCrashes.Count -ne 0 -or
        $earthErrors.Count -ne 0 -or $sourceHashBefore -ne $sourceHashAfter) {
    Get-Content -LiteralPath $reportPath
    throw "WORLD_MIGRATION_FAILED report=$reportPath"
}
Write-Output "WORLD_MIGRATION_OK mods=$($copiedMods.Count) report=$reportPath"
