import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class main {

	// we assumed that there must be 'b' or 'w' in front of square brackets ('[')
	// since Mr. Ozturan answered that way in Piazza.

	public static void main(String[] args) throws IOException {

		String fileAsString;// can be the parameter of the Hyp86 object !!
		fileAsString = parser(args[0]);// whole code as a string
		Hyp86 assembly = new Hyp86(fileAsString);
		assembly.execute();
		
	
	}

	/**
	 * parser takes the input file and parse it to labels
	 * 
	 * @param segments "key" is the name of the label "value" is the block of code
	 *                 associated with the "key" label
	 * @return the whole assembly code as a String
	 */
	private static String parser(String xx) throws FileNotFoundException, IOException {
		String fileAsString;
		BufferedReader br = new BufferedReader(new FileReader(xx));
		StringBuilder sb = new StringBuilder();
		String line = br.readLine();
		while (line != null) {
			// got rid of comments in Assembly code
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
