package com.example.sampleapp

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper.myLooper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.sampleapp.models.WeatherModel
import com.example.sampleapp.network.WeatherService
import com.google.android.gms.location.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.Exception
import java.net.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    val CITY: String = "Ikeja, Lagos"
    val API: String = ""
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var dialog: ProgressBar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        dialog = findViewById(R.id.dialog)
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        if (!isLocationEnabled()) {
            Toast.makeText(
                this,
                "Your location provider is turned off. Please turn it on.",
                Toast.LENGTH_SHORT
            ).show()

            // This will redirect you to settings from where you need to turn on the location provider.
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
        getLocationPermission()
    }


    @SuppressLint("SetTextI18n")
    private fun buildUI(weatherList: WeatherModel) {
        for (i in weatherList.weather.indices) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                findViewById<TextView>(R.id.temp).text =
                    String.format(
                        "%.0f",
                        weatherList.main.temp
                    ) + getUnit(application.resources.configuration.locales.toString())
            }
            findViewById<TextView>(R.id.status).text = weatherList.weather[i].main
            findViewById<TextView>(R.id.location).text =
                weatherList.name + ", " + weatherList.sys.country
            findViewById<TextView>(R.id.updated_at).text =
                "Updated at " + unixTime(weatherList.date)
            findViewById<TextView>(R.id.sunset).text = unixTime(weatherList.sys.sunset)
            findViewById<TextView>(R.id.sunrise).text = unixTime(weatherList.sys.sunrise)

            val imageView = findViewById<ImageView>(R.id.imageIcon)
            when (weatherList.weather[i].icon) {
                "01d" -> imageView.setImageResource(R.drawable.sunny)
                "02d" -> imageView.setImageResource(R.drawable.cloud)
                "03d" -> imageView.setImageResource(R.drawable.cloud)
                "04d" -> imageView.setImageResource(R.drawable.cloud)
                "04n" -> imageView.setImageResource(R.drawable.cloud)
                "10d" -> imageView.setImageResource(R.drawable.rain)
                "11d" -> imageView.setImageResource(R.drawable.storm)
                "13d" -> imageView.setImageResource(R.drawable.snowflake)
                "01n" -> imageView.setImageResource(R.drawable.cloud)
                "02n" -> imageView.setImageResource(R.drawable.cloud)
                "03n" -> imageView.setImageResource(R.drawable.cloud)
                "10n" -> imageView.setImageResource(R.drawable.cloud)
                "11n" -> imageView.setImageResource(R.drawable.rain)
                "13n" -> imageView.setImageResource(R.drawable.snowflake)
            }


        }
    }

    private fun getUnit(value: String): String {
        var value = "°C"
        if ("US" == value || "LR" == value || "MM" == value) {
            value = "°F"
        }
        return value
    }

    @SuppressLint("SimpleDateFormat")
    private fun unixTime(timex: Long): String {
        val date = Date(timex * 1000L)
        val sdf = SimpleDateFormat("HH:mm", Locale.ENGLISH)
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }

    //////////////////////////////////////////////////////////////////////////////////////
    /**
     * A function used to show the alert dialog when the permissions are denied and need to allow it from settings app info.
     */
    private fun getLocationPermission() = Dexter.withActivity(this).withPermissions(
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    ).withListener(
        object : MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                if (report!!.areAllPermissionsGranted()) {
                    requestUserLocation()
                }
                if (report.isAnyPermissionPermanentlyDenied) {
                    Toast.makeText(
                        this@MainActivity,
                        "You have denied location permission. Please allow it is mandatory.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onPermissionRationaleShouldBeShown(
                permissions: MutableList<PermissionRequest>?,
                token: PermissionToken?
            ) {
                AlertDialog.Builder(this@MainActivity).setMessage("Please turn on your location")
                    .setPositiveButton("GO TO SETTINGS") { _, _ ->
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", packageName, null)
                            intent.data = uri
                            startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            e.printStackTrace()
                        }
                    }.setNegativeButton("CANCEL") { dialog, _ ->
                        dialog.dismiss()
                    }.show()
            }
        }
    ).onSameThread().check()


    /////////////////////////////////////////////////////////////////////
    /**
     * A function to request the current location. Using the fused location provider client.
     */
    @SuppressLint("MissingPermission")
    private fun requestUserLocation() {
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mFusedLocationProviderClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback, myLooper()
        )
    }


    ////////////////////////////////////////////////////////////////////////
    /**
     * A location callback object of fused location provider client where we will get the current location details.
     */
    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            val mLastLocation: Location = locationResult.lastLocation
            val latitude = mLastLocation.latitude
            val longitude = mLastLocation.longitude
            getLocationWeatherDetails(latitude, longitude)
        }
    }


    ///////////////////////////////////////////////////////////////////
    /**
     * Function is used to get the weather details of the current location based on the latitude longitude
     */
    private fun getLocationWeatherDetails(latitude: Double, longitude: Double) {
        if (Constants.isNetWorkAvailable(this)) {
            val retrofit: Retrofit = Retrofit.Builder().baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build()

            val service: WeatherService =
                retrofit.create(WeatherService::class.java)
            val listCall: Call<WeatherModel> = service.getWeather(
                latitude, longitude, Constants.METRIC_UNIT, Constants.API_KEY
            )
            dialog.visibility = View.VISIBLE
            listCall.enqueue(object : Callback<WeatherModel> {
                override fun onResponse(
                    call: Call<WeatherModel>,
                    response: Response<WeatherModel>
                ) {
                    if (response.isSuccessful) {
                        dialog.visibility = View.GONE
                        val weatherList: WeatherModel = response.body()!!
                        buildUI(weatherList)

                    } else {
                        Log.i("", "${response.code()}")
                    }
                }

                override fun onFailure(call: Call<WeatherModel>, t: Throwable) {
                    Log.e("Error", t.message.toString())
                    dialog.visibility = View.GONE
                }

            })

        } else {
            AlertDialog.Builder(this).setMessage("Internet connection error")
                .setPositiveButton("OK") { _, _ ->
                    try {
                        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
                        val uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        e.printStackTrace()
                    }
                }.setNegativeButton("CANCEL") { dialog, _ ->
                    dialog.dismiss()
                }.show()
        }
    }


    ///////////////////////////////////////////////////////////////////////////////
    /**
     * A function which is used to verify that the location or GPS is enable or not of the user's device.
     */
    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }


    private inner class NetworkRequest() : AsyncTask<Any, Void, String>() {
        private var dialog: ProgressBar = findViewById(R.id.dialog)
        override fun onPreExecute() {
            dialog.visibility = View.VISIBLE
            super.onPreExecute()
        }

        override fun doInBackground(vararg params: Any?): String? {
            var response: String? = ""
            try {
                response =
                    URL("https://api.openweathermap.org/data/2.5/weather?q=$CITY&units=metric&appid=$API").readText(
                        Charsets.UTF_8
                    )

            } catch (e: Exception) {
                e.printStackTrace()
            }
            return response
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            try {
                val jsonObject = JSONObject(result)
                val main = jsonObject.getJSONObject("main")
                val sys = jsonObject.getJSONObject("sys")
                val wind = jsonObject.getJSONObject("wind")
                val weather = jsonObject.getJSONArray("weather").getJSONObject(0)


                val updatedAt: Long = jsonObject.getLong("dt")
                val updatedAtText =
                    "Updated at: " + java.text.SimpleDateFormat("dd/MM/yyy hh:mm a", Locale.ENGLISH)
                        .format(Date(updatedAt * 1000))
                val temp = main.getString("temp") + "°C"
                val tempMin = "Min Temp: " + main.getString("temp_min") + "°C"
                val tempMax = "Max Temp: " + main.getString("temp_max") + "°C"
                val pressure = main.getString("pressure")
                val humidity = main.getString("humidity")

                val sunrise: Long = sys.getLong("sunrise")
                val sunset: Long = sys.getLong("sunset")
                val windSpeed = wind.getString("speed")
                val weatherDescription = weather.getString("description")
                val address = jsonObject.getString("name") + ", " + sys.getString("country")

                findViewById<TextView>(R.id.location).text = address
                findViewById<TextView>(R.id.updated_at).text = updatedAtText
                findViewById<TextView>(R.id.status).text = weatherDescription.capitalize()
                findViewById<TextView>(R.id.temp).text = temp
                findViewById<TextView>(R.id.temp_min).text = tempMin
                findViewById<TextView>(R.id.temp_max).text = tempMax
                findViewById<TextView>(R.id.sunrise).text =
                    java.text.SimpleDateFormat("hh:mm a", Locale.ENGLISH)
                        .format(Date(sunrise * 1000))
                findViewById<TextView>(R.id.sunset).text =
                    java.text.SimpleDateFormat("hh:mm a", Locale.ENGLISH)
                        .format(Date(sunset * 1000))
                findViewById<TextView>(R.id.wind).text = windSpeed
                findViewById<TextView>(R.id.humidity).text = humidity
                findViewById<TextView>(R.id.pressure).text = pressure

                dialog.visibility = View.GONE


            } catch (e: SocketException) {
                findViewById<TextView>(R.id.errorText).text = e.toString()
            }

        }


    }
}