package com.example.comp439_mohammad;

import java.io.*;
import java.util.*;

public class ScannerLexer {

    // Reserved words and tokens
    private static final Set<String> RESERVED_WORDS = new HashSet<>(Arrays.asList(
            "#include", "const", "var", "function", "newb", "endb", "if", "else",
            "while", "repeat", "until", "call", "cin", "cout", "exit"
    ));
    private static final Set<String> OPERATORS = new HashSet<>(Arrays.asList(
            ":=", "+", "-", "*", "/", "mod", "div", "=", ">", "<", ">=", "<=", "==", "!=", "=>"
    ));









    // Exception class for syntax errors
    static class SyntaxError extends Exception {
        public SyntaxError(String message) {
            super(message);
        }
    }
    // Token class to store information about each token
    static class Token {
        String type;
        String value;
        int line;

        public Token(String type, String value, int line) {
            this.type = type;
            this.value = value;
            this.line = line;
        }

        @Override
        public String toString() {
            return "Token{" +
                    "type='" + type + '\'' +
                    ", value='" + value + '\'' +
                    ", line=" + line +
                    '}';
        }
    }

    public static List<Token> scanFile(String filename) throws IOException, SyntaxError {
        List<Token> tokens = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(filename));

        String line;
        int lineNumber = 0;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            tokens.addAll(tokenizeLine(line, lineNumber));
        }

        reader.close();

        // Debug print to display all tokens after tokenization
        System.out.println("Tokenized: " + tokens);

        return tokens;
    }
    private static List<Token> tokenizeLine(String line, int lineNumber) throws SyntaxError {
        List<Token> tokens = new ArrayList<>();
        // Split the line based on operators, punctuation, and whitespace
        String[] parts = line.split("\\s+|(?=[(){};,<>=:+*\\-!])|(?<=[(){};,<>=:+*\\-!])");

        boolean inIncludeContext = line.trim().startsWith("#include");

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.isEmpty()) continue;

            // Handle #include context for < and >
            if (inIncludeContext && (part.equals("<") || part.equals(">"))) {
                tokens.add(new Token("Punctuation", part, lineNumber));
                continue;
            }

            // Check for compound operators like >=, <=, ==, !=, :=, =>, =<, and fix invalid operators
            if (i + 1 < parts.length) {
                String combined = part + parts[i + 1];

                if (combined.equals("=<")) {
                    tokens.add(new Token("Operator", "<=", lineNumber)); // Replace `=<` with `<=`
                    i++; // Skip the next part as it is part of the compound operator
                    continue;
                } else if (combined.equals("=!")) {
                    tokens.add(new Token("Operator", "!=", lineNumber)); // Replace `=!` with `!=`
                    i++;
                    continue;
                } else if (OPERATORS.contains(combined)) {
                    tokens.add(new Token("Operator", combined, lineNumber));
                    i++; // Skip the next part as it is part of the compound operator
                    continue;
                }
            }

            // Reserved words
            if (RESERVED_WORDS.contains(part)) {
                tokens.add(new Token("ReservedWord", part, lineNumber));
            }
            // Operators
            else if (OPERATORS.contains(part)) {
                tokens.add(new Token("Operator", part, lineNumber));
            }
            // Numbers (integers or decimals)
            else if (part.matches("\\d+(\\.\\d+)?")) {
                tokens.add(new Token("Number", part, lineNumber));
            }
            // Identifiers (variable names, function names, etc.)
            else if (part.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                tokens.add(new Token("Identifier", part, lineNumber));
            }
            // Punctuation symbols like parentheses, semicolons, etc.
            else if (part.matches("[(){};,]")) {
                tokens.add(new Token("Punctuation", part, lineNumber));
            }
            // Handle unexpected tokens
            else {
                throw new SyntaxError("Unexpected token '" + part + "' on line " + lineNumber);
            }
        }

        return tokens;
    }








    static class Parser {
        private final List<Token> tokens;
        private int current = 0;
        private static final int MAX_NESTED_DEPTH = 50; // الحد الأقصى للتداخل
        private int nestedDepth = 0; // لتتبع العمق الحالي

        // استثناء خاص لتجاوز العمق
        static class MaxDepthExceededException extends RuntimeException {
            public MaxDepthExceededException(String message) {
                super(message);
            }
        }

        public Parser(List<Token> tokens) {
            this.tokens = tokens;
        }

        public void parse() throws SyntaxError {
            program();
        }
        private void program() throws SyntaxError {
            System.out.println("Parsing program...");

            // Parse library declarations first
            libDecl();

            // Parse declarations and statements
            while (!isAtEnd()) {
                if (match("ReservedWord", "const", "var")) {
                    declarations(); // Parse variable or constant declarations
                } else if (match("ReservedWord", "function")) {
                    functionDecl(); // Parse function declarations
                } else if (match("ReservedWord", "newb")) {
                    block(); // Parse blocks
                } else if (match("ReservedWord", "if", "while", "repeat", "call", "exit")) {
                    statement(); // Parse standalone statements
                } else {
                    throw error(peek(), "Unexpected token in program.");
                }
            }

            System.out.println("Finished program.");
        }




        private void libDecl() throws SyntaxError {
            while (match("ReservedWord", "#include")) {
                consume("ReservedWord", "#include", null); // Match #include
                consume("Punctuation", "<", "Expected '<' after #include."); // Match <
                consume("Identifier", null, "Expected library name after '<'."); // Match library name
                consume("Punctuation", ">", "Expected '>' after library name."); // Match >
                consume("Punctuation", ";", "Expected ';' after library declaration."); // Match ;
                System.out.println("Parsed #include directive.");
            }
            System.out.println("Finished parsing library declarations.");
        }

        private void declarations() throws SyntaxError {
            System.out.println("Parsing declarations...");
            while (!isAtEnd() && match("ReservedWord", "const", "var")) {
                Token declarationType = advance(); // Move forward and get the current token
                System.out.println("Matched declaration type: " + declarationType.value);

                if ("const".equals(declarationType.value)) {
                    constDecl();
                } else if ("var".equals(declarationType.value)) {
                    varDecl();
                } else {
                    throw error(peek(), "Unexpected declaration type: " + declarationType.value);
                }
            }
            System.out.println("Finished declarations.");
        }


        private void constDecl() throws SyntaxError {
            System.out.println("Parsing const declaration...");
            // Skip consuming 'const', as it is already matched in `declarations`
            consume("Identifier", null, "Expected type (e.g., int, float) after 'const'.");
            consume("Identifier", null, "Expected constant name after type.");
            consume("Operator", "=", "Expected '=' after constant name.");
            consume("Number", null, "Expected constant value after '='.");
            consume("Punctuation", ";", "Expected ';' at the end of constant declaration.");
            System.out.println("Finished const declaration.");
        }




        private void varDecl() throws SyntaxError {
            System.out.println("Parsing var declaration...");

            // تحقق من أن نوع البيانات هو معرّف صالح
            if (!match("Identifier")) {
                throw error(peek(), "Expected type after 'var'.");
            }

            // Consume the type (e.g., int, float, etc.)
            Token typeToken = consume("Identifier", null, "Expected type after 'var'.");
            System.out.println("Variable type: " + typeToken.value);

            // Consume the first variable name
            Token firstVariableName = consume("Identifier", null, "Expected variable name.");
            System.out.println("Declared variable: " + typeToken.value + " " + firstVariableName.value);

            // Handle additional variable names separated by commas
            while (match("Punctuation", ",")) {
                consume("Punctuation", ",", null); // Consume the comma
                Token additionalVariableName = consume("Identifier", null, "Expected variable name after ','.");
                System.out.println("Declared additional variable: " + additionalVariableName.value);
            }

            // Ensure the declaration ends with a semicolon
            consume("Punctuation", ";", "Expected ';' at the end of variable declaration.");
            System.out.println("Finished var declaration.");
        }


        private void functionDecl() throws SyntaxError {
            System.out.println("Parsing function declaration...");
            consume("ReservedWord", "function", "Expected 'function' keyword.");
            Token functionName = consume("Identifier", null, "Expected function name.");
            consume("Punctuation", ";", "Expected ';' after function declaration.");
            System.out.println("Declared function: " + functionName.value);

            // Parse nested declarations inside the function
            while (match("ReservedWord", "const", "var")) {
                declarations();
            }

            // Parse the block for the function
            block();
        }


        private void block() throws SyntaxError {
            System.out.println("Parsing block...");
            nestedDepth++; // Increment nested block depth

            if (nestedDepth > MAX_NESTED_DEPTH) {
                throw new MaxDepthExceededException("Exceeded maximum nested block depth.");
            }

            consume("ReservedWord", "newb", "Expected 'newb' to start a block.");

            while (!check("ReservedWord", "endb") && !isAtEnd()) {
                statement(); // Recursively parse statements inside the block
            }

            consume("ReservedWord", "endb", "Expected 'endb' to close the block.");
            nestedDepth--; // Decrement nested depth

            // Optional semicolon after endb
            if (match("Punctuation", ";")) {
                advance(); // Consume optional semicolon
                System.out.println("Optional semicolon after 'endb' consumed.");
            }

            System.out.println("Block parsed successfully.");
        }


        private void assignment() throws SyntaxError {
            Token variable = consume("Identifier", null, "Expected variable name for assignment.");
            consume("Operator", ":=", "Expected ':=' in assignment.");
            Token value = expression(); // Parse the assigned expression

            // لا تتطلب فاصلة منقوطة إذا كانت تسبق else أو endb
            if (!match("ReservedWord", "else", "endb") && !isAtEnd()) {
                consume("Punctuation", ";", "Expected ';' at the end of assignment.");
            }

            System.out.println("Assignment: " + variable.value + " := " + value.value);
        }

        private void statement() throws SyntaxError {
            System.out.println("Parsing statement...");

            if (match("ReservedWord", "newb")) {
                block(); // Parse a nested block
            } else if (match("Identifier")) {
                assignment(); // Parse variable assignment
            } else if (match("ReservedWord", "if")) {
                ifStatement(); // Parse an if condition
            } else if (match("ReservedWord", "while")) {
                whileStatement(); // Parse a while loop
            } else if (match("ReservedWord", "repeat")) {
                repeatStatement(); // Parse a repeat-until loop
            } else if (match("ReservedWord", "call")) {
                callStatement(); // Parse a function call
            } else if (match("ReservedWord", "exit")) {
                exitStatement(); // Parse an exit statement
            } else if (match("ReservedWord", "cin")) {
                cinStatement(); // Parse cin input
            } else if (match("ReservedWord", "cout")) {
                coutStatement(); // Parse cout output
            } else {
                throw error(peek(), "Unexpected statement.");
            }
        }

        private void cinStatement() throws SyntaxError {
            System.out.println("Parsing cin statement...");
            consume("ReservedWord", "cin", "Expected 'cin' keyword.");
            consume("Operator", ">", "Expected '>>' after 'cin'.");
            consume("Operator", ">", "Expected '>>' after 'cin'.");
            Token variable = consume("Identifier", null, "Expected variable after 'cin >>'.");
            System.out.println("Input value assigned to: " + variable.value);

            // Optional semicolon
            if (match("Punctuation", ";")) {
                advance();
                System.out.println("Optional semicolon after 'cin' consumed.");
            }
            System.out.println("Cin statement parsed successfully.");
        }

        private void coutStatement() throws SyntaxError {
            System.out.println("Parsing cout statement...");
            consume("ReservedWord", "cout", "Expected 'cout' keyword.");
            consume("Operator", "<", "Expected '<<' after 'cout'.");
            consume("Operator", "<", "Expected '<<' after 'cout'.");

            // Allow numbers, identifiers, or constants as output
            if (match("Identifier") || match("Number") || match("ReservedWord", "const")) {
                Token outputToken = advance();
                System.out.println("Output value: " + outputToken.value);
            } else {
                throw error(peek(), "Expected variable, number, or constant after 'cout <<'.");
            }

            // Optional semicolon after cout statement
            if (match("Punctuation", ";")) {
                advance();
                System.out.println("Optional semicolon after 'cout' consumed.");
            }

            System.out.println("Cout statement parsed successfully.");
        }




        private void exitStatement() throws SyntaxError {
            System.out.println("Parsing exit statement...");
            consume("ReservedWord", "exit", "Expected 'exit' keyword.");

            // Directly handle the optional semicolon
            if (match("Punctuation", ";")) {
                advance();
                System.out.println("Optional semicolon after 'exit' consumed.");
            }

            System.out.println("Exit statement parsed successfully.");
        }






        private void ifStatement() throws SyntaxError {
            System.out.println("Parsing if statement...");
            consume("ReservedWord", "if", "Expected 'if' keyword.");
            consume("Punctuation", "(", "Expected '(' after 'if'.");

            parseCondition(); // Parse the condition

            consume("Punctuation", ")", "Expected ')' after condition.");

            // Parse the 'if' body
            statement();

            // Parse the optional 'else' branch
            if (match("ReservedWord", "else")) {
                advance(); // Consume 'else'
                statement(); // Parse the 'else' body
            }

            System.out.println("Finished parsing if statement.");
        }







        private void whileStatement() throws SyntaxError {
            System.out.println("Parsing while statement...");
            consume("ReservedWord", "while", "Expected 'while' keyword.");
            consume("Punctuation", "(", "Expected '(' after 'while'.");

            parseCondition(); // Parse the condition

            consume("Punctuation", ")", "Expected ')' after condition.");

            block(); // Parse the block for the loop
        }

        private void repeatStatement() throws SyntaxError {
            System.out.println("Parsing repeat statement...");
            consume("ReservedWord", "repeat", "Expected 'repeat' keyword.");

            while (!check("ReservedWord", "until") && !isAtEnd()) {
                statement(); // Parse statements inside the repeat block
            }

            consume("ReservedWord", "until", "Expected 'until' after repeat block.");
            parseCondition(); // Parse the condition after 'until'

            // Optional semicolon after repeat-until
            if (match("Punctuation", ";")) {
                advance();
                System.out.println("Optional semicolon after 'repeat-until' consumed.");
            }

            System.out.println("Repeat statement parsed successfully.");
        }



        private void callStatement() throws SyntaxError {
            System.out.println("Parsing function call...");
            consume("ReservedWord", "call", "Expected 'call' keyword.");
            Token functionName = consume("Identifier", null, "Expected function name after 'call'.");
            System.out.println("Function call: " + functionName.value);

            // استهلاك الفاصلة المنقوطة الاختيارية
            if (match("Punctuation", ";")) {
                advance();
                System.out.println("Optional semicolon after 'call' consumed.");
            }
        }

        private Token expression() throws SyntaxError {
            System.out.println("Parsing expression...");
            Token left = term();
            System.out.println("Left term: " + left);

            while (match("Operator", "+", "-", "*", "/", "mod", "div")) {
                Token operator = advance(); // Consume the operator
                Token right = term(); // Parse the next term
                System.out.println("Expression part: " + left.value + " " + operator.value + " " + right.value);
                left = new Token("Expression", left.value + " " + operator.value + " " + right.value, left.line);
            }

            System.out.println("Expression parsed successfully. Next token: " + peek());
            return left;
        }







        private Token term() throws SyntaxError {
            if (match("Punctuation", "(")) {
                consume("Punctuation", "(", "Expected '(' at the start of a nested expression.");
                Token nestedExpression = expression(); // Parse the nested expression
                consume("Punctuation", ")", "Expected ')' to close the nested expression.");
                System.out.println("Parsed nested expression: " + nestedExpression.value);
                return new Token("Expression", "(" + nestedExpression.value + ")", nestedExpression.line);
            } else if (match("Identifier")) {
                Token token = advance();
                System.out.println("Parsed identifier in term: " + token);
                return token;
            } else if (match("Number")) {
                Token token = advance();
                System.out.println("Parsed number in term: " + token);
                return token;
            } else {
                throw new SyntaxError("Expected identifier, number, or nested expression in term. Found: " + peek());
            }
        }

        private void parseCondition() throws SyntaxError {
            System.out.println("Parsing condition...");

            if (match("Punctuation", "(")) {
                consume("Punctuation", "(", "Expected '(' at the start of the condition.");
                expression(); // Parse the nested condition
                consume("Punctuation", ")", "Expected ')' to close the condition.");
            } else {
                Token left = term(); // Parse left-hand side
                Token operator = consume("Operator", null, "Expected a comparison operator.");
                if (!OPERATORS.contains(operator.value)) {
                    throw new SyntaxError("Invalid comparison operator: " + operator.value + " at line " + operator.line);
                }
                Token right = term(); // Parse right-hand side
                System.out.println("Condition parsed successfully: " + left.value + " " + operator.value + " " + right.value);
            }
        }



        private boolean match(String type, String... values) {
            if (!check(type)) return false;
            if (values == null || values.length == 0) return true; // Allow any value if `values` is null or empty
            for (String value : values) {
                if (peek().value.equals(value)) return true;
            }
            return false;
        }




        private boolean check(String type, String... values) {
            if (isAtEnd()) return false;
            Token token = peek();
            if (!token.type.equals(type)) return false; // Token type must match
            if (values.length == 0 || values[0] == null) return true; // Allow any value if `values` is empty or contains `null`
            for (String value : values) {
                if (token.value.equals(value)) return true;
            }
            return false;
        }
        private Token consume(String type, String value, String errorMessage) throws SyntaxError {
            Token currentToken = peek();
            if (!check(type) || (value != null && !currentToken.value.equals(value))) {
                throw error(currentToken, errorMessage);
            }
            System.out.println("Consuming token: " + currentToken);
            return advance();
        }



        private Token advance() {
            if (isAtEnd()) return new Token("EOF", "", tokens.size());
            System.out.println("Advancing token: " + peek());
            return tokens.get(current++);
        }





        private boolean isAtEnd() {
            return current >= tokens.size();
        }

        private Token peek() {
            return isAtEnd() ? new Token("EOF", "", current) : tokens.get(current);
        }



        private Token previous() {
            if (current > 0) {
                return tokens.get(current - 1);
            }
            throw new IllegalStateException("No previous token available. `previous()` called at the start of the token list.");
        }




        private SyntaxError error(Token token, String message) {
            return new SyntaxError(message + " at line " + token.line + ", near '" + token.value + "'. " +
                    "Current block depth: " + nestedDepth);
        }


    }
}