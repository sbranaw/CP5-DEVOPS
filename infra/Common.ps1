Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Read-DeploymentConfig([string]$ConfigPath) {
    $config = Get-Content -LiteralPath $ConfigPath -Raw | ConvertFrom-Json
    foreach ($key in @('subscriptionId','deploymentId','location','resourceGroup','appServicePlan','webApp','sqlServer','database','sqlAdmin','appServiceSku','sqlServiceObjective')) {
        if ([string]::IsNullOrWhiteSpace($config.$key)) { throw "Configuração ausente: $key" }
    }
    if ($config.subscriptionId -eq '00000000-0000-0000-0000-000000000000' -or $config.subscriptionId -notmatch '^[0-9a-fA-F-]{36}$') {
        throw 'Informe uma assinatura válida no arquivo de configuração.'
    }
    foreach ($key in @('deploymentId','location','resourceGroup','appServicePlan','webApp','sqlServer','database','sqlAdmin')) {
        if ($config.$key -notmatch '^[a-z0-9][a-z0-9-]{1,58}[a-z0-9]$') { throw "Nome inválido: $key" }
    }
    if ($config.appServiceSku -notin @('F1','B1')) { throw 'Use F1 ou B1 explicitamente. Não há aumento automático de plano.' }
    if ($config.sqlServiceObjective -ne 'Basic') { throw 'Este roteiro foi preparado para Azure SQL Basic.' }
    return $config
}

function Invoke-Az([string[]]$Arguments, [switch]$Json) {
    $format = if ($Json) { 'json' } else { 'none' }
    $result = & az @Arguments --only-show-errors --output $format
    if ($LASTEXITCODE -ne 0) { throw "Azure CLI falhou em: az $($Arguments[0]) $($Arguments[1]). Consulte o erro exibido acima." }
    if ($Json) { return ($result -join "`n" | ConvertFrom-Json) }
}

function New-DeploymentPassword {
    $bytes = [byte[]]::new(24)
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $generator.GetBytes($bytes) } finally { $generator.Dispose() }
    return 'Dd9!' + ([System.BitConverter]::ToString($bytes).Replace('-', ''))
}

function Get-JdbcDriver([string]$ProjectRoot) {
    $driverFolder = Join-Path $ProjectRoot '.local/maven-repository/com/microsoft/sqlserver/mssql-jdbc'
    $drivers = @(Get-ChildItem -LiteralPath $driverFolder -Recurse -File -Filter 'mssql-jdbc-*.jar' | Where-Object { $_.Name -notmatch '(sources|javadoc)' })
    if ($drivers.Count -ne 1) { throw 'Compile com ./mvnw.cmd verify para disponibilizar uma única versão do driver JDBC neste projeto.' }
    return $drivers[0].FullName
}

function Set-DatabaseEnvironment([string]$Url, [string]$Username, [string]$Password) {
    $env:DB_URL = $Url
    $env:DB_USERNAME = $Username
    $env:DB_PASSWORD = $Password
}
