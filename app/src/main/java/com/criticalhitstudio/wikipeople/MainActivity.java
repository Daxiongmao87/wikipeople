package com.criticalhitstudio.wikipeople;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private KeyListener listener;
    private AutoCompleteTextView nameEntry;
    private ArrayAdapter<String> suggestions;
    private String[] suggestionStrings;
    private CharSequence currentText;
    private ProgressBar progressBar;
    private LinearLayout listItems;
    //Wikipedia needed API
    private String imageSearchURLPrefix = "https://en.wikipedia.org/w/api.php?action=query&titles=";
    private String imageSearchURLSuffix = "&prop=pageimages&format=json&pithumbsize=8000";
    private String imageSearchURLInput;
    private String imageSuggestURLPrefix = "https://en.wikipedia.org/w/api.php?action=opensearch&search=";
    private String imageSuggestURLSuffix = "&limit=5&namespace=0&format=json";
    private String imageSuggestURLInput;
    private String imageURL;// = "https://upload.wikimedia.org/wikipedia/commons/thumb/7/71/Tom_Cruise_avp_2014_4.jpg/212px-Tom_Cruise_avp_2014_4.jpg";
    private String[] history;
    Context mainContext;
    Bitmap photo = null;
    Bitmap background = null;
    AsyncTask searchTask = null;
    AsyncTask suggestTask = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Setup View Items
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainContext = this;
        nameEntry = (AutoCompleteTextView) findViewById(R.id.NameText);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        // Get History
        String[] list = new String[0];
        SharedPreferences  pref = this.getSharedPreferences("history",mainContext.MODE_PRIVATE);
        listItems = (LinearLayout) findViewById(R.id.ListItems);
        if(pref.getString("names","") != "") {
            list = (String[]) Arrays.asList(pref.getString("names", "").split(",")).toArray();
            TextView errorView = (TextView) findViewById(R.id.ErrorText);

            errorView.setText(String.valueOf(list.length));
            for (int i = 0; i < list.length; i++) {
                final Button historyButton = new Button(this);
                historyButton.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                historyButton.setBackgroundColor(Color.WHITE);
                historyButton.setText(list[i]);
                listItems.addView(historyButton);
                historyButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        nameEntry.setText(historyButton.getText());
                        imageSearchURLInput = nameEntry.getText().toString();
                        String imageSearchURLInputEncoded = URLEncoder.encode(imageSearchURLInput);

                        progressBar.setVisibility(View.VISIBLE);
                        if(searchTask != null) searchTask.cancel(true);
                        searchTask = new GetImageURL().execute(imageSearchURLPrefix + imageSearchURLInputEncoded + imageSearchURLSuffix);
                    }
                });
            }
        }
        ////
        //when user hits enter
        nameEntry.addTextChangedListener(textWatcher);
        nameEntry.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    imageSearchURLInput = nameEntry.getText().toString();
                    String imageSearchURLInputEncoded = URLEncoder.encode(imageSearchURLInput);

                    progressBar.setVisibility(View.VISIBLE);
                    if(searchTask != null) searchTask.cancel(true);
                    searchTask = new GetImageURL().execute(imageSearchURLPrefix + imageSearchURLInputEncoded + imageSearchURLSuffix);
                    return true;
                }
                return false;
            }
        });
    }

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            //nada
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            //nada
        }

        @Override
        public void afterTextChanged(Editable s) {
            //asynctask opensearch wiki for suggestions
            imageSuggestURLInput = nameEntry.getText().toString();
            String imageSuggestURLInputEncoded = URLEncoder.encode(imageSuggestURLInput);
            if(suggestTask != null) suggestTask.cancel(true);
            suggestTask = new GetSuggestions().execute(imageSuggestURLPrefix + imageSuggestURLInputEncoded + imageSuggestURLSuffix);


        }

        };
        //get suggestions
        class GetSuggestions extends AsyncTask<String, String, String> {
            String errormsg;
            protected String doInBackground(String... urlString) {

                String data = "";
                try {
                    URL url = new URL(urlString[0]);
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setConnectTimeout(15000);
                    urlConnection.setReadTimeout(15000);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    String line = "";
                    while ((line = reader.readLine()) != null) {
                        data += line + "\n";
                    }
                } catch (Exception e) {
                    data = e.getMessage();
                }

                try {
                    JSONArray jsonArray = new JSONArray(data).getJSONArray(1);
                    suggestionStrings = new String[jsonArray.length()];
                    errormsg = String.valueOf(jsonArray.length());
                    for(int i = 0; i < jsonArray.length(); i++) {
                        String json = jsonArray.getString(i);
                        suggestionStrings[i] = json;
                    }

                } catch (JSONException e) {
                    suggestionStrings = new String[0];
                    errormsg = e.getMessage();
                }
                return errormsg;
                }
            protected void onPostExecute(String data) {
                if(suggestionStrings != null) {
                    suggestions = new ArrayAdapter<String>(mainContext, android.R.layout.simple_list_item_1, suggestionStrings);
                    nameEntry.setAdapter(suggestions);
                    }
                }
            }
        //really get image, not just url
        class GetImageURL extends AsyncTask<String, String, String> {
            String errormsg;
            protected String doInBackground(String... urlString) {
                errormsg = "";
                String data = "";
                try {
                    URL url = new URL(urlString[0]);
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setConnectTimeout(15000);
                    urlConnection.setReadTimeout(15000);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    String line = "";
                    while ((line = reader.readLine()) != null) {
                        data += line + "\n";
                    }
                } catch (Exception e) {
                    data = e.getMessage();
                }
                JSONObject jsonData = null;
                try {
                    jsonData = new JSONObject(data).getJSONObject("query").getJSONObject("pages");
                    if(jsonData.has("-1")) {
                        errormsg = "No results for " + imageSearchURLInput;

                    }
                    else {
                        JSONObject pageData = jsonData.getJSONObject(jsonData.keys().next());
                        if(pageData.has("thumbnail")) {
                            imageURL = pageData.getJSONObject("thumbnail").getString("source");
                        }
                        else errormsg = "No image found for " + imageSearchURLInput;
                    }

                } catch (JSONException e) {
                    errormsg = e.getMessage();
                }
                if(errormsg == "") {
                    try {
                        URL url = new URL(imageURL);
                        URLConnection connection = url.openConnection();
                        connection.setConnectTimeout(15000);
                        connection.setReadTimeout(15000);
                        connection.connect();
                        int lengthofFile = connection.getContentLength();
                        InputStream in = new BufferedInputStream(url.openStream());
                        photo = BitmapFactory.decodeStream(in);
                        background = Bitmap.createScaledBitmap(Bitmap.createScaledBitmap(photo, 16, 16, true), photo.getWidth(), photo.getHeight(), true);
                    } catch (IOException e) {
                        errormsg = e.getMessage();
                    }
                }
                return imageURL;
            }
            @Override
            protected void onPostExecute(String data) {
                TextView errorView = (TextView) findViewById(R.id.ErrorText);
                errorView.setText(errormsg);
                ImageView imagePhoto;
                ImageView imageBackground;
                imagePhoto = (ImageView) findViewById(R.id.imagePhoto);
                imageBackground = (ImageView) findViewById(R.id.imageBackground);
                if(errormsg != "") {
                    imagePhoto.setImageBitmap(null);
                    imageBackground.setImageBitmap(null);
                }
                else {
                    imagePhoto.setImageBitmap(photo);
                    imageBackground.setImageBitmap(background);
                    SharedPreferences pref = mainContext.getSharedPreferences("history",mainContext.MODE_PRIVATE);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString("names",imageSearchURLInput + "," + pref.getString("names",""));
                    editor.apply();
                    final Button historyButton = new Button(mainContext);
                    historyButton.setBackgroundColor(Color.WHITE);
                    historyButton.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                    historyButton.setText(imageSearchURLInput);
                    listItems.addView(historyButton,0);
                    historyButton.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            nameEntry.setText(historyButton.getText());
                            imageSearchURLInput = nameEntry.getText().toString();
                            String imageSearchURLInputEncoded = URLEncoder.encode(imageSearchURLInput);
                            progressBar.setVisibility(View.VISIBLE);
                            if(searchTask != null) searchTask.cancel(true);
                            searchTask = new GetImageURL().execute(imageSearchURLPrefix + imageSearchURLInputEncoded + imageSearchURLSuffix);
                        }
                    });

                }
                progressBar.setVisibility(View.INVISIBLE);
            }
        }
    }
