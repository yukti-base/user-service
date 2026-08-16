# User Service

A Spring Boot microservice that manages student profile data. Provides APIs to retrieve and update a student's profile.

## Table of Contents
- [Overview](#overview)
- [Technology Stack](#technology-stack)
- [API Endpoints](#api-endpoints)
  - [Get Student Profile](#get-student-profile)
  - [Update Student Profile](#update-student-profile)
- [Authentication](#authentication)
- [Request & Response Formats](#request--response-formats)
- [Error Handling](#error-handling)
- [Running the Service](#running-the-service)
- [Build & Test](#build--test)
- [Frontend Integration Guide](#frontend-integration-guide)

## Overview

The User Service exposes two RESTful endpoints under the base path `/student-profile`:
- **GET** `/student-profile` – Retrieve the authenticated student's profile.
- **PATCH** `/student-profile` – Update the authenticated student's profile.

The service is stateless and uses JWT bearer-token authentication. Upon successful authentication, the user ID is extracted from the token and used to fetch or update the profile.

## Technology Stack
- **Language:** Java 17+
- **Framework:** Spring Boot 3.x
- **Build Tool:** Gradle (wrapper included)
- **Security:** Spring Security with JWT validation
- **Validation:** Jakarta Validation (Bean Validation 3.0)
- **Packaging:** Executable JAR

## API Endpoints

### Base URL
```
http://localhost:8080
```
*(Adjust host/port as per deployment environment)*

All endpoints require an `Authorization: Bearer <jwt>` header.

---

### Get Student Profile
- **URL:** `GET /student-profile`
- **Description:** Returns the profile of the currently authenticated student.
- **Headers:**
  - `Authorization: Bearer <jwt>`
- **Success Response:**
  - **Code:** 200 OK
  - **Content-Type:** `application/json`
  - **Body:** `StudentProfileResponse` (see format below)
- **Error Responses:**
  - 401 Unauthorized – Missing or invalid token
  - 403 Forbidden – Token valid but insufficient permissions (should not occur for this endpoint)
  - 500 Internal Server Error – Unexpected server error

### Update Student Profile
- **URL:** `PATCH /student-profile`
- **Description:** Updates fields of the authenticated student's profile. Only supplied fields are modified; omitted fields retain their current values.
- **Headers:**
  - `Authorization: Bearer <jwt>`
  - `Content-Type: application/json`
- **Request Body:** `StudentProfileRequest` (see format below)
- **Success Response:**
  - **Code:** 200 OK
  - **Content-Type:** `application/json`
  - **Body:** Updated `StudentProfileResponse`
- **Error Responses:**
  - 400 Bad Request – Validation errors (e.g., invalid date format)
  - 401 Unauthorized – Missing or invalid token
  - 403 Forbidden – Token valid but insufficient permissions
  - 500 Internal Server Error – Unexpected server error

## Authentication

The service expects a JWT in the `Authorization` header with the `Bearer` scheme. The JWT must be signed and contain a `userId` claim (or equivalent) that maps to the student's internal identifier.

- **Header:** `Authorization: Bearer <jwt_token>`
- The token is validated by a `JwtTokenVerifier` (provided by the shared `yuktisetu-core` library).
- Upon validation, a `UserPrincipal` object is made available to controller methods via `@AuthenticationPrincipal`.

> **Note:** Token issuance and management are handled outside this service (e.g., by an auth service). Ensure the token includes the `userId` claim.

## Request & Response Formats

### StudentProfileRequest (PATCH body)
```json
{
  "dateOfBirth": "1998-04-15T00:00:00Z",
  "address": "123 Main St, City, State",
  "institution": "XYZ University",
  "degree": "Bachelor of Technology",
  "branch": "Computer Science",
  "cgpa": 8.5,
  "graduationYear": 2022,
  "tenthPercentage": 90.0,
  "twelfthPercentage": 85.5,
  "coCubesScore": 75.0,
  "skills": ["Java", "Spring Boot", "REST"],
  "codingProfiles": [
    {
      "platform": "LeetCode",
      "username": "johndoe",
      "profileLink": "https://leetcode.com/johndoe",
      "rating": 1800
    }
  ],
  "professionalProfiles": [
    {
      "platform": "LinkedIn",
      "profileLink": "https://linkedin.com/in/johndoe"
    }
  ],
  "projects": [
    {
      "title": "E-commerce Platform",
      "projectLink": "https://github.com/johndoe/ecommerce",
      "description": "A full-stack online store.",
      "technologies": "Java, Spring Boot, React, MySQL"
    }
  ],
  "workExperiences": [
    {
      "company": "ABC Corp",
      "role": "Software Engineer Intern",
      "duration": "Jun 2021 - Aug 2021",
      "description": "Developed backend APIs."
    }
  ],
  "achievements": [
    {
      "title": "Winner, Hackathon 2023",
      "description": "Built an AI-based resume matcher."
    }
  ]
}
```
*All fields are optional; only include those you wish to update.*

### StudentProfileResponse (GET / PATCH response)
```json
{
  "id": 101,
  "userId": 5001,
  "dateOfBirth": "1998-04-15T00:00:00Z",
  "address": "123 Main St, City, State",
  "institution": "XYZ University",
  "degree": "Bachelor of Technology",
  "branch": "Computer Science",
  "cgpa": 8.5,
  "graduationYear": 2022,
  "tenthPercentage": 90.0,
  "twelfthPercentage": 85.5,
  "coCubesScore": 75.0,
  "compositeScore": 82.3,
  "skills": ["Java", "Spring Boot", "REST"],
  "codingProfiles": [
    {
      "platform": "LeetCode",
      "username": "johndoe",
      "profileLink": "https://leetcode.com/johndoe",
      "rating": 1800
    }
  ],
  "professionalProfiles": [
    {
      "platform": "LinkedIn",
      "profileLink": "https://linkedin.com/in/johndoe"
    }
  ],
  "projects": [
    {
      "title": "E-commerce Platform",
      "projectLink": "https://github.com/johndoe/ecommerce",
      "description": "A full-stack online store.",
      "technologies": "Java, Spring Boot, React, MySQL"
    }
  ],
  "workExperiences": [
    {
      "company": "ABC Corp",
      "role": "Software Engineer Intern",
      "duration": "Jun 2021 - Aug 2021",
      "description": "Developed backend APIs."
    }
  ],
  "achievements": [
    {
      "title": "Winner, Hackathon 2023",
      "description": "Built an AI-based resume matcher."
    }
  ]
}
```
*Note:* `id` is the internal profile primary key; `userId` links to the authentication system.

## Error Handling

All error responses follow a consistent JSON structure (provided by the shared core library):
```json
{
  "timestamp": "2026-08-16T12:34:56.789Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed: dateOfBirth must be in the past",
  "path": "/student-profile"
}
```
- **4xx** – Client errors (validation, authentication, authorization)
- **5xx** – Server errors

## Running the Service

### Prerequisites
- JDK 17 or later
- Gradle 8.x (wrapper provided)

### Steps
```bash
# Clone the repository
git clone <repo-url>
cd user-service

# Build the project
./gradlew clean build

# Run the service
./gradlew bootRun
```
The service will start on port **8080** by default (configurable via `application.yml` or environment variables).

### Docker (optional)
A Dockerfile can be added; currently not included.

## Build & Test

```bash
# Compile and run unit/integration tests
./gradlew test

# Create a runnable JAR
./gradlew bootJar
# The JAR will be located at build/libs/user-service-<version>.jar

# Run the JAR
java -jar build/libs/user-service-<version>.jar
```

## Frontend Integration Guide

The frontend developer should consume the two endpoints as described above.

### General Guidelines
1. **Authentication**  
   - Obtain a JWT from the auth service (outside the scope of this repo).  
   - Include the token in the `Authorization` header for every request to `/student-profile`.

2. **Fetching Profile**  
   - `GET https://<api-host>/student-profile`  
   - On success (200), map the JSON to the frontend's profile model.  
   - Handle 401/403 by redirecting to login or showing an error.

3. **Updating Profile**  
   - Gather the fields to update into a plain JavaScript object matching `StudentProfileRequest`.  
   - Send a `PATCH` request to `https://<api-host>/student-profile` with:  
     - Header: `Authorization: Bearer <jwt>`  
     - Header: `Content-Type: application/json`  
     - Body: JSON-serialized request object (only include changed fields).  
   - On success (200), replace the local profile data with the returned `StudentProfileResponse`.  
   - On 400, display validation errors (from response message).  
   - On 401/403, handle authentication issues.  
   - On 5xx, show a generic error and optionally retry.

4. **Data Types**  
   - Dates are ISO‑8601 strings (UTC).  
   - Numbers are JSON numbers (double/int).  
   - Lists are JSON arrays.

5. **Example (using fetch)**
   ```javascript
   // GET profile
   async function fetchProfile(token) {
     const resp = await fetch('https://api.example.com/student-profile', {
       headers: { Authorization: `Bearer ${token}` }
     });
     if (!resp.ok) throw new Error(`Failed to fetch profile: ${resp.status}`);
     return resp.json();
   }

   // PATCH update
   async function updateProfile(token, updates) {
     const resp = await fetch('https://api.example.com/student-profile', {
       method: 'PATCH',
       headers: {
         Authorization: `Bearer ${token}`,
         'Content-Type': 'application/json'
       },
       body: JSON.stringify(updates)
     });
     if (!resp.ok) {
       const err = await resp.json();
       throw new Error(`Update failed: ${err.message}`);
     }
     return resp.json();
   }
   ```

6. **Testing**  
   - Use tools like Postman or curl to verify endpoints before integrating.  
   - Example curl:
     ```bash
     curl -X GET http://localhost:8080/student-profile \
          -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
     ```

---

*This README serves as the contract between the backend service and the frontend consumer. Keep it updated if the API evolves.*