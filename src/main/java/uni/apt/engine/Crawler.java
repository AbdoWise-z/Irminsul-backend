package uni.apt.engine;

import com.google.search.robotstxt.Matcher;
import com.google.search.robotstxt.Parser;
import com.google.search.robotstxt.RobotsParseHandler;
import com.google.search.robotstxt.RobotsParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import uni.apt.core.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class Crawler{


    public static final Log log = Log.getLog(Crawler.class);

    private Queue<String> toBeSearched;

    private final Queue<String> visitedPages = new LinkedList<>();
    private final List<String> visitedPagesLog = new LinkedList<>();
    private final Queue<Document> visitedPagesData = new LinkedList<>();

    private int crawledCount;
    private int LIMIT = 100;
    private final int threadCount;

    private final Object finishCountLock = new Object();
    private int finishCount;

    private final Map<String , Matcher> cachedRobots = new HashMap<>();

    class CrawlerThread extends Thread{
        private CrawlerThread(String n){
            setName(n);
        }
        @Override
        public void run() {
            super.run();
            log.i(Thread.currentThread().getName() , "Starting CrawlerThread");
            final Parser p = new RobotsParser(new RobotsParseHandler());

            while (KeepRunning()){
                String link = getNext();

                if (link == null) //jop done
                    continue;

                log.i(Thread.currentThread().getName() , "Processing:" + link);

                //Do shit with the link
                ArrayList<String> links;
                Document doc;
                try {
                    doc = Jsoup.connect(link).get();
                } catch (Exception e) {
                    log.i(Thread.currentThread().getName() ,"Couldn't connect to Document");
                    continue;
                }

                Matcher matcher = null;
                log.i(getName() , "Loading Robots.txt");

                try {
                    URL url = new URL(link);
                    String host = url.getHost();
                    String robots_txt_link = "https://" + host + "/robots.txt";
                    matcher = cachedRobots.get(robots_txt_link);

                    if (matcher == null) {
                        URL robots_text_url = new URL(robots_txt_link);

                        BufferedReader reader = new BufferedReader(new InputStreamReader(robots_text_url.openStream()));
                        StringBuilder text = new StringBuilder();
                        String l = null;

                        while ((l = reader.readLine()) != null) {
                            l = l.trim();
                            if (l.startsWith("<!DOCTYPE")) throw new Exception("link was html page");
                            if (l.startsWith("#")) continue;
                            if (l.isEmpty()) continue;

                            text.append(l);
                            text.append("\n");
                        }

                        //System.out.println(text);

                        matcher = p.parse(text.toString().getBytes(StandardCharsets.UTF_8));
                        cachedRobots.put(robots_txt_link , matcher);
                    }

                } catch (Exception e) {
                    log.e(Thread.currentThread().getName() ,  "Couldn't read Robots.txt (" + e.getMessage() + ")");
                }

                log.i(getName() , "Loaded Robots.txt");

                links = extractLinks(doc);
                log.i(Thread.currentThread().getName() , "Loaded \"" + link + "\"");

                for (String s : links)
                {
                    log.i(Thread.currentThread().getName() ,"Trying to add : \"" + s + "\"");
                    if (matcher != null){
                        if (!matcher.singleAgentAllowedByRobots("*" , link)){
                            log.e(getName() , String.format("Robots.txt blocked \"%s\"" , link));
                            continue;
                        }
                    }
                    addLink(s);
                }
                markFinish(link , doc);
            }

            synchronized (finishCountLock){
                finishCount++;
                visitedPagesLog.clear(); //free mem , I know all threads will do it , but it doesn't really matter
            }

            log.i(Thread.currentThread().getName() , "Finished CrawlerThread " + isRunning());
        }
    }

    private final Object addLock = new Object();
    private void addLink(String str){
        synchronized (addLock){
            synchronized (finishLock){
                if (visitedPagesLog.contains(str)){
                    //log.w("Already visited \"" + str + "\" ignoring.");
                    return;
                }
            }

            synchronized (finishLock){
                if (crawledCount >= LIMIT){
                    //log.w("Limit reached , \"" + str + "\" ignoring.");
                    return;
                }
            }

            toBeSearched.add(str);
            //log.i(Thread.currentThread().getName() , "Added: " + str);
        }
    }

    private final Object getLock = new Object();
    private String getNext(){
        if (crawledCount > LIMIT){
            return null;
        }
        synchronized (getLock){
            if (toBeSearched.size() > 0) {
                return toBeSearched.remove();
            }
            return null;
        }
    }
    private final Object finishLock = new Object();
    private void markFinish(String str , Document doc){
        synchronized (finishLock){
            crawledCount++;
            visitedPages.add(str);
            visitedPagesLog.add(str);
            visitedPagesData.add(doc);
            log.i("Finished: " + str + " [" + crawledCount + "]");
        }
    }

    private boolean KeepRunning(){
        synchronized (finishLock){
            return crawledCount < LIMIT;
        }
    }

    public boolean isRunning() {
        synchronized (finishCountLock) {
            return finishCount != threadCount;
        }
    }

    public LoadedSite getNextSite(){ //used to quickly obtain the loaded data
        synchronized (finishLock){
            LoadedSite s = new LoadedSite();
            s.link = visitedPages.poll();
            if (s.link == null)
                return null;

            s.doc = visitedPagesData.poll();


            return s;
        }

    }

    public Queue<String> getVisitedPages() {
        if (isRunning())
            return null;
        return visitedPages;
    }

    public Queue<Document> getVisitedPagesData() {
        if (isRunning())
            return null;
        return visitedPagesData;
    }

    public int getCrawledCount() {
        return crawledCount;
    }

    public int getLimit() {
        return LIMIT;
    }

    public Crawler(int thread_count) {
        crawledCount = 0;
        threadCount = thread_count;
        finishCount = thread_count;
    }

    public synchronized void start(int limit , Queue<String> seed , boolean clear){
        if (isRunning())
            throw new IllegalStateException("already running");

        finishCount = 0;

        toBeSearched = seed;

        LIMIT = limit;

        if (clear) {
            cachedRobots.clear();
            visitedPages.clear();
            visitedPagesData.clear();
            crawledCount = 0;
        }

        for (int i = 0;i < threadCount;i++){
            new CrawlerThread("TH-" + i).start();
        }
    }

    private URI normalize(String url) throws URISyntaxException
    {
        URI uri = new URI(url);
        return new URI("https",uri.getHost().replace("www.",""),uri.getPath(),null);
    }

    private boolean isSamePage(String url1, String url2) throws URISyntaxException
    {
        return normalize(url1).equals(normalize(url2));
    }
    private ArrayList<String> extractLinks(Document doc)
    {
        ArrayList<String> list = new ArrayList<>();

        Elements elements = doc.select("a[href~=^(http(s):\\/\\/.)[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)$]");
        for (Element e : elements){
            list.add(e.absUrl("href"));
        }
        return list;
    }

}