package uni.apt.engine;

import java.util.List;

public interface RankerSearchThread {
    void setStrict(boolean b); //if b is true , then we should match words if and only if they match the search word exactly
    List<Ranker.SearchResult> getResult();
    void WaitForResult();
}
