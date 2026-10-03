import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

// Utilitário de implantação. Executado pelo lançador de arquivos fonte do Java 21.
public class SqlSetup {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Informe o caminho do DDL.");
        try (Connection connection = DriverManager.getConnection(required("DB_URL"), required("DB_USERNAME"), required("DB_PASSWORD"))) {
            int tables;
            try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(
                    "SELECT COUNT(*) FROM sys.tables WHERE schema_id = SCHEMA_ID('dbo') AND name IN ('pedido','item_pedido')")) {
                rows.next(); tables = rows.getInt(1);
            }
            if (tables == 1) throw new IllegalStateException("Esquema incompleto: apenas uma das tabelas existe. Nenhuma tabela será removida.");
            if (tables == 0) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(Files.readString(Path.of(args[0]), StandardCharsets.UTF_8));
                }
                System.out.println("DDL aplicado: pedido e item_pedido criadas.");
            } else {
                System.out.println("As duas tabelas já existem. DDL preservado; Hibernate verificará o mapeamento.");
            }
            String password = required("DB_APP_PASSWORD");
            // Usuário fixo e senha gerada como caracteres ASCII pelo script, escapada por segurança.
            String escaped = password.replace("'", "''");
            String ddl = "IF DATABASE_PRINCIPAL_ID('dimdim_app') IS NULL CREATE USER dimdim_app WITH PASSWORD='" + escaped
                + "'; ELSE ALTER USER dimdim_app WITH PASSWORD='" + escaped + "';"
                + " GRANT SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.pedido TO dimdim_app;"
                + " GRANT SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.item_pedido TO dimdim_app;";
            try (Statement statement = connection.createStatement()) { statement.execute(ddl); }
            System.out.println("Usuário da aplicação configurado com permissões de CRUD nas duas tabelas.");
        } catch (SQLException ex) {
            // Não imprimir mensagens SQL completas: podem conter a instrução com credencial.
            throw new IllegalStateException("Falha SQL. Código=" + ex.getErrorCode() + ", estado=" + ex.getSQLState());
        }
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("Variável obrigatória: " + name);
        return value;
    }
}
