package ibb.api.phenotypeservice.queryparser;

import static ibb.api.phenotypeservice.search.queryparser.Tokenizer.AND;
import static ibb.api.phenotypeservice.search.queryparser.Tokenizer.LPAREN;
import static ibb.api.phenotypeservice.search.queryparser.Tokenizer.NOT;
import static ibb.api.phenotypeservice.search.queryparser.Tokenizer.OR;
import static ibb.api.phenotypeservice.search.queryparser.Tokenizer.PHRASE;
import static ibb.api.phenotypeservice.search.queryparser.Tokenizer.RPAREN;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ibb.api.phenotypeservice.search.queryparser.Token;
import ibb.api.phenotypeservice.search.queryparser.Tokenizer;

public class TokenizerTest {

    private static Stream<Arguments> testTokenizerProvider() {
        return Stream.of(
            Arguments.of(
                "head -smaller OR (leg AND NOT wing closed)",
                List.of(
                    PHRASE("head -smaller"),
                    OR,
                    LPAREN,
                    PHRASE("leg"),
                    AND,
                    NOT,
                    PHRASE("wing closed"),
                    RPAREN
                )
            ),
            Arguments.of(
                "   ( head -smaller         OR leg ) AND NOT wing closed       ",
                List.of(
                    LPAREN,
                    PHRASE("head -smaller"),
                    OR,
                    PHRASE("leg"),
                    RPAREN,
                    AND,
                    NOT,
                    PHRASE("wing closed")
                )
            )
            // Arguments.of(
            //     "((head | leg) -smaller) AND (wing (closed | open))",
            //     List.of(
            //         LPAREN,
            //         PHRASE("(head | leg) -smaller"),
            //         RPAREN,
            //         AND,
            //         LPAREN,
            //         PHRASE("wing closed"),
            //         OR,
            //         PHRASE("wing open"),
            //         RPAREN
            //     )
            // )
        );
    }
    
    @ParameterizedTest
    @MethodSource("testTokenizerProvider")
    public void testTokenizer(String query, List<Token> expected) {
        assertEquals(expected, tokenize(query));
    }

    private List<Token> tokenize(String query) {
        Tokenizer tokenizer = new Tokenizer(query);
        List<Token> tokens = new ArrayList<>();
        Token token = tokenizer.next();
        while (token.getType() != Token.Type.END) {
            tokens.add(token);
            token = tokenizer.next();
        }
        return tokens;
    }
}
