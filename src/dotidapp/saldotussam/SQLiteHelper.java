package dotidapp.saldotussam;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class SQLiteHelper extends SQLiteOpenHelper{
	
	// Sentencia SQL para crear la tabla de Tarjetas
	String sqlCreateTarjetas = "create table if not exists Tarjetas (id TEXT, saldo TEXT, titulo TEXT, caducidad TEXT)";

	public SQLiteHelper(Context contexto, String nombre,
			CursorFactory factory, int version) {
		super(contexto, nombre, factory, version);
	}
	
	@Override
    public void onCreate(SQLiteDatabase db) {
        //Se ejecuta la sentencia SQL de creación de la tabla
        db.execSQL(sqlCreateTarjetas);
    }
	
    @Override
    public void onUpgrade(SQLiteDatabase db, int versionAnterior, int versionNueva) {
        //NOTA: Por simplicidad del ejemplo aquí utilizamos directamente la opción de
        //      eliminar la tabla anterior y crearla de nuevo vacía con el nuevo formato.
        //      Sin embargo lo normal será que haya que migrar datos de la tabla antigua
        //      a la nueva, por lo que este método debería ser más elaborado.
 
        //Se elimina la versión anterior de la tabla
        db.execSQL("DROP TABLE IF EXISTS Usuarios");
 
        //Se crea la nueva versión de la tabla
        //db.execSQL(sqlCreate);
    }
}