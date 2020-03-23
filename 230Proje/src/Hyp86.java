import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

// mov da bi özelliği implemente ederken önce ax'e ediyorum.
// değişik inputlarla testen sonra başarılı 
// olursa diğerlierine copy paste basit zaten
//**
//immeadite, register yapıldı gibi
//register indirect ve memory yapıyorum
//stack addresssing tam ne bilmiyoum, öğreniyim bakıcam
// mov dl,"*" diye bişi varmış, asciiden hexaya çevirip yazıyo. yeni öğrendim bakıcam
//**
// mov da bi Ã¶zelliÃ°i implemente ederken Ã¶nce ax'e ediyorum.
// deÃ°iÃ¾ik inputlarla testen sonra baÃ¾arÃ½lÃ½ 
// olursa diÃ°erlierine copy paste basit zaten
//**
//immeadite, register yapÃ½ldÃ½ gibi
//register indirect ve memory yapÃ½yorum
//stack addresssing tam ne bilmiyoum, Ã¶Ã°reniyim bakÃ½cam
// mov dl,"*" diye biÃ¾i varmÃ½Ã¾, asciiden hexaya Ã§evirip yazÃ½yo. yeni Ã¶Ã°rendim bakÃ½cam
//**
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;



public class Hyp86 {
	
	/*labels, <label, index of where the label's instruction starts>
	 *instructionList, to see is the given label a kind of instruction
	 *variables, array of variables defined 
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

	int numberOfInstructions = 0;
	private int instrustionAreaEnd = -1;
	String SP = "FFFE"; // stack pointer , memoryye erisirken hexadan decimale cevircez
	int MP = 0; // memory Pointer, instructionlarokuduktan sonra 6*n' e kadar -1 falan yap

	
	
	
	
	/**
	 * add instructions in an order to the memory until int 20h comes
	 * get rid of commas at instructions,
	 * finally have "mov ax bx" instead of "mov ax,bx"
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
		while(scanner.hasNextLine()) {
			line = scanner.nextLine();
			token = new Scanner(line);
			String first = token.next();
			
			if(!int20hCame && instructionList.contains(first)) {//means instruction
				numberOfInstructions++; // veyis add
				if(!label.equals("")) {
					labels.put(label, indexCursor);
					label = "";
				}
				if(line.indexOf(',') != -1) {
					int temo = line.indexOf(',');
					String temp = line.substring(0, temo) + " " + line.substring(temo +1, line.length());
					line = temp;
				}
				
				if(first.equals("int") && token.next().equals("20h")) {
					instrustionAreaEnd = indexCursor + 5 ;// new addition
					int20hCame = true;
				}
				memory[indexCursor] = line;
				indexCursor +=6;
				
			}
			if(line.indexOf(":") != -1) {// means label
				label = line.trim().substring(0,line.length()-1);				  
				continue;
			}
			
			if(line.indexOf("dw") != -1 ||line.indexOf("db") != -1 ) {// variable definition
			
				if(first.equals("dw")) {
					variables.add(new Variable(label, 0, token.next(), true));
				}else if(first.equals("db")) {
					variables.add(new Variable(label, 0, token.next(), false));
				}else {
					if(token.next().equals("dw")) {
						variables.add(new Variable(first, 0, token.next(), true));
					}else{
						variables.add(new Variable(first, 0, token.next(), false));
					}
				}
				
				
			}
			
			token.close();
			label = "";
			
			
			
		}
		Variable x;
		for(int i = 0; i< variables.size() ; i++) {
			x = variables.get(i);
			if(x.type == true) {
				memory[indexCursor] = x.name;
				x.memoryIndex = indexCursor;
				indexCursor +=2;	
			}else {
				memory[indexCursor] = x.name;
				x.memoryIndex = indexCursor;
				indexCursor +=1;
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
		
		if(reg.equals("ax")) {
			memory[index] = ""+ ax[0] + ax[1] + ax[2] + ax[3];

			
		}else if(reg.equals("bx")) {
			memory[index] = ""+ bx[0] + bx[1] + bx[2] + bx[3];
		
		}else if(reg.equals("cx")) {
			memory[index] = ""+ cx[0] + cx[1] + cx[2] + cx[3];
			
		}else if(reg.equals("dx")) {
			memory[index] = ""+ dx[0] + dx[1] + dx[2] + dx[3];
			
		}else if(reg.equals("di")) {
			memory[index] = ""+ di[0] + di[1] + di[2] + di[3];
			
		}else if(reg.equals("si")) {
			memory[index] = ""+ si[0] + si[1] + si[2] + si[3];
			
		}else  if(reg.equals("bp")) {
			memory[index] = ""+ bp[0] + bp[1] + bp[2] + bp[3];
			
		}else{

			int a = 0;
			for(int i = 0 ; i < variables.size() ; i++) {
			if(variables.get(i).getName().equals(reg) && variables.get(i).isType()) {
				String data = NumberToFourByteHexa(""+variables.get(i).resultData);
				memory[index] = data;
				break;
			}
			
			a++;
			}
			if(a== variables.size()) {
				if(reg.indexOf('[') != -1) {
					reg = reg.substring(1,reg.length()-1);
					
					if(instrustionAreaEnd < Integer.parseInt(NumberToFourByteHexa(reg),16)) {
						memory[index] = memory[Integer.parseInt(NumberToFourByteHexa(reg),16)];
					}else {
						System.out.println("don't try to reach instruction area");
						return;
					}
					
				}else {
				
					memory[index] = NumberToFourByteHexa(reg);
				}
			}
			
					
		}
		
		SP = NumberToFourByteHexa("" + (index-2)); 
	}
	
	/**
	 * pop allows popping to a variable, register or memory address
	 * register and variable cases are perfect
	 * @param reg
	 */
	
	public void pop(String reg) {
		int index = Integer.parseInt(SP,16) + 2;
		if(index >= 1024*64) {
			System.out.println("no element to pop");
			return;
		}
		if(reg.equals("ax")) {
			ax[0] = memory[index].charAt(0);
			ax[1] = memory[index].charAt(1);
			ax[2] = memory[index].charAt(2);
			ax[3] = memory[index].charAt(3);

			
		}else if(reg.equals("bx")) {
			bx[0] = memory[index].charAt(0);
			bx[1] = memory[index].charAt(1);
			bx[2] = memory[index].charAt(2);
			bx[3] = memory[index].charAt(3);
		
		}else if(reg.equals("cx")) {
			cx[0] = memory[index].charAt(0);
			cx[1] = memory[index].charAt(1);
			cx[2] = memory[index].charAt(2);
			cx[3] = memory[index].charAt(3);
			
		}else if(reg.equals("dx")) {
			dx[0] = memory[index].charAt(0);
			dx[1] = memory[index].charAt(1);
			dx[2] = memory[index].charAt(2);
			dx[3] = memory[index].charAt(3);
			
		}else if(reg.equals("di")) {
			di[0] = memory[index].charAt(0);
			di[1] = memory[index].charAt(1);
			di[2] = memory[index].charAt(2);
			di[3] = memory[index].charAt(3);
			
		}else if(reg.equals("si")) {
			si[0] = memory[index].charAt(0);
			si[1] = memory[index].charAt(1);
			si[2] = memory[index].charAt(2);
			si[3] = memory[index].charAt(3);
			
		}else  if(reg.equals("bp")) {
			bp[0] = memory[index].charAt(0);
			bp[1] = memory[index].charAt(1);
			bp[2] = memory[index].charAt(2);
			bp[3] = memory[index].charAt(3);
			
		}else {
			int a = 0;
			for(int i = 0 ; i < variables.size() ; i++) {
			if(variables.get(i).getName().equals(reg) && variables.get(i).isType()) {
				variables.get(i).resultData = Integer.parseInt(memory[index],16);
				break;
			}
			
			a++;
			}
			
		if(a == variables.size() && reg.indexOf('[') != -1) {
				reg = reg.substring(1,reg.length()-1);
				if(instrustionAreaEnd < Integer.parseInt(NumberToFourByteHexa(reg),16)) {
					memory[Integer.parseInt(NumberToFourByteHexa(reg),16)] = memory[index] ;		
				}else {
					System.out.println("don't try to reach instruction area");
					return;
				}
				
		}	
			
			
		
		}
		
		SP = NumberToFourByteHexa(""+index);
		memory[index] = null;
		
		
		
	}
	

	public static void add(String first, String second) {

	}

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
				if (second.contains("ax") || second.contains("bx") || second.contains("cx") || second.contains("dx")
						|| second.contains("al") || second.contains("bl") || second.contains("bh")
						|| second.contains("cl") || second.contains("ch") || second.contains("dl")
						|| second.contains("dh") || second.equals("di") || second.equals("si") || second.equals("bp")) {// register
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
							ax[i] = '0';
						}
						for (int i = 0; i <= 3 && i < num.length(); i++) {
							ax[3 - i] = num.charAt(num.length() - i - 1);
						}
						return;
					}
				} else {// number
					num = NumberToFourByteHexa(second);
				}

				// got the value, just insert to ax
				for (int i = 0; i <= 3; i++) {
					ax[i] = '0';
				}

				if (Integer.parseInt(num, 16) >= memory.length
						|| Integer.parseInt(num, 16) < numberOfInstructions * 6) {
					System.out.println("Address is not valid");
					System.exit(0);

				} else if (memory[Integer.parseInt(num, 16)] == null) {
				} else {
					String memoryLocaitonOfNum = memory[Integer.parseInt(num, 16)];
					for (int i = 0; i <= 3 && i < num.length(); i++) {
						ax[3 - i] = memoryLocaitonOfNum.charAt(memoryLocaitonOfNum.length() - i - 1);
					}
				}
			}
		} else if (second.contains("bx") || second.contains("cx") || second.contains("dx") || second.contains("al")
				|| second.contains("ah") || second.contains("bl") || second.contains("bh") || second.contains("cl")
				|| second.contains("ch") || second.contains("dl") || second.contains("dh") || second.equals("di")
				|| second.equals("si") || second.equals("bp")) { // register

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
			} else if (second.equals("si")) {
				for (int i = 3; i >= 0; i--) {
					ax[i] = si[i];
				}
			} else if (second.equals("bp")) {
				for (int i = 3; i >= 0; i--) {
					ax[i] = bp[i];
				}
			} else if (second.equals("di")) {
				for (int i = 3; i >= 0; i--) {
					ax[i] = di[i];
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
			for (int i = 0; i < 3; i++) {
				ax[i] = 0;
			}
			for (int i = 0; i <= 3 && i < second.length(); i++) {
				ax[3 - i] = second.charAt(second.length() - i - 1);
			}
		}
	}

	public void mov_bx_unknown(String second) {
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
				bx[i] = '0';
			}
			for (int i = 0; i <= 3 && i < value.length(); i++) {
				bx[3 - i] = value.charAt(value.length() - i - 1);
			}
		} else if (second.contains("[") && second.contains("]")) { // second is memory
			if (second.charAt(0) == 'b') { // 1 byte
				System.out.println("#ERROR 13: Byte/Word Combination Not Allowed");
				System.exit(0);

			} else { // 2 byte
				if (second.charAt(0) == 'w') {
					second = second.substring(1); // got rid of 'w'
				}

				second = second.substring(1, second.length() - 1); // got rid of [ and ]
				String num = "";
				if ((second.contains("si") || second.contains("di") || second.contains("bp"))) {// register
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

					}
				} else if (isVar) {// variable
					if (!var.type) {
						System.out.println("#ERROR 13: Byte/Word Combination Not Allowed");
						System.exit(0);
					}
					num = NumberToFourByteHexa(var.data);
					for (int i = 0; i <= 3; i++) {
						bx[i] = '0';
					}
					for (int i = 0; i <= 3 && i < num.length(); i++) {
						bx[3 - i] = num.charAt(num.length() - i - 1);
					}
					return;
				} else {// number
					num = NumberToFourByteHexa(second);
				}

				// got the value, just insert to bx
				for (int i = 0; i <= 3; i++) {
					bx[i] = '0';
				}

				if (Integer.parseInt(num, 16) >= memory.length
						|| Integer.parseInt(num, 16) < numberOfInstructions * 6) {
					System.out.println("Address is not valid");
					System.exit(0);

				} else if (memory[Integer.parseInt(num, 16)] == null) {
				} else {
					String memoryLocaitonOfNum = memory[Integer.parseInt(num, 16)];
					for (int i = 0; i <= 3 && i < num.length(); i++) {
						bx[3 - i] = memoryLocaitonOfNum.charAt(memoryLocaitonOfNum.length() - i - 1);
					}
				}
			}
		} else if (second.contains("ax") || second.contains("cx") || second.contains("dx") || second.contains("al")
				|| second.contains("ah") || second.contains("bl") || second.contains("bh") || second.contains("cl")
				|| second.contains("ch") || second.contains("dl") || second.contains("dh") || second.equals("di")
				|| second.equals("si") || second.equals("bp")) { // register

			if (second.equals("ax")) {
				for (int i = 3; i >= 0; i--) {
					bx[i] = ax[i];
				}
			} else if (second.equals("cx")) {
				for (int i = 3; i >= 0; i--) {
					bx[i] = cx[i];
				}
			} else if (second.equals("dx")) {
				for (int i = 3; i >= 0; i--) {
					bx[i] = dx[i];
				}
			} else {// error
				System.out.println("#ERROR 13: Byte/Word Combination Not Allowed");
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
			for (int i = 0; i < 3; i++) {
				bx[i] = 0;
			}
			for (int i = 0; i <= 3 && i < second.length(); i++) {
				bx[3 - i] = second.charAt(second.length() - i - 1);
			}
		}
	}

	public void mov_cx_unknown(String second) {
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
				cx[i] = '0';
			}
			for (int i = 0; i <= 3 && i < value.length(); i++) {
				cx[3 - i] = value.charAt(value.length() - i - 1);
			}
		}

		else if (second.contains("[") && second.contains("]")) {

			if (second.charAt(0) == 'b') { // 1 byte
				System.out.println("#ERROR 13: Byte/Word Combination Not Allowed");
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
					if (!var.type) {
						System.out.println("#ERROR 13: Byte/Word Combination Not Allowed");
						System.exit(0);
					}
					num = NumberToFourByteHexa(var.data);
					for (int i = 0; i <= 3; i++) {
						cx[i] = '0';
					}
					for (int i = 0; i <= 3 && i < num.length(); i++) {
						cx[3 - i] = num.charAt(num.length() - i - 1);
					}
					return;
				} else {// number
					num = NumberToFourByteHexa(second);
				}

				// got the value, just insert to cx
				for (int i = 0; i <= 3; i++) {
					cx[i] = '0';
				}

				if (Integer.parseInt(num, 16) >= memory.length
						|| Integer.parseInt(num, 16) < numberOfInstructions * 6) {
					System.out.println("Address is not valid");
					System.exit(0);

				} else if (memory[Integer.parseInt(num, 16)] == null) {
				} else {
					String memoryLocaitonOfNum = memory[Integer.parseInt(num, 16)];
					for (int i = 0; i <= 3 && i < num.length(); i++) {
						cx[3 - i] = memoryLocaitonOfNum.charAt(memoryLocaitonOfNum.length() - i - 1);
					}
				}
			}
		} else if (second.contains("bx") || second.contains("ax") || second.contains("dx") || second.contains("al")
				|| second.contains("ah") || second.contains("bl") || second.contains("bh") || second.contains("cl")
				|| second.contains("ch") || second.contains("dl") || second.contains("dh") || second.equals("di")
				|| second.equals("si") || second.equals("bp")) { // register

			if (second.equals("ax")) {
				for (int i = 3; i >= 0; i--) {
					cx[i] = ax[i];
				}
			} else if (second.equals("bx")) {
				for (int i = 3; i >= 0; i--) {
					cx[i] = bx[i];
				}
			} else if (second.equals("dx")) {
				for (int i = 3; i >= 0; i--) {
					cx[i] = dx[i];
				}
			} else if (second.equals("si")) {
				for (int i = 3; i >= 0; i--) {
					cx[i] = si[i];
				}
			} else if (second.equals("bp")) {
				for (int i = 3; i >= 0; i--) {
					cx[i] = bp[i];
				}
			} else if (second.equals("di")) {
				for (int i = 3; i >= 0; i--) {
					cx[i] = di[i];
				}
			} else {// error
				System.out.println("#ERROR 13: Byte/Word Combination Not Allowed");
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
			for (int i = 0; i < 3; i++) {
				cx[i] = 0;
			}
			for (int i = 0; i <= 3 && i < second.length(); i++) {
				cx[3 - i] = second.charAt(second.length() - i - 1);
			}
		}

	}

	public void mov_dx_unknown(String second) {
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
				dx[i] = '0';
			}
			for (int i = 0; i <= 3 && i < value.length(); i++) {
				dx[3 - i] = value.charAt(value.length() - i - 1);
			}
		}

		else if (second.contains("[") && second.contains("]")) {

			if (second.charAt(0) == 'b') { // 1 byte
				System.out.println("#ERROR 13: Byte/Word Combination Not Allowed");
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
					if (!var.type) {
						System.out.println("#ERROR 13: Byte/Word Combination Not Allowed");
						System.exit(0);
					}
					num = NumberToFourByteHexa(var.data);
					for (int i = 0; i <= 3; i++) {
						dx[i] = '0';
					}
					for (int i = 0; i <= 3 && i < num.length(); i++) {
						dx[3 - i] = num.charAt(num.length() - i - 1);
					}
					return;
				} else {// number
					num = NumberToFourByteHexa(second);
				}

				// got the value, just insert to dx
				for (int i = 0; i <= 3; i++) {
					dx[i] = '0';
				}

				if (Integer.parseInt(num, 16) >= memory.length
						|| Integer.parseInt(num, 16) < numberOfInstructions * 6) {
					System.out.println("Address is not valid");
					System.exit(0);

				} else if (memory[Integer.parseInt(num, 16)] == null) {
				} else {
					String memoryLocaitonOfNum = memory[Integer.parseInt(num, 16)];
					for (int i = 0; i <= 3 && i < num.length(); i++) {
						dx[3 - i] = memoryLocaitonOfNum.charAt(memoryLocaitonOfNum.length() - i - 1);
					}
				}
			}
		} else if (second.contains("bx") || second.contains("cx") || second.contains("ax") || second.contains("al")
				|| second.contains("ah") || second.contains("bl") || second.contains("bh") || second.contains("cl")
				|| second.contains("ch") || second.contains("dl") || second.contains("dh") || second.equals("di")
				|| second.equals("si") || second.equals("bp")) { // register

			if (second.equals("ax")) {
				for (int i = 3; i >= 0; i--) {
					dx[i] = ax[i];
				}
			} else if (second.equals("bx")) {
				for (int i = 3; i >= 0; i--) {
					dx[i] = bx[i];
				}
			} else if (second.equals("cx")) {
				for (int i = 3; i >= 0; i--) {
					dx[i] = cx[i];
				}
			} else if (second.equals("si")) {
				for (int i = 3; i >= 0; i--) {
					dx[i] = si[i];
				}
			} else if (second.equals("bp")) {
				for (int i = 3; i >= 0; i--) {
					dx[i] = bp[i];
				}
			} else if (second.equals("di")) {
				for (int i = 3; i >= 0; i--) {
					dx[i] = di[i];
				}
			} else {// error
				System.out.println("#ERROR 13: Byte/Word Combination Not Allowed");
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
			for (int i = 0; i < 3; i++) {
				dx[i] = 0;
			}
			for (int i = 0; i <= 3 && i < second.length(); i++) {
				dx[3 - i] = second.charAt(second.length() - i - 1);
			}
		}
	}

	public void mov_al_unknown(String second) {
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
			for (int i = 0; i <= 1; i++) {
				ax[2 + i] = '0';
			}
			for (int i = 0; i <= 1 && i < value.length(); i++) {
				ax[3 - i] = value.charAt(value.length() - i - 1);
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

				if (second.contains("bx") || second.contains("cx") || second.contains("dx") || second.contains("al")
						|| second.contains("ah") || second.contains("bl") || second.contains("bh")
						|| second.contains("cl") || second.contains("ch") || second.contains("dl")
						|| second.contains("dh") || second.equals("di") || second.equals("si") || second.equals("bp")) {// register
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
							ax[2 + i] = '0';
						}
						for (int i = 0; i <= 1 && i < num.length(); i++) {
							ax[3 - i] = num.charAt(num.length() - i - 1);
						}
						return;
					}
				} else {// number
					num = NumberToFourByteHexa(second);
				}

				ax[0] = '0';
				ax[1] = '0';
				if (memory[Integer.parseInt(num, 16)] == null) {

				} else if (Integer.parseInt(num, 16) < numberOfInstructions * 6
						|| Integer.parseInt(num, 16) >= 64 * 1024) {
					System.out.println("Address is not valid");
					System.exit(0);
				} else {
					num = memory[Integer.parseInt(num, 16)];
					for (int i = 0; i <= 1 && i < num.length(); i++) {
						ax[3 - i] = num.charAt(num.length() - i - 1);
					}
				}

			}

		} else if (second.contains("ax") || second.contains("bx") || second.contains("cx") || second.contains("dx")
				|| second.contains("al") || second.contains("bl") || second.contains("bh") || second.contains("cl")
				|| second.contains("ch") || second.contains("dl") || second.contains("dh") || second.equals("di")
				|| second.equals("si") || second.equals("bp")) { // register
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
				second = NumberToFourByteHexa(var.data); // variable
			} else {
				second = NumberToFourByteHexa(second); // number
			}
			if (Integer.parseInt(second, 16) > 255) {
				System.out.println("#ERROR 30: Byte-Sized Constant Required");
				System.exit(0);
			}
			for (int i = 0; i < 1; i++) {
				ax[2 + i] = 0;
			}
			for (int i = 0; i <= 1 && i < second.length(); i++) {
				ax[3 - i] = second.charAt(second.length() - i - 1);
			}
		}
	}

	public void mov_ah_unknown(String second) {
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
			for (int i = 0; i <= 1; i++) {
				ax[i] = '0';
			}
			for (int i = 0; i <= 1 && i < value.length(); i++) {
				ax[1 - i] = value.charAt(value.length() - i - 1);
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

				if (second.contains("bx") || second.contains("cx") || second.contains("dx") || second.contains("al")
						|| second.contains("ah") || second.contains("bl") || second.contains("bh")
						|| second.contains("cl") || second.contains("ch") || second.contains("dl")
						|| second.contains("dh") || second.equals("di") || second.equals("si") || second.equals("bp")) {// register
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
							ax[i] = '0';
						}
						for (int i = 2; i <= 3 && i < num.length(); i++) {
							ax[3 - i] = num.charAt(num.length() - i - 1);
						}
						return;
					}
				} else {// number
					num = NumberToFourByteHexa(second);
				}

				ax[0] = '0';
				ax[1] = '0';
				if (memory[Integer.parseInt(num, 16)] == null) {

				} else if (Integer.parseInt(num, 16) < numberOfInstructions * 6
						|| Integer.parseInt(num, 16) >= 64 * 1024) {
					System.out.println("Address is not valid");
					System.exit(0);
				} else {
					num = memory[Integer.parseInt(num, 16)];
					for (int i = 0; i <= 1 && i < num.length(); i++) {
						ax[1 - i] = num.charAt(num.length() - i - 1);
					}
				}

			}

		} else if (second.contains("ax") || second.contains("bx") || second.contains("cx") || second.contains("dx")
				|| second.contains("al") || second.contains("bl") || second.contains("bh") || second.contains("cl")
				|| second.contains("ch") || second.contains("dl") || second.contains("dh") || second.equals("di")
				|| second.equals("si") || second.equals("bp")) { // register
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
				second = NumberToFourByteHexa(var.data); // variable
			} else {
				second = NumberToFourByteHexa(second); // number
			}
			if (Integer.parseInt(second, 16) > 255) {
				System.out.println("#ERROR 30: Byte-Sized Constant Required");
				System.exit(0);
			}
			for (int i = 0; i < 1; i++) {
				ax[i] = 0;
			}
			for (int i = 0; i <= 1 && i < second.length(); i++) {
				ax[1 - i] = second.charAt(second.length() - i - 1);
			}
		}
	}

	public void mov_bl_unknown(String second) {
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
			for (int i = 0; i <= 1; i++) {
				bx[2 + i] = '0';
			}
			for (int i = 0; i <= 1 && i < value.length(); i++) {
				bx[3 - i] = value.charAt(value.length() - i - 1);
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

				if (second.contains("bx") || second.contains("cx") || second.contains("dx") || second.contains("al")
						|| second.contains("ah") || second.contains("bl") || second.contains("bh")
						|| second.contains("cl") || second.contains("ch") || second.contains("dl")
						|| second.contains("dh") || second.equals("di") || second.equals("si") || second.equals("bp")) {// register
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
							bx[2 + i] = '0';
						}
						for (int i = 0; i <= 1 && i < num.length(); i++) {
							bx[3 - i] = num.charAt(num.length() - i - 1);
						}
						return;
					}
				} else {// number
					num = NumberToFourByteHexa(second);
				}

				bx[0] = '0';
				bx[1] = '0';
				if (memory[Integer.parseInt(num, 16)] == null) {

				} else if (Integer.parseInt(num, 16) < numberOfInstructions * 6
						|| Integer.parseInt(num, 16) >= 64 * 1024) {
					System.out.println("Address is not valid");
					System.exit(0);
				} else {
					num = memory[Integer.parseInt(num, 16)];
					for (int i = 0; i <= 1 && i < num.length(); i++) {
						ax[3 - i] = num.charAt(num.length() - i - 1);
					}
				}

			}

		} else if (second.contains("ax") || second.contains("bx") || second.contains("cx") || second.contains("dx")
				|| second.contains("al") || second.contains("bl") || second.contains("bh") || second.contains("cl")
				|| second.contains("ch") || second.contains("dl") || second.contains("dh") || second.equals("di")
				|| second.equals("si") || second.equals("bp")) { // register
			if (second.equals("al")) {
				for (int i = 3; i >= 2; i--) {
					bx[i] = ax[i];
				}
			} else if (second.equals("ah")) {
				for (int i = 3; i >= 2; i--) {
					bx[i] = ax[i - 2];
				}
			} else if (second.equals("bh")) {
				for (int i = 3; i >= 2; i--) {
					bx[i] = bx[i - 2];
				}
			} else if (second.equals("cl")) {
				for (int i = 3; i >= 2; i--) {
					bx[i] = cx[i];
				}
			} else if (second.equals("ch")) {
				for (int i = 3; i >= 2; i--) {
					bx[i] = cx[i - 2];
				}
			} else if (second.equals("dl")) {
				for (int i = 3; i >= 2; i--) {
					bx[i] = dx[i];
				}
			} else if (second.equals("dh")) {
				for (int i = 3; i >= 2; i--) {
					bx[i] = dx[i - 2];
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
				second = NumberToFourByteHexa(var.data); // variable
			} else {
				second = NumberToFourByteHexa(second); // number
			}
			if (Integer.parseInt(second, 16) > 255) {
				System.out.println("#ERROR 30: Byte-Sized Constant Required");
				System.exit(0);
			}
			for (int i = 0; i < 1; i++) {
				bx[2 + i] = 0;
			}
			for (int i = 0; i <= 1 && i < second.length(); i++) {
				bx[3 - i] = second.charAt(second.length() - i - 1);
			}
		}
	}

	public void mov_bh_unknown(String second) {
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
			for (int i = 0; i <= 1; i++) {
				bx[i] = '0';
			}
			for (int i = 0; i <= 1 && i < value.length(); i++) {
				bx[1 - i] = value.charAt(value.length() - i - 1);
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

				if (second.contains("bx") || second.contains("cx") || second.contains("dx") || second.contains("al")
						|| second.contains("ah") || second.contains("bl") || second.contains("bh")
						|| second.contains("cl") || second.contains("ch") || second.contains("dl")
						|| second.contains("dh") || second.equals("di") || second.equals("si") || second.equals("bp")) {// register
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
							bx[i] = '0';
						}
						for (int i = 2; i <= 3 && i < num.length(); i++) {
							bx[3 - i] = num.charAt(num.length() - i - 1);
						}
						return;
					}
				} else {// number
					num = NumberToFourByteHexa(second);
				}

				bx[0] = '0';
				bx[1] = '0';
				if (memory[Integer.parseInt(num, 16)] == null) {

				} else if (Integer.parseInt(num, 16) < numberOfInstructions * 6
						|| Integer.parseInt(num, 16) >= 64 * 1024) {
					System.out.println("Address is not valid");
					System.exit(0);
				} else {
					num = memory[Integer.parseInt(num, 16)];
					for (int i = 0; i <= 1 && i < num.length(); i++) {
						bx[1 - i] = num.charAt(num.length() - i - 1);
					}
				}

			}

		} else if (second.contains("ax") || second.contains("bx") || second.contains("cx") || second.contains("dx")
				|| second.contains("al") || second.contains("bl") || second.contains("bh") || second.contains("cl")
				|| second.contains("ch") || second.contains("dl") || second.contains("dh") || second.equals("di")
				|| second.equals("si") || second.equals("bp")) { // register
			if (second.equals("al")) {
				for (int i = 3; i >= 2; i--) {
					bx[i - 2] = ax[i];
				}
			} else if (second.equals("ah")) {
				for (int i = 3; i >= 2; i--) {
					bx[i - 2] = dx[i - 2];
				}
			} else if (second.equals("bl")) {
				for (int i = 3; i >= 2; i--) {
					bx[i - 2] = bx[i];
				}
			} else if (second.equals("cl")) {
				for (int i = 3; i >= 2; i--) {
					bx[i - 2] = cx[i];
				}
			} else if (second.equals("dh")) {
				for (int i = 3; i >= 2; i--) {
					bx[i - 2] = dx[i - 2];
				}
			} else if (second.equals("dl")) {
				for (int i = 3; i >= 2; i--) {
					bx[i - 2] = dx[i];
				}
			} else if (second.equals("dh")) {
				for (int i = 3; i >= 2; i--) {
					bx[i - 2] = dx[i - 2];
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
				second = NumberToFourByteHexa(var.data); // variable
			} else {
				second = NumberToFourByteHexa(second); // number
			}
			if (Integer.parseInt(second, 16) > 255) {
				System.out.println("#ERROR 30: Byte-Sized Constant Required");
				System.exit(0);
			}
			for (int i = 0; i < 1; i++) {
				bx[i] = 0;
			}
			for (int i = 0; i <= 1 && i < second.length(); i++) {
				bx[1 - i] = second.charAt(second.length() - i - 1);
			}
		}
	}

	public void mov_cl_unknown(String second) {
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
			for (int i = 0; i <= 1; i++) {
				cx[2 + i] = '0';
			}
			for (int i = 0; i <= 1 && i < value.length(); i++) {
				cx[3 - i] = value.charAt(value.length() - i - 1);
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

				if (second.contains("bx") || second.contains("cx") || second.contains("dx") || second.contains("al")
						|| second.contains("ah") || second.contains("bl") || second.contains("bh")
						|| second.contains("cl") || second.contains("ch") || second.contains("dl")
						|| second.contains("dh") || second.equals("di") || second.equals("si") || second.equals("bp")) {// register
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
							cx[2 + i] = '0';
						}
						for (int i = 0; i <= 1 && i < num.length(); i++) {
							cx[3 - i] = num.charAt(num.length() - i - 1);
						}
						return;
					}
				} else {// number
					num = NumberToFourByteHexa(second);
				}

				cx[0] = '0';
				cx[1] = '0';
				if (memory[Integer.parseInt(num, 16)] == null) {

				} else if (Integer.parseInt(num, 16) < numberOfInstructions * 6
						|| Integer.parseInt(num, 16) >= 64 * 1024) {
					System.out.println("Address is not valid");
					System.exit(0);
				} else {
					num = memory[Integer.parseInt(num, 16)];
					for (int i = 0; i <= 1 && i < num.length(); i++) {
						cx[3 - i] = num.charAt(num.length() - i - 1);
					}
				}

			}

		} else if (second.contains("ax") || second.contains("bx") || second.contains("cx") || second.contains("dx")
				|| second.contains("al") || second.contains("bl") || second.contains("bh") || second.contains("cl")
				|| second.contains("ch") || second.contains("dl") || second.contains("dh") || second.equals("di")
				|| second.equals("si") || second.equals("bp")) { // register
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
				second = NumberToFourByteHexa(var.data); // variable
			} else {
				second = NumberToFourByteHexa(second); // number
			}
			if (Integer.parseInt(second, 16) > 255) {
				System.out.println("#ERROR 30: Byte-Sized Constant Required");
				System.exit(0);
			}
			for (int i = 0; i < 1; i++) {
				cx[2 + i] = 0;
			}
			for (int i = 0; i <= 1 && i < second.length(); i++) {
				cx[3 - i] = second.charAt(second.length() - i - 1);
			}
		}
	}

	public void mov_ch_unknown(String second) {
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
			for (int i = 0; i <= 1; i++) {
				cx[i] = '0';
			}
			for (int i = 0; i <= 1 && i < value.length(); i++) {
				cx[1 - i] = value.charAt(value.length() - i - 1);
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

				if (second.contains("bx") || second.contains("cx") || second.contains("dx") || second.contains("al")
						|| second.contains("ah") || second.contains("bl") || second.contains("bh")
						|| second.contains("cl") || second.contains("ch") || second.contains("dl")
						|| second.contains("dh") || second.equals("di") || second.equals("si") || second.equals("bp")) {// register
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
							cx[i] = '0';
						}
						for (int i = 2; i <= 3 && i < num.length(); i++) {
							cx[3 - i] = num.charAt(num.length() - i - 1);
						}
						return;
					}
				} else {// number
					num = NumberToFourByteHexa(second);
				}

				cx[0] = '0';
				cx[1] = '0';
				if (memory[Integer.parseInt(num, 16)] == null) {

				} else if (Integer.parseInt(num, 16) < numberOfInstructions * 6
						|| Integer.parseInt(num, 16) >= 64 * 1024) {
					System.out.println("Address is not valid");
					System.exit(0);
				} else {
					num = memory[Integer.parseInt(num, 16)];
					for (int i = 0; i <= 1 && i < num.length(); i++) {
						cx[1 - i] = num.charAt(num.length() - i - 1);
					}
				}

			}

		} else if (second.contains("ax") || second.contains("bx") || second.contains("cx") || second.contains("dx")
				|| second.contains("al") || second.contains("bl") || second.contains("bh") || second.contains("cl")
				|| second.contains("ch") || second.contains("dl") || second.contains("dh") || second.equals("di")
				|| second.equals("si") || second.equals("bp")) { // register
			if (second.equals("al")) {
				for (int i = 3; i >= 2; i--) {
					dx[i - 2] = ax[i];
				}
			} else if (second.equals("ah")) {
				for (int i = 3; i >= 2; i--) {
					cx[i - 2] = dx[i - 2];
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
			} else if (second.equals("dh")) {
				for (int i = 3; i >= 2; i--) {
					cx[i - 2] = dx[i - 2];
				}
			} else if (second.equals("dl")) {
				for (int i = 3; i >= 2; i--) {
					cx[i - 2] = dx[i];
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
				second = NumberToFourByteHexa(var.data); // variable
			} else {
				second = NumberToFourByteHexa(second); // number
			}
			if (Integer.parseInt(second, 16) > 255) {
				System.out.println("#ERROR 30: Byte-Sized Constant Required");
				System.exit(0);
			}
			for (int i = 0; i < 1; i++) {
				cx[i] = 0;
			}
			for (int i = 0; i <= 1 && i < second.length(); i++) {
				cx[1 - i] = second.charAt(second.length() - i - 1);
			}
		}
	}

	public void mov_dl_unknown(String second) {
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
			for (int i = 0; i <= 1; i++) {
				dx[2 + i] = '0';
			}
			for (int i = 0; i <= 1 && i < value.length(); i++) {
				dx[3 - i] = value.charAt(value.length() - i - 1);
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

				if (second.contains("bx") || second.contains("cx") || second.contains("dx") || second.contains("al")
						|| second.contains("ah") || second.contains("bl") || second.contains("bh")
						|| second.contains("cl") || second.contains("ch") || second.contains("dl")
						|| second.contains("dh") || second.equals("di") || second.equals("si") || second.equals("bp")) {// register
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
							dx[2 + i] = '0';
						}
						for (int i = 0; i <= 1 && i < num.length(); i++) {
							dx[3 - i] = num.charAt(num.length() - i - 1);
						}
						return;
					}
				} else {// number
					num = NumberToFourByteHexa(second);
				}

				dx[0] = '0';
				dx[1] = '0';
				if (memory[Integer.parseInt(num, 16)] == null) {

				} else if (Integer.parseInt(num, 16) < numberOfInstructions * 6
						|| Integer.parseInt(num, 16) >= 64 * 1024) {
					System.out.println("Address is not valid");
					System.exit(0);
				} else {
					num = memory[Integer.parseInt(num, 16)];
					for (int i = 0; i <= 1 && i < num.length(); i++) {
						ax[3 - i] = num.charAt(num.length() - i - 1);
					}
				}

			}

		} else if (second.contains("ax") || second.contains("bx") || second.contains("cx") || second.contains("dx")
				|| second.contains("al") || second.contains("bl") || second.contains("bh") || second.contains("cl")
				|| second.contains("ch") || second.contains("dl") || second.contains("dh") || second.equals("di")
				|| second.equals("si") || second.equals("bp")) { // register
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
				second = NumberToFourByteHexa(var.data); // variable
			} else {
				second = NumberToFourByteHexa(second); // number
			}
			if (Integer.parseInt(second, 16) > 255) {
				System.out.println("#ERROR 30: Byte-Sized Constant Required");
				System.exit(0);
			}
			for (int i = 0; i < 1; i++) {
				dx[2 + i] = 0;
			}
			for (int i = 0; i <= 1 && i < second.length(); i++) {
				dx[3 - i] = second.charAt(second.length() - i - 1);
			}
		}
	}

	public void mov_dh_unknown(String second) {
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
			for (int i = 0; i <= 1; i++) {
				dx[i] = '0';
			}
			for (int i = 0; i <= 1 && i < value.length(); i++) {
				dx[1 - i] = value.charAt(value.length() - i - 1);
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

				if (second.contains("bx") || second.contains("cx") || second.contains("dx") || second.contains("al")
						|| second.contains("ah") || second.contains("bl") || second.contains("bh")
						|| second.contains("cl") || second.contains("ch") || second.contains("dl")
						|| second.contains("dh") || second.equals("di") || second.equals("si") || second.equals("bp")) {// register
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
							dx[i] = '0';
						}
						for (int i = 2; i <= 3 && i < num.length(); i++) {
							dx[3 - i] = num.charAt(num.length() - i - 1);
						}
						return;
					}
				} else {// number
					num = NumberToFourByteHexa(second);
				}

				dx[0] = '0';
				dx[1] = '0';
				if (memory[Integer.parseInt(num, 16)] == null) {

				} else if (Integer.parseInt(num, 16) < numberOfInstructions * 6
						|| Integer.parseInt(num, 16) >= 64 * 1024) {
					System.out.println("Address is not valid");
					System.exit(0);
				} else {
					num = memory[Integer.parseInt(num, 16)];
					for (int i = 0; i <= 1 && i < num.length(); i++) {
						dx[1 - i] = num.charAt(num.length() - i - 1);
					}
				}

			}

		} else if (second.contains("ax") || second.contains("bx") || second.contains("cx") || second.contains("dx")
				|| second.contains("al") || second.contains("bl") || second.contains("bh") || second.contains("cl")
				|| second.contains("ch") || second.contains("dl") || second.contains("dh") || second.equals("di")
				|| second.equals("si") || second.equals("bp")) { // register
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
				second = NumberToFourByteHexa(var.data); // variable
			} else {
				second = NumberToFourByteHexa(second); // number
			}
			if (Integer.parseInt(second, 16) > 255) {
				System.out.println("#ERROR 30: Byte-Sized Constant Required");
				System.exit(0);
			}
			for (int i = 0; i < 1; i++) {
				dx[i] = 0;
			}
			for (int i = 0; i <= 1 && i < second.length(); i++) {
				dx[1 - i] = second.charAt(second.length() - i - 1);
			}
		}
	}

	public void mov_si_unknown(String second) {
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
				si[i] = '0';
			}
			for (int i = 0; i <= 3 && i < value.length(); i++) {
				si[3 - i] = value.charAt(value.length() - i - 1);
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
				if (second.contains("ax") || second.contains("bx") || second.contains("cx") || second.contains("dx")
						|| second.contains("al") || second.contains("bl") || second.contains("bh")
						|| second.contains("cl") || second.contains("ch") || second.contains("dl")
						|| second.contains("dh") || second.equals("di") || second.equals("si") || second.equals("bp")) {// register
					if (second.equals("di")) {
						for (int i = 0; i <= 3; i++) {
							num += di[i];
						}
					} else if (second.equals("si")) {
						for (int i = 0; i <= 3; i++) {
							num += si[i];
						}
					} else if (second.equals("bp")) {
						for (int i = 0; i <= 3; i++) {
							num += bp[i];
						}
					} else if (second.equals("bx")) {
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
							si[i] = '0';
						}
						for (int i = 0; i <= 3 && i < num.length(); i++) {
							si[3 - i] = num.charAt(num.length() - i - 1);
						}
						return;
					}
				} else {// number
					num = NumberToFourByteHexa(second);
				}

				// got the value, just insert to si
				for (int i = 0; i <= 3; i++) {
					si[i] = '0';
				}

				if (Integer.parseInt(num, 16) >= memory.length
						|| Integer.parseInt(num, 16) < numberOfInstructions * 6) {
					System.out.println("Address is not valid");
					System.exit(0);

				} else if (memory[Integer.parseInt(num, 16)] == null) {
				} else {
					String memoryLocaitonOfNum = memory[Integer.parseInt(num, 16)];
					for (int i = 0; i <= 3 && i < num.length(); i++) {
						si[3 - i] = memoryLocaitonOfNum.charAt(memoryLocaitonOfNum.length() - i - 1);
					}
				}
			}
		} else if (second.contains("bx") || second.contains("cx") || second.contains("dx") || second.contains("al")
				|| second.contains("ah") || second.contains("bl") || second.contains("bh") || second.contains("cl")
				|| second.contains("ch") || second.contains("dl") || second.contains("dh") || second.equals("di")
				|| second.equals("ax") || second.equals("bp")) { // register

			if (second.equals("ax")) {
				for (int i = 3; i >= 0; i--) {
					si[i] = ax[i];
				}
			} else if (second.equals("bx")) {
				for (int i = 3; i >= 0; i--) {
					si[i] = bx[i];
				}
			} else if (second.equals("cx")) {
				for (int i = 3; i >= 0; i--) {
					si[i] = cx[i];
				}
			} else if (second.equals("dx")) {
				for (int i = 3; i >= 0; i--) {
					si[i] = dx[i];
				}
			} else if (second.equals("bp")) {
				for (int i = 3; i >= 0; i--) {
					si[i] = bp[i];
				}
			} else if (second.equals("di")) {
				for (int i = 3; i >= 0; i--) {
					si[i] = di[i];
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
			for (int i = 0; i < 3; i++) {
				si[i] = 0;
			}
			for (int i = 0; i <= 3 && i < second.length(); i++) {
				si[3 - i] = second.charAt(second.length() - i - 1);
			}
		}

	}

	public void mov_di_unknown(String second) {
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
				di[i] = '0';
			}
			for (int i = 0; i <= 3 && i < value.length(); i++) {
				di[3 - i] = value.charAt(value.length() - i - 1);
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

				// get the index of memory address
				if (second.contains("ax") || second.contains("bx") || second.contains("cx") || second.contains("dx")
						|| second.contains("al") || second.contains("bl") || second.contains("bh")
						|| second.contains("cl") || second.contains("ch") || second.contains("dl")
						|| second.contains("dh") || second.equals("di") || second.equals("si") || second.equals("bp")) {// register
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
							di[i] = '0';
						}
						for (int i = 0; i <= 3 && i < num.length(); i++) {
							di[3 - i] = num.charAt(num.length() - i - 1);
						}
						return;
					}
				} else {// number
					num = NumberToFourByteHexa(second);
				}

				// got the index, check it's valid then insert
				for (int i = 0; i <= 3; i++) {
					di[i] = '0';
				}

				if (Integer.parseInt(num, 16) >= memory.length
						|| Integer.parseInt(num, 16) < numberOfInstructions * 6) {
					System.out.println("Address is not valid");
					System.exit(0);

				} else if (memory[Integer.parseInt(num, 16)] == null) {
				} else {
					String memoryLocaitonOfNum = memory[Integer.parseInt(num, 16)];
					for (int i = 0; i <= 3 && i < num.length(); i++) {
						di[3 - i] = memoryLocaitonOfNum.charAt(memoryLocaitonOfNum.length() - i - 1);
					}
				}
			}
		} else if (second.contains("bx") || second.contains("cx") || second.contains("dx") || second.contains("al")
				|| second.contains("ah") || second.contains("bl") || second.contains("bh") || second.contains("cl")
				|| second.contains("ch") || second.contains("dl") || second.contains("dh") || second.equals("ax")
				|| second.equals("si") || second.equals("bp")) { // register

			if (second.equals("ax")) {
				for (int i = 3; i >= 0; i--) {
					di[i] = ax[i];
				}
			} else if (second.equals("bx")) {
				for (int i = 3; i >= 0; i--) {
					di[i] = bx[i];
				}
			} else if (second.equals("cx")) {
				for (int i = 3; i >= 0; i--) {
					di[i] = cx[i];
				}
			} else if (second.equals("dx")) {
				for (int i = 3; i >= 0; i--) {
					di[i] = dx[i];
				}
			} else if (second.equals("si")) {
				for (int i = 3; i >= 0; i--) {
					di[i] = si[i];
				}
			} else if (second.equals("bp")) {
				for (int i = 3; i >= 0; i--) {
					di[i] = bp[i];
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
			for (int i = 0; i < 3; i++) {
				di[i] = 0;
			}
			for (int i = 0; i <= 3 && i < second.length(); i++) {
				di[3 - i] = second.charAt(second.length() - i - 1);
			}
		}

	}

	public void mov_bp_unknown(String second) {
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
				bp[i] = '0';
			}
			for (int i = 0; i <= 3 && i < value.length(); i++) {
				bp[3 - i] = value.charAt(value.length() - i - 1);
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

				// get the index of memory address
				if (second.contains("ax") || second.contains("bx") || second.contains("cx") || second.contains("dx")
						|| second.contains("al") || second.contains("bl") || second.contains("bh")
						|| second.contains("cl") || second.contains("ch") || second.contains("dl")
						|| second.contains("dh") || second.equals("di") || second.equals("si") || second.equals("bp")) {// register
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
							bp[i] = '0';
						}
						for (int i = 0; i <= 3 && i < num.length(); i++) {
							bp[3 - i] = num.charAt(num.length() - i - 1);
						}
						return;
					}
				} else {// number
					num = NumberToFourByteHexa(second);
				}

				// got the index, check it's valid then insert
				for (int i = 0; i <= 3; i++) {
					bp[i] = '0';
				}

				if (Integer.parseInt(num, 16) >= memory.length
						|| Integer.parseInt(num, 16) < numberOfInstructions * 6) {
					System.out.println("Address is not valid");
					System.exit(0);

				} else if (memory[Integer.parseInt(num, 16)] == null) {
				} else {
					String memoryLocaitonOfNum = memory[Integer.parseInt(num, 16)];
					for (int i = 0; i <= 3 && i < num.length(); i++) {
						bp[3 - i] = memoryLocaitonOfNum.charAt(memoryLocaitonOfNum.length() - i - 1);
					}
				}
			}
		} else if (second.contains("bx") || second.contains("cx") || second.contains("dx") || second.contains("al")
				|| second.contains("ah") || second.contains("bl") || second.contains("bh") || second.contains("cl")
				|| second.contains("ch") || second.contains("dl") || second.contains("dh") || second.equals("ax")
				|| second.equals("si") || second.equals("di")) { // register

			if (second.equals("ax")) {
				for (int i = 3; i >= 0; i--) {
					bp[i] = ax[i];
				}
			} else if (second.equals("bx")) {
				for (int i = 3; i >= 0; i--) {
					bp[i] = bx[i];
				}
			} else if (second.equals("cx")) {
				for (int i = 3; i >= 0; i--) {
					bp[i] = cx[i];
				}
			} else if (second.equals("dx")) {
				for (int i = 3; i >= 0; i--) {
					bp[i] = dx[i];
				}
			} else if (second.equals("si")) {
				for (int i = 3; i >= 0; i--) {
					bp[i] = si[i];
				}
			} else if (second.equals("di")) {
				for (int i = 3; i >= 0; i--) {
					bp[i] = di[i];
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
			for (int i = 0; i < 3; i++) {
				bp[i] = 0;
			}
			for (int i = 0; i <= 3 && i < second.length(); i++) {
				bp[3 - i] = second.charAt(second.length() - i - 1);
			}
		}

	}

	public void mov_reg_unknown(String first, String second) {

		if (first.equalsIgnoreCase("ax")) {
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
		} else if (first.equalsIgnoreCase("di")) {
			mov_dh_unknown(second);
		} else if (first.equalsIgnoreCase("sp")) {
			mov_dh_unknown(second);
		} else if (first.equalsIgnoreCase("bp")) {
			mov_dh_unknown(second);
		}
	}

	public void mov(String first, String second) {

		if (first.contains("[") && first.contains("]")) { // first is memory

			mov_mem_xx(first, second);

		} else if (first.equals("ax") || first.equals("cx") || first.equals("bx") || first.equals("dx")
				|| first.equals("ah") || first.equals("al") || first.equals("bl") || first.equals("bh")
				|| first.equals("ch") || first.equals("cl") || first.equals("dl") || first.equals("dh")
				|| first.equals("di") || first.equals("si") || first.equals("bp")) { // first is reg
			mov_reg_unknown(first, second);

		} else { // reg veya memoryye yazmýyo hata ver
			System.out.println("Undefined symbols are listed");
			System.exit(0);
		}

	}

	public int binaryToDecimal(int a) {
		return Integer.parseInt("+a+", 2);
	}

	public static int DecimalToBinary(int a) {
		return Integer.valueOf(Integer.toBinaryString(a).substring(0, Integer.toBinaryString(a).length()));
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

	