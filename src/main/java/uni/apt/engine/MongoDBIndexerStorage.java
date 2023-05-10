package uni.apt.engine;

import org.bson.Document;
import uni.apt.Defaults;
import uni.apt.core.Log;
import uni.apt.core.OnlineDB;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MongoDBIndexerStorage implements IndexerStorage{
    private final LinkedHashMap<String , WordProps> cache = new LinkedHashMap<>();
    private int numWebsites = 0;

    private static final Log log = Log.getLog(MongoDBIndexerStorage.class);

    @Override
    public boolean save(String id) {
        //send the linked hash map to the MongoDB
        if (!OnlineDB.ready())
            return false;

        log.i("Saving ...");

        log.i("Starting words");
        clearCache();

        log.i("Starting meta");

        OnlineDB.MetaDB.insertOne(new Document().append("obj-id" , "indexer-meta").append("websites" , numWebsites));

        log.i("Finished");

        return true;
    }

    private void clearCache(){
        LinkedList<Document> insert = new LinkedList<>();

        for (Map.Entry<String , WordProps> ent : cache.entrySet()){
            Document doc = new Document();
            doc.put("word" , ent.getKey());
            log.i("Saving: " + ent.getKey());
            WordProps props = ent.getValue();
            List<Document> mentions = new LinkedList<>();
            for (int i = 0; i < props.links.size(); i++) {
                Document it = new Document();
                it.put("link" , props.links.get(i));
                it.put("TF" , props.TFs.get(i));
                it.put("title" , props.titleIds.get(i));
                List<WordRecord> records = props.indices.get(i);
                List<Document> record = new LinkedList<>();
                for (WordRecord w : records) {
                    record.add(new Document()
                            .append("type" , w.type)
                            .append("paragraphIndex" , w.paragraphIndex)
                            .append("pos" , w.pos)
                            .append("tagIndex" , w.tagIndex)
                    );
                }
                it.put("records" , record);
                mentions.add(it);
            }
            doc.put("mentions" , mentions);

            //Document matcher = new Document().append("word" , ent.getKey());
            //index.deleteOne(matcher);

            insert.add(doc);

            if (insert.size() > 500){
                OnlineDB.IndexerDB.insertMany(insert);
                insert.clear();
            }
        }
        if (insert.size() > 0)
            OnlineDB.IndexerDB.insertMany(insert);


        log.i("Clearing mem");
        cache.clear();
    }

    @Override
    public boolean restore(String id) {
        if (!OnlineDB.ready())
            return false;


        cache.clear();

        Document m = OnlineDB.MetaDB.findOneAndDelete(new Document().append("obj-id" , "indexer-meta"));
        numWebsites = (m != null) ? m.getInteger("websites") : 0;

        return true;
    }

    @Override
    public WordProps get(String word) {
        WordProps cache_ret = cache.get(word);
        if (cache_ret != null)
            return cache_ret;


        Document doc = OnlineDB.IndexerDB.findOneAndDelete(new Document().append("word" , word));

        if (doc != null){
            WordProps props = new WordProps();
            List<Document> records = doc.getList("mentions" , Document.class);
            for (Document d : records){
                props.links.add(d.getString("link"));
                props.TFs.add(((Double) d.get("TF")).floatValue());
                props.titleIds.add(d.getLong("title"));
                List<Document> wProps = d.getList("records" , Document.class);
                List<WordRecord> record = new LinkedList<>();

                for (Document r : wProps){
                    WordRecord rr = new WordRecord();
                    rr.type = r.getInteger("type");
                    rr.paragraphIndex = r.getLong("paragraphIndex");
                    rr.pos = r.getInteger("pos");
                    rr.tagIndex = r.getInteger("tagIndex");
                    record.add(rr);
                }

                props.indices.add(record);
            }

            cache.put(word , props);

            return props;
        }

        return null;
    }

    @Override
    public void set(String word, WordProps props) {
        cache.put(word , props);
    }

    @Override
    public void clear() {
        cache.clear();
        OnlineDB.IndexerDB.drop();
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

    @Override
    public List<SearchResult> search(String word, float threshold) {
        return null;
    }
}
