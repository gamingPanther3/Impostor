package com.mlprograms.impostor;

import static com.mlprograms.impostor.ToastHelper.showToastLong;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.appcompat.app.AppCompatActivity;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor data;

    private int playerPos = 1;
    private CountDownTimer timer;
    int minute, second;
    TextView countdownTextView;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        startUpAnimation();

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        sharedPreferences = getSharedPreferences("imposterSettings", Context.MODE_PRIVATE);
        data = sharedPreferences.edit();
    }

    private void startTimer() {
        countdownTextView = findViewById(R.id.timeTextView);

        long duration = TimeUnit.MINUTES.toMillis(minute) + TimeUnit.SECONDS.toMillis(second);

        timer = new CountDownTimer(duration, 100) {
            @Override
            public void onTick(long l) {
                String sDuration = String.format(Locale.GERMAN, "%02d : %02d",
                        TimeUnit.MILLISECONDS.toMinutes(l),
                        TimeUnit.MILLISECONDS.toSeconds(l) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(l))
                );
                countdownTextView.setText(sDuration);
            }

            @Override
            public void onFinish() {
                TextView textView1 = findViewById(R.id.timeTextViewText);
                TextView textView2 = findViewById(R.id.pleaseWaitTillGameIsOver);

                textView1.setVisibility(View.GONE);
                textView2.setText("");
                countdownTextView.setText("DIE ZEIT IST ABGELAUFEN");
                countdownTextView.setTextSize(65F);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(5000, VibrationEffect.DEFAULT_AMPLITUDE));
                }
            }
        };
        timer.start();
    }

    private void stopTimerAndVibrate() {
        if (timer != null) {
            timer.cancel();
            vibrator.cancel();
        }
    }

    private void doAfterStartUpAnimation() {
        setContentView(R.layout.main_layout);

        Button continueButton = findViewById(R.id.continueButton);
        Spinner chooseCategory = findViewById(R.id.chooseCategory);
        EditText editTextPlayerNumber = findViewById(R.id.editTextNumberPlayers);
        EditText editTextNumberImpostor = findViewById(R.id.editTextNumberImpostor);
        EditText editTextMinute = findViewById(R.id.editTextMinute);
        EditText editTextSecond = findViewById(R.id.editTextSecond);

        List<String> categories = new ArrayList<>();
        categories.add("Zufall");
        categories.add("Tiere");
        categories.add("Fußballspieler");
        categories.add("Gegenstände");
        categories.add("Essen");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        chooseCategory.setAdapter(adapter);

        chooseCategory.setSelection(sharedPreferences.getInt("categoryPosition", 0));
        editTextPlayerNumber.setText(sharedPreferences.getString("playerNumber", ""));
        editTextNumberImpostor.setText(sharedPreferences.getString("imposterNumber", ""));
        editTextMinute.setText(sharedPreferences.getString("timeMinute", ""));
        editTextSecond.setText(sharedPreferences.getString("timeSecond", ""));

        continueButton.setOnClickListener((v) -> runOnUiThread(() -> {
            if(checkNumbers()) {
                final int randomInt = ThreadLocalRandom.current().nextInt(0, 3);
                final int playerNumber = Integer.parseInt(editTextPlayerNumber.getText().toString());
                final int imposterNumber = Integer.parseInt(editTextNumberImpostor.getText().toString());

                String randomThing = "";
                switch (chooseCategory.getSelectedItemPosition()) {
                    case 0:
                        if(randomInt == 0) {
                            randomThing = getRandomAnimal();
                        } else if(randomInt == 1) {
                            randomThing = getRandomSoccerPlayer();
                        } else if(randomInt == 2) {
                            randomThing = getRandomThings();
                        } else if(randomInt == 3) {
                            randomThing = getRandomFood();
                        }
                        break;
                    case 1:
                        randomThing = getRandomAnimal();
                        break;
                    case 2:
                        randomThing = getRandomSoccerPlayer();
                        break;
                    case 3:
                        randomThing = getRandomThings();
                        break;
                    case 4:
                        randomThing = getRandomFood();
                        break;
                }

                data.putString("playerNumber", String.valueOf(playerNumber));
                data.putString("imposterNumber", String.valueOf(imposterNumber));
                data.putInt("categoryPosition", chooseCategory.getSelectedItemPosition());
                data.putString("timeMinute", String.valueOf(minute));
                data.putString("timeSecond", String.valueOf(second));
                data.apply();

                List<Integer> imposterList = new LinkedList<>();
                while(imposterList.size() != imposterNumber) {
                    int randomImposter = ThreadLocalRandom.current().nextInt(1, playerNumber + 1);
                    if(!imposterList.contains(randomImposter)) {
                        imposterList.add(randomImposter);
                    }
                }

                playerPos = 1;
                showPlayersTheme(playerNumber, imposterList, randomThing);
            }
        }));
    }

    private void showGameResults(List<Integer> imposterList) {
        Button showResult = findViewById(R.id.showResult);
        Button quitGame = findViewById(R.id.quitGame);
        showResult.setText("Neue Runde");
        quitGame.setVisibility(View.VISIBLE);

        TextView pleaseWaitTillGameIsOver = findViewById(R.id.pleaseWaitTillGameIsOver);

        StringBuilder impostors = new StringBuilder();
        impostors.append("Imposter war(en):\n\n");
        for (Integer imposter : imposterList) {
            impostors.append("Spieler ").append(imposter).append("\n");
        }

        pleaseWaitTillGameIsOver.setText(impostors.toString());

        showResult.setOnClickListener((view) ->
            doAfterStartUpAnimation()
        );

        quitGame.setOnClickListener((view) -> {
            setContentView(R.layout.loading_screen);
            Handler handler = new Handler();
            handler.postDelayed(this::finish, 1000);
        });
    }

    private void showPlayersTheme(int playerNumber, List<Integer> imposterList, String randomThing) {
        setContentView(R.layout.player_screen);
        Button continuePlayerScreen = findViewById(R.id.coninueButton);
        TextView playersNumber = findViewById(R.id.textViewPlayerNumber);

        if(playerPos <= playerNumber) {
            playersNumber.setText("Spieler " + playerPos);
        } else {
            setContentView(R.layout.show_game_results);
            Button showResult = findViewById(R.id.showResult);

            showResult.setOnClickListener((view) -> {
                TextView textView1 = findViewById(R.id.timeTextViewText);
                TextView textView2 = findViewById(R.id.timeTextView);

                textView1.setVisibility(View.GONE);
                textView2.setVisibility(View.GONE);

                stopTimerAndVibrate();
                showGameResults(imposterList);
            });

            return;
        }

        continuePlayerScreen.setOnClickListener((v) -> {
            if(playerPos <= playerNumber) {
                setContentView(R.layout.gameui);

                TextView playerNumberTextView = findViewById(R.id.textViewPlayerNumber);
                TextView roleOrThemeText = findViewById(R.id.textViewRoleOrThemeText);
                TextView roleOrTheme = findViewById(R.id.textViewRoleOrTheme);
                Button searchOnGoogle = findViewById(R.id.searchOnGoogleButton);
                Button continueToNextPlayer = findViewById(R.id.continueToNextPlayerButton);

                playerNumberTextView.setText("Spieler " + playerPos);
                if(imposterList.contains(playerPos)) {
                    searchOnGoogle.setVisibility(TextView.INVISIBLE);
                    roleOrThemeText.setText("und du hast die Rolle:");
                    roleOrTheme.setText("Impostor");
                } else {
                    searchOnGoogle.setVisibility(TextView.VISIBLE);
                    roleOrThemeText.setText("und du hast das Thema:");
                    roleOrTheme.setText(randomThing);

                    searchOnGoogle.setOnClickListener((view) ->
                            searchForThingOnGoogle(randomThing)
                    );
                }

                playerPos++;

                if(playerPos == playerNumber + 1) {
                    continueToNextPlayer.setText("Fertig");
                }

                continueToNextPlayer.setOnClickListener((view) -> {
                        showPlayersTheme(playerNumber, imposterList, randomThing);

                        if(continueToNextPlayer.getText().equals("Fertig")) {
                            startTimer();
                        }
                });
            }
        });
    }

    private void searchForThingOnGoogle(String keyword) {
        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        intent.putExtra(SearchManager.QUERY, keyword);
        startActivity(intent);
    }

    private void startUpAnimation() {
        setContentView(R.layout.loading_screen);
        Handler handler = new Handler();
        handler.postDelayed(this::doAfterStartUpAnimation, 3000);
    }

    private boolean checkNumbers() {
        EditText editTextPlayers = findViewById(R.id.editTextNumberPlayers);
        EditText editTextImpostors = findViewById(R.id.editTextNumberImpostor);
        EditText editTextSecond = findViewById(R.id.editTextSecond);
        EditText editTextMinute = findViewById(R.id.editTextMinute);

        if(
                !String.valueOf(editTextPlayers.getText()).isEmpty() &&
                !String.valueOf(editTextImpostors.getText()).isEmpty()
        ) {
            final int numPlayers = Integer.parseInt(editTextPlayers.getText().toString());
            final int numImpostor = Integer.parseInt(editTextImpostors.getText().toString());

            if(String.valueOf(editTextSecond.getText()).isEmpty()) {
                editTextSecond.setText("0");
            }
            if(String.valueOf(editTextMinute.getText()).isEmpty()) {
                editTextMinute.setText("0");
            }

            if(!((Integer.parseInt(String.valueOf(editTextSecond.getText())) + (Integer.parseInt(String.valueOf(editTextMinute.getText())) * 60)) >= 30)) {
                showToastLong("Die Rundendauer muss länger als 30s sein", getApplicationContext());

                if(String.valueOf(editTextSecond.getText()).equals("0")) {
                    editTextSecond.setText("");
                }
                if(String.valueOf(editTextMinute.getText()).equals("0")) {
                    editTextMinute.setText("");
                }
                return false;
            }

            if(numPlayers <= 1) {
                showToastLong("Du musst mehr als ein Spieler sein", getApplicationContext());
                return false;
            }

            if(numImpostor == 0) {
                showToastLong("Es muss mindestens ein Spieler Impostor sein", getApplicationContext());
                return false;
            }

            if(numImpostor >= numPlayers) {
                showToastLong("Anzahl der Impostor's muss kleiner als die Anzahl der Spieler sein", getApplicationContext());
                return false;
            }

            second = Integer.parseInt(String.valueOf(editTextSecond.getText()));
            minute = Integer.parseInt(String.valueOf(editTextMinute.getText()));
            return true;
        }
        showToastLong("Fülle zuerst alle Felder aus", getApplicationContext());

        return false;
    }

    private static String getRandomAnimal() {
        return RandomThingList.animalList.get(ThreadLocalRandom.current().nextInt(0, RandomThingList.animalList.size()));
    }

    private static String getRandomSoccerPlayer() {
        return RandomThingList.soccerPlayerList.get(ThreadLocalRandom.current().nextInt(0, RandomThingList.soccerPlayerList.size()));
    }

    private static String getRandomThings() {
        return RandomThingList.thingsList.get(ThreadLocalRandom.current().nextInt(0, RandomThingList.thingsList.size()));
    }

    private static String getRandomFood() {
        return RandomThingList.foodList.get(ThreadLocalRandom.current().nextInt(0, RandomThingList.foodList.size()));
    }
}
