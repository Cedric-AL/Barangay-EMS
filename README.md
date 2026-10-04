# BARANGAY-EMS
## Barangay Evacuation Management and Resource Allocation System

## About the Project

BARANGAY-EMS is a proposed Java-based desktop application designed to help barangay officials manage evacuation centers, monitor evacuees, track emergency resources, and coordinate evacuation and resource allocation during natural disasters.

This repository is currently in the **planning and setup stage**. The project has not yet been implemented; this README outlines what we intend to develop and serves as an overview for the development team.

## Background and Purpose

The Philippines frequently experiences typhoons, floods, earthquakes, and other natural disasters. When these emergencies occur, barangay officials face significant operational challenges:

- Managing multiple evacuation centers with limited and varying capacities
- Recording and tracking displaced individuals and families
- Monitoring the availability and distribution of essential emergency supplies
- Preventing duplicate registrations and maintaining accurate evacuee records
- Making quick decisions about resource allocation under pressure

Currently, these operations rely heavily on manual processes, leading to lost data, duplicates, inefficiencies, and delays in response. BARANGAY-EMS aims to solve these problems by providing a centralized, structured system for disaster response management at the barangay level.

## Project Objectives

1. Provide a single platform for managing all evacuation center operations
2. Enable real-time monitoring of evacuation center capacities
3. Streamline evacuee registration and profile management
4. Facilitate emergency resource tracking and inventory management
5. Identify and prevent duplicate evacuee registrations
6. Support informed decision-making through dashboards and reports
7. Assist in resource allocation based on center needs and available supplies
8. Improve response times and operational efficiency during disasters

## Planned Features

### Evacuation Center Management
- Add, update, and manage evacuation centers
- Record maximum capacity, current occupants, facilities, and location
- Display center status (Available, Near Capacity, or Full)

### Evacuee Registration
- Record evacuee information: name, age, household details, contact information
- Assign evacuees to evacuation centers
- Track special needs and vulnerability categories
- Support evacuee profile updates

### Capacity Monitoring
- Automatically calculate available spaces in each evacuation center
- Monitor occupancy levels and identify centers approaching capacity
- Alert staff to full evacuation centers

### Emergency Resource Management
- Track inventory of emergency supplies (food, water, medicine, blankets, hygiene kits, first-aid supplies, etc.)
- Record available, used, and remaining quantities
- Maintain supply counts by evacuation center

### Resource Shortage Monitoring
- Identify supplies that are insufficient for the current number of evacuees
- Highlight resources requiring replenishment or resupply

### Evacuation Center Recommendation
- Help officials identify suitable evacuation centers based on available capacity, location, resources, and evacuee needs
- Provide recommendations for assigning evacuees to appropriate centers

### Resource Allocation
- Assist in determining how emergency supplies should be distributed among evacuation centers
- Recommend allocation based on evacuee counts and resource availability

### Dashboard
- Display overview metrics: total evacuation centers, total evacuees, available spaces
- Show full centers, low-resource alerts, and supplies requiring replenishment

### Reports
- Generate basic reports on evacuees, center occupancy, available capacity
- Provide resource inventory and shortage reports
- Document evacuation history and operations

### Duplicate Registration Prevention
- Identify possible duplicate registrations across evacuation centers
- Compare additional information (age, household, contact number) for accuracy
- Allow authorized staff to update an evacuee's assigned center when necessary

## Technologies and Java Concepts

### Programming Language and Framework
- **Java** – Primary programming language
- **Java Swing** – Desktop GUI framework

### Object-Oriented Programming Concepts
- **Classes and Objects** – Represent evacuation centers, evacuees, and resources
- **Encapsulation** – Data hiding and controlled access through private attributes and public methods
- **Inheritance** – Share common behavior among related classes
- **Polymorphism** – Handle different types of resources and evacuee categories flexibly

### Additional Java Concepts
- **String Manipulation** – Process and format evacuee names, contact information, and center details
- **Regular Expressions** – Validate input formats (contact numbers, email addresses, etc.)
- **File Handling** – Store and retrieve application data
- **Exception Handling** – Manage errors and invalid inputs gracefully

## Intended Users

- **Barangay Officials** – Local government administrators coordinating disaster response
- **Evacuation Center Coordinators** – Staff managing individual evacuation facilities
- **Community Volunteers** – Support personnel assisting during disaster operations
- **Emergency Response Teams** – Personnel making resource allocation decisions

## Expected System Output

The application is expected to produce:

- **User Interfaces** – Windows and screens for managing centers, registering evacuees, and tracking resources
- **Real-time Data** – Current capacity, evacuee counts, and resource availability
- **Reports** – Summaries of center occupancy, evacuee records, resource inventory, and shortages
- **Alerts and Recommendations** – Notifications for low resources and suggestions for center assignments
- **Persistent Data** – Storage and retrieval of evacuation center and evacuee information

## Development Status

**Status:** Planning and Setup Phase

- Project structure and requirements are being finalized
- Java source code development has not yet begun
- No GUI components have been created
- No data models or business logic have been implemented
- The team is preparing to begin development

## Project Team

**Repository:** [Cedric-AL/Barangay-EMS](https://github.com/Cedric-AL/Barangay-EMS)

---

*This is an academic project developed to demonstrate software engineering principles, Object-Oriented Programming in Java, and real-world problem-solving in disaster management contexts.*
