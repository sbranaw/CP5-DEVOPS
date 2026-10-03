[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$ConfigPath,
    [switch]$Execute,
    [switch]$Resume
)
. (Join-Path $PSScriptRoot 'Common.ps1')
$projectRoot = Split-Path $PSScriptRoot -Parent
$config = Read-DeploymentConfig $ConfigPath
$sub = $config.subscriptionId
$group = $config.resourceGroup
$jar = Join-Path $projectRoot 'target/dimdim-pedidos-0.1.0.jar'

Write-Host "Destino: $group / $($config.location) / assinatura $sub"
Write-Host "Web App: $($config.webApp), Java 21, plano $($config.appServiceSku)."
Write-Host "Azure SQL: $($config.sqlServer) / $($config.database), plano Basic (consome créditos)."
Write-Host 'Firewall SQL: IP deste computador e IPs de saída do Web App. Sem regra de liberação para todos os serviços Azure.'
if (!$Execute) {
    Write-Host 'Plano apenas. Nenhum recurso criado. Use -Execute para provisionar, aplicar o DDL, testar e publicar.'
    return
}

if (!(Test-Path -LiteralPath $jar)) { throw 'Compile e teste o projeto antes de executar a implantação.' }
$driver = Get-JdbcDriver $projectRoot
$account = Invoke-Az -Arguments @('account','show','--subscription',$sub) -Json
if ($account.state -ne 'Enabled') { throw 'A assinatura não está ativa.' }
$exists = Invoke-Az -Arguments @('group','exists','--name',$group,'--subscription',$sub) -Json
if ($exists) {
    $existing = Invoke-Az -Arguments @('group','show','--name',$group,'--subscription',$sub) -Json
    if (!$Resume -or !$existing.tags -or $existing.tags.dimdimDeployment -ne $config.deploymentId) {
        throw 'O grupo já existe. Para retomar um grupo criado por este roteiro, confirme a configuração e utilize -Resume.'
    }
    if ($existing.location -ne $config.location) { throw 'A região do grupo existente difere da configuração. Não será alterada.' }
}

$previous = @{}
foreach ($name in @('DB_URL','DB_USERNAME','DB_PASSWORD','DB_APP_PASSWORD','DIMDIM_AZURE_TEST')) {
    $previous[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}
$settingsFile = $null
$adminPassword = New-DeploymentPassword
$appPassword = New-DeploymentPassword
try {
    if (!$exists) {
        Invoke-Az -Arguments @('group','create','--name',$group,'--location',$config.location,'--subscription',$sub,
            '--tags',"dimdimDeployment=$($config.deploymentId)",'project=dimdim-pedidos')
    }
    Write-Host 'Criando plano e Web App...'
    Invoke-Az -Arguments @('appservice','plan','create','--name',$config.appServicePlan,'--resource-group',$group,
        '--location',$config.location,'--is-linux','--sku',$config.appServiceSku,'--subscription',$sub)
    Invoke-Az -Arguments @('webapp','create','--name',$config.webApp,'--resource-group',$group,
        '--plan',$config.appServicePlan,'--runtime','JAVA:21-java21','--https-only','true','--subscription',$sub)
    Invoke-Az -Arguments @('webapp','config','set','--name',$config.webApp,'--resource-group',$group,
        '--min-tls-version','1.2','--ftps-state','Disabled','--always-on','false','--subscription',$sub)

    Write-Host 'Criando servidor e banco Azure SQL...'
    $servers = @(Invoke-Az -Arguments @('sql','server','list','--resource-group',$group,'--subscription',$sub) -Json)
    if ($servers | Where-Object { $_ -and $_.name -eq $config.sqlServer }) {
        Invoke-Az -Arguments @('sql','server','update','--name',$config.sqlServer,'--resource-group',$group,
            '--admin-password',$adminPassword,'--subscription',$sub)
    } else {
        Invoke-Az -Arguments @('sql','server','create','--name',$config.sqlServer,'--resource-group',$group,
            '--location',$config.location,'--admin-user',$config.sqlAdmin,'--admin-password',$adminPassword,
            '--minimal-tls-version','1.2','--subscription',$sub)
    }
    Invoke-Az -Arguments @('sql','db','create','--name',$config.database,'--server',$config.sqlServer,
        '--resource-group',$group,'--service-objective',$config.sqlServiceObjective,'--backup-storage-redundancy','Local','--subscription',$sub)

    $clientIp = ([string](Invoke-RestMethod -Uri 'https://api.ipify.org')).Trim()
    if ($clientIp -notmatch '^\d{1,3}(\.\d{1,3}){3}$') { throw 'Não foi possível identificar o IPv4 local para o firewall.' }
    Invoke-Az -Arguments @('sql','server','firewall-rule','create','--name','DimDimClient','--server',$config.sqlServer,
        '--resource-group',$group,'--start-ip-address',$clientIp,'--end-ip-address',$clientIp,'--subscription',$sub)
    $web = Invoke-Az -Arguments @('webapp','show','--name',$config.webApp,'--resource-group',$group,'--subscription',$sub) -Json
    $addresses = @((($web.outboundIpAddresses + ',' + $web.possibleOutboundIpAddresses).Split(',') |
        Where-Object { $_ } | Sort-Object -Unique))
    if ($addresses.Count -eq 0) { throw 'Nenhum IP de saída disponível para o Web App.' }
    $index = 0
    foreach ($ip in $addresses) {
        Invoke-Az -Arguments @('sql','server','firewall-rule','create','--name',"DimDimWeb$index",'--server',$config.sqlServer,
            '--resource-group',$group,'--start-ip-address',$ip,'--end-ip-address',$ip,'--subscription',$sub)
        $index++
    }

    $jdbcUrl = "jdbc:sqlserver://$($config.sqlServer).database.windows.net:1433;databaseName=$($config.database);encrypt=true;trustServerCertificate=false;loginTimeout=30;"
    Set-DatabaseEnvironment $jdbcUrl $config.sqlAdmin $adminPassword
    $env:DB_APP_PASSWORD = $appPassword
    Write-Host 'Aplicando DDL e criando usuário de CRUD...'
    & java --class-path $driver (Join-Path $projectRoot 'database/SqlSetup.java') (Join-Path $projectRoot 'database/001-create-tables.sql')
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao preparar o esquema SQL.' }
    Set-DatabaseEnvironment $jdbcUrl 'dimdim_app' $appPassword
    $env:DIMDIM_AZURE_TEST = 'true'
    Write-Host 'Validando o mapeamento e CRUD com Azure SQL...'
    Push-Location $projectRoot
    try {
        & (Join-Path $projectRoot 'mvnw.cmd') '-B' '-Dspring.profiles.active=azure' '-Dtest=AzureSqlIntegrationTest' 'test'
        if ($LASTEXITCODE -ne 0) { throw 'Os testes com Azure SQL falharam. O deploy não será executado.' }
    } finally { Pop-Location }

    $settingsFile = Join-Path $projectRoot ('.local/appsettings-' + [guid]::NewGuid().ToString('N') + '.json')
    $settings = @{DB_URL=$jdbcUrl; DB_USERNAME='dimdim_app'; DB_PASSWORD=$appPassword;
        APP_TIMEZONE='America/Sao_Paulo'; SPRING_PROFILES_ACTIVE='azure'; JAVA_OPTS='-XX:MaxRAMPercentage=65.0'}
    [System.IO.File]::WriteAllText($settingsFile, ($settings | ConvertTo-Json), [System.Text.UTF8Encoding]::new($false))
    Invoke-Az -Arguments @('webapp','config','appsettings','set','--name',$config.webApp,'--resource-group',$group,
        '--settings',"@$settingsFile",'--subscription',$sub)
    Remove-Item -LiteralPath $settingsFile
    $settingsFile = $null
    Write-Host 'Publicando o JAR pelo Azure CLI...'
    Invoke-Az -Arguments @('webapp','deploy','--name',$config.webApp,'--resource-group',$group,
        '--src-path',$jar,'--type','jar','--timeout','600000','--subscription',$sub)
    Write-Host "Deploy enviado. Validar: https://$($web.defaultHostName)/pedidos"
} catch {
    Write-Warning "A implantação não foi concluída. Recursos já criados ficam preservados no grupo $group; nenhum recurso será excluído automaticamente."
    throw
} finally {
    if ($settingsFile -and (Test-Path -LiteralPath $settingsFile)) { Remove-Item -LiteralPath $settingsFile }
    foreach ($name in $previous.Keys) { [Environment]::SetEnvironmentVariable($name, $previous[$name], 'Process') }
    $adminPassword = $null
    $appPassword = $null
}
