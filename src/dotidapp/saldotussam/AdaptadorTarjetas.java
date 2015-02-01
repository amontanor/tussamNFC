package dotidapp.saldotussam;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.opengl.Visibility;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class AdaptadorTarjetas extends ArrayAdapter {
	Activity context;
	List<Tarjetas> tarjetas;
	Context contexto;
	String id;

	AdaptadorTarjetas(Activity context, List<Tarjetas> tarjetas,
			Context contexto) {
		super(context, R.layout.itemlista, tarjetas);
		this.tarjetas = tarjetas;
		this.context = context;
		this.contexto = contexto;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = context.getLayoutInflater();
		View item = inflater.inflate(R.layout.itemlista, null);
		
		final int pos = position;

		TextView lblId = (TextView) item.findViewById(R.id.lblId);
		id = tarjetas.get(position).getId();
		lblId.setText(id);

		TextView lblTitulo = (TextView) item.findViewById(R.id.lblTitulo);
		lblTitulo.setText(tarjetas.get(position).getTitulo());

		TextView lblCaducidad = (TextView) item.findViewById(R.id.lblCaducidad);
		lblCaducidad.setText(tarjetas.get(position).getCaducidad());

		TextView lblSaldo = (TextView) item.findViewById(R.id.lblSaldo);
		lblSaldo.setText(tarjetas.get(position).getSaldo() + " €");

		// Es negativo
		if (lblSaldo.getText().toString().contains("-")) {
			lblSaldo.setTextColor(Color.RED);
		}// No contiene valor, es decir, la lista esta vacia
		else if (lblSaldo.getText().toString().equals(" €")) {
			lblSaldo.setText("");
		} else {
			lblSaldo.setTextColor(Color.parseColor("#A9BA88"));
		}

		ImageView iconoRefrescar = (ImageView) item
				.findViewById(R.id.iconoRefrescar);
		ImageView iconoBorrar = (ImageView) item.findViewById(R.id.iconoBorrar);
		
		if (id.equals(""))
		{
			iconoBorrar.setVisibility(View.INVISIBLE);
			iconoRefrescar.setVisibility(View.INVISIBLE);
			
		}
		else
		{
			iconoBorrar.setVisibility(View.VISIBLE);
			iconoRefrescar.setVisibility(View.VISIBLE);
		}

		iconoRefrescar.setOnClickListener(new View.OnClickListener() {

			// @Override
			public void onClick(View v) {
				// Borrar Lista
				try {
					new Thread() {
						@Override
						public void run() {
							Looper.prepare();
							Toast.makeText(
									contexto,
									contexto.getResources().getString(
											R.string.actualiza), Toast.LENGTH_LONG)
									.show();
							Looper.loop();
						}
					}.start();
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
				id = tarjetas.get(pos).getId();
				MainActivity.cargarDatosDeUnoEnUno(id,pos);
				MainActivity.actualizarLista();
			}
		});

		iconoBorrar.setOnClickListener(new View.OnClickListener() {
			// @Override
			public void onClick(View v) {
				crearMensaje();
			}

			private void crearMensaje() {
				AlertDialog.Builder builder = new AlertDialog.Builder(contexto);
				 builder.setIcon(android.R.drawable.ic_dialog_info);
				 builder.setTitle("Saldo Tussam");
				 builder.setMessage("¿Eliminar tarjeta?");
				 builder.setPositiveButton("Sí", new DialogInterface.OnClickListener() {
				 public void onClick(DialogInterface arg0, int arg1) {
				     BorrarTarjetaBBDD();
				     MainActivity.refrescarLista();
				  }

				private void BorrarTarjetaBBDD() {
					// Abrimos la base de datos en modo lectura
					SQLiteHelper usdbh = new SQLiteHelper(contexto, "baseDeDatos", null, 1);

					SQLiteDatabase db = usdbh.getReadableDatabase();
					db.execSQL("delete from Tarjetas where id ='" + id + "'");

					db.close();

				}
				  });
				 builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
				 public void onClick(DialogInterface arg0, int arg1) {
				    //DO TASK
				 }
				});

				AlertDialog dialog = builder.create();
				dialog.show();
				
			}
		});

		return (item);
	}

}
