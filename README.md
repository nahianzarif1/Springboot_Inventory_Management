# Inventory Management (Spring Boot)

This project implements a simple inventory management system with:
- Users + roles (ADMIN/SELLER/BUYER)
- Products (owned by a SELLER)
- Cart and Orders (BUYER)
- REST endpoints + minimal Thymeleaf login/home pages

## Default seeded users
On startup, the app seeds 3 users:
- admin / admin123 (ROLE_ADMIN)
- seller / seller123 (ROLE_SELLER)
- buyer / buyer123 (ROLE_BUYER)

## Run locally
- Configure Postgres in `src/main/resources/application.yml` (defaults provided)
- Start the app from IDE or Maven.

## Run with Docker
Use `docker compose up --build` to start Postgres + app.

## UI pages
- `/` home
- `/login` login
- `/ui/products` products placeholder
- `/ui/cart` cart placeholder
- `/ui/orders` orders placeholder
- `/ui/admin` admin placeholder

## REST endpoints
AuthController
- POST `/auth/register`
- POST `/auth/login` (handled by Spring Security)
- POST `/auth/logout`

ProductController
- GET `/products`
- GET `/products/{id}`
- POST `/products`
- PUT `/products/{id}`
- DELETE `/products/{id}`

OrderController
- POST `/orders`
- GET `/orders`
- GET `/orders/{id}`
- PUT `/orders/{id}/status`

CartController
- POST `/cart/add`
- DELETE `/cart/remove`
- GET `/cart`
- POST `/cart/checkout` (creates an order from cart)

AdminController
- GET `/admin/users`
- PUT `/admin/users/{id}/role`
- DELETE `/admin/users/{id}`

## Notes
- Security rules are configured in `SecurityConfig`.
- Edge cases like negative price/stock, duplicate SKU, stock checks, and seller ownership checks are enforced in services.
