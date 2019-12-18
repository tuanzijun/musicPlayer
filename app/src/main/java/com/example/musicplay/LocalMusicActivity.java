package com.example.musicplay;

import android.Manifest;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.List;

public class LocalMusicActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private MusicService musicService;
    private MusicAdapter adapter;
    private Boolean Exist = false;
    ListView listView;
    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            musicService = ((MusicService.MyBinder) service).getService();
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,};
    private List<Music> musics;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.localmusic);
        getSupportActionBar().hide();
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
//
        listView = (ListView) findViewById(R.id.listView);
        //获取权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},REQUEST_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_EXTERNAL_STORAGE);
        }

        dbHelper = new DatabaseHelper(this,"login.db",null,1);
        Toast.makeText(LocalMusicActivity.this,"succeeded",Toast.LENGTH_SHORT).show();
//
        Findmusic findmusic = new Findmusic();
        musics = findmusic.getmusics(LocalMusicActivity.this.getContentResolver());   //找到资源，music型组
        adapter = new MusicAdapter(LocalMusicActivity.this,R.layout.musicitem,musics); //新建想对应的适配器
        listView.setAdapter(adapter);
//
        listView.setOnItemClickListener (new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id){
                Music music = musics.get(position);
                String url = music.getUrl();
                String title = music.getTitle();
                String artist = music.getArtist();

                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                Cursor cursor = db.query("login",null,null,null,null,null,null);
                Log.e("huizhong","当前歌曲的title是："+title );
                for (int i = 0; i < cursor.getCount(); i++) {
                    cursor.moveToNext();
                    Log.e("huizhong","当前游标title是："+cursor.getString(cursor.getColumnIndex("title")));
                    if(title.equals(cursor.getString(cursor.getColumnIndex("title")))) {
                        Log.e("huizhong","已经存在歌曲，不插入了" );
                        Exist = true;
                        break;
                    }
                }
                Log.e("huizhong","当前歌曲是否存在 "+Exist );
                if(Exist==false) {
                    Log.e("huizhong", "创建键");
                    values.put("title", title);
                    values.put("artist", artist);
                    values.put("url", url);
                    db.insert("login", null, values);
                    values.clear();
                    Log.e("huizhong", "成功插入login表");
                    Exist = false;
                }
                cursor.close();
                Intent intent = new Intent("startnew");
                intent.putExtra("url",url);
                intent.putExtra("title",title);
                intent.putExtra("artist",artist);

                final Intent eintent = new Intent(createExplicitFromImplicitIntent(LocalMusicActivity.this,intent));
                bindService(eintent,conn, Service.BIND_AUTO_CREATE);
                startService(eintent);
            }
        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(conn);
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
}
