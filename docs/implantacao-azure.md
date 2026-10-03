# Implantação na Azure com Azure CLI

## Recursos

Um grupo de recursos exclusivo para DimDim, plano App Service Linux, Web App Java 21, servidor lógico Azure SQL e banco SQL Basic com duas tabelas relacionadas.

O roteiro usa F1 no Web App por padrão. Se essa modalidade não estiver disponível na assinatura/região, ele interrompe a execução; não muda automaticamente para um plano pago. Azure SQL Basic consome créditos. O custo depende da região, oferta e tempo de uso. Consulte a assinatura e a calculadora antes de trocar o plano.

## Pré-requisitos

- JDK 21, Maven Wrapper e Azure CLI.
- `./mvnw.cmd verify` concluído antes da publicação.
- Autenticação no Azure CLI com uma assinatura ativa: `az login`.
- Permissão para criar os recursos e conectividade local à porta SQL 1433.
- Escolher uma região permitida pelas políticas da assinatura.

## 1. Configuração

Copie `infra/azure.example.json` para `.local/azure-config.json` e preencha a assinatura e nomes exclusivos. O arquivo contém apenas identificadores de recursos, sem credenciais. `.local` fica fora do Git.

## 2. Revisar o plano sem criar recursos

```powershell
./infra/Deploy-Azure.ps1 -ConfigPath .local/azure-config.json
```

Sem `-Execute`, o script mostra o destino e os planos sem acessar nem alterar recursos Azure.

## 3. Provisionar, preparar o banco e publicar

```powershell
./infra/Deploy-Azure.ps1 -ConfigPath .local/azure-config.json -Execute
```

O roteiro executa:

1. Verificação da assinatura e proteção contra uso de um grupo existente de outro projeto.
2. `az group create` para o grupo dedicado.
3. `az appservice plan create` para Linux/F1.
4. `az webapp create` para `JAVA:21-java21` e HTTPS.
5. `az webapp config set` para TLS mínimo 1.2 e FTP desativado.
6. `az sql server create` e `az sql db create` para SQL Basic.
7. `az sql server firewall-rule create` para o IPv4 deste computador e os IPs de saída possíveis do Web App. Não usa a regra 0.0.0.0 que libera serviços Azure de outras assinaturas.
8. Aplicação de `database/001-create-tables.sql` por JDBC e criação de `dimdim_app` com SELECT, INSERT, UPDATE e DELETE apenas nas duas tabelas.
9. Testes de integração opcionais contra o Azure SQL, incluindo o CRUD e ON DELETE CASCADE definido no DDL.
10. `az webapp config appsettings set` com DB_URL, DB_USERNAME e DB_PASSWORD. O perfil `demo` não é ativado.
11. `az webapp deploy --type jar` para publicar o pacote validado.

O DDL é aplicado quando nenhuma das duas tabelas existe. Um esquema incompleto interrompe a execução sem excluir dados. Se as duas tabelas já existirem, o teste inicializa Hibernate com validação do mapeamento antes do deploy.

## Credenciais

O roteiro gera senhas aleatórias em memória. A senha da aplicação é enviada às configurações do Web App e não é impressa. O arquivo JSON temporário de configurações é removido no bloco de limpeza. A senha administrativa não é salva: se precisar de acesso administrativo posteriormente, redefina-a pelo serviço Azure. Para o CRUD, utilize o usuário restrito da aplicação.

Não executar com `--debug`, não publicar arquivos `.local` e não registrar credenciais em capturas. As senhas não devem ser passadas literalmente no histórico do terminal.

## Se a execução falhar

O script mantém os recursos já criados. Não há exclusão automática nem alteração automática para um plano mais caro.

Para retomar um grupo deste roteiro, com a mesma identificação e região:

```powershell
./infra/Deploy-Azure.ps1 -ConfigPath .local/azure-config.json -Execute -Resume
```

Ao retomar, o script redefine as senhas SQL geradas e atualiza as configurações da aplicação. Faça isso em um momento sem uso da aplicação. Não altere nomes de recursos na configuração de um grupo existente sem revisar os efeitos.

## Validar a aplicação publicada

Abra a URL HTTPS de `defaultHostName` retornada pelo Web App, acrescentando `/pedidos`. Confirme que a página não mostra o aviso de demonstração temporária.

Para consultar o estado dos recursos e aguardar a resposta da aplicação:

```powershell
./infra/Test-AzureDeployment.ps1 -ConfigPath .local/azure-config.json
```

O roteiro consulta os recursos, verifica que o banco está online e que `/pedidos` apresenta a aplicação sem o aviso de demonstração. Não modifica os recursos nem os dados.

Cadastre um pedido e confira a persistência no banco.

Também é possível conferir um pedido diretamente por JDBC sem imprimir credenciais:

```powershell
./infra/Inspect-AzurePedido.ps1 -ConfigPath .local/azure-config.json -PedidoId 3
```

Substitua 3 pelo ID que deseja consultar. O roteiro usa as configurações do Web App em memória e consulta somente esse pedido e seus itens. É necessário que o IPv4 do computador esteja liberado no firewall SQL. Se o IP mudar, atualize a regra `DimDimClient` antes da consulta.

Consultas equivalentes em uma ferramenta SQL:

```sql
SELECT id, nome_cliente, data_pedido, status FROM dbo.pedido ORDER BY id DESC;
SELECT id, pedido_id, descricao_produto, quantidade, preco_unitario
FROM dbo.item_pedido ORDER BY id DESC;
```

## Reexecutar somente os testes Azure SQL

Informe DB_URL, DB_USERNAME e DB_PASSWORD como variáveis da sessão usando o procedimento de senha oculta no guia local. Então:

```powershell
$env:DIMDIM_AZURE_TEST = 'true'
try {
    ./mvnw.cmd '-Dspring.profiles.active=azure' '-Dtest=AzureSqlIntegrationTest' test
} finally {
    Remove-Item Env:DIMDIM_AZURE_TEST -ErrorAction SilentlyContinue
    Remove-Item Env:DB_PASSWORD -ErrorAction SilentlyContinue
}
```

Esses testes criam e removem apenas os próprios registros de teste. A validação de concorrência dos bloqueios ainda requer um teste específico, além deste CRUD sequencial.

## Referências oficiais

- [Java SE e deploy no App Service](https://learn.microsoft.com/en-us/azure/app-service/configure-language-java-deploy-run).
- [Comandos Azure CLI para Web App](https://learn.microsoft.com/en-us/cli/azure/webapp?view=azure-cli-latest).
- [Comandos Azure CLI para banco SQL](https://learn.microsoft.com/en-us/cli/azure/sql/db?view=azure-cli-latest).
- [Regras de firewall do Azure SQL](https://learn.microsoft.com/en-us/azure/azure-sql/database/firewall-configure?view=azuresql).
- [Preços do App Service Linux](https://azure.microsoft.com/en-us/pricing/details/app-service/linux/).
