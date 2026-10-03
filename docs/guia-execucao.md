# Guia de requisitos, passos e caminhos

## 1. Escolha como executar

| Opção | Requisitos | Banco |
| --- | --- | --- |
| Site publicado | Navegador e internet | Azure SQL |
| Demonstração local | JDK 21, terminal e internet inicialmente | H2 temporário |
| Local com Azure SQL | Requisitos locais, banco, credenciais e TCP 1433 | Azure SQL |
| Nova implantação | JDK 21, PowerShell 7, Azure CLI e assinatura autorizada | Azure SQL |

Site: https://dimdim-pedidos-261002-ce05.azurewebsites.net/pedidos. Para navegar no site não é necessário instalar Java.

## 2. Obtenha o projeto

Clone o repositório e entre na pasta:
```powershell
git clone https://github.com/sbranaw/CP5-DEVOPS.git
cd CP5-DEVOPS
```
Todos os comandos seguintes partem da pasta que contém pom.xml e mvnw.cmd. Se usar o ZIP do GitHub, extraia-o e abra o terminal nessa pasta.

## 3. Confira JDK 21

Instale JDK 21, configure JAVA_HOME para sua pasta e inclua bin no PATH. Abra terminal novo:
```powershell
java -version
javac -version
./mvnw.cmd -version
```
Java e o compilador devem indicar 21. Não é necessário instalar Maven: o Wrapper baixa a versão do projeto. A primeira execução precisa de internet; o cache do projeto fica em .local/maven-repository.

## 4. Compile e teste

```powershell
./mvnw.cmd verify
```
Aguarde BUILD SUCCESS. O pacote fica em target/dimdim-pedidos-0.1.0.jar, e os relatórios em target/surefire-reports. Testes locais não precisam de credenciais. Os testes Azure são opcionais.

Em Linux/macOS, substitua ./mvnw.cmd por sh ./mvnw.

## 5. Rode a demonstração

```powershell
java -jar target/dimdim-pedidos-0.1.0.jar --spring.profiles.active=demo
```
Mantenha o terminal aberto. Acesse http://127.0.0.1:8080/pedidos. Cadastre pedido e itens. Encerre com Ctrl+C.

O banco começa vazio e os dados desaparecem ao encerrar. O aviso demo aparece na interface. Esse perfil só atende no computador local e não deve ser usado na implantação.

Se a porta estiver ocupada:
```powershell
java -jar target/dimdim-pedidos-0.1.0.jar --spring.profiles.active=demo --server.port=8081
```
Acesse http://127.0.0.1:8081/pedidos.

## 6. Rode com Azure SQL persistente

O banco deve conter as tabelas de database/001-create-tables.sql. Não reaplique DDL sem conferir o banco existente. O usuário precisa de CRUD nas tabelas e o firewall deve permitir o IPv4 atual do computador. A conexão usa TCP 1433.

Substitua os campos de exemplo e leia a senha sem colocá-la no histórico:
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
Abra http://localhost:8080/pedidos. Solicite credenciais ao responsável pelo banco. Quando o provisionamento gera uma senha administrativa em memória, acesso administrativo posterior pode exigir redefinição no Azure. Isso não é necessário para navegar no site.

| Variável | Uso |
| --- | --- |
| DB_URL | URL JDBC obrigatória no perfil padrão |
| DB_USERNAME | Usuário obrigatório no perfil padrão |
| DB_PASSWORD | Senha obrigatória no perfil padrão |
| PORT | Porta padrão 8080 |
| APP_TIMEZONE | Padrão America/Sao_Paulo |
| DIMDIM_AZURE_TEST | true apenas para testes opcionais Azure |

O perfil padrão valida o esquema e não ativa H2 automaticamente.

## 7. Azure e diagnósticos

Veja [How To](how-to.md), [implantação](implantacao-azure.md) e [monitoramento](application-insights.md).

| Problema | Conferência |
| --- | --- |
| Java não encontrado | JDK 21, JAVA_HOME, PATH, terminal novo |
| Java incompatível | Confira ./mvnw.cmd -version |
| Dependências não baixam | Internet, proxy e Maven Central |
| JAR ausente | Execute verify e confira BUILD SUCCESS |
| Porta ocupada | Encerre outra aplicação ou use 8081 |
| DB_URL ausente | Configure SQL ou ative demo explicitamente |
| SQL timeout | Servidor, rede, TCP 1433 e firewall |
| Login SQL falha | Usuário, senha e nome do banco |
| Schema-validation falha | DDL e tipos de ambas as tabelas |
| Edição bloqueada | Apenas rascunhos podem ser alterados |
| Sem telemetria | Confira configuração, gere acessos e aguarde ingestão |

Não publique .local, target, senhas ou tokens.
