# Resultado da etapa 3

## Telas e comportamento

- Lista de pedidos com cliente, data, status, quantidade de itens e total.
- Resumo de quantidade de pedidos, rascunhos e valor dos confirmados, sem incluir cancelados.
- Formulário de cadastro e edição de pedido.
- Detalhes do pedido com todos os itens, subtotais e total calculado.
- Formulário de inclusão e edição de item.
- Página de confirmação antes de excluir pedido ou item.
- Confirmação de pedido com pelo menos um item e bloqueio de edição após confirmação/cancelamento.
- Mensagens de sucesso, erros nos campos e páginas para registros inexistentes e violações das regras.
- Interface em português, valores em reais, adaptação para telas pequenas e tabelas com rolagem própria quando necessário.
- Formulários POST com proteção CSRF e escape de texto digitado pelo usuário.

## Demonstração local

Incluído perfil explícito `demo` com H2 em memória para experimentar as telas sem depender da Azure. A página apresenta aviso dos dados temporários. O servidor desse perfil atende somente em 127.0.0.1.

O modo padrão permanece configurado para SQL Server/Azure SQL por variáveis de ambiente e valida o esquema sem modificá-lo. O DDL acadêmico continua em `database/001-create-tables.sql`.

## Testes realizados

| Conjunto | Testes | Falhas | Erros | Ignorados |
| --- | ---: | ---: | ---: | ---: |
| PedidoTest | 19 | 0 | 0 | 0 |
| PedidoServiceTest | 11 | 0 | 0 | 0 |
| PedidoWebTest | 14 | 0 | 0 | 0 |
| Total | 44 | 0 | 0 | 0 |

Os testes Web renderizam templates Thymeleaf com Spring MVC e utilizam os serviços e repositório reais com H2. Foram verificados cadastro, leitura, alteração, exclusão de pedido e itens, FK pela gravação do pedido_id, remoção dos itens ao excluir um pedido pelo serviço, validações de formulário, confirmação de exclusão, status, CSRF e escape de HTML.

Compilação, testes e geração do JAR concluídos com BUILD SUCCESS. Um ajuste posterior apenas no CSS foi reempacotado sem repetir os testes de Java.

## Verificação no navegador

Aplicação iniciada como demonstração local. Cadastro de um pedido com cliente fictício e inclusão de duas unidades de teclado a R$ 149,90, com total calculado de R$ 299,80. Captura da tela em `imagens/etapa-3-pedido.jpg`.

Também verificada a tela em largura de celular de 390 px. Corrigida uma rolagem horizontal da página causada por texto oculto de acessibilidade. Após o ajuste, a página ficou dentro da largura disponível; a tabela mantém sua própria rolagem para acomodar as colunas.

## Limites da validação

- O Docker está instalado, mas o serviço não estava ativo. Nenhum SQL Server local estava disponível.
- A persistência demonstrada nesta etapa usa H2, não SQL Server/Azure SQL.
- O DDL de SQL Server, a exclusão via ON DELETE CASCADE diretamente no banco e o comportamento concorrente dos bloqueios precisam de verificação no banco de destino.
- Não foram criados recursos Azure, executado deploy nem publicado código no GitHub.
- Application Insights será configurado na etapa prevista para monitoramento.

## Próxima etapa

Etapa 4: preparar scripts Azure CLI, provisionar os recursos autorizados e validar a aplicação com Azure SQL. Começar somente após autorização do usuário. A definição da assinatura e autenticação serão necessárias antes de executar o provisionamento.
