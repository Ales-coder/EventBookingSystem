package com.selenium.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.synonym.SolrSynonymParser;
import org.apache.lucene.analysis.synonym.SynonymGraphFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class LuceneAnalyzers {

    private LuceneAnalyzers() {}

    private static SynonymMap loadSynonyms() {
        try {

            SolrSynonymParser parser = new SolrSynonymParser(true, true, new WhitespaceAnalyzer());

            try (var in = LuceneAnalyzers.class.getResourceAsStream("/synonyms.txt")) {
                if (in == null) throw new RuntimeException("Missing resource: /resources/synonyms.txt");
                parser.parse(new InputStreamReader(in, StandardCharsets.UTF_8));
            }

            return parser.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load synonyms", e);
        }
    }

    public static Analyzer build() {
        SynonymMap synonymMap = loadSynonyms();

        return new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                Tokenizer source = new StandardTokenizer();
                TokenStream ts = source;

                ts = new LowerCaseFilter(ts);


                ts = new SynonymGraphFilter(ts, synonymMap, true);


                ts = new PorterStemFilter(ts);

                return new TokenStreamComponents(source, ts);
            }
        };
    }
}