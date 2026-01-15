import java.util.List;

public class ZaraParser {
    private List<LexicalAnalyzer.Token> tokens;
    private int pos; // Current position in the list
    private symboltable symbolTable;
    public ZaraParser(List<LexicalAnalyzer.Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
        this.symbolTable = new symboltable();
    }

    // Helper: Peek at the current token without consuming it
    private LexicalAnalyzer.Token current() {
        if (pos >= tokens.size()) {
            // FIX: Added the 3rd argument (-1) for the line number
            return new LexicalAnalyzer.Token(LexicalAnalyzer.TokenType.EOF, "", -1);
        }
        return tokens.get(pos);
    }

    // The logic to "eat" a token
    private void match(LexicalAnalyzer.TokenType expectedType) {
        LexicalAnalyzer.Token token = current();

        if (token.type == expectedType) {
            System.out.println("Matched: " + token.data);
            pos++; // Move to the next token
        } else {
            System.out.println("Error: Expected " + expectedType + " but found " + token.type);
        }
    }

    // We will add grammar rules here next...
    public void parseProgram() {
        // While we haven't reached the End of File (EOF)...
        while (current().type != LexicalAnalyzer.TokenType.EOF) {
            // ... parse the next statement
            parseStatement();
        }
    }

    public void parseStatement() {
        LexicalAnalyzer.Token token = current();

        // 1. Check for Keywords (Declarations or Control Flow)
        if (token.type == LexicalAnalyzer.TokenType.KEYWORD) {
            if (token.data.equals("if")) {
                parseIf();
            } else if (token.data.equals("for")) {
                parseFor();
            }else if (token.data.equals("while")) {
                parseWhile();
            }else if (token.data.equals("do")) {
                parseDoWhile();
            }else if (token.data.equals("integer") || token.data.equals("float")
                    || token.data.equals("string") || token.data.equals("boolean")) {
                parseDeclaration();
            } else {
                System.out.println("Unexpected keyword: " + token.data);
                pos++; // Skip to avoid infinite loop
            }
        }
        // 2. Check for Identifiers (Assignments like 'count = count + 1')
        else if (token.type == LexicalAnalyzer.TokenType.IDENTIFIER) {
            parseAssignment();
        }
        // 3. Skip unknown things to keep the compiler running
        else {
            System.out.println("Error: Unexpected token at start of statement: " + token);
            pos++;
        }
    }

    private void parseDeclaration() {
        System.out.println("--- Parsing Declaration ---");

        // 1. Capture the Type (e.g., "integer")
        LexicalAnalyzer.Token typeToken = current();
        match(LexicalAnalyzer.TokenType.KEYWORD);

        // 2. Capture the Name (e.g., "x")
        LexicalAnalyzer.Token nameToken = current();
        match(LexicalAnalyzer.TokenType.IDENTIFIER);

        // 3. Register it in the Symbol Table!
        // We use the name ("x") as the key, and the type ("integer") as the value.
        symbolTable.add(nameToken.data, typeToken.data);

        match(LexicalAnalyzer.TokenType.OPERATOR);
        match(LexicalAnalyzer.TokenType.CONSTANT);
    }
    private void parseCondition() {
        System.out.println("--- Parsing Condition ---");
        match(LexicalAnalyzer.TokenType.IDENTIFIER);
        match(LexicalAnalyzer.TokenType.OPERATOR);
        // Part 3: The Right Side (e.g., '5')
        // (For simplicity, we assume Identifier or Constant here)
        LexicalAnalyzer.Token token = current();
        if (token.type == LexicalAnalyzer.TokenType.IDENTIFIER) {
            match(LexicalAnalyzer.TokenType.IDENTIFIER);
        } else {
            match(LexicalAnalyzer.TokenType.CONSTANT);
        }

        // --- NEW: Check for Logical Operators (&&, ||) ---
        LexicalAnalyzer.Token next = current();
        if (next.type == LexicalAnalyzer.TokenType.OPERATOR) {
            if (next.data.equals("&&") || next.data.equals("||")) {
                System.out.println("--- Found Logical Operator '" + next.data + "' ---");
                match(LexicalAnalyzer.TokenType.OPERATOR); // Eat && or ||
                parseCondition();
            }
        }
    }
    private void parseIf() {
        System.out.println("--- Parsing If Statement ---");
        match(LexicalAnalyzer.TokenType.KEYWORD);

        match(LexicalAnalyzer.TokenType.PUNCTUATOR);

        parseCondition();

        match(LexicalAnalyzer.TokenType.PUNCTUATOR);

        match(LexicalAnalyzer.TokenType.PUNCTUATOR);

        parseStatement();

        match(LexicalAnalyzer.TokenType.PUNCTUATOR);


        if (current().data.equals("else")) {
            System.out.println("--- Parsing Else Block ---");
            match(LexicalAnalyzer.TokenType.KEYWORD);    // Eat 'else'
            match(LexicalAnalyzer.TokenType.PUNCTUATOR); // Eat '{'
            parseStatement();                            // Eat body
            match(LexicalAnalyzer.TokenType.PUNCTUATOR); // Eat '}'
        }
    }
    private void parseAssignment() {
        System.out.println("--- Parsing Assignment ---");
        match(LexicalAnalyzer.TokenType.IDENTIFIER);
        match(LexicalAnalyzer.TokenType.OPERATOR);
        parseExpression();
    }
    private void parseExpression() {
        System.out.println("--- Parsing Expression ---");

        // Step A: We expect a value first (Number or Variable)
        LexicalAnalyzer.Token token = current();
        if (token.type == LexicalAnalyzer.TokenType.IDENTIFIER) {
            match(LexicalAnalyzer.TokenType.IDENTIFIER);
        } else if (token.type == LexicalAnalyzer.TokenType.CONSTANT) {
            match(LexicalAnalyzer.TokenType.CONSTANT);
        } else {
            System.out.println("Error: Expected Identifier or Constant in expression.");
        }

        // Step B: Is there an operator next? (e.g., '+')
        if (current().type == LexicalAnalyzer.TokenType.OPERATOR) {
            System.out.println("--- Found Operator, continuing expression ---");
            match(LexicalAnalyzer.TokenType.OPERATOR); // Eat '+'
            parseExpression(); // Recurse to handle the rest (e.g., '1')
        }
    }

    private void parseFor() {
        System.out.println("--- Parsing For Loop ---");

        match(LexicalAnalyzer.TokenType.KEYWORD);    // Match 'for'
        match(LexicalAnalyzer.TokenType.PUNCTUATOR); // Match '('
        parseAssignment();
        match(LexicalAnalyzer.TokenType.PUNCTUATOR);
        parseCondition();
        match(LexicalAnalyzer.TokenType.PUNCTUATOR);
        parseAssignment();
        match(LexicalAnalyzer.TokenType.PUNCTUATOR); // Match ')'
        match(LexicalAnalyzer.TokenType.PUNCTUATOR); // Match '{'
        parseStatement();                            // Body
        match(LexicalAnalyzer.TokenType.PUNCTUATOR); // Match '}'
    }

    private void parseWhile() {

        System.out.println("--- Parsing while loop---");

        match(LexicalAnalyzer.TokenType.KEYWORD);
        match(LexicalAnalyzer.TokenType.PUNCTUATOR);
        parseCondition();
        match(LexicalAnalyzer.TokenType.PUNCTUATOR);
        match(LexicalAnalyzer.TokenType.PUNCTUATOR);
        parseStatement();
        match(LexicalAnalyzer.TokenType.PUNCTUATOR);
    }

    private void parseDoWhile() {
        System.out.println("--- Parsing do_while loop---");
        match(LexicalAnalyzer.TokenType.KEYWORD);

        match(LexicalAnalyzer.TokenType.PUNCTUATOR);

        parseStatement();

        match(LexicalAnalyzer.TokenType.PUNCTUATOR);

        match(LexicalAnalyzer.TokenType.KEYWORD);

        match(LexicalAnalyzer.TokenType.PUNCTUATOR);
        parseCondition();


        match(LexicalAnalyzer.TokenType.PUNCTUATOR);
        match(LexicalAnalyzer.TokenType.PUNCTUATOR);


    }

}