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
    $phase = Read-Host -Prompt "Enter the Maven lifecycle phase"
    if ($phase -eq "deploy")
    {
        $securePassword = Read-Host -Prompt "Enter the Sonatype password" -AsSecureString
        $password = (New-Object System.Net.NetworkCredential("", $securePassword)).Password
        mvn clean $phase "-Dsonatype.password=$password"
    }
    else
    {
        mvn clean $phase
    }
}
finally
{
    $env:JAVA_HOME = $javaHome

}
Read-Host -Prompt "Press ENTER to exit"