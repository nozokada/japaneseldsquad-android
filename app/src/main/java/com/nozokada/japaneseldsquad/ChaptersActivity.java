package com.nozokada.japaneseldsquad;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;

import io.realm.Realm;
import io.realm.RealmResults;

public class ChaptersActivity extends AppCompatActivity {
    private Realm realm;

    private String bookId;
    private boolean slided;
    private String targetBookName;
    private Book targetBook;
    private RealmResults<Scripture> chaptersList;
    private RealmResults<Scripture> titleChaptersList;

    private ArrayAdapter<Scripture> arrayAdapter;

    private SharedPreferences settings;
    private SwitchCompat dualSwitch;
    private boolean dualEnabled;
    private boolean secondaryExists = true;

    private boolean gsViewed = false;
    private boolean hymnsViewed = false;

    private Toolbar toolbar;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        settings = getSharedPreferences(Constant.PREFS_NAME, 0);
        dualEnabled = settings.getBoolean("dualEnabled", false);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        listView = findViewById(R.id.list);

        realm = Realm.getDefaultInstance();

        if (savedInstanceState == null) {
            bookId = getIntent().getExtras().getString("id");
            slided = getIntent().getExtras().getBoolean("slided", false);

            if (slided) overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }
        else {
            bookId = savedInstanceState.getString("id");
        }

        targetBook = realm.where(Book.class).equalTo("id", bookId).findFirst();
        targetBookName = targetBook.getName_primary();

        if (targetBook.getLink().startsWith("gs")) gsViewed = true;
        if (targetBook.getLink().startsWith("hymns")) hymnsViewed = true;

        if (gsViewed || hymnsViewed)
            titleChaptersList = targetBook.getChild_scriptures().where().equalTo("verse", "title").findAll().sort("id");

        chaptersList = targetBook.getChild_scriptures().where().equalTo("verse", "counter").findAll().sort("id");

        if (titleChaptersList != null) {
            if (titleChaptersList.first().getScripture_secondary().equals(""))
                secondaryExists = false;
        }

        getSupportActionBar().setTitle(targetBookName);

        arrayAdapter = new ArrayAdapter<Scripture>(this, R.layout.list_item, android.R.id.text1, chaptersList) {
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                dualEnabled = settings.getBoolean("dualEnabled", false);

                String jpText = chaptersList.get(position).getScripture_primary() + " ";

                if (gsViewed || hymnsViewed)
                    jpText += " " + titleChaptersList.get(position).getScripture_primary().replaceAll("<[^>]*>", "");

                if (dualEnabled && secondaryExists) {
                    String enText = chaptersList.get(position).getScripture_secondary() + " ";

                    if (gsViewed || hymnsViewed)
                        enText += " " + titleChaptersList.get(position).getScripture_secondary().replaceAll("<[^>]*>", "");

                    TextView text1 = view.findViewById(android.R.id.text1);
                    text1.setText(jpText);

                    TextView text2 = view.findViewById(android.R.id.text2);
                    text2.setVisibility(View.VISIBLE);
                    text2.setText(enText);
                }
                else {
                    TextView text1 = view.findViewById(android.R.id.text1);
                    text1.setText(jpText);
                    TextView text2 = view.findViewById(android.R.id.text2);
                    text2.setVisibility(View.GONE);
                }

                return view;
            }
        };

        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), ContentActivity.class);
                intent.putExtra("id", targetBook.getId());
                intent.putExtra("name", targetBookName);
                intent.putExtra("chapter", position + 1);
                intent.putExtra("verse", "");
                intent.putExtra("count", chaptersList.size());
                intent.putExtra("slided", true);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        invalidateOptionsMenu();
        arrayAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem item = menu.findItem(R.id.switchEng);
        item.setActionView(R.layout.switch_eng);

        dualEnabled = settings.getBoolean("dualEnabled", false);

        dualSwitch = item.getActionView().findViewById(R.id.switchForActionBar);
        dualSwitch.setChecked(dualEnabled);

        dualSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("dualEnabled", isChecked);
                editor.apply();

                arrayAdapter.notifyDataSetChanged();
            }
        });

        if (secondaryExists) dualSwitch.setEnabled(true);
        else dualSwitch.setEnabled(false);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.bookmarks) {
            Intent intent = new Intent(getApplicationContext(), BookmarksActivity.class);
            startActivity(intent);
        }
        else if(id == R.id.search) {
            Intent intent = new Intent(getApplicationContext(), SearchActivity.class);
            startActivity(intent);
        }
        else if(id == R.id.home) {
            Intent intent = new Intent(getApplicationContext(), BooksActivity.class);
            intent.putExtra("id", "0");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
//        Log.d("ChaptersActivity", "onSaveInstanceState");
        super.onSaveInstanceState(outState);

        outState.putString("id", bookId);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        realm.close();
    }
}
