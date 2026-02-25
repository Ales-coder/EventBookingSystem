package com.selenium.search;

import com.selenium.model.Event;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;

import java.io.IOException;
import java.util.List;

public class LuceneEventIndexer {

    private final Directory directory = new ByteBuffersDirectory();
    private final Analyzer analyzer = LuceneAnalyzers.build();

    public void indexEvents(List<Event> events) throws IOException {

        IndexWriterConfig config = new IndexWriterConfig(analyzer);


        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        try (IndexWriter writer = new IndexWriter(directory, config)) {

            for (Event event : events) {

                Document doc = new Document();

                doc.add(new StringField("id", String.valueOf(event.getEventId()), Field.Store.YES));


                doc.add(new TextField("title", safe(event.getTitle()), Field.Store.YES));
                doc.add(new TextField("category", safe(event.getCategory()), Field.Store.YES));

                writer.addDocument(doc);
            }
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    public Directory getDirectory() {
        return directory;
    }

    public Analyzer getAnalyzer() {
        return analyzer;
    }
}