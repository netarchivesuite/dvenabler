package dk.statsbiblioteket.netark.dvenabler.gui;

public class SchemaField {

	private String name;
	private String type;
	private boolean stored;
	private boolean docVal;
	
	public SchemaField(){
		
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public boolean isStored() {
		return stored;
	}
	public void setStored(boolean stored) {
		this.stored = stored;
	}

	public boolean isDocVal() {
		return docVal;
	}

	public void setDocVal(boolean docVal) {
		this.docVal = docVal;
	}
	

}
