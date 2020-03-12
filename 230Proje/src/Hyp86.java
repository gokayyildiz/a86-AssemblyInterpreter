import java.io.File;

public class Hyp86 {

	char[] memory = new char[64 * 1024];
	// hexa tutalým
	char[] ax = new char[4];
	char[] bx = new char[4];
	char[] cx = new char[4];
	char[] dx = new char[4];
	boolean ZF = false;
	boolean AF = false;
	boolean CF = false;
	String SF = "FFFE";
	boolean OF = false;
	Hyp86(String s){
		
	}
	public static void add(String first, String second) {

	}

	// firstin memorydei adresine secondýn deðerini taþý
	// second pointer olabilir ex: [454]
	// second sayý olabilir ex: 454
	// second reg olabilir ex: cl
	// first reg veya memory
	// second boyutu firstten büyükse hata ver

	public void mov(String first, String second) {

		if (first.contains("[") && first.contains("]")) { // first is memory

		} else if (first.equals("ax") || first.equals("cx") || first.equals("bx") || first.equals("dx")
				|| first.equals("ah") || first.equals("al") || first.equals("bl") || first.equals("bh")
				|| first.equals("ch") || first.equals("cl") || first.equals("dl") || first.equals("dh")) { // first is
																											// reg

			if (first.equalsIgnoreCase("ax")) { // mov ax,al hatasý +, mov bx,cx +,
				if (second.contains("[") && second.contains("]")) { // second is memory

				} else if (second.equals("bx")) {
					for (int i = 3; i >= 0; i--) {
						ax[i] = bx[i];
					}
				} else if (second.equals("cx")) {
					for (int i = 3; i >= 0; i--) {
						ax[i] = cx[i];
					}
				} else if (second.equals("dx")) {
					for (int i = 3; i >= 0; i--) {
						ax[i] = dx[i - 4];
					}
				} else if (second.contains("l") || second.contains("h")) {// error
					System.out.println("Error: Byte/Word Combination Not Allowed");
					System.exit(0);
				} else {// number hexa olabilir (sonunda h ile veya baþýnda 0), decimal olabilir

				}
			}

			// bx
			else if (first.equalsIgnoreCase("bx")) {
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
				} else if (second.contains("l") || second.contains("h")) {// error
					System.out.println("Error: Byte/Word Combination Not Allowed");
					System.exit(0);
				} else {// number

				}
			}

			else if (first.equalsIgnoreCase("cx")) {
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
				} else if (second.contains("l") || second.contains("h")) {// error
					System.out.println("Error: Byte/Word Combination Not Allowed");
					System.exit(0);
				} else {// number

				}
			} else if (first.equalsIgnoreCase("dx")) {
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
				} else if (second.contains("l") || second.contains("h")) {// error
					System.out.println("Error: Byte/Word Combination Not Allowed");
					System.exit(0);
				} else {// number

				}
			} else if (first.equalsIgnoreCase("al")) {

			} else if (first.equalsIgnoreCase("ah")) {

			} else if (first.equalsIgnoreCase("bl")) {

			} else if (first.equalsIgnoreCase("bh")) {

			} else if (first.equalsIgnoreCase("cl")) {

			} else if (first.equalsIgnoreCase("ch")) {

			} else if (first.equalsIgnoreCase("dl")) {

			} else if (first.equalsIgnoreCase("dh")) {

			}
		} else { // reg veya memoryye yazmýyo hata ver
			System.out.println("Undefined sysbols are listed");
			System.exit(0);
		}

	}

	public int hexaToDecimal(int a) {
		return Integer.parseInt("+a+");
	}

	public int DecimalToHexa(int a) {
		return Integer.valueOf(Integer.toHexString(a).substring(0, Integer.toHexString(a).length()));
	}

	public int binaryToDecimal(int a) {
		return Integer.parseInt("+a+", 2);
	}

	public static int DecimalToBinary(int a) {
		return Integer.valueOf(Integer.toBinaryString(a).substring(0, Integer.toBinaryString(a).length()));
	}

}
