package com.gestionalmacen.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gestionalmacen.entity.Sale;

// Acceso a la tabla ventas. Los renglones y el pago se guardan junto con la venta.
@Repository
public interface ISaleRepository extends JpaRepository<Sale, Long> {
}
