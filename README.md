# Notification Platform

> Plataforma assíncrona e multicanal de notificações, desenvolvida com Java 21 e Spring Boot, com processamento orientado a eventos através do RabbitMQ.

Uma plataforma backend concebida para processar notificações de forma **assíncrona, desacoplada e resiliente**, suportando diferentes canais de entrega através de uma arquitectura extensível.

O projecto foi desenvolvido com foco em problemas reais de engenharia de software: **processamento assíncrono, desacoplamento, resiliência, idempotência, persistência, testes de integração e evolução independente dos canais de notificação**.

---

## 🎯 Objectivo

Em aplicações distribuídas, enviar uma notificação directamente durante o processamento de uma requisição HTTP pode criar vários problemas:

* aumentar o tempo de resposta da API;
* acoplar o domínio aos fornecedores externos;
* tornar falhas de Email/SMS/Push capazes de afectar a operação principal;
* dificultar retries;
* dificultar o controlo do estado de entrega;
* tornar a adição de novos canais mais complexa.

A **Notification Platform** separa a criação da notificação da sua entrega.

O fluxo principal é:

```text
Client
   │
   ▼
REST API
   │
   ▼
Notification
   │
   ├──────────────► PostgreSQL
   │
   ▼
RabbitMQ
   │
   ▼
Notification Consumer
   │
   ▼
Channel Strategy
   │
   ├──► Email
   ├──► SMS
   └──► Push
```

Desta forma, a API não precisa aguardar pela conclusão do fornecedor externo para concluir a operação de criação da notificação.

---

# 🏗️ Arquitectura

O projecto segue princípios de **Clean Architecture**, **Hexagonal Architecture** e conceitos de **Domain-Driven Design (DDD)**.

A organização procura manter o domínio independente de detalhes de infraestrutura, frameworks e fornecedores externos.

```text
┌─────────────────────────────────────────────┐
│                  REST API                   │
│            Inbound Adapter                  │
└──────────────────────┬──────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────┐
│                Application                  │
│                                             │
│  Use Cases / Ports / Application Services   │
└──────────────────────┬──────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────┐
│                   Domain                    │
│                                             │
│ Entities / Value Objects / Business Rules   │
└──────────────────────┬──────────────────────┘
                       │
             ┌─────────┴──────────┐
             ▼                    ▼
┌──────────────────────┐ ┌────────────────────┐
│   PostgreSQL/JPA     │ │     RabbitMQ       │
│   Outbound Adapter   │ │  Messaging Adapter │
└──────────────────────┘ └─────────┬──────────┘
                                    │
                                    ▼
                         ┌────────────────────┐
                         │ Notification       │
                         │ Consumer           │
                         └─────────┬──────────┘
                                   │
                                   ▼
                         ┌────────────────────┐
                         │ Channel Strategy   │
                         ├────────────────────┤
                         │ Email              │
                         │ SMS                │
                         │ Push               │
                         └────────────────────┘
```

### Princípios aplicados

* **Clean Architecture**
* **Hexagonal Architecture**
* **Domain-Driven Design**
* **Dependency Inversion**
* **Separation of Concerns**
* **SOLID**
* **Strategy Pattern**
* **Ports and Adapters**
* **Event-driven processing**

---

# 🔄 Processamento assíncrono

O RabbitMQ é utilizado como broker de mensagens entre a criação da notificação e o processamento da entrega.

Fluxo conceptual:

```text
POST /notifications
        │
        ▼
Create Notification
        │
        ├──────────────► PostgreSQL
        │
        ▼
Publish Event
        │
        ▼
     RabbitMQ
        │
        ▼
Notification Consumer
        │
        ▼
Resolve Channel
        │
        ▼
Channel Strategy
        │
        ├──── Email
        ├──── SMS
        └──── Push
```

Esta abordagem permite desacoplar:

**produção da notificação**

de

**processamento da entrega**.

---

# 📢 Estratégia para canais

Os diferentes canais de comunicação não são implementados como condicionais espalhadas pelo sistema.

É utilizado o **Strategy Pattern**, permitindo que cada canal tenha a sua própria implementação.

Conceito:

```text
NotificationChannelStrategy
          │
          ├── EmailNotificationStrategy
          │
          ├── SmsNotificationStrategy
          │
          └── PushNotificationStrategy
```

Isto permite adicionar novos canais sem alterar significativamente o fluxo principal da aplicação.

Por exemplo, um novo canal como WhatsApp pode ser introduzido através de uma nova implementação da estratégia, mantendo o restante fluxo desacoplado.

---

# 🧩 Principais componentes

## REST API

Responsável por receber os pedidos de criação de notificações.

A API valida os dados de entrada e inicia o processamento assíncrono.

---

## Application Layer

Contém os casos de uso e a orquestração da aplicação.

Esta camada não deve depender directamente de implementações concretas de infraestrutura.

---

## Domain Layer

Representa as regras e conceitos fundamentais do domínio de notificações.

O objectivo é manter as regras de negócio independentes de:

* PostgreSQL;
* RabbitMQ;
* APIs externas;
* Spring;
* fornecedores de comunicação.

---

## Persistence

O PostgreSQL é utilizado para persistência das notificações.

O acesso aos dados é realizado através de:

* Spring Data JPA;
* Hibernate;
* PostgreSQL;
* Flyway.

As alterações do schema são controladas através de migrations versionadas.

---

## Messaging

O RabbitMQ é responsável pelo processamento assíncrono das notificações.

A utilização de uma fila permite que a aplicação:

* desacople produtores e consumidores;
* processe notificações em background;
* suporte crescimento horizontal dos consumidores;
* isole falhas dos fornecedores externos;
* implemente mecanismos de retry.

---

# 🛡️ Resiliência

Uma plataforma de notificações precisa considerar que fornecedores externos podem falhar.

O desenho do projecto considera cenários como:

```text
Notification
     │
     ▼
 RabbitMQ
     │
     ▼
 Consumer
     │
     ▼
 Provider
     │
     ├── SUCCESS ─────► SENT
     │
     └── FAILURE
            │
            ▼
          RETRY
            │
            ├── SUCCESS
            │
            └── FINAL FAILURE
```

O objectivo é evitar que uma falha temporária de um fornecedor externo provoque a perda da notificação.

---

# 🔁 Idempotência

Sistemas assíncronos podem processar uma mensagem mais de uma vez.

Por isso, a plataforma considera **idempotência** como uma preocupação importante no processamento das notificações.

O processamento deve evitar que a mesma operação seja executada de forma indevida quando uma mensagem é entregue novamente.

---

# 🧪 Estratégia de testes

O projecto utiliza diferentes níveis de testes.

### Testes unitários

Utilizados para validar regras de negócio e componentes isolados.

```text
Domain
Application
Strategies
Business Rules
```

### Testes de integração

Utilizados para verificar a integração entre os componentes reais da aplicação.

O projecto utiliza **Testcontainers** para executar dependências reais durante os testes.

Infraestrutura utilizada nos testes:

```text
┌──────────────────────┐
│   Integration Test   │
└──────────┬───────────┘
           │
     ┌─────┴─────┐
     ▼           ▼
PostgreSQL    RabbitMQ
Container     Container
```

Desta forma, os testes não dependem exclusivamente de mocks para validar integrações críticas.

---

# 🐳 Docker

O projecto pode ser executado através do Docker Compose.

A stack local inclui:

```text
┌─────────────────────────────┐
│       Notification API      │
│        Spring Boot          │
└──────────────┬──────────────┘
               │
       ┌───────┴────────┐
       │                │
       ▼                ▼
 PostgreSQL          RabbitMQ
```

Os serviços são configurados através de variáveis de ambiente.

---

# 🛠️ Stack tecnológica

| Categoria           | Tecnologia               |
| ------------------- | ------------------------ |
| Linguagem           | Java 21                  |
| Framework           | Spring Boot              |
| API                 | Spring Web MVC           |
| Persistência        | Spring Data JPA          |
| ORM                 | Hibernate                |
| Database            | PostgreSQL               |
| Migrations          | Flyway                   |
| Messaging           | RabbitMQ                 |
| Containerização     | Docker / Docker Compose  |
| Testes              | JUnit / Spring Boot Test |
| Integration Testing | Testcontainers           |
| Build               | Maven                    |
| Monitoring          | Spring Boot Actuator     |
| Architecture        | Clean Architecture       |
| Architecture Style  | Hexagonal Architecture   |
| Design              | Domain-Driven Design     |
| Design Pattern      | Strategy Pattern         |

---

# ⚙️ Pré-requisitos

Para executar o projecto localmente:

* Java 21
* Docker
* Docker Compose
* Git

Verificar:

```bash
java -version
docker --version
docker compose version
```

---

# 🚀 Executar o projecto

### 1. Clonar o repositório

```bash
git clone git@github.com:alfredobaptista/notification-platform.git
```

Ou:

```bash
git clone https://github.com/alfredobaptista/notification-platform.git
```

Entrar no projecto:

```bash
cd notification-platform
```

### 2. Criar o ficheiro de ambiente

```bash
cp .env.example .env
```

Preencher as variáveis necessárias no `.env`.

> O ficheiro `.env` não deve ser versionado.

### 3. Subir a infraestrutura

```bash
docker compose up -d
```

Para acompanhar os serviços:

```bash
docker compose ps
```

Logs:

```bash
docker compose logs -f
```

### 4. Executar a aplicação

A aplicação pode ser executada através do Maven Wrapper:

```bash
./mvnw spring-boot:run
```

Ou através do Docker Compose, conforme a configuração do ambiente.

---

# 🧪 Executar os testes

Executar todos os testes:

```bash
./mvnw test
```

Para executar apenas uma classe específica:

```bash
./mvnw test -Dtest=NotificationRepositoryIntegrationTest
```

Os testes de integração que utilizam Testcontainers necessitam de um ambiente Docker funcional.

---

# 📊 Health Check

A aplicação utiliza **Spring Boot Actuator** para disponibilizar endpoints de monitorização e health checks.

O endpoint principal pode ser consultado através de:

```text
http://localhost:8080/actuator/health
```

---

# 🔐 Configuração e segurança

As credenciais e chaves de fornecedores externos devem ser fornecidas através de variáveis de ambiente.

Exemplo:

```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=notification_db
DB_USER=
DB_PASSWORD=

RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USER=
RABBITMQ_PASS=

BREVO_API_KEY=
BREVO_SENDER_EMAIL=

TWILIO_ACCOUNT_SID=
TWILIO_AUTH_TOKEN=
TWILIO_FROM_NUMBER=
```

**Nunca coloque credenciais reais no Git.**

---

# 🗂️ Estrutura do projecto

A estrutura segue a separação de responsabilidades definida pela arquitectura:

```text
src/
├── main/
│   ├── java/
│   │   └── com/github/alfredobaptista/notification/
│   │       ├── domain/
│   │       ├── application/
│   │       └── adapter/
│   │
│   └── resources/
│       ├── application.yml
│       └── db/
│           └── migration/
│
└── test/
    └── java/
        └── com/github/alfredobaptista/notification/
```

A organização interna procura manter as dependências direccionadas para o domínio e separar as preocupações de infraestrutura.

---

# 🔭 Roadmap

O projecto está em evolução.

Possíveis evoluções:

* [ ] Retry configurável por canal
* [ ] Dead Letter Exchange / Dead Letter Queue
* [ ] Circuit Breaker por fornecedor
* [ ] Rate limiting por canal
* [ ] Observabilidade distribuída
* [ ] Métricas de entrega
* [ ] Templates de notificações
* [ ] Suporte a WhatsApp
* [ ] Persistência do histórico de tentativas
* [ ] Idempotency keys na API
* [ ] CI/CD com GitHub Actions
* [ ] Testes de carga
* [ ] Dashboard operacional

---

# 📚 Objectivos técnicos

Este projecto foi criado como um laboratório prático para explorar problemas comuns em sistemas backend distribuídos:

* processamento assíncrono;
* sistemas orientados a eventos;
* mensageria;
* integração com fornecedores externos;
* resiliência;
* consistência;
* idempotência;
* persistência;
* testes de integração;
* arquitectura limpa;
* princípios de DDD;
* design patterns;
* containerização.

O foco não é apenas **enviar uma mensagem**, mas estudar como construir uma plataforma capaz de processar operações de comunicação de forma desacoplada e resiliente.

---

# 👨‍💻 Autor

**Alfredo Fernando Baptista**

Backend Developer | Java • Spring Boot

Estudante de Ciências da Computação na Universidade Agostinho Neto.

### Contactos

* GitHub: [@alfredobaptista](https://github.com/alfredobaptista)
* LinkedIn: [alfredobaptista](https://www.linkedin.com/in/alfredobaptista/)

---

## 📄 Licença

Este projecto é desenvolvido para fins de estudo, experimentação e portfólio.
