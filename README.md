# Army Central Library Management System

An academic Software Engineering project inspired by Army Central Library,
Rawalpindi. It is not an officially approved or deployed Army application.

The Java 21 and JavaFX application provides a dashboard with live collection
statistics and activity, searchable and filterable book tables, status
management, and modal forms for adding and editing books. Issuing a book
requires a return-by date; overdue returns accrue PKR 50 per day, starting the
day after the due date. Settings includes a Night mode switch for changing
between light and dark themes during the current session.

The ACL book emblem is a temporary project logo, not an official insignia.
Replace it only with an authorized logo at `src/main/resources/branding/logo.png`.
Institution identity and application text are configured in
`src/main/resources/branding.properties`.

## Requirements

- JDK 21
- Apache Maven 3.9+

## Run from the project directory

```powershell
mvn clean test
mvn javafx:run
```

The JavaFX window remains open while the application is running. Close it to
return to the terminal.

## Generate Javadoc

```powershell
mvn javadoc:javadoc
```

HTML documentation is available in `docs/javadoc/index.html`.
