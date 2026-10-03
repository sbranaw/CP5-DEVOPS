# Resultado da etapa 5 — Application Insights

Etapa concluída em 2 de outubro de 2026. A consulta de validação terminou às 21:49:57, horário de São Paulo. A próxima etapa aguarda autorização.

## O que mudou

- Application Insights `ai-dimdim-pedidos-261002-ce05` criado e vinculado ao workspace `law-dimdim-pedidos-261002-ce05`, no grupo `rg-dimdim-261002-ce05` e região Brazil South.
- Agente Java gerenciado habilitado no Web App, com configuração `~3`, papel `dimdim-pedidos` e amostragem de 100% para a demonstração.
- Workspace configurado com retenção de 30 dias e limite diário de ingestão de 0,1 GB. Esse limite controla volume, sem garantir teto monetário.
- Scripts de habilitação, validação e consulta adicionados em `infra`, e consultas KQL em `monitoring`.
- Guia de configuração e demonstração disponível em [application-insights.md](application-insights.md).

Não foi necessário alterar o código Java. O Web App foi reiniciado para aplicar o agente e voltou a responder. As configurações de banco foram preservadas.

## Resultado observado no Azure

| Registro na janela consultada | Quantidade | Falhas | Duração média |
| --- | ---: | ---: | ---: |
| GET /pedidos, HTTP 200 | 11 | 0 | 116,72 ms |
| Dependências SQL do banco DimDim | 16 | 0 | 57,72 ms |
| GET /pedidos/0, HTTP 404 controlado | 1 | 1 | 70,63 ms |
| Aquecimento do App Service, HTTP 404 | 1 | 1 | 302,45 ms |

Os números representam os eventos recebidos na janela de uma hora da consulta, não uma medição contínua de desempenho. A validação fez dez leituras da lista e uma leitura inicial para confirmar disponibilidade. Não criou, alterou ou excluiu pedidos.

O 404 de `/pedidos/0` foi provocado para demonstrar a coleta de respostas de erro. O outro veio de `http://169.254.129.2/robots933456.txt`, caminho padrão solicitado pelo App Service durante a inicialização. O Azure aceita respostas como 404 nessa verificação quando não foram configurados códigos específicos de aquecimento. [Referência Microsoft](https://learn.microsoft.com/en-us/azure/app-service/reference-app-settings).

## Evidências e reprodução

- [telemetria-etapa-5.json](telemetria-etapa-5.json): resultado agregado da validação, sem credenciais.
- [respostas-http-etapa-5.json](respostas-http-etapa-5.json): endereços e horários dos dois registros HTTP 404.
- `infra/Enable-ApplicationInsights.ps1`: plano e aplicação da configuração pelo Azure CLI.
- `infra/Test-ApplicationInsights.ps1`: geração de tráfego e confirmação da ingestão.
- `infra/Query-ApplicationInsights.ps1`: consulta dos registros pelo Azure CLI.

A validação somente retornou sucesso depois de encontrar requisições, chamadas SQL e o 404 no recurso correto. A ingestão inicial levou alguns minutos. A coleta real comprova a instrumentação; configurar variáveis isoladamente não teria sido suficiente.

[Abrir Application Insights no portal Azure](https://portal.azure.com/#resource/subscriptions/7ffd9a93-f463-4310-b7bd-d9cd1b458836/resourceGroups/rg-dimdim-261002-ce05/providers/Microsoft.Insights/components/ai-dimdim-pedidos-261002-ce05/overview).

## Limites e próxima etapa

Não foram criados alertas ou notificações. Não houve alteração de código Java nesta etapa; as verificações foram feitas contra a aplicação e a telemetria reais no Azure. Os resultados de testes Java anteriores permanecem documentados nas etapas 3 e 4.

A etapa 6 será GitHub, How To e material de apresentação/entrega final, após autorização do usuário.
