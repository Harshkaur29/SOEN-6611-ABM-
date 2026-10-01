# AI Interaction Log – P2 

## Purpose

This log documents the AI interactions used to develop the Goal-Question-Metric (GQM) approach for P2 of the SOEN 6611 Software Measurement project.

The prompts were designed using the CASTROFF Prompt Engineering Framework, incorporating Constraints, Audience, Structure, Tone, Role, Output format, Focus, and Function.

---

## Interaction 1 – Initial GQM Generation

### Prompt

Act as a software measurement expert and develop a GQM approach for the iBank Java-based ABM. Constraints: provide one SMART goal and exactly six questions, with two questions for each of functional correctness, reliability, and maintainability. Audience: SOEN 6611 students and instructors. Structure: provide the goal followed by a table of questions and metrics. Tone: formal and academic. Role: GQM and software measurement expert. Output: concise Markdown suitable for a PowerPoint slide. Focus: measurable quality of the iBank prototype. Function: generate an initial GQM model for evaluation.

### AI Output Summary

The AI generated a SMART measurement goal and six GQM questions covering functional correctness, reliability, and maintainability. It also suggested measurable software metrics and formulas associated with the questions.

### Evaluation and Modification

The initial output provided a useful starting point, but the questions and metrics required further review. The questions were checked for overlap, measurability, feasibility, and consistency with the defined iBank system scope. Additional refinement was required before selecting the final GQM questions and metrics.

---

## Interaction 2 – GQM Evaluation and Review

### Prompt

Review the previously generated GQM for the iBank ABM. Constraints: identify overlapping questions, vague wording, metrics that are difficult to collect, and any inconsistency with the defined system scope. Audience: SOEN 6611 project team. Structure: list each issue followed by a recommended correction. Tone: objective, critical, and academic. Role: senior software measurement reviewer. Output: concise table. Focus: validity, measurability, and consistency of the GQM. Function: evaluate and improve the initial AI-generated GQM.

### AI Output Summary

The AI reviewed the initial GQM and identified areas where questions could overlap or where the relationship between a question and its metric could be made clearer. It also provided recommendations for improving measurability and alignment with the iBank scope.

### Evaluation and Modification

The recommendations were reviewed against the project requirements. The reliability questions were refined to distinguish general transaction failures from the handling of invalid inputs and transaction errors. Maintainability questions were also revised so that complexity and size/readability metrics were treated as measurable indicators rather than direct measures of maintenance difficulty.

---

## Interaction 3 – Final GQM Refinement

### Prompt

Refine the iBank GQM using the identified issues. Constraints: retain exactly six questions, maintain two questions per quality attribute, ensure every question has an objectively measurable metric, and keep all metrics feasible for a Java prototype. Audience: SOEN 6611 instructors and students. Structure: provide the final SMART goal and a six-row GQM table with quality attribute, question, metric, and formula. Tone: formal and concise. Role: software measurement expert. Output: PowerPoint-ready table. Focus: functional correctness, reliability, and maintainability. Function: produce the final GQM model for P2.

### AI Output Summary

The AI produced a refined SMART goal and six GQM questions organized into functional correctness, reliability, and maintainability. The final output included measurable metrics and formulas suitable for implementation and testing of the Java-based iBank prototype.

### Evaluation and Modification

The final output was independently reviewed and modified to ensure consistency with the iBank scope and the P2 requirements. The final GQM contains two questions for each quality attribute and uses measurable metrics that can be collected during implementation and testing.

---

## Final P2 GQM Result

### SMART Goal

Analyze the iBank Java-based ABM prototype for the purpose of evaluating its functional correctness, reliability, and maintainability from the developer’s perspective, using measurable software metrics collected during implementation and testing throughout the SOEN 6611 Fall 2026 project.

### GQM Questions and Metrics

| Quality Attribute | GQM Question | Metric |
|---|---|---|
| Functional Correctness | To what extent are the specified functional requirements implemented in the iBank prototype? | Requirements Implementation Coverage (RIC) |
| Functional Correctness | How successfully does the iBank prototype perform its implemented functions during testing? | Test Pass Rate (TPR) |
| Reliability | How frequently does the iBank prototype encounter failures during representative transaction scenarios? | Failure Rate (FR) |
| Reliability | How effectively does the iBank prototype handle invalid inputs and transaction errors without incorrectly modifying account data? | Error Recovery Success Rate (ERSR) |
| Maintainability | What is the structural complexity of the iBank source code? | Cyclomatic Complexity (CC), WMC |
| Maintainability | What are the size and readability characteristics of the iBank source code? | Physical SLOC, Logical SLOC, Readability Metric |

---

## AI Use Declaration

I certify that all AI interactions for this submission are completely and accurately documented in this log. All content derived from or inspired by AI has been independently evaluated and modified as per AI Interaction Log.
