# DimDim Pedidos

Aplicação web Java para gerenciar pedidos e itens, com CRUD, cálculo de valores, controle de status, persistência Azure SQL e monitoramento Application Insights.

[Aplicação publicada e validada](https://dimdim-pedidos-261002-ce05-h8gbb9a4hghjch3.brazilsouth-01.azurewebsites.net/pedidos)

O banco Azure SQL foi recriado e validado com `dbo.pedido` e `dbo.item_pedido`, PKs, FK e exclusão em cascata. O CRUD da aplicação e a telemetria SQL no Application Insights foram conferidos. Um segundo Web App, `dimdim-pedidos-261002-ce05-cli`, foi criado pelo Azure CLI; sua configuração e publicação ainda precisam de confirmação. Consulte o [ambiente Azure atual e comandos de criação/deploy](docs/azure-atual.md).

Para publicar no destino CLI usando o banco existente, no Cloud Shell (Bash), com Java 21:

```bash
bash ./mvnw verify
bash infra/Deploy-ExistingAzure.sh
bash infra/Deploy-ExistingAzure.sh --execute --deploy
```

O primeiro comando compila e testa; o segundo mostra o plano; o terceiro configura/publica. O roteiro reutiliza as credenciais da aplicação de origem e não redefine senhas SQL.

## Tecnologias

Java 21 · Spring Boot 4.0.8 · Maven Wrapper 3.9.15 · Thymeleaf · Spring Data JPA · Azure SQL · Azure App Service · Application Insights.

## Executar localmente

Requisitos: **JDK 21**, Git e internet na primeira compilação. Maven está incluído pelo Wrapper.

```powershell
git clone https://github.com/sbranaw/CP5-DEVOPS.git
cd CP5-DEVOPS
./mvnw.cmd verify
java -jar target/dimdim-pedidos-0.1.0.jar --spring.profiles.active=demo
```

Abra http://127.0.0.1:8080/pedidos. O perfil demo utiliza H2 em memória; os dados são apagados ao encerrar com Ctrl+C. Em Linux/macOS, use `sh ./mvnw verify` para compilar.

Para persistência Azure SQL, configure DB_URL, DB_USERNAME e DB_PASSWORD e execute o JAR sem o perfil demo. Consulte o [guia de execução](docs/guia-execucao.md).

## Funcionalidades

- CRUD de pedidos e itens.
- Estados RASCUNHO, CONFIRMADO e CANCELADO.
- Edição apenas em rascunho; confirmação exige pelo menos um item.
- Subtotal e total calculados com BigDecimal.
- Exclusão de pedido com remoção de seus itens.
- Formulários HTML com validação e proteção CSRF.

## Documentação

| Documento | Conteúdo |
| --- | --- |
| [Visão técnica completa](READM.MD) | Arquitetura, rotas e organização do código |
| [Guia de execução](docs/guia-execucao.md) | Requisitos, configuração, comandos e diagnósticos |
| [How To](docs/how-to.md) | Compilação, implantação e validação |
| [Modelo e regras](docs/modelo-e-regras.md) | Tabelas, relacionamento e regras de negócio |
| [Implantação Azure](docs/implantacao-azure.md) | Provisionamento e publicação pelo Azure CLI |
| [Azure atual](docs/azure-atual.md) | Banco recriado, estado confirmado e criação/deploy no Cloud Shell |
| [Application Insights](docs/application-insights.md) | Instrumentação e consultas de telemetria |
| [Uso da aplicação](docs/uso.md) | Fluxo de pedidos e conferência de dados |

O DDL está em database/001-create-tables.sql. Os scripts de infraestrutura ficam em infra e as consultas KQL em monitoring.

## Testes

`./mvnw.cmd verify` executa os testes de regras e interface com banco H2 temporário. Os testes Azure SQL são opcionais: exigem credenciais e DIMDIM_AZURE_TEST=true. Veja o [guia de implantação](docs/implantacao-azure.md).

## Escopo

Esta versão utiliza páginas e formulários HTML, sem API REST JSON ou autenticação. Não há pagamentos, impostos, descontos ou cadastro separado de produtos. Credenciais são fornecidas pelo ambiente; .local, target e arquivos .env não são versionados.

## Autores

- Gabriel Sbrana Campos — RM565849
- Thiago Rodrigues da Mota — RM563650
- Moisés Waidemann Molinillo — RM563719
