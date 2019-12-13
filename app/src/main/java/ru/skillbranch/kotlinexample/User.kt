package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import ru.skillbranch.kotlinexample.User.Factory.fullNameToPair
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.IllegalArgumentException

class User private constructor(
    private val firstName:String,
    private val lastName:String?,
    var email:String? = null,
    var rawPhone:String? = null,
    var meta: Map<String,Any>? = null)
{
    val userInfo:String

    private val fullName:String
        get() = listOfNotNull(firstName, lastName).joinToString ( " " ).capitalize()

    private val initials:String
        get() = listOfNotNull(firstName, lastName).map { it.first().toUpperCase() }.joinToString ( " " )

    private var phone:String? = null
        set(value){
            value?:return
            val normal = value.replace("[^+\\d]".toRegex(),"")
            if(((normal.length == 12) && (normal[0]=='+') && (normal.count{it == '+'}==1)).not()) {
                throw IllegalArgumentException("Enter a valid phone number starting with a + and containing 11 digits")
            }
            field = normal
        }


    private var _login:String? = null
    internal var login:String
        set(value) {_login = value?.toLowerCase()}
        get() = _login!!

    private var _salt:String? = null
    private val salt: String by lazy {_salt?:ByteArray(16).also { SecureRandom().nextBytes(it)}.toString()}

    private lateinit var passwordHash:String


    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode: String? = null

    constructor(firstName: String,
                lastName: String?,
                email: String,
                password: String ):this (firstName, lastName, email = email, meta = mapOf("auth" to "password")) {
        println("Secondary mail constructor")
        passwordHash = encrypt(password)
    }

    private constructor(firstName: String,
                lastName: String?,
                email: String?,
                rawPhone: String?,
                sequrity:Pair<String,String>, meta: Map<String, Any>):
            this (firstName, lastName, email = email, rawPhone = rawPhone, meta = meta) {
        println("Secondary import constructor")
        _salt = sequrity.first
        passwordHash = sequrity.second
    }

    constructor(firstName: String,
                lastName: String?,
                rawPhone: String?):this (firstName, lastName, rawPhone = rawPhone, meta = mapOf("auth" to "sms")) {
        println("Secondary phone constructor")
        requestAccessCode()
    }

    init {
        println("First init block, primary constructor was called")
        check(firstName.isNotBlank()){"First name must be not blank!"}
        check(email.isNullOrBlank().not() || rawPhone.isNullOrBlank().not()){"Email or phone must be not blank"}

        phone = rawPhone
        login = email?:phone!!

        userInfo = """
            firstName: $firstName
            lastName: $lastName
            login: $login
            fullName: $fullName
            initials: $initials
            email: $email
            phone: $phone
            meta: $meta
            """.trimIndent()
    }

    fun checkPassword(pass:String) = encrypt(pass) == passwordHash

    fun changePassword(oldPass:String, newPass:String){
        if (checkPassword(oldPass)) passwordHash = encrypt(newPass)
        else throw IllegalArgumentException("The entered password does not match the current password")
    }

    fun requestAccessCode(){
        val code = generateAccessCode()
        passwordHash = encrypt(code)
        accessCode = code
        sendAccessCodeToUser(phone!!, code)
    }

    private fun encrypt(password: String) = salt.plus(password).md5()

    private fun generateAccessCode(): String {
        val possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

        return StringBuilder().apply {
            repeat(6){
                (possible.indices).random().also {index ->
                    append(possible[index])
                }
            }
        }.toString()
    }

    private fun sendAccessCodeToUser(phone: String, code: String) {
        println("..... sending access code $code on $phone")
    }

    private fun String.md5():String{
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(toByteArray()) //16 byte
        val hexString = BigInteger(1, digest).toString(16)
        return hexString.padStart(32, '0')
    }

    companion object Factory{
        fun makeUser(
            fullName:String,
            email:String? = null,
            password:String? = null,
            phone:String?=null
        ):User {
            val (firstName, lastName) = fullName.fullNameToPair()

            return when{
                !phone.isNullOrBlank() -> User(firstName, lastName, phone)
                !email.isNullOrBlank() && !password.isNullOrBlank() -> User(firstName, lastName, email, password)
                else -> throw IllegalArgumentException("Email or phone must be not null or blank")
            }
        }

        fun importUser(userData:String):User {
            //Полное имя пользователя;
            // email;
            // соль:хеш пароля;
            // телефон (Пример: " John Doe ;JohnDoe@unknow.com;[B@7591083d:c6adb4becdc64e92857e1e2a0fd6af84;;)
            val splittedData = userData.split(";")
            val (firstName, lastName) = splittedData[0].fullNameToPair()
            val mail = splittedData[1]
            val phone = splittedData[3]
            val sequrity = splittedData[2].split(":").run{first() to last()}
            val meta = mapOf("auth" to if(mail.isNotBlank()) "password" else "sms")
            return User(firstName, lastName, mail, phone, sequrity, meta)
        }

        private fun String.fullNameToPair():Pair<String, String?>{
            return this.split(" ")
                .filter { it.isNotBlank() }
                .run{
                    when (size){
                        1 -> first() to null
                        2 -> first() to last()
                        else -> throw IllegalArgumentException("Fullname mus be contain onli first name and last name, current split result ${this@fullNameToPair}")
                    }
                }
        }
    }
}