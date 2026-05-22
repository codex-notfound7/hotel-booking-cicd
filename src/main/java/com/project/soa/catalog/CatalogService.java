package com.project.soa.catalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CatalogService {

    HotelListResponseDto browseActiveHotels(int page, int size,
                                            String name, String city, String area,
                                            String country,
                                            BigDecimal minPrice, BigDecimal maxPrice,
                                            String roomType, List<String> amenities,
                                            Integer minRating,
                                            String sortBy, String sortDir);

    HotelResponseDto getActiveHotelDetails(UUID id);

    List<RoomTypeResponseDto> getRoomTypesForActiveHotel(UUID hotelId);

    List<HotelResponseDto> getAllHotelsForManager(UUID managerId);

    HotelResponseDto getHotelByIdForManager(UUID id, UUID managerId);

    List<RoomTypeResponseDto> getRoomTypesByHotelForManager(UUID hotelId, UUID managerId);

    Hotel createHotel(HotelRequestDto dto, UUID managerId);

    Hotel updateHotel(UUID id, HotelRequestDto dto, UUID managerId);

    void deleteHotel(UUID id, UUID managerId);

    RoomType createRoomType(UUID hotelId, RoomTypeRequestDto dto, UUID managerId);

    RoomType updateRoomType(UUID hotelId, UUID roomTypeId, RoomTypeRequestDto dto, UUID managerId);

    void deleteRoomType(UUID hotelId, UUID roomTypeId, UUID managerId);
}
