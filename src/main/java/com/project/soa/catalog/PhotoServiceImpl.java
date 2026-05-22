package com.project.soa.catalog;

import com.project.soa.common.exception.BusinessRuleException;
import com.project.soa.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PhotoServiceImpl implements PhotoService {

    private final PhotoRepository photoRepository;
    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;

    public PhotoServiceImpl(PhotoRepository photoRepository,
                            HotelRepository hotelRepository,
                            RoomTypeRepository roomTypeRepository) {
        this.photoRepository    = photoRepository;
        this.hotelRepository    = hotelRepository;
        this.roomTypeRepository = roomTypeRepository;
    }

    @Override
    public PhotoResponseDto createHotelPhoto(UUID hotelId, CreatePhotoRequestDto dto) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", hotelId));
        validatePhotoUrl(hotelId, null, dto.getUrl());
        Photo photo = PhotoMapper.toEntity(dto, hotel, null);
        Integer maxOrder = photoRepository.findMaxDisplayOrderByHotelId(hotelId);
        photo.setDisplayOrder(maxOrder != null ? maxOrder + 1 : 0);
        return PhotoMapper.toDto(photoRepository.save(photo));
    }

    @Override
    public PhotoResponseDto createRoomTypePhoto(UUID roomTypeId, CreatePhotoRequestDto dto) {
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("RoomType", roomTypeId));
        validatePhotoUrl(null, roomTypeId, dto.getUrl());
        Photo photo = PhotoMapper.toEntity(dto, null, roomType);
        Integer maxOrder = photoRepository.findMaxDisplayOrderByRoomTypeId(roomTypeId);
        photo.setDisplayOrder(maxOrder != null ? maxOrder + 1 : 0);
        return PhotoMapper.toDto(photoRepository.save(photo));
    }

    @Override
    public PhotoResponseDto updatePhoto(UUID photoId, CreatePhotoRequestDto dto) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo", photoId));
        if (!dto.getUrl().equals(photo.getUrl())) {
            UUID hotelId    = photo.getHotel()    != null ? photo.getHotel().getId()    : null;
            UUID roomTypeId = photo.getRoomType() != null ? photo.getRoomType().getId() : null;
            validatePhotoUrl(hotelId, roomTypeId, dto.getUrl());
        }
        PhotoMapper.updateEntity(photo, dto);
        return PhotoMapper.toDto(photoRepository.save(photo));
    }

    @Override
    public void deletePhoto(UUID photoId) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo", photoId));
        photoRepository.delete(photo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PhotoResponseDto> getHotelPhotos(UUID hotelId) {
        return photoRepository.findByHotelIdOrderByDisplayOrderAsc(hotelId)
                .stream().map(PhotoMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PhotoResponseDto> getHotelPhotosByType(UUID hotelId, PhotoType type) {
        return photoRepository.findByHotelIdAndTypeOrderByDisplayOrderAsc(hotelId, type)
                .stream().map(PhotoMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PhotoResponseDto> getActiveHotelPhotos(UUID hotelId) {
        return photoRepository.findByHotelIdAndIsActiveOrderByDisplayOrderAsc(hotelId, true)
                .stream().map(PhotoMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PhotoResponseDto> getRoomTypePhotos(UUID roomTypeId) {
        return photoRepository.findByRoomTypeIdOrderByDisplayOrderAsc(roomTypeId)
                .stream().map(PhotoMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PhotoResponseDto> getActiveRoomTypePhotos(UUID roomTypeId) {
        return photoRepository.findByRoomTypeIdAndIsActiveOrderByDisplayOrderAsc(roomTypeId, true)
                .stream().map(PhotoMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PhotoResponseDto getPhoto(UUID photoId) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo", photoId));
        return PhotoMapper.toDto(photo);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void validatePhotoUrl(UUID hotelId, UUID roomTypeId, String url) {
        if (hotelId != null && photoRepository.existsByHotelIdAndUrl(hotelId, url)) {
            throw new BusinessRuleException("Photo URL already exists for this hotel.");
        }
        if (roomTypeId != null && photoRepository.existsByRoomTypeIdAndUrl(roomTypeId, url)) {
            throw new BusinessRuleException("Photo URL already exists for this room type.");
        }
    }
}
