package com.project.soa.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    @Query("SELECT r FROM Review r JOIN FETCH r.user JOIN FETCH r.hotel " +
            "WHERE r.hotel.id = :hotelId AND r.status = :status ORDER BY r.createdAt DESC")
    List<Review> findByHotelIdAndStatus(@Param("hotelId") UUID hotelId,
                                        @Param("status") ReviewStatus status);

    @Query("SELECT r FROM Review r JOIN FETCH r.user JOIN FETCH r.hotel ORDER BY r.createdAt DESC")
    List<Review> findAllWithDetails();

    @Query("SELECT r FROM Review r JOIN FETCH r.user JOIN FETCH r.hotel " +
            "WHERE r.hotel.id = :hotelId ORDER BY r.createdAt DESC")
    List<Review> findAllByHotelId(@Param("hotelId") UUID hotelId);

    Optional<Review> findByUserIdAndHotelId(UUID userId, UUID hotelId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.hotel.id = :hotelId AND r.status = 'VISIBLE'")
    Double averageRatingByHotelId(@Param("hotelId") UUID hotelId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.status = 'VISIBLE'")
    Double getOverallAverageRating();
}