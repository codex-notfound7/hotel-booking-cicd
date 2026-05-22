package com.project.soa.review;


public final class ReviewMapper {
    private ReviewMapper() {}

    public static ReviewResponseDto toDto(Review r) {
        ReviewResponseDto dto = new ReviewResponseDto();
        dto.setId(r.getId());
        dto.setHotelId(r.getHotel().getId());
        dto.setHotelName(r.getHotel().getName());
        dto.setUserId(r.getUser().getId());
        dto.setUserName(r.getUser().getName());
        dto.setRating(r.getRating());
        dto.setComment(r.getComment());
        dto.setStatus(r.getStatus().name());
        dto.setCreatedAt(r.getCreatedAt());
        dto.setUpdatedAt(r.getUpdatedAt());
        return dto;
    }
}