package uni.apt.engine;

import com.mongodb.client.MongoIterable;
import org.bson.Document;
import org.tartarus.snowball.ext.PorterStemmer;
import uni.apt.core.OnlineDB;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class MongoSearchThread extends Thread implements RankerSearchThread{
    String _word;

    LinkedList<Ranker.SearchResult> result;
    boolean strict = false;

    PorterStemmer stemmer;
    public MongoSearchThread(String _word) {
        this._word = _word;
        stemmer = new PorterStemmer();
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
            matcher = new Document("word", new Document("$regex", Pattern.compile("" + stemmer.getCurrent() + "(?i)")));
        }
        else
            matcher = new Document("word", new Document("$regex", Pattern.compile("^" + _word + "$(?i)")));

        MongoIterable<Document> docs = OnlineDB.IndexerDB.find(matcher);
        for (Document dd : docs){
            List<Document> mentions = dd.getList("mentions" , Document.class);
            float IDF = ((float) indexerWebsites / mentions.size());

            for (Document doc : mentions){
                List<Document> records = doc.getList("records" , Document.class);
                Ranker.SearchResult res = new Ranker.SearchResult();
                res.link = doc.getString("link");
                res.originalWord = _word;
                res.word = dd.getString("word");
                res.TF   = (doc.getDouble("TF")).floatValue();
                res.IDF  = IDF;
                res.popularity = (float) OnlineDB.getPopularity(doc.getString("link")) / crawlerWebsites;
                res.titleIndex = doc.getLong("title");

                res.paragraphIndex = new ArrayList<>(records.size());
                res.wordIndex      = new ArrayList<>(records.size());
                res.tag            = new ArrayList<>(records.size());
                res.type           = new ArrayList<>(records.size());

                int i = 0;
                for (Document record : records){
                    res.paragraphIndex.add(record.getLong("paragraphIndex"));
                    res.wordIndex     .add(record.getInteger("pos"));
                    res.tag           .add(record.getInteger("tagIndex"));
                    res.type          .add(record.getInteger("type"));

                    i++;
                }

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
