package com.project.soa.catalog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class HotelResponseDto {
    private UUID id;
    private String name;
    private String location;
    private String description;
    private String city;
    private String country;
    private String address;
    private Integer rating;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<RoomTypeResponseDto> roomTypes;
    private List<PhotoResponseDto> photos;

    private UUID managerId;
    private String managerName;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<RoomTypeResponseDto> getRoomTypes() { return roomTypes; }
    public void setRoomTypes(List<RoomTypeResponseDto> roomTypes) { this.roomTypes = roomTypes; }
    public List<PhotoResponseDto> getPhotos() { return photos; }
    public void setPhotos(List<PhotoResponseDto> photos) { this.photos = photos; }
    public UUID getManagerId() { return managerId; }
    public void setManagerId(UUID managerId) { this.managerId = managerId; }
    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }
}