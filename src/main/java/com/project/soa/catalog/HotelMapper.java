package com.project.soa.catalog;

import java.util.Collections;
import java.util.stream.Collectors;

public final class HotelMapper {

    private HotelMapper() {}

    public static Hotel toEntity(HotelRequestDto dto) {
        Hotel hotel = new Hotel();
        hotel.setName(dto.getName());
        hotel.setLocation(dto.getLocation());
        hotel.setDescription(dto.getDescription());
        hotel.setCity(dto.getCity());
        hotel.setCountry(dto.getCountry());
        hotel.setAddress(dto.getAddress());
        hotel.setRating(dto.getRating());
        return hotel;
    }

    public static HotelResponseDto toDto(Hotel hotel) {
        HotelResponseDto dto = new HotelResponseDto();
        dto.setId(hotel.getId());
        dto.setName(hotel.getName());
        dto.setLocation(hotel.getLocation());
        dto.setDescription(hotel.getDescription());
        dto.setCity(hotel.getCity());
        dto.setCountry(hotel.getCountry());
        dto.setAddress(hotel.getAddress());
        dto.setRating(hotel.getRating());
        dto.setStatus(hotel.getStatus() != null ? hotel.getStatus().name() : null);
        dto.setCreatedAt(hotel.getCreatedAt());
        dto.setUpdatedAt(hotel.getUpdatedAt());
        // Expose manager identity so dashboards don't need a separate call
        if (hotel.getManager() != null) {
            dto.setManagerId(hotel.getManager().getId());
            dto.setManagerName(hotel.getManager().getName());
        }
        if (hotel.getRoomTypes() != null && !hotel.getRoomTypes().isEmpty()) {
            dto.setRoomTypes(hotel.getRoomTypes().stream()
                    .map(RoomTypeMapper::toDto)
                    .collect(Collectors.toList()));
        } else {
            dto.setRoomTypes(Collections.emptyList());
        }
        if (hotel.getPhotos() != null && !hotel.getPhotos().isEmpty()) {
            dto.setPhotos(hotel.getPhotos().stream()
                    .map(PhotoMapper::toDto)
                    .collect(Collectors.toList()));
        } else {
            dto.setPhotos(Collections.emptyList());
        }
        return dto;
    }

    public static HotelResponseDto toDtoSummary(Hotel hotel) {
        HotelResponseDto dto = new HotelResponseDto();
        dto.setId(hotel.getId());
        dto.setName(hotel.getName());
        dto.setLocation(hotel.getLocation());
        dto.setDescription(hotel.getDescription());
        dto.setCity(hotel.getCity());
        dto.setCountry(hotel.getCountry());
        dto.setAddress(hotel.getAddress());
        dto.setRating(hotel.getRating());
        dto.setStatus(hotel.getStatus() != null ? hotel.getStatus().name() : null);
        dto.setCreatedAt(hotel.getCreatedAt());
        dto.setUpdatedAt(hotel.getUpdatedAt());
        if (hotel.getManager() != null) {
            dto.setManagerId(hotel.getManager().getId());
            dto.setManagerName(hotel.getManager().getName());
        }
        dto.setRoomTypes(Collections.emptyList());
        dto.setPhotos(Collections.emptyList());
        return dto;
    }
}