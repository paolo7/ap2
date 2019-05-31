package logic;

public abstract class TextTemplateAbstr implements TextTemplate{

	@Override
    public boolean equals(Object o) {
  
        if (o == this) {
            return true;
        }
        if (!(o instanceof TextTemplate)) {
            return false;
        }
        TextTemplate p = (TextTemplate) o;
        if(this.isVar() != p.isVar()) return false;
        if(this.isVar()) {
        	if(this.getVar() == p.getVar()) return true;
        } else {
        	if(this.getText().toLowerCase().equals(p.getText().toLowerCase())) return true;
        }
        return false;
    }
	
	@Override
	public String toString() {
		if(this.isText()) return this.getText();
		else return "[?v"+this.getVar()+"]";
	}
	
	@Override
	public int hashCode() {
		String text = null;
		if(this.isText()) text = this.getText().toLowerCase();
		int var = -1;
		if(this.isVar()) var = this.getVar();
		
		final int prime = 31;
		int result = 7;
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		result = prime * result + var;
		return result;
	}
}
