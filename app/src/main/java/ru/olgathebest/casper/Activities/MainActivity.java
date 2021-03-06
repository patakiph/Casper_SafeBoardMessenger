package ru.olgathebest.casper.activities;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.math.BigInteger;

import ru.olgathebest.casper.database.AndroidDatabaseManager;
import ru.olgathebest.casper.utils.EmailValidator;
import ru.olgathebest.casper.MessengerNDK;
import ru.olgathebest.casper.callbacks.OnLogin;
import ru.olgathebest.casper.R;
import ru.olgathebest.casper.utils.Coding;

public class MainActivity extends Activity implements OnLogin {
    private EditText loginField;
    private EditText passField;
    private CheckBox isSecureCheckBox;
    private boolean isSecure = true;
    private Intent intent;
    private String login = "patakiph";
    private String pass = "bad girl";
    public static final String PUBLIC_KEY = "Public Key";
    MessengerNDK messengerNDK = MessengerNDK.getMessengerNDK();
    private String serverUrl = "93.188.161.205";//"195.123.211.113";//"192.168.1.2";//"172.20.10.5"; //"93.188.161.205"
    private int serverPort = 5222;
    private ProgressDialog pd;
    Button db;
    private EmailValidator emailValidator = new EmailValidator();
    SharedPreferences SP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        loginField = (EditText) findViewById(R.id.user);
        passField = (EditText) findViewById(R.id.pwd);
        db = (Button) findViewById(R.id.db);
        isSecureCheckBox = (CheckBox) findViewById(R.id.security);

        db.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Intent dbmanager = new Intent(getApplicationContext(), AndroidDatabaseManager.class);
                startActivity(dbmanager);
            }
        });
        messengerNDK.setContext(getApplicationContext());
        messengerNDK.setMainActivity(this);
        SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
//        Intent i = new Intent(this, MyPreferencesActivity.class);
//        startActivity(i);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_settings:
                //Toast.makeText(this, "ADD!", Toast.LENGTH_SHORT).show();
                Intent i = new Intent(this, MyPreferencesActivity.class);
                startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    @Override
    public void onStart() {
        super.onStart();
        startService(new Intent(this, MessengerNDK.class));
        messengerNDK.addOnLogin(this);
        serverUrl = SP.getString("ip","192.168.1.2");
        serverPort = Integer.parseInt(SP.getString("port","5222"));
        if (SP.getBoolean("database",false))
            db.setVisibility(View.VISIBLE);
        else
            db.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        messengerNDK.deleteOnLogin(this);
    }

    public void login(View view) {
       // notification("jjj","jkj",false);
        login = loginField.getText().toString();

        pass = passField.getText().toString();
        isSecure = isSecureCheckBox.isChecked();
        if (!emailValidator.validate(login)){
            Toast toast = Toast.makeText(getApplicationContext(),
                    "Use your email!", Toast.LENGTH_SHORT);
            toast.show();
        }
        else if (login.equals("") || pass.equals("")) {
            Toast toast = Toast.makeText(getApplicationContext(),
                    "All fields should be filled in!", Toast.LENGTH_SHORT);
            toast.show();
        } else if (!isNetworkAvailable()) {
            Toast toast = Toast.makeText(getApplicationContext(),
                    "No Internet connection!", Toast.LENGTH_SHORT);
            toast.show();
        } else {
            pd = new ProgressDialog(this);
            pd.setMax(10000);
            pd.show(this, "Loading", "Wait while loading...");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        messengerNDK.nativeConnect(serverUrl, serverPort);
                        BigInteger keyb = BigInteger.ONE;
                        byte[] key;
                        if (isSecure) {
                            keyb = messengerNDK.getPublicKey();
                        }
                            String str = keyb.toString();
                            key = str.getBytes();

                        messengerNDK.nativeLogin(Coding.encode(login), Coding.encode(pass), key);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public void onLogin(int result) {
        if (result == 0) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    intent = new Intent(getApplicationContext(), ListUsersActivity.class);
                    intent.putExtra(PUBLIC_KEY,messengerNDK.getPublicKey().toString());
                    messengerNDK.setCurrentUser(login);
                    startActivity(intent);

                }
            });
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast toast = Toast.makeText(getApplicationContext(),
                            "Connection error, check server is up!", Toast.LENGTH_SHORT);
                    toast.show();
                    /*костыль, тут нужно как-то останавливать прогресс бар*/
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                }
            });
        }

    }
    public void notification(String user, String str, boolean userList) {
        String name = (user.indexOf('@') > 0) ? user.substring(0, user.indexOf('@')) : user;
        Context context = getApplicationContext();
        Intent notificationIntent = new Intent(context, userList ? ListUsersActivity.class : MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        Resources res = context.getResources();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_launcher)
                .setTicker(name + ": " + ((str.length() < 30) ? str : (str.substring(0, 30) + "...")))
                .setAutoCancel(true)
                .setContentTitle(name)
                .setContentText(str)
                .setOnlyAlertOnce(true);
        Notification notification = builder.build();
        notification.defaults = Notification.DEFAULT_VIBRATE;
        notification.flags = notification.flags | Notification.FLAG_ONLY_ALERT_ONCE;
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(1, notification);
    }
}
