Event Booking System

Advanced Database Systems Project

⸻

Project Overview

A secure desktop-based event reservation system demonstrating advanced database concepts including:

•	Relational schema design (PostgreSQL)

•	Transaction-safe seat booking

•	Fraud detection & security logging

•	Role-based access (User/Admin)

•	Lucene-powered search

•	AI-based event recommendations

•	Payment simulation (PayPal-style)

⸻

Technologies

•	Java (JDK 21)
•	JavaFX
•	PostgreSQL + pgAdmin 4
•	JDBC
•	Apache Lucene
•	Maven

⸻

Database Setup
1.	Create database:

CREATE DATABASE ticket_booking_db;

2.	Import the provided file:

      ticket_booking_db.sql


3. Ensure credentials in DatabaseConnection.java match:
Username: postgres  
Password: postgres  
Database: ticket_booking_db

Running the Project

Using Maven:

mvn clean javafx:run

Or run the Main class from your IDE.



Security

The system includes fraud detection logic that blocks booking/payment when suspicious behavior is detected (e.g., repeated login failures, rapid booking attempts).


👩‍💻 Author
: Alesia Gjeta

© 2026 – All Rights Reserved