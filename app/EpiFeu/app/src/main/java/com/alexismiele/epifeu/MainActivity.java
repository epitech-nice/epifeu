package com.alexismiele.epifeu;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HttpClientStack;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.orhanobut.hawk.Hawk;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.CookieHandler;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private RequestQueue queue;
    private String url;
    private Button button;
    private ImageView picture;
    private ImageView image;
    private TextView name;
    private TextView close;
    private View error;

    public static final String PREF_NAME = "params_epifeu";
    private static String firebase_url = "https://epitechnice.page.link/V9Hh";

    static ColorFilter screen(int c) {
        return new LightingColorFilter(0xFFFFFFFF - c, c);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Hawk.init(this).build();

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        url = prefs.getString("url", null);
        //url = "https://epitechnice.page.link/V9Hh";

        java.net.CookieManager manager = new java.net.CookieManager();
        CookieHandler.setDefault( manager  );
        queue = Volley.newRequestQueue(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        button = (Button) findViewById(R.id.button);
        picture = (ImageView) findViewById(R.id.picture);
        image = (ImageView) findViewById(R.id.image);
        name = (TextView) findViewById(R.id.name);
        error = (View) findViewById(R.id.error);
        close = (TextView) findViewById(R.id.close);
        Button errorButton = (Button) findViewById(R.id.error_button);

        setSupportActionBar(toolbar);

        errorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshUrl();
            }
        });
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeState();
            }
        });
        if (url == null) {
            refreshUrl();
        } else {
            connect();
        }
    }

    private void refreshUrl() {
        JSONObject parameters = new JSONObject();
        JsonObjectRequest request2 = new JsonObjectRequest(Request.Method.GET, firebase_url,
                parameters,new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONObject data = response.getJSONObject("data");
                    url = data.getString("ip");
                    SharedPreferences.Editor editor = getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit();
                    editor.putString("url", url);
                    editor.apply();
                    connect();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = super.getHeaders();
                return headers;
            }
        };
        queue.add(request2);
    }

    private void connect() {
        JSONObject parameters = new JSONObject();
        try {
            parameters.put("cookie", CookieManager.getInstance().getCookie("https://intra.epitech.eu"));
        } catch (Exception ignored) {
        }
        JsonObjectRequest request2 = new JsonObjectRequest(Request.Method.POST, url + "login",
                parameters,new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONObject data = response.getJSONObject("data");
                    Picasso.get().load(data.getString("picture")).into(picture);
                    name.setText(data.getString("name"));
                    if (!data.getBoolean("admin")) {
                        button.setEnabled(false);
                    }
                    Calendar rightNow = Calendar.getInstance();
                    int currentHour = rightNow.get(Calendar.HOUR_OF_DAY);
                    int day = rightNow.get(Calendar.DAY_OF_WEEK);
                    if (data.getBoolean("admin") || (day != Calendar.SATURDAY && day != Calendar.SUNDAY && ((currentHour >= 9 && currentHour < 12) || (currentHour >= 14 && currentHour < 18)))) {
                        switch (data.getString("state")) {
                            case "RED":
                                image.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        image.setColorFilter(screen(Color.RED));
                                    }
                                });
                                close.setVisibility(View.INVISIBLE);
                                break;
                            case "GREEN":
                                image.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        image.setColorFilter(screen(Color.GREEN));
                                    }
                                });
                                close.setVisibility(View.INVISIBLE);
                                break;
                            default:
                                image.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        image.setColorFilter(screen(Color.BLACK));
                                    }
                                });
                                close.setVisibility(View.VISIBLE);
                                break;
                        }
                    } else {
                        image.setColorFilter(screen(Color.BLACK));
                        close.setVisibility(View.VISIBLE);
                    }
                    error.setVisibility(View.GONE);
                    final Handler handler = new Handler();
                    Timer    timer = new Timer();
                    TimerTask doAsynchronousTask = new TimerTask() {
                        @Override
                        public void run() {
                            handler.post(new Runnable() {
                                public void run() {
                                    getState();
                                }
                            });
                        }
                    };
                    timer.schedule(doAsynchronousTask, 60000, 60000);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = super.getHeaders();
                if (headers == null || headers.equals(Collections.emptyMap())) {
                    headers = new HashMap<>();
                }
                String sessionId = Hawk.get("connect.sid", "");
                if (sessionId.length() > 0) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("connect.sid");
                    builder.append("=");
                    builder.append(sessionId);
                    if (headers.containsKey("Set-Cookie")) {
                        builder.append("; ");
                        builder.append(headers.get("Set-Cookie"));
                    }
                    headers.put("Set-Cookie", builder.toString());
                }
                return headers;
            }
        };
        queue.add(request2);
    }

    private void changeState() {
        JSONObject parameters = new JSONObject();
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url + "state",
                parameters,new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                JSONObject data = null;
                try {
                    data = response.getJSONObject("data");
                    Calendar rightNow = Calendar.getInstance();
                    int currentHour = rightNow.get(Calendar.HOUR_OF_DAY);
                    int day = rightNow.get(Calendar.DAY_OF_WEEK);
                    if (data.getBoolean("admin") || (day != Calendar.SATURDAY && day != Calendar.SUNDAY && ((currentHour >= 9 && currentHour < 12) || (currentHour >= 14 && currentHour < 18)))) {
                        switch (data.getString("state")) {
                            case "RED":
                                image.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        image.setColorFilter(screen(Color.RED));
                                    }
                                });
                                close.setVisibility(View.INVISIBLE);
                                break;
                            case "GREEN":
                                image.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        image.setColorFilter(screen(Color.GREEN));
                                    }
                                });
                                close.setVisibility(View.INVISIBLE);
                                break;
                            default:
                                image.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        image.setColorFilter(screen(Color.BLACK));
                                    }
                                });
                                close.setVisibility(View.VISIBLE);
                                break;
                        }
                    } else {
                        image.setColorFilter(screen(Color.BLACK));
                        close.setVisibility(View.VISIBLE);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError e) {
                error.setVisibility(View.VISIBLE);

            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = super.getHeaders();
                if (headers == null || headers.equals(Collections.emptyMap())) {
                    headers = new HashMap<>();
                }
                String sessionId = Hawk.get("connect.sid", "");
                if (sessionId.length() > 0) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("connect.sid");
                    builder.append("=");
                    builder.append(sessionId);
                    if (headers.containsKey("Set-Cookie")) {
                        builder.append("; ");
                        builder.append(headers.get("Set-Cookie"));
                    }
                    headers.put("Set-Cookie", builder.toString());
                }
                return headers;
            }
        };
        queue.add(request);
    }

    private void getState() {
        JSONObject parameters = new JSONObject();
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url + "state",
                parameters,new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                JSONObject data = null;
                try {
                    data = response.getJSONObject("data");
                    Calendar rightNow = Calendar.getInstance();
                    int currentHour = rightNow.get(Calendar.HOUR_OF_DAY);
                    int day = rightNow.get(Calendar.DAY_OF_WEEK);
                    if (data.getBoolean("admin") || (day != Calendar.SATURDAY && day != Calendar.SUNDAY && ((currentHour >= 9 && currentHour < 12) || (currentHour >= 14 && currentHour < 18)))) {
                        switch (data.getString("state")) {
                            case "RED":
                                image.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        image.setColorFilter(screen(Color.RED));
                                    }
                                });
                                close.setVisibility(View.INVISIBLE);
                                break;
                            case "GREEN":
                                image.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        image.setColorFilter(screen(Color.GREEN));
                                    }
                                });
                                close.setVisibility(View.INVISIBLE);
                                break;
                            default:
                                image.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        image.setColorFilter(screen(Color.BLACK));
                                    }
                                });
                                close.setVisibility(View.VISIBLE);
                                break;
                        }
                    } else {
                        image.setColorFilter(screen(Color.BLACK));
                        close.setVisibility(View.VISIBLE);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError e) {
                error.setVisibility(View.VISIBLE);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = super.getHeaders();
                if (headers == null || headers.equals(Collections.emptyMap())) {
                    headers = new HashMap<>();
                }
                String sessionId = Hawk.get("connect.sid", "");
                if (sessionId.length() > 0) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("connect.sid");
                    builder.append("=");
                    builder.append(sessionId);
                    if (headers.containsKey("Set-Cookie")) {
                        builder.append("; ");
                        builder.append(headers.get("Set-Cookie"));
                    }
                    headers.put("Set-Cookie", builder.toString());
                }
                return headers;
            }
        };
        queue.add(request);
    }

    private void logout() {
        CookieManager.getInstance().removeAllCookies(new ValueCallback<Boolean>() {
            @Override
            public void onReceiveValue(Boolean aBoolean) {
            }
        });
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST, url + "logout", null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                    }
                }
        );
        queue.add(jsonObjectRequest);
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_main_setting:
                AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                final EditText edittext = new EditText(MainActivity.this);
                edittext.setText(url);
                edittext.setEnabled(false);
                alert.setMessage("Entrez l'url du serveur");
                alert.setTitle("URL");

                alert.setView(edittext);

                alert.setPositiveButton("valider", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        /*url = edittext.getText().toString();
                        SharedPreferences.Editor editor = getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit();
                        editor.putString("url", url);
                        editor.apply();*/
                    }
                });

                alert.setNegativeButton("Annuler", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });

                //alert.show();
                return true;
            case R.id.menu_main_logout:
                logout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}