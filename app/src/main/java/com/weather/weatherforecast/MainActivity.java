package com.weather.weatherforecast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.squareup.picasso.Picasso;
import com.weather.weatherforecast.adapter.AdapterLocation;
import com.weather.weatherforecast.adapter.AdapterWeather;
import com.weather.weatherforecast.datalocal.DataManager;
import com.weather.weatherforecast.model.ItemLocation;
import com.weather.weatherforecast.model.Weather;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private ImageView imgWeather,imgMenu;
    private TextView tvTemp, tvLocation, tvStatus, tvDate, tvTempRange, tvHumidity, tvUpdate,tvAddLocation;
    private TextView tvTempFeel;
    String location;
    private SwipeRefreshLayout layout;
    private RecyclerView listWeather;
    private AdapterWeather adapterWeather;
    private List<Weather> weatherList;
    private Map<String, String> statusData;
    private DrawerLayout drawerLayout;
    private LinearLayout mainLayout;
    private RecyclerView listLocation;
    public static List<ItemLocation> locationList;
    private AdapterLocation adapterLocation;
    private ItemLocation selectLocation;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        locationList = new ArrayList<>();
        initUI();
        hideStatusBar();
        getStatusData();
        drawerLayout.setVisibility(View.GONE);
        checkFirstInstall();

        locationList = DataManager.getListLocation();
        for(int i = 0; i < locationList.size(); i++){
            if(locationList.get(i).isSelected()){
                selectLocation = locationList.get(i);
                location = locationList.get(i).getId();
                break;
            }
        }
        adapterLocation = new AdapterLocation(this, new AdapterLocation.IAdapter() {
            @Override
            public void clickChanged(ItemLocation itemLocation) {
                for(int i = 0; i < locationList.size(); i++){
                    if(locationList.get(i).isSelected()){
                        locationList.get(i).setSelected(false);
                    }
                }
                itemLocation.setSelected(true);
                updateData(itemLocation.getId());
                selectLocation = itemLocation;
                location = itemLocation.getId();
                drawerLayout.closeDrawers();
            }
        });
        listLocation.setAdapter(adapterLocation);
        listLocation.setLayoutManager(new LinearLayoutManager(this));
        adapterLocation.setData(locationList);

        adapterWeather = new AdapterWeather(this);
        listWeather.setLayoutManager(new LinearLayoutManager(this,RecyclerView.HORIZONTAL,true));
        listWeather.setAdapter(adapterWeather);
        updateData(location);
        layout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getData(location);
                setListData(location);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        layout.setRefreshing(false);
                    }
                },4000);
            }
        });
        imgMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        tvAddLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(),LocationActivity.class);
                drawerLayout.closeDrawers();
                startActivity(intent);
            }
        });
    }

    private void checkFirstInstall()  {
        if(!DataManager.getFirstInstall()){
            try {
                getAllLocation();
            } catch (IOException e) {
                e.printStackTrace();
            }
            location = selectLocation.getId();
            getData(location);
            setListData(location);
            DataManager.setFirstInstall(true);
        }
    }

    private void getAllLocation() throws IOException {
        InputStream inputStream = this.getResources().openRawResource(R.raw.citylist);
        BufferedReader bf = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();
        String s = null;
        while((s = bf.readLine())!=null){
            sb.append(s);
            sb.append("\n");
        }
        s = sb.toString();
        List<ItemLocation> list = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(s);
            for(int i = 0; i < jsonArray.length(); i++){
                JSONObject object = jsonArray.getJSONObject(i);
                ItemLocation itemLocation = new ItemLocation(object.getString("name"),String.valueOf(object.getInt("id")),false);
                list.add(itemLocation);
                Log.e("Location",itemLocation.getId());
                if(itemLocation.getId().equalsIgnoreCase("1581129")){
                    itemLocation.setSelected(true);
                    locationList.add(itemLocation);
                    selectLocation = itemLocation;
                    DataManager.setListLocation(locationList);
                }
            }
            DataManager.setListAllLocation(list);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private void hideStatusBar() {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(getApplicationContext(),R.color.transparent));
    }


    private void getStatusData() {
        statusData = new HashMap<>();
        statusData.put("overcast clouds","U ??m");
        statusData.put("broken clouds","C?? m??y");
        statusData.put("scattered clouds","C?? m??y");
        statusData.put("few clouds","??t m??y");
        statusData.put("clear sky", "Quang ????ng");

        statusData.put("Smoke","C?? s????ng m??");
        statusData.put("Haze","C?? s????ng m??");
        statusData.put("Dust","C?? s????ng m??");
        statusData.put("Fog","C?? s????ng m??");
        statusData.put("Ash","C?? s????ng m??");
        statusData.put("Squall","C?? s????ng m??");
        statusData.put("Tornado","C?? s????ng m??");
        statusData.put("Snow","C?? tuy???t");
        statusData.put("Drizzle","M??a ph??n");
        statusData.put("Rain","M??a r??o");
        statusData.put("Thunderstorm","B??o c?? s???m s??t");

    }

    private void setListData(String location) {
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        String url = "https://api.openweathermap.org/data/2.5/forecast/daily?id="+location+"&units=metric&cnt=5&appid=53fbf527d52d4d773e828243b90c1f8e";
        StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    weatherList = new ArrayList<>();
                    weatherList.clear();
                    JSONObject jsonObject = new JSONObject(response);
                    JSONArray array = jsonObject.getJSONArray("list");
                    for(int i = 4; i >=0 ; i--){
                        JSONObject object = array.getJSONObject(i);
                        String location = jsonObject.getJSONObject("city").getString("name");
                        int temp =(int) object.getJSONObject("temp").getDouble("day") ;
                        int tempMin = (int) object.getJSONObject("temp").getDouble("min");
                        int tempMax = (int) object.getJSONObject("temp").getDouble("max");
                        long date = object.getLong("dt");
                        int humidity = object.getInt("humidity");
                        String status = object.getJSONArray("weather").getJSONObject(0).getString("description");
                        String main = object.getJSONArray("weather").getJSONObject(0).getString("main");
                        String icon = object.getJSONArray("weather").getJSONObject(0).getString("icon");
                        Double tempFeel = object.getJSONObject("feels_like").getDouble("day");
                        Weather weather = new Weather(location,temp,tempMin,tempMax,date,humidity,status,main,icon,tempFeel);
                        weatherList.add(weather);
                    }
                    DataManager.setFiveDayWeather(weatherList);
                    setList();

                } catch (JSONException e) {
                    Toast.makeText(getApplicationContext(), "C???p nh???t d??? b??o 5 ng??y kh??ng th??nh c??ng!", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        requestQueue.add(request);
    }
    private void updateData(String location){
        setListData(location);
        getData(location);
    }
    private void getData(String location) {
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        String url = "https://api.openweathermap.org/data/2.5/weather?id="+location+"&units=metric&appid=bdf219a3bd6e56842bbb2917f1ab37a7";
        StringRequest request = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);

                            JSONObject object = jsonObject.getJSONObject("main");
                            int temp = object.getInt("temp");

                            String icon = jsonObject.getJSONArray("weather").getJSONObject(0).getString("icon");
                            String url = "http://openweathermap.org/img/wn/"+icon+"@2x.png";
                            String status = jsonObject.getJSONArray("weather").getJSONObject(0).getString("description");
                            String main = jsonObject.getJSONArray("weather").getJSONObject(0).getString("main");
                            int humidity = object.getInt("humidity");
                            int tempMin = object.getInt("temp_min"), tempMax = object.getInt("temp_max");
                            String lc = jsonObject.getString("name");
                            long day = jsonObject.getLong("dt");
                            double tempFeel = object.getInt("feels_like");
                            Weather weather = new Weather(lc,temp,tempMin,tempMax,day,humidity,status,main,icon,tempFeel);
                            DataManager.setTodayWeather(weather);
                            setData();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        layout.setRefreshing(false);
                        Toast.makeText(getApplicationContext(), "C???p nh???t th??nh c??ng!", Toast.LENGTH_SHORT).show();
                        drawerLayout.setVisibility(View.VISIBLE);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), "C???p nh???t kh??ng th??nh c??ng, ki???m tra l???i m???ng!", Toast.LENGTH_SHORT).show();
            }
        });
        requestQueue.add(request);
    }

    private void setList(){
        List<Weather> list = new ArrayList<>();
        list               = DataManager.getFiveDayWeather();
        adapterWeather.setData(list);
    }
    private void setData() {
        Weather weather = DataManager.getTodayWeather();

        int temp = weather.getTemp();
        tvTemp.setText(String.valueOf(temp)+"??");

        String icon = weather.getIcon();
        String url = "https://openweathermap.org/img/wn/01d@2x.png";

        Picasso.with(this).load(url).into(imgWeather);
        Set set = statusData.keySet();
        boolean check = false;
        String status = weather.getStatus();
        for(Object key:set){
            if(status.equalsIgnoreCase(key.toString())){
                tvStatus.setText(statusData.get(key));
                check = true;
                break;
            }
        }
        String w = weather.getMain();
        if(!check){
            for(Object key:set){
                if(w.equalsIgnoreCase(key.toString())){
                    tvStatus.setText(statusData.get(key));
                    check = true;
                    break;
                }
            }
        }
        switch (w){
            case "Thunderstorm":
                mainLayout.setBackground(getDrawable(R.drawable.thunder));
                break;
            case "Drizzle":
                mainLayout.setBackground(getDrawable(R.drawable.dizzle));
                break;
            case "Rain":
                mainLayout.setBackground(getDrawable(R.drawable.rain));
                break;
            case "Clear":
                mainLayout.setBackground(getDrawable(R.drawable.clear));
                break;
            case "Clouds":
                mainLayout.setBackground(getDrawable(R.drawable.cloudy));
                break;
            default:
                mainLayout.setBackground(getDrawable(R.drawable.sun));
        }

        int humidity = weather.getHumidity();
        tvHumidity.setText("????? ???m: "+String.valueOf(humidity)+"%");

        int tempMin = weather.getTempMin(), tempMax = weather.getTempMax();
        if (tempMin<tempMax){
            tvTempRange.setText(String.valueOf(tempMin)+"??C - "+String.valueOf(tempMax)+"??C");
            tvTempRange.setVisibility(View.VISIBLE);
        }else{
            tvTempRange.setVisibility(View.GONE);
        }
        String lc = weather.getLocation();
        tvLocation.setText(selectLocation.getName());

        long day = weather.getDate();
        Date dt = new Date(day*1000);
        SimpleDateFormat formatDate = new SimpleDateFormat("EEE ,dd MMM");
        String date = formatDate.format(dt);
        tvDate.setText(date);
        SimpleDateFormat formatTime = new SimpleDateFormat("HH:mm, dd-MM-yyyy");
        String time = formatTime.format(dt);
        tvUpdate.setText("C???p nh???t "+ time);
        tvTempFeel.setText("C???m gi??c nh?? "+weather.getTempFeel()+" ??C");
        setList();
    }

    private void initUI() {
        tvUpdate = findViewById(R.id.tvUpdate);
        imgWeather = findViewById(R.id.imgWeather);
        tvTemp = findViewById(R.id.tvTemperature);
        tvTempRange = findViewById(R.id.tvTempRange);
        tvLocation = findViewById(R.id.tvLocation);
        tvStatus = findViewById(R.id.tvStatus);
        tvDate = findViewById(R.id.tvDate);
        tvHumidity = findViewById(R.id.tvHumidity);
        layout = findViewById(R.id.layoutSwipe);
        listWeather = findViewById(R.id.listDay);
        drawerLayout = findViewById(R.id.drawerLayout);
        imgMenu = findViewById(R.id.imgMenu);
        mainLayout = findViewById(R.id.mainLayout);
        listLocation = findViewById(R.id.listLocation);
        tvAddLocation = findViewById(R.id.addLocation);
        tvTempFeel = findViewById(R.id.tvTempFeel);
    }

}