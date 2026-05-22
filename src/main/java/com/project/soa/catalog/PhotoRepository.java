package com.project.soa.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PhotoRepository extends JpaRepository<Photo, UUID> {

    List<Photo> findByHotelIdOrderByDisplayOrderAsc(UUID hotelId);

    List<Photo> findByHotelIdAndIsActiveOrderByDisplayOrderAsc(UUID hotelId, Boolean isActive);

    List<Photo> findByRoomTypeIdOrderByDisplayOrderAsc(UUID roomTypeId);

    List<Photo> findByRoomTypeIdAndIsActiveOrderByDisplayOrderAsc(UUID roomTypeId, Boolean isActive);

    @Query("SELECT p FROM Photo p WHERE p.hotel.id = :hotelId AND p.type = :type ORDER BY p.displayOrder ASC")
    List<Photo> findByHotelIdAndTypeOrderByDisplayOrderAsc(@Param("hotelId") UUID hotelId, @Param("type") PhotoType type);

    @Query("SELECT p FROM Photo p WHERE p.hotel.id = :hotelId AND p.type = :type AND p.isActive = :isActive ORDER BY p.displayOrder ASC")
    List<Photo> findByHotelIdAndTypeAndIsActiveOrderByDisplayOrderAsc(@Param("hotelId") UUID hotelId, 
                                                                    @Param("type") PhotoType type, 
                                                                    @Param("isActive") Boolean isActive);

    @Query("SELECT p FROM Photo p WHERE p.roomType.id = :roomTypeId AND p.type = :type ORDER BY p.displayOrder ASC")
    List<Photo> findByRoomTypeIdAndTypeOrderByDisplayOrderAsc(@Param("roomTypeId") UUID roomTypeId, 
                                                             @Param("type") PhotoType type);

    @Query("SELECT p FROM Photo p WHERE p.roomType.id = :roomTypeId AND p.type = :type AND p.isActive = :isActive ORDER BY p.displayOrder ASC")
    List<Photo> findByRoomTypeIdAndTypeAndIsActiveOrderByDisplayOrderAsc(@Param("roomTypeId") UUID roomTypeId, 
                                                                       @Param("type") PhotoType type, 
                                                                       @Param("isActive") Boolean isActive);

    @Query("SELECT COUNT(p) FROM Photo p WHERE p.hotel.id = :hotelId AND p.isActive = :isActive")
    long countByHotelIdAndIsActive(@Param("hotelId") UUID hotelId, @Param("isActive") Boolean isActive);

    @Query("SELECT MAX(p.displayOrder) FROM Photo p WHERE p.hotel.id = :hotelId")
    Integer findMaxDisplayOrderByHotelId(@Param("hotelId") UUID hotelId);

    @Query("SELECT MAX(p.displayOrder) FROM Photo p WHERE p.roomType.id = :roomTypeId")
    Integer findMaxDisplayOrderByRoomTypeId(@Param("roomTypeId") UUID roomTypeId);

    boolean existsByHotelIdAndUrl(UUID hotelId, String url);

    boolean existsByRoomTypeIdAndUrl(UUID roomTypeId, String url);
}
