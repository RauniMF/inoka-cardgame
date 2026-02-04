# Inoka - Card and Dice Game Web Application

A full-stack card-and-dice game developed using **Spring Boot** (backend) and **Angular** (frontend). This project demonstrates modern web application development with real-time gameplay using WebSockets.

---

## Quick Start with Docker (Recommended)

The fastest way to demo the application is using Docker Compose, which automatically sets up the database and application.

### Prerequisites
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (includes Docker Compose)
- No need for Node.js, MySQL, or Java installed locally!

### Steps

1. **Clone the repository**
   ```bash
   git clone <your-repo-url>
   cd inoka
   ```

2. **Configure environment variables**
   ```bash
   # Copy the example environment file
   cp .env.example .env
   
   # Edit .env and update the CHANGE_ME values
   # At minimum, change these:
   #   - DB_USERNAME
   #   - DB_PASSWORD
   #   - MYSQL_ROOT_PASSWORD
   #   - JWT_SECRET (generate with: openssl rand -hex 32)
   ```

   **Example .env:**
   ```bash
   DB_USERNAME=inoka_user
   DB_PASSWORD=SecurePass123!
   MYSQL_ROOT_PASSWORD=RootPass456!
   JWT_SECRET=4ef43e621e52963bfada0b3060aa902d74991a9e898b3e32cba763611a45a14e
   ```

3. **Start the application**
   ```bash
   docker-compose up --build
   ```

   The first time will take 5-10 minutes to build. Subsequent starts are much faster.

4. **Access the application**
   - Open your browser and navigate to: **http://localhost:8080**
   - The Angular frontend is served by the Spring Boot backend

5. **Stop the application**
   ```bash
   # Press Ctrl+C in the terminal, then:
   docker-compose down
   ```

---

## Build from Source (Development Setup)

If you want to develop or modify the application, follow these instructions to run it locally without Docker.

### Prerequisites

Ensure you have the following installed:
- [Node.js](https://nodejs.org/) (v18+)
- [MySQL](https://www.mysql.com/) (v8.0+)
- [Java JDK](https://www.oracle.com/java/technologies/javase-downloads.html) (v21)
- [Gradle](https://gradle.org/) (or use included Gradle wrapper)

---

### Database Setup

1. **Install and start MySQL**

2. **Create the database**
   ```sql
   CREATE DATABASE playerdb;
   ```

3. **Create application user** (optional but recommended)
   ```sql
   CREATE USER 'inoka_user'@'localhost' IDENTIFIED BY 'your_password';
   GRANT ALL PRIVILEGES ON playerdb.* TO 'inoka_user'@'localhost';
   FLUSH PRIVILEGES;
   ```

---

### Back-End Setup

1. **Navigate to the backend directory**
   ```bash
   cd inoka-app
   ```

2. **Configure application properties**
   ```bash
   # Copy the example file
   cp src/main/resources/templates/application.properties.example \
      src/main/resources/application.properties
   ```

3. **Edit `application.properties`**
   
   Update these values in `src/main/resources/application.properties`:
   ```properties
   # MySQL Configuration
   spring.datasource.url=jdbc:mysql://localhost:3306/playerdb
   spring.datasource.username=inoka_user
   spring.datasource.password=your_password
   
   # JWT Configuration (generate with: openssl rand -hex 32)
   jwt.secret=your_64_character_hex_secret_here
   jwt.expiration=86400000
   ```

4. **Build the backend**
   ```bash
   ./gradlew build
   ```

5. **Start the backend server**
   ```bash
   ./gradlew bootRun
   ```
   
   Backend runs on **http://localhost:8080**

---

### Front-End Setup

1. **Navigate to the frontend directory** (in a new terminal)
   ```bash
   cd inoka-front
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Start the development server**
   ```bash
   npm start
   # or
   ng serve --open
   ```
   
   Frontend runs on **http://localhost:4200**

---

## Project Structure 📁 

```
inoka/
├── inoka-app/              # Spring Boot backend
│   ├── src/main/java/      # Java source code
│   ├── src/main/resources/ # Configuration files
│   └── build.gradle        # Gradle build configuration
├── inoka-front/            # Angular frontend
│   ├── src/app/            # Angular components & services
│   ├── package.json        # NPM dependencies
│   └── angular.json        # Angular configuration
├── Dockerfile              # Multi-stage Docker build
├── docker-compose.yaml     # Docker Compose configuration
├── .env.example            # Environment variables template
└── README.md               # This file
```

---

## Running Tests

### Backend Tests
```bash
cd inoka-app
./gradlew test
```

### Frontend Tests

Note: Frontend tests have not been implemented yet.

```bash
cd inoka-front
npm test
```

---

## Notes

- This project is in active development - expect bugs and incomplete features
- The application uses WebSockets for real-time gameplay synchronization
- Frontend Angular app is served by Spring Boot in production (Docker) mode
- For development, run frontend and backend separately on ports 4200 and 8080

---

## Technologies Used

### Backend
- Spring Boot 3.5.8
- Spring Security (JWT authentication)
- Spring WebSocket (STOMP)
- Spring Data JPA
- MySQL 8.0
- Java 21

### Frontend
- Angular 19.2.1
- TypeScript 5.5.2
- RxJS 7.8.0
- SockJS + STOMP.js (WebSocket client)

### DevOps
- Docker & Docker Compose
- Multi-stage builds
- Gradle build system
- NPM package manager

---