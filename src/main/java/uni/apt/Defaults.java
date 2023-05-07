package uni.apt;

public class Defaults {

    //DB stuff
    public static final String BASE_NAME = "apt-project";
    public static final String CONNECTION_STR = "mongodb://127.0.0.1:27017/";

    //Crawler stuff
    public static final String CRAWLER_COLLECTION_LOG = "crawler-visited-log";
    public static final String CRAWLER_COLLECTION_SEEDS = "crawler-seed";
    public static final String CRAWLER_COLLECTION_CRAWLED = "crawler-crawled2";

    public static final int ROBOTS_TIMEOUT_MS = 5000;
    public static final int CONNECTION_TIMEOUT_MS = 10000;

    //Indexer stuff
    public static final String INDEXER_INDEXED_PARAGRAPHS = "indexer-paragraphs";
    public static final String INDEXER_LOCAL_FILE = "base.bidx";

}
