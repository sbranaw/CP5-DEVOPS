# How To — executar, implantar e comprovar

Comece pelos [requisitos e caminhos](guia-execucao.md). Execute os comandos na raiz do repositório, onde está pom.xml.

## Compilar e experimentar

```powershell
./mvnw.cmd verify
java -jar target/dimdim-pedidos-0.1.0.jar --spring.profiles.active=demo
```
Abra http://127.0.0.1:8080/pedidos e encerre com Ctrl+C.

## Configurar Azure CLI

Com JDK 21, PowerShell 7 e Azure CLI:
```powershell
az version
az login
New-Item -ItemType Directory -Path .local -Force
Copy-Item infra/azure.example.json .local/azure-config.json
```
**Para implantação existente, não sobrescreva o JSON já preenchido.** Para nova implantação, preencha assinatura, região permitida e nomes exclusivos. SQL Basic e logs podem consumir créditos.

## Implantar

```powershell
./infra/Deploy-Azure.ps1 -ConfigPath .local/azure-config.json
./infra/Deploy-Azure.ps1 -ConfigPath .local/azure-config.json -Execute
./infra/Test-AzureDeployment.ps1 -ConfigPath .local/azure-config.json
```
O primeiro comando mostra o plano, o segundo cria recursos/aplica DDL/configura acesso/testa/publica, e o terceiro verifica. Consulte [implantacao-azure.md](implantacao-azure.md) antes de usar -Resume: a retomada redefine senhas.

## Monitorar

```powershell
./infra/Enable-ApplicationInsights.ps1 -ConfigPath .local/azure-config.json
./infra/Enable-ApplicationInsights.ps1 -ConfigPath .local/azure-config.json -Execute
./infra/Test-ApplicationInsights.ps1 -ConfigPath .local/azure-config.json
./infra/Query-ApplicationInsights.ps1 -ConfigPath .local/azure-config.json -QueryPath monitoring/http-errors.kql
```
A configuração reinicia o site; a ingestão leva alguns minutos. Veja [application-insights.md](application-insights.md).

## Conferir dados

Veja o [guia de uso](uso.md). Para conferir um pedido no banco:
```powershell
./infra/Inspect-AzurePedido.ps1 -ConfigPath .local/azure-config.json -PedidoId 3
```
Substitua 3 pelo ID criado. O firewall precisa permitir o computador.
