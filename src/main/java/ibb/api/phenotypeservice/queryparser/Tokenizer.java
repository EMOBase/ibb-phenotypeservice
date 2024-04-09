package ibb.api.phenotypeservice.queryparser;

public class Tokenizer {

    private static final Token AND = new Token(Token.Type.AND, "AND");
    private static final Token OR = new Token(Token.Type.OR, "OR");
    private static final Token NOT = new Token(Token.Type.NOT, "NOT");
    private static final Token LPAREN = new Token(Token.Type.LPAREN, "(");
    private static final Token RPAREN = new Token(Token.Type.RPAREN, ")");
    private static final Token END = new Token(Token.Type.END, "");
    private static Token PHRASE(String value) {
        return new Token(Token.Type.PHRASE, value);
    }

    private String query;
    private int pos;
    private Token nextToken;

    public Tokenizer(String query) {
        this.query = query;
        this.pos = 0;
        nextToken = findNextToken();
    }

    public Token peek() {
        return nextToken;
    }

    public Token next() {
        Token token = nextToken;
        nextToken = findNextToken();
        return token;
    }

    private String findNextWord() {
        while (pos < query.length() && query.charAt(pos) == ' ') {
            pos++;
        }
        int start = pos;
        while (pos < query.length() && query.charAt(pos) != ' ' && query.charAt(pos) != '(' && query.charAt(pos) != ')') {
            pos++;
        }
        return query.substring(start, pos);
    }

    private Token findNextToken() {
        if (pos >= query.length()) {
            return END;
        } else if (query.charAt(pos) == '(') {
            pos++;
            return LPAREN;
        } else if (query.charAt(pos) == ')') {
            pos++;
            return RPAREN;
        } else {
            String word = findNextWord();
            if (word.equals("AND")) {
                return AND;
            } else if (word.equals("OR")) {
                return OR;
            } else if (word.equals("NOT")) {
                return NOT;
            } else {
                String phrase = word;
                Token nextToken = findNextToken();
                while (nextToken.getType() == Token.Type.PHRASE) {
                    phrase += " " + nextToken.getValue();
                    nextToken = findNextToken();
                }
                pos -= nextToken.getValue().length();
                return PHRASE(phrase);
            }
        }
    }

    public static void main(String[] args) {
        Tokenizer tokenizer = new Tokenizer("(head smaller OR leg) AND NOT wing closed");
        Token token = tokenizer.next();
        while (token.getType() != Token.Type.END) {
            System.out.println(token);
            token = tokenizer.next();
        }
    }
}
