# Uso da aplicação

1. Abra /pedidos e escolha **Novo pedido**.
2. Informe cliente e data. O pedido inicia em **RASCUNHO**.
3. Adicione itens com descrição, quantidade e preço unitário.
4. Consulte o total e edite os dados enquanto o pedido estiver em rascunho.
5. Confirme quando houver pelo menos um item. **CONFIRMADO** bloqueia a edição.
6. Para cancelar, use a ação correspondente. **CANCELADO** é definitivo e conserva dados.
7. Para excluir, abra a confirmação e confirme. Excluir pedido remove também seus itens.

Cancelar conserva registros; excluir remove registros. A consulta permanece disponível em todos os estados. Duas unidades de R$ 149,90 resultam em subtotal de R$ 299,80; o total soma os subtotais.

## Conferir a persistência

Informe o ID desejado:
```sql
DECLARE @pedido_id BIGINT = 3;
SELECT id, nome_cliente, data_pedido, status
FROM dbo.pedido WHERE id = @pedido_id;
SELECT id, pedido_id, descricao_produto, quantidade, preco_unitario,
       quantidade * preco_unitario AS subtotal
FROM dbo.item_pedido WHERE pedido_id = @pedido_id ORDER BY id;
```

Ou consulte pelo script:
```powershell
./infra/Inspect-AzurePedido.ps1 -ConfigPath .local/azure-config.json -PedidoId 3
```

Substitua 3 pelo ID desejado. O script usa as configurações do Web App em memória e requer acesso à assinatura e ao firewall SQL. Após excluir o pedido, ambas as consultas retornam zero linhas para esse ID.

## Validação

Campos inválidos exibem mensagens no formulário. Confirmar pedido vazio é recusado. Pedidos confirmados ou cancelados não permitem alteração de dados ou itens. IDs inexistentes recebem resposta HTTP 404.
