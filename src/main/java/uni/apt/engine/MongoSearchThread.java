package uni.apt.engine;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoIterable;
import org.bson.Document;
import org.tartarus.snowball.ext.PorterStemmer;
import uni.apt.core.OnlineDB;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class MongoSearchThread extends Thread implements RankerSearchThread{
    String _word;

    LinkedList<Ranker.SearchResult> result;
    boolean strict = false;

    PorterStemmer stemmer = new PorterStemmer();
    public MongoSearchThread(String _word) {
        this._word = _word;
    }

    @Override
    public void run() {
        super.run();
        result = new LinkedList<>();


        int indexerWebsites = OnlineDB.MetaDB.find(new Document("obj-id" , "indexer-meta")).first().getInteger("websites");
        int crawlerWebsites = OnlineDB.MetaDB.find(new Document("obj-id" , "crawler-meta")).first().getInteger("websites");
        Document matcher;

        if (!strict) {
            stemmer.setCurrent(_word);
            stemmer.stem();
            matcher = new Document("$or", Arrays.asList(new Document("stemmed",
                            new Document("$regex", Pattern.compile(  stemmer.getCurrent() + "(?i)"))),
                    new Document("word",
                            new Document("$regex", Pattern.compile(_word + "(?i)")))));

        }
        else //if it's a phrase search , then the results need to be exact
            matcher = new Document("word", new Document("$regex", Pattern.compile("^" + _word + "$(?i)")));

        MongoIterable<Document> docs = OnlineDB.IndexerWordsDB.find(matcher);
        for (Document dd : docs){
            MongoIterable<Document> mentions = OnlineDB.IndexerLinksDB.find(new Document("word_id", dd.getLong("word_id")));

            List<Ranker.SearchResult> wordResult = new LinkedList<>();
            int size = 0;
            for (Document doc : mentions){
                List<Document> records = doc.getList("tags" , Document.class);
                Ranker.SearchResult res = new Ranker.SearchResult();
                res.link = doc.getString("link");
                res.originalWord = _word;
                res.word = dd.getString("word");
                res.TF   = (doc.getDouble("TF")).floatValue();
                res.IDF  = indexerWebsites;
                res.popularity = (float) OnlineDB.getPopularity(doc.getString("link")) / crawlerWebsites;
                res.titleIndex = doc.getLong("title");

                res.paragraphIndex = new ArrayList<>(records.size());
                res.wordIndex      = new ArrayList<>(records.size());
                res.type           = new ArrayList<>(records.size());

                for (Document record : records){
                    res.paragraphIndex.add(record.getLong("paragraphIndex"));
                    res.wordIndex     .add(record.getInteger("pos"));
                    res.type          .add(record.getInteger("type"));
                }

                wordResult.add(res);

                size++;
            }

            for (Ranker.SearchResult res : wordResult){
                res.IDF /= size; //calculate the IDF
                result.add(res);
            }


        }
    }

    @Override
    public void setStrict(boolean b) {
        strict = b;
    }

    @Override
    public List<Ranker.SearchResult> getResult() {
        return result;
    }

    @Override
    public void WaitForResult() {
        try {
            this.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
