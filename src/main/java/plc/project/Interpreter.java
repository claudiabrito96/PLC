package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {

        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Field ast) {
        if (ast.getValue().isPresent())
            scope.defineVariable( ast.getName(), visit( ast.getValue().get() ));
        else
            scope.defineVariable( ast.getName(), Environment.NIL);

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Method ast) {

        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Declaration ast) {
        if (ast.getValue().isPresent())
            scope.defineVariable( ast.getName(), visit( ast.getValue().get() ));
        else
            scope.defineVariable( ast.getName(), Environment.NIL);

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Assignment ast) {

        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.If ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.For ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.While ast) {
        while (requireType( Boolean.class, visit( ast.getCondition()))){
            try {
                scope = new Scope(scope);
                for ( Ast.Stmt stmt : ast.getStatements() )
                    visit(stmt);
            }finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Return ast) {
       throw  new Return(Environment.create(ast.getValue()));
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Literal ast) {
        if( Environment.create(ast.getLiteral()).getValue() == null)
            return Environment.NIL;
        else
            return Environment.create(ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Group ast) {
        return visit(ast.getExpression());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Binary ast) {
        //Check if the operands are string or integer
        String sright = null;
        String sleft = null;

        boolean bright = false;
        boolean bleft = false;

        int right = 0;
        int left = 0;

        sright = visit(ast.getRight()).getValue().toString();
        sleft = visit(ast.getLeft()).getValue().toString();
        if(sright.matches("[a-zA-Z_]*"))
            bright = Boolean.parseBoolean(sright);
        else
            right = Integer.parseInt(sright);

        if(sleft.matches("[a-zA-Z_]*"))
            bleft = Boolean.parseBoolean(sleft);
        else
            left = Integer.parseInt(sleft);

        //AND or OR
        if(ast.getOperator().equals("AND"))
            return Environment.create(bleft && bright);
        else if(ast.getOperator().equals("OR"))
            return Environment.create(bleft || bright);
        else if(ast.getOperator().equals("<"))
            return Environment.create(left < right);
        else if(ast.getOperator().equals(">"))
            return Environment.create(left > right);
        else if(ast.getOperator().equals(">="))
            return Environment.create(left >= right);
        else if (ast.getOperator().equals("<="))
            return Environment.create(left <= right);
        else if(ast.getOperator().equals("=="))
            return Environment.create(left == right);
        else if (ast.getOperator().equals("+")){
            if(!sright.matches("[0-9.]*"))
                return Environment.create(sleft+sright);
            else
                return Environment.create(new BigInteger(sleft).add(new BigInteger(sright)));

        }
        else if(ast.getOperator().equals("-"))
            return Environment.create(left-right);
        else if(ast.getOperator().equals("*"))
            return Environment.create(left*right);
        else if(ast.getOperator().equals("/"))
            return Environment.create(new BigDecimal(sleft).divide(new BigDecimal(sright)));
        else
            return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Access ast) {
        if(ast.getReceiver().isPresent())
            return visit(ast.getReceiver().get()).getField(ast.getName()).getValue();
        else
            return scope.lookupVariable(ast.getName()).getValue();
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Function ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    public class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
