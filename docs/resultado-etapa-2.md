# Resultado da etapa 2

## Alterações

- Criado projeto Maven com Java 21 e Spring Boot 4.0.8.
- Incluído Maven Wrapper para compilar sem instalação manual do Maven.
- Ajustado o script Windows do Wrapper para aceitar diretórios de cache sem a propriedade Target, encontrada nula no PowerShell deste computador.
- Implementadas entidades Pedido e ItemPedido mapeadas às tabelas do DDL existente, com PK gerada e FK entre itens e pedidos.
- Implementado repositório Spring Data JPA e serviço transacional para criar, consultar, listar, editar, confirmar, cancelar e excluir pedidos, além de adicionar, alterar e excluir itens.
- Implementados cálculos com BigDecimal, validação de textos e valores, estados de pedido e proteção contra alteração de itens de outro pedido.
- Configurado bloqueio do pedido nas operações de escrita para coordenar alterações concorrentes feitas pelo serviço. O comportamento desse bloqueio no SQL Server ainda precisa ser verificado.
- Configurada conexão ao SQL Server por DB_URL, DB_USERNAME e DB_PASSWORD, com validação do esquema em vez de criação automática.
- Criado guia de execução local e atualizada a documentação de progresso.

## Verificações realizadas

Compilação, testes e empacotamento Maven concluídos com BUILD SUCCESS usando o JDK 21.0.11 e Maven 3.9.15 disponíveis neste computador.

| Conjunto | Testes | Falhas | Erros | Ignorados |
| --- | ---: | ---: | ---: | ---: |
| PedidoTest | 19 | 0 | 0 | 0 |
| PedidoServiceTest | 11 | 0 | 0 | 0 |
| Total | 30 | 0 | 0 | 0 |

Exemplo verificado: 3 itens a R$ 0,10 e 2 itens a R$ 12,35 produzem total exato de R$ 25,00. Também foram verificados limites de valores, pedido vazio, alteração e remoção de itens, normalização de nomes, data no fuso configurado e transições de status.

Os testes do serviço utilizam um repositório simulado. Não comprovam conexão, gravação, exclusão em cascata, transações nem bloqueios em SQL Server/Azure SQL. Não há validação de integração com banco real nesta etapa.

## Próxima etapa

Etapa 3: criar telas e conectar os formulários ao serviço para demonstrar CRUD. Aguardar autorização do usuário antes de começar.
