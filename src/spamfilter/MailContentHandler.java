/**
 * 
 */
package spamfilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.message.SimpleContentHandler;
import org.apache.james.mime4j.stream.BodyDescriptor;

/**
 * @author Lukas
 *
 */
public class MailContentHandler extends SimpleContentHandler
{

	private Stats stats;
	private int mode = 0;
	private int learnMode = 0;
	
	public MailContentHandler(Stats stats)
	{
		this.stats = stats;
	}
	
	public void setStats(Stats stats)
	{
		this.stats = stats;
	}
	
	public void setToHAMMode()
	{
		this.mode = this.stats.getHAMMode();
	}
	
	public void setToSPAMMode()
	{
		this.mode = this.stats.getSPAMMode();
	}
	
	private void read(String text)
	{
		if(this.mode==this.stats.getHAMMode())
		{
			this.stats.readSingleHam(text);
		}
		else
		{
			this.stats.readSingleSpam(text);
		}
	}
	
	@Override
	public void headers(Header arg0) 
	{
//		System.out.println("header: " + arg0);
		
	}
	
	public void body(BodyDescriptor bd, InputStream is)
            throws MimeException, IOException {
		
//        System.out.println("Body detected, contents = "
//            + is + ", header data = " + bd);
		String html = getStringFromInputStream(is);//is.toString();
		String plainText = html.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ");
//		System.out.println(plainText);
		this.read(plainText);
		
		
    }
	
	// convert InputStream to String (source: http://www.mkyong.com/java/how-to-convert-inputstream-to-string-in-java/)
		public static String getStringFromInputStream(InputStream is) {

			BufferedReader br = null;
			StringBuilder sb = new StringBuilder();

			String line;
			try {

				br = new BufferedReader(new InputStreamReader(is));
				while ((line = br.readLine()) != null) {
					sb.append(line);
				}

			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (br != null) {
					try {
						br.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			return sb.toString();

		}
	
//	 public void field(String fieldData) throws MimeException {
//         System.out.println("Header field detected: "
//             + fieldData);
//     }
//	 
//	 public void startMultipart(BodyDescriptor bd)
//	 {
//		 System.out.println("################ " + bd +" #############"); 
//	 }
//	
	
}
