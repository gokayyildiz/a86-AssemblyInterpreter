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

	// byte arrayi yapýnca charý koyamayýz o yüzden array boyutunu yarýya düþürdüm.
	// bu 2^15 yapýyo. hoca da bu uzunlukta tutmuþtu zaten
	// ama fffe 65534 yapýyo
	// bence biz arrayi yine 64k tutmalýyýz
	char[] memory = new char[32768 * 4]; // hexa tutalým
	// memoryyi 128k yaptım. çünkü registerlar 2 byte e 4 uzunlupunda bu da 64kbyte
	// ise 128k uzunluğunda olsun.
	// her char 4 bit düşünüyoruz(hexa karakter)
	// bu 4 ü ne ise yarýyo bilmiyorum henuz
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

	Hyp86(String FileAsString) { // constructor
		numberOfInstructions = 0; // bunu initialize edip ona göre memory ayýrcaz

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

	public void mov_mem_xx(String first, String second) { // gökay yeni mmeory yaptıktan sonra yazıcam

	}

	public void mov_ax_unknown(String second) {
/*
		if (second.contains("[") && second.contains("]")) { // indirect adrressing
			// w[xxxx] ya da b[xxxx] olabilir
			*
			 * Bad Index Register:
			 * 
			 * This is reported when you attempt to use a register other than SI, DI, BX, or
			 * BP for indexing. Those are the only registers that the 86 architecture allows
			 * you to place inside brackets, to address memory.
			 *
/*
			if (second.charAt(0) == 'b') { // 1 byte
				System.out.println("Byte/Word Combination Not Allowed");
				System.exit(0);

			}

			else { // 2 byte
				if (second.charAt(0) == 'w') {
					second = second.substring(1); // got rid of 'w'
				}
				second = second.substring(1, second.length() - 1); // got rid of [ and ]
				String num = "";
				if (second.contains("si") || second.contains("di") || second.contains("bx") || second.contains("bp")) { // register
					// indirect
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
					// got the value put in ax

				} else {// sayı vardır içinde, sayıyı hexaya çevir
					num = NumberToFourByteHexa(second);
				}

				for (int i = 0; i <= 3; i++) {
					ax[ i] = memory[Integer.parseInt(num, 16) * 2 + i];
				}

			}
		}
		
		
		 burası değişecek çünkü memory şekli değişti
		*/
		

		
		
		
		
		
		//else
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
		} else if (second.contains("al") || second.contains("ah") || second.contains("bl") || second.contains("bh")
				|| second.contains("cl") || second.contains("ch") || second.contains("dl") || second.contains("dh")) {// error
			System.out.println("Error: Byte/Word Combination Not Allowed");
			System.exit(0);
		}
		// bu kýsým ok gibi ama deðiþik inputlarla test etmek lazým pek emin deðilim
		else {// var olanilir, "offset var" olabilir

			second = NumberToFourByteHexa(second);
			for (int i = 0; i < 3; i++)
				ax[i] = 0;
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

}
