package uni.apt;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import uni.apt.core.Log;
import uni.apt.core.OnlineDB;
import uni.apt.engine.Crawler;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

public class CrawlerMain {
    private static void printf(String str , Object... args){
        System.out.printf(str , args);
    }

    private static final Log log = Log.getLog(CrawlerMain.class);

    public static void main(String[] args) throws InterruptedException, IOException {
        OnlineDB.init();
        Thread.sleep(2000);

        log.w("Started");
        Scanner in = new Scanner(System.in);
        int num_threads = -1;
        while (num_threads < 0 || num_threads > 30){
            printf("Enter the number of threads: ");
            num_threads = in.nextInt();
            if (num_threads < 0 || num_threads > 30){
                printf("%d is not a valid number\n" , num_threads);
            }
        }


        Crawler crawler = new Crawler(num_threads);

        Queue<String> seed = new LinkedList<>();
        printf("Enter the starting seed: (evey link on a line)\n");
        printf("(if you want to load from DB type DB , exit to finish)\n");

        while (true){
            String str = in.nextLine().trim();
            if (str.equals("exit")){
                if (seed.size() == 0){
                    printf("Type at least one seed link\n");
                    continue;
                }else {
                    break;
                }
            }

            if (str.equals("DB")){
                seed = null;
                break;
            }

            if (!str.startsWith("https://"))
                str = "https://" + str;

            if (!str.contains("?") && !str.endsWith("/"))
                str = str + "/"; //if it doesn't contain any parameters & doesn't end with a "/" then add it

            seed.add(str);
        }

        if (seed == null){
            //load the seed from DB
            log.i("Loading the seed from DB ...");
            MongoCollection<Document> docs = OnlineDB.base.getCollection(Defaults.CRAWLER_COLLECTION_SEEDS);
            FindIterable<Document> seeds = docs.find();
            seed = new LinkedList<>();
            for (Document doc : seeds){
                String link = doc.getString("link");
                log.i("Loaded: " + link);
                seed.add(link);
            }

            log.i("Seed loaded!");
        }

        printf("Do you want to continue from last crawl ? (Y/N) ");
        String str = in.nextLine().trim();
        LinkedList<String> visitedPages = new LinkedList<>();

        if (str.equalsIgnoreCase("y") || str.equalsIgnoreCase("yes")){
            log.i("Loading old log ..");
            MongoCollection<Document> docs = OnlineDB.base.getCollection(Defaults.CRAWLER_COLLECTION_LOG);
            FindIterable<Document> visited_log = docs.find();
            for (Document doc : visited_log){
                String link = doc.getString("link");
                log.i("Loaded: " + link);
                visitedPages.add(link);
            }
            log.i("done");
        }else{
            log.i("Clearing ..");
            OnlineDB.base.getCollection(Defaults.CRAWLER_COLLECTION_LOG).drop();
            OnlineDB.base.getCollection(Defaults.CRAWLER_COLLECTION_SEEDS).drop();
            OnlineDB.base.getCollection(Defaults.CRAWLER_COLLECTION_CRAWLED).drop();
            OnlineDB.base.getCollection(Defaults.DB_META).findOneAndDelete(new org.bson.Document().append("obj-id" , "crawler-meta"));
            log.i("Done");
        }

        int crawl_count = -1;
        while (crawl_count < 0){
            printf("Enter the number of websites to crawl: ");
            crawl_count = in.nextInt();
            if (crawl_count < 0){
                printf("%d is not a valid number\n" , num_threads);
            }
        }

        while (System.in.available() > 0 && in.hasNextLine()) {
            log.i("hasNextLine");
            String l = in.nextLine(); //clear the input
            log.i("Cleared: " + l);
        }

        log.i("Starting Crawler (type any key to stop) ...");
        crawler.start(crawl_count , seed , visitedPages);

        int c = 0;
        while (crawler.isRunning()){
            if (System.in.available() > 0 && in.hasNextLine()){
                in.nextLine();
                log.w("Stopping Crawler");
                break;
            }

            if (c != crawler.getCrawledCount()){
                c = crawler.getCrawledCount();
                log.i("Crawled : " + c + "/" + crawl_count);
            }
        }

        if (crawler.isRunning())
            crawler.stop();

        while (crawler.isRunning()){} //wait for it to stop

        log.i("Crawler stopped , sending new data to db");
        seed = crawler.getCurrentSeed();
        visitedPages = (LinkedList<String>) crawler.getVisitedPages();

        MongoCollection<Document> seeds = OnlineDB.base.getCollection(Defaults.CRAWLER_COLLECTION_SEEDS);
        String s;
        while ((s = seed.poll()) != null){
            seeds.insertOne(new Document().append("link" , s));
        }

        log.i("seeds done , sending visited log");

        MongoCollection<Document> visited = OnlineDB.base.getCollection(Defaults.CRAWLER_COLLECTION_LOG);
        visited.drop(); //clear the db first
                        //TODO: maybe I should just insert after the last loaded link from the visited log instead

        for (String li : visitedPages){
            visited.insertOne(new Document().append("link" , li));
        }

        log.i("visited log done");

        log.w("Finished");

    }
}
