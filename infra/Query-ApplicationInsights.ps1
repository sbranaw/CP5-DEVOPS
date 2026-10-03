[CmdletBinding()]
param([Parameter(Mandatory)][string]$ConfigPath, [Parameter(Mandatory)][string]$QueryPath)
. (Join-Path $PSScriptRoot 'Common.ps1')
$config = Read-DeploymentConfig $ConfigPath
$projectRoot = Split-Path $PSScriptRoot -Parent
$workspaceName = 'law-' + $config.webApp
$componentId = "/subscriptions/$($config.subscriptionId)/resourceGroups/$($config.resourceGroup)/providers/Microsoft.Insights/components/ai-$($config.webApp)"
$workspace = Invoke-Az -Arguments @('monitor','log-analytics','workspace','show','--name',$workspaceName,
    '--resource-group',$config.resourceGroup,'--subscription',$config.subscriptionId) -Json
$query = (Get-Content -LiteralPath $QueryPath -Raw).Replace('__COMPONENT_ID__',$componentId)
$queryFile = Join-Path $projectRoot ('.local/insights-query-' + [guid]::NewGuid().ToString('N') + '.json')
try {
    [System.IO.File]::WriteAllText($queryFile, (@{query=$query;timespan='PT1H'} | ConvertTo-Json), [System.Text.UTF8Encoding]::new($false))
    $response = Invoke-Az -Arguments @('rest','--method','post','--resource','https://api.loganalytics.io',
        '--url',"https://api.loganalytics.azure.com/v1/workspaces/$($workspace.customerId)/query",'--body',"@$queryFile",'--subscription',$config.subscriptionId) -Json
    $table = $response.tables[0]
    $rows = @()
    foreach ($row in $table.rows) {
        $entry = [ordered]@{}
        for ($index = 0; $index -lt $table.columns.Count; $index++) { $entry[$table.columns[$index].name]=$row[$index] }
        $rows += [pscustomobject]$entry
    }
    ConvertTo-Json -InputObject $rows -Depth 8
} finally { if (Test-Path -LiteralPath $queryFile) { Remove-Item -LiteralPath $queryFile } }
