package ibb.api.phenotypeservice.queryparser;

public class Token {
    public static enum Type {
        AND, OR, NOT, LPAREN, RPAREN, PHRASE, END
    }
    private Type type;
    private String value;
    public Token(Type type, String value) {
        this.type = type;
        this.value = value;
    }
    public Type getType() {
        return type;
    }
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        if (type == Type.PHRASE) {
            return type.toString() + "(" + value + ")";
        } else {
            return type.toString();
        }
    }
}
