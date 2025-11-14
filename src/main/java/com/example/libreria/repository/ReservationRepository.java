package com.example.libreria.repository;

import com.example.libreria.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    
    // TODO: Implementar los métodos de la reserva
    //Resulucion:
    
    // Busca reservaciones por el ID del usuario.     
    List<Reservation> findByUserId(Long userId);    
    //Busca reservaciones por un estado específico     
    List<Reservation> findByStatus(Reservation.ReservationStatus status);

    // Busca reservaciones activas cuya fecha de devolución esperada ya ha pasado (es anterior a la fecha actual).
    @Query("SELECT r FROM Reservation r WHERE r.status = 'ACTIVE' AND r.expectedReturnDate < CURRENT_DATE")
    List<Reservation> findOverdueReservations();
}

