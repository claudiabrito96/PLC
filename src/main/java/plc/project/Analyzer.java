package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Method method;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Field ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Method ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        visit(ast.getExpression());
        try {
            if (ast.getExpression().getClass() != Ast.Expr.Function.class)
                throw new RuntimeException("Error: Function Type Missing.");
        }
        catch (RuntimeException re) {
            throw new RuntimeException(re);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {
        if(!ast.getTypeName().isPresent() && !ast.getValue().isPresent()){
            throw new RuntimeException("Declaration must have type or value to infer type.");
        }

        Environment.Type type = null;

        if(ast.getTypeName().isPresent()){
            type = Environment.getType(ast.getTypeName().get());
        }

        if (ast.getValue().isPresent()){

            visit(ast.getValue().get());

            if(type == null){
                type = ast.getValue().get().getType();
            }

            requireAssignable(type,ast.getValue().get().getType() );
        }

        ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), type, Environment.NIL));
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        try {
            if (ast.getReceiver().getClass() != Ast.Expr.Access.class)
                throw new RuntimeException("Error!");

            visit(ast.getValue());
            visit(ast.getReceiver());
            requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());
        }
        catch (RuntimeException re) {
            throw new RuntimeException(re);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        //throw new UnsupportedOperationException();
        visit(ast.getCondition());
        requireAssignable(ast.getCondition().getType(), Environment.Type.BOOLEAN);

        if(ast.getThenStatements().isEmpty())
            throw new RuntimeException("The then statement is empty");

        try {
            scope = new Scope(scope);
            for (Ast.Stmt stmt: ast.getThenStatements())
                visit(stmt);
        }finally {
            scope = scope.getParent();
        }

        try {
            scope = new Scope(scope);
            for (Ast.Stmt stmt: ast.getElseStatements())
                visit(stmt);
        }finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        try {
            visit(ast.getCondition());
            requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
            try {
                scope = new Scope(scope);
                for (Ast.Stmt statement : ast.getStatements())
                    visit(statement);
            }
            finally {
                scope = scope.getParent();
            }
        }
        catch (RuntimeException re) {
            throw new RuntimeException(re);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        visit(ast.getValue());
        try {
            requireAssignable(scope.lookupVariable("returnType").getType(), ast.getValue().getType());
        }
        catch (RuntimeException re) {
            throw new RuntimeException(re);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        try {
            if (ast.getLiteral() instanceof BigDecimal) {
                try {
                    if ((((BigDecimal) ast.getLiteral()).doubleValue() > Double.MAX_VALUE) ||
                            (((BigDecimal) ast.getLiteral()).doubleValue() < Double.MIN_VALUE))
                        throw new RuntimeException("Error");
                    ast.setType(Environment.Type.DECIMAL);
                }
                catch (RuntimeException re) {
                    throw new RuntimeException(re);
                }
            }
            else if (ast.getLiteral() instanceof BigInteger) {
                try {
                    if ((((BigInteger) ast.getLiteral()).intValueExact() > Integer.MAX_VALUE) ||
                            (((BigInteger) ast.getLiteral()).intValueExact() < Integer.MIN_VALUE))
                        throw new RuntimeException("Error");
                    ast.setType(Environment.Type.INTEGER);
                }
                catch (RuntimeException re) {
                    throw new RuntimeException(re);
                }
            }
            else if (ast.getLiteral() instanceof String)
                ast.setType(Environment.Type.STRING);
            else if (ast.getLiteral() instanceof Character)
                ast.setType(Environment.Type.CHARACTER);
            else if (ast.getLiteral() == Environment.NIL)
                ast.setType(Environment.Type.NIL);
            else if (ast.getLiteral() instanceof Boolean)
                ast.setType(Environment.Type.BOOLEAN);
        }
        catch (RuntimeException re) {
            throw new RuntimeException(re);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        try {
            visit(ast.getExpression());
            try {
                if (ast.getExpression().getClass() != Ast.Expr.Binary.class)
                    throw new RuntimeException("Error: Type is not binary.");
            }
            catch (RuntimeException re) {
                throw new RuntimeException(re);
            }
        }
        catch (RuntimeException re) {
            throw new RuntimeException(re);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        try {
            if (target != type && target != Environment.Type.ANY && target != Environment.Type.COMPARABLE)
                throw new RuntimeException("Error: Types Do Not Match.");
        }
        catch (RuntimeException re) {
            throw new RuntimeException(re);
        }
    }

}
