package ibb.api.phenotypeservice.search.queryparser;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.SimpleQueryStringFlag;
import co.elastic.clients.json.jackson.JacksonJsonProvider;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;

/**
 * Parse a query string into an Elasticsearch query to search for phenotype groups.
 * Grammar:
 * 
 * <pre>
 * expr -> term (OR expr)?
 * term -> factor (AND term)?
 * factor -> NOT factor | operand
 * operand -> PHRASE | LPAREN expr RPAREN
 * </pre>
 */
public class QueryParser {
    private Tokenizer tokenizer;
    private boolean randomizeInnerHitKeys = true;

    public Query parse(String query) throws QueryParserException {
        tokenizer = new Tokenizer(query);
        return expr();
    }

    private Query expr() throws QueryParserException {
        List<Query> queries = new ArrayList<>();
        queries.add(term());
        Token token = tokenizer.peek();
        if (token.getType() == Token.Type.OR) {
            tokenizer.next();
            queries.add(expr());
        }
        if (queries.size() == 1) {
            return queries.get(0);
        } else {
            return Query.of(q -> q.bool(b -> b.should(queries)));
        }
    }

    private Query term() throws QueryParserException {
        List<Query> queries = new ArrayList<>();
        queries.add(factor());
        Token token = tokenizer.peek();
        if (token.getType() == Token.Type.AND) {
            tokenizer.next();
            queries.add(term());
        }
        if (queries.size() == 1) {
            return queries.get(0);
        } else {
            return Query.of(q -> q.bool(b -> b.must(queries)));
        }
    }

    private Query factor() throws QueryParserException {
        Token token = tokenizer.peek();
        if (token.getType() == Token.Type.NOT) {
            tokenizer.next();
            Query query = factor();
            return Query.of(q -> q.bool(b -> b.mustNot(query)));
        } else {
            return operand();
        }
    }

    private Query operand() throws QueryParserException {
        Token token = tokenizer.next();
        if (token.getType() == Token.Type.LPAREN) {
            Query query = expr();
            token = tokenizer.next();
            if (token.getType() != Token.Type.RPAREN) {
                throw new QueryParserException("Expected RPAREN but got: " + token);
            }
            return query;
        } else if (token.getType() == Token.Type.PHRASE) {
            String phrase = token.getValue();
            String innerHitKey = randomizeInnerHitKeys ? UUID.randomUUID().toString() : phrase;
            return Query.of(q -> q
                .nested(n -> n
                    .path("phenotypes")
                    .query(q2 -> q2
                        .simpleQueryString(q3 -> q3
                            .query(phrase)
                            .defaultOperator(Operator.And)
                            .flags(SimpleQueryStringFlag.And, SimpleQueryStringFlag.Not, SimpleQueryStringFlag.Phrase)
                            .fields("phenotypes.description")
                        )
                    )
                    .innerHits(h -> h.name(innerHitKey).size(10))
                )
            );
        } else {
            throw new QueryParserException("Unexpected token: " + token);
        }
    }

    public static void main(String[] args) throws Exception {
        // String queryString = "(head -smaller OR leg) AND NOT wing closed";
        String queryString = "((head -smaller OR leg)) AND NOT wing closed";
        QueryParser parser = new QueryParser();
        Query query = parser.parse(queryString);
        StringWriter writer = new StringWriter();
        var generator = JacksonJsonProvider.provider().createGenerator(writer);
        query.serialize(generator, new JacksonJsonpMapper());
        generator.close();
        System.out.println(writer.toString());
    }
}
