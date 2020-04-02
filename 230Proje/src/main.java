import java.io.BufferedReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

//TODO
//add xx,char - add var,xx
//sub xx,char - sub var,xx
//mov xx,char kısımları kalmış!!!
public class main {

	// we assumed that there must be 'b' or 'w' in front of square brackets ('[')
	// since Mr. Ozturan answered that way in Piazza.

	public static void main(String[] args) throws IOException {

		
		String fileAsString;// can be the parameter of the Hyp86 object !!
		fileAsString = parser();// whole code as a string

		Hyp86 assembly = new Hyp86(fileAsString);
		System.out.print("");
		assembly.execute();
		assembly.memory[500] = "0a";
		assembly.memory[4660] = "ab";
		assembly.mov("w[500d]","5aabh");
		assembly.mov("b[500d]","2bh");
		assembly.shr("ax", "1");
		assembly.or("ax", "2f11");
		assembly.xor("bp", "500d");
		assembly.and("bx", "508d");
		assembly.or("si", "500d");
		assembly.xor("al", "25");
	//	assembly.and("w[bp]", "\'a\'");
		assembly.or("ax", "var1");
		assembly.xor("bx", "w var2");
		assembly.and("w[500]", "ax");
		// assembly.add("w[508]", "[bp]");
		// assembly.sub("b[512]", "var2");
		assembly.sub("w[1f6]", "offset var3");
		// assembly.sub("w[500]", "var1");
		assembly.sub("w[1f6]", "045ah");
		// assembly.sub("b[504]", "b[si]");
		assembly.mov("cx", "var1");
		assembly.mov("w var1", "b var2");
		assembly.mov("w var2", "w[1234h]");
		assembly.mov("cx", "var1");
		assembly.mov("w var1", "[1234h]");
		assembly.mov("cx", "var1");
		assembly.mov("w var1", "offset var1");
		assembly.mov("w var1", "ax");
		assembly.mov("w var1", "[bx]");
		assembly.mov("w var1", "b[1234h]");
		assembly.mov("w var1", "var2");
		assembly.mov("w var1", "al");
		assembly.mov("w var1", "255");
		assembly.mov("w var1", "257");

		// assembly.add("al", "0a234");
		// assembly.add("al", "ax");
		assembly.mov("b var2 ", "21");
		// assembly.mov("al","124h");
		assembly.mov("w var1 ", "124d");
		assembly.mov("w var1 ", "0124");
		// assembly.add("b[var2]", "1f24");
//		assembly.add("w[500h]", "ah");
		assembly.mov("w var1", "var2");
		assembly.mov("al", "[500]");
		assembly.mov("al", "offset var1");
		
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
