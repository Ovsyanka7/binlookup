package com.example.binlookup

import android.util.JsonReader
import com.example.binlookup.classes.Bank
import com.example.binlookup.classes.Bin
import com.example.binlookup.classes.Country
import com.example.binlookup.classes.Number
import java.io.InputStream
import java.io.InputStreamReader
import javax.net.ssl.HttpsURLConnection

// Вся логика расшифровки Json в объект Bin перенесена в этот класс.
class JsonReader {
    fun readBin(connection: HttpsURLConnection): Bin {
        val bin = Bin()
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
        return bin
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
}