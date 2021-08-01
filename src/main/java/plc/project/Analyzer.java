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
        try {
            boolean main = false;

            if (!ast.getFields().isEmpty()) {
                for (int i = 0; i < ast.getFields().size(); i++)
                    visit(ast.getFields().get(i));
            }
            if (!ast.getMethods().isEmpty()) {
                for (int i = 0; i < ast.getMethods().size(); i++) {
                    visit(ast.getMethods().get(i));
                    Ast.Method temp = ast.getMethods().get(i);
                    if (temp.getName().equals("main") && temp.getReturnTypeName().get().equals("Integer") && temp.getParameters().isEmpty()) {
                        main = true;
                    }
                }
            }

            if (!main) {
                throw new RuntimeException("Error: No main method!");
            }
        }
        catch (RuntimeException re) {
            throw new RuntimeException(re);
        }


        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        try {
            if (ast.getValue().isPresent()) {
                visit(ast.getValue().get());
                requireAssignable(Environment.getType(ast.getTypeName()), ast.getValue().get().getType());
                scope.defineVariable(ast.getName(), ast.getName(), ast.getValue().get().getType(), Environment.NIL);
            }
            else {
                scope.defineVariable(ast.getName(), ast.getName(), Environment.getType(ast.getTypeName()), Environment.NIL);
            }
            ast.setVariable(scope.lookupVariable(ast.getName()));
        }
        catch (RuntimeException re) {
            throw new RuntimeException(re);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        Environment.Type r_type;
        List<String> parameters = ast.getParameterTypeNames();
        Environment.Type[] _parameters = new Environment.Type[parameters.size()];

        try {

            if (!ast.getStatements().isEmpty()) {
                for (int i = 0; i < ast.getStatements().size(); i++) {
                    try {
                        scope = new Scope(scope);
                        visit(ast.getStatements().get(i));
                    }
                    finally {
                        scope = scope.getParent();
                    }
                }
            }

            if (!ast.getReturnTypeName().isPresent())
                r_type = Environment.Type.NIL;
            else
                r_type = Environment.getType(ast.getReturnTypeName().get());

            if (!parameters.isEmpty()) {
                for (int i = 0; i < parameters.size(); i++)
                    _parameters[i] = Environment.getType(parameters.get(i));
            }

            scope.defineVariable("r_type", "r_type", r_type, Environment.NIL);
            scope.defineFunction(ast.getName(), ast.getName(), Arrays.asList(_parameters), r_type, args -> Environment.NIL);
            ast.setFunction(scope.lookupFunction(ast.getName(), ast.getParameters().size()));

        }
        catch (RuntimeException re) {
            throw new RuntimeException(re);
        }

        return null;
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
        try {
            if (ast.getStatements().isEmpty())
                throw new RuntimeException("Error");

            visit(ast.getValue());
            requireAssignable(Environment.Type.INTEGER_ITERABLE, ast.getValue().getType());

            ast.getStatements().forEach(stmt ->
            {
                try {
                    scope = new Scope(scope);
                    scope.defineVariable(ast.getName(), ast.getName(), Environment.Type.INTEGER, Environment.NIL);
                }
                finally {
                    scope = scope.getParent();
                }
            }
            );
        }
        catch (RuntimeException re) {
            throw new RuntimeException(re);
        }

        return null;
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
        //throw new UnsupportedOperationException();
        visit(ast.getLeft());
        visit(ast.getRight());
        switch (ast.getOperator()) {
            case "AND": case "OR":
                requireAssignable(Environment.Type.BOOLEAN,ast.getRight().getType());
                requireAssignable(Environment.Type.BOOLEAN,ast.getLeft().getType());
                ast.setType(Environment.Type.BOOLEAN);
                break;
            case "<": case "<=": case ">": case ">=": case "==": case "!=":
                requireAssignable(Environment.Type.COMPARABLE,ast.getLeft().getType());
                requireAssignable(ast.getLeft().getType(),ast.getRight().getType());
                ast.setType(Environment.Type.BOOLEAN);
                break;
            case "+":
                if(ast.getLeft().getType().equals(Environment.Type.STRING)||ast.getRight().getType().equals(Environment.Type.STRING)) {
                    ast.setType(Environment.Type.STRING);
                    break;
                }
            case "-": case "*": case "/":
                if(ast.getLeft().getType().equals(Environment.Type.INTEGER)){
                    requireAssignable(Environment.Type.INTEGER,ast.getRight().getType());
                    ast.setType(Environment.Type.INTEGER);
                }
                else if(ast.getLeft().getType().equals(Environment.Type.DECIMAL)){
                    requireAssignable(Environment.Type.DECIMAL,ast.getRight().getType());
                    ast.setType(Environment.Type.DECIMAL);
                }else
                    throw new RuntimeException("invalid type");
                break;
            default:
                throw new AssertionError(ast.getOperator());
        }


        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        try {
            if (ast.getReceiver().isPresent()) {
                Ast.Expr.Access temp = (Ast.Expr.Access) ast.getReceiver().get();
                temp.setVariable(scope.lookupVariable(temp.getName()));
                try {
                    scope = scope.lookupVariable(temp.getName()).getType().getScope();
                    ast.setVariable(scope.lookupVariable(ast.getName()));
                }
                finally {
                    scope = scope.getParent();
                }
            }
            else {
                ast.setVariable(scope.lookupVariable(ast.getName()));
            }
        }
        catch (RuntimeException re) {
            throw new RuntimeException(re);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        List<Environment.Type> f_parameters;

        try {
            if (ast.getReceiver().isPresent()) {
                visit(ast.getReceiver().get());

                f_parameters = scope.lookupVariable(((Ast.Expr.Access) ast.getReceiver().get()).getName()).getType().getMethod(ast.getName(),
                        ast.getArguments().size()).getParameterTypes();

                for (int i = 0; i < ast.getArguments().size(); i++) {
                    visit(ast.getArguments().get(i));
                    requireAssignable(f_parameters.get(i + 1), ast.getArguments().get(i).getType());
                }

                ast.setFunction(scope.lookupVariable(((Ast.Expr.Access) ast.getReceiver().get()).getName()).getType().getMethod(ast.getName(),
                        ast.getArguments().size()));

            }
            else {
                f_parameters = scope.lookupFunction(ast.getName(),
                        ast.getArguments().size()).getParameterTypes();

                for (int i = 0; i < ast.getArguments().size(); i++) {
                    visit(ast.getArguments().get(i));
                    requireAssignable(f_parameters.get(i), ast.getArguments().get(i).getType());
                }

                ast.setFunction(scope.lookupFunction(ast.getName(), ast.getArguments().size()));
            }
        }
        catch (RuntimeException re) {
            throw new RuntimeException(re);
        }

        return null;
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
