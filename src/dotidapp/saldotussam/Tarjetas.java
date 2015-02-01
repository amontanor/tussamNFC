package dotidapp.saldotussam;

public class Tarjetas {

	private String id, saldo, titulo, caducidad;

	
	public Tarjetas(String id, String saldo, String titulo, String caducidad) {
		super();
		this.id = id;
		this.saldo = saldo;
		this.titulo = titulo;
		this.caducidad = caducidad;
	}

	public String getSaldo() {
		return saldo;
	}

	public void setSaldo(String saldo) {
		this.saldo = saldo;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return id + "  -  Saldo: " + saldo + "€";
	}

	public String getTitulo() {
		return titulo;
	}

	public void setTitulo(String titulo) {
		this.titulo = titulo;
	}

	public String getCaducidad() {
		return caducidad;
	}

	public void setCaducidad(String caducidad) {
		this.caducidad = caducidad;
	}
	
	
	
	
}
