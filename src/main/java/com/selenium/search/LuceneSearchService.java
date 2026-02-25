package com.selenium.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;

import java.util.ArrayList;
import java.util.List;

public class LuceneSearchService {

    private final Directory directory;
    private final Analyzer analyzer;

    public LuceneSearchService(Directory directory, Analyzer analyzer) {
        this.directory = directory;
        this.analyzer = analyzer;
    }

    public List<Long> searchEventIds(String keyword) throws Exception {

        List<Long> results = new ArrayList<>();

        String[] fields = {"title", "category"};
        Query query = new MultiFieldQueryParser(fields, analyzer).parse(keyword);

        try (DirectoryReader reader = DirectoryReader.open(directory)) {

            IndexSearcher searcher = new IndexSearcher(reader);
            TopDocs docs = searcher.search(query, 10);

            for (ScoreDoc scoreDoc : docs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                results.add(Long.parseLong(doc.get("id")));
            }
        }

        return results;
    }
}