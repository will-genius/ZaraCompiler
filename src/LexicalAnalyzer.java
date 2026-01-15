import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LexicalAnalyzer {

    // 1. Define Token Types
    public enum TokenType {
        KEYWORD, CONSTANT, IDENTIFIER, OPERATOR, PUNCTUATOR, EOF
    }

    // 2. Token Class to hold data (UPDATED WITH LINE NUMBER)
    public static class Token {
        public TokenType type;
        public String data;
        public int line; // <--- New Field: Where did this token come from?

        public Token(TokenType type, String data, int line) {
            this.type = type;
            this.data = data;
            this.line = line;
        }

        @Override
        public String toString() {
            // Pretty print with line number
            return String.format("(%s, \"%s\", Line: %d)", type.name(), data, line);
        }
    }

    // 3. The Logic
    private static class TokenData {
        public Pattern pattern;
        public TokenType type;

        public TokenData(Pattern pattern, TokenType type) {
            this.pattern = pattern;
            this.type = type;
        }
    }
    private ArrayList<TokenData> tokenDatas;

    public LexicalAnalyzer() {
        tokenDatas = new ArrayList<>();

        // 1. KEYWORDS
        tokenDatas.add(new TokenData(Pattern.compile("^\\b(integer|float|string|array|stack|if|else|do|while|for|class|subprogram|return|void|end|extends|new|this|print)\\b"), TokenType.KEYWORD));

        // 2. CONSTANTS
        tokenDatas.add(new TokenData(Pattern.compile("^\"[^\"]*\""), TokenType.CONSTANT));
        tokenDatas.add(new TokenData(Pattern.compile("^[0-9]+(\\.[0-9]+)?"), TokenType.CONSTANT));

        // 3. IDENTIFIERS
        tokenDatas.add(new TokenData(Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*"), TokenType.IDENTIFIER));

        // 4. OPERATORS
        tokenDatas.add(new TokenData(Pattern.compile("^(==|>=|<=|!=|&&|\\|\\||\\+=|-=|\\*=|/=|\\+\\+|--|[+\\-*/=<>])"), TokenType.OPERATOR));

        // 5. PUNCTUATORS
        tokenDatas.add(new TokenData(Pattern.compile("^[\\(\\)\\[\\]\\{\\},;]"), TokenType.PUNCTUATOR));
        tokenDatas.add(new TokenData(Pattern.compile("^\\."), TokenType.PUNCTUATOR)); // Dot for OOP
    }

    // 4. The Main Tokenization Logic (UPDATED TO TRACK LINES)
    public List<Token> tokenize(String source) {
        List<Token> tokens = new ArrayList<>();
        int pos = 0;
        int line = 1; // <--- Start at Line 1

        while (pos < source.length()) {
            char c = source.charAt(pos);

            // Skip Whitespace & Count Newlines
            if (Character.isWhitespace(c)) {
                if (c == '\n') {
                    line++; // <--- Increment Line Counter
                }
                pos++;
                continue;
            }

            // Skip Comments (# to end of line)
            if (c == '#') {
                while (pos < source.length() && source.charAt(pos) != '\n') {
                    pos++;
                }
                continue;
            }

            // Try to match patterns
            boolean matched = false;
            String substring = source.substring(pos);

            for (TokenData tokenData : tokenDatas) {
                Matcher matcher = tokenData.pattern.matcher(substring);
                if (matcher.find()) {
                    String match = matcher.group();
                    // Pass 'line' to the new Token
                    tokens.add(new Token(tokenData.type, match, line));
                    pos += match.length();
                    matched = true;
                    break;
                }
            }

            if (!matched) {
                System.out.println("Lexical Error at line " + line + ": Unexpected character '" + c + "'");
                pos++;
            }
        }

        // Add EOF Token at the end so the parser knows when to stop
        tokens.add(new Token(TokenType.EOF, "", line));

        return tokens;
    }

    // 5. Test
    public static void main(String[] args) {
        String code =
                "integer count = 0\n" +
                        "integer max = 10\n" +
                        "while (count < max) {\n" +
                        "    count = count + 1\n" +
                        "}";

        System.out.println("--- 1. Lexical Analysis ---");
        LexicalAnalyzer lexer = new LexicalAnalyzer();
        List<LexicalAnalyzer.Token> tokens = lexer.tokenize(code);

        for (LexicalAnalyzer.Token t : tokens) {
            System.out.println(t);
        }
    }
}