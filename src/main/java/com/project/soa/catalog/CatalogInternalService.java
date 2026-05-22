package com.project.soa.catalog;

import java.util.Optional;
import java.util.UUID;

public interface CatalogInternalService {

    RoomType getRoomTypeForUpdate(UUID id);

    Optional<Hotel> findHotelById(UUID id);

    long countHotels();
}
