import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

/**
 * Created by Dan on 01/05/2017.
 */
public class Main {

    public static void main(String[] args) {
        double totalProfit = 0;
        double totalProfitSquared = 0;
        int n = Integer.parseInt(args[9]);
        try {
            PrintWriter writer = new PrintWriter("profitshedging10x.txt", "UTF-8");
            for (int i = 0; i < n; i++) {
                double profit = 0;
                if (i % (n / 10) == 0) {
                    System.out.println(i / (n / 100)+ "% done");
                }
                OptionPricer optionPricer = new OptionPricer(args);
                if (!optionPricer.hedge) {
                    //System.out.println("alsjkdflasjf");
                    profit = optionPricer.calculateFinalProfitOneDay();
                    System.out.println(profit);
                    //profit = optionPricer.calculateFinalProfitNoHedge();
                }
                else {
                    profit = optionPricer.calculateProfit();
                }
                totalProfit += profit;
                writer.print(i + ",");
                writer.println(totalProfit / (double) i);
                totalProfitSquared += profit * profit;
            }
            double eX = totalProfit / n;
            double eX2 = totalProfitSquared / n;
            System.out.println("E(x): " + eX);
            System.out.println("Variance: " + (eX2 - eX * eX));
            writer.close();
        } catch (IOException e){

        }

    }

}
