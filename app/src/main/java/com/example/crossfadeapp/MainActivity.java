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
                if (audio1IsConfigured && audio2IsConfigured) {
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
                }else{
                    Toast.makeText(getApplicationContext(), R.string.bt_mix_exception, Toast.LENGTH_SHORT).show();
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
            playSong(mediaPlayer1, 0, crossfadeDuration / 2);
            try {
                while (true) {
                    initMediaPlayer1();
                    isCrossfade1 = true;
                    Thread.sleep(crossfadeDuration);
                    initMediaPlayer1();
                    initMediaPlayer2();
                    playSong(mediaPlayer2, crossfadeDuration , crossfadeDuration );
                    initMediaPlayer2();
                    isCrossfade2 = true;
                    Thread.sleep(crossfadeDuration);
                    initMediaPlayer1();
                    initMediaPlayer2();
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
                audio1Name = getNameFromUri(getApplicationContext(), audio1Path);
                audio1Duraion = mediaPlayer1.getDuration();
                audio1DuraionString = getTimeFromDuration(audio1Duraion);
                tv_audio1Name.setText(audio1Name);
                tv_audio1Duration.setText(audio1DuraionString);
                audio1IsConfigured = true;
            }catch (NullPointerException e){
                Toast.makeText(getApplicationContext(), R.string.bt_audio_exception, Toast.LENGTH_LONG).show();
            }
        }
        //Second track`s block setup
        if (requestCode == 2 && resultCode == RESULT_OK) {
            try {
                audio2Path = data.getData(); //The uri with the location of the file
                initMediaPlayer2();
                audio2Name = getNameFromUri(getApplicationContext(), audio2Path);
                audio2Duraion = mediaPlayer2.getDuration();
                audio2DuraionString = getTimeFromDuration(audio2Duraion);
                tv_audio2Name.setText(audio2Name);
                tv_audio2Duration.setText(audio2DuraionString);
                audio2IsConfigured = true;
            }catch (NullPointerException e){
                Toast.makeText(getApplicationContext(), R.string.bt_audio_exception, Toast.LENGTH_LONG).show();
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


    //Converting Uri to String
    public String getStringFromUri(final Context context, final Uri uri) {
        if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
            final String docId = DocumentsContract.getDocumentId(uri);
            final String[] split = docId.split(":");
            final String type = split[0];
            Uri contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            final String selection = "_id=?";
            final String[] selectionArgs = new String[]{
                    split[1]
            };
            return getDataColumn(context, contentUri, selection, selectionArgs);
        }
        return null;
    }
    public String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs){
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    //Receiving filename from Uri
    public String getNameFromUri(final Context context, final Uri uri) {
        String[] buffer = getStringFromUri(context, uri).split("/");
        return buffer[buffer.length-1];
    }

    private void crossfade(MediaPlayer mediaPlayer1, MediaPlayer mediaPlayer2) {
       fadeOut(mediaPlayer1, crossfadeDuration);
       fadeIn(mediaPlayer2, crossfadeDuration);
    }
    public void fadeOut(final MediaPlayer _player, final int duration) {
        final float deviceVolume = getDeviceVolume();
        final Handler h = new Handler();
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
                if (time > 0)
                    h.postDelayed(this, 100);
                else {
                    _player.stop();
                    _player.release();
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
                else {
                    _player.stop();
                    _player.release();
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