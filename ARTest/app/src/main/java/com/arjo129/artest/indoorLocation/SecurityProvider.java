package com.arjo129.artest.indoorLocation;

import android.content.Context;

import com.arjo129.artest.R;

import java.io.InputStream;
import java.security.KeyStore;

import cz.msebera.android.httpclient.conn.ssl.SSLSocketFactory;


public class SecurityProvider {
    public static SSLSocketFactory getSocketFactory(Context ctx) {
        try {
            // Get an instance of the Bouncy Castle KeyStore format
            KeyStore trusted = KeyStore.getInstance("BKS");
            // Get the raw resource, which contains the keystore with
            // your trusted certificates (root and any intermediate certs)
            InputStream in = ctx.getResources().openRawResource(R.raw.publickey); //name of your keystore file here
            try {
                // Initialize the keystore with the provided trusted certificates
                // Provide the password of the keystore
                trusted.load(in, "ch1aR2wF".toCharArray());
            } finally {
                in.close();
            }
            // Pass the keystore to the SSLSocketFactory. The factory is responsible
            // for the verification of the server certificate.
            SSLSocketFactory sf = new SSLSocketFactory(trusted);
            // Hostname verification from certificate
            // http://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html#d4e506
            sf.setHostnameVerifier(SSLSocketFactory.STRICT_HOSTNAME_VERIFIER); // This can be changed to less stricter verifiers, according to need
            return sf;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

}
