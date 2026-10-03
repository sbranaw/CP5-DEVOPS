# Etapas de desenvolvimento

## Regra de acompanhamento

Ao concluir uma etapa, apresentar os arquivos, o comportamento implementado, as verificações e as limitações. Aguardar autorização explícita para a próxima etapa. Não publicar recursos Azure nem enviar código ao GitHub como parte de etapas anteriores.

## 1. Planejamento e banco — concluída

Entregas: estrutura prevista, escopo, modelo master-detail, regras e DDL de duas tabelas com PK e FK.

Verificação: revisão do DDL contra o modelo. Execução no Azure SQL fica pendente da etapa de infraestrutura.

## 2. Base Java e persistência — concluída

Criar o projeto Maven/Spring Boot, escolher versões compatíveis, configurar acesso por variáveis de ambiente e implementar entidades, repositórios e serviços. Validar regras e cálculos com testes significativos. Identificar e preparar o ambiente Java necessário para compilar e executar.

Critério: projeto compilando, testes das regras aprovados e instruções locais de execução disponíveis. A validação de integração com SQL Server requer um banco de teste acessível e deve ser identificada separadamente dos testes de unidade.

Resultado: compilação e empacotamento aprovados, 30 testes unitários aprovados. Mapeamento JPA e repositório implementados; integração com banco real ainda não executada. Consulte `resultado-etapa-2.md` e `execucao-local.md`.

## 3. Telas e CRUD — concluída

Implementar listagem, cadastro, consulta, edição e exclusão de pedidos e itens, mensagens de validação, confirmação de exclusão e total calculado.

Critério: demonstrar o fluxo completo em execução e verificar a persistência em SQL Server/Azure SQL. Se o banco não estiver disponível, registrar a limitação e não declarar essa integração validada.

Resultado: telas e operações implementadas e verificadas com H2 temporário, incluindo gravação em duas tabelas por JPA. Total de 44 testes aprovados. Sem SQL Server local ativo ou Azure SQL configurado nesta etapa, a validação exigida no banco de destino continua pendente e será feita com a infraestrutura da etapa 4. Consulte `resultado-etapa-3.md`.

Atualização após etapa 4: a integração com o Azure SQL real foi validada, resolvendo a pendência desta etapa. O relatório da etapa 3 preserva o estado da validação naquela ocasião.

## 4. Azure CLI e implantação — concluída

Preparar scripts para grupo de recursos, servidor SQL, banco, plano e Web App. Documentar aplicação do DDL, configuração e deploy pelo CLI. Executar na assinatura escolhida após autorização desta etapa e autenticação do usuário.

Critério: aplicação acessível na Azure com operações persistidas no banco. Registrar os comandos reproduzíveis e recursos utilizados.

Resultado: Web App Linux Java 21 publicado em HTTPS e Azure SQL Basic online em Brazil South. DDL aplicado, usuário restrito para CRUD e firewall limitado a IPs do computador e do Web App. Dois testes de integração aprovados no Azure SQL e pedido cadastrado pela interface conferido diretamente no banco. Consulte `implantacao-azure.md` e `resultado-etapa-4.md`.

## 5. Application Insights — concluída

Configurar a instrumentação adequada ao Java e gerar tráfego de demonstração.

Critério: comprovar recebimento de telemetria no recurso configurado.

Resultado: agente Java gerenciado habilitado no Web App, Application Insights vinculado ao Log Analytics e ingestão comprovada por consultas reais. Foram registradas 11 requisições à lista com HTTP 200, 16 chamadas SQL sem falhas e o 404 controlado. Um segundo 404 foi identificado como a verificação de inicialização do Azure. Consulte `application-insights.md` e `resultado-etapa-5.md`. A etapa 6 aguarda autorização.

## 6. GitHub, How To e apresentação — concluída

Concluir o guia de implantação, incluir fonte, DDL e scripts no repositório autorizado. Preparar roteiro para mostrar cada operação no banco e os comandos do CLI. Produzir o material para `<nome_grupo>_webapp.pdf` com identificação do grupo e evidências reais.

A implementação proposta usa telas. Se adicionarmos uma API, incluir também a documentação e os JSON das operações GET, POST, PUT e DELETE.

Critério: documentação reproduzível e material de entrega revisado. O representante do grupo realiza o upload no Teams.

Resultado: READM.MD completo, guia de execução com caminhos, How To, roteiro e PDF CP5-DEVOPS_webapp.pdf preparados com os três integrantes. Compilação verificada novamente com 44 testes locais aprovados; testes Azure opcionais não repetidos. O destino autorizado é https://github.com/sbranaw/CP5-DEVOPS. Consulte resultado-etapa-6.md para o estado da publicação. A apresentação do CRUD e o upload no Teams são realizados pelo grupo.

## Penalidades do enunciado

| Falha | Desconto |
| --- | ---: |
| Entrega fora do padrão | 0,5 ponto |
| Não mostrar cada operação no banco | 2 pontos |
| Não entregar Application Insights | 1 ponto |
| Sem How To no GitHub | 1,5 ponto |
| Sem DDL | 1 ponto |
| Apenas uma tabela | 2,5 pontos |

Também respeitar a proibição de reutilizar checkpoints anteriores/Sprint 3 e a exigência do Azure CLI.
