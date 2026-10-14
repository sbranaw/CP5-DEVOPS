# Azure atual — banco recriado e publicação pelo CLI

Registro atualizado em 14/10/2026, a partir da execução e dos resultados confirmados durante a preparação da apresentação.

## Recursos

| Recurso | Nome / configuração |
| --- | --- |
| Grupo de recursos | `rg-dimdim-261002-ce05` |
| Região | Brazil South (`brazilsouth`) |
| Plano Linux | `ASP-rgdimdim261002ce05-9099`, Free F1 |
| Servidor SQL | `dimdim-sql-261002-ce05.database.windows.net` |
| Banco Azure SQL | `dimdim-pedidos` |
| Administrador SQL | `dimdimadmin` |
| Usuário da aplicação | `dimdim_app` |
| Tabelas | `dbo.pedido` e `dbo.item_pedido` |
| Web App de origem | `dimdim-pedidos-261002-ce05` |
| Web App criado pelo CLI | `dimdim-pedidos-261002-ce05-cli` |

As senhas do administrador e de `dimdim_app` foram redefinidas durante a configuração. As credenciais ficam nas configurações do Web App e não são versionadas. O arquivo [azure.current.json](../infra/azure.current.json) contém apenas identificadores públicos; ele é destinado ao roteiro Bash, não ao antigo `Deploy-Azure.ps1`.

## Estado comprovado

- Duas tabelas com PKs; FK de `item_pedido` para `pedido`, ativa e validada, com `ON DELETE CASCADE`.
- `dimdim_app` com SELECT, INSERT, UPDATE e DELETE nas duas tabelas.
- Dois testes de integração Azure SQL passaram, cobrindo CRUD e cascata.
- Compilação e testes com Java 21: `BUILD SUCCESS` no Cloud Shell.
- Web App de origem publicado pelo CLI: `RuntimeSuccessful`, uma instância bem-sucedida, nenhuma instância com falha.
- CRUD na aplicação de origem e persistência conferida no banco após criação, alteração e exclusão. O pedido de teste #3 foi removido com seus itens.
- Application Insights habilitado na aplicação de origem: requisições, dependências SQL e tempos de resposta recebidos. Um HTTP 403 foi identificado e explicado como envio de formulário com token CSRF antigo após reinício; não se deve declarar ausência absoluta de falhas.
- Web App `-cli` criado por `az webapp create`, retornando `Running`. A cópia de configurações, o deploy e o recebimento de telemetria desse novo destino ainda precisam ser confirmados.

O nome efetivo do Application Insights deve ser consultado no portal: a captura da validação identifica `dimdim-pedidos-261002-ce05`; o nome sugerido durante o roteiro não foi confirmado. O novo roteiro copia a conexão de monitoramento existente, sem criar outro recurso de telemetria.

## Endereços

- Aplicação validada de origem: https://dimdim-pedidos-261002-ce05-h8gbb9a4hghjch3.brazilsouth-01.azurewebsites.net/pedidos
- Destino criado pelo CLI: https://dimdim-pedidos-261002-ce05-cli.azurewebsites.net/pedidos

O segundo endereço só deve ser usado na apresentação após concluir a publicação e confirmar a página. Consulte sempre `defaultHostName`; recriar recursos pode alterar o domínio.

## Compilar no Cloud Shell (Bash)

```bash
git clone https://github.com/sbranaw/CP5-DEVOPS.git
cd CP5-DEVOPS
```

Se o clone já existir, entre na pasta e atualize-o com `git pull --ff-only` (preservando previamente alterações locais). Confira `java -version`. Caso a sessão use Java 17:

```bash
mkdir -p "$HOME/dimdim-tools/java21" &&
curl -fL --retry 3 \
  https://aka.ms/download-jdk/microsoft-jdk-21-linux-x64.tar.gz \
  -o "$HOME/dimdim-tools/java21.tar.gz" &&
tar -xzf "$HOME/dimdim-tools/java21.tar.gz" \
  -C "$HOME/dimdim-tools/java21" --strip-components=1 &&
export JAVA_HOME="$HOME/dimdim-tools/java21" &&
export PATH="$JAVA_HOME/bin:$PATH" &&
java -version

bash ./mvnw verify
```

Os exports valem para a sessão atual. Aguarde `BUILD SUCCESS`. Esses testes locais não são uma nova execução dos testes opcionais contra Azure SQL.

## Criar, configurar e publicar usando o banco existente

No Cloud Shell, a autenticação já está disponível. Fora dele, execute `az login`. Revise os identificadores de `infra/azure.current.json`.

```bash
# Mostrar o plano sem acessar nem alterar a Azure:
bash infra/Deploy-ExistingAzure.sh

# Criar/configurar o Web App e publicar o pacote compilado:
bash infra/Deploy-ExistingAzure.sh --execute --deploy
```

O roteiro usa explicitamente a assinatura do JSON. Confere o banco e as configurações da origem; cria o plano Linux/F1 e o Web App Java 21 caso não existam; configura HTTPS/TLS; copia as variáveis de banco e de monitoramento por arquivo temporário protegido; libera os possíveis IPv4 de saída no firewall SQL; publica o JAR. Não altera o DDL nem as senhas SQL. Se F1 não estiver disponível, a execução falha sem escolher um plano pago.

O arquivo temporário contém credenciais e é removido ao terminar. Não execute com rastreamento de shell (`bash -x`) nem com `--debug`. Os dois Web Apps compartilham banco, telemetria e capacidade do plano F1.

Para configurar sem publicar, use somente `--execute`. Se algum passo falhar, os recursos já criados ficam preservados; corrija o erro e execute novamente. O roteiro não confirma o funcionamento da aplicação nem a ingestão de telemetria apenas pela presença das configurações.

### Comandos centrais para explicar na apresentação

O grupo, o plano e o SQL atuais foram reutilizados. A criação do novo Web App e o deploy são feitos pelo CLI:

```bash
# Apenas para um ambiente novo: criação de grupo e plano.
az group create --name rg-dimdim-261002-ce05 --location brazilsouth
az appservice plan create \
  --resource-group rg-dimdim-261002-ce05 \
  --name ASP-rgdimdim261002ce05-9099 \
  --location brazilsouth --is-linux --sku F1

# Comando efetivamente usado para criar o novo Web App:
az webapp create \
  --resource-group rg-dimdim-261002-ce05 \
  --plan ASP-rgdimdim261002ce05-9099 \
  --name dimdim-pedidos-261002-ce05-cli \
  --runtime 'JAVA:21-java21' --https-only true

# Após configurar banco, firewall e monitoramento:
az webapp deploy \
  --resource-group rg-dimdim-261002-ce05 \
  --name dimdim-pedidos-261002-ce05-cli \
  --src-path target/dimdim-pedidos-0.1.0.jar \
  --type jar --timeout 600000
```

Esses exemplos pressupõem a assinatura correta selecionada. O roteiro completo passa `--subscription` explicitamente. Para criar também servidor/banco novos e aplicar o DDL, existe o fluxo PowerShell [implantacao-azure.md](implantacao-azure.md). Não use sua opção `-Resume` no ambiente atual: ela redefine as senhas e exige as tags daquele provisionamento.

## Conexão e DDL

```text
DB_URL=jdbc:sqlserver://dimdim-sql-261002-ce05.database.windows.net:1433;databaseName=dimdim-pedidos;encrypt=true;trustServerCertificate=false;loginTimeout=30;
DB_USERNAME=dimdim_app
DB_PASSWORD=<configurada exclusivamente no ambiente>
```

O DDL completo está em [database/001-create-tables.sql](../database/001-create-tables.sql). Não reaplique `CREATE TABLE` sobre as tabelas existentes. Spring usa `ddl-auto: validate`; o esquema é administrado separadamente.

## Demonstração presencial

Abra `/pedidos`, crie um pedido e um item, altere a quantidade e exclua o pedido. Após cada operação, mostre no Query editor os registros de `dbo.pedido` e `dbo.item_pedido`, filtrando pelo ID real criado. Depois mostre em Application Insights > Logs as consultas `requests` e `dependencies`, com filtro `type contains "SQL"`, requisições, resultados e duração. Capturas não são necessárias para executar essa demonstração.

O enunciado pede também o PDF `<nome_grupo>_webapp.pdf` no Teams, código/DDL/scripts e How To no GitHub. A criação de grupo/plano/SQL atuais pelo portal não deve ser apresentada como criação pelo CLI; o novo Web App tem criação pelo CLI comprovada.

## Referências

- [Publicar JAR no App Service](https://learn.microsoft.com/en-us/azure/app-service/deploy-zip).
- [Configurar variáveis pelo CLI](https://learn.microsoft.com/en-us/cli/azure/webapp/config/appsettings).
- [IPs de saída do Web App](https://learn.microsoft.com/en-us/azure/app-service/overview-inbound-outbound-ips).
- [Firewall Azure SQL pelo CLI](https://learn.microsoft.com/en-us/cli/azure/sql/server/firewall-rule).
- [Microsoft OpenJDK](https://learn.microsoft.com/en-us/java/openjdk/download).
