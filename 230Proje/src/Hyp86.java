import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

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
	char[] ax = new char[4];
	char[] bx = new char[4];
	char[] cx = new char[4];
	char[] dx = new char[4];
	boolean ZF = false;
	boolean AF = false;
	boolean CF = false;
	boolean OF = false;
	boolean SF = false;
	int numberOfInstructions = 0;
	String SP = "fffe"; // stack pointer
	static int MP = 0;// memory Pointer

	/**
	 * Initializes registers. Adds instructions in an order such that operator is
	 * inserted in memory[6*k],first operand is inserted in memory[6*k+1] and ,if
	 * any,second operand is inserted in memory[6*k+2] until int 20h is inserted
	 * finally. It gets rid of commas at instructions, finally have "mov ax bx"
	 * instead of "mov ax,bx". Creates variable objects and puts labels and
	 * variables according lists.
	 * 
	 * @param fileAsString :instructions
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
		Scanner scanner = new Scanner(fileAsString);
		String line;
		Scanner token;
		String isChar1;

		int indexCursor = 0;
		boolean int20hCame = false;
		while (scanner.hasNextLine()) {
			line = scanner.nextLine();
			token = new Scanner(line);
			if (!token.hasNext()) {// for empty lines
				continue;
			}
			String first = token.next().toLowerCase();

			if (!int20hCame && instructionList.contains(first)) {// means instruction
				numberOfInstructions++;
				memory[indexCursor] = first;
				line = line.trim();
				line = line.substring(first.length());
				if (line.indexOf(',') != -1) {// when there is two operands

					int temo = line.indexOf(',');
					memory[indexCursor + 1] = line.substring(0, temo).trim().toLowerCase();// first operand
					isChar1 = line.substring(temo + 1, line.length()).trim();// second operand

					if (isChar1.contains("'")) {// when var.data is 'x'
						int a = isChar1.charAt(isChar1.indexOf("'") + 1) + 1 - 1;
						isChar1 = NumberToFourByteHexa("" + a, false).substring(2) + "h";
					} else if (isChar1.contains("\"")) {// when var.data is "x"
						int a = isChar1.charAt(isChar1.indexOf("\"") + 1) + 1 - 1;
						isChar1 = NumberToFourByteHexa("" + a, false).substring(2) + "h";
					}
					memory[indexCursor + 2] = isChar1.toLowerCase();
				} else {// when there is only one operand
					isChar1 = line.trim();
					if (isChar1.contains("'")) {// when var.data is 'x'
						int a = isChar1.charAt(isChar1.indexOf("'") + 1) + 1 - 1;
						isChar1 = NumberToFourByteHexa("" + a, false).substring(2) + "h";
					} else if (isChar1.contains("\"")) {// when var.data is "x"
						int a = isChar1.charAt(isChar1.indexOf("\"") + 1) + 1 - 1;
						isChar1 = NumberToFourByteHexa("" + a, false).substring(2) + "h";
					}
					memory[indexCursor + 1] = isChar1;
				}
				if (first.equals("int") && token.next().equals("20h")) {
					int20hCame = true;
				}
				indexCursor += 6;
			} else if (line.indexOf(":") != -1) {// means label
				line = line.trim().substring(0, line.indexOf(":"));
				labels.put(line, indexCursor);
				continue;
			} else if (line.indexOf("dw") != -1 || line.indexOf("db") != -1) {// variable definition
				if (token.next().equals("dw")) {
					variables.add(new Variable(first, 0, token.next(), true));
				} else {
					variables.add(new Variable(first, 0, token.next(), false));
				}
			} else if (line.trim().equalsIgnoreCase("code segment") || line.trim().equalsIgnoreCase("code ends")) {
				continue;
			} else {
				// System.out
				// .println("Undefined symbols are listed:" + first + " at line - " +
				// (numberOfInstructions + 1));
				System.out.println("error");
				System.exit(0);
			}
			token.close();
		}
		Variable x;
		for (int i = 0; i < variables.size(); i++) {
			x = variables.get(i);
			if (x.getType()) {
				memory[indexCursor + 1] = x.getData().substring(0, 2);
				memory[indexCursor] = x.getData().substring(2);
				x.setMemoryIndex(indexCursor);
				indexCursor += 2;
			} else {
				memory[indexCursor] = x.getData();
				x.setMemoryIndex(indexCursor);
				indexCursor += 1;
			}
		}
		scanner.close();
	}

	/**
	 * helper method to determine whether operand is variable
	 * 
	 * @param token: candidate variable name
	 * @return : null if token is not variable otherwise memory address of variable
	 */

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
				if (var.getType()) {
					return "w[" + var.getMemoryIndex() + "d]";
				}
				return "b[" + var.getMemoryIndex() + "d]";
			}
		}
		return null;
	}

	/**
	 * After instructions were inserted into memory this method starts from 0th
	 * index to memory then calls corresponding operator with operands and exits
	 * when int 20h came.
	 */
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

				shl(first, second, false);
			} else if (memory[MP].equals("shr")) {
				first = execute_helper_isVar(memory[MP + 1]);
				second = execute_helper_isVar(memory[MP + 2]);
				if (first == null) {
					first = memory[MP + 1];
				}
				if (second == null) {
					second = memory[MP + 2];
				}
				shr(first, second, false);
			} else if (memory[MP].equals("rcl")) {
				first = execute_helper_isVar(memory[MP + 1]);
				second = execute_helper_isVar(memory[MP + 2]);
				if (first == null) {
					first = memory[MP + 1];
				}
				if (second == null) {
					second = memory[MP + 2];
				}

				shl(first, second, true);
			} else if (memory[MP].equals("nop")) {

			} else if (memory[MP].equals("rcr")) {
				first = execute_helper_isVar(memory[MP + 1]);
				second = execute_helper_isVar(memory[MP + 2]);
				if (first == null) {
					first = memory[MP + 1];
				}
				if (second == null) {
					second = memory[MP + 2];
				}

				shr(first, second, true);
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

				if (execute_helper_isVar(memory[MP + 1]) != null) {
					System.out.println("error");
					System.exit(0);
				}
				first = memory[MP + 1];
				not(first);
			} else if (memory[MP].equals("inc")) {
				first = execute_helper_isVar(memory[MP + 1]);
				if (first == null) {
					first = memory[MP + 1];
				}
				add(first, "1");
			} else if (memory[MP].equals("dec")) {
				first = execute_helper_isVar(memory[MP + 1]);
				if (first == null) {
					first = memory[MP + 1];
				}
				sub(first, "1");
			} else if (memory[MP].equals("jmp")) {
				jmp(memory[MP + 1]);
				continue;
			} else if (memory[MP].equals("jz") || memory[MP].equals("je")) {
				if (ZF) {
					jmp(memory[MP + 1]);
					continue;
				}
			} else if (memory[MP].equals("jnz") || memory[MP].equals("jne")) {
				if (!ZF) {
					jmp(memory[MP + 1]);
					continue;
				}
			} else if (memory[MP].equals("jbe") || memory[MP].equals("jna")) {
				if (ZF || CF) {
					jmp(memory[MP + 1]);
					continue;
				}
			} else if (memory[MP].equals("ja") || memory[MP].equals("jnbe")) {
				if (!CF && !ZF) {
					jmp(memory[MP + 1]);
					continue;
				}

			} else if (memory[MP].equals("jb") || memory[MP].equals("jnae") || memory[MP].equals("jc")) {
				if (CF) {
					jmp(memory[MP + 1]);
					continue;
				}
			} else if (memory[MP].equals("jae") || memory[MP].equals("jnb") || memory[MP].equals("jnc")) {
				if (!CF) {
					jmp(memory[MP + 1]);
					continue;
				}
			} else {
				System.out.println("Undefined symbols are listed: " + memory[MP] + " at line: " + (MP / 6 + 1));
				System.exit(0);
			}
			MP += 6;
		}
	}

	/**
	 * Jumps if label exists otherwise gives error.
	 * 
	 * @param label: name of label to be jumped
	 */
	public void jmp(String label) {
		if (labels.containsKey(label)) {
			MP = labels.get(label);
		} else {
			// System.out.println("label does not exist: " + label);
			System.out.println("error");
			System.exit(0);
		}
	}

	/**
	 * push allows pushing a register, memory address, variable, or number
	 * 
	 * @param reg : token to be pushed
	 */

	public void push(String reg) {
		int index = Integer.parseInt(SP, 16);

		if (isRegOneByte(reg)) {
//			System.out.println("#ERROR 13: Byte/Word Combination Not Allowed" + " at line: " + (MP / 6 + 1));
			System.out.println("error");
			System.exit(0);
		}
		if (reg.indexOf("[") != -1 && reg.indexOf("]") != -1) {
			if (reg.charAt(0) == 'b') {
				// System.out.println("#ERROR 13: Byte/Word Combination Not Allowed" + " at
				// line: " + (MP / 6 + 1));
				System.out.println("error");
				System.exit(0);
			} else if (reg.charAt(0) == '[') {
				reg = "w" + reg;
			}
		}
		mov("w[" + index + "]", reg);

		SP = NumberToFourByteHexa("" + (index - 2), false);
	}

	/**
	 * pop allows popping to a variable, register or memory address register and
	 * variable cases are perfect
	 * 
	 * @param reg : token to be popped
	 */

	public void pop(String reg) {
		int index = Integer.parseInt(SP, 16) + 2;
		if (index >= 1024 * 64) {
			// System.out.println("no element to pop" + " at line: " + (MP / 6 + 1));
			System.out.println("error");
			System.exit(0);
		}
		if (isRegOneByte(reg)) {
//			System.out.println("#ERROR 13: Byte/Word Combination Not Allowed" + " at line: " + (MP / 6 + 1));
			System.out.println("error");
			System.exit(0);
		}
		if (reg.indexOf("[") != -1 && reg.indexOf("]") != -1) {
			if (reg.charAt(0) == 'b') {
//				System.out.println("#ERROR 13: Byte/Word Combination Not Allowed" + " at line: " + (MP / 6 + 1));
				System.out.println("error");
				System.exit(0);
			} else if (reg.charAt(0) == '[') {
				reg = "w" + reg;
			}
		}
		SP = NumberToFourByteHexa("" + index, true);
		mov(reg, "0" + memory[index + 1] + memory[index]);
		memory[index] = null;
		memory[index + 1] = null;

	}

	/**
	 * It shifts left or rotates left with carry flag with the help of third
	 * parameter isRotate.
	 * 
	 * @param first    : first operand of SHL or RCL.
	 * @param number   : second operand of SHL or RCL. Means how many times to
	 *                 rotate or shift.
	 * @param isRotate if it's true it rotates left with carry flag otherwise it
	 *                 shifts left.
	 */
	public void shl(String first, String number, boolean isRotate) {
//		
//		OF = false;
//		CF = false;
//		ZF = false;
		int numero;
		if (number.equalsIgnoreCase("cl")) {
			numero = Integer.parseInt("" + cx[2] + cx[3], 16);
		} else {// immediate number
			numero = Integer.parseInt(NumberToFourByteHexa(number, false), 16);
		}
		if (numero > 31) {
			// System.out.println("#ERROR 29: Bad Rotate/Shift Operand" + "at line: " + (MP
			// / 6 + 1));
			System.out.println("error");
			System.exit(0);
		}
		int otherMult = 0;
		String regg;

		if (first.contains("[") && first.contains("]")) { // first is memory
			int index = memoryIndexOfFirst(first);
			if (first.charAt(0) == 'w') {
				for (int i = 0; i < numero; i++) {
					regg = memory[index + 1] + memory[index];
					otherMult = Integer.parseInt(regg, 16);
					if (isRotate && CF)
						mov("w[" + index + "]", "" + (otherMult * 2 + 1) + "d");
					else
						mov("w[" + index + "]", "" + (otherMult * 2) + "d");
					if (Integer.parseInt("" + regg.charAt(0), 16) <= 7)
						CF = false;
					else
						CF = true;
				}
				if (Integer.parseInt(memory[index + 1] + memory[index], 16) == 0) {
					ZF = true;
				}
			} else {
				for (int i = 0; i < numero; i++) {
					regg = memory[index];
					otherMult = Integer.parseInt(regg, 16);
					if (isRotate && CF)
						mov("b[" + index + "]", "" + (otherMult * 2 + 1) + "d");
					else
						mov("b[" + index + "]", "" + (otherMult * 2) + "d");

					if (Integer.parseInt("" + regg.charAt(0), 16) <= 7)
						CF = false;
					else
						CF = true;

				}
				if (Integer.parseInt(memory[index], 16) == 0) {
					ZF = true;
				}
			}

		} else if (isRegTwoByte(first)) {// 16 bit register

			for (int i = 0; i < numero; i++) {
				regg = source_when_first_operand_is_twoByteReg(first);
				otherMult = Integer.parseInt(regg, 16);
				if (isRotate && CF)
					mov(first, "" + (otherMult * 2 + 1) + "d");
				else
					mov(first, "" + (otherMult * 2) + "d");

				if (Integer.parseInt("" + regg.charAt(0), 16) <= 7)
					CF = false;
				else
					CF = true;

			}
			if (Integer.parseInt(source_when_first_operand_is_twoByteReg(first), 16) == 0) {
				ZF = true;
			}

		} else if (isRegOneByte(first)) {// 8 bit register

			for (int i = 0; i < numero; i++) {
				regg = source_when_first_operand_is_oneByteReg(first);
				otherMult = Integer.parseInt(regg, 16);

				if (isRotate && CF) {
					if (otherMult * 2 + 1 > 255) {
						mov(first, "" + (otherMult * 2 + 1 - 256) + "d");
					} else {
						mov(first, "" + (otherMult * 2 + 1) + "d");
					}

				} else {
					if (otherMult * 2 > 255) {
						mov(first, "" + (otherMult * 2 - 256) + "d");
					} else {
						mov(first, "" + (otherMult * 2) + "d");
					}
				}
				if (Integer.parseInt("" + regg.charAt(0), 16) <= 7)
					CF = false;
				else
					CF = true;
			}

			if (Integer.parseInt(source_when_first_operand_is_oneByteReg(first), 16) == 0) {
				ZF = true;
			}
		} else { // not a reg or memory
			// System.out.println("Undefined symbols are listed: " + first + " at line - " +
			// (MP / 6 + 1));
			System.out.println("error");
			System.exit(0);
		}

	}

	/**
	 * It shifts right or rotates right with carry flag with the help of third
	 * parameter isRotate.
	 * 
	 * @param first    : first operand of SHR or RCR.
	 * @param number   : second operand of SHR or RCR. Means how many times to
	 *                 rotate or shift.
	 * @param isRotate if it's true it rotates right with carry flag otherwise it
	 *                 shifts right.
	 */

	public void shr(String first, String number, boolean isRotate) {
//		OF = false;
//		CF = false;
//		ZF = false;
		int numero;
		if (number.equalsIgnoreCase("cl")) {
			numero = Integer.parseInt("" + cx[2] + cx[3], 16);
		} else {
			numero = Integer.parseInt(NumberToFourByteHexa(number, false), 16);
		}
		if (numero > 31) {
			// System.out.println("#ERROR 29: Bad Rotate/Shift Operand" + "at line: " + (MP
			// / 6 + 1));
			System.out.println("error");
			System.exit(0);
		}

		int otherMult = 0;
		String regg;
		if (first.contains("[") && first.contains("]")) { // first is memory
			int index = memoryIndexOfFirst(first);
			if (first.charAt(0) == 'w') {
				for (int i = 0; i < numero; i++) {
					regg = memory[index + 1] + memory[index];
					otherMult = Integer.parseInt(regg, 16);
					if (isRotate && CF)
						// TODO 32768 may not be true
						mov("w[" + index + "]", "" + (otherMult / 2 + 32768) + "d");
					else
						mov("w[" + index + "]", "" + (otherMult / 2) + "d");
					if (Integer.parseInt("" + regg.charAt(3), 16) % 2 == 0)
						CF = false;
					else
						CF = true;
				}
				if (Integer.parseInt(memory[index + 1] + memory[index], 16) == 0) {
					ZF = true;
				}
			} else {
				for (int i = 0; i < numero; i++) {
					regg = memory[index];
					otherMult = Integer.parseInt(regg, 16);
					if (isRotate && CF)// TODO
						mov("b[" + index + "]", "" + (otherMult / 2 + 128) + "d");
					else
						mov("b[" + index + "]", "" + (otherMult / 2) + "d");
					if (Integer.parseInt("" + regg.charAt(1), 16) % 2 == 0)
						CF = false;
					else
						CF = true;
				}
			}
			if (Integer.parseInt(memory[index], 16) == 0) {
				ZF = true;
			}
		} else if (isRegTwoByte(first)) {// 16 bit register

			for (int i = 0; i < numero; i++) {
				regg = source_when_first_operand_is_twoByteReg(first);
				otherMult = Integer.parseInt(regg, 16);
				if (isRotate && CF)
					mov(first, "" + (otherMult / 2 + 32768) + "d");
				else
					mov(first, "" + (otherMult / 2) + "d");
				if (Integer.parseInt("" + regg.charAt(3), 16) % 2 == 0)
					CF = false;
				else
					CF = true;
			}
			if (otherMult / 2 == 0) {
				ZF = true;
			}
		} else if (isRegOneByte(first)) {// 8 bit register

			for (int i = 0; i < numero; i++) {
				regg = source_when_first_operand_is_oneByteReg(first);
				otherMult = Integer.parseInt(regg, 16);
				if (isRotate && CF)
					mov(first, "" + (otherMult / 2 + 128) + "d");
				else
					mov(first, "" + (otherMult / 2) + "d");
				if (Integer.parseInt("" + regg.charAt(1), 16) % 2 == 0)
					CF = false;
				else
					CF = true;
			}
			if (otherMult / 2 == 0) {
				ZF = true;
			}

		} else { // not a reg or memory
			// System.out.println("Undefined symbols are listed: " + first + " at line - " +
			// (MP / 6 + 1));
			System.out.println("error");
			System.exit(0);
		}

	}

	/**
	 * interrupt with 01 and 02 functions. 01 for getting a char input, 02 for
	 * writing an output char
	 */
	public void int21h() {
		String input;
		String ah = "" + ax[0] + ax[1];
		int ascii;
		@SuppressWarnings("resource")
		Scanner conc = new Scanner(System.in);
		if (ah.equals("01")) {
			input = conc.next();
			ascii = (int) (input.charAt(0));
			String hexa = NumberToFourByteHexa("" + ascii, false);
			ax[2] = hexa.charAt(2);
			ax[3] = hexa.charAt(3);
		} else if (ah.equals("02")) {
			String chs = "" + dx[2] + dx[3];
			ascii = Integer.parseInt(chs, 16);
			System.out.print((char) ascii);
			ax[2] = dx[2];
			ax[3] = dx[3];
		}
	}

	/**
	 * all compare instructions and methods are based on the design of MOV
	 * instructions
	 * 
	 * @param first  : first operand of CMP operation.
	 * @param second : second operand of CMP operation.
	 */
	public void cmp(String first, String second) {
		SF = false;
		AF = false;
		CF = false;
		ZF = false;
		if (first.contains("[") && first.contains("]")) { // first is memory
			cmp_mem_xx(first, second);
		} else if (isRegTwoByte(first) || first.equalsIgnoreCase("sp")) {// 16 bit register
			CMP_TwoByteReg(first, second);
		} else if (isRegOneByte(first)) {// 8 bit register
			CMP_OneByteReg(first, second);
		} else {
			// System.out.println("Undefined symbols are listed: " + first + " at line - " +
			// (MP / 6 + 1));
			System.out.println("error");
			System.exit(0);
		}
	}

	/**
	 * This helper function is called when first operand is a memory address. It
	 * compares values of two parameters then sets flags or gives error.
	 * 
	 * @param first  : first operand of CMP operation.
	 * @param second : second operand of CMP operation.
	 */
	private void cmp_mem_xx(String first, String second) { //
		String source = "";

		if (first.charAt(0) == 'b') {// first operand is kind of b[xx] so source must be one byte.
			first = first.substring(2, first.length() - 1);// got rid of "b[" and "]"
			source = contentsOfSecondOperandOfADDSUBOneByte(second);
		} else if (first.charAt(0) == 'w') {// first operand is kind of w[xx] so source must be two byte.
			first = first.substring(1); // got rid of "w"
		} else {
			first = first.substring(1, first.length() - 1);// got rid of "[" and "]"
			source = contentsOfSecondOperandOfADDSUBOneByte(second);
		}
		int memoryIndex = memoryIndexOfFirst(first);
		int dest = Integer.parseInt(memory[memoryIndex], 16);
		int src = Integer.parseInt(source, 16);
		// for AF
		if (memory[memoryIndex].charAt(memory[memoryIndex].length() - 1) < source.charAt(source.length() - 1))
			AF = true;
		if (dest > src) {
			CF = false;
			ZF = false;
		} else if (dest < src) {
			SF = true;
			CF = true;
			ZF = false;
		} else {
			ZF = true;
		}
	}

	/**
	 * This helper function is called when first operand is a two byte register. It
	 * compares values of two parameters then sets flags or gives error.
	 * 
	 * @param first  : first operand of CMP operation.
	 * @param second : second operand of CMP operation.
	 */
	private void CMP_TwoByteReg(String first, String second) {
		int destValue = 0;
		int srcValue = Integer.parseInt(contentsOfSecondOperandOfADDSUBTwoByte(second), 16);

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
		} else if (first.equalsIgnoreCase("sp")) {
			destValue = Integer.parseInt(SP, 16);
		} else {// error
				// System.out.println("#ERROR 13: Byte/Word Combination Not Allowed. At line - "
				// + (MP / 6 + 1));
			System.out.println("error");
			System.exit(0);
		}
		if (Integer.toHexString(destValue).charAt(Integer.toHexString(destValue).length() - 1) < Integer
				.toHexString(srcValue).charAt(Integer.toHexString(srcValue).length() - 1)) {
			AF = true;
		}
		if (destValue > srcValue) {
			CF = false;
			ZF = false;
		} else if (destValue < srcValue) {
			SF = true;
			CF = true;
			ZF = false;
		} else {
			ZF = true;
		}

	}

	/**
	 * This helper function is called when first operand is a one byte register. It
	 * compares values of two parameters then sets flags or gives error.
	 * 
	 * @param first  : first operand of CMP operation.
	 * @param second : second operand of CMP operation.
	 */

	private void CMP_OneByteReg(String first, String second) {
		int destValue = 0;
		int srcValue = Integer.parseInt(contentsOfSecondOperandOfADDSUBOneByte(second), 16);

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
		if (Integer.toHexString(destValue).charAt(Integer.toHexString(destValue).length() - 1) < Integer
				.toHexString(srcValue).charAt(Integer.toHexString(srcValue).length() - 1)) {
			AF = true;
		}
		if (destValue > srcValue) {
			CF = false;
			ZF = false;
		} else if (destValue < srcValue) {
			CF = true;
			SF = true;
			ZF = false;
		} else {
			ZF = true;
		}

	}

	/**
	 * Calls helper method with values of parameters and clears CF and OF.
	 * 
	 * @param first  : first operand of XOR operator.
	 * @param second : second operand of XOR operator.
	 */

	public void xor(String first, String second) {
		CF = false;
		OF = false;
		if (first.contains("[") && first.contains("]")) { // first is memory
			String source = source_when_first_operand_is_memory(first, second);
			if (first.charAt(0) == 'b') {
				first = first.substring(1);// got rid of b
				int memoryIndex = memoryIndexOfFirst(first);
				memory[memoryIndex] = helperXor(memory[memoryIndex], source).substring(2);
			} else if (first.charAt(0) == 'w') {
				first = first.substring(1);// got rid of w
				int memoryIndex = memoryIndexOfFirst(first);
				if (memory[memoryIndex + 1] == null)
					memory[memoryIndex + 1] = "00";
				String data = helperXor("" + memory[memoryIndex + 1] + memory[memoryIndex], source);
				memory[memoryIndex] = data.substring(2);
				memory[memoryIndex + 1] = data.substring(0, 2);
			}
		} else if (first.equalsIgnoreCase("sp")) {
			// TODO sp first operand olabilir
			String source = source_when_first_operand_is_twoByteReg(second);
			SP = helperXor(SP, source);
		} else if (isRegTwoByte(first)) {// 16 bit register
			String dest = contentsOfTwoByteRegister(first);
			String source = source_when_first_operand_is_twoByteReg(second);
			mov(first, "0" + helperXor(dest, source));
		} else if (isRegOneByte(first)) {// 8 bit register
			String source = source_when_first_operand_is_oneByteReg(second);
			String dest = contentsOfOneByteRegister(first);
			mov(first, "0" + helperXor(dest, source).substring(2));
		} else {
			// System.out.println("Undefined symbols are listed: " + first + " at line: " +
			// (MP / 6 + 1));
			System.out.println("error");
			System.exit(0);
		}

	}

	/**
	 * xor's two operand and returns 4 digit hexa representation and sets ZF
	 * accordingly.
	 * 
	 * @param first  : first operand of XOR operator.
	 * @param second : second operand of XOR operator.
	 * @return 4 digit hexadecimal number.
	 */
	private String helperXor(String first, String second) {
		int a = Integer.parseInt(first, 16) ^ Integer.parseInt(second, 16);
		if (a == 0)
			ZF = true;
		return NumberToFourByteHexa(a + "d", false);
	}

	/**
	 * Calls helper method with values of parameters and clears CF and OF.
	 * 
	 * @param first  : first operand of OR operator.
	 * @param second : second operand of OR operator.
	 */
	public void or(String first, String second) {
		CF = false;
		OF = false;

		if (first.contains("[") && first.contains("]")) {
			// first is memory
			String source = source_when_first_operand_is_memory(first, second);
			if (first.charAt(0) == 'b') {
				first = first.substring(1);// got rid of b
				int memoryIndex = memoryIndexOfFirst(first);
				memory[memoryIndex] = helperOr(memory[memoryIndex], source).substring(2);
			} else if (first.charAt(0) == 'w') {
				first = first.substring(1);// got rid of w
				int memoryIndex = memoryIndexOfFirst(first);
				if (memory[memoryIndex + 1] == null)
					memory[memoryIndex + 1] = "00";
				String data = helperOr("" + memory[memoryIndex + 1] + memory[memoryIndex], source);
				memory[memoryIndex] = data.substring(2);
				memory[memoryIndex + 1] = data.substring(0, 2);
			}
		} else if (first.equalsIgnoreCase("sp")) {
			// TODO sp first operand olabilir
			String source = source_when_first_operand_is_twoByteReg(second);
			SP = helperOr(SP, source);
		} else if (isRegTwoByte(first)) {// 16 bit register
			String source = source_when_first_operand_is_twoByteReg(second);
			String dest = contentsOfTwoByteRegister(first);
			mov(first, "0" + helperOr(dest, source));
		} else if (isRegOneByte(first)) {// 8 bit register
			String source = source_when_first_operand_is_oneByteReg(second);
			String dest = contentsOfOneByteRegister(first);
			mov(first, "0" + helperOr(dest, source).substring(2));
		} else {
			// System.out.println("Undefined symbols are listed: " + first + " at line: " +
			// (MP / 6 + 1));
			System.out.println("error");
			System.exit(0);
		}

	}

	/**
	 * or's two operand and returns 4 digit hexa representation and sets ZF
	 * accordingly.
	 * 
	 * @param first  : first operand of OR operator.
	 * @param second : second operand of OR operator.
	 * @return 4 digit hexadecimal number.
	 */
	private String helperOr(String first, String second) {
		int a = Integer.parseInt(first, 16) | Integer.parseInt(second, 16);
		if (a == 0)
			ZF = true;
		return NumberToFourByteHexa(a + "d", false);
	}

	/**
	 * It subtracts the value of operand from 0xffff or 0xff and does not change
	 * flags.
	 * 
	 * @param operand : only operand of NOT operator
	 */
	public void not(String operand) {
		if (operand.contains("[") && operand.contains("]")) {
			if (operand.charAt(0) == 'w') {
				int memoryIndex = memoryIndexOfFirst(operand.substring(1).trim());
				String data = Integer
						.toHexString(0xffff - Integer.parseInt(memory[memoryIndex + 1] + memory[memoryIndex] + "", 16));
				while (data.length() < 4)
					data = "0" + data;
				memory[memoryIndex] = data.substring(2);
				memory[memoryIndex + 1] = data.substring(0, 2);
			} else if (operand.charAt(0) == 'b') {
				int memoryIndex = memoryIndexOfFirst(operand.substring(1).trim());
				String data = Integer.toHexString(0xff - Integer.parseInt(memory[memoryIndex] + "", 16));
				while (data.length() < 2)
					data = "0" + data;
				memory[memoryIndex] = data;
			} else {
				// System.out.println("There must be 'w' or 'b' in front of square brackets.");
				System.out.println("error");
				System.exit(0);
			}
		} else if (isRegOneByte(operand)) {
			String data = source_when_first_operand_is_oneByteReg(operand);
			mov(operand, "0" + Integer.toHexString(0xff - Integer.parseInt(data, 16)));
		} else if (isRegTwoByte(operand)) {
			String data = source_when_first_operand_is_twoByteReg(operand);
			mov(operand, "0" + Integer.toHexString(0xffff - Integer.parseInt(data, 16)));
		} else {
			// System.out.println("ERROR 21: Bad Single Operand. at line - ");
			System.out.println("error");
			System.exit(0);
		}
	}

	/**
	 * Calls helper method with values of parameters and clears CF and OF.
	 * 
	 * @param first  : first operand of AND operator.
	 * @param second : second operand of AND operator.
	 */
	public void and(String first, String second) {
		CF = false;
		OF = false;

		if (first.contains("[") && first.contains("]")) { // first is memory
			String source = source_when_first_operand_is_memory(first, second);
			if (first.charAt(0) == 'b') {
				first = first.substring(1);// got rid of b
				int memoryIndex = memoryIndexOfFirst(first);
				memory[memoryIndex] = helperAnd(memory[memoryIndex], source).substring(2);
			} else if (first.charAt(0) == 'w') {
				first = first.substring(1);// got rid of w
				int memoryIndex = memoryIndexOfFirst(first);
				if (memory[memoryIndex + 1] == null)
					memory[memoryIndex + 1] = "00";
				String data = helperAnd("" + memory[memoryIndex + 1] + memory[memoryIndex], source);
				memory[memoryIndex] = data.substring(2);
				memory[memoryIndex + 1] = data.substring(0, 2);
			}
		} else if (first.equalsIgnoreCase("sp")) {
			// TODO sp first operand olabilir
			String source = source_when_first_operand_is_twoByteReg(second);
			SP = helperAnd(SP, source);
		} else if (isRegTwoByte(first)) {// 16 bit register
			String dest = contentsOfTwoByteRegister(first);
			String source = source_when_first_operand_is_twoByteReg(second);
			mov(first, "0" + helperAnd(dest, source));

		} else if (isRegOneByte(first)) {// 8 bit register
			String source = source_when_first_operand_is_oneByteReg(second);
			String dest = contentsOfOneByteRegister(first);
			mov(first, "0" + helperAnd(dest, source).substring(2));
		} else {
			// System.out.println("Undefined symbols are listed: " + first + " at line: " +
			// (MP / 6 + 1));
			System.out.println("error");
			System.exit(0);
		}

	}

	/**
	 * And's two operand and returns 4 digit hexa representation and sets ZF
	 * accordingly.
	 * 
	 * @param first  : first operand of AND operator.
	 * @param second : second operand of AND operator.
	 * @return 4 digit hexadecimal number.
	 */
	private String helperAnd(String first, String second) {
		int a = Integer.parseInt(first, 16) & Integer.parseInt(second, 16);
		if (a == 0)
			ZF = true;
		return NumberToFourByteHexa(a + "d", false);
	}

	/**
	 * Multiplication operation, works on AX register
	 * chooses one of the multiplier based on the given sources byte-size
	 * @param first : source of MUL operation
	 */
	public void mul(String first) {
		CF = false;
		OF = false;
		String source;
		if (Integer.parseInt("" + ax[0], 16) > 7) {
			CF = true;
			OF = true;
		} else {
			CF = false;
			OF = false;
		}
		if (first.contains("[") && first.contains("]")) { // first is memory
			if (first.charAt(0) == 'w') {
				source = contentsOfSecondOperandOfADDSUBTwoByte(first);
				mul_2Byte(source);
			} else if (first.charAt(0) == 'b') {
				source = contentsOfSecondOperandOfADDSUBOneByte(first);
				mul_1Byte(source);
			}

		} else if (isRegTwoByte(first) || first.equalsIgnoreCase("sp")) {// 16 bit register
			source = contentsOfSecondOperandOfADDSUBTwoByte(first);
			mul_2Byte(source);
		} else if (isRegOneByte(first)) {// 8 bit register
			source = contentsOfSecondOperandOfADDSUBOneByte(first);
			mul_1Byte(source);
		} else {
			// System.out.println("Undefined symbols are listed: " + first + " at line: " +
			// (MP / 6 + 1));
			System.out.println("error");
			System.exit(0);
		}

	}

	/**
	 * if a 1-byte source is called, other multiplier is al
	 * and result is hold in ax
	 * @param source : source of MUL operation
	 */
	private void mul_1Byte(String source) {
		int dest = Integer.parseInt("" + ax[2] + ax[3], 16);
		dest *= Integer.parseInt(source, 16);
		String result = Integer.toHexString(dest);
		while (result.length() < 4)
			result = "0" + result;
		for (int i = 0; i < 4; i++)
			ax[i] = result.charAt(i);

	}
	/**
	 * if a 2-byte source is called other multiplier is ax
	 * and result is hold in dx:ax
	 * @param source : source of MUL operation
	 */
	private void mul_2Byte(String source) {
		int dest = Integer.parseInt("" + ax[0] + ax[1] + ax[2] + ax[3], 16);
		dest *= Integer.parseInt(source, 16);
		String result = Integer.toHexString(dest);
		while (result.length() < 8)
			result = "0" + result;

		for (int i = 0; i < 4; i++) {
			ax[i] = result.charAt(i + 4);
			dx[i] = result.charAt(i);
		}

	}

	/**
	 * Division operation, works on AX register
	 * chooses the dividend based on the given sources byte-size
	 * @param first: source of DIV operation
	 */
	public void div(String first) {
		String source;
		if (first.contains("[") && first.contains("]")) { // first is memory
			if (first.charAt(0) == 'w') {
				source = contentsOfSecondOperandOfADDSUBTwoByte(first);
				div_2Byte(source);
			} else if (first.charAt(0) == 'b') {
				source = contentsOfSecondOperandOfADDSUBOneByte(first);
				div_1Byte(source);
			}

		} else if (isRegTwoByte(first) || first.equalsIgnoreCase("sp")) {// 16 bit register
			source = contentsOfSecondOperandOfADDSUBTwoByte(first);
			div_2Byte(source);
		} else if (isRegOneByte(first)) {// 8 bit register
			source = contentsOfSecondOperandOfADDSUBOneByte(first);
			div_1Byte(source);
		} else {
			// System.out.println("Undefined symbols are listed: " + first + " at line: " +
			// (MP / 6 + 1));
			System.out.println("error");
			System.exit(0);
		}
	}

	/**
	 * if a 1-byte source is called, other dividend is ax
	 * and quotient is hold in al, remainder is hold in ah
	 * @param source : source of DIV operation
	 */
	private void div_1Byte(String source) {
		int dest = Integer.parseInt("" + ax[0] + ax[1] + ax[2] + ax[3], 16);
		int src = Integer.parseInt(source, 16);
		if (dest / src > 0xff) {
			// System.out.println("divide overflow at line - " + (MP / 6 + 1));
			System.out.println("error");
			System.exit(0);
		}
		String quot = NumberToFourByteHexa("" + dest / src, false);
		String remainder = NumberToFourByteHexa("" + dest % src, false);
		ax[0] = remainder.charAt(2);
		ax[1] = remainder.charAt(3);
		ax[2] = quot.charAt(2);
		ax[3] = quot.charAt(3);

	}

	/**
	 * if a 2-byte source is called, other dividend is dx:ax
	 * and quotient is hold in ax, remainder is hold in dx
	 * @param source : source of DIV operation
	 */
	private void div_2Byte(String source) {
		int dest = Integer.parseInt("" + dx[0] + dx[1] + dx[2] + dx[3] + ax[0] + ax[1] + ax[2] + ax[3], 16);
		int src = Integer.parseInt(source, 16);

		if (dest / src > 0xffff) {
			// System.out.println("divide overflow at line - " + (MP / 6 + 1));
			System.out.println("error");
			System.exit(0);
		}
		String quot = NumberToFourByteHexa("" + dest / src, false);
		String remainder = NumberToFourByteHexa("" + dest % src, false);
		for (int i = 0; i < 4; i++) {
			ax[i] = quot.charAt(i);
			dx[i] = remainder.charAt(i);
		}

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
		} else if (isRegOneByte(first)) {
			String minuend = contentsOfOneByteRegister(first);
			String subtrahend = contentsOfSecondOperandOfADDSUBOneByte(second);
			int difference = Integer.parseInt(minuend, 16) - Integer.parseInt(subtrahend, 16);// decimal diff
			String differenceStringForm = NumberToFourByteHexa("" + difference, false);
			while (subtrahend.length() < 2) {
				subtrahend = "0" + subtrahend;
			}
			if (Integer.parseInt("" + minuend.charAt(minuend.length() - 1), 16)
					- Integer.parseInt("" + subtrahend.charAt(subtrahend.length() - 1), 16) < 0) {
				AF = true;
			}
			if (difference == 0) {
				ZF = true;
				differenceStringForm = "00";
			} else {
				if (difference < 0) {
					CF = true;
					SF = true;
					difference += 0x100;
					differenceStringForm = NumberToFourByteHexa("" + difference, false);
				}
			}
			ax[2] = differenceStringForm.charAt(differenceStringForm.length() - 2);
			ax[3] = differenceStringForm.charAt(differenceStringForm.length() - 1);
			mov(first, "0" + differenceStringForm.substring(2));
		} else if (isRegTwoByte(first)) {
			String minuend = contentsOfTwoByteRegister(first);
			String subtrahend = contentsOfSecondOperandOfADDSUBTwoByte(second);
			int difference = Integer.parseInt(minuend, 16) - Integer.parseInt(subtrahend, 16);
			String differenceStringForm = NumberToFourByteHexa("" + difference, false);

			while (subtrahend.length() < 4) {
				subtrahend = "0" + subtrahend;
			}
			if (Integer.parseInt("" + minuend.charAt(minuend.length() - 1), 16)
					- Integer.parseInt("" + subtrahend.charAt(3), 16) < 0) {
				AF = true;
			}
			if (difference == 0) {
				ZF = true;
				differenceStringForm = "0000";
			} else {
				if (difference < 0) {
					SF = true;
					CF = true;
					difference += 0x10000;
					differenceStringForm = NumberToFourByteHexa("" + difference, false);
				}
			}
			mov(first, "0" + differenceStringForm);
		} else {
			// System.out.println("Undefined symbols are listed: " + first + " at line: " +
			// (MP / 6 + 1));
			System.out.println("error");
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
		} else if (isRegOneByte(first)) {
			String augend = contentsOfOneByteRegister(first);
			String addend = contentsOfSecondOperandOfADDSUBOneByte(second);
			int sum = Integer.parseInt(augend, 16) + Integer.parseInt(addend, 16);// decimal sum
			String sumStringForm = NumberToFourByteHexa("" + sum, false);
			while (addend.length() < 2) {
				addend = "0" + addend;
			}
			if (Integer.parseInt("" + augend.charAt(augend.length() - 1), 16)
					+ Integer.parseInt("" + addend.charAt(addend.length() - 1), 16) > 15) {
				AF = true;
			}
			if (sum == 0) {
				ZF = true;
				sumStringForm = "00";
			} else {
				if (sum > 0xff) {
					CF = true;
					sum -= 256;
					sumStringForm = NumberToFourByteHexa("" + sum, false);
				}
			}
			mov(first, "0" + sumStringForm.substring(2));
		} else if (isRegTwoByte(first)) {
			String augend = contentsOfTwoByteRegister(first);
			String addend = contentsOfSecondOperandOfADDSUBTwoByte(second);
			int sum = Integer.parseInt(augend, 16) + Integer.parseInt(addend, 16);
			String sumStringForm = NumberToFourByteHexa("" + sum, false);

			// for AF
			while (addend.length() < 4) {
				addend = "0" + addend;
			}
			if (Integer.parseInt("" + augend.charAt(3), 16) + Integer.parseInt("" + addend.charAt(3), 16) > 15) {
				AF = true;
			}
			if (sum == 0) {
				ZF = true;
				sumStringForm = "0000";
			} else {
				if (sum > 0xffff) {
					CF = true;
					sum -= 0x10000;
					sumStringForm = NumberToFourByteHexa("" + sum, false);
				}
			}
			mov(first, "0" + sumStringForm);
		} else { // destination is neither reg nor memory
			// System.out.println("Undefined symbols are listed: " + first + " at line: " +
			// (MP / 6 + 1));
			System.out.println("error");
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
		String source = "";
		if (first.contains("[") && first.contains("]")) { // first is memory
			source = source_when_first_operand_is_memory(first, second);
			if (first.charAt(0) == 'w') {
				first = first.substring(1);// got rid of w
				int memoryIndex = memoryIndexOfFirst(first);
				memory[memoryIndex + 1] = source.substring(0, 2);
				memory[memoryIndex] = source.substring(2);
			} else if (first.charAt(0) == 'b') {
				first = first.substring(1);// got rid of b
				int memoryIndex = memoryIndexOfFirst(first);
				memory[memoryIndex] = source.substring(2);
			}
		} else if (isRegTwoByte(first)) {// 16 bit register
			source = source_when_first_operand_is_twoByteReg(second);

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
			source = source_when_first_operand_is_oneByteReg(second);
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
					bx[i + 2] = source.charAt(i);
				}
			} else if (first.equalsIgnoreCase("bh")) {
				for (int i = 0; i <= 1; i++) {
					bx[i] = source.charAt(i);
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
		} else {
			// System.out.println("Undefined symbols are listed: " + first + " at line: " +
			// (MP / 6 + 1));
			System.out.println("error");
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
		String source = "";

		if (first.charAt(0) == 'b') {// first operand is kind of b[xx] so source must be one byte.
			source = contentsOfSecondOperandOfADDSUBOneByte(second);
		} else if (first.charAt(0) == 'w') {// assume there is w
			source = contentsOfSecondOperandOfADDSUBTwoByte(second);
		} else {
			// System.out.println("There must be w or b in front of square brackets." + " at
			// line: " + (MP / 6 + 1));
			System.out.println("error");
			System.exit(0);
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
				// System.out.println("#ERROR 13: Byte/Word Combination Not Allowed" + " at
				// line: " + (MP / 6 + 1));
				System.out.println("error");
				System.exit(0);
			} else { // 1 byte
				if (second.charAt(0) == 'b') {
					second = second.substring(1); // got rid of "b"
				}
				second = second.substring(second.indexOf('[') + 1, second.length() - 1).trim(); // got rid of "[" and
																								// "]"
				String num = "";

				if (isRegOneByte(second) || isRegTwoByte(second) || second.equalsIgnoreCase("sp")) {// register
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
						// System.out.println("#ERROR 39: Bad Index Register " + " at line: " + (MP / 6
						// + 1));
						System.out.println("error");
						System.exit(0);
					}
				} else {// number
					num = NumberToFourByteHexa(second, true);
				}
				// now, num is the address in 4length hexa
				temp[0] = '0';
				temp[1] = '0';
				if (Integer.parseInt(num, 16) < numberOfInstructions * 6 || Integer.parseInt(num, 16) >= 64 * 1024) {
					// System.out.println("Address is not valid" + " at line: " + (MP / 6 + 1));
					System.out.println("error");
					System.exit(0);
				} else if (memory[Integer.parseInt(num, 16)] == null) {

				} else {
					num = memory[Integer.parseInt(num, 16)];// now, num is the content of that address
					for (int i = 0; i <= 1; i++) {
						temp[i] = num.charAt(i);
					}
				}
			}
		} else if (isRegOneByte(second) || isRegTwoByte(second) || second.equalsIgnoreCase("sp")) { // register
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
				// System.out.println("#ERROR 13: Byte/Word Combination Not Allowed" + " at
				// line: " + (MP / 6 + 1));
				System.out.println("error");
				System.exit(0);
			}
		} else { // number
			second = NumberToFourByteHexa(second, false); // number
			if (Integer.parseInt(second, 16) > 255) {
				// System.out.println("#ERROR 30: Byte-Sized Constant Required" + " at line: " +
				// (MP / 6 + 1));
				System.out.println("error");
				System.exit(0);
			}
			for (int i = 0; i <= 1; i++) {
				temp[i] = second.charAt(i + 2);
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
				// System.out.println("#ERROR 13: Byte/Word Combination Not Allowed" + " at
				// line: " + (MP / 6 + 1));
				System.out.println("error");
				System.exit(0);

			} else { // 2 byte
				if (second.charAt(0) == 'w') {
					second = second.substring(1); // got rid of 'w'
				}

				second = second.substring(second.indexOf('[') + 1, second.length() - 1).trim(); // got rid of [ and ]
				String num = "";
				if (isRegOneByte(second) || isRegTwoByte(second) || second.equalsIgnoreCase("sp")) {// register
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
						// System.out.println("#ERROR 39: Bad Index Register " + " at line: " + (MP / 6
						// + 1));
						System.out.println("error");
						System.exit(0);
					}
				} else {// number
					num = NumberToFourByteHexa(second, true);

				}
				// got the address
				if (Integer.parseInt(num, 16) >= memory.length
						|| Integer.parseInt(num, 16) < numberOfInstructions * 6) {
					// System.out.println("Address is not valid" + " at line: " + (MP / 6 + 1));
					System.out.println("error");
					System.exit(0);

				} else if (memory[Integer.parseInt(num, 16)] == null) {
					memory[Integer.parseInt(num, 16)] = "00";
					if (memory[Integer.parseInt(num, 16) + 1] == null) {
						memory[Integer.parseInt(num, 16) + 1] = "00";
					}
				} else {
					temp[0] = memory[Integer.parseInt(num, 16) + 1].charAt(0);
					temp[1] = memory[Integer.parseInt(num, 16) + 1].charAt(1);
					temp[2] = memory[Integer.parseInt(num, 16)].charAt(0);
					temp[3] = memory[Integer.parseInt(num, 16)].charAt(1);
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
				// System.out.println("#ERROR 13: Byte/Word Combination Not Allowed" + " at
				// line: " + (MP / 6 + 1));
				System.out.println("error");
				System.exit(0);
			}
		} else if (second.equalsIgnoreCase("sp")) {
			for (int i = 0; i <= 3; i++)
				temp[i] = SP.charAt(i);
		} else { // number
			second = NumberToFourByteHexa(second, false);
			for (int i = 0; i <= 3; i++) {
				temp[i] = second.charAt(i);
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
	 */// ++
	private String addsub_mem1B_xx(String second) {
		String addend = "";
		if (isRegTwoByte(second) || second.equalsIgnoreCase("sp")) {
			// System.out.println("#ERROR 13: Byte/Word Combination Not Allowed " + " at
			// line: " + (MP / 6 + 1));
			System.out.println("error");
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
			if (Integer.parseInt(NumberToFourByteHexa(second, false), 16) > 255) {
				// System.out.println("#ERROR 30: Byte-Sized Constant Required" + " at line: " +
				// (MP / 6 + 1));
				System.out.println("error");
				System.exit(0);
			} else {
				addend += NumberToFourByteHexa(second, false).substring(2);
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
			// System.out.println("#ERROR 13: Byte/Word Combination Not Allowed " + " at
			// line: " + (MP / 6 + 1));
			System.out.println("error");
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
		} else if (second.equalsIgnoreCase("sp")) {
			addend = SP;
		} else { // number

			addend += NumberToFourByteHexa(second, false);
		}
		return addend;
	}

	/**
	 * this method adds source to memory destination.
	 * 
	 * @param first  : first operand (augend) of ADD operation. It's a memory
	 *               address for sure.
	 * @param second : second operand (addend) of ADD operation.
	 */// ++
	private void add_mem_xx(String first, String second) {
		boolean wordOrByte = false;// false if byte, true if word
		String addend = sourceOfADDorSUBOperation(first, second);
		int memoryIndex = 0;
		if (first.charAt(0) == 'b') {
			wordOrByte = false;
			first = first.substring(1);// got rid of "b"
			memoryIndex = memoryIndexOfFirst(first);
		} else if (first.charAt(0) == 'w') {
			wordOrByte = true;
			first = first.substring(1);// got rid of "w"
			memoryIndex = memoryIndexOfFirst(first);
		}
		if (memoryIndex < numberOfInstructions * 6 || memoryIndex >= 64 * 1024) {
			// System.out.println("Bad Memory Address at line - " + (MP / 6 + 1));
			System.out.println("error");
			System.exit(0);
		}
		// augend + addend = sum
		if (wordOrByte) {// for inputs like w[xx]
			if (memory[memoryIndex] != null || memory[memoryIndex + 1] != null) {
				while (addend.length() < 4) {
					addend = "0" + addend;
				}
				if (Integer.parseInt("" + memory[memoryIndex].charAt(1), 16)
						+ Integer.parseInt("" + addend.charAt(3), 16) > 15) {// is there and carry from 4th bit to
																				// 5th
																				// bit?
					AF = true;
				}
				int augend = Integer.parseInt(memory[memoryIndex + 1] + "" + memory[memoryIndex], 16);
				int sum = Integer.parseInt(addend, 16) + augend;
				if (sum == 0) {
					ZF = true;
				} else if (sum == 0x10000) {
					CF = true;
					ZF = true;
					sum = 0;
				} else if (sum > 0x10000) {

					sum -= 0x10000;
					CF = true;
				}
				String data = NumberToFourByteHexa(sum + "", false);
				memory[memoryIndex + 1] = data.substring(0, 2);
				memory[memoryIndex] = data.substring(2);
			} else {// augend is empty(which means 0), result is addend
				memory[memoryIndex + 1] = addend.substring(0, 2);
				memory[memoryIndex] = addend.substring(2);
			}
		} else { // for inputs like b[xx]
			if (memory[memoryIndex] != null) {
				while (addend.length() < 2) {
					addend = "0" + addend;
				}
				if (Integer.parseInt("" + memory[memoryIndex].charAt(1), 16)
						+ Integer.parseInt("" + addend.charAt(1), 16) > 15) {// is there and carry from 4th bit to
																				// 5th
																				// bit?
					AF = true;
				}
				int augend = Integer.parseInt(memory[memoryIndex], 16);
				int sum = Integer.parseInt(addend, 16) + augend;
				if (sum == 0) {
					ZF = true;
				} else if (sum == 0x100) {
					CF = true;
					ZF = true;
					sum = 0;
				} else if (sum > 0x100) {
					sum -= 0x100;
					CF = true;
				}
				memory[memoryIndex] = NumberToFourByteHexa(sum + "", false).substring(2);
			} else {// augend is empty(which means 0), result is addend
				memory[memoryIndex] = addend;
			}
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
			// System.out.println("#ERROR 50: Reg,Mem Required " + " at line: " + (MP / 6 +
			// 1));
			System.out.println("error");
			System.exit(0);
		} else {
			if (first.charAt(0) == 'b') {// constant must be byte sized and regs too
				toBeReturned = addsub_mem1B_xx(second);
			} else if (first.charAt(0) == 'w') { // regs must be two byte sized
				toBeReturned = addsub_mem2B_xx(second);
			} else {
				// System.out.println("there must be 'b' or
				// 'w' in front of square brackets" + " at line:" + (MP / 6 + 1));
				System.out.println("error");
				System.exit(0);
			}
		}
		return toBeReturned;
	}

	/**
	 * this method is called when destination of SUB operation is a memory address.
	 * 
	 * @param first   : destination of SUB operation. It's a memory address for
	 *                sure.
	 * @param second: source of SUB operation.
	 */
	private void sub_mem_xx(String first, String second) {
		boolean wordOrByte = false;// false if byte, true if word
		String subtrahend = sourceOfADDorSUBOperation(first, second);
		int memoryIndex = 0;
		if (first.charAt(0) == 'b') {
			wordOrByte = false;
			first = first.substring(1);// got rid of "b"
			memoryIndex = memoryIndexOfFirst(first);
		} else if (first.charAt(0) == 'w') {
			wordOrByte = true;
			first = first.substring(1);// got rid of "w"
			memoryIndex = memoryIndexOfFirst(first);
		}
		int difference = 0;
		if (memoryIndex < numberOfInstructions * 6 || memoryIndex >= 64 * 1024) {
			// System.out.println("Bad Memory Address at line - " + (MP / 6 + 1));
			System.out.println("error");
			System.exit(0);
		}
		if (wordOrByte) {// for inputs like w[xx]
			if (memory[memoryIndex + 1] != null || memory[memoryIndex] != null) {
				if (memory[memoryIndex + 1] == null)
					memory[memoryIndex + 1] = "00";
				if (memory[memoryIndex] == null)
					memory[memoryIndex] = "00";
				while (subtrahend.length() < 4) {
					subtrahend = "0" + subtrahend;
				}
				if (Integer.parseInt("" + memory[memoryIndex].charAt(1), 16)
						- Integer.parseInt("" + subtrahend.charAt(3), 16) < 0) {
					// to check whether any carry from 5th bit to 4th
					AF = true;
				}
				int minuend = Integer.parseInt(memory[memoryIndex + 1] + "" + memory[memoryIndex], 16);
				difference = minuend - Integer.parseInt(subtrahend, 16);
				if (difference == 0) {
					ZF = true;
				} else if (difference < 0) {
					difference += 0x10000;
					CF = true;
					SF = true;
				}
			} else {// minuend is empty(which means 0), result is -subtrahend
				difference = 0x10000 - Integer.parseInt(subtrahend, 16);
				CF = true;
				SF = true;
				AF = true;
			}
			String diff = NumberToFourByteHexa(difference + "", false);
			memory[memoryIndex] = diff.substring(2);
			memory[memoryIndex + 1] = diff.substring(0, 2);
		} else {// for inputs like b[xx]
			if (memory[memoryIndex] != null) {
				while (subtrahend.length() < 2) {// to make sure
					subtrahend = "0" + subtrahend;
				}
				if (Integer.parseInt("" + memory[memoryIndex].charAt(1), 16)
						- Integer.parseInt("" + subtrahend.charAt(1), 16) < 0) {
					// to check whether any carry from 5th bit to 4th
					AF = true;
				}
				int minuend = Integer.parseInt(memory[memoryIndex], 16);
				difference = minuend - Integer.parseInt(subtrahend, 16);
				if (difference == 0) {
					ZF = true;
				} else if (difference < 0) {
					difference += 0x100;
					CF = true;
					SF = true;
				}
			} else {// minuend is empty(which means 0), result is -subtrahend
				difference = 0x100 - Integer.parseInt(subtrahend, 16);
				CF = true;
				SF = true;
				AF = true;
			}
			memory[memoryIndex] = NumberToFourByteHexa("" + difference, false).substring(2);
		}
	}

	/**
	 * calculates and returns the memoryIndex of input parameter
	 * 
	 * @param input [bx] or [bp] or [di] or [si] or [number] otherwise error.
	 * @return memoryIndex of input parameter
	 */
	private int memoryIndexOfFirst(String input) {
		if (input.contains("[") && input.contains("]"))
			input = input.substring(input.indexOf('[') + 1, input.length() - 1).trim();// got rid of "x[","]"
		int memoryIndex = 0;// memory index of first operand
		if (input.equalsIgnoreCase("bx")) {
			memoryIndex = Integer.parseInt("" + bx[0] + bx[1] + bx[2] + bx[3], 16);
		} else if (input.equalsIgnoreCase("bp")) {
			memoryIndex = Integer.parseInt("" + bp[0] + bp[1] + bp[2] + bp[3], 16);
		} else if (input.equalsIgnoreCase("si")) {
			memoryIndex = Integer.parseInt("" + si[0] + si[1] + si[2] + si[3], 16);
		} else if (input.equalsIgnoreCase("di")) {
			memoryIndex = Integer.parseInt("" + di[0] + di[1] + di[2] + di[3], 16);
		} else if (input.equalsIgnoreCase("sp")) {
			memoryIndex = Integer.parseInt(SP, 16);
		} else if (isRegOneByte(input) || isRegTwoByte(input)) {
			// System.out.println("#ERROR 39: Bad Index Register" + " at line: " + (MP / 6 +
			// 1));
			System.out.println("error");
			System.exit(0);
		} else {
			memoryIndex = Integer.parseInt(NumberToFourByteHexa(input, true), 16);
		}
		return memoryIndex;
	}

	/**
	 * a function that returns 4-digit-hexadecimal representation of parameter.
	 * 
	 * @param input as any type of representation
	 * @return returns 4-digit-hexadecimal number as a String
	 */
	public static String NumberToFourByteHexa(String s, boolean isMemoryIndex) {

		if (s.charAt(0) == 'a' || s.charAt(0) == 'b' || s.charAt(0) == 'c' || s.charAt(0) == 'd' || s.charAt(0) == 'e'
				|| s.charAt(0) == 'f') {// hexa numbers cant start with a letter
			// System.out.println("Undefined symbol:" + s + " at line: " + (MP / 6 + 1));
			System.out.println("error");
			System.exit(0);
		} else if (s.charAt(0) == '0') {// those which starts with 0 are hexa definitely
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
		if (isMemoryIndex && s.length() > 4) {
			// System.out.println("ERROR: Address is not valid. At line - " + (MP / 6 + 1));
			System.out.println("error");
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

	/**
	 * A helper method to determine token is an operator.
	 */
	private void fillInstructions() {
		instructionList.add("mov");
		instructionList.add("add");
		instructionList.add("sub");
		instructionList.add("mul");
		instructionList.add("div");
		instructionList.add("xor");
		instructionList.add("or");
		instructionList.add("inc");
		instructionList.add("dec");
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
		instructionList.add("jna");
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
				// System.out.println("#ERROR 13: Byte/Word Combination Not Allowed" + " at
				// line: " + (MP / 6 + 1));
				System.out.println("error");
				System.exit(0);

			} else { // 2 byte
				if (second.charAt(0) == 'w') {
					second = second.substring(1); // got rid of 'w'
				}

				second = second.substring(second.indexOf('[') + 1, second.length() - 1).trim(); // got rid of [ and ]

				if (isRegOneByte(second) || isRegTwoByte(second) || second.equalsIgnoreCase("sp")) {// register
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
						// System.out.println("#ERROR 39: Bad Index Register " + " at line: " + (MP / 6
						// + 1));
						System.out.println("error");
						System.exit(0);
					}
				} else {// number
					addend += NumberToFourByteHexa(second, true);
				}

			}
			// addend is a four length hexadecimal number which contains memory address
			if (Integer.parseInt(addend, 16) >= memory.length
					|| Integer.parseInt(addend, 16) < numberOfInstructions * 6) {
				// System.out.println("Address is not valid" + " at line: " + (MP / 6 + 1));
				System.out.println("error");
				System.exit(0);

			} else if (memory[Integer.parseInt(addend, 16)] == null) {
				addend = "0";
			} else {
				addend = memory[Integer.parseInt(addend, 16) + 1] + memory[Integer.parseInt(addend, 16)];
			} // addend is now the content of that address

		} else if (isRegOneByte(second) || isRegTwoByte(second) || second.equalsIgnoreCase("sp")) { // addend is
																									// register
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
			} else if (second.equalsIgnoreCase("sp")) {
				for (int i = 0; i <= 3; i++) {
					addend += SP.charAt(i);
				}
			} else {// error
				// System.out.println("#ERROR 13: Byte/Word Combination Not Allowed" + " at
				// line: " + (MP / 6 + 1));
				System.out.println("error");
				System.exit(0);
			}
		} else { // number or variable
			second = NumberToFourByteHexa(second, false); // number
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
		String addend = "";// until I get the memory address, this variable holds address
		if (second.contains("[") && second.contains("]")) {// if source is a memory address
			if (second.charAt(0) == 'w') { // 2 byte
				// System.out.println("#ERROR 13: Byte/Word Combination Not Allowed" + " at
				// line: " + (MP / 6 + 1));
				System.out.println("error");
				System.exit(0);

			} else { // 1 byte
				if (second.charAt(0) == 'b') {
					second = second.substring(1); // got rid of 'b'
				}
				second = second.substring(second.indexOf('[') + 1, second.length() - 1).trim(); // got rid of [ and ]
				if (isRegOneByte(second) || isRegTwoByte(second) || second.equalsIgnoreCase("sp")) {
					// second is register within square brackets
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
						// System.out.println("#ERROR 39: Bad Index Register " + " at line: " + (MP / 6
						// + 1));
						System.out.println("error");
						System.exit(0);
					}
				} else {// number
					addend = NumberToFourByteHexa(second, true); // number within square brackets
				}
			}
			// now addend is a four length hexadecimal number which contains memory address
			if (Integer.parseInt(addend, 16) >= memory.length
					|| Integer.parseInt(addend, 16) < numberOfInstructions * 6) {// to check address is valid
				// System.out.println("Address is not valid" + " at line: " + (MP / 6 + 1));
				System.out.println("error");
				System.exit(0);

			} else if (memory[Integer.parseInt(addend, 16)] == null) {// if that memory address was not initialized
																		// before
				addend = "0";
			} else { // if that memory address has a value inside
				addend = memory[Integer.parseInt(addend, 16)];
			}
			// addend is now the content of that address
			return addend.substring(addend.length() - 2, addend.length());// took last byte
		} else if (isRegOneByte(second) || isRegTwoByte(second) || second.equalsIgnoreCase("sp")) { // addend is any
																									// register
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
				// System.out.println("#ERROR 13: Byte/Word Combination Not Allowed" + " at
				// line: " + (MP / 6 + 1));
				System.out.println("error");
				System.exit(0);
			}
			return addend;
		} else { // source is number
			second = NumberToFourByteHexa(second, false);
			if (Integer.parseInt(second, 16) > 255) {// since destination is one byte
				// System.out.println("#ERROR 30: Byte-Sized Constant Required" + " at line: " +
				// (MP / 6 + 1));
				System.out.println("error");
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

	private String contentsOfOneByteRegister(String first) {
		String data;
		if (first.equalsIgnoreCase("al")) {
			data = "" + ax[2] + ax[3];

		} else if (first.equalsIgnoreCase("ah")) {
			data = "" + ax[0] + ax[1];

		} else if (first.equalsIgnoreCase("bl")) {
			data = "" + bx[2] + bx[3];

		} else if (first.equalsIgnoreCase("bh")) {
			data = "" + bx[0] + bx[1];

		} else if (first.equalsIgnoreCase("cl")) {
			data = "" + cx[2] + cx[3];

		} else if (first.equalsIgnoreCase("ch")) {
			data = "" + cx[0] + cx[1];

		} else if (first.equalsIgnoreCase("dl")) {
			data = "" + dx[2] + dx[3];

		} else {
			data = "" + dx[0] + dx[1];
		}
		return data;
	}

	private String contentsOfTwoByteRegister(String first) {
		String data;
		if (first.equalsIgnoreCase("ax")) {
			data = "" + ax[0] + ax[1] + ax[2] + ax[3];

		} else if (first.equalsIgnoreCase("bx")) {
			data = "" + bx[0] + bx[1] + bx[2] + bx[3];

		} else if (first.equalsIgnoreCase("cx")) {
			data = "" + cx[0] + cx[1] + cx[2] + cx[3];

		} else if (first.equalsIgnoreCase("dx")) {
			data = "" + dx[0] + dx[1] + dx[2] + dx[3];

		} else if (first.equalsIgnoreCase("di")) {
			data = "" + di[0] + di[1] + di[2] + di[3];

		} else if (first.equalsIgnoreCase("si")) {
			data = "" + si[0] + si[1] + si[2] + si[3];

		} else {
			data = "" + bp[0] + bp[1] + bp[2] + bp[3];
		}
		return data;
	}

}
