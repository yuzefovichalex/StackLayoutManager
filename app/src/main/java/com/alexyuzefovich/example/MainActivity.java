package com.alexyuzefovich.example;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import com.alexyuzefovich.stacklayoutmanager.StackLayoutManager;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        CardsAdapter cardsAdapter = new CardsAdapter();
        cardsAdapter.setItems(generateItems());
        recyclerView.setAdapter(cardsAdapter);
        StackLayoutManager stackLayoutManager = new StackLayoutManager();
        stackLayoutManager.setBottomOffset(50);
        stackLayoutManager.setScaleFactor(0.5f);
        recyclerView.setLayoutManager(stackLayoutManager);
    }

    private List<String> generateItems() {
        List<String> items = new ArrayList<>();
        items.add("#077c11");
        items.add("#e29cfd");
        items.add("#9bdaaa");
        items.add("#523bef");
        items.add("#fc2769");
        items.add("#feba7e");
        items.add("#6c15aa");
        items.add("#6d6b59");
        items.add("#08a0ff");
        items.add("#844adb");
        return items;
    }
}
