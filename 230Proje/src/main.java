import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

public class main {

	public static void main(String[] args) throws IOException{
		
		String fileAsString;// can be the parameter of the Hyp86 object !!
		fileAsString = parser();//whole code as a string
		

		Hyp86 assembly=new Hyp86(fileAsString);
		
		//registerda ters tutuluyo aray tarzýna göre
		// yani sað taraf 0 sol taraf 15 regde
		// biz ax=ah:al yapalým
		// al indexi daha yüksek olucak ama yapcak biþi yok

		assembly.NumberToFourByteHexa("213");
	
	}

	/**
	 * parser takes the input file and parse it to labels
	 * 
	 * @param segments "key" is the name of the label "value" is the block of code
	 *                 associated with the "key" label
	 * @return the whole assembly code as a String
	 */
	private static String parser() throws FileNotFoundException, IOException {

		String fileAsString;
		BufferedReader br = new BufferedReader(new FileReader("src/file.txt"));
		StringBuilder sb = new StringBuilder();
		String line = br.readLine();

		while (line != null) {
			//got rid of comments in Assembly code
			if (line.indexOf(';') != -1) {
				line = line.substring(0, line.indexOf(';'));
			}
			sb.append(line).append("\n");
			line = br.readLine();
		}
		fileAsString = sb.toString(); // this is the whole assembly code as a String

		br.close();
		return fileAsString;
	}
	
	

}
