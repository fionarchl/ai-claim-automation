$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$mavenHome = "$projectRoot\.tools\apache-maven-3.9.6"
$localRepository = "$projectRoot\.m2\repository"
$requiredJavaMajor = 21

function Get-JavaMajorVersion($javaExe) {
    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $versionOutput = & "$javaExe" -version 2>&1
    $ErrorActionPreference = $previousErrorActionPreference
    $versionLine = ($versionOutput | Select-Object -First 1).ToString()
    if ($versionLine -notmatch '"(?<major>\d+)(\.(?<minor>\d+))?') {
        throw "Could not determine Java version from: $versionLine"
    }

    $major = [int]$Matches["major"]
    if ($major -eq 1 -and $Matches["minor"]) {
        return [int]$Matches["minor"]
    }
    return $major
}

function Get-LocalJavaExe {
    $localJdkRoot = Join-Path $projectRoot ".tools\temurin-jdk-21"
    if (-not (Test-Path $localJdkRoot)) {
        return $null
    }

    $localJdk = Get-ChildItem -Path $localJdkRoot -Directory |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($null -eq $localJdk) {
        return $null
    }

    $localJavaExe = Join-Path $localJdk.FullName "bin\java.exe"
    if (Test-Path $localJavaExe) {
        return $localJavaExe
    }
    return $null
}

if ([string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
    $localJavaExe = Get-LocalJavaExe
    if ($null -ne $localJavaExe) {
        $javaExe = $localJavaExe
        $env:JAVA_HOME = Split-Path -Parent (Split-Path -Parent $javaExe)
        $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    } else {
        $javaCommand = Get-Command java -ErrorAction SilentlyContinue
        if ($null -eq $javaCommand) {
            throw "Java was not found. Install JDK $requiredJavaMajor, set JAVA_HOME, or add java.exe to PATH."
        }
        $javaExe = $javaCommand.Source
    }
} else {
    $javaExe = Join-Path $env:JAVA_HOME "bin\java.exe"
    if (-not (Test-Path $javaExe)) {
        throw "JAVA_HOME does not contain bin\java.exe: $env:JAVA_HOME"
    }
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
}

$javaMajor = Get-JavaMajorVersion $javaExe
if ($javaMajor -lt $requiredJavaMajor) {
    $localJavaExe = Get-LocalJavaExe
    if ($null -ne $localJavaExe -and $localJavaExe -ne $javaExe) {
        $javaExe = $localJavaExe
        $env:JAVA_HOME = Split-Path -Parent (Split-Path -Parent $javaExe)
        $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
        $javaMajor = Get-JavaMajorVersion $javaExe
    }

    if ($javaMajor -lt $requiredJavaMajor) {
        throw "JDK $requiredJavaMajor or newer is required. Found Java $javaMajor at $javaExe. Set JAVA_HOME to a newer JDK or install one under .tools\temurin-jdk-21."
    }
}

if (-not (Test-Path $mavenHome)) {
    $mvnCommand = Get-Command mvn -ErrorAction SilentlyContinue
    if ($null -eq $mvnCommand) {
        throw "Maven was not found. Install Maven, add mvn to PATH, or place Maven at $mavenHome."
    }

    & "$($mvnCommand.Source)" "-Dmaven.repo.local=$localRepository" @args
    if ($LASTEXITCODE -ne 0) {
        throw "Maven failed with exit code $LASTEXITCODE"
    }
    return
}

$env:PATH = "$mavenHome\bin;$env:PATH"
$classworldsJar = Get-ChildItem -Path "$mavenHome\boot" -Filter "plexus-classworlds-*.jar" | Select-Object -First 1
& "$javaExe" `
    "-classpath" "$($classworldsJar.FullName)" `
    "-Dclassworlds.conf=$mavenHome\bin\m2.conf" `
    "-Dmaven.home=$mavenHome" `
    "-Dmaven.multiModuleProjectDirectory=$projectRoot" `
    "-Dmaven.repo.local=$localRepository" `
    "org.codehaus.plexus.classworlds.launcher.Launcher" `
    @args
if ($LASTEXITCODE -ne 0) {
    throw "Maven failed with exit code $LASTEXITCODE"
}
