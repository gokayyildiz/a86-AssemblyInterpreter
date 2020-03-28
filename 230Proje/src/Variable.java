public class Variable {
	public String name;
	public int memoryIndex;
	// where data stored at the memory
	public String data; // data is saved as string even if it's integer
	// we can think about it
	public boolean type; // true dw, false db

	public String getName() {
		return name;
	}

	public int getMemoryIndex() {
		return memoryIndex;
	}

	public boolean isType() {
		return type;
	}

	/**
	 * @param name
	 * @param memoryIndex
	 * @param data
	 * @param type
	 */
	public Variable(String name, int memoryIndex, String data, boolean type) {
		this.name = name;
		this.memoryIndex = memoryIndex;
		// data is saved as hexa
		// data could be char. Then this line gives error. Needs to be fixed!!
		// herþeye küöçük harfe çevirince eðer input büyükse variablenýn deðeri
		// deðiþiyo.Ýstenmeyen bir þey! e.g: var1 dw 'W', var2 db 'H'.
		if (data.contains("\'")) {
			int a = data.charAt(data.indexOf("\'") + 1) + 1 - 1;
			this.data = "" + a;
		} else {
			this.data = Hyp86.NumberToFourByteHexa(data);
		}
		this.type = type;
	}

}
