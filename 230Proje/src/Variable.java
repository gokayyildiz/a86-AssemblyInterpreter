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
		//data is saved as hexa
		this.data = Hyp86.NumberToFourByteHexa(data);
		this.type = type;
	}

}
