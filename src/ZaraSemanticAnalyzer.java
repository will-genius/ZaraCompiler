import java.util.List;

public class ZaraSemanticAnalyzer {
    private List<LexicalAnalyzer.Token> tokens;
    private int pos;
    private symboltable symbolTable;
    private TACInstruction.CodeGenerator generator; // External class

    // Global Error Flag
    private boolean hasError = false;

    public ZaraSemanticAnalyzer(List<LexicalAnalyzer.Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
        this.symbolTable = new symboltable();
        this.generator = new TACInstruction.CodeGenerator();
    }

    // --- Helper Methods ---
    private void reportError(String message) {
        System.out.println("⛔ " + message);
        hasError = true;
    }
    private LexicalAnalyzer.Token current() {
        if (pos >= tokens.size()) {
            // FIX: Added the 3rd argument (-1) for the line number
            return new LexicalAnalyzer.Token(LexicalAnalyzer.TokenType.EOF, "", -1);
        }
        return tokens.get(pos);
    }

    private void match(LexicalAnalyzer.TokenType expectedType) {
        if (current().type == expectedType) {
            pos++;
        } else {
            reportError("Syntax Error: Expected " + expectedType + " but found " + current().type);
            pos++;
        }
    }

    private void checkTypeConsistency(String declaredType, String valueData) {
        String inferredType = "unknown";
        if (valueData.startsWith("\"")) inferredType = "string";
        else if (valueData.contains(".")) inferredType = "float";
        else if (valueData.matches("\\d+")) inferredType = "integer";
        else if (valueData.equals("true") || valueData.equals("false")) inferredType = "boolean";
        else if (valueData.startsWith("t") && valueData.matches("t\\d+")) return;

        if (!inferredType.equals("unknown") && !declaredType.equals(inferredType)) {
            reportError("Semantic Error: Type Mismatch! Cannot assign " + inferredType + " ('" + valueData + "') to " + declaredType + ".");
        }
    }

    // ==========================================
    // THE PIPELINE: Parse -> Check -> Optimize
    // ==========================================
    public void parseProgram() {
        System.out.println("--- 1. Starting Syntax & Semantic Analysis ---");
        while (current().type != LexicalAnalyzer.TokenType.EOF) {
            parseStatement();
        }
        System.out.println("--- Analysis Complete ---");

        // The Gate: Only proceed if the code is clean
        if (!hasError) {
            System.out.println("✅ Build Successful! Generating Code...");

            // --- CONNECTING THE OPTIMIZER ---
            // 1. Get the raw TAC code
            List<TACInstruction> rawCode = generator.getInstructions();

            // 2. Initialize the external Optimizer
            ZaraOptimizer optimizer = new ZaraOptimizer(rawCode);

            // 3. Run Optimization
            optimizer.optimize();
            // --------------------------------

            // 4. Print the final result
            generator.printCode();
        } else {
            System.out.println("❌ Build Failed: Semantic/Syntax errors found. Optimization skipped.");
        }
    }

    private void parseStatement() {
        LexicalAnalyzer.Token token = current();
        if (token.type == LexicalAnalyzer.TokenType.KEYWORD) {
            if (token.data.matches("integer|float|string|boolean")) {
                parseDeclaration();
            } else if (token.data.equals("if")) {
                parseIf();
            } else if (token.data.equals("while")) {
                parseWhile();
            } else {
                match(LexicalAnalyzer.TokenType.KEYWORD);
            }
        }
        // CRITICAL FIX: Do not skip '}', return so loops can close!
        else if (token.data.equals("}")) {
            return;
        }
        else if (token.type == LexicalAnalyzer.TokenType.IDENTIFIER) {
            parseAssignment();
        } else {
            pos++;
        }
    }

    // --- Parsing Logic (Standard) ---
    private void parseDeclaration() {
        String declaredType = current().data;
        match(LexicalAnalyzer.TokenType.KEYWORD);
        String varName = current().data;
        match(LexicalAnalyzer.TokenType.IDENTIFIER);

        symbolTable.add(varName, declaredType);

        if (current().data.equals("=")) {
            match(LexicalAnalyzer.TokenType.OPERATOR);
            String resultTemp = parseExpression();
            checkTypeConsistency(declaredType, resultTemp);
            generator.emit(varName, resultTemp, "", null);
        }
    }

    private void parseAssignment() {
        String varName = current().data;
        match(LexicalAnalyzer.TokenType.IDENTIFIER);
        symbol existingSym = symbolTable.get(varName);
        if (existingSym == null) reportError("Semantic Error: Variable '" + varName + "' used before declaration.");

        match(LexicalAnalyzer.TokenType.OPERATOR);
        String resultTemp = parseExpression();
        if (existingSym != null) checkTypeConsistency(existingSym.getType(), resultTemp);
        generator.emit(varName, resultTemp, "", null);
    }

    private String parseExpression() {
        String op1 = current().data;
        if (current().type == LexicalAnalyzer.TokenType.IDENTIFIER) {
            if (symbolTable.get(op1) == null) reportError("Semantic Error: Variable '" + op1 + "' used in expression before declaration.");
            pos++;
        } else if (current().type == LexicalAnalyzer.TokenType.CONSTANT) {
            pos++;
        }

        if (current().type == LexicalAnalyzer.TokenType.OPERATOR && !current().data.equals(";")) {
            String operator = current().data;
            pos++;
            String op2 = parseExpression();
            if (op1.startsWith("\"") || op2.startsWith("\"")) reportError("Semantic Error: Cannot perform math on Strings!");

            String temp = generator.newTemp();
            generator.emit(temp, op1, operator, op2);
            return temp;
        }
        return op1;
    }

    private void parseIf() {
        match(LexicalAnalyzer.TokenType.KEYWORD); match(LexicalAnalyzer.TokenType.PUNCTUATOR);
        String condition = parseExpression();
        match(LexicalAnalyzer.TokenType.PUNCTUATOR); match(LexicalAnalyzer.TokenType.PUNCTUATOR);

        String labelElse = generator.newLabel();
        String labelEnd = generator.newLabel();
        generator.emitIfFalse(condition, labelElse);

        while(!current().data.equals("}") && current().type != LexicalAnalyzer.TokenType.EOF) parseStatement();
        match(LexicalAnalyzer.TokenType.PUNCTUATOR);

        generator.emitJump(labelEnd);
        generator.emitLabel(labelElse);

        if (current().data.equals("else")) {
            match(LexicalAnalyzer.TokenType.KEYWORD); match(LexicalAnalyzer.TokenType.PUNCTUATOR);
            while(!current().data.equals("}") && current().type != LexicalAnalyzer.TokenType.EOF) parseStatement();
            match(LexicalAnalyzer.TokenType.PUNCTUATOR);
        }
        generator.emitLabel(labelEnd);
    }

    private void parseWhile() {
        String labelStart = generator.newLabel();
        String labelEnd = generator.newLabel();
        generator.emitLabel(labelStart);

        match(LexicalAnalyzer.TokenType.KEYWORD); match(LexicalAnalyzer.TokenType.PUNCTUATOR);
        String condition = parseExpression();
        match(LexicalAnalyzer.TokenType.PUNCTUATOR); match(LexicalAnalyzer.TokenType.PUNCTUATOR);

        generator.emitIfFalse(condition, labelEnd);
        while(!current().data.equals("}") && current().type != LexicalAnalyzer.TokenType.EOF) parseStatement();
        match(LexicalAnalyzer.TokenType.PUNCTUATOR);

        generator.emitJump(labelStart);
        generator.emitLabel(labelEnd);
    }



    // ==========================================
    // 3. MAIN TESTER
    // ==========================================
    public static void main(String[] args) {
        // TEST CASE: Contains semantic errors
        /*String code =
                "integer x = 10 \n" +
                        "integer y = 5 + 3 * 2 \n" +
                        "integer z = \"hello\" \n" +
                        "x = x + 1 \n" +
                        "unknownVar = 50";*/
       // String Code = "integer x = 10 \n integer y = x + 5";
       /* String code =
                "integer x = 0 \n" +
                        "integer limit = 5 \n" +
                        "while ( x < limit ) { \n" +
                        "    x = x + 1 \n" +
                        "    if ( x == 3 ) { \n" +
                        "        print(x) \n" + // Function Call!
                        "    } \n" +
                        "}";*/
        // Full Optimization Test
        String code =
                "integer x = 0 \n" +
                        "integer limit = 10 \n" +
                        "integer unusedVar = 999 \n" +      // Should be removed (Dead Code)
                        "while ( x < limit ) { \n" +
                        "    integer y = 5 * 20 \n" +       // Should become 100 (Const Fold) AND move out (Code Motion)
                        "    x = x + 1 \n" +
                        "}";
        System.out.println("--- Test  Code  ---");
        LexicalAnalyzer lexer = new LexicalAnalyzer();
        List<LexicalAnalyzer.Token> tokens = lexer.tokenize(code);
        ZaraSemanticAnalyzer analyzer = new ZaraSemanticAnalyzer(tokens);
        analyzer.parseProgram();

        System.out.println("\n-----------------------------------\n");


    }
}