package uni.apt.engine;

import java.util.List;

public interface RankerSearchThread {
    List<Ranker.SearchResult> getResult();
    void WaitForResult();
}
