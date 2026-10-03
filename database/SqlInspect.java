import java.math.BigDecimal;
import java.sql.*;

// Consulta somente o pedido indicado, sem alterar registros ou exibir credenciais.
public class SqlInspect {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Informe o ID do pedido.");
        long id = Long.parseLong(args[0]);
        if (id <= 0) throw new IllegalArgumentException("ID deve ser positivo.");
        try (Connection connection = DriverManager.getConnection(System.getenv("DB_URL"), System.getenv("DB_USERNAME"), System.getenv("DB_PASSWORD"))) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT id, nome_cliente, data_pedido, status FROM dbo.pedido WHERE id = ?")) {
                statement.setLong(1, id);
                try (ResultSet rows = statement.executeQuery()) {
                    if (!rows.next()) throw new IllegalStateException("Pedido não encontrado no banco.");
                    System.out.printf("Pedido %d | Cliente: %s | Data: %s | Status: %s%n", rows.getLong(1), rows.getString(2), rows.getDate(3), rows.getString(4));
                }
            }
            BigDecimal total = new BigDecimal("0.00");
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id, pedido_id, descricao_produto, quantidade, preco_unitario FROM dbo.item_pedido WHERE pedido_id = ? ORDER BY id")) {
                statement.setLong(1, id);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        BigDecimal subtotal = rows.getBigDecimal(5).multiply(BigDecimal.valueOf(rows.getInt(4)));
                        total = total.add(subtotal);
                        System.out.printf("Item %d | FK pedido_id: %d | Produto: %s | Quantidade: %d | Preço: %s | Subtotal: %s%n",
                            rows.getLong(1), rows.getLong(2), rows.getString(3), rows.getInt(4), rows.getBigDecimal(5), subtotal);
                    }
                }
            }
            System.out.println("Total conferido no banco: " + total);
        } catch (SQLException ex) {
            throw new IllegalStateException("Falha SQL. Código=" + ex.getErrorCode() + ", estado=" + ex.getSQLState());
        }
    }
}
