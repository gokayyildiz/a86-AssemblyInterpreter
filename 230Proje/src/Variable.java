public class Variable {
	private String name;
	private int memoryIndex;
	// where data stored at the memory
	private String data; // data is saved as string even if it's integer
	// we can think about it
	private boolean type; // true dw, false db

	/**
	 * @param name
	 * @param memoryIndex
	 * @param data
	 * @param type
	 */
	public Variable(String name, int memoryIndex, String data, boolean type) {
		this.name = name;
		this.memoryIndex = memoryIndex;
		this.type = type;
		// TODO
		// herşeye küöçük harfe çevirince eğer input büyükse variablenın değeri
		// değişiyo.İstenmeyen bir şey! e.g: var1 dw 'W', var2 db 'H'.
		
		if (data.contains("'")) {// when var.data is 'x'
			int a = data.charAt(data.indexOf("'") + 1) + 1 - 1;
			this.setData("" + Integer.toHexString(a));
		} else if (data.contains("\"")) {// when var.data is "x"
			int a = data.charAt(data.indexOf("\"") + 1) + 1 - 1;
			this.setData("" + Integer.toHexString(a));
		} else {
			if(type) {
				this.setData(Hyp86.NumberToFourByteHexa(data,false));
			}else {
				this.setData(Hyp86.NumberToFourByteHexa(data,false).substring(2));
			}
		}
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getMemoryIndex() {
		return memoryIndex;
	}

	public void setMemoryIndex(int memoryIndex) {
		this.memoryIndex = memoryIndex;
	}

	public boolean getType() {
		return type;
	}

	public void setType(boolean type) {
		this.type = type;
	}

}
