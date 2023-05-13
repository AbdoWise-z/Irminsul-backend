package uni.apt.engine;

import com.mongodb.client.MongoIterable;
import org.bson.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.tartarus.snowball.ext.PorterStemmer;
import uni.apt.core.Log;
import uni.apt.core.OnlineDB;
import uni.apt.core.QuerySelector;

import java.util.*;

import static uni.apt.core.QuerySelector.Trim;

public class Indexer {

    public static final Log log = Log.getLog(Indexer.class);
    private final int threadCount;
    private int finishCount;
    private final Object finishCountLock = new Object();
    private final IndexerStorage storage;

    public Indexer(int thread_count , IndexerStorage storage){
        if (thread_count < 1)
            throw new IllegalArgumentException("thread_count < 1");

        if (storage == null)
            throw new NullPointerException("Storage was null");

        this.storage = storage;

        threadCount = thread_count;
        finishCount = thread_count;
    }

    public boolean isRunning(){
        synchronized (finishCountLock){
            return finishCount != threadCount;
        }
    }
    private final Object insertLock = new Object();
    //final Map<String , WordProps> indexedWords = new LinkedHashMap<>();
    private final List<String> currentActive = new LinkedList<>(); //linked because we will add and remove quickly
    private final Object activeLick = new Object();
    private void insert(String word , long title , String link , float tf , List<WordRecord> indices){
        while (true) {
            synchronized (activeLick) { //synchronized must be on the inside to avoid locking
                if (! currentActive.contains(word)) {
                    currentActive.add(word); //prevent everyone form editing this word rn
                    break;
                }
            }
        }

        WordProps props;

        //synchronized (insertLock){
            props = storage.get(word);
        //}

        if (!props._indexerCreated) {
            PorterStemmer stemmer = new PorterStemmer();
            stemmer.setCurrent(word);
            stemmer.stem();
            props.stemmed = stemmer.getCurrent();
        }

        int idx = props.links.indexOf(link);
        if (idx > 0){
            log.e("Error , link already exists : " + link);
            props.links.remove(idx);
            props.indices.remove(idx);
            props.TFs.remove(idx);
            props.titleIds.remove(idx);
        }

        props.links.add(link);
        props.indices.add(indices);
        props.TFs.add(tf);
        props.titleIds.add(title);

        //synchronized (insertLock){
            storage.set(word , props);
        //}

        synchronized (activeLick){
            currentActive.remove(word); //allow other threads to use it
        }

    }

    private final Object getLock = new Object();

    private LoadedSite getNextSite(){
        synchronized (getLock){
            if (load_cache.size() == 0){
                MongoIterable<Document> docs = OnlineDB.CrawlerCrawledDB.find().limit(LOAD_CACHE_MAX_SIZE);
                for (Document doc : docs) {
                    load_cache.add(doc);
                    OnlineDB.CrawlerCrawledDB.deleteOne(doc);
                }
            }

            if (load_cache.size() == 0){
                return null;
            }

            LoadedSite s = new LoadedSite();
            Document item = load_cache.pollFirst();
            s.link = item.getString("link");
            s.doc  = Jsoup.parse(item.getString("body"));
            s.title = item.getString("title");
            return s;
        }
    }


    private final Object paragraphLock = new Object();
    private long paragraph_counter = 0;

    private final Object titleLock = new Object();
    private long title_counter = 0;

    private LinkedList<Document> load_cache;
    private static final int LOAD_CACHE_MAX_SIZE = 300; //load at most 300 documents at one time
                                                        //the higher, the better performance but higher mem usage

    public synchronized void start(String snapshot_id){
        if (isRunning())
            throw new IllegalStateException("already running");

        if (!OnlineDB.ready())
            throw new IllegalStateException("Online DB not running");

        if (!storage.restore(snapshot_id)){
            throw new IllegalArgumentException("no such snapshot");
        }

        finishCount = 0;
        paragraph_counter = OnlineDB.ParagraphsDB.countDocuments();
        title_counter     = OnlineDB.TitlesDB.countDocuments();
        load_cache = new LinkedList<>();

        for (int i = 0;i < threadCount;i++){
            new IndexerThread("IX-" + i).start();
        }
    }

    class IndexerThread extends Thread{
        private IndexerThread(String n){
            setName(n);
        }

        @Override
        public void run() {
            super.run();
            log.i(getName() , "Started");
            LoadedSite s = null;
            while ((s = getNextSite()) != null){

                log.i(getName() , "Indexing: " + s.link);

                Map<String , List<WordRecord>> words = new HashMap<>();
                int tagIndex = 0;
                int wordIndex;
                int wordsCount = 0;

                HashMap<Integer , Long> paragraphMapping = new HashMap<>();
                //use a map to map paragraph ids to avoid any racing condition

                for (Element element : s.doc.select("h1, h2, h3, h4, h5, h6, p, div")) {
                    String tagName = element.tagName();
                    String text = element.text().trim();

                    if (text.isEmpty()) continue;

                    String[] ws = text.split("\\s+");
                    wordIndex = 0;
                    for (String word : ws){
                        word = Trim(word);

                        wordsCount++; //take account for arabic words
                                      //because trimming removes them

                        if (!QuerySelector.isWordAllowed(word)) continue;


                        WordRecord idx = new WordRecord();
                        idx.type = (tagName.equals("div") || tagName.equals("p")) ? WordRecord.PARAGRAPH : WordRecord.HEADER;
                        idx.pos  = wordIndex++;
                        idx.tagIndex = tagIndex;
                        idx.paragraphIndex = -1; //tag index is our local paragraph id , I'll use the map later to fix it to its write position

                        List<WordRecord> l = words.get(word);
                        if (l == null){
                            l = new LinkedList<>();
                        }
                        l.add(idx);

                        words.put(word , l);
                    }

                    if (wordIndex != 0) { //if we didn't add any words then skip this paragraph
                        synchronized (paragraphLock) {
                            OnlineDB.ParagraphsDB.insertOne(new Document().append("text", text).append("index", paragraph_counter));
                            paragraphMapping.put(tagIndex , paragraph_counter);
                            paragraph_counter++;
                        }
                    }

                    tagIndex++;
                }

                long titleId; //to avoid any racing condition
                synchronized (titleLock){
                    OnlineDB.TitlesDB.insertOne(new Document().append("text", s.title).append("index", title_counter));
                    titleId = title_counter;
                    title_counter++;
                }

                //finished , now we add all of this to the final array
                for (Map.Entry<String , List<WordRecord>> et : words.entrySet()){
                    for (WordRecord w : et.getValue()){
                        //un-map the paragraph index
                        w.paragraphIndex = paragraphMapping.getOrDefault(w.tagIndex , (long) -2); //-2 == error
                    }
                    insert(et.getKey() , titleId , s.link , (float) et.getValue().size() / wordsCount , et.getValue());
                }

                synchronized (insertLock){
                    storage.setNumWebsites(storage.getNumWebsites() + 1);
                }
            }

            synchronized (finishCountLock){
                finishCount++;
            }

            log.i(getName() , "Finished");
        }
    }



    public IndexerStorage getStorage() {
        if (isRunning()) return null;
        return storage;
    }
}
