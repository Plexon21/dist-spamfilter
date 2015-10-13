package spamfilter;

import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Word cache for spam and ham words.
 * 
 * @author Lukas Keller
 *
 */
public class Stats {
    /**
     * cache
     */
    private HashMap<String, double[]> words;

    private final int SPAM = 0;
    private final int HAM = 1;

    private int SPAM_SIZE = 0;
    private int HAM_SIZE = 0;

    private final double MIN_ALPHA = 0.0000000001;

    /**
     * Learn, calibration or test mode
     */
    private int MODE;

    private final int LEARN = 0;
    private final int KALIB = 1;
    private final int TEST = 2;

    private double[] alpha;

    private double default_alpha = 0.000001;

    public Stats() {
        this.words = new HashMap<String, double[]>();
        this.MODE = this.LEARN;

        this.alpha = new double[2];
        this.alpha[0] = default_alpha;
        this.alpha[1] = default_alpha;
    }

    public void setLearnMode() {
        this.MODE = this.LEARN;
    }

    public void setKalibrationMode() {
        this.MODE = this.KALIB;
    }

    public void setTestMode() {
        this.MODE = this.TEST;
    }

    /**
     * Adds a word to the spam cache
     * 
     * @param spam
     */
    public void addSpam(String spam) {
        this.addWord(spam, this.SPAM);
    }

    /**
     * Adds a word to the ham cache
     * 
     * @param ham
     */
    public void addHam(String ham) {
        this.addWord(ham, this.HAM);
    }

    public void setHamSize(int ham) {
        this.HAM_SIZE = ham;
    }

    /**
     * 
     * @param spam size of training-mail set
     */
    public void setSpamSize(int spam) {
        this.SPAM_SIZE = spam;
    }

    private double[] getDefault() {

        return this.alpha; // TODO: anstelle [1,1]
    }

//    public Set<String> getWords() {
//        return this.words.keySet();
//    }
    
    public String[] getWords()
    {
    	Set<String> set = this.words.keySet();
    	return set.toArray(new String[set.size()]);
    }

    /**
     * clears the whole cache. Only used after reading a single test or calibration mail.
     */
    public void clear() {
        this.words.clear();
    }

    /**
     * Calculates the probability P(word|SPAM)
     * 
     * @param word
     * @return probability P(word|SPAM)
     */
    public double calcSpam(String word) {
        // System.out.println("SPAM_SIZE" + this.SPAM_SIZE);

        double[] values = this.words.getOrDefault(word, this.getDefault());

        double value = values[this.SPAM];

        if (value == 0) {
            value = this.alpha[this.SPAM];
            // System.out.println("MISSING SPAM VALUE: " + word + " -> " + value);
        }

        return value / (double) this.SPAM_SIZE;
    }

    /**
     * Calculates the probability P(word|HAM)
     * 
     * @param word
     * @return probability P(word|HAM)
     */
    public double calcHam(String word) {
        // return this.words.getOrDefault(word, this.getDefault())[this.HAM]/(double) this.HAM_SIZE;

        double[] values = this.words.getOrDefault(word, this.getDefault());

        double value = values[this.HAM];

        if (value == 0) {
            value = this.alpha[this.HAM];
//            System.out.println("MISSING HAM VALUE: " + word + " -> " + value);
        }

        return value / (double) this.HAM_SIZE;
    }

    /**
     * Calculates the probability P(HAM|word[0] & word[1] & ... word[n])
     * 
     * @param word
     * @return probability P(HAM|word[0] & word[1] & ... word[n])
     */
    public double calcHam(String[] words) {

        BigDecimal a = new BigDecimal(1);
//        double b1 = 1;
//        double b2 = 1;
        
        BigDecimal B1 = new BigDecimal(1);
        BigDecimal B2 = new BigDecimal(1);
        
        BigDecimal hamProb;
        for (String word : words) {
            hamProb = new BigDecimal(this.calcHam(word));
            a = a.multiply(hamProb);
//            b1 *= this.calcSpam(word);
//            b2 *= hamProb.doubleValue();
//            
            B1 = B1.multiply(new BigDecimal(this.calcSpam(word)));
            B2=B2.multiply(hamProb);
//            System.out.println("hamP: " + "" +" a: " + a +" b1: " + b1 +" b2: " + b2);
//            System.out.println("b1: " + b1 + " b2: " + b2 +" B1: " + B1 +" B2: " + B2 );


        }
        
//        System.out.println("B1: " + b1 + "B2: " + b2);
         
        //return a.divide(new BigDecimal(b1 + b2)).doubleValue();
        BigDecimal divisor = B1.add(B2);
        
//        System.out.println("A: " + a);
//        System.out.println(" B1: " + B1 +" B2: " + B2 +" divisor: " + divisor);
        BigDecimal result = a.divide(divisor,2,RoundingMode.HALF_UP);
        
        return result.doubleValue();
    }

    /**
     * Calculates the probability P(SPAM|word[0] & word[1] & ... word[n])
     * 
     * @param word
     * @return probability P(SPAM|word[0] & word[1] & ... word[n])
     */
    public double calcSpam(String[] words) {
        BigDecimal a = new BigDecimal(1);
//        double b1 = 1;
//        double b2 = 1;
        
        BigDecimal B1 = new BigDecimal(1);
        BigDecimal B2 = new BigDecimal(1);
        
        BigDecimal spamProb;
        for (String word : words) {
            spamProb = new BigDecimal(this.calcSpam(word));
            a = a.multiply(spamProb);
//
//            b1 *= this.calcHam(word);
//            b2 *= spamProb.doubleValue();
            
            B1 = B1.multiply(new BigDecimal(this.calcHam(word)));
            B2 = B2.multiply(spamProb);
        }
        
        BigDecimal divisor = B1.add(B2);
        BigDecimal result = a.divide(divisor,2,RoundingMode.HALF_UP);
        
        return result.doubleValue();
        
        
//        return a.divide(new BigDecimal(b1 + b2)).doubleValue();
    }

    /**
     * Add a single word to spam or ham and increase its counter
     * 
     * @param word
     * @param index spam if {@link #SPAM}; ham if {@link #HAM}
     */
    private void addWord(String word, int index) {
        double[] counter = this.words.getOrDefault(word, new double[2]);
        counter[index]++;
        this.words.put(word, counter);
    }

    /**
     * Calculates the {@literal n} most frequent spam words in a mail. Only works after reading a
     * single mail!
     * 
     * @param max number of the most frequent words
     * @return sorted list of the {@literal n} most frequent words
     */
    public List<Tuple> getTopSpam(int max) {
        return this.toNTupleList(this.getTop(this.SPAM), max);
    }

    /**
     * Calculates the {@literal n} most frequent ham words in a mail. Only works after reading a
     * single mail!
     * 
     * @param max number of the most frequent words
     * @return sorted list of the {@literal n} most frequent words
     */
    public List<Tuple> getTopHam(int max) {
        return this.toNTupleList(this.getTop(this.HAM), max);
    }

    /**
     * Transforms a sortedMap of <Double,List<String> into a sorted list of tuples
     * 
     * @param tree
     * @param top
     * @return
     */
    private List<Tuple> toNTupleList(SortedMap<Double, List<String>> tree, int top) {
        List<Tuple> list = new ArrayList<Tuple>();

        int i = 0;

        for (Entry<Double, List<String>> entry : tree.entrySet()) {
            Double count = entry.getKey();

            for (String word : entry.getValue()) {
                if (i < top) {
                    list.add(new Tuple(word, count));

                    i++;
                } else {
                    return list;
                }
            }
        }

        return list;
    }

    /**
     * sorts all the words
     * 
     * @param index spam if {@link #SPAM}; ham if {@link #HAM}
     * @return a sorted map of <Double, List<String>> of all the words
     */
    private SortedMap<Double, List<String>> getTop(int index) {
        SortedMap<Double, List<String>> tree = new TreeMap<Double, List<String>>(Collections.reverseOrder());

        for (Entry<String, double[]> entry : this.words.entrySet()) {
            double counter = entry.getValue()[index];
            String word = entry.getKey();

            List<String> list = tree.getOrDefault(counter, new ArrayList<String>());
            list.add(word);

            tree.put(counter, list);
        }

        return tree;
    }

    public void showTopSpam(int max) {
        this.showTop(max, 0);
    }

    public void showTopHam(int max) {
        this.showTop(max, 1);
    }

    private void showTop(int max, int index) {
        List<Tuple> top = null;

        if (index == 0) {
            System.out.println("** TOP " + max + " spam **");
            top = this.getTopSpam(max);
        } else if (index == 1) {
            System.out.println("** TOP " + max + " ham **");
            top = this.getTopHam(max);
        } else {
            System.err.println("Unbekannter Modus...");
            System.exit(-1);
        }

        for (Tuple tuple : top) {
            System.out.println(tuple + " ham: " + this.calcHam(tuple.getWord()) + " spam: "
                    + this.calcSpam(tuple.getWord()));
        }
    }

    /**
     * Adds a spam text to the cache.
     * 
     * @param rawText whole text of a single spam mail
     */
    public void readSingleSpam(String rawText) {
        Collection<String> words = this.readSingle(rawText);

        this.saveWords(words, 0);
    }

    /**
     * Adds a ham text to the cache.
     * 
     * @param rawText whole text of a single spam mail
     */
    public void readSingleHam(String rawText) {
        Collection<String> words = this.readSingle(rawText);

        this.saveWords(words, 1);
    }

    private void saveWords(Collection<String> words, int index) {
        if (index == 0) {
            for (String word : words) {
                this.addSpam(word);
            }
        } else if (index == 1) {
            for (String word : words) {
                this.addHam(word);
            }
        } else {
            System.err.println("Unbekannter Modus...");
            System.exit(-1);
        }
    }

    private String cleanText(String rawText) {
        // TODO: remove html

        // Punctuation:
        String plainText = rawText.replaceAll("\\p{Punct}+", ""); // Source:
                                                                  // http://stackoverflow.com/a/17531480
        return rawText.toLowerCase();
    }

    private Collection<String> readSingle(String rawText) {
        if (this.MODE == this.LEARN) {
            return this.readSingleLearn(rawText);
        } else if (this.MODE == this.KALIB) {
            return this.readSingleKalib(rawText);
        } else if (this.MODE == this.TEST) {
            return this.readSingleTest(rawText);
        } else {
            System.err.println("Unbekannter modus...");
            System.exit(-2);
        }

        return Collections.EMPTY_LIST;
    }

    private List<String> readSingleKalib(String rawText) {
        String[] singleWords = this.createWords(rawText);

        List<String> words = new ArrayList<String>();

        for (String word : singleWords) {
            words.add(word);
        }

        return words;
    }

    private List<String> readSingleTest(String rawText) {
        String[] singleWords = this.createWords(rawText);

        List<String> words = new ArrayList<String>();

        for (String word : singleWords) {
            words.add(word);
        }

        return words;
    }

    private Set<String> readSingleLearn(String rawText) {
        Set<String> words = new HashSet<String>();

        String[] singleWords = this.createWords(rawText);

        for (String word : singleWords) {
            words.add(word);
        }

        return words;
    }

    private String[] createWords(String rawText) {
        String plainText = this.cleanText(rawText);
        String[] singleWords = plainText.split("\\s"); // splitt SPACE

        return singleWords;
    }

    public int getHAMMode() {
        return this.HAM;
    }

    public int getSPAMMode() {
        return this.SPAM;
    }

    public static void main(String[] args) {
        // Stats s = new Stats();
        // s.addHam("Hallo");
        // s.addHam("Hi");
        // s.addHam("Hi");
        // s.addHam("Hallo");
        // s.addHam("Hi");
        // s.addHam("Hallo");
        // s.addHam("Bye");
        // s.addHam("Java");
        // s.addHam("Java");
        //
        // s.addSpam("Welt");
        // s.addSpam("Hallo");
        // s.addSpam("Welt");
        // s.addSpam("Spam");
        //
        // s.showTopHam(6);
        // s.showTopSpam(6);

        Stats s = new Stats();

        String spam1 = "Hallo Welt. Das ist ein Spam Mail.";
        String spam2 = "Hallo XY. Das ist lustig.";
        String spam3 = "Suchst du nach einem Spam Mail?";

        String ham1 = "Guten Tag. Wir haben ihre Mail erhalten";
        String ham2 = "Sehr geehrter Herr Einstein. Ihre Theorie ist lustig";
        String ham3 = "Lieber Angestellte. Nach was suchst du denn?";

        s.readSingleHam(ham1);
        s.readSingleHam(ham2);
        s.readSingleHam(ham3);

        s.readSingleSpam(spam1);
        s.readSingleSpam(spam2);
        s.readSingleSpam(spam3);

        int max = 40;

        s.showTopHam(max);
        s.showTopSpam(max);

    }

    public double getSpamAlpha() {
        // TODO Auto-generated method stub
        return this.alpha[this.SPAM];
    }

    public double getHamAlpha() {
        return this.alpha[this.HAM];
    }

    public void setSpamAlpha(double alpha) {
        this.alpha[this.SPAM] = Math.max(alpha, this.MIN_ALPHA);
    }

    public void setHamAlpha(double alpha) {
        this.alpha[this.HAM] = Math.max(alpha, this.MIN_ALPHA);
        ;
    }

}
