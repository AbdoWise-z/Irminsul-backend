package uni.apt.engine;

import java.util.Iterator;
import java.util.List;

public interface IndexerStorage {

    class SearchResult{
        float match_factor;
        WordProps props;
    }

    /**
     * saves this indexer storage to the database (local or online)
     * with a specific id
     *
     * @param id the id to associate
     * @return true if success , false otherwise
     * */
    boolean save(String id);

    /**
     * restores the indexer storage with a specific id
     * @param id the id associated with the db
     * @return true if success , false otherwise
     * */
    boolean restore(String id);

    /**
     * returns the WordProps for a word
     * @param word the word to find the props for
     * */
    WordProps get(String word);

    /**
     * sets the WordProps for a word
     *
     * @param word the word
     * @param props the props
     * */
    void set(String word , WordProps props);

    /**
     * clears the storage
     * */
    void clear();

    /**
     * returns the total number of websites
     * */
    int getNumWebsites();

    /**
     * set the total number of websites
     * this is just a flag and its doesn't
     * affect the database in any way
     * */
    void setNumWebsites(int nnw);


    /**
     * searches for a word using
     * for example : search("travel")
     * should match with travel , traveller , travelling , etc ..
     * the match_factor is one if it's the same word and always should be <=1
     * the items returned should be ordered by the match_factor in a descending
     * manner
     * @param word the word to search for
     * @param threshold the min value of the match_factor to be acceptable
     *                  if threshold <= 0 then it will return the entire db
     * @return List of items matching that word
     * */
    List<SearchResult> search(String word , float threshold);
}
