package uni.apt;

import uni.apt.engine.*;
import uni.apt.core.Log;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;

public class Main {

    public static final Log log = Log.getLog(Main.class);
    public static void main(String[] args) throws IOException {
        log.i("Starting...");
        Queue<String> seed = new LinkedList<>();
        seed.add("https://google.com");
        seed.add("https://wikipedia.com");
        seed.add("https://yahoo.com");

        Crawler.log.setEnabled(false);

        Crawler crawler = new Crawler(12);
        crawler.start(1000 , seed , true);

        int c = 0;

        log.i("Crawler started");

        Indexer indexer = new Indexer(12);
        indexer.start(crawler);

        log.i("Indexer started");

        while (crawler.isRunning()){
            if (c != crawler.getCrawledCount()) {
                log.i(crawler.getCrawledCount() + "/" + crawler.getLimit());
                c = crawler.getCrawledCount();
            }
        }

        log.i("Crawler finished");


        while (indexer.isRunning()){}

        log.i("Indexer finished");

        IndexedDatabase database = new IndexedDatabase();
        database.insert(indexer);

        database.WriteToFile("base.bidx");

        log.i("Finished.");

    }
}