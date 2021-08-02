package plc.project;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
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
        //++indent;
        print("public class Main {");
        newline(indent);
        newline(++indent);
        for (Ast.Field field:ast.getFields()) {
            visit(field);
        }
        print("public static void main(String[] args) {");
        newline(++indent);
        print("System.exit(new Main().main());");
        newline(--indent);
        print("}");
        newline(0);
        newline(indent);
        for (Ast.Method method:ast.getMethods()) {
            visit(method);
        }
        newline(0);
        newline(0);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        switch (ast.getTypeName()) {
            case "Integer":
                print("int");
                break;
            case "Decimal":
                print("double");
                break;
            case "Boolean":
                print("boolean");
                break;
            case "Character":
                print("char");
                break;
            case "String":
                print("String");
                break;
        }

        print(" " + ast.getName());

        if (ast.getValue().isPresent())
            print(" = " + ast.getValue().get());

        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        print(ast.getFunction().getReturnType().getJvmName());
        print(" ");
        print(ast.getFunction().getName());
        print("(");

        for (int i = 0; i < ast.getParameters().size(); i++) {
            print(ast.getParameterTypeNames().get(i));
            print(" ");
            print(ast.getParameters().get(i));
            if (i != ast.getParameters().size() - 1)
                print(", ");
        }

        print(") " + "{");

        if (!ast.getStatements().isEmpty()) {
            ++indent;
            for (int i = 0; i < ast.getStatements().size(); i++) {
                newline(indent);
                print(ast.getStatements().get(i));
            }
            newline(--indent);
        }
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        print(ast.getExpression());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {
        print(ast.getVariable().getType().getJvmName(), " ", ast.getVariable().getJvmName());
        if (ast.getValue().isPresent())
            print(" = ", ast.getValue().get());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        print(ast.getReceiver());
        print(" = ");
        print(ast.getValue() + ";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        print("if" + " (");
        visit(ast.getCondition());
        print(") {");
        newline(++indent);
        for (Ast.Stmt stmt: ast.getThenStatements()) {
            visit(stmt);
        }
        newline(--indent);
        print("}");
        if(!ast.getElseStatements().isEmpty()){
            print(" else {");
            newline(++indent);
            for (Ast.Stmt elseStmt:ast.getElseStatements()) {
                visit(elseStmt);
            }
            newline(--indent);
            print("}");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
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
        print("return ");
        print(ast.getValue());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        if (ast.getType() == Environment.Type.DECIMAL)
            print(((BigDecimal) ast.getLiteral()).doubleValue());
        else if (ast.getType() == Environment.Type.INTEGER)
            print(((BigInteger) ast.getLiteral()).intValue());
        else if (ast.getType() == Environment.Type.CHARACTER)
            print("'" + ast.getLiteral() + "'");
        else if (ast.getType() == Environment.Type.STRING)
            print("\"" + ast.getLiteral() + "\"");
        else
            print(ast.getLiteral());

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        print("(" + ast.getExpression());
        print(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        visit(ast.getLeft());
        print(" ");
        if (ast.getOperator().equals("OR"))
            print("||");
        else if (ast.getOperator().equals("AND"))
            print("&&");
        else
            print(ast.getOperator());

        print(" ");
        visit(ast.getRight());

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        if(ast.getReceiver().isPresent()){
            print(ast.getReceiver().get());
            print(".");
        }
        print(ast.getVariable().getJvmName());
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        if (ast.getReceiver().isPresent()) {
            Ast.Expr temp = ast.getReceiver().get();
            print(temp);
            print(".");
        }

        print(ast.getFunction().getJvmName() + "(");

        if (!ast.getArguments().isEmpty()) {
            for (int i = 0; i < ast.getArguments().size(); i++) {
                print(ast.getArguments().get(i));
                if (i != ast.getArguments().size() - 1)
                    print(", ");
            }
        }

        print(")");
        return null;

    }

}
