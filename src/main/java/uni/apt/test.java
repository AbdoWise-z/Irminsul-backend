package uni.apt;

import org.apache.commons.text.similarity.CosineDistance;
import org.apache.commons.text.similarity.CosineSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.tartarus.snowball.ext.PorterStemmer;

import java.util.LinkedList;
import java.util.List;

import static uni.apt.core.InversionCounter.countInversions;

//TODO: fix issues related to stemming

public class test {

    private static int getOrder(List<List<Integer>> indexes , int posX , int prev){
        if (posX == indexes.size())
            return 1;

        List<Integer> selections = indexes.get(posX);
        for (Integer i : selections){
            if (i - prev == 1 || posX == 0){
                int k = getOrder(indexes , posX + 1 , i);
                if (k > 0){
                    if (posX == 0){ //first call
                        return i;
                    }
                    return 1;
                }
            }
        }

        return -1;
    }
    public static void main(String[] args){
        List<List<Integer>> items = new LinkedList<>();
        items.add(List.of(1,6,8));
        items.add(List.of(4,2,7));
        items.add(List.of(8,9));
        System.out.println(Runtime.getRuntime().freeMemory());
        System.out.println(getOrder(items , 0 , -1));
    }
}
