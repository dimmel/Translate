package com.dmitry.translate;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit.Call;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;


public class TwoActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, View.OnClickListener{
    private static final String TAG = "myLogs";
    String fileName = "translateDB";
    DBHelper dbHelper;
    ListView lv2;
    Button bt2, bt3;
    ToggleButton tb;
    SQLiteDatabase db;
    String word;
    int ID;
    String wordTrans;
    String local = "en";
    String vvodimiyText;
    String ruWord, enWord;
    ContentValues cv = new ContentValues();
    String LANG = "en-ru";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_two);

        try{delTable();}catch (Exception e){e.printStackTrace();}

        lv2 = (ListView)findViewById(R.id.listView2);

        lv2.setOnItemClickListener(this);
        bt2 = (Button) findViewById(R.id.button2);
        bt3 = (Button) findViewById(R.id.button3);
        bt2.setOnClickListener(this);
        bt3.setOnClickListener(this);
        bt2.setEnabled(false);
        bt3.setEnabled(false);
        tb = (ToggleButton) findViewById(R.id.toggleButton);
        tb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try{delTable();}catch (Exception e){e.printStackTrace();}
                if (isChecked){Log.d(TAG, "включено"); local = "ru"; readDB();}
                else {Log.d(TAG, "вЫключено"); local="en";readDB();}
            }
        });

        //Исключаем ошибку с правами на более позние версии SDK
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        try {readDB();}catch (Exception e){e.printStackTrace();Log.d(TAG, "ОШИБКА ЧТЕНИЯ БАЗЫ ПРИ ЗАПУСКЕ!!!");}
        //Прячем клавиатуру при старте
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

    }

    void readDB() {
        String SORT=null;
        dbHelper = new DBHelper(this);
        db=dbHelper.getWritableDatabase();
        try {
            //забиваю данные из БД в лист
            List<String> list= new ArrayList<String>();
            if(local=="ru"){SORT="ru";}
            if(local=="en"){SORT="en";}
            Cursor listCurs = db.query("translateTable", null, null, null, null, null, SORT);
            if (listCurs.moveToFirst()) {
                //если английское слово вводится - вывести все англ слова из базы
                if (local=="en") {
                    do {
                        list.add(listCurs.getString(1));

                    } while (listCurs.moveToNext());
                }
                //если русские слова вводятся - вывести все русские слова из базы
                else if (local=="ru") {
                    do {
                        list.add(listCurs.getString(2));

                    } while (listCurs.moveToNext());
                }
                //при старте вывести всю базу (и русс и англ слова)
                else {
                    do {
                        list.add(listCurs.getString(2)+" - "+listCurs.getString(1));
                    } while (listCurs.moveToNext());
                }

            }
            if (listCurs != null && !listCurs.isClosed()) {
                listCurs.close();
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_multiple_choice, list);
            //прикручиваю адаптер
            lv2.setAdapter(adapter);
            lv2.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        }
        catch(Throwable t) {
            Toast.makeText(getApplicationContext(),
                    "Exception: " + t.toString(), Toast.LENGTH_LONG).show();
        }
        dbHelper.close();
    }

    void addToDB() {
        dbHelper = new DBHelper(this);
        db=dbHelper.getWritableDatabase();
        cv.put("en", enWord);
        cv.put("ru", ruWord);
        db.insert("sendTable", null, cv);
        dbHelper.close();
    }

/*    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.share:
                final Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                *//*intent.putExtra(Intent.EXTRA_SUBJECT, "_SUBJECT_");*//*
                intent.putExtra(Intent.EXTRA_TEXT, "ЭТО НЕ ТО! НАДО БД ОТПРАВЛЯТЬ!");
                startActivity(Intent.createChooser(intent, getString(R.string.app_name)));
                break;
            case R.id.send:

                break;
        }
        return super.onOptionsItemSelected(item);
    }*/


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        bt2.setEnabled(true);
        bt3.setEnabled(true);
        //выбор слова, по которому кликнули
        word = ((TextView) view).getText().toString();
        Log.d(TAG, "word=" + word);
        //выбираем слово из базе данных по клику(локаль еще не определена, первый выбор из полного списка)
        searchWordInDB(1);
    }


    //Ищем слово в базе данных в том случае, если нажали на кнопку SEARCH вместо клика по слову, таким образом избегаем двоного сохранения в базе
    private void searchWordInDB(int arg) {
        dbHelper = new DBHelper(this);
        db = dbHelper.getWritableDatabase();

        switch (arg) {
            case 1:
                Cursor cursSearch = db.query("sendTable", null, null, null, null, null, null);
                cursSearch.moveToFirst();
                int checkWord = 0;
                if (cursSearch.moveToFirst()) {
                    do {
                        if (local == "en") {
                            String wordDB = cursSearch.getString(1);

                            if (wordDB.equals(word)) {
                                ID = cursSearch.getInt(0);
                                delROW();
                                Log.d(TAG, "ENGLISH FIND");
                                checkWord = 1;
                            }
                        } else if (local == "ru") {
                            String wordDB = cursSearch.getString(2);
                            Log.d(TAG, "word=" + word + " wordDB=" + wordDB);

                            if (wordDB.equals(word)) {
                                ID = cursSearch.getInt(0);
                                delROW();
                                Log.d(TAG, "RUSSIAN FIND");
                                checkWord = 1;
                            }
                        } else {
                            Log.d(TAG, "что-то пошло не так в searchWordInDB(1)");

                            /*searchWordInDB(1);*/

                        }
                    } while (cursSearch.moveToNext());
                }
                if (checkWord == 0) {
                    searchWordInDB(2);
                }
                cursSearch.close();
                break;
            case 2:
/*                dbHelper = new DBHelper(this);
                db=dbHelper.getWritableDatabase();*/
                Cursor cursSearch2 = db.query("translateTable", null, null, null, null, null, null);
                cursSearch2.moveToFirst();
                do {
                    if(local=="en"){

                        String wordDB = cursSearch2.getString(1);
                        if(wordDB.equals(word)){
                            enWord=word;
                            wordTrans = cursSearch2.getString(2);
                            ruWord = wordTrans;
                            Log.d(TAG, "enWORD="+enWord+" ruWord="+ruWord);
                        }
                    }
                    else if(local=="ru"){

                        String wordDB = cursSearch2.getString(2);
                        if(wordDB.equals(word)){
                            ruWord=word;
                            wordTrans = cursSearch2.getString(1);
                            enWord=wordTrans;
                            Log.d(TAG, "ruWord="+ruWord+" enWORD="+enWord);
                            }
                    }
                    else{Log.d(TAG, "что-то пошло не так in REAL db!!!");}
                }while (cursSearch2.moveToNext());
/*                dbHelper.close();*/
                cursSearch2.close();
                addToDB();
                break;
        }
        dbHelper.close();

    }

    private void delROW() {
        int delCount = db.delete("sendTable", "id = " + ID, null);
        Log.d(TAG, "deleted rows count = " + delCount);
    }

    private void delTable() {
        try {
            dbHelper = new DBHelper(this);
            db = dbHelper.getWritableDatabase();
            int clearCount = db.delete("sendTable", null, null);
            Log.d(TAG, "deleted rows count = " + clearCount);
            dbHelper.close();
        }catch (Exception e){e.printStackTrace();}
    }

    private void delRowsFromTranslateTable() {
        dbHelper = new DBHelper(this);
        db = dbHelper.getWritableDatabase();
        Cursor cursDel = db.query("sendTable", null, null, null, null, null, null);
        Cursor cursDel2= db.query("translateTable", null, null, null, null, null, null);
        int delRowsFromTranslateTable;
        int id_translate;
        String entextInSendTable, rutextInSendTable;
        String entextInTranslateTable, rutextInTranslateTable;
        cursDel.moveToFirst();
        do {
            cursDel2.moveToFirst();
            entextInSendTable = cursDel.getString(1);
            rutextInSendTable = cursDel.getString(2);
            do {
                id_translate = cursDel2.getInt(0);
                entextInTranslateTable = cursDel2.getString(1);
                rutextInTranslateTable = cursDel2.getString(2);
                if(entextInSendTable.equals(entextInTranslateTable) && rutextInSendTable.equals(rutextInTranslateTable)){

                    delRowsFromTranslateTable = db.delete("translateTable", "id = " + id_translate, null);
                    Log.d(TAG, "deleted rows count = " + delRowsFromTranslateTable + " id=" + id_translate);
                }

            } while (cursDel2.moveToNext());

        } while (cursDel.moveToNext());
        cursDel2.close();
        cursDel.close();
        dbHelper.close();
        Intent intents = new Intent(this,MainActivity.class);
        startActivity(intents);
        finish();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button2:
                try {

                    final Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");

                    dbHelper = new DBHelper(this);
                    db = dbHelper.getWritableDatabase();
                    Cursor cursSearch4 = db.query("sendTable", null, null, null, null, null, null);
                    cursSearch4.moveToFirst();
                    String SEND_TEXT="Отправлено из приложения \"Переводчик-Блокнот\" https://play.google.com/store/apps/developer?id=Dmitry+Melnichenko\"";
                    do {
                        if (local == "en") {
                            SEND_TEXT = SEND_TEXT+"\nСлово: "+cursSearch4.getString(1)+", перевод: "+cursSearch4.getString(2);
                        } else if (local == "ru") {
                            SEND_TEXT = SEND_TEXT+"\nСлово: "+cursSearch4.getString(2)+", перевод: "+cursSearch4.getString(1);
                        } else {
                            Log.d(TAG, "что-то пошло не так ПРИ ШАРИНГЕ!!!");
                        }
                    } while (cursSearch4.moveToNext());
                    cursSearch4.close();
                    dbHelper.close();

                    intent.putExtra(Intent.EXTRA_TEXT, SEND_TEXT);
                    startActivity(Intent.createChooser(intent, getString(R.string.app_name)));
                } catch (Exception e) {e.printStackTrace();Intent intents = new Intent(this,MainActivity.class);
                    startActivity(intents);}
                //finish();
                break;
            case R.id.button3:
                try {
                    delRowsFromTranslateTable();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                break;
        }

    }


    class DBHelper extends SQLiteOpenHelper
    {
        public DBHelper(Context context) {
            super(context, fileName, null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.d(TAG, "--- onCreate database ---");
            String base2, mytable = "sendTable";
            base2 = "create table " + mytable + "("
                    + "id integer primary key autoincrement,"
                    + "en text,"
                    + "ru text" + ");";
            db.execSQL(base2);

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }

    @Override
    public void onBackPressed() {
        Intent intents = new Intent(this,MainActivity.class);
        startActivity(intents);
        super.onBackPressed();
    }
}
