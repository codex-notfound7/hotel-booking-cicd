[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/CU6l4amx)
# Hotel Booking System 


---

## Seeded Accounts

The app seeds these accounts automatically on startup:

| Email | Password | Role |
|---|---|---|
| admin@hotel.com | Admin@123 | ADMIN |
| manager@hotel.com | Manager@123 | MANAGER |
| guest@hotel.com | Guest@123 | GUEST |
| john@hotel.com | John@123 | GUEST |

---


## Project Structure

```
src/main/java/com/project/soa/
├── auth/               # JWT auth, refresh tokens, user management
├── catalog/            # Hotels, room types, photos
├── availability_pricing/  # Availability checks, pricing rules
├── booking/            # Booking lifecycle
├── payment/            # Mock payment processing
├── notification/       # Async email notifications
├── review/             # Hotel reviews
├── analytics/          # Admin dashboard stats
└── common/             # Shared config, error handling
```


## Modules

| Module | Future Microservice |
|---|---|
| catalog | Hotel Catalog Service |
| availability_pricing | Pricing Service |
| booking | Booking Service |
| payment | Payment Service |
| notification | Notification Service |
