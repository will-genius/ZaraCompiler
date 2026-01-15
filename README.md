# Zara Compiler 

A robust, multi-stage compiler for the custom Object-Oriented language **Zara**, built from scratch in Java. 

This project demonstrates a complete compiler pipeline, featuring Lexical Analysis, Syntax & Semantic Analysis, Intermediate Code Generation (TAC), Optimization, and Low-Level x86-64 Assembly generation. It also includes robust error handling with Panic Mode Recovery.

##  Key Features

* **Object-Oriented Design:** Supports Class definitions, Inheritance (`extends`), Object instantiation (`new`), and Method calls.
* **Complete Pipeline:**
    1.  **Lexer:** Tokenization with line-number tracking.
    2.  **Parser/Semantics:** Scope resolution, Type checking, and Symbol Table management (Global, Class, Method scopes).
    3.  **Intermediate Code:** Generates Three-Address Code (TAC).
    4.  **Optimizer:** Implements Constant Folding, Dead Code Elimination, and Loop Invariant Code Motion.
    5.  **Backend:** Generates x86-64 Assembly code (Stack-based allocation).
* **Robust Error Handling:** Implements **Panic Mode Recovery** to detect multiple errors in a single pass without crashing.

##  Tech Stack

* **Language:** Java (JDK 25)
* **Build Tool:** Standard Java (IntelliJ IDEA)
* **Architecture:** Modular pipeline (Lexer -> Analyzer -> Optimizer -> Backend)

## 📂 Project Structure

* `ZaraSemanticAnalyzer2.java`: The main driver. specifices the pipeline, handles parsing, and error recovery.for the oop based
*  `ZaraSemanticAnalyzer.java`: carries out the pipeline only until the intermediate code generation is not object oriented
*  `Symbol.java`: original symbol table before the adaptation of an oop aproach
* `LexicalAnalyzer.java`: Breaks source code into Tokens.
* `OOPSymbolTable.java`: Manages scopes for Classes, Fields, and Local variables.
* `TACInstruction.java`: Data structures for Intermediate Code Generation.
* `ZaraOptimizer.java`: The optimization engine (Constant Folding, Dead Code, etc.).
* `ZaraBackend.java`: Converts TAC into executable x86-64 Assembly.
*  `ZaraParser.java`: The main parser used for the entire project it is a top down parser
*   `ZaraBottomUpParser.java`: implementation of a bottomupparser

## The Zara Language Syntax

Zara is a strongly-typed, class-based language similar to Java or C#.

### Example Code
```zara
class Calculator {
    void compute() {
        integer count = 0
        integer max = 10
        
        while (count < max) {
            count = count + 1
        }
        
        print(count)
    }
}
expected ouput the syntax and semantic breakdown and the x86-64 Assembly code
How to Run:
Clone the repository
Open in IntelliJ IDEA.
Run the Main Class: Open ZaraSemanticAnalyzer.java and run the main method.
