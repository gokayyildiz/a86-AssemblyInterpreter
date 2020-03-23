
public class Variable {
	public String name;
	public int memoryIndex;
	//where data stored at the memory
	public String data;	// data is saved as string even if it's integer 
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

	public int resultData;// hold the data as a decimal integer
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
		data_check();
	}
	
	private void data_check(){
		int index = -1;
		String temp;
	
		if(data.indexOf('h') != -1) {
			index = data.indexOf('h');
			temp = data.substring(0,index);
			resultData = Integer.parseInt(temp, 16);
		}else if(data.indexOf('b') != -1) {
			index = data.indexOf('b');
			temp = data.substring(0,index);
			resultData = Integer.parseInt(temp, 2);
		}else {
			if(data.indexOf('d') != -1) {
				index = data.indexOf('b');
				temp = data.substring(0,index);
				resultData = Integer.parseInt(temp);
			}else {
				resultData = Integer.parseInt(data);
			}
		}
		
		
		
	}
	
	
	

}
