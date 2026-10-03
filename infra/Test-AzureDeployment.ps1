[CmdletBinding()]
param([Parameter(Mandatory)][string]$ConfigPath)
. (Join-Path $PSScriptRoot 'Common.ps1')
$config = Read-DeploymentConfig $ConfigPath
$web = Invoke-Az -Arguments @('webapp','show','--name',$config.webApp,'--resource-group',$config.resourceGroup,
    '--subscription',$config.subscriptionId) -Json
$db = Invoke-Az -Arguments @('sql','db','show','--name',$config.database,'--server',$config.sqlServer,
    '--resource-group',$config.resourceGroup,'--subscription',$config.subscriptionId) -Json
$runtime = Invoke-Az -Arguments @('webapp','config','show','--name',$config.webApp,'--resource-group',$config.resourceGroup,
    '--subscription',$config.subscriptionId) -Json
if ($db.status -ne 'Online') { throw "Banco ainda não está online: $($db.status)" }
if ($runtime.linuxFxVersion -ne 'JAVA|21-java21') { throw 'Runtime do Web App difere de Java 21.' }
if (!$web.httpsOnly) { throw 'O Web App precisa exigir HTTPS.' }
$url = "https://$($web.defaultHostName)/pedidos"
$available = $false
for ($attempt = 1; $attempt -le 24; $attempt++) {
    try {
        $page = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 20
        if ($page.StatusCode -eq 200 -and $page.Content.Contains('Seus pedidos, organizados.') -and
            !$page.Content.Contains('Demonstração local:')) { $available = $true; break }
    } catch {
        Write-Host "Aguardando inicialização da aplicação ($attempt/24)..."
    }
    if ($attempt -lt 24) { Start-Sleep -Seconds 10 }
}
if (!$available) { throw "A aplicação não respondeu corretamente em $url. Consulte os logs do Web App." }
$result = [ordered]@{
    resourceGroup = $config.resourceGroup
    url = $url
    webAppState = $web.state
    javaRuntime = $runtime.linuxFxVersion
    httpsOnly = $web.httpsOnly
    databaseStatus = $db.status
    databaseSku = $db.sku.name
    checkedAtUtc = [DateTime]::UtcNow.ToString('o')
}
$result | ConvertTo-Json
$projectRoot = Split-Path $PSScriptRoot -Parent
[System.IO.File]::WriteAllText((Join-Path $projectRoot '.local/azure-validation.json'),
    ($result | ConvertTo-Json), [System.Text.UTF8Encoding]::new($false))
