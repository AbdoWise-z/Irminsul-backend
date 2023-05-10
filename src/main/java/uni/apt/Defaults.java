package uni.apt;

public class Defaults {

    //DB stuff
    public static final String BASE_NAME = "apt-project";
    public static final String CONNECTION_STR = "mongodb://127.0.0.1:27017/";
    public static final String META_DB = "meta";

    //Crawler stuff
    public static final String CRAWLER_VISITED_LOG = "crawler-visited-log";
    public static final String CRAWLER_SEEDS = "crawler-seed";
    public static final String CRAWLER_CRAWLED = "crawler-crawled";
    public static final int ROBOTS_TIMEOUT_MS = 5000;
    public static final int CONNECTION_TIMEOUT_MS = 10000;

    //Indexer stuff
    public static final String INDEXER_INDEXED_PARAGRAPHS = "indexer-paragraphs";
    public static final String INDEXER_INDEXED_TITLES = "indexer-titles";

    //public static final String INDEXER_LOCAL_FILE = "base.bidx";
    public static final String INDEXER_MONGO_NAME = "indexer-data";

    //Ranker stuff
    public static final String RANKER_POPULARITY_DB = "ranker-pop-db";
    public static final String RANKER_SUGGESTIONS_DB = "ranker-suggestions";
}
