package com.fahim.servertest;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import com.fahim.servertest.databinding.ActivityMainBinding;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;


public class MainActivity extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = DataBindingUtil.setContentView(this,R.layout.activity_main);

        MainActivityViewModel viewModel = ViewModelProviders.of(this).get(MainActivityViewModel.class);
        viewModel.init();

        binding.setLifecycleOwner(this);
        binding.setViewModel(viewModel);

        binding.graph1.addSeries(viewModel.getGraph1Series().getValue());
        binding.graph2.addSeries(viewModel.getGraph2Series().getValue());

        binding.graph1.getViewport().setXAxisBoundsManual(true);
        binding.graph1.getViewport().setMinX(0);
        binding.graph1.getViewport().setMaxX(2000);
        binding.graph1.getViewport().setScalable(true);

        binding.graph2.getViewport().setXAxisBoundsManual(true);
        binding.graph2.getViewport().setMinX(0);
        binding.graph2.getViewport().setMaxX(2000);
        binding.graph2.getViewport().setScalable(true);

        viewModel.getGraph1Series().observe(this, series ->
                binding.graph1.onDataChanged(false,false));

        viewModel.getGraph2Series().observe(this, series ->
                binding.graph2.onDataChanged(false,false));

    }
}