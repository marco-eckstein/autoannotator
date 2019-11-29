if (-not(Test-Path env:JAVA_HOME))
{
    Write-Host "You must set environment variable JAVA_HOME."
    exit 1
}
if (-not(Test-Path env:JAVA8_HOME))
{
    Write-Host "You must set environment variable JAVA8_HOME."
    exit 1
}

$javaHome = $env:JAVA_HOME
try
{
    $env:JAVA_HOME = $env:JAVA8_HOME
    mvn clean install
}
finally
{
    $env:JAVA_HOME = $javaHome

}
Read-Host -Prompt "Press ENTER to exit"