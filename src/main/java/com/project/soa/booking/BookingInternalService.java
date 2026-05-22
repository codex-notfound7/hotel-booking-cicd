package com.project.soa.booking;

import java.util.List;

public interface BookingInternalService {

    List<Booking> findAllWithHotelDetails();
}
