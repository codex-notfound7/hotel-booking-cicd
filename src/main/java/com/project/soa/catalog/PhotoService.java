package com.project.soa.catalog;

import java.util.List;
import java.util.UUID;

public interface PhotoService {

    PhotoResponseDto createHotelPhoto(UUID hotelId, CreatePhotoRequestDto dto);

    PhotoResponseDto createRoomTypePhoto(UUID roomTypeId, CreatePhotoRequestDto dto);

    PhotoResponseDto updatePhoto(UUID photoId, CreatePhotoRequestDto dto);

    void deletePhoto(UUID photoId);

    List<PhotoResponseDto> getHotelPhotos(UUID hotelId);

    List<PhotoResponseDto> getHotelPhotosByType(UUID hotelId, PhotoType type);

    List<PhotoResponseDto> getActiveHotelPhotos(UUID hotelId);

    List<PhotoResponseDto> getRoomTypePhotos(UUID roomTypeId);

    List<PhotoResponseDto> getActiveRoomTypePhotos(UUID roomTypeId);

    PhotoResponseDto getPhoto(UUID photoId);
}
