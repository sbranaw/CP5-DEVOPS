package br.com.dimdim.pedidos.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DIMDIM_AZURE_TEST", matches = "true")
class AzureSqlIntegrationTest {
    @Autowired PedidoService service;
    @Autowired JdbcTemplate jdbc;

    @Test
    void crudPersisteNoBancoDeDestino() {
        Long id = null;
        try {
            id = service.criar("Teste de implantação DimDim", LocalDate.now()).id();
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM dbo.pedido WHERE id = ?", Integer.class, id));
            var pedido = service.adicionarItem(id, "Produto de teste", 2, new BigDecimal("12.35"));
            Long itemId = pedido.itens().getFirst().id();
            assertEquals(id, jdbc.queryForObject("SELECT pedido_id FROM dbo.item_pedido WHERE id = ?", Long.class, itemId));
            assertEquals(new BigDecimal("24.70"), service.consultar(id).total());
            service.alterarItem(id, itemId, "Produto atualizado", 3, new BigDecimal("20.00"));
            assertEquals(3, jdbc.queryForObject("SELECT quantidade FROM dbo.item_pedido WHERE id = ?", Integer.class, itemId));
            assertEquals(new BigDecimal("60.00"), service.consultar(id).total());
            service.removerItem(id, itemId);
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dbo.item_pedido WHERE id = ?", Integer.class, itemId));
            service.excluir(id);
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dbo.pedido WHERE id = ?", Integer.class, id));
            id = null;
        } finally {
            if (id != null) service.excluir(id);
        }
    }

    @Test
    void foreignKeyRemoveDetalhesAoExcluirPedidoDiretamente() {
        Long id = service.criar("Teste de FK DimDim", LocalDate.now()).id();
        try {
            service.adicionarItem(id, "Teste de cascata", 1, BigDecimal.ONE);
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM dbo.item_pedido WHERE pedido_id = ?", Integer.class, id));
            jdbc.update("DELETE FROM dbo.pedido WHERE id = ?", id);
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dbo.item_pedido WHERE pedido_id = ?", Integer.class, id));
        } finally {
            if (jdbc.queryForObject("SELECT COUNT(*) FROM dbo.pedido WHERE id = ?", Integer.class, id) > 0) service.excluir(id);
        }
    }
}
