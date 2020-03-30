import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

//variable önünde w olduğu veya b olduğu sürece typeı hiç önemli değil.
//..
public class Hyp86 {

	/*
	 * labels, <label, index of where the label's instruction starts>
	 * instructionList, to see is the given label a kind of instruction variables,
	 * array of variables defined
	 */
	HashMap<String, Integer> labels = new HashMap<>();
	ArrayList<String> instructionList = new ArrayList<>();
	ArrayList<Variable> variables = new ArrayList<>();
	String[] memory = new String[64 * 1024];

	char[] di = new char[4];
	char[] si = new char[4];
	char[] bp = new char[4];

	char[] ax = new char[4]; // ah:al seklinde tutuyoruz. ilk 2 indis ah, son 2 indis al
	char[] bx = new char[4];
	char[] cx = new char[4];
	char[] dx = new char[4];

	boolean ZF = false;
	boolean AF = false;
	boolean CF = false;
	boolean OF = false;
	boolean SF = false;

	private int instrustionAreaEnd = -1;
	int numberOfInstructions = 1; // 1 since Int 20h absolutely will be there.
	String SP = "FFFE"; // stack pointer , memoryye erisirken hexadan decimale cevircez
	int MP = 0;// memory Pointer

	/**
	 * add instructions in an order to the memory until int 20h comes get rid of
	 * commas at instructions, finally have "mov ax bx" instead of "mov ax,bx"
	 * 
	 * @param fileAsString
	 */
	Hyp86(String fileAsString) {

		for (int i = 0; i < 4; i++) {// initialize registers to zero
			di[i] = '0';
			bp[i] = '0';
			si[i] = '0';
			dx[i] = '0';
			cx[i] = '0';
			bx[i] = '0';
			ax[i] = '0';

		}
		fillInstructions();
		fileAsString = fileAsString.toLowerCase();

		Scanner scanner = new Scanner(fileAsString);
		String line;
		Scanner token;
		int indexCursor = 0;
		String label = "";
		boolean int20hCame = false;
		while (scanner.hasNextLine()) {
			line = scanner.nextLine();
			token = new Scanner(line);
			String first = token.next();

			if (!int20hCame && instructionList.contains(first)) {// means instruction
				numberOfInstructions++; // Veyis add this
				if (!label.equals("")) {
					labels.put(label, indexCursor);
					label = "";
				}
				//
				// memory[6k+1] 6k+2 ye koyma işlemi burda olucak sanırım
				// @veyis add wrote these comments20
				if (line.indexOf(',') != -1) {
					int temo = line.indexOf(',');
					String temp = line.substring(0, temo) + " " + line.substring(temo + 1, line.length());
					line = temp;
				}
				//
				//
				if (first.equals("int") && token.next().equals("20h")) {
					int20hCame = true;
				}
				memory[indexCursor] = line;
				indexCursor += 6;

			}
			if (line.indexOf(":") != -1) {// means label
				label = line.trim().substring(0, line.length() - 1);
				continue;
			}

			if (line.indexOf("dw") != -1 || line.indexOf("db") != -1) {// variable definition

				if (first.equals("dw")) {
					variables.add(new Variable(label, 0, token.next(), true));
				} else if (first.equals("db")) {
					variables.add(new Variable(label, 0, token.next(), false));
				} else {
					if (token.next().equals("dw")) {
						variables.add(new Variable(first, 0, token.next(), true));
					} else {
						variables.add(new Variable(first, 0, token.next(), false));
					}
				}

			}

			token.close();
			label = "";

		}
		Variable x;
		for (int i = 0; i < variables.size(); i++) {
			x = variables.get(i);
			if (x.type == true) {
				memory[indexCursor] = x.name;
				x.memoryIndex = indexCursor;
				indexCursor += 2;
			} else {
				memory[indexCursor] = x.name;
				x.memoryIndex = indexCursor;
				indexCursor += 1;
			}

		}

		scanner.close();

	}

	/**
	 * push allows pushing a register, memory address, variable, or number
	 * 
	 * @param reg
	 */

	public void push(String reg) {
		int index = Integer.parseInt(SP, 16);

		if (reg.equals("ax")) {
			memory[index] = "" + ax[0] + ax[1] + ax[2] + ax[3];

		} else if (reg.equals("bx")) {
			memory[index] = "" + bx[0] + bx[1] + bx[2] + bx[3];

		} else if (reg.equals("cx")) {
			memory[index] = "" + cx[0] + cx[1] + cx[2] + cx[3];

		} else if (reg.equals("dx")) {
			memory[index] = "" + dx[0] + dx[1] + dx[2] + dx[3];

		} else if (reg.equals("di")) {
			memory[index] = "" + di[0] + di[1] + di[2] + di[3];

		} else if (reg.equals("si")) {
			memory[index] = "" + si[0] + si[1] + si[2] + si[3];

		} else if (reg.equals("bp")) {
			memory[index] = "" + bp[0] + bp[1] + bp[2] + bp[3];

		} else {

			int a = 0;
			for (int i = 0; i < variables.size(); i++) {
				if (variables.get(i).getName().equals(reg) && variables.get(i).isType()) {
					memory[index] = variables.get(i).data;
					break;
				}

				a++;
			}
			if (a == variables.size()) {
				if (reg.indexOf('[') != -1) {
					reg = reg.substring(1, reg.length() - 1);

					if (instrustionAreaEnd < Integer.parseInt(NumberToFourByteHexa(reg), 16)) {
						memory[index] = memory[Integer.parseInt(NumberToFourByteHexa(reg), 16)];
					} else {
						System.out.println("don't try to reach instruction area");
						return;
					}

				} else {

					memory[index] = NumberToFourByteHexa(reg);
				}
			}

		}

		SP = NumberToFourByteHexa("" + (index - 2));
	}

	/**
	 * shift left works properly for registers but cannot control memory or variable
	 * case due to not completed "move operator"
	 * 
	 * @param xx
	 * @param number
	 */
	public void shl(String xx, String number) {
		int numero;
		if (number.equalsIgnoreCase("cl")) {
			numero = Integer.parseInt("" + cx[2] + cx[3], 16);
		} else {
			numero = Integer.parseInt(NumberToFourByteHexa(number), 16);
		}
		if (numero > 31) {
			System.out.println("not a good number");
			System.exit(40);
		}
		// int multiplier = (int)Math.pow(2, numero);
		int otherMult;
		String regg;
		if (xx.equals("ax")) {
			for (int i = 0; i < numero; i++) {
				regg = "" + ax[0] + ax[1] + ax[2] + ax[3];
				otherMult = Integer.parseInt(regg, 16);
				if (Integer.parseInt("" + ax[0], 16) <= 7)
					CF = false;
				else
					CF = true;

				mov("ax", "" + (otherMult * 2));
			}

		} else if (xx.equals("bx")) {
			for (int i = 0; i < numero; i++) {
				regg = "" + bx[0] + bx[1] + bx[2] + bx[3];
				otherMult = Integer.parseInt(regg, 16);
				if (Integer.parseInt("" + bx[0], 16) <= 7)
					CF = false;
				else
					CF = true;

				mov("bx", "" + otherMult * 2);
			}
		} else if (xx.equals("cx")) {
			for (int i = 0; i < numero; i++) {
				regg = "" + cx[0] + cx[1] + cx[2] + cx[3];
				otherMult = Integer.parseInt(regg, 16);
				if (Integer.parseInt("" + cx[0], 16) <= 7)
					CF = false;
				else
					CF = true;

				mov("cx", "" + otherMult * 2);
			}
		} else if (xx.equals("dx")) {
			for (int i = 0; i < numero; i++) {
				regg = "" + dx[0] + dx[1] + dx[2] + dx[3];
				otherMult = Integer.parseInt(regg, 16);
				if (Integer.parseInt("" + dx[0], 16) <= 7)
					CF = false;
				else
					CF = true;

				mov("dx", "" + otherMult * 2);
			}
		} else if (xx.equals("si")) {
			for (int i = 0; i < numero; i++) {
				regg = "" + si[0] + si[1] + si[2] + si[3];
				otherMult = Integer.parseInt(regg, 16);
				if (Integer.parseInt("" + si[0], 16) <= 7)
					CF = false;
				else
					CF = true;

				mov("si", "" + otherMult * 2);
			}
		} else if (xx.equals("bp")) {
			for (int i = 0; i < numero; i++) {
				regg = "" + bp[0] + bp[1] + bp[2] + bp[3];
				otherMult = Integer.parseInt(regg, 16);
				if (Integer.parseInt("" + bp[0], 16) <= 7)
					CF = false;
				else
					CF = true;

				mov("bp", "" + otherMult * 2);
			}
		} else if (xx.equals("di")) {
			for (int i = 0; i < numero; i++) {
				regg = "" + di[0] + di[1] + di[2] + di[3];
				otherMult = Integer.parseInt(regg, 16);
				if (Integer.parseInt("" + di[0], 16) <= 7)
					CF = false;
				else
					CF = true;

				mov("di", "" + otherMult * 2);
			}
		} else if (xx.equals("al")) {
			for (int i = 0; i < numero; i++) {
				regg = "" + ax[2] + ax[3];
				otherMult = Integer.parseInt(regg, 16);
				if (Integer.parseInt("" + ax[2], 16) <= 7)
					CF = false;
				else
					CF = true;

				mov("al", "" + otherMult * 2);
			}
		} else if (xx.equals("bl")) {
			for (int i = 0; i < numero; i++) {
				regg = "" + bx[2] + bx[3];
				otherMult = Integer.parseInt(regg, 16);
				if (Integer.parseInt("" + bx[2], 16) <= 7)
					CF = false;
				else
					CF = true;

				mov("bl", "" + otherMult * 2);
			}
		} else if (xx.equals("cl")) {
			for (int i = 0; i < numero; i++) {
				regg = "" + cx[2] + cx[3];
				otherMult = Integer.parseInt(regg, 16);
				if (Integer.parseInt("" + cx[2], 16) <= 7)
					CF = false;
				else
					CF = true;

				mov("cl", "" + otherMult * 2);
			}
		} else if (xx.equals("dl")) {
			for (int i = 0; i < numero; i++) {
				regg = "" + dx[2] + dx[3];
				otherMult = Integer.parseInt(regg, 16);
				if (Integer.parseInt("" + dx[2], 16) <= 7)
					CF = false;
				else
					CF = true;

				mov("al", "" + otherMult * 2);
			}
		} else if (xx.equals("ah")) {
			for (int i = 0; i < numero; i++) {
				regg = "" + ax[0] + ax[1];
				otherMult = Integer.parseInt(regg, 16);
				if (Integer.parseInt("" + ax[0], 16) <= 7)
					CF = false;
				else
					CF = true;

				mov("ah", "" + otherMult * 2);
			}
		} else if (xx.equals("bh")) {
			for (int i = 0; i < numero; i++) {
				regg = "" + bx[0] + bx[1];
				otherMult = Integer.parseInt(regg, 16);
				if (Integer.parseInt("" + bx[0], 16) <= 7)
					CF = false;
				else
					CF = true;

				mov("bh", "" + otherMult * 2);
			}
		} else if (xx.equals("ch")) {
			for (int i = 0; i < numero; i++) {
				regg = "" + cx[0] + cx[1];
				otherMult = Integer.parseInt(regg, 16);
				if (Integer.parseInt("" + cx[0], 16) <= 7)
					CF = false;
				else
					CF = true;

				mov("ch", "" + otherMult * 2);
			}
		} else if (xx.equals("dh")) {
			for (int i = 0; i < numero; i++) {
				regg = "" + dx[0] + dx[1];
				otherMult = Integer.parseInt(regg, 16);
				if (Integer.parseInt("" + dx[0], 16) <= 7)
					CF = false;
				else
					CF = true;

				mov("dh", "" + otherMult * 2);
			}
		} else {
			int a = 0;
			for (int i = 0; i < variables.size(); i++) {
				if (variables.get(i).getName().equals(xx)) {
					if (variables.get(i).type) {
						for (int j = 0; j < numero; j++) {
							regg = "" + variables.get(i).data.charAt(0) + variables.get(i).data.charAt(1)
									+ variables.get(i).data.charAt(2) + variables.get(i).data.charAt(3);
							otherMult = Integer.parseInt(regg, 16);
							if (Integer.parseInt("" + variables.get(i).data.charAt(0), 16) <= 7)
								CF = false;
							else
								CF = true;

							mov("[" + variables.get(i).memoryIndex + "]", "" + otherMult * 2);
						}
					} else {
						for (int j = 0; j < numero; j++) {
							regg = "" + variables.get(i).data.charAt(2) + variables.get(i).data.charAt(3);
							otherMult = Integer.parseInt(regg, 16);
							if (Integer.parseInt("" + variables.get(i).data.charAt(2), 16) <= 7)
								CF = false;
							else
								CF = true;

							mov("[" + variables.get(i).memoryIndex + "]", "" + otherMult * 2);
						}

					}
					break;
				}

				a++;
			}

			if (a == variables.size() && xx.indexOf('[') != -1) {
				xx = xx.substring(1, xx.length() - 1);
				int indexful = Integer.parseInt(NumberToFourByteHexa(xx), 16);
				for (int i = 0; i < numero; i++) {
					regg = memory[indexful];
					otherMult = Integer.parseInt(regg, 16);
					if (Integer.parseInt("" + regg.charAt(0), 16) <= 7)
						CF = false;
					else
						CF = true;

					mov("[" + indexful + "]", "" + otherMult * 2);
				}
			}

		}

	}

	/**
	 * shift right works properly for registers but cannot control memory or
	 * variable case due to not completed "move operator"
	 * 
	 * @param xx
	 * @param number
	 */

	public void shr(String xx, String number) {
		int numero;
		if (number.equalsIgnoreCase("cl")) {
			numero = Integer.parseInt("" + cx[2] + cx[3], 16);
		} else {
			numero = Integer.parseInt(NumberToFourByteHexa(number), 16);
		}
		if (numero > 31) {
			System.out.println("not a good number");
			System.exit(40);
		}

		int otherMult;
		String regg;
		if (xx.equals("ax")) {
			for (int i = 0; i < numero; i++) {
				regg = "" + ax[0] + ax[1] + ax[2] + ax[3];
				otherMult = Integer.parseInt(regg, 16);
				if (Integer.parseInt("" + ax[3], 16) % 2 == 0)
					CF = false;
				else
					CF = true;

				mov("ax", "" + (otherMult / 2));
			}
		} else if (xx.equals("bx")) {
			for (int i = 0; i < numero; i++) {
				regg = "" + bx[0] + bx[1] + bx[2] + bx[3];
				otherMult = Integer.parseInt(regg, 16);
				if (Integer.parseInt("" + bx[3], 16) % 2 == 0)
					CF = false;
				else
					CF = true;

				mov("bx", "" + (otherMult / 2));
			}
		} else if (xx.equals("cx")) {
			for (int i = 0; i < numero; i++) {
				regg = "" + cx[0] + cx[1] + cx[2] + cx[3];
				otherMult = Integer.parseInt(regg, 16);
				if (Integer.parseInt("" + cx[3], 16) % 2 == 0)
					CF = false;
				else
					CF = true;

				mov("cx", "" + (otherMult / 2));
			}
		} else if (xx.equals("dx")) {
			for (int i = 0; i < numero; i++) {
				regg = "" + dx[0] + dx[1] + dx[2] + dx[3];
				otherMult = Integer.parseInt(regg, 16);
				if (Integer.parseInt("" + dx[3], 16) % 2 == 0)
					CF = false;
				else
					CF = true;

				mov("dx", "" + (otherMult / 2));
			}
		} else if (xx.equals("bp")) {
			for (int i = 0; i < numero; i++) {
				regg = "" + bp[0] + bp[1] + bp[2] + bp[3];
				otherMult = Integer.parseInt(regg, 16);
				if (Integer.parseInt("" + bp[3], 16) % 2 == 0)
					CF = false;
				else
					CF = true;

				mov("bp", "" + (otherMult / 2));
			}
		} else if (xx.equals("si")) {
			for (int i = 0; i < numero; i++) {
				regg = "" + si[0] + si[1] + si[2] + si[3];
				otherMult = Integer.parseInt(regg, 16);
				if (Integer.parseInt("" + si[3], 16) % 2 == 0)
					CF = false;
				else
					CF = true;

				mov("si", "" + (otherMult / 2));
			}
		} else if (xx.equals("al")) {
			for (int i = 0; i < numero; i++) {
				regg = "" + ax[2] + ax[3];
				otherMult = Integer.parseInt(regg, 16);
				if (Integer.parseInt("" + ax[3], 16) % 2 == 0)
					CF = false;
				else
					CF = true;

				mov("al", "" + otherMult / 2);
			}
		} else if (xx.equals("bl")) {
			for (int i = 0; i < numero; i++) {
				regg = "" + bx[2] + bx[3];
				otherMult = Integer.parseInt(regg, 16);
				if (Integer.parseInt("" + bx[3], 16) % 2 == 0)
					CF = false;
				else
					CF = true;

				mov("bl", "" + otherMult / 2);
			}
		} else if (xx.equals("cl")) {
			for (int i = 0; i < numero; i++) {
				regg = "" + cx[2] + cx[3];
				otherMult = Integer.parseInt(regg, 16);
				if (Integer.parseInt("" + cx[3], 16) % 2 == 0)
					CF = false;
				else
					CF = true;

				mov("cl", "" + otherMult / 2);
			}
		} else if (xx.equals("dl")) {
			for (int i = 0; i < numero; i++) {
				regg = "" + dx[2] + dx[3];
				otherMult = Integer.parseInt(regg, 16);
				if (Integer.parseInt("" + dx[3], 16) % 2 == 0)
					CF = false;
				else
					CF = true;

				mov("al", "" + otherMult / 2);
			}
		} else if (xx.equals("ah")) {
			for (int i = 0; i < numero; i++) {
				regg = "" + ax[0] + ax[1];
				otherMult = Integer.parseInt(regg, 16);
				if (Integer.parseInt("" + ax[1], 16) % 2 == 0)
					CF = false;
				else
					CF = true;

				mov("ah", "" + otherMult / 2);
			}
		} else if (xx.equals("bh")) {
			for (int i = 0; i < numero; i++) {
				regg = "" + bx[0] + bx[1];
				otherMult = Integer.parseInt(regg, 16);
				if (Integer.parseInt("" + bx[1], 16) % 2 == 0)
					CF = false;
				else
					CF = true;

				mov("bh", "" + otherMult / 2);
			}
		} else if (xx.equals("ch")) {
			for (int i = 0; i < numero; i++) {
				regg = "" + cx[0] + cx[1];
				otherMult = Integer.parseInt(regg, 16);
				if (Integer.parseInt("" + cx[1], 16) % 2 == 0)
					CF = false;
				else
					CF = true;

				mov("ch", "" + otherMult / 2);
			}
		} else if (xx.equals("dh")) {
			for (int i = 0; i < numero; i++) {
				regg = "" + dx[0] + dx[1];
				otherMult = Integer.parseInt(regg, 16);
				if (Integer.parseInt("" + dx[1], 16) % 2 == 0)
					CF = false;
				else
					CF = true;

				mov("dh", "" + otherMult / 2);
			}
		} else {
			int a = 0;
			for (int i = 0; i < variables.size(); i++) {
				if (variables.get(i).getName().equals(xx)) {
					if (variables.get(i).type) {
						for (int j = 0; j < numero; j++) {
							regg = "" + variables.get(i).data.charAt(0) + variables.get(i).data.charAt(1)
									+ variables.get(i).data.charAt(2) + variables.get(i).data.charAt(3);
							otherMult = Integer.parseInt(regg, 16);
							if (Integer.parseInt("" + variables.get(i).data.charAt(0), 16) % 2 == 0)
								CF = false;
							else
								CF = true;

							mov("[" + variables.get(i).memoryIndex + "]", "" + otherMult / 2);
						}
					} else {
						for (int j = 0; j < numero; j++) {
							regg = "" + variables.get(i).data.charAt(2) + variables.get(i).data.charAt(3);
							otherMult = Integer.parseInt(regg, 16);
							if (Integer.parseInt("" + variables.get(i).data.charAt(2), 16) % 2 == 0)
								CF = false;
							else
								CF = true;

							mov("[" + variables.get(i).memoryIndex + "]", "" + otherMult / 2);
						}

					}
					break;
				}

				a++;
			}

			if (a == variables.size() && xx.indexOf('[') != -1) {
				xx = xx.substring(1, xx.length() - 1);
				int indexful = Integer.parseInt(NumberToFourByteHexa(xx), 16);
				for (int i = 0; i < numero; i++) {
					regg = memory[indexful];
					otherMult = Integer.parseInt(regg, 16);
					if (Integer.parseInt("" + regg.charAt(0), 16) % 2 == 0)
						CF = false;
					else
						CF = true;

					mov("[" + indexful + "]", "" + otherMult / 2);
				}
			}

		}
	}

	/**
	 * interrupt with 01 and 02 functions 01 --> getting a char input 02 --> gives
	 * an output char
	 */
	public void int21h() {
		String ah = "" + ax[0] + ax[1];
		int ascii;
		Scanner conc = new Scanner(System.in);
		if (ah.equals("01")) {
			char ch = conc.next().charAt(0);
			ascii = (int) ch;
			String hexa = NumberToFourByteHexa("" + ascii);
			ax[2] = hexa.charAt(2);
			ax[3] = hexa.charAt(3);

		} else if (ah.equals("02")) {
			String chs = "" + dx[2] + dx[3];
			ascii = Integer.parseInt(chs, 16);
			System.out.print((char) ascii);

		}
		conc.close();

	}

	/**
	 * pop allows popping to a variable, register or memory address register and
	 * variable cases are perfect
	 * 
	 * @param reg
	 */

	public void pop(String reg) {
		int index = Integer.parseInt(SP, 16) + 2;
		if (index >= 1024 * 64) {
			System.out.println("no element to pop");
			return;
		}
		if (reg.equals("ax")) {
			ax[0] = memory[index].charAt(0);
			ax[1] = memory[index].charAt(1);
			ax[2] = memory[index].charAt(2);
			ax[3] = memory[index].charAt(3);

		} else if (reg.equals("bx")) {
			bx[0] = memory[index].charAt(0);
			bx[1] = memory[index].charAt(1);
			bx[2] = memory[index].charAt(2);
			bx[3] = memory[index].charAt(3);

		} else if (reg.equals("cx")) {
			cx[0] = memory[index].charAt(0);
			cx[1] = memory[index].charAt(1);
			cx[2] = memory[index].charAt(2);
			cx[3] = memory[index].charAt(3);

		} else if (reg.equals("dx")) {
			dx[0] = memory[index].charAt(0);
			dx[1] = memory[index].charAt(1);
			dx[2] = memory[index].charAt(2);
			dx[3] = memory[index].charAt(3);

		} else if (reg.equals("di")) {
			di[0] = memory[index].charAt(0);
			di[1] = memory[index].charAt(1);
			di[2] = memory[index].charAt(2);
			di[3] = memory[index].charAt(3);

		} else if (reg.equals("si")) {
			si[0] = memory[index].charAt(0);
			si[1] = memory[index].charAt(1);
			si[2] = memory[index].charAt(2);
			si[3] = memory[index].charAt(3);

		} else if (reg.equals("bp")) {
			bp[0] = memory[index].charAt(0);
			bp[1] = memory[index].charAt(1);
			bp[2] = memory[index].charAt(2);
			bp[3] = memory[index].charAt(3);

		} else {
			int a = 0;
			for (int i = 0; i < variables.size(); i++) {
				if (variables.get(i).getName().equals(reg) && variables.get(i).isType()) {
					variables.get(i).data = memory[index];
					break;
				}

				a++;
			}

			if (a == variables.size() && reg.indexOf('[') != -1) {
				reg = reg.substring(1, reg.length() - 1);
				if (instrustionAreaEnd < Integer.parseInt(NumberToFourByteHexa(reg), 16)) {
					memory[Integer.parseInt(NumberToFourByteHexa(reg), 16)] = memory[index];
				} else {
					System.out.println("don't try to reach instruction area");
					return;
				}

			}

		}

		SP = NumberToFourByteHexa("" + index);
		memory[index] = null;

	}

	public void inc(String operand) {

		add(operand, "" + 1);
	}

	public void dec(String operand) {
		sub(operand, "" + 1);
	}

	public void sub(String first, String second) {

	}

	/**
	 * Calls helper MOV methods according to the contents of @param first.
	 * 
	 * @param first:  first operand of MOV operation
	 * @param second: second operand of MOV operation
	 */

	public void mov(String first, String second) {

		AF = false;
		OF = false;
		ZF = false;

		boolean isFirstVar = false;
		Variable firstvar = null;
		Variable temp;
		Iterator<Variable> itr = variables.iterator();
		while (itr.hasNext()) {
			temp = itr.next();
			if (first.contains(temp.name)) {
				isFirstVar = true;
				firstvar = temp;
				break;
			}
		} // now we know first operand is whether variable ->(add w[var1],ax)
		if (isFirstVar) {
			if (first.charAt(0) == 'w') {
				firstvar.type = true;
			} else if (first.charAt(0) == 'b') {
				firstvar.type = false;
			}
			mov_var_xx(firstvar, second);
		} else if (first.contains("[") && first.contains("]")) { // first is memory
			mov_mem_xx(first, second);
		} else if (isRegTwoByte(first)) {// 16 bit register
			MOV_TwoByteReg(first, second);
		} else if (isRegOneByte(first)) {// 8 bit register
			MOV_OneByteReg(first, second);
		} else { // reg veya memoryye yazmiyo hata ver
			System.out.println("Undefined symbols are listed: " + first);
			System.exit(0);
		}

	}

	private void mov_var_xx(Variable firstvar, String second) {
		// input is mov w/b[firstvar], second
		char[] temp = new char[4];
		for (int i = 0; i <= 3; i++)
			temp[i] = '0';
		boolean isSecondVar = false;
		Variable secondVar = null;
		Variable tempVar;
		Iterator<Variable> itr = variables.iterator();
		while (itr.hasNext()) {
			tempVar = itr.next();
			if (second.contains(tempVar.name)) {
				isSecondVar = true;
				secondVar = tempVar;
				break;
			}
		}

		if (second.contains("offset")) {

			String value = NumberToFourByteHexa("" + secondVar.memoryIndex);
			for (int i = 0; i <= 3; i++) {
				temp[i] = '0';
			}
			for (int i = 0; i <= 3 && i < value.length(); i++) {
				temp[3 - i] = value.charAt(value.length() - i - 1);
			}

		} else if (second.contains("[") && second.contains("]")) {
			boolean thereWasb = false;
			if (second.charAt(0) == 'b') { // 1 byte
				second = second.substring(1); // got rid of 'b'
				thereWasb = true;
			}
			if (second.charAt(0) == 'w') {
				second = second.substring(1); // got rid of 'w'
			}
			if (isSecondVar) {
				firstvar.data = secondVar.data;
				return;
			}
			second = second.substring(1, second.length() - 1); // got rid of [ and ]
			String num = "";
			if (isRegOneByte(second) || isRegTwoByte(second)) {// register
				if (second.equalsIgnoreCase("si")) {
					for (int i = 0; i <= 3; i++) {
						num += si[i];
					}
				} else if (second.equalsIgnoreCase("di")) {
					for (int i = 0; i <= 3; i++) {
						num += di[i];
					}
				} else if (second.equalsIgnoreCase("bp")) {
					for (int i = 0; i <= 3; i++) {
						num += bp[i];
					}
				} else if (second.equalsIgnoreCase("bx")) {
					for (int i = 0; i <= 3; i++) {
						num += bx[i];
					}
				} else {
					System.out.println("#ERROR 39: Bad Index Register ");
					System.exit(0);
				}
			} else {// number
				num = NumberToFourByteHexa(second);
			}

			if (Integer.parseInt(num, 16) >= memory.length || Integer.parseInt(num, 16) < numberOfInstructions * 6) {
				System.out.println("Address is not valid");
				System.exit(0);

			} else if (memory[Integer.parseInt(num, 16)] == null) {
			} else {
				String memoryLocaitonOfNum = memory[Integer.parseInt(num, 16)];
				for (int i = 0; i <= 3 && i < num.length(); i++) {
					temp[3 - i] = memoryLocaitonOfNum.charAt(memoryLocaitonOfNum.length() - i - 1);
				}
			}

			if (thereWasb) {
				temp[0] = '0';
				temp[1] = '0';
			}
		} else if (isRegOneByte(second) || isRegTwoByte(second)) { // register

			if (second.equalsIgnoreCase("ax")) {
				for (int i = 3; i >= 0; i--) {
					temp[i] = ax[i];
				}
			} else if (second.equalsIgnoreCase("bx")) {
				for (int i = 3; i >= 0; i--) {
					temp[i] = bx[i];
				}
			} else if (second.equalsIgnoreCase("cx")) {
				for (int i = 3; i >= 0; i--) {
					temp[i] = cx[i];
				}
			} else if (second.equalsIgnoreCase("dx")) {
				for (int i = 3; i >= 0; i--) {
					temp[i] = dx[i];
				}
			} else if (second.equalsIgnoreCase("si")) {
				for (int i = 3; i >= 0; i--) {
					temp[i] = si[i];
				}
			} else if (second.equalsIgnoreCase("bp")) {
				for (int i = 3; i >= 0; i--) {
					temp[i] = bp[i];
				}
			} else if (second.equalsIgnoreCase("di")) {
				for (int i = 3; i >= 0; i--) {
					temp[i] = di[i];
				}
			} else if (second.equalsIgnoreCase("ah")) {
				for (int i = 3; i >= 2; i--) {
					temp[i] = ax[i - 2];
				}
			} else if (second.equalsIgnoreCase("bh")) {
				for (int i = 3; i >= 2; i--) {
					temp[i] = bx[i - 2];
				}
			} else if (second.equalsIgnoreCase("ch")) {
				for (int i = 3; i >= 2; i--) {
					temp[i] = cx[i - 2];
				}
			} else if (second.equalsIgnoreCase("dh")) {
				for (int i = 3; i >= 2; i--) {
					temp[i] = dx[i - 2];
				}
			} else if (second.equalsIgnoreCase("al")) {
				for (int i = 3; i >= 2; i--) {
					temp[i] = ax[i];
				}
			} else if (second.equalsIgnoreCase("bl")) {
				for (int i = 3; i >= 2; i--) {
					temp[i] = bx[i];
				}
			} else if (second.equalsIgnoreCase("cl")) {
				for (int i = 3; i >= 2; i--) {
					temp[i] = cx[i];
				}
			} else if (second.equalsIgnoreCase("dl")) {
				for (int i = 3; i >= 2; i--) {
					temp[i] = dx[i];
				}
			}
		} else { // number or variable
			if (isSecondVar) {
				second = NumberToFourByteHexa("0" + secondVar.data + "h");
			} else {
				second = NumberToFourByteHexa(second); // number
			}

			for (int i = 0; i <= 3 && i < second.length(); i++) {
				temp[3 - i] = second.charAt(second.length() - i - 1);
			}
		}

		firstvar.data = "" + temp[0] + temp[1] + temp[2] + temp[3];

	}

	/*
	 * implement it NOW!!
	 */
	private void mov_mem_xx(String first, String second) { //
		/*
		 * first is not variable for sure first is number or bx di si second could be
		 * reg or mem check xp
		 * 
		 */
		String source;
		
		if (first.charAt(0) == 'b') {
			first = first.substring(2, first.length() - 1);// got rid of "b[" and "]"
			source = contentsOfSecondOperandOfADDOneByte(second);
		} else {// assume there is w
			if (first.charAt(0) == 'w') {
				first = first.substring(1); // got rid of "w"
			}
			first = first.substring(1, first.length() - 1);// got rid of "[" and "]"
			source = contentsOfSecondOperandOfADDTwoByte(second);
		}
		while(source.length()<4) source="0"+source;
		int memoryIndex = Integer.parseInt(NumberToFourByteHexa(first), 16);
		memory[memoryIndex] = source;
	}

	/**
	 * Calls helper ADD methods according to the contents of @param first.
	 * 
	 * @param first:  first operand of ADD operation
	 * @param second: second operand of ADD operation
	 */

	public void add(String first, String second) {
		AF = false;
		ZF = false;
		if (first.contains("[") && first.contains("]")) { // first is memory

			add_mem_xx(first, second);

		} else if (isRegOneByte(first) || isRegTwoByte(first)) {
			// first is reg
			add_reg_unknown(first, second);

		} else { // reg veya memoryye eklemiyo
			System.out.println("Undefined symbols are listed: " + first);
			System.exit(0);
		}

	}

	private String add_mem1B_xx(String first, String second) {
		String addend = "";
		first = first.substring(2, first.length() - 1);// got rid of "b[","]"
		if (Integer.parseInt(NumberToFourByteHexa(first), 16) <= numberOfInstructions * 6
				|| Integer.parseInt(NumberToFourByteHexa(first), 16) >= 64 * 1024) {
			System.out.println("Bad Memory Address");
			System.exit(0);
		}

		if (isRegTwoByte(second)) {
			System.out.println("#ERROR 13: Byte/Word Combination Not Allowed ");
			System.exit(0);
		} else if (isRegOneByte(second)) {
			if (second.equals("al")) {
				addend += "" + ax[2] + "" + ax[3];
			} else if (second.equals("ah")) {
				addend += "" + ax[0] + "" + ax[1];
			} else if (second.equals("bl")) {
				addend += "" + bx[2] + "" + bx[3];
			} else if (second.equals("bh")) {
				addend += "" + bx[0] + "" + bx[1];
			} else if (second.equals("cl")) {
				addend += "" + cx[2] + "" + cx[3];
			} else if (second.equals("ch")) {
				addend += "" + cx[0] + "" + cx[1];
			} else if (second.equals("dl")) {
				addend += "" + dx[2] + "" + dx[3];
			} else if (second.equals("dh")) {
				addend += "" + dx[0] + "" + dx[1];
			}
		} else { // numbers must be 8 bits which means less than 256
			if (Integer.parseInt(NumberToFourByteHexa(second), 16) > 255) {
				System.out.println("#ERROR 30: Byte-Sized Constant Required");
				System.exit(0);
			} else {
				addend += NumberToFourByteHexa(second);
			}
		}
		return addend;
	}

	private String add_mem2B_xx(String first, String second) {
		String addend = "";
		first = first.substring(2, first.length() - 1);// got rid of "w[","]"
		if (Integer.parseInt(NumberToFourByteHexa(first), 16) <= numberOfInstructions * 6
				|| Integer.parseInt(NumberToFourByteHexa(first), 16) >= 64 * 1024) {
			System.out.println("Bad Memory Address");
			System.exit(0);
		}
		if (isRegOneByte(second)) {
			System.out.println("#ERROR 13: Byte/Word Combination Not Allowed ");
			System.exit(0);
		} else if (isRegTwoByte(second)) {
			if (second.equals("ax")) {
				addend += "" + ax[0] + "" + ax[1] + ax[2] + "" + ax[3];
			} else if (second.equals("bx")) {
				addend += "" + bx[0] + "" + bx[1] + bx[2] + "" + bx[3];
			} else if (second.equals("cx")) {
				addend += "" + cx[0] + "" + cx[1] + cx[2] + "" + cx[3];
			} else if (second.equals("dx")) {
				addend += "" + dx[0] + "" + dx[1] + dx[2] + "" + dx[3];
			} else if (second.equals("bp")) {
				addend += "" + bp[0] + "" + bp[1] + bp[2] + "" + bp[3];
			} else if (second.equals("si")) {
				addend += "" + si[0] + "" + si[1] + si[2] + "" + si[3];
			} else if (second.equals("di")) {
				addend += "" + di[0] + "" + di[1] + di[2] + "" + di[3];
			}
		} else { // number
			addend += NumberToFourByteHexa(second);
		}
		return addend;
	}

	private void add_mem_xx(String first, String second) {
		String addend = "";
		boolean isFirstVar = false;
		Variable firstvar = null;
		Variable temp;
		Iterator<Variable> itr = variables.iterator();
		while (itr.hasNext()) {
			temp = itr.next();
			if (first.contains(temp.name)) {
				isFirstVar = true;
				firstvar = temp;
				break;
			}
		} // now we know first operand is whether variable ->(add w[var1],ax)
		boolean isSecondVar = false;
		Variable secondVar = null;
		Iterator<Variable> itrr = variables.iterator();
		while (itrr.hasNext()) {
			temp = itrr.next();
			if (second.contains(temp.name)) {
				isSecondVar = true;
				secondVar = temp;

				break;
			}
		} // now we know second operand is whether variable ->(add w[1234h],var2)

		if (isFirstVar) {
			if (first.charAt(0) == 'w') {
				if (!firstvar.isType()) {
					System.out
							.println(firstvar.name + " is defined as db but you used 'w' in front of square brackets.");
					System.exit(0);
				}
				if (second.contains("[")) {// add w[var2],[bx] - add w[var2],[1234h] - add w[var2],w [var2]
					System.out.println("#ERROR 50: Reg,Mem Required  ");
					System.exit(0);
				} else if (second.contains("offset")) {// add w[var2],offset var2
					addend = NumberToFourByteHexa("" + secondVar.memoryIndex);
				} else if (isSecondVar) {// add w[var2],var2
					addend += NumberToFourByteHexa(secondVar.data);
				} else if (isRegOneByte(second)) {
					if (second.equalsIgnoreCase("ah")) {
						addend += NumberToFourByteHexa("0" + ax[0] + "" + ax[1] + "h");
					} else if (second.equalsIgnoreCase("bh")) {
						addend += NumberToFourByteHexa("0" + bx[0] + "" + bx[1] + "h");
					} else if (second.equalsIgnoreCase("ch")) {
						addend += NumberToFourByteHexa("0" + cx[0] + "" + cx[1] + "h");
					} else if (second.equalsIgnoreCase("dh")) {
						addend += NumberToFourByteHexa("0" + dx[0] + "" + dx[1] + "h");
					} else if (second.equalsIgnoreCase("al")) {
						addend += NumberToFourByteHexa("0" + ax[2] + "" + ax[3] + "h");
					} else if (second.equalsIgnoreCase("bl")) {
						addend += NumberToFourByteHexa("0" + bx[2] + "" + bx[3] + "h");
					} else if (second.equalsIgnoreCase("cl")) {
						addend += NumberToFourByteHexa("0" + cx[2] + "" + cx[3] + "h");
					} else if (second.equalsIgnoreCase("dl")) {
						addend += NumberToFourByteHexa("0" + dx[2] + "" + dx[3] + "h");
					}
				} else if (isRegTwoByte(second)) {
					if (second.equalsIgnoreCase("ax")) {
						addend += NumberToFourByteHexa("0" + ax[0] + "" + ax[1] + "" + ax[2] + "" + ax[3] + "h");
					} else if (second.equalsIgnoreCase("bx")) {
						addend += NumberToFourByteHexa("0" + bx[0] + "" + bx[1] + "" + bx[2] + "" + bx[3] + "h");
					} else if (second.equalsIgnoreCase("cx")) {
						addend += NumberToFourByteHexa("0" + cx[0] + "" + cx[1] + "" + cx[2] + "" + cx[3] + "h");
					} else if (second.equalsIgnoreCase("dx")) {
						addend += NumberToFourByteHexa("0" + dx[0] + "" + dx[1] + "" + dx[2] + "" + dx[3] + "h");
					} else if (second.equalsIgnoreCase("si")) {
						addend += NumberToFourByteHexa("0" + si[0] + "" + si[1] + "" + si[2] + "" + si[3] + "h");
					} else if (second.equalsIgnoreCase("bp")) {
						addend += NumberToFourByteHexa("0" + bp[0] + "" + bp[1] + "" + bp[2] + "" + bp[3] + "h");
					} else if (second.equalsIgnoreCase("di")) {
						addend += NumberToFourByteHexa("0" + di[0] + "" + di[1] + "" + di[2] + "" + di[3] + "h");
					}
				} else {// number
					addend += NumberToFourByteHexa(second);
				}
				/*
				 * 
				 */
				if (Integer.parseInt("" + firstvar.data.charAt(3), 16)
						+ Integer.parseInt("" + addend.charAt(3), 16) > 15) {// is there and carry from 4th bit to
																				// 5th
																				// bit?
					AF = true;
				}
				int augend = Integer.parseInt(firstvar.data, 16);
				int sum = Integer.parseInt(addend, 16) + augend;
				if (sum == 0) {
					ZF = true;
				} else if (sum == Integer.parseInt("10000", 16)) {
					CF = true;
					ZF = true;
					sum = 0;
				} else if (sum > Integer.parseInt("10000", 16)) {
					sum -= Integer.parseInt("10000", 16);
					CF = true;
				}
				/*
				 * 
				 */
				firstvar.data = "" + NumberToFourByteHexa("" + sum);

			} else if (first.charAt(0) == 'b') {
				if (firstvar.isType()) {
					System.out
							.println(firstvar.name + " is defined as dw but you used 'b' in front of square brackets.");
					System.exit(0);
				}
				if (second.contains("[") || isSecondVar) {// add b[var2],[bx] - add b[var2],[1234h] - add b[var2],[var1]
					System.out.println("#ERROR 50: Reg,Mem Required  ");
					System.exit(0);
				} else if (second.contains("offset")) {// add b[var2],offset var2
					addend = NumberToFourByteHexa("" + secondVar.memoryIndex);
				} else if (isRegOneByte(second) || isRegTwoByte(second)) {
					if (second.equalsIgnoreCase("ax") || second.equalsIgnoreCase("al")) {
						addend += NumberToFourByteHexa("0" + ax[2] + "" + ax[3] + "h");
					} else if (second.equalsIgnoreCase("bx") || second.equalsIgnoreCase("bl")) {
						addend += NumberToFourByteHexa("0" + bx[2] + "" + bx[3] + "h");
					} else if (second.equalsIgnoreCase("cx") || second.equalsIgnoreCase("cl")) {
						addend += NumberToFourByteHexa("0" + cx[2] + "" + cx[3] + "h");
					} else if (second.equalsIgnoreCase("dx") || second.equalsIgnoreCase("dl")) {
						addend += NumberToFourByteHexa("0" + dx[2] + "" + dx[3] + "h");
					}
					if (second.equalsIgnoreCase("ah")) {
						addend += NumberToFourByteHexa("0" + ax[0] + "" + ax[1] + "h");
					} else if (second.equalsIgnoreCase("bh")) {
						addend += NumberToFourByteHexa("0" + bx[0] + "" + bx[1] + "h");
					} else if (second.equalsIgnoreCase("ch")) {
						addend += NumberToFourByteHexa("0" + cx[0] + "" + cx[1] + "h");
					} else if (second.equalsIgnoreCase("dh")) {
						addend += NumberToFourByteHexa("0" + dx[0] + "" + dx[1] + "h");
					} else if (second.equalsIgnoreCase("si")) {
						addend += NumberToFourByteHexa("0" + si[2] + "" + si[3] + "h");
					} else if (second.equalsIgnoreCase("bp")) {
						addend += NumberToFourByteHexa("0" + bp[2] + "" + bp[3] + "h");
					} else if (second.equalsIgnoreCase("di")) {
						addend += NumberToFourByteHexa("0" + di[2] + "" + di[3] + "h");
					}
				} else {// number
					addend += NumberToFourByteHexa(second);
					if (Integer.parseInt(addend, 16) > 255) {
						System.out.println("#ERROR 30: Byte-Sized Constant Required");
						System.exit(0);
					}
				}

				/*
				 * 
				 */
				if (Integer.parseInt("" + firstvar.data.charAt(3), 16)
						+ Integer.parseInt("" + addend.charAt(3), 16) > 15) {// is there and carry from 4th bit to
																				// 5th
																				// bit?
					AF = true;
				}
				int augend = Integer.parseInt(firstvar.data, 16);
				int sum = Integer.parseInt(addend, 16) + augend;
				if (sum == 0) {
					ZF = true;
				} else if (sum == Integer.parseInt("100", 16)) {
					CF = true;
					ZF = true;
					sum = 0;
				} else if (sum > Integer.parseInt("100", 16)) {
					sum -= Integer.parseInt("100", 16);
					CF = true;
				}
				/*
				 * 
				 */

				firstvar.data = NumberToFourByteHexa("" + sum);

			} else {
				System.out.println("there must be 'b' or 'w' in front of square brackets");
				System.exit(0);
			}
		} else {
			if (second.contains("[") || second.contains("offset") || isSecondVar) {
				System.out.println("#ERROR 50: Reg,Mem Required  ");
				System.exit(0);
			}

			if (second.indexOf('\'') != -1) { // addind a char. takes its ascii value.
				if (first.charAt(0) == 'b' || first.charAt(0) == 'w') {
					addend += second.charAt(second.indexOf('\'') + 1) + 0;
				} else {
					System.out.println("#ERROR 50: Reg,Mem Required  ");
					System.exit(0);
				}
			} else {
				if (first.charAt(0) == 'b') {// constant must be byte sized and regs too
					addend = add_mem1B_xx(first, second);
				} else if (first.charAt(0) == 'w') { // regs must be two byte sized
					addend = add_mem2B_xx(first, second);
				} else {
					System.out.println("there must be 'b' or 'w' in front of square brackets");
					System.exit(0);
				}

				/*
				 * elimde addend var hexa şeklinde. onu firstin dediği adrestekyle
				 * topluycam.sonuca göre AF CF+ ZF+ değiştiricem
				 */
				// augend + addend = sum
				// Minuend - Subtrahend = Difference.
				// Multiplicand × Multiplier = Product
				int memoryIndex = Integer.parseInt(NumberToFourByteHexa(first), 16);// memory index of first operand
				if (memory[memoryIndex] != null) {
					while (addend.length() < 4) {
						addend = "0" + addend;
					}

					if (Integer.parseInt("" + memory[memoryIndex].charAt(3), 16)
							+ Integer.parseInt("" + addend.charAt(3), 16) > 15) {// is there and carry from 4th bit to
																					// 5th
																					// bit?
						AF = true;
					}
					int augend = Integer.parseInt(memory[memoryIndex], 16);
					int sum = Integer.parseInt(addend, 16) + augend;
					if (sum == 0) {
						ZF = true;
					} else if (sum == Integer.parseInt("10000", 16)) {
						CF = true;
						ZF = true;
						sum = 0;
					} else if (sum > Integer.parseInt("10000", 16)) {

						sum -= Integer.parseInt("10000", 16);
						CF = true;
					}
					memory[memoryIndex] = NumberToFourByteHexa(sum + "");
				} else {// augend is empty(which means 0), result is addend
					memory[memoryIndex] = addend;
				}

			}
		}
	}

	/**
	 * calls corresponding add_"first"_unknown function
	 * 
	 * @param first:  first operand of add operation. It's a register for sure.
	 * @param second: second operand of add operation
	 */

	private void add_reg_unknown(String first, String second) {

		// got the addend. Take their sum
		if (first.equalsIgnoreCase("ax")) {
			String addend = contentsOfSecondOperandOfADDTwoByte(second);
			int sum = Integer.parseInt((ax[0] + "" + ax[1] + "" + ax[2] + "" + ax[3]), 16)
					+ Integer.parseInt(addend, 16);
			String sumStringForm = NumberToFourByteHexa("" + sum);

			// for AF
			while (addend.length() < 4) {
				addend = "0" + addend;
			}
			if (Integer.parseInt("" + ax[3], 16) + Integer.parseInt("" + addend.charAt(3), 16) > 15) {
				AF = true;
			}
			if (sum == 0) {
				ZF = true;
				ax[0] = '0';
				ax[1] = '0';
				ax[2] = '0';
				ax[3] = '0';
			} else {
				if (sum > Integer.parseInt("ffff", 16)) {
					CF = true;
					sum -= Integer.parseInt("10000", 16);
					sumStringForm = NumberToFourByteHexa("" + sum);
				}

				ax[0] = sumStringForm.charAt(0);
				ax[1] = sumStringForm.charAt(1);
				ax[2] = sumStringForm.charAt(2);
				ax[3] = sumStringForm.charAt(3);

			}
		} else if (first.equalsIgnoreCase("bx")) {
			String addend = contentsOfSecondOperandOfADDTwoByte(second);
			int sum = Integer.parseInt((bx[0] + "" + bx[1] + "" + bx[2] + "" + bx[3]), 16)
					+ Integer.parseInt(addend, 16);
			String sumStringForm = NumberToFourByteHexa("" + sum);

			// for AF
			while (addend.length() < 4) {
				addend = "0" + addend;
			}
			if (Integer.parseInt("" + bx[3], 16) + Integer.parseInt("" + addend.charAt(3), 16) > 15) {
				AF = true;
			}
			if (sum == 0) {
				ZF = true;
				bx[0] = '0';
				bx[1] = '0';
				bx[2] = '0';
				bx[3] = '0';
			} else {
				if (sum > Integer.parseInt("ffff", 16)) {

					CF = true;
					sum -= Integer.parseInt("10000", 16);
					sumStringForm = NumberToFourByteHexa("" + sum);
				}

				bx[0] = sumStringForm.charAt(0);
				bx[1] = sumStringForm.charAt(1);
				bx[2] = sumStringForm.charAt(2);
				bx[3] = sumStringForm.charAt(3);
			}

		} else if (first.equalsIgnoreCase("cx")) {
			String addend = contentsOfSecondOperandOfADDTwoByte(second);
			int sum = Integer.parseInt((cx[0] + "" + cx[1] + "" + cx[2] + "" + cx[3]), 16)
					+ Integer.parseInt(addend, 16);
			String sumStringForm = NumberToFourByteHexa("" + sum);

			// for AF
			while (addend.length() < 4) {
				addend = "0" + addend;
			}
			if (Integer.parseInt("" + cx[3], 16) + Integer.parseInt("" + addend.charAt(3), 16) > 15) {
				AF = true;
			}
			if (sum == 0) {
				ZF = true;
				cx[0] = '0';
				cx[1] = '0';
				cx[2] = '0';
				cx[3] = '0';
			} else {
				if (sum > Integer.parseInt("ffff", 16)) {
					CF = true;
					sum -= Integer.parseInt("10000", 16);
					sumStringForm = NumberToFourByteHexa("" + sum);
				}
				cx[0] = sumStringForm.charAt(0);
				cx[1] = sumStringForm.charAt(1);
				cx[2] = sumStringForm.charAt(2);
				cx[3] = sumStringForm.charAt(3);
			}

		} else if (first.equalsIgnoreCase("dx")) {
			String addend = contentsOfSecondOperandOfADDTwoByte(second);
			int sum = Integer.parseInt((dx[0] + "" + dx[1] + "" + dx[2] + "" + dx[3]), 16)
					+ Integer.parseInt(addend, 16);
			String sumStringForm = NumberToFourByteHexa("" + sum);

			// for AF
			while (addend.length() < 4) {
				addend = "0" + addend;
			}
			if (Integer.parseInt("" + dx[3], 16) + Integer.parseInt("" + addend.charAt(3), 16) > 15) {
				AF = true;
			}
			if (sum == 0) {
				ZF = true;
				dx[0] = '0';
				dx[1] = '0';
				dx[2] = '0';
				dx[3] = '0';
			} else {
				if (sum > Integer.parseInt("ffff", 16)) {
					CF = true;
					sum -= Integer.parseInt("10000", 16);
					sumStringForm = NumberToFourByteHexa("" + sum);
				}
				dx[0] = sumStringForm.charAt(0);
				dx[1] = sumStringForm.charAt(1);
				dx[2] = sumStringForm.charAt(2);
				dx[3] = sumStringForm.charAt(3);
			}
		} else if (first.equalsIgnoreCase("al")) {
			String addend = contentsOfSecondOperandOfADDOneByte(second);
			int sum = Integer.parseInt(("" + ax[2] + "" + ax[3]), 16) + Integer.parseInt(addend, 16);// decimal sum
			String sumStringForm = NumberToFourByteHexa("" + sum);// sum is now hexa
			// sumStringForm = sumStringForm.substring(sumStringForm.length() - 2,
			// sumStringForm.length()); // took last byte
			// for AF
			while (addend.length() < 2) {
				addend = "0" + addend;
			}
			if (Integer.parseInt("" + ax[3], 16) + Integer.parseInt("" + addend.charAt(addend.length() - 1), 16) > 15) {
				AF = true;
			}
			if (sum == 0) {
				ZF = true;
				ax[2] = '0';
				ax[3] = '0';
			} else {
				if (sum > Integer.parseInt("ff", 16)) {

					CF = true;
					sum -= 256;
					sumStringForm = NumberToFourByteHexa("" + sum);
				}
				ax[2] = sumStringForm.charAt(sumStringForm.length() - 2);
				ax[3] = sumStringForm.charAt(sumStringForm.length() - 1);
			}
		} else if (first.equalsIgnoreCase("ah")) {
			String addend = contentsOfSecondOperandOfADDOneByte(second);
			int sum = Integer.parseInt(("" + ax[0] + "" + ax[1]), 16) + Integer.parseInt(addend, 16);// decimal sum
			String sumStringForm = NumberToFourByteHexa("" + sum);// sum is now hexa
			// sumStringForm = sumStringForm.substring(sumStringForm.length() - 2,
			// sumStringForm.length()); // took last byte
			// for AF
			while (addend.length() < 2) {
				addend = "0" + addend;
			}
			if (Integer.parseInt("" + ax[1], 16) + Integer.parseInt("" + addend.charAt(addend.length() - 1), 16) > 15) {
				AF = true;
			}
			if (sum == 0) {
				ZF = true;
				ax[0] = '0';
				ax[1] = '0';
			} else {
				if (sum > Integer.parseInt("ff", 16)) {

					CF = true;
					sum -= 256;
					sumStringForm = NumberToFourByteHexa("" + sum);
				}
				ax[0] = sumStringForm.charAt(sumStringForm.length() - 2);
				ax[1] = sumStringForm.charAt(sumStringForm.length() - 1);
			}

		} else if (first.equalsIgnoreCase("bl")) {
			String addend = contentsOfSecondOperandOfADDOneByte(second);
			int sum = Integer.parseInt(("" + bx[2] + "" + bx[3]), 16) + Integer.parseInt(addend, 16);// decimal sum
			String sumStringForm = NumberToFourByteHexa("" + sum);// sum is now hexa
			// sumStringForm = sumStringForm.substring(sumStringForm.length() - 2,
			// sumStringForm.length()); // took last byte
			// for AF
			while (addend.length() < 2) {
				addend = "0" + addend;
			}
			if (Integer.parseInt("" + bx[3], 16) + Integer.parseInt("" + addend.charAt(addend.length() - 1), 16) > 15) {
				AF = true;
			}
			if (sum == 0) {
				ZF = true;
				bx[2] = '0';
				bx[3] = '0';
			} else {
				if (sum > Integer.parseInt("ff", 16)) {

					CF = true;
					sum -= 256;
					sumStringForm = NumberToFourByteHexa("" + sum);
				}
				bx[2] = sumStringForm.charAt(sumStringForm.length() - 2);
				bx[3] = sumStringForm.charAt(sumStringForm.length() - 1);
			}
		} else if (first.equalsIgnoreCase("bh")) {
			String addend = contentsOfSecondOperandOfADDOneByte(second);
			int sum = Integer.parseInt(("" + bx[0] + "" + bx[1]), 16) + Integer.parseInt(addend, 16);// decimal sum
			String sumStringForm = NumberToFourByteHexa("" + sum);// sum is now hexa
			// sumStringForm = sumStringForm.substring(sumStringForm.length() - 2,
			// sumStringForm.length()); // took last byte
			// for AF
			while (addend.length() < 2) {
				addend = "0" + addend;
			}
			if (Integer.parseInt("" + bx[1], 16) + Integer.parseInt("" + addend.charAt(addend.length() - 1), 16) > 15) {
				AF = true;
			}
			if (sum == 0) {
				ZF = true;
				bx[0] = '0';
				bx[1] = '0';
			} else {
				if (sum > Integer.parseInt("ff", 16)) {

					CF = true;
					sum -= 256;
					sumStringForm = NumberToFourByteHexa("" + sum);
				}
				bx[0] = sumStringForm.charAt(sumStringForm.length() - 2);
				bx[1] = sumStringForm.charAt(sumStringForm.length() - 1);
			}
		} else if (first.equalsIgnoreCase("cl")) {
			String addend = contentsOfSecondOperandOfADDOneByte(second);
			int sum = Integer.parseInt(("" + cx[2] + "" + cx[3]), 16) + Integer.parseInt(addend, 16);// decimal sum
			String sumStringForm = NumberToFourByteHexa("" + sum);// sum is now hexa
			// sumStringForm = sumStringForm.substring(sumStringForm.length() - 2,
			// sumStringForm.length()); // took last byte
			// for AF
			while (addend.length() < 2) {
				addend = "0" + addend;
			}
			if (Integer.parseInt("" + cx[3], 16) + Integer.parseInt("" + addend.charAt(addend.length() - 1), 16) > 15) {
				AF = true;
			}
			if (sum == 0) {
				ZF = true;
				cx[2] = '0';
				cx[3] = '0';
			} else {
				if (sum > Integer.parseInt("ff", 16)) {

					CF = true;
					sum -= 256;
					sumStringForm = NumberToFourByteHexa("" + sum);
				}
				cx[2] = sumStringForm.charAt(sumStringForm.length() - 2);
				cx[3] = sumStringForm.charAt(sumStringForm.length() - 1);
			}
		} else if (first.equalsIgnoreCase("ch")) {
			String addend = contentsOfSecondOperandOfADDOneByte(second);

			int sum = Integer.parseInt(("" + cx[0] + "" + cx[1]), 16) + Integer.parseInt(addend, 16);// decimal sum
			String sumStringForm = NumberToFourByteHexa("" + sum);// sum is now hexa
			// sumStringForm = sumStringForm.substring(sumStringForm.length() - 2,
			// sumStringForm.length()); // took last byte
			// for AF
			while (addend.length() < 2) {
				addend = "0" + addend;
			}
			if (Integer.parseInt("" + cx[1], 16) + Integer.parseInt("" + addend.charAt(addend.length() - 1), 16) > 15) {
				AF = true;
			}
			if (sum == 0) {
				ZF = true;
				cx[0] = '0';
				cx[1] = '0';
			} else {
				if (sum > Integer.parseInt("ff", 16)) {

					CF = true;
					sum -= 256;
					sumStringForm = NumberToFourByteHexa("" + sum);
				}
				cx[0] = sumStringForm.charAt(sumStringForm.length() - 2);
				cx[1] = sumStringForm.charAt(sumStringForm.length() - 1);
			}
		} else if (first.equalsIgnoreCase("dl")) {
			String addend = contentsOfSecondOperandOfADDOneByte(second);

			int sum = Integer.parseInt(("" + dx[2] + "" + dx[3]), 16) + Integer.parseInt(addend, 16);// decimal sum
			String sumStringForm = NumberToFourByteHexa("" + sum);// sum is now hexa
			// sumStringForm = sumStringForm.substring(sumStringForm.length() - 2,
			// sumStringForm.length()); // took last byte
			// for AF
			while (addend.length() < 2) {
				addend = "0" + addend;
			}
			if (Integer.parseInt("" + dx[3], 16) + Integer.parseInt("" + addend.charAt(addend.length() - 1), 16) > 15) {
				AF = true;
			}
			if (sum == 0) {
				ZF = true;
				dx[2] = '0';
				dx[3] = '0';
			} else {
				if (sum > Integer.parseInt("ff", 16)) {

					CF = true;
					sum -= 256;
					sumStringForm = NumberToFourByteHexa("" + sum);
				}
				dx[2] = sumStringForm.charAt(sumStringForm.length() - 2);
				dx[3] = sumStringForm.charAt(sumStringForm.length() - 1);
			}
		} else if (first.equalsIgnoreCase("dh")) {
			String addend = contentsOfSecondOperandOfADDOneByte(second);
			int sum = Integer.parseInt(("" + dx[0] + "" + dx[1]), 16) + Integer.parseInt(addend, 16);// decimal sum
			String sumStringForm = NumberToFourByteHexa("" + sum);// sum is now hexa
			// sumStringForm = sumStringForm.substring(sumStringForm.length() - 2,
			// sumStringForm.length()); // took last byte
			// for AF
			while (addend.length() < 2) {
				addend = "0" + addend;
			}
			if (Integer.parseInt("" + dx[1], 16) + Integer.parseInt("" + addend.charAt(addend.length() - 1), 16) > 15) {
				AF = true;
			}
			if (sum == 0) {
				ZF = true;
				dx[0] = '0';
				dx[1] = '0';
			} else {
				if (sum > Integer.parseInt("ff", 16)) {

					CF = true;
					sum -= 256;
					sumStringForm = NumberToFourByteHexa("" + sum);
				}
				dx[0] = sumStringForm.charAt(sumStringForm.length() - 2);
				dx[1] = sumStringForm.charAt(sumStringForm.length() - 1);
			}
		} else if (first.equalsIgnoreCase("di")) {
			String addend = contentsOfSecondOperandOfADDTwoByte(second);
			int sum = Integer.parseInt((di[0] + "" + di[1] + "" + di[2] + "" + di[3]), 16)
					+ Integer.parseInt(addend, 16);
			String sumStringForm = NumberToFourByteHexa("" + sum);

			// for AF
			while (addend.length() < 4) {
				addend = "0" + addend;
			}
			if (Integer.parseInt("" + di[3], 16) + Integer.parseInt("" + addend.charAt(3), 16) > 15) {
				AF = true;
			}
			if (sum == 0) {
				ZF = true;
				di[0] = '0';
				di[1] = '0';
				di[2] = '0';
				di[3] = '0';
			} else {
				if (sum > Integer.parseInt("ffff", 16)) {
					CF = true;
					sum -= Integer.parseInt("10000", 16);
					sumStringForm = NumberToFourByteHexa("" + sum);
				}
				di[0] = sumStringForm.charAt(0);
				di[1] = sumStringForm.charAt(1);
				di[2] = sumStringForm.charAt(2);
				di[3] = sumStringForm.charAt(3);
			}
		} else if (first.equalsIgnoreCase("si")) {
			String addend = contentsOfSecondOperandOfADDTwoByte(second);
			int sum = Integer.parseInt((si[0] + "" + si[1] + "" + si[2] + "" + si[3]), 16)
					+ Integer.parseInt(addend, 16);
			String sumStringForm = NumberToFourByteHexa("" + sum);

			// for AF
			while (addend.length() < 4) {
				addend = "0" + addend;
			}
			if (Integer.parseInt("" + si[3], 16) + Integer.parseInt("" + addend.charAt(3), 16) > 15) {
				AF = true;
			}
			if (sum == 0) {
				ZF = true;
				si[0] = '0';
				si[1] = '0';
				si[2] = '0';
				si[3] = '0';
			} else {
				if (sum > Integer.parseInt("ffff", 16)) {
					CF = true;
					sum -= Integer.parseInt("10000", 16);
					sumStringForm = NumberToFourByteHexa("" + sum);
				}
				si[0] = sumStringForm.charAt(0);
				si[1] = sumStringForm.charAt(1);
				si[2] = sumStringForm.charAt(2);
				si[3] = sumStringForm.charAt(3);
			}

		} else if (first.equalsIgnoreCase("bp")) {
			String addend = contentsOfSecondOperandOfADDTwoByte(second);
			int sum = Integer.parseInt((bp[0] + "" + bp[1] + "" + bp[2] + "" + bp[3]), 16)
					+ Integer.parseInt(addend, 16);
			String sumStringForm = NumberToFourByteHexa("" + sum);

			// for AF
			while (addend.length() < 4) {
				addend = "0" + addend;
			}
			if (Integer.parseInt("" + bp[3], 16) + Integer.parseInt("" + addend.charAt(3), 16) > 15) {
				AF = true;
			}
			if (sum == 0) {
				ZF = true;
				bp[0] = '0';
				bp[1] = '0';
				bp[2] = '0';
				bp[3] = '0';
			} else {
				if (sum > Integer.parseInt("ffff", 16)) {
					CF = true;
					sum -= Integer.parseInt("10000", 16);
					sumStringForm = NumberToFourByteHexa("" + sum);
				}
				bp[0] = sumStringForm.charAt(0);
				bp[1] = sumStringForm.charAt(1);
				bp[2] = sumStringForm.charAt(2);
				bp[3] = sumStringForm.charAt(3);
			}
		}
	}

	/**
	 * a function that returns 4-digit-hexadecimal representation of parameter.
	 * 
	 * @param input as any type of representation
	 * @return returns 4-digit-hexadecimal number as a String
	 */
	public static String NumberToFourByteHexa(String s) {
		if (s.charAt(0) == 'a' || s.charAt(0) == 'b' || s.charAt(0) == 'c' || s.charAt(0) == 'd' || s.charAt(0) == 'e'
				|| s.charAt(0) == 'f') {
			System.out.println("Undefined symbol:" + s);
			System.exit(0);
		} else if (s.charAt(0) == '0') {// hexa
			if (s.charAt(s.length() - 1) == 'h') {
				s = s.substring(0, s.length() - 1);
			}
		} else if (s.indexOf("d") == s.length() - 1
				&& !(s.contains("a") || s.contains("b") || s.contains("c") || s.contains("e") || s.contains("f"))) { // deci
			s = s.substring(0, s.length() - 1);
			s = Integer.toHexString(Integer.valueOf(s));
		} else if (s.charAt(s.length() - 1) == 'h') {// hexa

			s = s.substring(0, s.length() - 1);
		} else if (!(s.contains("a") || s.contains("b") || s.contains("c") || s.contains("d") || s.contains("e")
				|| s.contains("f"))) {// number
			s = Integer.toHexString(Integer.valueOf(s));
		}

		if (s.length() > 4) {
			return s.substring(s.length() - 4, s.length());
		} else {
			while (s.length() < 4) {
				s = "0" + s;
			}
			return s;
		}
	}

	private void fillInstructions() {
		instructionList.add("mov");
		instructionList.add("add");
		instructionList.add("sub");
		instructionList.add("mul");
		instructionList.add("div");
		instructionList.add("xor");
		instructionList.add("or");
		instructionList.add("and");
		instructionList.add("not");
		instructionList.add("rcl");
		instructionList.add("rcr");
		instructionList.add("shl");
		instructionList.add("shr");
		instructionList.add("push");
		instructionList.add("pop");
		instructionList.add("nop");
		instructionList.add("cmp");
		instructionList.add("jmp");
		instructionList.add("jz");
		instructionList.add("jnz");
		instructionList.add("je");
		instructionList.add("jne");
		instructionList.add("ja");
		instructionList.add("jae");
		instructionList.add("jb");
		instructionList.add("jbe");
		instructionList.add("jnae");
		instructionList.add("jnb");
		instructionList.add("jnbe");
		instructionList.add("jnc");
		instructionList.add("jc");
		instructionList.add("int");

	}

	/**
	 * this function works with 8 bit registers. It handles several errors and moves
	 * source to destination
	 * 
	 * @param first:  destination of MOV operation
	 * @param second: source of MOV operation
	 */
	private void MOV_OneByteReg(String first, String second) {
		char[] temp = new char[2];
		boolean isVar = false;
		Variable var = null;
		Variable tempVar;
		Iterator<Variable> itr = variables.iterator();
		while (itr.hasNext()) {
			tempVar = itr.next();
			if (second.contains(tempVar.name)) {
				isVar = true;
				var = tempVar;
				break;
			}
		}
		if (second.contains("offset")) {
			String value = NumberToFourByteHexa("" + var.memoryIndex);
			for (int i = 0; i <= 1; i++) {
				temp[i] = '0';
			}
			for (int i = 0; i <= 1 && i < value.length(); i++) {
				temp[1 - i] = value.charAt(value.length() - i - 1);
			}
		} else if (second.contains("[") && second.contains("]")) {// memory
			if (second.charAt(0) == 'w') { // 2 byte
				System.out.println("#ERROR 13: Byte/Word Combination Not Allowed");
				System.exit(0);
			} else { // 1 byte
				if (second.charAt(0) == 'b') {
					second = second.substring(1); // got rid of "b"
				}
				second = second.substring(1, second.length() - 1); // got rid of "[" and "]"
				String num = "";

				if (isRegOneByte(second) || isRegTwoByte(second)) {// register
					if (second.equalsIgnoreCase("si")) {
						for (int i = 0; i <= 3; i++) {
							num += si[i];
						}
					} else if (second.equalsIgnoreCase("di")) {
						for (int i = 0; i <= 3; i++) {
							num += di[i];
						}
					} else if (second.equalsIgnoreCase("bp")) {
						for (int i = 0; i <= 3; i++) {
							num += bp[i];
						}
					} else if (second.equalsIgnoreCase("bx")) {
						for (int i = 0; i <= 3; i++) {
							num += bx[i];
						}
					} else {
						System.out.println("#ERROR 39: Bad Index Register ");
						System.exit(0);
					}
				} else if (isVar) {// variable
					if (var.type) {
						System.out.println("#ERROR 13: Byte/Word Combination Not Allowed");
						System.exit(0);
					} else {
						num = NumberToFourByteHexa(var.data);
						for (int i = 0; i <= 1; i++) {
							temp[i] = '0';
						}
						for (int i = 0; i <= 1 && i < num.length(); i++) {
							temp[1 - i] = num.charAt(num.length() - i - 1);
						}
						return;
					}
				} else {// number
					num = NumberToFourByteHexa(second);
				}

				temp[0] = '0';
				temp[1] = '0';
				if (memory[Integer.parseInt(num, 16)] == null) {

				} else if (Integer.parseInt(num, 16) < numberOfInstructions * 6
						|| Integer.parseInt(num, 16) >= 64 * 1024) {
					System.out.println("Address is not valid");
					System.exit(0);
				} else {
					num = memory[Integer.parseInt(num, 16)];
					for (int i = 0; i <= 1 && i < num.length(); i++) {
						temp[1 - i] = num.charAt(num.length() - i - 1);
					}
				}

			}

		} else if (isRegOneByte(second) || isRegTwoByte(second)) { // register
			if (second.equalsIgnoreCase("al")) {
				for (int i = 1; i >= 0; i--) {
					temp[i] = ax[i + 2];
				}
			} else if (second.equalsIgnoreCase("ah")) {
				for (int i = 1; i >= 0; i--) {
					temp[i] = ax[i];
				}
			} else if (second.equalsIgnoreCase("bl")) {
				for (int i = 1; i >= 0; i--) {
					temp[i] = bx[i + 2];
				}
			} else if (second.equalsIgnoreCase("bh")) {
				for (int i = 1; i >= 0; i--) {
					temp[i] = bx[i];
				}
			} else if (second.equalsIgnoreCase("cl")) {
				for (int i = 1; i >= 0; i--) {
					temp[i] = cx[i + 2];
				}
			} else if (second.equalsIgnoreCase("ch")) {
				for (int i = 1; i >= 0; i--) {
					temp[i] = cx[i];
				}
			} else if (second.equalsIgnoreCase("dl")) {
				for (int i = 1; i >= 0; i--) {
					temp[i] = dx[i + 2];
				}
			} else if (second.equalsIgnoreCase("dh")) {
				for (int i = 1; i >= 0; i--) {
					temp[i] = dx[i];
				}
			} else {
				System.out.println("#ERROR 13: Byte/Word Combination Not Allowed");
				System.exit(0);
			}
		} else { // number or variable
			if (isVar) {
				if (var.type) {
					System.out.println("#ERROR 13: Byte/Word Combination Not Allowed");
					System.exit(0);
				}
				second = NumberToFourByteHexa("0" + var.data + "h"); // variable
			} else {
				second = NumberToFourByteHexa(second); // number
			}
			if (Integer.parseInt(second, 16) > 255) {
				System.out.println("#ERROR 30: Byte-Sized Constant Required");
				System.exit(0);
			}

			for (int i = 0; i <= 1 && i < second.length(); i++) {
				temp[1 - i] = second.charAt(second.length() - i - 1);
			}
		}

		// temp has the value just insert it
		if (first.equalsIgnoreCase("al")) {
			for (int i = 0; i <= 1; i++) {
				ax[i + 2] = temp[i];
			}
		} else if (first.equalsIgnoreCase("ah")) {
			for (int i = 0; i <= 1; i++) {
				ax[i] = temp[i];
			}
		} else if (first.equalsIgnoreCase("bl")) {
			for (int i = 0; i <= 1; i++) {
				cx[i + 2] = temp[i];
			}
		} else if (first.equalsIgnoreCase("bh")) {
			for (int i = 0; i <= 1; i++) {
				dx[i] = temp[i];
			}
		} else if (first.equalsIgnoreCase("cl")) {
			for (int i = 0; i <= 1; i++) {
				cx[i + 2] = temp[i];
			}
		} else if (first.equalsIgnoreCase("ch")) {
			for (int i = 0; i <= 1; i++) {
				cx[i] = temp[i];
			}
		} else if (first.equalsIgnoreCase("dl")) {
			for (int i = 0; i <= 1; i++) {
				dx[i + 2] = temp[i];
			}
		} else if (first.equalsIgnoreCase("dh")) {
			for (int i = 0; i <= 1; i++) {
				dx[i] = temp[i];
			}
		}

	}

	/**
	 * this function works with 16 bit registers. It handles several errors and
	 * moves source to destination
	 * 
	 * @param first:  destination of MOV operation
	 * @param second: source of MOV operation
	 */
	private void MOV_TwoByteReg(String first, String second) {
		char[] temp = new char[4];
		for (int i = 0; i < 3; i++)
			temp[i] = '0';
		boolean isVar = false;
		Variable var = null;
		Variable tempVar;
		Iterator<Variable> itr = variables.iterator();
		while (itr.hasNext()) {
			tempVar = itr.next();
			if (second.contains(tempVar.name)) {
				isVar = true;
				var = tempVar;
				break;
			}
		}

		if (second.contains("offset")) {

			String value = NumberToFourByteHexa("" + var.memoryIndex);
			for (int i = 0; i <= 3; i++) {
				temp[i] = '0';
			}
			for (int i = 0; i <= 3 && i < value.length(); i++) {
				temp[3 - i] = value.charAt(value.length() - i - 1);
			}

		} else if (second.contains("[") && second.contains("]")) {

			if (second.charAt(0) == 'b') { // 1 byte
				System.out.println("#ERROR 13: Byte/Word Combination Not Allowed");
				System.exit(0);

			} else { // 2 byte
				if (second.charAt(0) == 'w') {
					second = second.substring(1); // got rid of 'w'
				}

				second = second.substring(1, second.length() - 1); // got rid of [ and ]
				String num = "";
				if (isRegOneByte(second) || isRegTwoByte(second)) {// register
					if (second.equalsIgnoreCase("si")) {
						for (int i = 0; i <= 3; i++) {
							num += si[i];
						}
					} else if (second.equalsIgnoreCase("di")) {
						for (int i = 0; i <= 3; i++) {
							num += di[i];
						}
					} else if (second.equalsIgnoreCase("bp")) {
						for (int i = 0; i <= 3; i++) {
							num += bp[i];
						}
					} else if (second.equalsIgnoreCase("bx")) {
						for (int i = 0; i <= 3; i++) {
							num += bx[i];
						}
					} else {
						System.out.println("#ERROR 39: Bad Index Register ");
						System.exit(0);
					}
				} else if (isVar) {// variable
					if (!var.type) {
						System.out.println("#ERROR 13: Byte/Word Combination Not Allowed");
						System.exit(0);
					} else {
						num = NumberToFourByteHexa(var.data);
						for (int i = 0; i <= 3; i++) {
							temp[i] = '0';
						}
						for (int i = 0; i <= 3 && i < num.length(); i++) {
							temp[3 - i] = num.charAt(num.length() - i - 1);
						}
						return;
					}
				} else {// number
					num = NumberToFourByteHexa(second);
				}

				// got the value, just insert to temp
				for (int i = 0; i <= 3; i++) {
					temp[i] = '0';
				}

				if (Integer.parseInt(num, 16) >= memory.length
						|| Integer.parseInt(num, 16) < numberOfInstructions * 6) {
					System.out.println("Address is not valid");
					System.exit(0);

				} else if (memory[Integer.parseInt(num, 16)] == null) {
				} else {
					String memoryLocaitonOfNum = memory[Integer.parseInt(num, 16)];
					for (int i = 0; i <= 3 && i < num.length(); i++) {
						temp[3 - i] = memoryLocaitonOfNum.charAt(memoryLocaitonOfNum.length() - i - 1);
					}
				}
			}
		} else if (isRegOneByte(second) || isRegTwoByte(second)) { // register

			if (second.equalsIgnoreCase("ax")) {
				for (int i = 3; i >= 0; i--) {
					temp[i] = ax[i];
				}
			} else if (second.equalsIgnoreCase("bx")) {
				for (int i = 3; i >= 0; i--) {
					temp[i] = bx[i];
				}
			} else if (second.equalsIgnoreCase("cx")) {
				for (int i = 3; i >= 0; i--) {
					temp[i] = cx[i];
				}
			} else if (second.equalsIgnoreCase("dx")) {
				for (int i = 3; i >= 0; i--) {
					temp[i] = dx[i];
				}
			} else if (second.equalsIgnoreCase("si")) {
				for (int i = 3; i >= 0; i--) {
					temp[i] = si[i];
				}
			} else if (second.equalsIgnoreCase("bp")) {
				for (int i = 3; i >= 0; i--) {
					temp[i] = bp[i];
				}
			} else if (second.equalsIgnoreCase("di")) {
				for (int i = 3; i >= 0; i--) {
					temp[i] = di[i];
				}
			} else {// error
				System.out.println("#ERROR 13: Error: Byte/Word Combination Not Allowed");
				System.exit(0);
			}
		} else { // number or variable
			if (isVar) {
				if (!var.type) {
					System.out.println("#ERROR 13: Byte/Word Combination Not Allowed");
					System.exit(0);
				}
				second = NumberToFourByteHexa("0" + var.data + "h");// variable
			} else {
				second = NumberToFourByteHexa(second); // number
			}
			for (int i = 0; i < 3; i++) {
				temp[i] = 0;
			}
			for (int i = 0; i <= 3 && i < second.length(); i++) {
				temp[3 - i] = second.charAt(second.length() - i - 1);
			}
		}
//temp has the value just insert it
		if (first.equalsIgnoreCase("ax")) {
			for (int i = 0; i <= 3; i++) {
				ax[i] = temp[i];
			}
		} else if (first.equalsIgnoreCase("bx")) {
			for (int i = 0; i <= 3; i++) {
				bx[i] = temp[i];
			}
		} else if (first.equalsIgnoreCase("cx")) {
			for (int i = 0; i <= 3; i++) {
				cx[i] = temp[i];
			}
		} else if (first.equalsIgnoreCase("dx")) {
			for (int i = 0; i <= 3; i++) {
				dx[i] = temp[i];
			}
		} else if (first.equalsIgnoreCase("di")) {
			for (int i = 0; i <= 3; i++) {
				di[i] = temp[i];
			}
		} else if (first.equalsIgnoreCase("si")) {
			for (int i = 0; i <= 3; i++) {
				si[i] = temp[i];
			}
		} else if (first.equalsIgnoreCase("bp")) {
			for (int i = 0; i <= 3; i++) {
				bp[i] = temp[i];
			}
		}

	}

	/**
	 * this function just calculates source(addend of addition operation) of ADD
	 * operation when first operand is 16 bit
	 * 
	 * @param second source operand of ADD operation
	 * @return source operand of ADD in hexadecimal representation
	 */
	private String contentsOfSecondOperandOfADDTwoByte(String second) {

		boolean isVar = false;
		Variable var = null;
		Variable temp;
		Iterator<Variable> itr = variables.iterator();
		while (itr.hasNext()) {
			temp = itr.next();
			if (second.contains(temp.name)) {
				isVar = true;
				var = temp;
				break;
			}
		}
		String addend = "";
		if (second.contains("offset")) {
			second = NumberToFourByteHexa("" + var.memoryIndex);
			addend += second;
		} else if (second.contains("[") && second.contains("]")) {

			if (second.charAt(0) == 'b') { // 1 byte
				System.out.println("#ERROR 13: Byte/Word Combination Not Allowed");
				System.exit(0);

			} else { // 2 byte
				if (second.charAt(0) == 'w') {
					second = second.substring(1); // got rid of 'w'
				}

				second = second.substring(1, second.length() - 1); // got rid of [ and ]

				if (isRegOneByte(second) || isRegTwoByte(second)) {// register
					if (second.equalsIgnoreCase("si")) {
						for (int i = 0; i <= 3; i++) {
							addend += si[i];
						}
					} else if (second.equalsIgnoreCase("di")) {
						for (int i = 0; i <= 3; i++) {
							addend += di[i];
						}
					} else if (second.equalsIgnoreCase("bp")) {
						for (int i = 0; i <= 3; i++) {
							addend += bp[i];
						}
					} else if (second.equalsIgnoreCase("bx")) {
						for (int i = 0; i <= 3; i++) {
							addend += bx[i];
						}
					} else {
						System.out.println("#ERROR 39: Bad Index Register ");
						System.exit(0);
					}
				} else if (isVar) {// variable
					if (!var.type) {
						System.out.println("#ERROR 13: Byte/Word Combination Not Allowed");
						System.exit(0);
					} else {
						addend += NumberToFourByteHexa(var.data);
					}
				} else {// number
					addend += NumberToFourByteHexa(second);
				}

			}
			// addend is a four length hexadecimal number which contains memory address
			if (Integer.parseInt(addend, 16) >= memory.length
					|| Integer.parseInt(addend, 16) < numberOfInstructions * 6) {
				System.out.println("Address is not valid");
				System.exit(0);

			} else if (memory[Integer.parseInt(addend, 16)] == null) {
				addend = "0";
			} else {
				addend = memory[Integer.parseInt(addend, 16)];
			} // addend is now the content of that address

		} else if (isRegOneByte(second) || isRegTwoByte(second)) { // addend is register
			if (second.equalsIgnoreCase("ax")) {
				for (int i = 3; i >= 0; i--) {
					addend += ax[3 - i];
				}
			} else if (second.equalsIgnoreCase("bx")) {
				for (int i = 3; i >= 0; i--) {
					addend += bx[3 - i];
				}
			} else if (second.equalsIgnoreCase("cx")) {
				for (int i = 3; i >= 0; i--) {
					addend += cx[3 - i];
				}
			} else if (second.equalsIgnoreCase("dx")) {
				for (int i = 3; i >= 0; i--) {
					addend += dx[3 - i];
				}
			} else if (second.equalsIgnoreCase("si")) {
				for (int i = 3; i >= 0; i--) {
					addend += si[3 - i];
				}
			} else if (second.equalsIgnoreCase("bp")) {
				for (int i = 3; i >= 0; i--) {
					addend += bp[3 - i];
				}
			} else if (second.equalsIgnoreCase("di")) {
				for (int i = 3; i >= 0; i--) {
					addend += di[3 - i];
				}
			} else {// error
				System.out.println("#ERROR 13: Error: Byte/Word Combination Not Allowed");
				System.exit(0);
			}
		} else { // number or variable
			if (isVar) {
				if (!var.type) {
					System.out.println("#ERROR 13: Byte/Word Combination Not Allowed");
					System.exit(0);
				}
				second = NumberToFourByteHexa(var.data); // variable
			} else {
				second = NumberToFourByteHexa(second); // number
			}
			addend += second;
		}
		return addend;
	}

	/**
	 * this function just calculates source(addend of addition operation) of ADD
	 * operation when first operand is 8 bit
	 * 
	 * @param second source operand of ADD operation
	 * @return source operand of ADD in hexadecimal representation
	 */
	private String contentsOfSecondOperandOfADDOneByte(String second) {
		boolean isVar = false;
		Variable var = null;
		Variable temp;
		Iterator<Variable> itr = variables.iterator();
		while (itr.hasNext()) {
			temp = itr.next();
			if (second.contains(temp.name)) {
				isVar = true;
				var = temp;
				break;
			}
		}
		String addend = "";
		if (second.contains("offset")) {
			second = NumberToFourByteHexa("" + (var.memoryIndex));
			addend += second;
			addend = addend.substring(addend.length() - 2, addend.length());// took last byte
		} else if (second.contains("[") && second.contains("]")) {

			if (second.charAt(0) == 'w') { // 2 byte
				System.out.println("#ERROR 13: Byte/Word Combination Not Allowed");
				System.exit(0);

			} else { // 1 byte
				if (second.charAt(0) == 'b') {
					second = second.substring(1); // got rid of 'b'
				}

				second = second.substring(1, second.length() - 1); // got rid of [ and ]

				if (isRegOneByte(second) || isRegTwoByte(second)) {// register
					if (second.equalsIgnoreCase("si")) {
						for (int i = 0; i <= 3; i++) {
							addend += si[i];
						}
					} else if (second.equalsIgnoreCase("di")) {
						for (int i = 0; i <= 3; i++) {
							addend += di[i];
						}
					} else if (second.equalsIgnoreCase("bp")) {
						for (int i = 0; i <= 3; i++) {
							addend += bp[i];
						}
					} else if (second.equalsIgnoreCase("bx")) {
						for (int i = 0; i <= 3; i++) {
							addend += bx[i];
						}
					} else {
						System.out.println("#ERROR 39: Bad Index Register ");
						System.exit(0);
					}
				} else if (isVar) {// variable
					if (var.type) {
						System.out.println("#ERROR 13: Byte/Word Combination Not Allowed");
						System.exit(0);
					} else {
						addend = NumberToFourByteHexa(var.data); // variable
					}
				} else {// number
					addend = NumberToFourByteHexa(second); // number
				}	
			}
			// addend is a four length hexadecimal number which contains memory address
			if (Integer.parseInt(addend, 16) >= memory.length
					|| Integer.parseInt(addend, 16) < numberOfInstructions * 6) {
				System.out.println("Address is not valid");
				System.exit(0);

			} else if (memory[Integer.parseInt(addend, 16)] == null) {
				addend = "0";
			} else {
				addend = memory[Integer.parseInt(addend, 16)];
			} // addend is now the content of that address
			addend = addend.substring(addend.length() - 2, addend.length());// took last byte
		} else if (isRegOneByte(second) || isRegTwoByte(second)) { // addend is register

			if (second.equalsIgnoreCase("al")) {
				addend += "" + ax[2] + "" + ax[3];
			} else if (second.equalsIgnoreCase("ah")) {
				addend += "" + ax[0] + "" + ax[1];
			} else if (second.equalsIgnoreCase("bl")) {
				addend += "" + bx[2] + "" + bx[3];
			} else if (second.equalsIgnoreCase("bh")) {
				addend += "" + bx[0] + "" + bx[1];
			} else if (second.equalsIgnoreCase("cl")) {
				addend += "" + cx[2] + "" + cx[3];
			} else if (second.equalsIgnoreCase("ch")) {
				addend += "" + cx[0] + "" + cx[1];
			} else if (second.equalsIgnoreCase("dl")) {
				addend += "" + dx[2] + "" + dx[3];
			} else if (second.equalsIgnoreCase("dh")) {
				addend += "" + dx[0] + "" + dx[1];
			} else {// error
				System.out.println("#ERROR 13: Error: Byte/Word Combination Not Allowed");
				System.exit(0);
			}
		} else { // number or variable
			if (isVar) {
				if (var.type) {
					System.out.println("#ERROR 13: Byte/Word Combination Not Allowed");
					System.exit(0);
				}
				second = NumberToFourByteHexa(var.data); // variable
			} else {
				second = NumberToFourByteHexa(second); // number
				if (Integer.parseInt(second, 16) > 255) {
					System.out.println("#ERROR 30: Byte-Sized Constant Required");
					System.exit(0);
				}
			}
			addend += second;
		}
		return addend;
	}

	private boolean isRegTwoByte(String regg) {
		return regg.equalsIgnoreCase("ax") || regg.equalsIgnoreCase("cx") || regg.equalsIgnoreCase("bx")
				|| regg.equalsIgnoreCase("dx") || regg.equalsIgnoreCase("di") || regg.equalsIgnoreCase("si")
				|| regg.equalsIgnoreCase("bp");
	}

	private boolean isRegOneByte(String regg) {
		return regg.equalsIgnoreCase("ah") || regg.equalsIgnoreCase("al") || regg.equalsIgnoreCase("bl")
				|| regg.equalsIgnoreCase("bh") || regg.equalsIgnoreCase("ch") || regg.equalsIgnoreCase("cl")
				|| regg.equalsIgnoreCase("dl") || regg.equalsIgnoreCase("dh");
	}

}
