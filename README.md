# BARANGAY-EMS

Barangay Evacuation Management and Resource Allocation System

## Project Overview

BARANGAY-EMS is a Java-based application designed to manage evacuation centers, evacuees, emergency resources, evacuation-center capacity, resource allocation, recommendations, and reports during emergency situations in barangay communities.

## Project Structure

### Directory Layout

```
BARUNGAY-EMS/
├── src/
│   └── barangayems/
│       ├── Main.java                    (Application entry point)
│       ├── frontend/                    (GUI/User Interface)
│       │   ├── Login.java
│       │   ├── Dashboard.java
│       │   ├── EvacuationCenters.java
│       │   ├── Evacuees.java
│       │   ├── Resources.java
│       │   ├── Allocation.java
│       │   └── Reports.java
│       └── backend/                     (Models & Business Logic)
│           ├── User.java
│           ├── BarangayOfficial.java
│           ├── CenterStaff.java
│           ├── EvacuationCenter.java
│           ├── Evacuee.java
│           ├── Resource.java
│           └── DataManager.java
└── README.md
```

### Package Organization

**barangayems**
- Main.java: Application entry point and initialization

**barangayems.frontend**
- Contains all GUI/user interface classes
- Developed by the Frontend Team
- Includes screens for Login, Dashboard, Evacuation Centers, Evacuees, Resources, Allocation, and Reports

**barangayems.backend**
- Contains all data models and business logic
- Developed by the Backend Team
- Includes entity models (User, BarangayOfficial, CenterStaff, EvacuationCenter, Evacuee, Resource) and DataManager for data operations

## Development Environment

- **IDE**: Eclipse
- **Language**: Java
- **Project Type**: Standard Eclipse Java Project
- **Source Folder**: src
- **Base Package**: barangayems

## Running the Project

1. Open Eclipse
2. Import the BARANGAY-EMS project
3. Right-click on Main.java
4. Select "Run As" → "Java Application"
5. Output should display: "BARANGAY-EMS System Started"

## Team Responsibilities

### Frontend Team
- Develop GUI screens in the `barangayems.frontend` package
- Implement user interface components
- Handle user interactions and screen navigation

### Backend Team
- Develop data models and business logic in the `barangayems.backend` package
- Implement entity classes with required attributes and methods
- Handle data management and system operations

### Project Manager
- Oversee project structure and integration
- Coordinate between frontend and backend teams
- Manage GitHub repository and issue tracking

## Notes

- This is Week 1 of a 3-week development plan
- Focus is on project setup and system design
- All team members should ensure their code is properly organized within their respective packages
- Detailed feature implementation will begin in Week 2
