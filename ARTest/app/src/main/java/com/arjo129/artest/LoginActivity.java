package com.arjo129.artest;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.arjo129.artest.datacollection.WifiFingerprint;
import com.arjo129.artest.datacollection.WifiFingerprintList;
import com.arjo129.artest.indoorLocation.SecurityProvider;
import com.loopj.android.http.*;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

/**
 * Done by Chelsey
 */
public class LoginActivity extends AppCompatActivity {
    EditText admin_editText, password_editText;
    Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        admin_editText = findViewById(R.id.admin_text);
        password_editText = findViewById(R.id.password_text);
        loginButton = findViewById(R.id.login_button);
        loginButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {

                String username = admin_editText.getText().toString();
                String password = password_editText.getText().toString();
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("username", username);
                    obj.put("password", password);
                    StringEntity ent = new StringEntity(obj.toString());
                    AsyncHttpClient client = new AsyncHttpClient();
                    client.setSSLSocketFactory(SecurityProvider.getSocketFactory(LoginActivity.this));
                    client.post(getApplication(), getString(R.string.server_url)+"session_token",
                            ent, "application/json", new JsonHttpResponseHandler() {
                                @Override
                                public void onSuccess(int statusCode, Header[] headers, JSONObject responseBody) {
                                    try {
                                        if (responseBody.get("status").equals("ok")) {
                                            //Correct
                                            String session_id = (String)responseBody.get("session_id");
                                            String session_secret = (String)responseBody.get("session_secret");
                                            Intent collect_data = new Intent(getApplicationContext(),CollectData.class);
                                            collect_data.putExtra("session_id", session_id);
                                            collect_data.putExtra("session_secret", session_secret);
                                            WifiFingerprintList wifiFingerprintList = WifiFingerprintList.getInstance();
                                            wifiFingerprintList.session_id = session_id;
                                            wifiFingerprintList.session_secret = session_secret;
                                            startActivity(collect_data);
                                        } else {
                                            //'Tis not working
                                            Log.d("LoginActivity", "Bad user");
                                            Toast.makeText(LoginActivity.this, "Wrong username or password", Toast.LENGTH_SHORT).show();
                                        }
                                    } catch(JSONException j){
                                        Log.d("LoginActivity", "JSON problem");
                                        Toast.makeText(LoginActivity.this,"Server sent weird response",Toast.LENGTH_LONG);
                                    }
                                }
                            });

                } catch (Exception j) {
                    Log.d("LoginActivity","input problem");
                    Toast.makeText(LoginActivity.this, "error processing the input", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

}
