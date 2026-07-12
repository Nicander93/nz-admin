# Optional Redis monitoring

nz-starter-monitor does not force Redis onto an application. Redis metrics are activated only when both Spring Data Redis and a RedisConnectionFactory are present.

To enable it, add nz-starter-cache (or spring-boot-starter-data-redis) to nz-app and configure spring.data.redis. The existing /api/system/monitor/summary response then includes:

- connection health and failure message;
- Redis version and deployment mode;
- connected clients;
- used memory;
- current database key count.

Without Redis, redisAvailable is false; the endpoint remains healthy and the frontend displays Not configured. No database migration is required for this slice because it reuses the existing system:monitor:query menu permission and does not persist data.