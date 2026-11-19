# Army Central Library Management System

An academic Software Engineering project inspired by Army Central Library,
Rawalpindi, Pakistan. This is a Software Construction lab project.

The application is a desktop library catalogue and circulation-management
prototype built with Java and JavaFX. It provides a branded interface for
managing book records, tracking their current status, recording issue and
return dates, and reviewing overdue fines.

## Features

- **Dashboard:** Live totals for all, available, issued, and reserved books;
  recently added books; and recent library activity.
- **Book catalogue:** Add, edit, view, and delete records containing a book ID,
  title, and author. Duplicate IDs and incomplete entries are rejected.
- **Search and filters:** Search by book ID, title, or author and filter the
  catalogue by Available, Issued, or Reserved status. Table columns can be
  sorted.
- **Circulation:** Issuing a book requires a return-by date. Record a return
  through the return action; the book then becomes available again.
- **Overdue fines:** The fine is PKR 50 per day, beginning the day after the
  return-by date. Returning on or before the due date incurs no fine.
- **Reservations:** Books can be marked Reserved and viewed in the Reserved
  Books section. This prototype does not implement a reservation queue.
- **Settings:** Switch between light and dark themes for the current session,
  and reset the search and status filters.
- **About and branding:** Shows the application identity, academic-project
  context, version, and technology information. The temporary ACL book emblem
  is a project mark, not an official insignia.

## Technology

- Java 
- JavaFX 
- Apache Maven
- JUnit 5

The application follows a small MVC structure:

- `librarysystem.view` builds the JavaFX screens and dialogs.
- `librarysystem.controller` connects interface actions to the service.
- `librarysystem.service` validates and performs catalogue and circulation
  operations.
- `librarysystem.model` contains the book, status, and activity models.

## Requirements

- JDK 21
- Apache Maven 3.9 or later

Confirm the installations in Command Prompt or PowerShell:

```text
java -version
mvn -version
```

## Run the application

Open Command Prompt and change to the project directory (keep the quotation
marks because the path contains spaces):

```bat
mvn clean test
mvn javafx:run
```

The test command compiles the project and runs the service tests. The JavaFX
window stays open while the application is running; close the window to return
to the terminal.

In PowerShell, use `Set-Location` instead of `cd /d`:

```powershell
Set-Location "---"
mvn clean test
mvn javafx:run
```

## Data and project limitations

Book and activity data are held in memory. **Changes are lost when the
application closes**; this version does not use a database or save records to
disk. The fine rate is currently fixed at PKR 50 per overdue day. 
