com.example.notification/
│
├── api/                    # Camada de Entrada (REST Controllers & DTOs)
│   ├── NotificationController.java
│   ├── dto/
│   │   ├── NotificationRequestDto.java
│   │   └── NotificationResponseDto.java
│   └── exception/          # Tratamento global de erros da API
│
├── application/            # Casos de Uso / Lógica de Aplicação
│   ├── CreateNotificationUseCase.java
│   └── GetNotificationUseCase.java
│
├── domain/                 # Regras de Negócio e Modelos de Domínio
│   ├── Notification.java
│   ├── NotificationStatus.java
│   └── NotificationChannel.java
│
├── infrastructure/         # Camada de Integração Externa e Persistência
│   ├── persistence/        # Repositórios JPA e Entities de Banco de Dados
│   │   ├── NotificationEntity.java
│   │   └── SpringDataNotificationRepository.java
│   ├── messaging/          # Produtores e Consumidores RabbitMQ
│   │   ├── NotificationPublisher.java
│   │   └── NotificationConsumer.java
│   └── provider/           # Integração com Provedores Externos (SendGrid, Twilio, etc.)
│       ├── EmailProviderClient.java
│       ├── SmsProviderClient.java
│       └── PushProviderClient.java
│
└── config/                 # Configurações do Spring (RabbitMQ, Jackson, etc.)
    └── RabbitMQConfig.java





                 ┌─────────────────┐
                 │      DOMAIN     │
                 │                 │
                 │ Notification    │
                 │ Enums           │
                 │ Business Rules  │
                 └────────▲────────┘
                          │
                          │
                 ┌────────┴────────┐
                 │   APPLICATION   │
                 │                 │
                 │ Use Cases       │
                 │ Input Ports     │
                 │ Output Ports    │
                 └────────▲────────┘
                          │
                ┌─────────┴──────────┐
                │                    │
        ┌───────┴───────┐    ┌───────┴───────┐
        │ ADAPTER IN    │    │  ADAPTER OUT  │
        │               │    │               │
        │ REST          │    │ PostgreSQL    │
        │ RabbitMQ      │    │ RabbitMQ      │
        └───────────────┘    └───────────────┘



















    src/
├── main/
│   ├── java/
│   │   └── com/github/alfredobaptista/notification/
│   │       │
│   │       ├── NotificationPlatformApplication.java
│   │       │
│   │       ├── domain/
│   │       │   ├── model/
│   │       │   │   ├── Notification.java
│   │       │   │   ├── NotificationChannel.java
│   │       │   │   ├── NotificationPriority.java
│   │       │   │   └── NotificationStatus.java
│   │       │   │
│   │       │   └── exception/
│   │       │       └── ...
│   │       │
│   │       ├── application/
│   │       │   ├── port/
│   │       │   │   ├── in/
│   │       │   │   │   └── CreateNotificationUseCase.java
│   │       │   │   │
│   │       │   │   └── out/
│   │       │   │       ├── NotificationRepository.java
│   │       │   │       └── NotificationPublisher.java
│   │       │   │
│   │       │   └── service/
│   │       │       └── CreateNotificationService.java
│   │       │
│   │       ├── adapter/
│   │       │   ├── in/
│   │       │   │   ├── web/
│   │       │   │   │   ├── NotificationController.java
│   │       │   │   │   ├── dto/
│   │       │   │   │   │   ├── NotificationRequestDto.java
│   │       │   │   │   │   └── NotificationResponseDto.java
│   │       │   │   │   └── exception/
│   │       │   │   │
│   │       │   │   └── messaging/
│   │       │   │       ├── EmailNotificationConsumer.java
│   │       │   │       ├── SmsNotificationConsumer.java
│   │       │   │       └── PushNotificationConsumer.java
│   │       │   │
│   │       │   └── out/
│   │       │       ├── persistence/
│   │       │       │   ├── NotificationEntity.java
│   │       │       │   ├── NotificationMapper.java
│   │       │       │   ├── SpringDataNotificationRepository.java
│   │       │       │   └── NotificationRepositoryAdapter.java
│   │       │       │
│   │       │       └── messaging/
│   │       │           └── RabbitMQNotificationPublisher.java
│   │       │
│   │       └── infrastructure/
│   │           └── config/
│   │               ├── RabbitMQConfig.java
│   │               └── ApplicationConfig.java
│   │
│   └── resources/
│       ├── application.yml
│       ├── application-dev.yml
│       ├── application-prod.yml
│       └── db/
│           └── migration/
│               └── V1__create_notifications_table.sql
│
└── test/
    └── java/
        └── com/github/alfredobaptista/notification/
            ├── application/
            ├── adapter/
            ├── domain/
            └── ...



A arquitectura de produção que eu recomendo

                         INTERNET
                            │
                            ▼
                    ┌───────────────┐
                    │    Railway    │
                    │               │
                    │ Spring Boot   │
                    │ notification  │
                    │ platform      │
                    └───────┬───────┘
                            │
              ┌─────────────┼─────────────┐
              │             │             │
              ▼             ▼             ▼
        PostgreSQL      RabbitMQ       Providers
        Railway         Railway        externos
                                      │
                              ┌───────┼────────┐
                              │       │        │
                            Brevo   Twilio   Push
                                      │      simulated