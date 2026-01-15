import java.util.List;
import java.util.Stack;

public class ZaraBottomUpParser {
    private List<LexicalAnalyzer.Token> tokens;
    private Stack<String> stack;
    private int currentPosition;

    public ZaraBottomUpParser(List<LexicalAnalyzer.Token> tokens) {
        this.tokens = tokens;
        this.stack = new Stack<>();
        this.currentPosition = 0;
    }

    public void parse() {
        System.out.println("Stack\t\t\tInput\t\t\tAction");
        System.out.println("-----\t\t\t-----\t\t\t------");

        while (currentPosition < tokens.size() || stack.size() > 1) {
            // Simple Logic: Try to Reduce first; if we can't, then Shift.
            if (tryReduce()) {
                continue;
            }

            // If we are out of tokens and can't reduce, we are done (or stuck)
            if (currentPosition >= tokens.size()) {
                break;
            }

            // SHIFT ACTION
            shift();
        }

        // Final check
        if (stack.size() == 1 && stack.peek().equals("STATEMENT")) {
            System.out.println("✅ Accepted!");
        } else {
            System.out.println("❌ Syntax Error: Stack not empty or root not reached.");
        }
    }

    private void shift() {
        LexicalAnalyzer.Token token = tokens.get(currentPosition);
        // 2. Push the TYPE (e.g., "KEYWORD", "IDENTIFIER") onto the stack
        stack.push(token.type.toString()+ ":" + token.data);

        currentPosition++;

        System.out.println("SHIFT: " + token.type + " (\"" + token.data + "\")");
    }

    // We will implement this next!
    private boolean tryReduce() {
        // 0. Identifier Rule: IDENTIFIER -> EXPRESSION (only if not assigning!)
        if (stack.size() > 0 && stack.peek().startsWith("IDENTIFIER")) {
            // Lookahead: Only reduce if the NEXT token is NOT "="
            boolean isAssignment = false;
            if (currentPosition < tokens.size()) {
                if (tokens.get(currentPosition).data.equals("=")) {
                    isAssignment = true;
                }
            }

            if (!isAssignment) {
                System.out.println("REDUCE: IDENTIFIER -> EXPRESSION");
                stack.pop();
                stack.push("EXPRESSION");
                return true;
            }
        }
        if (stack.size() > 0 && stack.peek().startsWith("CONSTANT")) {
            System.out.println("REDUCE: CONSTANT -> EXPRESSION");
            stack.pop();
            stack.push("EXPRESSION");
            return true;
        }


        if (stack.size() >= 3) {
            String t1 = stack.get(stack.size() - 3);
            String t2 = stack.get(stack.size() - 2);
            String t3 = stack.peek();

            // Check if it is a boolean operator (starts with OPERATOR and contains <, >, or =)
            if (t1.equals("EXPRESSION") &&
                    t2.startsWith("OPERATOR") && (t2.contains("<") || t2.contains(">") || t2.contains("==")) &&
                    t3.equals("EXPRESSION")) {

                System.out.println("REDUCE: " + t1 + " " + t2 + " " + t3 + " -> CONDITION");
                stack.pop(); stack.pop(); stack.pop();
                stack.push("CONDITION");
                return true;
            }
        }
        if (stack.size() >= 3) {
            String t8 = stack.get(stack.size() - 3);
            String t9 = stack.get(stack.size() - 2); // Now looks like "OPERATOR:+"
            String t10 = stack.peek();

            // Check for EXPRESSION and valid MATH operators
            if (t8.equals("EXPRESSION") &&
                    t9.startsWith("OPERATOR") &&
                    (t9.contains("+") || t9.contains("-") || t9.contains("*") || t9.contains("/")) &&
                    t10.equals("EXPRESSION")) {

                System.out.println("REDUCE: " + t8 + " " + t9 + " " + t10 + " -> EXPRESSION");
                stack.pop(); stack.pop(); stack.pop();
                stack.push("EXPRESSION");
                return true;
            }
        }

        if (stack.size() >= 4) {
            String t4 = stack.get(stack.size() - 4); // KEYWORD
            String t5 = stack.get(stack.size() - 3); // IDENTIFIER
            String t6 = stack.get(stack.size() - 2); // OPERATOR
            String t7= stack.peek();                // CONSTANT

            if (t4.startsWith("KEYWORD") && t5.startsWith("IDENTIFIER") &&
                    t6.startsWith("OPERATOR") && t7.equals("EXPRESSION")) {

                // If the NEXT token is an operator (like +), do NOT reduce yet!
                if (currentPosition < tokens.size()) {
                    LexicalAnalyzer.Token nextToken = tokens.get(currentPosition);
                    if (nextToken.type == LexicalAnalyzer.TokenType.OPERATOR) {
                        return false;
                    }
                }

                System.out.println("REDUCE: " + t4 + " " + t5 + " " + t6 + " " + t7 + " -> STATEMENT");


                stack.pop();
                stack.pop();
                stack.pop();
                stack.pop();

                // 2. Push "STATEMENT" onto the stack
                stack.push("STATEMENT");


                return true;
            }
        }
        // Assignment Rule: IDENTIFIER = EXPRESSION -> STATEMENT
        if (stack.size() >= 3) {
            String t11 = stack.get(stack.size() - 3); // IDENTIFIER
            String t12 = stack.get(stack.size() - 2); // OPERATOR:=
            String t13 = stack.peek();                // EXPRESSION

            if (t11.startsWith("IDENTIFIER") && t12.contains("=") && t13.equals("EXPRESSION")) {
                // Lookahead: Don't reduce if math is coming (x = 10 + 5)
                if (currentPosition < tokens.size()) {
                    if (tokens.get(currentPosition).type == LexicalAnalyzer.TokenType.OPERATOR) {
                        return false;
                    }
                }

                System.out.println("REDUCE: " + t11 + " " + t12 + " " + t13 + " -> STATEMENT");
                stack.pop(); stack.pop(); stack.pop();
                stack.push("STATEMENT");
                return true;
            }
        }

        if (stack.size() >= 7) {
            // We need to check indices from [size-7] to [size-1]
            String kwd  = stack.get(stack.size() - 7); // KEYWORD:if
            String p1   = stack.get(stack.size() - 6); // (
            String cond = stack.get(stack.size() - 5); // CONDITION
            String p2   = stack.get(stack.size() - 4); // )
            String p3   = stack.get(stack.size() - 3); // {
            String stmt = stack.get(stack.size() - 2); // STATEMENT
            String p4   = stack.peek();                // }

            if (kwd.contains("if") && p1.contains("(") && cond.equals("CONDITION") &&
                    p2.contains(")") && p3.contains("{") && stmt.equals("STATEMENT") &&
                    p4.contains("}")) {

                System.out.println("REDUCE: if ( CONDITION ) { STATEMENT } -> STATEMENT");

                // Pop all 7 items!
                for (int i = 0; i < 7; i++) { stack.pop(); }

                stack.push("STATEMENT");
                return true;
            }
        }
        // 5. Sequence Rule: STATEMENT STATEMENT -> STATEMENT
        if (stack.size() >= 2) {
            String t1 = stack.get(stack.size() - 2);
            String t2 = stack.peek();

            if (t1.equals("STATEMENT") && t2.equals("STATEMENT")) {
                System.out.println("REDUCE: STATEMENT STATEMENT -> STATEMENT");
                stack.pop();
                stack.pop();
                stack.push("STATEMENT");
                return true;
            }
        }
        // 6. FOR Loop Rule
        // Pattern: for ( STATEMENT ; CONDITION ; STATEMENT ) { STATEMENT }
        if (stack.size() >= 11) {
            // Grab all 11 items (Reverse index from -11 to -1)
            String kwd   = stack.get(stack.size() - 11); // KEYWORD:for
            String p1    = stack.get(stack.size() - 10); // (
            String init  = stack.get(stack.size() - 9);  // STATEMENT (init)
            String semi1 = stack.get(stack.size() - 8);  // ;
            String cond  = stack.get(stack.size() - 7);  // CONDITION
            String semi2 = stack.get(stack.size() - 6);  // ;
            String updat = stack.get(stack.size() - 5);  // STATEMENT (update)
            String p2    = stack.get(stack.size() - 4);  // )
            String p3    = stack.get(stack.size() - 3);  // {
            String body  = stack.get(stack.size() - 2);  // STATEMENT (body)
            String p4    = stack.peek();                 // }

            if (kwd.contains("for") && p1.contains("(") && init.equals("STATEMENT") &&
                    semi1.contains(";") && cond.equals("CONDITION") && semi2.contains(";") &&
                    updat.equals("STATEMENT") && p2.contains(")") && p3.contains("{") &&
                    body.equals("STATEMENT") && p4.contains("}")) {

                System.out.println("REDUCE: for ( ... ) { ... } -> STATEMENT");

                // Pop all 11 items
                for (int i = 0; i < 11; i++) { stack.pop(); }

                stack.push("STATEMENT");
                return true;
            }
        }
        // 7. WHILE Loop Rule
        // Pattern: while ( CONDITION ) { STATEMENT }
        if (stack.size() >= 7) {
            String kwd  = stack.get(stack.size() - 7); // KEYWORD:while
            String p1   = stack.get(stack.size() - 6); // (
            String cond = stack.get(stack.size() - 5); // CONDITION
            String p2   = stack.get(stack.size() - 4); // )
            String p3   = stack.get(stack.size() - 3); // {
            String stmt = stack.get(stack.size() - 2); // STATEMENT
            String p4   = stack.peek();                // }

            if (kwd.contains("while") && p1.contains("(") && cond.equals("CONDITION") &&
                    p2.contains(")") && p3.contains("{") && stmt.equals("STATEMENT") &&
                    p4.contains("}")) {

                System.out.println("REDUCE: while ( CONDITION ) { STATEMENT } -> STATEMENT");
                for (int i = 0; i < 7; i++) { stack.pop(); } // Pop 7 items
                stack.push("STATEMENT");
                return true;
            }
        }
        // 8. DO-WHILE Loop Rule
        // Pattern: do { STATEMENT } while ( CONDITION ) ;
        if (stack.size() >= 9) {
            String kwdDo = stack.get(stack.size() - 9); // KEYWORD:do
            String p1    = stack.get(stack.size() - 8); // {
            String stmt  = stack.get(stack.size() - 7); // STATEMENT
            String p2    = stack.get(stack.size() - 6); // }
            String kwdWh = stack.get(stack.size() - 5); // KEYWORD:while
            String p3    = stack.get(stack.size() - 4); // (
            String cond  = stack.get(stack.size() - 3); // CONDITION
            String p4    = stack.get(stack.size() - 2); // )
            String semi  = stack.peek();                // ;

            if (kwdDo.contains("do") && p1.contains("{") && stmt.equals("STATEMENT") &&
                    p2.contains("}") && kwdWh.contains("while") && p3.contains("(") &&
                    cond.equals("CONDITION") && p4.contains(")") && semi.contains(";")) {

                System.out.println("REDUCE: do { STATEMENT } while ( CONDITION ) ; -> STATEMENT");
                for (int i = 0; i < 9; i++) { stack.pop(); } // Pop 9 items
                stack.push("STATEMENT");
                return true;
            }
        }


        return false; // No pattern matched
    }

    public static void main(String[] args) {
        // 1. The Source Code (Variable Declaration)
        /* String code = "integer x = 10 " +
                "if (x < 20) { " +
                "    x = x + 1 " +
                "}";*/
        /*String code = "for ( i = 0 ; i < 10 ; i = i + 1 ) { " +
                "    x = x + 1 " +
                "}";*/
        // Test Code: One while loop, one do-while loop
        String code = "while ( x < 10 ) { " +
                "    x = x + 1 " +
                "} " +
                "do { " +
                "    x = x - 1 " +
                "} while ( x > 0 ) ;";


        System.out.println("--- 1. Lexical Analysis ---");
        LexicalAnalyzer lexer = new LexicalAnalyzer();
        List<LexicalAnalyzer.Token> tokens = lexer.tokenize(code);

        // Optional: Print tokens to verify
        for (LexicalAnalyzer.Token t : tokens) {
            System.out.println(t);
        }

        System.out.println("\n--- 2. Bottom-Up Parsing ---");
        // 2. Initialize the Parser with the tokens
        ZaraBottomUpParser parser = new ZaraBottomUpParser(tokens);

        // 3. Start Parsing
        parser.parse();
    }

}
