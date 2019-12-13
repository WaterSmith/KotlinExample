package ru.skillbranch.kotlinexample.extensions

fun <T> List<T>.dropLastUntil(predicate: (T) -> Boolean): List<T>{
    return dropLastWhile{predicate(it).not()}.dropLast(1)
}
