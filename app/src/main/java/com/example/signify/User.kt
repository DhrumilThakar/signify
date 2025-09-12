package com.example.signify

class User(var id : Int, var email : String, var name : String, var password : String) {

    override fun toString(): String {
        return "User($id, $email, $name, $password)"
    }
}