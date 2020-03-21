import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

// mov da bi �zelli�i implemente ederken �nce ax'e ediyorum.
// de�i�ik inputlarla testen sonra ba�ar�l� 
// olursa di�erlierine copy paste basit zaten
//**
//immeadite, register yap�ld� gibi
//register indirect ve memory yap�yorum
//stack addresssing tam ne bilmiyoum, ��reniyim bak�cam
// mov dl,"*" diye bi�i varm��, asciiden hexaya �evirip yaz�yo. yeni ��rendim bak�cam
//**
// mov da bi özelliði implemente ederken önce ax'e ediyorum.
// deðiþik inputlarla testen sonra baþarýlý 
// olursa diðerlierine copy paste basit zaten
//**
//immeadite, register yapýldý gibi
//register indirect ve memory yapýyorum
//stack addresssing tam ne bilmiyoum, öðreniyim bakýcam
// mov dl,"*" diye biþi varmýþ, asciiden hexaya çevirip yazýyo. yeni öðrendim bakýcam
//**

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

	// erişirken 2 ile çarp
	char[] di = new char[4];
	char[] sp = new char[4];
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

	int numberOfInstructions;
	String SP = "FFFE"; // stack pointer , memoryye erisirken hexadan decimale cevircez
	int MP = 0; // memory Pointer, instructionlarý okuduktan sonra 6*n' e kadar -1 falan yapýp
	// eriþilmez kýlmamýz lazým. (n=number of instructions)

	/**
	 * add instructions in an order to the memory until int 20h comes get rid of
	 * commas at instructions, finally have "mov ax bx" instead of "mov ax,bx"
	 * 
	 * @param fileAsString
	 */
	Hyp86(String fileAsString) {
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

				if (!label.equals("")) {
					labels.put(label, indexCursor);
					label = "";
				}
				if (line.indexOf(',') != -1) {
					int temo = line.indexOf(',');
					String temp = line.substring(0, temo) + " " + line.substring(temo + 1, line.length());
					line = temp;
				}

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

	public static void add(String first, String second) {

	}

	// !!!!!!!!!!!!! mov:
	// second offset içerebilir veya var içeribilir
	// When using variable names offset variable-name accesses the address,
	// just the variable-name accesses the value.
	// !!!!!!!!!!!!!

	// firstin memorydei adresine secondýn deðerini taþý
	// second pointer olabilir ex: [454]
	// second sayý olabilir ex: 454
	// second reg olabilir ex: cl
	// first reg veya memory
	// boyut uyumsuzluğunda hata ver

	public void mov_mem_xx(String first, String second) { //

	}

	public void mov_ax_unknown(String second) {
		boolean isVar = false;
		Variable var = null;
		Variable temp;
		Iterator<Variable> itr = variables.iterator();
		while (itr.hasNext()) {
			temp = itr.next();
			if (second.contains(temp.name)) {
				isVar = true;
				var = temp;
			}
		}
		if (second.contains("offset")) {
			String value = NumberToFourByteHexa("" + var.memoryIndex);
			for (int i = 0; i <= 3; i++) {
				ax[i] = '0';
			}
			for (int i = 0; i <= 3 && i < value.length(); i++) {
				ax[3 - i] = value.charAt(value.length() - i - 1);
			}
		}

		else if (second.contains("[") && second.contains("]")) {
			/*
			 * * Bad Index Register:
			 * 
			 * This is reported when you attempt to use a register other than SI, DI, BX, or
			 * BP for indexing. Those are the only registers that the 86 architecture allows
			 * you to place inside brackets, to address memory.
			 *
			 */

			if (second.charAt(0) == 'b') { // 1 byte
				System.out.println("Byte/Word Combination Not Allowed");
				System.exit(0);

			} else { // 2 byte
				if (second.charAt(0) == 'w') {
					second = second.substring(1); // got rid of 'w'
				}

				second = second.substring(1, second.length() - 1); // got rid of [ and ]
				String num = "";
				if ((second.contains("si") || second.contains("di") || second.contains("bx")
						|| second.contains("bp"))) {// register
					if (second.equals("si")) {
						for (int i = 0; i <= 3; i++) {
							num += si[i];
						}
					} else if (second.equals("di")) {
						for (int i = 0; i <= 3; i++) {
							num += di[i];
						}
					} else if (second.equals("bp")) {
						for (int i = 0; i <= 3; i++) {
							num += bp[i];
						}
					} else if (second.equals("bx")) {
						for (int i = 0; i <= 3; i++) {
							num += bx[i];
						}
					}
				} else if (isVar) {// variable
					num = NumberToFourByteHexa(var.data);
					for (int i = 0; i <= 3; i++) {
						ax[i] = '0';
					}
					for (int i = 0; i <= 3 && i < num.length(); i++) {
						ax[3 - i] = num.charAt(num.length() - i - 1);
					}
					return;
				} else {// number
					num = NumberToFourByteHexa(second);
				}

				// got the value, just insert to ax
				for (int i = 0; i <= 3; i++) {
					ax[i] = '0';
				}

				if (Integer.parseInt(num, 16) >= memory.length || Integer.parseInt(num, 16) < numberOfInstructions * 6
						|| memory[Integer.parseInt(num, 16)] == null) {// if memory is null then ax becomes 0 or not a
																		// valid
																		// address
					// ZF = true;
				} else {
					String memoryLocaitonOfNum = memory[Integer.parseInt(num, 16)];
					for (int i = 0; i <= 3 && i < num.length(); i++) {
						ax[3 - i] = memoryLocaitonOfNum.charAt(memoryLocaitonOfNum.length() - i - 1);
					}
				}
			}
		} else if (second.contains("bx") || second.contains("cx") || second.contains("dx") || second.contains("al")
				|| second.contains("ah") || second.contains("bl") || second.contains("bh") || second.contains("cl")
				|| second.contains("ch") || second.contains("dl") || second.contains("dh")) { // register

			if (second.equals("bx")) {
				for (int i = 3; i >= 0; i--) {
					ax[i] = bx[i];
				}
			} else if (second.equals("cx")) {
				for (int i = 3; i >= 0; i--) {
					ax[i] = cx[i];
				}
			} else if (second.equals("dx")) {
				for (int i = 3; i >= 0; i--) {
					ax[i] = dx[i];
				}
			} else {// error
				System.out.println("Error: Byte/Word Combination Not Allowed");
				System.exit(0);
			}
		} else { // number or variable
			if (isVar) {
				second = NumberToFourByteHexa(var.data); // variable
			} else {
				second = NumberToFourByteHexa(second); // number
			}
			for (int i = 0; i < 3; i++) {
				ax[i] = 0;
			}
			for (int i = 0; i <= 3 && i < second.length(); i++) {
				ax[3 - i] = second.charAt(second.length() - i - 1);
			}
		}

	}

	public void mov_bx_unknown(String second) {
		if (second.contains("[") && second.contains("]")) { // second is memory

		} else if (second.contains("ax")) {
			for (int i = 3; i >= 0; i--) {
				bx[i] = ax[i];
			}
		} else if (second.contains("cx")) {
			for (int i = 3; i >= 0; i--) {
				bx[i] = cx[i];
			}
		} else if (second.contains("dx")) {
			for (int i = 3; i >= 0; i--) {
				bx[i] = dx[i];
			}
		} else if (second.contains("al") || second.contains("ah") || second.contains("bl") || second.contains("bh")
				|| second.contains("cl") || second.contains("ch") || second.contains("dl") || second.contains("dh")) {// error
			System.out.println("Error: Byte/Word Combination Not Allowed");
			System.exit(0);
		} else {// number

		}

	}

	public void mov_cx_unknown(String second) {
		if (second.contains("[") && second.contains("]")) { // second is memory

		} else if (second.contains("ax")) {
			for (int i = 3; i >= 0; i--) {
				cx[i] = ax[i];
			}
		} else if (second.contains("bx")) {
			for (int i = 3; i >= 0; i--) {
				cx[i] = bx[i];
			}
		} else if (second.contains("dx")) {
			for (int i = 3; i >= 0; i--) {
				cx[i] = dx[i];
			}
		} else if (second.contains("al") || second.contains("ah") || second.contains("bl") || second.contains("bh")
				|| second.contains("cl") || second.contains("ch") || second.contains("dl") || second.contains("dh")) {// error
			System.out.println("Error: Byte/Word Combination Not Allowed");
			System.exit(0);
		} else {// number

		}

	}

	public void mov_dx_unknown(String second) {
		if (second.contains("[") && second.contains("]")) { // second is memory

		} else if (second.contains("ax")) {
			for (int i = 3; i >= 0; i--) {
				dx[i] = ax[i];
			}
		} else if (second.contains("bx")) {
			for (int i = 3; i >= 0; i--) {
				dx[i] = bx[i];
			}
		} else if (second.contains("cx")) {
			for (int i = 3; i >= 0; i--) {
				dx[i] = cx[i];
			}
		} else if (second.contains("al") || second.contains("ah") || second.contains("bl") || second.contains("bh")
				|| second.contains("cl") || second.contains("ch") || second.contains("dl") || second.contains("dh")) {// error
			System.out.println("Error: Byte/Word Combination Not Allowed");
			System.exit(0);
		} else {// number
		}
	}

	public void mov_al_unknown(String second) {
		if (second.equals("ah")) {
			for (int i = 3; i >= 2; i--) {
				ax[i] = ax[i - 2];
			}
		} else if (second.equals("bl")) {
			for (int i = 3; i >= 2; i--) {
				ax[i] = bx[i];
			}
		} else if (second.equals("bh")) {
			for (int i = 3; i >= 2; i--) {
				ax[i] = bx[i - 2];
			}
		} else if (second.equals("cl")) {
			for (int i = 3; i >= 2; i--) {
				ax[i] = cx[i];
			}
		} else if (second.equals("ch")) {
			for (int i = 3; i >= 2; i--) {
				ax[i] = cx[i - 2];
			}
		} else if (second.equals("dl")) {
			for (int i = 3; i >= 2; i--) {
				ax[i] = dx[i];
			}
		} else if (second.equals("dh")) {
			for (int i = 3; i >= 2; i--) {
				ax[i] = dx[i - 2];
			}
		} else if (second.contains("x")) {
			System.out.println("Byte/Word Combination Not Allowed");
			System.exit(0);
		} else { // þu diger regler olabýlýr sonra yapýcam

		}
	}

	public void mov_ah_unknown(String second) {
		if (second.equals("al")) {
			for (int i = 3; i >= 2; i--) {
				ax[i - 2] = ax[i];
			}
		} else if (second.equals("bl")) {
			for (int i = 3; i >= 2; i--) {
				ax[i - 2] = bx[i];
			}
		} else if (second.equals("bh")) {
			for (int i = 3; i >= 2; i--) {
				ax[i - 2] = bx[i - 2];
			}
		} else if (second.equals("cl")) {
			for (int i = 3; i >= 2; i--) {
				ax[i - 2] = cx[i];
			}
		} else if (second.equals("ch")) {
			for (int i = 3; i >= 2; i--) {
				ax[i - 2] = cx[i - 2];
			}
		} else if (second.equals("dl")) {
			for (int i = 3; i >= 2; i--) {
				ax[i - 2] = dx[i];
			}
		} else if (second.equals("dh")) {
			for (int i = 3; i >= 2; i--) {
				ax[i - 2] = dx[i - 2];
			}
		} else if (second.contains("x")) {
			System.out.println("Byte/Word Combination Not Allowed");
			System.exit(0);
		} else { // þu diger regler olabýlýr sonra yapýcam

		}
	}

	public void mov_bl_unknown(String second) {
		if (second.equals("al")) {
			for (int i = 3; i >= 2; i--) {
				ax[i - 2] = ax[i];
			}
		} else if (second.equals("bl")) {
			for (int i = 3; i >= 2; i--) {
				ax[i - 2] = bx[i];
			}
		} else if (second.equals("bh")) {
			for (int i = 3; i >= 2; i--) {
				ax[i - 2] = bx[i - 2];
			}
		} else if (second.equals("cl")) {
			for (int i = 3; i >= 2; i--) {
				ax[i - 2] = cx[i];
			}
		} else if (second.equals("ch")) {
			for (int i = 3; i >= 2; i--) {
				ax[i - 2] = cx[i - 2];
			}
		} else if (second.equals("dl")) {
			for (int i = 3; i >= 2; i--) {
				ax[i - 2] = dx[i];
			}
		} else if (second.equals("dh")) {
			for (int i = 3; i >= 2; i--) {
				ax[i - 2] = dx[i - 2];
			}
		} else if (second.contains("x")) {
			System.out.println("Byte/Word Combination Not Allowed");
			System.exit(0);
		} else { // þu diger regler olabýlýr sonra yapýcam
		}
	}

	public void mov_bh_unknown(String second) {

	}

	public void mov_cl_unknown(String second) {
		if (second.equals("al")) {
			for (int i = 3; i >= 2; i--) {
				cx[i] = ax[i];
			}
		} else if (second.equals("ah")) {
			for (int i = 3; i >= 2; i--) {
				cx[i] = ax[i - 2];
			}
		} else if (second.equals("bl")) {
			for (int i = 3; i >= 2; i--) {
				cx[i] = bx[i];
			}
		} else if (second.equals("bh")) {
			for (int i = 3; i >= 2; i--) {
				cx[i] = bx[i - 2];
			}
		} else if (second.equals("ch")) {
			for (int i = 3; i >= 2; i--) {
				cx[i] = cx[i - 2];
			}
		} else if (second.equals("dl")) {
			for (int i = 3; i >= 2; i--) {
				cx[i] = dx[i];
			}
		} else if (second.equals("dh")) {
			for (int i = 3; i >= 2; i--) {
				cx[i] = dx[i - 2];
			}
		} else if (second.contains("x")) {
			System.out.println("Byte/Word Combination Not Allowed");
			System.exit(0);
		} else { // þu diger regler olabýlýr sonra yapýcam

		}
	}

	public void mov_ch_unknown(String second) {
		if (second.equals("al")) {
			for (int i = 3; i >= 2; i--) {
				cx[i - 2] = ax[i];
			}
		} else if (second.equals("ah")) {
			for (int i = 3; i >= 2; i--) {
				cx[i - 2] = ax[i - 2];
			}
		} else if (second.equals("bl")) {
			for (int i = 3; i >= 2; i--) {
				cx[i - 2] = bx[i];
			}
		} else if (second.equals("bh")) {
			for (int i = 3; i >= 2; i--) {
				cx[i - 2] = bx[i - 2];
			}
		} else if (second.equals("cl")) {
			for (int i = 3; i >= 2; i--) {
				cx[i - 2] = cx[i];
			}
		} else if (second.equals("dl")) {
			for (int i = 3; i >= 2; i--) {
				cx[i - 2] = dx[i];
			}
		} else if (second.equals("dh")) {
			for (int i = 3; i >= 2; i--) {
				cx[i - 2] = dx[i - 2];
			}
		} else if (second.contains("x")) {
			System.out.println("Byte/Word Combination Not Allowed");
			System.exit(0);
		} else { // þu diger regler olabýlýr sonra yapýcam
		}
	}

	public void mov_dl_unknown(String second) {
		if (second.equals("al")) {
			for (int i = 3; i >= 2; i--) {
				dx[i] = ax[i];
			}
		} else if (second.equals("ah")) {
			for (int i = 3; i >= 2; i--) {
				dx[i] = ax[i - 2];
			}
		} else if (second.equals("bl")) {
			for (int i = 3; i >= 2; i--) {
				dx[i] = bx[i];
			}
		} else if (second.equals("bh")) {
			for (int i = 3; i >= 2; i--) {
				dx[i] = bx[i - 2];
			}
		} else if (second.equals("cl")) {
			for (int i = 3; i >= 2; i--) {
				dx[i] = cx[i];
			}
		} else if (second.equals("ch")) {
			for (int i = 3; i >= 2; i--) {
				dx[i] = cx[i - 2];
			}
		} else if (second.equals("dh")) {
			for (int i = 3; i >= 2; i--) {
				dx[i] = dx[i - 2];
			}
		} else if (second.contains("x")) {
			System.out.println("Byte/Word Combination Not Allowed");
			System.exit(0);
		} else { // þu diger regler olabýlýr sonra yapýcam

		}
	}

	public void mov_dh_unknown(String second) {
		if (second.equals("al")) {
			for (int i = 3; i >= 2; i--) {
				dx[i - 2] = ax[i];
			}
		} else if (second.equals("ah")) {
			for (int i = 3; i >= 2; i--) {
				dx[i - 2] = ax[i - 2];
			}
		} else if (second.equals("bl")) {
			for (int i = 3; i >= 2; i--) {
				dx[i - 2] = bx[i];
			}
		} else if (second.equals("bh")) {
			for (int i = 3; i >= 2; i--) {
				dx[i - 2] = bx[i - 2];
			}
		} else if (second.equals("cl")) {
			for (int i = 3; i >= 2; i--) {
				dx[i - 2] = cx[i];
			}
		} else if (second.equals("ch")) {
			for (int i = 3; i >= 2; i--) {
				dx[i - 2] = cx[i - 2];
			}
		} else if (second.equals("dl")) {
			for (int i = 3; i >= 2; i--) {
				dx[i - 2] = dx[i];
			}
		} else if (second.contains("x")) {
			System.out.println("Byte/Word Combination Not Allowed");
			System.exit(0);
		} else { // þu diger regler olabýlýr sonra yapýcam
		}
	}

	public void mov_reg_unknown(String first, String second) {

		if (first.equalsIgnoreCase("ax")) { // mov *x,*l ya da *x, *h hatasý yapýldý
			mov_ax_unknown(second);
		} else if (first.equalsIgnoreCase("bx")) {
			mov_bx_unknown(second);
		} else if (first.equalsIgnoreCase("cx")) {
			mov_cx_unknown(second);
		} else if (first.equalsIgnoreCase("dx")) {
			mov_dx_unknown(second);
		} else if (first.equalsIgnoreCase("al")) {
			mov_al_unknown(second);
		} else if (first.equalsIgnoreCase("ah")) {
			mov_ah_unknown(second);
		} else if (first.equalsIgnoreCase("bl")) {
			mov_bl_unknown(second);
		} else if (first.equalsIgnoreCase("bh")) {
			mov_bh_unknown(second);
		} else if (first.equalsIgnoreCase("cl")) {
			mov_cl_unknown(second);
		} else if (first.equalsIgnoreCase("ch")) {
			mov_ch_unknown(second);
		} else if (first.equalsIgnoreCase("dl")) {
			mov_dl_unknown(second);
		} else if (first.equalsIgnoreCase("dh")) {
			mov_dh_unknown(second);
		}
	}

	public void mov(String first, String second) {

		if (first.contains("[") && first.contains("]")) { // first is memory

			mov_mem_xx(first, second);

		} else if (first.equals("ax") || first.equals("cx") || first.equals("bx") || first.equals("dx")
				|| first.equals("ah") || first.equals("al") || first.equals("bl") || first.equals("bh")
				|| first.equals("ch") || first.equals("cl") || first.equals("dl") || first.equals("dh")) { // first is
			// reg
			mov_reg_unknown(first, second);

		} else { // reg veya memoryye yazmýyo hata ver
			System.out.println("Undefined sysbols are listed");
			System.exit(0);
		}

	}

	public int hexaToDecimal(String s) {
		return Integer.parseInt(s, 16);
	}

	public String DecimalToHexa(int a) {
		return Integer.toHexString(a);
	}

	public int binaryToDecimal(int a) {
		return Integer.parseInt("+a+", 2);
	}

	public static int DecimalToBinary(int a) {
		return Integer.valueOf(Integer.toBinaryString(a).substring(0, Integer.toBinaryString(a).length()));
	}

	public String NumberToFourByteHexa(String s) {// 2abd hexa mesela
		if (s.charAt(0) == '0') {// hexa

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

}
