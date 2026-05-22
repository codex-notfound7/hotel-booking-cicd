package com.project.soa.catalog;

import com.project.soa.common.exception.ErrorResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;


@RestController
@Tag(name = "Catalog", description = "Hotels and room types")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    // ── Guest: browse & search ────────────────────────────────────────────────

    @GetMapping("/api/hotels")
    @PreAuthorize("hasAnyRole('GUEST', 'MANAGER')")
    @Operation(
            summary = "Browse & search active hotels",
            description = "Returns ACTIVE hotels only. " +
                    "Search by name (partial, case-insensitive) and/or location (city or area). " +
                    "Also supports filtering by minPrice, maxPrice, roomType, amenities, minRating. " +
                    "Sort by: name, rating, city, createdAt. " +
                    "Paginated — use `page` (0-based) and `size`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Paginated list of active hotels returned successfully",
                    content = @Content(schema = @Schema(implementation = HotelListResponseDto.class))),
            @ApiResponse(responseCode = "400",
                    description = "Invalid sort field or direction",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "403",
                    description = "Access denied — GUEST or MANAGER role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public HotelListResponseDto browseHotels(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) List<String> amenities,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc")  String sortDir) {
        return catalogService.browseActiveHotels(
                page, size, name, city, area, country,
                minPrice, maxPrice, roomType, amenities, minRating,
                sortBy, sortDir);
    }

    @GetMapping("/api/hotels/{id}")
    @PreAuthorize("hasAnyRole('GUEST', 'MANAGER')")
    @Operation(summary = "Get active hotel details",
            description = "Returns a single ACTIVE hotel with its room types and photos. " +
                    "Returns 404 if the hotel does not exist or is INACTIVE.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hotel details returned successfully",
                    content = @Content(schema = @Schema(implementation = HotelResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Hotel not found or INACTIVE",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public HotelResponseDto getHotelDetails(@PathVariable UUID id) {
        return catalogService.getActiveHotelDetails(id);
    }

    @GetMapping("/api/hotels/{hotelId}/room-types")
    @PreAuthorize("hasAnyRole('GUEST', 'MANAGER')")
    @Operation(summary = "List room types for an active hotel",
            description = "Returns ACTIVE room types for an ACTIVE hotel. " +
                    "Returns 404 if the hotel is INACTIVE.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of room types returned successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Hotel not found or INACTIVE",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public List<RoomTypeResponseDto> getRoomTypes(@PathVariable UUID hotelId) {
        return catalogService.getRoomTypesForActiveHotel(hotelId);
    }

    // ── Manager: CRUD ─────────────────────────────────────────────────────────

    @GetMapping("/api/manager/hotels")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "List my hotels (all statuses)",
            description = "Returns all hotels owned by the authenticated manager, including INACTIVE ones.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of manager's hotels returned successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "403",
                    description = "Access denied — MANAGER role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public List<HotelResponseDto> listMyHotels(@AuthenticationPrincipal Jwt jwt) {
        return catalogService.getAllHotelsForManager(managerId(jwt));
    }

    @GetMapping("/api/manager/hotels/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Get one of my hotels (any status)",
            description = "Returns the hotel only if it is owned by the authenticated manager.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hotel returned successfully",
                    content = @Content(schema = @Schema(implementation = HotelResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "403",
                    description = "Access denied — not the owner of this hotel",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Hotel not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public HotelResponseDto getMyHotel(@PathVariable UUID id,
                                       @AuthenticationPrincipal Jwt jwt) {
        return catalogService.getHotelByIdForManager(id, managerId(jwt));
    }

    @GetMapping("/api/manager/hotels/{hotelId}/room-types")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "List room types for one of my hotels",
            description = "Returns all room types (any status) for a hotel owned by the authenticated manager.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Room types returned successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "403",
                    description = "Access denied — not the owner of this hotel",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Hotel not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public List<RoomTypeResponseDto> getMyRoomTypes(@PathVariable UUID hotelId,
                                                    @AuthenticationPrincipal Jwt jwt) {
        return catalogService.getRoomTypesByHotelForManager(hotelId, managerId(jwt));
    }

    @PostMapping("/api/manager/hotels")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Create a hotel",
            description = "Creates a new ACTIVE hotel owned by the authenticated manager.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Hotel created successfully",
                    content = @Content(schema = @Schema(implementation = HotelResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed (e.g. missing name or city)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "403",
                    description = "Access denied — MANAGER role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<HotelResponseDto> createHotel(
            @Valid @RequestBody HotelRequestDto dto,
            @AuthenticationPrincipal Jwt jwt,
            UriComponentsBuilder uriBuilder) {
        Hotel hotel = catalogService.createHotel(dto, managerId(jwt));
        HotelResponseDto response = HotelMapper.toDto(hotel);
        return ResponseEntity.created(
                        uriBuilder.path("/api/hotels/{id}").buildAndExpand(hotel.getId()).toUri())
                .body(response);
    }

    @PutMapping("/api/manager/hotels/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Update a hotel",
            description = "Updates a hotel owned by the authenticated manager.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hotel updated successfully",
                    content = @Content(schema = @Schema(implementation = HotelResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "403",
                    description = "Access denied — not the owner of this hotel",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Hotel not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public HotelResponseDto updateHotel(@PathVariable UUID id,
                                        @Valid @RequestBody HotelRequestDto dto,
                                        @AuthenticationPrincipal Jwt jwt) {
        return HotelMapper.toDto(catalogService.updateHotel(id, dto, managerId(jwt)));
    }

    @DeleteMapping("/api/manager/hotels/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Delete a hotel",
            description = "Deletes a hotel owned by the authenticated manager. " +
                    "Fails if the hotel has active or upcoming bookings.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Hotel deleted successfully"),
            @ApiResponse(responseCode = "400",
                    description = "Cannot delete — hotel has active upcoming bookings",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "403",
                    description = "Access denied — not the owner of this hotel",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Hotel not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<Void> deleteHotel(@PathVariable UUID id,
                                            @AuthenticationPrincipal Jwt jwt) {
        catalogService.deleteHotel(id, managerId(jwt));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/manager/hotels/{hotelId}/room-types")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Add a room type to one of my hotels")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Room type created successfully",
                    content = @Content(schema = @Schema(implementation = RoomTypeResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "403",
                    description = "Access denied — not the owner of this hotel",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Hotel not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<RoomTypeResponseDto> createRoomType(
            @PathVariable UUID hotelId,
            @Valid @RequestBody RoomTypeRequestDto dto,
            @AuthenticationPrincipal Jwt jwt,
            UriComponentsBuilder uriBuilder) {
        RoomType rt = catalogService.createRoomType(hotelId, dto, managerId(jwt));
        RoomTypeResponseDto response = RoomTypeMapper.toDto(rt);
        return ResponseEntity.created(
                        uriBuilder.path("/api/hotels/{hotelId}/room-types/{rtId}")
                                .buildAndExpand(hotelId, rt.getId()).toUri())
                .body(response);
    }

    @PutMapping("/api/manager/hotels/{hotelId}/room-types/{roomTypeId}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Update a room type")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Room type updated successfully",
                    content = @Content(schema = @Schema(implementation = RoomTypeResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "403",
                    description = "Access denied — not the owner of this hotel",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Hotel or room type not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public RoomTypeResponseDto updateRoomType(
            @PathVariable UUID hotelId,
            @PathVariable UUID roomTypeId,
            @Valid @RequestBody RoomTypeRequestDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        return RoomTypeMapper.toDto(
                catalogService.updateRoomType(hotelId, roomTypeId, dto, managerId(jwt)));
    }

    @DeleteMapping("/api/manager/hotels/{hotelId}/room-types/{roomTypeId}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Delete a room type",
            description = "Fails if the room type has active or upcoming bookings.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Room type deleted successfully"),
            @ApiResponse(responseCode = "400",
                    description = "Cannot delete — room type has active upcoming bookings",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "403",
                    description = "Access denied — not the owner of this hotel",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Hotel or room type not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<Void> deleteRoomType(
            @PathVariable UUID hotelId,
            @PathVariable UUID roomTypeId,
            @AuthenticationPrincipal Jwt jwt) {
        catalogService.deleteRoomType(hotelId, roomTypeId, managerId(jwt));
        return ResponseEntity.noContent().build();
    }

    private UUID managerId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
