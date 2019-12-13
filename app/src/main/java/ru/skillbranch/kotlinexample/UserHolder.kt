package ru.skillbranch.kotlinexample

object UserHolder {
    private val map = mutableMapOf<String, User>()

    fun loginUser(login:String, password: String):String?{
        val user = map[login.mailToLogin()]?:map[login.phoneToLogin()]

        return user?.run {
            if (this.checkPassword(password)) this.userInfo
            else null
        }
    }

    fun registerUser(fullName: String, email: String, password: String): User {
        if(map[email.mailToLogin()]!=null) {
            throw IllegalArgumentException("A user with this email already exists")
        } else {

            return User.makeUser(fullName = fullName, email = email, password = password)
                .also { user -> map[user.login] = user }
        }
    }

    fun registerUserByPhone(fullName: String, rawPhone: String): User {
        if(map[rawPhone.phoneToLogin()]!=null){throw IllegalArgumentException("A user with this phone already exists")}
        //IllegalArgumentException("Enter a valid phone number starting with a + and containing 11 digits")

        return User.makeUser(fullName = fullName, phone = rawPhone)
            .also { user ->  map[user.login] = user}
    }

    fun importUsers(list:List<String>):List<User>{
        return list.map {
            User.importUser(it).also { user -> map[user.login] = user }
        }
    }

    fun requestAccessCode(rawPhone: String) {
        val user = map[rawPhone.phoneToLogin()]
        user?:throw IllegalArgumentException("Unregistred phone number")
        user.requestAccessCode()
    }

    private fun String.phoneToLogin():String{
        return replace("[^+\\d]".toRegex(),"")
    }
    private fun String.mailToLogin():String{
        return trim().toLowerCase()
    }
    fun dropBase(){
        map.clear()
    }


}