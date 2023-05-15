package uni.apt;


import uni.apt.core.OnlineDB;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
        OnlineDB.init();
        System.out.println("البتاع دا بيفهم عربي ؟");
        String title = OnlineDB.getParagraph(12864);
        System.out.println(title);

        String utf8 = new String(title.getBytes( StandardCharsets.UTF_8) , StandardCharsets.UTF_16);
        System.out.println(utf8);

        if (title.contains("من")){
            System.out.println("it does");
        }
    }
}
