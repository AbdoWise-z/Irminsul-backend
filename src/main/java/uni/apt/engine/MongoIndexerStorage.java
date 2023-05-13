package uni.apt.engine;

import com.mongodb.client.model.Indexes;
import org.bson.Document;
import uni.apt.core.Log;
import uni.apt.core.OnlineDB;

import java.util.*;


//TODO: read the shit we just send to the db from the ranker
public class MongoIndexerStorage implements IndexerStorage{
    private static final int MAX_CACHE_SIZE = 80000;
    private final Map<String , Long> cache = new HashMap<>(MAX_CACHE_SIZE + 1 , 1.0f);
    //ok , I'll try a new caching plan , hope it works ..

    private int numWebsites = 0;
    private long numWords    = 0;
    private long numLinks    = 0;

    private static final Log log = Log.getLog(MongoIndexerStorage.class);
    static {
        log.setEnabled(true);
    }


    @Override
    public boolean save(String id) {
        //send the linked hash map to the MongoDB
        if (!OnlineDB.ready())
            return false;

        log.i("Saving ...");
        log.i("Starting meta");

        OnlineDB.MetaDB.insertOne( new Document().append("obj-id" , "indexer-meta")
                .append("websites" , numWebsites)
                .append("words" , numWords)
                .append("links" , numLinks)
        );

        log.i("Creating indexes");
        OnlineDB.IndexerWordsDB.createIndex(Indexes.ascending("word"));
        OnlineDB.IndexerLinksDB.createIndex(Indexes.ascending("word_id"));
        OnlineDB.IndexerRecordsDB.createIndex(Indexes.ascending("link_id"));


        log.i("Finished");

        return true;
    }

    @Override
    public boolean restore(String id) {
        if (!OnlineDB.ready())
            return false;

        //cache.clear();

        log.i("Restored , MEM: " + (Runtime.getRuntime().freeMemory() / 1024 / 1024));

        Document m = OnlineDB.MetaDB.findOneAndDelete(new Document().append("obj-id" , "indexer-meta"));
        if (m == null){

            numWebsites = 0;
            numWords = 0;
            numLinks = 0;

        } else {
            numWebsites = m.getInteger("websites");
            numWords    = m.getLong("words");
            numLinks    = m.getLong("links");
        }

        log.i("numWebsites: " + numWebsites + " , numWords: " + numWords + " , numLinks: " + numLinks);
        return true;
    }

    private final Object mapLock = new Object();
    @Override
    public WordProps get(String word) {
        synchronized (mapLock) {
            Long cache_ret = cache.get(word);
            if (cache_ret != null){
                WordProps props = new WordProps();
                props._indexerWordID = cache_ret;
                props.stemmed = null;
                props._indexerCreated = true;
                return props;
            }
        }

        Document doc = OnlineDB.IndexerWordsDB.find(new Document().append("word" , word)).first();

        if (doc != null){
            WordProps props = new WordProps();
            props._indexerWordID = doc.getLong("word_id");
            props.stemmed = null;
            props._indexerCreated = true;

            synchronized (mapLock){
                if (cache.size() < MAX_CACHE_SIZE){
                    cache.put(word , props._indexerWordID);
                }
            }

            return props;
        }

        WordProps props = new WordProps();
        props._indexerWordID = numWords++;
        props._indexerCreated = false;

        return props;
    }

    private void instantWrite(String word , WordProps props){
        if (!props._indexerCreated){
            OnlineDB.IndexerWordsDB.insertOne(
                    new Document()
                            .append("word" , word)
                            .append("stemmed" , props.stemmed)
                            .append("word_id" , props._indexerWordID)
            );
        }

        for (int i = 0; i < props.TFs.size(); i++) {

            List<Document> records = new LinkedList<>();

            for (WordRecord r : props.indices.get(i)){
                records.add(
                        new Document()
                                .append("pos" , r.pos)
                                .append("paragraphIndex" , r.paragraphIndex)
                                .append("type" , r.type)
                );

            }

            OnlineDB.IndexerLinksDB.insertOne(
                    new Document()
                            .append("word_id" , props._indexerWordID)
                            .append("link" , props.links.get(i))
                            .append("TF" , props.TFs.get(i))
                            .append("title" , props.titleIds.get(i))
                            .append("tags" , records)
            );

            numLinks++;
        }
    }

    @Override
    public void set(String word, WordProps props) {
        instantWrite(word , props);
    }

    @Override
    public void clear() {
        cache.clear();

        OnlineDB.IndexerWordsDB.drop();
        OnlineDB.IndexerRecordsDB.drop();
        OnlineDB.IndexerLinksDB.drop();

        OnlineDB.MetaDB.findOneAndDelete(new Document().append("obj-id" , "indexer-meta"));
    }


    @Override
    public int getNumWebsites() {
        return numWebsites;
    }

    @Override
    public void setNumWebsites(int nnw) {
        numWebsites = nnw;
    }
}
