package com.project.soa.catalog;

import java.util.Collections;

public final class RoomTypeMapper {

    private RoomTypeMapper() {}

    public static RoomType toEntity(RoomTypeRequestDto dto, Hotel hotel) {
        RoomType roomType = new RoomType();
        roomType.setName(dto.getName());
        roomType.setDescription(dto.getDescription());
        roomType.setCapacity(dto.getCapacity());
        roomType.setBasePrice(dto.getBasePrice());

        roomType.setTotalRooms(dto.getTotalRooms());
        roomType.setHotel(hotel);
        if (dto.getAmenities() != null) {
            roomType.setAmenities(dto.getAmenities());
        }
        return roomType;
    }

    public static RoomTypeResponseDto toDto(RoomType roomType) {
        RoomTypeResponseDto dto = new RoomTypeResponseDto();
        dto.setId(roomType.getId());
        dto.setName(roomType.getName());
        dto.setDescription(roomType.getDescription());
        dto.setBasePrice(roomType.getBasePrice());
        dto.setCapacity(roomType.getCapacity());
        dto.setTotalRooms(roomType.getTotalRooms());
        dto.setAmenities(roomType.getAmenities() != null ? roomType.getAmenities() : Collections.emptyList());
        dto.setStatus(roomType.getStatus() != null ? roomType.getStatus().name() : null);
        dto.setCreatedAt(roomType.getCreatedAt());
        dto.setUpdatedAt(roomType.getUpdatedAt());
        if (roomType.getHotel() != null) {
            dto.setHotelId(roomType.getHotel().getId());
        }
        return dto;
    }
}