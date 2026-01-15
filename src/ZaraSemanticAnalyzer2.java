import java.util.List;
import java.util.ArrayList;

public class ZaraSemanticAnalyzer2 {
    private List<LexicalAnalyzer.Token> tokens;
    private List<String> errors = new ArrayList<>(); // To collect all errors
    private int pos;

    // SWITCHED TO NEW TABLE
    private OOPSymbolTable symbolTable;

    private TACInstruction.CodeGenerator generator;
    private boolean hasError = false;

    public ZaraSemanticAnalyzer2(List<LexicalAnalyzer.Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
        this.symbolTable = new OOPSymbolTable(); // <--- New OOPSymbolTable
        this.generator = new TACInstruction.CodeGenerator();
    }

    // --- Helper Methods ---
    private void reportError(String message) {
        String err = " Error at Line " + current().line + ": " + message;
        System.out.println(err); // Print immediately
        errors.add(err);         // Add to log
        hasError = true;
    }
    private void synchronize() {
        System.out.println("   ...Panic Mode: Skipping tokens until safe point...");

        pos++; // Advance one token to avoid infinite loops

        while (current().type != LexicalAnalyzer.TokenType.EOF) {
            // Safe Point 1: We just passed a semicolon (end of previous statement)
            if (pos > 0 && tokens.get(pos - 1).data.equals(";")) {
                return;
            }

            // Safe Point 2: We see a keyword that starts a NEW statement or declaration
            String data = current().data;
            if (data.equals("class") ||
                    data.equals("void") ||
                    data.equals("integer") ||
                    data.equals("float") ||
                    data.equals("string") ||
                    data.equals("if") ||
                    data.equals("while") ||
                    data.equals("return")) {
                return;
            }

            pos++; // Skip current token
        }
    }

    private LexicalAnalyzer.Token current() {
        if (pos >= tokens.size()) {
            // Pass -1 as the line number for EOF
            return new LexicalAnalyzer.Token(LexicalAnalyzer.TokenType.EOF, "", -1);
        }
        return tokens.get(pos);
    }


    private void match(LexicalAnalyzer.TokenType expectedType) {
        if (current().type == expectedType) {
            pos++;
        } else {
            reportError("Expected " + expectedType + " but found " + current().type);
            synchronize();
        }
    }


    // ==========================================
    // 1. TOP LEVEL: PROGRAM -> CLASSES
    // ==========================================
    public void parseProgram() {
        System.out.println("--- Starting OOP Semantic Analysis ---");

        // Loop: Expect "class" keywords until EOF
        while (current().type != LexicalAnalyzer.TokenType.EOF) {
            if (current().data.equals("class")) {
                parseClass();
            } else {
                reportError("Syntax Error: Expected 'class' definition at top level.");
                pos++; // Skip garbage
            }
        }

        System.out.println("--- Analysis Complete ---");

        if (!hasError) {
            System.out.println("Build Successful! Generating Code...");

            // 1. Get Code
            List<TACInstruction> rawCode = generator.getInstructions();

            // 2. Optimization (Optional - you can keep or remove)
            ZaraOptimizer optimizer = new ZaraOptimizer(rawCode);
            optimizer.optimize();

            // 3. Print TAC (Intermediate)
            generator.printCode();

            //  4. NEW: BACKEND GENERATION ---
            ZaraBackend backend = new ZaraBackend(rawCode);
            backend.generate();


        } else {
            System.out.println(" Build Failed.");
        }
    }

    // 2. CLASS LEVEL: FIELDS & METHODS
    private void parseClass() {
        match(LexicalAnalyzer.TokenType.KEYWORD); // class

        String className = current().data;
        match(LexicalAnalyzer.TokenType.IDENTIFIER);

        String parentClass = null;
        // Check for Inheritance
        if (current().data.equals("extends")) {
            match(LexicalAnalyzer.TokenType.KEYWORD);
            parentClass = current().data;
            match(LexicalAnalyzer.TokenType.IDENTIFIER);
        }

        // Define Class in Symbol Table
        symbolTable.defineClass(className, parentClass);

        match(LexicalAnalyzer.TokenType.PUNCTUATOR); // {

        // Parse Class Members (Fields or Methods)
        while (!current().data.equals("}") && current().type != LexicalAnalyzer.TokenType.EOF) {
            parseMember();
        }

        match(LexicalAnalyzer.TokenType.PUNCTUATOR); // }
    }

    private void parseMember() {
        // Look ahead: "integer x =" (Field) vs "void func() {" (Method)
        String type = current().data;
        match(LexicalAnalyzer.TokenType.KEYWORD); // type (int, void...)

        String name = current().data;
        match(LexicalAnalyzer.TokenType.IDENTIFIER);

        // If '(', it's a method!
        if (current().data.equals("(")) {
            parseMethod(name, type);
        }
        // If '=' or ';', it's a field!
        else {
            parseField(name, type);
        }
    }

    private void parseField(String name, String type) {
        symbolTable.defineField(name, type);

        if (current().data.equals("=")) {
            match(LexicalAnalyzer.TokenType.OPERATOR);
            // We use simple constants for field init for now
            String value = current().data;
            if (current().type == LexicalAnalyzer.TokenType.CONSTANT) pos++;

            // Simple TAC for field init
            generator.emit(name, value, "", null);
        }
        // Optional semicolon
        if (current().data.equals(";")) match(LexicalAnalyzer.TokenType.PUNCTUATOR);
    }

    private void parseMethod(String name, String returnType) {
        symbolTable.defineMethod(name, returnType);

        match(LexicalAnalyzer.TokenType.PUNCTUATOR); // (
        // TODO: Parse Parameters here later
        match(LexicalAnalyzer.TokenType.PUNCTUATOR); // )

        match(LexicalAnalyzer.TokenType.PUNCTUATOR); // {

        // Parse Method Body (Statements)
        while (!current().data.equals("}") && current().type != LexicalAnalyzer.TokenType.EOF) {
            parseStatement();
        }

        match(LexicalAnalyzer.TokenType.PUNCTUATOR); // }
    }

    // 3. METHOD LEVEL: STATEMENTS (Old Logic)

    private void parseStatement() {
        LexicalAnalyzer.Token token = current();

        if (token.type == LexicalAnalyzer.TokenType.KEYWORD) {
            if (token.data.matches("integer|float|string|boolean")) {
                parseLocalDeclaration();
            } else if (token.data.equals("if")) {
                parseIf();
            } else if (token.data.equals("while")) {
                parseWhile();
            } else {
                pos++;
            }
        }
        else if (token.type == LexicalAnalyzer.TokenType.IDENTIFIER) {
            // --- FIX: Check for Class Types or 'print' ---

            // 1. Is it 'print'?
            if (token.data.equals("print")) {
                parsePrint();
            }
            // 2. Is it a Class Type declaration? (e.g. "Dog d")
            // We check if the NEXT token is an identifier (the variable name)
            else if (pos + 1 < tokens.size() && tokens.get(pos + 1).type == LexicalAnalyzer.TokenType.IDENTIFIER) {
                parseLocalDeclaration();
            }
            // 3. Otherwise, it's an Assignment or Method Call (d.bark)
            else {
                parseIdentifierStatement();
            }
        }
        else if (token.data.equals("}")) {
            return;
        }
        else {
            pos++;
        }
    }
    private void parsePrint() {
        match(LexicalAnalyzer.TokenType.IDENTIFIER); // Eat 'print'
        match(LexicalAnalyzer.TokenType.PUNCTUATOR); // Eat '('

        // Allow printing strings or variables
        String content = current().data;
        if (current().type == LexicalAnalyzer.TokenType.CONSTANT) {
            pos++; // String literal
        } else {
            content = parseExpression(); // Variable/Expression
        }

        match(LexicalAnalyzer.TokenType.PUNCTUATOR); // Eat ')'

        generator.emit("print", content, "", null);
    }
    private void parseIdentifierStatement() {
        String name = current().data;
        match(LexicalAnalyzer.TokenType.IDENTIFIER);

        // CASE 1: Method Call (e.g., d.bark() )
        if (current().data.equals(".")) {
            match(LexicalAnalyzer.TokenType.PUNCTUATOR); // Eat '.'
            String methodName = current().data;
            match(LexicalAnalyzer.TokenType.IDENTIFIER); // Eat 'bark'
            match(LexicalAnalyzer.TokenType.PUNCTUATOR); // Eat '('
            match(LexicalAnalyzer.TokenType.PUNCTUATOR); // Eat ')'

            // Verify object exists
            if (symbolTable.resolve(name) == null) reportError("Object '" + name + "' not found.");

            // Generate TAC: call d.bark
            generator.emit("call", name + "." + methodName, "", "0");
        }
        // CASE 2: Assignment (e.g., x = 5 )
        else {
            if (symbolTable.resolve(name) == null) reportError("Variable '" + name + "' not found.");
            match(LexicalAnalyzer.TokenType.OPERATOR); // Eat '='
            String val = parseExpression();
            generator.emit(name, val, "", null);
        }
    }

    // REUSED LOGIC (Slightly modified to use resolve()) ---

    private void parseLocalDeclaration() {
        // 1. Capture the Type (Could be "integer" or "Dog")
        String type = current().data;

        // Allow KEYWORD (primitive) OR IDENTIFIER (object type)
        if (current().type == LexicalAnalyzer.TokenType.KEYWORD) {
            match(LexicalAnalyzer.TokenType.KEYWORD);
        } else {
            match(LexicalAnalyzer.TokenType.IDENTIFIER);
            // Optional: You could check if 'type' exists in symbolTable.classes here
        }

        // 2. Capture the Name
        String name = current().data;
        match(LexicalAnalyzer.TokenType.IDENTIFIER);

        symbolTable.defineLocal(name, type);

        // 3. Handle Initialization ( = new Dog() )
        if (current().data.equals("=")) {
            match(LexicalAnalyzer.TokenType.OPERATOR);
            String val = parseExpression();
            generator.emit(name, val, "", null);
        }
    }



    // (parseExpression, parseIf, parseWhile are the same as before,
    //  just ensure parseExpression uses symbolTable.resolve(op1) for checks)
    private String parseExpression() {
        String op1 = current().data;

        // --- NEW: Handle Object Creation (new Dog) ---
        if (op1.equals("new")) {
            match(LexicalAnalyzer.TokenType.KEYWORD); // Eat 'new'
            String className = current().data;
            match(LexicalAnalyzer.TokenType.IDENTIFIER); // Eat 'Dog'
            match(LexicalAnalyzer.TokenType.PUNCTUATOR); // Eat '('
            match(LexicalAnalyzer.TokenType.PUNCTUATOR); // Eat ')'

            // Generate TAC: t0 = new Dog
            String temp = generator.newTemp();
            generator.emit(temp, "new " + className, "", null);
            return temp;
        }


        if (current().type == LexicalAnalyzer.TokenType.IDENTIFIER) {
            // Check if variable exists
            if (symbolTable.resolve(op1) == null) reportError("Variable '" + op1 + "' not found.");
            pos++;
        } else if (current().type == LexicalAnalyzer.TokenType.CONSTANT) {
            pos++;
        }

        if (current().type == LexicalAnalyzer.TokenType.OPERATOR && !current().data.equals(";") && !current().data.equals(")")) {
            String operator = current().data;
            pos++;
            String op2 = parseExpression();
            String temp = generator.newTemp();
            generator.emit(temp, op1, operator, op2);
            return temp;
        }
        return op1;
    }

    // Stubs for If/While to keep code valid
    private void parseIf() { match(LexicalAnalyzer.TokenType.KEYWORD); match(LexicalAnalyzer.TokenType.PUNCTUATOR); String c = parseExpression(); match(LexicalAnalyzer.TokenType.PUNCTUATOR); match(LexicalAnalyzer.TokenType.PUNCTUATOR); while(!current().data.equals("}")) parseStatement(); match(LexicalAnalyzer.TokenType.PUNCTUATOR); }
    private void parseWhile() { match(LexicalAnalyzer.TokenType.KEYWORD); match(LexicalAnalyzer.TokenType.PUNCTUATOR); String c = parseExpression(); match(LexicalAnalyzer.TokenType.PUNCTUATOR); match(LexicalAnalyzer.TokenType.PUNCTUATOR); while(!current().data.equals("}")) parseStatement(); match(LexicalAnalyzer.TokenType.PUNCTUATOR); }


    // 4. MAIN TESTER

    public static void main(String[] args) {
       /* String code =
                "class Animal { \n" +
                        "    integer age = 0 \n" +
                        "    void grow() { \n" +
                        "        age = age + 1 \n" +
                        "    } \n" +
                        "} \n" +
                        "class Dog extends Animal { \n" +
                        "    string breed = \"Pug\" \n" +
                        "}";*/
       /* String code =
                "class Calculator { \n" +
                        "    integer count = 0 \n" +
                        "    void compute() { \n" +
                        "        while (count < 5) { \n" +
                        "            count = count + 1 \n" +
                        "        } \n" +
                        "    } \n" +
                        "}";*/
        /*String code =
                "class Dog { \n" +
                        "    void bark() { \n" +
                        "        print(\"Woof\") \n" +
                        "    } \n" +
                        "} \n" +
                        "class Main { \n" +
                        "    void run() { \n" +
                        "        Dog d = new Dog() \n" +  // Object Creation!
                        "        d.bark() \n" +           // Method Call!
                        "    } \n" +
                        "}";*/
        String code =
                "class Main { \n" +
                        "    void run() { \n" +
                        "       integer count = 0 \n" +
                        "       integer limit = 5 \n" +
                        "       while (count < limit) { \n" +
                        "           count = count + 1 \n" +
                        "       } \n" +
                        "    } \n" +
                        "}";
       /* String code =
                "class Test { \n" +
                        "    void run() { \n" +
                        "        integer x = 10 \n" +
                        "        integer y = = 5   \n" + // ❌ Error 1: Syntax (Double equals)
                        "        integer z = 20 \n" +    // ✅ Valid: Should recover and parse this!
                        "        print(unknownVar) \n" + // ❌ Error 2: Semantic (Undefined variable)
                        "    } \n" +
                        "}";*/

        System.out.println("--- Source Code ---");
        System.out.println(code);

        LexicalAnalyzer lexer = new LexicalAnalyzer();
        List<LexicalAnalyzer.Token> tokens = lexer.tokenize(code);
        ZaraSemanticAnalyzer2 analyzer = new ZaraSemanticAnalyzer2(tokens);
        analyzer.parseProgram();
    }
}