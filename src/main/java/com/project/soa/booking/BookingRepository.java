package com.project.soa.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByUserIdOrderByCheckInDesc(UUID userId);


    List<Booking> findByCheckInGreaterThanEqualOrderByCheckInAsc(LocalDate today);

    @Query("SELECT COUNT(b) FROM Booking b " +
            "WHERE b.roomType.id = :roomTypeId " +
            "AND b.checkOut > :today " +
            "AND b.status <> com.project.soa.booking.BookingStatus.CANCELLED")
    long countActiveOrUpcomingNotCancelledForRoomType(@Param("roomTypeId") UUID roomTypeId,
                                                      @Param("today") LocalDate today);

    @Query("SELECT b FROM Booking b " +
            "WHERE b.checkIn >= :today " +
            "AND b.status <> com.project.soa.booking.BookingStatus.CANCELLED " +
            "ORDER BY b.checkIn ASC")
    List<Booking> findUpcomingNotCancelled(@Param("today") LocalDate today);

    @Query("SELECT b FROM Booking b " +
            "WHERE b.roomType.hotel.manager.id = :managerId " +
            "AND b.checkIn >= :today " +
            "AND b.status <> com.project.soa.booking.BookingStatus.CANCELLED " +
            "ORDER BY b.checkIn ASC")
    List<Booking> findUpcomingNotCancelledForManager(@Param("managerId") UUID managerId,
                                                     @Param("today") LocalDate today);


    @Query("SELECT b FROM Booking b " +
            "WHERE b.roomType.id = :roomTypeId " +
            "AND b.status = com.project.soa.booking.BookingStatus.CONFIRMED " +
            "AND b.checkIn < :checkOut " +
            "AND b.checkOut > :checkIn")
    List<Booking> findConfirmedBookingsForRoomTypeInRange(@Param("roomTypeId") UUID roomTypeId,
                                                          @Param("checkIn") LocalDate checkIn,
                                                          @Param("checkOut") LocalDate checkOut);

    @Query("SELECT b FROM Booking b " +
            "WHERE b.roomType.id = :roomTypeId " +
            "AND b.checkIn < :checkOut " +
            "AND b.checkOut > :checkIn " +
            "AND (" +
            "  b.status = com.project.soa.booking.BookingStatus.CONFIRMED " +
            "  OR (b.status = com.project.soa.booking.BookingStatus.PENDING AND b.pendingExpiresAt > :now)" +
            ")")
    List<Booking> findBlockingBookingsForRoomTypeInRange(@Param("roomTypeId") UUID roomTypeId,
                                                         @Param("checkIn") LocalDate checkIn,
                                                         @Param("checkOut") LocalDate checkOut,
                                                         @Param("now") LocalDateTime now);

    @Query("SELECT b FROM Booking b " +
            "JOIN FETCH b.roomType rt " +
            "JOIN FETCH rt.hotel")
    List<Booking> findAllWithHotelDetails();
}