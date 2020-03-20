
public class Variable {
	String name;
	int memoryIndex;
	//where data stored at the memory
	String data;	// data is saved as string even if it's integer 
	// we can think about it
	boolean type; // true dw, false db
	/**
	 * @param name
	 * @param memoryIndex
	 * @param data
	 * @param type
	 */
	public Variable(String name, int memoryIndex, String data, boolean type) {
		this.name = name;
		this.memoryIndex = memoryIndex;
		this.data = data;
		this.type = type;
	}
	
	

}
