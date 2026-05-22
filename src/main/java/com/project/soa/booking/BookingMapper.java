package com.project.soa.booking;

import com.project.soa.catalog.RoomType;

public final class BookingMapper { // i dont what this clas to be inherited , thats why its final

    private BookingMapper() {}

    public static BookingResponseDto toDto(Booking booking) {
        BookingResponseDto dto = new BookingResponseDto();
        dto.setId(booking.getId());
        dto.setUserId(booking.getUser() != null ? booking.getUser().getId() : null);
        dto.setRoomTypeId(booking.getRoomType() != null ? booking.getRoomType().getId() : null);
        if (booking.getRoomType() != null) {
            RoomType rt = booking.getRoomType();
            dto.setRoomTypeName(rt.getName());
            if (rt.getHotel() != null) {
                dto.setHotelName(rt.getHotel().getName());
            }
        }
        dto.setCheckIn(booking.getCheckIn());
        dto.setCheckOut(booking.getCheckOut());
        dto.setNumberOfGuests(booking.getNumberOfGuests());
        dto.setStatus(booking.getStatus() != null ? booking.getStatus().name() : null);
        dto.setTotalPrice(booking.getTotalPrice());
        dto.setCreatedAt(booking.getCreatedAt());
        dto.setUpdatedAt(booking.getUpdatedAt());
        return dto;
    }
}
