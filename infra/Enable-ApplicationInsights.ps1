[CmdletBinding()]
param([Parameter(Mandatory)][string]$ConfigPath, [switch]$Execute)
. (Join-Path $PSScriptRoot 'Common.ps1')
$config = Read-DeploymentConfig $ConfigPath
$projectRoot = Split-Path $PSScriptRoot -Parent
$workspaceName = 'law-' + $config.webApp
$componentName = 'ai-' + $config.webApp
$workspaceId = "/subscriptions/$($config.subscriptionId)/resourceGroups/$($config.resourceGroup)/providers/Microsoft.OperationalInsights/workspaces/$workspaceName"
$componentId = "/subscriptions/$($config.subscriptionId)/resourceGroups/$($config.resourceGroup)/providers/Microsoft.Insights/components/$componentName"
Write-Host "Application Insights: $componentName; workspace: $workspaceName; região: $($config.location)."
Write-Host 'Retenção de 30 dias; limite de ingestão do workspace: 0,1 GB/dia. Ingestão pode consumir créditos.'
Write-Host 'Configura o agente Java gerenciado e reinicia o Web App. Não modifica o banco nem o código Java.'
if (!$Execute) { Write-Host 'Plano apenas. Use -Execute para aplicar.'; return }

$group = Invoke-Az -Arguments @('group','show','--name',$config.resourceGroup,'--subscription',$config.subscriptionId) -Json
if (!$group.tags -or $group.tags.dimdimDeployment -ne $config.deploymentId) { throw 'O grupo não pertence à implantação indicada.' }
$web = Invoke-Az -Arguments @('webapp','show','--name',$config.webApp,'--resource-group',$config.resourceGroup,'--subscription',$config.subscriptionId) -Json
$resourceFile = $null
$settingsFile = $null
try {
    foreach ($provider in @('Microsoft.OperationalInsights','Microsoft.Insights')) {
        $state = Invoke-Az -Arguments @('provider','show','--namespace',$provider,'--query','registrationState','--subscription',$config.subscriptionId) -Json
        if ($state -ne 'Registered') {
            Write-Host "Registrando provedor $provider..."
            Invoke-Az -Arguments @('provider','register','--namespace',$provider,'--wait','--subscription',$config.subscriptionId)
        }
    }
    Write-Host 'Criando/configurando o workspace de logs...'
    Invoke-Az -Arguments @('monitor','log-analytics','workspace','create','--name',$workspaceName,
        '--resource-group',$config.resourceGroup,'--location',$config.location,'--sku','PerGB2018',
        '--retention-time','30','--quota','0.1','--subscription',$config.subscriptionId)
    $tags = @{project='dimdim-pedidos';dimdimDeployment=$config.deploymentId}
    $tags['hidden-link:' + $web.id] = 'Resource'
    $body = @{location=$config.location;kind='web';tags=$tags;
        properties=@{Application_Type='web';WorkspaceResourceId=$workspaceId;IngestionMode='LogAnalytics';RetentionInDays=30;Request_Source='rest'}}
    $resourceFile = Join-Path $projectRoot ('.local/insights-resource-' + [guid]::NewGuid().ToString('N') + '.json')
    [System.IO.File]::WriteAllText($resourceFile, ($body | ConvertTo-Json -Depth 8), [System.Text.UTF8Encoding]::new($false))
    Write-Host 'Criando/configurando Application Insights...'
    # az rest evita depender de extensão adicional do Azure CLI.
    $component = Invoke-Az -Arguments @('rest','--method','put','--url',
        "https://management.azure.com${componentId}?api-version=2020-02-02",'--body',"@$resourceFile",'--subscription',$config.subscriptionId) -Json
    if (!$component.properties.ConnectionString) { throw 'O recurso não retornou a conexão de telemetria.' }
    $agentConfig = @{role=@{name='dimdim-pedidos'};sampling=@{percentage=100};customDimensions=@{'service.version'='0.1.0'}}
    $settings = @{
        APPLICATIONINSIGHTS_CONNECTION_STRING=$component.properties.ConnectionString
        ApplicationInsightsAgent_EXTENSION_VERSION='~3'
        APPLICATIONINSIGHTS_CONFIGURATION_CONTENT=($agentConfig | ConvertTo-Json -Depth 6 -Compress)
    }
    $settingsFile = Join-Path $projectRoot ('.local/insights-settings-' + [guid]::NewGuid().ToString('N') + '.json')
    [System.IO.File]::WriteAllText($settingsFile, ($settings | ConvertTo-Json), [System.Text.UTF8Encoding]::new($false))
    Write-Host 'Habilitando o agente Java...'
    Invoke-Az -Arguments @('webapp','config','appsettings','set','--name',$config.webApp,
        '--resource-group',$config.resourceGroup,'--settings',"@$settingsFile",'--subscription',$config.subscriptionId)
    Invoke-Az -Arguments @('webapp','restart','--name',$config.webApp,'--resource-group',$config.resourceGroup,'--subscription',$config.subscriptionId)
    $result = @{applicationInsights=$componentName;applicationInsightsResourceId=$componentId;
        workspace=$workspaceName;workspaceResourceId=$workspaceId;agent='App Service Java ~3';dailyQuotaGb=0.1;retentionDays=30;
        configuredAtUtc=[DateTime]::UtcNow.ToString('o')}
    [System.IO.File]::WriteAllText((Join-Path $projectRoot '.local/insights-configuration.json'),
        ($result | ConvertTo-Json), [System.Text.UTF8Encoding]::new($false))
    Write-Host 'Configuração aplicada. Execute Test-ApplicationInsights.ps1 para comprovar a ingestão.'
} finally {
    foreach ($file in @($settingsFile,$resourceFile)) {
        if ($file -and (Test-Path -LiteralPath $file)) { Remove-Item -LiteralPath $file }
    }
    $component = $null
    $settings = $null
}
