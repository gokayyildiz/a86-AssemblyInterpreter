import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

public class main {

	public static void main(String[] args) throws IOException {

		HashMap<String, String> segments = new HashMap<>();// label-code mapping happens here
		String fileAsString;// can be the parameter of the Hyp86 object !!
		fileAsString = parser(segments);// whole code as a string
		Hyp86 assembly = new Hyp86(fileAsString);
		assembly.bx[0]='0';
		assembly.bx[1]='3';
		assembly.bx[2]='e';
		assembly.bx[3]='8';
		// registerda ters tutuluyo aray tarzýna göre
		// yani sað taraf 0 sol taraf 15 regde
		// biz ax=ah:al yapalým
		// al indexi daha yüksek olucak ama yapcak biþi yok
assembly.memory[2000]='b';
assembly.memory[2001]='e';
assembly.memory[2002]='w';
assembly.memory[2003]='s';
assembly.mov("ax", "[bx]");
		assembly.NumberToFourByteHexa("213");
	
	}

	/**
	 * parser takes the input file and parse it to labels
	 * 
	 * @param segments "key" is the name of the label "value" is the block of code
	 *                 associated with the "key" label
	 * @return the whole assembly code as a String
	 */
	private static String parser(HashMap<String, String> segments) throws FileNotFoundException, IOException {

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

		Scanner scanCode = new Scanner(fileAsString);

		String theLine = scanCode.nextLine();
		while (scanCode.hasNextLine()) {
			// theLine = scanCode.nextLine();
			if (theLine.indexOf(":") != -1) {
				theLine = theLine.trim();
				theLine = theLine.substring(0, theLine.length() - 1);
				String key = theLine;
				StringBuilder sb2 = new StringBuilder();
				String build = scanCode.nextLine();
				while (build.indexOf(":") == -1) {
					sb2.append(build).append("\n");
					if (scanCode.hasNextLine()) {
						build = scanCode.nextLine();
					} else {
						break;
					}
				}
				String value = sb2.toString();
				segments.put(key, value);
				if (build.indexOf(':') != -1) {
					theLine = build;
					continue;
				}
			}
			if (scanCode.hasNextLine()) {
				theLine = scanCode.nextLine();
			}
		}
		return fileAsString;
	}

}
