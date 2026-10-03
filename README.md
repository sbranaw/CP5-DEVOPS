# DimDim Pedidos

Aplicação web Java para gerenciar pedidos e itens, com CRUD, cálculo de valores, controle de status, persistência Azure SQL e monitoramento Application Insights.

[Aplicação publicada](https://dimdim-pedidos-261002-ce05.azurewebsites.net/pedidos)

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
