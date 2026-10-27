package cl.gtoledo.registroclases;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Datos del widget: se descargan del archivo del portal (parte "agenda", cifrada con la llave del widget). */
final class Agenda {
    static final String PREFS = "widget";
    static final String K_FEED = "feed";
    static final String K_KEY = "key";
    static final String K_JSON = "agenda";
    static final String K_FETCHED = "fetched";

    static final class Clase {
        String id, curso, inicio, fin;
        int dia; // 1 = lunes … 7 = domingo
        int ini() { return min(inicio); }
        int end() { return min(fin); }
    }
    static final class Entrega {
        String nombre, curso;
        long limite;
    }

    final List<Clase> clases = new ArrayList<>();
    final List<Entrega> entregas = new ArrayList<>();

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static boolean conectado(Context c) {
        SharedPreferences p = prefs(c);
        return p.getString(K_FEED, null) != null && p.getString(K_KEY, null) != null;
    }

    static int min(String hhmm) {
        try {
            String[] p = hhmm.split(":");
            return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
        } catch (Exception e) {
            return 0;
        }
    }

    /** Día de hoy con la numeración de la app: 1 = lunes … 7 = domingo. */
    static int diaApp(Calendar cal) {
        return (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1;
    }

    static Agenda cargar(Context c) {
        Agenda a = new Agenda();
        String json = prefs(c).getString(K_JSON, null);
        if (json == null) return null;
        try {
            JSONObject o = new JSONObject(json);
            JSONArray cl = o.optJSONArray("clases");
            if (cl != null) for (int i = 0; i < cl.length(); i++) {
                JSONObject x = cl.getJSONObject(i);
                Clase k = new Clase();
                k.id = x.optString("id");
                k.dia = x.optInt("d");
                k.inicio = x.optString("i", "00:00");
                k.fin = x.optString("f", k.inicio);
                k.curso = x.optString("c");
                a.clases.add(k);
            }
            JSONArray en = o.optJSONArray("entregas");
            if (en != null) for (int i = 0; i < en.length(); i++) {
                JSONObject x = en.getJSONObject(i);
                Entrega e = new Entrega();
                e.nombre = x.optString("n");
                e.curso = x.optString("c");
                e.limite = x.optLong("t");
                a.entregas.add(e);
            }
        } catch (Exception e) {
            return null;
        }
        Collections.sort(a.clases, (p, q) -> p.dia != q.dia ? p.dia - q.dia : p.ini() - q.ini());
        Collections.sort(a.entregas, (p, q) -> Long.compare(p.limite, q.limite));
        return a;
    }

    /** Descarga el archivo del portal y guarda la agenda descifrada. Devuelve true si cambió algo. */
    static boolean descargar(Context c) {
        SharedPreferences p = prefs(c);
        String feed = p.getString(K_FEED, null);
        String key = p.getString(K_KEY, null);
        if (feed == null || key == null) return false;
        HttpURLConnection con = null;
        try {
            URL url = new URL(feed + (feed.contains("?") ? "&" : "?") + "t=" + System.currentTimeMillis());
            con = (HttpURLConnection) url.openConnection();
            con.setConnectTimeout(6000);
            con.setReadTimeout(6000);
            con.setUseCaches(false);
            if (con.getResponseCode() != 200) return false;
            InputStream in = con.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            JSONObject data = new JSONObject(out.toString("UTF-8"));
            JSONObject ag = data.optJSONObject("agenda");
            if (ag == null) return false;
            byte[] k = Base64.decode(key, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
            byte[] iv = Base64.decode(ag.getString("iv"), Base64.DEFAULT);
            byte[] ct = Base64.decode(ag.getString("ct"), Base64.DEFAULT);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(k, "AES"), new GCMParameterSpec(128, iv));
            String json = new String(cipher.doFinal(ct), "UTF-8");
            new JSONObject(json); // valida
            p.edit().putString(K_JSON, json).putLong(K_FETCHED, System.currentTimeMillis()).apply();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (con != null) con.disconnect();
        }
    }

    static String hora(long ms) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);
        return String.format(Locale.US, "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }
}
