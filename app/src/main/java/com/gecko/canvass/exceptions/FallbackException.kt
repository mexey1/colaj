package com.gecko.canvass.exceptions

class FallbackException : Exception{
    constructor(message:String):super(message)
    constructor(cause:Throwable):super(cause)
}