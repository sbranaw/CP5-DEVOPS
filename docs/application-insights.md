# Application Insights para DimDim Pedidos

## Configuração

O monitoramento usa o agente Java gerenciado pelo App Service Linux. Não requer incluir SDK no código da aplicação nem empacotar outro agente no JAR. A conexão de telemetria fica nas configurações do Web App.

Recursos derivados do nome do Web App:

- Application Insights: `ai-<nome-do-webapp>`.
- Log Analytics workspace: `law-<nome-do-webapp>`.
- Mesma região e grupo de recursos do projeto.

Configurações previstas: retenção de 30 dias, ingestão com limite diário de 0,1 GB no workspace, papel da aplicação `dimdim-pedidos` e amostragem de 100% para o tráfego pequeno da demonstração.

O limite diário ajuda a controlar volume, mas não representa garantia de custo máximo e pode interromper a coleta ao atingir a cota. A ingestão e a retenção seguem a cobrança aplicável à assinatura. Não há aumento automático de planos neste roteiro.

## Habilitar

Revisar o plano:

```powershell
./infra/Enable-ApplicationInsights.ps1 -ConfigPath .local/azure-config.json
```

Aplicar ao Web App já implantado:

```powershell
./infra/Enable-ApplicationInsights.ps1 -ConfigPath .local/azure-config.json -Execute
```

O roteiro confirma que o grupo pertence à implantação, cria/configura o workspace, cria/configura Application Insights por `az rest`, define as configurações do agente e reinicia o Web App. Esse reinício pode tornar o site temporariamente indisponível durante a inicialização.

Variáveis configuradas no Web App:

| Nome | Finalidade |
| --- | --- |
| APPLICATIONINSIGHTS_CONNECTION_STRING | Destino da telemetria, armazenado no serviço |
| ApplicationInsightsAgent_EXTENSION_VERSION | `~3`, agente gerenciado para Linux |
| APPLICATIONINSIGHTS_CONFIGURATION_CONTENT | Papel da aplicação, versão e amostragem |

As configurações existentes de acesso ao banco são preservadas. Arquivos temporários com a conexão de telemetria são removidos após a operação. Não publicar connection strings no repositório nem em capturas de tela.

## Comprovar que a telemetria chegou

```powershell
./infra/Test-ApplicationInsights.ps1 -ConfigPath .local/azure-config.json
```

O teste:

1. Aguarda a aplicação responder após o reinício.
2. Faz dez consultas de leitura à lista de pedidos, sem alterar dados.
3. Consulta `/pedidos/0` para gerar uma resposta 404 controlada.
4. Consulta Log Analytics por KQL, com autenticação do Azure CLI, sem imprimir tokens.
5. Só confirma a validação após encontrar requisições, dependências SQL e a resposta 404 no recurso indicado.

O recebimento dos dados pode levar alguns minutos. O resultado fica em `.local/insights-validation.json`, sem credenciais. Se a espera expirar, isso significa que a ingestão ainda não foi comprovada; o roteiro não declara sucesso somente porque as variáveis estão configuradas.

## Consultas de telemetria

Abra o workspace do projeto no portal Azure e selecione **Logs**. As consultas estão em:

- `monitoring/requests.kql`: requisições, respostas HTTP, falhas e duração média.
- `monitoring/sql-dependencies.kql`: chamadas SQL e duração média.
- `monitoring/http-errors.kql`: origem, endereço e código das respostas HTTP de erro.
- `monitoring/validation.kql`: verificação conjunta usada pelo roteiro, filtrada pelo ID do recurso Application Insights.

Para executar uma consulta pelo CLI e retornar os registros em JSON:

```powershell
./infra/Query-ApplicationInsights.ps1 -ConfigPath .local/azure-config.json -QueryPath monitoring/http-errors.kql
```

O roteiro substitui o marcador do recurso nas consultas que o utilizam e remove o arquivo temporário da consulta ao terminar.

A falha HTTP 404 gerada pelo teste é intencional. Ela não representa falha da conexão SQL. Uma resposta 404 também não exige que exista uma linha em AppExceptions, porque a aplicação trata o pedido inexistente e responde ao usuário.

A telemetria pode registrar requisições para `/robots933456.txt`, feitas pelo Azure durante a inicialização. Esse é o caminho padrão de aquecimento do App Service; uma resposta 404 é aceita nessa verificação quando não há códigos específicos configurados. Veja a [referência de configurações do App Service](https://learn.microsoft.com/en-us/azure/app-service/reference-app-settings).

## Inspecionar a telemetria

1. Mostrar o recurso Application Insights vinculado ao workspace.
2. Acessar a aplicação publicada e consultar os pedidos.
3. Mostrar os registros de requisições e os tempos de resposta.
4. Mostrar as dependências SQL, comprovando as chamadas ao banco.
5. Mostrar a resposta 404 controlada e explicar a origem do teste.

A configuração não cria alertas, notificações ou monitoramento agendado.

## Referências oficiais

- [Monitoramento do App Service e agente Java gerenciado](https://learn.microsoft.com/en-us/azure/app-service/monitor-app-service).
- [Configuração do agente Application Insights para Java](https://learn.microsoft.com/en-us/azure/azure-monitor/app/java-standalone-config).
- [Application Insights com workspace](https://learn.microsoft.com/en-us/azure/azure-monitor/app/create-workspace-resource).
- [Autenticação da API de consultas de logs](https://learn.microsoft.com/en-us/azure/azure-monitor/logs/api/access-api).
- [Limite diário de ingestão](https://learn.microsoft.com/en-us/azure/azure-monitor/logs/daily-cap).
