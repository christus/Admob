package com.christus.release1.admobApp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdCallback;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.mukesh.OnOtpCompletionListener;
import com.mukesh.OtpView;
import com.onesignal.OneSignal;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;



/** Main Activity. Inflates main activity xml. */
public class MainActivity extends Activity {

    String DATA_URL = "https://christus.github.io/Admob/data.json";

    //Test Ad
    private static final String AD_UNIT_ID = BuildConfig.AD_UNIT_ID;

    //Prod Ad
    //private static final String AD_UNIT_ID = "ca-app-pub-8752511525491597/8926447535";

    int showAdAfter = BuildConfig.showAdAfter;

    private static final long COUNTER_TIME = 10;
    private static final int GAME_OVER_REWARD = 0;

    private int coinCount = 0;
    private TextView coinCountText, puchTxt;
    private CountDownTimer countDownTimer;
    private boolean gameOver;
    private boolean gamePaused;

    private RewardedAd rewardedAd;
    private AdView mAdView;

    private Button retryButton, nextQa, previousQa;
    private Button showVideoButton, ansPls, check;
    private long timeRemaining;
    boolean isLoading;

    private EditText puzzleAns;
    private ImageButton shareBtn;
    ProgressDialog pd;

    int stateIndex;


    String JSON_STRING;

    static int item = 0;

    TextView puzzleTitle, speechTxtView, resultTxtView;

    ImageView imageView, punchView;

    private static final int REQ_CODE_SPEECH_INPUT = 100;
    private String answer;

    private OtpView otpView;

    private int attemp = 1;

    private int totalAttemp = 2;


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        storeCurrentState(item);
        storeCoins(coinCount);
        storeDate();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        new JsonTask().execute(DATA_URL);



        OneSignal.startInit(this)
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .unsubscribeWhenNotificationsAreDisabled(true)
                .init();


        puzzleTitle = (TextView) findViewById(R.id.puzzle_title);

        speechTxtView = (TextView) findViewById(R.id.speechTxt);

        imageView = (ImageView) findViewById(R.id.imageUrl);

        punchView =  (ImageView) findViewById(R.id.punch);

        resultTxtView = (TextView) findViewById(R.id.result);

        puchTxt = (TextView) findViewById(R.id.puchTxt);

        nextQa = (Button) findViewById(R.id.next_qa);

        previousQa = (Button) findViewById(R.id.previous);

        shareBtn = (ImageButton) findViewById(R.id.share_btn);

        otpView = findViewById(R.id.otp_view);

        check = (Button) findViewById(R.id.check);

        puzzleAns = (EditText) findViewById(R.id.puzzle_ans);

        String compareDate = getDate();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd");
        String currentDate = sdf.format(new Date());


        stateIndex = getCurrentState();

        coinCount = getCoins();

        System.out.println("&&&&&&&&&&&&Index"+ stateIndex);

        item = stateIndex;

        if(!compareDate.equals(currentDate)){
            coinCount = 100;
        }



        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        loadRewardedAd();

        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);



        // Create the "retry" button, which tries to show a rewarded ad between game plays.
        retryButton = findViewById(R.id.retry_button);
        retryButton.setVisibility(View.INVISIBLE);
        retryButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startGame();
                    }
                });

        // Create the "show" button, which shows a rewarded video if one is loaded.
        ansPls = (Button) findViewById(R.id.ans_pls);
        showVideoButton = findViewById(R.id.show_video_button);
        showVideoButton.setVisibility(View.INVISIBLE);
        showVideoButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showRewardedVideo();
                    }
                });

        // Display current coin count to user.
        coinCountText = findViewById(R.id.coin_count_text);
        coinCountText.setText("Coins: " + "" +coinCount);
       // coinCountText.setVisibility(View.GONE);

        startGame();

        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideKeyBoard();

                String puzzleAnswer = puzzleAns.getText().toString();
                String rightImg = "tick";
                String wrongImg = "wrong";
                if (!puzzleAnswer.isEmpty() && answer.toLowerCase().contains(puzzleAnswer.toLowerCase())){
                    Toast.makeText(MainActivity.this, "correct", Toast.LENGTH_SHORT)
                                    .show();
                    imageView.setImageResource(getResources().getIdentifier(rightImg, "drawable", "com.christus.release1.admobApp"));
                    imageView.setVisibility(View.VISIBLE);
                    coinCount = coinCount+20;
                    coinCountText.setText("Coins: " + coinCount);
                    startVoiceInput();
                }else {
                    Toast.makeText(MainActivity.this, "not correct", Toast.LENGTH_SHORT)
                                    .show();
                    imageView.setImageResource(getResources().getIdentifier(wrongImg, "drawable", "com.christus.release1.admobApp"));
                    imageView.setVisibility(View.VISIBLE);

                    if(attemp == totalAttemp){
                        check.setEnabled(false);
                        ansPls.setEnabled(true);
                        Toast.makeText(MainActivity.this, "You consumed all the free attempts", Toast.LENGTH_SHORT)
                                .show();
                    }else{
                        Toast.makeText(MainActivity.this, totalAttemp-attemp+ " attempt left", Toast.LENGTH_SHORT)
                                .show();
                        attemp ++;
                    }
                }



            }
        });

        ansPls.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                String message = "You will lose 20 points, but by seeing reward video you will gain points";
//                showAlert(message);

                hideKeyBoard();
                ansPls.setEnabled(false);
                check.setEnabled(true);
                if(coinCount !=0 && attemp == totalAttemp){
                    coinCount = coinCount-20;

                    coinCountText.setText("Coins: " + coinCount);
                    storeCoins(coinCount);
                    startVoiceInput();
                }else{
                    //show video to gain reward points
                    String msg = "To gain the points, see the video AD?";
                    showAlert(msg);
                    ansPls.setEnabled(true);
                }

            }
        });

        nextQa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemp = 0;
                puzzleAns.setText("");
                item++;
                puchTxt.setVisibility(View.GONE);
                if(item % showAdAfter == 0){
                  //  showRewardedVideo();
                }else {
                    storeCurrentState(item);
                    showQA(item);
                    speechTxtView.setText("");
                    System.out.println("Item,,,,"+item);
                }

            }
        });

        previousQa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                item--;
                showQA(item);
                speechTxtView.setText("");
            }
        });

        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String s = puzzleTitle.getText().toString();
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "From Silly riddles");
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, s);
                startActivity(Intent.createChooser(sharingIntent, "Share text via"));
            }
        });

        punchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String punchImg = "dash";
                imageView.setImageResource(getResources().getIdentifier(punchImg, "drawable", "com.christus.release1.admobApp"));
                punchView.setVisibility(View.GONE);
                puchTxt.setVisibility(View.GONE);
            }
        });

        otpView.setOtpCompletionListener(new OnOtpCompletionListener() {
            @Override
            public void onOtpCompleted(String otp) {

                // do Stuff
                Log.d("onOtpCompleted=>", otp);
            }
        });
    }

    private void showAlert(String message) {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Alert")
                .setMessage(message)

                // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Continue with delete operation
                        showRewardedVideo();
                    }
                })

                // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();

    }

    private void hideKeyBoard() {
        InputMethodManager imm = (InputMethodManager) MainActivity.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = MainActivity.this.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(MainActivity.this);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private String readJsonFromAsset() {
        String json = null;
        try {
            InputStream is = getAssets().open("data.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    private void showQA(int item) {
       // JSON_STRING = readJsonFromAsset();
        System.out.println("JSON_STRING"+ JSON_STRING);;
        try{
            JSONObject  jsonRootObject = new JSONObject(JSON_STRING);

            //Get the instance of JSONArray that contains JSONObjects
            JSONArray jsonArray = jsonRootObject.optJSONArray("result");
            this.showCard(jsonArray.getJSONObject(item));

    /*       for(int i=0; i < jsonArray.length(); i++){
               JSONObject jsonObject = jsonArray.getJSONObject(i);

              // int id = Integer.parseInt(jsonObject.optString("id").toString());
               String name = jsonObject.optString("title").toString();
               String imageUrl = jsonObject.optString("imageUrl").toString();

               System.out.println("name **"+name);;
               System.out.println("imageUrl **"+imageUrl);;

           }*/


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void startVoiceInput() {
//        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
//        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
//        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
//        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hello, How can I help you?");
//        try {
//            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
//        } catch (ActivityNotFoundException a) {
//
//        }

//        ChatSDK.ui().startSplashScreenActivity(this);
        String[] arrStr = {"sarcastic", "smiriking", "spriking"};
        String imageUrl = getRandom(arrStr);
        String punchImg = "right_facing_fist";
//        imageView.setImageResource(getResources().getIdentifier(imageUrl, "drawable", "com.christus.release1.admobApp"));
//        punchView.setImageResource(getResources().getIdentifier(punchImg, "drawable", "com.christus.release1.admobApp"));
//        puchTxt.setText("PUNCH!!!");
        speechTxtView.setText(this.answer);
        speechTxtView.setVisibility(View.VISIBLE);
        // punchView.setVisibility(View.VISIBLE);
      //  puchTxt.setVisibility(View.VISIBLE);


    }

    private void showCard(JSONObject jsonObject) {

        String title = jsonObject.optString("title").toString();
        this.answer = jsonObject.optString("result").toString();
        String imageUrl = "thinking";
        imageView.setImageResource(getResources().getIdentifier(imageUrl, "drawable", "com.christus.release1.admobApp"));
        puzzleTitle.setText(title);
    }

    @Override
    public void onPause() {
        super.onPause();
        pauseGame();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!gameOver && gamePaused) {
            resumeGame();
        }
    }

    private void pauseGame() {
        countDownTimer.cancel();
        gamePaused = true;
    }

    private void resumeGame() {
        createTimer(timeRemaining);
        gamePaused = false;
    }

    private void loadRewardedAd() {
        if (rewardedAd == null || !rewardedAd.isLoaded()) {



            rewardedAd = new RewardedAd(this, AD_UNIT_ID);
            isLoading = true;
            rewardedAd.loadAd(
                    new AdRequest.Builder().build(),
                    new RewardedAdLoadCallback() {
                        @Override
                        public void onRewardedAdLoaded() {
                            // Ad successfully loaded.
                            MainActivity.this.isLoading = false;
//                            Toast.makeText(MainActivity.this, "onRewardedAdLoaded", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onRewardedAdFailedToLoad(int errorCode) {
                            // Ad failed to load.
                            MainActivity.this.isLoading = false;
//                            Toast.makeText(MainActivity.this, "onRewardedAdFailedToLoad", Toast.LENGTH_SHORT)
//                                    .show();
                        }
                    });
        }
    }

    private void addCoins(int coins) {
        coinCount += 20;
        coinCountText.setText("Coins: " + coinCount);
    }

    private void startGame() {
        // Hide the retry button, load the ad, and start the timer.
        retryButton.setVisibility(View.INVISIBLE);
        showVideoButton.setVisibility(View.INVISIBLE);
        if (!rewardedAd.isLoaded() && !isLoading) {
            loadRewardedAd();
        }
        createTimer(COUNTER_TIME);
        gamePaused = false;
        gameOver = false;
    }

    // Create the game timer, which counts down to the end of the level
    // and shows the "retry" button.
    private void createTimer(long time) {
        final TextView textView = findViewById(R.id.timer);
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        countDownTimer =
                new CountDownTimer(time * 1000, 50) {
                    @Override
                    public void onTick(long millisUnitFinished) {
                        timeRemaining = ((millisUnitFinished / 1000) + 1);
                        textView.setText("seconds remaining: " + timeRemaining);
                    }

                    @Override
                    public void onFinish() {
                        if (rewardedAd.isLoaded()) {
//                            showVideoButton.setVisibility(View.VISIBLE);
                        }
                        textView.setText("You Lose!");
                     //   addCoins(GAME_OVER_REWARD);
                        gameOver = true;
                    }
                };
        countDownTimer.start();
    }

    private void showRewardedVideo() {
        showVideoButton.setVisibility(View.INVISIBLE);
        if (rewardedAd.isLoaded()) {
            RewardedAdCallback adCallback =
                    new RewardedAdCallback() {
                        @Override
                        public void onRewardedAdOpened() {
                            // Ad opened.
//                            Toast.makeText(MainActivity.this, "onRewardedAdOpened", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onRewardedAdClosed() {
                            // Ad closed.
//                            Toast.makeText(MainActivity.this, "onRewardedAdClosed", Toast.LENGTH_SHORT).show();
                            // Preload the next video ad.
                            MainActivity.this.loadRewardedAd();
                        }

                        @Override
                        public void onUserEarnedReward(RewardItem rewardItem) {
                            // User earned reward.
//                            Toast.makeText(MainActivity.this, "onUserEarnedReward", Toast.LENGTH_SHORT).show();
                            addCoins(rewardItem.getAmount());

                            //showQA(item);
                            startVoiceInput();
                            speechTxtView.setText("");
                            System.out.println("Item,,,,"+item);
                        }

                        @Override
                        public void onRewardedAdFailedToShow(int errorCode) {
                            // Ad failed to display
//                            Toast.makeText(MainActivity.this, "onRewardedAdFailedToShow", Toast.LENGTH_SHORT)
//                                    .show();
                            //showQA(item);
                            startVoiceInput();
                            speechTxtView.setText("");
                            System.out.println("Item,,,,"+item);
                        }
                    };
            rewardedAd.show(this, adCallback);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    String speechTxt = result.get(0);
                    Toast.makeText(MainActivity.this, result.get(0), Toast.LENGTH_SHORT)
                            .show();
                    speechTxtView.setText(""+speechTxt);
                    speechTxtView.setVisibility(View.VISIBLE);
                    if(this.answer.equals(speechTxt)) {
                        Toast.makeText(MainActivity.this, "Move to next", Toast.LENGTH_SHORT)
                                .show();
                        resultTxtView.setText("Correct");
                    }else {
                        Toast.makeText(MainActivity.this, "Wrong", Toast.LENGTH_SHORT)
                                .show();
                        resultTxtView.setText("Wrong");
                        retryButton.setVisibility(View.VISIBLE);
                    }
                }
                break;
            }

        }
    }


    public void storeCurrentState(int item) {
        // Storing data into ShareitedPreferences
        SharedPreferences sharedPreferences
                = getSharedPreferences("MySharedPref",
                MODE_PRIVATE);

        // Creating an Editor object
        // to edit(write to the file)
        SharedPreferences.Editor myEdit
                = sharedPreferences.edit();


        myEdit.putInt(
                "currentIndex",
                item);

        myEdit.commit();

    }

    public void storeCoins(int coins) {
        // Storing data into ShareitedPreferences
        SharedPreferences sharedPreferences
                = getSharedPreferences("MySharedPref",
                MODE_PRIVATE);

        // Creating an Editor object
        // to edit(write to the file)
        SharedPreferences.Editor myEdit
                = sharedPreferences.edit();


        myEdit.putInt(
                "coins",
                coins);

        myEdit.commit();

    }

    public void storeDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd");
        String currentDateandTime = sdf.format(new Date());

        SharedPreferences sharedPreferences
                = getSharedPreferences("MySharedPref",
                MODE_PRIVATE);

        // Creating an Editor object
        // to edit(write to the file)
        SharedPreferences.Editor myEdit
                = sharedPreferences.edit();


        myEdit.putString(
                "date",
                currentDateandTime);

        myEdit.commit();

    }

    public String getDate(){

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd");
        String currentDateandTime = sdf.format(new Date());

        SharedPreferences sh
                = getSharedPreferences("MySharedPref", MODE_PRIVATE);

        String a = sh.getString("date", currentDateandTime );

        return a;

    }


    public int getCoins(){
        SharedPreferences sh
                = getSharedPreferences("MySharedPref", MODE_PRIVATE);

        int a = sh.getInt("coins", 100);

        return a;

    }

    public int getCurrentState(){
        // Retrieving the value using its keys
        // the file name must be same in both saving
        // and retrieving the data
        SharedPreferences sh
                = getSharedPreferences("MySharedPref", MODE_PRIVATE);

        int a = sh.getInt("currentIndex", 0);

        return a;


    }

    public static String getRandom(String[] array) {
        int rnd = new Random().nextInt(array.length);
        return array[rnd];
    }


    private class JsonTask extends AsyncTask<String, String, String> {

        protected void onPreExecute() {
            super.onPreExecute();

            pd = new ProgressDialog(MainActivity.this);
            pd.setMessage("Please wait");
            pd.setCancelable(false);
            pd.show();
        }

        protected String doInBackground(String... params) {


            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();


                InputStream stream = connection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuffer buffer = new StringBuffer();
                String line = "";

                while ((line = reader.readLine()) != null) {
                    buffer.append(line+"\n");
                    Log.d("Response: ", "> " + line);   //here u ll get whole response...... :-)

                }

                return buffer.toString();


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (pd.isShowing()){
                pd.dismiss();
            }

            JSON_STRING = result;

            System.out.println("JSON_STRING*********"+ JSON_STRING);

            showQA(stateIndex);

        }
    }




}

