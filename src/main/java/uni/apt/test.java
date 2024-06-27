package uni.apt;


import uni.apt.core.OnlineDB;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static uni.apt.core.InversionCounter.countInversions;

//TODO: fix issues related to stemming

public class test {

    static void submit(float ans){
        //System.out.println(ans);
    }
    static void solve(){
        float start = 0;
        float end = 1;
        float dx = 0.00001f;
        float x = start;
        while (x <= end){
            submit(x);
            x += dx;
        }
    }


    public static void main(String[] args){
        solve();
        if (true) return;

        double chance = 0.006;
        int count = 1000;
        int max_streak = 0;
        int streak = 0;
        double avg = 0;
        for (int i = 0;i < count;i++){
            Random rand = new Random();
            int att = 0;
            while (rand.nextDouble() > chance){
                att++;
                if (att > 80)
                    break;
            }

            if (att == 81){
                streak++;
            }else{
                streak = 0;
            }

            max_streak = Math.max(streak , max_streak);

            avg += att;
            System.out.print(att + " ");
        }

        System.out.println();
        avg = avg / count;
        System.out.println("AVG : " + avg + ", MaxStreak: " + max_streak);
    }
}
