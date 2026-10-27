package cl.gtoledo.registroclases;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

/** Recibe desde la app web la dirección del archivo del portal y la llave del widget. */
public class ConectarActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Uri u = getIntent() != null ? getIntent().getData() : null;
        String feed = u != null ? u.getQueryParameter("feed") : null;
        String key = u != null ? u.getQueryParameter("key") : null;
        if (feed != null && feed.startsWith("https://") && key != null && key.length() >= 40) {
            Agenda.prefs(this).edit().putString(Agenda.K_FEED, feed).putString(Agenda.K_KEY, key).apply();
            AgendaWidget.refrescar(this, 0);
            AgendaWidget.refrescar(this, 150_000); // el portal recién publicado tarda 1–2 minutos en verse
            Toast.makeText(this, "Widget conectado. Mantén presionada la pantalla de inicio → Widgets → Registro de Clases.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "No se pudo conectar el widget.", Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}
