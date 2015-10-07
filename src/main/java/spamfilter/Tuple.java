package spamfilter;

public class Tuple
{
	private String word;
	private double counter;
	
	public Tuple(String word, double counter)
	{
		this.word = word;
		this.counter = counter;
	}
	
	public String getWord()
	{
		return this.word;
	}
	
	public double getCounter()
	{
		return this.counter;
	}
	
	@Override
	public String toString()
	{
		return this.counter +": " + this.word;
	}

}
