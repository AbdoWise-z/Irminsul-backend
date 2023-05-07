package uni.apt.engine;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoIterable;
import org.bson.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import uni.apt.Defaults;
import uni.apt.core.Log;
import uni.apt.core.OnlineDB;

import java.io.Serializable;
import java.util.*;

public class Indexer {



    public static final Log log = Log.getLog(Indexer.class);
    private final int threadCount;
    private int finishCount;
    private final Object finishCountLock = new Object();

    public Indexer(int thread_count){
        if (thread_count < 1)
            throw new IllegalArgumentException("thread_count < 1");

        threadCount = thread_count;
        finishCount = thread_count;
    }

    public boolean isRunning(){
        synchronized (finishCountLock){
            return finishCount != threadCount;
        }
    }
    private final Object insertLock = new Object();
    final Map<String , WordProps> indexedWords = new LinkedHashMap<>();
    private final List<String> currentActive = new LinkedList<>(); //linked because we will add and remove quickly
    private final Object activeLick = new Object();
    private void insert(String word , String link , List<WordRecord> indices){
        while (true) {
            synchronized (activeLick) { //synchronized must be on the inside to avoid locking
                if (! currentActive.contains(word)) {
                    currentActive.add(word); //prevent everyone form editing this word rn
                    break;
                }
            }
        }

        WordProps props = null;

        synchronized (insertLock){
            props = indexedWords.get(word);
        }

        if (props == null)
            props = new WordProps();

        int idx = props.links.indexOf(link);
        if (idx > 0){
            log.e("Error , link already exists : " + link);
            props.links.remove(idx);
            props.indices.remove(idx);
        }

        props.links.add(link);
        props.indices.add(indices);

        synchronized (insertLock){
            indexedWords.put(word , props);
        }

        synchronized (activeLick){
            currentActive.remove(word); //allow other threads to use it
        }

    }

    private final Object getLock = new Object();

    private LoadedSite getNextSite(){
        synchronized (getLock){
            if (load_cache.size() == 0){
                MongoIterable<Document> docs = crawler_result.find().limit(LOAD_CACHE_MAX_SIZE);
                for (Document doc : docs) {
                    load_cache.add(doc);
                    crawler_result.deleteOne(doc);
                }
            }

            if (load_cache.size() == 0){
                return null;
            }

            LoadedSite s = new LoadedSite();
            Document item = load_cache.pollFirst();
            s.link = item.getString("link");
            s.doc  = Jsoup.parse(item.getString("body"));
            return s;
        }
    }


    private final Object paragraphLock = new Object();
    private long paragraph_counter = 0;
    private MongoCollection<Document> paragraphs;
    private MongoCollection<Document> crawler_result;

    private LinkedList<Document> load_cache;
    private static final int LOAD_CACHE_MAX_SIZE = 300; //load at most 300 documents at one time
                                                        //the higher, the better performance but higher mem usage

    public synchronized void start(String snapshot_file){
        if (isRunning())
            throw new IllegalStateException("already running");

        if (!OnlineDB.ready())
            throw new IllegalStateException("Online DB not running");

        indexedWords.clear();

        if (snapshot_file != null){ // set the initial state
            if (!IndexerIO.getFromFile(snapshot_file , this)){
                throw new IllegalArgumentException("the snapshot file is not valid");
            }
        }

        finishCount = 0;
        crawler_result = OnlineDB.base.getCollection(Defaults.CRAWLER_COLLECTION_CRAWLED);
        paragraphs     = OnlineDB.base.getCollection(Defaults.INDEXER_INDEXED_PARAGRAPHS);
        paragraph_counter = paragraphs.countDocuments();

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
                long para;

                for (Element element : s.doc.select("h1, h2, h3, h4, h5, h6, p, div")) {
                    String tagName = element.tagName();
                    String text = element.text();

                    synchronized (paragraphLock){
                        paragraphs.insertOne(new Document().append("text" , text).append("index" , paragraph_counter));
                        para = paragraph_counter;
                        paragraph_counter++;
                    }

                    String[] ws = text.split("\\s+");
                    wordIndex = 0;
                    for (String word : ws){
                        word = Trim(word);

                        if (word.length() < 3) continue; //a , an and garbage letters
                        if (word.equals("the")) continue;

                        WordRecord idx = new WordRecord();
                        idx.type = (tagName.equals("div") || tagName.equals("p")) ? WordRecord.PARAGRAPH : WordRecord.HEADER;
                        idx.pos  = wordIndex++;
                        idx.tagIndex = tagIndex;
                        idx.paragraphIndex = para;

                        List<WordRecord> l = words.get(word);
                        if (l == null){
                            l = new LinkedList<>();
                        }
                        l.add(idx);

                        words.put(word , l);
                    }

                    tagIndex++;
                }

                //finished , now we add all of this to the final array
                for (Map.Entry<String , List<WordRecord>> et : words.entrySet()){
                    insert(et.getKey() , s.link , et.getValue());
                }
            }

            synchronized (finishCountLock){
                finishCount++;
            }

            log.i(getName() , "Finished");
        }
    }

    private static final String[] TrimChars = {"," , "." , "/" , "\\" , "|" , ">" , "<" , "?" , "'" , "\""};
    private static String Trim(String s){
        s = s.trim();
        boolean done = false;
        do {
            done = true;
            for (String trimChar : TrimChars) {
                if (s.startsWith(trimChar)) {
                    s = s.substring(trimChar.length());
                    done = false;
                    break;
                }
            }
        } while (!done);

        do {
            done = true;
            for (String trimChar : TrimChars) {
                if (s.endsWith(trimChar)) {
                    s = s.substring(0 , s.length() - trimChar.length() - 1);
                    done = false;
                    break;
                }
            }
        } while (!done);

        return s;
    }

    public Map<String, WordProps> getIndexedWords() {
        if (isRunning())
            return null;
        return indexedWords;
    }

    public int getIndexedCount(){
        synchronized (insertLock){
            return indexedWords.size();
        }
    }

}
