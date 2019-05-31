package com.example.crossfadeapp;

import android.Manifest;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, SeekBar.OnSeekBarChangeListener, SoundPool.OnLoadCompleteListener {

    //Creating variables
    private String audio1Name;
    private Uri audio1Path;
    private Integer audio1Duration;
    private String audio1DurationString;
    private Button bt_audio1Set;
    private TextView tv_audio1Name;
    private TextView tv_audio1Duration;
    private boolean audio1IsConfigured;

    private String audio2Name;
    private Uri audio2Path;
    private Integer audio2Duration;
    private String audio2DurationString;
    private Button bt_audio2Set;
    private TextView tv_audio2Name;
    private TextView tv_audio2Duration;
    private boolean audio2IsConfigured;

    private MediaPlayer mediaPlayer;
    private SeekBar sb_crossfadeTime;
    private TextView tv_seekDynamic;
    private Integer crossfadeDuration;
    private Button bt_mix;
    private CrossfadePlayer crossfadePlayer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Associating elements of layout with variables
        bt_audio1Set = (Button) findViewById(R.id.bt_audio1Set);
        tv_audio1Name = (TextView) findViewById(R.id.tv_audio1Name);
        tv_audio1Duration = (TextView) findViewById(R.id.tv_audio1Duration);

        bt_audio2Set = (Button) findViewById(R.id.bt_audio2Set);
        tv_audio2Name = (TextView) findViewById(R.id.tv_audio2Name);
        tv_audio2Duration = (TextView) findViewById(R.id.tv_audio2Duration);

        sb_crossfadeTime = (SeekBar) findViewById(R.id.sb_crossFadeTime);
        sb_crossfadeTime.setOnSeekBarChangeListener(this);
        bt_mix = (Button) findViewById(R.id.bt_mix);
        tv_seekDynamic = (TextView) findViewById(R.id.tv_seekDynamic);

        //Accessing for permissions
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);

        //Setup of mediaplayer
        crossfadePlayer = new CrossfadePlayer(getApplicationContext());
        //Default duration
        crossfadeDuration = 10;

        //Associating buttons with listeners
        bt_audio1Set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTrack(Settings.FileRequestType.FIRST_TRACK);
            }
        });
        bt_audio2Set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTrack(Settings.FileRequestType.SECOND_TRACK);
            }
        });
        bt_mix.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((audio1IsConfigured && audio2IsConfigured)) && ((audio1Duration > crossfadeDuration*2*1000) && (audio2Duration > crossfadeDuration*2*1000))) {
                    crossfadePlayer.start();
                    crossfadePlayer.setCrossfadeTime(crossfadeDuration);
                }else if (!(audio1IsConfigured && audio2IsConfigured)){
                    Toast.makeText(getApplicationContext(), R.string.bt_mix_exception, Toast.LENGTH_SHORT).show();
                }else if ((audio1Duration <= crossfadeDuration*2*1000) || audio2Duration <= crossfadeDuration*2*1000){
                    Toast.makeText(getApplicationContext(), R.string.bt_duration_exception, Toast.LENGTH_LONG).show();
                }

            }

        });
    }

    public void selectTrack(int requestCode) {
        Intent intent = new Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT);

        startActivityForResult(Intent.createChooser(intent, "Select a file"), requestCode);
    }

    public void initMediaPlayer(Uri uri){
        try{
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(MainActivity.this, uri);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.prepare();
        }catch (IOException e){

        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //First track`s block setup
        if (requestCode == Settings.FileRequestType.FIRST_TRACK && resultCode == RESULT_OK) {
            try {
                audio1Path = data.getData(); //The uri with the location of the file
                initMediaPlayer(audio1Path);
                audio1Name = getNameFromUri(audio1Path);
                audio1Duration = mediaPlayer.getDuration();
                audio1DurationString = getTimeFromDuration(audio1Duration);
                tv_audio1Name.setText(audio1Name);
                tv_audio1Duration.setText(audio1DurationString);
                crossfadePlayer.setFirstTrack(audio1Path);
                audio1IsConfigured = true;
            }catch (Exception e){
                Toast.makeText(getApplicationContext(), "Choose AUDIOfile!", Toast.LENGTH_LONG);
            }
        }
        //Second track`s block setup
        if (requestCode == Settings.FileRequestType.SECOND_TRACK && resultCode == RESULT_OK) {
            try {
                audio2Path = data.getData(); //The uri with the location of the file
                initMediaPlayer(audio2Path);
                audio2Name = getNameFromUri(audio2Path);
                audio2Duration = mediaPlayer.getDuration();
                audio2DurationString = getTimeFromDuration(audio2Duration);
                tv_audio2Name.setText(audio2Name);
                tv_audio2Duration.setText(audio2DurationString);
                crossfadePlayer.setSecondTrack(audio2Path);
                audio2IsConfigured = true;
            }catch (Exception e){
                Toast.makeText(getApplicationContext(), "Choose AUDIOfile!", Toast.LENGTH_LONG);
            }
        }
    }

    //Converting info about music duration to String
    public String getTimeFromDuration(Integer duration){
        if (duration%60000/1000 < 10) {
            return new Integer(duration/60000).toString() + ":0" + new Integer(duration%60000/1000).toString();
        }
        else if(duration%60000/1000 >= 10){
            return new Integer(duration/60000).toString() + ":" + new Integer(duration%60000/1000).toString();
        }
        return "";
    }


    public String getNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    //Managing seekbar
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        crossfadeDuration = sb_crossfadeTime.getProgress() + 2;
        tv_seekDynamic.setText(String.valueOf(crossfadeDuration) + " seconds");
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
    }

    @Override
    public void onPrepared(MediaPlayer mp) {

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
    }
}
