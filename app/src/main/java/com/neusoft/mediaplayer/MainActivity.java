package com.neusoft.mediaplayer;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.ActivityManager;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE};
    private static final int PERMISSIONS_COUNT = 1;
    private MediaPlayer player = null;
    private boolean isPlayerCreated;
    private ArrayList<String> mSongs;
    private SeekBar mSeekBar;
    private TextView mCurrentPos;
    private TextView mEndPos;
    private int songPos;
    private ImageButton mPlayPause;
    private ImageButton mPlayNext;
    private ImageButton mPlayPrevious;
    private boolean mSongPlaying;
    private int mSongPos;
    private ListView mSongListView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSeekBar = findViewById(R.id.song_seekbar);
        mCurrentPos = findViewById(R.id.current_pos);
        mEndPos = findViewById(R.id.end_pos);
        mPlayPause = findViewById(R.id.play_pause);
        mPlayNext=findViewById(R.id.next_button);
        mPlayPrevious=findViewById(R.id.previous_button);
        mSongListView=findViewById(R.id.song_list);
        //need to ask permissions after api 23
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean checkPermissionsDenied() {
        for (int i = 0; i < PERMISSIONS_COUNT; i++) {
            if (checkSelfPermission(PERMISSIONS[i]) != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (checkPermissionsDenied()) {
            ((ActivityManager) (Objects.requireNonNull(this.getSystemService(ACTIVITY_SERVICE))))
                    .clearApplicationUserData();
        } else {
            onResume();
        }
    }

    private void playSong(String path) {
        if (player == null)
            player = new MediaPlayer();
        else {
            //player.stop();
            player.reset();
        }

        try {
            player.setDataSource(path);
            player.prepare();
            for (int i = 0; i < mSongListView.getChildCount(); i++) {
                if(mSongPos == i ){
                    mSongListView.getChildAt(i).setBackgroundColor(Color.BLUE);
                }else{
                    mSongListView.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
                }
            }
            mSeekBar.setMax(player.getDuration());
            int endTime = player.getDuration();
            mEndPos.setText(String.format(Locale.getDefault(), "%d : %d",
                    TimeUnit.MILLISECONDS.toMinutes(endTime),
                    TimeUnit.MILLISECONDS.toSeconds(endTime) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.
                                    toMinutes(endTime)))
            );
            mSeekBar.setVisibility(View.VISIBLE);
            findViewById(R.id.playback_controls).setVisibility(View.VISIBLE);
            //create a new thread to change progress on the seek bar
            new Thread() {
                public void run() {
                    songPos = 0;
                    while (songPos < player.getDuration()) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        songPos += 500;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                songPos = player.getCurrentPosition();
                                mSeekBar.setProgress(songPos);
                                int currTime = player.getCurrentPosition();
                                mCurrentPos.setText(String.format(Locale.getDefault(), "%d : %d",
                                        TimeUnit.MILLISECONDS.toMinutes(currTime),
                                        TimeUnit.MILLISECONDS.toSeconds(currTime) -
                                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.
                                                        toMinutes(currTime)))
                                );
                            }
                        });
                    }
                }
            }.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        player.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkPermissionsDenied()) {
            requestPermissions(PERMISSIONS, 12345);
            return;
        }
        //create a player if one doesn't exist already
        if (!isPlayerCreated) {
            mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    //user stops touching the bar
                    player.seekTo(seekBar.getProgress());
                    int currTime = player.getCurrentPosition();
                    mCurrentPos.setText(String.format(Locale.getDefault(), "%d : %d",
                            TimeUnit.MILLISECONDS.toMinutes(currTime),
                            TimeUnit.MILLISECONDS.toSeconds(currTime) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.
                                            toMinutes(currTime)))
                    );
                }
            });
            mSongs = new ArrayList<>();
            populateList();
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getBaseContext(), android.R.layout.simple_list_item_1, mSongs);
            mSongListView.setAdapter(arrayAdapter);
            mSongListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    mSongPos=position;
                    final String path = mSongs.get(position);
                    playSong(path);
                    mPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                    mSongPlaying = true;
                }
            });
            mPlayPause.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mSongPlaying) {
                        player.pause();
                        songPaused();
                    } else {
                        player.start();
                        songPlayed();
                    }
                    mSongPlaying = !mSongPlaying;
                }
            });
            mPlayPrevious.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    changeSong(--mSongPos);
                }
            });
            mPlayNext.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    changeSong(++mSongPos);
                }
            });
            isPlayerCreated = true;
        }
    }
    private void songPlayed(){
        mPlayPause.setImageResource(android.R.drawable.ic_media_pause);
    }
    private void songPaused(){
        mPlayPause.setImageResource(android.R.drawable.ic_media_play);
    }
    private void changeSong(int pos){
        if(pos<0)
            pos=mSongPos=mSongs.size()-1;
        else if(pos==mSongs.size())
            pos=mSongPos=0;
        playSong(mSongs.get(pos));
    }

    private void getFileList(File dir) {
        File root = new File(dir.getAbsolutePath());
        File[] list = root.listFiles();
        if (list == null) return;
        for (File f : list) {
            if (f.isDirectory()) {
                getFileList(f);
            } else {
                if (f.getName().contains(".mp3")) {
                    mSongs.add(f.getAbsolutePath());
                }
            }
        }
    }

    private void populateList() {
        mSongs.clear();
        getFileList(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
        getFileList(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC));
    }

}
