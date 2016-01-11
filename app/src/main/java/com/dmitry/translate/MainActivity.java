package com.dmitry.translate;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

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


public class MainActivity extends AppCompatActivity implements TextWatcher, AdapterView.OnItemClickListener{
    private static final String TAG = "myLogs";
    String fileName = "translateDB";
    DBHelper dbHelper;
    AutoCompleteTextView actv;
    ListView lv;
    Button bt;
    final int DIALOG_EXIT = 1;
    SQLiteDatabase db;
    String word;
    String wordTrans;
    String local;
    String vvodimiyText;
    String ruWord, enWord;
    ContentValues cv = new ContentValues();
    String LANG = "en-ru";

    private final String URL = "https://translate.yandex.net";
    private final String KEY = "trnsl.1.1.20160108T135852Z.7b673d1999b55f8b.1a96a0d0ea5feda9fa8359a18664b34483707ef9";

    private Gson gson = new GsonBuilder().create();
    private Retrofit retrofit = new Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create(gson))
            .baseUrl(URL)
            .build();
    private API intf = retrofit.create(API.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        actv = (AutoCompleteTextView) findViewById(R.id.actv);
        lv = (ListView)findViewById(R.id.listView);
        actv.addTextChangedListener(this);
        lv.setOnItemClickListener(this);
        bt = (Button) findViewById(R.id.button);
        bt.setEnabled(false);


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
        db=dbHelper.getWritableDatabase();/*
        Log.d(TAG, "Чтение файла: " + fileName);
        Cursor curs = db.query("translateTable", null, null, null, null, null, SORT);
        if (curs.moveToFirst()){
            int idColIndex=curs.getColumnIndex("id");
            int enColindex = curs.getColumnIndex("en");
            int ruColIndex = curs.getColumnIndex("ru");
            do{
                Log.d(TAG,"ID = " + curs.getInt(idColIndex) +
                                ", en = " + curs.getString(enColindex) +
                                ", ru = " + curs.getString(ruColIndex));
            }while(curs.moveToNext());
        } else Log.d(TAG, "0 rows");
        curs.close();*/
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
                        list.add(listCurs.getString(1));
                        list.add(listCurs.getString(2));

                    } while (listCurs.moveToNext());
                }

            }
            if (listCurs != null && !listCurs.isClosed()) {
                listCurs.close();
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,   R.layout.support_simple_spinner_dropdown_item, list); //прикручиваю адаптер

            lv.setAdapter(adapter);
            actv.setAdapter(adapter);
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
        //вставить сверку значений с уже существующим в БД
        db.insert("translateTable", null, cv);
        dbHelper.close();
        readDB();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.share:
                final Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                /*intent.putExtra(Intent.EXTRA_SUBJECT, "_SUBJECT_");*/
                intent.putExtra(Intent.EXTRA_TEXT, "ЭЬТО НЕ ТО! НАДО БД ОТПРАВЛЯТЬ!");
                startActivity(Intent.createChooser(intent, getString(R.string.app_name)));
                break;
            case R.id.send:break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void search(View v) {
        //Toast.makeText(this, "SEARCH", Toast.LENGTH_SHORT).show();
        word=vvodimiyText;
        try {searchWordInDB();}catch (Exception e){e.printStackTrace();Log.d(TAG, "ОШИБКА ЧТЕНИЯ БАЗЫ при клике!!!");}
        //try{ searchWordInDB();}catch (Exception e){e.printStackTrace(); getTranslate();}


    }

    private void getTranslate() {

        Map<String,String> mapJson = new HashMap<>();
        mapJson.put("key", KEY);
        mapJson.put("text", vvodimiyText);
        mapJson.put("lang", LANG);

        Call<Object> call = intf.translate(mapJson);
        try {
            Response<Object> responce = call.execute();

                Map<String,String> map = gson.fromJson(responce.body().toString().replace("the ","").replace(" ","").replace("a ",""),Map.class);

            for(Map.Entry e : map.entrySet()){
                if(e.getKey().equals("text")){
                    Log.d(TAG, "перевод "+e.getValue().toString());
                    if (vvodimiyText.equals(e.getValue().toString().replace("[", "").replace("]", ""))){Toast.makeText(this, "Такого слова не существует!", Toast.LENGTH_SHORT).show();hideKeyboard();return;}
                        else {
                        if (local.equals("ru")) {ruWord = vvodimiyText;enWord = e.getValue().toString().replace("[", "").replace("]", "");}
                        if (local.equals("en")) {enWord = vvodimiyText;ruWord = e.getValue().toString().replace("[", "").replace("]", "");}
                        actv.setText("");
                        wordTrans =  e.getValue().toString().replace("[", "").replace("]", "");
                        showDialog(DIALOG_EXIT);
                        hideKeyboard();
                        }
                    }
            }

            addToDB();


        } catch (Exception e) {e.printStackTrace(); Log.d(TAG,"ОШИБКА скорей всего слово с проблеом");}
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        //Log.d(TAG,"Local="+getApplicationContext().getResources().getConfiguration().locale.getLanguage());
    }

    @Override
    public void afterTextChanged(Editable s) {


        vvodimiyText = actv.getText().toString().toLowerCase();

        if (vvodimiyText.length()==1) {
            bt.setEnabled(false);
            //проверка языка ввода
            testLanguage();
        }
        else if(vvodimiyText.length()>=2){bt.setEnabled(true);}
        //продолжаем код
        actv.dismissDropDown();

    }

    private void testLanguage() {
        //проверка языка ввода
        char mas[] = new char[vvodimiyText.length()];
        for (int i = 0; i < vvodimiyText.length(); i++) {
            mas[i] = vvodimiyText.charAt(i);
            //		Log.d(TAG, "mas="+mas[i]);
            if (mas[0] == 'a'||mas[0] == 'b'||mas[0] == 'c'||mas[0] == 'd'||mas[0] == 'e'||mas[0] == 'f'||mas[0] == 'g'||mas[0] == 'h'||mas[0] == 'i'||mas[0] == 'j'
                    ||mas[0] == 'k'||mas[0] == 'l'||mas[0] == 'm'||mas[0] == 'n'||mas[0] == 'o'||mas[0] == 'p'||mas[0] == 'q'||mas[0] == 'r'||mas[0] == 's'||mas[0] == 't'
                    ||mas[0] == 'u'||mas[0] == 'v'||mas[0] == 'w'||mas[0] == 'x'||mas[0] == 'y'||mas[0] == 'z') {
                Log.d(TAG, "en");
                local = "en"; readDB();
            } else {
                local = "ru"; readDB();
                Log.d(TAG, "Ru");
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //выбор слова, по которому кликнули
        word = ((TextView) view).getText().toString();
        Log.d(TAG, "word=" + word);
        //выбираем слово из базе данных по клику(локаль еще не определена, первый выбор из полного списка)
        searchWordInRealDB();
    }

    private void searchWordInRealDB() {
        dbHelper = new DBHelper(this);
        db=dbHelper.getWritableDatabase();
        Cursor cursSearch = db.query("translateTable", null, null, null, null, null, null);
        cursSearch.moveToFirst();
        do {
            if(local=="en"){
                String wordDB = cursSearch.getString(1);
                if(wordDB.equals(word)){
                    wordTrans = cursSearch.getString(2);
                    showDialog(DIALOG_EXIT);
                    actv.setText("");
                    hideKeyboard();
                   }
            }
            else if(local=="ru"){
                String wordDB = cursSearch.getString(2);
                if(wordDB.equals(word)){
                    wordTrans = cursSearch.getString(1);
                    showDialog(DIALOG_EXIT);}
                actv.setText("");
                hideKeyboard();
            }
            else{Log.d(TAG, "что-то пошло не так in REAL db!!!");
                vvodimiyText=word;
                testLanguage();
                searchWordInRealDB();}
        }while (cursSearch.moveToNext());
        dbHelper.close();
        cursSearch.close();
    }

    //метод прячет клавиатуру
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    //Ищем слово в базе данных в том случае, если нажали на кнопку SEARCH вместо клика по слову, таким образом избегаем двоного сохранения в базе
    private void searchWordInDB() {
        dbHelper = new DBHelper(this);
        db=dbHelper.getWritableDatabase();
        Cursor cursSearch = db.query("translateTable", null, null, null, null, null, null);
        cursSearch.moveToFirst();
        int checkWord=0;
        if (cursSearch.moveToFirst()) {
            do {
                if (local == "en") {
                    String wordDB = cursSearch.getString(1);

                    if (wordDB.equals(word)) {
                        wordTrans = cursSearch.getString(2);
                        showDialog(DIALOG_EXIT);
                        Log.d(TAG, "ENGLISH");
                        checkWord = 1;
                    }
                } else if (local == "ru") {
                    String wordDB = cursSearch.getString(2);
                    Log.d(TAG, "word=" + word + " wordDB=" + wordDB);

                    if (wordDB.equals(word)) {
                        wordTrans = cursSearch.getString(1);
                        showDialog(DIALOG_EXIT);
                        Log.d(TAG, "RUSSIAN");
                        checkWord = 1;
                    }
                } else {
                    Log.d(TAG, "что-то пошло не так");
                    //кликнув на русскае слово - остались все русские, на англ - англ
                    vvodimiyText = word;
                    testLanguage();
                    searchWordInDB();
                    actv.setText("");

                }
            } while (cursSearch.moveToNext());
        }
        if (checkWord==0){
            if(local=="ru"){
                Log.d(TAG, "else Russian");
                //ищем в инете русс перевод
                LANG = "ru-en";
                getTranslate();
            }
            if(local=="en"){
                //ищем в инете англ перевод
                Log.d(TAG, "else English");
                LANG = "en-ru";
                getTranslate();
            }
        }
        dbHelper.close();
        cursSearch.close();
    }

    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_EXIT) {
            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            // заголовок
            adb.setTitle(R.string.translate);
            // сообщение
            adb.setMessage("Слово: "+word+"\nПеревод: "+wordTrans);
            // иконка
            adb.setIcon(android.R.drawable.ic_dialog_info);
            // кнопка положительного ответа
            adb.setPositiveButton(R.string.ok, myClickListener);
            // создаем диалог
            return adb.create();
        }
        return super.onCreateDialog(id);
    }

    DialogInterface.OnClickListener myClickListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                // положительная кнопка
                case Dialog.BUTTON_POSITIVE:

                    removeDialog(DIALOG_EXIT);
                    /*finish();*/

                    break;
            }
        }
    };
    class DBHelper extends SQLiteOpenHelper
    {
        public DBHelper(Context context) {
            super(context, fileName, null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.d(TAG, "--- onCreate database ---");
            String base, mytable = "translateTable";
            base = "create table " + mytable + "("
                    + "id integer primary key autoincrement,"
                    + "en text,"
                    + "ru text" + ");";
            db.execSQL(base);

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }

}
