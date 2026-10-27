package cl.gtoledo.registroclases;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;

import com.google.androidbrowserhelper.trusted.LauncherActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** Widget "Mi día": clase actual o siguiente, las que quedan hoy, entrega próxima y tres accesos. */
public class AgendaWidget extends AppWidgetProvider {
    static final String ACTION_REFRESH = "cl.gtoledo.registroclases.REFRESH";
    private static final String[] DIAS = {"", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        dibujar(ctx);
        descargarEnSegundoPlano(ctx);
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String a = intent.getAction();
        if (ACTION_REFRESH.equals(a)) {
            dibujar(ctx);
            descargarEnSegundoPlano(ctx);
            return;
        }
        if (Intent.ACTION_TIME_CHANGED.equals(a) || Intent.ACTION_TIMEZONE_CHANGED.equals(a)
                || Intent.ACTION_DATE_CHANGED.equals(a)) {
            dibujar(ctx);
            return;
        }
        super.onReceive(ctx, intent);
    }

    private void descargarEnSegundoPlano(Context ctx) {
        final PendingResult pr = goAsync();
        final Context app = ctx.getApplicationContext();
        new Thread(() -> {
            try {
                Agenda.descargar(app);
                dibujar(app);
            } finally {
                pr.finish();
            }
        }).start();
    }

    /** Pide una actualización del widget (por ejemplo, después de conectarlo). */
    static void refrescar(Context ctx, long enMs) {
        Intent i = new Intent(ctx, AgendaWidget.class).setAction(ACTION_REFRESH);
        if (enMs <= 0) {
            ctx.sendBroadcast(i);
            return;
        }
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 7, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.set(AlarmManager.RTC, System.currentTimeMillis() + enMs, pi);
    }

    private static PendingIntent abrir(Context ctx, String ir, int code) {
        String url = ctx.getString(R.string.app_url) + "?ir=" + ir;
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .setClass(ctx, LauncherActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(ctx, code, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static void dibujar(Context ctx) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, AgendaWidget.class));
        if (ids == null || ids.length == 0) return;
        RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.widget_agenda);

        v.setOnClickPendingIntent(R.id.btn_registrar, abrir(ctx, "registrar", 1));
        v.setOnClickPendingIntent(R.id.btn_notas, abrir(ctx, "notas", 2));
        v.setOnClickPendingIntent(R.id.btn_portal, abrir(ctx, "portal", 3));
        Intent ref = new Intent(ctx, AgendaWidget.class).setAction(ACTION_REFRESH);
        v.setOnClickPendingIntent(R.id.btn_refrescar, PendingIntent.getBroadcast(ctx, 4, ref,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));

        Agenda ag = Agenda.conectado(ctx) ? Agenda.cargar(ctx) : null;
        if (!Agenda.conectado(ctx)) {
            v.setTextViewText(R.id.tv_etiqueta, "Widget sin conectar");
            v.setTextViewText(R.id.tv_curso, "Registro de Clases");
            v.setTextViewText(R.id.tv_resto, "Abre la app → Tarjetas y portal → Portal → Conectar widget");
            v.setViewVisibility(R.id.tv_resto, View.VISIBLE);
            v.setViewVisibility(R.id.tv_entrega, View.GONE);
            v.setOnClickPendingIntent(R.id.zona_clase, abrir(ctx, "widget", 5));
        } else if (ag == null) {
            v.setTextViewText(R.id.tv_etiqueta, "Esperando datos");
            v.setTextViewText(R.id.tv_curso, "Registro de Clases");
            v.setTextViewText(R.id.tv_resto, "Publica el portal desde la app y toca ↻ en un par de minutos.");
            v.setViewVisibility(R.id.tv_resto, View.VISIBLE);
            v.setViewVisibility(R.id.tv_entrega, View.GONE);
            v.setOnClickPendingIntent(R.id.zona_clase, abrir(ctx, "registrar", 5));
        } else {
            llenar(ctx, v, ag);
        }
        mgr.updateAppWidget(ids, v);
    }

    private static void llenar(Context ctx, RemoteViews v, Agenda ag) {
        Calendar now = Calendar.getInstance();
        int hoy = Agenda.diaApp(now);
        int ahora = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

        List<Agenda.Clase> deHoy = new ArrayList<>();
        for (Agenda.Clase c : ag.clases) if (c.dia == hoy) deHoy.add(c);

        Agenda.Clase foco = null;
        String etiqueta = null;
        int desde = -1;
        for (int i = 0; i < deHoy.size(); i++) {
            Agenda.Clase c = deHoy.get(i);
            if (c.ini() <= ahora && ahora < c.end()) { foco = c; etiqueta = "Ahora · " + c.inicio + "–" + c.fin; desde = i + 1; break; }
            if (c.ini() > ahora) { foco = c; etiqueta = "Siguiente · " + c.inicio; desde = i + 1; break; }
        }
        if (foco == null) {
            // Primera clase de los próximos días
            for (int s = 1; s <= 7 && foco == null; s++) {
                int d = (hoy - 1 + s) % 7 + 1;
                for (Agenda.Clase c : ag.clases) if (c.dia == d) { foco = c; break; }
                if (foco != null) etiqueta = (s == 1 ? "Mañana" : DIAS[d]) + " · " + foco.inicio;
            }
        }

        if (foco == null) {
            v.setTextViewText(R.id.tv_etiqueta, "Sin clases en el horario");
            v.setTextViewText(R.id.tv_curso, "Registro de Clases");
            v.setOnClickPendingIntent(R.id.zona_clase, abrir(ctx, "horario", 5));
        } else {
            v.setTextViewText(R.id.tv_etiqueta, etiqueta);
            v.setTextViewText(R.id.tv_curso, foco.curso);
            v.setOnClickPendingIntent(R.id.zona_clase, abrir(ctx, "clase&id=" + Uri.encode(foco.id), 5));
        }

        StringBuilder resto = new StringBuilder();
        int n = 0;
        if (desde >= 0) for (int i = desde; i < deHoy.size() && n < 2; i++, n++) {
            Agenda.Clase c = deHoy.get(i);
            if (resto.length() > 0) resto.append('\n');
            resto.append(c.inicio).append("   ").append(c.curso);
        }
        if (desde >= 0 && n == 0) resto.append("Última clase del día");
        v.setTextViewText(R.id.tv_resto, resto.toString());
        v.setViewVisibility(R.id.tv_resto, resto.length() > 0 ? View.VISIBLE : View.GONE);

        // Entrega que vence hoy o mañana
        Calendar finManana = (Calendar) now.clone();
        finManana.add(Calendar.DAY_OF_YEAR, 1);
        finManana.set(Calendar.HOUR_OF_DAY, 23);
        finManana.set(Calendar.MINUTE, 59);
        Calendar finHoy = (Calendar) now.clone();
        finHoy.set(Calendar.HOUR_OF_DAY, 23);
        finHoy.set(Calendar.MINUTE, 59);
        String chip = null;
        int cuantas = 0;
        for (Agenda.Entrega e : ag.entregas) {
            if (e.limite < now.getTimeInMillis() || e.limite > finManana.getTimeInMillis()) continue;
            cuantas++;
            if (chip == null) chip = e.nombre + (e.limite <= finHoy.getTimeInMillis() ? " vence hoy" : " vence mañana");
        }
        if (chip != null && cuantas > 1) chip += " (+" + (cuantas - 1) + ")";
        if (chip != null) {
            v.setTextViewText(R.id.tv_entrega, chip);
            v.setViewVisibility(R.id.tv_entrega, View.VISIBLE);
        } else {
            v.setViewVisibility(R.id.tv_entrega, View.GONE);
        }
    }
}
