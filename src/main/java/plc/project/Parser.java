package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;


    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        List<Ast.Field> fields = new ArrayList<>();
        List<Ast.Method> methods = new ArrayList<>();

        while (match("LET"))
            fields.add(parseField());
        while (match("DEF"))
            methods.add(parseMethod());

        return new Ast.Source(fields,methods);
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Field parseField() throws ParseException {
        Ast.Expr expr = null;

            String ident1 = tokens.get(0).getLiteral();
            tokens.advance();
            tokens.advance();
            String type = tokens.get(0).getLiteral();
            tokens.advance();
            if(match("="))
                 expr = parseExpression();
            return new Ast.Field(ident1,type,Optional.of(expr));

    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {
        List<String> indents = new ArrayList<>();
        List<Ast.Stmt> stmts = new ArrayList<>();
        List<String> typeNames = new ArrayList<>();
           String indent1 = tokens.get(0).getLiteral();
           tokens.advance();
           if(!match("("))
               throw new ParseException("Error", tokens.index);
           while (!match(")")){
               indents.add(tokens.get(-1).getLiteral());
               tokens.advance();
               typeNames.add(tokens.get(0).getLiteral());
           }
           tokens.advance();
           String ReturnType = tokens.get(0).getLiteral();
           tokens.advance();
           if(!match("DO"))
               throw new ParseException("DO expected", tokens.index);
           while (!match(";")){
               stmts.add(parseStatement());
           }

           return new Ast.Method(indent1,indents,typeNames,Optional.of(ReturnType),stmts);
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Stmt parseStatement() throws ParseException {
        if(match("LET"))
           return parseDeclarationStatement();
        else if(match("IF"))
            return parseIfStatement();
        else if(match("FOR"))
            return parseForStatement();
        else if(match("WHILE"))
            return parseWhileStatement();
        else if(match("RETURN"))
            return parseReturnStatement();
        else {
            Ast.Expr expr = parseExpression();
//            if(!match(";"))
//                throw new ParseException("Expected ;", tokens.index);

            if(match("=")){
                Ast.Expr secExpr = parseExpression();
                return new Ast.Stmt.Assignment(expr,secExpr);
            }else
                return new Ast.Stmt.Expression(expr);
        }
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Stmt.Declaration parseDeclarationStatement() throws ParseException {
        Ast.Expr expr = null;
            String ident = tokens.get(0).getLiteral();
            tokens.advance();
            if (match(":")){
                String type = tokens.get(0).getLiteral();
                return new Ast.Stmt.Declaration(ident,Optional.of(type),Optional.empty());
            }else if(!match("="))
               return new Ast.Stmt.Declaration(ident,Optional.empty());
            else {
                expr = parseExpression();
                return new Ast.Stmt.Declaration(ident,Optional.of(expr));
            }
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Stmt.If parseIfStatement() throws ParseException {
        List<Ast.Stmt> stmts = new ArrayList<>();
        List<Ast.Stmt> elsestmt = new ArrayList<>();
           Ast.Expr expr = parseExpression();
           if(!match("DO"))
               throw new ParseException("DO expected", tokens.index);
           while (!match("END")){
               stmts.add(parseStatement());
               tokens.advance();
               if(match("ELSE")){
                   elsestmt.add(parseStatement());
                   tokens.advance();
               }
           }

           return new Ast.Stmt.If(expr,stmts,elsestmt);
    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Stmt.For parseForStatement() throws ParseException {
        List<Ast.Stmt> stmts = new ArrayList<>();

            String ident = tokens.get(0).getLiteral();
            tokens.advance();
            tokens.advance();
            //if(!match("IN"))
                //throw new ParseException("IN expected", tokens.index);
            Ast.Expr expr = parseExpression();
            tokens.advance();
            //if (!match("DO"))
                //throw new ParseException("DO expected", tokens.index);
            while (!match(";")){
                stmts.add(parseStatement());
            }
            return new Ast.Stmt.For(ident,expr,stmts);
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Stmt.While parseWhileStatement() throws ParseException {
        List<Ast.Stmt> stmts = new ArrayList<>();
            Ast.Expr expr = parseExpression();
            if(!match("DO"))
                throw new ParseException("DO expected", tokens.index);
            else {
                while (!match(";"))
                    stmts.add(parseStatement());
            }
            return new Ast.Stmt.While(expr,stmts);
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Stmt.Return parseReturnStatement() throws ParseException {
        Ast.Expr expr = parseExpression();

        if(!match(";"))
            throw new ParseException("Semicolon expected ", tokens.index);

        return new Ast.Stmt.Return(expr);

    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expr parseExpression() throws ParseException {
//       //TODO
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expr parseLogicalExpression() throws ParseException {
         //TODO
        Ast.Expr left = parseEqualityExpression();

        while (match("AND")||match("OR")){
            String operator = tokens.get(-1).getLiteral();
            Ast.Expr right = parseEqualityExpression();
            left = new Ast.Expr.Binary(operator,left,right);
        }
        return left;
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expr parseEqualityExpression() throws ParseException {
         //TODO
        Ast.Expr expr = parseAdditiveExpression();

         while (match("<")||match("<=")||match(">")||match(">=")||match( "==")||match( "!=")){
             String operator = tokens.get(-1).getLiteral();
             Ast.Expr right = parseAdditiveExpression();
             expr = new Ast.Expr.Binary(operator,expr,right);
         }

         return expr;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expr parseAdditiveExpression() throws ParseException {
        Ast.Expr secExpr = parseMultiplicativeExpression();

        while (match("+")||match("-")){
            String operator = tokens.get(-1).getLiteral();
            Ast.Expr right = parseMultiplicativeExpression();
            secExpr = new Ast.Expr.Binary(operator,secExpr,right);
        }

        return secExpr;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expr parseMultiplicativeExpression() throws ParseException {
        //TODO
        Ast.Expr secExpr = parseSecondaryExpression();

        while (match("/")||match("*")){
            String operator = tokens.get(-1).getLiteral();
            Ast.Expr right = parseSecondaryExpression();
            secExpr = new Ast.Expr.Binary(operator,secExpr,right);
        }

        return secExpr;
    }

    /**
     * Parses the {@code secondary-expression} rule.
     */
    //If you look at the grammar secondary expression calls primary and so on
    public Ast.Expr parseSecondaryExpression() throws ParseException {
      //TODO
        List<Ast.Expr> exprs = new ArrayList<>();
        Ast.Expr primExpr = parsePrimaryExpression();

        while (match(".")){
            String name = tokens.get(0).getLiteral();
            tokens.advance();
            if(!match("("))
                return new Ast.Expr.Access(Optional.of(primExpr),name);
            else {
                while (!match(")")){
                    exprs.add(parseExpression());
                    if(!peek(")")){
                        if(!match(","))
                            throw new ParseException("Expected commas", tokens.index);
                        else if(!match(")"))
                            throw new ParseException("Trailing comma error", tokens.index);
                    }
                }
                return new Ast.Expr.Function(Optional.of(primExpr),name,exprs);
            }
        }
        return primExpr;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expr parsePrimaryExpression() throws ParseException {
        //Match and peek just take a string or a Token.Type
        //Daniel: First you have to look at the grammar. Here we are parsing primary expression. The grammar says:
        //a primary expression  = TRUE. So,if match TRUE then we create a expresion Literal true as argument.
        if(match("TRUE"))
            return new Ast.Expr.Literal(true);
        else if(match("NIL"))
            return new Ast.Expr.Literal(null);
        else if(match("FALSE"))
            return new Ast.Expr.Literal(false);
        else if(match(Token.Type.INTEGER))
            return new Ast.Expr.Literal(new BigInteger(tokens.get(-1).getLiteral()));
        else if (match(Token.Type.DECIMAL))
            return new Ast.Expr.Literal(new BigDecimal(tokens.get(-1).getLiteral()));
        else if (match(Token.Type.CHARACTER))
            return new Ast.Expr.Literal(new Character( tokens.get(-1).getLiteral().charAt(1)));
        //Here we are paring a string creating another ASt.Expr.Literal
        //We have to check for double quotes and escapes
        else if(match(Token.Type.STRING)){
            //get string
            String st = tokens.get(-1).getLiteral();
            //Go through the string to find scape symbol
            for(int i  = 0; i< st.length(); i++){
                if (st.charAt(i) == '\\'){
                    //If scape symbol create new string with the appropriate scape
                   st = st.replace("\\n","\n");
                   st = st.replace("\\b","\b");
                   st = st.replace("\\r", "\r");
                   st = st.replace("\\t","\t");
                   st = st.replace("\\'","\'");
                   st = st.replace("\\\\","\\");
                }
            }
            // parse string without double quotes
            return new Ast.Expr.Literal(st.substring(1,st.length()-1));
            //Here we are checking for an identifier if It does not have parentheses we create Expr.Access
            //If it does we create a Expr.Function because for example getNum() is a function getNum is Identifier
        } else if(match(Token.Type.IDENTIFIER)){
            String name = tokens.get(-1).getLiteral();
            if (match("(")){
                List<Ast.Expr> args = new ArrayList<>();
                while (!match(")")){
                    args.add(parseExpression());
                    if(!peek(")")){
                        if(!match(","))
                            throw new ParseException("Expected commas", tokens.index);
                        else if(peek(")"))
                            throw new ParseException("Trailing comma error", tokens.index);
                    }
                }
                return new Ast.Expr.Function(Optional.empty(),name,args);
            }else
                return new Ast.Expr.Access(Optional.empty(),name);
        }else if(match("(")) {
            Ast.Expr expr = parseExpression();

            if(!match(")")){
                throw  new ParseException("Expected closing parenthesis.", tokens.index);
            }
            return  new Ast.Expr.Group(expr);
        }
        else{
            throw new ParseException("error", tokens.index);
        }
    }



    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for ( int i = 0; i < patterns.length; i++){
            if (!tokens.has(i))
                return false;
            else if(patterns[i] instanceof  Token.Type){
                if (patterns[i] != tokens.get(i).getType())
                    return false;
            } else if (patterns[i] instanceof String){
                System.out.print("pattern: ");System.out.println(patterns[i]);
                System.out.print("token: ");System.out.println(tokens.get(i).getLiteral());
                if (!patterns[i].equals(tokens.get(i).getLiteral()))
                    return false;
            }else
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);

        if (peek){
            for (int i = 0; i < patterns.length; i++)
                tokens.advance();
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
