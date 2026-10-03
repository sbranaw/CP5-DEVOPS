# Roteiro e checklist — CP5-DEVOPS

Gabriel Sbrana Campos — RM565849
Thiago Rodrigues da Mota — RM563650
Moisés Waidemann Molinillo — RM563719

Repositório: https://github.com/sbranaw/CP5-DEVOPS
Site: https://dimdim-pedidos-261002-ce05.azurewebsites.net/pedidos

## Ordem prática da apresentação

1. Apresente Java, telas e relacionamento pedido → itens.
2. Mostre fontes, DDL e How To no GitHub, incluindo scripts Azure CLI.
3. Mostre Web App Java 21, Azure SQL e recursos de monitoramento.
4. Crie pedido novo e anote ID; confira dbo.pedido.
5. Adicione dois itens e confira dbo.item_pedido, FK e valores.
6. Consulte detalhes e compare com o banco.
7. Altere cliente/data e quantidade/preço; confira ambas as tabelas.
8. Exclua um item e confira ausência da linha e novo total.
9. Confirme o pedido e mostre bloqueio de edição; cancele se desejar mostrar status preservado.
10. Exclua o pedido e confira ausência do pedido e de seus itens.
11. Mostre requisições e dependências SQL no Application Insights.

**Confira no banco após cada operação**, não apenas ao final. Use registros próprios para a demonstração e preserve o pedido de exemplo existente.

## Consultas

Substitua 3 pelo ID real:
```sql
SELECT id, nome_cliente, data_pedido, status
FROM dbo.pedido WHERE id = 3;
SELECT id, pedido_id, descricao_produto, quantidade, preco_unitario,
       quantidade * preco_unitario AS subtotal
FROM dbo.item_pedido WHERE pedido_id = 3 ORDER BY id;
```
Ou use infra/Inspect-AzurePedido.ps1. Após excluir, ambas as consultas devem retornar zero linhas.

## Evidências

docs/imagens/etapa-3-pedido.jpg mostra demonstração local; etapa-4-azure.jpg mostra site publicado. Os relatórios das etapas 4 e 5 e os JSON de telemetria registram testes reais. Essas evidências não substituem demonstrar cada CRUD no banco. Não exponha credenciais em capturas.

## Checklist

- [ ] Fontes, DDL e How To disponíveis ao avaliador no GitHub.
- [ ] CRUD completo de pedidos e itens conferido no banco.
- [ ] Application Insights demonstrado.
- [ ] PDF CP5-DEVOPS_webapp.pdf revisado com integrantes e links.
- [ ] Representante envia o PDF no Teams.

## Penalidades

| Falha | Desconto |
| --- | ---: |
| Entrega fora do padrão | 0,5 ponto |
| Não mostrar cada operação no banco | 2 pontos |
| Não entregar Application Insights | 1 ponto |
| Sem How To no GitHub | 1,5 ponto |
| Sem DDL | 1 ponto |
| Apenas uma tabela | 2,5 pontos |

Respeitar Azure CLI e a proibição de reutilizar checkpoints anteriores/Sprint 3. Não apresentar PUT/DELETE ou JSON como API existente: esta versão usa formulários HTML.
