package com.easywiki.data.api

class ApiException(val httpCode: Int, message: String) : Exception(message)

class WikiConflictException : Exception("页面已被他人更新，请刷新")
