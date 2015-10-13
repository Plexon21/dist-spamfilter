package spamfilter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.apache.james.mime4j.stream.MimeConfig;

import com.sun.net.ssl.internal.www.protocol.https.Handler;

public class MailReader {
    private final String HAM = "ham";
    private final String SPAM = "spam";

    private final String ANLERN = "anlern";
    private final String KALIB = "kallibrierung";
    private final String TEST = "test";

    private Stats stats;
    private final int TOP_N = 10;
    private final double CERTAINTY = 0.9;

    private MailContentHandler handler;
    private MimeStreamParser parser;

    private final int MAX_CALIBRATION_ITERATIONS = 10; //100
    private final double STEPS = 0.00001;// 0.00001;//0.001;

    /**
     * 
     * @param stats Stats for the learningset
     */
    public MailReader(Stats stats) {
        this.stats = stats;

        this.handler = new MailContentHandler(this.stats);
        MimeConfig config = new MimeConfig();
        this.parser = new MimeStreamParser(config);

        parser.setContentHandler(this.handler);
    }

    public void setStats(Stats stats) {
        this.stats = stats;
        this.handler.setStats(stats);

        this.parser.setContentHandler(this.handler);
    }

    public void readHamAnlern() throws IOException {
        this.handler.setToHAMMode();

        int size = this.readMails(this.HAM, this.ANLERN);
        this.stats.setHamSize(size);

        // this.stats.showTopHam(this.TOP_N);
    }

    public void readSpamAnlern() throws IOException {
        this.handler.setToSPAMMode();

        int size = this.readMails(this.SPAM, this.ANLERN);
        this.stats.setSpamSize(size);

        // this.stats.showTopSpam(this.TOP_N);
    }

    /**
     * 
     * @param stats
     * @return probability of correct classified ham mails as ham
     * @throws IOException
     */
    public double readHamKalibration(Stats stats) throws IOException {
        this.handler.setToHAMMode();

        File[] mails = this.getMails(this.HAM, this.KALIB);

        int counter = 0;

        for (File mail : mails) {
            this.readSingleMail(mail);

            // List<Tuple> topN = this.stats.getTopHam(this.TOP_N);
            //
            // String[] topWords = MailReader.tupleToString(topN, this.TOP_N);

//            double hamProbability = stats.calcHam((String[]) stats.getWords().toArray());
          double hamProbability = stats.calcHam(this.stats.getWords());

            if (hamProbability >= this.CERTAINTY) {
                counter++;
            }

            this.stats.clear();
        }

        double percent = (100.0 / mails.length) * counter;

        System.out.println("CALIBRATION (HAM): " + counter + " out of " + mails.length
                + " were classfied correctly as ham. -> " + percent + "%");

        return percent;
    }

    /**
     * 
     * @param stats
     * @return probability of correct classified spam mails as spam
     * @throws IOException
     */
    public double readSpamKalibration(Stats stats) throws IOException {
        this.handler.setToSPAMMode();

        File[] mails = this.getMails(this.SPAM, this.KALIB);

        int counter = 0;

        for (File mail : mails) {
            this.readSingleMail(mail);

            // List<Tuple> topN = this.stats.getTopSpam(this.TOP_N);
            //
            // String[] topWords = MailReader.tupleToString(topN, this.TOP_N);

//            double spamProbability = stats.calcSpam((String[]) stats.getWords().toArray());
            double spamProbability = stats.calcSpam(this.stats.getWords());


            if (spamProbability >= this.CERTAINTY) {
                counter++;
            }

            this.stats.clear();
        }

        double percent = (100.0 / mails.length) * counter;

        System.out.println("CALIBRATION (SPAM): " + counter + " out of " + mails.length
                + " were classfied correctly as spam. -> " + percent + "%");

        return percent;
    }
    public boolean readTest(Stats stats, File mail) throws IOException{
    	this.readSingleMail(mail);

        List<Tuple> topN = this.stats.getTopSpam(this.TOP_N);

        String[] topWords = MailReader.tupleToString(topN, this.TOP_N);

//        double spamProbability = stats.calcSpam((String[]) stats.getWords().toArray());
        double spamProbability = stats.calcSpam(this.stats.getWords());

        if (spamProbability >= this.CERTAINTY) {
            return true;
        }
        return false;
    }
    public void readHamTest(Stats stats) throws IOException {
        this.handler.setToHAMMode();

        File[] mails = this.getMails(this.HAM, this.TEST);

        int counter = 0;

        for (File mail : mails) {
            this.readSingleMail(mail);

            List<Tuple> topN = this.stats.getTopHam(this.TOP_N);

            String[] topWords = MailReader.tupleToString(topN, this.TOP_N);

//            double hamProbability = stats.calcHam((String[]) stats.getWords().toArray());
            double hamProbability = stats.calcHam(this.stats.getWords());

            
            if (hamProbability >= this.CERTAINTY) {
                counter++;
            }

            this.stats.clear();
        }

        double percent = (100.0 / mails.length) * counter;

        System.out.println("TEST (HAM): " + counter + " out of " + mails.length
                + " were classfied correctly as ham. -> " + percent + "%");

    }

    public void readSpamTest(Stats stats) throws IOException {
        this.handler.setToSPAMMode();

        File[] mails = this.getMails(this.SPAM, this.TEST);

        int counter = 0;

        for (File mail : mails) {
            this.readSingleMail(mail);

            List<Tuple> topN = this.stats.getTopSpam(this.TOP_N);

            String[] topWords = MailReader.tupleToString(topN, this.TOP_N);

//            double spamProbability = stats.calcSpam((String[]) stats.getWords().toArray());
            double spamProbability = stats.calcSpam(this.stats.getWords());

            if (spamProbability >= this.CERTAINTY) {
                counter++;
            }

            this.stats.clear();
        }

        double percent = (100.0 / mails.length) * counter;

        System.out.println("TEST (SPAM): " + counter + " out of " + mails.length
                + " were classfied correctly as spam. -> " + percent + "%");

    }

    public void calibrateSpam(Stats stats, double epsilon) throws IOException {
        int counter = 0;

        double percent = this.readSpamKalibration(stats);
        double delta = Double.MAX_VALUE;
        double alpha = 0;

        while (delta > epsilon && counter < this.MAX_CALIBRATION_ITERATIONS) {
            alpha = stats.getHamAlpha();
            stats.setHamAlpha(alpha - this.STEPS);

            double percent2 = this.readSpamKalibration(stats);

            delta = Math.abs(percent - percent2); // FIXME
            if (delta == 0) {
                delta = 2 * epsilon;
            }

            percent = percent2;

            counter++;
        }

        System.out.println("Best alpha for spam: " + alpha + " -> " + percent + "%");
    }

    public void calibrateHam(Stats stats, double epsilon) throws IOException {
        int counter = 0;

        double percent = this.readHamKalibration(stats);
        double delta = Double.MAX_VALUE;
        double alpha = 0;

        while (delta > epsilon && counter < this.MAX_CALIBRATION_ITERATIONS) {
            alpha = stats.getSpamAlpha();
            // System.out.println("SPAM ALPHA: " + alpha);
            stats.setSpamAlpha(alpha - this.STEPS);

            double percent2 = this.readHamKalibration(stats);

            delta = Math.abs(percent - percent2); // FIXME
            percent = percent2;
            if (delta == 0) {
                delta = 2 * epsilon;
            }

            // System.out.println("calib ham " + counter + " delta: " + delta +" epsilon: " +
            // epsilon);

            counter++;
        }

        // System.out.println("Delta>e: " + delta +" > " + epsilon + " counter<max_it: " + counter +
        // "<" + this.MAX_CALIBRATION_ITERATIONS);
        System.out.println("Best alpha for ham: " + alpha + " -> " + percent + "%");
    }

    private void readSingleMail(File mail) throws IOException {
        InputStream inStream = new FileInputStream(mail);

        try {
            this.parser.parse(inStream);
        } catch (MimeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            inStream.close();
        }
    }

    private File[] getMails(String type, String function) {
        return new File(type + "-" + function).listFiles();
    }

    private int readMails(String type, String function) throws IOException {
        File[] files = getMails(type, function);

        for (File f : files) {
            this.readSingleMail(f);
        }

        return files.length;
    }

    // ////////
    // MAIL //
    // ////////

    public static void main(String[] args) throws IOException {
        Stats stats = new Stats();
        Stats kalibStats = new Stats();
        Stats testStats = new Stats();

        int max = 10;

        MailReader reader = new MailReader(stats);

        // Learn
        reader.readHamAnlern();
        reader.readSpamAnlern();

        // Test
        System.out.println("**** BEFORE CALIBRATION ****");
        reader.setStats(testStats);

        reader.readHamTest(stats);
        reader.readSpamTest(stats);

        // Calibration
        reader.setStats(kalibStats);

        double epsilon = 0.01;
        reader.calibrateHam(stats, epsilon);
        reader.calibrateSpam(stats, epsilon);
        //
        // reader.readHamKalibration(stats);
        // reader.readSpamKalibration(stats);

        // Test
        System.out.println("**** AFTER CALIBRATION ****");
        reader.setStats(testStats);

        reader.readHamTest(stats);
        reader.readSpamTest(stats);
        
        System.out.println("Enter Filename for E-Mail to check");
        Scanner scanner = new Scanner(System.in);
        String filename = scanner.nextLine();
        if(reader.readTest(stats, new File(filename))){
        	System.out.println("Spam");
        }
        else{
        	System.out.println("Ham");
        }
        
        scanner.close();
        
        
        

        // // reader.readSpamKalibration();
        // // reader.readSpamTest();
        //
        // int max = 10;
        // List<Tuple> nHam = stats.getTopHam(max);
        //
        // String[] hams = new String[max];
        //
        // int i=0;
        // for(Tuple tuple:nHam)
        // {
        // hams[i] = tuple.getWord();
        // i++;
        // }
        //
        // System.out.println("hams: " + stats.calcSpam(hams));
    }

    public static String[] tupleToString(List<Tuple> tuples, int max) {
        String[] words = new String[max];

        int i = 0;
        for (Tuple tuple : tuples) {
            words[i] = tuple.getWord();
            i++;
        }

        return words;
    }

}
