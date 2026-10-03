[CmdletBinding()]
param([Parameter(Mandatory)][string]$ConfigPath, [ValidateRange(1,30)][int]$Attempts = 20)
. (Join-Path $PSScriptRoot 'Common.ps1')
$config = Read-DeploymentConfig $ConfigPath
$projectRoot = Split-Path $PSScriptRoot -Parent
$componentName = 'ai-' + $config.webApp
$workspaceName = 'law-' + $config.webApp
$componentId = "/subscriptions/$($config.subscriptionId)/resourceGroups/$($config.resourceGroup)/providers/Microsoft.Insights/components/$componentName"
$workspace = Invoke-Az -Arguments @('monitor','log-analytics','workspace','show','--name',$workspaceName,
    '--resource-group',$config.resourceGroup,'--subscription',$config.subscriptionId) -Json
$web = Invoke-Az -Arguments @('webapp','show','--name',$config.webApp,'--resource-group',$config.resourceGroup,
    '--subscription',$config.subscriptionId) -Json
$url = "https://$($web.defaultHostName)/pedidos"
Write-Host 'Verificando a aplicação e gerando requisições de demonstração...'
$healthy = $false
for ($attempt = 1; $attempt -le 18; $attempt++) {
    try {
        $page = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 20
        if ($page.StatusCode -eq 200 -and $page.Content.Contains('Seus pedidos, organizados.')) { $healthy=$true; break }
    } catch { Write-Host "Aguardando aplicação após reinício ($attempt/18)..." }
    if ($attempt -lt 18) { Start-Sleep -Seconds 10 }
}
if (!$healthy) { throw 'A aplicação não ficou disponível após habilitar o monitoramento.' }
for ($index = 1; $index -le 10; $index++) {
    $null = Invoke-WebRequest -Uri ($url + '?monitoring=validation') -UseBasicParsing -TimeoutSec 30
}
# ID zero é sempre inválido no serviço. Gera 404 controlado, sem modificar dados.
$controlled404 = $false
try { $null = Invoke-WebRequest -Uri ($url + '/0') -UseBasicParsing -TimeoutSec 30 }
catch {
    if ($_.Exception.Response -and [int]$_.Exception.Response.StatusCode -eq 404) { $controlled404=$true }
    else { throw }
}
if (!$controlled404) { throw 'A requisição de erro controlado não retornou 404.' }

$queryTemplate = Get-Content -LiteralPath (Join-Path $projectRoot 'monitoring/validation.kql') -Raw
$query = $queryTemplate.Replace('__COMPONENT_ID__',$componentId)
$queryFile = Join-Path $projectRoot ('.local/insights-query-' + [guid]::NewGuid().ToString('N') + '.json')
[System.IO.File]::WriteAllText($queryFile, (@{query=$query;timespan='PT1H'} | ConvertTo-Json), [System.Text.UTF8Encoding]::new($false))
try {
    for ($attempt = 1; $attempt -le $Attempts; $attempt++) {
        $response = Invoke-Az -Arguments @('rest','--method','post','--resource','https://api.loganalytics.io',
            '--url',"https://api.loganalytics.azure.com/v1/workspaces/$($workspace.customerId)/query",'--body',"@$queryFile",'--subscription',$config.subscriptionId) -Json
        $table = $response.tables[0]
        $entries = @()
        foreach ($row in $table.rows) {
            $entry = [ordered]@{}
            for ($column = 0; $column -lt $table.columns.Count; $column++) { $entry[$table.columns[$column].name] = $row[$column] }
            $entries += [pscustomobject]$entry
        }
        $requests = @($entries | Where-Object { $_.Category -eq 'request' })
        $sql = @($entries | Where-Object { $_.Category -eq 'dependency' -and $_.Label -match 'SQL|JDBC' })
        $error404 = @($requests | Where-Object { $_.ResultCode -eq '404' })
        if ($requests.Count -gt 0 -and $sql.Count -gt 0 -and $error404.Count -gt 0) {
            $result = [ordered]@{applicationInsights=$componentName;workspace=$workspaceName;url=$url;
                verifiedAtUtc=[DateTime]::UtcNow.ToString('o');window='última hora';controlled404=$controlled404;telemetry=$entries}
            [System.IO.File]::WriteAllText((Join-Path $projectRoot '.local/insights-validation.json'),
                ($result | ConvertTo-Json -Depth 8), [System.Text.UTF8Encoding]::new($false))
            $entries | Format-Table -AutoSize
            Write-Host 'Ingestão comprovada: requisições, dependências SQL e resposta 404 controlada.'
            return
        }
        Write-Host "Aguardando ingestão de telemetria ($attempt/$Attempts)..."
        if ($attempt -lt $Attempts) { Start-Sleep -Seconds 30 }
    }
    throw 'Telemetria completa não apareceu no período de espera. Configuração não equivale a ingestão validada; consultar logs e reexecutar a validação.'
} finally {
    if (Test-Path -LiteralPath $queryFile) { Remove-Item -LiteralPath $queryFile }
}
