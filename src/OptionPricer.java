import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.correlation.Covariance;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

/**
 * Created by Dan on 01/05/2017.
 */
public class OptionPricer {

    private double muCorrelated;
    private double sigmaCorrelated;
    private double muUntraded;
    private double sigmaUntraded;
    private double startPriceCorrelated;
    private double startPriceUntraded;
    private int numDays;
    private int hedgesPerDay;
    private double[] untradedPrices;
    private double[] correlatedPrices;
    private double[] gs;
    private double[] dzc;
    private double[] dze;
    private double[] d1s;
    private double[] phis;
    private static double[] NORMS;
    private static double DT;
    private static double INTEREST = 0.05;
    private static double STRIKE = 300;
    private double w;
    private double bec;
    public boolean hedge;


    public OptionPricer(String[] args) {
        this.muCorrelated = Double.parseDouble(args[0]);
        this.sigmaCorrelated = Double.parseDouble(args[1]);
        this.muUntraded = Double.parseDouble(args[2]);
        this.sigmaUntraded = Double.parseDouble(args[3]);
        this.startPriceUntraded = Double.parseDouble(args[4]);
        this.startPriceCorrelated = Double.parseDouble(args[5]);
        this.numDays = Integer.parseInt(args[6]);
        this.hedgesPerDay = Integer.parseInt(args[7]);
        this.hedge = Boolean.parseBoolean(args[8].split("=")[1]);
        DT = 1.0 / (this.numDays * this.hedgesPerDay);
        this.gs = new double[this.numDays * this.hedgesPerDay];
        this.d1s = new double[this.numDays * this.hedgesPerDay];
        this.phis = new double[this.numDays * this.hedgesPerDay];
        NORMS = new double[this.numDays * this.hedgesPerDay];
        try {
            makeRandomNormals();
            setRandoms();
        } catch (Exception e) {

        }
        this.setuntradedPrices();
        this.setcorrelatedPrices();
        this.bec = bec();
        this.w = this.muUntraded - this.bec*(this.muCorrelated - INTEREST);
        this.setD1s();
        this.setGs();
        this.setPhis();

    }

    private void setPhis() {
        NormalDistribution distribution = new NormalDistribution(0, 1);
        for (int i = 0; i < this.numDays; i++) {
            this.phis[i] = distribution.cumulativeProbability(this.d1s[i]) * Math.exp((this.w - INTEREST) * (1 - i * DT)) * this.untradedPrices[i] * this.bec;
        }
    }

    private void setuntradedPrices() {
        this.untradedPrices = new double[this.numDays * this.hedgesPerDay];
        this.untradedPrices[0] = this.startPriceUntraded;
        for (int i = 1; i < this.untradedPrices.length; i++) {
            this.untradedPrices[i] = this.untradedPrices[i - 1] *
                    Math.exp(
                            (this.muUntraded - 0.5 * this.sigmaUntraded * this.sigmaUntraded) * DT
                                    + this.sigmaUntraded * Math.sqrt(DT) * NORMS[i]
                    );
        }
    }

    private void setcorrelatedPrices() {
        this.correlatedPrices = new double[this.numDays * this.hedgesPerDay];
        this.correlatedPrices[0] = this.startPriceCorrelated;
        for (int i = 1; i < this.correlatedPrices.length; i++) {
            this.correlatedPrices[i] = this.correlatedPrices[i - 1] *
                    Math.exp(
                            (this.muCorrelated - 0.5 * this.sigmaCorrelated * this.sigmaCorrelated) * DT
                                    + this.sigmaCorrelated * Math.sqrt(DT) * NORMS[i]
                    );
        }
    }

    public double bec() {
        this.dze = new double[this.numDays * this.hedgesPerDay];
        this.dzc = new double[this.numDays * this.hedgesPerDay];
        for (int i = 1; i < this.numDays * this.hedgesPerDay; i++) {
            this.dze[i] = (((this.untradedPrices[i] - this.untradedPrices[i-1]) - this.muUntraded * this.untradedPrices[i] * DT)/ (this.sigmaUntraded * this.untradedPrices[i]));
            this.dzc[i] = (((this.correlatedPrices[i] - this.correlatedPrices[i-1]) - this.muCorrelated * this.correlatedPrices[i] * DT)/ (this.sigmaCorrelated * this.correlatedPrices[i]));
        }
        Covariance cov = new Covariance();
        double pec = cov.covariance(this.dze, this.dzc) / StatUtils.variance(this.dzc);
        return pec * this.sigmaUntraded / this.sigmaCorrelated;
    }

    public double calculateOptionPrice(int day) {
        double d1 = this.d1s[day];
        double d2 = (Math.log(this.untradedPrices[day] / STRIKE) + (this.w - (0.5)*this.sigmaUntraded*this.sigmaUntraded)*(1- day * DT))
                / (this.sigmaUntraded * (1 - day * DT));
        NormalDistribution distribution = new NormalDistribution(0, 1);
        double V = this.startPriceUntraded * Math.exp((w - INTEREST) * (1 - day * DT)) * distribution.cumulativeProbability(d1)
                - STRIKE * Math.exp(-1 * INTEREST * (1 - day * DT)) * distribution.cumulativeProbability(d2);
        return V;
    }

    public double calculateFinalProfitOneDay() {
        double d1 = (Math.log((this.untradedPrices[0] / STRIKE)) + (this.w + (0.5)*this.sigmaUntraded*this.sigmaUntraded))
                / (this.sigmaUntraded * (1 - 0 * DT));
        NormalDistribution distribution = new NormalDistribution(0,1);
        double phi = distribution.cumulativeProbability(d1) * Math.exp((this.w - INTEREST)) * this.untradedPrices[0] * this.bec;
        double units = phi / this.correlatedPrices[0];
        double V = calculateOptionPrice(0);
        double profit =  Math.min(0, STRIKE - this.untradedPrices[this.untradedPrices.length - 1]) + units * this.correlatedPrices[this.correlatedPrices.length - 1] - (Math.abs(V - phi))*Math.exp(INTEREST);
        return profit;
    }

    public double calculateFinalProfitNoHedge() {
        double V = calculateOptionPrice(0);
        double profit = V + Math.min(0, STRIKE-this.untradedPrices[this.untradedPrices.length - 1]);
        return profit;
    }

    public void setGs() {
        this.gs[0] = calculateOptionPrice(0);
        for (int i = 1; i < this.numDays; i++) {
            double phi = this.phis[i];
            double dG = (((this.gs[i - 1] - phi) * INTEREST) + phi * this.muCorrelated) * DT + (phi * this.sigmaCorrelated * this.dzc[i - 1]);
            this.gs[i] = this.gs[i - 1] + dG;
        }
    }

    public void setD1s(){
        for (int i = 0; i < this.numDays * this.hedgesPerDay; i++) {
            this.d1s[i] = (
                    Math.log(this.untradedPrices[i] / STRIKE) +
                            (this.w + 0.5 * this.sigmaUntraded * this.sigmaUntraded) * (1 - i * DT)
            ) / (this.sigmaUntraded * (1 - i * DT));
        }
    }

    public double calculateProfit() {
        double profit = 0;
        double costOfBorrowing = 0;
        double profitFromCorrelated = 0;
        //Not calculating the profitFromCorrelated correctly.
        for (int i = 1; i < numDays * this.hedgesPerDay; i++) {
            double diffCash = (this.gs[i] - this.phis[i]) - (this.gs[i - 1] - this.phis[i - 1]);
            costOfBorrowing += diffCash * INTEREST * DT;
            profitFromCorrelated += (this.phis[i] - this.phis[i - 1]) / this.correlatedPrices[i];
            profit += ((this.phis[i] - this.phis[i - 1]) / this.correlatedPrices[i]) - diffCash * INTEREST * DT;
        }
        return profit;
    }


    public void printPrices() {
        for (int i = 0; i < this.numDays; i++) {
            System.out.println("Xe(" + i + ")" + " = " + this.untradedPrices[i]);
            System.out.println("Xc(" + i + ")" + " = " + this.correlatedPrices[i]);
        }
    }

    private void setRandoms() throws Exception {

        BufferedReader br = new BufferedReader(new FileReader("/Users/Dan/IdeaProjects/TestingLuenberger/randoms.txt"));
        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < this.numDays * this.hedgesPerDay; i++) {
                String line = br.readLine();
                NORMS[i] = Double.parseDouble(line);
            }

        }
        finally {
            br.close();
        }
    }

    private void makeRandomNormals() {
        Random random = new Random();
        try{
            PrintWriter writer = new PrintWriter("randoms.txt", "UTF-8");
            for (int i = 0; i < this.numDays * this.numDays; i ++) {
                writer.println(random.nextGaussian());
            }
            writer.close();
        } catch (IOException e) {
            // do something
        }
    }
}
