package dk.statsbiblioteket.netark.dvenabler.gui;

public class SchemaField {

	private String name;
	private String type;
	private boolean stored;
	private boolean hasDocVal;
	
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
	public boolean isHasDocVal() {
		return hasDocVal;
	}
	public void setHasDocVal(boolean hasDocVal) {
		this.hasDocVal = hasDocVal;
	}
	
	
	
	
	
	

}
