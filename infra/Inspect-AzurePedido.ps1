[CmdletBinding()]
param([Parameter(Mandatory)][string]$ConfigPath, [Parameter(Mandatory)][ValidateRange(1,[long]::MaxValue)][long]$PedidoId)
. (Join-Path $PSScriptRoot 'Common.ps1')
$config = Read-DeploymentConfig $ConfigPath
$projectRoot = Split-Path $PSScriptRoot -Parent
$driver = Get-JdbcDriver $projectRoot
$previous = @{}
foreach ($key in @('DB_URL','DB_USERNAME','DB_PASSWORD')) { $previous[$key] = [Environment]::GetEnvironmentVariable($key, 'Process') }
try {
    # Captura as configurações em memória. Não imprimir o resultado desta consulta.
    $appSettings = Invoke-Az -Arguments @('webapp','config','appsettings','list','--name',$config.webApp,
        '--resource-group',$config.resourceGroup,'--subscription',$config.subscriptionId) -Json
    foreach ($key in @('DB_URL','DB_USERNAME','DB_PASSWORD')) {
        $value = @($appSettings | Where-Object { $_.name -eq $key })
        if ($value.Count -ne 1) { throw "Configuração de banco ausente: $key" }
        [Environment]::SetEnvironmentVariable($key, $value[0].value, 'Process')
    }
    & java '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' --class-path $driver (Join-Path $projectRoot 'database/SqlInspect.java') $PedidoId
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao consultar o pedido no banco.' }
} finally {
    foreach ($key in $previous.Keys) { [Environment]::SetEnvironmentVariable($key, $previous[$key], 'Process') }
    $appSettings = $null
    $value = $null
}
