package br.com.dimdim.pedidos.repository;

import br.com.dimdim.pedidos.domain.Pedido;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    @Override
    @EntityGraph(attributePaths = "itens")
    Optional<Pedido> findById(Long id);

    @EntityGraph(attributePaths = "itens")
    List<Pedido> findAllByOrderByIdDesc();

    // Todas as escritas bloqueiam primeiro o pedido para serializar alterações de seus itens.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :id")
    Optional<Pedido> buscarParaAlteracao(@Param("id") Long id);
}
