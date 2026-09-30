Smart Solar Microgrid Trading System
SE4040 – Enterprise Application Development
Assignment: Smart Solar Microgrid Trading System – Client-Server Application  
Academic Year: 2026  
Degree: BSc (Hons) in Information Technology – Specialized in Software Engineering  
Semester: Year 4 Semester 2  
Group Size: 4 Members
---
1. Project Overview
The Smart Solar Microgrid Trading System is an end-to-end client-server application developed for the SE4040 Enterprise Application Development module.
The system provides a platform for managing solar microgrid stations, energy booking slots, power trading reservations and energy transfer operations.
The system consists of three main application components:
Web Application
Native Android Application
C# Web API / Web Service
The system uses MongoDB as the server-side NoSQL database and SQLite for local persistence within the Android application.
The main system users are:
Backoffice
Grid Operator
Solar Prosumer
The Web Application provides administrative and operational functionality, while the Native Android Application provides mobile functionality for Solar Prosumers and Grid Operators.
The C# Web API acts as the centralized service layer and handles the application's business logic and communication with the MongoDB database.
---
2. System Features
2.1 Web Application
The Web Application provides functionality for:
User and authentication management
Role-based authorization
Prosumer management
Account deactivation and reactivation
Microgrid station management
Energy slot management
Reservation management
Booking management
Dashboard and booking monitoring
Grid Operator operational functionality
Nearby station information
2.2 Native Android Application
The Native Android Application provides functionality for:
User authentication
Prosumer registration
Prosumer profile management
Energy slot booking
Reservation modification
Reservation cancellation
Booking history
Dashboard functionality
QR code generation
QR code scanning
Transaction verification
Energy transfer completion
Grid Operator operations
Nearby station viewing
Google Maps integration
SQLite local persistence
2.3 C# Web API
The centralized Web API provides:
Authentication services
User management
Prosumer management
Microgrid station management
Energy slot management
Reservation management
Grid Operator operations
Transaction processing
QR transaction verification
MongoDB database communication
---
3. User Roles
3.1 Backoffice
Backoffice users are responsible for administrative functions such as:
Managing Web Application users
Managing Prosumer accounts
Processing account deactivation requests
Reactivating deactivated accounts
Managing microgrid stations
Managing operational schedules
Managing energy booking slots
Managing reservations
3.2 Grid Operator
Grid Operators are responsible for operational activities such as:
Monitoring energy bookings
Updating battery slot availability
Viewing nearby microgrid stations
Using the mobile application
Scanning Prosumer transaction QR codes
Verifying transaction information
Finalizing energy transfer operations
3.3 Solar Prosumer
Solar Prosumers can:
Register using their NIC
Log into the system
Manage their profile
Search available energy slots
Create reservations
Modify reservations
Cancel reservations
View booking information
View booking history
Generate transaction QR codes
View nearby microgrid stations
---
4. Technologies Used
Component	Technology
Web Application	React + TypeScript
Mobile Application	Native Android + Kotlin
Backend / Web Service	C# ASP.NET Web API
Server-side Database	MongoDB
Mobile Local Database	SQLite
API Communication	REST API
Maps	Google Maps API
Web Service Hosting	IIS
Version Control	Git / GitHub
---
5. System Architecture
The Smart Solar Microgrid Trading System follows a client-server architecture.
The Web Application and Native Android Application communicate with the centralized C# Web API through RESTful API calls.
The Web API contains the central business logic and communicates with MongoDB for server-side data storage.
The Android application also uses SQLite for local persistence.
```text
                     SMART SOLAR MICROGRID
                       TRADING SYSTEM

          ┌─────────────────────────────────┐
          │        Web Application          │
          │       React + TypeScript        │
          │                                 │
          │   Backoffice / Grid Operator    │
          └────────────────┬────────────────┘
                           │
                           │ REST API
                           │
                           ▼
          ┌─────────────────────────────────┐
          │          C# Web API             │
          │        (Web Service)            │
          │                                 │
          │        Business Logic           │
          └────────────────┬────────────────┘
                           │
                           ▼
          ┌─────────────────────────────────┐
          │            MongoDB              │
          │      Server-side Database       │
          └─────────────────────────────────┘


          ┌─────────────────────────────────┐
          │       Native Android App        │
          │             Kotlin              │
          │                                 │
          │    Prosumer / Grid Operator     │
          └────────────────┬────────────────┘
                           │
               ┌───────────┴───────────┐
               │                       │
               ▼                       ▼
            SQLite                  REST API
                                       │
                                       ▼
                                  C# Web API


          Native Android Application
                     │
                     ▼
              Google Maps API
```
---
6. Individual Contribution
The following table presents the individual contributions of all four group members.
Table 1: Individual Contribution
Name	Registration Number	Contribution
Dilki H. P. C	IT22111210	User and Authentication Management, Prosumer Management, Role-Based Authorization, Account Deactivation and Reactivation
Perera M. A. I	IT22206282	Microgrid Station Management, Energy Slot Management, Nearby Station API and Google Maps Integration in Web Application
Samuel B.G	IT23165816	Prosumer Reservation and Booking Management, Reservation Modification and Cancellation, Booking History and Dashboard
Perirs T.D.S	IT23241800	Grid Operator Operations, QR Transaction Verification, QR Scanning and Energy Transfer Completion, Nearby Station API and Google Maps Integration in Web Application
---
7. Project Repository
The complete project source code is maintained using Git and GitHub.
GitHub Repository
Repository: [Smart Solar Microgrid – GitHub Repository] https://github.com/Dilki687/Smart-Solar-Microgrid.git
The repository contains the source code for:
Web Application
Native Android Application
C# Web API
Supporting project files
Documentation
Individual contributions are maintained through Git version control and project commits.
---
8. Project Demonstration Video
A demonstration video explaining the functionality of the Smart Solar Microgrid Trading System is provided below.
Demonstration Video
[Watch the Smart Solar Microgrid Trading System Demonstration] https://drive.google.com/file/d/1jvBD4H_63pVvQWr0NdzWmMkbGjJZafBj/view?usp=drive_link
The demonstration video covers the main workflows of the system, including:
User login and authentication
Role-based access
Web Application functionality
Prosumer registration
Prosumer account management
Microgrid station management
Energy slot management
Reservation creation
Reservation modification
Reservation cancellation
Booking history and dashboard
Grid Operator operations
QR code generation and scanning
Transaction verification
Energy transfer completion
Nearby station and Google Maps functionality
---
9. Project Structure
```text
Smart-Solar-Microgrid/
│
├── Android/
│   └── Native Android Application
│
├── Backend/
│   └── SmartSolarMicrogrid.API/
│
├── Frontend/
│   └── SmartSolarMicrogrid.Web/
│
├── docs/
│
└── README.md
```
---
Contact / Project Team
Dilki H. P. C
Registration Number: IT22111210
Perera M. A. I
Registration Number: IT22206282
Samuel B.G
Registration Number: IT23165816
Perirs T.D.S
Registration Number: IT23241800