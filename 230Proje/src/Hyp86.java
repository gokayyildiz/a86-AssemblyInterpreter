import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;
//TODO
//variable önünde w olduğu veya b olduğu sürece typeı hiç önemli değil.

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
		//TODO
		//bunu yapmıycaktık
		//fileAsString = fileAsString.toLowerCase();

		Scanner scanner = new Scanner(fileAsString);
		String line;
		Scanner token;
		String isChar1;

		int indexCursor = 0;
		boolean int20hCame = false;
		while (scanner.hasNextLine()) {
			line = scanner.nextLine();
			token = new Scanner(line);
			String first = token.next().toLowerCase();

			if (!int20hCame && instructionList.contains(first)) {// means instruction
				numberOfInstructions++; // Veyis add this

				memory[indexCursor] = first;

				line = line.trim();
				line = line.substring(first.length());
				if (line.indexOf(',') != -1) {
					int temo = line.indexOf(',');
					memory[indexCursor + 1] = line.substring(0, temo).trim().toLowerCase();
					isChar1 = line.substring(temo + 1, line.length()).trim();

					if (isChar1.contains("\'")) {// when var.data is 'x'
						int a = isChar1.charAt(isChar1.indexOf("\'") + 1) + 1 - 1;
						isChar1 = NumberToFourByteHexa("" + a);
					} else if (isChar1.contains("\"")) {// when var.data is "x"
						int a = isChar1.charAt(isChar1.indexOf("\"") + 1) + 1 - 1;
						isChar1 = NumberToFourByteHexa("" + a);
					}

					memory[indexCursor + 2] = isChar1.toLowerCase();
				} else {
					isChar1 = line.trim();
					if (isChar1.contains("\'")) {// when var.data is 'x'
						int a = isChar1.charAt(isChar1.indexOf("\'") + 1) + 1 - 1;
						isChar1 = NumberToFourByteHexa("" + a);
					} else if (isChar1.contains("\"")) {// when var.data is "x"
						int a = isChar1.charAt(isChar1.indexOf("\"") + 1) + 1 - 1;
						isChar1 = NumberToFourByteHexa("" + a);
					}

					memory[indexCursor + 1] = isChar1;
				}
				if (first.equals("int") && token.next().equals("20h")) {
					int20hCame = true;
				}
				indexCursor += 6;

			}
			if (line.indexOf(":") != -1) {// means label
				line = line.trim().substring(0, line.length() - 1);
				labels.put(line, indexCursor);
				continue;
			}

			if (line.indexOf("dw") != -1 || line.indexOf("db") != -1) {// variable definition

				if (token.next().equals("dw")) {
					variables.add(new Variable(first, 0, token.next(), true));
				} else {
					variables.add(new Variable(first, 0, token.next(), false));
				}
			}

			token.close();

		}
		Variable x;
		String dataVar;
		for (int i = 0; i < variables.size(); i++) {
			x = variables.get(i);
			if (x.getType() == true) {
				memory[indexCursor] = x.getData().substring(0, 2);
				memory[indexCursor + 1] = x.getData().substring(2);
				x.setMemoryIndex(indexCursor);
				indexCursor += 2;
			} else {
				memory[indexCursor] = x.getData().substring(2);
				x.setMemoryIndex(indexCursor);
				indexCursor += 1;
			}

		}

		scanner.close();

	}

	private String execute_helper_isVar(String token) {
		Variable var = null;
		Variable temp;
		Iterator<Variable> itr = variables.iterator();
		while (itr.hasNext()) {
			temp = itr.next();
			if (token.contains(temp.getName())) {
				var = temp;
				if (token.contains("offset")) {
					return var.getMemoryIndex() + "d";
				}
				return "[" + var.getMemoryIndex() + "d]";
			}
		}
		return null;
	}

	public void execute() {
		String first, second;
		MP = 0;
		while (true) {
			if (memory[MP].equals("int")) {
				if (memory[MP + 1].equals("20h")) {
					System.exit(0);
				} else {
					int21h();
				}
			} else if (memory[MP].equals("mov")) {
				first = execute_helper_isVar(memory[MP + 1]);
				second = execute_helper_isVar(memory[MP + 2]);
				if (first == null) {
					first = memory[MP + 1];
				}
				if (second == null) {
					second = memory[MP + 2];
				}

				mov(first, second);

			} else if (memory[MP].equals("add")) {
				first = execute_helper_isVar(memory[MP + 1]);
				second = execute_helper_isVar(memory[MP + 2]);
				if (first == null) {
					first = memory[MP + 1];
				}
				if (second == null) {
					second = memory[MP + 2];
				}

				add(first, second);
			} else if (memory[MP].equals("sub")) {
				first = execute_helper_isVar(memory[MP + 1]);
				second = execute_helper_isVar(memory[MP + 2]);
				if (first == null) {
					first = memory[MP + 1];
				}
				if (second == null) {
					second = memory[MP + 2];
				}

				sub(first, second);
			} else if (memory[MP].equals("cmp")) {
				first = execute_helper_isVar(memory[MP + 1]);
				second = execute_helper_isVar(memory[MP + 2]);
				if (first == null) {
					first = memory[MP + 1];
				}
				if (second == null) {
					second = memory[MP + 2];
				}

				cmp(first, second);
			} else if (memory[MP].equals("and")) {
				first = execute_helper_isVar(memory[MP + 1]);
				second = execute_helper_isVar(memory[MP + 2]);
				if (first == null) {
					first = memory[MP + 1];
				}
				if (second == null) {
					second = memory[MP + 2];
				}

				and(first, second);
			} else if (memory[MP].equals("or")) {
				first = execute_helper_isVar(memory[MP + 1]);
				second = execute_helper_isVar(memory[MP + 2]);
				if (first == null) {
					first = memory[MP + 1];
				}
				if (second == null) {
					second = memory[MP + 2];
				}

				or(first, second);
			} else if (memory[MP].equals("xor")) {
				first = execute_helper_isVar(memory[MP + 1]);
				second = execute_helper_isVar(memory[MP + 2]);
				if (first == null) {
					first = memory[MP + 1];
				}
				if (second == null) {
					second = memory[MP + 2];
				}

				xor(first, second);
			} else if (memory[MP].equals("shl")) {
				first = execute_helper_isVar(memory[MP + 1]);
				second = execute_helper_isVar(memory[MP + 2]);
				if (first == null) {
					first = memory[MP + 1];
				}
				if (second == null) {
					second = memory[MP + 2];
				}

				shl(first, second);
			} else if (memory[MP].equals("shr")) {
				first = execute_helper_isVar(memory[MP + 1]);
				second = execute_helper_isVar(memory[MP + 2]);
				if (first == null) {
					first = memory[MP + 1];
				}
				if (second == null) {
					second = memory[MP + 2];
				}

				shr(first, second);
			} else if (memory[MP].equals("rcl")) {
				first = execute_helper_isVar(memory[MP + 1]);
				second = execute_helper_isVar(memory[MP + 2]);
				if (first == null) {
					first = memory[MP + 1];
				}
				if (second == null) {
					second = memory[MP + 2];
				}

				// rcl(first , second);
			} else if (memory[MP].equals("rcr")) {
				first = execute_helper_isVar(memory[MP + 1]);
				second = execute_helper_isVar(memory[MP + 2]);
				if (first == null) {
					first = memory[MP + 1];
				}
				if (second == null) {
					second = memory[MP + 2];
				}

				// rcr(first , second);
			} else if (memory[MP].equals("mul")) {
				first = execute_helper_isVar(memory[MP + 1]);
				if (first == null) {
					first = memory[MP + 1];
				}

				mul(first);
			} else if (memory[MP].equals("div")) {
				first = execute_helper_isVar(memory[MP + 1]);
				if (first == null) {
					first = memory[MP + 1];
				}

				div(first);
			} else if (memory[MP].equals("push")) {
				first = execute_helper_isVar(memory[MP + 1]);
				if (first == null) {
					first = memory[MP + 1];
				}

				push(first);
			} else if (memory[MP].equals("pop")) {
				first = execute_helper_isVar(memory[MP + 1]);
				if (first == null) {
					first = memory[MP + 1];
				}

				pop(first);
			} else if (memory[MP].equals("not")) {
				first = execute_helper_isVar(memory[MP + 1]);
				if (first == null) {
					first = memory[MP + 1];
				}

				// not(first);
			} else if (memory[MP].equals("inc")) {
				first = execute_helper_isVar(memory[MP + 1]);
				if (first == null) {
					first = memory[MP + 1];
				}

				add(first,"1");
			} else if (memory[MP].equals("dec")) {
				first = execute_helper_isVar(memory[MP + 1]);
				if (first == null) {
					first = memory[MP + 1];
				}

				sub(first,"1");
			} else if (memory[MP].equals("jmp")) {
				jmp(memory[MP + 1]);
				continue;
			} else if (memory[MP].equals("jz")) {
				if (ZF) {
					jmp(memory[MP + 1]);
					continue;
				}
			} else if (memory[MP].equals("jnz")) {
				if (!ZF) {
					jmp(memory[MP + 1]);
					continue;
				}
			}

			MP += 6;
		}

	}

	public void jmp(String label) {
		if (labels.containsKey(label)) {
			MP = labels.get(label);
		} else {
			System.out.println("label does not exist");
			System.exit(0);
		}
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
//TODO
			// var parts are gonna be deleted
			int a = 0;
			for (int i = 0; i < variables.size(); i++) {
				if (variables.get(i).getName().equals(reg) && variables.get(i).getType()) {
					memory[index] = variables.get(i).getData();
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
					if (variables.get(i).getType()) {
						for (int j = 0; j < numero; j++) {
							regg = "" + variables.get(i).getData().charAt(0) + variables.get(i).getData().charAt(1)
									+ variables.get(i).getData().charAt(2) + variables.get(i).getData().charAt(3);
							otherMult = Integer.parseInt(regg, 16);
							if (Integer.parseInt("" + variables.get(i).getData().charAt(0), 16) <= 7)
								CF = false;
							else
								CF = true;

							mov("[" + variables.get(i).getMemoryIndex() + "]", "" + otherMult * 2);
						}
					} else {
						for (int j = 0; j < numero; j++) {
							regg = "" + variables.get(i).getData().charAt(2) + variables.get(i).getData().charAt(3);
							otherMult = Integer.parseInt(regg, 16);
							if (Integer.parseInt("" + variables.get(i).getData().charAt(2), 16) <= 7)
								CF = false;
							else
								CF = true;

							mov("[" + variables.get(i).getMemoryIndex() + "]", "" + otherMult * 2);
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
					if (variables.get(i).getType()) {
						for (int j = 0; j < numero; j++) {
							regg = "" + variables.get(i).getData().charAt(0) + variables.get(i).getData().charAt(1)
									+ variables.get(i).getData().charAt(2) + variables.get(i).getData().charAt(3);
							otherMult = Integer.parseInt(regg, 16);
							if (Integer.parseInt("" + variables.get(i).getData().charAt(0), 16) % 2 == 0)
								CF = false;
							else
								CF = true;

							mov("[" + variables.get(i).getMemoryIndex() + "]", "" + otherMult / 2);
						}
					} else {
						for (int j = 0; j < numero; j++) {
							regg = "" + variables.get(i).getData().charAt(2) + variables.get(i).getData().charAt(3);
							otherMult = Integer.parseInt(regg, 16);
							if (Integer.parseInt("" + variables.get(i).getData().charAt(2), 16) % 2 == 0)
								CF = false;
							else
								CF = true;

							mov("[" + variables.get(i).getMemoryIndex() + "]", "" + otherMult / 2);
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
//		conc.close();

	}

	/**
	 * all compare instructions and methods are based on the design of MOV
	 * instructions
	 * 
	 * @param first
	 * @param second
	 */
	public void cmp(String first, String second) {
		if (first.contains("[") && first.contains("]")) { // first is memory
			cmp_mem_xx(first, second);
		} else if (isRegTwoByte(first)) {// 16 bit register
			CMP_TwoByteReg(first, second);
		} else if (isRegOneByte(first)) {// 8 bit register
			CMP_OneByteReg(first, second);
		} else { // reg veya memoryye yazmiyo hata ver
			System.out.println("Undefined symbols are listed: " + first);
			System.exit(0);
		}

	}

	private void cmp_mem_xx(String first, String second) { //
		String source;

		if (first.charAt(0) == 'b') {// first operand is kind of b[xx] so source must be one byte.
			first = first.substring(2, first.length() - 1);// got rid of "b[" and "]"
			source = contentsOfSecondOperandOfADDSUBOneByte(second);
		} else {// assume there is w
			if (first.charAt(0) == 'w') {// first operand is kind of w[xx] so source must be two byte.
				first = first.substring(1); // got rid of "w"
			}
			first = first.substring(1, first.length() - 1);// got rid of "[" and "]"
			source = contentsOfSecondOperandOfADDSUBTwoByte(second);
		}

		int memoryIndex = memoryIndexOfFirst(first);
		int dest = Integer.parseInt(memory[memoryIndex], 16);
		int src = Integer.parseInt(source, 16);

		if (dest > src) {
			CF = false;
			ZF = false;
		} else if (dest < src) {
			CF = true;
			ZF = false;
		} else {
			ZF = true;
		}

	}

	private void CMP_TwoByteReg(String first, String second) {
		int destValue = 0;
		int srcValue = 0;

		if (second.contains("[") && second.contains("]")) {

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
				} else {// number
					num = NumberToFourByteHexa(second);
				}

				if (Integer.parseInt(num, 16) >= memory.length
						|| Integer.parseInt(num, 16) < numberOfInstructions * 6) {
					System.out.println("Address is not valid");
					System.exit(0);

				} else if (memory[Integer.parseInt(num, 16)] == null) {
				} else {
					srcValue = Integer.parseInt(memory[Integer.parseInt(num, 16)], 16);

				}
			}
		} else if (isRegOneByte(second) || isRegTwoByte(second)) { // register

			if (second.equalsIgnoreCase("ax")) {
				srcValue = Integer.parseInt("" + ax[0] + ax[1] + ax[2] + ax[3], 16);
			} else if (second.equalsIgnoreCase("bx")) {
				srcValue = Integer.parseInt("" + bx[0] + bx[1] + bx[2] + bx[3], 16);
			} else if (second.equalsIgnoreCase("cx")) {
				srcValue = Integer.parseInt("" + cx[0] + cx[1] + cx[2] + cx[3], 16);
			} else if (second.equalsIgnoreCase("dx")) {
				srcValue = Integer.parseInt("" + dx[0] + dx[1] + dx[2] + dx[3], 16);
			} else if (second.equalsIgnoreCase("si")) {
				srcValue = Integer.parseInt("" + si[0] + si[1] + si[2] + si[3], 16);
			} else if (second.equalsIgnoreCase("bp")) {
				srcValue = Integer.parseInt("" + bp[0] + bp[1] + bp[2] + bp[3], 16);
			} else if (second.equalsIgnoreCase("di")) {
				srcValue = Integer.parseInt("" + di[0] + di[1] + di[2] + di[3], 16);
			} else {// error
				System.out.println("#ERROR 13: Error: Byte/Word Combination Not Allowed");
				System.exit(0);
			}
		} else { // number or variable
			srcValue = Integer.parseInt(NumberToFourByteHexa(second), 16); // number
		}

		if (first.equalsIgnoreCase("ax")) {
			destValue = Integer.parseInt("" + ax[0] + ax[1] + ax[2] + ax[3], 16);
		} else if (first.equalsIgnoreCase("bx")) {
			destValue = Integer.parseInt("" + bx[0] + bx[1] + bx[2] + bx[3], 16);
		} else if (first.equalsIgnoreCase("cx")) {
			destValue = Integer.parseInt("" + cx[0] + cx[1] + cx[2] + cx[3], 16);
		} else if (first.equalsIgnoreCase("dx")) {
			destValue = Integer.parseInt("" + dx[0] + dx[1] + dx[2] + dx[3], 16);
		} else if (first.equalsIgnoreCase("si")) {
			destValue = Integer.parseInt("" + si[0] + si[1] + si[2] + si[3], 16);
		} else if (first.equalsIgnoreCase("bp")) {
			destValue = Integer.parseInt("" + bp[0] + bp[1] + bp[2] + bp[3], 16);
		} else if (first.equalsIgnoreCase("di")) {
			destValue = Integer.parseInt("" + di[0] + di[1] + di[2] + di[3], 16);
		} else {// error
			System.out.println("#ERROR 13: Error: Byte/Word Combination Not Allowed");
			System.exit(0);
		}

		if (destValue > srcValue) {
			CF = false;
			ZF = false;
		} else if (destValue < srcValue) {
			CF = true;
			ZF = false;
		} else {
			ZF = true;
		}

	}

	private void CMP_OneByteReg(String first, String second) {
		int destValue = 0;
		int srcValue = 0;
		boolean isVar = false;
		Variable var = null;
		Variable tempVar;
		Iterator<Variable> itr = variables.iterator();
		while (itr.hasNext()) {
			tempVar = itr.next();
			if (second.contains(tempVar.getName())) {
				isVar = true;
				var = tempVar;
				break;
			}
		}
		if (second.contains("offset")) {
			srcValue = var.getMemoryIndex();

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
					if (var.getType()) {
						System.out.println("#ERROR 13: Byte/Word Combination Not Allowed");
						System.exit(0);
					} else {
						num = NumberToFourByteHexa(var.getData());

					}
				} else {// number
					num = NumberToFourByteHexa(second);
				}

				if (memory[Integer.parseInt(num, 16)] == null) {

				} else if (Integer.parseInt(num, 16) < numberOfInstructions * 6
						|| Integer.parseInt(num, 16) >= 64 * 1024) {
					System.out.println("Address is not valid");
					System.exit(0);
				} else {
					String memoryLocaitonOfNum = memory[Integer.parseInt(num, 16)];
					srcValue = Integer.parseInt(memoryLocaitonOfNum, 16);
				}

			}

		} else if (isRegOneByte(second) || isRegTwoByte(second)) { // register
			if (second.equalsIgnoreCase("ah")) {
				srcValue = Integer.parseInt("" + ax[0] + ax[1], 16);
			} else if (second.equalsIgnoreCase("bh")) {
				srcValue = Integer.parseInt("" + bx[0] + bx[1], 16);
			} else if (second.equalsIgnoreCase("ch")) {
				srcValue = Integer.parseInt("" + cx[0] + cx[1], 16);
			} else if (second.equalsIgnoreCase("dh")) {
				srcValue = Integer.parseInt("" + dx[0] + dx[1], 16);
			} else if (second.equalsIgnoreCase("al")) {
				srcValue = Integer.parseInt("" + ax[2] + ax[3], 16);
			} else if (second.equalsIgnoreCase("bl")) {
				srcValue = Integer.parseInt("" + bx[2] + bx[3], 16);
			} else if (second.equalsIgnoreCase("cl")) {
				srcValue = Integer.parseInt("" + cx[2] + cx[3], 16);
			} else if (second.equalsIgnoreCase("dl")) {
				srcValue = Integer.parseInt("" + dx[2] + dx[3], 16);
			} else {
				System.out.println("#ERROR 13: Byte/Word Combination Not Allowed");
				System.exit(0);
			}
		} else { // number or variable
			if (isVar) {
				if (var.getType()) {
					System.out.println("#ERROR 13: Byte/Word Combination Not Allowed");
					System.exit(0);
				}
				srcValue = Integer.parseInt(NumberToFourByteHexa("0" + var.getData() + "h"), 16); // variable
			} else {
				srcValue = Integer.parseInt(NumberToFourByteHexa(second), 16); // number
			}
			if (Integer.parseInt(second, 16) > 255) {
				System.out.println("#ERROR 30: Byte-Sized Constant Required");
				System.exit(0);
			}
		}

		// destValue
		if (first.equalsIgnoreCase("ah")) {
			destValue = Integer.parseInt("" + ax[0] + ax[1], 16);
		} else if (first.equalsIgnoreCase("bh")) {
			destValue = Integer.parseInt("" + bx[0] + bx[1], 16);
		} else if (first.equalsIgnoreCase("ch")) {
			destValue = Integer.parseInt("" + cx[0] + cx[1], 16);
		} else if (first.equalsIgnoreCase("dh")) {
			destValue = Integer.parseInt("" + dx[0] + dx[1], 16);
		} else if (first.equalsIgnoreCase("al")) {
			destValue = Integer.parseInt("" + ax[2] + ax[3], 16);
		} else if (first.equalsIgnoreCase("bl")) {
			destValue = Integer.parseInt("" + bx[2] + bx[3], 16);
		} else if (first.equalsIgnoreCase("cl")) {
			destValue = Integer.parseInt("" + cx[2] + cx[3], 16);
		} else if (first.equalsIgnoreCase("dl")) {
			destValue = Integer.parseInt("" + dx[2] + dx[3], 16);
		}

		if (destValue > srcValue) {
			CF = false;
			ZF = false;
		} else if (destValue < srcValue) {
			CF = true;
			ZF = false;
		} else {
			ZF = true;
		}

	}

	public void xor(String first, String second) {
		CF = false;
		OF = false;
		if (first.contains("[") && first.contains("]")) { // first is memory
			String source = source_when_first_operand_is_memory(first, second);
			int memoryIndex = memoryIndexOfFirst(first);
			memory[memoryIndex] = helperXor(memory[memoryIndex], source);

		} else if (isRegTwoByte(first)) {// 16 bit register

			String source = source_when_first_operand_is_twoByteReg(second);
			if (first.equalsIgnoreCase("ax")) {
				String data = helperXor("" + ax[0] + ax[1] + ax[2] + ax[3], source);
				for (int i = 0; i <= 3; i++) {
					ax[i] = data.charAt(i);
				}
			} else if (first.equalsIgnoreCase("bx")) {
				String data = helperXor("" + bx[0] + bx[1] + bx[2] + bx[3], source);
				for (int i = 0; i <= 3; i++) {
					bx[i] = data.charAt(i);
				}
			} else if (first.equalsIgnoreCase("cx")) {
				String data = helperXor("" + cx[0] + cx[1] + cx[2] + cx[3], source);
				for (int i = 0; i <= 3; i++) {
					cx[i] = data.charAt(i);
				}
			} else if (first.equalsIgnoreCase("dx")) {
				String data = helperXor("" + dx[0] + dx[1] + dx[2] + dx[3], source);
				for (int i = 0; i <= 3; i++) {
					dx[i] = data.charAt(i);
				}
			} else if (first.equalsIgnoreCase("di")) {
				String data = helperXor("" + di[0] + di[1] + di[2] + di[3], source);
				for (int i = 0; i <= 3; i++) {
					di[i] = data.charAt(i);
				}
			} else if (first.equalsIgnoreCase("si")) {
				String data = helperXor("" + si[0] + si[1] + si[2] + si[3], source);
				for (int i = 0; i <= 3; i++) {
					si[i] = data.charAt(i);
				}
			} else if (first.equalsIgnoreCase("bp")) {
				String data = helperXor("" + bp[0] + bp[1] + bp[2] + bp[3], source);
				for (int i = 0; i <= 3; i++) {
					bp[i] = data.charAt(i);
				}
			}
		} else if (isRegOneByte(first)) {// 8 bit register
			String source = source_when_first_operand_is_oneByteReg(second);
			if (first.equalsIgnoreCase("al")) {
				String data = helperXor("" + ax[2] + ax[3], source);
				for (int i = 0; i <= 1; i++) {
					ax[i + 2] = data.charAt(i + 2);
				}
			} else if (first.equalsIgnoreCase("ah")) {
				String data = helperXor("" + ax[0] + ax[1], source);
				for (int i = 0; i <= 1; i++) {
					ax[i] = data.charAt(i + 2);
				}
			} else if (first.equalsIgnoreCase("bl")) {
				String data = helperXor("" + ax[2] + ax[3], source);
				for (int i = 0; i <= 1; i++) {
					cx[i + 2] = data.charAt(i + 2);
				}
			} else if (first.equalsIgnoreCase("bh")) {
				String data = helperXor("" + bx[0] + bx[1], source);
				for (int i = 0; i <= 1; i++) {
					dx[i] = data.charAt(i + 2);
				}
			} else if (first.equalsIgnoreCase("cl")) {
				String data = helperXor("" + ax[2] + ax[3], source);
				for (int i = 0; i <= 1; i++) {
					cx[i + 2] = data.charAt(i + 2);
				}
			} else if (first.equalsIgnoreCase("ch")) {
				String data = helperXor("" + cx[0] + cx[1], source);
				for (int i = 0; i <= 1; i++) {
					cx[i] = data.charAt(i + 2);
				}
			} else if (first.equalsIgnoreCase("dl")) {
				String data = helperXor("" + dx[2] + dx[3], source);
				for (int i = 0; i <= 1; i++) {
					dx[i + 2] = data.charAt(i + 2);
				}
			} else if (first.equalsIgnoreCase("dh")) {
				String data = helperXor("" + dx[0] + dx[1], source);
				for (int i = 0; i <= 1; i++) {
					dx[i] = data.charAt(i);
				}
			}
		} else { // reg veya memoryye yazmiyo hata ver
			System.out.println("Undefined symbols are listed: " + first);
			System.exit(0);
		}

	}

	private String helperXor(String first, String second) {
		int a = Integer.parseInt(first, 16) ^ Integer.parseInt(second, 16);
		// TODO
		// other flags???
		if (a == 0)
			ZF = true;
		return NumberToFourByteHexa("" + a);
	}

	public void or(String first, String second) {
		CF = false;
		OF = false;

		if (first.contains("[") && first.contains("]")) { // first is memory
			String source = source_when_first_operand_is_memory(first, second);
			int memoryIndex = memoryIndexOfFirst(first);
			memory[memoryIndex] = helperOr(memory[memoryIndex], source);

		} else if (isRegTwoByte(first)) {// 16 bit register

			String source = source_when_first_operand_is_twoByteReg(second);
			if (first.equalsIgnoreCase("ax")) {
				String data = helperOr("" + ax[0] + ax[1] + ax[2] + ax[3], source);
				for (int i = 0; i <= 3; i++) {
					ax[i] = data.charAt(i);
				}
			} else if (first.equalsIgnoreCase("bx")) {
				String data = helperOr("" + bx[0] + bx[1] + bx[2] + bx[3], source);
				for (int i = 0; i <= 3; i++) {
					bx[i] = data.charAt(i);
				}
			} else if (first.equalsIgnoreCase("cx")) {
				String data = helperOr("" + cx[0] + cx[1] + cx[2] + cx[3], source);
				for (int i = 0; i <= 3; i++) {
					cx[i] = data.charAt(i);
				}
			} else if (first.equalsIgnoreCase("dx")) {
				String data = helperOr("" + dx[0] + dx[1] + dx[2] + dx[3], source);
				for (int i = 0; i <= 3; i++) {
					dx[i] = data.charAt(i);
				}
			} else if (first.equalsIgnoreCase("di")) {
				String data = helperOr("" + di[0] + di[1] + di[2] + di[3], source);
				for (int i = 0; i <= 3; i++) {
					di[i] = data.charAt(i);
				}
			} else if (first.equalsIgnoreCase("si")) {
				String data = helperOr("" + si[0] + si[1] + si[2] + si[3], source);
				for (int i = 0; i <= 3; i++) {
					si[i] = data.charAt(i);
				}
			} else if (first.equalsIgnoreCase("bp")) {
				String data = helperOr("" + bp[0] + bp[1] + bp[2] + bp[3], source);
				for (int i = 0; i <= 3; i++) {
					bp[i] = data.charAt(i);
				}
			}
		} else if (isRegOneByte(first)) {// 8 bit register
			String source = source_when_first_operand_is_oneByteReg(second);
			if (first.equalsIgnoreCase("al")) {
				String data = helperOr("" + ax[2] + ax[3], source);
				for (int i = 0; i <= 1; i++) {
					ax[i + 2] = data.charAt(i + 2);
				}
			} else if (first.equalsIgnoreCase("ah")) {
				String data = helperOr("" + ax[0] + ax[1], source);
				for (int i = 0; i <= 1; i++) {
					ax[i] = data.charAt(i + 2);
				}
			} else if (first.equalsIgnoreCase("bl")) {
				String data = helperOr("" + ax[2] + ax[3], source);
				for (int i = 0; i <= 1; i++) {
					cx[i + 2] = data.charAt(i + 2);
				}
			} else if (first.equalsIgnoreCase("bh")) {
				String data = helperOr("" + bx[0] + bx[1], source);
				for (int i = 0; i <= 1; i++) {
					dx[i] = data.charAt(i + 2);
				}
			} else if (first.equalsIgnoreCase("cl")) {
				String data = helperOr("" + ax[2] + ax[3], source);
				for (int i = 0; i <= 1; i++) {
					cx[i + 2] = data.charAt(i + 2);
				}
			} else if (first.equalsIgnoreCase("ch")) {
				String data = helperOr("" + cx[0] + cx[1], source);
				for (int i = 0; i <= 1; i++) {
					cx[i] = data.charAt(i + 2);
				}
			} else if (first.equalsIgnoreCase("dl")) {
				String data = helperOr("" + dx[2] + dx[3], source);
				for (int i = 0; i <= 1; i++) {
					dx[i + 2] = data.charAt(i + 2);
				}
			} else if (first.equalsIgnoreCase("dh")) {
				String data = helperOr("" + dx[0] + dx[1], source);
				for (int i = 0; i <= 1; i++) {
					dx[i] = data.charAt(i);
				}
			}
		} else { // reg veya memoryye yazmiyo hata ver
			System.out.println("Undefined symbols are listed: " + first);
			System.exit(0);
		}

	}

	private String helperOr(String first, String second) {
		int a = Integer.parseInt(first, 16) | Integer.parseInt(second, 16);
		// TODO
		// other flags???
		if (a == 0)
			ZF = true;
		return NumberToFourByteHexa("" + a);
	}

	public void not(String first, String second) {

	}

	public void and(String first, String second) {
		CF = false;
		OF = false;

		if (first.contains("[") && first.contains("]")) { // first is memory
			String source = source_when_first_operand_is_memory(first, second);
			int memoryIndex = memoryIndexOfFirst(first);
			memory[memoryIndex] = helperAnd(memory[memoryIndex], source);
		} else if (isRegTwoByte(first)) {// 16 bit register

			String source = source_when_first_operand_is_twoByteReg(second);
			if (first.equalsIgnoreCase("ax")) {
				String data = helperAnd("" + ax[0] + ax[1] + ax[2] + ax[3], source);
				for (int i = 0; i <= 3; i++) {
					ax[i] = data.charAt(i);
				}
			} else if (first.equalsIgnoreCase("bx")) {
				String data = helperAnd("" + bx[0] + bx[1] + bx[2] + bx[3], source);

				for (int i = 0; i <= 3; i++) {
					bx[i] = data.charAt(i);
				}
			} else if (first.equalsIgnoreCase("cx")) {
				String data = helperAnd("" + cx[0] + cx[1] + cx[2] + cx[3], source);
				for (int i = 0; i <= 3; i++) {
					cx[i] = data.charAt(i);
				}
			} else if (first.equalsIgnoreCase("dx")) {
				String data = helperAnd("" + dx[0] + dx[1] + dx[2] + dx[3], source);
				for (int i = 0; i <= 3; i++) {
					dx[i] = data.charAt(i);
				}
			} else if (first.equalsIgnoreCase("di")) {
				String data = helperAnd("" + di[0] + di[1] + di[2] + di[3], source);
				for (int i = 0; i <= 3; i++) {
					di[i] = data.charAt(i);
				}
			} else if (first.equalsIgnoreCase("si")) {
				String data = helperAnd("" + si[0] + si[1] + si[2] + si[3], source);
				for (int i = 0; i <= 3; i++) {
					si[i] = data.charAt(i);
				}
			} else if (first.equalsIgnoreCase("bp")) {
				String data = helperAnd("" + bp[0] + bp[1] + bp[2] + bp[3], source);
				for (int i = 0; i <= 3; i++) {
					bp[i] = data.charAt(i);
				}
			}
		} else if (isRegOneByte(first)) {// 8 bit register
			String source = source_when_first_operand_is_oneByteReg(second);
			if (first.equalsIgnoreCase("al")) {
				String data = helperAnd("" + ax[2] + ax[3], source);
				for (int i = 0; i <= 1; i++) {
					ax[i + 2] = data.charAt(i + 2);
				}
			} else if (first.equalsIgnoreCase("ah")) {
				String data = helperAnd("" + ax[0] + ax[1], source);
				for (int i = 0; i <= 1; i++) {
					ax[i] = data.charAt(i + 2);
				}
			} else if (first.equalsIgnoreCase("bl")) {
				String data = helperAnd("" + ax[2] + ax[3], source);
				for (int i = 0; i <= 1; i++) {
					cx[i + 2] = data.charAt(i + 2);
				}
			} else if (first.equalsIgnoreCase("bh")) {
				String data = helperAnd("" + bx[0] + bx[1], source);
				for (int i = 0; i <= 1; i++) {
					dx[i] = data.charAt(i + 2);
				}
			} else if (first.equalsIgnoreCase("cl")) {
				String data = helperAnd("" + ax[2] + ax[3], source);
				for (int i = 0; i <= 1; i++) {
					cx[i + 2] = data.charAt(i + 2);
				}
			} else if (first.equalsIgnoreCase("ch")) {
				String data = helperAnd("" + cx[0] + cx[1], source);
				for (int i = 0; i <= 1; i++) {
					cx[i] = data.charAt(i + 2);
				}
			} else if (first.equalsIgnoreCase("dl")) {
				String data = helperAnd("" + dx[2] + dx[3], source);
				for (int i = 0; i <= 1; i++) {
					dx[i + 2] = data.charAt(i + 2);
				}
			} else if (first.equalsIgnoreCase("dh")) {
				String data = helperAnd("" + dx[0] + dx[1], source);
				for (int i = 0; i <= 1; i++) {
					dx[i] = data.charAt(i);
				}
			}
		} else { // reg veya memoryye yazmiyo hata ver
			System.out.println("Undefined symbols are listed: " + first);
			System.exit(0);
		}

	}

	private String helperAnd(String first, String second) {
		int a = Integer.parseInt(first, 16) & Integer.parseInt(second, 16);
		// TODO
		// other flags???
		if (a == 0)
			ZF = true;
		return NumberToFourByteHexa("" + a);
	}

	public void mul(String first) {
		// variable part can be deleted later

		String source;
		boolean isFirstVar = false;
		Variable firstvar = null;
		Variable temp;
		Iterator<Variable> itr = variables.iterator();
		while (itr.hasNext()) {
			temp = itr.next();
			if (first.contains(temp.getName())) {
				isFirstVar = true;
				firstvar = temp;
				break;
			}
		} // now we know first operand is whether variable ->(add w[var1],ax)
		if (isFirstVar) {
			if (first.charAt(0) == 'w' && firstvar.getType()) {
				source = contentsOfSecondOperandOfADDSUBTwoByte(first);
				mul_2Byte(source);
			} else if (first.charAt(0) == 'b' && !firstvar.getType()) {
				source = contentsOfSecondOperandOfADDSUBOneByte(first);
				mul_1Byte(source);
			}
		} else if (first.contains("[") && first.contains("]")) { // first is memory
			if (first.charAt(0) == 'w') {
				source = contentsOfSecondOperandOfADDSUBTwoByte(first);
				mul_2Byte(source);
			} else if (first.charAt(0) == 'b') {
				source = contentsOfSecondOperandOfADDSUBOneByte(first);
				mul_1Byte(source);
			}

		} else if (isRegTwoByte(first)) {// 16 bit register
			source = contentsOfSecondOperandOfADDSUBTwoByte(first);
			mul_2Byte(source);
		} else if (isRegOneByte(first)) {// 8 bit register
			source = contentsOfSecondOperandOfADDSUBOneByte(first);
			mul_1Byte(source);
		} else {
			System.out.println("Undefined symbols are listed: " + first);
			System.exit(0);
		}

		if (Integer.parseInt("" + ax[0], 16) > 7) {
			CF = true;
			OF = true;
		} else {
			CF = false;
			OF = false;
		}

	}

	private void mul_1Byte(String source) {
		int dest = Integer.parseInt("" + ax[2] + ax[3], 16);
		dest = dest * Integer.parseInt(source, 16);
		String result = NumberToFourByteHexa("" + dest);
		for (int i = 0; i < 4; i++)
			ax[i] = result.charAt(i);

	}

	private void mul_2Byte(String source) {
		int dest = Integer.parseInt("" + ax[0] + ax[1] + ax[2] + ax[3], 16);
		dest = dest * Integer.parseInt(source, 16);
		String result = Integer.toHexString(dest);
		while (result.length() < 8)
			result = "0" + result;

		for (int i = 0; i < 4; i++) {
			ax[i] = result.charAt(i + 4);
			dx[i] = result.charAt(i);
		}

	}

	public void div(String first) {
		String source;
		boolean isFirstVar = false;
		Variable firstvar = null;
		Variable temp;
		Iterator<Variable> itr = variables.iterator();
		while (itr.hasNext()) {
			temp = itr.next();
			if (first.contains(temp.getName())) {
				isFirstVar = true;
				firstvar = temp;
				break;
			}
		} // now we know first operand is whether variable ->(add w[var1],ax)
		if (isFirstVar) {
			if (first.charAt(0) == 'w' && firstvar.getType()) {
				source = contentsOfSecondOperandOfADDSUBTwoByte(first);
				div_2Byte(source);
			} else if (first.charAt(0) == 'b' && !firstvar.getType()) {
				source = contentsOfSecondOperandOfADDSUBOneByte(first);
				div_1Byte(source);
			}
		} else if (first.contains("[") && first.contains("]")) { // first is memory
			if (first.charAt(0) == 'w') {
				source = contentsOfSecondOperandOfADDSUBTwoByte(first);
				div_2Byte(source);
			} else if (first.charAt(0) == 'b') {
				source = contentsOfSecondOperandOfADDSUBOneByte(first);
				div_1Byte(source);
			}

		} else if (isRegTwoByte(first)) {// 16 bit register
			source = contentsOfSecondOperandOfADDSUBTwoByte(first);
			div_2Byte(source);
		} else if (isRegOneByte(first)) {// 8 bit register
			source = contentsOfSecondOperandOfADDSUBOneByte(first);
			div_1Byte(source);
		} else {
			System.out.println("Undefined symbols are listed: " + first);
			System.exit(0);
		}
	}

	private void div_1Byte(String source) {
		int dest = Integer.parseInt("" + ax[0] + ax[1] + ax[2] + ax[3], 16);
		int src = Integer.parseInt(source, 16);
		String quot = NumberToFourByteHexa("" + dest / src);
		String remainder = NumberToFourByteHexa("" + dest % src);
		ax[0] = remainder.charAt(2);
		ax[1] = remainder.charAt(3);
		ax[2] = quot.charAt(2);
		ax[3] = quot.charAt(3);

	}

	private void div_2Byte(String source) {
		int dest = Integer.parseInt("" + dx[0] + dx[1] + dx[2] + dx[3] + ax[0] + ax[1] + ax[2] + ax[3], 16);
		int src = Integer.parseInt(source, 16);
		String quot = NumberToFourByteHexa("" + dest / src);
		String remainder = NumberToFourByteHexa("" + dest % src);
		for (int i = 0; i < 4; i++) {
			ax[i] = quot.charAt(i);
			dx[i] = remainder.charAt(i);
		}

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
				if (variables.get(i).getName().equals(reg) && variables.get(i).getType()) {
					variables.get(i).setData(memory[index]);
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


	/**
	 * Calls helper SUB methods according to the contents of @param first.
	 * 
	 * @param first:  first operand of SUB operation (minuend)
	 * @param second: second operand of SUB operation (subtrahend)
	 */
	public void sub(String first, String second) {
		CF = false;
		SF = false;
		AF = false;
		OF = false;
		ZF = false;

		if (first.contains("[") && first.contains("]")) { // first is memory
			sub_mem_xx(first, second);
		} else if (isRegOneByte(first) || isRegTwoByte(first)) {
			// first is reg
			sub_reg_unknown(first, second);
		} else { // reg veya memoryye eklemiyo
			System.out.println("Undefined symbols are listed: " + first);
			System.exit(0);
		}
	}

	/**
	 * Calls helper ADD methods according to the contents of @param first.
	 * 
	 * @param first:  first operand of ADD operation (augend)s
	 * @param second: second operand of ADD operation (addend)
	 */

	public void add(String first, String second) {
		CF = false;
		SF = false;
		AF = false;
		OF = false;
		ZF = false;
		if (first.contains("[") && first.contains("]")) { // first is memory
			add_mem_xx(first, second);
		} else if (isRegOneByte(first) || isRegTwoByte(first)) {
			// first is reg
			add_reg_unknown(first, second);
		} else { // destination is neither reg nor memory
			System.out.println("Undefined symbols are listed: " + first);
			System.exit(0);
		}
	}

	/**
	 * Calls helper MOV methods according to the contents of @param first.
	 * 
	 * @param first:  first operand of MOV operation
	 * @param second: second operand of MOV operation
	 */

	public void mov(String first, String second) {
		if (first.contains("[") && first.contains("]")) { // first is memory
			String source = source_when_first_operand_is_memory(first, second);
			if (first.charAt(0) == 'w') {
				first = first.substring(1);// got rid of w
				int memoryIndex = memoryIndexOfFirst(first);
				memory[memoryIndex] = source.substring(0, 2);
				memory[memoryIndex + 1] = source.substring(2);
			} else if (first.charAt(0) == 'b') {
				first = first.substring(1);// got rid of b
				int memoryIndex = memoryIndexOfFirst(first);
				memory[memoryIndex] = source.substring(2);
			} else {
				System.out.println("There must be w or b in front of square brackets.");
				System.exit(0);
			}

		} else if (isRegTwoByte(first)) {// 16 bit register

			String source = source_when_first_operand_is_twoByteReg(second);
			if (first.equalsIgnoreCase("ax")) {
				for (int i = 0; i <= 3; i++) {
					ax[i] = source.charAt(i);
				}
			} else if (first.equalsIgnoreCase("bx")) {
				for (int i = 0; i <= 3; i++) {
					bx[i] = source.charAt(i);
				}
			} else if (first.equalsIgnoreCase("cx")) {
				for (int i = 0; i <= 3; i++) {
					cx[i] = source.charAt(i);
				}
			} else if (first.equalsIgnoreCase("dx")) {
				for (int i = 0; i <= 3; i++) {
					dx[i] = source.charAt(i);
				}
			} else if (first.equalsIgnoreCase("di")) {
				for (int i = 0; i <= 3; i++) {
					di[i] = source.charAt(i);
				}
			} else if (first.equalsIgnoreCase("si")) {
				for (int i = 0; i <= 3; i++) {
					si[i] = source.charAt(i);
				}
			} else if (first.equalsIgnoreCase("bp")) {
				for (int i = 0; i <= 3; i++) {
					bp[i] = source.charAt(i);
				}
			}
		} else if (isRegOneByte(first)) {// 8 bit register
			String source = source_when_first_operand_is_oneByteReg(second);
			if (first.equalsIgnoreCase("al")) {
				for (int i = 0; i <= 1; i++) {
					ax[i + 2] = source.charAt(i);
				}
			} else if (first.equalsIgnoreCase("ah")) {
				for (int i = 0; i <= 1; i++) {
					ax[i] = source.charAt(i);
				}
			} else if (first.equalsIgnoreCase("bl")) {
				for (int i = 0; i <= 1; i++) {
					cx[i + 2] = source.charAt(i);
				}
			} else if (first.equalsIgnoreCase("bh")) {
				for (int i = 0; i <= 1; i++) {
					dx[i] = source.charAt(i);
				}
			} else if (first.equalsIgnoreCase("cl")) {
				for (int i = 0; i <= 1; i++) {
					cx[i + 2] = source.charAt(i);
				}
			} else if (first.equalsIgnoreCase("ch")) {
				for (int i = 0; i <= 1; i++) {
					cx[i] = source.charAt(i);
				}
			} else if (first.equalsIgnoreCase("dl")) {
				for (int i = 0; i <= 1; i++) {
					dx[i + 2] = source.charAt(i);
				}
			} else if (first.equalsIgnoreCase("dh")) {
				for (int i = 0; i <= 1; i++) {
					dx[i] = source.charAt(i);
				}
			}
		} else { // reg veya memoryye yazmiyo hata ver
			System.out.println("Undefined symbols are listed: " + first);
			System.exit(0);
		}

	}

	/**
	 * when the first operand of MOV operation is a memory address , this helper
	 * method is called. It handles several errors and moves source to destination.
	 * 
	 * @param first  Destination of MOV operation
	 * @param second Source of MOV operation
	 */
	private String source_when_first_operand_is_memory(String first, String second) { //
		String source;

		if (first.charAt(0) == 'b') {// first operand is kind of b[xx] so source must be one byte.
			source = contentsOfSecondOperandOfADDSUBOneByte(second);
		} else {// assume there is w
			source = contentsOfSecondOperandOfADDSUBTwoByte(second);
		}
		while (source.length() < 4) // to make sure source is a 4 length hexadecimal number.
			source = "0" + source;
		return source;
	}

	/**
	 * when the first operand of MOV operation is a one byte register, this helper
	 * method is called. It handles several errors and moves source to destination.
	 * 
	 * @param first  Destination of MOV operation, it's a two byte register
	 * @param second Source of MOV operation
	 */
	private String source_when_first_operand_is_oneByteReg(String second) {
		char[] temp = new char[2];
		if (second.contains("[") && second.contains("]")) {// memory
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
			second = NumberToFourByteHexa(second); // number
			if (Integer.parseInt(second, 16) > 255) {
				System.out.println("#ERROR 30: Byte-Sized Constant Required");
				System.exit(0);
			}
			for (int i = 0; i <= 1 && i < second.length(); i++) {
				temp[1 - i] = second.charAt(second.length() - i - 1);
			}
		}
		return "" + temp[0] + temp[1];
		// temp has the value just insert it

	}

	/**
	 * when the first operand of MOV operation is a two byte register, this helper
	 * method is called. It handles several errors and moves source to destination.
	 * 
	 * @param first:  destination of MOV operation
	 * @param second: source of MOV operation
	 */
	private String source_when_first_operand_is_twoByteReg(String second) {
		char[] temp = new char[4];
		for (int i = 0; i < 3; i++)
			temp[i] = '0';
		if (second.contains("[") && second.contains("]")) {

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
			second = NumberToFourByteHexa(second); // number
			for (int i = 0; i < 3; i++) {
				temp[i] = 0;
			}
			for (int i = 0; i <= 3 && i < second.length(); i++) {
				temp[3 - i] = second.charAt(second.length() - i - 1);
			}
		}
		// temp has the value just return it

		return "" + temp[0] + temp[1] + temp[2] + temp[3];

	}

	/**
	 * this method is called when source operand of ADD or SUB operation is a one
	 * byte register
	 * 
	 * @param second source operand of ADD or SUB operation
	 * @return addend or subtrahend of the ADD or SUB operation
	 */
	private String addsub_mem1B_xx(String second) {
		String addend = "";
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

	/**
	 * this method is called when source operand of ADD or SUB operation is a two
	 * byte register
	 * 
	 * @param second source operand of ADD or SUB operation
	 * @return addend or subtrahend of the ADD or SUB operation
	 */
	private String addsub_mem2B_xx(String second) {
		String addend = "";

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

	/**
	 * this method adds source to memory destination.
	 * 
	 * @param first  : first operand (augend) of ADD operation. It's a memory
	 *               address for sure.
	 * @param second : second operand (addend) of ADD operation.
	 */
	private void add_mem_xx(String first, String second) {
		String addend = sourceOfADDorSUBOperation(first, second);
		// augend + addend = sum
		int memoryIndex = memoryIndexOfFirst(first);
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
			memory[memoryIndex] = NumberToFourByteHexa(sum + "").substring(0, 2);
			memory[memoryIndex + 1] = NumberToFourByteHexa(sum + "").substring(2);
		} else {// augend is empty(which means 0), result is addend
			memory[memoryIndex] = addend.substring(0, 2);
			memory[memoryIndex + 1] = addend.substring(2);
		}

	}

	/**
	 * this method returns source of ADD or SUB operation and handles several
	 * errors.
	 * 
	 * @param first  : first operand (augend or minuend) of ADD or SUB operation.
	 *               It's a memory address for sure.
	 * @param second : second operand (addend or subtrahend) of ADD or SUB
	 *               operation.
	 * @return source of ADD or SUB operation.
	 */
	private String sourceOfADDorSUBOperation(String first, String second) {
		String toBeReturned = "";
		if (second.contains("[")) {
			System.out.println("#ERROR 50: Reg,Mem Required  ");
			System.exit(0);
		} else {
			if (first.charAt(0) == 'b') {// constant must be byte sized and regs too
				toBeReturned = addsub_mem1B_xx(second);
			} else if (first.charAt(0) == 'w') { // regs must be two byte sized
				toBeReturned = addsub_mem2B_xx(second);
			} else {
				System.out.println("there must be 'b' or 'w' in front of square brackets");
				System.exit(0);
			}
		}
		return toBeReturned;
	}

	/**
	 * calls helper methods to calculate source then adds it corresponding register
	 * destination.
	 * 
	 * @param first:  first operand of ADD operation. It's a register for sure.
	 * @param second: second operand of ADD operation
	 */

	private void add_reg_unknown(String first, String second) {

		if (first.equalsIgnoreCase("ax")) {
			String addend = contentsOfSecondOperandOfADDSUBTwoByte(second);
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
				sumStringForm = "0000";
			} else {
				if (sum > Integer.parseInt("ffff", 16)) {
					CF = true;
					sum -= Integer.parseInt("10000", 16);
					sumStringForm = NumberToFourByteHexa("" + sum);
				}
			}
			ax[0] = sumStringForm.charAt(0);
			ax[1] = sumStringForm.charAt(1);
			ax[2] = sumStringForm.charAt(2);
			ax[3] = sumStringForm.charAt(3);
		} else if (first.equalsIgnoreCase("bx")) {
			String addend = contentsOfSecondOperandOfADDSUBTwoByte(second);
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
				sumStringForm = "0000";

			} else {
				if (sum > Integer.parseInt("ffff", 16)) {

					CF = true;
					sum -= Integer.parseInt("10000", 16);
					sumStringForm = NumberToFourByteHexa("" + sum);
				}
			}
			bx[0] = sumStringForm.charAt(0);
			bx[1] = sumStringForm.charAt(1);
			bx[2] = sumStringForm.charAt(2);
			bx[3] = sumStringForm.charAt(3);
		} else if (first.equalsIgnoreCase("cx")) {
			String addend = contentsOfSecondOperandOfADDSUBTwoByte(second);
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
				sumStringForm = "0000";

			} else {
				if (sum > Integer.parseInt("ffff", 16)) {
					CF = true;
					sum -= Integer.parseInt("10000", 16);
					sumStringForm = NumberToFourByteHexa("" + sum);
				}

			}
			cx[0] = sumStringForm.charAt(0);
			cx[1] = sumStringForm.charAt(1);
			cx[2] = sumStringForm.charAt(2);
			cx[3] = sumStringForm.charAt(3);
		} else if (first.equalsIgnoreCase("dx")) {
			String addend = contentsOfSecondOperandOfADDSUBTwoByte(second);
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
				sumStringForm = "0000";

			} else {
				if (sum > Integer.parseInt("ffff", 16)) {
					CF = true;
					sum -= Integer.parseInt("10000", 16);
					sumStringForm = NumberToFourByteHexa("" + sum);
				}

			}
			dx[0] = sumStringForm.charAt(0);
			dx[1] = sumStringForm.charAt(1);
			dx[2] = sumStringForm.charAt(2);
			dx[3] = sumStringForm.charAt(3);
		} else if (first.equalsIgnoreCase("al")) {
			String addend = contentsOfSecondOperandOfADDSUBOneByte(second);
			int sum = Integer.parseInt(("" + ax[2] + "" + ax[3]), 16) + Integer.parseInt(addend, 16);// decimal sum
			String sumStringForm = NumberToFourByteHexa("" + sum);// sum is now hexa
			while (addend.length() < 2) {
				addend = "0" + addend;
			}
			if (Integer.parseInt("" + ax[3], 16) + Integer.parseInt("" + addend.charAt(addend.length() - 1), 16) > 15) {
				AF = true;
			}
			if (sum == 0) {
				ZF = true;
				sumStringForm = "00";
			} else {
				if (sum > Integer.parseInt("ff", 16)) {
					CF = true;
					sum -= 256;
					sumStringForm = NumberToFourByteHexa("" + sum);
				}
			}
			ax[2] = sumStringForm.charAt(sumStringForm.length() - 2);
			ax[3] = sumStringForm.charAt(sumStringForm.length() - 1);
		} else if (first.equalsIgnoreCase("ah")) {
			String addend = contentsOfSecondOperandOfADDSUBOneByte(second);
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
				sumStringForm = "00";
			} else {
				if (sum > Integer.parseInt("ff", 16)) {
					CF = true;
					sum -= 256;
					sumStringForm = NumberToFourByteHexa("" + sum);
				}
			}
			ax[0] = sumStringForm.charAt(sumStringForm.length() - 2);
			ax[1] = sumStringForm.charAt(sumStringForm.length() - 1);
		} else if (first.equalsIgnoreCase("bl")) {
			String addend = contentsOfSecondOperandOfADDSUBOneByte(second);
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
				sumStringForm = "00";
			} else {
				if (sum > Integer.parseInt("ff", 16)) {
					CF = true;
					sum -= 256;
					sumStringForm = NumberToFourByteHexa("" + sum);
				}
			}
			bx[2] = sumStringForm.charAt(sumStringForm.length() - 2);
			bx[3] = sumStringForm.charAt(sumStringForm.length() - 1);
		} else if (first.equalsIgnoreCase("bh")) {
			String addend = contentsOfSecondOperandOfADDSUBOneByte(second);
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
				sumStringForm = "00";
			} else {
				if (sum > Integer.parseInt("ff", 16)) {
					CF = true;
					sum -= 256;
					sumStringForm = NumberToFourByteHexa("" + sum);
				}
			}
			bx[0] = sumStringForm.charAt(sumStringForm.length() - 2);
			bx[1] = sumStringForm.charAt(sumStringForm.length() - 1);
		} else if (first.equalsIgnoreCase("cl")) {
			String addend = contentsOfSecondOperandOfADDSUBOneByte(second);
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
				sumStringForm = "00";
			} else {
				if (sum > Integer.parseInt("ff", 16)) {
					CF = true;
					sum -= 256;
					sumStringForm = NumberToFourByteHexa("" + sum);
				}
			}
			cx[2] = sumStringForm.charAt(sumStringForm.length() - 2);
			cx[3] = sumStringForm.charAt(sumStringForm.length() - 1);
		} else if (first.equalsIgnoreCase("ch")) {
			String addend = contentsOfSecondOperandOfADDSUBOneByte(second);

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
				sumStringForm = "00";
			} else {
				if (sum > Integer.parseInt("ff", 16)) {
					CF = true;
					sum -= 256;
					sumStringForm = NumberToFourByteHexa("" + sum);
				}
			}
			cx[0] = sumStringForm.charAt(sumStringForm.length() - 2);
			cx[1] = sumStringForm.charAt(sumStringForm.length() - 1);
		} else if (first.equalsIgnoreCase("dl")) {
			String addend = contentsOfSecondOperandOfADDSUBOneByte(second);

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
				sumStringForm = "00";
			} else {
				if (sum > Integer.parseInt("ff", 16)) {
					CF = true;
					sum -= 256;
					sumStringForm = NumberToFourByteHexa("" + sum);
				}
			}
			dx[2] = sumStringForm.charAt(sumStringForm.length() - 2);
			dx[3] = sumStringForm.charAt(sumStringForm.length() - 1);
		} else if (first.equalsIgnoreCase("dh")) {
			String addend = contentsOfSecondOperandOfADDSUBOneByte(second);
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
				sumStringForm = "00";
			} else {
				if (sum > Integer.parseInt("ff", 16)) {

					CF = true;
					sum -= 256;
					sumStringForm = NumberToFourByteHexa("" + sum);
				}
			}
			dx[0] = sumStringForm.charAt(sumStringForm.length() - 2);
			dx[1] = sumStringForm.charAt(sumStringForm.length() - 1);
		} else if (first.equalsIgnoreCase("di")) {
			String addend = contentsOfSecondOperandOfADDSUBTwoByte(second);
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
				sumStringForm = "0000";

			} else {
				if (sum > Integer.parseInt("ffff", 16)) {
					CF = true;
					sum -= Integer.parseInt("10000", 16);
					sumStringForm = NumberToFourByteHexa("" + sum);
				}

			}
			di[0] = sumStringForm.charAt(0);
			di[1] = sumStringForm.charAt(1);
			di[2] = sumStringForm.charAt(2);
			di[3] = sumStringForm.charAt(3);
		} else if (first.equalsIgnoreCase("si")) {
			String addend = contentsOfSecondOperandOfADDSUBTwoByte(second);
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
				sumStringForm = "0000";

			} else {
				if (sum > Integer.parseInt("ffff", 16)) {
					CF = true;
					sum -= Integer.parseInt("10000", 16);
					sumStringForm = NumberToFourByteHexa("" + sum);
				}
			}
			si[0] = sumStringForm.charAt(0);
			si[1] = sumStringForm.charAt(1);
			si[2] = sumStringForm.charAt(2);
			si[3] = sumStringForm.charAt(3);

		} else if (first.equalsIgnoreCase("bp")) {
			String addend = contentsOfSecondOperandOfADDSUBTwoByte(second);
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
				sumStringForm = "0000";

			} else {
				if (sum > Integer.parseInt("ffff", 16)) {
					CF = true;
					sum -= Integer.parseInt("10000", 16);
					sumStringForm = NumberToFourByteHexa("" + sum);
				}

			}
			bp[0] = sumStringForm.charAt(0);
			bp[1] = sumStringForm.charAt(1);
			bp[2] = sumStringForm.charAt(2);
			bp[3] = sumStringForm.charAt(3);
		}
	}

	/**
	 * calls helper methods to calculate source then subtracts it corresponding
	 * register destination.
	 * 
	 * @param first:  first operand of SUB operation. It's a register for sure.
	 * @param second: second operand of SUB operation
	 */

	private void sub_reg_unknown(String first, String second) {
		if (first.equalsIgnoreCase("ax")) {
			String subtrahend = contentsOfSecondOperandOfADDSUBTwoByte(second);
			int difference = Integer.parseInt((ax[0] + "" + ax[1] + "" + ax[2] + "" + ax[3]), 16)
					- Integer.parseInt(subtrahend, 16);
			String differenceStringForm = NumberToFourByteHexa("" + difference);

			while (subtrahend.length() < 4) {
				subtrahend = "0" + subtrahend;
			}
			if (Integer.parseInt("" + ax[3], 16) - Integer.parseInt("" + subtrahend.charAt(3), 16) < 0) {
				AF = true;
			}
			if (difference == 0) {
				ZF = true;
				differenceStringForm = "0000";
			} else {
				if (difference < 0) {
					SF = true;
					CF = true;
					difference += Integer.parseInt("10000", 16);
					differenceStringForm = NumberToFourByteHexa("" + difference);
				}
			}
			ax[0] = differenceStringForm.charAt(0);
			ax[1] = differenceStringForm.charAt(1);
			ax[2] = differenceStringForm.charAt(2);
			ax[3] = differenceStringForm.charAt(3);
		} else if (first.equalsIgnoreCase("bx")) {
			String subtrahend = contentsOfSecondOperandOfADDSUBTwoByte(second);
			int difference = Integer.parseInt((bx[0] + "" + bx[1] + "" + bx[2] + "" + bx[3]), 16)
					- Integer.parseInt(subtrahend, 16);
			String differenceStringForm = NumberToFourByteHexa("" + difference);

			while (subtrahend.length() < 4) {
				subtrahend = "0" + subtrahend;
			}
			if (Integer.parseInt("" + bx[3], 16) - Integer.parseInt("" + subtrahend.charAt(3), 16) < 0) {
				AF = true;
			}
			if (difference == 0) {
				ZF = true;
				differenceStringForm = "0000";
			} else {
				if (difference < 0) {
					SF = true;
					CF = true;
					difference += Integer.parseInt("10000", 16);
					differenceStringForm = NumberToFourByteHexa("" + difference);
				}
			}
			bx[0] = differenceStringForm.charAt(0);
			bx[1] = differenceStringForm.charAt(1);
			bx[2] = differenceStringForm.charAt(2);
			bx[3] = differenceStringForm.charAt(3);
		} else if (first.equalsIgnoreCase("cx")) {
			String subtrahend = contentsOfSecondOperandOfADDSUBTwoByte(second);
			int difference = Integer.parseInt((cx[0] + "" + cx[1] + "" + cx[2] + "" + cx[3]), 16)
					- Integer.parseInt(subtrahend, 16);
			String differenceStringForm = NumberToFourByteHexa("" + difference);

			while (subtrahend.length() < 4) {
				subtrahend = "0" + subtrahend;
			}
			if (Integer.parseInt("" + cx[3], 16) - Integer.parseInt("" + subtrahend.charAt(3), 16) < 0) {
				AF = true;
			}
			if (difference == 0) {
				ZF = true;
				differenceStringForm = "0000";
			} else {
				if (difference < 0) {
					SF = true;
					CF = true;
					difference += Integer.parseInt("10000", 16);
					differenceStringForm = NumberToFourByteHexa("" + difference);
				}
			}
			cx[0] = differenceStringForm.charAt(0);
			cx[1] = differenceStringForm.charAt(1);
			cx[2] = differenceStringForm.charAt(2);
			cx[3] = differenceStringForm.charAt(3);
		} else if (first.equalsIgnoreCase("dx")) {
			String subtrahend = contentsOfSecondOperandOfADDSUBTwoByte(second);
			int difference = Integer.parseInt((dx[0] + "" + dx[1] + "" + dx[2] + "" + dx[3]), 16)
					- Integer.parseInt(subtrahend, 16);
			String differenceStringForm = NumberToFourByteHexa("" + difference);

			while (subtrahend.length() < 4) {
				subtrahend = "0" + subtrahend;
			}
			if (Integer.parseInt("" + dx[3], 16) - Integer.parseInt("" + subtrahend.charAt(3), 16) < 0) {
				AF = true;
			}
			if (difference == 0) {
				ZF = true;
				differenceStringForm = "0000";
			} else {
				if (difference < 0) {
					SF = true;
					CF = true;
					difference += Integer.parseInt("10000", 16);
					differenceStringForm = NumberToFourByteHexa("" + difference);
				}
			}
			dx[0] = differenceStringForm.charAt(0);
			dx[1] = differenceStringForm.charAt(1);
			dx[2] = differenceStringForm.charAt(2);
			dx[3] = differenceStringForm.charAt(3);
		} else if (first.equalsIgnoreCase("bp")) {
			String subtrahend = contentsOfSecondOperandOfADDSUBTwoByte(second);
			int difference = Integer.parseInt((bp[0] + "" + bp[1] + "" + bp[2] + "" + bp[3]), 16)
					- Integer.parseInt(subtrahend, 16);
			String differenceStringForm = NumberToFourByteHexa("" + difference);

			while (subtrahend.length() < 4) {
				subtrahend = "0" + subtrahend;
			}
			if (Integer.parseInt("" + bp[3], 16) - Integer.parseInt("" + subtrahend.charAt(3), 16) < 0) {
				AF = true;
			}
			if (difference == 0) {
				ZF = true;
				differenceStringForm = "0000";
			} else {
				if (difference < 0) {
					SF = true;
					CF = true;
					difference += Integer.parseInt("10000", 16);
					differenceStringForm = NumberToFourByteHexa("" + difference);
				}
			}
			bp[0] = differenceStringForm.charAt(0);
			bp[1] = differenceStringForm.charAt(1);
			bp[2] = differenceStringForm.charAt(2);
			bp[3] = differenceStringForm.charAt(3);
		} else if (first.equalsIgnoreCase("di")) {
			String subtrahend = contentsOfSecondOperandOfADDSUBTwoByte(second);
			int difference = Integer.parseInt((di[0] + "" + di[1] + "" + di[2] + "" + di[3]), 16)
					- Integer.parseInt(subtrahend, 16);
			String differenceStringForm = NumberToFourByteHexa("" + difference);

			while (subtrahend.length() < 4) {
				subtrahend = "0" + subtrahend;
			}
			if (Integer.parseInt("" + di[3], 16) - Integer.parseInt("" + subtrahend.charAt(3), 16) < 0) {
				AF = true;
			}
			if (difference == 0) {
				ZF = true;
				differenceStringForm = "0000";
			} else {
				if (difference < 0) {
					SF = true;
					CF = true;
					difference += Integer.parseInt("10000", 16);
					differenceStringForm = NumberToFourByteHexa("" + difference);
				}
			}
			di[0] = differenceStringForm.charAt(0);
			di[1] = differenceStringForm.charAt(1);
			di[2] = differenceStringForm.charAt(2);
			di[3] = differenceStringForm.charAt(3);
		} else if (first.equalsIgnoreCase("si")) {
			String subtrahend = contentsOfSecondOperandOfADDSUBTwoByte(second);
			int difference = Integer.parseInt((si[0] + "" + si[1] + "" + si[2] + "" + si[3]), 16)
					- Integer.parseInt(subtrahend, 16);
			String differenceStringForm = NumberToFourByteHexa("" + difference);

			while (subtrahend.length() < 4) {
				subtrahend = "0" + subtrahend;
			}
			if (Integer.parseInt("" + si[3], 16) - Integer.parseInt("" + subtrahend.charAt(3), 16) < 0) {
				AF = true;
			}
			if (difference == 0) {
				ZF = true;
				differenceStringForm = "0000";
			} else {
				if (difference < 0) {
					SF = true;
					CF = true;
					difference += Integer.parseInt("10000", 16);
					differenceStringForm = NumberToFourByteHexa("" + difference);
				}
			}
			si[0] = differenceStringForm.charAt(0);
			si[1] = differenceStringForm.charAt(1);
			si[2] = differenceStringForm.charAt(2);
			si[3] = differenceStringForm.charAt(3);
		} else if (first.equalsIgnoreCase("al")) {
			String subtrahend = contentsOfSecondOperandOfADDSUBOneByte(second);
			int difference = Integer.parseInt(("" + ax[2] + "" + ax[3]), 16) + Integer.parseInt(subtrahend, 16);// decimal
																												// sum
			String differenceStringForm = NumberToFourByteHexa("" + difference);
			while (subtrahend.length() < 2) {
				subtrahend = "0" + subtrahend;
			}
			if (Integer.parseInt("" + ax[3], 16)
					- Integer.parseInt("" + subtrahend.charAt(subtrahend.length() - 1), 16) < 0) {
				AF = true;
			}
			if (difference == 0) {
				ZF = true;
				differenceStringForm = "00";
			} else {
				if (difference < 0) {
					CF = true;
					difference += 256;
					differenceStringForm = NumberToFourByteHexa("" + difference);
				}
			}
			ax[2] = differenceStringForm.charAt(differenceStringForm.length() - 2);
			ax[3] = differenceStringForm.charAt(differenceStringForm.length() - 1);
		} else if (first.equalsIgnoreCase("ah")) {
			String subtrahend = contentsOfSecondOperandOfADDSUBOneByte(second);
			int difference = Integer.parseInt(("" + ax[0] + "" + ax[1]), 16) + Integer.parseInt(subtrahend, 16);// decimal
																												// sum
			String differenceStringForm = NumberToFourByteHexa("" + difference);
			while (subtrahend.length() < 2) {
				subtrahend = "0" + subtrahend;
			}
			if (Integer.parseInt("" + ax[1], 16)
					- Integer.parseInt("" + subtrahend.charAt(subtrahend.length() - 1), 16) < 0) {
				AF = true;
			}
			if (difference == 0) {
				ZF = true;
				differenceStringForm = "00";
			} else {
				if (difference < 0) {
					CF = true;
					difference += 256;
					differenceStringForm = NumberToFourByteHexa("" + difference);
				}
			}
			ax[0] = differenceStringForm.charAt(differenceStringForm.length() - 2);
			ax[1] = differenceStringForm.charAt(differenceStringForm.length() - 1);
		} else if (first.equalsIgnoreCase("bl")) {
			String subtrahend = contentsOfSecondOperandOfADDSUBOneByte(second);
			int difference = Integer.parseInt(("" + bx[2] + "" + bx[3]), 16) + Integer.parseInt(subtrahend, 16);// decimal
																												// sum
			String differenceStringForm = NumberToFourByteHexa("" + difference);
			while (subtrahend.length() < 2) {
				subtrahend = "0" + subtrahend;
			}
			if (Integer.parseInt("" + bx[3], 16)
					- Integer.parseInt("" + subtrahend.charAt(subtrahend.length() - 1), 16) < 0) {
				AF = true;
			}
			if (difference == 0) {
				ZF = true;
				differenceStringForm = "00";
			} else {
				if (difference < 0) {
					CF = true;
					difference += 256;
					differenceStringForm = NumberToFourByteHexa("" + difference);
				}
			}
			bx[2] = differenceStringForm.charAt(differenceStringForm.length() - 2);
			bx[3] = differenceStringForm.charAt(differenceStringForm.length() - 1);
		} else if (first.equalsIgnoreCase("bh")) {
			String subtrahend = contentsOfSecondOperandOfADDSUBOneByte(second);
			int difference = Integer.parseInt(("" + bx[0] + "" + bx[1]), 16) + Integer.parseInt(subtrahend, 16);// decimal
																												// sum
			String differenceStringForm = NumberToFourByteHexa("" + difference);
			while (subtrahend.length() < 2) {
				subtrahend = "0" + subtrahend;
			}
			if (Integer.parseInt("" + bx[1], 16)
					- Integer.parseInt("" + subtrahend.charAt(subtrahend.length() - 1), 16) < 0) {
				AF = true;
			}
			if (difference == 0) {
				ZF = true;
				differenceStringForm = "00";
			} else {
				if (difference < 0) {
					CF = true;
					difference += 256;
					differenceStringForm = NumberToFourByteHexa("" + difference);
				}
			}
			bx[0] = differenceStringForm.charAt(differenceStringForm.length() - 2);
			bx[1] = differenceStringForm.charAt(differenceStringForm.length() - 1);
		} else if (first.equalsIgnoreCase("al")) {
			String subtrahend = contentsOfSecondOperandOfADDSUBOneByte(second);
			int difference = Integer.parseInt(("" + cx[2] + "" + cx[3]), 16) + Integer.parseInt(subtrahend, 16);// decimal
																												// sum
			String differenceStringForm = NumberToFourByteHexa("" + difference);
			while (subtrahend.length() < 2) {
				subtrahend = "0" + subtrahend;
			}
			if (Integer.parseInt("" + cx[3], 16)
					- Integer.parseInt("" + subtrahend.charAt(subtrahend.length() - 1), 16) < 0) {
				AF = true;
			}
			if (difference == 0) {
				ZF = true;
				differenceStringForm = "00";
			} else {
				if (difference < 0) {
					CF = true;
					difference += 256;
					differenceStringForm = NumberToFourByteHexa("" + difference);
				}
			}
			cx[2] = differenceStringForm.charAt(differenceStringForm.length() - 2);
			cx[3] = differenceStringForm.charAt(differenceStringForm.length() - 1);
		} else if (first.equalsIgnoreCase("ah")) {
			String subtrahend = contentsOfSecondOperandOfADDSUBOneByte(second);
			int difference = Integer.parseInt(("" + cx[0] + "" + cx[1]), 16) + Integer.parseInt(subtrahend, 16);// decimal
																												// sum
			String differenceStringForm = NumberToFourByteHexa("" + difference);
			while (subtrahend.length() < 2) {
				subtrahend = "0" + subtrahend;
			}
			if (Integer.parseInt("" + cx[1], 16)
					- Integer.parseInt("" + subtrahend.charAt(subtrahend.length() - 1), 16) < 0) {
				AF = true;
			}
			if (difference == 0) {
				ZF = true;
				differenceStringForm = "00";
			} else {
				if (difference < 0) {
					CF = true;
					difference += 256;
					differenceStringForm = NumberToFourByteHexa("" + difference);
				}
			}
			cx[0] = differenceStringForm.charAt(differenceStringForm.length() - 2);
			cx[1] = differenceStringForm.charAt(differenceStringForm.length() - 1);
		} else if (first.equalsIgnoreCase("al")) {
			String subtrahend = contentsOfSecondOperandOfADDSUBOneByte(second);
			int difference = Integer.parseInt(("" + dx[2] + "" + dx[3]), 16) + Integer.parseInt(subtrahend, 16);// decimal
																												// sum
			String differenceStringForm = NumberToFourByteHexa("" + difference);
			while (subtrahend.length() < 2) {
				subtrahend = "0" + subtrahend;
			}
			if (Integer.parseInt("" + dx[3], 16)
					- Integer.parseInt("" + subtrahend.charAt(subtrahend.length() - 1), 16) < 0) {
				AF = true;
			}
			if (difference == 0) {
				ZF = true;
				differenceStringForm = "00";
			} else {
				if (difference < 0) {
					CF = true;
					difference += 256;
					differenceStringForm = NumberToFourByteHexa("" + difference);
				}
			}
			dx[2] = differenceStringForm.charAt(differenceStringForm.length() - 2);
			dx[3] = differenceStringForm.charAt(differenceStringForm.length() - 1);
		} else if (first.equalsIgnoreCase("dh")) {
			String subtrahend = contentsOfSecondOperandOfADDSUBOneByte(second);
			int difference = Integer.parseInt(("" + dx[0] + "" + dx[1]), 16) + Integer.parseInt(subtrahend, 16);// decimal
																												// sum
			String differenceStringForm = NumberToFourByteHexa("" + difference);
			while (subtrahend.length() < 2) {
				subtrahend = "0" + subtrahend;
			}
			if (Integer.parseInt("" + dx[1], 16)
					- Integer.parseInt("" + subtrahend.charAt(subtrahend.length() - 1), 16) < 0) {
				AF = true;
			}
			if (difference == 0) {
				ZF = true;
				differenceStringForm = "00";
			} else {
				if (difference < 0) {
					CF = true;
					difference += 256;
					differenceStringForm = NumberToFourByteHexa("" + difference);
				}
			}
			dx[0] = differenceStringForm.charAt(differenceStringForm.length() - 2);
			dx[1] = differenceStringForm.charAt(differenceStringForm.length() - 1);
		}
	}

	/**
	 * this method is called when destination of SUB operation is a memory address.
	 * 
	 * @param first   : destination of SUB operation. It's a memory address for
	 *                sure.
	 * @param second: source of SUB operation.
	 */
	private void sub_mem_xx(String first, String second) {
		String subtrahend = sourceOfADDorSUBOperation(first, second);
		int memoryIndex = memoryIndexOfFirst(first);
		int difference;
		if (memoryIndex <= numberOfInstructions * 6 || memoryIndex >= 64 * 1024) {
			System.out.println("Bad Memory Address");
			System.exit(0);
		}
		if (memory[memoryIndex] != null) {
			while (subtrahend.length() < 4) {
				subtrahend = "0" + subtrahend;
			}
			if (Integer.parseInt("" + memory[memoryIndex].charAt(3), 16)
					- Integer.parseInt("" + subtrahend.charAt(3), 16) < 0) {
				// to check whether any carry from 5th bit to 4th
				AF = true;
			}
			int minuend = Integer.parseInt(memory[memoryIndex], 16);
			difference = minuend - Integer.parseInt(subtrahend, 16);
			if (difference == 0) {
				ZF = true;
			} else if (difference < 0) {
				difference += Integer.parseInt("10000", 16);
				CF = true;
				SF = true;
			}
		} else {// minuend is empty(which means 0), result is -subtrahend
			difference = Integer.parseInt("10000", 16) - Integer.parseInt(subtrahend, 16);
			CF = true;
			SF = true;
			AF = true;
		}
		memory[memoryIndex] = NumberToFourByteHexa(difference + "");
	}

	/**
	 * calculates and returns the memoryIndex of input parameter
	 * 
	 * @param input [bx] or [bp] or [di] or [si] or [number] otherwise error.
	 * @return memoryIndex of input parameter
	 */
	private int memoryIndexOfFirst(String input) {
		input = input.substring(1, input.length() - 1);// got rid of "[","]"
		int memoryIndex = 0;// memory index of first operand
		if (input.equalsIgnoreCase("bx")) {
			memoryIndex = Integer.parseInt("" + bx[0] + bx[1] + bx[2] + bx[3], 16);
		} else if (input.equalsIgnoreCase("bp")) {
			memoryIndex = Integer.parseInt("" + bp[0] + bp[1] + bp[2] + bp[3], 16);
		} else if (input.equalsIgnoreCase("si")) {
			memoryIndex = Integer.parseInt("" + si[0] + si[1] + si[2] + si[3], 16);
		} else if (input.equalsIgnoreCase("di")) {
			memoryIndex = Integer.parseInt("" + di[0] + di[1] + di[2] + di[3], 16);
		} else if (isRegOneByte(input) || isRegTwoByte(input)) {
			System.out.println("#ERROR 39: Bad Index Register");
			System.exit(0);
		} else {
			memoryIndex = Integer.parseInt(NumberToFourByteHexa(input), 16);
		}
		return memoryIndex;
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
	 * this function just calculates source(addend of addition operation) of ADD or
	 * SUB operation when first operand is 16 bit
	 * 
	 * @param second source operand of ADD operation
	 * @return source operand of ADD in hexadecimal representation
	 */
	private String contentsOfSecondOperandOfADDSUBTwoByte(String second) {

		String addend = "";
		if (second.contains("[") && second.contains("]")) {

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
			second = NumberToFourByteHexa(second); // number
			addend += second;
		}
		return addend;
	}

	/**
	 * this function just calculates source(addend of addition operation or
	 * subtrahend of subtraction operation) of ADD or SUB operation when first
	 * operand is 8 bit
	 * 
	 * @param second source operand of ADD or SUB operation
	 * @return source operand of ADD or SUB in hexadecimal representation
	 */
	private String contentsOfSecondOperandOfADDSUBOneByte(String second) {
		// to check whether source is variable

		String addend = "";// even though it's name until I get the memory address, this variable holds
							// address.
		if (second.contains("[") && second.contains("]")) {// if source is a memory address

			if (second.charAt(0) == 'w') { // 2 byte
				System.out.println("#ERROR 13: Byte/Word Combination Not Allowed");
				System.exit(0);

			} else { // 1 byte
				if (second.charAt(0) == 'b') {
					second = second.substring(1); // got rid of 'b'
				}

				second = second.substring(1, second.length() - 1); // got rid of [ and ]

				if (isRegOneByte(second) || isRegTwoByte(second)) {// second is register within square brackets
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
					} else {// no other register is allowed within square brackets
						System.out.println("#ERROR 39: Bad Index Register ");
						System.exit(0);
					}
				} else {// number
					addend = NumberToFourByteHexa(second); // number within square brackets
				}
			}
			// now addend is a four length hexadecimal number which contains memory address
			if (Integer.parseInt(addend, 16) >= memory.length
					|| Integer.parseInt(addend, 16) < numberOfInstructions * 6) {// to check address is valid
				System.out.println("Address is not valid");
				System.exit(0);

			} else if (memory[Integer.parseInt(addend, 16)] == null) {// if that memory address was not initialized
																		// before
				addend = "0";
			} else { // if that memory address has a value inside
				addend = memory[Integer.parseInt(addend, 16)];
			}
			// addend is now the content of that address
			return addend.substring(addend.length() - 2, addend.length());// took last byte
		} else if (isRegOneByte(second) || isRegTwoByte(second)) { // addend is any register
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
			return addend;
		} else { // source is number
			second = NumberToFourByteHexa(second);
			if (Integer.parseInt(second, 16) > 255) {// since destination is one byte
				System.out.println("#ERROR 30: Byte-Sized Constant Required");
				System.exit(0);
			}
			return second;
		}
	}

	/**
	 * a helper method to determine whether parameter is a two byte register
	 * 
	 * @param regg String
	 * @return true if Two Byte Register
	 */
	private boolean isRegTwoByte(String regg) {
		return regg.equalsIgnoreCase("ax") || regg.equalsIgnoreCase("cx") || regg.equalsIgnoreCase("bx")
				|| regg.equalsIgnoreCase("dx") || regg.equalsIgnoreCase("di") || regg.equalsIgnoreCase("si")
				|| regg.equalsIgnoreCase("bp");
	}

	/**
	 * a helper method to determine whether parameter is a one byte register
	 * 
	 * @param regg String
	 * @return true if One Byte Register
	 */
	private boolean isRegOneByte(String regg) {
		return regg.equalsIgnoreCase("ah") || regg.equalsIgnoreCase("al") || regg.equalsIgnoreCase("bl")
				|| regg.equalsIgnoreCase("bh") || regg.equalsIgnoreCase("ch") || regg.equalsIgnoreCase("cl")
				|| regg.equalsIgnoreCase("dl") || regg.equalsIgnoreCase("dh");
	}

}
