package org.ajeet.sheets;

import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * Resolves Sheets API credentials, in priority order:
 * <ol>
 *   <li>Service-account key file — SERVICE_ACCOUNT_KEY_PATH or
 *       GOOGLE_APPLICATION_CREDENTIALS pointing to a JSON key.</li>
 *   <li>OAuth "Desktop app" client — OAUTH_CLIENT_SECRET_PATH or
 *       ./credentials.json; opens a browser for consent on first run and
 *       caches tokens in ./tokens/. Use this when org policy blocks
 *       service-account key creation.</li>
 *   <li>Application Default Credentials — gcloud / workload identity.</li>
 * </ol>
 */
public final class SheetsAuth {

    private SheetsAuth() {
    }

    public static HttpRequestInitializer resolve() throws IOException, GeneralSecurityException {
        String saPath = env("SERVICE_ACCOUNT_KEY_PATH", "GOOGLE_APPLICATION_CREDENTIALS");
        if (saPath != null && new File(saPath).isFile()) {
            System.out.println("[auth] service-account key: " + saPath);
            GoogleCredentials creds = GoogleCredentials
                    .fromStream(new FileInputStream(saPath))
                    .createScoped(List.of(SheetsScopes.SPREADSHEETS));
            return new HttpCredentialsAdapter(creds);
        }

        String oauthPath = env("OAUTH_CLIENT_SECRET_PATH");
        File oauthFile = oauthPath != null ? new File(oauthPath) : new File("credentials.json");
        if (oauthFile.isFile()) {
            System.out.println("[auth] OAuth desktop flow: " + oauthFile
                    + " (browser consent on first run; token cached in tokens/)");
            var transport = GoogleNetHttpTransport.newTrustedTransport();
            GoogleClientSecrets secrets = GoogleClientSecrets.load(
                    GsonFactory.getDefaultInstance(),
                    new InputStreamReader(new FileInputStream(oauthFile)));
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    transport, GsonFactory.getDefaultInstance(), secrets,
                    List.of(SheetsScopes.SPREADSHEETS))
                    .setDataStoreFactory(new FileDataStoreFactory(new File("tokens")))
                    .setAccessType("offline")
                    .build();
            // Credential implements HttpRequestInitializer.
            return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver())
                    .authorize("user");
        }

        System.out.println("[auth] Application Default Credentials");
        return new HttpCredentialsAdapter(GoogleCredentials.getApplicationDefault()
                .createScoped(List.of(SheetsScopes.SPREADSHEETS)));
    }

    private static String env(String... names) {
        for (String n : names) {
            String v = System.getenv(n);
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }
}
