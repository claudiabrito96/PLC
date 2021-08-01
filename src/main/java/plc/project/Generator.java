package plc.project;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        //throw new UnsupportedOperationException(); //TODO
        //create a "class Main {"
        //declare fields
        //declare "public static void main(String[] args) {
        //                    System.exit(new Main().main());
        //}"
        //declare each of our methods
        //one of our methods is called main()!

        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
       // throw new UnsupportedOperationException(); //TODO
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
       // throw new UnsupportedOperationException();
        print(ast.getFunction().getReturnType().getJvmName() + " " + ast.getFunction().getName() + "(");

       // generate(IntStream.range(0, ast.getParameters().size()).mapToObj(i -> ast.getFunction().getParameterTypes().get(i).getJvmName() + " " + ast.getParameters().get(i)).collect(Collectors)
        print(") " + "{");

        if(ast.getStatements().isEmpty())
            print("}");
        else {
            newline(++indent);
            for (int i = 0; i <ast.getStatements().size(); i++) {
                print(ast.getStatements().get(i));
                if(i+1 != ast.getStatements().size())
                    newline(indent);
            }
            newline(--indent);
            print("}");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        //throw new UnsupportedOperationException();
        print(ast.getExpression() + ";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {
        //throw new UnsupportedOperationException();
        print(ast.getVariable().getType().getJvmName(), " ", ast.getVariable().getJvmName());
        if (ast.getValue().isPresent())
            print(" = ", ast.getValue().get());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        //throw new UnsupportedOperationException();
        print(ast.getReceiver() + " = " + ast.getValue() + ";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        //throw new UnsupportedOperationException(); //TODO
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        //throw new UnsupportedOperationException();
        print("for (" + ast.getValue().getType()+ " " + ast.getName() + " : " + ast.getValue() + "{");
        newline(++indent);
        for (int i = 0; i <ast.getStatements().size(); i++){
            print(ast.getStatements().get(i));
            if(i+1 != ast.getStatements().size())
                newline(indent);
        }
        newline(--indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        //throw new UnsupportedOperationException();
        print("while (", ast.getCondition(), ") {");

        if(!ast.getStatements().isEmpty()) {
            newline(++indent);
            for (int i = 0; i <ast.getStatements().size(); i++){
                if( i != 0){
                    newline(indent);
                }
                print(ast.getStatements().get(i));
            }
            newline(--indent);
        }
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        print("return " + ast.getValue() + ";");
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        //throw new UnsupportedOperationException(); //TODO
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        print("(" + ast.getExpression() + ")");
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        //throw new UnsupportedOperationException(); //TODO
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        //throw new UnsupportedOperationException();
        if(ast.getReceiver().isPresent())
            print(ast.getReceiver().get() + ".");
        print(ast.getVariable().getJvmName());
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        //throw new UnsupportedOperationException();
        if(ast.getReceiver().isPresent())
            print(ast.getReceiver().get() + ".");
        print(ast.getFunction().getJvmName() + "(");

        //generate(ast.getArguments());
        print(")");
        return null;
    }

}
