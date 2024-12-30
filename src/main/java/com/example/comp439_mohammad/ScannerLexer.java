package com.example.comp439_mohammad;

import java.io.*;
import java.util.*;

public class ScannerLexer {

    private static final Set<String> RESERVED_WORDS = new HashSet<>(Arrays.asList(
            "#include", "const", "var", "function", "newb", "endb", "if", "else",
            "while", "repeat", "until", "call", "cin", "cout", "exit"
    ));
    private static final Set<String> OPERATORS = new HashSet<>(Arrays.asList(
            ":=", "+", "-", "*", "/", "mod", "div", "=", ">", "<", "=>", "=<", "=!"
    ));

    static class SyntaxError extends Exception {
        public SyntaxError(String message) {
            super(message);
        }
    }

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

        System.out.println("Tokenized: " + tokens);

        return tokens;
    }

    private static List<Token> tokenizeLine(String line, int lineNumber) throws SyntaxError {
        List<Token> tokens = new ArrayList<>();
        String[] parts = line.split("\\s+|(?=[(){};,<>=:+*\\-!])|(?<=[(){};,<>=:+*\\-!])");

        boolean inIncludeContext = line.trim().startsWith("#include");

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.isEmpty()) continue;

            if (inIncludeContext && (part.equals("<") || part.equals(">"))) {
                tokens.add(new Token("Punctuation", part, lineNumber));
                continue;
            }

            if (i + 1 < parts.length) {
                String combined = part + parts[i + 1];

                if (combined.equals("=<")) {
                    tokens.add(new Token("Operator", "=<", lineNumber));
                    i++;
                    continue;
                } else if (combined.equals("=!")) {
                    tokens.add(new Token("Operator", "=!", lineNumber));
                    i++;
                    continue;
                } else if (OPERATORS.contains(combined)) {
                    tokens.add(new Token("Operator", combined, lineNumber));
                    i++;
                    continue;
                }
            }

            if (RESERVED_WORDS.contains(part)) {
                tokens.add(new Token("ReservedWord", part, lineNumber));
            }
            else if (OPERATORS.contains(part)) {
                tokens.add(new Token("Operator", part, lineNumber));
            }
            else if (part.matches("\\d+(\\.\\d+)?")) {
                tokens.add(new Token("Number", part, lineNumber));
            }
            else if (part.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                tokens.add(new Token("Identifier", part, lineNumber));
            }
            else if (part.matches("[(){};,]")) {
                tokens.add(new Token("Punctuation", part, lineNumber));
            }
            else {
                throw new SyntaxError("Unexpected token '" + part + "' on line " + lineNumber);
            }
        }

        return tokens;
    }

    static class Parser {
        private final List<Token> tokens;
        private int current = 0;

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

            libDecl();

            declarations();

            while (match("ReservedWord", "function")) {
                functionDecl();
            }

            block();

            exitStatement();

            if (!isAtEnd()) {
                throw error(peek(), "Extra tokens after 'exit'. Program should end.");
            }

            System.out.println("Finished program.");
        }

        private void libDecl() throws SyntaxError {
            while (match("ReservedWord", "#include")) {
                consume("ReservedWord", "#include", null);        // Match #include
                consume("Punctuation", "<", "Expected '<' after #include.");
                consume("Identifier", null, "Expected library name after '<'.");
                consume("Punctuation", ">", "Expected '>' after library name.");
                consume("Punctuation", ";", "Expected ';' after library declaration.");
                System.out.println("Parsed #include directive.");
            }
            System.out.println("Finished parsing library declarations (if any).");
        }

        private void declarations() throws SyntaxError {
            while (!isAtEnd() && match("ReservedWord", "const")) {
                constDecl();
            }
            while (!isAtEnd() && match("ReservedWord", "var")) {
                varDecl();
            }
        }

        private void constDecl() throws SyntaxError {
            System.out.println("Parsing const declaration...");
            consume("ReservedWord", "const", "Expected 'const' keyword.");
            Token type = consume("Identifier", null, "Expected data type (e.g., int, float, char) after 'const'.");
            Token constName = consume("Identifier", null, "Expected constant name after type.");
            consume("Operator", "=", "Expected '=' after constant name.");

            Token valToken = null;
            if (match("Number")) {
                valToken = advance();
            } else {
                throw error(peek(), "Expected numeric constant value after '='.");
            }

            consume("Punctuation", ";", "Expected ';' at the end of constant declaration.");

            System.out.println("Finished const declaration: "
                    + type.value + " " + constName.value + " = " + valToken.value);
        }

        private void varDecl() throws SyntaxError {
            System.out.println("Parsing var declaration...");
            consume("ReservedWord", "var", "Expected 'var' keyword.");

            Token typeToken = consume("Identifier", null, "Expected data type after 'var'.");
            System.out.println("Variable type: " + typeToken.value);

            Token firstVar = consume("Identifier", null, "Expected variable name.");
            System.out.println("Declared variable: " + typeToken.value + " " + firstVar.value);

            while (match("Punctuation", ",")) {
                consume("Punctuation", ",", null);
                Token additionalVariableName = consume("Identifier", null, "Expected variable name after ','.");
                System.out.println("Declared additional variable: " + additionalVariableName.value);
            }

            consume("Punctuation", ";", "Expected ';' at the end of variable declaration.");
            System.out.println("Finished var declaration.");
        }

        private void functionDecl() throws SyntaxError {
            System.out.println("Parsing function declaration...");

            consume("ReservedWord", "function", "Expected 'function' keyword.");
            Token functionName = consume("Identifier", null, "Expected function name.");
            consume("Punctuation", ";", "Expected ';' after function name.");

            System.out.println("Declared function: " + functionName.value);

            declarations();

            block();

            consume("Punctuation", ";", "Expected ';' after function block.");

            System.out.println("Finished function declaration: " + functionName.value);
        }

        private void block() throws SyntaxError {
            System.out.println("Parsing block...");

            consume("ReservedWord", "newb", "Expected 'newb' to start a block.");
            stmtList();
            consume("ReservedWord", "endb", "Expected 'endb' to close the block.");

            System.out.println("Block parsed successfully.");
        }

        private void stmtList() throws SyntaxError {
            while (!check("ReservedWord", "endb") && !isAtEnd()) {
                statement();

                maybeConsumeSemicolon();
            }
        }

        private void maybeConsumeSemicolon() throws SyntaxError {
            if (match("Punctuation", ";")) {
                advance(); // consume semicolon
                System.out.println("Consumed semicolon after statement.");
            } else {
                Token currentToken = peek();
                throw new SyntaxError(
                        "Unexpected token '" + currentToken.value +
                                "' instead of a semicolon after statement. (Line " + currentToken.line + ")"
                );
            }
        }

        private void statement() throws SyntaxError {
            System.out.println("Parsing statement...");

            if (match("ReservedWord", "newb")) {
                block();
            }
            else if (match("ReservedWord", "if")) {
                ifStatement();
            }
            else if (match("ReservedWord", "while")) {
                whileStatement();
            }
            else if (match("ReservedWord", "repeat")) {
                repeatStatement();
            }
            else if (match("ReservedWord", "call")) {
                callStatement();
            }
            else if (match("ReservedWord", "cin")) {
                cinStatement();
            }
            else if (match("ReservedWord", "cout")) {
                coutStatement();
            }
            else if (match("Identifier")) {
                assignment();
            }
            else if (match("ReservedWord", "exit")) {
                exitStatement();
            }
            else {
                throw error(peek(), "Unexpected statement.");
            }
        }

        private void assignment() throws SyntaxError {
            Token varName = consume("Identifier", null, "Expected variable name for assignment.");
            consume("Operator", ":=", "Expected ':=' in assignment.");
            Token value = expression();
            System.out.println("Assignment: " + varName.value + " := " + value.value);
        }

        private void cinStatement() throws SyntaxError {
            System.out.println("Parsing cin statement...");
            consume("ReservedWord", "cin", "Expected 'cin' keyword.");
            consume("Operator", ">", "Expected '>>' after 'cin'.");
            consume("Operator", ">", "Expected '>>' after 'cin'.");
            Token variable = consume("Identifier", null, "Expected variable after 'cin >>'.");
            System.out.println("Input value assigned to: " + variable.value);
        }

        private void coutStatement() throws SyntaxError {
            System.out.println("Parsing cout statement...");
            consume("ReservedWord", "cout", "Expected 'cout' keyword.");
            consume("Operator", "<", "Expected '<<' after 'cout'.");
            consume("Operator", "<", "Expected '<<' after 'cout'.");

            if (match("Identifier") || match("Number")) {
                Token outputToken = advance();
                System.out.println("Output value: " + outputToken.value);
            } else {
                throw error(peek(), "Expected variable or number after 'cout <<'.");
            }
        }

        private void ifStatement() throws SyntaxError {
            System.out.println("Parsing if statement...");
            consume("ReservedWord", "if", "Expected 'if' keyword.");
            consume("Punctuation", "(", "Expected '(' after 'if'.");
            parseCondition();
            consume("Punctuation", ")", "Expected ')' after condition.");

            statement();

            if (match("ReservedWord", "else")) {
                advance();
                statement();
            }
            System.out.println("Finished if statement.");
        }

        private void whileStatement() throws SyntaxError {
            System.out.println("Parsing while statement...");
            consume("ReservedWord", "while", "Expected 'while' keyword.");
            consume("Punctuation", "(", "Expected '(' after 'while'.");
            parseCondition();
            consume("Punctuation", ")", "Expected ')' after condition.");
            block();
        }

        private void repeatStatement() throws SyntaxError {
            System.out.println("Parsing repeat statement...");
            consume("ReservedWord", "repeat", "Expected 'repeat' keyword.");
            while (!check("ReservedWord", "until") && !isAtEnd()) {
                statement();
                maybeConsumeSemicolon();
            }
            consume("ReservedWord", "until", "Expected 'until' after repeat block.");
            parseCondition();
            System.out.println("Repeat statement parsed successfully.");
        }

        private void callStatement() throws SyntaxError {
            System.out.println("Parsing function call...");
            consume("ReservedWord", "call", "Expected 'call' keyword.");
            Token functionName = consume("Identifier", null, "Expected function name after 'call'.");
            System.out.println("Function call: " + functionName.value);
        }

        private void exitStatement() throws SyntaxError {
            System.out.println("Parsing exit statement...");
            consume("ReservedWord", "exit", "Expected 'exit' keyword.");
            // You may want to consume a semicolon if your grammar says so
            if (match("Punctuation", ";")) {
                advance();
                System.out.println("Optional semicolon after 'exit' consumed.");
            }
            System.out.println("Exit statement parsed successfully.");
        }

        private Token expression() throws SyntaxError {
            System.out.println("Parsing expression...");
            Token left = term();

            while (match("Operator", "+", "-", "*", "/", "mod", "div")) {
                Token operator = advance();
                Token right = term();
                left = new Token("Expression",
                        "(" + left.value + " " + operator.value + " " + right.value + ")",
                        left.line);
            }
            System.out.println("Parsed expression: " + left.value);
            return left;
        }

        private Token term() throws SyntaxError {
            if (match("Punctuation", "(")) {
                consume("Punctuation", "(", "Expected '(' for nested expression.");
                Token nested = expression();
                consume("Punctuation", ")", "Expected ')' after nested expression.");
                return new Token("Expression", "(" + nested.value + ")", nested.line);
            }
            else if (match("Identifier")) {
                return advance();
            }
            else if (match("Number")) {
                return advance();
            }
            else {
                throw error(peek(), "Expected identifier, number, or '(...)' in expression.");
            }
        }

        private void parseCondition() throws SyntaxError {
            System.out.println("Parsing condition...");

            Token left = term();

            if (!match("Operator", "=", "=!", "<", "=<", ">", "=>")) {
                throw error(peek(), "Expected relational operator in condition.");
            }
            Token op = advance();

            Token right = term();

            System.out.println("Parsed condition: " + left.value + " " + op.value + " " + right.value);
        }

        private boolean match(String type, String... values) {
            if (!check(type)) return false;
            if (values == null || values.length == 0) return true;
            for (String value : values) {
                if (peek().value.equals(value)) return true;
            }
            return false;
        }

        private boolean check(String type, String... values) {
            if (isAtEnd()) return false;
            Token token = peek();
            if (!token.type.equals(type)) return false;
            if (values.length == 0 || values[0] == null) return true;
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
            return advance();
        }

        private Token advance() {
            if (!isAtEnd()) {
                return tokens.get(current++);
            }
            return new Token("EOF", "", current);
        }

        private boolean isAtEnd() {
            return current >= tokens.size();
        }

        private Token peek() {
            return isAtEnd() ? new Token("EOF", "", current) : tokens.get(current);
        }

        private SyntaxError error(Token token, String message) {
            return new SyntaxError(
                    message + " at line " + token.line + ", near '" + token.value + "'. " +
                            "Current block depth: "
            );
        }
    }
}
