package plc.project;

import jdk.nashorn.internal.runtime.Undefined;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
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

        scope.defineFunction("logarithm",1,arg_list -> {
            if ( !(arg_list.get(0).getValue() instanceof  BigDecimal)){
                throw new RuntimeException("Expected type BigDecimal"+
                        arg_list.get(0).getValue().getClass().getName() + ".");
            }
            BigDecimal bd1 = (BigDecimal) arg_list.get(0).getValue();
            BigDecimal bd2 = requireType(
                    BigDecimal.class,
                    Environment.create(arg_list.get(0).getValue())
            );
            BigDecimal result = BigDecimal.valueOf(Math.log(bd2.doubleValue()));
            return Environment.create(result);
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        ast.getFields().forEach(this::visit);
        ast.getMethods().forEach(this::visit);
        List <Environment.PlcObject> args = new ArrayList<>();
        return scope.lookupFunction("main",0).invoke(new ArrayList<>());
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
        Scope parent = scope;
        scope.defineFunction(ast.getName(), ast.getParameters().size(),args -> {
            Scope child = scope;

                scope = new Scope(parent);
                for (int i = 0; i < args.size(); i++) {
                    scope.defineVariable(ast.getParameters().get(i), args.get(i));
                }try {
                    ast.getStatements().forEach(this :: visit);
                    return  Environment.NIL;
                }catch (Return exception) {
                    System.out.println(exception);
                    return exception.value;
                }
                finally {
                scope = child;
            }
        });
        return Environment.NIL;
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
        if(ast.getReceiver() == null)
            throw new RuntimeException("Assignment Target Error!");
        else if (!(ast.getReceiver() instanceof Ast.Expr.Access))
            throw new RuntimeException("Assignment Target Error!");

        Ast.Expr.Access target = (Ast.Expr.Access)ast.getReceiver();
        String name = target.getName();
        Environment.PlcObject value = visit(ast.getValue());
        if(target.getReceiver().isPresent()) {
            Environment.PlcObject receiver = visit(target.getReceiver().get());
            receiver.setField(name, value);
        }
        else
            scope.lookupVariable(name).setValue(value);

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.If ast) {
        if(requireType(Boolean.class, visit(ast.getCondition()))) {
            scope = new Scope(scope);
            for(Ast.Stmt statement: ast.getThenStatements())
                visit(statement);
        }
        else {
            for(Ast.Stmt else_statement: ast.getElseStatements())
                visit(else_statement);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.For ast) {
        Iterator iter = requireType(Iterable.class, visit(ast.getValue())).iterator();
        for (Object ob : requireType(Iterable.class, visit(ast.getValue()))){
            try {
                scope = new Scope(scope);
                scope.defineVariable(ast.getName(), (Environment.PlcObject) ob);
                for(Ast.Stmt statement: ast.getStatements())
                    visit(statement);
            }
            finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.While ast) {
        while (requireType(Boolean.class, visit( ast.getCondition()))){
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

        if(ast.getOperator().equals("AND"))
            return Environment.create(requireType(Boolean.class,visit(ast.getLeft())) && requireType(Boolean.class,visit(ast.getRight())));
        else if(ast.getOperator().equals("OR"))
            return Environment.create(requireType(Boolean.class,visit(ast.getLeft())) || requireType(Boolean.class,visit(ast.getRight())));
        else if(ast.getOperator().equals("<")){
            if(requireType(Comparable.class,visit(ast.getLeft())).compareTo(requireType(Comparable.class,visit(ast.getRight()))) < 0)
                return Environment.create(true);
            else
                return Environment.create(false);
        }
        else if(ast.getOperator().equals(">")){
            if(requireType(Comparable.class,visit(ast.getLeft())).compareTo(requireType(Comparable.class,visit(ast.getRight()))) > 0)
                return Environment.create(true);
            else
                return Environment.create(false);
        } else if(ast.getOperator().equals(">=")){
            if(requireType(Comparable.class,visit(ast.getLeft())).compareTo(requireType(Comparable.class,visit(ast.getRight()))) >= 0)
                return Environment.create(true);
            else
                return Environment.create(false);
        } else if (ast.getOperator().equals("<=")){
            if(requireType(Comparable.class,visit(ast.getLeft())).compareTo(requireType(Comparable.class,visit(ast.getRight()))) <= 0)
                return Environment.create(true);
            else
                return Environment.create(false);
        }
        else if(ast.getOperator().equals("==")){
            if(requireType(Comparable.class,visit(ast.getLeft())).compareTo(requireType(Comparable.class,visit(ast.getRight()))) == 0)
                return Environment.create(true);
            else
                return Environment.create(false);
        }
        else if(ast.getOperator().equals("!=")){
            if(requireType(Comparable.class,visit(ast.getLeft())).compareTo(requireType(Comparable.class,visit(ast.getRight()))) != 0)
                return Environment.create(true);
            else
                return Environment.create(false);
        }
        else if (ast.getOperator().equals("+")){
            Environment.PlcObject left = visit(ast.getLeft());
            Environment.PlcObject right = visit(ast.getRight());
            if(left.getValue() instanceof String)
                return Environment.create(requireType(String.class,visit(ast.getLeft())).concat(requireType(String.class,visit(ast.getRight()))));
            else if(left.getValue() instanceof BigInteger){
                if (right.getValue() instanceof BigInteger)
                    return Environment.create(requireType(BigInteger.class,visit(ast.getLeft())).add(requireType(BigInteger.class,visit(ast.getRight()))));
                else
                    throw new RuntimeException();
            } else{
                if(right.getValue() instanceof BigDecimal)
                    return Environment.create(requireType(BigDecimal.class,visit(ast.getLeft())).add(requireType(BigDecimal.class,visit(ast.getRight()))));
                else
                    throw new RuntimeException();
            }

        }
        else if(ast.getOperator().equals("-")){
            Environment.PlcObject left = visit(ast.getLeft());
            Environment.PlcObject right = visit(ast.getRight());

            if(left.getValue() instanceof BigInteger){
                if (right.getValue() instanceof BigInteger)
                    return Environment.create(requireType(BigInteger.class,visit(ast.getLeft())).add(requireType(BigInteger.class,visit(ast.getRight()))));
                else
                    throw new RuntimeException();
            } else{
                if(right.getValue() instanceof BigDecimal)
                    return Environment.create(requireType(BigDecimal.class,visit(ast.getLeft())).add(requireType(BigDecimal.class,visit(ast.getRight()))));
                else
                    throw new RuntimeException();
            }
        }
        else if(ast.getOperator().equals("*")){
            Environment.PlcObject left = visit(ast.getLeft());
            Environment.PlcObject right = visit(ast.getRight());

            if(left.getValue() instanceof BigInteger){
                if (right.getValue() instanceof BigInteger)
                    return Environment.create(requireType(BigInteger.class,visit(ast.getLeft())).add(requireType(BigInteger.class,visit(ast.getRight()))));
                else
                    throw new RuntimeException();
            } else{
                if(right.getValue() instanceof BigDecimal)
                    return Environment.create(requireType(BigDecimal.class,visit(ast.getLeft())).add(requireType(BigDecimal.class,visit(ast.getRight()))));
                else
                    throw new RuntimeException();
            }

        }
        else if(ast.getOperator().equals("/")){
            Environment.PlcObject left = visit(ast.getLeft());
            Environment.PlcObject right = visit(ast.getRight());

            if(left.getValue() instanceof BigInteger){
                if (right.getValue() instanceof BigInteger)
                    return Environment.create(requireType(BigInteger.class,visit(ast.getLeft())).add(requireType(BigInteger.class,visit(ast.getRight()))));
                else
                    throw new RuntimeException();
            } else{
                if(right.getValue() instanceof BigDecimal){
                    if(left.getValue().equals(0))
                        throw new RuntimeException();
                    else
                        return Environment.create(requireType(BigDecimal.class,visit(ast.getLeft())).divide(requireType(BigDecimal.class,visit(ast.getRight())),1,RoundingMode.HALF_EVEN));
                } else
                    throw new RuntimeException();
            }
        } else
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

        List<Environment.PlcObject> args = new ArrayList<>();

        for(Ast.Expr argument : ast.getArguments())
            args.add(visit(argument));

        Environment.PlcObject product;

        if(ast.getReceiver().isPresent())
            product = visit(ast.getReceiver().get()).callMethod(ast.getName(), args);
        else
            product = scope.lookupFunction(ast.getName(), ast.getArguments().size()).invoke(args);

        return product;
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
