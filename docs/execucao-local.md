# Execução local

## Pré-requisitos

JDK 21 e acesso à internet na primeira execução para baixar Maven e dependências. Maven Wrapper está incluído no projeto. Neste computador foi encontrado Java 21.0.11.

Se o comando não encontrar o Java, configure JAVA_HOME com a pasta de instalação do JDK 21. Maven não precisa ser instalado separadamente.

## Compilar e testar

Abra um terminal PowerShell na pasta `dimdim-pedidos`:

```powershell
./mvnw.cmd verify
```

O comando executa os testes e gera `target/dimdim-pedidos-0.1.0.jar`. Os testes de unidade não acessam banco. Os testes Web iniciam o Spring, renderizam as páginas e exercitam persistência JPA em H2 temporário. Os testes Azure SQL são opcionais e só executam com DIMDIM_AZURE_TEST=true e credenciais configuradas. O cache das dependências fica em `.local/maven-repository` e não deve ser enviado ao GitHub. O Wrapper utiliza também seu cache padrão de distribuições Maven no perfil do usuário.

Em Linux/macOS, use `sh ./mvnw verify`.

## Ver as telas sem configurar um banco

Para experimentar a aplicação no seu computador, após compilar:

```powershell
java -jar target/dimdim-pedidos-0.1.0.jar --spring.profiles.active=demo
```

Abra [DimDim Pedidos](http://127.0.0.1:8080/pedidos) no navegador. Cadastre um pedido, adicione itens, altere os dados e teste a exclusão. Encerre com Ctrl+C no terminal.

O perfil `demo` usa H2 em memória e se limita ao endereço local 127.0.0.1. A página identifica esse modo em um aviso. Os dados são apagados ao encerrar e o banco inicia vazio. Essa demonstração não substitui a persistência em Azure SQL exigida pelo checkpoint e não deve ser ativada na implantação.

O perfil padrão continua exigindo DB_URL, DB_USERNAME e DB_PASSWORD para SQL Server/Azure SQL; não ativa H2 automaticamente se a conexão falhar.

## Configurar e executar com SQL Server / Azure SQL

Esta parte depende de um banco acessível. A integração com Azure SQL real foi validada na etapa 4. No banco de destino, execute uma única vez `database/001-create-tables.sql`. Ele cria as duas tabelas em `dbo`; não cria o servidor nem o banco.

Defina as variáveis apenas na sessão do terminal. A URL abaixo é um exemplo com campos para substituir:

```powershell
$env:DB_URL = 'jdbc:sqlserver://SEU_SERVIDOR.database.windows.net:1433;databaseName=SEU_BANCO;encrypt=true;trustServerCertificate=false;loginTimeout=30;'
$env:DB_USERNAME = 'SEU_USUARIO'
$senhaBanco = Read-Host 'Senha do banco' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $senhaBanco).Password
try {
    java -jar target/dimdim-pedidos-0.1.0.jar
} finally {
    Remove-Item Env:DB_PASSWORD -ErrorAction SilentlyContinue
}
```

Forneça usuário, senha e permissão de rede válidos para o banco. Nunca coloque valores reais de credenciais no código ou nos arquivos versionados. A leitura oculta da senha evita incluí-la no histórico de comandos.

O servidor Web utiliza a porta 8080 por padrão, ajustável com `PORT`. A data padrão do pedido usa America/Sao_Paulo, ajustável com `APP_TIMEZONE`.

Após a inicialização, acesse `/pedidos` para utilizar as telas de CRUD. `/` redireciona para a lista. A aplicação usa páginas e formulários HTML, sem API REST nesta versão.

## Tratamento do esquema

No perfil padrão, `ddl-auto: validate` verifica o mapeamento do banco ao iniciar. A aplicação não cria nem modifica automaticamente as tabelas. Banco vazio, conexão inválida ou ausência das variáveis impedem a inicialização. Somente o perfil explícito `demo` gera suas tabelas temporárias de demonstração.

## Fluxo das telas

1. Lista: visualizar pedidos, seus status e totais.
2. Novo pedido: informar nome do cliente e data.
3. Detalhes: adicionar produtos e revisar o total calculado.
4. Editar: alterar dados e itens enquanto o pedido estiver em rascunho.
5. Confirmar: exige pelo menos um item e bloqueia a edição.
6. Cancelar: muda o status definitivamente, preservando os dados para consulta.
7. Excluir: abrir a página de confirmação e clicar em “Sim, excluir”. Excluir um pedido remove também seus itens.

Todos os formulários de alteração usam POST e token CSRF. O escopo permanece sem login, conforme a proposta inicial.

## Referências técnicas

- [Requisitos do Spring Boot 4.0](https://docs.spring.io/spring-boot/4.0/system-requirements.html).
- [Driver JDBC da Microsoft para SQL Server](https://learn.microsoft.com/en-us/sql/connect/jdbc/download-microsoft-jdbc-driver-for-sql-server).
