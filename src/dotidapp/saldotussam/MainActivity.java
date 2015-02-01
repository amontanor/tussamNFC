package dotidapp.saldotussam;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.flurry.android.FlurryAgent;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.os.StrictMode;
import android.text.Editable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final String Webpage = "https://recargas.tussam.es:443/TPW/Common/cardStatus.do?swNumber=";
	static TextView label;
	IntentFilter[] filters;
	String[][] techs;
	PendingIntent pendingIntent;
	NfcAdapter adapter;
	private ListView listaTarjetasLayout;
	private static ArrayAdapter adapterArray;
	private static AdaptadorTarjetas adaptadorTarjetas;
	private static long number;
	static String cadenaResultado = "";
	static Context contexto;
	private static List<Tarjetas> listaTarjetas = new ArrayList<>();
	private static int tipoDeBusqueda;// 0 Nfc, 1 a mano, 2 refrescar
	private static String codigoEscritoAMano;
	private static String numberString;
	private static String titulo;
	private static String caducidad;
	static String numeroCompleto = null;
	static Boolean semaforoAbierto = false;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Alternativa 1
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onStart() {
		super.onStart();
		FlurryAgent.onStartSession(this, "S7VG5J294X7ZYWZR6WV2");

	}



	@Override
	protected void onStop() {
		super.onStop();
		FlurryAgent.onEndSession(this);
	}



	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.optionShare:
			compartirOtros();
			FlurryAgent.logEvent("Compartir");
			return true;
		case R.id.optionAdd:
			metodoAdd();
			FlurryAgent.logEvent("Añadir sin NFC");
			return true;
		case R.id.OptionRefresh:
			metodoRefresh();
			FlurryAgent.logEvent("Refresco");
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private void compartirOtros() {
		try {
			Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
			shareIntent.setType("text/plain");
			shareIntent.putExtra(android.content.Intent.EXTRA_TEXT,
					getResources().getString(R.string.compartir));
			startActivity(Intent.createChooser(shareIntent, "MeetUs"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void metodoRefresh() {

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

		for (int i = 0; i < listaTarjetas.size(); i++) {
			cargarDatosDeUnoEnUno(listaTarjetas.get(i).getId(),i);
		}
		
		actualizarLista();
	}

	public static void actualizarLista() {
		
		// Actualizar BBDD
				for (int i = 0; i < listaTarjetas.size(); i++) {
					actualizaBBDD(listaTarjetas.get(i).getId(), listaTarjetas.get(i)
							.getSaldo());
				}

		
				refrescarLista();

		
	}

	public static void refrescarLista() {
		// Actualizar lista
		listaTarjetas.clear();
		comprobarBBDD();
		// adapterArray.notifyDataSetChanged();
		adaptadorTarjetas.notifyDataSetChanged();
		
	}

	public static void cargarDatosDeUnoEnUno(String id, int pos) {
		semaforoAbierto = false;
		comprobarSaldoTarjeta(id);
		while (!semaforoAbierto) {
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		listaTarjetas.get(pos).setSaldo(cadenaResultado);
	}

	private static void actualizaBBDD(String id, String saldo) {
		// Abrimos la base de datos en modo lectura
		SQLiteHelper usdbh = new SQLiteHelper(contexto, "baseDeDatos", null, 1);

		SQLiteDatabase db = usdbh.getReadableDatabase();
		db.execSQL("UPDATE Tarjetas set saldo='" + saldo + "' where id='" + id
				+ "';");

		db.close();

	}

	private static void comprobarSaldoTarjeta(String id) {
		tipoDeBusqueda = 2;
		codigoEscritoAMano = id;
		cargarDatosThread cargardatosthread = new cargarDatosThread();
		cargardatosthread.execute();
	}

	private void metodoAdd() {

		AlertDialog.Builder alert = new AlertDialog.Builder(contexto);

		alert.setTitle(contexto.getResources().getString(R.string.titulo));
		alert.setMessage(contexto.getResources().getString(R.string.inserta));

		// Set an EditText view to get user input
		final EditText input = new EditText(contexto);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int whichButton) {
				Editable value = input.getText();
				codigoEscritoAMano = value.toString();
				Boolean datoCorrecto = comprobarValorEntrada(codigoEscritoAMano);

				if (!datoCorrecto) {
					Toast.makeText(getApplicationContext(),
							"El número introducido es correcto.",
							Toast.LENGTH_LONG).show();
				} else {
					// Comprobar si existe internet
					if (comprobaciones()) {
						tipoDeBusqueda = 1;
						cargarDatosThread cargardatosthread = new cargarDatosThread();
						cargardatosthread.execute();
					}
				}
			}

			private Boolean comprobarValorEntrada(String numero) {

				if (numero.length() != 12) {
					return false;
				}
				// Comprobar si es numero
				Pattern patron = Pattern.compile("[0-9]{12}");
				Matcher encaja = patron.matcher(numero);
				if (!encaja.find()) {
					return false;
				}

				return true;
			}
		});

		alert.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {

					}
				});

		alert.show();

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		contexto = this;

		// Comprobar si existen datos
		comprobarBBDD();

		// BorrarBBDD();

		// Agregar tarjetas a la pantalla
		listaTarjetasLayout = (ListView) findViewById(R.id.listview);
		adaptadorTarjetas = new AdaptadorTarjetas(this, listaTarjetas, this);
		
		// adapterArray = new ArrayAdapter(contexto,
		// android.R.layout.simple_list_item_1, listaTarjetas);
		listaTarjetasLayout.setAdapter(adaptadorTarjetas);

		// Permisos para Network
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
				.permitAll().build();
		StrictMode.setThreadPolicy(policy);

		// label = (TextView) findViewById(R.id.label);

		// Configuracion para arrancar al detectr tarjeta
		pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		IntentFilter mifare = new IntentFilter(
				(NfcAdapter.ACTION_TECH_DISCOVERED));
		filters = new IntentFilter[] { mifare };
		techs = new String[][] { new String[] { NfcA.class.getName() } };
		adapter = NfcAdapter.getDefaultAdapter(this);
	}

	private void BorrarBBDD() {
		// Abrimos la base de datos en modo lectura
		SQLiteHelper usdbh = new SQLiteHelper(this, "baseDeDatos", null, 1);

		SQLiteDatabase db = usdbh.getReadableDatabase();

		// Si hemos abierto correctamente la base de datos
		if (db != null) {
			// Consultamos el valor esLaPrimeraVez
			db.execSQL("DELETE from Tarjetas;");

			db.close();
		}
	}

	private static void comprobarBBDD() {

		// Abrimos la base de datos en modo lectura
		SQLiteHelper usdbh = new SQLiteHelper(contexto, "baseDeDatos", null, 1);

		SQLiteDatabase db = usdbh.getReadableDatabase();
		
		listaTarjetas.clear();

		// Si hemos abierto correctamente la base de datos
		if (db != null) {
			// Consultamos el valor esLaPrimeraVez
			Cursor c = db.rawQuery("SELECT * from Tarjetas;", null);
			// Nos aseguramos de que existe al menos un registro
			// Nos aseguramos de que existe al menos un registro
			if (c.moveToFirst()) {
				do {
					listaTarjetas.add(new Tarjetas(c.getString(0), c
							.getString(1), c.getString(2), c.getString(3)));
					// Herramientas.getYo().setId(c.getString(0));
				} while (c.moveToNext());
			}
			if (listaTarjetas.size() == 0)
			{
				NfcAdapter nfcAdapter = nfcAdapter = NfcAdapter.getDefaultAdapter(contexto);

				if (nfcAdapter == null) {       
					listaTarjetas.add(new Tarjetas("", "",contexto.getResources().getString(R.string.textoVacio1),""));
				}
				else if (!nfcAdapter.isEnabled()) {
					listaTarjetas.add(new Tarjetas("", "",contexto.getResources().getString(R.string.textoVacio1),""));
				} else {
					listaTarjetas.add(new Tarjetas("", "",contexto.getResources().getString(R.string.textoVacio2),""));
				}

			}

			db.close();
		}
	}

	public static Boolean comprobaciones() {

		// Comprobar Internet
		ConnectivityManager cm = (ConnectivityManager) contexto
				.getSystemService(contexto.CONNECTIVITY_SERVICE);

		NetworkInfo netInfo = cm.getActiveNetworkInfo();

		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			return true;
		}

		Toast.makeText(contexto,
				contexto.getResources().getString(R.string.no_internet),
				Toast.LENGTH_LONG).show();
		return false;
	}

	public void onPause() {
		super.onPause();
		try {
			adapter.disableForegroundDispatch(this);
		} catch (Exception e) {

		}
	}

	public void onResume() {
		super.onResume();
		// Actualizar Listado
		listaTarjetas.clear();
		comprobarBBDD();
		// adapterArray.notifyDataSetChanged();
		adaptadorTarjetas.notifyDataSetChanged();
		try {
			adapter.enableForegroundDispatch(this, pendingIntent, filters,
					techs);
		} catch (Exception e) {

		}
	}

	public void onNewIntent(Intent intent) {
		Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		byte[] id = tag.getId();
		ByteBuffer wrapped = ByteBuffer.wrap(id);
		wrapped.order(ByteOrder.LITTLE_ENDIAN);
		int signedInt = wrapped.getInt();
		number = signedInt & 0xffffffffl;
		// label.setText("Tag detected: " + number);
		// label.setText("Acerque la tarjeta al móvil!");

		// Comprobar si existe internet
		if (comprobaciones()) {
			FlurryAgent.logEvent("Añadir con NFC");
			tipoDeBusqueda = 0;
			cargarDatosThread cargardatosthread = new cargarDatosThread();
			cargardatosthread.execute();
		}

	}

	private static class cargarDatosThread extends AsyncTask<Void, Void, Void> {

		private AlertDialog alert = null, alertNuevaTarjeta = null;
		private Builder ringProgressDialogMapa, insertarNuevaTarjeta;

		public cargarDatosThread() {
			if (android.os.Build.VERSION.SDK_INT > 9) {
				StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
						.permitAll().build();
				StrictMode.setThreadPolicy(policy);
			}
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			cadenaResultado = "";

			ringProgressDialogMapa = new AlertDialog.Builder(contexto);
			ringProgressDialogMapa.setTitle(contexto.getResources().getString(
					R.string.titulo));
			ringProgressDialogMapa.setMessage(contexto.getResources()
					.getString(R.string.localizando));
			ringProgressDialogMapa.setCancelable(false);

			alert = ringProgressDialogMapa.create();

			alert.show();

		}

		@Override
		protected Void doInBackground(Void... accion) {
			buscarCodigoCorrecto();
			semaforoAbierto = true;
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if (cadenaResultado.equals("")) {
				Toast.makeText(contexto,
						"El número introducido es incorrecto.",
						Toast.LENGTH_LONG).show();
			} else {
				if (tipoDeBusqueda != 2) {
					comprobarSiExisteLaTarjetaEnBBDD();
				} else {

				}
			}

			alert.cancel();

		}

		private void comprobarSiExisteLaTarjetaEnBBDD() {
			// Abrimos la base de datos en modo lectura
			SQLiteHelper usdbh = new SQLiteHelper(contexto, "baseDeDatos",
					null, 1);

			SQLiteDatabase db = usdbh.getWritableDatabase();

			// Si hemos abierto correctamente la base de datos
			if (db != null) {
				// Consultamos el valor esLaPrimeraVez
				Cursor c = db.rawQuery("SELECT * from Tarjetas where id="
						+ number, null);
				if (!c.moveToFirst()) {
					insertarNuevaTarjeta = new AlertDialog.Builder(contexto);
					insertarNuevaTarjeta.setTitle(contexto.getResources()
							.getString(R.string.titulo));
					insertarNuevaTarjeta.setMessage(contexto.getResources()
							.getString(R.string.guardarTarjeta));
					insertarNuevaTarjeta.setCancelable(false);
					insertarNuevaTarjeta.setNegativeButton("No",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									Toast.makeText(
											contexto,
											contexto.getResources().getString(
													R.string.saldo)
													+ " " + cadenaResultado,
											Toast.LENGTH_LONG).show();
									dialog.cancel();

								}
							});
					insertarNuevaTarjeta.setPositiveButton("Si",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									insertarTarjeta();

									// Actualizar Listado
									listaTarjetas.clear();
									comprobarBBDD();
									adaptadorTarjetas.notifyDataSetChanged();
									dialog.cancel();
								}
							});

					alertNuevaTarjeta = insertarNuevaTarjeta.create();

					alertNuevaTarjeta.show();
					// Herramientas.getYo().setId(c.getString(0));
				}

				db.close();
			}

		}

		private void insertarTarjeta() {
			try {
				// Abrimos la base de datos en modo lectura
				SQLiteHelper usdbh = new SQLiteHelper(contexto, "baseDeDatos",
						null, 1);

				SQLiteDatabase db = usdbh.getReadableDatabase();

				String codigo;

				// Comprobamos si esta la tarjeta en la bbdd
				if (tipoDeBusqueda == 1) {
					codigo = codigoEscritoAMano;
				} else {
					codigo = numeroCompleto;
				}
				Cursor c = db.rawQuery("SELECT * from Tarjetas where id ='"
						+ codigo + "';", null);
				if (!c.moveToFirst()) {
					db.execSQL("INSERT INTO Tarjetas VALUES ('" + codigo
							+ "','" + cadenaResultado + "','" + titulo + "','"
							+ caducidad + "');");
				} else {
					Toast.makeText(contexto,
							"La tarjeta ya se encuentra almacenada!",
							Toast.LENGTH_LONG).show();
				}
				// fin comprobar si esta en bbdd

				db.close();
			} catch (Exception e) {

			}
		}

		// Metodos auxiliares
		@SuppressWarnings("unused")
		private void buscarCodigoCorrecto() {
			int i = 0;
			boolean encontrado = false;
			if (tipoDeBusqueda == 0) {
				while (!encontrado && i<100) {
					encontrado = analizaCodigoCorrecto(i);
					i++;
				}
			} else {
				encontrado = analizaCodigoCorrecto(0);
			}

		}

		@SuppressWarnings("unused")
		private Boolean analizaCodigoCorrecto(int i) {
			Boolean resultado = false;
			String tiempo = "time=" + System.currentTimeMillis();
			numberString = String.valueOf(number);

			// Comprobar que tenga 10 digitos
			int longitud = numberString.length();
			for (int o = longitud; o < 10; o++) {
				numberString = "0" + numberString;
			}

			String cadena1 = numberString.substring(0, 5);
			String cadena2 = numberString.substring(5, 10);

			if (tipoDeBusqueda == 0) {
				numeroCompleto = cadena1 + String.valueOf(i) + cadena2;
			} else {
				numeroCompleto = codigoEscritoAMano;
			}
			String url = Webpage + numeroCompleto;
			try {

				HttpClient httpClient = new DefaultHttpClient();
				HttpGet get = new HttpGet(url);

				HttpResponse response = httpClient.execute(get);

				// Build up result
				String bodyHtml = EntityUtils.toString(response.getEntity());

				resultado = buscarSaldo(bodyHtml);

				if (resultado) {
					// Capturamos saldo
					Pattern patron = Pattern.compile("\\s+(.+)&euro");
					Matcher encaja = patron.matcher(bodyHtml);
					if (encaja.find()) {
						cadenaResultado = encaja.group(1);
					}
					// Capturamos titulo
					patron = Pattern.compile("tulo:.+&nbsp;(.+)<");
					encaja = patron.matcher(bodyHtml);
					if (encaja.find()) {
						titulo = encaja.group(1);
					}
					// Capturamos caducidad
					patron = Pattern.compile("Caducidad.+&nbsp;(.+)<");
					encaja = patron.matcher(bodyHtml);
					if (encaja.find()) {
						caducidad = encaja.group(1);
					}
				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return resultado;

		}

		private Boolean buscarSaldo(String html) {
			return (html.indexOf("Saldo") != -1);
		}

	}

}
