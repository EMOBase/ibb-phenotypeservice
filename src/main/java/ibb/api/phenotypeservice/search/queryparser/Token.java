package ibb.api.phenotypeservice.search.queryparser;

import java.util.Objects;

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

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (this == obj) {
            return true;
        } else if (obj instanceof Token) {
            Token other = (Token) obj;
            return type == other.type && value.equals(other.value);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value);
    }
}
