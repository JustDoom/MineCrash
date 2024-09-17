package com.imjustdoom.minecrash.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.imjustdoom.minecrash.dto.out.ErrorDto;
import com.imjustdoom.minecrash.exception.ErrorResponseException;
import com.imjustdoom.minecrash.exception.HttpConnectException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class NetworkUtil {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final String USER_AGENT = "MineCrash 1.0";

    private static final String BASE_URL = "http://localhost:8080/error/";

    // Get
    private static final URL STATISTICS;

    // Post
    private static final URL CHECK;

    public static int getStatistics() throws IOException {
        JsonObject object = sendGet(STATISTICS);
        if (!object.has("solvedErrors"))
            throw new IOException("Unable to local stats");

        return object.get("solvedErrors").getAsInt();
    }

    public static String[] sendErrorForCheck(String error) throws IOException {
        JsonObject object = sendPost(CHECK, error);

        if (object.has("solution") && object.has("title")) {
            return new String[]{object.get("title").getAsString(), object.get("solution").getAsString()};
        } else if (object.has("response")) {
            return new String[]{object.get("response").getAsString()};
        }

        throw new IOException("Oh no something went wrong");
    }

    private static JsonObject sendGet(URL url) throws IOException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", USER_AGENT);
        int responseCode = con.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            JsonObject object = GSON.fromJson(reader, JsonElement.class).getAsJsonObject();
            reader.close();
            return object;
        } else {
            throw new IOException("Failed to connect to the endpoint");
        }
    }

    private static JsonObject sendPost(URL url, String body) throws IOException {
        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", USER_AGENT);
            con.setRequestProperty("Content-Type", "application/json; utf-8");
            con.setRequestProperty("Accept", "application/json");

            // For POST only - START
            con.setDoOutput(true);
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = GSON.toJson(ErrorDto.create(body)).getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            // For POST only - END
            int responseCode = con.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) { //success
                BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                JsonObject object = GSON.fromJson(reader, JsonElement.class).getAsJsonObject();
                reader.close();
                return object;
            } else if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                JsonObject object = GSON.fromJson(reader, JsonElement.class).getAsJsonObject();
                reader.close();

                if (object.has("error")) {
                    throw new ErrorResponseException(object.get("error").getAsString());
                } else {
                    throw new IOException();
                }
            } else {
                BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                JsonObject object = GSON.fromJson(reader, JsonElement.class).getAsJsonObject();
                reader.close();

                if (object.has("message")) {
                    throw new ErrorResponseException(object.get("message").getAsString());
                } else {
                    throw new IOException();
                }
            }
        } catch (IOException exception) {
            throw new HttpConnectException();
        }
    }

    static {
        try {
            STATISTICS = URI.create(BASE_URL + "statistics").toURL();
            CHECK = URI.create(BASE_URL + "check").toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
