package com.example.crossfadeapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.media.TimedMetaData;
import android.net.Uri;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.GetChars;
import android.util.Log;
import android.view.GestureDetector;
import android.view.View;
import android.webkit.MimeTypeMap;
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
    private Integer audio1Duraion;
    private String audio1DuraionString;
    private Button bt_audio1Set;
    private TextView tv_audio1Name;
    private TextView tv_audio1Duration;
    private boolean audio1IsConfigured;

    private String audio2Name;
    private Uri audio2Path;
    private Integer audio2Duraion;
    private String audio2DuraionString;
    private Button bt_audio2Set;
    private TextView tv_audio2Name;
    private TextView tv_audio2Duration;
    private boolean audio2IsConfigured;

    private MediaPlayer mediaPlayer1;
    private MediaPlayer mediaPlayer2;
    private SeekBar sb_crossfadeTime;
    private TextView tv_seekDynamic;
    private Integer crossfadeDuration = 10000;
    private Button bt_mix;
    public boolean isCrossfade1 = false;
    public boolean isCrossfade2 = false;

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

        //Associating buttons with listeners
        bt_audio1Set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent()
                        .setType("*/*")
                        .setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(intent, "Select a file"), 1);
            }
        });
        bt_audio2Set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent()
                        .setType("*/*")
                        .setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(intent, "Select a file"), 2);
            }
        });
        bt_mix.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((audio1IsConfigured && audio2IsConfigured)) && ((audio1Duraion > crossfadeDuration*2) && (audio2Duraion > crossfadeDuration*2))) {

                    //Thread, what manages songs
                    playThread playThread = new playThread();
                    playThread.start();

                    //Handler manages crossfades
                    final Handler h = new Handler();
                    h.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (isCrossfade1) {
                                isCrossfade1 = false;
                                crossfade(mediaPlayer1, mediaPlayer2);
                            } else if (isCrossfade2) {
                                isCrossfade2 = false;
                                crossfade(mediaPlayer2, mediaPlayer1);
                            }
                            h.postDelayed(this, 50);
                        }
                    }, 50);
                }else if (!(audio1IsConfigured && audio2IsConfigured)){
                    Toast.makeText(getApplicationContext(), R.string.bt_mix_exception, Toast.LENGTH_SHORT).show();
                }else if ((audio1Duraion <= crossfadeDuration*2) || audio2Duraion <= crossfadeDuration*2){
                    Toast.makeText(getApplicationContext(), R.string.bt_duration_exception, Toast.LENGTH_LONG).show();
                }

            }

        });
    }
    //Thread, what manages songs
    public class playThread extends Thread {

        public void playSong(final MediaPlayer mediaPlayer, int startDelay, final int finishDelay){
            final int duration = mediaPlayer.getDuration();
            mediaPlayer.seekTo(startDelay);
            mediaPlayer.start();
            try {
                Thread.sleep(duration - mediaPlayer.getCurrentPosition() - finishDelay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        @Override
        public void run() {
            playSong(mediaPlayer1, 0, crossfadeDuration);
            try {
                while (true) {
                    initMediaPlayer1();
                    isCrossfade1 = true;
                    Thread.sleep(crossfadeDuration);
                    initMediaPlayer2();
                    playSong(mediaPlayer2, crossfadeDuration , crossfadeDuration );
                    initMediaPlayer2();
                    isCrossfade2 = true;
                    Thread.sleep(crossfadeDuration);
                    initMediaPlayer1();
                    playSong(mediaPlayer1, crossfadeDuration , crossfadeDuration);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //First track`s block setup
        if (requestCode == 1 && resultCode == RESULT_OK) {
            try {
                audio1Path = data.getData(); //The uri with the location of the file
                initMediaPlayer1();
                audio1Name = getNameFromUri(audio1Path);
                audio1Duraion = mediaPlayer1.getDuration();
                audio1DuraionString = getTimeFromDuration(audio1Duraion);
                tv_audio1Name.setText(audio1Name);
                tv_audio1Duration.setText(audio1DuraionString);
                audio1IsConfigured = true;
            }catch (Exception e){
                Toast.makeText(getApplicationContext(), "Choose AUDIOfile!", Toast.LENGTH_LONG);
            }
        }
        //Second track`s block setup
        if (requestCode == 2 && resultCode == RESULT_OK) {
            try {
                audio2Path = data.getData(); //The uri with the location of the file
                initMediaPlayer2();
                audio2Name = getNameFromUri(audio2Path);
                audio2Duraion = mediaPlayer2.getDuration();
                audio2DuraionString = getTimeFromDuration(audio2Duraion);
                tv_audio2Name.setText(audio2Name);
                tv_audio2Duration.setText(audio2DuraionString);
                audio2IsConfigured = true;
            }catch (Exception e){
                Toast.makeText(getApplicationContext(), "Choose AUDIOfile!", Toast.LENGTH_LONG);
            }
        }
    }

    //Configuring first track`s mediaplayer
    public void initMediaPlayer1(){
        try{
            mediaPlayer1 = new MediaPlayer();
            mediaPlayer1.setDataSource(MainActivity.this, audio1Path);
            mediaPlayer1.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer1.prepare();
        }catch (IOException e){

        }
    }
    //Configuring second track`s mediaplayer
    public void initMediaPlayer2(){
        try{
            mediaPlayer2 = new MediaPlayer();
            mediaPlayer2.setDataSource(MainActivity.this, audio2Path);
            mediaPlayer2.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer2.prepare();
        }catch (IOException e){

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


    private void crossfade(MediaPlayer mp1, MediaPlayer mp2) {
       fadeOut(mp1, crossfadeDuration);
       fadeIn(mp2, crossfadeDuration);
    }

    public void fadeOut(final MediaPlayer _player, final int duration) {
        final float deviceVolume = getDeviceVolume();
        final Handler h = new Handler();
        _player.seekTo(_player.getDuration() - duration);
        h.postDelayed(new Runnable() {
            private float time = duration;
            private float volume = 0.0f;
            @Override
            public void run() {
                if (!_player.isPlaying())
                    _player.start();
                // can call h again after work!
                time -= 100;
                volume = (deviceVolume * time) / duration;
                _player.setVolume(volume, volume);
                if (time >= 100)
                    h.postDelayed(this, 100);
                else if ((time < 100) && (time > 0)){
                    h.postDelayed(this, (long) time);
                }
                else {
                    _player.stop();
                }
            }
        }, 100); // 1 second delay (takes millis)
    }
    public void fadeIn(final MediaPlayer _player, final int duration) {
        final float deviceVolume = getDeviceVolume();
        final Handler h = new Handler();
        h.postDelayed(new Runnable() {
            private float time = 0.0f;
            private float volume = 0.0f;

            @Override
            public void run() {
                if (!_player.isPlaying())
                    _player.start();
                // can call h again after work!
                time += 100;
                volume = (deviceVolume * time) / duration;
                _player.setVolume(volume, volume);
                if (time < duration)
                    h.postDelayed(this, 100);
                else if ((duration - time < 100) && (duration - time > 0))
                    h.postDelayed(this, (long) time);

                else {
                    _player.stop();
                }
            }
        }, 100); // 1 second delay (takes millis)

    }

    public float getDeviceVolume() {
        AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        int volumeLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        return (float) volumeLevel / maxVolume;
    }
//Managing seekbar
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        crossfadeDuration = (sb_crossfadeTime.getProgress() + 2)*1000;
        tv_seekDynamic.setText(String.valueOf(crossfadeDuration/1000) + " seconds");
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
