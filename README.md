# Worker Archetype

This project is a Maven Archetype template for the generation of a new CAF worker's back-end module. Generation of a worker's back-end module should be after generation of the new CAF worker's shared module and before the generation of the new CAF worker's container module.

Order of a new CAF worker's module generation and build:

1. Worker Shared Module (worker-shared-archetype)
2. Worker Back-end Module (worker-archetype)
3. Worker Container Module (worker-container-archetype)