package com.example.binlookup

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.JsonReader
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.binlookup.classes.Bank
import com.example.binlookup.classes.Bin
import com.example.binlookup.classes.Country
import com.example.binlookup.classes.Number
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {

    private val bin = Bin()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val requestButton: Button = findViewById(R.id.RequestButton)
        val displayText: TextView = findViewById(R.id.tvSchemeTitle)

        requestButton.setOnClickListener {
            val dataField: EditText =  findViewById(R.id.DataField)
            val num = dataField.text

            val gitHubEndpoint = URL("https://lookup.binlist.net/$num")
            val connection: HttpsURLConnection = gitHubEndpoint.openConnection() as HttpsURLConnection
            connection.setRequestProperty("User-Agent", "my-rest-app-v0.1")

            thread {
                if (connection.responseCode == 200) {
                    readBin(connection)
                    runOnUiThread {
                        fillInValues()
                    }
                } else {
                    runOnUiThread {
                        displayText.text = "Ошибка"
                    }
                }
            }
        }
    }

    private fun fillInValues() {
        val tvScheme: TextView = findViewById(R.id.tvScheme)
        val tvType: TextView = findViewById(R.id.tvType)
        val tvBrand: TextView = findViewById(R.id.tvBrand)
        val tvPrepaid: TextView = findViewById(R.id.tvPrepaid)
        val tvCardNumberLength: TextView = findViewById(R.id.tvCardNumberLength)
        val tvCardNumberLuhn: TextView = findViewById(R.id.tvCardNumberLuhn)
        val tvCountry: TextView = findViewById(R.id.tvCountry)
        val tvCountryLatitudeAndLongitude: TextView = findViewById(R.id.tvCountryLatitudeAndLongitude)
        val tvBankAddress: TextView = findViewById(R.id.tvBankAddress)
        val tvBankWebsite: TextView = findViewById(R.id.tvBankWebsite)
        val tvBankPhoneNumber: TextView = findViewById(R.id.tvBankPhoneNumber)

        tvScheme.text = bin.scheme
        tvType.text = bin.type
        tvBrand.text = bin.brand
        tvPrepaid.text =  if (bin.prepaid == true) "Yes" else "No"
        tvCardNumberLength.text = bin.number.length.toString()
        tvCardNumberLuhn.text = bin.number.luhn.toString()
        tvCountry.text = bin.country.emoji + " " + bin.country.name
        tvCountryLatitudeAndLongitude.text = "(latitude: ${bin.country.latitude},\n" +
                "longitude: ${bin.country.longitude})"
        tvBankAddress.text = bin.bank.name + ", " + bin.bank.city
        tvBankWebsite.text = bin.bank.url
        tvBankPhoneNumber.text = bin.bank.phone
    }

    private fun readBin(connection: HttpsURLConnection) {
        val responseBody: InputStream = connection.inputStream
        val responseBodyReader = InputStreamReader(responseBody, "UTF-8")
        val jsonReader = JsonReader(responseBodyReader)

        jsonReader.beginObject()

        while (jsonReader.hasNext()) {
            try {
                when (jsonReader.nextName()) {
                    "number" -> {
                        bin.number = readNumber(jsonReader)
                    }
                    "scheme" -> {
                        bin.scheme = jsonReader.nextString()
                    }
                    "type" -> {
                        bin.type = jsonReader.nextString()
                    }
                    "brand" -> {
                        bin.brand = jsonReader.nextString()
                    }
                    "prepaid" -> {
                        bin.prepaid = jsonReader.nextBoolean()
                    }
                    "country" -> {
                        bin.country = readCountry(jsonReader)
                    }
                    "bank" -> {
                        bin.bank = readBank(jsonReader)
                    }
                    else -> {
                        jsonReader.skipValue()
                    }
                }
            } catch (_: Exception) { }
        }

        jsonReader.close()
        connection.disconnect()
    }

    private fun readNumber(jsonReader: JsonReader): Number {
        val number = Number()
        jsonReader.beginObject()
        while (jsonReader.hasNext()) {
            when (jsonReader.nextName()) {
                "length" -> {
                    number.length = jsonReader.nextInt()
                }
                "luhn" -> {
                    number.luhn = jsonReader.nextBoolean()
                }
                else -> {
                    jsonReader.skipValue()
                }
            }
        }
        jsonReader.endObject()
        return number
    }

    private fun readCountry(jsonReader: JsonReader): Country {
        val country = Country()
        jsonReader.beginObject()
        while (jsonReader.hasNext()) {
            when (jsonReader.nextName()) {
                "numeric" -> {
                    country.numeric = jsonReader.nextString()
                }
                "alpha2" -> {
                    country.alpha2 = jsonReader.nextString()
                }
                "name" -> {
                    country.name = jsonReader.nextString()
                }
                "emoji" -> {
                    country.emoji = jsonReader.nextString()
                }
                "currency" -> {
                    country.currency = jsonReader.nextString()
                }
                "latitude" -> {
                    country.latitude = jsonReader.nextInt()
                }
                "longitude" -> {
                    country.longitude = jsonReader.nextInt()
                }
                else -> {
                    jsonReader.skipValue()
                }
            }
        }
        jsonReader.endObject()
        return country
    }

    private fun readBank(jsonReader: JsonReader): Bank {
        val bank = Bank()
        jsonReader.beginObject()
        while (jsonReader.hasNext()) {
            when (jsonReader.nextName()) {
                "name" -> {
                    bank.name = jsonReader.nextString()
                }
                "url" -> {
                    bank.url = jsonReader.nextString()
                }
                "phone" -> {
                    bank.phone = jsonReader.nextString()
                }
                "city" -> {
                    bank.city = jsonReader.nextString()
                }
                else -> {
                    jsonReader.skipValue()
                }
            }
        }
        jsonReader.endObject()
        return bank
    }

    fun onClickMap(view: View) {
        val latitude: Float? = bin.country.latitude?.toFloat()
        val longitude: Float? = bin.country.longitude?.toFloat()
        if (latitude != null && longitude != null) {
            val uri: String =
                java.lang.String.format(Locale.ENGLISH, "geo:%f,%f", latitude, longitude)

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            this.startActivity(intent)
        }
    }

    fun onClickSite(view: View) {
        val url = "http://" + bin.bank.url
        if (url != null) {
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }
    }
}