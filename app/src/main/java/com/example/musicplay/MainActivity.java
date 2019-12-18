package com.example.musicplay;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.ServiceConfigurationError;

import static com.example.musicplay.playlist.createExplicitFromImplicitIntent;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private ImageButton localmusic,play,playlist,playnext,playlast;
    private Button chose;
    private SeekBar seekBar;
    private TextView textView,textView2;
    private MusicService musicService;
    private DatabaseHelper dbHelper;
    private String CurrentTitle = "CurrentTitle";
    private GestureDetector mGestureDetector;
    private int flag = 0;//0:顺序播放 1:随机播放 2:单曲循环
    private final String[] item ={"顺序播放","随机播放","单曲循环"};
    private List<String> song_title = new ArrayList<String>();
    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            musicService = ((MusicService.MyBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        init();
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE);

        IntentFilter filter = new IntentFilter();
        filter.addAction("seekbarmaxprogress");
        filter.addAction("seekbarprogress");
        filter.addAction("gettitle");
        filter.addAction("pauseimage");
        filter.addAction("playimage");
        filter.addAction("nextsong");
        registerReceiver(broadcastReceiver, filter);

        dbHelper = new DatabaseHelper(this,"login.db",null,1);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                if (fromUser) {
                    Intent intent = new Intent("changed");
                    intent.putExtra("seekbarprogress", progress);
                    final Intent eiintent = new Intent(createExplicitFromImplicitIntent(MainActivity.this,intent));
                    bindService(eiintent,conn, Service.BIND_AUTO_CREATE);
                    startService(eiintent);
                }
            }
        });

        mGestureDetector = new GestureDetector(this,new GestureDetector.SimpleOnGestureListener(){
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if((e1.getRawX() - e2.getRawX()) >200){
                    Intent intent = new Intent(MainActivity.this,playlist.class);
                    startActivity(intent);
                    return true;
                }
                if((e2.getRawX() - e1.getRawX()) >200){
                    Intent intent = new Intent(MainActivity.this,LocalMusicActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.animator.lefttodleft,R.animator.righttoleft);
                    return true;
                }
                return super.onFling(e1, e2, velocityX, velocityY);
            }
        });


    }
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("seekbarmaxprogress")) {
                seekBar.setMax(intent
                        .getIntExtra("seekbarmaxprogress", 100));
            } else if (intent.getAction().equals("seekbarprogress")) {
                seekBar.setProgress(intent
                        .getIntExtra("seekbarprogress", 0));
            } else if (intent.getAction().equals("pauseimage")) {
                play.setBackgroundResource(R.drawable.pause);
            } else if (intent.getAction().equals("playimage")) {
                play.setBackgroundResource(R.drawable.play);
            } else if (intent.getAction().equals("gettitle")) {
                CurrentTitle = intent.getStringExtra("title");
                Log.e("Music","CurrentTitle = "+CurrentTitle);
                textView2.setText(intent.getStringExtra("artist"));
                textView.setText(intent.getStringExtra("title"));}
            else if (intent.getAction().equals("nextsong")) {
                Log.e("Music","歌曲播放结束，接收到广播，发送下一首歌曲");
                SQLiteDatabase dbb = dbHelper.getWritableDatabase();
                Cursor cursorr = dbb.query("login",null,null,null,null,null,null);
                cursorr.moveToFirst();
                do{
                    Log.e("Music","CurrentTitle = "+CurrentTitle);  //播放列表不会有同名歌曲，所以根据标题对比
                    if(CurrentTitle.equals(cursorr.getString(cursorr.getColumnIndex("title")))) {
                        Log.e("Music","找到匹配");
                        if(flag == 0)
                            cursorr.moveToNext();
                        else if(flag == 1){
                            int i = new Random().nextInt(cursorr.getColumnCount());
                            cursorr.moveToPosition(i);
                        }
                        else if (flag == 2){
                            int j = cursorr.getPosition();
                            cursorr.moveToPosition(j);
                        }
                        if(cursorr.isAfterLast()) {
                            Log.e("Music","当前歌曲在最后一行返回第一行");
                            cursorr.moveToFirst();
                            String url = cursorr.getString(cursorr.getColumnIndex("url"));
                            String title = cursorr.getString(cursorr.getColumnIndex("title"));
                            String artist = cursorr.getString(cursorr.getColumnIndex("artist"));
                            Intent intent2 = new Intent("startnew");
                            intent2.putExtra("url",url);
                            intent2.putExtra("title",title);
                            intent2.putExtra("artist",artist);
                            final Intent eiiintent = new Intent(createExplicitFromImplicitIntent(MainActivity.this,intent2));
                            bindService(eiiintent,conn, Service.BIND_AUTO_CREATE);
                            startService(eiiintent);
                            break;
                        }   else {
                            Log.e("Music", "当前歌曲不是在最后一行");
                            String url = cursorr.getString(cursorr.getColumnIndex("url"));
                            String title = cursorr.getString(cursorr.getColumnIndex("title"));
                            String artist = cursorr.getString(cursorr.getColumnIndex("artist"));
                            cursorr.moveToLast();
                            Intent intent2 = new Intent("startnew");
                            intent2.putExtra("url", url);
                            intent2.putExtra("title", title);
                            intent2.putExtra("artist", artist);
                            final Intent eiintent = new Intent(createExplicitFromImplicitIntent(MainActivity.this, intent2));
                            bindService(eiintent, conn, Service.BIND_AUTO_CREATE);
                            startService(eiintent);
                            break;
                        }
                    }
                }while(cursorr.moveToNext());
                cursorr.close();
            }
        }
    };

    public void init(){
        textView2 = (TextView)findViewById(R.id.textView2);
        textView = (TextView)findViewById(R.id.textView);
        play = (ImageButton)findViewById(R.id.playing);
        seekBar = (SeekBar)findViewById(R.id.seekBar);
        localmusic = (ImageButton)findViewById(R.id.localmusic);
        playlist = (ImageButton) findViewById(R.id.playlist);
        playnext = (ImageButton) findViewById(R.id.playnext);
        playlast = (ImageButton) findViewById(R.id.playlast);
        chose = (Button)findViewById(R.id.chose_play);
        chose.setOnClickListener(this);
        playlast.setOnClickListener(this);
        playnext.setOnClickListener(this);
        play.setOnClickListener(this);
        localmusic.setOnClickListener(this);
        playlist.setOnClickListener(this);

    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.playing:
                musicService.start();
                break;
            case R.id.localmusic:
                Intent intent = new Intent(MainActivity.this,LocalMusicActivity.class);
                startActivity(intent);
                break;
            case R.id.playlist:
                Intent intent3 = new Intent(MainActivity.this,playlist.class);
                startActivity(intent3);
                break;
            case R.id.playnext:
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                Cursor cursor = db.query("login",null,null,null,null,null,null);
                cursor.moveToFirst();
                do{
                    Log.e("Music","CurrentTitle = "+CurrentTitle);  //播放列表不会有同名歌曲，所以根据标题对比
                    if(CurrentTitle.equals(cursor.getString(cursor.getColumnIndex("title")))) {
                        Log.e("Music","找到匹配");
                        if(flag == 0)
                            cursor.moveToNext();
                        else if(flag == 1){
                            int i = new Random().nextInt(cursor.getColumnCount());
                            cursor.moveToPosition(i);
                        }
                        else if (flag == 2){
                            int j = cursor.getPosition();
                            cursor.moveToPosition(j);
                        }
                        if(cursor.isAfterLast()) {
                            Log.e("Music","当前歌曲在最后一行返回第一行");
                            cursor.moveToFirst();
                            String url = cursor.getString(cursor.getColumnIndex("url"));
                            String title = cursor.getString(cursor.getColumnIndex("title"));
                            String artist = cursor.getString(cursor.getColumnIndex("artist"));
                            Intent intent2 = new Intent("startnew");
                            intent2.putExtra("url",url);
                            intent2.putExtra("title",title);
                            intent2.putExtra("artist",artist);
                            final Intent eiiintent = new Intent(createExplicitFromImplicitIntent(MainActivity.this,intent2));
                            bindService(eiiintent,conn, Service.BIND_AUTO_CREATE);
                            startService(eiiintent);
                            break;
                        }   else{
                            Log.e("Music","当前歌曲不是在最后一行");
                            String url = cursor.getString(cursor.getColumnIndex("url"));
                            String title = cursor.getString(cursor.getColumnIndex("title"));
                            String artist = cursor.getString(cursor.getColumnIndex("artist"));
                            cursor.moveToLast();
                            Intent intent2 = new Intent("startnew");
                            intent2.putExtra("url",url);
                            intent2.putExtra("title",title);
                            intent2.putExtra("artist",artist);
                            final Intent eiintent = new Intent(createExplicitFromImplicitIntent(MainActivity.this,intent2));
                            bindService(eiintent,conn, Service.BIND_AUTO_CREATE);
                            startService(eiintent);
                            break;
                        }
                    }
                }while(cursor.moveToNext());
                cursor.close();
                break;
            case R.id.playlast:
                SQLiteDatabase dbb = dbHelper.getWritableDatabase();
                Cursor cursorr = dbb.query("login",null,null,null,null,null,null);
                cursorr.moveToFirst();
                do{
                    if(CurrentTitle.equals(cursorr.getString(cursorr.getColumnIndex("title")))) {

                        cursorr.moveToPrevious();
                        if(cursorr.isBeforeFirst()){
                            cursorr.moveToLast();
                            String url = cursorr.getString(cursorr.getColumnIndex("url"));
                            String title = cursorr.getString(cursorr.getColumnIndex("title"));
                            String artist = cursorr.getString(cursorr.getColumnIndex("artist"));
                            Intent intent8 = new Intent("startnew");
                            intent8.putExtra("url",url);
                            intent8.putExtra("title",title);
                            intent8.putExtra("artist",artist);
                            final Intent eeiintent = new Intent(createExplicitFromImplicitIntent(MainActivity.this,intent8));
                            bindService(eeiintent,conn, Service.BIND_AUTO_CREATE);
                            startService(eeiintent);
                            break;
                        }
                        else{
                            String url = cursorr.getString(cursorr.getColumnIndex("url"));
                            String title = cursorr.getString(cursorr.getColumnIndex("title"));
                            String artist = cursorr.getString(cursorr.getColumnIndex("artist"));
                            cursorr.moveToNext();
                            Intent intent8 = new Intent("startnew");
                            intent8.putExtra("url",url);
                            intent8.putExtra("title",title);
                            intent8.putExtra("artist",artist);
                            final Intent eeiintent = new Intent(createExplicitFromImplicitIntent(MainActivity.this,intent8));
                            bindService(eeiintent,conn, Service.BIND_AUTO_CREATE);
                            startService(eeiintent);
                            break;
                        }

                    }
                }while(cursorr.moveToNext());
                cursorr.close();
                break;
            case R.id.chose_play:
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("选择播放模式");
                builder.setItems(item, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        chose.setText(item[which]);
                        flag = which;
                    }
                });
                builder.create().show();
                break;
            default:
                break;
        }
    }
    public static Intent createExplicitFromImplicitIntent(Context context, Intent implicitIntent) {
        // Retrieve all services that can match the given intent
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfo = pm.queryIntentServices(implicitIntent, 0);

        // Make sure only one match was found
        if (resolveInfo == null || resolveInfo.size() != 1) {
            return null;
        }

        // Get component info and create ComponentName
        ResolveInfo serviceInfo = resolveInfo.get(0);
        String packageName = serviceInfo.serviceInfo.packageName;
        String className = serviceInfo.serviceInfo.name;
        ComponentName component = new ComponentName(packageName, className);

        // Create a new intent. Use the old one for extras and such reuse
        Intent explicitIntent = new Intent(implicitIntent);

        // Set the component to be explicit
        explicitIntent.setComponent(component);

        return explicitIntent;
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(conn);
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //2.让手势识别器生效
        mGestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }


}
