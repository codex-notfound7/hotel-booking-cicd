package com.project.soa.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

public interface RoomTypeRepository extends JpaRepository<RoomType, UUID> {
    List<RoomType> findByHotelId(UUID hotelId);
    Optional<RoomType> findByHotelAndName(Hotel hotel, String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rt FROM RoomType rt WHERE rt.id = :id")
    Optional<RoomType> findByIdForUpdate(@Param("id") UUID id);
}
