package uni.apt.engine;

import com.google.search.robotstxt.Matcher;
import com.google.search.robotstxt.Parser;
import com.google.search.robotstxt.RobotsParseHandler;
import com.google.search.robotstxt.RobotsParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import uni.apt.Defaults;
import uni.apt.core.Log;
import uni.apt.core.OnlineDB;

import java.io.*;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class Crawler{


    public static final Log log = Log.getLog(Crawler.class);
    static {
        log.setEnabled(true);
    }

    private Queue<String> toBeSearched;

    private final List<String> visitedPagesLog = new LinkedList<>();
    private final String[] currentActive;
    private int crawledCount;
    private int LIMIT = 100;
    private final int threadCount;

    private final Object finishCountLock = new Object();
    private int finishCount;

    private final Map<String , Matcher> cachedRobots = new HashMap<>();

    private boolean force_stop = false;

    class CrawlerThread extends Thread{
        private HashMap<String,String> cookies = new HashMap<>(); //empty
        private PrintWriter logger;
        private final int id;
        private CrawlerThread(String n , int id){
            setName(n);
            this.id = id;
            try {
                logger = new PrintWriter(new FileOutputStream("logger/crawler" + id + ".txt"), true);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        @Override
        public void run() {
            super.run();
            log.i(Thread.currentThread().getName() , "Starting CrawlerThread");
            final Parser p = new RobotsParser(new RobotsParseHandler());

            logger.println("Started");

            while (KeepRunning()){
                try {
                    String link = getNext(id);

                    if (link == null) //jop done
                        continue;

                    log.i(Thread.currentThread().getName(), "Processing:" + link);
                    logger.println("Starting link: " + link);

                    //Do shit with the link
                    ArrayList<String> links;
                    Document doc;

                    try {
                        doc = Jsoup.connect(link).timeout(Defaults.CONNECTION_TIMEOUT_MS).cookies(cookies).get();
                    } catch (Exception e) {
                        log.i(Thread.currentThread().getName(), "Couldn't connect to Document");
                        continue;
                    }

                    Matcher matcher = null;
                    log.i(getName(), "Loading Robots.txt");
                    logger.println("Loading Robots");
                    HttpURLConnection connection = null;
                    try {
                        URL url = new URL(link);
                        String host = url.getHost();
                        String robots_txt_link = "https://" + host + "/robots.txt";
                        log.i(getName() , "Robots:" + robots_txt_link);
                        logger.println("Robots: " + robots_txt_link);

                        matcher = cachedRobots.get(robots_txt_link);

                        if (matcher == null) {

                            URL robots_text_url = new URL(robots_txt_link);
                            connection = (HttpURLConnection) robots_text_url.openConnection();
                            connection.setRequestMethod("GET");
                            connection.setReadTimeout(Defaults.ROBOTS_TIMEOUT_MS);
                            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                            StringBuilder text = new StringBuilder();
                            String l;

                            while ((l = reader.readLine()) != null){
                                text.append(l);
                            }

                            matcher = p.parse(text.toString().getBytes(StandardCharsets.UTF_8));

                            if (matcher == null)
                                throw new NullPointerException("failed to parse");

                            cachedRobots.put(robots_txt_link, matcher);
                        }

                        log.i(getName(), "Loaded Robots.txt");
                        logger.println("Loaded Robots");
                    } catch (Exception e) {
                        log.e(Thread.currentThread().getName(), "Couldn't read Robots.txt (" + e.getMessage() + ")");
                        logger.println("Couldn't read Robots.txt (" + e.getMessage() + ")");
                    } finally {
                        if (connection != null) connection.disconnect();
                    }


                    links = extractLinks(doc);
                    log.i(Thread.currentThread().getName(), "Loaded \"" + link + "\"");

                    for (String s : links) {
                        //log.i(Thread.currentThread().getName() ,"Trying to add : \"" + s + "\"");
                        if (matcher != null) {
                            if (!matcher.singleAgentAllowedByRobots("*", link)) {
                                log.e(getName(), String.format("Robots.txt blocked \"%s\"", link));
                                continue;
                            }
                        }
                        addLink(s);
                    }

                    logger.println("writing in the db");
                    markFinish(id, link, doc);
                    logger.println("Finished link");

                } catch (Exception e) { //this should never happen
                    log.e("General Error: " + e.getMessage());
                    e.printStackTrace();
                }
            }


            synchronized (finishCountLock){
                finishCount++;
                try {
                    if (finishCount == threadCount) {
                        log.i("writing meta");
                        OnlineDB.MetaDB.insertOne(new org.bson.Document().append("obj-id", "crawler-meta").append("websites", _websiteCount));
                        LinkedList<org.bson.Document> docs = new LinkedList<>();
                        for (Map.Entry<String, Integer> ent : popMap.entrySet()) {
                            org.bson.Document doc = OnlineDB.RankerPopularityDB.findOneAndDelete(new org.bson.Document("link", ent.getKey()));
                            Integer pop = ent.getValue();
                            if (doc != null) {
                                pop += doc.getInteger("mentions");
                            }
                            docs.add(new org.bson.Document().append("link", ent.getKey()).append("mentions", pop));
                        }
                        OnlineDB.RankerPopularityDB.insertMany(docs);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }

            logger.println("Finished");
            logger.close();

            log.i(Thread.currentThread().getName() , "Finished CrawlerThread ");
        }
    }

    private final Object addLock = new Object();
    private void addLink(String str){
        synchronized (addLock){

            Integer pop = popMap.get(str);
            if (pop == null)
                pop = 0;
            pop++;
            popMap.put(str , pop);
            _websiteCount++;


            if (toBeSearched.contains(str))
                return;

            synchronized (finishLock) {
                if (visitedPagesLog.contains(str)) {
                    //log.w("Already visited \"" + str + "\" ignoring.");
                    return;
                }

                for (int i = 0; i < threadCount; i++) {
                    if (currentActive[i] != null && currentActive[i].equals(str))
                        return;
                }
            }
//            synchronized (finishLock){
//                if (crawledCount >= LIMIT){
//                    //log.w("Limit reached , \"" + str + "\" ignoring.");
//                    return;
//                }
//            }


            toBeSearched.add(str);
            //log.i(Thread.currentThread().getName() , "Added: " + str);
        }
    }
    private String getNext(int id){
        if (crawledCount >= LIMIT){
            return null;
        }

        synchronized (addLock){
            String n = toBeSearched.poll();

            synchronized (finishLock) {
                currentActive[id] = n;
            }

            return n;
        }
    }
    private final Object finishLock = new Object();
    private void markFinish(int id , String str , Document doc){
        synchronized (finishLock){
            crawledCount++;
            visitedPagesLog.add(str);

            currentActive[id] = null;

            log.i("Finished: " + str + " [" + crawledCount + "]");
        }

        String page_src = doc.select("body").html();

        org.bson.Document item = new org.bson.Document();
        item.put("link" , str);
        item.put("body" , page_src);
        item.put("title" , doc.select("title").text());

        OnlineDB.CrawlerCrawledDB.insertOne(item); //snd the object to the db
    }

    private boolean KeepRunning(){
        if (force_stop)
            return false;

        synchronized (finishLock){
            return crawledCount < LIMIT;
        }
    }

    public boolean isRunning() {
        synchronized (finishCountLock) {
            return finishCount != threadCount;
        }
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

        currentActive = new String[thread_count];
    }

    private final HashMap<String , Integer> popMap = new HashMap<>();
    private int _websiteCount = 0;
    public synchronized void start(int limit , Queue<String> seed , LinkedList<String> visitedPagesLog){
        if (isRunning())
            throw new IllegalStateException("already running");

        if (!OnlineDB.ready())
            throw new IllegalStateException("Online DB not running");

        if (seed == null){
            throw new NullPointerException("seed is null!!");
        }else{
            toBeSearched = seed;
        }

        popMap.clear();

        force_stop = false;

        org.bson.Document m = OnlineDB.MetaDB.findOneAndDelete(new org.bson.Document().append("obj-id" , "crawler-meta"));
        _websiteCount = (m != null) ? m.getInteger("websites") : 0;



        this.visitedPagesLog.clear();

        if (visitedPagesLog != null){
            this.visitedPagesLog.addAll(visitedPagesLog);
        }

        finishCount = 0;

        LIMIT = limit;


        for (int i = 0;i < threadCount;i++){
            new CrawlerThread("TH-" + i , i).start();
        }
    }

    public synchronized void stop(){
        if (!isRunning())
            throw new IllegalStateException("not running.");

        force_stop = true;
    }

    public Queue<String> getCurrentSeed() {
        if (isRunning())
            return null;
        return toBeSearched;
    }

    public List<String> getVisitedPages(){
        if (isRunning())
            return null;
        return visitedPagesLog;
    }

    private static URI normalize(String url) throws URISyntaxException
    {
        URI uri = new URI(url);
        return new URI("https",uri.getHost().replace("www.",""),uri.getPath(),null);
    }

    private static boolean isSamePage(String url1, String url2) throws URISyntaxException
    {
        return normalize(url1).equals(normalize(url2));
    }
    private static ArrayList<String> extractLinks(Document doc)
    {
        ArrayList<String> list = new ArrayList<>();

        Elements elements = doc.select("a[href~=^(http(s):\\/\\/.)[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)$]");
        for (Element e : elements){
            list.add(e.absUrl("href"));
        }
        return list;
    }

}