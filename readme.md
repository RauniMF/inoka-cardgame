# Card Game Web App

A card-and-dice game developed using Spring Boot and Angular. This project is in its early stages and consists of a front-end and back-end, located in the `inoka-front` and `inoka-app` directories, respectively. Below are instructions to set up and run the application locally.

---

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Setup Instructions](#setup-instructions)
    - [Database Setup](#database-setup)
    - [Back-End Setup](#back-end-setup)
    - [Front-End Setup](#front-end-setup)
3. [Running the Application](#running-the-application)

---

## Prerequisites

Ensure you have the following installed on your system:
- [Node.js](https://nodejs.org/) (for the front-end)
- [MySQL](https://www.mysql.com/) (for the database)
- [Java JDK](https://www.oracle.com/java/technologies/javase-downloads.html) (for the back-end)
- [Gradle](https://gradle.org/) (for building the back-end)

---

## Setup Instructions

### Database Setup
1. Install and start MySQL.
2. Create a new database:
    ```sql
    CREATE DATABASE playerdb;
    ```

### Back-End Setup
1. Navigate to the `inoka-app` directory:
    ```bash
    cd inoka-app
    ```
2. Create and configure the `application.properties` file located in `src/main/resources`:
    ```properties
    spring.application.name=inoka_app
    # MySQL Config
    spring.datasource.url=jdbc:mysql://localhost:3306/playerdb
    # Configure local MySQL user information
    spring.datasource.username= 
    spring.datasource.password=
    spring.jpa.show-sql=true
    spring.jpa.hibernate.ddl-auto=update
    spring.jackson.default-property-inclusion=always
    ```
    > Note: For JWT authentication, jwt.secret and jwt.expiration must also be configured.

3. Build the back-end:
    ```bash
    gradle build
    ```
4. Start the back-end server:
    ```bash
    gradle bootRun
    ```

### Front-End Setup
1. Navigate to the `inoka-front` directory:
    ```bash
    cd inoka-front
    ```
2. Install dependencies:
    ```bash
    npm install
    ```
3. Start the front-end application:
    ```baseh
    ng serve --open
    ```

---

## Running the Application

1. Ensure the back-end server is running (`http://localhost:8080` by default).
3. Open your browser and navigate to `http://localhost:4200` to access the application.

---

## Notes
- This project is in its early stages, so expect potential bugs or incomplete features.

---  