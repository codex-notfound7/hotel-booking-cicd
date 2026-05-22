package com.project.soa.catalog;

public final class PhotoMapper {

    private PhotoMapper() {}

    public static Photo toEntity(CreatePhotoRequestDto dto, Hotel hotel, RoomType roomType) {
        Photo photo = new Photo();
        photo.setUrl(dto.getUrl());
        photo.setCaption(dto.getCaption());
        photo.setType(dto.getType());
        photo.setDisplayOrder(dto.getDisplayOrder());
        photo.setIsActive(dto.getIsActive());
        photo.setHotel(hotel);
        photo.setRoomType(roomType);
        return photo;
    }

    public static PhotoResponseDto toDto(Photo photo) {
        PhotoResponseDto dto = new PhotoResponseDto();
        dto.setId(photo.getId());
        dto.setUrl(photo.getUrl());
        dto.setCaption(photo.getCaption());
        dto.setType(photo.getType());
        dto.setDisplayOrder(photo.getDisplayOrder());
        dto.setIsActive(photo.getIsActive());
        dto.setCreatedAt(photo.getCreatedAt());
        dto.setUpdatedAt(photo.getUpdatedAt());
        
        if (photo.getHotel() != null) {
            dto.setHotelId(photo.getHotel().getId());
        }
        
        if (photo.getRoomType() != null) {
            dto.setRoomTypeId(photo.getRoomType().getId());
        }
        
        return dto;
    }

    public static void updateEntity(Photo photo, CreatePhotoRequestDto dto) {
        photo.setUrl(dto.getUrl());
        photo.setCaption(dto.getCaption());
        photo.setType(dto.getType());
        photo.setDisplayOrder(dto.getDisplayOrder());
        photo.setIsActive(dto.getIsActive());
    }
}
