package de.htw.ai.busbunching;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.content.SharedPreferences;
import android.location.Location;

import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Layout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;


//public class MainActivity extends AppCompatActivity implements LocationHandler.LocationHandlerListener, SharedPreferences.Editor {
public class MainActivity extends AppCompatActivity implements LocationHandler.LocationHandlerListener {

    private Button start_button;
    private EditText busline_text;
    private LocationHandler locationHandler;

    List<Route> routes = new ArrayList<>();
    private Route currentRoute;
    private String currentRouteId;
    private long currentJourneyId;
    private String buslinieId;
    private String deviceId = "";

    private static String uniqueID = null;
    private static final String PREF_UNIQUE_ID = "PREF_UNIQUE_ID";

    private static AsyncHttpClient httpClient = new AsyncHttpClient();

    private static final String TAG = "MainActivity";
    //Error den wir erhalten wenn der user nicht die korrekte version der Google Play Services besitzt
    private static final int ERROR_DIALOG_REQUEST = 9001;

    /*

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:

                    mTextMessage.setText(R.string.title_home);
                    return true;
                case R.id.navigation_map:
                    Intent intentMap = new Intent(MainActivity.this, MapsActivity.class);
                    startActivity(intentMap);
                    mTextMessage.setText(R.string.map);
                    return true;
                case R.id.navigation_credits:
                    Intent intentCredits = new Intent(MainActivity.this, CreditsActivity.class);
                    startActivity(intentCredits);
                    mTextMessage.setText(R.string.credits);
                    return true;
            }
            return false;
        }
    };

    */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


//        uniqueID = id(this);
//        writeToFile("sdlfkjslk", this);
//
//        System.out.println("uniqueID onCreate after initialisation: " + uniqueID);
//        System.out.println("PREF_UNIQUE_ID" + PREF_UNIQUE_ID);
//
//        Toast.makeText(this, "uniqueID onCreate after initialisation: " + uniqueID, Toast.LENGTH_LONG).show();



        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        //MenuItem an der Stelle 0 und angeben das es ausgewählt ist (setChecked(true)
        Menu menu = navigation.getMenu();
        MenuItem menuItem = menu.getItem(0);
        menuItem.setChecked(true);
        navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {

                    case R.id.navigation_home:
                        //Hier muss nix hin da wir uns schon in Home befinden
                        break;

                    case R.id.navigation_map:
                        Intent intentMap = new Intent(MainActivity.this, MapsActivity.class);
                        intentMap.putExtra("JOURNEYID", currentJourneyId);
                        intentMap.putExtra("ROUTEID", currentRouteId);
                        startActivity(intentMap);
                        break;

                    case R.id.navigation_credits:
                        Intent intentCredits = new Intent(MainActivity.this, CreditsActivity.class);
                        startActivity(intentCredits);
                        break;
                }
                return false;
            }
        });

        locationHandler = LocationHandler.createInstance(this, 10000);
        locationHandler.askForPermission(this);
        locationHandler.addListener(this);
        deviceId = locationHandler.getDeviceID();

        configureButton(this);
    }

    public void configureButton(final Context context) {

        start_button = findViewById(R.id.start_button);
        busline_text = findViewById(R.id.busline);
        //start_button.setOnClickListener(view -> locationHandler.startLocationHandler());
        start_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buslinieId = busline_text.getText().toString();
                locationHandler.startLocationHandler();
                busline_text.setText("");
//                showDialog();
                getRouteDetail(buslinieId);
                //Dialog
            }
        });

    }

    //Bestätigen das der User die korrekte Version der googlePlayServices besitzt um googlemaps zu nutzen Checking the verison
    public Boolean checkServices() {
        Log.d(TAG, "checkServices: checking google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);

        if (available == ConnectionResult.SUCCESS) {
            //everything is fine alles ok
            Log.d(TAG, "checkServices: Google Play Services is working");
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            //Error occured but we can resolve it we can fix it
            Log.d(TAG, "checkingServices: an error occured but it can be fixed ");
            //Take the error that was thrown and google will give us a dialog where we can find that solution
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        } else {
            //We cant solve the Error
            Toast.makeText(this, "checkServices: You cant make map request", Toast.LENGTH_SHORT).show();
            return false;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case 101: {
                // If request is cancelled, the result arrays are empty.
                if (!(grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED)) {
                    if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
                        Toast.makeText(this, "Access denied", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    @Override
    public void onLocationUpdate(Location location) {
        Toast.makeText(this, location.getLatitude() + " " + location.getLongitude(), Toast.LENGTH_SHORT).show();
        try {
            UpdateCurrentPosition(location);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void getRouteDetail(String ref) {
        httpClient.get(this, "http://h2650399.stratoserver.net:4545/api/v1/route/" + ref, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    JSONArray allRoute = new JSONArray(new String(responseBody));
                    for(int i=0; i< allRoute.length();i++) {
                        Gson gson = new Gson();
                        JSONObject jsonRoute = allRoute.getJSONObject(i);
                        Route route = gson.fromJson(String.valueOf(jsonRoute),Route.class);
                        routes.add(route);
                    }
                    showDialog();
                    System.out.println(routes);
//                    System.out.println("routes.length: " + routes.size());

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                error.printStackTrace();
                System.out.println("Failed success " + statusCode);
            }
        });
    }


    private void putRouteDetail() throws JSONException, UnsupportedEncodingException {
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("ref", currentRouteId);
        jsonParam.put("routeId", currentJourneyId);
        jsonParam.put("time", System.currentTimeMillis());
//        jsonParam.put("position", "test");

        httpClient.put(this, "http://h2650399.stratoserver.net:4545/api/v1/vehicle/" + deviceId, new StringEntity(jsonParam.toString()), "application/json", new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                System.out.println("PUT success");
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                error.printStackTrace();
                System.out.println("PUT Failed " + statusCode);
            }
        });
    }


    private void UpdateCurrentPosition(final Location location) throws JSONException, UnsupportedEncodingException {
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("ref", currentRouteId);
        jsonParam.put("routeId", currentJourneyId);
        jsonParam.put("time", System.currentTimeMillis());
        JSONObject positionParam = new JSONObject();
        positionParam.put("lng", location.getLongitude());
        positionParam.put("lat", location.getLatitude());
        jsonParam.put("position", positionParam);

        httpClient.put(this, "http://h2650399.stratoserver.net:4545/api/v1/vehicle/" + deviceId, new StringEntity(jsonParam.toString()), "application/json", new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                System.out.println("PUT UPDATE success");
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                error.printStackTrace();
                System.out.println("PUT UPDATE Failed " + statusCode);
            }
        });
    }

    private void showDialog(/*Route[] route*/) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        View view = getLayoutInflater().inflate(R.layout.dialog_route, null);

        CharSequence [] routeNames = new CharSequence[routes.size()];
        for (int i = 0 ; i < routes.size(); i++) {
            routeNames[i] = routes.get(i).getName();
        }

        builder.setItems(routeNames,
            new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
                currentRoute = routes.get(i);
                currentJourneyId = currentRoute.getId();
                currentRouteId = currentRoute.getRef();
                Toast.makeText(MainActivity.this, "clicked " + i + " currentRoute: " + currentRoute, Toast.LENGTH_LONG).show();
                try {
                    putRouteDetail();
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                routes.clear();
            }
        });

        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                routes.clear();
            }
        });
        dialog.show();
    }

    private void writeToFile(String data,Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("config.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

//    public synchronized static String id(Context context) {
//        if (uniqueID == null) {
//            SharedPreferences sharedPrefs = context.getSharedPreferences(
//                    PREF_UNIQUE_ID, Context.MODE_PRIVATE);
//            uniqueID = sharedPrefs.getString(PREF_UNIQUE_ID, null);
//            if (uniqueID == null) {
//                //uniqueID = UUID.randomUUID().toString();
//                uniqueID = Settings.Secure.getString(context.getContentResolver(),
//                Settings.Secure.ANDROID_ID);
//                SharedPreferences.Editor editor = sharedPrefs.edit();
//                editor.putString(PREF_UNIQUE_ID, uniqueID);
//                editor.commit();
//            }
//        }
//        return uniqueID;
//    }
//
//    @Override
//    public SharedPreferences.Editor putString(String s, @Nullable String s1) {
//        return null;
//    }
//
//    @Override
//    public SharedPreferences.Editor putStringSet(String s, @Nullable Set<String> set) {
//        return null;
//    }
//
//    @Override
//    public SharedPreferences.Editor putInt(String s, int i) {
//        return null;
//    }
//
//    @Override
//    public SharedPreferences.Editor putLong(String s, long l) {
//        return null;
//    }
//
//    @Override
//    public SharedPreferences.Editor putFloat(String s, float v) {
//        return null;
//    }
//
//    @Override
//    public SharedPreferences.Editor putBoolean(String s, boolean b) {
//        return null;
//    }
//
//    @Override
//    public SharedPreferences.Editor remove(String s) {
//        return null;
//    }
//
//    @Override
//    public SharedPreferences.Editor clear() {
//        return null;
//    }
//
//    @Override
//    public boolean commit() {
//        return false;
//    }
//
//    @Override
//    public void apply() {
//
//    }
}
