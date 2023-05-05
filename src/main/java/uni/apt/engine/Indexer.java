package uni.apt.engine;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import uni.apt.core.Log;

import java.io.Serializable;
import java.util.*;

public class Indexer {



    public static final Log log = Log.getLog(Indexer.class);

    private Crawler crawler;
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
    private final Map<String , WordProps> indexedWords = new HashMap<>();

    private final LinkedList<String> paragraphList = new LinkedList<>();
    private final Object paragraphLock = new Object();
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
    public LoadedSite nextSite(){
        synchronized (getLock){
            LoadedSite s = new LoadedSite();
            s.link = crawler.getVisitedPages().poll();
            s.doc  = crawler.getVisitedPagesData().poll();

            if (s.link == null)
                return null;

            return s;
        }
    }


    public synchronized void start(Crawler c){
        if (isRunning())
            throw new IllegalStateException("already running");

        if (c == null)
            throw new NullPointerException("Crawler is null");

        if (c.isRunning())
            throw new IllegalStateException("Crawler is still running");

        indexedWords.clear();
        paragraphList.clear();

        finishCount = 0;
        crawler = c;

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
            LoadedSite s;
            while ((s = nextSite()) != null){
                log.i(getName() , "Indexing: " + s.link);

                Map<String , List<WordRecord>> words = new HashMap<>();
                int tagIndex = 0;
                int wordIndex;
                int para;

                for (Element element : s.doc.select("h1, h2, h3, h4, h5, h6, p, div")) {
                    String tagName = element.tagName();
                    String text = element.text();

                    synchronized (paragraphLock){
                        paragraphList.add(text);
                        para = paragraphList.size() - 1;
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

    public LinkedList<String> getParagraphList() {
        if (isRunning())
            return null;
        return paragraphList;
    }
}
