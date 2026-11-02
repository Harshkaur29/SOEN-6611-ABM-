# Deliverable 1 – Team Roles and Responsibilities

The roles and responsibilities for Deliverable 1 were decided collectively by all team members. Each team member is responsible for a specific part of the deliverable, with minimum overlap among responsibilities.

**Team Member Roles:**

* **Harshdeep Kaur – P1:** Responsible for completing and documenting P1 of Deliverable 1.
* **Swathi Priya Pasumarthy – P2:** Responsible for completing and documenting P2 of Deliverable 1.
* **Syed Shahab Shah – P3:** Responsible for completing and documenting P3 of Deliverable 1.

Each team member is responsible for completing their assigned part and ensuring that the corresponding work is completed and available by the required deadline. 

# Deliverable 2 – Team Roles and Responsibilities

The roles and responsibilities for Deliverable 2 were decided collectively by all team members. Each team member is responsible for a specific part of the deliverable, with minimal overlap among responsibilities to ensure efficient collaboration and timely completion.

**Team Member Roles:**

* **Harshdeep Kaur – P5,P6:** Responsible for completing and documenting P5 and P6 of Deliverable 2, including the required analysis, calculations, and supporting explanations, as applicable to each problem.
* **Swathi Priya Pasumarthy – P4.b:** Responsible for testing the iBank application using representative data, preparing and executing relevant test cases, documenting expected and actual results, and reporting the testing outcomes with supporting evidence, as required.
* **Syed Shahab Shah – P4.a:** Responsible for implementing the assigned iBank functionality and documenting the implementation details, including relevant design decisions and supporting evidence, as required.
  
Each team member is responsible for completing their assigned tasks, ensuring the correctness and clarity of their work, and making the corresponding materials available to the team by the agreed deadline. Team members will coordinate where necessary to ensure that the testing activities for P4(b) align with the implementation completed for P4(a). All contributions will be reviewed collectively to maintain consistency and completeness across Deliverable 2.

# iBank - Problem 4a

Author: SYED SHAHAB SHAH

## Goal
The goal of this project is to model a realistic retail banking and ABM experience in a Java Swing application. The system is designed to allow customers to authenticate securely, view balances, transfer funds, deposit cash, withdraw cash, and perform everyday banking actions in a simulated environment. In this version, the system is expanded to support multiple currencies so that it aligns with the requirement that customers may have accounts and ABM transactions in several currencies.

## Security
This project applies basic but meaningful security checks for a teaching simulation:

- Customer authentication requires a valid card number and 4-digit PIN.
- Incorrect PIN attempts are tracked per card.
- After three consecutive failed login attempts, the card is locked for the current application session.
- A successful login clears the failure count and re-enables access.
- The technician login also uses a code-based check to restrict maintenance actions.
- Transactions such as withdrawal and transfer validate account balances before processing.
- The application is not production-grade security; it is a classroom simulation and does not connect to live banking systems or real card networks.

This means the system is reasonably secure for a demonstration project, but it is not designed for real-world financial deployment.

## Multi-Currency Support
The application supports three currencies:

- CAD
- USD
- EUR

Customers can manage accounts in multiple currencies, and ABM actions can be chosen based on the selected currency. This includes:

- multi-currency account balances
- deposits in the chosen currency
- withdrawals in the chosen currency
- transfers between accounts in the same chosen currency
- separate cash-note inventories for each currency
- technician replenishment of notes for each currency independently

This satisfies the banking requirement that customers have accounts in multiple currencies and that the ABM supports relevant transactions in those currencies.

## Usability
The interface is designed to be straightforward for both customers and technicians:

- Clear login screens for customer and technician access
- Simple transaction menus for withdrawal, deposit, transfer, and balance inquiry
- Currency selection prompts before transactions are processed
- Technician maintenance controls for diagnostics and cash-note replenishment
- On-screen receipts showing transaction details and updated balances

The workflow is intentionally simple and easy to understand, which makes the system usable for teaching and demonstration purposes while still demonstrating realistic ABM behavior.

## Files
- `IBank.java`: starts the Swing GUI.
- `BankGUI.java`: customer and ABM Technician screens.
- `BankBackend.java`: banking rules, authentication, transactions, receipts and maintenance.
- `BankCustomer.java`: card/PIN data, linked accounts and daily withdrawal totals.
- `Account.java`: one Chequing or Savings account.
- `BankException.java`: expected application errors.
- `IBankDemo.java`: representative console test/demo only. Exclude this file from Problem 5 SLOC counting.

## Compile
```bash
javac *.java
```

(The code uses Java 8-compatible language features.)

## Run the GUI
```bash
java IBank
```

## Run the Backend Demo
```bash
java IBankDemo
```

## Sample Retail Logins
- Card number 1001, PIN 1234
- Card number 1002, PIN 4321

## Sample Technician Code
- 2468

## Explicit Assumptions
1. This is an in-memory simulation, not a connection to a real bank or card network.
2. Each retail customer may hold Chequing and Savings accounts in CAD, USD, and EUR.
3. The daily cash withdrawal limit is CAD $1,000 per customer per calendar day for each supported currency.
4. A card is locked for the current application run after three consecutive failed login attempts. A successful login resets the failure counter.
5. Cash notes are $100, $50, $20, $10 and $5.
6. Deposits are simulated by entering counts of each bill denomination for the chosen currency; no physical currency reader is connected.
7. The technician code is a demonstration credential. It is not suitable for production security.
8. Hardware-error and empty-receipt-roll simulation buttons exist only to demonstrate/test the maintenance use case without physical ABM hardware.
9. Receipts are displayed on screen; no physical printer is connected.
10. Amounts are held as double values rounded to cents for teaching simplicity. This is not a production financial ledger design.

## Problem 4(b) – Testing

Author: SWATHI PRIYA PASUMARTHY

### Overview

Problem 4(b) focuses on verifying the functionality, reliability, and error handling of the iBank application using representative test cases. Both automated testing and manual GUI testing are used to evaluate the application's behavior under valid and invalid conditions.

### Automated Testing

Automated tests are implemented in `IBankTests.java` using a standalone Java test runner without an external testing framework.

The test suite covers the following functional areas:

- Customer authentication and invalid PIN handling.
- Account balance inquiries.
- Cash deposits and withdrawals.
- Fund transfers between accounts.
- PIN changes and input validation.
- Currency and transaction validation.
- Daily withdrawal limits and insufficient-funds handling.
- Exception handling and error conditions.
- Technician authentication and maintenance operations.
- Cash replenishment, receipt-roll replacement, and hardware error handling.