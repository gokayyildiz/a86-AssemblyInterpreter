import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

public class main {

	// we assumed that there must be 'b' or 'w' in front of square brackets ('[')
	// since Mr. Ozturan answered that way in Piazza.

	public static void main(String[] args) throws IOException {

		String fileAsString;// can be the parameter of the Hyp86 object !!
		fileAsString = parser();// whole code as a string

		Hyp86 assembly = new Hyp86(fileAsString);
		assembly.memory[500] = "0a34";
		assembly.ax[0] = '2';
		assembly.ax[1] = 'f';
		assembly.bp[0] = '3';
		assembly.bp[1] = '3';
		assembly.bp[2] = 'c';
		assembly.bx[0] = '3';
		assembly.bx[1] = 'b';
		assembly.bx[2] = '6';
		assembly.bx[3] = 'a';
		// assembly.add("al", "0a234");
		// assembly.add("al", "ax");
		assembly.inc("al");
		assembly.add("al", "ah");
		assembly.add("al", "var2");
		assembly.add("al", "[500]");
		assembly.add("al", "offset var1");
		assembly.inc("ah");
		assembly.add("ah", "ah");
		// assembly.mov("vax", "0a52dh");
//		assembly.add("al", "var1");
		assembly.add("ah", "var2");
		assembly.add("ah", "[500]");
		assembly.add("ah", "offset var1");
		// assembly.inc("w[500d]");
		assembly.add("ah", "1");
		// assembly.add("al", "1000h");

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
