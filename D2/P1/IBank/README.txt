iBank - Problem 4 source
SOEN 6611, Concordia University, Fall 2026

FILES
- IBank.java: starts the Swing GUI.
- BankGUI.java: customer and ABM Technician screens.
- BankBackend.java: banking rules, authentication, transactions, receipts and maintenance.
- BankCustomer.java: card/PIN data, linked accounts and daily withdrawal total.
- Account.java: one Chequing or Savings account.
- BankException.java: expected application errors.
- IBankDemo.java: representative console test/demo only. Exclude this file from Problem 5 SLOC counting.

COMPILE
  javac *.java

  (The code uses Java 8-compatible language features.)

RUN THE GUI
  java IBank

RUN THE BACKEND DEMO
  java IBankDemo

SAMPLE RETAIL LOGINS
  Card number 1001, PIN 1234
  Card number 1002, PIN 4321

SAMPLE TECHNICIAN CODE
  2468

EXPLICIT ASSUMPTIONS
1. This is an in-memory simulation, not a connection to a real bank or card network.
2. Each retail customer has one linked Chequing and one linked Savings account.
3. The daily cash withdrawal limit is CAD $1,000 per customer per calendar day.
4. A card is locked for the current application run after three consecutive failed login attempts. A successful login resets the failure counter.
5. Cash notes are $100, $50, $20, $10 and $5; cash withdrawals must be whole CAD amounts divisible by $5.
6. Deposits are simulated by entering counts of each bill denomination; no physical currency reader is connected.
7. The technician code is a demonstration credential. It is not suitable for production security.
8. Hardware-error and empty-receipt-roll simulation buttons exist only to demonstrate/test the maintenance use case without physical ABM hardware.
9. Receipts are displayed on screen; no physical printer is connected.
10. Amounts are held as double values rounded to cents for teaching simplicity. This is not a production financial ledger design.
