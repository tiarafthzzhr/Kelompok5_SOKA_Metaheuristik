param(
    [ValidateRange(0,20)][int]$Tasks = 0,
    [switch]$Full,
    [switch]$ImageInfrastructure,
    [ValidateRange(100,1000)][int]$StartTasks = 100,
    [ValidateRange(100,1000)][int]$EndTasks = 500,
    [string]$OutputDirectory = "."
)
$ErrorActionPreference = 'Stop'
$taskPrevious = Join-Path (Split-Path $PSScriptRoot -Parent) 'soka-taskscheduling1'
$taskJdk = Join-Path $taskPrevious '.tools/jdk-21.0.12.1+1'
$taskMaven = Join-Path $taskPrevious '.tools/apache-maven-3.9.9/bin/mvn.cmd'
$taskSettings = Join-Path $taskPrevious '.vscode/maven-settings.xml'
$taskMode = if ($ImageInfrastructure) { '--image-range' } else { '--range' }
if ($ImageInfrastructure -and $OutputDirectory -eq '.') { $OutputDirectory = 'experiments/infrastruktur-gambar' }
$taskArgs = if ($Full -or $Tasks -eq 0) { "$taskMode $StartTasks $EndTasks $OutputDirectory" } else { "$Tasks" }
Push-Location $PSScriptRoot
try {
    if (Test-Path $taskMaven) {
        $env:JAVA_HOME = $taskJdk
        $env:Path = "$taskJdk\bin;$env:Path"
        & $taskMaven -q -s $taskSettings compile exec:java '-Dexec.mainClass=soka.Main' "-Dexec.args=$taskArgs"
    } else {
        & mvn -q compile exec:java '-Dexec.mainClass=soka.Main' "-Dexec.args=$taskArgs"
    }
    if ($LASTEXITCODE -ne 0) { throw 'Eksekusi gagal. Periksa pesan di atas.' }
} finally {
    Pop-Location
}
