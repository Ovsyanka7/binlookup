package com.example.binlookup.classes

class Bin (
    var number: Number = Number(),
    var scheme: String? = null,
    var type: String? = null,
    var brand: String? = null,
    var prepaid: Boolean? = null,
    var country: Country = Country(),
    var bank: Bank = Bank()
)