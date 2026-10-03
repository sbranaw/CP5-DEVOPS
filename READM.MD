# DimDim Pedidos — explicação de ponta a ponta

Aplicação Java de gestão de pedidos com telas em português, CRUD de pedidos e itens, Azure SQL e monitoramento Application Insights.

**Grupo CP5-DEVOPS**
- Gabriel Sbrana Campos — RM565849
- Thiago Rodrigues da Mota — RM563650
- Moisés Waidemann Molinillo — RM563719

**Site:** https://dimdim-pedidos-261002-ce05.azurewebsites.net/pedidos
**Repositório:** https://github.com/sbranaw/CP5-DEVOPS

## Comece aqui

O [guia de execução](docs/guia-execucao.md) apresenta requisitos, caminhos e comandos. O [How To](docs/how-to.md) explica implantação e validação. O [guia de uso](docs/uso.md) descreve as operações e a conferência dos dados.

## Tecnologias

| Parte | Tecnologia |
| --- | --- |
| Linguagem | Java 21 |
| Framework | Spring Boot 4.0.8 |
| Build | Maven Wrapper 3.9.15 |
| Interface | Thymeleaf, HTML e CSS |
| Persistência | Spring Data JPA / Hibernate e JDBC Microsoft |
| Banco publicado | Azure SQL Database Basic |
| Hospedagem | Azure App Service Linux Java 21, plano F1 |
| Monitoramento | Application Insights e Log Analytics |
| Demonstração local | H2 em memória, perfil explícito demo |

## Funcionamento de ponta a ponta

O navegador envia um formulário ao PedidoController. O controlador valida a entrada e chama PedidoService. O serviço verifica as regras e executa a alteração em uma transação. PedidoRepository acessa Pedido e ItemPedido por JPA; o banco mantém as chaves e o relacionamento. A resposta é uma página Thymeleaf ou um redirecionamento com mensagem. No Azure, o agente Java coleta requisições e dependências SQL.

O sistema usa páginas HTML e formulários; não possui API REST JSON nesta versão. As alterações usam POST e token CSRF. A aplicação não exige login.

## Fluxo do usuário

1. Abra /pedidos e crie um pedido informando cliente e data.
2. O pedido começa em RASCUNHO. Adicione descrição, quantidade e preço de cada item.
3. Consulte detalhes e total; edite pedido e itens enquanto estiver em rascunho.
4. Confirme quando houver ao menos um item: o status vira CONFIRMADO e a edição fica bloqueada.
5. Cancele se necessário: CANCELADO é definitivo, preservando o registro.
6. Para excluir, abra a confirmação e confirme. Excluir pedido remove também seus itens.

Cancelar conserva dados; excluir remove dados. A consulta permanece disponível para qualquer estado.

## Regras e valores

- Cliente obrigatório até 120 caracteres; descrição até 200.
- Quantidade inteira entre 1 e 10.000.
- Preço entre R$ 0,01 e R$ 9.999.999.999,99, com no máximo duas casas decimais.
- Subtotal = quantidade × preço; total = soma dos subtotais, calculados com BigDecimal.
- Total e subtotal não são armazenados no banco.
- Pedido vazio tem total zero e não pode ser confirmado.
- Item só pode ser acessado pelo pedido proprietário.
- IDs inexistentes recebem resposta 404 com tratamento de erro.

Exemplo: 2 unidades de R$ 149,90 totalizam R$ 299,80. Não há pagamentos, impostos, descontos ou cadastro separado de produtos.

## Modelo do banco

dbo.pedido contém id, nome_cliente, data_pedido e status.
dbo.item_pedido contém id, pedido_id, descricao_produto, quantidade e preco_unitario.

Um pedido possui vários itens. A FK pedido_id aponta para pedido.id, com ON DELETE CASCADE. As duas tabelas têm chave primária gerada pelo banco. O [DDL](database/001-create-tables.sql) define o esquema.

No perfil padrão, Hibernate apenas valida as tabelas. A aplicação exige DB_URL, DB_USERNAME e DB_PASSWORD e não substitui conexão inválida por H2. O usuário publicado possui permissões de CRUD nas duas tabelas.

## Rotas

| Finalidade | Caminho | Método |
| --- | --- | --- |
| Lista / cadastro | /pedidos | GET / POST |
| Novo pedido | /pedidos/novo | GET |
| Detalhes | /pedidos/{id} | GET |
| Editar pedido | /pedidos/{id}/editar | GET / POST |
| Novo item | /pedidos/{id}/itens/novo | GET |
| Adicionar item | /pedidos/{id}/itens | POST |
| Editar item | /pedidos/{id}/itens/{itemId}/editar | GET / POST |
| Excluir item | /pedidos/{id}/itens/{itemId}/excluir | GET / POST |
| Confirmar / cancelar | /pedidos/{id}/confirmar e /cancelar | POST |
| Excluir pedido | /pedidos/{id}/excluir | GET / POST |

As rotas GET de exclusão exibem a confirmação; a alteração ocorre por POST.

## Organização do código

| Caminho | Conteúdo |
| --- | --- |
| src/main/java/br/com/dimdim/pedidos/domain | Entidades, validações e estados |
| src/main/java/br/com/dimdim/pedidos/repository | Acesso ao banco |
| src/main/java/br/com/dimdim/pedidos/service | Regras, cálculos e transações |
| src/main/java/br/com/dimdim/pedidos/web | Controlador, formulários e mensagens |
| src/main/java/br/com/dimdim/pedidos/config | Configuração de segurança |
| src/main/resources/templates | Telas Thymeleaf |
| src/main/resources/static/css | Aparência |
| src/main/resources/application.yml | Perfil padrão Azure SQL |
| src/main/resources/application-demo.yml | Perfil local temporário |
| src/test/java | Testes Java, Web e Azure SQL |
| database | DDL e utilitários JDBC |
| infra | Scripts Azure CLI |
| monitoring | Consultas KQL |
| docs | Guias e evidências |
| target | JAR gerado, fora do Git |
| .local | Configurações e cache, fora do Git |

## Execução rápida

Na pasta que contém pom.xml, com JDK 21:
```powershell
./mvnw.cmd verify
java -jar target/dimdim-pedidos-0.1.0.jar --spring.profiles.active=demo
```
Abra http://127.0.0.1:8080/pedidos. Encerre com Ctrl+C. Os dados demo desaparecem ao encerrar. Para persistência Azure, siga o guia completo.

## Azure e monitoramento

Deploy-Azure.ps1 provisiona recursos pelo CLI, aplica DDL, configura acesso SQL, testa integração e publica o JAR. As credenciais ficam no ambiente, sem versionamento.

Enable-ApplicationInsights.ps1 habilita o agente. Test-ApplicationInsights.ps1 comprova ingestão e Query-ApplicationInsights.ps1 consulta registros. O workspace possui retenção de 30 dias e limite diário de 0,1 GB; esse limite não garante teto monetário.

## Testes e limites

Os testes locais verificam regras de negócio e formulários com persistência JPA em H2. AzureSqlIntegrationTest valida CRUD no banco de destino e a exclusão em cascata do DDL. Os testes de integração são ativados somente com DIMDIM_AZURE_TEST=true e conexão configurada.

Não há autenticação, auditoria financeira, pagamentos, paginação ou teste específico de concorrência nesta versão. A exclusão é definitiva.

Consulte o [modelo de dados](docs/modelo-e-regras.md), o [guia de execução](docs/guia-execucao.md) e o [guia de uso](docs/uso.md).
