-- DimDim Pedidos: DDL inicial para SQL Server / Azure SQL Database.
-- Executar uma única vez em um banco novo. Não remove tabelas existentes.
-- O banco e o servidor serão provisionados separadamente pelo Azure CLI.
SET XACT_ABORT ON;

BEGIN TRANSACTION;

CREATE TABLE dbo.pedido (
    id BIGINT IDENTITY(1,1) NOT NULL,
    nome_cliente NVARCHAR(120) NOT NULL,
    data_pedido DATE NOT NULL,
    status VARCHAR(20) NOT NULL
        CONSTRAINT DF_pedido_status DEFAULT ('RASCUNHO'),
    CONSTRAINT PK_pedido PRIMARY KEY (id),
    CONSTRAINT CK_pedido_nome_cliente
        CHECK (LEN(LTRIM(RTRIM(nome_cliente))) > 0),
    CONSTRAINT CK_pedido_status
        CHECK (status IN ('RASCUNHO', 'CONFIRMADO', 'CANCELADO'))
);

CREATE TABLE dbo.item_pedido (
    id BIGINT IDENTITY(1,1) NOT NULL,
    pedido_id BIGINT NOT NULL,
    descricao_produto NVARCHAR(200) NOT NULL,
    quantidade INT NOT NULL,
    preco_unitario DECIMAL(12,2) NOT NULL,
    CONSTRAINT PK_item_pedido PRIMARY KEY (id),
    CONSTRAINT FK_item_pedido_pedido FOREIGN KEY (pedido_id)
        REFERENCES dbo.pedido (id) ON DELETE CASCADE,
    CONSTRAINT CK_item_pedido_descricao
        CHECK (LEN(LTRIM(RTRIM(descricao_produto))) > 0),
    CONSTRAINT CK_item_pedido_quantidade
        CHECK (quantidade BETWEEN 1 AND 10000),
    CONSTRAINT CK_item_pedido_preco_unitario
        CHECK (preco_unitario > 0)
);

CREATE INDEX IX_item_pedido_pedido_id ON dbo.item_pedido (pedido_id);

COMMIT TRANSACTION;
