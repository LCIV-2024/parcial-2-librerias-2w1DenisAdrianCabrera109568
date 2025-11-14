package com.example.libreria.service;

import com.example.libreria.dto.ReservationRequestDTO;
import com.example.libreria.dto.ReservationResponseDTO;
import com.example.libreria.dto.ReturnBookRequestDTO;
import com.example.libreria.model.Book;
import com.example.libreria.model.Reservation;
import com.example.libreria.model.User;
import com.example.libreria.repository.BookRepository;
import com.example.libreria.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {
    
    private static final BigDecimal LATE_FEE_PERCENTAGE = new BigDecimal("0.15"); // 15% por día
    
    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final BookService bookService;
    private final UserService userService;
    
    @Transactional
    public ReservationResponseDTO createReservation(ReservationRequestDTO requestDTO) {

        // TODO: Implementar la creación de una reserva        
        // 1. Validar que el usuario existe
        User user = userService.getUserEntity(requestDTO.getUserId());
        // 2. Validar que el libro existe
        Book book = bookRepository.findByExternalId(requestDTO.getBookExternalId())
                .orElseThrow(() -> new RuntimeException("Libro no encontrado con ID externo: " + requestDTO.getBookExternalId()));
                
        // 3. Reducir la cantidad disponible el método ya valida el stock si falla (no hay stock), lanza excepción y la transacción hace rollback.     
        bookService.decreaseAvailableQuantity(book.getExternalId());

        // 4. Crear la reserva (Solo si el paso 3 fue exitoso)
        Reservation reservation = new Reservation();
        reservation.setUser(user); 
        reservation.setBook(book);
        reservation.setRentalDays(requestDTO.getRentalDays());
        reservation.setStartDate(requestDTO.getStartDate());    
        LocalDate expectedReturnDate = requestDTO.getStartDate().plusDays(requestDTO.getRentalDays());
        reservation.setExpectedReturnDate(expectedReturnDate);

        //5. Calcular la tarifa diaria y el total de la reserva
        reservation.setDailyRate(book.getPrice()); 
        reservation.setTotalFee(calculateTotalFee(book.getPrice(), requestDTO.getRentalDays()));        
        reservation.setStatus(Reservation.ReservationStatus.ACTIVE);
        reservation.setLateFee(BigDecimal.ZERO);
        Reservation savedReservation = reservationRepository.save(reservation);
        return convertToDTO(savedReservation);
    
    }
    
    @Transactional
    public ReservationResponseDTO returnBook(Long reservationId, ReturnBookRequestDTO returnRequest) {

        // TODO: Implementar la devolución de un libro
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada con ID: " + reservationId));
        
        if (reservation.getStatus() != Reservation.ReservationStatus.ACTIVE) {
            throw new RuntimeException("La reserva ya fue devuelta");
        }
        
        LocalDate returnDate = returnRequest.getReturnDate();
        reservation.setActualReturnDate(returnDate);
        reservation.setStatus(Reservation.ReservationStatus.RETURNED);

        LocalDate expectedReturnDate = reservation.getExpectedReturnDate(); 
        BigDecimal lateFee = BigDecimal.ZERO;

        // 1. Comparamos si la devolución es tardía
        if (returnDate.isAfter(expectedReturnDate)) {
        long daysLate = ChronoUnit.DAYS.between(expectedReturnDate, returnDate);

        if (daysLate > 0) {
            lateFee = calculateLateFee(reservation.getBook().getPrice(), daysLate);
            }
        }
        // 2. Asignamos la multa (sea 0 o el valor calculado) a la reserva
         reservation.setLateFee(lateFee);  

        // Aumentar la cantidad disponible
        bookService.increaseAvailableQuantity(reservation.getBook().getExternalId());    
        Reservation updatedReservation = reservationRepository.save(reservation);
        return convertToDTO(updatedReservation);
    
        


    }
    
    @Transactional(readOnly = true)
    public ReservationResponseDTO getReservationById(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada con ID: " + id));
        return convertToDTO(reservation);
    }
    
    @Transactional(readOnly = true)
    public List<ReservationResponseDTO> getAllReservations() {
        return reservationRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ReservationResponseDTO> getReservationsByUserId(Long userId) {
        return reservationRepository.findByUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ReservationResponseDTO> getActiveReservations() {
        return reservationRepository.findByStatus(Reservation.ReservationStatus.ACTIVE).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ReservationResponseDTO> getOverdueReservations() {
        return reservationRepository.findOverdueReservations().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    private BigDecimal calculateTotalFee(BigDecimal dailyRate, Integer rentalDays) {
        // TODO: Implementar el cálculo del total de la reserva
        //Resolución:
        if (dailyRate == null || rentalDays == null || rentalDays < 0) {
            return BigDecimal.ZERO;
        }
        return dailyRate.multiply(new BigDecimal(rentalDays));
        
    }
    
    private BigDecimal calculateLateFee(BigDecimal bookPrice, long daysLate) {
        // 15% del precio del libro por cada día de demora
        // TODO: Implementar el cálculo de la multa por demora
        //Resolución:
        if (bookPrice == null || daysLate <= 0) {
            return BigDecimal.ZERO;
        }        
        // Multa diaria = 15% del precio
        BigDecimal dailyLateFee = bookPrice.multiply(LATE_FEE_PERCENTAGE);        
        // Multa total = Multa diaria * días de demora
        BigDecimal totalLateFee = dailyLateFee.multiply(new BigDecimal(daysLate));        
        // Redondear a 2 decimales
        return totalLateFee.setScale(2, RoundingMode.HALF_UP);
    }
    
    private ReservationResponseDTO convertToDTO(Reservation reservation) {
        ReservationResponseDTO dto = new ReservationResponseDTO();
        dto.setId(reservation.getId());
        dto.setUserId(reservation.getUser().getId());
        dto.setUserName(reservation.getUser().getName());
        dto.setBookExternalId(reservation.getBook().getExternalId());
        dto.setBookTitle(reservation.getBook().getTitle());
        dto.setRentalDays(reservation.getRentalDays());
        dto.setStartDate(reservation.getStartDate());
        dto.setExpectedReturnDate(reservation.getExpectedReturnDate());
        dto.setActualReturnDate(reservation.getActualReturnDate());
        dto.setDailyRate(reservation.getDailyRate());
        dto.setTotalFee(reservation.getTotalFee());
        dto.setLateFee(reservation.getLateFee());
        dto.setStatus(reservation.getStatus());
        dto.setCreatedAt(reservation.getCreatedAt());
        return dto;
    }
}

