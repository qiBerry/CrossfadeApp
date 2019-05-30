package com.example.crossfadeapp;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class CrossfadePlayer {
    private float deltaValue;
    private Context context;
    private MediaPlayer firstPlayer, secondPlayer;
    private int crossfadeTime;
    private float fadeInVolume, fadeOutVolume;


    CrossfadePlayer(Context context) {
        this.context = context;
    }

    public void setCrossfadeTime(int crossfadeTime) {
        this.crossfadeTime = crossfadeTime * 1000;
        this.deltaValue = 1f / (this.crossfadeTime / Settings.FADE_STEP_INTERVAL);
    }

    //Init first mediaplayer
    public void setFirstTrack(Uri firstTrack) {
        if (firstPlayer == null) {
            firstPlayer = new MediaPlayer();
        } else {
            firstPlayer.reset();
        }

        try {
            firstPlayer.setDataSource(context, firstTrack);
            firstPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            firstPlayer.prepareAsync();
        } catch (IOException ex) {
            ex.printStackTrace();
            firstPlayer = null;
        }
    }

    //Init second mediaplayer
    public void setSecondTrack(Uri secondTrack) {
        if (secondPlayer == null) {
            secondPlayer = new MediaPlayer();
        } else {
            secondPlayer.reset();
        }

        try {
            secondPlayer.setDataSource(context, secondTrack);
            secondPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            secondPlayer.prepareAsync();
        } catch (IOException ex) {
            ex.printStackTrace();
            secondPlayer = null;
        }

    }

    //Start playback
    public Exception start() {
        if (firstPlayer != null & secondPlayer != null) {
            if (firstPlayer.getDuration() > crossfadeTime & secondPlayer.getDuration() > crossfadeTime) {
                play(firstPlayer, secondPlayer, false);
                return null;

            }
        }
        return null;
    }

    private void cleanTimerList(List<Timer> timerList) {
        for (Timer timer : timerList) {
            timer.cancel();
            timer.purge();
        }
        timerList.clear();

    }


    private void play(final MediaPlayer currentTrack, final MediaPlayer nextTrack, boolean needFadeIn) {
        if (needFadeIn) {
            startWithFadeIn(currentTrack);
        } else {
            currentTrack.setVolume(1f, 1f);
            currentTrack.start();
        }

        final Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (currentTrack.getCurrentPosition() >= currentTrack.getDuration() - crossfadeTime) {
                    play(nextTrack, currentTrack, true);
                    fadeOut(currentTrack);
                    timer.cancel();
                    timer.purge();
                }
            }
        };
        timer.scheduleAtFixedRate(task, 1000, 1000);
    }


    private void startWithFadeIn(MediaPlayer player) {
        player.setVolume(0f, 0f);
        player.start();
        fadeInVolume = 0f;
        fadePlayer(player, Settings.FADE_IN);
    }

    private void fadeOut(MediaPlayer player) {
        fadeOutVolume = 1f;
        fadePlayer(player, Settings.FADE_OUT);

    }

    private void fadePlayer(final MediaPlayer player, final int fadeMode) {
        final Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                makeFadeStep(player, fadeMode == Settings.FADE_OUT ? deltaValue * -1 : deltaValue);
                if (fadeMode == Settings.FADE_OUT) {
                    if (fadeOutVolume <= 0f) {
                        timer.cancel();
                        timer.purge();
                    }
                } else {
                    if (fadeInVolume >= 1f) {
                        timer.cancel();
                        timer.purge();
                    }
                }
            }
        };

        timer.scheduleAtFixedRate(timerTask, Settings.FADE_STEP_INTERVAL, Settings.FADE_STEP_INTERVAL);
    }

    private void makeFadeStep(MediaPlayer player, float v) {
        float newVolume;

        if (v > 0) {
            fadeInVolume += v;
            newVolume = fadeInVolume;
        } else {
            fadeOutVolume += v;
            newVolume = fadeOutVolume;
        }
        player.setVolume(newVolume, newVolume);
    }
}
