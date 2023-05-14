package uni.apt;


import java.util.LinkedList;
import java.util.List;

import static uni.apt.core.InversionCounter.countInversions;

//TODO: fix issues related to stemming

public class test {

    private static int getOrder(List<List<Integer>> indexes , int posX , int prev){
        if (posX == indexes.size())
            return 0;

        List<Integer> selections = indexes.get(posX);

        if (selections.size() == 0){
            return getOrder(indexes , posX + 1 , -1);
        }

        int m = 0;
        for (Integer i : selections){
            int k = getOrder(indexes , posX + 1 , i);
            if (i - prev == 1 || prev == -1){
                m = Math.max(m , k + 1);
            }else{
                m = Math.max(m , k);
            }
        }

        return m;
    }

    public static void main(String[] args){
        List<List<Integer>> items = new LinkedList<>();
        items.add(List.of());
        items.add(List.of(4,2,10));
        items.add(List.of(8,9));
        items.add(List.of());
        items.add(List.of(10,11));
        System.out.println(getOrder(items , 0 , -1));
    }
}
