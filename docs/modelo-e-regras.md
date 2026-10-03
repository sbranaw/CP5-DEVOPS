# Modelo e regras de negócio

A aplicação gerencia pedidos e seus itens, com as regras e o relacionamento descritos abaixo.

## Pedido: tabela dbo.pedido

| Campo | Tipo SQL | Regra |
| --- | --- | --- |
| id | BIGINT IDENTITY | Chave primária gerada pelo banco |
| nome_cliente | NVARCHAR(120) | Obrigatório, até 120 caracteres |
| data_pedido | DATE | Obrigatória; inicialmente preenchida com a data atual da aplicação |
| status | VARCHAR(20) | RASCUNHO, CONFIRMADO ou CANCELADO; inicia como RASCUNHO |

## Item: tabela dbo.item_pedido

| Campo | Tipo SQL | Regra |
| --- | --- | --- |
| id | BIGINT IDENTITY | Chave primária gerada pelo banco |
| pedido_id | BIGINT | FK obrigatória para dbo.pedido(id) |
| descricao_produto | NVARCHAR(200) | Obrigatória, até 200 caracteres |
| quantidade | INT | Inteiro entre 1 e 10.000 |
| preco_unitario | DECIMAL(12,2) | Entre R$ 0,01 e R$ 9.999.999.999,99 |

Um pedido possui zero ou mais itens. Cada item pertence a exatamente um pedido. Um rascunho pode começar vazio; a confirmação exige pelo menos um item.

## Valores

- Utilizar BigDecimal em Java para valores monetários, em reais.
- Rejeitar preço com mais de duas casas decimais; não arredondar silenciosamente a entrada.
- Calcular subtotal = quantidade × preço unitário.
- Calcular o total somando os subtotais. Um pedido vazio tem total zero.
- Não armazenar subtotal nem total nas tabelas nesta versão, evitando divergência entre valores calculados e itens.
- Não há juros, impostos, descontos ou integração com pagamentos nesta versão.

## Operações

- Criar um pedido como RASCUNHO.
- Consultar a lista e os detalhes de qualquer pedido, com seus itens e total.
- Editar cliente e data apenas em RASCUNHO.
- Adicionar, editar e excluir itens apenas em RASCUNHO.
- Confirmar um RASCUNHO com pelo menos um item.
- Cancelar um RASCUNHO ou CONFIRMADO. CANCELADO é estado final.
- Excluir um pedido em qualquer estado após confirmação na tela; o banco também remove seus itens por ON DELETE CASCADE. A exclusão é definitiva e não implementa auditoria financeira.
- Tratar IDs inexistentes com mensagem clara. Impedir acesso a um item por um pedido diferente do seu proprietário.
- Executar alterações em transações para preservar a consistência.

## Validação

Remover espaços nas extremidades de cliente e descrição. Rejeitar texto vazio, datas inválidas, status desconhecido, quantidade fora da faixa e preço fora da faixa. Regras de status e de confirmação serão verificadas pelo serviço Java, além das restrições estruturais do banco.

## Fluxo de uso

Criar pedido, adicionar itens, consultar detalhes, alterar quantidade/preço, excluir um item e excluir o pedido. Após cada operação, conferir as tabelas no banco. Mostrar também o impedimento de confirmar um pedido vazio.
